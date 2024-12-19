import { Circle } from "./circle";
import { Ellipse } from "./ellipse";
import { Group } from "./group";
import { Line } from "./line";
import { Paint, SolidPaint } from "./paint";
import { Path } from "./path";
import { Rect } from "./rect";
import { Shape } from "./shape";
import { Text } from "./text";
import { Transform } from "./transform";

// This also exists as Node.ELEMENT_NODE but Node.js (which we use for testing) doesn't support this.
const NODE_TYPE_ELEMENT = 1;

export function parseSVG(s: string, parser?: DOMParser): Shape {
  if (!parser) {
    parser = new DOMParser();
  }
  const dom = parser!.parseFromString(s, "image/svg+xml");
  const svgRoot = dom.getElementsByTagName("svg")[0];
  if (!svgRoot) {
    throw new Error(`No SVG element found`);
  }

  const svgElement = svgRoot as Element;
  const height = svgElement.getAttribute("height");
  const width = svgElement.getAttribute("width");
  const viewBox = svgElement.getAttribute("viewBox");

  const childElements = Array.from(svgRoot.childNodes).filter((n) => n.nodeType === NODE_TYPE_ELEMENT) as Element[];
  if (childElements.length === 0) {
    return new Group();
  } else if (childElements.length === 1) {
    const shape = parseElement(childElements[0]);
    if (shape) return shape;
  } else {
    const g = new Group();
    if (width) g.width = parseInt(width);
    if (height) g.height = parseInt(height);
    if (viewBox) g.viewBox = viewBox;
    for (const node of childElements) {
      const shape = parseElement(node);
      if (shape) g.add(shape);
    }
    return g;
  }
  return new Group();
}

function parseElement(el: Element, parentAttributes?: Record<string, any>): Shape | null {
  parentAttributes = parentAttributes || {};
  let shape, fill;

  const id = el.getAttribute("id") || undefined;
  const stroke = parseAttribute(el, "stroke", Paint.parse, Paint.none(), parentAttributes);
  const strokeWidth = parseAttribute(el, "stroke-width", parseFloat, 1, parentAttributes);
  const opacity = parseAttribute(el, "opacity", parseFloat, 1, parentAttributes);
  const fillOpacity = parseAttribute(el, "fill-opacity", parseFloat, 1, parentAttributes);
  const strokeOpacity = parseAttribute(el, "stroke-opacity", parseFloat, 1, parentAttributes);
  //const tags = parseAttribute(el, "className", String, undefined, )

  switch (el.nodeName) {
    case "circle":
      shape = new Circle(num(el, "cx"), num(el, "cy"), num(el, "r"));
      fill = parseAttribute(el, "fill", Paint.parse, Paint.black(), parentAttributes);
      break;
    case "ellipse":
      shape = new Ellipse(num(el, "cx"), num(el, "cy"), num(el, "rx"), num(el, "ry"));
      fill = parseAttribute(el, "fill", Paint.parse, Paint.black(), parentAttributes);
      break;
    case "g":
      shape = new Group();
      fill = parseAttribute(el, "fill", Paint.parse, Paint.none(), parentAttributes);
      const childElements = Array.from(el.childNodes).filter((n) => n.nodeType === NODE_TYPE_ELEMENT) as Element[];
      for (const node of childElements) {
        const childShape = parseElement(node, {
          ...parentAttributes,
          id,
          fill,
          stroke,
          strokeWidth,
          opacity,
          fillOpacity,
          strokeOpacity,
        });
        if (childShape) (shape as Group).add(childShape);
      }
      break;
    case "line":
      shape = new Line(num(el, "x1"), num(el, "y1"), num(el, "x2"), num(el, "y2"));
      break;
    case "path":
      shape = Path.fromPathData(el.getAttribute("d") || "");
      fill = parseAttribute(el, "fill", Paint.parse, Paint.black(), parentAttributes);
      break;
    case "rect":
      shape = new Rect(num(el, "x"), num(el, "y"), num(el, "width"), num(el, "height"));
      fill = parseAttribute(el, "fill", Paint.parse, Paint.black(), parentAttributes);
      break;
    case "text":
      shape = new Text(
        // id,
        el.textContent || "",
        num(el, "x"),
        num(el, "y"),
        str(el, "font-size", "12px"),
        str(el, "font-family", "sans-serif"),
        str(el, "font-weight", "normal") as "normal",
      );
      (shape as Text).textAnchor = str(el, "text-anchor", "start") as "start" | "end" | "middle";
      fill = parseAttribute(el, "fill", Paint.parse, Paint.black(), parentAttributes);
      break;
    case "metadata":
      return null;
    default:
      console.warn(`Unknown element type ${el.nodeName}`);
      return null;
  }

  if (id) {
    let tagMatch = id.match(/\[.*?\]/g);
    if (tagMatch) {
      shape.tags = tagMatch.map((tag) => tag.slice(1, -1));
    } else {
      let tagMatch = id.match(/%5B.*?%5D/g);
      if (tagMatch) {
        shape.tags = tagMatch.map((tag) => tag.slice(3, -3));
      } else {
        shape.tags = id.split(" ");
      }
    }
  }

  if (el.hasAttribute("transform")) {
    shape.transform = Transform.fromSvgTransform(el.getAttribute("transform")!);
  }

  if (fill) shape.fill = blendPaintWithOpacity(fill, fillOpacity);
  shape.stroke = blendPaintWithOpacity(stroke, strokeOpacity);
  shape.strokeWidth = strokeWidth;
  shape.opacity = opacity;
  return shape;
}

function parseAttribute<T>(
  el: Element,
  name: string,
  parse: (value: string) => T,
  defaultValue: T,
  parentAttributes?: Record<string, any>,
): T {
  if (el.hasAttribute(name)) {
    return parse(el.getAttribute(name)!);
  } else if (parentAttributes && parentAttributes[name]) {
    return parentAttributes[name];
  } else {
    return defaultValue;
  }
}

function num(el: Element, key: string, defaultValue?: number): number {
  return el.hasAttribute(key) ? parseFloat(el.getAttribute(key)!) : defaultValue !== undefined ? defaultValue : 0;
}

function str(el: Element, key: string, defaultValue?: string): string {
  return el.hasAttribute(key) ? el.getAttribute(key)! : defaultValue !== undefined ? defaultValue : "";
}

function blendPaintWithOpacity(paint: Paint, opacity: number): Paint {
  if (paint.type === "solid") {
    const solid = paint as SolidPaint;
    return new SolidPaint(solid.r, solid.g, solid.b, solid.a * opacity);
  } else {
    return paint;
  }
}
