import { describe, it, expect } from 'vitest';
import { color, grayColor, hsbColor, rgbColor } from '../../src/ops/color.js';

describe('Color Operations', () => {
  it('color passthrough', () => {
    const c = { r: 0.5, g: 0.5, b: 0.5, a: 1 };
    expect(color(c)).toBe(c);
  });

  it('grayColor', () => {
    const c = grayColor(128, 255, 255);
    expect(c.r).toBeCloseTo(128 / 255);
    expect(c.g).toBeCloseTo(128 / 255);
    expect(c.b).toBeCloseTo(128 / 255);
    expect(c.a).toBeCloseTo(1);
  });

  it('hsbColor', () => {
    const c = hsbColor(0, 255, 255, 255, 255);
    expect(c.r).toBeCloseTo(1, 1);
    expect(c.g).toBeCloseTo(0, 1);
    expect(c.b).toBeCloseTo(0, 1);
  });

  it('rgbColor', () => {
    const c = rgbColor(255, 0, 0, 255, 255);
    expect(c.r).toBeCloseTo(1);
    expect(c.g).toBeCloseTo(0);
    expect(c.b).toBeCloseTo(0);
    expect(c.a).toBeCloseTo(1);
  });

  it('rgbColor zero range', () => {
    const c = rgbColor(100, 100, 100, 100, 0);
    expect(c.r).toBe(0);
  });
});
