import { describe, it, expect } from 'vitest';
import * as m from '../../src/ops/math.js';

describe('Math Operations', () => {
  it('abs', () => expect(m.abs(-5)).toBe(5));
  it('ceil', () => expect(m.ceil(3.2)).toBe(4));
  it('floor', () => expect(m.floor(3.8)).toBe(3));
  it('round', () => expect(m.round(3.5)).toBe(4));
  it('negate', () => expect(m.negate(5)).toBe(-5));

  it('add', () => expect(m.add(3, 4)).toBe(7));
  it('subtract', () => expect(m.subtract(10, 3)).toBe(7));
  it('multiply', () => expect(m.multiply(3, 4)).toBe(12));
  it('divide', () => expect(m.divide(10, 4)).toBe(2.5));
  it('divide by zero', () => expect(m.divide(10, 0)).toBe(0));
  it('mod', () => expect(m.mod(10, 3)).toBe(1));

  it('sin (radians)', () => expect(m.sin(Math.PI / 2)).toBeCloseTo(1));
  it('cos (radians)', () => expect(m.cos(0)).toBeCloseTo(1));
  it('sqrt', () => expect(m.sqrt(9)).toBe(3));
  it('pow', () => expect(m.pow(2, 10)).toBe(1024));
  it('log', () => expect(m.log(Math.E)).toBeCloseTo(1));
  it('log of negative', () => expect(m.log(-1)).toBe(0));

  it('radians', () => expect(m.radians(180)).toBeCloseTo(Math.PI));
  it('degrees', () => expect(m.degrees(Math.PI)).toBeCloseTo(180));

  it('pi', () => expect(m.pi()).toBeCloseTo(Math.PI));
  it('e', () => expect(m.e()).toBeCloseTo(Math.E));

  it('even', () => { expect(m.even(4)).toBe(true); expect(m.even(3)).toBe(false); });
  it('odd', () => { expect(m.odd(3)).toBe(true); expect(m.odd(4)).toBe(false); });

  it('min', () => expect(m.min([5, 3, 8])).toBe(3));
  it('max', () => expect(m.max([5, 3, 8])).toBe(8));
  it('average', () => expect(m.average([2, 4, 6])).toBe(4));
  it('sum', () => expect(m.sum([1, 2, 3])).toBe(6));

  it('range', () => expect(m.range(0, 5, 1)).toEqual([0, 1, 2, 3, 4]));
  it('range negative step', () => expect(m.range(5, 0, -1)).toEqual([5, 4, 3, 2, 1]));
  it('sample', () => {
    const s = m.sample(5, 0, 10);
    expect(s.length).toBe(5);
    expect(s[0]).toBeCloseTo(0);
    expect(s[4]).toBeCloseTo(10);
  });
  it('randomNumbers deterministic', () => {
    const a = m.randomNumbers(5, 0, 1, 42);
    const b = m.randomNumbers(5, 0, 1, 42);
    expect(a).toEqual(b);
  });

  it('convertRange', () => {
    expect(m.convertRange(5, 0, 10, 0, 100, 'none')).toBeCloseTo(50);
  });
  it('convertRange clamp', () => {
    expect(m.convertRange(15, 0, 10, 0, 100, 'clamp')).toBeCloseTo(100);
  });

  it('coordinates', () => {
    const p = m.coordinates({ x: 0, y: 0 }, 0, 100);
    expect(p.x).toBeCloseTo(100);
    expect(p.y).toBeCloseTo(0);
  });

  it('angle', () => expect(m.angle({ x: 0, y: 0 }, { x: 1, y: 0 })).toBeCloseTo(0));
  it('distance', () => expect(m.distance({ x: 0, y: 0 }, { x: 3, y: 4 })).toBeCloseTo(5));

  it('makeNumbers', () => expect(m.makeNumbers('1,2,3', ',')).toEqual([1, 2, 3]));
  it('runningTotal', () => expect(m.runningTotal([1, 2, 3])).toEqual([1, 3, 6]));

  it('wave sine', () => {
    const v = m.wave(0, 1, 4, 0, 'sine', 1);
    expect(v).toBeGreaterThanOrEqual(0);
    expect(v).toBeLessThanOrEqual(1);
  });

  it('compare', () => {
    expect(m.compare(5, 3, 'greater-than')).toBe(true);
    expect(m.compare(3, 5, 'less-than')).toBe(true);
    expect(m.compare(5, 5, 'equal')).toBe(true);
  });

  it('logicOperator', () => {
    expect(m.logicOperator(true, false, 'and')).toBe(false);
    expect(m.logicOperator(true, false, 'or')).toBe(true);
    expect(m.logicOperator(true, false, 'not')).toBe(false);
    expect(m.logicOperator(true, false, 'xor')).toBe(true);
  });
});
