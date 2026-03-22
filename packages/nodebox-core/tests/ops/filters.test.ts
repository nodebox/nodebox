import { describe, it, expect } from 'vitest';
import {
  align, colorize, copy, fit, fitTo, translateOp, rotateOp, scaleOp, skewOp,
  reflect, snap, resample, wiggle, ungroup, scatter, centroid, pointOnPathOp,
  sortShapes, stack, distribute, deletePaths, roundSegments, compound, shapeOnPath,
} from '../../src/ops/filters.js';
import { rect, ellipse, line, polygon } from '../../src/ops/generators.js';
import { pathBounds, createPath, moveTo, lineTo, closePath } from '../../src/geometry/path.js';

describe('Filter Operations', () => {
  const testRect = () => rect({ x: 0, y: 0 }, 100, 100, { x: 0, y: 0 });
  const testSquare = () => rect({ x: 50, y: 50 }, 20, 20, { x: 0, y: 0 });

  describe('align', () => {
    it('aligns to left', () => {
      const p = align(testRect(), { x: 0, y: 0 }, 'left', 'top');
      const b = pathBounds(p);
      expect(b.x).toBeCloseTo(0);
      expect(b.y).toBeCloseTo(0);
    });

    it('aligns to center', () => {
      const p = align(testRect(), { x: 200, y: 200 }, 'center', 'middle');
      const b = pathBounds(p);
      expect(b.x + b.width / 2).toBeCloseTo(200);
      expect(b.y + b.height / 2).toBeCloseTo(200);
    });
  });

  describe('colorize', () => {
    it('sets fill and stroke', () => {
      const p = colorize(testRect(), { r: 1, g: 0, b: 0, a: 1 }, { r: 0, g: 0, b: 1, a: 1 }, 2);
      expect(p.fill).toEqual({ r: 1, g: 0, b: 0, a: 1 });
      expect(p.stroke).toEqual({ r: 0, g: 0, b: 1, a: 1 });
      expect(p.strokeWidth).toBe(2);
    });
  });

  describe('copy', () => {
    it('creates copies with translation', () => {
      const copies = copy(testRect(), 3, 'tsr', { x: 10, y: 0 }, 0, { x: 100, y: 100 });
      expect(copies.length).toBe(3);
      const b0 = pathBounds(copies[0]);
      const b1 = pathBounds(copies[1]);
      expect(b1.x - b0.x).toBeCloseTo(10);
    });

    it('single copy returns original', () => {
      const copies = copy(testRect(), 1, 'tsr', { x: 10, y: 0 }, 0, { x: 100, y: 100 });
      expect(copies.length).toBe(1);
    });
  });

  describe('fit', () => {
    it('fits shape to target size', () => {
      const p = fit(testRect(), { x: 0, y: 0 }, 50, 50, false);
      const b = pathBounds(p);
      expect(b.width).toBeCloseTo(50);
      expect(b.height).toBeCloseTo(50);
    });

    it('keeps proportions', () => {
      const p = fit(testRect(), { x: 0, y: 0 }, 200, 100, true);
      const b = pathBounds(p);
      expect(b.width).toBeCloseTo(100);
      expect(b.height).toBeCloseTo(100);
    });
  });

  describe('translate', () => {
    it('moves shape', () => {
      const p = translateOp(testRect(), { x: 50, y: 30 });
      const b = pathBounds(p);
      expect(b.x).toBeCloseTo(0);
      expect(b.y).toBeCloseTo(-20);
    });
  });

  describe('rotate', () => {
    it('rotates shape', () => {
      const p = rotateOp(testRect(), 45, { x: 0, y: 0 });
      expect(p.contours[0].points.length).toBe(4);
    });
  });

  describe('scale', () => {
    it('scales shape by percentage', () => {
      const p = scaleOp(testRect(), { x: 200, y: 200 }, { x: 0, y: 0 });
      const b = pathBounds(p);
      expect(b.width).toBeCloseTo(200);
      expect(b.height).toBeCloseTo(200);
    });
  });

  describe('reflect', () => {
    it('reflects shape', () => {
      const p = reflect(testRect(), { x: 0, y: 0 }, 0, false);
      expect((p as any).contours).toBeDefined();
    });

    it('keeps original when requested', () => {
      const result = reflect(testRect(), { x: 0, y: 0 }, 0, true);
      expect(Array.isArray(result)).toBe(true);
      expect((result as any[]).length).toBe(2);
    });
  });

  describe('snap', () => {
    it('snaps points to grid', () => {
      const p = snap(testRect(), 50, 100, { x: 0, y: 0 });
      for (const pt of p.contours[0].points) {
        expect(pt.point.x % 50).toBeCloseTo(0);
        expect(pt.point.y % 50).toBeCloseTo(0);
      }
    });
  });

  describe('resample', () => {
    it('resamples by amount', () => {
      const l = line({ x: 0, y: 0 }, { x: 100, y: 0 }, 2);
      const r = resample(l, 'amount', 0, 10, false);
      expect(r.contours[0].points.length).toBe(10);
    });
  });

  describe('wiggle', () => {
    it('wiggles points', () => {
      const result = wiggle(testRect(), 'points', { x: 10, y: 10 }, 42);
      expect(result.length).toBe(1);
    });

    it('is deterministic with same seed', () => {
      const r1 = wiggle(testRect(), 'points', { x: 10, y: 10 }, 42);
      const r2 = wiggle(testRect(), 'points', { x: 10, y: 10 }, 42);
      expect(r1[0].contours[0].points[0].point.x).toBe(r2[0].contours[0].points[0].point.x);
    });
  });

  describe('ungroup', () => {
    it('splits path into contours', () => {
      let p = createPath();
      p = moveTo(p, 0, 0);
      p = lineTo(p, 10, 0);
      p = moveTo(p, 20, 0);
      p = lineTo(p, 30, 0);
      const result = ungroup(p);
      expect(result.length).toBe(2);
    });
  });

  describe('centroid', () => {
    it('finds center of rect', () => {
      const c = centroid(testRect());
      expect(c.x).toBeCloseTo(0);
      expect(c.y).toBeCloseTo(0);
    });
  });

  describe('pointOnPath', () => {
    it('gets midpoint', () => {
      const l = line({ x: 0, y: 0 }, { x: 100, y: 0 }, 2);
      const p = pointOnPathOp(l, 0.5);
      expect(p.x).toBeCloseTo(50);
    });
  });

  describe('sortShapes', () => {
    it('sorts by x position', () => {
      const s1 = rect({ x: 50, y: 0 }, 10, 10, { x: 0, y: 0 });
      const s2 = rect({ x: 0, y: 0 }, 10, 10, { x: 0, y: 0 });
      const sorted = sortShapes([s1, s2], 'x', { x: 0, y: 0 });
      expect(pathBounds(sorted[0]).x).toBeLessThan(pathBounds(sorted[1]).x);
    });
  });

  describe('stack', () => {
    it('stacks shapes horizontally', () => {
      const s1 = rect({ x: 0, y: 0 }, 20, 20, { x: 0, y: 0 });
      const s2 = rect({ x: 0, y: 0 }, 20, 20, { x: 0, y: 0 });
      const stacked = stack([s1, s2], 'right', 5);
      expect(stacked.length).toBe(2);
      const b0 = pathBounds(stacked[0]);
      const b1 = pathBounds(stacked[1]);
      expect(b1.x).toBeCloseTo(b0.x + b0.width + 5);
    });
  });

  describe('compound', () => {
    it('unites two shapes (simplified)', () => {
      const p = compound(testRect(), testSquare(), 'united', false);
      expect(p.contours.length).toBe(2);
    });
  });

  describe('roundSegments', () => {
    it('rounds corners of polygon', () => {
      const tri = polygon({ x: 0, y: 0 }, 50, 3, true);
      const rounded = roundSegments(tri, 5);
      expect(rounded.contours[0].points.length).toBeGreaterThan(3);
    });
  });
});
