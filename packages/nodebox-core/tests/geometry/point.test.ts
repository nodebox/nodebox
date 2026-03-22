import { describe, it, expect } from 'vitest';
import {
  createPoint, addPoints, subtractPoints, scalePoint,
  distanceBetween, angleBetween, lerpPoint, rotatePoint, pointEquals, ZERO,
} from '../../src/geometry/point.js';

describe('Point', () => {
  it('creates a point', () => {
    const p = createPoint(3, 4);
    expect(p.x).toBe(3);
    expect(p.y).toBe(4);
  });

  it('adds points', () => {
    expect(addPoints({ x: 1, y: 2 }, { x: 3, y: 4 })).toEqual({ x: 4, y: 6 });
  });

  it('subtracts points', () => {
    expect(subtractPoints({ x: 5, y: 7 }, { x: 2, y: 3 })).toEqual({ x: 3, y: 4 });
  });

  it('scales a point', () => {
    expect(scalePoint({ x: 3, y: 4 }, 2)).toEqual({ x: 6, y: 8 });
  });

  it('calculates distance', () => {
    expect(distanceBetween({ x: 0, y: 0 }, { x: 3, y: 4 })).toBe(5);
  });

  it('calculates angle', () => {
    expect(angleBetween({ x: 0, y: 0 }, { x: 1, y: 0 })).toBe(0);
    expect(angleBetween({ x: 0, y: 0 }, { x: 0, y: 1 })).toBe(90);
    expect(angleBetween({ x: 0, y: 0 }, { x: -1, y: 0 })).toBe(180);
  });

  it('lerps between points', () => {
    const result = lerpPoint({ x: 0, y: 0 }, { x: 10, y: 20 }, 0.5);
    expect(result).toEqual({ x: 5, y: 10 });
  });

  it('lerps at boundaries', () => {
    const a = { x: 1, y: 2 };
    const b = { x: 5, y: 6 };
    expect(lerpPoint(a, b, 0)).toEqual(a);
    expect(lerpPoint(a, b, 1)).toEqual(b);
  });

  it('rotates a point', () => {
    const p = rotatePoint({ x: 1, y: 0 }, 90);
    expect(p.x).toBeCloseTo(0, 10);
    expect(p.y).toBeCloseTo(1, 10);
  });

  it('rotates around an origin', () => {
    const p = rotatePoint({ x: 2, y: 0 }, 90, { x: 1, y: 0 });
    expect(p.x).toBeCloseTo(1, 10);
    expect(p.y).toBeCloseTo(1, 10);
  });

  it('compares points with epsilon', () => {
    expect(pointEquals({ x: 1, y: 2 }, { x: 1, y: 2 })).toBe(true);
    expect(pointEquals({ x: 1, y: 2 }, { x: 1.0000000001, y: 2 })).toBe(true);
    expect(pointEquals({ x: 1, y: 2 }, { x: 2, y: 2 })).toBe(false);
  });

  it('ZERO constant', () => {
    expect(ZERO).toEqual({ x: 0, y: 0 });
  });
});
