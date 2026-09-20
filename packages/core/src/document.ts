// High-level entry points: open a document in any supported format and render it.

import { Library } from "./model/types";
import { NodeRepository } from "./model/library";
import { parseNdbx, ReadResult } from "./ndbx/reader";
import { writeNdbx } from "./ndbx/writer";
import { builtinFunctionRepository } from "./functions";
import { builtinNodeRepository } from "./libraries";
import { ContextOptions, NodeContext } from "./runtime/context";
import { FunctionRepository } from "./runtime/function-repository";

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
