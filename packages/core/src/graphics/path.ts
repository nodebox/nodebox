import polygonClipping from "polygon-clipping";
import { Color } from "./color";
import { Contour } from "./contour";
import { Point } from "./point";
import { Rect } from "./rect";
import { Transform } from "./transform";
import { curvePoint } from "./math";
import type { Text } from "./text";

const KAPPA = 0.5522847498;

/**
 * A path is a list of contours with a fill, stroke and stroke width (nodebox.graphics.Path).
 * Coordinates are absolute: transformations produce new geometry, there is no transform matrix.
 */
export class Path {
  contours: Contour[];
  fillColor: Color | null;
  strokeColor: Color | null;
  strokeWidth: number;
  private currentContour: Contour | null = null;

  constructor(other?: Path, cloneContours = true) {
    if (other) {
      this.fillColor = other.fillColor;
      this.strokeColor = other.strokeColor;
      this.strokeWidth = other.strokeWidth;
      this.contours = cloneContours ? other.contours.map((c) => c.clone()) : [];
    } else {
      this.fillColor = Color.BLACK;
      this.strokeColor = null;
      // A fresh path has no stroke: width 0, as in Java's Path(). Nodes that stroke set the width.
      this.strokeWidth = 0;
      this.contours = [];
    }
  }

  static isPath(value: unknown): value is Path {
    return value instanceof Path;
  }

  static fromContour(c: Contour): Path {
    const p = new Path();
    p.add(c);
    return p;
  }

  /** Build a path out of "M x y x y ... M x y ..." point lists (the freehand node format). */
  static fromPointString(s: string): Path {
    const p = new Path();
    for (let curve of s.trim().split("M")) {
      curve = curve.trim();
      if (!curve) continue;
      let coords = curve
        .split(" ")
        .map((c) => parseFloat(c))
        .filter((c) => !Number.isNaN(c));
      if (coords.length % 2 === 1) coords = coords.slice(0, -1);
      for (let i = 0; i < coords.length; i += 2) {
        if (i === 0) p.moveto(coords[i], coords[i + 1]);
        else p.lineto(coords[i], coords[i + 1]);
      }
    }
    return p;
  }

  // Aliases used by Python-era code.
  get fill(): Color | null {
    return this.fillColor;
  }
  set fill(c: Color | null) {
    this.fillColor = c;
  }
  get stroke(): Color | null {
    return this.strokeColor;
  }
  set stroke(c: Color | null) {
    this.strokeColor = c;
  }

  asGeometry(): Geometry {
    const g = new Geometry();
    g.add(this);
    return g;
  }

  //// Point operations ////

  get pointCount(): number {
    let n = 0;
    for (const c of this.contours) n += c.points.length;
    return n;
  }

  get points(): Point[] {
    return this.getPoints();
  }

  getPoints(): Point[] {
    const points: Point[] = [];
    for (const c of this.contours) for (const p of c.points) points.push(p);
    return points;
  }

  //// Primitives ////

  moveto(x: number, y: number): void {
    this.currentContour = null;
    this.addPoint(x, y);
  }

  lineto(x: number, y: number): void {
    if (this.currentContour === null) throw new Error("Lineto without moveto first.");
    this.addPoint(x, y);
  }

  curveto(x1: number, y1: number, x2: number, y2: number, x3: number, y3: number): void {
    if (this.currentContour === null) throw new Error("Curveto without moveto first.");
    this.addPoint(new Point(x1, y1, Point.CURVE_DATA));
    this.addPoint(new Point(x2, y2, Point.CURVE_DATA));
    this.addPoint(new Point(x3, y3, Point.CURVE_TO));
  }

  close(): void {
    if (this.currentContour !== null) this.currentContour.close();
    this.currentContour = null;
  }

  newContour(): void {
    this.currentContour = null;
  }

  addPoint(pt: Point): void;
  addPoint(x: number, y: number): void;
  addPoint(a: Point | number, b?: number): void {
    this.ensureCurrentContour();
    if (a instanceof Point) this.currentContour!.addPoint(a);
    else this.currentContour!.addPoint(a, b!);
  }

  private ensureCurrentContour(): void {
    if (this.currentContour !== null) return;
    this.currentContour = new Contour();
    this.add(this.currentContour);
  }

  //// Basic shapes ////

  /** A rectangle centered at (cx, cy). */
  rect(cx: number, cy: number, width: number, height: number, rx = 0, ry = rx): void {
    if (rx !== 0 || ry !== 0) {
      this.roundedRect(cx, cy, width, height, rx, ry);
      return;
    }
    const x = cx - width / 2;
    const y = cy - height / 2;
    this.cornerRect(x, y, width, height);
  }

  /** A rectangle with its top-left corner at (x, y). */
  cornerRect(x: number, y: number, width: number, height: number, rx = 0, ry = rx): void {
    if (rx !== 0 || ry !== 0) {
      this.cornerRoundedRect(x, y, width, height, rx, ry);
      return;
    }
    this.moveto(x, y);
    this.lineto(x + width, y);
    this.lineto(x + width, y + height);
    this.lineto(x, y + height);
    this.close();
  }

  roundedRect(cx: number, cy: number, width: number, height: number, rx: number, ry = rx): void {
    this.cornerRoundedRect(cx - width / 2, cy - height / 2, width, height, rx, ry);
  }

  cornerRoundedRect(x: number, y: number, width: number, height: number, rx: number, ry = rx): void {
    // Mirrors Java's Path.roundedRect: rx/ry are corner radii, capped at half the size (SVG rule),
    // with the curve handles at 0.448 of the radius.
    const ONE_MINUS_QUARTER = 1.0 - 0.552;
    const dx = Math.min(rx, width * 0.5);
    const dy = Math.min(ry, height * 0.5);
    const left = x;
    const right = x + width;
    const top = y;
    const bottom = y + height;
    this.moveto(left + dx, top);
    if (dx < width * 0.5) this.lineto(right - rx, top);
    this.curveto(right - dx * ONE_MINUS_QUARTER, top, right, top + dy * ONE_MINUS_QUARTER, right, top + dy);
    if (dy < height * 0.5) this.lineto(right, bottom - dy);
    this.curveto(right, bottom - dy * ONE_MINUS_QUARTER, right - dx * ONE_MINUS_QUARTER, bottom, right - dx, bottom);
    if (dx < width * 0.5) this.lineto(left + dx, bottom);
    this.curveto(left + dx * ONE_MINUS_QUARTER, bottom, left, bottom - dy * ONE_MINUS_QUARTER, left, bottom - dy);
    if (dy < height * 0.5) this.lineto(left, top + dy);
    this.curveto(left, top + dy * ONE_MINUS_QUARTER, left + dx * ONE_MINUS_QUARTER, top, left + dx, top);
    this.close();
  }


  /** An ellipse centered at (cx, cy). */
  ellipse(cx: number, cy: number, width: number, height: number): void {
    this.cornerEllipse(cx - width / 2, cy - height / 2, width, height);
  }

  cornerEllipse(x: number, y: number, width: number, height: number): void {
    // Same construction as java.awt.geom.Ellipse2D: four cubic beziers starting at 3 o'clock, clockwise.
    const rx = width / 2;
    const ry = height / 2;
    const cx = x + rx;
    const cy = y + ry;
    const kx = rx * KAPPA;
    const ky = ry * KAPPA;
    this.moveto(cx + rx, cy);
    this.curveto(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry);
    this.curveto(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy);
    this.curveto(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry);
    this.curveto(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy);
    this.close();
  }

  line(x1: number, y1: number, x2: number, y2: number): void {
    this.moveto(x1, y1);
    this.lineto(x2, y2);
  }

  /**
   * An elliptical arc like java.awt.geom.Arc2D: angles in degrees, counter-clockwise positive as in AWT,
   * so callers pass negated angles for NodeBox's clockwise convention.
   */
  arc(cx: number, cy: number, width: number, height: number, startAngle: number, extent: number, type: "pie" | "chord" | "open"): void {
    const rx = width / 2;
    const ry = height / 2;
    // AWT measures angles counter-clockwise with y pointing up; on screen (y down) that reads clockwise.
    const toPoint = (deg: number) => {
      const r = (-deg * Math.PI) / 180;
      return [cx + rx * Math.cos(r), cy + ry * Math.sin(r)];
    };
    const segments = Math.max(1, Math.ceil(Math.abs(extent) / 90));
    const step = extent / segments;
    const [sx, sy] = toPoint(startAngle);
    if (type === "pie") {
      this.moveto(cx, cy);
      this.lineto(sx, sy);
    } else {
      this.moveto(sx, sy);
    }
    let a0 = startAngle;
    for (let i = 0; i < segments; i++) {
      const a1 = a0 + step;
      const r0 = (-a0 * Math.PI) / 180;
      const r1 = (-a1 * Math.PI) / 180;
      const k = (4 / 3) * Math.tan((r1 - r0) / 4);
      const p0x = cx + rx * Math.cos(r0);
      const p0y = cy + ry * Math.sin(r0);
      const p3x = cx + rx * Math.cos(r1);
      const p3y = cy + ry * Math.sin(r1);
      const c1x = p0x - k * rx * Math.sin(r0);
      const c1y = p0y + k * ry * Math.cos(r0);
      const c2x = p3x + k * rx * Math.sin(r1);
      const c2y = p3y - k * ry * Math.cos(r1);
      this.curveto(c1x, c1y, c2x, c2y, p3x, p3y);
      a0 = a1;
    }
    if (type !== "open") this.close();
  }

  text(t: Text): void {
    this.extend(t.getPath());
  }

  //// Container operations ////

  add(c: Contour): void {
    this.contours.push(c);
    this.currentContour = c;
  }

  get size(): number {
    return this.contours.length;
  }

  isEmpty(): boolean {
    return this.pointCount === 0;
  }

  clear(): void {
    this.contours = [];
    this.currentContour = null;
  }

  extend(p: Path): void {
    for (const c of p.contours) this.add(c.clone());
    this.currentContour = null;
  }

  getContours(): Contour[] {
    return this.contours;
  }

  isClosed(): boolean {
    if (this.contours.length === 0) return false;
    return this.contours.every((c) => c.closed);
  }

  //// Geometric math ////

  get length(): number {
    return this.getLength();
  }

  getLength(): number {
    let length = 0;
    for (const c of this.contours) length += c.getLength();
    return length;
  }

  contourAt(t: number): Contour | undefined {
    let absT = t * this.getLength();
    for (const c of this.contours) {
      const cLength = c.getLength();
      if (absT <= cLength) return c;
      absT -= cLength;
    }
    return undefined;
  }

  pointAt(t: number): Point {
    const length = this.getLength();
    let absT = t * length;
    let resT = t;
    let current: Contour | null = null;
    for (const c of this.contours) {
      current = c;
      const cLength = c.getLength();
      if (absT <= cLength) break;
      absT -= cLength;
      resT -= cLength / length;
    }
    if (current === null) return new Point();
    resT /= current.getLength() / length;
    return current.pointAt(resT);
  }

  point(t: number): Point {
    return this.pointAt(t);
  }

  makePoints(amount: number, perContour = false): Point[] {
    amount = Math.floor(amount);
    if (perContour) {
      const points: Point[] = [];
      for (const c of this.contours) points.push(...c.makePoints(amount));
      return points;
    }
    const delta = pointDelta(amount, this.isClosed());
    const points: Point[] = new Array(Math.max(amount, 0));
    for (let i = 0; i < amount; i++) points[i] = this.pointAt(delta * i);
    return points;
  }

  resampleByAmount(amount: number, perContour = false): Path {
    amount = Math.floor(amount);
    const p = this.cloneAndClear();
    if (perContour) {
      for (const c of this.contours) p.add(c.resampleByAmount(amount));
    } else {
      const delta = pointDelta(amount, this.isClosed());
      for (let i = 0; i < amount; i++) p.addPoint(this.pointAt(delta * i));
      if (this.isClosed()) p.close();
    }
    return p;
  }

  resampleByLength(segmentLength: number): Path {
    const p = this.cloneAndClear();
    for (const c of this.contours) p.add(c.resampleByLength(segmentLength));
    return p;
  }

  /** Construct a smooth path through the given points. Curvature 0 is straight, 1 is smooth. */
  static findPath(points: readonly Point[], curvature = 1): Path | null {
    if (points.length === 0) return null;
    const path = new Path();
    if (points.length === 1) {
      path.moveto(points[0].x, points[0].y);
      return path;
    }
    if (points.length === 2) {
      path.moveto(points[0].x, points[0].y);
      path.lineto(points[1].x, points[1].y);
      return path;
    }
    curvature = Math.max(0, Math.min(1, curvature));
    if (curvature === 0) {
      path.moveto(points[0].x, points[0].y);
      for (const point of points) path.lineto(point.x, point.y);
      return path;
    }
    curvature = 4 + (1.0 - curvature) * 40;
    const n = points.length;
    const dx: number[] = new Array(n).fill(0);
    const dy: number[] = new Array(n).fill(0);
    const bi: number[] = new Array(n).fill(0);
    const ax: number[] = new Array(n).fill(0);
    const ay: number[] = new Array(n).fill(0);
    bi[1] = 1 / curvature;
    ax[1] = (points[2].x - points[0].x - dx[0]) * bi[1];
    ay[1] = (points[2].y - points[0].y - dy[0]) * bi[1];
    for (let i = 2; i < n - 1; i++) {
      bi[i] = -1 / (curvature + bi[i - 1]);
      ax[i] = -(points[i + 1].x - points[i - 1].x - ax[i - 1]) * bi[i];
      ay[i] = -(points[i + 1].y - points[i - 1].y - ay[i - 1]) * bi[i];
    }
    for (let i = n - 2; i >= 1; i--) {
      dx[i] = ax[i] + dx[i + 1] * bi[i];
      dy[i] = ay[i] + dy[i + 1] * bi[i];
    }
    path.moveto(points[0].x, points[0].y);
    for (let i = 0; i < n - 1; i++) {
      path.curveto(
        points[i].x + dx[i],
        points[i].y + dy[i],
        points[i + 1].x - dx[i + 1],
        points[i + 1].y - dy[i + 1],
        points[i + 1].x,
        points[i + 1].y,
      );
    }
    return path;
  }

  //// Geometric queries ////

  /** Point-in-path test with the non-zero winding rule on the flattened path, like java.awt.geom.Path2D. */
  contains(p: Point): boolean;
  contains(x: number, y: number): boolean;
  contains(a: Point | number, b?: number): boolean {
    const x = a instanceof Point ? a.x : a;
    const y = a instanceof Point ? a.y : b!;
    let winding = 0;
    for (const contour of this.contours) {
      const pts = contour.flattened().points;
      const n = pts.length;
      if (n < 2) continue;
      for (let i = 0; i < n; i++) {
        const p0 = pts[i];
        const p1 = pts[(i + 1) % n];
        // Path2D implicitly closes every subpath for containment tests.
        if (p0.y <= y) {
          if (p1.y > y && cross(p0, p1, x, y) > 0) winding++;
        } else if (p1.y <= y && cross(p0, p1, x, y) < 0) {
          winding--;
        }
      }
    }
    return winding !== 0;
  }

  containsRect(r: Rect): boolean {
    const n = r.normalized();
    return (
      this.contains(n.x, n.y) &&
      this.contains(n.x + n.width, n.y) &&
      this.contains(n.x, n.y + n.height) &&
      this.contains(n.x + n.width, n.y + n.height)
    );
  }

  intersects(other: Path | Rect): boolean {
    if (other instanceof Rect) {
      if (!this.getBounds().intersects(other)) return false;
      const rp = new Path();
      rp.cornerRect(other.x, other.y, other.width, other.height);
      return this.intersects(rp);
    }
    if (!this.getBounds().intersects(other.getBounds())) return false;
    return !this.intersected(other).isEmpty();
  }

  //// Boolean operations ////

  intersected(p: Path): Path {
    return booleanOp(this, p, "intersected");
  }

  subtracted(p: Path): Path {
    return booleanOp(this, p, "subtracted");
  }

  united(p: Path): Path {
    return booleanOp(this, p, "united");
  }

  get bounds(): Rect {
    return this.getBounds();
  }

  getBounds(): Rect {
    if (this.isEmpty()) return new Rect();
    let r: Rect | null = null;
    for (const c of this.contours) {
      if (c.isEmpty()) continue;
      const b = c.getBounds();
      r = r === null ? b : r.united(b);
    }
    return r ?? new Rect();
  }

  //// Transformations ////

  transform(t: Transform): void {
    for (const c of this.contours) c.setPoints(t.mapPoints(c.points));
  }

  transformed(t: Transform): Path {
    const p = new Path(this, false);
    for (const c of this.contours) p.add(new Contour(t.mapPoints(c.points), c.closed));
    p.currentContour = null;
    return p;
  }

  flattened(): Path {
    const p = new Path(this, false);
    for (const c of this.contours) p.add(c.flattened());
    p.currentContour = null;
    return p;
  }

  clone(): Path {
    return new Path(this);
  }

  cloneAndClear(): Path {
    return new Path(this, false);
  }

  mapPoints(fn: (p: Point) => Point): Path {
    const p = this.cloneAndClear();
    for (const c of this.contours) p.add(c.mapPoints(fn));
    p.currentContour = null;
    return p;
  }

  *[Symbol.iterator](): Iterator<Point> {
    for (const c of this.contours) for (const p of c.points) yield p;
  }

  toString(): string {
    return "<Path>";
  }
}

/**
 * A group of paths (nodebox.graphics.Geometry). Setting the fill or stroke applies to every path.
 */
export class Geometry {
  paths: Path[];
  private currentPath: Path | null = null;

  constructor(other?: Geometry) {
    this.paths = other ? other.paths.map((p) => p.clone()) : [];
  }

  static isGeometry(value: unknown): value is Geometry {
    return value instanceof Geometry;
  }

  getPaths(): Path[] {
    return this.paths;
  }

  add(shape: Path | Geometry): void {
    if (shape instanceof Geometry) {
      this.extend(shape);
    } else {
      this.paths.push(shape);
      this.currentPath = shape;
    }
  }

  extend(g: Geometry): void {
    for (const p of g.paths) this.paths.push(p);
    this.currentPath = null;
  }

  get size(): number {
    return this.paths.length;
  }

  isEmpty(): boolean {
    return this.paths.every((p) => p.isEmpty());
  }

  clear(): void {
    this.paths = [];
    this.currentPath = null;
  }

  isClosed(): boolean {
    if (this.paths.length === 0) return false;
    return this.paths.every((p) => p.isClosed());
  }

  set fillColor(c: Color | null) {
    for (const p of this.paths) p.fillColor = c;
  }
  set fill(c: Color | null) {
    this.fillColor = c;
  }
  set strokeColor(c: Color | null) {
    for (const p of this.paths) p.strokeColor = c;
  }
  set stroke(c: Color | null) {
    this.strokeColor = c;
  }
  set strokeWidth(w: number) {
    for (const p of this.paths) p.strokeWidth = w;
  }

  get pointCount(): number {
    let n = 0;
    for (const p of this.paths) n += p.pointCount;
    return n;
  }

  get points(): Point[] {
    return this.getPoints();
  }

  getPoints(): Point[] {
    const points: Point[] = [];
    for (const p of this.paths) for (const pt of p.getPoints()) points.push(pt);
    return points;
  }

  addPoint(pt: Point): void;
  addPoint(x: number, y: number): void;
  addPoint(a: Point | number, b?: number): void {
    if (this.currentPath === null) {
      this.currentPath = new Path();
      this.paths.push(this.currentPath);
    }
    if (a instanceof Point) this.currentPath.addPoint(a);
    else this.currentPath.addPoint(a, b!);
  }

  get bounds(): Rect {
    return this.getBounds();
  }

  getBounds(): Rect {
    if (this.isEmpty()) return new Rect();
    let r: Rect | null = null;
    for (const p of this.paths) {
      if (p.isEmpty()) continue;
      const b = p.getBounds();
      r = r === null ? b : r.united(b);
    }
    return r ?? new Rect();
  }

  get length(): number {
    return this.getLength();
  }

  getLength(): number {
    let length = 0;
    for (const p of this.paths) length += p.getLength();
    return length;
  }

  pointAt(t: number): Point {
    const length = this.getLength();
    let absT = t * length;
    let resT = t;
    let current: Path | null = null;
    for (const p of this.paths) {
      current = p;
      const pLength = p.getLength();
      if (absT <= pLength) break;
      absT -= pLength;
      resT -= pLength / length;
    }
    if (current === null) return Point.ZERO;
    resT /= current.getLength() / length;
    return current.pointAt(resT);
  }

  contains(p: Point): boolean;
  contains(x: number, y: number): boolean;
  contains(a: Point | number, b?: number): boolean {
    const pt = a instanceof Point ? a : new Point(a, b!);
    return this.paths.some((p) => p.contains(pt));
  }

  containsRect(r: Rect): boolean {
    return this.paths.some((p) => p.containsRect(r));
  }

  intersects(other: Path | Geometry): boolean {
    const others = other instanceof Geometry ? other.paths : [other];
    return this.paths.some((p) => others.some((o) => p.intersects(o)));
  }

  makePoints(amount: number, perContour = false): Point[] {
    amount = Math.floor(amount);
    if (perContour) {
      const points: Point[] = [];
      for (const p of this.paths) for (const c of p.contours) points.push(...c.makePoints(amount));
      return points;
    }
    const delta = pointDelta(amount, this.isClosed());
    const points: Point[] = new Array(Math.max(amount, 0));
    for (let i = 0; i < amount; i++) points[i] = this.pointAt(delta * i);
    return points;
  }

  resampleByAmount(amount: number, perContour = false): Geometry {
    amount = Math.floor(amount);
    const g = new Geometry();
    if (perContour) {
      for (const p of this.paths) g.add(p.resampleByAmount(amount, true));
    } else {
      const delta = pointDelta(amount, this.isClosed());
      for (let i = 0; i < amount; i++) g.addPoint(this.pointAt(delta * i));
      if (this.isClosed() && g.paths.length === 1) g.paths[0].close();
    }
    return g;
  }

  resampleByLength(segmentLength: number): Geometry {
    const g = new Geometry();
    for (const p of this.paths) g.add(p.resampleByLength(segmentLength));
    return g;
  }

  transform(t: Transform): void {
    for (const p of this.paths) p.transform(t);
  }

  transformed(t: Transform): Geometry {
    const g = new Geometry();
    for (const p of this.paths) g.add(p.transformed(t));
    return g;
  }

  flattened(): Geometry {
    const g = new Geometry();
    for (const p of this.paths) g.add(p.flattened());
    return g;
  }

  mapPoints(fn: (p: Point) => Point): Geometry {
    const g = new Geometry();
    for (const p of this.paths) g.add(p.mapPoints(fn));
    return g;
  }

  clone(): Geometry {
    return new Geometry(this);
  }

  *[Symbol.iterator](): Iterator<Path> {
    yield* this.paths;
  }

  toString(): string {
    return "<Geometry>";
  }
}

/** Anything with points, a bounding box and a length: Contour, Path or Geometry. */
export type IGeometry = Contour | Path | Geometry;

export function isGeometryLike(value: unknown): value is IGeometry {
  return value instanceof Contour || value instanceof Path || value instanceof Geometry;
}

/** The spacing between resampled points; the last point of an open shape lands at t = 1. */
export function pointDelta(amount: number, closed: boolean): number {
  if (closed) return amount > 0 ? 1.0 / amount : 1;
  return amount > 2 ? 1.0 / (amount - 1.0) : 1;
}

function cross(p0: Point, p1: Point, x: number, y: number): number {
  return (p1.x - p0.x) * (y - p0.y) - (x - p0.x) * (p1.y - p0.y);
}

type Ring = [number, number][];
type Polygon = Ring[];
type MultiPolygon = Polygon[];
type BooleanOperation = "united" | "subtracted" | "intersected";

/**
 * A closed contour as a cycle of anchors and the segments between them. Java's Area closes every
 * subpath, so an open contour is treated as closed by a straight line.
 */
interface Loop {
  anchors: { x: number; y: number }[];
  /** segments[i] runs from anchors[i] to anchors[(i + 1) % n]. */
  segments: LoopSegment[];
}

type LoopSegment = { kind: "L" } | { kind: "C"; c1: { x: number; y: number }; c2: { x: number; y: number } };

/**
 * Boolean operations as java.awt.geom.Area did them, as far as a polygon clipper allows: shapes that
 * do not cross keep their curves (only their orientation and start point are normalized); shapes
 * that do cross are clipped on their flattened outlines.
 */
function booleanOp(a: Path, b: Path, op: BooleanOperation): Path {
  const polygonsA = toPolygons(a);
  const polygonsB = toPolygons(b);
  const loopsA = () => loopsOf(a);
  const loopsB = () => loopsOf(b);
  let loops: Loop[] | null = null;
  if (polygonsA.length === 0 || polygonsB.length === 0) {
    if (op === "united") loops = [...loopsA(), ...loopsB()];
    else if (op === "subtracted") loops = loopsA();
    else loops = [];
  } else if (polygonClipping.intersection(polygonsA, polygonsB).length === 0) {
    if (op === "united") loops = [...loopsA(), ...loopsB()];
    else if (op === "subtracted") loops = loopsA();
    else loops = [];
  } else if (polygonClipping.difference(polygonsB, polygonsA).length === 0) {
    // b lies inside a.
    if (op === "united") loops = loopsA();
    else if (op === "intersected") loops = loopsB();
    else loops = [...loopsA(), ...loopsB()];
  } else if (polygonClipping.difference(polygonsA, polygonsB).length === 0) {
    // a lies inside b.
    if (op === "united") loops = loopsB();
    else if (op === "intersected") loops = loopsA();
    else loops = [];
  }
  if (loops === null) {
    const clip = op === "united" ? polygonClipping.union : op === "subtracted" ? polygonClipping.difference : polygonClipping.intersection;
    return fromPolygons(clip(polygonsA, polygonsB));
  }
  return areaPath(loops.map((loop) => ({ loop, hole: false })), true);
}

function loopsOf(path: Path): Loop[] {
  const loops: Loop[] = [];
  for (const c of path.contours) {
    const loop = loopFromContour(c);
    if (loop) loops.push(loop);
  }
  return loops;
}

function loopFromContour(c: Contour): Loop | null {
  const pts = c.points;
  if (pts.length === 0) return null;
  const anchors: { x: number; y: number }[] = [{ x: pts[0].x, y: pts[0].y }];
  const segments: LoopSegment[] = [];
  let i = 1;
  while (i < pts.length) {
    const p = pts[i];
    if (p.isCurveData() && i + 2 < pts.length && pts[i + 2].isCurveTo()) {
      segments.push({ kind: "C", c1: { x: p.x, y: p.y }, c2: { x: pts[i + 1].x, y: pts[i + 1].y } });
      anchors.push({ x: pts[i + 2].x, y: pts[i + 2].y });
      i += 3;
    } else if (p.isLineTo() || p.isCurveTo()) {
      segments.push({ kind: "L" });
      anchors.push({ x: p.x, y: p.y });
      i++;
    } else {
      i++;
    }
  }
  if (anchors.length < 2) return null;
  const first = anchors[0];
  const last = anchors[anchors.length - 1];
  // An explicit segment back to the start closes the loop; otherwise a straight line does.
  if (Math.abs(first.x - last.x) < 1e-9 && Math.abs(first.y - last.y) < 1e-9) anchors.pop();
  else segments.push({ kind: "L" });
  if (anchors.length < 2) return null;
  return { anchors, segments };
}

/** Points along the loop (curves sampled) for orientation, containment and bounds. */
function flattenLoop(loop: Loop, samples = 8): [number, number][] {
  const out: [number, number][] = [];
  const n = loop.anchors.length;
  for (let i = 0; i < n; i++) {
    const from = loop.anchors[i];
    const to = loop.anchors[(i + 1) % n];
    const seg = loop.segments[i];
    out.push([from.x, from.y]);
    if (seg.kind === "C") {
      for (let s = 1; s < samples; s++) {
        const p = curvePoint(s / samples, from.x, from.y, seg.c1.x, seg.c1.y, seg.c2.x, seg.c2.y, to.x, to.y);
        out.push([p.x, p.y]);
      }
    }
  }
  return out;
}

function reversedLoop(loop: Loop): Loop {
  const n = loop.anchors.length;
  const anchors = [...loop.anchors].reverse();
  const segments: LoopSegment[] = [];
  // The reversed segment i runs from anchors[i] (old n-1-i) back along old segment n-2-i.
  for (let i = 0; i < n; i++) {
    const seg = loop.segments[(2 * n - 2 - i) % n];
    segments.push(seg.kind === "C" ? { kind: "C", c1: seg.c2, c2: seg.c1 } : seg);
  }
  return { anchors, segments };
}

function rotatedLoop(loop: Loop, start: number): Loop {
  const n = loop.anchors.length;
  return {
    anchors: loop.anchors.map((_, i) => loop.anchors[(start + i) % n]),
    segments: loop.segments.map((_, i) => loop.segments[(start + i) % n]),
  };
}

function contourFromLoop(loop: Loop): Contour {
  const c = new Contour();
  const n = loop.anchors.length;
  c.addPoint(new Point(loop.anchors[0].x, loop.anchors[0].y, Point.LINE_TO));
  for (let i = 0; i < n; i++) {
    const seg = loop.segments[i];
    const to = loop.anchors[(i + 1) % n];
    if (seg.kind === "C") {
      c.addPoint(new Point(seg.c1.x, seg.c1.y, Point.CURVE_DATA));
      c.addPoint(new Point(seg.c2.x, seg.c2.y, Point.CURVE_DATA));
      c.addPoint(new Point(to.x, to.y, Point.CURVE_TO));
    } else if (i < n - 1) {
      c.addPoint(new Point(to.x, to.y, Point.LINE_TO));
    }
  }
  c.close();
  return c;
}

/** Shoelace sum in screen coordinates: negative means counter-clockwise as seen on screen. */
function shoelace(pts: [number, number][]): number {
  let sum = 0;
  for (let i = 0; i < pts.length; i++) {
    const [x0, y0] = pts[i];
    const [x1, y1] = pts[(i + 1) % pts.length];
    sum += x0 * y1 - x1 * y0;
  }
  return sum;
}

function pointInRing(x: number, y: number, ring: [number, number][]): boolean {
  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i];
    const [xj, yj] = ring[j];
    if (yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) inside = !inside;
  }
  return inside;
}

/**
 * Assemble contours the way Area's path iterator hands them out: outer rings counter-clockwise
 * on screen starting at their top-left anchor, holes clockwise starting top-right, contours
 * ordered from the rightmost to the leftmost. With classify, holes are found by containment.
 */
function areaPath(entries: { loop: Loop; hole: boolean }[], classify: boolean): Path {
  const flat = entries.map((e) => flattenLoop(e.loop));
  const roles = entries.map((e, i) => {
    if (!classify) return e.hole;
    const [x, y] = flat[i][0];
    let depth = 0;
    for (let j = 0; j < entries.length; j++) if (j !== i && pointInRing(x, y, flat[j])) depth++;
    return depth % 2 === 1;
  });
  const contours = entries.map((e, i) => {
    const hole = roles[i];
    const ccwOnScreen = shoelace(flat[i]) < 0;
    let loop = ccwOnScreen === !hole ? e.loop : reversedLoop(e.loop);
    loop = rotatedLoop(loop, startAnchor(loop, hole));
    const c = contourFromLoop(loop);
    const b = flat[i];
    let minY = Infinity;
    let minX = Infinity;
    for (const [x, y] of b) {
      if (y < minY) minY = y;
      if (x < minX) minX = x;
    }
    return { c, minY, minX };
  });
  // Area hands the rightmost subpath out first (its edges are sorted, and the walk runs backwards).
  contours.sort((p, q) => q.minX - p.minX || p.minY - q.minY);
  const path = new Path();
  for (const { c } of contours) path.add(c);
  path.newContour();
  return path;
}

/** The topmost anchor; ties go to the leftmost for outer rings and the rightmost for holes. */
function startAnchor(loop: Loop, hole: boolean): number {
  let best = 0;
  for (let i = 1; i < loop.anchors.length; i++) {
    const a = loop.anchors[i];
    const b = loop.anchors[best];
    if (a.y < b.y || (a.y === b.y && (hole ? a.x > b.x : a.x < b.x))) best = i;
  }
  return best;
}

function toPolygons(path: Path): MultiPolygon {
  // Flattened rings; holes (by containment parity) are attached to the outer ring around them.
  const loops = loopsOf(path);
  const rings = loops.map((loop) => flattenLoop(loop, 16));
  const depth = rings.map((ring, i) => {
    let d = 0;
    for (let j = 0; j < rings.length; j++) if (j !== i && pointInRing(ring[0][0], ring[0][1], rings[j])) d++;
    return d;
  });
  const polygons: MultiPolygon = [];
  const outerIndex = new Map<number, number>();
  rings.forEach((ring, i) => {
    if (ring.length < 3 || depth[i] % 2 === 1) return;
    outerIndex.set(i, polygons.length);
    polygons.push([closedRing(ring)]);
  });
  rings.forEach((ring, i) => {
    if (ring.length < 3 || depth[i] % 2 === 0) return;
    for (const [j, polygonIndex] of outerIndex) {
      if (pointInRing(ring[0][0], ring[0][1], rings[j])) {
        polygons[polygonIndex].push(closedRing(ring));
        break;
      }
    }
  });
  return polygons;
}

function closedRing(pts: [number, number][]): Ring {
  return [...pts, pts[0]];
}

function fromPolygons(polygons: MultiPolygon): Path {
  const entries: { loop: Loop; hole: boolean }[] = [];
  for (const polygon of polygons) {
    polygon.forEach((ring, ringIndex) => {
      const pts = openRing(ring);
      if (pts.length < 3) return;
      entries.push({
        loop: { anchors: pts.map(([x, y]) => ({ x, y })), segments: pts.map(() => ({ kind: "L" as const })) },
        hole: ringIndex > 0,
      });
    });
  }
  return areaPath(entries, false);
}

function openRing(ring: Ring): [number, number][] {
  const n = ring.length > 1 && ring[0][0] === ring[ring.length - 1][0] && ring[0][1] === ring[ring.length - 1][1]
    ? ring.length - 1
    : ring.length;
  return ring.slice(0, n) as [number, number][];
}

export { Contour };
