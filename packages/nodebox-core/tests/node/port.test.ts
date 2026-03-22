import { describe, it, expect } from 'vitest';
import {
  createPort, isFileWidget, isPublishedPort,
  getChildNodeName, getChildPortName, clampPortValue,
} from '../../src/node/port.js';
import { floatValue } from '../../src/node/value.js';

describe('Port', () => {
  it('creates a port with defaults', () => {
    const p = createPort('width', 'float', floatValue(100));
    expect(p.name).toBe('width');
    expect(p.type).toBe('float');
    expect(p.widget).toBe('float');
    expect(p.range).toBe('value');
    expect(p.minimumValue).toBeNull();
    expect(p.childReference).toBeNull();
  });

  it('assigns default widget per type', () => {
    expect(createPort('a', 'int').widget).toBe('int');
    expect(createPort('a', 'boolean').widget).toBe('toggle');
    expect(createPort('a', 'color').widget).toBe('color');
    expect(createPort('a', 'geometry').widget).toBe('none');
  });

  it('detects file widget', () => {
    const p = { ...createPort('file', 'string'), widget: 'file' as const };
    expect(isFileWidget(p)).toBe(true);
    expect(isFileWidget(createPort('x', 'string'))).toBe(false);
  });

  it('detects published ports', () => {
    const p = { ...createPort('list', 'list'), childReference: 'slice1/list' };
    expect(isPublishedPort(p)).toBe(true);
    expect(getChildNodeName(p)).toBe('slice1');
    expect(getChildPortName(p)).toBe('list');
  });

  it('clamps port value', () => {
    const p = { ...createPort('x', 'float'), minimumValue: 0, maximumValue: 100 };
    expect(clampPortValue(p, 50)).toBe(50);
    expect(clampPortValue(p, -10)).toBe(0);
    expect(clampPortValue(p, 200)).toBe(100);
  });

  it('clamps with only min', () => {
    const p = { ...createPort('x', 'float'), minimumValue: 0, maximumValue: null };
    expect(clampPortValue(p, -5)).toBe(0);
    expect(clampPortValue(p, 1000)).toBe(1000);
  });
});
