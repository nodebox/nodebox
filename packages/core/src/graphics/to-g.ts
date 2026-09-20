// Conversion between the NodeBox 3 geometry model (absolute coordinates, contours of typed points)
// and the @ndbx/g shape tree that the viewer, the SVG exporter and NodeBox Live nodes work with.

import * as g from "@ndbx/g";
import { Color } from "./color";
import { Contour } from "./contour";
import { Geometry, Path } from "./path";
import { Point } from "./point";
import { Text } from "./text";

export type Grob = Path | Geometry | Text | Contour;

function toPaint(color: Color | null): g.Paint {
  if (color === null || !color.isVisible()) return g.Paint.none();
  return g.Paint.solid(color.r, color.g, color.b, color.a);
}

function contourToG(contour: Contour, target: g.Path): void {
  const points = contour.points;
  for (let i = 0; i < points.length; i++) {
    const pt = points[i];
    if (i === 0) {
      target.moveTo(pt.x, pt.y);
    } else if (pt.isCurveTo() && i >= 3) {
      const c1 = points[i - 2];
      const c2 = points[i - 1];
      target.cubicTo(c1.x, c1.y, c2.x, c2.y, pt.x, pt.y);
    } else if (pt.isLineTo()) {
      target.lineTo(pt.x, pt.y);
    }
  }
  if (contour.closed && points.length > 0) target.close();
}

export function pathToG(path: Path): g.Path {
  const out = new g.Path();
  for (const contour of path.contours) contourToG(contour, out);
  out.fill = toPaint(path.fillColor);
  out.stroke = path.strokeWidth > 0 ? toPaint(path.strokeColor) : g.Paint.none();
  out.strokeWidth = path.strokeWidth;
  return out;
}

export function geometryToG(geometry: Geometry): g.Group {
  const group = new g.Group();
  for (const path of geometry.paths) group.add(pathToG(path));
  return group;
}

export function textToG(text: Text): g.Shape {
  const path = text.getPath();
  if (!path.isEmpty()) return pathToG(path);
  const anchor = text.align === "LEFT" ? "start" : text.align === "RIGHT" ? "end" : "middle";
  const shape = new g.Text(
    text.text,
    text.baseLineX + (text.width > 0 && text.align === "CENTER" ? text.width / 2 : text.width > 0 && text.align === "RIGHT" ? text.width : 0),
    text.baseLineY,
    `${text.fontSize}px`,
    text.fontName,
    "normal",
    anchor,
    toPaint(text.fillColor),
  );
  const [a, b, c, d, e, f] = text.transform.toArray();
  shape.transform = new g.Transform(new Float32Array([a, b, c, d, e, f]));
  return shape;
}

/** Convert any render result (a grob, a point, a list of those, or an @ndbx/g shape) to a shape tree. */
export function toG(value: unknown): g.Shape | null {
  if (value === null || value === undefined) return null;
  if (value instanceof g.Shape) return value;
  if (value instanceof Path) return pathToG(value);
  if (value instanceof Geometry) return geometryToG(value);
  if (value instanceof Text) return textToG(value);
  if (value instanceof Contour) return pathToG(Path.fromContour(value));
  if (value instanceof Point) {
    const dot = new g.Path();
    dot.circle(value.x, value.y, 2);
    dot.fill = g.Paint.solid(0.2, 0.4, 1);
    return dot;
  }
  if (Array.isArray(value)) {
    const group = new g.Group();
    for (const item of value) {
      const shape = toG(item);
      if (shape) group.add(shape);
    }
    return group;
  }
  return null;
}

/** Convert an @ndbx/g shape tree into NodeBox 3 geometry, baking transforms into the coordinates. */
export function fromG(shape: g.Shape): Geometry {
  const geometry = new Geometry();
  const cloned = shape.clone();
  cloned.bakeTransform();
  collect(cloned, geometry);
  return geometry;
}

function collect(shape: g.Shape, into: Geometry): void {
  if (shape instanceof g.Group) {
    for (const child of shape.children) collect(child, into);
    return;
  }
  const path = new Path();
  const data = shape.toPathData(true);
  const parsed = g.Path.fromPathData(data);
  let pi = 0;
  for (const verb of parsed.verbs) {
    switch (verb) {
      case g.Verb.Move: {
        const p = parsed.points[pi++];
        path.moveto(p.x, p.y);
        break;
      }
      case g.Verb.Line: {
        const p = parsed.points[pi++];
        path.lineto(p.x, p.y);
        break;
      }
      case g.Verb.Quad: {
        // Elevate the quadratic to a cubic so the point model stays cubic-only.
        const prev = lastOnCurve(path);
        const c = parsed.points[pi++];
        const p = parsed.points[pi++];
        const c1x = prev.x + (2 / 3) * (c.x - prev.x);
        const c1y = prev.y + (2 / 3) * (c.y - prev.y);
        const c2x = p.x + (2 / 3) * (c.x - p.x);
        const c2y = p.y + (2 / 3) * (c.y - p.y);
        path.curveto(c1x, c1y, c2x, c2y, p.x, p.y);
        break;
      }
      case g.Verb.Cubic: {
        const c1 = parsed.points[pi++];
        const c2 = parsed.points[pi++];
        const p = parsed.points[pi++];
        path.curveto(c1.x, c1.y, c2.x, c2.y, p.x, p.y);
        break;
      }
      case g.Verb.Close:
        path.close();
        break;
    }
  }
  path.fillColor = fromPaint(shape.fill, Color.BLACK);
  path.strokeColor = fromPaint(shape.stroke, null);
  path.strokeWidth = shape.strokeWidth;
  into.add(path);
}

function lastOnCurve(path: Path): Point {
  const contour = path.contours[path.contours.length - 1];
  return contour && contour.points.length > 0 ? contour.points[contour.points.length - 1] : Point.ZERO;
}

function fromPaint(paint: g.Paint | undefined, fallback: Color | null): Color | null {
  if (!paint) return fallback;
  if (paint.type === "solid") {
    const solid = paint as g.SolidPaint;
    return new Color(solid.r, solid.g, solid.b, solid.a);
  }
  if (paint.type === "none") return null;
  return fallback;
}
