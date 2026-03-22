import type { Point } from '../geometry/point.js';
import type { Color } from '../geometry/color.js';
import type { Path } from '../geometry/path.js';

export type Value =
  | { type: 'int'; value: number }
  | { type: 'float'; value: number }
  | { type: 'string'; value: string }
  | { type: 'boolean'; value: boolean }
  | { type: 'point'; value: Point }
  | { type: 'color'; value: Color }
  | { type: 'geometry'; value: Path[] }
  | { type: 'list'; value: Value[] }
  | { type: 'data'; value: Record<string, unknown> }
  | { type: 'null' };

export const NULL_VALUE: Value = { type: 'null' };

export function intValue(v: number): Value {
  return { type: 'int', value: Math.round(v) };
}

export function floatValue(v: number): Value {
  return { type: 'float', value: v };
}

export function stringValue(v: string): Value {
  return { type: 'string', value: v };
}

export function booleanValue(v: boolean): Value {
  return { type: 'boolean', value: v };
}

export function pointValue(x: number, y: number): Value {
  return { type: 'point', value: { x, y } };
}

export function colorValue(r: number, g: number, b: number, a = 1): Value {
  return { type: 'color', value: { r, g, b, a } };
}

export function geometryValue(paths: Path[]): Value {
  return { type: 'geometry', value: paths };
}

export function listValue(values: Value[]): Value {
  return { type: 'list', value: values };
}

export function dataValue(data: Record<string, unknown>): Value {
  return { type: 'data', value: data };
}

export function valueToString(v: Value): string {
  switch (v.type) {
    case 'int': return String(v.value);
    case 'float': return String(v.value);
    case 'string': return v.value;
    case 'boolean': return String(v.value);
    case 'point': return `${v.value.x},${v.value.y}`;
    case 'color': {
      const { r, g, b, a } = v.value;
      const toHex = (n: number) => Math.round(n * 255).toString(16).padStart(2, '0');
      return `#${toHex(r)}${toHex(g)}${toHex(b)}${toHex(a)}`;
    }
    case 'geometry': return `[Geometry: ${v.value.length} paths]`;
    case 'list': return `[List: ${v.value.length} items]`;
    case 'data': return JSON.stringify(v.value);
    case 'null': return '';
  }
}

export function unwrapValue(v: Value): unknown {
  if (v.type === 'null') return null;
  return v.value;
}
