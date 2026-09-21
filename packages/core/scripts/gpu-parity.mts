// Render the print on the CPU and on WebGPU and compare, pixel by pixel.
//
// WebGPU needs a secure context and a real browser, so vitest cannot host this: the page is
// bundled with esbuild, served over 127.0.0.1 (which counts as secure) and driven with Playwright.
//
//   node --import tsx scripts/gpu-parity.mts [size]
//
// In this container Chromium runs WebGPU on SwiftShader, which is a software device: the numbers
// it produces are the ones the kernels compute, but its timings say nothing about a real GPU.

import esbuild from "esbuild";
import http from "node:http";
import path from "node:path";
import { chromium } from "playwright";

const here = path.dirname(new URL(import.meta.url).pathname);
const size = Number(process.argv[2] ?? 256);

const bundle = await esbuild.build({
  entryPoints: [path.join(here, "gpu", "parity-entry.ts")],
  bundle: true,
  format: "iife",
  write: false,
  target: "es2022",
  logLevel: "warning",
});
const script = bundle.outputFiles[0].text;

const server = http.createServer((_request, response) => {
  response.writeHead(200, { "content-type": "text/html" });
  response.end(`<!doctype html><meta charset="utf-8"><title>parity</title><script>${script}</script>`);
});
await new Promise<void>((resolve) => server.listen(4271, "127.0.0.1", () => resolve()));

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM ?? "/opt/pw-browsers/chromium-1194/chrome-linux/chrome",
  args: [
    "--no-sandbox",
    "--enable-unsafe-webgpu",
    "--enable-features=Vulkan",
    "--use-angle=vulkan",
    "--use-vulkan=swiftshader",
  ],
  env: {
    ...process.env,
    VK_ICD_FILENAMES:
      process.env.VK_ICD_FILENAMES ?? "/opt/pw-browsers/chromium-1194/chrome-linux/vk_swiftshader_icd.json",
  },
});
const page = await browser.newPage();
page.on("pageerror", (e) => console.error("page error:", e.message));
page.on("console", (m) => {
  if (m.type() === "error") console.error("console:", m.text());
});
await page.goto("http://127.0.0.1:4271/");
if (process.env.KERNELS) {
  const per = await page.evaluate((n) => (globalThis as Record<string, any>).risoKernels(n), size);
  console.log(per);
  await browser.close();
  server.close();
  process.exit(0);
}
const result = await page.evaluate((n) => (globalThis as Record<string, any>).risoParity(n), size);
await browser.close();
server.close();

if (result.error) {
  console.error(result.error);
  process.exit(1);
}
console.log(`adapter        ${result.adapter}`);
console.log(`size           ${result.size} x ${result.size}`);
console.log(`cpu            ${result.cpuMs} ms`);
console.log(`gpu            ${result.gpuMs} ms`);
console.log(`worst channel  ${result.worst.toExponential(3)} (at ${result.worstAt})`);
console.log(`mean channel   ${result.mean.toExponential(3)}`);
console.log(`visible        ${result.visible} of ${result.channels} channels differ by half a printed step or more`);
// The CPU kernels compute in doubles and store floats; the GPU computes in floats throughout, so
// the two never agree to the last bit. What matters is that no difference survives to the print:
// a channel is 1/255 wide, and the soft steps in the halftone amplify the rounding at a dot's
// edge by about a hundred, which is where the worst case sits.
const tolerance = Number(process.env.TOLERANCE ?? 1e-3);
if (!(result.worst <= tolerance) || result.visible > 0) {
  console.error(`The two paths disagree: worst ${result.worst}, ${result.visible} visible channels.`);
  process.exit(1);
}
console.log("The CPU and GPU paths give the same print.");
