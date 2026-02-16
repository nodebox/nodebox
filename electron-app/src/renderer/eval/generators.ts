import type { Point, Color, Path, Contour, PathPoint } from '../types/geometry';

const DEFAULT_FILL: Color = { r: 0, g: 0, b: 0, a: 1 };
const KAPPA = 0.5522847498;

export function rectPath(
  position: Point,
  width: number,
  height: number,
  _roundness: Point,
): Path {
  const hw = width / 2;
  const hh = height / 2;
  const cx = position.x;
  const cy = position.y;

  const contour: Contour = {
    points: [
      { x: cx - hw, y: cy - hh, pointType: 'moveTo' },
      { x: cx + hw, y: cy - hh, pointType: 'lineTo' },
      { x: cx + hw, y: cy + hh, pointType: 'lineTo' },
      { x: cx - hw, y: cy + hh, pointType: 'lineTo' },
    ],
    closed: true,
  };

  return {
    contours: [contour],
    fill: { ...DEFAULT_FILL },
    stroke: null,
    strokeWidth: 1,
  };
}

export function ellipsePath(
  position: Point,
  width: number,
  height: number,
): Path {
  const hw = width / 2;
  const hh = height / 2;
  const cx = position.x;
  const cy = position.y;
  const kx = hw * KAPPA;
  const ky = hh * KAPPA;

  const contour: Contour = {
    points: [
      { x: cx, y: cy - hh, pointType: 'moveTo' },
      // Top to right
      { x: cx + kx, y: cy - hh, pointType: 'curveData' },
      { x: cx + hw, y: cy - ky, pointType: 'curveData' },
      { x: cx + hw, y: cy, pointType: 'curveTo' },
      // Right to bottom
      { x: cx + hw, y: cy + ky, pointType: 'curveData' },
      { x: cx + kx, y: cy + hh, pointType: 'curveData' },
      { x: cx, y: cy + hh, pointType: 'curveTo' },
      // Bottom to left
      { x: cx - kx, y: cy + hh, pointType: 'curveData' },
      { x: cx - hw, y: cy + ky, pointType: 'curveData' },
      { x: cx - hw, y: cy, pointType: 'curveTo' },
      // Left to top
      { x: cx - hw, y: cy - ky, pointType: 'curveData' },
      { x: cx - kx, y: cy - hh, pointType: 'curveData' },
      { x: cx, y: cy - hh, pointType: 'curveTo' },
    ],
    closed: true,
  };

  return {
    contours: [contour],
    fill: { ...DEFAULT_FILL },
    stroke: null,
    strokeWidth: 1,
  };
}

export function colorizePath(shape: Path, fill: Color): Path {
  return {
    ...shape,
    fill: { ...fill },
  };
}

export function strokePath(shape: Path, color: Color, width: number): Path {
  return {
    ...shape,
    stroke: { ...color },
    strokeWidth: width,
  };
}

export function translatePath(shape: Path, offset: Point): Path {
  return {
    ...shape,
    contours: shape.contours.map((contour) => ({
      ...contour,
      points: contour.points.map((pt) => ({
        ...pt,
        x: pt.x + offset.x,
        y: pt.y + offset.y,
      })),
    })),
  };
}

export function rotatePath(shape: Path, angle: number, origin: Point): Path {
  const rad = (angle * Math.PI) / 180;
  const cos = Math.cos(rad);
  const sin = Math.sin(rad);
  return {
    ...shape,
    contours: shape.contours.map((contour) => ({
      ...contour,
      points: contour.points.map((pt) => {
        const dx = pt.x - origin.x;
        const dy = pt.y - origin.y;
        return {
          ...pt,
          x: origin.x + dx * cos - dy * sin,
          y: origin.y + dx * sin + dy * cos,
        };
      }),
    })),
  };
}

export function scalePath(shape: Path, sx: number, sy: number, origin: Point): Path {
  return {
    ...shape,
    contours: shape.contours.map((contour) => ({
      ...contour,
      points: contour.points.map((pt) => ({
        ...pt,
        x: origin.x + (pt.x - origin.x) * (sx / 100),
        y: origin.y + (pt.y - origin.y) * (sy / 100),
      })),
    })),
  };
}

export function copyPaths(
  shape: Path,
  copies: number,
  translate: Point,
  rotate: number,
  scale: Point,
): Path[] {
  const results: Path[] = [];
  let current = shape;
  for (let i = 0; i < copies; i++) {
    current = translatePath(current, translate);
    if (rotate !== 0) {
      current = rotatePath(current, rotate, { x: 0, y: 0 });
    }
    if (scale.x !== 100 || scale.y !== 100) {
      current = scalePath(current, scale.x, scale.y, { x: 0, y: 0 });
    }
    results.push(current);
  }
  return results;
}

export function linePath(point1: Point, point2: Point): Path {
  const contour: Contour = {
    points: [
      { x: point1.x, y: point1.y, pointType: 'moveTo' },
      { x: point2.x, y: point2.y, pointType: 'lineTo' },
    ],
    closed: false,
  };
  return {
    contours: [contour],
    fill: null,
    stroke: { r: 0, g: 0, b: 0, a: 1 },
    strokeWidth: 1,
  };
}

export function polygonPath(
  position: Point,
  radius: number,
  sides: number,
  rotation: number,
): Path {
  if (sides < 3) sides = 3;
  const points: PathPoint[] = [];
  const rotRad = (rotation * Math.PI) / 180;
  for (let i = 0; i < sides; i++) {
    const angle = (i / sides) * Math.PI * 2 + rotRad - Math.PI / 2;
    const x = position.x + Math.cos(angle) * radius;
    const y = position.y + Math.sin(angle) * radius;
    points.push({ x, y, pointType: i === 0 ? 'moveTo' : 'lineTo' });
  }
  return {
    contours: [{ points, closed: true }],
    fill: { r: 0, g: 0, b: 0, a: 1 },
    stroke: null,
    strokeWidth: 1,
  };
}

export function starPath(
  position: Point,
  points: number,
  outerRadius: number,
  innerRadius: number,
): Path {
  if (points < 2) points = 2;
  const pathPoints: PathPoint[] = [];
  const total = points * 2;
  for (let i = 0; i < total; i++) {
    const angle = (i / total) * Math.PI * 2 - Math.PI / 2;
    const r = i % 2 === 0 ? outerRadius : innerRadius;
    const x = position.x + Math.cos(angle) * r;
    const y = position.y + Math.sin(angle) * r;
    pathPoints.push({ x, y, pointType: i === 0 ? 'moveTo' : 'lineTo' });
  }
  return {
    contours: [{ points: pathPoints, closed: true }],
    fill: { r: 0, g: 0, b: 0, a: 1 },
    stroke: null,
    strokeWidth: 1,
  };
}

export function gridPoints(
  rows: number,
  columns: number,
  width: number,
  height: number,
  position: Point,
): Point[] {
  const result: Point[] = [];
  const actualRows = Math.max(1, Math.round(rows));
  const actualCols = Math.max(1, Math.round(columns));
  for (let r = 0; r < actualRows; r++) {
    for (let c = 0; c < actualCols; c++) {
      const x = actualCols <= 1 ? position.x : position.x - width / 2 + (c / (actualCols - 1)) * width;
      const y = actualRows <= 1 ? position.y : position.y - height / 2 + (r / (actualRows - 1)) * height;
      result.push({ x, y });
    }
  }
  return result;
}
