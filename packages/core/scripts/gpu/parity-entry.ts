// The entry point of the WebGPU parity check: render the same print twice, once on the CPU and
// once on the device, and report how far apart the two are. Served over localhost by
// scripts/gpu-parity.mts, because WebGPU needs a secure context.

import { NodeContext } from "../../src/runtime/context";
import { builtinFunctionRepository } from "../../src/functions";
import { builtinNodeRepository } from "../../src/libraries";
import { addChild, connect, createNetworkNode, extendNode, setInputValue } from "../../src/model/node";
import { Point } from "../../src/graphics/point";
import { Raster } from "../../src/graphics/raster";
import { GpuRaster, getImageDevice, installImageDevice } from "../../src/graphics/gpu";
import type { Library } from "../../src/model/types";

function printDocument(size: number): Library {
  const repository = builtinNodeRepository();
  const root = extendNode(createNetworkNode(), "core.network", "root");
  const add = (prototype: string, name: string) =>
    addChild(root, extendNode(repository.getNode(prototype)!, prototype, name));
  for (const [ink, x, y] of [
    ["blue", -40, -24],
    ["pink", 40, -24],
    ["yellow", 0, 45],
  ] as const) {
    const shape = add("corevector.ellipse", `${ink}Shape`);
    setInputValue(shape, "position", new Point(x, y));
    setInputValue(shape, "width", 150);
    setInputValue(shape, "height", 150);
    const fill = add("image.fill", `${ink}Fill`);
    setInputValue(fill, "width", size);
    setInputValue(fill, "height", size);
    connect(root, `${ink}Shape`, `${ink}Fill`, "shape");
  }
  const print = add("image.print", "print");
  setInputValue(print, "width", size);
  setInputValue(print, "height", size);
  setInputValue(print, "fibres", 0);
  for (const ink of ["blue", "pink", "yellow"] as const) connect(root, `${ink}Fill`, "print", ink);
  root.renderedChild = "print";
  return {
    name: "parity",
    root,
    functionLinks: [],
    devices: [],
    properties: {},
    dependencies: {},
    assets: {},
    sourceFormat: "memory",
    meta: {},
  };
}

async function render(size: number): Promise<{ raster: Raster; ms: number }> {
  const context = new NodeContext(printDocument(size), builtinFunctionRepository());
  const started = performance.now();
  const results = await context.render("/");
  let value = results[0] as Raster | GpuRaster;
  if (value instanceof GpuRaster) value = await getImageDevice()!.readback(value);
  return { raster: value as Raster, ms: performance.now() - started };
}

function compare(a: Raster, b: Raster) {
  let worst = 0;
  let total = 0;
  let worstAt = -1;
  // A printed step is 1/255; half of one is the point where a difference could ever be seen.
  let visible = 0;
  for (let i = 0; i < a.data.length; i += 1) {
    const d = Math.abs(a.data[i] - b.data[i]);
    total += d;
    if (d > 1 / 510) visible += 1;
    if (d > worst) {
      worst = d;
      worstAt = i;
    }
  }
  return { worst, mean: total / a.data.length, worstAt, visible, channels: a.data.length };
}

export async function run(size: number) {
  installImageDevice(undefined);
  const cpu = await render(size);
  if (!("gpu" in navigator)) return { error: "This browser has no WebGPU." };
  const adapter = await navigator.gpu.requestAdapter();
  if (!adapter) return { error: "No WebGPU adapter." };
  installImageDevice(await adapter.requestDevice());
  const gpu = await render(size);
  installImageDevice(undefined);
  return {
    size,
    adapter: adapter.info ? `${adapter.info.vendor} ${adapter.info.architecture}` : "unknown",
    cpuMs: Math.round(cpu.ms),
    gpuMs: Math.round(gpu.ms),
    ...compare(cpu.raster, gpu.raster),
    cpuPixels: Array.from(cpu.raster.data.slice(0, 12)),
    gpuPixels: Array.from(gpu.raster.data.slice(0, 12)),
  };
}

(globalThis as Record<string, unknown>).risoParity = run;

/** Each kernel on its own, so a disagreement points at one of them. */
export async function kernels(size = 64) {
  const inputs = (make: () => unknown) => make();
  if (!("gpu" in navigator)) return { error: "This browser has no WebGPU." };
  const adapter = await navigator.gpu.requestAdapter();
  if (!adapter) return { error: "No WebGPU adapter." };
  const device = await adapter.requestDevice();

  const image = await import("../../src/functions/image");
  const { Color } = await import("../../src/graphics/color");
  const cases: [string, () => unknown][] = [
    ["constant", () => image.constant(size, size, 0.37)],
    ["screen", () => image.screen(size, size, 5.6, 15)],
    ["noise", () => image.noise(size, size, 30, 7)],
    ["grain1", () => image.grain(size, size, 1, 7)],
    ["grain3", () => image.grain(size, size, 3, 11)],
  ];
  // Operators need an input, which has to be made the same way on both sides.
  const source = () => image.noise(size, size, 17, 3);
  const other = () => image.screen(size, size, 4.2, 30);
  cases.push(
    ["remap", () => image.remap(source(), -0.25, 1.5)],
    ["shift", () => image.shift(source(), new Point(2.5, -1.5), 3)],
    ["ink", () => image.ink(source(), new Color(1, 0.28, 0.69))],
    ["add", () => image.add([source(), other(), source()])],
    ["multiply", () => image.multiply([source(), other(), source()])],
    ["maximum", () => image.maximum([source(), other()])],
    ["minimum", () => image.minimum([source(), other()])],
    ["compare", () => image.compare(source(), other(), 0.14)],
  );

  const out: Record<string, unknown> = {};
  for (const [name, make] of cases) {
    installImageDevice(undefined);
    const cpu = inputs(make) as Raster;
    installImageDevice(device);
    let gpu = inputs(make) as Raster | GpuRaster;
    if (gpu instanceof GpuRaster) gpu = await getImageDevice()!.readback(gpu);
    installImageDevice(undefined);
    const a = cpu;
    const b = gpu as Raster;
    if (a.data.length !== b.data.length) {
      out[name] = `size ${a.width}x${a.height}x${a.channels} vs ${b.width}x${b.height}x${b.channels}`;
      continue;
    }
    let worst = 0;
    let at = -1;
    for (let i = 0; i < a.data.length; i += 1) {
      const d = Math.abs(a.data[i] - b.data[i]);
      if (d > worst) {
        worst = d;
        at = i;
      }
    }
    out[name] =
      worst === 0 ? "exact" : `worst ${worst.toExponential(3)} at ${at} (cpu ${a.data[at]}, gpu ${b.data[at]})`;
  }
  return out;
}

(globalThis as Record<string, unknown>).risoKernels = kernels;
