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
  isNativeItem,
  liveFunctions,
  nativeLibraryProjects,
  parseLiveProject,
  toG,
} from "@ndbx/core";
import type { LiveProject } from "@ndbx/core";
import Context from "./context";
import { config } from "./loaders";
import { Item, Project } from "./types";

/** Whether the project must run on the core engine (native NodeBox 3 items or subnetwork calls). */
export function needsCoreEngine(project: Project): boolean {
  if (Object.keys(project.dependencies ?? {}).some((key) => key.startsWith("nodebox/"))) return true;
  const networkNames = new Set(project.items.filter((item) => item.type === "NETWORK").map((item) => item.name));
  return project.items.some(
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
    const library = parseLiveProject(project as unknown as LiveProject, { projectKey: key, repository, upgrade: false }).library;
    repository.add(library);
    all.push(library);
  }
  // Built-in NodeBox 3 libraries resolve against the core repository, not against Live items.
  for (const library of builtinNodeRepository().getLibraries()) repository.add(library);
  const main = parseLiveProject(cx.project as unknown as LiveProject, { projectKey: "self/self", repository, upgrade: false }).library;
  repository.add(main);
  all.push(main);
  state.libraries = new Map(all.map((l) => [l.name, l]));
  state.repository = repository;
  return { main, all, repository };
}

/** Render one item of the project with the core engine and return its primary result. */
export async function renderItemWithCore(cx: Context, item: Item, data: Record<string, unknown> = {}): Promise<unknown> {
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
  const persistent = (state.contexts.get(persistentKey)?.persistent as Map<string, unknown> | undefined) ?? new Map<string, unknown>();
  const context = new NodeContext(main, functions, { data: { frame: 1, ...data }, persistent });
  state.contexts.set(persistentKey, context);
  const results = await context.render(`/${item.name}`);
  return primaryResult(results);
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
