// Geometry helpers shared by the graphics classes and the node functions.
// These mirror nodebox.util.Geometry and the static helpers on nodebox.graphics.Path.

import { Point } from "./point";

export function radians(degrees: number): number {
  return (degrees * Math.PI) / 180;
}

export function degrees(radians: number): number {
  return (radians * 180) / Math.PI;
}

/** The angle, in degrees, of the line from (x0, y0) to (x1, y1). */
export function angle(x0: number, y0: number, x1: number, y1: number): number {
  return degrees(Math.atan2(y1 - y0, x1 - x0));
}

export function distance(x0: number, y0: number, x1: number, y1: number): number {
  return Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2));
}

/** The point at the given distance and angle (in degrees) from (x0, y0). */
export function coordinates(x0: number, y0: number, distance: number, angle: number): [number, number] {
  return [x0 + Math.cos(radians(angle)) * distance, y0 + Math.sin(radians(angle)) * distance];
}

export function reflect(x0: number, y0: number, x1: number, y1: number, d: number, a: number): [number, number] {
  d *= distance(x0, y0, x1, y1);
  a += angle(x0, y0, x1, y1);
  return coordinates(x0, y0, d, a);
}

export function lineLength(x0: number, y0: number, x1: number, y1: number): number {
  const dx = Math.abs(x0 - x1);
  const dy = Math.abs(y0 - y1);
  return Math.sqrt(dx * dx + dy * dy);
}

export function linePoint(t: number, x0: number, y0: number, x1: number, y1: number): Point {
  return new Point(x0 + t * (x1 - x0), y0 + t * (y1 - y0));
}

/** Approximate the length of a cubic bezier by sampling n line segments. */
export function curveLength(
  x0: number,
  y0: number,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  x3: number,
  y3: number,
  n = 20,
): number {
  let length = 0;
  let xi = x0;
  let yi = y0;
  for (let i = 0; i < n; i++) {
    const t = (i + 1) / n;
    const pt = curvePoint(t, x0, y0, x1, y1, x2, y2, x3, y3);
    length += Math.sqrt((pt.x - xi) * (pt.x - xi) + (pt.y - yi) * (pt.y - yi));
    xi = pt.x;
    yi = pt.y;
  }
  return length;
}

export function curvePoint(
  t: number,
  x0: number,
  y0: number,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  x3: number,
  y3: number,
): Point {
  const mint = 1 - t;
  const x01 = x0 * mint + x1 * t;
  const y01 = y0 * mint + y1 * t;
  const x12 = x1 * mint + x2 * t;
  const y12 = y1 * mint + y2 * t;
  const x23 = x2 * mint + x3 * t;
  const y23 = y2 * mint + y3 * t;
  const outX1 = x01 * mint + x12 * t;
  const outY1 = y01 * mint + y12 * t;
  const outX2 = x12 * mint + x23 * t;
  const outY2 = y12 * mint + y23 * t;
  return new Point(outX1 * mint + outX2 * t, outY1 * mint + outY2 * t, Point.CURVE_TO);
}

/** Axis-aligned extrema of a cubic bezier, as [minX, minY, maxX, maxY]. */
export function bezierExtrema(p1: Point, p2: Point, p3: Point, p4: Point): [number, number, number, number] {
  const x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y, x3 = p3.x, y3 = p3.y, x4 = p4.x, y4 = p4.y;
  let minx = Math.min(x1, x4);
  let maxx = Math.max(x1, x4);
  let miny = Math.min(y1, y4);
  let maxy = Math.max(y1, y4);

  function fuzzyCompare(a: number, b: number): boolean {
    return Math.abs(a - b) <= 0.000000000001 * Math.min(Math.abs(a), Math.abs(b));
  }
  function check(t: number) {
    if (t >= 0 && t <= 1) {
      const p = curvePoint(t, x1, y1, x2, y2, x3, y3, x4, y4);
      if (p.x < minx) minx = p.x;
      else if (p.x > maxx) maxx = p.x;
      if (p.y < miny) miny = p.y;
      else if (p.y > maxy) maxy = p.y;
    }
  }
  function axis(a1: number, a2: number, a3: number, a4: number) {
    const a = 3 * (-a1 + 3 * a2 - 3 * a3 + a4);
    const b = 6 * (a1 - 2 * a2 + a3);
    const c = 3 * (-a1 + a2);
    if (fuzzyCompare(a + 1, 1)) {
      if (!fuzzyCompare(b + 1, 1)) check(-c / b);
    } else {
      const d = b * b - 4 * a * c;
      if (d >= 0) {
        const temp = Math.sqrt(d);
        const rcp = 1 / (2 * a);
        check((-b + temp) * rcp);
        check((-b - temp) * rcp);
      }
    }
  }
  axis(x1, x2, x3, x4);
  axis(y1, y2, y3, y4);
  return [minx, miny, maxx, maxy];
}

export function clamp(v: number, min: number, max: number): number {
  return v < min ? min : v > max ? max : v;
}
