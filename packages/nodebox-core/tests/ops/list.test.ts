import { describe, it, expect } from 'vitest';
import * as lst from '../../src/ops/list.js';
import { intValue, stringValue, type Value } from '../../src/node/value.js';

const v = (n: number): Value => intValue(n);
const vs = (...nums: number[]): Value[] => nums.map(v);

describe('List Operations', () => {
  it('count', () => expect(lst.count(vs(1, 2, 3))).toBe(3));
  it('count empty', () => expect(lst.count([])).toBe(0));
  it('first', () => expect(lst.first(vs(10, 20))).toEqual(v(10)));
  it('first empty', () => expect(lst.first([])).toBeNull());
  it('second', () => expect(lst.second(vs(10, 20, 30))).toEqual(v(20)));
  it('last', () => expect(lst.last(vs(10, 20, 30))).toEqual(v(30)));
  it('rest', () => expect(lst.rest(vs(1, 2, 3))).toEqual(vs(2, 3)));

  it('slice', () => expect(lst.slice(vs(1, 2, 3, 4, 5), 1, 3, false)).toEqual(vs(2, 3, 4)));
  it('slice inverted', () => expect(lst.slice(vs(1, 2, 3, 4, 5), 1, 3, true)).toEqual(vs(1, 5)));

  it('reverse', () => expect(lst.reverse(vs(1, 2, 3))).toEqual(vs(3, 2, 1)));

  it('shuffle deterministic', () => {
    const a = lst.shuffle(vs(1, 2, 3, 4, 5), 42);
    const b = lst.shuffle(vs(1, 2, 3, 4, 5), 42);
    expect(a).toEqual(b);
  });

  it('shift', () => {
    expect(lst.shift(vs(1, 2, 3), 1)).toEqual(vs(2, 3, 1));
  });

  it('repeat list', () => {
    expect(lst.repeat(vs(1, 2), 3, false)).toEqual(vs(1, 2, 1, 2, 1, 2));
  });
  it('repeat per item', () => {
    expect(lst.repeat(vs(1, 2), 3, true)).toEqual(vs(1, 1, 1, 2, 2, 2));
  });

  it('takeEvery', () => expect(lst.takeEvery(vs(1, 2, 3, 4, 5, 6), 2)).toEqual(vs(1, 3, 5)));

  it('cull', () => {
    const bools: Value[] = [
      { type: 'boolean', value: true },
      { type: 'boolean', value: false },
    ];
    expect(lst.cull(vs(1, 2, 3, 4), bools)).toEqual(vs(1, 3));
  });

  it('distinct', () => expect(lst.distinct(vs(1, 2, 1, 3, 2), '')).toEqual(vs(1, 2, 3)));

  it('combine', () => {
    expect(lst.combine(vs(1), vs(2), vs(3), [], [], [], [])).toEqual(vs(1, 2, 3));
  });

  it('keys', () => {
    const data: Value = { type: 'data', value: { a: 1, b: 2 } };
    const result = lst.keys([data]);
    expect(result).toEqual([stringValue('a'), stringValue('b')]);
  });
});
