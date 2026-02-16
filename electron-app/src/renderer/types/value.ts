// TypeScript types mirroring crates/nodebox-core/src/value.rs
// Uses Rust serde externally-tagged enum format.

import type { Point, Color, Path } from './geometry';

export type Value =
  | 'Null'
  | { Int: number }
  | { Float: number }
  | { String: string }
  | { Boolean: boolean }
  | { Point: Point }
  | { Color: Color }
  | { Path: Path }
  | { List: Value[] }
  | { Map: Record<string, Value> };

export function nullValue(): Value {
  return 'Null';
}

export function intValue(value: number): Value {
  return { Int: value };
}

export function floatValue(value: number): Value {
  return { Float: value };
}

export function stringValue(value: string): Value {
  return { String: value };
}

export function booleanValue(value: boolean): Value {
  return { Boolean: value };
}

export function pointValue(value: Point): Value {
  return { Point: value };
}

export function colorValue(value: Color): Value {
  return { Color: value };
}

// Helpers to extract values from the externally-tagged format
export function getFloat(v: Value): number {
  if (typeof v === 'object' && 'Float' in v) return v.Float;
  if (typeof v === 'object' && 'Int' in v) return v.Int;
  return 0;
}

export function getInt(v: Value): number {
  if (typeof v === 'object' && 'Int' in v) return v.Int;
  if (typeof v === 'object' && 'Float' in v) return v.Float;
  return 0;
}

export function getPoint(v: Value): Point {
  if (typeof v === 'object' && 'Point' in v) return v.Point;
  return { x: 0, y: 0 };
}

export function getColor(v: Value): Color {
  if (typeof v === 'object' && 'Color' in v) return v.Color;
  return { r: 0, g: 0, b: 0, a: 1 };
}

export function getString(v: Value): string {
  if (typeof v === 'object' && 'String' in v) return v.String;
  return '';
}

export function getBoolean(v: Value): boolean {
  if (typeof v === 'object' && 'Boolean' in v) return v.Boolean;
  return false;
}

export function getPath(v: Value): Path | null {
  if (typeof v === 'object' && 'Path' in v) return v.Path;
  return null;
}

export function isNull(v: Value): boolean {
  return v === 'Null';
}

export function isFloat(v: Value): boolean {
  return typeof v === 'object' && 'Float' in v;
}

export function isInt(v: Value): boolean {
  return typeof v === 'object' && 'Int' in v;
}

export function isPoint(v: Value): boolean {
  return typeof v === 'object' && 'Point' in v;
}

export function isColor(v: Value): boolean {
  return typeof v === 'object' && 'Color' in v;
}

export function isString(v: Value): boolean {
  return typeof v === 'object' && 'String' in v;
}

export function isBoolean(v: Value): boolean {
  return typeof v === 'object' && 'Boolean' in v;
}

export function isPath(v: Value): boolean {
  return typeof v === 'object' && 'Path' in v;
}

export function isList(v: Value): boolean {
  return typeof v === 'object' && 'List' in v;
}

export function valueToString(v: Value): string {
  if (v === 'Null') return 'null';
  if (typeof v === 'object') {
    if ('Int' in v) return String(v.Int);
    if ('Float' in v) return String(v.Float);
    if ('String' in v) return v.String;
    if ('Boolean' in v) return String(v.Boolean);
    if ('Point' in v) return `(${v.Point.x}, ${v.Point.y})`;
    if ('Color' in v) return `rgba(${v.Color.r}, ${v.Color.g}, ${v.Color.b}, ${v.Color.a})`;
    if ('Path' in v) return '[Path]';
    if ('List' in v) return `[List: ${v.List.length} items]`;
    if ('Map' in v) return `[Map: ${Object.keys(v.Map).length} keys]`;
  }
  return '';
}
