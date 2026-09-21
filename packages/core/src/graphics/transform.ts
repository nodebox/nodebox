import { Point } from "./point";
import { Rect } from "./rect";
import { radians } from "./math";

/**
 * A 2D affine transform with java.awt.geom.AffineTransform semantics: translate/rotate/scale/skew
 * post-multiply, so the operation added last is applied to points first.
 *
 * The matrix is [m00 m01 m02; m10 m11 m12]: x' = m00*x + m01*y + m02, y' = m10*x + m11*y + m12.
 */
export class Transform {
  m00: number;
  m10: number;
  m01: number;
  m11: number;
  m02: number;
  m12: number;

  constructor(m00 = 1, m10 = 0, m01 = 0, m11 = 1, m02 = 0, m12 = 0) {
    this.m00 = m00;
    this.m10 = m10;
    this.m01 = m01;
    this.m11 = m11;
    this.m02 = m02;
    this.m12 = m12;
  }

  static translated(tx: number | Point, ty = 0): Transform {
    const t = new Transform();
    if (tx instanceof Point) t.translate(tx.x, tx.y);
    else t.translate(tx, ty);
    return t;
  }

  static rotated(degrees: number): Transform {
    const t = new Transform();
    t.rotate(degrees);
    return t;
  }

  static rotatedRadians(radians: number): Transform {
    const t = new Transform();
    t.rotateRadians(radians);
    return t;
  }

  static scaled(sx: number | Point, sy?: number): Transform {
    const t = new Transform();
    if (sx instanceof Point) t.scale(sx.x, sx.y);
    else t.scale(sx, sy === undefined ? sx : sy);
    return t;
  }

  static skewed(kx: number, ky?: number): Transform {
    const t = new Transform();
    t.skew(kx, ky === undefined ? kx : ky);
    return t;
  }

  clone(): Transform {
    return new Transform(this.m00, this.m10, this.m01, this.m11, this.m02, this.m12);
  }

  isIdentity(): boolean {
    return (
      this.m00 === 1 && this.m10 === 0 && this.m01 === 0 && this.m11 === 1 && this.m02 === 0 && this.m12 === 0
    );
  }

  /** this = this * other (concatenate, like AffineTransform.concatenate). */
  concatenate(o: Transform): this {
    const m00 = this.m00 * o.m00 + this.m01 * o.m10;
    const m01 = this.m00 * o.m01 + this.m01 * o.m11;
    const m02 = this.m00 * o.m02 + this.m01 * o.m12 + this.m02;
    const m10 = this.m10 * o.m00 + this.m11 * o.m10;
    const m11 = this.m10 * o.m01 + this.m11 * o.m11;
    const m12 = this.m10 * o.m02 + this.m11 * o.m12 + this.m12;
    this.m00 = m00;
    this.m01 = m01;
    this.m02 = m02;
    this.m10 = m10;
    this.m11 = m11;
    this.m12 = m12;
    return this;
  }

  /** this = other * this (preConcatenate). */
  preConcatenate(o: Transform): this {
    const result = o.clone().concatenate(this);
    this.m00 = result.m00;
    this.m01 = result.m01;
    this.m02 = result.m02;
    this.m10 = result.m10;
    this.m11 = result.m11;
    this.m12 = result.m12;
    return this;
  }

  append(t: Transform): this {
    return this.concatenate(t);
  }

  prepend(t: Transform): this {
    return this.preConcatenate(t);
  }

  translate(tx: number | Point, ty = 0): this {
    if (tx instanceof Point) {
      ty = tx.y;
      tx = tx.x;
    }
    return this.concatenate(new Transform(1, 0, 0, 1, tx, ty));
  }

  rotate(degrees: number): this {
    return this.rotateRadians(radians(degrees));
  }

  rotateRadians(r: number): this {
    const cos = Math.cos(r);
    const sin = Math.sin(r);
    return this.concatenate(new Transform(cos, sin, -sin, cos, 0, 0));
  }

  scale(sx: number, sy?: number): this {
    return this.concatenate(new Transform(sx, 0, 0, sy === undefined ? sx : sy, 0, 0));
  }

  skew(kx: number, ky?: number): this {
    if (ky === undefined) ky = kx;
    const rx = (Math.PI * kx) / 180;
    const ry = (Math.PI * ky) / 180;
    return this.concatenate(new Transform(1, Math.tan(ry), -Math.tan(rx), 1, 0, 0));
  }

  determinant(): number {
    return this.m00 * this.m11 - this.m01 * this.m10;
  }

  /** Invert in place. Returns false (and leaves the transform unchanged) when singular. */
  invert(): boolean {
    const det = this.determinant();
    if (det === 0 || !Number.isFinite(det)) return false;
    const m00 = this.m11 / det;
    const m01 = -this.m01 / det;
    const m10 = -this.m10 / det;
    const m11 = this.m00 / det;
    const m02 = -(m00 * this.m02 + m01 * this.m12);
    const m12 = -(m10 * this.m02 + m11 * this.m12);
    this.m00 = m00;
    this.m01 = m01;
    this.m10 = m10;
    this.m11 = m11;
    this.m02 = m02;
    this.m12 = m12;
    return true;
  }

  inverted(): Transform | undefined {
    const t = this.clone();
    return t.invert() ? t : undefined;
  }

  mapPoint(p: Point): Point {
    return new Point(
      this.m00 * p.x + this.m01 * p.y + this.m02,
      this.m10 * p.x + this.m11 * p.y + this.m12,
      p.type,
    );
  }

  mapPoints(points: readonly Point[]): Point[] {
    return points.map((p) => this.mapPoint(p));
  }

  mapRect(r: Rect): Rect {
    const origin = this.mapPoint(new Point(r.x, r.y));
    // Delta transform of the size (no translation), as AffineTransform.deltaTransform does.
    const w = this.m00 * r.width + this.m01 * r.height;
    const h = this.m10 * r.width + this.m11 * r.height;
    return new Rect(origin.x, origin.y, w, h);
  }

  /** Transform a point, a rectangle, a list of points or any geometry (returns a new object). */
  map<T extends Point | Rect | readonly Point[] | Transformable>(shape: T): T {
    if (shape instanceof Point) return this.mapPoint(shape) as T;
    if (shape instanceof Rect) return this.mapRect(shape) as T;
    if (Array.isArray(shape)) return this.mapPoints(shape) as unknown as T;
    return (shape as Transformable).transformed(this) as T;
  }

  /** The matrix as the six numbers of a CSS/SVG matrix(a, b, c, d, e, f). */
  toArray(): [number, number, number, number, number, number] {
    return [this.m00, this.m10, this.m01, this.m11, this.m02, this.m12];
  }

  equals(other: unknown): boolean {
    return other instanceof Transform && this.toArray().every((v, i) => v === other.toArray()[i]);
  }
}

/** Anything that can produce a transformed copy of itself. */
export interface Transformable {
  transformed(t: Transform): Transformable;
}
