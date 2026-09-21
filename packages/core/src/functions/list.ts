// A port of nodebox.function.ListFunctions (namespace "list").

import { JavaScriptLibrary } from "../runtime/function-repository";
import { lookup } from "../runtime/lookup";
import { toArray } from "../runtime/values";
import { JavaRandom } from "./java-random";

export function count(list: unknown): number {
  return toArray(list).length;
}

export function first(list: unknown): unknown {
  const l = toArray(list);
  return l.length > 0 ? l[0] : null;
}

export function second(list: unknown): unknown {
  const l = toArray(list);
  return l.length > 1 ? l[1] : null;
}

export function rest(list: unknown): unknown[] {
  return toArray(list).slice(1);
}

export function last(list: unknown): unknown {
  const l = toArray(list);
  return l.length > 0 ? l[l.length - 1] : null;
}

export function combine(...lists: unknown[]): unknown[] {
  const result: unknown[] = [];
  for (const list of lists) if (list !== null && list !== undefined) result.push(...toArray(list));
  return result;
}

export function slice(list: unknown, startIndex: number, size: number, invert: boolean): unknown[] {
  const l = toArray(list);
  startIndex = Math.max(0, Math.trunc(startIndex));
  size = Math.max(0, Math.trunc(size));
  if (!invert) return l.slice(startIndex, startIndex + size);
  return [...l.slice(0, startIndex), ...l.slice(startIndex + size)];
}

export function shift(list: unknown, amount: number): unknown[] {
  const l = toArray(list);
  if (l.length === 0) return [];
  let a = Math.trunc(amount) % l.length;
  if (a < 0) a += l.length;
  if (a === 0) return l.slice();
  return [...l.slice(a), ...l.slice(0, a)];
}

export function doSwitch(...args: unknown[]): unknown[] {
  // The last argument is the index; the ones before it are the lists (six in NodeBox 3).
  const index = Math.trunc(Number(args[args.length - 1]) || 0);
  const lists = args.slice(0, -1);
  const n = lists.length;
  if (n === 0) return [];
  let i = index % n;
  if (i < 0) i += n;
  const list = lists[i];
  return list === null || list === undefined ? [] : toArray(list).slice();
}

export function repeat(list: unknown, amount: number, perItem: boolean): unknown[] {
  const l = toArray(list);
  amount = Math.trunc(amount);
  if (amount < 1) return [];
  if (amount === 1) return l.slice();
  const result: unknown[] = [];
  if (perItem) {
    for (const o of l) for (let i = 0; i < amount; i++) result.push(o);
  } else {
    for (let i = 0; i < amount; i++) result.push(...l);
  }
  return result;
}

export function reverse(list: unknown): unknown[] {
  return toArray(list).slice().reverse();
}

function naturalCompare(a: unknown, b: unknown): number {
  if (a === null || a === undefined || b === null || b === undefined) {
    throw new Error("To sort a list, all elements in the list need to be comparable and of the same type.");
  }
  if (typeof a === "number" && typeof b === "number") return a - b;
  if (typeof a === "string" && typeof b === "string") return a < b ? -1 : a > b ? 1 : 0;
  if (typeof a === "boolean" && typeof b === "boolean") return a === b ? 0 : a ? 1 : -1;
  if (typeof a !== typeof b)
    throw new Error("To sort a list, all elements in the list need to be comparable and of the same type.");
  const sa = String(a);
  const sb = String(b);
  return sa < sb ? -1 : sa > sb ? 1 : 0;
}

export function sort(list: unknown, key: string): unknown[] {
  const l = toArray(list).slice();
  if (!key) {
    const firstItem = l[0];
    if (firstItem !== null && typeof firstItem === "object" && !Array.isArray(firstItem)) {
      const firstKey = firstItem instanceof Map ? firstItem.keys().next().value : Object.keys(firstItem)[0];
      if (typeof firstKey === "string") return sort(l, firstKey);
    }
    return l.sort(naturalCompare);
  }
  return l.sort((o1, o2) => {
    const c1 = lookup(o1, key);
    const c2 = lookup(o2, key);
    if (c1 === null || c2 === null) throw new Error(`Invalid key for this type of object: ${key}`);
    return naturalCompare(c1, c2);
  });
}

export function shuffle(list: unknown, seed: number): unknown[] {
  return JavaRandom.fromSeed(seed).shuffle(toArray(list).slice());
}

export function pick(list: unknown, amount: number, seed: number): unknown[] {
  if (amount <= 0) return [];
  const l = JavaRandom.fromSeed(seed).shuffle(toArray(list).slice());
  if (amount >= l.length) return l;
  return l.slice(0, Math.trunc(amount));
}

export function cull(list: unknown, booleans: unknown): unknown[] {
  const l = toArray(list);
  const b = toArray(booleans);
  if (b.length === 0) return l.slice();
  const result: unknown[] = [];
  l.forEach((o, i) => {
    if (b[i % b.length]) result.push(o);
  });
  return result;
}

function identityKey(v: unknown): string {
  if (v === null || v === undefined) return "null";
  if (typeof v === "object") {
    if (typeof (v as { toString?: unknown }).toString === "function" && (v as object).toString !== Object.prototype.toString)
      return `${(v as object).constructor.name}:${String(v)}`;
    return JSON.stringify(v);
  }
  return `${typeof v}:${String(v)}`;
}

export function distinct(list: unknown, key: string): unknown[] {
  const l = toArray(list);
  const k = key && key.trim() ? key : null;
  const seen = new Set<string>();
  const result: unknown[] = [];
  for (const o of l) {
    if (o === null || o === undefined) continue;
    const v = k === null ? o : lookup(o, k);
    const id = v === null ? null : identityKey(v);
    if (id !== null && seen.has(id)) continue;
    if (id !== null) seen.add(id);
    result.push(o);
  }
  return result;
}

export function takeEvery(list: unknown, n: number): unknown[] {
  const l = toArray(list);
  n = Math.trunc(n);
  if (n === 0) throw new Error("Divider cannot be zero.");
  return l.filter((_, i) => i % n === 0);
}

export function keys(list: unknown): string[] {
  const result = new Set<string>();
  for (const o of toArray(list)) {
    if (o instanceof Map) for (const k of o.keys()) result.add(String(k));
    else if (o !== null && o !== undefined) for (const k of javaProperties(o)) result.add(k);
  }
  return Array.from(result);
}

// Java's keys() lists the bean properties of anything that isn't a map (java.beans.Introspector on
// a JDK 11). Third-party networks probe them ("is this a map with two keys?"), so mirror the lists.
const JAVA_PROPERTIES: Record<string, string[]> = {
  string: ["blank", "bytes", "class", "empty"],
  float: ["class", "infinite", "naN"],
  int: ["class"],
  boolean: ["class"],
  Point: ["class", "curveData", "curveTo", "lineTo", "offCurve", "onCurve", "type", "x", "y"],
  Path: ["bounds", "class", "closed", "contours", "empty", "fill", "fillColor", "generalPath", "length", "pointCount",
    "points", "stroke", "strokeColor", "strokeWidth", "transformDelegate"],
  Geometry: ["bounds", "class", "closed", "empty", "fill", "fillColor", "length", "paths", "pointCount", "points",
    "stroke", "strokeColor", "strokeWidth", "transformDelegate"],
  Contour: ["bounds", "class", "closed", "empty", "length", "pointCount", "points", "transformDelegate"],
  Color: ["a", "alpha", "awtColor", "b", "blue", "brightness", "class", "g", "green", "h", "hue", "r", "red", "s",
    "saturation", "v", "visible"],
  Text: ["align", "baseLineX", "baseLineY", "bounds", "class", "empty", "fillColor", "font", "fontName", "fontSize",
    "height", "lineHeight", "metrics", "path", "text", "transform", "transformDelegate", "width"],
  Rect: ["centroid", "class", "empty", "height", "position", "rectangle2D", "width", "x", "y"],
};

function javaProperties(o: unknown): string[] {
  if (typeof o === "string") return JAVA_PROPERTIES.string;
  // Numbers are doubles unless they came from an int port, which the value alone cannot tell;
  // NodeBox numbers are floats far more often than not.
  if (typeof o === "number") return JAVA_PROPERTIES.float;
  if (typeof o === "boolean") return JAVA_PROPERTIES.boolean;
  if (typeof o === "object") {
    if (Array.isArray(o)) return ["class", "empty"];
    const name = (o as object).constructor?.name ?? "Object";
    if (name === "Object") return Object.keys(o as object);
    return JAVA_PROPERTIES[name] ?? ["class"];
  }
  return [];
}

export function zipMap(keyList: unknown, valueList: unknown): Record<string, unknown> {
  const ks = toArray(keyList);
  const vs = toArray(valueList);
  const result: Record<string, unknown> = {};
  const n = Math.min(ks.length, vs.length);
  for (let i = 0; i < n; i++) result[String(ks[i])] = vs[i];
  return result;
}

export const listLibrary = new JavaScriptLibrary("list", {
  count,
  first,
  second,
  rest,
  last,
  combine,
  slice,
  shift,
  doSwitch,
  distinct,
  repeat,
  reverse,
  sort,
  shuffle,
  pick,
  cull,
  takeEvery,
  keys,
  zipMap,
});
