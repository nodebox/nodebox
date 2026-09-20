// The vector library: a port of nodebox.function.CoreVectorFunctions (Java, namespace "corevector")
// and libraries/corevector/pyvector.py (Jython, namespace "pyvector"). Both namespaces are exported
// with the union of functions so documents can call either.
//
// Seeded randomness (scatter, wiggle) uses java.util.Random; the Python originals used Jython's
// Mersenne Twister, so those two nodes produce different, but equally random, positions.

import { parseSVG } from "@ndbx/g";
import { Color } from "../graphics/color";
import { Contour } from "../graphics/contour";
import { angle as geoAngle, coordinates as geoCoordinates, distance as geoDistance, radians } from "../graphics/math";
import { Geometry, Path } from "../graphics/path";
import { Point } from "../graphics/point";
import { Rect } from "../graphics/rect";
import { Text, getFontProvider, parseTextAlign } from "../graphics/text";
import { fromG } from "../graphics/to-g";
import { Transform } from "../graphics/transform";
import { JavaScriptLibrary } from "../runtime/function-repository";
import { toArray } from "../runtime/values";
import { getTextFileReader } from "./data";
import { JavaRandom } from "./java-random";

type Shape = Path | Geometry | Contour | Text;
type Mappable = Shape | Point | Point[];

function isShape(v: unknown): v is Shape {
  return v instanceof Path || v instanceof Geometry || v instanceof Contour || v instanceof Text;
}

/** Apply a transform to whatever geometry the node received: a shape, a point or a list of points. */
function mapShape<T extends Mappable | null>(t: Transform, shape: T): T {
  if (shape === null || shape === undefined) return shape;
  if (shape instanceof Point) return t.mapPoint(shape) as T;
  if (Array.isArray(shape)) return t.mapPoints(shape) as T;
  if (shape instanceof Contour) return shape.transformed(t) as T;
  return (shape as Path | Geometry | Text).transformed(t) as T;
}

function boundsOf(shape: unknown): Rect {
  if (shape instanceof Point) return new Rect(shape.x, shape.y, 0, 0);
  if (Array.isArray(shape)) {
    let r: Rect | null = null;
    for (const p of shape) {
      const b = boundsOf(p);
      r = r === null ? b : r.united(b);
    }
    return r ?? new Rect();
  }
  if (isShape(shape)) return shape.getBounds();
  return new Rect();
}

/** Points -> a path through them (the geometry-port convention for point lists). */
function asPath(shape: unknown): Path | Geometry | null {
  if (shape instanceof Path || shape instanceof Geometry) return shape;
  if (shape instanceof Contour) return Path.fromContour(shape);
  if (shape instanceof Text) return shape.getPath();
  if (Array.isArray(shape)) {
    const points = shape.filter((p): p is Point => p instanceof Point);
    if (points.length === 0) return null;
    const p = new Path();
    for (const pt of points) p.addPoint(pt);
    return p;
  }
  return null;
}

//// Generators ////

export function generator(): Path {
  const p = new Path();
  p.rect(0, 0, 100, 100);
  return p;
}

export function filter(geometry: unknown): unknown {
  const shape = asPath(geometry);
  if (!shape) return null;
  return Transform.rotated(45).map(shape);
}

export function align(geometry: unknown, position: Point, hAlign: string, vAlign: string): unknown {
  if (geometry === null || geometry === undefined) return null;
  const bounds = boundsOf(geometry);
  let dx: number;
  let dy: number;
  if (hAlign === "left") dx = position.x - bounds.x;
  else if (hAlign === "right") dx = position.x - bounds.x - bounds.width;
  else if (hAlign === "center") dx = position.x - bounds.x - bounds.width / 2;
  else dx = 0;
  if (vAlign === "top") dy = position.y - bounds.y;
  else if (vAlign === "bottom") dy = position.y - bounds.y - bounds.height;
  else if (vAlign === "middle") dy = position.y - bounds.y - bounds.height / 2;
  else dy = 0;
  return mapShape(Transform.translated(dx, dy), geometry as Mappable);
}

export function arc(position: Point, width: number, height: number, startAngle: number, degrees: number, arcType: string): Path {
  const type = arcType === "chord" ? "chord" : arcType === "pie" ? "pie" : "open";
  const p = new Path();
  // Arcs rotate opposite to AWT's Arc2D so they follow NodeBox's clockwise angles.
  p.arc(position.x, position.y, width, height, -startAngle, -degrees, type);
  return p;
}

export function centroid(shape: unknown): Point {
  if (shape === null || shape === undefined) return Point.ZERO;
  return boundsOf(shape).centroid;
}

export function colorize(shape: unknown, fill: Color, stroke: Color, strokeWidth: number): unknown {
  if (shape === null || shape === undefined) return null;
  if (shape instanceof Text) {
    const t = shape.clone();
    t.fillColor = fill;
    return t;
  }
  if (Array.isArray(shape)) return shape.map((s) => colorize(s, fill, stroke, strokeWidth));
  if (!(shape instanceof Path || shape instanceof Geometry)) return shape;
  const newShape = shape.clone();
  newShape.fillColor = fill;
  if (strokeWidth > 0) {
    newShape.strokeColor = stroke;
    newShape.strokeWidth = strokeWidth;
  } else {
    newShape.strokeColor = null;
  }
  return newShape;
}

export function connect(points: unknown, closed: boolean): Path | Geometry | null {
  if (points === null || points === undefined) return null;
  const list = toArray(points);
  const firstItem = list[0];
  if (isShape(firstItem)) {
    if (list.length === 1) return connectPoints(pointsOf(firstItem), closed);
    const g = new Geometry();
    for (const el of list) g.add(connectPoints(pointsOf(el), closed));
    return g;
  }
  return connectPoints(list.filter((p): p is Point => p instanceof Point), closed);
}

function pointsOf(shape: unknown): Point[] {
  if (shape instanceof Point) return [shape];
  if (shape instanceof Text) return shape.getPath().points;
  if (shape instanceof Path || shape instanceof Geometry || shape instanceof Contour) return shape.points;
  if (Array.isArray(shape)) return shape.filter((p): p is Point => p instanceof Point);
  return [];
}

function connectPoints(points: Point[], closed: boolean): Path {
  const p = new Path();
  // Points keep their type, so connecting the points of a curve gives the curve back (as in Java).
  for (const pt of points) p.addPoint(pt);
  if (closed) p.close();
  p.fillColor = null;
  p.strokeColor = Color.BLACK;
  p.strokeWidth = 1.0;
  return p;
}

export function copy(shape: unknown, copies: number, order: string, translate: Point, rotate: number, scale: Point): unknown[] {
  if (shape === null || shape === undefined) return [];
  const result: unknown[] = [];
  let tx = 0;
  let ty = 0;
  let r = 0;
  let sx = 1.0;
  let sy = 1.0;
  for (let i = 0; i < Math.trunc(copies); i++) {
    const t = new Transform();
    for (const op of order) {
      if (op === "t") t.translate(tx, ty);
      else if (op === "r") t.rotate(r);
      else if (op === "s") t.scale(sx, sy);
    }
    result.push(mapShape(t, shape as Mappable));
    tx += translate.x;
    ty += translate.y;
    r += rotate;
    sx += scale.x / 100 - 1;
    sy += scale.y / 100 - 1;
  }
  return result;
}

export function doNothing(object: unknown): unknown {
  return object;
}

export function ellipse(position: Point, width: number, height: number): Path {
  const p = new Path();
  p.ellipse(position.x, position.y, width, height);
  return p;
}

export function fit(shape: unknown, position: Point, width: number, height: number, keepProportions: boolean): unknown {
  if (shape === null || shape === undefined) return null;
  const bounds = boundsOf(shape);
  const bw = bounds.width > 0.000000000001 ? bounds.width : 0;
  const bh = bounds.height > 0.000000000001 ? bounds.height : 0;
  const t = new Transform();
  t.translate(position.x, position.y);
  let sx: number;
  let sy: number;
  if (keepProportions) {
    sx = bw > 0 ? width / bw : Number.MAX_VALUE;
    sy = bh > 0 ? height / bh : Number.MAX_VALUE;
    sx = sy = Math.min(sx, sy);
  } else {
    sx = bw > 0 ? width / bw : 1;
    sy = bh > 0 ? height / bh : 1;
  }
  t.scale(sx, sy);
  t.translate(-bw / 2 - bounds.x, -bh / 2 - bounds.y);
  return mapShape(t, shape as Mappable);
}

export function fitTo(shape: unknown, bounding: unknown, keepProportions: boolean): unknown {
  if (shape === null || shape === undefined) return null;
  if (bounding === null || bounding === undefined) return shape;
  const bounds = boundsOf(bounding);
  return fit(shape, bounds.centroid, bounds.width, bounds.height, keepProportions);
}

export function freehand(pathString: string): Path {
  if (pathString === null || pathString === undefined) return new Path();
  const p = parsePath(pathString);
  p.fillColor = null;
  p.strokeColor = Color.BLACK;
  p.strokeWidth = 1;
  return p;
}

export function grid(columns: number, rows: number, width: number, height: number, position: Point): Point[] {
  columns = Math.trunc(columns);
  rows = Math.trunc(rows);
  let columnSize: number;
  let left: number;
  let rowSize: number;
  let top: number;
  if (columns > 1) {
    columnSize = width / (columns - 1);
    left = position.x - width / 2;
  } else {
    columnSize = left = position.x;
  }
  if (rows > 1) {
    rowSize = height / (rows - 1);
    top = position.y - height / 2;
  } else {
    rowSize = top = position.y;
  }
  const points: Point[] = [];
  for (let ri = 0; ri < rows; ri++) {
    for (let ci = 0; ci < columns; ci++) points.push(new Point(left + ci * columnSize, top + ri * rowSize));
  }
  return points;
}

export function group(shapes: unknown): Geometry {
  const g = new Geometry();
  for (const shape of toArray(shapes)) {
    if (shape instanceof Geometry) g.extend(shape);
    else if (shape instanceof Path) g.add(shape);
    else if (shape instanceof Contour) g.add(Path.fromContour(shape));
    else if (shape instanceof Text) g.add(shape.getPath());
    else if (shape instanceof Point) {
      // Points cannot be grouped in NodeBox 3; keep them as a one-point path so nothing is lost.
      const p = new Path();
      p.addPoint(shape);
      g.add(p);
    } else if (shape !== null && shape !== undefined) {
      throw new Error(`Unable to group ${String(shape)}. I can only group paths or geometry objects.`);
    }
  }
  return g;
}

export function line(p1: Point, p2: Point, points = 2): Path {
  let p = new Path();
  p.line(p1.x, p1.y, p2.x, p2.y);
  p.fillColor = null;
  p.strokeColor = Color.BLACK;
  p.strokeWidth = 1;
  if (points !== 2) p = p.resampleByAmount(Math.trunc(points), true);
  return p;
}

export function lineAngle(point: Point, angle: number, distance: number, points = 2): Path {
  const [x, y] = geoCoordinates(point.x, point.y, distance, angle);
  return line(point, new Point(x, y), points);
}

export function link(shape1: unknown, shape2: unknown, orientation: string): Path | null {
  if (shape1 === null || shape1 === undefined || shape2 === null || shape2 === undefined) return null;
  const p = new Path();
  const a = boundsOf(shape1);
  const b = boundsOf(shape2);
  if (orientation === "horizontal") {
    const hw = (b.x - (a.x + a.width)) / 2;
    p.moveto(a.x + a.width, a.y);
    p.curveto(a.x + a.width + hw, a.y, b.x - hw, b.y, b.x, b.y);
    p.lineto(b.x, b.y + b.height);
    p.curveto(b.x - hw, b.y + b.height, a.x + a.width + hw, a.y + a.height, a.x + a.width, a.y + a.height);
  } else {
    const hh = (b.y - (a.y + a.height)) / 2;
    p.moveto(a.x, a.y + a.height);
    p.curveto(a.x, a.y + a.height + hh, b.x, b.y - hh, b.x, b.y);
    p.lineto(b.x + b.width, b.y);
    p.curveto(b.x + b.width, b.y - hh, a.x + a.width, a.y + a.height + hh, a.x + a.width, a.y + a.height);
  }
  return p;
}

export function pointOnPath(shape: unknown, t: number): Point | null {
  const path = asPath(shape);
  if (!path) return null;
  t = Math.abs(t % 100);
  return path.pointAt(t / 100);
}

export function skew(shape: unknown, skewPoint: Point, origin: Point): unknown {
  if (shape === null || shape === undefined) return null;
  const t = new Transform();
  t.translate(origin);
  t.skew(skewPoint.x, skewPoint.y);
  t.translate(-origin.x, -origin.y);
  return mapShape(t, shape as Mappable);
}

function snapValue(v: number, distance: number, strength: number): number {
  return v * (1.0 - strength) + strength * Math.round(v / distance) * distance;
}

export function snap(shape: unknown, distance: number, strength: number, position: Point): unknown {
  if (shape === null || shape === undefined) return null;
  const dStrength = strength / 100.0;
  const fn = (point: Point) =>
    new Point(
      snapValue(point.x + position.x, distance, dStrength) - position.x,
      snapValue(point.y + position.y, distance, dStrength) - position.y,
      point.type,
    );
  if (shape instanceof Point) return fn(shape);
  if (Array.isArray(shape)) return shape.map((p) => (p instanceof Point ? fn(p) : p));
  if (shape instanceof Text) return snap(shape.getPath(), distance, strength, position);
  if (shape instanceof Path || shape instanceof Geometry || shape instanceof Contour) return shape.mapPoints(fn);
  return shape;
}

export function rect(position: Point, width: number, height: number, roundness: Point): Path {
  const p = new Path();
  if (roundness.x === 0 && roundness.y === 0) p.rect(position.x, position.y, width, height);
  else p.roundedRect(position.x, position.y, width, height, roundness.x, roundness.y);
  return p;
}

export function toPoints(shape: unknown): Point[] | null {
  if (shape === null || shape === undefined) return null;
  return pointsOf(shape);
}

export function ungroup(shape: unknown): unknown[] | null {
  if (shape === null || shape === undefined) return null;
  if (shape instanceof Geometry) return shape.paths;
  if (shape instanceof Path) return [shape];
  if (Array.isArray(shape)) return shape;
  return [shape];
}

export function textpath(text: string, fontName: string, fontSize: number, alignment: string, position: Point, width: number): Path | Text {
  const align = parseTextAlign(alignment ?? "CENTER");
  let pos = position;
  if (align === "CENTER") pos = position.moved(-width / 2, 0);
  else if (align === "RIGHT") pos = position.moved(-width, 0);
  const t = new Text(String(text ?? ""), pos.x, pos.y, width, 0);
  t.fontName = fontName;
  t.fontSize = fontSize;
  t.align = align;
  // Without a font provider the outline is unknown; hand back the text so it still renders as text.
  return getFontProvider() ? t.getPath() : t;
}

export function makePoint(x: number, y: number): Point {
  return new Point(x, y);
}

export function point(value: Point): Point {
  return value;
}

//// Utility parsers ////

export function parsePath(s: string): Path {
  const p = new Path();
  for (let contourString of s.trim().split("M")) {
    contourString = contourString.trim();
    if (contourString !== "") p.add(parseContour(contourString));
  }
  p.newContour();
  return p;
}

export function parseContour(s: string): Contour {
  const contour = new Contour();
  const parts = s
    .replace(/,/g, " ")
    .split(" ")
    .filter((part) => part.length > 0);
  if (parts.length % 2 === 1) throw new Error(`Could not parse point ${parts[parts.length - 1]}`);
  for (let i = 0; i < parts.length; i += 2) {
    const x = Number(parts[i]);
    const y = Number(parts[i + 1]);
    if (Number.isNaN(x) || Number.isNaN(y)) throw new Error(`Could not parse point ${parts[i]},${parts[i + 1]}`);
    contour.addPoint(new Point(x, y));
  }
  return contour;
}

export function parsePoint(s: string): Point {
  const parts = s.split(",");
  if (parts.length !== 2) throw new Error(`Could not parse point ${s}`);
  return new Point(Number(parts[0]), Number(parts[1]));
}

//// Handles: descriptors the editor turns into interactive handles ////

export interface HandleDescriptor {
  type: string;
  ports?: Record<string, string>;
}

const handle = (type: string, ports?: Record<string, string>) => (): HandleDescriptor => ({ type, ports });

export const fourPointHandle = handle("fourPoint");
export const freehandHandle = handle("freehand");
export const lineAngleHandle = handle("lineAngle");
export const lineHandle = handle("line");
export const pointHandle = handle("point");
export const snapHandle = handle("snap");
export const translateHandle = handle("translate");
export const rotateHandle = handle("rotate");
export const scaleHandle = handle("scale");
export const starHandle = handle("star");
export const reflectHandle = handle("reflect");
export const polygonHandle = handle("polygon");
export const wiggleHandle = handle("point", { position: "offset" });

//// pyvector: the Python half of the library ////

function flattenGeometry(geo: Path | Geometry): Path {
  if (geo instanceof Path) return geo;
  let compound: Path | null = null;
  for (const path of geo.paths) compound = compound === null ? path : compound.united(path);
  return compound ?? new Path();
}

export function compound(shape1: unknown, shape2: unknown, fn = "united", invertDifference = false): Path | null {
  let s1 = asPath(shape1);
  let s2 = asPath(shape2);
  if (!s1) return null;
  if (!s2) return flattenGeometry(s1).clone();
  if (invertDifference) [s1, s2] = [s2, s1];
  const a = flattenGeometry(s1);
  const b = flattenGeometry(s2);
  if (fn === "united") return a.united(b);
  if (fn === "subtracted") return a.subtracted(b);
  if (fn === "intersected") return a.intersected(b);
  return null;
}

function containsPoint(bounding: unknown, p: Point): boolean {
  if (bounding instanceof Path || bounding instanceof Geometry) return bounding.contains(p);
  if (bounding instanceof Rect) return bounding.contains(p);
  if (bounding instanceof Text) return bounding.getPath().contains(p);
  return false;
}

function deletePoints(shape: unknown, bounding: unknown, deleteSelected: boolean): unknown {
  const keep = (p: Point) => containsPoint(bounding, p) !== deleteSelected;
  if (Array.isArray(shape)) return shape.filter((p) => !(p instanceof Point) || keep(p));
  if (shape instanceof Path) {
    const newPath = new Path(shape, false);
    for (const c of shape.contours) newPath.add(new Contour(c.points.filter(keep), c.closed));
    newPath.newContour();
    return newPath;
  }
  if (shape instanceof Geometry) {
    const g = new Geometry();
    for (const p of shape.paths) g.add(deletePoints(p, bounding, deleteSelected) as Path);
    return g;
  }
  return null;
}

function deletePaths(shape: unknown, bounding: unknown, deleteSelected: boolean): Geometry | null {
  const geo = shape instanceof Path ? shape.asGeometry() : shape instanceof Geometry ? shape : null;
  if (!geo) return null;
  const g = new Geometry();
  for (const oldPath of geo.paths) {
    // A path is selected as soon as one of its points lies inside the bounding shape.
    const selected = oldPath.points.some((p) => containsPoint(bounding, p));
    if (selected !== deleteSelected) g.add(oldPath.clone());
  }
  return g;
}

export function del(shape: unknown, bounding: unknown, scope = "points", operation = "selected"): unknown {
  if (shape === null || shape === undefined || bounding === null || bounding === undefined) return null;
  const deleteSelected = operation === "selected";
  if (scope === "points") return deletePoints(shape, bounding, deleteSelected);
  if (scope === "paths") return deletePaths(shape, bounding, deleteSelected);
  return null;
}

type Extent = (shape: Shape) => number;
const left: Extent = (s) => s.getBounds().x;
const center: Extent = (s) => s.getBounds().x + s.getBounds().width / 2;
const right: Extent = (s) => s.getBounds().x + s.getBounds().width;
const top: Extent = (s) => s.getBounds().y;
const middle: Extent = (s) => s.getBounds().y + s.getBounds().height / 2;
const bottom: Extent = (s) => s.getBounds().y + s.getBounds().height;

function distributeAlong(shapes: Shape[], mainFn: Extent): Shape[] {
  const horizontal = mainFn === left || mainFn === right || mainFn === center;
  const ext1 = horizontal ? left : top;
  const ext2 = horizontal ? right : bottom;
  const translate = (s: Shape, d: number) => mapShape(horizontal ? Transform.translated(d, 0) : Transform.translated(0, d), s);
  const sorted = shapes.slice().sort((a, b) => mainFn(a) - mainFn(b));
  const extremum1 = shapes.slice().sort((a, b) => ext1(a) - ext1(b))[0];
  const extremum2 = shapes.slice().sort((a, b) => ext2(a) - ext2(b))[shapes.length - 1];
  const outer1 = mainFn(extremum1);
  const outer2 = mainFn(extremum2);
  const skip = (outer2 - outer1) / (shapes.length - 1);
  const index = new Map<Shape, number>();
  sorted.forEach((s, i) => index.set(s, i));
  const iE1 = index.get(extremum1)!;
  const iE2 = index.get(extremum2)!;
  return shapes.map((shape) => {
    if (shape === extremum1 || shape === extremum2) return shape.clone() as Shape;
    let i = index.get(shape)!;
    if (i < iE1) i += 1;
    if (i > iE2) i -= 1;
    return translate(shape, outer1 + i * skip - mainFn(shape));
  });
}

export function distribute(shapes: unknown, horizontal: string, vertical: string): unknown[] | null {
  if (shapes === null || shapes === undefined) return null;
  const list = toArray(shapes).filter(isShape);
  if (list.length < 3 || (horizontal === "none" && vertical === "none")) return list.map((s) => s.clone());
  const fns: Record<string, Extent> = { left, right, center, top, bottom, middle };
  let result: Shape[] = horizontal === "none" ? list.map((s) => s.clone() as Shape) : distributeAlong(list, fns[horizontal]);
  if (vertical !== "none") result = distributeAlong(result, fns[vertical]);
  return result;
}

export function edit(shape: unknown, pointDeltas: string): unknown {
  if (shape === null || shape === undefined) return null;
  const deltas = parseDeltas(pointDeltas ?? "");
  const move = (points: Point[]) =>
    points.map((p, i) => {
      const d = deltas.get(i);
      return d ? new Point(p.x + d[0], p.y + d[1], p.type) : p;
    });
  if (shape instanceof Path) {
    let offset = 0;
    const newPath = new Path(shape, false);
    for (const c of shape.contours) {
      const moved = c.points.map((p, i) => {
        const d = deltas.get(offset + i);
        return d ? new Point(p.x + d[0], p.y + d[1], p.type) : p;
      });
      offset += c.points.length;
      newPath.add(new Contour(moved, c.closed));
    }
    newPath.newContour();
    return newPath;
  }
  if (shape instanceof Geometry) {
    const all = move(shape.points);
    let index = 0;
    const g = new Geometry();
    for (const p of shape.paths) {
      const np = new Path(p, false);
      for (const c of p.contours) {
        np.add(new Contour(all.slice(index, index + c.points.length), c.closed));
        index += c.points.length;
      }
      np.newContour();
      g.add(np);
    }
    return g;
  }
  if (Array.isArray(shape)) return move(shape.filter((p): p is Point => p instanceof Point));
  return shape;
}

function parseDeltas(s: string): Map<number, [number, number]> {
  const d = new Map<number, [number, number]>();
  for (const el of s.split("P")) {
    if (!el.trim()) continue;
    const item = el.trim().split(" ");
    const index = parseInt(item[0], 10);
    const dx = parseFloat(item[1]);
    const dy = parseFloat(item[2]);
    if (!Number.isNaN(index) && !Number.isNaN(dx) && !Number.isNaN(dy)) d.set(index, [dx, dy]);
  }
  return d;
}

export async function importSvg(fileName: string, centered = false, position: Point = Point.ZERO): Promise<Geometry | null> {
  if (!fileName) return null;
  const reader = getTextFileReader();
  if (!reader) throw new Error(`Could not read file ${fileName}: no file reader is installed.`);
  const text = await reader(fileName);
  return importSvgString(text, centered, position);
}

type DomParserClass = new () => DOMParser;
let domParserClass: DomParserClass | null = null;

/** Install a DOMParser for SVG import outside the browser (e.g. from jsdom or linkedom). */
export function setDomParser(parser: DomParserClass | null): void {
  domParserClass = parser;
}

export function getDomParser(): DomParserClass | null {
  return domParserClass ?? (globalThis as { DOMParser?: DomParserClass }).DOMParser ?? null;
}

export function importSvgString(svg: string, centered = false, position: Point = Point.ZERO): Geometry {
  const parserClass = getDomParser();
  if (!parserClass) throw new Error("Importing SVG needs a DOM parser; install one with setDomParser or run in a browser.");
  const shape = parseSVG(svg, new parserClass());
  let g = fromG(shape);
  const t = new Transform();
  if (centered) {
    const b = g.getBounds();
    t.translate(-b.x - b.width / 2, -b.y - b.height / 2);
  }
  t.translate(position);
  if (!t.isIdentity()) g = g.transformed(t);
  return g;
}

export function nullShape(shape: unknown): unknown {
  return shape;
}

export function polygon(position: Point, radius: number, sides: number, alignRadius: boolean): Path {
  const p = new Path();
  const x = position.x;
  const y = position.y;
  const r = radius;
  sides = Math.max(Math.trunc(sides), 3);
  const a = 360.0 / sides;
  let da = 0;
  if (alignRadius) {
    const [x0, y0] = geoCoordinates(x, y, r, 0);
    const [x1, y1] = geoCoordinates(x, y, r, a);
    da = -geoAngle(x1, y1, x0, y0);
  }
  for (let i = 0; i < sides; i++) {
    const [x1, y1] = geoCoordinates(x, y, r, a * i + da);
    if (i === 0) p.moveto(x1, y1);
    else p.lineto(x1, y1);
  }
  p.close();
  return p;
}

export function reflect(shape: unknown, position: Point, angle: number, keepOriginal: boolean): unknown {
  if (shape === null || shape === undefined) return null;
  if (shape instanceof Geometry) {
    const g = new Geometry();
    for (const path of shape.paths) {
      const result = reflect(path, position, angle, keepOriginal);
      if (result instanceof Path) g.add(result);
      else if (result instanceof Geometry) g.extend(result);
    }
    return g;
  }
  const path = asPath(shape);
  if (!(path instanceof Path)) return null;
  const reflectPoint = (point: Point): Point => {
    let d = geoDistance(point.x, point.y, position.x, position.y);
    let a = geoAngle(point.x, point.y, position.x, position.y);
    const [x, y] = geoCoordinates(position.x, position.y, d * Math.cos(radians(a - angle)), 180 + angle);
    d = geoDistance(point.x, point.y, x, y);
    a = geoAngle(point.x, point.y, x, y);
    const [px, py] = geoCoordinates(point.x, point.y, d * 2, a);
    return new Point(px, py, point.type);
  };
  const newShape = path.cloneAndClear();
  for (const contour of path.contours) newShape.add(new Contour(contour.points.map(reflectPoint), contour.closed));
  newShape.newContour();
  if (keepOriginal) {
    const g = new Geometry();
    g.add(path);
    g.add(newShape);
    return g;
  }
  return newShape;
}

export function resample(shape: unknown, method: string, length: number, points: number, perContour = false): unknown {
  const path = asPath(shape);
  if (!path) return null;
  if (method === "length") return path.resampleByLength(length);
  return path.resampleByAmount(Math.trunc(points), perContour);
}

function constructPath(path: Path, points: Point[], closed: boolean): void {
  const segments: { in: Point; pt: Point; out: Point }[] = [];
  for (let i = 0; i + 2 < points.length; i += 3) segments.push({ in: points[i], pt: points[i + 1], out: points[i + 2] });
  const length = closed ? segments.length + 1 : segments.length;
  for (let i = 0; i < length; i++) {
    const seg = segments[i % segments.length];
    if (i === 0) path.moveto(seg.pt.x, seg.pt.y);
    else {
      const prev = segments[i - 1];
      path.curveto(prev.out.x, prev.out.y, seg.in.x, seg.in.y, seg.pt.x, seg.pt.y);
    }
  }
}

export function roundSegments(shape: unknown, d: number): unknown {
  if (shape instanceof Geometry) {
    const g = new Geometry();
    for (const p of shape.paths) g.add(roundSegments(p, d) as Path);
    return g;
  }
  const path = asPath(shape);
  if (!(path instanceof Path)) return null;
  const points = path.points;
  if (points.length === 0) return path.cloneAndClear();
  const newPoints: Point[] = [];
  points.forEach((pt, i) => {
    const prev = points[(i - 1 + points.length) % points.length];
    const next = points[(i + 1) % points.length];
    const a = geoAngle(prev.x, prev.y, next.x, next.y);
    const [c1x, c1y] = geoCoordinates(pt.x, pt.y, -d, a);
    const [c2x, c2y] = geoCoordinates(pt.x, pt.y, d, a);
    newPoints.push(new Point(c1x, c1y), pt, new Point(c2x, c2y));
  });
  const newPath = path.cloneAndClear();
  constructPath(newPath, newPoints, path.isClosed());
  newPath.newContour();
  return newPath;
}

export function scatter(shape: unknown, amount: number, seed: number): Point[] | null {
  const path = asPath(shape);
  if (!path) return null;
  const r = new JavaRandom(seed);
  const b = path.getBounds();
  const points: Point[] = [];
  for (let i = 0; i < Math.trunc(amount); i++) {
    let tries = 100;
    while (tries > 0) {
      const pt = new Point(b.x + r.nextDouble() * b.width, b.y + r.nextDouble() * b.height);
      if (path.contains(pt)) {
        points.push(pt);
        break;
      }
      tries--;
    }
  }
  return points;
}

export function shapeOnPath(
  shapes: unknown,
  pathShape: unknown,
  amount: number,
  alignment: string,
  spacing: number,
  margin: number,
  baselineOffset: number,
): unknown[] {
  let list = toArray(shapes);
  const path = asPath(pathShape);
  if (list.length === 0 || !path) return [];
  if (alignment === "trailing") list = list.slice().reverse();
  const pathLength = path.getLength();
  if (pathLength === 0) return [];
  const length = pathLength - margin;
  const m = margin / pathLength;
  let c = 0;
  const result: unknown[] = [];
  for (let i = 0; i < Math.trunc(amount); i++) {
    for (const shape of list) {
      let pos: number;
      if (alignment === "distributed") {
        const p = length / (amount * list.length - 1);
        pos = (c * p) / length;
        pos = m + pos * (1 - 2 * m);
      } else {
        pos = ((c * spacing) % length) / length;
        pos = m + pos * (1 - m);
        if (alignment === "trailing") pos = 1 - pos;
      }
      let p1 = path.pointAt(pos);
      const p2 = path.pointAt(pos + 0.0000001);
      const a = geoAngle(p1.x, p1.y, p2.x, p2.y);
      if (baselineOffset) {
        const [bx, by] = geoCoordinates(p1.x, p1.y, baselineOffset, a - 90);
        p1 = new Point(bx, by);
      }
      const t = new Transform();
      t.translate(p1);
      t.rotate(a);
      result.push(mapShape(t, shape as Mappable));
      c++;
    }
  }
  return result;
}

function shapeX(shape: unknown): number {
  return shape instanceof Point ? shape.x : boundsOf(shape).x;
}
function shapeY(shape: unknown): number {
  return shape instanceof Point ? shape.y : boundsOf(shape).y;
}
function shapeCentroid(shape: unknown): Point {
  return shape instanceof Point ? shape : boundsOf(shape).centroid;
}

export function sort(shapes: unknown, orderBy: string, point: Point): unknown[] | null {
  if (shapes === null || shapes === undefined) return null;
  const list = toArray(shapes);
  const methods: Record<string, (s: unknown) => number> = {
    x: shapeX,
    y: shapeY,
    angle: (s) => {
      const c = shapeCentroid(s);
      return geoAngle(c.x, c.y, point.x, point.y);
    },
    distance: (s) => {
      const c = shapeCentroid(s);
      return geoDistance(c.x, c.y, point.x, point.y);
    },
  };
  const method = methods[orderBy];
  if (!method) return list;
  return list.slice().sort((a, b) => method(a) - method(b));
}

export function stack(shapes: unknown, direction: string, margin: number): unknown[] {
  const list = toArray(shapes);
  if (list.length <= 1) return list;
  const firstBounds = boundsOf(list[0]);
  const result: unknown[] = [];
  if (direction === "e") {
    let tx = firstBounds.x;
    for (const shape of list) {
      const b = boundsOf(shape);
      result.push(mapShape(Transform.translated(tx - b.x, 0), shape as Mappable));
      tx += b.width + margin;
    }
  } else if (direction === "w") {
    let tx = firstBounds.x + firstBounds.width;
    for (const shape of list) {
      const b = boundsOf(shape);
      result.push(mapShape(Transform.translated(tx - (b.x + b.width), 0), shape as Mappable));
      tx -= b.width + margin;
    }
  } else if (direction === "n") {
    let ty = firstBounds.y + firstBounds.height;
    for (const shape of list) {
      const b = boundsOf(shape);
      result.push(mapShape(Transform.translated(0, ty - (b.y + b.height)), shape as Mappable));
      ty -= b.height + margin;
    }
  } else if (direction === "s") {
    let ty = firstBounds.y;
    for (const shape of list) {
      const b = boundsOf(shape);
      result.push(mapShape(Transform.translated(0, ty - b.y), shape as Mappable));
      ty += b.height + margin;
    }
  } else {
    throw new Error(`Invalid direction "${direction}."`);
  }
  return result;
}

export function star(position: Point, points: number, outer: number, inner: number): Path {
  const p = new Path();
  points = Math.trunc(points);
  p.moveto(position.x, position.y + outer / 2);
  for (let i = 1; i < points * 2; i++) {
    const a = (i * Math.PI) / points;
    const radius = i % 2 ? inner / 2 : outer / 2;
    p.lineto(position.x + radius * Math.sin(a), position.y + radius * Math.cos(a));
  }
  p.close();
  return p;
}

export function switchShape(shapes: unknown, index = 0): unknown {
  const list = toArray(shapes);
  if (list.length === 0) return null;
  if (index === 0) return list[0];
  let i = Math.trunc(index) % list.length;
  if (i < 0) i += list.length;
  return list[i];
}

function textWidth(text: string, fontName: string, fontSize: number): number {
  const provider = getFontProvider();
  if (provider) return provider.width(text, fontName, fontSize);
  return text.length * fontSize * 0.5;
}

export function textOnPath(
  text: string,
  shape: unknown,
  fontName: string,
  fontSize: number,
  alignment: string,
  margin: number,
  baselineOffset: number,
): Path | null {
  const path = asPath(shape);
  if (!path || path.getLength() <= 0) return null;
  if (text === null || text === undefined) return null;
  text = String(text);
  const p = new Path();
  const stringWidth = textWidth(text, fontName, fontSize);
  const dw = stringWidth / path.getLength();
  let t = 0;
  if (alignment === "trailing") {
    let firstChar = true;
    for (const char of text) {
      const charWidth = textWidth(char, fontName, fontSize);
      if (firstChar) {
        t = (99.9 - margin) / 100.0;
        firstChar = false;
      } else {
        t -= (charWidth / stringWidth) * dw;
      }
      t = ((t % 1.0) + 1.0) % 1.0;
    }
    margin = t * 100;
  }
  let first = true;
  for (const char of text) {
    const charWidth = textWidth(char, fontName, fontSize);
    if (first) {
      t = margin / 100.0;
      first = false;
    } else {
      t += (charWidth / stringWidth) * dw;
    }
    t = ((t % 1.0) + 1.0) % 1.0;
    const pt1 = path.pointAt(t);
    const pt2 = path.pointAt(t + 0.0000001);
    const a = geoAngle(pt2.x, pt2.y, pt1.x, pt1.y);
    const tp = new Text(char, -charWidth, -baselineOffset);
    tp.align = "LEFT";
    tp.fontName = fontName;
    tp.fontSize = fontSize;
    tp.translate(pt1.x, pt1.y);
    tp.rotate(a - 180);
    for (const contour of tp.getPath().contours) p.add(contour);
  }
  p.newContour();
  return p;
}

export function translate(shape: unknown, offset: Point): unknown {
  if (shape === null || shape === undefined) return null;
  return mapShape(Transform.translated(offset), shape as Mappable);
}

export function scale(shape: unknown, scaleBy: Point, origin: Point = Point.ZERO): unknown {
  if (shape === null || shape === undefined) return null;
  const t = new Transform();
  t.translate(origin);
  t.scale(scaleBy.x / 100.0, scaleBy.y / 100.0);
  t.translate(-origin.x, -origin.y);
  return mapShape(t, shape as Mappable);
}

export function rotate(shape: unknown, angle: number, origin: Point = Point.ZERO): unknown {
  if (shape === null || shape === undefined) return null;
  const t = new Transform();
  t.translate(origin);
  t.rotate(angle);
  t.translate(-origin.x, -origin.y);
  return mapShape(t, shape as Mappable);
}

function wigglePoints(shape: unknown, offset: Point, r: JavaRandom): unknown {
  const jitter = (p: Point) => {
    const dx = (r.nextDouble() - 0.5) * offset.x * 2;
    const dy = (r.nextDouble() - 0.5) * offset.y * 2;
    return new Point(p.x + dx, p.y + dy, p.type);
  };
  if (Array.isArray(shape)) return shape.map((p) => (p instanceof Point ? jitter(p) : p));
  if (shape instanceof Point) return jitter(shape);
  if (shape instanceof Contour) return new Contour(shape.points.map(jitter), shape.closed);
  if (shape instanceof Path) {
    const path = shape.cloneAndClear();
    for (const c of shape.contours) path.add(new Contour(c.points.map(jitter), c.closed));
    path.newContour();
    return path;
  }
  if (shape instanceof Geometry) {
    const g = new Geometry();
    for (const p of shape.paths) g.add(wigglePoints(p, offset, r) as Path);
    return g;
  }
  return shape;
}

function wiggleContours(shape: unknown, offset: Point, r: JavaRandom): unknown {
  const move = (c: Contour) => {
    const dx = (r.nextDouble() - 0.5) * offset.x * 2;
    const dy = (r.nextDouble() - 0.5) * offset.y * 2;
    return new Contour(Transform.translated(dx, dy).mapPoints(c.points), c.closed);
  };
  if (shape instanceof Contour) return move(shape);
  if (shape instanceof Path) {
    const path = shape.cloneAndClear();
    for (const c of shape.contours) path.add(move(c));
    path.newContour();
    return path;
  }
  if (shape instanceof Geometry) {
    const g = new Geometry();
    for (const p of shape.paths) g.add(wiggleContours(p, offset, r) as Path);
    return g;
  }
  if (Array.isArray(shape)) return shape.map((s) => wiggleContours(s, offset, r));
  return shape;
}

function wigglePaths(shape: unknown, offset: Point, r: JavaRandom): unknown {
  const move = (p: Path) => {
    const dx = (r.nextDouble() - 0.5) * offset.x * 2;
    const dy = (r.nextDouble() - 0.5) * offset.y * 2;
    return p.transformed(Transform.translated(dx, dy));
  };
  if (shape instanceof Path) return move(shape);
  if (shape instanceof Geometry) {
    const g = new Geometry();
    for (const p of shape.paths) g.add(move(p));
    return g;
  }
  if (Array.isArray(shape)) return shape.map((s) => (s instanceof Path ? move(s) : s));
  return shape;
}

export function wiggle(shape: unknown, scope: string, offset: Point, seed = 0): unknown {
  if (shape === null || shape === undefined) return null;
  const r = new JavaRandom(seed);
  if (scope === "points") return wigglePoints(shape, offset, r);
  if (scope === "contours") return wiggleContours(shape, offset, r);
  if (scope === "paths") return wigglePaths(shape, offset, r);
  return null;
}

export function quadCurve(pt1: Point, pt2: Point, t: number, distance: number): Path {
  t /= 100.0;
  const cx = pt1.x + t * (pt2.x - pt1.x);
  const cy = pt1.y + t * (pt2.y - pt1.y);
  const a = geoAngle(pt1.x, pt1.y, pt2.x, pt2.y) + 90;
  const [qx, qy] = geoCoordinates(cx, cy, distance, a);
  const p = new Path();
  p.moveto(pt1.x, pt1.y);
  const c1x = pt1.x + (2 / 3.0) * (qx - pt1.x);
  const c1y = pt1.y + (2 / 3.0) * (qy - pt1.y);
  const c2x = pt2.x + (2 / 3.0) * (qx - pt2.x);
  const c2y = pt2.y + (2 / 3.0) * (qy - pt2.y);
  p.curveto(c1x, c1y, c2x, c2y, pt2.x, pt2.y);
  p.fillColor = null;
  p.strokeColor = Color.BLACK;
  p.strokeWidth = 1.0;
  return p;
}

export function centerPoint(shape: unknown): Point {
  if (shape === null || shape === undefined) return Point.ZERO;
  return boundsOf(shape).centroid;
}

const javaFunctions = {
  generator,
  filter,
  align,
  arc,
  centroid,
  colorize,
  connect,
  copy,
  doNothing,
  ellipse,
  fit,
  fitTo,
  freehand,
  grid,
  group,
  line,
  lineAngle,
  link,
  makePoint,
  point,
  pointOnPath,
  rect,
  snap,
  skew,
  toPoints,
  ungroup,
  textpath,
  fourPointHandle,
  freehandHandle,
  lineAngleHandle,
  lineHandle,
  pointHandle,
  snapHandle,
  translateHandle,
};

const pythonFunctions = {
  generator,
  filter,
  align,
  arc,
  colorize,
  compound,
  connect,
  copy,
  delete: del,
  distribute,
  edit,
  ellipse,
  fit,
  fit_to: fitTo,
  freehand,
  grid,
  to_points: toPoints,
  group,
  ungroup,
  import_svg: importSvg,
  line: (p1: Point, p2: Point) => line(p1, p2, 2),
  line_angle: (p: Point, a: number, d: number) => lineAngle(p, a, d, 2),
  null: nullShape,
  polygon,
  rect,
  reflect,
  resample,
  round_segments: roundSegments,
  scatter,
  shape_on_path: shapeOnPath,
  snap,
  sort,
  stack,
  star,
  switch: switchShape,
  text_on_path: textOnPath,
  textpath: (text: string, fontName = "Verdana", fontSize = 24, align = "CENTER", position: Point = Point.ZERO, width = 0) =>
    textpath(text, fontName, fontSize, align, position, width),
  translate,
  scale,
  rotate,
  wiggle,
  make_point: makePoint,
  link,
  point_on_path: (shape: unknown, t: number, range: number) => {
    const path = asPath(shape);
    if (!path) return Point.ZERO;
    t = t % range;
    return path.pointAt(t / range);
  },
  quad_curve: quadCurve,
  center_point: centerPoint,
  handle_point: pointHandle,
  handle_four_point: fourPointHandle,
  handle_translate: translateHandle,
  handle_rotate: rotateHandle,
  handle_scale: scaleHandle,
  handle_line: lineHandle,
  handle_star: starHandle,
  handle_reflect: reflectHandle,
  handle_polygon: polygonHandle,
  handle_freehand: freehandHandle,
  handle_snap: snapHandle,
  handle_wiggle: wiggleHandle,
};

export const corevectorLibrary = new JavaScriptLibrary("corevector", { ...pythonFunctions, ...javaFunctions }, {
  impure: ["import_svg"],
});
export const pyvectorLibrary = new JavaScriptLibrary("pyvector", { ...javaFunctions, ...pythonFunctions }, {
  impure: ["import_svg"],
});
