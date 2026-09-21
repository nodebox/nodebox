// Rasters: the pixel values that flow between image nodes.
//
// A raster is a rectangle of 32-bit floats with one or three channels. One channel is coverage or
// tone (0 = nothing, 1 = full); three channels are linear RGB. Kernels mix the two freely: a
// one-channel raster broadcasts to all three, which is what lets a tone drive a colour without a
// conversion node in between. Values are not clamped in transit, because a chain of adds and
// multiplies is easier to reason about when the clamp happens where it is meant to.

export type RasterChannels = 1 | 3;

export class Raster {
  readonly width: number;
  readonly height: number;
  readonly channels: RasterChannels;
  readonly data: Float32Array;

  constructor(width: number, height: number, channels: RasterChannels = 1, data?: Float32Array) {
    this.width = Math.max(1, Math.round(width));
    this.height = Math.max(1, Math.round(height));
    this.channels = channels;
    this.data = data ?? new Float32Array(this.width * this.height * channels);
  }

  static isRaster(value: unknown): value is Raster {
    return value instanceof Raster;
  }

  /** A raster of the same size and channel count, zeroed. */
  like(channels: RasterChannels = this.channels): Raster {
    return new Raster(this.width, this.height, channels);
  }

  get pixelCount(): number {
    return this.width * this.height;
  }

  /** The channel value at a pixel, broadcasting a one-channel raster to any channel index. */
  at(x: number, y: number, channel = 0): number {
    if (x < 0 || y < 0 || x >= this.width || y >= this.height) return 0;
    const i = (y * this.width + x) * this.channels;
    return this.channels === 1 ? this.data[i] : this.data[i + channel];
  }

  /** Bilinear sample in pixel coordinates; outside the raster reads as 0. */
  sample(x: number, y: number, channel = 0): number {
    const x0 = Math.floor(x);
    const y0 = Math.floor(y);
    const fx = x - x0;
    const fy = y - y0;
    const a = this.at(x0, y0, channel);
    const b = this.at(x0 + 1, y0, channel);
    const c = this.at(x0, y0 + 1, channel);
    const d = this.at(x0 + 1, y0 + 1, channel);
    return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
  }

  toString(): string {
    return `<Raster ${this.width}x${this.height}x${this.channels}>`;
  }
}

/** A filled raster; the value is written to every channel. */
export function constantRaster(width: number, height: number, value: number, channels: RasterChannels = 1): Raster {
  const raster = new Raster(width, height, channels);
  raster.data.fill(value);
  return raster;
}

/** The size a combining kernel works at: the largest of its inputs, so nothing is silently cropped. */
export function commonSize(rasters: readonly Raster[]): { width: number; height: number } {
  let width = 1;
  let height = 1;
  for (const raster of rasters) {
    if (raster.width > width) width = raster.width;
    if (raster.height > height) height = raster.height;
  }
  return { width, height };
}

export function commonChannels(rasters: readonly Raster[]): RasterChannels {
  return rasters.some((raster) => raster.channels === 3) ? 3 : 1;
}

/**
 * Apply a kernel over a set of rasters. Inputs of different sizes are read in normalised
 * coordinates, so a small tile and a full frame line up.
 */
export function combine(
  inputs: readonly Raster[],
  kernel: (samples: number[], x: number, y: number, channel: number) => number,
  options: { width?: number; height?: number; channels?: RasterChannels } = {},
): Raster {
  const size = commonSize(inputs);
  const width = options.width ?? size.width;
  const height = options.height ?? size.height;
  const channels = options.channels ?? commonChannels(inputs);
  const out = new Raster(width, height, channels);
  const samples = new Array<number>(inputs.length);
  // A raster the same size is read pixel for pixel; a different one is scaled, not resampled.
  const scales = inputs.map((raster) => [raster.width / width, raster.height / height] as const);
  let i = 0;
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      for (let c = 0; c < channels; c += 1) {
        for (let k = 0; k < inputs.length; k += 1) {
          const [sx, sy] = scales[k];
          samples[k] =
            sx === 1 && sy === 1 ? inputs[k].at(x, y, c) : inputs[k].at(Math.floor(x * sx), Math.floor(y * sy), c);
        }
        out.data[i++] = kernel(samples, x, y, c);
      }
    }
  }
  return out;
}

/** Apply a kernel to every channel of one raster. */
export function map(input: Raster, kernel: (v: number, x: number, y: number, channel: number) => number): Raster {
  const out = input.like();
  let i = 0;
  for (let y = 0; y < input.height; y += 1) {
    for (let x = 0; x < input.width; x += 1) {
      for (let c = 0; c < input.channels; c += 1) {
        out.data[i] = kernel(input.data[i], x, y, c);
        i += 1;
      }
    }
  }
  return out;
}

/** Build a raster from a function of the pixel position. */
export function generate(
  width: number,
  height: number,
  channels: RasterChannels,
  kernel: (x: number, y: number, channel: number) => number,
): Raster {
  const out = new Raster(width, height, channels);
  let i = 0;
  for (let y = 0; y < out.height; y += 1) {
    for (let x = 0; x < out.width; x += 1) {
      for (let c = 0; c < channels; c += 1) out.data[i++] = kernel(x, y, c);
    }
  }
  return out;
}
