// Geometry to coverage.
//
// A scanline filler with the nonzero winding rule, sampled at `samples` rows per pixel and
// integrated exactly across x. That gives clean edges without a canvas, which matters because the
// core has to rasterize the same way in a browser, in Electron and in Node, and a test has to be
// able to say the two agree.
//
// Strokes go through the same filler: every segment becomes a quad and every joint a small
// polygon, all wound the same way, so the nonzero rule unions them instead of cancelling.

import { Contour } from "./contour";
import { Geometry, Path } from "./path";
import { Point } from "./point";
import { Raster } from "./raster";
import { fromG, isGShape } from "./to-g";

export interface RasterizeOptions {
  width: number;
  height: number;
  /** Scene units mapped onto the raster: the rectangle the raster covers. */
  left?: number;
  top?: number;
  /** The width of the scene rectangle; defaults to the raster width, so one unit is one pixel. */
  spanX?: number;
  spanY?: number;
  /** Subsample rows per pixel. 4 means 4 rows, each integrated exactly across x. */
  samples?: number;
  /** The coverage a fully covered pixel gets. */
  tone?: number;
}

interface Edge {
  x0: number;
  y0: number;
  x1: number;
  y1: number;
  /** +1 when the edge runs down the raster, -1 when it runs up. */
  winding: number;
}

/** Everything the core can draw, as flat polylines in scene units. */
export function toContours(value: unknown): Contour[] {
  const out: Contour[] = [];
  collectContours(value, out);
  return out;
}

function collectContours(value: unknown, out: Contour[]): void {
  if (value === null || value === undefined) return;
  if (Array.isArray(value)) {
    for (const item of value) collectContours(item, out);
    return;
  }
  if (value instanceof Contour) {
    out.push(value.flattened());
    return;
  }
  if (value instanceof Path) {
    for (const contour of value.flattened().contours) out.push(contour);
    return;
  }
  if (value instanceof Geometry) {
    for (const path of value.paths) collectContours(path, out);
    return;
  }
  if (isGShape(value)) {
    collectContours(fromG(value), out);
    return;
  }
}

/** Fill the geometry with the nonzero winding rule. */
export function rasterizeFill(value: unknown, options: RasterizeOptions): Raster {
  return fillContours(toContours(value), options, true);
}

/** Stroke the geometry: every segment as a quad, every joint and cap as a round polygon. */
export function rasterizeStroke(value: unknown, strokeWidth: number, options: RasterizeOptions): Raster {
  const half = Math.max(strokeWidth, 0) / 2;
  if (half === 0) return new Raster(options.width, options.height, 1);
  const outline: Contour[] = [];
  for (const contour of toContours(value)) {
    const points = contour.closed ? [...contour.points, contour.points[0]] : contour.points;
    if (points.length < 2) {
      if (points.length === 1) outline.push(discContour(points[0], half));
      continue;
    }
    for (let i = 0; i < points.length - 1; i += 1) {
      const quad = segmentQuad(points[i], points[i + 1], half);
      if (quad) outline.push(quad);
    }
    // Round joins and caps, so a corner does not open up and an end is not square.
    for (const point of points) outline.push(discContour(point, half));
  }
  return fillContours(outline, options, true);
}

function segmentQuad(a: Point, b: Point, half: number): Contour | undefined {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  const length = Math.hypot(dx, dy);
  if (length === 0) return undefined;
  const nx = (-dy / length) * half;
  const ny = (dx / length) * half;
  return new Contour(
    [
      new Point(a.x + nx, a.y + ny),
      new Point(b.x + nx, b.y + ny),
      new Point(b.x - nx, b.y - ny),
      new Point(a.x - nx, a.y - ny),
    ],
    true,
  );
}

const DISC_SIDES = 12;

function discContour(center: Point, radius: number): Contour {
  const points: Point[] = [];
  for (let i = 0; i < DISC_SIDES; i += 1) {
    const angle = (i / DISC_SIDES) * Math.PI * 2;
    points.push(new Point(center.x + Math.cos(angle) * radius, center.y + Math.sin(angle) * radius));
  }
  return new Contour(points, true);
}

/** Coverage of a set of closed polylines, with the nonzero winding rule. */
export function fillContours(contours: readonly Contour[], options: RasterizeOptions, closeOpen = false): Raster {
  const width = Math.max(1, Math.round(options.width));
  const height = Math.max(1, Math.round(options.height));
  const raster = new Raster(width, height, 1);
  const samples = Math.max(1, Math.round(options.samples ?? 4));
  const tone = options.tone ?? 1;
  const spanX = options.spanX ?? width;
  const spanY = options.spanY ?? height;
  const left = options.left ?? 0;
  const top = options.top ?? 0;
  const scaleX = width / spanX;
  const scaleY = height / spanY;

  const edges: Edge[] = [];
  let minY = Infinity;
  let maxY = -Infinity;
  for (const contour of contours) {
    const points = contour.points;
    const count = points.length;
    if (count < 2) continue;
    const last = contour.closed || closeOpen ? count : count - 1;
    for (let i = 0; i < last; i += 1) {
      const a = points[i];
      const b = points[(i + 1) % count];
      const x0 = (a.x - left) * scaleX;
      const y0 = (a.y - top) * scaleY;
      const x1 = (b.x - left) * scaleX;
      const y1 = (b.y - top) * scaleY;
      if (y0 === y1) continue;
      edges.push(y0 < y1 ? { x0, y0, x1, y1, winding: 1 } : { x0: x1, y0: y1, x1: x0, y1: y0, winding: -1 });
      if (Math.min(y0, y1) < minY) minY = Math.min(y0, y1);
      if (Math.max(y0, y1) > maxY) maxY = Math.max(y0, y1);
    }
  }
  if (edges.length === 0) return raster;

  const firstRow = Math.max(0, Math.floor(minY * samples));
  const lastRow = Math.min(height * samples - 1, Math.ceil(maxY * samples));
  const weight = tone / samples;

  // An active edge list: edges enter at the row where they start and leave where they end, so a
  // row only walks the edges that cross it. A page of paper fibres is tens of thousands of edges
  // and scanning all of them per row is what makes a naive filler slow.
  edges.sort((a, b) => a.y0 - b.y0);
  let next = 0;
  let active: Edge[] = [];
  const crossings: { x: number; winding: number }[] = [];

  for (let row = firstRow; row <= lastRow; row += 1) {
    const y = (row + 0.5) / samples;
    while (next < edges.length && edges[next].y0 <= y) active.push(edges[next++]);
    if (active.length === 0) {
      // Skip ahead to the next edge instead of walking empty rows one at a time.
      if (next >= edges.length) break;
      row = Math.max(row, Math.floor(edges[next].y0 * samples) - 1);
      continue;
    }
    let live = 0;
    crossings.length = 0;
    for (const edge of active) {
      if (y >= edge.y1) continue;
      active[live++] = edge;
      if (y < edge.y0) continue;
      const t = (y - edge.y0) / (edge.y1 - edge.y0);
      crossings.push({ x: edge.x0 + (edge.x1 - edge.x0) * t, winding: edge.winding });
    }
    active.length = live;
    if (crossings.length < 2) continue;
    crossings.sort((a, b) => a.x - b.x);
    let winding = 0;
    const rowOffset = Math.floor(row / samples) * width;
    for (let i = 0; i < crossings.length - 1; i += 1) {
      winding += crossings[i].winding;
      if (winding === 0) continue;
      addSpan(raster.data, rowOffset, width, crossings[i].x, crossings[i + 1].x, weight);
    }
  }
  return raster;
}

/** Add coverage over [x0, x1) of one subsample row, exact at the fractional ends. */
function addSpan(data: Float32Array, rowOffset: number, width: number, x0: number, x1: number, weight: number): void {
  if (x1 <= x0) return;
  const start = Math.max(0, x0);
  const end = Math.min(width, x1);
  if (end <= start) return;
  const first = Math.floor(start);
  const last = Math.ceil(end) - 1;
  if (first === last) {
    data[rowOffset + first] += (end - start) * weight;
    return;
  }
  data[rowOffset + first] += (first + 1 - start) * weight;
  for (let x = first + 1; x < last; x += 1) data[rowOffset + x] += weight;
  data[rowOffset + last] += (end - last) * weight;
}
