import type { Point } from './point.js';
import type { Rect } from './rect.js';

// 2D affine matrix stored as [a, b, c, d, tx, ty]
// Represents: | a  c  tx |
//             | b  d  ty |
//             | 0  0  1  |
export interface Transform {
  m: [number, number, number, number, number, number];
}

export function identityTransform(): Transform {
  return { m: [1, 0, 0, 1, 0, 0] };
}

export function translateTransform(tx: number, ty: number): Transform {
  return { m: [1, 0, 0, 1, tx, ty] };
}

export function rotateTransform(degrees: number): Transform {
  const radians = degrees * Math.PI / 180;
  const cos = Math.cos(radians);
  const sin = Math.sin(radians);
  return { m: [cos, sin, -sin, cos, 0, 0] };
}

export function scaleTransform(sx: number, sy: number): Transform {
  return { m: [sx, 0, 0, sy, 0, 0] };
}

export function skewTransform(kx: number, ky: number): Transform {
  const kxRad = Math.PI * kx / 180;
  const kyRad = Math.PI * ky / 180;
  return { m: [1, Math.tan(kyRad), -Math.tan(kxRad), 1, 0, 0] };
}

export function multiplyTransforms(a: Transform, b: Transform): Transform {
  const [a0, a1, a2, a3, a4, a5] = a.m;
  const [b0, b1, b2, b3, b4, b5] = b.m;
  return {
    m: [
      a0 * b0 + a2 * b1,
      a1 * b0 + a3 * b1,
      a0 * b2 + a2 * b3,
      a1 * b2 + a3 * b3,
      a0 * b4 + a2 * b5 + a4,
      a1 * b4 + a3 * b5 + a5,
    ],
  };
}

export function transformPoint(transform: Transform, point: Point): Point {
  const [a, b, c, d, tx, ty] = transform.m;
  return {
    x: a * point.x + c * point.y + tx,
    y: b * point.x + d * point.y + ty,
  };
}

export function invertTransform(t: Transform): Transform | null {
  const [a, b, c, d, tx, ty] = t.m;
  const det = a * d - b * c;
  if (Math.abs(det) < 1e-10) return null;
  const invDet = 1 / det;
  return {
    m: [
      d * invDet,
      -b * invDet,
      -c * invDet,
      a * invDet,
      (c * ty - d * tx) * invDet,
      (b * tx - a * ty) * invDet,
    ],
  };
}

export function transformRect(transform: Transform, rect: Rect): Rect {
  const p1 = transformPoint(transform, { x: rect.x, y: rect.y });
  const p2 = transformPoint(transform, { x: rect.x + rect.width, y: rect.y });
  const p3 = transformPoint(transform, { x: rect.x, y: rect.y + rect.height });
  const p4 = transformPoint(transform, { x: rect.x + rect.width, y: rect.y + rect.height });
  const minX = Math.min(p1.x, p2.x, p3.x, p4.x);
  const minY = Math.min(p1.y, p2.y, p3.y, p4.y);
  const maxX = Math.max(p1.x, p2.x, p3.x, p4.x);
  const maxY = Math.max(p1.y, p2.y, p3.y, p4.y);
  return { x: minX, y: minY, width: maxX - minX, height: maxY - minY };
}
