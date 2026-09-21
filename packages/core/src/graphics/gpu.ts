// The pixel kernels on WebGPU.
//
// Every raster on the device is an rgba32float storage texture. A one-channel raster keeps its
// value in r, g and b, so mixing one- and three-channel inputs needs no special case in a shader;
// `channels` says how many are meaningful. Kernels are 8 by 8 compute passes recorded into one
// encoder and submitted when something has to be read back or shown.
//
// Each kernel is written to give the same number as its twin in functions/image.ts: the same
// closed form for the generator, the same noise grid, the same order of operations. test/gpu.test.ts
// renders the whole print both ways and compares them, which is the only way that claim stays true.

import { Raster, RasterChannels } from "./raster";

const WORKGROUP = 8;
const NOISE_GRID = 32;

/** A raster that lives on the device. It carries the same shape as a Raster, minus the data. */
export class GpuRaster {
  readonly width: number;
  readonly height: number;
  readonly channels: RasterChannels;
  readonly texture: GPUTexture;
  private readonly owner: ImageDevice;

  constructor(owner: ImageDevice, texture: GPUTexture, width: number, height: number, channels: RasterChannels) {
    this.owner = owner;
    this.texture = texture;
    this.width = width;
    this.height = height;
    this.channels = channels;
  }

  static isGpuRaster(value: unknown): value is GpuRaster {
    return value instanceof GpuRaster;
  }

  get pixelCount(): number {
    return this.width * this.height;
  }

  /** Return the texture to the pool. */
  release(): void {
    this.owner.release(this);
  }

  toString(): string {
    return `<GpuRaster ${this.width}x${this.height}x${this.channels}>`;
  }
}

interface Kernel {
  inputs: number;
  body: string;
}

/**
 * Uniforms are always one vec4f and one vec4u, so no kernel has to reason about the alignment
 * rules of the uniform address space, and a seed keeps its exact bits.
 */
const PREAMBLE = `
struct P { f: vec4f, u: vec4u }
@group(0) @binding(0) var<uniform> p: P;
@group(0) @binding(1) var dst: texture_storage_2d<rgba32float, write>;
`;

const KERNELS: Record<string, Kernel> = {
  constant: { inputs: 0, body: `  let v = p.f.x; textureStore(dst, q, vec4f(v, v, v, 1.0));` },
  // A round dot that grows past 1 towards the corners of its cell.
  screen: {
    inputs: 0,
    body: `
  let xr = (f32(x) * p.f.y + f32(y) * p.f.z) / p.f.x;
  let yr = (-f32(x) * p.f.z + f32(y) * p.f.y) / p.f.x;
  let u = xr - floor(xr) - 0.5;
  let v = yr - floor(yr) - 0.5;
  let o = 3.141592653589793 * (u * u + v * v) / 0.9;
  textureStore(dst, q, vec4f(o, o, o, 1.0));`,
  },
  // t0 is the value grid; smoothstep between its corners, tiling.
  noise: {
    inputs: 1,
    body: `
  let fx0 = f32(x) / p.f.x;
  let fy0 = f32(y) / p.f.x;
  let xi = i32(floor(fx0));
  let yi = i32(floor(fy0));
  let fx = fx0 - f32(xi);
  let fy = fy0 - f32(yi);
  let sx = fx * fx * (3.0 - 2.0 * fx);
  let sy = fy * fy * (3.0 - 2.0 * fy);
  let n = ${NOISE_GRID};
  let i0 = ((xi % n) + n) % n;
  let i1 = (i0 + 1) % n;
  let j0 = ((yi % n) + n) % n;
  let j1 = (j0 + 1) % n;
  let a = textureLoad(t0, vec2i(i0, j0), 0).r;
  let b = textureLoad(t0, vec2i(i1, j0), 0).r;
  let c = textureLoad(t0, vec2i(i0, j1), 0).r;
  let d = textureLoad(t0, vec2i(i1, j1), 0).r;
  let o = (a + (b - a) * sx) * (1.0 - sy) + (c + (d - c) * sx) * sy;
  textureStore(dst, q, vec4f(o, o, o, 1.0));`,
  },
  // The state of mulberry32 after k calls is seed + k*0x6D2B79F5, so the k-th value is a pure
  // function of k and no loop is needed to reach it.
  grain: {
    inputs: 0,
    body: `
  let block = p.u.y;
  let columns = p.u.z;
  let k = (u32(y) / block) * columns + u32(x) / block;
  var a = p.u.x + (k + 1u) * 0x6D2B79F5u;
  var t = (a ^ (a >> 15u)) * (1u | a);
  t = (t + (t ^ (t >> 7u)) * (61u | t)) ^ t;
  let o = f32(t ^ (t >> 14u)) / 4294967296.0;
  textureStore(dst, q, vec4f(o, o, o, 1.0));`,
  },
  // Bilinear about the centre; a sample whose footprint leaves the raster stays empty.
  shift: {
    inputs: 1,
    body: `
  let w = f32(textureDimensions(t0).x);
  let h = f32(textureDimensions(t0).y);
  let cx = w * 0.5;
  let cy = h * 0.5;
  let py = f32(y) - p.f.y - cy;
  let px = f32(x) - p.f.x - cx;
  let sx = px * p.f.z - py * p.f.w + cx;
  let sy = px * p.f.w + py * p.f.z + cy;
  if (sx < 0.0 || sy < 0.0 || sx >= w - 1.0 || sy >= h - 1.0) {
    textureStore(dst, q, vec4f(0.0, 0.0, 0.0, 1.0));
    return;
  }
  let x0 = i32(floor(sx));
  let y0 = i32(floor(sy));
  let tx = sx - f32(x0);
  let ty = sy - f32(y0);
  let a = textureLoad(t0, vec2i(x0, y0), 0);
  let b = textureLoad(t0, vec2i(x0 + 1, y0), 0);
  let c = textureLoad(t0, vec2i(x0, y0 + 1), 0);
  let d = textureLoad(t0, vec2i(x0 + 1, y0 + 1), 0);
  let o = (a + (b - a) * tx) * (1.0 - ty) + (c + (d - c) * tx) * ty;
  textureStore(dst, q, vec4f(o.rgb, 1.0));`,
  },
  remap: {
    inputs: 1,
    body: `
  let v = textureLoad(t0, q, 0).rgb;
  textureStore(dst, q, vec4f(p.f.x + v * (p.f.y - p.f.x), 1.0));`,
  },
  add: {
    inputs: 2,
    body: `  textureStore(dst, q, vec4f(textureLoad(t0, q, 0).rgb + textureLoad(t1, q, 0).rgb, 1.0));`,
  },
  multiply: {
    inputs: 2,
    body: `  textureStore(dst, q, vec4f(textureLoad(t0, q, 0).rgb * textureLoad(t1, q, 0).rgb, 1.0));`,
  },
  maximum: {
    inputs: 2,
    body: `  textureStore(dst, q, vec4f(max(textureLoad(t0, q, 0).rgb, textureLoad(t1, q, 0).rgb), 1.0));`,
  },
  minimum: {
    inputs: 2,
    body: `  textureStore(dst, q, vec4f(min(textureLoad(t0, q, 0).rgb, textureLoad(t1, q, 0).rgb), 1.0));`,
  },
  compare: {
    inputs: 2,
    body: `
  let v = clamp((textureLoad(t0, q, 0).r - textureLoad(t1, q, 0).r) * p.f.x + 0.5, 0.0, 1.0);
  textureStore(dst, q, vec4f(v, v, v, 1.0));`,
  },
  ink: {
    inputs: 1,
    body: `
  let v = textureLoad(t0, q, 0).r;
  let c = vec3f(p.f.x, p.f.y, p.f.z);
  textureStore(dst, q, vec4f(1.0 - v * (1.0 - c), 1.0));`,
  },
};

function kernelSource(name: string, kernel: Kernel): string {
  let source = PREAMBLE;
  for (let i = 0; i < kernel.inputs; i += 1) source += `@group(0) @binding(${2 + i}) var t${i}: texture_2d<f32>;\n`;
  source += `@compute @workgroup_size(${WORKGROUP}, ${WORKGROUP}) fn main(@builtin(global_invocation_id) id: vec3u) {
  let size = textureDimensions(dst);
  if (id.x >= size.x || id.y >= size.y) { return; }
  let x = i32(id.x);
  let y = i32(id.y);
  let q = vec2i(x, y);
${kernel.body}
}
// ${name}`;
  return source;
}

interface Uniforms {
  f?: [number, number, number, number];
  u?: [number, number, number, number];
}

/** A pool of textures by size, so a frame of kernels allocates once and reuses after that. */
export class ImageDevice {
  readonly device: GPUDevice;
  private readonly pipelines = new Map<string, GPUComputePipeline>();
  private readonly layouts = new Map<number, GPUBindGroupLayout>();
  private readonly pool = new Map<string, GPUTexture[]>();
  private readonly live = new Set<GPUTexture>();
  private encoder: GPUCommandEncoder | null = null;
  private readonly scratch: GPUBuffer[] = [];
  private noiseGrids = new Map<number, GpuRaster>();

  constructor(device: GPUDevice) {
    this.device = device;
    // A validation error otherwise shows up as a raster full of zeros and nothing else.
    device.addEventListener?.("uncapturederror", (event) => {
      console.error("WebGPU:", (event as GPUUncapturedErrorEvent).error.message);
    });
  }

  /**
   * The layout is spelled out rather than inferred: "auto" leaves out a binding the shader does
   * not read, and then handing it one is a validation error and the dispatch quietly does nothing.
   * The n-ary kernels read no uniforms at all, which is exactly that case.
   */
  private layout(inputs: number): GPUBindGroupLayout {
    let layout = this.layouts.get(inputs);
    if (!layout) {
      const entries: GPUBindGroupLayoutEntry[] = [
        { binding: 0, visibility: GPUShaderStage.COMPUTE, buffer: { type: "uniform" } },
        {
          binding: 1,
          visibility: GPUShaderStage.COMPUTE,
          storageTexture: { access: "write-only", format: "rgba32float" },
        },
      ];
      for (let i = 0; i < inputs; i += 1)
        entries.push({
          binding: 2 + i,
          visibility: GPUShaderStage.COMPUTE,
          texture: { sampleType: "unfilterable-float" },
        });
      layout = this.device.createBindGroupLayout({ entries });
      this.layouts.set(inputs, layout);
    }
    return layout;
  }

  private pipeline(name: string): GPUComputePipeline {
    let pipeline = this.pipelines.get(name);
    if (!pipeline) {
      const kernel = KERNELS[name];
      if (!kernel) throw new Error(`There is no GPU kernel named ${name}.`);
      const module = this.device.createShaderModule({ code: kernelSource(name, kernel) });
      pipeline = this.device.createComputePipeline({
        layout: this.device.createPipelineLayout({ bindGroupLayouts: [this.layout(kernel.inputs)] }),
        compute: { module, entryPoint: "main" },
      });
      this.pipelines.set(name, pipeline);
    }
    return pipeline;
  }

  private texture(width: number, height: number): GPUTexture {
    const key = `${width}x${height}`;
    const free = this.pool.get(key);
    const texture =
      free && free.length > 0
        ? free.pop()!
        : this.device.createTexture({
            size: { width, height },
            format: "rgba32float",
            usage:
              GPUTextureUsage.STORAGE_BINDING |
              GPUTextureUsage.TEXTURE_BINDING |
              GPUTextureUsage.COPY_SRC |
              GPUTextureUsage.COPY_DST,
          });
    this.live.add(texture);
    return texture;
  }

  release(raster: GpuRaster): void {
    if (!this.live.delete(raster.texture)) return;
    const key = `${raster.width}x${raster.height}`;
    const free = this.pool.get(key);
    if (free) free.push(raster.texture);
    else this.pool.set(key, [raster.texture]);
  }

  private commands(): GPUCommandEncoder {
    if (!this.encoder) this.encoder = this.device.createCommandEncoder();
    return this.encoder;
  }

  /** Submit everything recorded so far. Kernels queue up until a readback needs them. */
  flush(): void {
    if (!this.encoder) return;
    this.device.queue.submit([this.encoder.finish()]);
    this.encoder = null;
    for (const buffer of this.scratch.splice(0)) buffer.destroy();
  }

  /** Run one kernel over the inputs and return its result. */
  run(
    name: string,
    inputs: readonly GpuRaster[],
    uniforms: Uniforms,
    size: { width: number; height: number; channels: RasterChannels },
  ): GpuRaster {
    const pipeline = this.pipeline(name);
    const texture = this.texture(size.width, size.height);
    const values = new ArrayBuffer(32);
    new Float32Array(values, 0, 4).set(uniforms.f ?? [0, 0, 0, 0]);
    new Uint32Array(values, 16, 4).set(uniforms.u ?? [0, 0, 0, 0]);
    const buffer = this.device.createBuffer({ size: 32, usage: GPUBufferUsage.UNIFORM | GPUBufferUsage.COPY_DST });
    this.device.queue.writeBuffer(buffer, 0, values);
    this.scratch.push(buffer);
    const entries: GPUBindGroupEntry[] = [
      { binding: 0, resource: { buffer } },
      { binding: 1, resource: texture.createView() },
    ];
    inputs.forEach((input, i) => entries.push({ binding: 2 + i, resource: input.texture.createView() }));
    const pass = this.commands().beginComputePass();
    pass.setPipeline(pipeline);
    pass.setBindGroup(0, this.device.createBindGroup({ layout: this.layout(KERNELS[name].inputs), entries }));
    pass.dispatchWorkgroups(Math.ceil(size.width / WORKGROUP), Math.ceil(size.height / WORKGROUP));
    pass.end();
    return new GpuRaster(this, texture, size.width, size.height, size.channels);
  }

  /** Put a raster on the device, replicating a single channel into r, g and b. */
  upload(raster: Raster): GpuRaster {
    const texture = this.texture(raster.width, raster.height);
    const rgba = new Float32Array(raster.width * raster.height * 4);
    for (let p = 0, i = 0; p < raster.pixelCount; p += 1, i += 4) {
      if (raster.channels === 1) {
        const v = raster.data[p];
        rgba[i] = v;
        rgba[i + 1] = v;
        rgba[i + 2] = v;
      } else {
        rgba[i] = raster.data[p * 3];
        rgba[i + 1] = raster.data[p * 3 + 1];
        rgba[i + 2] = raster.data[p * 3 + 2];
      }
      rgba[i + 3] = 1;
    }
    this.device.queue.writeTexture(
      { texture },
      rgba,
      { bytesPerRow: raster.width * 16, rowsPerImage: raster.height },
      { width: raster.width, height: raster.height },
    );
    return new GpuRaster(this, texture, raster.width, raster.height, raster.channels);
  }

  /** Bring a raster back to the CPU, keeping only the channels it declares. */
  async readback(raster: GpuRaster): Promise<Raster> {
    this.flush();
    // A texture copy needs rows that are a multiple of 256 bytes.
    const bytesPerRow = Math.ceil((raster.width * 16) / 256) * 256;
    const buffer = this.device.createBuffer({
      size: bytesPerRow * raster.height,
      usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.MAP_READ,
    });
    const encoder = this.device.createCommandEncoder();
    encoder.copyTextureToBuffer(
      { texture: raster.texture },
      { buffer, bytesPerRow, rowsPerImage: raster.height },
      { width: raster.width, height: raster.height },
    );
    this.device.queue.submit([encoder.finish()]);
    await buffer.mapAsync(GPUMapMode.READ);
    const source = new Float32Array(buffer.getMappedRange());
    const out = new Raster(raster.width, raster.height, raster.channels);
    const stride = bytesPerRow / 4;
    for (let y = 0; y < raster.height; y += 1) {
      for (let x = 0; x < raster.width; x += 1) {
        const from = y * stride + x * 4;
        const to = (y * raster.width + x) * raster.channels;
        if (raster.channels === 1) out.data[to] = source[from];
        else {
          out.data[to] = source[from];
          out.data[to + 1] = source[from + 1];
          out.data[to + 2] = source[from + 2];
        }
      }
    }
    buffer.unmap();
    buffer.destroy();
    return out;
  }

  /** The value grid a noise kernel interpolates, kept per seed. */
  noiseGrid(seed: number, grid: Float32Array): GpuRaster {
    const cached = this.noiseGrids.get(seed);
    if (cached) return cached;
    const raster = new Raster(NOISE_GRID, NOISE_GRID, 1, grid);
    const uploaded = this.upload(raster);
    this.live.delete(uploaded.texture);
    this.noiseGrids.set(seed, uploaded);
    return uploaded;
  }
}

let installed: ImageDevice | undefined;

/** Install a WebGPU device; the image nodes then run their kernels on it. */
export function installImageDevice(device: GPUDevice | undefined): ImageDevice | undefined {
  installed = device ? new ImageDevice(device) : undefined;
  return installed;
}

export function getImageDevice(): ImageDevice | undefined {
  return installed;
}

/** A raster as it is needed on the CPU: read back from the device if that is where it is. */
export async function toCpuRaster(value: unknown): Promise<unknown> {
  if (!GpuRaster.isGpuRaster(value)) return value;
  const device = getImageDevice();
  if (!device) return value;
  return device.readback(value);
}
