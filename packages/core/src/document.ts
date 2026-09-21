// High-level entry points: open a document in any supported format and render it.

import { Library } from "./model/types";
import { NodeRepository } from "./model/library";
import { parseNdbx, ReadResult } from "./ndbx/reader";
import { writeNdbx } from "./ndbx/writer";
import { builtinFunctionRepository } from "./functions";
import { builtinNodeRepository } from "./libraries";
import { ContextOptions, NodeContext } from "./runtime/context";
import { FunctionRepository } from "./runtime/function-repository";
import { LiveFunctionLibrary, LiveLibraryOptions } from "./live/library";
import { LiveProject } from "./live/types";
import { LiveReadResult, parseLiveProject } from "./live/reader";
import { writeLiveProject } from "./live/writer";

export interface OpenOptions {
  /** Where the document came from; used for relative paths and the library name. */
  file?: string;
  name?: string;
  /** Extra node libraries (user libraries) that prototypes may reference. */
  repository?: NodeRepository;
}

/** Recognize the format of a document from its text. */
export function detectFormat(text: string): "ndbx" | "live" | "unknown" {
  const head = text.slice(0, 512).trimStart();
  if (head.startsWith("<")) return "ndbx";
  if (head.startsWith("{")) return "live";
  return "unknown";
}

/** Open a .ndbx document; upgrades older format versions on the way in. */
export function openNdbx(xml: string, options: OpenOptions = {}): ReadResult {
  const repository = options.repository ?? builtinNodeRepository();
  return parseNdbx(xml, { repository, file: options.file, name: options.name });
}

export function saveNdbx(library: Library, repository?: NodeRepository): string {
  return writeNdbx(library, { repository: repository ?? builtinNodeRepository() });
}

export interface OpenLiveOptions {
  /** "userId/projectId" of the project. Defaults to "self/self". */
  projectKey?: string;
  /** Libraries the project depends on (e.g. core/g), already loaded as Live libraries. */
  dependencies?: Library[];
  repository?: NodeRepository;
}

/** Open a NodeBox Live project.json; dependency prototypes resolve against the given libraries. */
export function openLive(json: string | LiveProject, options: OpenLiveOptions = {}): LiveReadResult {
  const repository = options.repository ?? NodeRepository.of(...(options.dependencies ?? []));
  return parseLiveProject(json, { projectKey: options.projectKey, repository });
}

export function saveLive(library: Library): LiveProject {
  return writeLiveProject(library);
}

/** A function library serving the JavaScript items of the given Live libraries. */
export function liveFunctions(libraries: Library[], options: LiveLibraryOptions = {}): FunctionRepository {
  const live = new LiveFunctionLibrary(options);
  for (const library of libraries) live.addLibrary(library);
  return FunctionRepository.of(live);
}

export interface RenderOptions extends ContextOptions {
  functionRepository?: FunctionRepository;
  /** The node to render; defaults to the root network. */
  nodePath?: string;
}

/** Create an evaluation context for a library with the built-in functions plus any given ones. */
export function createContext(library: Library, options: RenderOptions = {}): NodeContext {
  const functions = options.functionRepository
    ? builtinFunctionRepository().combine(options.functionRepository)
    : builtinFunctionRepository();
  return new NodeContext(library, functions, options);
}

/** Render a library and return the primary result list of the rendered node. */
export async function renderLibrary(library: Library, options: RenderOptions = {}): Promise<unknown[]> {
  const context = createContext(library, options);
  const result = await context.render(options.nodePath ?? "/");
  await context.renderAlwaysRenderedNodes(options.nodePath ?? "/");
  return result;
}
