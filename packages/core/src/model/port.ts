import { Color } from "../graphics/color";
import { Point } from "../graphics/point";
import {
  DEFAULT_VALUES,
  LiteralValue,
  MenuItem,
  Port,
  PortRange,
  PortType,
  PortValue,
  PortWidget,
  isStandardType,
} from "./types";

export const WIDGETS: PortWidget[] = [
  "none",
  "angle",
  "color",
  "data",
  "file",
  "font",
  "gradient",
  "image",
  "int",
  "menu",
  "seed",
  "string",
  "text",
  "password",
  "toggle",
  "point",
  "float",
];

export function defaultWidgetForType(type: PortType): PortWidget {
  switch (type) {
    case "int":
      return "int";
    case "float":
      return "float";
    case "string":
      return "string";
    case "boolean":
      return "toggle";
    case "point":
      return "point";
    case "color":
      return "color";
    default:
      return "none";
  }
}

export function defaultValueForType(type: PortType): PortValue {
  return isStandardType(type) ? DEFAULT_VALUES[type] : null;
}

/** A fresh port with the defaults NodeBox 3 gives `Port.portForType(name, type)`. */
export function createPort(name: string, type: PortType, overrides: Partial<Port> = {}): Port {
  const port: Port = {
    name,
    type,
    label: "",
    description: "",
    widget: defaultWidgetForType(type),
    range: "value",
    value: defaultValueForType(type),
    menuItems: [],
    ...overrides,
  };
  port.value = clampValue(port, port.value);
  return port;
}

export function clonePort(port: Port): Port {
  return { ...port, menuItems: port.menuItems.map((m) => ({ ...m })) };
}

export function portLabel(port: Port): string {
  return port.label || humanizeName(port.name);
}

/** "start_index" -> "Start Index", like NodeBox 3's StringUtils.humanizeName. */
export function humanizeName(name: string): string {
  return name
    .split("_")
    .filter((s) => s.length > 0)
    .map((s) => s.charAt(0).toUpperCase() + s.slice(1))
    .join(" ");
}

export function parseWidget(s: string): PortWidget {
  const w = s.toLowerCase() as PortWidget;
  if (!WIDGETS.includes(w)) throw new Error(`Unknown widget '${s}'.`);
  return w;
}

export function parseRange(s: string): PortRange {
  if (s === "value" || s === "list") return s;
  throw new Error(`Unknown range '${s}'.`);
}

/** Parse the on-disk representation of a standard value. */
export function parseValue(type: PortType, valueString: string): LiteralValue {
  switch (type) {
    case "int": {
      const v = Number(valueString);
      if (!Number.isFinite(v)) throw new Error(`Invalid int '${valueString}'.`);
      return Math.trunc(v);
    }
    case "float": {
      const v = Number(valueString);
      if (Number.isNaN(v)) throw new Error(`Invalid float '${valueString}'.`);
      return v;
    }
    case "string":
      return valueString;
    case "boolean":
      return valueString.toLowerCase() === "true";
    case "point":
      return Point.parse(valueString);
    case "color":
      return Color.parse(valueString);
    default:
      throw new Error(`Unknown type ${type}`);
  }
}

/** The on-disk representation of a standard value. */
export function formatValue(type: PortType, value: PortValue): string {
  if (value === null || value === undefined) return "";
  switch (type) {
    case "int":
      return String(Math.trunc(Number(value)));
    case "float":
      return formatFloat(Number(value));
    case "boolean":
      return value ? "true" : "false";
    case "point":
      return value instanceof Point ? value.toString() : String(value);
    case "color":
      return value instanceof Color ? value.toString() : String(value);
    default:
      return String(value);
  }
}

/** Java's Double.toString: "100.0" for integral values, otherwise the shortest round-trip form. */
export function formatFloat(v: number): string {
  if (Number.isInteger(v) && Math.abs(v) < 1e7) return `${v}.0`;
  return String(v);
}

/** Coerce a JavaScript value into the representation the port type stores. */
export function convertValue(type: PortType, value: unknown): PortValue {
  if (value === null || value === undefined) return defaultValueForType(type);
  switch (type) {
    case "int":
      if (typeof value === "number") return Math.round(value);
      if (typeof value === "boolean") return value ? 1 : 0;
      if (typeof value === "string") return Math.trunc(Number(value)) || 0;
      return 0;
    case "float":
      if (typeof value === "number") return value;
      if (typeof value === "boolean") return value ? 1 : 0;
      if (typeof value === "string") return Number(value) || 0;
      return 0;
    case "string":
      return typeof value === "string" ? value : String(value);
    case "boolean":
      if (typeof value === "boolean") return value;
      if (typeof value === "number") return value > 0;
      if (typeof value === "string") return value === "true";
      return false;
    case "point":
      if (value instanceof Point) return value;
      if (typeof value === "number") return new Point(value, value);
      if (typeof value === "string") return Point.parse(value);
      if (typeof value === "object" && value !== null && "x" in value && "y" in value)
        return new Point(Number((value as { x: unknown }).x), Number((value as { y: unknown }).y));
      return Point.ZERO;
    case "color":
      if (value instanceof Color) return value;
      if (typeof value === "string") return Color.parse(value);
      if (typeof value === "number") return new Color(value / 255, value / 255, value / 255);
      if (typeof value === "boolean") return value ? Color.WHITE : Color.BLACK;
      if (typeof value === "object" && value !== null && "r" in value) {
        const c = value as { r: number; g: number; b: number; a?: number };
        return new Color(c.r, c.g, c.b, c.a ?? 1);
      }
      return Color.BLACK;
    default:
      return null;
  }
}

export function clampValue(port: Pick<Port, "type" | "min" | "max">, value: PortValue): PortValue {
  if (port.type !== "int" && port.type !== "float") return value;
  if (typeof value !== "number") return value;
  let v = value;
  if (port.min !== undefined && v < port.min) v = port.min;
  else if (port.max !== undefined && v > port.max) v = port.max;
  return port.type === "int" ? Math.trunc(v) : v;
}

export function withValue(port: Port, value: unknown): Port {
  if (!isStandardType(port.type)) return port;
  return { ...port, value: clampValue(port, convertValue(port.type, value)) };
}

/**
 * Classic NodeBox Live keeps its declared types as "classic:<type>". Those ports are untyped at
 * run time: no value is converted, and null is a value like any other rather than "no value".
 */
export const CLASSIC_TYPE_PREFIX = "classic:";

export function isClassicPort(port: Port): boolean {
  return port.type.startsWith(CLASSIC_TYPE_PREFIX);
}

/** The classic type of a port, or undefined when the port did not come from a classic project. */
export function classicTypeOf(port: Port): string | undefined {
  return isClassicPort(port) ? port.type.slice(CLASSIC_TYPE_PREFIX.length) : undefined;
}

/** A classic `shape` port: the one classic type the evaluator itself has a rule for. */
export function isClassicShapePort(port: Port): boolean {
  return port.type === `${CLASSIC_TYPE_PREFIX}shape`;
}

export function isPublishedPort(port: Port): boolean {
  return port.childReference !== undefined && port.childReference !== "";
}

export function childNodeName(port: Port): string | undefined {
  return port.childReference?.split(".")[0];
}

export function childPortName(port: Port): string | undefined {
  const parts = port.childReference?.split(".");
  return parts && parts.length > 1 ? parts.slice(1).join(".") : undefined;
}

/** Every child port a published port feeds: its childReference first, then any childReferences. */
export function publishedTargets(port: Port): { node: string; port: string }[] {
  const targets: { node: string; port: string }[] = [];
  for (const reference of [port.childReference, ...(port.childReferences ?? [])]) {
    if (!reference) continue;
    const dot = reference.indexOf(".");
    if (dot < 0) continue;
    targets.push({ node: reference.slice(0, dot), port: reference.slice(dot + 1) });
  }
  return targets;
}

export function isFileWidget(port: Port): boolean {
  return port.widget === "file" || port.widget === "image";
}

export function hasValueRange(port: Port): boolean {
  return port.range !== "list";
}

export function hasListRange(port: Port): boolean {
  return port.range === "list";
}

export function menuItem(key: string, label?: string): MenuItem {
  return { key, label: label ?? key };
}

/** Equality as NodeBox 3 defines it: name, type, label, value, description and range. */
export function portsEqual(a: Port, b: Port): boolean {
  return (
    a.name === b.name &&
    a.type === b.type &&
    a.label === b.label &&
    valuesEqual(a.value, b.value) &&
    a.description === b.description &&
    a.range === b.range
  );
}

export function valuesEqual(a: PortValue | undefined, b: PortValue | undefined): boolean {
  if (a === b) return true;
  if (a === null || b === null || a === undefined || b === undefined) return false;
  if (a instanceof Point) return a.equals(b);
  if (a instanceof Color) return a.equals(b);
  return false;
}
