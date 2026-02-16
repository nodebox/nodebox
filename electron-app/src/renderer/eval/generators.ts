import type { Point, Color, Path, Contour } from '../types/geometry';

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
