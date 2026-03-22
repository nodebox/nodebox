import { describe, it, expect } from 'vitest';
import {
  createColor, colorFromHex, colorToHex, colorFromHSB, colorToCSS,
  colorEquals, BLACK, WHITE, TRANSPARENT,
} from '../../src/geometry/color.js';

describe('Color', () => {
  it('creates a color', () => {
    const c = createColor(0.5, 0.6, 0.7, 0.8);
    expect(c).toEqual({ r: 0.5, g: 0.6, b: 0.7, a: 0.8 });
  });

  it('defaults alpha to 1', () => {
    const c = createColor(0.5, 0.6, 0.7);
    expect(c.a).toBe(1);
  });

  it('parses 6-digit hex', () => {
    const c = colorFromHex('#ff8040');
    expect(c.r).toBeCloseTo(1, 2);
    expect(c.g).toBeCloseTo(128 / 255, 2);
    expect(c.b).toBeCloseTo(64 / 255, 2);
    expect(c.a).toBe(1);
  });

  it('parses 8-digit hex', () => {
    const c = colorFromHex('#ff804080');
    expect(c.r).toBeCloseTo(1, 2);
    expect(c.a).toBeCloseTo(128 / 255, 2);
  });

  it('parses hex without #', () => {
    const c = colorFromHex('ff0000');
    expect(c.r).toBeCloseTo(1, 2);
    expect(c.g).toBeCloseTo(0, 2);
    expect(c.b).toBeCloseTo(0, 2);
  });

  it('throws on invalid hex', () => {
    expect(() => colorFromHex('#abc')).toThrow('Invalid hex color');
  });

  it('converts to hex', () => {
    expect(colorToHex({ r: 1, g: 0, b: 0, a: 1 })).toBe('#ff0000ff');
    expect(colorToHex({ r: 0, g: 0, b: 0, a: 0 })).toBe('#00000000');
  });

  it('converts from HSB', () => {
    // Red
    const red = colorFromHSB(0, 1, 1);
    expect(red.r).toBeCloseTo(1, 2);
    expect(red.g).toBeCloseTo(0, 2);
    expect(red.b).toBeCloseTo(0, 2);

    // Green
    const green = colorFromHSB(120, 1, 1);
    expect(green.r).toBeCloseTo(0, 2);
    expect(green.g).toBeCloseTo(1, 2);
    expect(green.b).toBeCloseTo(0, 2);

    // Blue
    const blue = colorFromHSB(240, 1, 1);
    expect(blue.r).toBeCloseTo(0, 2);
    expect(blue.g).toBeCloseTo(0, 2);
    expect(blue.b).toBeCloseTo(1, 2);
  });

  it('converts to CSS rgb', () => {
    expect(colorToCSS({ r: 1, g: 0, b: 0, a: 1 })).toBe('rgb(255, 0, 0)');
  });

  it('converts to CSS rgba', () => {
    const css = colorToCSS({ r: 1, g: 0, b: 0, a: 0.5 });
    expect(css).toBe('rgba(255, 0, 0, 0.500)');
  });

  it('compares colors', () => {
    expect(colorEquals(BLACK, { r: 0, g: 0, b: 0, a: 1 })).toBe(true);
    expect(colorEquals(BLACK, WHITE)).toBe(false);
  });

  it('has correct constants', () => {
    expect(BLACK).toEqual({ r: 0, g: 0, b: 0, a: 1 });
    expect(WHITE).toEqual({ r: 1, g: 1, b: 1, a: 1 });
    expect(TRANSPARENT).toEqual({ r: 0, g: 0, b: 0, a: 0 });
  });
});
