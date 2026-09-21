// The pixel layer (namespace "image").
//
// One kernel per node, and a mode that picks a different algorithm is a different node: `add`,
// `multiply`, `maximum` and `minimum` are four nodes rather than one with an operator menu. What
// is left is composed in the graph, which is why the halftone and the print are networks in
// image.ndbx and not code here.
//
// The numerics follow riso-drawing (github.com/fdb/riso-drawing) exactly, down to the generator
// and the noise grid, so a print made there and a print made here with the same seed are the same
// picture, and so a WGSL twin of a kernel can be checked against this one.

import { Color } from "../graphics/color";
import { Point } from "../graphics/point";
import { Raster, RasterChannels, combine, commonSize, generate, map } from "../graphics/raster";
import { rasterizeFill, rasterizeStroke } from "../graphics/rasterize";
import { Contour } from "../graphics/contour";
import { Path } from "../graphics/path";
import { JavaScriptLibrary } from "../runtime/function-repository";

/** The generator riso-drawing uses; the k-th value is a pure function of the seed and k. */
export function mulberry32(seed: number): () => number {
  let a = seed | 0;
  return () => {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** Tiling value noise on an n by n grid, smoothstep between the corners. */
export function makeNoise(n: number, rng: () => number): (x: number, y: number) => number {
  const grid = new Float32Array(n * n);
  for (let i = 0; i < n * n; i += 1) grid[i] = rng();
  return (x, y) => {
    const xi = Math.floor(x);
    const yi = Math.floor(y);
    const fx = x - xi;
    const fy = y - yi;
    const sx = fx * fx * (3 - 2 * fx);
    const sy = fy * fy * (3 - 2 * fy);
    const i0 = ((xi % n) + n) % n;
    const i1 = (i0 + 1) % n;
    const j0 = ((yi % n) + n) % n;
    const j1 = (j0 + 1) % n;
    const a = grid[j0 * n + i0];
    const b = grid[j0 * n + i1];
    const c = grid[j1 * n + i0];
    const d = grid[j1 * n + i1];
    return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy;
  };
}

const NOISE_GRID = 32;
const clamp01 = (v: number) => (v < 0 ? 0 : v > 1 ? 1 : v);
const size = (width: number, height: number) => ({
  width: Math.max(1, Math.round(width)),
  height: Math.max(1, Math.round(height)),
});

// ---- sources ----

/** Fill the shape: coverage of the geometry under the nonzero winding rule. */
export function fill(shape: unknown, width: number, height: number, scale: number, samples: number): Raster {
  const s = size(width, height);
  return rasterizeFill(shape, {
    ...s,
    left: -s.width / 2 / (scale || 1),
    top: -s.height / 2 / (scale || 1),
    spanX: s.width / (scale || 1),
    spanY: s.height / (scale || 1),
    samples,
  });
}

/** Stroke the shape with a round join and cap. */
export function stroke(
  shape: unknown,
  strokeWidth: number,
  width: number,
  height: number,
  scale: number,
  samples: number,
): Raster {
  const s = size(width, height);
  return rasterizeStroke(shape, strokeWidth, {
    ...s,
    left: -s.width / 2 / (scale || 1),
    top: -s.height / 2 / (scale || 1),
    spanX: s.width / (scale || 1),
    spanY: s.height / (scale || 1),
    samples,
  });
}

export function constant(width: number, height: number, value: number): Raster {
  const s = size(width, height);
  const raster = new Raster(s.width, s.height, 1);
  raster.data.fill(value);
  return raster;
}

/**
 * The halftone threshold: a round dot that grows past 1 towards the corners of its cell, so
 * comparing a tone against it gives a dot whose area follows the tone.
 */
export function screen(width: number, height: number, cell: number, angle: number): Raster {
  const s = size(width, height);
  const a = (angle * Math.PI) / 180;
  const c = Math.cos(a);
  const sn = Math.sin(a);
  const size1 = Math.max(cell, 0.001);
  return generate(s.width, s.height, 1, (x, y) => {
    const xr = (x * c + y * sn) / size1;
    const yr = (-x * sn + y * c) / size1;
    const u = xr - Math.floor(xr) - 0.5;
    const v = yr - Math.floor(yr) - 0.5;
    return (Math.PI * (u * u + v * v)) / 0.9;
  });
}

export function noise(width: number, height: number, scale: number, seed: number): Raster {
  const s = size(width, height);
  const sample = makeNoise(NOISE_GRID, mulberry32(1000 + Math.round(seed) * 7919));
  const step = Math.max(scale, 0.001);
  return generate(s.width, s.height, 1, (x, y) => sample(x / step, y / step));
}

/** White noise in square blocks: the grain of the paper and of the press. */
export function grain(width: number, height: number, blockSize: number, seed: number): Raster {
  const s = size(width, height);
  const block = Math.max(1, Math.round(blockSize));
  const rng = mulberry32((Math.round(seed) * 2654435761) >>> 0);
  if (block === 1) {
    const raster = new Raster(s.width, s.height, 1);
    for (let i = 0; i < raster.data.length; i += 1) raster.data[i] = rng();
    return raster;
  }
  const columns = Math.ceil(s.width / block);
  const rows = Math.ceil(s.height / block);
  const cells = new Float32Array(columns * rows);
  for (let i = 0; i < cells.length; i += 1) cells[i] = rng();
  return generate(s.width, s.height, 1, (x, y) => cells[Math.floor(y / block) * columns + Math.floor(x / block)]);
}

/** Paper: a flat tone with short fibres drawn into it, the ground a print sits on. */
export function paper(width: number, height: number, color: Color, fibres: number, seed: number): Raster {
  const s = size(width, height);
  const raster = new Raster(s.width, s.height, 3);
  for (let i = 0; i < raster.data.length; i += 3) {
    raster.data[i] = color.r;
    raster.data[i + 1] = color.g;
    raster.data[i + 2] = color.b;
  }
  if (fibres <= 0) return raster;
  const rng = mulberry32(Math.round(seed) || 1);
  const count = Math.round(900 * fibres);
  const unit = Math.min(s.width, s.height) / 1080;
  const contours: Contour[] = [];
  const strengths: number[] = [];
  for (let i = 0; i < count; i += 1) {
    const x = rng() * s.width;
    const y = rng() * s.height;
    const length = (6 + rng() * 18) * unit;
    const a = rng() * Math.PI * 2;
    const cx = x + Math.cos(a + 0.25) * length * 0.5;
    const cy = y + Math.sin(a + 0.25) * length * 0.5;
    const ex = x + Math.cos(a) * length;
    const ey = y + Math.sin(a) * length;
    const path = new Path();
    path.moveto(x, y);
    // A quadratic as the cubic the path model stores.
    path.curveto(
      x + (2 / 3) * (cx - x),
      y + (2 / 3) * (cy - y),
      ex + (2 / 3) * (cx - ex),
      ey + (2 / 3) * (cy - ey),
      ex,
      ey,
    );
    contours.push(...path.flattened().contours);
    strengths.push(0.025 + rng() * 0.04);
  }
  // One pass over all the fibres, then a single darkening: a fibre is faint enough that the
  // difference between compositing them one by one and in one go is below a printed step.
  const mean = strengths.reduce((total, v) => total + v, 0) / (strengths.length || 1);
  const ink = rasterizeStroke(contours, Math.max(0.6 * unit, 0.5), { ...s, samples: 2 });
  const fibre = [110 / 255, 100 / 255, 80 / 255];
  for (let i = 0, p = 0; i < ink.data.length; i += 1, p += 3) {
    const alpha = clamp01(ink.data[i]) * mean;
    for (let c = 0; c < 3; c += 1) raster.data[p + c] = raster.data[p + c] * (1 - alpha) + fibre[c] * alpha;
  }
  return raster;
}

// ---- operators ----

const nary = (rasters: unknown, kernel: (a: number, b: number) => number, unit: number): Raster => {
  const list = (Array.isArray(rasters) ? rasters : [rasters]).filter(Raster.isRaster);
  if (list.length === 0) return new Raster(1, 1, 1);
  if (list.length === 1) return list[0];
  return combine(list, (samples) => samples.reduce(kernel, unit === undefined ? samples[0] : unit));
};

export function add(images: unknown): Raster {
  return nary(images, (a, b) => a + b, 0);
}

export function multiply(images: unknown): Raster {
  return nary(images, (a, b) => a * b, 1);
}

export function maximum(images: unknown): Raster {
  return nary(images, (a, b) => Math.max(a, b), -Infinity);
}

export function minimum(images: unknown): Raster {
  return nary(images, (a, b) => Math.min(a, b), Infinity);
}

/** A soft step of a against b: the halftone threshold, and every gate in the print. */
export function compare(a: Raster | null, b: Raster | null, softness: number): Raster {
  if (!Raster.isRaster(a) || !Raster.isRaster(b)) return new Raster(1, 1, 1);
  const inv = 1 / Math.max(softness, 1e-6);
  return combine([a, b], (samples) => clamp01((samples[0] - samples[1]) * inv + 0.5), { channels: 1 });
}

export function remap(image: Raster | null, low: number, high: number): Raster {
  if (!Raster.isRaster(image)) return new Raster(1, 1, 1);
  return map(image, (v) => low + v * (high - low));
}

/** Registration: the offset and rotation of one ink relative to the paper. Outside stays empty. */
export function shift(image: Raster | null, translate: Point, angle: number): Raster {
  if (!Raster.isRaster(image)) return new Raster(1, 1, 1);
  const rot = (angle * Math.PI) / 180;
  if (translate.x === 0 && translate.y === 0 && rot === 0) return image;
  const c = Math.cos(-rot);
  const s = Math.sin(-rot);
  const cx = image.width / 2;
  const cy = image.height / 2;
  const out = image.like();
  let i = 0;
  for (let y = 0; y < image.height; y += 1) {
    const py = y - translate.y - cy;
    for (let x = 0; x < image.width; x += 1) {
      const px = x - translate.x - cx;
      const sx = px * c - py * s + cx;
      const sy = px * s + py * c + cy;
      const inside = sx >= 0 && sy >= 0 && sx < image.width - 1 && sy < image.height - 1;
      for (let ch = 0; ch < image.channels; ch += 1) out.data[i++] = inside ? image.sample(sx, sy, ch) : 0;
    }
  }
  return out;
}

/** Coverage to the transmittance of one ink: nothing printed is white, full coverage is the ink. */
export function ink(image: Raster | null, color: Color): Raster {
  if (!Raster.isRaster(image)) return new Raster(1, 1, 3);
  const rgb = [color.r, color.g, color.b];
  const out = new Raster(image.width, image.height, 3);
  for (let p = 0, i = 0; p < image.pixelCount; p += 1) {
    const v = image.channels === 1 ? image.data[p] : image.data[p * 3];
    for (let c = 0; c < 3; c += 1) out.data[i++] = 1 - v * (1 - rgb[c]);
  }
  return out;
}

/** The escape hatch: an expression of the two inputs and the pixel position. */
export function expression(a: Raster | null, b: Raster | null, expr: string): Raster {
  const inputs = [a, b].filter(Raster.isRaster) as Raster[];
  if (inputs.length === 0) return new Raster(1, 1, 1);
  const kernel = compilePixelExpression(expr);
  const s = commonSize(inputs);
  const channels: RasterChannels = inputs.some((r) => r.channels === 3) ? 3 : 1;
  const left = Raster.isRaster(a) ? a : null;
  const right = Raster.isRaster(b) ? b : null;
  return generate(s.width, s.height, channels, (x, y, c) =>
    kernel(left ? left.at(x, y, c) : 0, right ? right.at(x, y, c) : 0, x / s.width, y / s.height, c),
  );
}

type PixelKernel = (a: number, b: number, x: number, y: number, channel: number) => number;
const PIXEL_KERNELS = new Map<string, PixelKernel>();

/** `@a` and `@b` are the inputs, `@x` and `@y` the position from 0 to 1, `@c` the channel. */
export function compilePixelExpression(expr: string): PixelKernel {
  const cached = PIXEL_KERNELS.get(expr);
  if (cached) return cached;
  const source = String(expr ?? "").replace(/@([A-Za-z_]\w*)/g, "$1");
  let kernel: PixelKernel;
  try {
    kernel = new Function("a", "b", "x", "y", "c", `with (Math) { return (${source || "a"}); }`) as PixelKernel;
    kernel(0, 0, 0, 0, 0);
  } catch {
    kernel = (a) => a;
  }
  PIXEL_KERNELS.set(expr, kernel);
  return kernel;
}

export const imageLibrary = new JavaScriptLibrary("image", {
  fill,
  stroke,
  constant,
  screen,
  noise,
  grain,
  paper,
  add,
  multiply,
  maximum,
  minimum,
  compare,
  remap,
  shift,
  ink,
  expression,
});
