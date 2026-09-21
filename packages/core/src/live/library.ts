// The function library that runs NodeBox Live function items inside the unified evaluator.
//
// Each Live function item is one NodeFunction. Per node instance (node path) the bridge keeps a
// LiveRuntimeNode across renders, so nodes with state or asynchronous loading behave as they do
// in NodeBox Live. On every invocation it binds the arguments to the parameters and input ports,
// runs onRender and returns the output ports as named outputs.

import { Contour } from "../graphics/contour";
import { Geometry, Path } from "../graphics/path";
import { Text } from "../graphics/text";
import { isGShape, toG } from "../graphics/to-g";
import { Library, Node } from "../model/types";
import { NodeContext, Outputs } from "../runtime/context";
import { FunctionLibrary, Invocation, NodeFunction } from "../runtime/function-repository";
import { LiveModuleLoader, ModuleLoaderOptions } from "./module-loader";
import { LIVE_FUNCTION_NAMESPACE } from "./reader";
import { LiveContextLike, LiveRuntimeNode } from "./runtime-node";

export interface LiveLibraryOptions extends ModuleLoaderOptions {
  /** Assets by file name (text, ArrayBuffer or image), as Live's Context.assetMap. */
  assetMap?: Map<string, unknown>;
}

/**
 * Serves "live/<userId>/<projectId>/<Item>" functions for a set of libraries that came from
 * NodeBox Live projects (the main project and its dependencies).
 */
export class LiveFunctionLibrary implements FunctionLibrary {
  namespace = LIVE_FUNCTION_NAMESPACE;
  private sources = new Map<string, string>();
  private functions = new Map<string, NodeFunction>();
  private loader: LiveModuleLoader;
  assetMap: Map<string, unknown>;

  constructor(options: LiveLibraryOptions = {}) {
    this.assetMap = options.assetMap ?? new Map();
    this.loader = new LiveModuleLoader({
      resolveBareImport: options.resolveBareImport,
      resolveProjectImport: (projectKey, itemName) =>
        options.resolveProjectImport?.(projectKey, itemName) ?? this.sources.get(`${projectKey}/${itemName}`),
    });
  }

  /** Register the function items of a library loaded from a Live project. */
  addLibrary(library: Library): void {
    const projectKey = String(library.meta.projectKey ?? library.name);
    for (const link of library.functionLinks) {
      if (link.language !== "javascript" || link.source === undefined) continue;
      const itemName = link.href.startsWith("module:")
        ? link.href.slice("module:".length).split("/").slice(2).join("/")
        : link.href;
      this.addSource(projectKey, itemName, link.source);
    }
  }

  addSource(projectKey: string, itemName: string, source: string): void {
    const id = `${projectKey}/${itemName}`;
    if (this.sources.get(id) !== source) this.loader.invalidate(projectKey, itemName);
    this.sources.set(id, source);
    this.functions.delete(id);
  }

  getFunctionNames(): string[] {
    return Array.from(this.sources.keys());
  }

  getFunction(name: string): NodeFunction | undefined {
    if (!this.sources.has(name)) return undefined;
    let fn = this.functions.get(name);
    if (!fn) {
      fn = this.createFunction(name);
      this.functions.set(name, fn);
    }
    return fn;
  }

  private createFunction(id: string): NodeFunction {
    const [userId, projectId, ...rest] = id.split("/");
    const projectKey = `${userId}/${projectId}`;
    const itemName = rest.join("/");
    return {
      name: id,
      handlesExpressions: true,
      impure: true,
      invoke: async (args, invocation) => {
        const source = this.sources.get(id)!;
        const module = await this.loader.load(projectKey, itemName, source);
        const runtimeNode = this.runtimeNodeFor(invocation, module.initializer, id);
        return this.render(runtimeNode, args, invocation);
      },
    };
  }

  private runtimeNodeFor(
    invocation: Invocation,
    initializer: ((node: unknown) => void) | undefined,
    id: string,
  ): LiveRuntimeNode {
    const key = `live-node:${invocation.nodePath}`;
    const existing = invocation.context.persistent.get(key) as { id: string; node: LiveRuntimeNode } | undefined;
    if (existing && existing.id === id) {
      existing.node.cx = this.contextFor(invocation.context);
      return existing.node;
    }
    const node = new LiveRuntimeNode(this.contextFor(invocation.context), invocation.nodePath);
    if (initializer) initializer(node);
    else
      node.onRender = () => {
        node.message = "This module does not contain a default export and is treated as a utility module.";
      };
    invocation.context.persistent.set(key, { id, node });
    return node;
  }

  private contextFor(context: NodeContext): LiveContextLike {
    return {
      assetMap: this.assetMap,
      project: context.library,
      warnings: [],
      generateId: () => `${Math.random().toString(36).slice(2, 10)}:${Date.now()}`,
      lookupItemByName: () => undefined,
      frame: context.frame,
    };
  }

  private async render(runtimeNode: LiveRuntimeNode, args: unknown[], invocation: Invocation): Promise<Outputs> {
    const node: Node = invocation.node;
    const context = invocation.context;
    // Bind the arguments, port by port, in the node's declared order.
    node.inputs.forEach((port, i) => {
      const arg = args[i];
      const parameter = runtimeNode.parameters.find((p) => p.name === port.name);
      if (parameter) {
        parameter.binding = port.expression !== undefined ? { expression: port.expression } : { value: arg };
        return;
      }
      const input = runtimeNode.inputPorts.find((p) => p.name === port.name);
      if (input) input.set(toLiveValue(arg, port.type));
    });
    runtimeNode.globals = { network: networkScope(invocation), frame: context.frame, $FRAME: context.frame };
    runtimeNode.errors = [];
    await runtimeNode.onRender(runtimeNode.cx);
    if (runtimeNode.errors.length > 0) throw new Error(runtimeNode.errors.join("\n"));
    const outputs: Record<string, unknown> = {};
    for (const port of runtimeNode.outputPorts) outputs[port.name] = port._value;
    return new Outputs(outputs);
  }
}

/** The `network` object expressions see: the published and parameter values of the enclosing network. */
function networkScope(invocation: Invocation): Record<string, unknown> {
  const parentPath = invocation.nodePath.slice(0, invocation.nodePath.lastIndexOf("/")) || "/";
  const parent = invocation.context.getNodeForPath(parentPath);
  const scope: Record<string, unknown> = {};
  if (parent) for (const p of parent.inputs) scope[p.name] = p.value;
  return scope;
}

/** Values arriving from NodeBox 3 style nodes are converted to what Live ports expect. */
function toLiveValue(value: unknown, portType: string): unknown {
  if (value === null || value === undefined) return null;
  if (portType === "shape") {
    if (isGShape(value)) return value;
    if (value instanceof Path || value instanceof Geometry || value instanceof Text || value instanceof Contour)
      return toG(value);
    if (Array.isArray(value)) return toG(value);
    return value;
  }
  if (portType === "table") {
    if (!Array.isArray(value)) return [value];
    // Scalars become rows with a `value` column so expressions like `value` work on them.
    if (value.length > 0 && value.every((v) => v === null || typeof v !== "object"))
      return value.map((v) => ({ value: v }));
    return value;
  }
  return value;
}
