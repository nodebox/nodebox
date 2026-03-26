import type { Point } from '../geometry/point.js';
import type { Color } from '../geometry/color.js';
import type { Path, Contour, PathPoint } from '../geometry/path.js';
import { createPath, DEFAULT_FILL, moveTo, lineTo, curveTo, closePath } from '../geometry/path.js';

// ─── Helper ────────────────────────────────────────────
function makeClosedPath(points: PathPoint[], fill: Color | null = DEFAULT_FILL): Path {
  return { contours: [{ points, closed: true }], fill, stroke: null, strokeWidth: 1 };
}

function makeOpenPath(points: PathPoint[], fill: Color | null = DEFAULT_FILL): Path {
  return { contours: [{ points, closed: false }], fill, stroke: null, strokeWidth: 1 };
}

function pp(x: number, y: number, type: PathPoint['type'] = 'lineTo'): PathPoint {
  return { point: { x, y }, type };
}

// ─── rect ──────────────────────────────────────────────
export function rect(
  position: Point, width: number, height: number, roundness: Point,
): Path {
  const x = position.x - width / 2;
  const y = position.y - height / 2;

  if (roundness.x === 0 && roundness.y === 0) {
    return makeClosedPath([
      pp(x, y), pp(x + width, y), pp(x + width, y + height), pp(x, y + height),
    ]);
  }

  // Rounded rectangle with bezier corners
  const rx = Math.min(Math.abs(roundness.x), width / 2);
  const ry = Math.min(Math.abs(roundness.y), height / 2);
  // Kappa for quarter-circle approximation
  const k = 0.5522847498;
  const kx = rx * k;
  const ky = ry * k;

  const points: PathPoint[] = [
    pp(x + rx, y),
    pp(x + width - rx, y),
    // top-right corner
    pp(x + width - rx + kx, y, 'curveData'),
    pp(x + width, y + ry - ky, 'curveData'),
    pp(x + width, y + ry, 'curveTo'),
    pp(x + width, y + height - ry),
    // bottom-right corner
    pp(x + width, y + height - ry + ky, 'curveData'),
    pp(x + width - rx + kx, y + height, 'curveData'),
    pp(x + width - rx, y + height, 'curveTo'),
    pp(x + rx, y + height),
    // bottom-left corner
    pp(x + rx - kx, y + height, 'curveData'),
    pp(x, y + height - ry + ky, 'curveData'),
    pp(x, y + height - ry, 'curveTo'),
    pp(x, y + ry),
    // top-left corner
    pp(x, y + ry - ky, 'curveData'),
    pp(x + rx - kx, y, 'curveData'),
    pp(x + rx, y, 'curveTo'),
  ];
  return makeClosedPath(points);
}

// ─── ellipse ───────────────────────────────────────────
export function ellipse(position: Point, width: number, height: number): Path {
  const cx = position.x;
  const cy = position.y;
  const rx = width / 2;
  const ry = height / 2;
  // Approximate ellipse with 4 cubic bezier segments
  const k = 0.5522847498;
  const kx = rx * k;
  const ky = ry * k;

  // Start at rightmost point going clockwise — matches Java's Ellipse2D PathIterator
  const points: PathPoint[] = [
    pp(cx + rx, cy),
    // bottom quadrant
    pp(cx + rx, cy + ky, 'curveData'),
    pp(cx + kx, cy + ry, 'curveData'),
    pp(cx, cy + ry, 'curveTo'),
    // left quadrant
    pp(cx - kx, cy + ry, 'curveData'),
    pp(cx - rx, cy + ky, 'curveData'),
    pp(cx - rx, cy, 'curveTo'),
    // top quadrant
    pp(cx - rx, cy - ky, 'curveData'),
    pp(cx - kx, cy - ry, 'curveData'),
    pp(cx, cy - ry, 'curveTo'),
    // right quadrant (back to start)
    pp(cx + kx, cy - ry, 'curveData'),
    pp(cx + rx, cy - ky, 'curveData'),
    pp(cx + rx, cy, 'curveTo'),
  ];
  return makeClosedPath(points);
}

// ─── arc ───────────────────────────────────────────────
export function arc(
  position: Point, width: number, height: number,
  startAngle: number, degrees: number,
  arcType: string,
): Path {
  const cx = position.x;
  const cy = position.y;
  const rx = width / 2;
  const ry = height / 2;

  // Convert to radians
  const startRad = startAngle * Math.PI / 180;
  const sweepRad = degrees * Math.PI / 180;

  // Approximate arc with cubic Bezier segments (90° max per segment)
  const numSegs = Math.max(1, Math.ceil(Math.abs(degrees) / 90));
  const segAngle = sweepRad / numSegs;

  const points: PathPoint[] = [];

  // Start point: for pie, start at the arc edge (closePath will return to center)
  const arcStartX = cx + rx * Math.cos(startRad);
  const arcStartY = cy + ry * Math.sin(startRad);

  if (arcType === 'pie') {
    // Pie: moveTo arc start (Java's Arc2D.PIE starts at the arc edge)
    points.push(pp(arcStartX, arcStartY));
  } else {
    points.push(pp(arcStartX, arcStartY));
  }

  for (let i = 0; i < numSegs; i++) {
    const a1 = startRad + i * segAngle;
    const a2 = a1 + segAngle;
    // Cubic bezier approximation of an arc segment
    const alpha = Math.sin(segAngle) * (Math.sqrt(4 + 3 * Math.pow(Math.tan(segAngle / 2), 2)) - 1) / 3;

    const x1 = cx + rx * Math.cos(a1);
    const y1 = cy + ry * Math.sin(a1);
    const x2 = cx + rx * Math.cos(a2);
    const y2 = cy + ry * Math.sin(a2);

    const cp1x = x1 - alpha * rx * Math.sin(a1);
    const cp1y = y1 + alpha * ry * Math.cos(a1);
    const cp2x = x2 + alpha * rx * Math.sin(a2);
    const cp2y = y2 - alpha * ry * Math.cos(a2);

    points.push(pp(cp1x, cp1y, 'curveData'));
    points.push(pp(cp2x, cp2y, 'curveData'));
    points.push(pp(x2, y2, 'curveTo'));
  }

  if (arcType === 'pie') {
    // Line to center — closePath will connect back to arc start
    points.push(pp(cx, cy));
  }

  const closed = arcType !== 'open';
  return {
    contours: [{ points, closed }],
    fill: DEFAULT_FILL,
    stroke: null,
    strokeWidth: 1,
  };
}

// ─── line ──────────────────────────────────────────────
export function line(point1: Point, point2: Point, points: number): Path {
  const n = Math.max(2, points);
  const pathPoints: PathPoint[] = [];
  for (let i = 0; i < n; i++) {
    const t = n <= 1 ? 0 : i / (n - 1);
    pathPoints.push(pp(
      point1.x + (point2.x - point1.x) * t,
      point1.y + (point2.y - point1.y) * t,
    ));
  }
  return makeOpenPath(pathPoints, null);
}

// ─── lineAngle ─────────────────────────────────────────
export function lineAngle(position: Point, angle: number, distance: number, points: number): Path {
  const rad = angle * Math.PI / 180;
  const endX = position.x + Math.cos(rad) * distance;
  const endY = position.y + Math.sin(rad) * distance;
  return line(position, { x: endX, y: endY }, points);
}

// ─── polygon ───────────────────────────────────────────
export function polygon(position: Point, radius: number, sides: number, align: boolean): Path {
  const n = Math.max(3, Math.round(sides));
  const points: PathPoint[] = [];
  for (let i = 0; i < n; i++) {
    let angle = (i / n) * Math.PI * 2;
    if (align) {
      angle -= Math.PI / 2;
    }
    points.push(pp(
      position.x + Math.cos(angle) * radius,
      position.y + Math.sin(angle) * radius,
    ));
  }
  return makeClosedPath(points);
}

// ─── star ──────────────────────────────────────────────
export function star(position: Point, pointsCount: number, outer: number, inner: number): Path {
  const n = Math.max(2, Math.round(pointsCount));
  const points: PathPoint[] = [];
  for (let i = 0; i < n * 2; i++) {
    const angle = (i / (n * 2)) * Math.PI * 2 - Math.PI / 2;
    const r = i % 2 === 0 ? outer : inner;
    points.push(pp(
      position.x + Math.cos(angle) * r,
      position.y + Math.sin(angle) * r,
    ));
  }
  return makeClosedPath(points);
}

// ─── grid ──────────────────────────────────────────────
export function grid(
  columns: number, rows: number, width: number, height: number, position: Point,
): Point[] {
  const cols = Math.max(1, Math.round(columns));
  const rowCount = Math.max(1, Math.round(rows));
  const result: Point[] = [];
  const colStep = cols > 1 ? width / (cols - 1) : 0;
  const rowStep = rowCount > 1 ? height / (rowCount - 1) : 0;
  const startX = position.x - width / 2;
  const startY = position.y - height / 2;

  for (let r = 0; r < rowCount; r++) {
    for (let c = 0; c < cols; c++) {
      result.push({
        x: cols > 1 ? startX + c * colStep : position.x,
        y: rowCount > 1 ? startY + r * rowStep : position.y,
      });
    }
  }
  return result;
}

// ─── connect ───────────────────────────────────────────
export function connect(points: Point[], closed: boolean): Path {
  if (points.length === 0) return createPath();
  const pathPoints: PathPoint[] = points.map(p => pp(p.x, p.y));
  return {
    contours: [{ points: pathPoints, closed }],
    fill: closed ? DEFAULT_FILL : null,
    stroke: null,
    strokeWidth: 1,
  };
}

// ─── quadCurve ─────────────────────────────────────────
export function quadCurve(point1: Point, point2: Point, t: number, distance: number): Path {
  // t is in range 0-100, normalize to 0-1
  const nt = t / 100;
  // Point on the line at parameter t
  const cx = point1.x + nt * (point2.x - point1.x);
  const cy = point1.y + nt * (point2.y - point1.y);
  // Perpendicular direction (angle + 90°)
  const a = Math.atan2(point2.y - point1.y, point2.x - point1.x) + Math.PI / 2;
  const qx = cx + Math.cos(a) * distance;
  const qy = cy + Math.sin(a) * distance;

  // Convert quadratic control point to cubic (matching Python/Java output)
  const c1x = point1.x + (2 / 3) * (qx - point1.x);
  const c1y = point1.y + (2 / 3) * (qy - point1.y);
  const c2x = point2.x + (2 / 3) * (qx - point2.x);
  const c2y = point2.y + (2 / 3) * (qy - point2.y);

  const points: PathPoint[] = [
    pp(point1.x, point1.y),
    pp(c1x, c1y, 'curveData'),
    pp(c2x, c2y, 'curveData'),
    pp(point2.x, point2.y, 'curveTo'),
  ];
  const path = makeOpenPath(points, null);
  // Python sets fill=None, stroke=BLACK by default
  return { ...path, fill: null, stroke: { r: 0, g: 0, b: 0, a: 1 }, strokeWidth: 1 };
}

// ─── link ──────────────────────────────────────────────
export function link(
  shape1: Path[], shape2: Path[],
  orientation: string,
): Path {
  // Get bounding boxes of both shapes
  const bounds1 = shapeBounds(shape1);
  const bounds2 = shapeBounds(shape2);
  if (!bounds1 || !bounds2) return createPath();

  const points: PathPoint[] = [];
  if (orientation === 'horizontal') {
    const y1mid = bounds1.y + bounds1.height / 2;
    const y2mid = bounds2.y + bounds2.height / 2;
    const x1 = bounds1.x + bounds1.width;
    const x2 = bounds2.x;
    const mx = (x1 + x2) / 2;
    points.push(pp(x1, y1mid));
    points.push(pp(mx, y1mid, 'curveData'));
    points.push(pp(mx, y2mid, 'curveData'));
    points.push(pp(x2, y2mid, 'curveTo'));
  } else {
    const x1mid = bounds1.x + bounds1.width / 2;
    const x2mid = bounds2.x + bounds2.width / 2;
    const y1 = bounds1.y + bounds1.height;
    const y2 = bounds2.y;
    const my = (y1 + y2) / 2;
    points.push(pp(x1mid, y1));
    points.push(pp(x1mid, my, 'curveData'));
    points.push(pp(x2mid, my, 'curveData'));
    points.push(pp(x2mid, y2, 'curveTo'));
  }
  return makeOpenPath(points, null);
}

function shapeBounds(paths: Path[]): { x: number; y: number; width: number; height: number } | null {
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const path of paths) {
    for (const contour of path.contours) {
      for (const pp of contour.points) {
        if (pp.point.x < minX) minX = pp.point.x;
        if (pp.point.y < minY) minY = pp.point.y;
        if (pp.point.x > maxX) maxX = pp.point.x;
        if (pp.point.y > maxY) maxY = pp.point.y;
      }
    }
  }
  if (!isFinite(minX)) return null;
  return { x: minX, y: minY, width: maxX - minX, height: maxY - minY };
}

// ─── makePoint ─────────────────────────────────────────
export function makePoint(x: number, y: number): Point {
  return { x, y };
}

// ─── freehand ──────────────────────────────────────────
export function freehand(pathData: string): Path {
  // Parse internal path data format: "M x y L x y C cx1 cy1 cx2 cy2 x y Z"
  if (!pathData || pathData.trim() === '') return createPath();
  const tokens = pathData.trim().split(/[\s,]+/);
  let path = createPath();
  let i = 0;
  let lastCmd = '';
  while (i < tokens.length) {
    let cmd = tokens[i];
    // Check if this token is a number — if so, use implicit command
    if (!isNaN(parseFloat(cmd)) && lastCmd) {
      // After M, implicit coordinates are L (lineTo) per SVG spec
      cmd = lastCmd === 'M' ? 'L' : lastCmd;
    } else {
      i++; // consume the command letter
    }
    switch (cmd) {
      case 'M':
        path = moveTo(path, parseFloat(tokens[i++]), parseFloat(tokens[i++]));
        lastCmd = 'M';
        break;
      case 'L':
        path = lineTo(path, parseFloat(tokens[i++]), parseFloat(tokens[i++]));
        lastCmd = 'L';
        break;
      case 'C':
        path = curveTo(path,
          parseFloat(tokens[i++]), parseFloat(tokens[i++]),
          parseFloat(tokens[i++]), parseFloat(tokens[i++]),
          parseFloat(tokens[i++]), parseFloat(tokens[i++]),
        );
        lastCmd = 'C';
        break;
      case 'Z':
        path = closePath(path);
        lastCmd = '';
        break;
      default:
        i++; // skip unknown token
        break;
    }
  }
  return path;
}

// ─── group ─────────────────────────────────────────────
export function group(shapes: Path[]): Path {
  // Merge all paths into a single multi-contour path (like Java's Geometry)
  if (shapes.length === 0) return createPath();
  if (shapes.length === 1) return shapes[0];
  const contours: Contour[] = [];
  let fill = shapes[0].fill;
  let stroke = shapes[0].stroke;
  let strokeWidth = shapes[0].strokeWidth;
  for (const shape of shapes) {
    contours.push(...shape.contours);
  }
  return { contours, fill, stroke, strokeWidth };
}

// ─── doNothing ─────────────────────────────────────────
export function doNothing(): null {
  return null;
}
