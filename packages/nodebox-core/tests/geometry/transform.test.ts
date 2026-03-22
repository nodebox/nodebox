import { describe, it, expect } from 'vitest';
import {
  identityTransform, translateTransform, rotateTransform, scaleTransform,
  skewTransform, multiplyTransforms, transformPoint, invertTransform, transformRect,
} from '../../src/geometry/transform.js';

describe('Transform', () => {
  it('identity transform does not change point', () => {
    const t = identityTransform();
    const p = transformPoint(t, { x: 5, y: 10 });
    expect(p).toEqual({ x: 5, y: 10 });
  });

  it('translates a point', () => {
    const t = translateTransform(10, 20);
    const p = transformPoint(t, { x: 5, y: 5 });
    expect(p).toEqual({ x: 15, y: 25 });
  });

  it('scales a point', () => {
    const t = scaleTransform(2, 3);
    const p = transformPoint(t, { x: 5, y: 10 });
    expect(p).toEqual({ x: 10, y: 30 });
  });

  it('rotates a point 90 degrees', () => {
    const t = rotateTransform(90);
    const p = transformPoint(t, { x: 1, y: 0 });
    expect(p.x).toBeCloseTo(0, 10);
    expect(p.y).toBeCloseTo(1, 10);
  });

  it('rotates a point 180 degrees', () => {
    const t = rotateTransform(180);
    const p = transformPoint(t, { x: 1, y: 0 });
    expect(p.x).toBeCloseTo(-1, 10);
    expect(p.y).toBeCloseTo(0, 10);
  });

  it('multiplies transforms', () => {
    const t1 = translateTransform(10, 0);
    const t2 = scaleTransform(2, 2);
    const combined = multiplyTransforms(t2, t1); // first translate, then scale
    const p = transformPoint(combined, { x: 5, y: 0 });
    // translate: (15, 0), then scale: (30, 0)
    expect(p.x).toBeCloseTo(30, 10);
    expect(p.y).toBeCloseTo(0, 10);
  });

  it('inverts a transform', () => {
    const t = translateTransform(10, 20);
    const inv = invertTransform(t);
    expect(inv).not.toBeNull();
    const p = transformPoint(inv!, { x: 15, y: 25 });
    expect(p.x).toBeCloseTo(5, 10);
    expect(p.y).toBeCloseTo(5, 10);
  });

  it('returns null for non-invertible transform', () => {
    const t = scaleTransform(0, 0);
    expect(invertTransform(t)).toBeNull();
  });

  it('skews a point', () => {
    const t = skewTransform(45, 0);
    const p = transformPoint(t, { x: 0, y: 10 });
    expect(p.x).toBeCloseTo(-10, 10);
    expect(p.y).toBeCloseTo(10, 10);
  });

  it('transforms a rect', () => {
    const t = translateTransform(5, 5);
    const r = transformRect(t, { x: 0, y: 0, width: 10, height: 10 });
    expect(r.x).toBeCloseTo(5, 10);
    expect(r.y).toBeCloseTo(5, 10);
    expect(r.width).toBeCloseTo(10, 10);
    expect(r.height).toBeCloseTo(10, 10);
  });
});
