import { describe, it, expect } from 'vitest';
import {
  intValue, floatValue, stringValue, booleanValue, pointValue,
  colorValue, geometryValue, listValue, dataValue,
  valueToString, unwrapValue, NULL_VALUE,
} from '../../src/node/value.js';

describe('Value', () => {
  it('creates typed values', () => {
    expect(intValue(5)).toEqual({ type: 'int', value: 5 });
    expect(floatValue(3.14)).toEqual({ type: 'float', value: 3.14 });
    expect(stringValue('hello')).toEqual({ type: 'string', value: 'hello' });
    expect(booleanValue(true)).toEqual({ type: 'boolean', value: true });
    expect(pointValue(1, 2)).toEqual({ type: 'point', value: { x: 1, y: 2 } });
  });

  it('intValue rounds', () => {
    expect(intValue(3.7).value).toBe(4);
  });

  it('creates color value', () => {
    const v = colorValue(1, 0, 0, 0.5);
    expect(v).toEqual({ type: 'color', value: { r: 1, g: 0, b: 0, a: 0.5 } });
  });

  it('creates geometry value', () => {
    const v = geometryValue([]);
    expect(v).toEqual({ type: 'geometry', value: [] });
  });

  it('creates list value', () => {
    const v = listValue([intValue(1), intValue(2)]);
    expect(v.type).toBe('list');
    expect((v as any).value.length).toBe(2);
  });

  it('creates data value', () => {
    const v = dataValue({ key: 'val' });
    expect(v).toEqual({ type: 'data', value: { key: 'val' } });
  });

  it('converts value to string', () => {
    expect(valueToString(intValue(42))).toBe('42');
    expect(valueToString(floatValue(3.14))).toBe('3.14');
    expect(valueToString(stringValue('hello'))).toBe('hello');
    expect(valueToString(booleanValue(true))).toBe('true');
    expect(valueToString(pointValue(1, 2))).toBe('1,2');
    expect(valueToString(NULL_VALUE)).toBe('');
  });

  it('converts color to hex string', () => {
    const hex = valueToString(colorValue(1, 0, 0, 1));
    expect(hex).toBe('#ff0000ff');
  });

  it('unwraps values', () => {
    expect(unwrapValue(intValue(5))).toBe(5);
    expect(unwrapValue(stringValue('hi'))).toBe('hi');
    expect(unwrapValue(NULL_VALUE)).toBeNull();
    expect(unwrapValue(pointValue(1, 2))).toEqual({ x: 1, y: 2 });
  });
});
