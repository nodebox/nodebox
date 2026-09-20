// Runtime value classification and conversion, a port of nodebox.util.ListUtils.listClass and
// nodebox.node.TypeConversions. JavaScript has one number type, so integer-ness travels with the
// declared port or output type instead of with the value.

import { Color } from "../graphics/color";
import { Contour } from "../graphics/contour";
import { Geometry, Path } from "../graphics/path";
import { Point } from "../graphics/point";
import { fromG, isGShape } from "../graphics/to-g";
import { Text } from "../graphics/text";
import { PortType } from "../model/types";

/** The runtime type of a value, in NodeBox 3 port-type vocabulary. */
export type ValueType =
  | "int"
  | "float"
  | "string"
  | "boolean"
  | "point"
  | "color"
  | "geometry"
  | "path"
  | "contour"
  | "text"
  | "gshape"
  | "list"
  | "map"
  | "null"
  | "object"
  | "mixed";

export function valueType(value: unknown, numberHint: "int" | "float" = "float"): ValueType {
  if (value === null || value === undefined) return "null";
  switch (typeof value) {
    case "number":
      return numberHint;
    case "string":
      return "string";
    case "boolean":
      return "boolean";
    default:
      break;
  }
  if (value instanceof Point) return "point";
  if (value instanceof Color) return "color";
  if (value instanceof Geometry) return "geometry";
  if (value instanceof Path) return "path";
  if (value instanceof Contour) return "contour";
  if (value instanceof Text) return "text";
  if (isGShape(value)) return "gshape";
  if (Array.isArray(value)) return "list";
  if (value instanceof Map) return "map";
  return "object";
}

/** The common type of a list of values ("mixed" when they differ), like ListUtils.listClass. */
export function listType(values: readonly unknown[], numberHint: "int" | "float" = "float"): ValueType {
  let type: ValueType | undefined;
  for (const v of values) {
    const t = valueType(v, numberHint);
    if (type === undefined) type = t;
    else if (type !== t) {
      if (isGeometryType(type) && isGeometryType(t)) type = "geometry";
      else return "mixed";
    }
  }
  return type ?? "null";
}

export function isGeometryType(t: ValueType): boolean {
  return t === "geometry" || t === "path" || t === "contour" || t === "text" || t === "gshape";
}

/**
 * Convert values of a known source type to the target port type. Only the conversions NodeBox 3
 * defines are applied; everything else passes through unchanged.
 */
export function convertValues(sourceType: ValueType, targetType: PortType, values: readonly unknown[]): unknown[] {
  if (values.length === 0) return values as unknown[];
  // Geometry flowing into a point port expands into its points (one shape may yield many points).
  if (isGeometryType(sourceType) && targetType === "point") {
    const points: unknown[] = [];
    for (const v of values) {
      if (v instanceof Text) points.push(...v.getPath().points);
      else if (isGShape(v)) points.push(...fromG(v).points);
      else if (v instanceof Path || v instanceof Geometry || v instanceof Contour) points.push(...v.points);
    }
    return points;
  }
  // @ndbx/g shapes from NodeBox Live nodes become geometry for NodeBox 3 nodes.
  if (sourceType === "gshape" && targetType === "geometry") return values.map((v) => (isGShape(v) ? fromG(v) : v));
  const convert = converter(sourceType, targetType);
  return convert ? values.map(convert) : (values as unknown[]);
}

export function canBeConverted(sourceType: ValueType, targetType: PortType): boolean {
  return sourceType === targetType || converter(sourceType, targetType) !== undefined;
}

type Converter = (v: unknown) => unknown;

function converter(sourceType: ValueType, targetType: PortType): Converter | undefined {
  if (targetType === "list") return undefined;
  switch (sourceType) {
    case "int":
      switch (targetType) {
        case "float":
          return (v) => v;
        case "string":
          return (v) => formatJavaLong(v as number);
        case "boolean":
          return (v) => (v as number) > 0;
        case "color":
          return (v) => Color.gray((v as number) / 255);
        case "point":
          return (v) => new Point(v as number, v as number);
      }
      return undefined;
    case "float":
      switch (targetType) {
        case "int":
          return (v) => Math.round(v as number);
        case "string":
          return (v) => formatJavaDouble(v as number);
        case "boolean":
          return (v) => (v as number) > 0;
        case "color":
          return (v) => Color.gray((v as number) / 255);
        case "point":
          return (v) => new Point(v as number, v as number);
      }
      return undefined;
    case "string":
      switch (targetType) {
        case "int":
          return (v) => parseJavaLong(v as string);
        case "float":
          return (v) => parseJavaDouble(v as string);
        case "boolean":
          return (v) => (v as string).toLowerCase() === "true";
        case "color":
          return (v) => Color.parse(v as string);
        case "point":
          return (v) => Point.parse(v as string);
      }
      return undefined;
    case "boolean":
      switch (targetType) {
        case "int":
          return (v) => (v ? 1 : 0);
        case "float":
          return (v) => (v ? 1.0 : 0.0);
        case "string":
          return (v) => String(v);
        case "color":
          return (v) => (v ? Color.WHITE : Color.BLACK);
      }
      return undefined;
    case "color":
    case "point":
    case "text":
      return targetType === "string" ? (v) => String(v) : undefined;
    case "geometry":
    case "path":
    case "contour":
      if (targetType === "string") return (v) => String(v);
      return undefined;
    default:
      return undefined;
  }
}

/** Java's Long.parseLong throws on anything that is not an integer literal. */
export function parseJavaLong(s: string): number {
  const trimmed = s.trim();
  if (!/^[+-]?\d+$/.test(trimmed)) throw new Error(`For input string: "${s}"`);
  return parseInt(trimmed, 10);
}

export function parseJavaDouble(s: string): number {
  const v = Number(s.trim());
  if (Number.isNaN(v) || s.trim() === "") throw new Error(`For input string: "${s}"`);
  return v;
}

export function formatJavaLong(v: number): string {
  return String(Math.trunc(v));
}

/** Java's Double.toString: integral values print with ".0", large or small values in exponent form. */
export function formatJavaDouble(v: number): string {
  if (Number.isNaN(v)) return "NaN";
  if (v === Infinity) return "Infinity";
  if (v === -Infinity) return "-Infinity";
  const abs = Math.abs(v);
  if (abs !== 0 && (abs < 1e-3 || abs >= 1e7)) {
    // Java: "1.0E7", "1.234E-4"
    const [mantissa, exponent] = v.toExponential().split("e");
    const m = mantissa.includes(".") ? mantissa : `${mantissa}.0`;
    return `${m}E${exponent.replace("+", "")}`;
  }
  if (Number.isInteger(v)) return `${v}.0`;
  return String(v);
}

/** Wrap a scalar into a one-element list; lists pass through; null becomes the empty list. */
export function asList(value: unknown): unknown[] {
  if (value === null || value === undefined) return [];
  return Array.isArray(value) ? value : [value];
}

/** Iterate NodeBox lists that may arrive as arrays, iterables or single values. */
export function toArray(value: unknown): unknown[] {
  if (value === null || value === undefined) return [];
  if (Array.isArray(value)) return value;
  if (typeof value === "string") return [value];
  if (typeof value === "object" && Symbol.iterator in (value as object)) return Array.from(value as Iterable<unknown>);
  return [value];
}
