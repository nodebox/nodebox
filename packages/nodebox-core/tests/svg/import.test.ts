import { describe, it, expect } from 'vitest';
import { parseSvgPathData } from '../../src/svg/import.js';

describe('SVG Path Data Parser', () => {
  it('parses M L commands', () => {
    const contours = parseSvgPathData('M 10 20 L 30 40 L 50 60');
    expect(contours.length).toBe(1);
    expect(contours[0].points.length).toBe(3);
    expect(contours[0].points[0].point).toEqual({ x: 10, y: 20 });
    expect(contours[0].points[2].point).toEqual({ x: 50, y: 60 });
    expect(contours[0].closed).toBe(false);
  });

  it('parses relative m l commands', () => {
    const contours = parseSvgPathData('m 10 20 l 30 40');
    expect(contours[0].points[0].point).toEqual({ x: 10, y: 20 });
    expect(contours[0].points[1].point).toEqual({ x: 40, y: 60 });
  });

  it('parses Z (close)', () => {
    const contours = parseSvgPathData('M 0 0 L 10 0 L 10 10 Z');
    expect(contours[0].closed).toBe(true);
  });

  it('parses H and V commands', () => {
    const contours = parseSvgPathData('M 0 0 H 10 V 20');
    expect(contours[0].points[1].point).toEqual({ x: 10, y: 0 });
    expect(contours[0].points[2].point).toEqual({ x: 10, y: 20 });
  });

  it('parses C (cubic bezier)', () => {
    const contours = parseSvgPathData('M 0 0 C 10 20 30 40 50 60');
    expect(contours[0].points.length).toBe(4);
    expect(contours[0].points[1].type).toBe('curveData');
    expect(contours[0].points[2].type).toBe('curveData');
    expect(contours[0].points[3].type).toBe('curveTo');
  });

  it('parses Q (quadratic bezier)', () => {
    const contours = parseSvgPathData('M 0 0 Q 50 50 100 0');
    expect(contours[0].points.length).toBe(3);
    expect(contours[0].points[1].type).toBe('quadData');
    expect(contours[0].points[2].type).toBe('quadTo');
  });

  it('parses S (smooth cubic)', () => {
    const contours = parseSvgPathData('M 0 0 C 10 20 30 40 50 60 S 80 90 100 100');
    expect(contours[0].points.length).toBe(7);
  });

  it('parses multiple subpaths', () => {
    const contours = parseSvgPathData('M 0 0 L 10 10 Z M 20 20 L 30 30');
    expect(contours.length).toBe(2);
    expect(contours[0].closed).toBe(true);
    expect(contours[1].closed).toBe(false);
  });

  it('parses arc commands', () => {
    const contours = parseSvgPathData('M 10 80 A 25 25 0 0 1 50 80');
    expect(contours[0].points.length).toBeGreaterThan(2);
    // Arc is converted to cubic beziers
    const lastPt = contours[0].points[contours[0].points.length - 1];
    expect(lastPt.point.x).toBeCloseTo(50, 1);
    expect(lastPt.point.y).toBeCloseTo(80, 1);
  });

  it('handles compact path data (no spaces)', () => {
    const contours = parseSvgPathData('M0,0L10,0L10,10Z');
    expect(contours[0].points.length).toBe(3);
    expect(contours[0].closed).toBe(true);
  });

  it('handles empty path', () => {
    const contours = parseSvgPathData('');
    expect(contours.length).toBe(0);
  });

  it('parses implicit L after M', () => {
    const contours = parseSvgPathData('M 0 0 10 10 20 20');
    expect(contours[0].points.length).toBe(3);
  });
});
