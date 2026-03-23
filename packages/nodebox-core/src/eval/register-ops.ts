import type { Value } from '../node/value.js';
import type { PortRange } from '../node/port.js';
import type { Point } from '../geometry/point.js';
import type { Color } from '../geometry/color.js';
import type { Path } from '../geometry/path.js';
import { createFunctionRegistry, type FunctionRegistry } from './function-registry.js';
import * as gen from '../ops/generators.js';
import * as flt from '../ops/filters.js';
import * as math from '../ops/math.js';
import * as lst from '../ops/list.js';
import * as clr from '../ops/color.js';
import * as str from '../ops/string.js';
import * as data from '../ops/data.js';
import * as net from '../ops/network.js';

export function unwrapValue(v: Value): unknown {
  if (v.type === 'null') return null;
  // Geometry values are Path[] — unwrap single-element arrays to a single Path
  // since most ops functions expect Path, not Path[]
  if (v.type === 'geometry' && Array.isArray(v.value) && v.value.length === 1) {
    return v.value[0];
  }
  return v.value;
}

export function wrapResult(result: unknown, outputType: string, _outputRange: PortRange = 'value'): Value {
  if (result === null || result === undefined) return { type: 'null' };
  if (outputType === 'geometry') {
    const paths = Array.isArray(result) ? result : [result];
    return { type: 'geometry', value: paths };
  }
  if (outputType === 'point') return { type: 'point', value: result as any };
  if (outputType === 'float') return { type: 'float', value: result as number };
  if (outputType === 'int') return { type: 'int', value: result as number };
  if (outputType === 'boolean') return { type: 'boolean', value: result as boolean };
  if (outputType === 'string') return { type: 'string', value: result as string };
  if (outputType === 'color') return { type: 'color', value: result as any };
  if (outputType === 'data') return { type: 'data', value: result as Record<string, unknown> };
  if (outputType === 'list') {
    if (Array.isArray(result)) return { type: 'list', value: result };
    return { type: 'list', value: [{ type: 'data', value: result as any }] };
  }
  return { type: 'null' };
}

export function createDefaultRegistry(): FunctionRegistry {
  const registry = createFunctionRegistry();

  // Core functions
  registry.register('core/zero', () => ({ type: 'float', value: 0.0 }));
  registry.register('core/frame', (ctx: Value) => {
    if (ctx.type === 'data') {
      const data = ctx.value as Record<string, unknown>;
      const frame = typeof data.frame === 'number' ? data.frame : 1;
      return { type: 'float', value: frame };
    }
    return { type: 'float', value: 1.0 };
  });

  // ─── Generators ─────────────────────────────────────
  registerOp(registry, 'corevector/rect', gen.rect, 'geometry');
  registerOp(registry, 'corevector/ellipse', gen.ellipse, 'geometry');
  registerOp(registry, 'corevector/arc', gen.arc, 'geometry');
  registerOp(registry, 'corevector/line', gen.line, 'geometry');
  registerOp(registry, 'corevector/lineAngle', gen.lineAngle, 'geometry');
  registerOp(registry, 'corevector/polygon', gen.polygon, 'geometry');
  registerOp(registry, 'corevector/star', gen.star, 'geometry');
  registerOp(registry, 'corevector/grid', gen.grid, 'point', 'list');
  registerOp(registry, 'corevector/connect', gen.connect, 'geometry');
  registerOp(registry, 'corevector/quadCurve', gen.quadCurve, 'geometry');
  registerOp(registry, 'corevector/link', gen.link, 'geometry');
  registerOp(registry, 'corevector/makePoint', gen.makePoint, 'point');
  registerOp(registry, 'corevector/freehand', gen.freehand, 'geometry');
  registerOp(registry, 'corevector/group', gen.group, 'geometry');
  registerOp(registry, 'corevector/doNothing', gen.doNothing, 'geometry');

  // ─── Filters ───────────────────────────────────────
  registerOp(registry, 'corevector/align', flt.align, 'geometry');
  registerOp(registry, 'corevector/colorize', flt.colorize, 'geometry');
  registerOp(registry, 'corevector/copy', flt.copy, 'geometry', 'list');
  registerOp(registry, 'corevector/fit', flt.fit, 'geometry');
  registerOp(registry, 'corevector/fitTo', flt.fitTo, 'geometry');
  registerOp(registry, 'corevector/translate', flt.translateOp, 'geometry');
  registerOp(registry, 'corevector/rotate', flt.rotateOp, 'geometry');
  registerOp(registry, 'corevector/scale', flt.scaleOp, 'geometry');
  registerOp(registry, 'corevector/skew', flt.skewOp, 'geometry');
  registerOp(registry, 'corevector/reflect', flt.reflect, 'geometry');
  registerOp(registry, 'corevector/snap', flt.snap, 'geometry');
  registerOp(registry, 'corevector/resample', flt.resample, 'geometry');
  registerOp(registry, 'corevector/wiggle', flt.wiggle, 'geometry', 'list');
  registerOp(registry, 'corevector/ungroup', flt.ungroup, 'geometry', 'list');
  registerOp(registry, 'corevector/scatter', flt.scatter, 'point', 'list');
  registerOp(registry, 'corevector/centroid', flt.centroid, 'point');
  registerOp(registry, 'corevector/pointOnPath', flt.pointOnPathOp, 'point');
  registerOp(registry, 'corevector/sort', flt.sortShapes, 'geometry', 'list');
  registerOp(registry, 'corevector/stack', flt.stack, 'geometry', 'list');
  registerOp(registry, 'corevector/distribute', flt.distribute, 'geometry', 'list');
  registerOp(registry, 'corevector/delete', flt.deletePaths, 'geometry');
  registerOp(registry, 'corevector/roundSegments', flt.roundSegments, 'geometry');
  registerOp(registry, 'corevector/compound', flt.compound, 'geometry');
  registerOp(registry, 'corevector/shapeOnPath', flt.shapeOnPath, 'geometry', 'list');

  // ─── Math ──────────────────────────────────────────
  registerOp(registry, 'math/abs', math.abs, 'float');
  registerOp(registry, 'math/ceil', math.ceil, 'int');
  registerOp(registry, 'math/floor', math.floor, 'int');
  registerOp(registry, 'math/round', math.round, 'int');
  registerOp(registry, 'math/negate', math.negate, 'float');
  registerOp(registry, 'math/add', math.add, 'float');
  registerOp(registry, 'math/subtract', math.subtract, 'float');
  registerOp(registry, 'math/multiply', math.multiply, 'float');
  registerOp(registry, 'math/divide', math.divide, 'float');
  registerOp(registry, 'math/mod', math.mod, 'float');
  registerOp(registry, 'math/sin', math.sin, 'float');
  registerOp(registry, 'math/cos', math.cos, 'float');
  registerOp(registry, 'math/sqrt', math.sqrt, 'float');
  registerOp(registry, 'math/pow', math.pow, 'float');
  registerOp(registry, 'math/log', math.log, 'float');
  registerOp(registry, 'math/radians', math.radians, 'float');
  registerOp(registry, 'math/degrees', math.degrees, 'float');
  registerOp(registry, 'math/pi', math.pi, 'float');
  registerOp(registry, 'math/e', math.e, 'float');
  registerOp(registry, 'math/even', math.even, 'boolean');
  registerOp(registry, 'math/odd', math.odd, 'boolean');
  registerOp(registry, 'math/min', math.min, 'float');
  registerOp(registry, 'math/max', math.max, 'float');
  registerOp(registry, 'math/average', math.average, 'float');
  registerOp(registry, 'math/sum', math.sum, 'float');
  registerOp(registry, 'math/number', math.number, 'float');
  registerOp(registry, 'math/integer', math.integer, 'int');
  registerOp(registry, 'math/boolean', math.boolean, 'boolean');
  registerOp(registry, 'math/range', math.range, 'float', 'list');
  registerOp(registry, 'math/sample', math.sample, 'float', 'list');
  registerOp(registry, 'math/randomNumbers', math.randomNumbers, 'float', 'list');
  registerOp(registry, 'math/convertRange', math.convertRange, 'float');
  registerOp(registry, 'math/coordinates', math.coordinates, 'point');
  registerOp(registry, 'math/angle', math.angle, 'float');
  registerOp(registry, 'math/distance', math.distance, 'float');
  registerOp(registry, 'math/reflect', math.reflectPoint, 'point');
  registerOp(registry, 'math/makeNumbers', math.makeNumbers, 'float', 'list');
  registerOp(registry, 'math/runningTotal', math.runningTotal, 'float', 'list');
  registerOp(registry, 'math/wave', math.wave, 'float');
  registerOp(registry, 'math/compare', math.compare, 'boolean');
  registerOp(registry, 'math/logicOperator', math.logicOperator, 'boolean');

  // ─── List ──────────────────────────────────────────
  // Note: list ops receive Value[] directly since they work with the list port type
  // They are registered differently — they get raw Value args
  registry.register('list/count', (list: Value) => {
    const items = list.type === 'list' ? list.value : [];
    return { type: 'int', value: lst.count(items) };
  });
  registry.register('list/first', (list: Value) => {
    const items = list.type === 'list' ? list.value : [];
    return lst.first(items) ?? { type: 'null' };
  });
  registry.register('list/second', (list: Value) => {
    const items = list.type === 'list' ? list.value : [];
    return lst.second(items) ?? { type: 'null' };
  });
  registry.register('list/last', (list: Value) => {
    const items = list.type === 'list' ? list.value : [];
    return lst.last(items) ?? { type: 'null' };
  });
  registry.register('list/rest', (list: Value) => {
    const items = list.type === 'list' ? list.value : [];
    return { type: 'list', value: lst.rest(items) };
  });
  registry.register('list/reverse', (list: Value) => {
    const items = list.type === 'list' ? list.value : [];
    return { type: 'list', value: lst.reverse(items) };
  });
  registry.register('list/slice', (list: Value, start: Value, size: Value, invert: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const s = start.type === 'int' ? start.value : 0;
    const sz = size.type === 'int' ? size.value : items.length;
    const inv = invert.type === 'boolean' ? invert.value : false;
    return { type: 'list', value: lst.slice(items, s, sz, inv) };
  });
  registry.register('list/shuffle', (list: Value, seed: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const s = seed.type === 'int' || seed.type === 'float' ? seed.value : 0;
    return { type: 'list', value: lst.shuffle(items, s) };
  });
  registry.register('list/shift', (list: Value, amount: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const n = amount.type === 'int' || amount.type === 'float' ? amount.value : 0;
    return { type: 'list', value: lst.shift(items, n) };
  });
  registry.register('list/repeat', (list: Value, amount: Value, perItem: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const n = amount.type === 'int' || amount.type === 'float' ? amount.value : 1;
    const pi = perItem.type === 'boolean' ? perItem.value : false;
    return { type: 'list', value: lst.repeat(items, n, pi) };
  });
  registry.register('list/sort', (list: Value, key: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const k = key.type === 'string' ? key.value : '';
    return { type: 'list', value: lst.sortList(items, k) };
  });
  registry.register('list/distinct', (list: Value, key: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const k = key.type === 'string' ? key.value : '';
    return { type: 'list', value: lst.distinct(items, k) };
  });
  registry.register('list/takeEvery', (list: Value, n: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const step = n.type === 'int' || n.type === 'float' ? n.value : 1;
    return { type: 'list', value: lst.takeEvery(items, step) };
  });
  registry.register('list/cull', (list: Value, booleans: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const bools = booleans.type === 'list' ? booleans.value : [];
    return { type: 'list', value: lst.cull(items, bools) };
  });
  registry.register('list/pick', (list: Value, amount: Value, seed: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const n = amount.type === 'int' || amount.type === 'float' ? amount.value : 1;
    const s = seed.type === 'int' || seed.type === 'float' ? seed.value : 0;
    return { type: 'list', value: lst.pick(items, n, s) };
  });
  registry.register('list/keys', (list: Value) => {
    const items = list.type === 'list' ? list.value : [];
    return { type: 'list', value: lst.keys(items) };
  });
  registry.register('list/combine', (...args: Value[]) => {
    const lists = args.map(a => a.type === 'list' ? a.value : [a]);
    while (lists.length < 7) lists.push([]);
    return { type: 'list', value: lst.combine(lists[0], lists[1], lists[2], lists[3], lists[4], lists[5], lists[6]) };
  });

  // ─── Color ─────────────────────────────────────────
  registerOp(registry, 'color/color', clr.color, 'color');
  registerOp(registry, 'color/grayColor', clr.grayColor, 'color');
  registerOp(registry, 'color/hsbColor', clr.hsbColor, 'color');
  registerOp(registry, 'color/rgbColor', clr.rgbColor, 'color');

  // ─── String ────────────────────────────────────────
  registerOp(registry, 'string/string', str.string, 'string');
  registerOp(registry, 'string/makeStrings', str.makeStrings, 'string', 'list');
  registerOp(registry, 'string/length', str.length, 'int');
  registerOp(registry, 'string/wordCount', str.wordCount, 'int');
  registerOp(registry, 'string/concatenate', str.concatenate, 'string');
  registerOp(registry, 'string/formatNumber', str.formatNumber, 'string');
  registerOp(registry, 'string/characters', str.characters, 'string', 'list');
  registerOp(registry, 'string/characterAt', str.characterAt, 'string');
  registerOp(registry, 'string/contains', str.contains, 'boolean');
  registerOp(registry, 'string/startsWith', str.startsWith, 'boolean');
  registerOp(registry, 'string/endsWith', str.endsWith, 'boolean');
  registerOp(registry, 'string/equals', str.equals, 'boolean');
  registerOp(registry, 'string/replace', str.replace, 'string');
  registerOp(registry, 'string/subString', str.subString, 'string');
  registerOp(registry, 'string/trim', str.trim, 'string');
  registerOp(registry, 'string/changeCase', str.changeCase, 'string');
  registerOp(registry, 'string/randomCharacter', str.randomCharacter, 'string');
  registerOp(registry, 'string/asBinaryString', str.asBinaryString, 'string');
  registerOp(registry, 'string/asBinaryList', str.asBinaryList, 'int', 'list');
  registerOp(registry, 'string/asNumberList', str.asNumberList, 'float', 'list');

  // ─── Data ──────────────────────────────────────────
  // importCsv and importText are async and need Platform, registered specially
  registry.register('data/lookup', (list: Value, key: Value) => {
    const items = list.type === 'list' ? list.value : [];
    const k = key.type === 'string' ? key.value : '';
    return { type: 'list', value: data.lookup(items, k) } as Value;
  });
  registry.register('data/filterData', (dataList: Value, key: Value, op: Value, value: Value) => {
    const items = dataList.type === 'list' ? dataList.value : [];
    const k = key.type === 'string' ? key.value : '';
    const o = op.type === 'string' ? op.value : 'equal';
    const v = value.type === 'string' ? value.value : '';
    return { type: 'list', value: data.filterData(items, k, o, v) } as Value;
  });

  // ─── Network ───────────────────────────────────────
  registerOp(registry, 'network/encodeUrl', net.encodeUrl, 'string');

  return registry;
}

// Helper to register an op with Value wrapping/unwrapping
function registerOp(
  registry: FunctionRegistry,
  name: string,
  fn: (...args: any[]) => any,
  outputType: string,
  outputRange: PortRange = 'value',
): void {
  registry.register(name, (...args: Value[]) => {
    const unwrapped = args.map(unwrapValue);
    const result = fn(...unwrapped);
    if (outputRange === 'list' && Array.isArray(result)) {
      // For list output, wrap each element individually
      return result.map((item: any) => wrapResult(item, outputType));
    }
    return wrapResult(result, outputType, outputRange);
  });
}
