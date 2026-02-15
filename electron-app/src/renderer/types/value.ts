// TypeScript types mirroring crates/nodebox-core/src/value.rs

import type { Point, Color, Path } from './geometry';

export type Value =
  | { type: 'null' }
  | { type: 'int'; value: number }
  | { type: 'float'; value: number }
  | { type: 'string'; value: string }
  | { type: 'boolean'; value: boolean }
  | { type: 'point'; value: Point }
  | { type: 'color'; value: Color }
  | { type: 'path'; value: Path }
  | { type: 'list'; value: Value[] }
  | { type: 'map'; value: Record<string, Value> };

export function nullValue(): Value {
  return { type: 'null' };
}

export function intValue(value: number): Value {
  return { type: 'int', value };
}

export function floatValue(value: number): Value {
  return { type: 'float', value };
}

export function stringValue(value: string): Value {
  return { type: 'string', value };
}

export function booleanValue(value: boolean): Value {
  return { type: 'boolean', value };
}

export function valueToString(v: Value): string {
  switch (v.type) {
    case 'null':
      return 'null';
    case 'int':
    case 'float':
      return String(v.value);
    case 'string':
      return v.value;
    case 'boolean':
      return String(v.value);
    case 'point':
      return `(${v.value.x}, ${v.value.y})`;
    case 'color':
      return `rgba(${v.value.r}, ${v.value.g}, ${v.value.b}, ${v.value.a})`;
    case 'path':
      return '[Path]';
    case 'list':
      return `[List: ${v.value.length} items]`;
    case 'map':
      return `[Map: ${Object.keys(v.value).length} keys]`;
  }
}
