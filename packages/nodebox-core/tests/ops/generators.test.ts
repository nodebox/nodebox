import { describe, it, expect } from 'vitest';
import {
  rect, ellipse, arc, line, lineAngle, polygon, star,
  grid, connect, quadCurve, link, makePoint, freehand, group, doNothing,
} from '../../src/ops/generators.js';
import { pathBounds } from '../../src/geometry/path.js';

describe('Generator Operations', () => {
  describe('rect', () => {
    it('creates a rectangle centered at position', () => {
      const p = rect({ x: 0, y: 0 }, 100, 50, { x: 0, y: 0 });
      expect(p.contours.length).toBe(1);
      expect(p.contours[0].closed).toBe(true);
      expect(p.contours[0].points.length).toBe(4);
      const bounds = pathBounds(p);
      expect(bounds.x).toBeCloseTo(-50);
      expect(bounds.y).toBeCloseTo(-25);
      expect(bounds.width).toBeCloseTo(100);
      expect(bounds.height).toBeCloseTo(50);
    });

    it('creates a rounded rectangle', () => {
      const p = rect({ x: 0, y: 0 }, 100, 50, { x: 10, y: 10 });
      expect(p.contours[0].closed).toBe(true);
      // Rounded rect has more points (curves at corners)
      expect(p.contours[0].points.length).toBeGreaterThan(4);
    });

    it('handles zero dimensions', () => {
      const p = rect({ x: 0, y: 0 }, 0, 0, { x: 0, y: 0 });
      expect(p.contours.length).toBe(1);
    });
  });

  describe('ellipse', () => {
    it('creates an ellipse', () => {
      const p = ellipse({ x: 0, y: 0 }, 100, 50);
      expect(p.contours.length).toBe(1);
      expect(p.contours[0].closed).toBe(true);
      const bounds = pathBounds(p);
      expect(bounds.width).toBeCloseTo(100, 0);
      expect(bounds.height).toBeCloseTo(50, 0);
    });

    it('centered at position', () => {
      const p = ellipse({ x: 100, y: 200 }, 10, 10);
      const bounds = pathBounds(p);
      expect(bounds.x).toBeCloseTo(95, 0);
      expect(bounds.y).toBeCloseTo(195, 0);
    });
  });

  describe('arc', () => {
    it('creates a pie arc', () => {
      const p = arc({ x: 0, y: 0 }, 100, 100, 0, 90, 'pie');
      expect(p.contours[0].closed).toBe(true);
      // Should have center point + arc points + center point
      expect(p.contours[0].points.length).toBeGreaterThan(2);
    });

    it('creates an open arc', () => {
      const p = arc({ x: 0, y: 0 }, 100, 100, 0, 180, 'open');
      expect(p.contours[0].closed).toBe(false);
    });

    it('creates a chord arc', () => {
      const p = arc({ x: 0, y: 0 }, 100, 100, 0, 90, 'chord');
      expect(p.contours[0].closed).toBe(true);
    });
  });

  describe('line', () => {
    it('creates a line with 2 points', () => {
      const p = line({ x: 0, y: 0 }, { x: 100, y: 0 }, 2);
      expect(p.contours[0].points.length).toBe(2);
      expect(p.contours[0].points[0].point).toEqual({ x: 0, y: 0 });
      expect(p.contours[0].points[1].point).toEqual({ x: 100, y: 0 });
      expect(p.contours[0].closed).toBe(false);
      expect(p.fill).toBeNull(); // lines have no fill
    });

    it('creates a line with interpolated points', () => {
      const p = line({ x: 0, y: 0 }, { x: 100, y: 0 }, 5);
      expect(p.contours[0].points.length).toBe(5);
      expect(p.contours[0].points[2].point.x).toBeCloseTo(50);
    });
  });

  describe('lineAngle', () => {
    it('creates a line at an angle', () => {
      const p = lineAngle({ x: 0, y: 0 }, 0, 100, 2);
      expect(p.contours[0].points[1].point.x).toBeCloseTo(100);
      expect(p.contours[0].points[1].point.y).toBeCloseTo(0);
    });

    it('creates a line at 90 degrees', () => {
      const p = lineAngle({ x: 0, y: 0 }, 90, 100, 2);
      expect(p.contours[0].points[1].point.x).toBeCloseTo(0, 5);
      expect(p.contours[0].points[1].point.y).toBeCloseTo(100);
    });
  });

  describe('polygon', () => {
    it('creates a triangle', () => {
      const p = polygon({ x: 0, y: 0 }, 50, 3, false);
      expect(p.contours[0].points.length).toBe(3);
      expect(p.contours[0].closed).toBe(true);
    });

    it('creates a hexagon', () => {
      const p = polygon({ x: 0, y: 0 }, 50, 6, false);
      expect(p.contours[0].points.length).toBe(6);
    });

    it('aligned polygon starts at top', () => {
      const p = polygon({ x: 0, y: 0 }, 50, 4, true);
      // First point should be at top (y = -50)
      expect(p.contours[0].points[0].point.y).toBeCloseTo(-50);
    });
  });

  describe('star', () => {
    it('creates a 5-pointed star', () => {
      const p = star({ x: 0, y: 0 }, 5, 100, 50);
      expect(p.contours[0].points.length).toBe(10); // 5 outer + 5 inner
      expect(p.contours[0].closed).toBe(true);
    });
  });

  describe('grid', () => {
    it('creates a grid of points', () => {
      const points = grid(3, 2, 100, 50, { x: 0, y: 0 });
      expect(points.length).toBe(6); // 3 * 2
    });

    it('1x1 grid returns single point', () => {
      const points = grid(1, 1, 100, 100, { x: 50, y: 50 });
      expect(points.length).toBe(1);
      expect(points[0]).toEqual({ x: 50, y: 50 });
    });

    it('grid spans correct width', () => {
      const points = grid(5, 1, 100, 0, { x: 0, y: 0 });
      expect(points[0].x).toBeCloseTo(-50);
      expect(points[4].x).toBeCloseTo(50);
    });
  });

  describe('connect', () => {
    it('connects points into a path', () => {
      const p = connect([{ x: 0, y: 0 }, { x: 10, y: 0 }, { x: 10, y: 10 }], false);
      expect(p.contours[0].points.length).toBe(3);
      expect(p.contours[0].closed).toBe(false);
    });

    it('creates closed path', () => {
      const p = connect([{ x: 0, y: 0 }, { x: 10, y: 0 }, { x: 10, y: 10 }], true);
      expect(p.contours[0].closed).toBe(true);
    });

    it('handles empty points', () => {
      const p = connect([], false);
      expect(p.contours.length).toBe(0);
    });
  });

  describe('quadCurve', () => {
    it('creates a quadratic curve (output as cubic)', () => {
      // t=50 means 50% along the line (range 0-100), distance=50
      const p = quadCurve({ x: 0, y: 0 }, { x: 100, y: 0 }, 50, 50);
      expect(p.contours[0].points.length).toBe(4); // M, curveData, curveData, curveTo
      expect(p.contours[0].points[1].type).toBe('curveData');
      expect(p.contours[0].points[2].type).toBe('curveData');
      expect(p.contours[0].points[3].type).toBe('curveTo');
      expect(p.contours[0].points[3].point.x).toBeCloseTo(100);
      expect(p.contours[0].points[3].point.y).toBeCloseTo(0);
      expect(p.fill).toBeNull();
      expect(p.stroke).not.toBeNull();
    });
  });

  describe('makePoint', () => {
    it('creates a point', () => {
      expect(makePoint(3, 4)).toEqual({ x: 3, y: 4 });
    });
  });

  describe('freehand', () => {
    it('parses basic path data', () => {
      const p = freehand('M 0 0 L 10 0 L 10 10 Z');
      expect(p.contours.length).toBe(1);
      expect(p.contours[0].closed).toBe(true);
    });

    it('handles empty input', () => {
      const p = freehand('');
      expect(p.contours.length).toBe(0);
    });
  });

  describe('group', () => {
    it('returns paths as a group', () => {
      const r1 = rect({ x: 0, y: 0 }, 10, 10, { x: 0, y: 0 });
      const r2 = rect({ x: 20, y: 0 }, 10, 10, { x: 0, y: 0 });
      const grouped = group([r1, r2]);
      expect(grouped.length).toBe(2);
      expect(grouped[0]).toBe(r1);
      expect(grouped[1]).toBe(r2);
    });
  });

  describe('doNothing', () => {
    it('returns null', () => {
      expect(doNothing()).toBeNull();
    });
  });
});
