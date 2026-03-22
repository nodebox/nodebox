import { describe, it, expect } from 'vitest';
import {
  createPath, moveTo, lineTo, curveTo, quadTo, closePath, clonePath,
  pathBounds, contourBounds, transformPath, pathLength, pointOnPath,
  resampleByAmount, resampleByLength, pathContains, getAllPoints,
} from '../../src/geometry/path.js';
import { translateTransform, scaleTransform } from '../../src/geometry/transform.js';

describe('Path', () => {
  it('creates an empty path', () => {
    const p = createPath();
    expect(p.contours).toEqual([]);
    expect(p.fill).toEqual({ r: 0, g: 0, b: 0, a: 1 });
    expect(p.stroke).toBeNull();
    expect(p.strokeWidth).toBe(1);
  });

  it('builds a line path', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 100, 0);
    p = lineTo(p, 100, 100);
    expect(p.contours.length).toBe(1);
    expect(p.contours[0].points.length).toBe(3);
  });

  it('builds a cubic curve path', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = curveTo(p, 25, 50, 75, 50, 100, 0);
    expect(p.contours[0].points.length).toBe(4); // start + 2 control + end
    expect(p.contours[0].points[1].type).toBe('curveData');
    expect(p.contours[0].points[2].type).toBe('curveData');
    expect(p.contours[0].points[3].type).toBe('curveTo');
  });

  it('builds a quadratic curve path', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = quadTo(p, 50, 50, 100, 0);
    expect(p.contours[0].points.length).toBe(3);
    expect(p.contours[0].points[1].type).toBe('quadData');
    expect(p.contours[0].points[2].type).toBe('quadTo');
  });

  it('closes a path', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 100, 0);
    p = lineTo(p, 100, 100);
    p = closePath(p);
    expect(p.contours[0].closed).toBe(true);
  });

  it('calculates bounds', () => {
    let p = createPath();
    p = moveTo(p, 10, 20);
    p = lineTo(p, 50, 80);
    const bounds = pathBounds(p);
    expect(bounds).toEqual({ x: 10, y: 20, width: 40, height: 60 });
  });

  it('returns zero bounds for empty path', () => {
    const bounds = pathBounds(createPath());
    expect(bounds).toEqual({ x: 0, y: 0, width: 0, height: 0 });
  });

  it('calculates contour bounds', () => {
    const contour = {
      points: [
        { point: { x: 0, y: 0 }, type: 'lineTo' as const },
        { point: { x: 10, y: 20 }, type: 'lineTo' as const },
      ],
      closed: false,
    };
    expect(contourBounds(contour)).toEqual({ x: 0, y: 0, width: 10, height: 20 });
  });

  it('transforms a path', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 10, 0);
    const t = translateTransform(5, 5);
    const tp = transformPath(p, t);
    expect(tp.contours[0].points[0].point).toEqual({ x: 5, y: 5 });
    expect(tp.contours[0].points[1].point).toEqual({ x: 15, y: 5 });
  });

  it('clones a path', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 10, 0);
    const c = clonePath(p);
    expect(c).toEqual(p);
    expect(c).not.toBe(p);
    expect(c.contours[0]).not.toBe(p.contours[0]);
  });

  it('calculates line path length', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 3, 4);
    expect(pathLength(p)).toBeCloseTo(5, 5);
  });

  it('gets point on line path', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 10, 0);
    const mid = pointOnPath(p, 0.5);
    expect(mid.x).toBeCloseTo(5, 5);
    expect(mid.y).toBeCloseTo(0, 5);
  });

  it('resamples by amount', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 10, 0);
    const r = resampleByAmount(p, 5);
    expect(r.contours[0].points.length).toBe(5);
    expect(r.contours[0].points[0].point.x).toBeCloseTo(0, 5);
    expect(r.contours[0].points[4].point.x).toBeCloseTo(10, 5);
  });

  it('resamples by length', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 10, 0);
    const r = resampleByLength(p, 5);
    expect(r.contours[0].points.length).toBeGreaterThanOrEqual(3);
  });

  it('detects point inside closed path', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = lineTo(p, 10, 0);
    p = lineTo(p, 10, 10);
    p = lineTo(p, 0, 10);
    p = closePath(p);
    expect(pathContains(p, { x: 5, y: 5 })).toBe(true);
    expect(pathContains(p, { x: 15, y: 5 })).toBe(false);
  });

  it('gets all on-curve points', () => {
    let p = createPath();
    p = moveTo(p, 0, 0);
    p = curveTo(p, 10, 10, 20, 10, 30, 0);
    const points = getAllPoints(p);
    expect(points.length).toBe(2); // start and end, not control points
  });

  it('lineTo on empty path creates moveTo', () => {
    let p = createPath();
    p = lineTo(p, 10, 20);
    expect(p.contours.length).toBe(1);
    expect(p.contours[0].points[0].point).toEqual({ x: 10, y: 20 });
  });
});
