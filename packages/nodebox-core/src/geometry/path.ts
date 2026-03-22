import type { Point } from './point.js';
import type { Color } from './color.js';
import type { Rect } from './rect.js';
import type { Transform } from './transform.js';
import { transformPoint } from './transform.js';

export type PointType = 'lineTo' | 'curveTo' | 'curveData' | 'quadTo' | 'quadData';

export interface PathPoint {
  point: Point;
  type: PointType;
}

export interface Contour {
  points: PathPoint[];
  closed: boolean;
}

export interface Path {
  contours: Contour[];
  fill: Color | null;
  stroke: Color | null;
  strokeWidth: number;
}

export const DEFAULT_FILL: Color = { r: 0, g: 0, b: 0, a: 1 };

export function createPath(fill: Color | null = DEFAULT_FILL, stroke: Color | null = null, strokeWidth = 1): Path {
  return { contours: [], fill, stroke, strokeWidth };
}

export function createContour(closed = false): Contour {
  return { points: [], closed };
}

// Path builder functions — each returns a new Path (immutable)
export function moveTo(path: Path, x: number, y: number): Path {
  const newContour: Contour = {
    points: [{ point: { x, y }, type: 'lineTo' }],
    closed: false,
  };
  return { ...path, contours: [...path.contours, newContour] };
}

export function lineTo(path: Path, x: number, y: number): Path {
  if (path.contours.length === 0) return moveTo(path, x, y);
  const contours = [...path.contours];
  const last = { ...contours[contours.length - 1] };
  last.points = [...last.points, { point: { x, y }, type: 'lineTo' }];
  contours[contours.length - 1] = last;
  return { ...path, contours };
}

export function curveTo(
  path: Path,
  cx1: number, cy1: number,
  cx2: number, cy2: number,
  x: number, y: number,
): Path {
  if (path.contours.length === 0) return moveTo(path, x, y);
  const contours = [...path.contours];
  const last = { ...contours[contours.length - 1] };
  last.points = [
    ...last.points,
    { point: { x: cx1, y: cy1 }, type: 'curveData' },
    { point: { x: cx2, y: cy2 }, type: 'curveData' },
    { point: { x, y }, type: 'curveTo' },
  ];
  contours[contours.length - 1] = last;
  return { ...path, contours };
}

export function quadTo(path: Path, cx: number, cy: number, x: number, y: number): Path {
  if (path.contours.length === 0) return moveTo(path, x, y);
  const contours = [...path.contours];
  const last = { ...contours[contours.length - 1] };
  last.points = [
    ...last.points,
    { point: { x: cx, y: cy }, type: 'quadData' },
    { point: { x, y }, type: 'quadTo' },
  ];
  contours[contours.length - 1] = last;
  return { ...path, contours };
}

export function closePath(path: Path): Path {
  if (path.contours.length === 0) return path;
  const contours = [...path.contours];
  const last = { ...contours[contours.length - 1], closed: true };
  contours[contours.length - 1] = last;
  return { ...path, contours };
}

export function clonePath(path: Path): Path {
  return {
    contours: path.contours.map(c => ({
      points: c.points.map(p => ({ point: { ...p.point }, type: p.type })),
      closed: c.closed,
    })),
    fill: path.fill ? { ...path.fill } : null,
    stroke: path.stroke ? { ...path.stroke } : null,
    strokeWidth: path.strokeWidth,
  };
}

export function contourBounds(contour: Contour): Rect {
  if (contour.points.length === 0) return { x: 0, y: 0, width: 0, height: 0 };
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const pp of contour.points) {
    if (pp.point.x < minX) minX = pp.point.x;
    if (pp.point.y < minY) minY = pp.point.y;
    if (pp.point.x > maxX) maxX = pp.point.x;
    if (pp.point.y > maxY) maxY = pp.point.y;
  }
  return { x: minX, y: minY, width: maxX - minX, height: maxY - minY };
}

export function pathBounds(path: Path): Rect {
  if (path.contours.length === 0) return { x: 0, y: 0, width: 0, height: 0 };
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const contour of path.contours) {
    for (const pp of contour.points) {
      if (pp.point.x < minX) minX = pp.point.x;
      if (pp.point.y < minY) minY = pp.point.y;
      if (pp.point.x > maxX) maxX = pp.point.x;
      if (pp.point.y > maxY) maxY = pp.point.y;
    }
  }
  if (!isFinite(minX)) return { x: 0, y: 0, width: 0, height: 0 };
  return { x: minX, y: minY, width: maxX - minX, height: maxY - minY };
}

export function transformPath(path: Path, transform: Transform): Path {
  return {
    ...path,
    contours: path.contours.map(contour => ({
      ...contour,
      points: contour.points.map(pp => ({
        ...pp,
        point: transformPoint(transform, pp.point),
      })),
    })),
  };
}

// Segment-level utilities for path length and point-on-path

interface Segment {
  type: 'line' | 'cubic' | 'quad';
  points: Point[]; // [start, ...controls, end]
}

function getSegments(contour: Contour): Segment[] {
  const segments: Segment[] = [];
  const pts = contour.points;
  if (pts.length < 2) return segments;

  let i = 1;
  while (i < pts.length) {
    const prev = pts[i - 1].point;
    if (pts[i].type === 'curveData' && i + 2 < pts.length) {
      segments.push({
        type: 'cubic',
        points: [prev, pts[i].point, pts[i + 1].point, pts[i + 2].point],
      });
      i += 3;
    } else if (pts[i].type === 'quadData' && i + 1 < pts.length) {
      segments.push({
        type: 'quad',
        points: [prev, pts[i].point, pts[i + 1].point],
      });
      i += 2;
    } else {
      segments.push({ type: 'line', points: [prev, pts[i].point] });
      i += 1;
    }
  }

  // Close segment
  if (contour.closed && pts.length >= 2) {
    const last = pts[pts.length - 1].point;
    const first = pts[0].point;
    if (Math.abs(last.x - first.x) > 1e-10 || Math.abs(last.y - first.y) > 1e-10) {
      segments.push({ type: 'line', points: [last, first] });
    }
  }
  return segments;
}

function lineLength(p0: Point, p1: Point): number {
  const dx = p1.x - p0.x;
  const dy = p1.y - p0.y;
  return Math.sqrt(dx * dx + dy * dy);
}

function cubicPointAt(p0: Point, p1: Point, p2: Point, p3: Point, t: number): Point {
  const u = 1 - t;
  const uu = u * u;
  const uuu = uu * u;
  const tt = t * t;
  const ttt = tt * t;
  return {
    x: uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x,
    y: uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y,
  };
}

function quadPointAt(p0: Point, p1: Point, p2: Point, t: number): Point {
  const u = 1 - t;
  return {
    x: u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x,
    y: u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y,
  };
}

function segmentLength(seg: Segment, steps = 20): number {
  if (seg.type === 'line') return lineLength(seg.points[0], seg.points[1]);
  let length = 0;
  let prev = seg.points[0];
  for (let i = 1; i <= steps; i++) {
    const t = i / steps;
    const pt = seg.type === 'cubic'
      ? cubicPointAt(seg.points[0], seg.points[1], seg.points[2], seg.points[3], t)
      : quadPointAt(seg.points[0], seg.points[1], seg.points[2], t);
    length += lineLength(prev, pt);
    prev = pt;
  }
  return length;
}

export function contourLength(contour: Contour): number {
  return getSegments(contour).reduce((sum, seg) => sum + segmentLength(seg), 0);
}

export function pathLength(path: Path): number {
  return path.contours.reduce((sum, c) => sum + contourLength(c), 0);
}

export function pointOnContour(contour: Contour, t: number): Point {
  const segments = getSegments(contour);
  if (segments.length === 0) {
    return contour.points.length > 0 ? contour.points[0].point : { x: 0, y: 0 };
  }
  const totalLength = segments.reduce((s, seg) => s + segmentLength(seg), 0);
  if (totalLength === 0) return segments[0].points[0];

  t = Math.max(0, Math.min(1, t));
  let targetDist = t * totalLength;
  for (const seg of segments) {
    const len = segmentLength(seg);
    if (targetDist <= len) {
      const localT = len > 0 ? targetDist / len : 0;
      if (seg.type === 'line') {
        return {
          x: seg.points[0].x + (seg.points[1].x - seg.points[0].x) * localT,
          y: seg.points[0].y + (seg.points[1].y - seg.points[0].y) * localT,
        };
      } else if (seg.type === 'cubic') {
        return cubicPointAt(seg.points[0], seg.points[1], seg.points[2], seg.points[3], localT);
      } else {
        return quadPointAt(seg.points[0], seg.points[1], seg.points[2], localT);
      }
    }
    targetDist -= len;
  }
  const lastSeg = segments[segments.length - 1];
  return lastSeg.points[lastSeg.points.length - 1];
}

export function pointOnPath(path: Path, t: number): Point {
  if (path.contours.length === 0) return { x: 0, y: 0 };
  // Distribute t across all contours based on their relative lengths
  const lengths = path.contours.map(c => contourLength(c));
  const totalLength = lengths.reduce((a, b) => a + b, 0);
  if (totalLength === 0) return path.contours[0].points[0]?.point ?? { x: 0, y: 0 };

  t = Math.max(0, Math.min(1, t));
  let targetDist = t * totalLength;
  for (let i = 0; i < path.contours.length; i++) {
    if (targetDist <= lengths[i]) {
      const localT = lengths[i] > 0 ? targetDist / lengths[i] : 0;
      return pointOnContour(path.contours[i], localT);
    }
    targetDist -= lengths[i];
  }
  const lastContour = path.contours[path.contours.length - 1];
  return pointOnContour(lastContour, 1);
}

export function resampleByAmount(path: Path, amount: number): Path {
  if (amount < 2) return clonePath(path);
  return {
    ...path,
    contours: path.contours.map(contour => {
      const points: PathPoint[] = [];
      for (let i = 0; i < amount; i++) {
        const t = i / (amount - 1);
        points.push({ point: pointOnContour(contour, t), type: 'lineTo' });
      }
      return { points, closed: contour.closed };
    }),
  };
}

export function resampleByLength(path: Path, maxLength: number): Path {
  if (maxLength <= 0) return clonePath(path);
  return {
    ...path,
    contours: path.contours.map(contour => {
      const len = contourLength(contour);
      const amount = Math.max(2, Math.ceil(len / maxLength) + 1);
      const points: PathPoint[] = [];
      for (let i = 0; i < amount; i++) {
        const t = i / (amount - 1);
        points.push({ point: pointOnContour(contour, t), type: 'lineTo' });
      }
      return { points, closed: contour.closed };
    }),
  };
}

export function pathContains(path: Path, point: Point): boolean {
  // Ray casting algorithm for point-in-polygon
  let inside = false;
  for (const contour of path.contours) {
    if (!contour.closed) continue;
    const onCurve = contour.points.filter(p => p.type !== 'curveData' && p.type !== 'quadData');
    const n = onCurve.length;
    for (let i = 0, j = n - 1; i < n; j = i++) {
      const yi = onCurve[i].point.y;
      const yj = onCurve[j].point.y;
      const xi = onCurve[i].point.x;
      const xj = onCurve[j].point.x;
      if ((yi > point.y) !== (yj > point.y)
        && point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi) {
        inside = !inside;
      }
    }
  }
  return inside;
}

export function getAllPoints(path: Path): Point[] {
  const points: Point[] = [];
  for (const contour of path.contours) {
    for (const pp of contour.points) {
      if (pp.type !== 'curveData' && pp.type !== 'quadData') {
        points.push(pp.point);
      }
    }
  }
  return points;
}
