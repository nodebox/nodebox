import type { Point } from '../geometry/point.js';
import type { Color } from '../geometry/color.js';
import type { Path, PathPoint, Contour } from '../geometry/path.js';
import type { Rect } from '../geometry/rect.js';
import {
  pathBounds, transformPath, clonePath, createPath, pointOnPath,
  resampleByAmount, resampleByLength, pathContains, getAllPoints,
  pointOnContour, contourLength,
} from '../geometry/path.js';
import {
  identityTransform, translateTransform, rotateTransform,
  scaleTransform, skewTransform, multiplyTransforms, transformPoint,
} from '../geometry/transform.js';
import { distanceBetween } from '../geometry/point.js';

function pp(x: number, y: number, type: PathPoint['type'] = 'lineTo'): PathPoint {
  return { point: { x, y }, type };
}

// ─── align ─────────────────────────────────────────────
export function align(shape: Path, position: Point, halign: string, valign: string): Path {
  const bounds = pathBounds(shape);
  let dx = 0, dy = 0;
  if (halign === 'left') dx = position.x - bounds.x;
  else if (halign === 'center') dx = position.x - (bounds.x + bounds.width / 2);
  else if (halign === 'right') dx = position.x - (bounds.x + bounds.width);

  if (valign === 'top') dy = position.y - bounds.y;
  else if (valign === 'middle') dy = position.y - (bounds.y + bounds.height / 2);
  else if (valign === 'bottom') dy = position.y - (bounds.y + bounds.height);

  return transformPath(shape, translateTransform(dx, dy));
}

// ─── colorize ──────────────────────────────────────────
export function colorize(shape: Path, fill: Color | null, stroke: Color | null, strokeWidth: number): Path {
  return { ...shape, fill, stroke, strokeWidth };
}

// ─── copy ──────────────────────────────────────────────
export function copy(
  shape: Path, copies: number, order: string,
  translate: Point, rotate: number, scale: Point,
): Path[] {
  const results: Path[] = [];
  const n = Math.max(1, Math.round(copies));
  for (let i = 0; i < n; i++) {
    let t = identityTransform();
    if (order === 'tsr' || order === '' || order === undefined) {
      t = multiplyTransforms(t, translateTransform(translate.x * i, translate.y * i));
      t = multiplyTransforms(t, scaleTransform(
        Math.pow(scale.x / 100, i),
        Math.pow(scale.y / 100, i),
      ));
      t = multiplyTransforms(t, rotateTransform(rotate * i));
    } else if (order === 'trs') {
      t = multiplyTransforms(t, translateTransform(translate.x * i, translate.y * i));
      t = multiplyTransforms(t, rotateTransform(rotate * i));
      t = multiplyTransforms(t, scaleTransform(
        Math.pow(scale.x / 100, i),
        Math.pow(scale.y / 100, i),
      ));
    } else if (order === 'str') {
      t = multiplyTransforms(t, scaleTransform(
        Math.pow(scale.x / 100, i),
        Math.pow(scale.y / 100, i),
      ));
      t = multiplyTransforms(t, translateTransform(translate.x * i, translate.y * i));
      t = multiplyTransforms(t, rotateTransform(rotate * i));
    } else if (order === 'srt') {
      t = multiplyTransforms(t, scaleTransform(
        Math.pow(scale.x / 100, i),
        Math.pow(scale.y / 100, i),
      ));
      t = multiplyTransforms(t, rotateTransform(rotate * i));
      t = multiplyTransforms(t, translateTransform(translate.x * i, translate.y * i));
    } else if (order === 'rts') {
      t = multiplyTransforms(t, rotateTransform(rotate * i));
      t = multiplyTransforms(t, translateTransform(translate.x * i, translate.y * i));
      t = multiplyTransforms(t, scaleTransform(
        Math.pow(scale.x / 100, i),
        Math.pow(scale.y / 100, i),
      ));
    } else if (order === 'rst') {
      t = multiplyTransforms(t, rotateTransform(rotate * i));
      t = multiplyTransforms(t, scaleTransform(
        Math.pow(scale.x / 100, i),
        Math.pow(scale.y / 100, i),
      ));
      t = multiplyTransforms(t, translateTransform(translate.x * i, translate.y * i));
    }
    results.push(transformPath(shape, t));
  }
  return results;
}

// ─── fit ───────────────────────────────────────────────
export function fit(
  shape: Path, position: Point, width: number, height: number, keepProportions: boolean,
): Path {
  const bounds = pathBounds(shape);
  if (bounds.width === 0 || bounds.height === 0) return clonePath(shape);

  let sx = width / bounds.width;
  let sy = height / bounds.height;
  if (keepProportions) {
    const s = Math.min(sx, sy);
    sx = s;
    sy = s;
  }
  const newWidth = bounds.width * sx;
  const newHeight = bounds.height * sy;
  const dx = position.x - newWidth / 2;
  const dy = position.y - newHeight / 2;

  let t = identityTransform();
  t = multiplyTransforms(t, translateTransform(-bounds.x, -bounds.y));
  t = multiplyTransforms(scaleTransform(sx, sy), t);
  t = multiplyTransforms(translateTransform(dx, dy), t);
  return transformPath(shape, t);
}

// ─── fitTo ─────────────────────────────────────────────
export function fitTo(shape: Path, bounding: Path, keepProportions: boolean): Path {
  const bounds = pathBounds(bounding);
  return fit(shape,
    { x: bounds.x + bounds.width / 2, y: bounds.y + bounds.height / 2 },
    bounds.width, bounds.height, keepProportions,
  );
}

// ─── translate ─────────────────────────────────────────
export function translateOp(shape: Path, translate: Point): Path {
  return transformPath(shape, translateTransform(translate.x, translate.y));
}

// ─── rotate ────────────────────────────────────────────
export function rotateOp(shape: Path, angle: number, origin: Point): Path {
  const bounds = pathBounds(shape);
  const cx = origin.x !== 0 ? origin.x : bounds.x + bounds.width / 2;
  const cy = origin.y !== 0 ? origin.y : bounds.y + bounds.height / 2;

  let t = identityTransform();
  t = multiplyTransforms(translateTransform(-cx, -cy), t);
  t = multiplyTransforms(rotateTransform(angle), t);
  t = multiplyTransforms(translateTransform(cx, cy), t);

  // Compose: translate to origin, rotate, translate back
  return transformPath(shape, t);
}

// ─── scale ─────────────────────────────────────────────
export function scaleOp(shape: Path, scale: Point, origin: Point): Path {
  const bounds = pathBounds(shape);
  const cx = origin.x !== 0 ? origin.x : bounds.x + bounds.width / 2;
  const cy = origin.y !== 0 ? origin.y : bounds.y + bounds.height / 2;
  const sx = scale.x / 100;
  const sy = scale.y / 100;

  let t = identityTransform();
  t = multiplyTransforms(translateTransform(-cx, -cy), t);
  t = multiplyTransforms(scaleTransform(sx, sy), t);
  t = multiplyTransforms(translateTransform(cx, cy), t);

  return transformPath(shape, t);
}

// ─── skew ──────────────────────────────────────────────
export function skewOp(shape: Path, skew: Point, origin: Point): Path {
  const bounds = pathBounds(shape);
  const cx = origin.x !== 0 ? origin.x : bounds.x + bounds.width / 2;
  const cy = origin.y !== 0 ? origin.y : bounds.y + bounds.height / 2;

  let t = identityTransform();
  t = multiplyTransforms(translateTransform(-cx, -cy), t);
  t = multiplyTransforms(skewTransform(skew.x, skew.y), t);
  t = multiplyTransforms(translateTransform(cx, cy), t);

  return transformPath(shape, t);
}

// ─── reflect ───────────────────────────────────────────
export function reflect(shape: Path, position: Point, angle: number, keepOriginal: boolean): Path | Path[] {
  const rad = angle * Math.PI / 180;
  const cos2 = Math.cos(2 * rad);
  const sin2 = Math.sin(2 * rad);

  const reflected = transformPath(shape, {
    m: [cos2, sin2, sin2, -cos2, position.x * (1 - cos2) - position.y * sin2,
      position.y * (1 + cos2) - position.x * sin2],
  });

  if (keepOriginal) return [clonePath(shape), reflected];
  return reflected;
}

// ─── snap ──────────────────────────────────────────────
export function snap(shape: Path, distance: number, strength: number, position: Point): Path {
  if (distance <= 0) return clonePath(shape);
  return {
    ...shape,
    contours: shape.contours.map(contour => ({
      ...contour,
      points: contour.points.map(pt => {
        const dx = pt.point.x - position.x;
        const dy = pt.point.y - position.y;
        const snapX = Math.round(dx / distance) * distance + position.x;
        const snapY = Math.round(dy / distance) * distance + position.y;
        const s = strength / 100;
        return {
          ...pt,
          point: {
            x: pt.point.x + (snapX - pt.point.x) * s,
            y: pt.point.y + (snapY - pt.point.y) * s,
          },
        };
      }),
    })),
  };
}

// ─── resample ──────────────────────────────────────────
export function resample(
  shape: Path, method: string, length: number, points: number, perContour: boolean,
): Path {
  if (method === 'length') return resampleByLength(shape, length);
  return resampleByAmount(shape, points);
}

// ─── wiggle ────────────────────────────────────────────
export function wiggle(shape: Path, scope: string, offset: Point, seed: number): Path[] {
  // Simple seeded pseudo-random
  function seededRandom(s: number): () => number {
    let state = s;
    return () => {
      state = (state * 1664525 + 1013904223) & 0xffffffff;
      return (state >>> 0) / 0xffffffff;
    };
  }
  const rand = seededRandom(seed);

  if (scope === 'points') {
    return [{
      ...shape,
      contours: shape.contours.map(contour => ({
        ...contour,
        points: contour.points.map(pt => ({
          ...pt,
          point: {
            x: pt.point.x + (rand() - 0.5) * 2 * offset.x,
            y: pt.point.y + (rand() - 0.5) * 2 * offset.y,
          },
        })),
      })),
    }];
  } else if (scope === 'contours') {
    return [{
      ...shape,
      contours: shape.contours.map(contour => {
        const dx = (rand() - 0.5) * 2 * offset.x;
        const dy = (rand() - 0.5) * 2 * offset.y;
        return {
          ...contour,
          points: contour.points.map(pt => ({
            ...pt,
            point: { x: pt.point.x + dx, y: pt.point.y + dy },
          })),
        };
      }),
    }];
  }
  // scope === 'paths'
  const dx = (rand() - 0.5) * 2 * offset.x;
  const dy = (rand() - 0.5) * 2 * offset.y;
  return [transformPath(shape, translateTransform(dx, dy))];
}

// ─── ungroup ───────────────────────────────────────────
export function ungroup(shape: Path): Path[] {
  return shape.contours.map(contour => ({
    contours: [contour],
    fill: shape.fill,
    stroke: shape.stroke,
    strokeWidth: shape.strokeWidth,
  }));
}

// ─── scatter ───────────────────────────────────────────
export function scatter(shape: Path, amount: number, seed: number): Point[] {
  const bounds = pathBounds(shape);
  function seededRandom(s: number): () => number {
    let state = s;
    return () => {
      state = (state * 1664525 + 1013904223) & 0xffffffff;
      return (state >>> 0) / 0xffffffff;
    };
  }
  const rand = seededRandom(seed);
  const points: Point[] = [];
  let attempts = 0;
  const maxAttempts = amount * 100;
  while (points.length < amount && attempts < maxAttempts) {
    const p: Point = {
      x: bounds.x + rand() * bounds.width,
      y: bounds.y + rand() * bounds.height,
    };
    if (pathContains(shape, p)) {
      points.push(p);
    }
    attempts++;
  }
  return points;
}

// ─── centroid ──────────────────────────────────────────
export function centroid(shape: Path): Point {
  const points = getAllPoints(shape);
  if (points.length === 0) return { x: 0, y: 0 };
  let sx = 0, sy = 0;
  for (const p of points) {
    sx += p.x;
    sy += p.y;
  }
  return { x: sx / points.length, y: sy / points.length };
}

// ─── pointOnPathOp ─────────────────────────────────────
export function pointOnPathOp(shape: Path, t: number): Point {
  return pointOnPath(shape, t);
}

// ─── sort ──────────────────────────────────────────────
export function sortShapes(shapes: Path[], orderBy: string, position: Point): Path[] {
  const sorted = [...shapes];
  sorted.sort((a, b) => {
    const ba = pathBounds(a);
    const bb = pathBounds(b);
    const ca = { x: ba.x + ba.width / 2, y: ba.y + ba.height / 2 };
    const cb = { x: bb.x + bb.width / 2, y: bb.y + bb.height / 2 };

    if (orderBy === 'x') return ca.x - cb.x;
    if (orderBy === 'y') return ca.y - cb.y;
    if (orderBy === 'distance') {
      return distanceBetween(position, ca) - distanceBetween(position, cb);
    }
    if (orderBy === 'angle') {
      const angleA = Math.atan2(ca.y - position.y, ca.x - position.x);
      const angleB = Math.atan2(cb.y - position.y, cb.x - position.x);
      return angleA - angleB;
    }
    if (orderBy === 'area') {
      return (ba.width * ba.height) - (bb.width * bb.height);
    }
    return 0;
  });
  return sorted;
}

// ─── stack ─────────────────────────────────────────────
export function stack(shapes: Path[], direction: string, margin: number): Path[] {
  if (shapes.length === 0) return [];
  const results: Path[] = [];
  let offset = 0;

  for (const shape of shapes) {
    const bounds = pathBounds(shape);
    const isHoriz = direction === 'right' || direction === 'left';
    const dx = isHoriz ? offset - bounds.x : 0;
    const dy = !isHoriz ? offset - bounds.y : 0;
    results.push(transformPath(shape, translateTransform(dx, dy)));
    offset += (isHoriz ? bounds.width : bounds.height) + margin;
  }
  return results;
}

// ─── distribute ────────────────────────────────────────
export function distribute(shapes: Path[], horizontal: string, vertical: string): Path[] {
  if (shapes.length <= 1) return shapes.map(clonePath);

  const sortedH = [...shapes].sort((a, b) => pathBounds(a).x - pathBounds(b).x);
  const sortedV = [...shapes].sort((a, b) => pathBounds(a).y - pathBounds(b).y);

  const results = shapes.map(clonePath);

  if (horizontal === 'distribute') {
    const first = pathBounds(sortedH[0]);
    const last = pathBounds(sortedH[sortedH.length - 1]);
    const totalWidth = (last.x + last.width) - first.x;
    const shapesWidth = sortedH.reduce((s, p) => s + pathBounds(p).width, 0);
    const gap = (totalWidth - shapesWidth) / (sortedH.length - 1);

    let x = first.x;
    for (const shape of sortedH) {
      const bounds = pathBounds(shape);
      const idx = shapes.indexOf(shape);
      const dx = x - bounds.x;
      results[idx] = transformPath(results[idx], translateTransform(dx, 0));
      x += bounds.width + gap;
    }
  }

  if (vertical === 'distribute') {
    const first = pathBounds(sortedV[0]);
    const last = pathBounds(sortedV[sortedV.length - 1]);
    const totalHeight = (last.y + last.height) - first.y;
    const shapesHeight = sortedV.reduce((s, p) => s + pathBounds(p).height, 0);
    const gap = (totalHeight - shapesHeight) / (sortedV.length - 1);

    let y = first.y;
    for (const shape of sortedV) {
      const bounds = pathBounds(shape);
      const idx = shapes.indexOf(shape);
      const dy = y - bounds.y;
      results[idx] = transformPath(results[idx], translateTransform(0, dy));
      y += bounds.height + gap;
    }
  }

  return results;
}

// ─── deletePaths ───────────────────────────────────────
export function deletePaths(
  shape: Path, bounding: Path, scope: string, operation: string,
): Path {
  const inBounding = (p: Point) => pathContains(bounding, p);

  if (scope === 'points') {
    return {
      ...shape,
      contours: shape.contours.map(contour => ({
        ...contour,
        points: contour.points.filter(pt => {
          const inside = inBounding(pt.point);
          return operation === 'selected' ? !inside : inside;
        }),
      })).filter(c => c.points.length > 0),
    };
  }

  // scope === 'paths'
  const keep = shape.contours.filter(contour => {
    const center = contourCenter(contour);
    const inside = inBounding(center);
    return operation === 'selected' ? !inside : inside;
  });
  return { ...shape, contours: keep };
}

function contourCenter(contour: Contour): Point {
  if (contour.points.length === 0) return { x: 0, y: 0 };
  let sx = 0, sy = 0;
  for (const pt of contour.points) {
    sx += pt.point.x;
    sy += pt.point.y;
  }
  return { x: sx / contour.points.length, y: sy / contour.points.length };
}

// ─── roundSegments ─────────────────────────────────────
export function roundSegments(shape: Path, d: number): Path {
  // Round each sharp corner by inserting bezier arcs
  return {
    ...shape,
    contours: shape.contours.map(contour => {
      if (contour.points.length < 3) return contour;
      const hasOnlyCurves = contour.points.some(p => p.type === 'curveData' || p.type === 'curveTo');
      if (hasOnlyCurves) return contour; // skip already curved contours

      const n = contour.points.length;
      const newPoints: PathPoint[] = [];

      for (let i = 0; i < n; i++) {
        const prev = contour.points[(i - 1 + n) % n];
        const curr = contour.points[i];
        const next = contour.points[(i + 1) % n];

        if (!contour.closed && (i === 0 || i === n - 1)) {
          newPoints.push(curr);
          continue;
        }

        const d1 = distanceBetween(prev.point, curr.point);
        const d2 = distanceBetween(curr.point, next.point);
        const t1 = Math.min(d, d1 / 2) / d1;
        const t2 = Math.min(d, d2 / 2) / d2;

        const p1: Point = {
          x: curr.point.x + (prev.point.x - curr.point.x) * t1,
          y: curr.point.y + (prev.point.y - curr.point.y) * t1,
        };
        const p2: Point = {
          x: curr.point.x + (next.point.x - curr.point.x) * t2,
          y: curr.point.y + (next.point.y - curr.point.y) * t2,
        };

        newPoints.push(pp(p1.x, p1.y));
        newPoints.push(pp(curr.point.x, curr.point.y, 'curveData'));
        newPoints.push(pp(curr.point.x, curr.point.y, 'curveData'));
        newPoints.push(pp(p2.x, p2.y, 'curveTo'));
      }

      return { ...contour, points: newPoints };
    }),
  };
}

// ─── shapeOnPath ───────────────────────────────────────
export function shapeOnPath(
  shapes: Path[], path: Path, amount: number, alignment: string,
  spacing: number, margin: number, baselineOffset: number,
): Path[] {
  if (shapes.length === 0) return [];
  const n = Math.max(1, amount);
  const results: Path[] = [];

  for (let i = 0; i < n; i++) {
    const t = n <= 1 ? 0.5 : margin + (1 - 2 * margin) * (i / (n - 1));
    const pt = pointOnPath(path, Math.max(0, Math.min(1, t)));
    const shape = shapes[i % shapes.length];
    const bounds = pathBounds(shape);
    const dx = pt.x - (bounds.x + bounds.width / 2);
    const dy = pt.y - (bounds.y + bounds.height / 2) + baselineOffset;
    results.push(transformPath(shape, translateTransform(dx, dy)));
  }
  return results;
}

// ─── compound (boolean ops) — simplified placeholder ───
export function compound(
  shape1: Path, shape2: Path, operation: string, _invertDifference: boolean,
): Path {
  // Boolean path operations are very complex for bezier paths.
  // This is a simplified placeholder — for full implementation,
  // a library like paper.js path operations would be needed.
  if (operation === 'united') {
    return {
      contours: [...shape1.contours, ...shape2.contours],
      fill: shape1.fill,
      stroke: shape1.stroke,
      strokeWidth: shape1.strokeWidth,
    };
  }
  // For subtracted/intersected, return shape1 as placeholder
  return clonePath(shape1);
}
