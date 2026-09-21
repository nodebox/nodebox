/// <reference path="./classic-modules.d.ts" />
// Evaluate a project through @ndbx/core instead of the Live engine.
//
// The core engine understands everything the Live engine does (JavaScript function items, tables,
// shapes, expressions) plus what it never implemented: subnetworks with inlets and outlets, list
// matching, and the NodeBox 3 built-in nodes ("nodebox/<library>/<node>" items), so documents
// opened from .ndbx files render in the same editor.

import {
  Library,
  NodeContext,
  NodeRepository,
  builtinFunctionRepository,
  builtinNodeRepository,
  getFontProvider,
  installOpenTypeFonts,
  isNativeItem,
  liveFunctions,
  nativeLibraryProjects,
  parseLiveProject,
  toG,
} from "@ndbx/core";
import type { ClassicDocument, ClassicProject, LiveProject } from "@ndbx/core";
import { GpuRaster, classicToG, getImageDevice, installImageDevice, openClassicProject } from "@ndbx/core";
import Context from "./context";
import { config } from "./loaders";
import { Item, Project } from "./types";

/** Whether the project must run on the core engine (native NodeBox 3 items or subnetwork calls). */
export function needsCoreEngine(project: Project): boolean {
  if (project.__classicSource !== undefined) return true;
  if (Object.keys(project.dependencies ?? {}).some((key) => key.startsWith("nodebox/"))) return true;
  const items = project.items ?? [];
  const networkNames = new Set(items.filter((item) => item.type === "NETWORK").map((item) => item.name));
  return items.some(
    (item) =>
      item.type === "NETWORK" &&
      item.children.some((child) => {
        if (child.type !== "NODE") return false;
        const fn = (child as unknown as { fn: string }).fn;
        return fn.startsWith("self/self/") && networkNames.has(fn.slice("self/self/".length));
      }),
  );
}

/** The engine setting: "auto" picks the core engine when a project needs it. */
export type Engine = "auto" | "live" | "core";

export function chooseEngine(project: Project, preference: Engine = "auto"): "live" | "core" {
  // A classic project has no ES modules to import, so the Live engine has nothing to run.
  if (project.__classicSource !== undefined) return "core";
  if (preference !== "auto") return preference;
  return needsCoreEngine(project) ? "core" : "live";
}

interface CoreState {
  version: number;
  libraries: Map<string, Library>;
  repository: NodeRepository;
  contexts: Map<string, NodeContext>;
}

const states = new WeakMap<Context, CoreState>();

/** The projects for "nodebox/<library>" dependencies, generated from the core's built-in libraries. */
export function nativeProject(projectKey: string): LiveProject | undefined {
  return nativeLibraryProjects()[projectKey];
}

/** Convert the loaded project and its dependencies into core libraries (cached per context). */
export function coreLibraries(cx: Context): { main: Library; all: Library[]; repository: NodeRepository } {
  let state = states.get(cx);
  if (!state) {
    state = { version: -1, libraries: new Map(), repository: new NodeRepository(), contexts: new Map() };
    states.set(cx, state);
  }
  const repository = new NodeRepository();
  const all: Library[] = [];
  for (const [key, project] of cx.dependencies) {
    if (key.startsWith("nodebox/")) continue;
    const library = parseLiveProject(project as unknown as LiveProject, {
      projectKey: key,
      repository,
      upgrade: false,
    }).library;
    repository.add(library);
    all.push(library);
  }
  // Built-in NodeBox 3 libraries resolve against the core repository, not against Live items.
  for (const library of builtinNodeRepository().getLibraries()) repository.add(library);
  const main = parseLiveProject(cx.project as unknown as LiveProject, {
    projectKey: "self/self",
    repository,
    upgrade: false,
  }).library;
  repository.add(main);
  all.push(main);
  state.libraries = new Map(all.map((l) => [l.name, l]));
  state.repository = repository;
  return { main, all, repository };
}

let fontsReady: Promise<void> | null = null;

/**
 * Text nodes need outlines: in a browser, fetch the fonts the web app ships (DejaVu Sans, the same
 * fallback face the Java engine ended up with) once and install them as the core's font provider.
 * Node hosts install their own provider (see @ndbx/core/fonts/node).
 */
export function ensureCoreFonts(): Promise<void> {
  if (fontsReady) return fontsReady;
  fontsReady = (async () => {
    if (getFontProvider() || typeof window === "undefined" || typeof fetch !== "function") return;
    try {
      const buffers: { buffer: ArrayBuffer }[] = [];
      for (const file of ["DejaVuSans.ttf", "DejaVuSans-Bold.ttf"]) {
        const response = await fetch(`/fonts/${file}`);
        if (response.ok) buffers.push({ buffer: await response.arrayBuffer() });
      }
      if (buffers.length > 0) installOpenTypeFonts(buffers);
    } catch (e) {
      console.warn("Could not load the bundled fonts; text nodes will render without outlines.", e);
    }
  })();
  return fontsReady;
}

/**
 * The classic document of a project in the first NodeBox Live format: its functions, evaluated
 * against the g.js package the classic app shipped, and the libraries the core renders from.
 * Opening it loads g.js and opentype 1.x, so it is only reached by a classic project.
 */
const classicDocuments = new WeakMap<Context, Promise<ClassicDocument>>();

function classicDocument(cx: Context): Promise<ClassicDocument> {
  let document = classicDocuments.get(cx);
  if (document) return document;
  document = (async () => {
    const [gModule, opentypeModule, lodashModule] = await Promise.all([
      import("g.js"),
      // g.js was written against opentype.js 1.x; 2.x builds different glyph outlines.
      import("opentype-classic"),
      import("lodash"),
    ]);
    const g = { ...((gModule as { default?: unknown }).default ?? gModule) } as Record<string, unknown>;
    // The classic app loaded g.js as a script, where toSVG was also reachable as _toSVG.
    g._toSVG = g.toSVG;
    const source = cx.project.__classicSource!;
    const dependencies = [...cx.dependencies.entries()]
      .filter(([, project]) => project.__classicSource !== undefined)
      .map(([key, project]) => ({ key, project: project.__classicSource!.project as ClassicProject }));
    const document = openClassicProject(source.key, source.project as ClassicProject, {
      dependencies,
      assets: Object.fromEntries(cx.assetMap),
      namespaces: { g },
      globals: {
        _: (lodashModule as { default?: unknown }).default ?? lodashModule,
        opentype: (opentypeModule as { default?: unknown }).default ?? opentypeModule,
        window: typeof window === "undefined" ? undefined : window,
      },
    });
    const font = await classicDefaultFont();
    if (font) document.runtime.ndbx.assets["default-font"] = font;
    for (const warning of document.warnings) cx.warnings.push(warning);
    return document;
  })();
  classicDocuments.set(cx, document);
  return document;
}

/** g.textPath outlines with this face when a project names no font of its own. */
async function classicDefaultFont(): Promise<ArrayBuffer | undefined> {
  if (typeof fetch !== "function") return undefined;
  try {
    const response = await fetch("/fonts/FiraSans-Regular.woff");
    return response.ok ? await response.arrayBuffer() : undefined;
  } catch (e) {
    console.warn("Could not load the classic default font; text nodes will render without outlines.", e);
    return undefined;
  }
}

/** Render one item of a classic project. Its network is the entry point, not a node in another. */
async function renderClassicItem(cx: Context, item: Item, data: Record<string, unknown>): Promise<unknown> {
  const document = await classicDocument(cx);
  const frame = typeof data.frame === "number" ? data.frame : 1;
  document.runtime.ndbx._currentFrame = frame;
  document.runtime.ndbx._mousePosition = (data.mousePosition as { x: number; y: number }) ?? { x: 0, y: 0 };
  const context = new NodeContext(document.library, document.functions, { data: { ...data, frame } });
  const results = await context.renderEntryPoint(`/${item.name}`);
  // g.js shapes are not @ndbx/g shapes; the viewer draws the latter.
  return classicToG(results.length === 1 ? results[0] : results) ?? (results.length === 1 ? results[0] : results);
}

let devicePromise: Promise<void> | null = null;

/**
 * The pixel nodes run their kernels on WebGPU when the browser has it. The device is asked for
 * once; until it answers, and where there is none, the kernels run on the CPU.
 */
export function ensureImageDevice(): Promise<void> {
  if (devicePromise) return devicePromise;
  devicePromise = (async () => {
    if (getImageDevice() || typeof navigator === "undefined") return;
    // Typed here rather than pulled in as a global, so the WebGPU types stay inside @ndbx/core.
    const gpu = (navigator as { gpu?: { requestAdapter(): Promise<{ requestDevice(): Promise<unknown> } | null> } })
      .gpu;
    if (!gpu) return;
    try {
      const adapter = await gpu.requestAdapter();
      const device = await adapter?.requestDevice();
      if (device) installImageDevice(device as never);
    } catch (e) {
      console.warn("No WebGPU device; the pixel nodes will run on the CPU.", e);
    }
  })();
  return devicePromise;
}

/** Render one item of the project with the core engine and return its primary result. */
export async function renderItemWithCore(
  cx: Context,
  item: Item,
  data: Record<string, unknown> = {},
): Promise<unknown> {
  if (cx.project.__classicSource !== undefined) return renderClassicItem(cx, item, data);
  await ensureCoreFonts();
  await ensureImageDevice();
  const { main, all } = coreLibraries(cx);
  const functions = builtinFunctionRepository().combine(
    liveFunctions(all, {
      assetMap: cx.assetMap,
      resolveBareImport: (specifier) => (specifier === "@ndbx/g" ? config.bareImportReplacer(specifier) : specifier),
    }),
  );
  const state = states.get(cx)!;
  // Runtime nodes (with their state and loaded modules) live across renders in `persistent`.
  const persistentKey = `self/self/${item.name}`;
  const persistent =
    (state.contexts.get(persistentKey)?.persistent as Map<string, unknown> | undefined) ?? new Map<string, unknown>();
  const context = new NodeContext(main, functions, { data: { frame: 1, ...data }, persistent });
  state.contexts.set(persistentKey, context);
  const results = await context.render(`/${item.name}`);
  return primaryResult(await Promise.all(results.map(fromDevice)));
}

/** A raster that ran its kernels on the device comes back to the CPU to be shown or exported. */
async function fromDevice(value: unknown): Promise<unknown> {
  if (!GpuRaster.isGpuRaster(value)) return value;
  const device = getImageDevice();
  if (!device) return value;
  const raster = await device.readback(value);
  value.release();
  return raster;
}

/** A rendered list becomes what the viewer expects: one shape tree, a table, or a single value. */
function primaryResult(results: unknown[]): unknown {
  if (results.length === 0) return null;
  if (results.length === 1) {
    const only = results[0];
    return toG(only) ?? only;
  }
  const shape = toG(results);
  if (shape && results.every((r) => toG(r) !== null)) return shape;
  return results;
}

export { isNativeItem };
