import { Shape, Group, Paint, SolidPaint, Path, Circle, Rect, Text, Point, Line } from "@ndbx/g";
import * as vega from "vega";
import {
  curveBasis,
  curveBasisClosed,
  curveBasisOpen,
  curveBundle,
  curveCardinal,
  curveCardinalClosed,
  curveCardinalOpen,
  curveCatmullRom,
  curveCatmullRomClosed,
  curveCatmullRomOpen,
  curveLinear,
  curveLinearClosed,
  curveMonotoneX,
  curveMonotoneY,
  curveNatural,
  curveStep,
  curveStepAfter,
  curveStepBefore,
  CurveFactory,
  CurveGenerator,
} from "d3-shape";

const HalfPi = Math.PI / 2;
const HalfSqrt3 = Math.sqrt(3) / 2;
const Tan30 = 0.5773502691896257;

type InterpolationMode =
  | "basis"
  | "basis-closed"
  | "basis-open"
  | "bundle"
  | "cardinal"
  | "cardinal-open"
  | "cardinal-closed"
  | "catmull-rom"
  | "catmull-rom-closed"
  | "catmull-rom-open"
  | "linear"
  | "linear-closed"
  | "monotone"
  | "natural"
  | "step"
  | "step-after"
  | "step-before";

interface VegaShape {
  context: (path: Path) => (item: VegaItem) => void;
}

interface VegaBounds {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}

interface VegaPadding {
  top: number;
  bottom: number;
  left: number;
  right: number;
}

interface VegaScene {
  root: VegaItem;
}

interface VegaItem {
  bounds: VegaBounds;
  marktype: string;
  items: VegaItem[];
  x: number;
  y: number;
  role: string;
}

interface VegaVisualItem extends VegaItem {
  fill: string;
  stroke: string;
  strokeWidth: number;
  opacity: number;
  fillOpacity?: number;
  strokeOpacity?: number;
}

interface VegaArcItem extends VegaVisualItem {
  innerRadius: number;
  outerRadius: number;
  startAngle: number;
  endAngle: number;
}

interface VegaAreaItem extends VegaVisualItem {
  orient: "horizontal" | "vertical";
  interpolate: InterpolationMode;
  x2: number;
  y2: number;
}

interface VegaGroupItem extends VegaItem {
  width: number;
  height: number;
  fill: string;
  clip?: boolean;
}

interface VegaLineItem extends VegaVisualItem {
  interpolate: InterpolationMode;
  tension?: number;
  orient?: "horizontal" | "vertical";
}

interface VegaPathItem extends VegaVisualItem {
  path: string;
}

interface VegaRectItem extends VegaVisualItem {
  width: number;
  height: number;
}

interface VegaSymbolItem extends VegaVisualItem {
  shape: string;
  innerRadius: number;
  size: number;
}

interface VegaTextItem extends VegaVisualItem {
  angle: number;
  align: "left" | "center" | "right";
  baseline: "top" | "middle" | "bottom" | "line-top" | "line-bottom";
  dx?: number;
  dy?: number;
  font: string;
  fontSize: number;
  fontWeight: "normal" | "bold";
  lineHeight?: number;
  limit: number;
  radius?: number;
  text: string;
  theta?: number;
}

interface VegaRuleItem extends VegaVisualItem {
  x2: number;
  y2: number;
}

interface VegaShapeItem extends VegaVisualItem {
  shape: VegaShape;
}

const TEXT_ALIGN_MAP = {
  left: "start",
  center: "middle",
  right: "end",
};

// prettier-ignore
const INTERPOLATION_MAP = {
  "basis": { curve: curveBasis },
  "basis-closed": { curve: curveBasisClosed },
  "basis-open": { curve: curveBasisOpen },
  "bundle": { curve: curveBundle, tension: "beta", value: 0.85 },
  "cardinal": { curve: curveCardinal, tension: "tension", value: 0 },
  "cardinal-open": { curve: curveCardinalOpen, tension: "tension", value: 0 },
  "cardinal-closed": { curve: curveCardinalClosed, tension: "tension", value: 0 },
  "catmull-rom": { curve: curveCatmullRom, tension: "alpha", value: 0.5 },
  "catmull-rom-closed": { curve: curveCatmullRomClosed, tension: "alpha", value: 0.5 },
  "catmull-rom-open": { curve: curveCatmullRomOpen, tension: "alpha", value: 0.5 },
  "linear": { curve: curveLinear },
  "linear-closed": { curve: curveLinearClosed },
  "monotone": { horizontal: curveMonotoneX, vertical: curveMonotoneY },
  "natural": { curve: curveNatural },
  "step": { curve: curveStep },
  "step-after": { curve: curveStepAfter },
  "step-before": { curve: curveStepBefore },
};

export function vegaToShape(spec: vega.Spec) {
  const flow = vega.parse(spec);
  const view = new vega.View(flow);
  view.run();
  view.finalize();
  const scenegraph = view.scenegraph() as unknown as VegaScene;
  const [dx, dy] = view.origin();
  const { top, left, bottom, right } = view.padding() as VegaPadding;
  const root = new Group();
  const bounds = scenegraph.root.bounds;
  const totalWidth = Math.ceil(bounds.x2 - bounds.x1 + left + right);
  const totalHeight = Math.ceil(bounds.y2 - bounds.y1 + top + bottom);
  const background = new Rect(0, 0, totalWidth, totalHeight);
  const viewBackground = view.background();
  background.fill = viewBackground ? Paint.parse(viewBackground) : Paint.white();
  root.add(background);

  const group = convertMarkDefinition(scenegraph.root);
  group.transform.translate(dx + left, dy + top);
  root.add(group);

  return root;
}

// From the docs:
// "The levels of the tree alternate between an enclosing mark definition and contained sets of mark instances called items."
// This means we have two functions: convertMarkDefinition and convertXXXInstance for specific instances.
function convertMarkDefinition(def: VegaItem) {
  const group = new Group();
  group.tags = [`mark-${def.marktype}`];
  switch (def.marktype) {
    case "arc":
      for (const item of def.items) {
        group.add(convertArcInstance(item as VegaArcItem));
      }
      break;
    case "area": {
      const path = convertArea(def);
      group.add(path);
      break;
    }
    case "group":
      if (def.role) {
        group.tags.push(`role-${def.role}`);
      }
      for (const item of def.items) {
        group.add(convertGroupInstance(item as VegaGroupItem));
      }
      break;
    case "line": {
      const path = convertLine(def);
      group.add(path);
      break;
    }
    case "path":
      for (const item of def.items) {
        group.add(convertPathInstance(item as VegaPathItem));
      }
      break;
    case "rect":
      for (const item of def.items) {
        group.add(convertRectInstance(item as VegaRectItem));
      }
      break;
    case "rule":
      for (const item of def.items) {
        group.add(convertRuleInstance(item as VegaRuleItem));
      }
      break;
    case "shape":
      for (const item of def.items) {
        group.add(convertShapeInstance(item as VegaShapeItem));
      }
      break;
    case "symbol":
      for (const item of def.items) {
        group.add(convertSymbolInstance(item as VegaSymbolItem));
      }
      break;
    case "text":
      for (const item of def.items) {
        group.add(convertTextInstance(item as VegaTextItem));
      }
      break;
    default:
      console.warn(`Unknown marktype ${def.marktype}`);
  }
  return group;
}

function convertArcInstance(instance: VegaArcItem): Shape {
  const path = new Path();
  path.complexArc(
    instance.x,
    instance.y,
    instance.innerRadius,
    instance.outerRadius,
    instance.startAngle,
    instance.endAngle,
  );
  applyStyles(path, instance);
  return path;
}

function convertArea(def: VegaItem): Shape {
  if (def.items.length < 2) {
    return new Path();
  }

  const firstItem = def.items[0] as VegaAreaItem;
  const [path, curve] = createCurve(firstItem.interpolate, firstItem.orient);
  const n = def.items.length;
  const isHorizontal = firstItem.orient === "horizontal";

  curve.areaStart();
  curve.lineStart();

  // Draw top line
  for (let i = 0; i < n; i++) {
    const instance = def.items[i] as VegaAreaItem;
    curve.point(instance.x, instance.y);
  }
  curve.lineEnd();

  // Draw bottom line in reverse
  curve.lineStart();
  for (let i = n - 1; i >= 0; i--) {
    const instance = def.items[i] as VegaAreaItem;
    const baseline = isHorizontal ? instance.x2 : instance.y2;
    curve.point(instance.x, baseline);
  }
  curve.lineEnd();
  curve.areaEnd();

  applyStyles(path, firstItem);
  return path;
}

function convertGroupInstance(instance: VegaGroupItem): Shape {
  const group = new Group();
  // Add background
  if (instance.fill) {
    const background = new Rect(0, 0, instance.width, instance.height);
    background.fill = Paint.parse(instance.fill);
    group.add(background);
  }
  let tx = instance.x || 0;
  let ty = instance.y || 0;
  if (tx || ty) {
    group.transform.translate(tx, ty);
  }
  if (instance.role) {
    group.tags = [instance.role];
  }
  if (instance.clip) {
    const clipRect = new Rect(0, 0, instance.width, instance.height);
    group.clip(clipRect);
  }
  for (const item of instance.items) {
    group.add(convertMarkDefinition(item));
  }
  return group;
}

function convertLine(def: VegaItem): Shape {
  const firstItem = def.items[0] as VegaLineItem;
  const [path, curve] = createCurve(firstItem.interpolate, firstItem.orient);
  if (def.items.length >= 2) {
    curve.lineStart();
    for (let i = 0; i < def.items.length; i++) {
      const instance = def.items[i];
      curve.point(instance.x, instance.y);
    }
    curve.lineEnd();
    applyStyles(path, firstItem);
  }
  return path;
}

function convertPathInstance(instance: VegaPathItem): Shape {
  if (!instance.path) return new Path();
  const path = Path.fromPathData(instance.path);
  path.fill = Paint.none();
  let { x, y } = instance;
  if ((x ?? 0) !== 0 || (y ?? 0) !== 0) {
    path.transform.translate(x, y);
  }
  applyStyles(path, instance);
  return path;
}

function convertRectInstance(instance: VegaRectItem): Shape {
  const rect = new Rect(instance.x, instance.y, instance.width, instance.height);
  return applyStyles(rect, instance);
}

function convertRuleInstance(instance: VegaRuleItem): Shape {
  let x2 = instance.x;
  let y2 = instance.y;
  if (typeof instance.x2 === "number") x2 = instance.x2;
  if (typeof instance.y2 === "number") y2 = instance.y2;
  const line = new Line(instance.x, instance.y, x2, y2);
  if (instance.stroke) line.stroke = Paint.parse(instance.stroke);
  if (instance.strokeWidth) line.strokeWidth = instance.strokeWidth;
  if (instance.opacity) {
    const stroke = line.stroke as Paint;
    if (stroke.type === "solid") {
      (stroke as SolidPaint).a = instance.opacity;
    }
  }
  return line;
}

function convertShapeInstance(instance: VegaShapeItem): Shape {
  const path = new Path();
  instance.shape.context(path)(instance);
  applyStyles(path, instance);
  return path;
}

function convertSymbolInstance(instance: VegaSymbolItem): Shape {
  const size = instance.size;
  const shape = instance.shape || "circle";

  switch (shape) {
    case "circle": {
      const r = Math.sqrt(size) / 2;
      const circle = new Circle(0, 0, r);
      return applyStylesAndTransform(circle, instance);
    }
    case "cross": {
      const r = Math.sqrt(size) / 2;
      const s = r / 2.5;
      const path = new Path();
      path.moveTo(-r, -s);
      path.lineTo(-r, s);
      path.lineTo(-s, s);
      path.lineTo(-s, r);
      path.lineTo(s, r);
      path.lineTo(s, s);
      path.lineTo(r, s);
      path.lineTo(r, -s);
      path.lineTo(s, -s);
      path.lineTo(s, -r);
      path.lineTo(-s, -r);
      path.lineTo(-s, -s);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "diamond": {
      const r = Math.sqrt(size) / 2;
      const path = new Path();
      path.moveTo(-r, 0);
      path.lineTo(0, -r);
      path.lineTo(r, 0);
      path.lineTo(0, r);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "square": {
      const w = Math.sqrt(size);
      const x = -w / 2;
      const rect = new Rect(x, x, w, w);
      return applyStylesAndTransform(rect, instance);
    }
    case "arrow": {
      const r = Math.sqrt(size) / 2;
      const s = r / 7;
      const t = r / 2.5;
      const v = r / 8;
      const path = new Path();
      path.moveTo(-s, r);
      path.lineTo(s, r);
      path.lineTo(s, -v);
      path.lineTo(t, -v);
      path.lineTo(0, -r);
      path.lineTo(-t, -v);
      path.lineTo(-s, -v);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "wedge": {
      const r = Math.sqrt(size) / 2;
      const h = HalfSqrt3 * r;
      const o = h - r * Tan30;
      const b = r / 4;
      const path = new Path();
      path.moveTo(0, -h - o);
      path.lineTo(-b, h - o);
      path.lineTo(b, h - o);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "triangle": {
      const r = Math.sqrt(size) / 2;
      const h = HalfSqrt3 * r;
      const o = h - r * Tan30;
      const path = new Path();
      path.moveTo(0, -h - o);
      path.lineTo(-r, h - o);
      path.lineTo(r, h - o);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "triangle-up": {
      const r = Math.sqrt(size) / 2;
      const h = HalfSqrt3 * r;
      const path = new Path();
      path.moveTo(0, -h);
      path.lineTo(-r, h);
      path.lineTo(r, h);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "triangle-down": {
      const r = Math.sqrt(size) / 2;
      const h = HalfSqrt3 * r;
      const path = new Path();
      path.moveTo(0, h);
      path.lineTo(-r, -h);
      path.lineTo(r, -h);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "triangle-right": {
      const r = Math.sqrt(size) / 2;
      const h = HalfSqrt3 * r;
      const path = new Path();
      path.moveTo(h, 0);
      path.lineTo(-h, -r);
      path.lineTo(-h, r);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "triangle-left": {
      const r = Math.sqrt(size) / 2;
      const h = HalfSqrt3 * r;
      const path = new Path();
      path.moveTo(-h, 0);
      path.lineTo(h, -r);
      path.lineTo(h, r);
      path.close();
      return applyStylesAndTransform(path, instance);
    }
    case "stroke": {
      const r = Math.sqrt(size) / 2;
      const path = new Path();
      path.moveTo(-r, 0);
      path.lineTo(r, 0);
      return applyStylesAndTransform(path, instance);
    }
    default:
      const path = Path.fromPathData(shape);
      return applyStylesAndTransform(path, instance);
  }
}

function convertTextInstance(instance: VegaTextItem): Shape {
  let x = instance.x || 0;
  let y = (instance.y || 0) + verticalOffset(instance);
  if (instance.dx) x += instance.dx;
  if (instance.dy) y += instance.dy;
  const r = instance.radius || 0;
  if (r) {
    const t = (instance.theta || 0) - HalfPi;
    x += r * Math.cos(t);
    y += r * Math.sin(t);
  }
  const text = new Text(instance.text, x, y, String(instance.fontSize), instance.font || "sans-serif");
  if (instance.fontWeight) text.fontWeight = instance.fontWeight;
  text.textAnchor = TEXT_ALIGN_MAP[instance.align] as "start" | "end" | "middle";
  if (instance.angle) {
    text.transform.rotateDegrees(instance.angle, new Point(instance.x, instance.y));
  }
  applyStyles(text, instance);
  return text;
}

function createCurve(
  interpolate: InterpolationMode,
  orient: "horizontal" | "vertical" | undefined,
): [Path, CurveGenerator] {
  const interp = INTERPOLATION_MAP[interpolate] || { curve: curveLinear };

  const path = new Path();
  const curveFn = "curve" in interp ? interp.curve : (interp[orient || "horizontal"] as unknown as CurveFactory);
  const curve = curveFn(path as unknown as Path2D);
  return [path, curve as CurveGenerator];
}

function verticalOffset(instance: VegaTextItem): number {
  // This code comes from vega-scenegraph/src/util/text.js#offset
  const fontSize = instance.fontSize || 11;
  switch (instance.baseline) {
    case "top":
      return fontSize * 0.79;
    case "middle":
      return fontSize * 0.3;
    case "bottom":
      return fontSize * -0.21;
    case "line-top":
      return fontSize * 0.29 + 0.5 * (instance.lineHeight || fontSize + 2);
    case "line-bottom":
      return fontSize * 0.29 - 0.5 * (instance.lineHeight || fontSize + 2);
    default:
      return 0;
  }
}

function applyStylesAndTransform(shape: Shape, instance: VegaVisualItem): Shape {
  shape.transform.translate(instance.x, instance.y);
  return applyStyles(shape, instance);
}

function applyStyles(shape: Shape, instance: VegaVisualItem): Shape {
  // By default, shapes have no fill (this is different from SVG shapes)
  shape.fill = Paint.none();
  if (instance.fill) shape.fill = Paint.parse(instance.fill);
  if (instance.stroke) shape.stroke = Paint.parse(instance.stroke);
  if (instance.strokeWidth) shape.strokeWidth = instance.strokeWidth;
  if (instance.opacity != null) shape.opacity = instance.opacity;

  const fill = shape.fill as SolidPaint;
  const stroke = shape.stroke as SolidPaint;

  if (fill) {
    if (instance.fillOpacity != null) fill.a = instance.fillOpacity;
  }
  if (stroke) {
    if (instance.strokeOpacity != null) stroke.a = instance.strokeOpacity;
  }

  return shape;
}
