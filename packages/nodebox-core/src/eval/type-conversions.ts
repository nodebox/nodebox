import type { Value } from '../node/value.js';
import type { PortType } from '../node/port.js';
import type { Point } from '../geometry/point.js';
import type { Color } from '../geometry/color.js';
import { colorFromHex, colorToHex } from '../geometry/color.js';
import { getAllPoints } from '../geometry/path.js';

export function convert(value: Value, targetType: PortType): Value {
  if (value.type === 'null') return value;
  if (value.type === targetType) return value;

  // List and data accept anything
  if (targetType === 'list') return value;
  if (targetType === 'data') return value;

  // Everything -> string
  if (targetType === 'string') return toStringValue(value);

  // int conversions
  if (targetType === 'int') return toIntValue(value);
  if (targetType === 'float') return toFloatValue(value);
  if (targetType === 'boolean') return toBooleanValue(value);
  if (targetType === 'point') return toPointValue(value);
  if (targetType === 'color') return toColorValue(value);

  // geometry accepts points
  if (targetType === 'geometry' && value.type === 'point') {
    // Single point is passed through — the evaluator wraps it later
    return value;
  }

  // Pass through if no conversion exists
  return value;
}

function toStringValue(v: Value): Value {
  switch (v.type) {
    case 'int': return { type: 'string', value: String(v.value) };
    case 'float': return { type: 'string', value: String(v.value) };
    case 'boolean': return { type: 'string', value: String(v.value) };
    case 'point': return { type: 'string', value: `${v.value.x},${v.value.y}` };
    case 'color': return { type: 'string', value: colorToHex(v.value) };
    case 'geometry': return { type: 'string', value: `[Geometry: ${v.value.length} paths]` };
    case 'list': return { type: 'string', value: `[List: ${v.value.length} items]` };
    case 'data': return { type: 'string', value: JSON.stringify(v.value) };
    default: return { type: 'string', value: '' };
  }
}

function toIntValue(v: Value): Value {
  switch (v.type) {
    case 'float': return { type: 'int', value: Math.round(v.value) };
    case 'boolean': return { type: 'int', value: v.value ? 1 : 0 };
    case 'string': {
      const n = parseInt(v.value, 10);
      return { type: 'int', value: isNaN(n) ? 0 : n };
    }
    default: return v;
  }
}

function toFloatValue(v: Value): Value {
  switch (v.type) {
    case 'int': return { type: 'float', value: v.value };
    case 'boolean': return { type: 'float', value: v.value ? 1.0 : 0.0 };
    case 'string': {
      const n = parseFloat(v.value);
      return { type: 'float', value: isNaN(n) ? 0.0 : n };
    }
    default: return v;
  }
}

function toBooleanValue(v: Value): Value {
  switch (v.type) {
    case 'int': return { type: 'boolean', value: v.value > 0 };
    case 'float': return { type: 'boolean', value: v.value > 0 };
    case 'string': return { type: 'boolean', value: v.value === 'true' };
    default: return v;
  }
}

function toPointValue(v: Value): Value {
  switch (v.type) {
    case 'int': return { type: 'point', value: { x: v.value, y: v.value } };
    case 'float': return { type: 'point', value: { x: v.value, y: v.value } };
    case 'string': {
      const parts = v.value.split(',');
      if (parts.length === 2) {
        const x = parseFloat(parts[0]);
        const y = parseFloat(parts[1]);
        if (!isNaN(x) && !isNaN(y)) return { type: 'point', value: { x, y } };
      }
      return v;
    }
    default: return v;
  }
}

function toColorValue(v: Value): Value {
  switch (v.type) {
    case 'int': {
      const g = Math.max(0, Math.min(1, v.value / 255));
      return { type: 'color', value: { r: g, g: g, b: g, a: 1 } };
    }
    case 'float': {
      const g = Math.max(0, Math.min(1, v.value / 255));
      return { type: 'color', value: { r: g, g: g, b: g, a: 1 } };
    }
    case 'boolean': {
      const g = v.value ? 1 : 0;
      return { type: 'color', value: { r: g, g: g, b: g, a: 1 } };
    }
    case 'string': {
      try {
        return { type: 'color', value: colorFromHex(v.value) };
      } catch {
        return v;
      }
    }
    default: return v;
  }
}

export function clampValue(value: Value, min: number | null, max: number | null): Value {
  if (min === null && max === null) return value;
  if (value.type === 'int' || value.type === 'float') {
    let v = value.value;
    if (min !== null) v = Math.max(min, v);
    if (max !== null) v = Math.min(max, v);
    return { ...value, value: v } as Value;
  }
  return value;
}
