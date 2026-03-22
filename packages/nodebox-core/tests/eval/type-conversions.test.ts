import { describe, it, expect } from 'vitest';
import { convert, clampValue } from '../../src/eval/type-conversions.js';
import type { Value } from '../../src/node/value.js';

describe('Type Conversions', () => {
  // int conversions
  it('int -> float', () => {
    expect(convert({ type: 'int', value: 5 }, 'float')).toEqual({ type: 'float', value: 5 });
  });
  it('int -> boolean', () => {
    expect(convert({ type: 'int', value: 1 }, 'boolean')).toEqual({ type: 'boolean', value: true });
    expect(convert({ type: 'int', value: 0 }, 'boolean')).toEqual({ type: 'boolean', value: false });
    expect(convert({ type: 'int', value: -1 }, 'boolean')).toEqual({ type: 'boolean', value: false });
  });
  it('int -> string', () => {
    expect(convert({ type: 'int', value: 42 }, 'string')).toEqual({ type: 'string', value: '42' });
  });
  it('int -> point', () => {
    expect(convert({ type: 'int', value: 5 }, 'point')).toEqual({ type: 'point', value: { x: 5, y: 5 } });
  });
  it('int -> color (grayscale)', () => {
    const c = convert({ type: 'int', value: 128 }, 'color') as any;
    expect(c.type).toBe('color');
    expect(c.value.r).toBeCloseTo(128 / 255, 2);
  });

  // float conversions
  it('float -> int (round)', () => {
    expect(convert({ type: 'float', value: 3.7 }, 'int')).toEqual({ type: 'int', value: 4 });
  });
  it('float -> boolean', () => {
    expect(convert({ type: 'float', value: 0.5 }, 'boolean')).toEqual({ type: 'boolean', value: true });
  });
  it('float -> point', () => {
    expect(convert({ type: 'float', value: 3.5 }, 'point')).toEqual({ type: 'point', value: { x: 3.5, y: 3.5 } });
  });

  // boolean conversions
  it('boolean -> int', () => {
    expect(convert({ type: 'boolean', value: true }, 'int')).toEqual({ type: 'int', value: 1 });
    expect(convert({ type: 'boolean', value: false }, 'int')).toEqual({ type: 'int', value: 0 });
  });
  it('boolean -> float', () => {
    expect(convert({ type: 'boolean', value: true }, 'float')).toEqual({ type: 'float', value: 1.0 });
  });
  it('boolean -> string', () => {
    expect(convert({ type: 'boolean', value: true }, 'string')).toEqual({ type: 'string', value: 'true' });
  });
  it('boolean -> color', () => {
    const white = convert({ type: 'boolean', value: true }, 'color') as any;
    expect(white.value.r).toBe(1);
    const black = convert({ type: 'boolean', value: false }, 'color') as any;
    expect(black.value.r).toBe(0);
  });

  // string conversions
  it('string -> int', () => {
    expect(convert({ type: 'string', value: '42' }, 'int')).toEqual({ type: 'int', value: 42 });
    expect(convert({ type: 'string', value: 'abc' }, 'int')).toEqual({ type: 'int', value: 0 });
  });
  it('string -> float', () => {
    expect(convert({ type: 'string', value: '3.14' }, 'float')).toEqual({ type: 'float', value: 3.14 });
  });
  it('string -> boolean', () => {
    expect(convert({ type: 'string', value: 'true' }, 'boolean')).toEqual({ type: 'boolean', value: true });
    expect(convert({ type: 'string', value: 'false' }, 'boolean')).toEqual({ type: 'boolean', value: false });
  });
  it('string -> point', () => {
    expect(convert({ type: 'string', value: '1.5,2.5' }, 'point')).toEqual({
      type: 'point', value: { x: 1.5, y: 2.5 },
    });
  });
  it('string -> color', () => {
    const c = convert({ type: 'string', value: '#ff0000ff' }, 'color') as any;
    expect(c.type).toBe('color');
    expect(c.value.r).toBeCloseTo(1, 2);
  });

  // list/data accept anything
  it('anything -> list', () => {
    expect(convert({ type: 'int', value: 5 }, 'list')).toEqual({ type: 'int', value: 5 });
  });
  it('anything -> data', () => {
    expect(convert({ type: 'string', value: 'hi' }, 'data')).toEqual({ type: 'string', value: 'hi' });
  });

  // same type passes through
  it('same type', () => {
    const v: Value = { type: 'int', value: 5 };
    expect(convert(v, 'int')).toBe(v);
  });

  // null passes through
  it('null passthrough', () => {
    expect(convert({ type: 'null' }, 'int')).toEqual({ type: 'null' });
  });
});

describe('clampValue', () => {
  it('clamps int', () => {
    expect(clampValue({ type: 'int', value: 50 }, 0, 100)).toEqual({ type: 'int', value: 50 });
    expect(clampValue({ type: 'int', value: -10 }, 0, 100)).toEqual({ type: 'int', value: 0 });
    expect(clampValue({ type: 'int', value: 200 }, 0, 100)).toEqual({ type: 'int', value: 100 });
  });

  it('clamps float', () => {
    expect(clampValue({ type: 'float', value: 1.5 }, 0, 1)).toEqual({ type: 'float', value: 1 });
  });

  it('skips non-numeric', () => {
    const v: Value = { type: 'string', value: 'hi' };
    expect(clampValue(v, 0, 100)).toBe(v);
  });

  it('no-op with null bounds', () => {
    const v: Value = { type: 'int', value: 999 };
    expect(clampValue(v, null, null)).toBe(v);
  });
});
