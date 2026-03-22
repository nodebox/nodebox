import { describe, it, expect } from 'vitest';
import {
  createRect, rectFromPoints, rectCenter, rectContains,
  rectUnion, rectIntersects, normalizeRect, EMPTY_RECT,
} from '../../src/geometry/rect.js';

describe('Rect', () => {
  it('creates a rect', () => {
    const r = createRect(10, 20, 30, 40);
    expect(r).toEqual({ x: 10, y: 20, width: 30, height: 40 });
  });

  it('creates rect from points', () => {
    const r = rectFromPoints({ x: 10, y: 20 }, { x: 40, y: 60 });
    expect(r).toEqual({ x: 10, y: 20, width: 30, height: 40 });
  });

  it('creates rect from reversed points', () => {
    const r = rectFromPoints({ x: 40, y: 60 }, { x: 10, y: 20 });
    expect(r).toEqual({ x: 10, y: 20, width: 30, height: 40 });
  });

  it('calculates center', () => {
    expect(rectCenter({ x: 0, y: 0, width: 10, height: 20 })).toEqual({ x: 5, y: 10 });
  });

  it('checks containment', () => {
    const r = createRect(0, 0, 10, 10);
    expect(rectContains(r, { x: 5, y: 5 })).toBe(true);
    expect(rectContains(r, { x: 0, y: 0 })).toBe(true);
    expect(rectContains(r, { x: 10, y: 10 })).toBe(true);
    expect(rectContains(r, { x: 11, y: 5 })).toBe(false);
  });

  it('calculates union', () => {
    const a = createRect(0, 0, 10, 10);
    const b = createRect(5, 5, 10, 10);
    const u = rectUnion(a, b);
    expect(u).toEqual({ x: 0, y: 0, width: 15, height: 15 });
  });

  it('checks intersection', () => {
    const a = createRect(0, 0, 10, 10);
    expect(rectIntersects(a, createRect(5, 5, 10, 10))).toBe(true);
    expect(rectIntersects(a, createRect(20, 20, 10, 10))).toBe(false);
  });

  it('normalizes negative dimensions', () => {
    const r = normalizeRect({ x: 10, y: 10, width: -5, height: -3 });
    expect(r).toEqual({ x: 5, y: 7, width: 5, height: 3 });
  });

  it('EMPTY_RECT constant', () => {
    expect(EMPTY_RECT).toEqual({ x: 0, y: 0, width: 0, height: 0 });
  });
});
