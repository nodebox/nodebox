// Property lookup on arbitrary values, a port of nodebox.function.DataFunctions.lookup. NodeBox 3
// used reflection to read Java getters, which the community relies on to peek inside geometry
// (contours, pointCount, bounds.rectangle2D.maxX, …). The TypeScript classes expose the same names.

import { Rect } from "../graphics/rect";

export function lookup(o: unknown, key: string): unknown {
  if (o === null || o === undefined) return null;
  if (key.includes(".")) return nestedLookup(o, key.split("."));
  return simpleLookup(o, key);
}

export function nestedLookup(o: unknown, keys: string[]): unknown {
  let current = o;
  for (const key of keys) {
    current = simpleLookup(current, key);
    if (current === null || current === undefined) return null;
  }
  return current;
}

function simpleLookup(o: unknown, key: string): unknown {
  if (o === null || o === undefined) return null;
  if (o instanceof Map) return o.has(key) ? o.get(key) : null;
  if (Array.isArray(o)) {
    if (/^\d+$/.test(key)) return o[parseInt(key, 10)] ?? null;
    if (key === "size" || key === "length") return o.length;
    if (key === "empty") return o.length === 0;
    return null;
  }
  if (o instanceof Rect && key === "rectangle2D") return rectangle2D(o);
  if (typeof o === "object") {
    const obj = o as Record<string, unknown>;
    if (key in obj) {
      const value = obj[key];
      return typeof value === "function" ? null : value;
    }
    // Java-style getters: "pointCount" -> getPointCount(), "closed" -> isClosed().
    const getter = `get${key.charAt(0).toUpperCase()}${key.slice(1)}`;
    const isser = `is${key.charAt(0).toUpperCase()}${key.slice(1)}`;
    for (const name of [getter, isser, key]) {
      const fn = obj[name];
      if (typeof fn === "function" && fn.length === 0) {
        try {
          return (fn as () => unknown).call(o);
        } catch {
          return null;
        }
      }
    }
    return null;
  }
  return null;
}

/** The java.awt.geom.Rectangle2D view NodeBox 3 users reached through bounds.rectangle2D. */
function rectangle2D(r: Rect): Record<string, number | boolean> {
  const n = r.normalized();
  return {
    x: n.x,
    y: n.y,
    width: n.width,
    height: n.height,
    minX: n.x,
    minY: n.y,
    maxX: n.x + n.width,
    maxY: n.y + n.height,
    centerX: n.x + n.width / 2,
    centerY: n.y + n.height / 2,
    empty: n.width <= 0 || n.height <= 0,
  };
}
