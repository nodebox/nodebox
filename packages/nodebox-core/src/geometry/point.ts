export interface Point {
  x: number;
  y: number;
}

export const ZERO: Point = { x: 0, y: 0 };

export function createPoint(x: number, y: number): Point {
  return { x, y };
}

export function addPoints(a: Point, b: Point): Point {
  return { x: a.x + b.x, y: a.y + b.y };
}

export function subtractPoints(a: Point, b: Point): Point {
  return { x: a.x - b.x, y: a.y - b.y };
}

export function scalePoint(p: Point, s: number): Point {
  return { x: p.x * s, y: p.y * s };
}

export function distanceBetween(a: Point, b: Point): number {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  return Math.sqrt(dx * dx + dy * dy);
}

export function angleBetween(a: Point, b: Point): number {
  const radians = Math.atan2(b.y - a.y, b.x - a.x);
  return radians * 180 / Math.PI;
}

export function lerpPoint(a: Point, b: Point, t: number): Point {
  return {
    x: a.x + (b.x - a.x) * t,
    y: a.y + (b.y - a.y) * t,
  };
}

export function rotatePoint(p: Point, angle: number, origin: Point = ZERO): Point {
  const radians = angle * Math.PI / 180;
  const cos = Math.cos(radians);
  const sin = Math.sin(radians);
  const dx = p.x - origin.x;
  const dy = p.y - origin.y;
  return {
    x: origin.x + dx * cos - dy * sin,
    y: origin.y + dx * sin + dy * cos,
  };
}

export function pointEquals(a: Point, b: Point, epsilon = 1e-9): boolean {
  return Math.abs(a.x - b.x) < epsilon && Math.abs(a.y - b.y) < epsilon;
}
