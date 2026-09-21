import { Point } from "./point";
import { Rect } from "./rect";
import { Transform } from "./transform";
import { bezierExtrema, curveLength, curvePoint, lineLength, linePoint } from "./math";

const SEGMENT_ACCURACY = 20;

/**
 * A single open or closed sequence of points, where curve segments are stored as two CURVE_DATA
 * control points followed by a CURVE_TO point (nodebox.graphics.Contour).
 */
export class Contour {
  points: Point[];
  closed: boolean;
  private segmentLengths: number[] | null = null;
  private length = -1;

  constructor(points: Iterable<Point> = [], closed = false) {
    this.points = Array.from(points);
    this.closed = closed;
  }

  static isContour(value: unknown): value is Contour {
    return value instanceof Contour;
  }

  get pointCount(): number {
    return this.points.length;
  }

  getPoints(): Point[] {
    return this.points;
  }

  setPoints(points: Iterable<Point>): void {
    this.points = Array.from(points);
    this.invalidate();
  }

  addPoint(pt: Point): void;
  addPoint(x: number, y: number): void;
  addPoint(a: Point | number, b?: number): void {
    this.points.push(a instanceof Point ? a : new Point(a, b));
    this.invalidate();
  }

  extend(points: Iterable<Point>): void {
    for (const pt of points) this.points.push(pt);
    this.invalidate();
  }

  isClosed(): boolean {
    return this.closed;
  }

  setClosed(closed: boolean): void {
    this.closed = closed;
    this.invalidate();
  }

  close(): void {
    this.setClosed(true);
  }

  isEmpty(): boolean {
    return this.points.length === 0;
  }

  invalidate(): void {
    this.segmentLengths = null;
    this.length = -1;
  }

  get bounds(): Rect {
    return this.getBounds();
  }

  getBounds(): Rect {
    if (this.points.length === 0) return new Rect();
    let minX = Number.MAX_VALUE;
    let minY = Number.MAX_VALUE;
    let maxX = -Number.MAX_VALUE;
    let maxY = -Number.MAX_VALUE;
    const points = this.points;
    for (let i = 0; i < points.length; i++) {
      const p = points[i];
      if (p.type === Point.LINE_TO) {
        if (p.x < minX) minX = p.x;
        if (p.y < minY) minY = p.y;
        if (p.x > maxX) maxX = p.x;
        if (p.y > maxY) maxY = p.y;
      } else if (p.type === Point.CURVE_TO && i >= 3) {
        const [x0, y0, x1, y1] = bezierExtrema(points[i - 3], points[i - 2], points[i - 1], p);
        if (x0 < minX) minX = x0;
        if (y0 < minY) minY = y0;
        if (x1 > maxX) maxX = x1;
        if (y1 > maxY) maxY = y1;
      }
    }
    if (minX === Number.MAX_VALUE) return new Rect();
    return new Rect(minX, minY, maxX - minX, maxY - minY);
  }

  updateSegmentLengths(): number {
    const points = this.points;
    const segmentLengths: number[] = [];
    let totalLength = 0;
    for (let pi = 1; pi < points.length; pi++) {
      const pt = points[pi];
      if (pt.isLineTo()) {
        const pt0 = points[pi - 1];
        const length = lineLength(pt0.x, pt0.y, pt.x, pt.y);
        segmentLengths.push(length);
        totalLength += length;
      } else if (pt.isCurveTo()) {
        const pt0 = points[pi - 3];
        const c1 = points[pi - 2];
        const c2 = points[pi - 1];
        const length = curveLength(pt0.x, pt0.y, c1.x, c1.y, c2.x, c2.y, pt.x, pt.y, SEGMENT_ACCURACY);
        segmentLengths.push(length);
        totalLength += length;
      }
    }
    if (this.closed && points.length > 0) {
      const pt0 = points[points.length - 1];
      const pt1 = points[0];
      const length = lineLength(pt0.x, pt0.y, pt1.x, pt1.y);
      segmentLengths.push(length);
      totalLength += length;
    }
    this.segmentLengths = segmentLengths;
    this.length = totalLength;
    return totalLength;
  }

  getLength(): number {
    if (this.segmentLengths === null) this.updateSegmentLengths();
    return this.length;
  }

  /** The point at relative position t (0..1) along the contour. */
  pointAt(t: number): Point {
    if (this.segmentLengths === null) this.updateSegmentLengths();
    const points = this.points;
    if (points.length === 0) throw new Error("The path is empty.");
    const segmentLengths = this.segmentLengths!;
    const length = this.length;
    if (length === 0) return points[0];

    let absT = t * length;
    let resT = t;
    let segnum = -1;
    for (const seglength of segmentLengths) {
      segnum++;
      if (absT <= seglength || segnum === segmentLengths.length - 1) break;
      absT -= seglength;
      resT -= seglength / length;
    }
    resT /= segmentLengths[segnum] / length;

    let pi = this.pointIndexForSegment(segnum + 1);
    const pt1 = points[pi];
    if (pi === 0) pi = points.length;
    if (pt1.isLineTo()) {
      const pt0 = points[pi - 1];
      return linePoint(resT, pt0.x, pt0.y, pt1.x, pt1.y);
    } else if (pt1.isCurveTo()) {
      const pt0 = points[pi - 3];
      const c1 = points[pi - 2];
      const c2 = points[pi - 1];
      return curvePoint(resT, pt0.x, pt0.y, c1.x, c1.y, c2.x, c2.y, pt1.x, pt1.y);
    }
    throw new Error("Incorrect point.");
  }

  point(t: number): Point {
    return this.pointAt(t);
  }

  private pointIndexForSegment(segnum: number): number {
    let pointIndex = 0;
    for (const pt of this.points) {
      if (pt.isCurveTo() || pt.isLineTo()) {
        if (segnum === 0) break;
        segnum--;
      }
      pointIndex++;
    }
    const pointCount = this.points.length;
    if (pointIndex < pointCount) return pointIndex;
    if (this.closed) return 0;
    return pointCount - 1;
  }

  makePoints(amount: number, _perContour = false): Point[] {
    if (this.points.length === 0) return [];
    amount = Math.floor(amount);
    const result: Point[] = new Array(Math.max(amount, 0));
    let delta = 1;
    if (this.closed) {
      if (amount > 0) delta = 1.0 / amount;
    } else if (amount > 2) {
      delta = 1.0 / (amount - 1.0);
    }
    for (let i = 0; i < amount; i++) result[i] = this.pointAt(delta * i);
    return result;
  }

  resampleByAmount(amount: number, _perContour = false): Contour {
    const c = new Contour();
    c.extend(this.makePoints(amount));
    c.setClosed(this.closed);
    return c;
  }

  resampleByLength(segmentLength: number): Contour {
    if (segmentLength <= 0.0000001) throw new Error("Segment length must be greater than zero.");
    const amount = Math.ceil(this.getLength() / segmentLength);
    return this.resampleByAmount(this.closed ? amount : amount + 1);
  }

  /** Approximate every curve with straight segments. */
  flattened(segmentsPerCurve = 16): Contour {
    const out: Point[] = [];
    const points = this.points;
    for (let i = 0; i < points.length; i++) {
      const pt = points[i];
      if (pt.isCurveTo() && i >= 3) {
        const p0 = points[i - 3];
        const c1 = points[i - 2];
        const c2 = points[i - 1];
        for (let s = 1; s <= segmentsPerCurve; s++) {
          const p = curvePoint(s / segmentsPerCurve, p0.x, p0.y, c1.x, c1.y, c2.x, c2.y, pt.x, pt.y);
          out.push(new Point(p.x, p.y));
        }
      } else if (pt.isLineTo()) {
        out.push(pt);
      }
    }
    return new Contour(out, this.closed);
  }

  transform(t: Transform): void {
    this.points = t.mapPoints(this.points);
    this.invalidate();
  }

  transformed(t: Transform): Contour {
    return new Contour(t.mapPoints(this.points), this.closed);
  }

  mapPoints(fn: (p: Point) => Point): Contour {
    return new Contour(this.points.map(fn), this.closed);
  }

  clone(): Contour {
    return new Contour(this.points, this.closed);
  }

  toString(): string {
    return "<Contour>";
  }
}
