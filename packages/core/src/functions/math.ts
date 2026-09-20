// A port of nodebox.function.MathFunctions (namespace "math").

import { Point } from "../graphics/point";
import { angle as geoAngle, coordinates as geoCoordinates, distance as geoDistance, degrees, radians } from "../graphics/math";
import { JavaScriptLibrary } from "../runtime/function-repository";
import { toArray } from "../runtime/values";
import { JavaRandom } from "./java-random";

export const OVERFLOW_WRAP = "wrap";
export const OVERFLOW_MIRROR = "mirror";
export const OVERFLOW_CLAMP = "clamp";
export const OVERFLOW_IGNORE = "ignore";
export const WAVE_SINE = "sine";
export const WAVE_SQUARE = "square";
export const WAVE_TRIANGLE = "triangle";
export const WAVE_SAWTOOTH = "sawtooth";

function num(v: unknown): number {
  if (typeof v === "number") return v;
  if (typeof v === "boolean") return v ? 1 : 0;
  if (typeof v === "string") return Number(v) || 0;
  return 0;
}

function numbers(v: unknown): number[] {
  return toArray(v).map(num);
}

export function number(n: number): number {
  return n;
}

export function integer(value: number): number {
  return Math.round(value);
}

export function makeBoolean(value: boolean): boolean {
  return value;
}

export function add(n1: number, n2: number): number {
  return n1 + n2;
}

export function subtract(n1: number, n2: number): number {
  return n1 - n2;
}

export function multiply(n1: number, n2: number): number {
  return n1 * n2;
}

export function divide(n1: number, n2: number): number {
  if (n2 === 0) throw new Error("Divider cannot be zero.");
  return n1 / n2;
}

export function mod(n1: number, n2: number): number {
  if (n2 === 0) throw new Error("Divider cannot be zero.");
  // Java's % keeps the sign of the dividend, as JavaScript does.
  return n1 % n2;
}

export function sqrt(n: number): number {
  return Math.sqrt(n);
}

export function pow(n1: number, n2: number): number {
  return Math.pow(n1, n2);
}

export function log(n: number): number {
  if (n === 0) throw new Error("Value cannot be zero.");
  return Math.log(n);
}

export function even(n: number): boolean {
  return n % 2 === 0;
}

export function odd(n: number): boolean {
  return n % 2 !== 0;
}

export function negate(n: number): number {
  return -n;
}

export function abs(n: number): number {
  return Math.abs(n);
}

export function sum(values: unknown): number {
  let total = 0;
  for (const d of numbers(values)) total += d;
  return total;
}

export function average(values: unknown): number {
  const list = numbers(values);
  if (list.length === 0) return 0;
  return sum(list) / list.length;
}

export function max(values: unknown): number {
  const list = numbers(values);
  if (list.length === 0) return 0;
  return Math.max(...list);
}

export function min(values: unknown): number {
  const list = numbers(values);
  if (list.length === 0) return 0;
  return Math.min(...list);
}

export function ceil(n: number): number {
  return Math.ceil(n);
}

export function floor(n: number): number {
  return Math.floor(n);
}

function compareValues(o1: unknown, o2: unknown): number {
  if (typeof o1 === "number" && typeof o2 === "number") return o1 < o2 ? -1 : o1 > o2 ? 1 : 0;
  if (typeof o1 === "string" && typeof o2 === "string") return o1 < o2 ? -1 : o1 > o2 ? 1 : 0;
  if (typeof o1 === "boolean" && typeof o2 === "boolean") return o1 === o2 ? 0 : o1 ? 1 : -1;
  // Mixed types: Java would throw a ClassCastException; compare numerically when both parse.
  const n1 = Number(o1);
  const n2 = Number(o2);
  if (!Number.isNaN(n1) && !Number.isNaN(n2)) return n1 < n2 ? -1 : n1 > n2 ? 1 : 0;
  const s1 = String(o1);
  const s2 = String(o2);
  return s1 < s2 ? -1 : s1 > s2 ? 1 : 0;
}

export function compare(o1: unknown, o2: unknown, comparator: string): boolean {
  const comparison = compareValues(o1, o2);
  switch (comparator) {
    case "<":
      return comparison < 0;
    case ">":
      return comparison > 0;
    case "<=":
      return comparison <= 0;
    case ">=":
      return comparison >= 0;
    case "==":
      return comparison === 0;
    case "!=":
      return comparison !== 0;
    default:
      throw new Error(`unknown comparison operation ${comparator}`);
  }
}

export function logicOperator(b1: boolean, b2: boolean, comparator: string): boolean {
  switch (comparator) {
    case "or":
      return b1 || b2;
    case "and":
      return b1 && b2;
    case "xor":
      return b1 !== b2;
    default:
      throw new Error("unknown logical operation ");
  }
}

export function makeNumbers(s: string, separator: string): number[] {
  if (!s) return [];
  const parts = !separator ? s.split("") : s.split(separator);
  return parts.map((part) => {
    const v = Number(part.trim());
    if (part.trim() === "" || Number.isNaN(v)) throw new Error(`For input string: "${part}"`);
    return v;
  });
}

export function randomNumbers(amount: number, start: number, end: number, seed: number): number[] {
  const r = JavaRandom.fromSeed(seed);
  const result: number[] = [];
  for (let i = 0; i < amount; i++) result.push(start + r.nextDouble() * (end - start));
  return result;
}

export function round(a: number): number {
  return Math.round(a);
}

export function sample(amount: number, start: number, end: number): number[] {
  amount = Math.trunc(amount);
  if (amount === 0) return [];
  if (amount === 1) return [start + (end - start) / 2];
  const step = (end - start) / (amount - 1);
  const result: number[] = [];
  for (let i = 0; i < amount; i++) result.push(start + step * i);
  return result;
}

export function range(start: number, end: number, step: number): number[] {
  if (step === 0 || start === end || (start < end && step < 0) || (start > end && step > 0)) return [];
  const result: number[] = [];
  if (step > 0) for (let v = start; v < end; v += step) result.push(v);
  else for (let v = start; v > end; v += step) result.push(v);
  return result;
}

export function runningTotal(values: unknown): number[] {
  const list = numbers(values);
  if (list.length === 0) return [0.0];
  let total = 0;
  const result: number[] = [];
  for (const d of list) {
    result.push(total);
    total += d;
  }
  return result;
}

export { radians, degrees };

export function angle(p1: Point, p2: Point): number {
  return geoAngle(p1.x, p1.y, p2.x, p2.y);
}

export function distance(p1: Point, p2: Point): number {
  return geoDistance(p1.x, p1.y, p2.x, p2.y);
}

export function coordinates(p: Point, angle: number, distance: number): Point {
  const [x, y] = geoCoordinates(p.x, p.y, distance, angle);
  return new Point(x, y);
}

export function reflect(p1: Point, p2: Point, angle: number, distance: number): Point {
  distance *= geoDistance(p1.x, p1.y, p2.x, p2.y);
  angle += geoAngle(p1.x, p1.y, p2.x, p2.y);
  return coordinates(p1, angle, distance);
}

export function sin(n: number): number {
  return Math.sin(n);
}

export function cos(n: number): number {
  return Math.cos(n);
}

export function pi(): number {
  return Math.PI;
}

export function e(): number {
  return Math.E;
}

function clamp(v: number, lo: number, hi: number): number {
  return v < lo ? lo : v > hi ? hi : v;
}

export function convertRange(
  value: number,
  srcMin: number,
  srcMax: number,
  targetMin: number,
  targetMax: number,
  overflowMethod: string,
): number {
  if (overflowMethod === OVERFLOW_WRAP) {
    value = srcMin + (value % (srcMax - srcMin));
  } else if (overflowMethod === OVERFLOW_MIRROR) {
    const rest = value % (srcMax - srcMin);
    if (Math.trunc(value / (srcMax - srcMin)) % 2 === 1) value = srcMax - rest;
    else value = srcMin + rest;
  } else if (overflowMethod === OVERFLOW_CLAMP) {
    value = clamp(value, srcMin, srcMax);
  }
  value = (value - srcMin) / (srcMax - srcMin);
  return targetMin + value * (targetMax - targetMin);
}

// The waves are computed in single precision, as nodebox.util.waves does.
const f = Math.fround;
const TWO_PI = f(2 * f(3.14159265358979323846));

export function wave(minValue: number, maxValue: number, period: number, offset: number, waveType: string): number {
  const fmin = f(minValue);
  const fmax = f(maxValue);
  const fperiod = f(period);
  const amplitude = f((fmax - fmin) / 2);
  const waveOffset = f(fmin + amplitude);
  const frequency = f(TWO_PI / fperiod);
  const adjustedTime = waveType === WAVE_TRIANGLE ? f(f(offset) + fperiod / 4) : f(f(offset) + fperiod / 2);
  let phase: number;
  if (f(adjustedTime % fperiod) === 0) phase = 0;
  else phase = f(f(adjustedTime * frequency) % TWO_PI);
  if (phase < 0) phase = f(phase + TWO_PI);
  let value: number;
  switch (waveType) {
    case WAVE_TRIANGLE:
      value = f(f(Math.abs(f(f(phase / TWO_PI) * 2 - 1)) * amplitude * 2) - amplitude);
      break;
    case WAVE_SQUARE:
      value = f((f(phase / TWO_PI) < 0.5 ? 1 : -1) * amplitude);
      break;
    case WAVE_SAWTOOTH:
      value = f(f(f(phase / TWO_PI) * 2 - 1) * -amplitude);
      break;
    default:
      value = f(f(Math.sin(phase)) * amplitude);
  }
  return f(value + waveOffset);
}

export const mathLibrary = new JavaScriptLibrary("math", {
  number,
  integer,
  makeBoolean,
  negate,
  abs,
  add,
  subtract,
  multiply,
  divide,
  mod,
  sqrt,
  pow,
  log,
  sum,
  average,
  compare,
  logicOperator,
  min,
  max,
  ceil,
  floor,
  runningTotal,
  even,
  odd,
  makeNumbers,
  randomNumbers,
  round,
  sample,
  range,
  radians,
  degrees,
  angle,
  distance,
  coordinates,
  reflect,
  sin,
  cos,
  pi,
  e,
  convertRange,
  wave,
});
