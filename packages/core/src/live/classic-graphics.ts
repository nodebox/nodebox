// Classic NodeBox Live shapes, as @ndbx/g shapes the editor can draw.
//
// The classic app rendered g.js objects straight onto a canvas: a path is `{commands, fill, stroke,
// strokeWidth}` with commands `{type: "M"|"L"|"Q"|"C"|"Z", …}`, a group is `{shapes}`, and a colour
// is either a g.js Color (channels from 0 to 1) or a CSS string. The editor draws @ndbx/g shapes,
// so the results are converted on their way to the viewer. Coordinates arrive baked: g.js applies a
// transform by rewriting the commands.

import * as g from "@ndbx/g";

interface ClassicCommand {
  type: string;
  x?: number;
  y?: number;
  x1?: number;
  y1?: number;
  x2?: number;
  y2?: number;
}

interface ClassicPath {
  commands: ClassicCommand[];
  fill?: unknown;
  stroke?: unknown;
  strokeWidth?: number;
}

interface ClassicGroup {
  shapes: unknown[];
}

interface ClassicText {
  text: string;
  fontFamily?: string;
  fontSize?: number;
  textAlign?: string;
  fill?: unknown;
  stroke?: unknown;
  strokeWidth?: number;
  _x?: number;
  _y?: number;
  x?: number;
  y?: number;
}

export function isClassicPath(value: unknown): value is ClassicPath {
  return typeof value === "object" && value !== null && Array.isArray((value as ClassicPath).commands);
}

export function isClassicGroup(value: unknown): value is ClassicGroup {
  return typeof value === "object" && value !== null && Array.isArray((value as ClassicGroup).shapes);
}

function isClassicText(value: unknown): value is ClassicText {
  return typeof value === "object" && value !== null && typeof (value as ClassicText).text === "string";
}

/** A classic result as an @ndbx/g shape, or null when it is not something to draw. */
export function classicToG(value: unknown): g.Shape | null {
  if (value === null || value === undefined) return null;
  if (value instanceof g.Shape) return value;
  if (isClassicGroup(value)) {
    const group = new g.Group();
    for (const child of value.shapes) {
      const shape = classicToG(child);
      if (shape) group.add(shape);
    }
    return group;
  }
  if (isClassicPath(value)) return classicPathToG(value);
  if (isClassicText(value)) return classicTextToG(value);
  if (Array.isArray(value)) {
    const group = new g.Group();
    for (const item of value) {
      const shape = classicToG(item);
      if (shape) group.add(shape);
    }
    return group.children.length > 0 ? group : null;
  }
  return null;
}

function classicPathToG(path: ClassicPath): g.Path {
  const out = new g.Path();
  for (const command of path.commands) {
    switch (command.type) {
      case "M":
        out.moveTo(command.x ?? 0, command.y ?? 0);
        break;
      case "L":
        out.lineTo(command.x ?? 0, command.y ?? 0);
        break;
      case "Q":
        out.quadTo(command.x1 ?? 0, command.y1 ?? 0, command.x ?? 0, command.y ?? 0);
        break;
      case "C":
        out.curveTo(command.x1 ?? 0, command.y1 ?? 0, command.x2 ?? 0, command.y2 ?? 0, command.x ?? 0, command.y ?? 0);
        break;
      case "Z":
        out.close();
        break;
    }
  }
  out.fill = classicPaint(path.fill, g.Paint.black());
  out.stroke = classicPaint(path.stroke, g.Paint.none());
  out.strokeWidth = path.strokeWidth ?? 1;
  return out;
}

const TEXT_ANCHORS: Record<string, g.Text["textAnchor"]> = { left: "start", center: "middle", right: "end" };

function classicTextToG(text: ClassicText): g.Text {
  const out = new g.Text(
    text.text,
    text._x ?? text.x ?? 0,
    text._y ?? text.y ?? 0,
    `${text.fontSize ?? 24}px`,
    text.fontFamily ?? "sans-serif",
    "normal",
    TEXT_ANCHORS[text.textAlign ?? "center"] ?? "middle",
    classicPaint(text.fill, g.Paint.black()),
  );
  out.stroke = classicPaint(text.stroke, g.Paint.none());
  out.strokeWidth = text.strokeWidth ?? 1;
  return out;
}

/** g.js colours are objects with channels from 0 to 1, or the CSS strings a source wrote by hand. */
function classicPaint(value: unknown, fallback: g.Paint): g.Paint {
  if (value === null) return g.Paint.none();
  if (value === undefined) return fallback;
  if (value instanceof g.Paint) return value;
  if (typeof value === "string") {
    try {
      return g.Paint.parse(value);
    } catch {
      return fallback;
    }
  }
  if (typeof value === "object" && "r" in value && "g" in value && "b" in value) {
    const color = value as { r: number; g: number; b: number; a?: number };
    return g.Paint.solid(color.r, color.g, color.b, color.a ?? 1);
  }
  return fallback;
}
