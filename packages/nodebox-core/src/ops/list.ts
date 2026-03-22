import type { Value } from '../node/value.js';

// All list operations work with Value[] directly since list ports receive the full list.

export function combine(
  list1: Value[], list2: Value[], list3: Value[], list4: Value[],
  list5: Value[], list6: Value[], list7: Value[],
): Value[] {
  return [...list1, ...list2, ...list3, ...list4, ...list5, ...list6, ...list7];
}

export function count(list: Value[]): number {
  return list.length;
}

export function first(list: Value[]): Value | null {
  return list.length > 0 ? list[0] : null;
}

export function second(list: Value[]): Value | null {
  return list.length > 1 ? list[1] : null;
}

export function last(list: Value[]): Value | null {
  return list.length > 0 ? list[list.length - 1] : null;
}

export function rest(list: Value[]): Value[] {
  return list.slice(1);
}

export function slice(list: Value[], startIndex: number, size: number, invert: boolean): Value[] {
  const start = Math.max(0, Math.round(startIndex));
  const s = Math.max(0, Math.round(size));
  if (invert) {
    return [...list.slice(0, start), ...list.slice(start + s)];
  }
  return list.slice(start, start + s);
}

export function sortList(list: Value[], key: string): Value[] {
  const sorted = [...list];
  sorted.sort((a, b) => {
    const va = getValueForSort(a, key);
    const vb = getValueForSort(b, key);
    if (typeof va === 'number' && typeof vb === 'number') return va - vb;
    return String(va).localeCompare(String(vb));
  });
  return sorted;
}

function getValueForSort(v: Value, key: string): unknown {
  if (v.type === 'data' && key) return (v.value as Record<string, unknown>)[key];
  if (v.type === 'int' || v.type === 'float') return v.value;
  if (v.type === 'string') return v.value;
  return 0;
}

export function reverse(list: Value[]): Value[] {
  return [...list].reverse();
}

export function shuffle(list: Value[], seed: number): Value[] {
  const result = [...list];
  let state = seed;
  function rand(): number {
    state = (state * 1664525 + 1013904223) & 0xffffffff;
    return (state >>> 0) / 0xffffffff;
  }
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

export function shift(list: Value[], amount: number): Value[] {
  if (list.length === 0) return [];
  const n = ((amount % list.length) + list.length) % list.length;
  return [...list.slice(n), ...list.slice(0, n)];
}

export function repeat(list: Value[], amount: number, perItem: boolean): Value[] {
  const n = Math.max(0, Math.round(amount));
  if (perItem) {
    const result: Value[] = [];
    for (const item of list) {
      for (let i = 0; i < n; i++) result.push(item);
    }
    return result;
  }
  const result: Value[] = [];
  for (let i = 0; i < n; i++) result.push(...list);
  return result;
}

export function pick(list: Value[], amount: number, seed: number): Value[] {
  if (list.length === 0) return [];
  let state = seed;
  function rand(): number {
    state = (state * 1664525 + 1013904223) & 0xffffffff;
    return (state >>> 0) / 0xffffffff;
  }
  const n = Math.max(0, Math.round(amount));
  const result: Value[] = [];
  for (let i = 0; i < n; i++) {
    const idx = Math.floor(rand() * list.length);
    result.push(list[idx]);
  }
  return result;
}

export function takeEvery(list: Value[], n: number): Value[] {
  const step = Math.max(1, Math.round(n));
  return list.filter((_, i) => i % step === 0);
}

export function cull(list: Value[], booleans: Value[]): Value[] {
  return list.filter((_, i) => {
    const b = booleans[i % booleans.length];
    return b && (b.type === 'boolean' ? b.value : true);
  });
}

export function distinct(list: Value[], key: string): Value[] {
  const seen = new Set<string>();
  return list.filter(v => {
    const k = key && v.type === 'data'
      ? String((v.value as Record<string, unknown>)[key])
      : JSON.stringify(v);
    if (seen.has(k)) return false;
    seen.add(k);
    return true;
  });
}

export function switchList(inputs: Value[][], index: number): Value[] {
  const i = Math.max(0, Math.min(inputs.length - 1, Math.round(index)));
  return inputs[i] ?? [];
}

export function keys(list: Value[]): Value[] {
  const result: Value[] = [];
  for (const item of list) {
    if (item.type === 'data') {
      for (const k of Object.keys(item.value)) {
        result.push({ type: 'string', value: k });
      }
    }
  }
  return result;
}

export function zipMap(keyList: Value[], valueList: Value[]): Value {
  const map: Record<string, unknown> = {};
  for (let i = 0; i < keyList.length; i++) {
    const k = keyList[i]?.type === 'string' ? (keyList[i] as any).value : String(i);
    const v = i < valueList.length ? valueList[i] : null;
    map[k] = v;
  }
  return { type: 'data', value: map };
}
