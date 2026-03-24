import type { Point } from '../geometry/point.js';

// ─── Basic math ────────────────────────────────────────
export function abs(v: number): number { return Math.abs(v); }
export function ceil(v: number): number { return Math.ceil(v); }
export function floor(v: number): number { return Math.floor(v); }
export function round(v: number): number { return Math.round(v); }
export function negate(v: number): number { return -v; }

// ─── Arithmetic ────────────────────────────────────────
export function add(v1: number, v2: number): number { return v1 + v2; }
export function subtract(v1: number, v2: number): number { return v1 - v2; }
export function multiply(v1: number, v2: number): number { return v1 * v2; }
export function divide(v1: number, v2: number): number { return v2 === 0 ? 0 : v1 / v2; }
export function mod(v1: number, v2: number): number { return v2 === 0 ? 0 : v1 % v2; }

// ─── Trigonometry (radians — matching Java's Math.sin/Math.cos) ──
export function sin(v: number): number { return Math.sin(v); }
export function cos(v: number): number { return Math.cos(v); }
export function sqrt(v: number): number { return Math.sqrt(v); }
export function pow(base: number, exponent: number): number { return Math.pow(base, exponent); }
export function log(v: number): number { return v <= 0 ? 0 : Math.log(v); }

// ─── Conversion ────────────────────────────────────────
export function radians(degrees: number): number { return degrees * Math.PI / 180; }
export function degrees(radians: number): number { return radians * 180 / Math.PI; }

// ─── Constants ─────────────────────────────────────────
export function pi(): number { return Math.PI; }
export function e(): number { return Math.E; }

// ─── Predicates ────────────────────────────────────────
export function even(v: number): boolean { return Math.round(v) % 2 === 0; }
export function odd(v: number): boolean { return Math.round(v) % 2 !== 0; }

// ─── Aggregates (take lists) ───────────────────────────
export function min(values: number[]): number {
  if (values.length === 0) return 0;
  return Math.min(...values);
}

export function max(values: number[]): number {
  if (values.length === 0) return 0;
  return Math.max(...values);
}

export function average(values: number[]): number {
  if (values.length === 0) return 0;
  return values.reduce((a, b) => a + b, 0) / values.length;
}

export function sum(values: number[]): number {
  return values.reduce((a, b) => a + b, 0);
}

// ─── Constructors ──────────────────────────────────────
export function number(v: number): number { return v; }
export function integer(v: number): number { return Math.round(v); }
export function boolean(v: boolean): boolean { return v; }

// ─── Range/Sample ──────────────────────────────────────
export function range(start: number, end: number, step: number): number[] {
  if (step === 0) return [];
  const result: number[] = [];
  if (step > 0) {
    for (let v = start; v < end; v += step) result.push(v);
  } else {
    for (let v = start; v > end; v += step) result.push(v);
  }
  return result;
}

export function sample(amount: number, start: number, end: number): number[] {
  const n = Math.max(1, Math.round(amount));
  const result: number[] = [];
  for (let i = 0; i < n; i++) {
    const t = n <= 1 ? 0.5 : i / (n - 1);
    result.push(start + (end - start) * t);
  }
  return result;
}

export function randomNumbers(amount: number, start: number, end: number, seed: number): number[] {
  let state = seed;
  function rand(): number {
    state = (state * 1664525 + 1013904223) & 0xffffffff;
    return (state >>> 0) / 0xffffffff;
  }
  const n = Math.max(0, Math.round(amount));
  const result: number[] = [];
  for (let i = 0; i < n; i++) {
    result.push(start + rand() * (end - start));
  }
  return result;
}

// ─── Convert range ─────────────────────────────────────
export function convertRange(
  value: number, srcStart: number, srcEnd: number,
  tgtStart: number, tgtEnd: number, method: string,
): number {
  if (srcEnd === srcStart) return tgtStart;
  let t = (value - srcStart) / (srcEnd - srcStart);
  if (method === 'clamp') t = Math.max(0, Math.min(1, t));
  return tgtStart + t * (tgtEnd - tgtStart);
}

// ─── Point math ────────────────────────────────────────
export function coordinates(position: Point, angle: number, distance: number): Point {
  const rad = angle * Math.PI / 180;
  return {
    x: position.x + Math.cos(rad) * distance,
    y: position.y + Math.sin(rad) * distance,
  };
}

export function angle(point1: Point, point2: Point): number {
  return Math.atan2(point2.y - point1.y, point2.x - point1.x) * 180 / Math.PI;
}

export function distance(point1: Point, point2: Point): number {
  const dx = point2.x - point1.x;
  const dy = point2.y - point1.y;
  return Math.sqrt(dx * dx + dy * dy);
}

export function reflectPoint(point: Point, position: Point, angle: number): Point {
  const rad = angle * Math.PI / 180;
  const cos2 = Math.cos(2 * rad);
  const sin2 = Math.sin(2 * rad);
  const dx = point.x - position.x;
  const dy = point.y - position.y;
  return {
    x: position.x + dx * cos2 + dy * sin2,
    y: position.y + dx * sin2 - dy * cos2,
  };
}

// ─── Miscellaneous ─────────────────────────────────────
export function makeNumbers(str: string, separator: string): number[] {
  return str.split(separator).map(s => parseFloat(s.trim())).filter(n => !isNaN(n));
}

export function runningTotal(values: number[]): number[] {
  const result: number[] = [];
  let total = 0;
  for (const v of values) {
    total += v;
    result.push(total);
  }
  return result;
}

export function wave(
  minVal: number, maxVal: number, period: number, offset: number, type: string, frame: number,
): number {
  const t = ((frame + offset) / period) * Math.PI * 2;
  let v: number;
  if (type === 'sine') v = Math.sin(t);
  else if (type === 'square') v = Math.sin(t) >= 0 ? 1 : -1;
  else if (type === 'triangle') v = 2 * Math.abs(2 * ((frame + offset) / period - Math.floor((frame + offset) / period + 0.5))) - 1;
  else if (type === 'sawtooth') v = 2 * ((frame + offset) / period - Math.floor((frame + offset) / period + 0.5));
  else v = Math.sin(t);

  return minVal + (v + 1) / 2 * (maxVal - minVal);
}

export function compare(value1: number, value2: number, comparator: string): boolean {
  if (comparator === '==' || comparator === 'equal') return value1 === value2;
  if (comparator === '!=' || comparator === 'not-equal') return value1 !== value2;
  if (comparator === '<' || comparator === 'less-than') return value1 < value2;
  if (comparator === '>' || comparator === 'greater-than') return value1 > value2;
  if (comparator === '<=' || comparator === 'less-or-equal') return value1 <= value2;
  if (comparator === '>=' || comparator === 'greater-or-equal') return value1 >= value2;
  return false;
}

export function logicOperator(bool1: boolean, bool2: boolean, op: string): boolean {
  if (op === 'and') return bool1 && bool2;
  if (op === 'or') return bool1 || bool2;
  if (op === 'not') return !bool1;
  if (op === 'xor') return bool1 !== bool2;
  return false;
}
