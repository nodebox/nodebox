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

  const points: PathPoint[] = [
    pp(cx, cy - ry),
    // right quadrant
    pp(cx + kx, cy - ry, 'curveData'),
    pp(cx + rx, cy - ky, 'curveData'),
    pp(cx + rx, cy, 'curveTo'),
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
  const endRad = (startAngle + degrees) * Math.PI / 180;

  // Approximate the arc with line segments (for simplicity, use many segments)
  const steps = Math.max(4, Math.ceil(Math.abs(degrees) / 5));
  const points: PathPoint[] = [];

  if (arcType === 'pie') {
    points.push(pp(cx, cy));
  }

  for (let i = 0; i <= steps; i++) {
    const t = i / steps;
    const angle = startRad + t * (endRad - startRad);
    points.push(pp(cx + rx * Math.cos(angle), cy + ry * Math.sin(angle)));
  }

  if (arcType === 'pie') {
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
  // Control point perpendicular to the line at parameter t
  const mx = point1.x + (point2.x - point1.x) * t;
  const my = point1.y + (point2.y - point1.y) * t;
  const dx = point2.x - point1.x;
  const dy = point2.y - point1.y;
  const len = Math.sqrt(dx * dx + dy * dy);
  if (len === 0) return makeOpenPath([pp(point1.x, point1.y)], null);
  const nx = -dy / len;
  const ny = dx / len;
  const cx = mx + nx * distance;
  const cy = my + ny * distance;

  const points: PathPoint[] = [
    pp(point1.x, point1.y),
    pp(cx, cy, 'quadData'),
    pp(point2.x, point2.y, 'quadTo'),
  ];
  return makeOpenPath(points, null);
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
  while (i < tokens.length) {
    const cmd = tokens[i++];
    switch (cmd) {
      case 'M':
        path = moveTo(path, parseFloat(tokens[i++]), parseFloat(tokens[i++]));
        break;
      case 'L':
        path = lineTo(path, parseFloat(tokens[i++]), parseFloat(tokens[i++]));
        break;
      case 'C':
        path = curveTo(path,
          parseFloat(tokens[i++]), parseFloat(tokens[i++]),
          parseFloat(tokens[i++]), parseFloat(tokens[i++]),
          parseFloat(tokens[i++]), parseFloat(tokens[i++]),
        );
        break;
      case 'Z':
        path = closePath(path);
        break;
      default:
        // Skip unknown commands
        break;
    }
  }
  return path;
}

// ─── group ─────────────────────────────────────────────
export function group(shapes: Path[]): Path[] {
  return shapes;
}

// ─── doNothing ─────────────────────────────────────────
export function doNothing(): null {
  return null;
}
