// Functions are what nodes call. A library publishes them under a namespace ("math/add"); a
// repository combines libraries. Backends differ (built-in TypeScript, NodeBox Live JavaScript
// modules, Python through an installed loader), so the interface is deliberately small.

import type { Node } from "../model/types";
import type { NodeContext } from "./context";

export interface Invocation {
  context: NodeContext;
  node: Node;
  nodePath: string;
}

export interface NodeFunction {
  name: string;
  /** Called with one positional argument per input port, in port order. */
  invoke(args: unknown[], invocation: Invocation): unknown | Promise<unknown>;
  /** True when the function evaluates port expressions itself (NodeBox Live modules do). */
  handlesExpressions?: boolean;
  /** True when the result may change between renders with equal inputs (network, devices). */
  impure?: boolean;
}

export interface FunctionLibrary {
  namespace: string;
  getFunction(name: string): NodeFunction | undefined;
  getFunctionNames(): string[];
  /** Optional link this library was loaded from ("python:pyvector.py"). */
  link?: string;
}

/** A plain object of functions becomes a library; each function receives the positional arguments. */
export class JavaScriptLibrary implements FunctionLibrary {
  namespace: string;
  link?: string;
  private functions = new Map<string, NodeFunction>();

  constructor(namespace: string, functions: Record<string, (...args: any[]) => unknown> = {}, options: { link?: string; impure?: string[] } = {}) {
    this.namespace = namespace;
    this.link = options.link;
    for (const [name, fn] of Object.entries(functions)) {
      this.add(name, fn, options.impure?.includes(name));
    }
  }

  add(name: string, fn: (...args: any[]) => unknown, impure = false): void {
    // Plain functions receive exactly the port values; a function that needs the evaluation
    // context declares a port of type "context", as NodeBox 3 nodes do.
    this.functions.set(name, {
      name,
      impure,
      invoke: (args) => fn(...args),
    });
  }

  addFunction(fn: NodeFunction): void {
    this.functions.set(fn.name, fn);
  }

  getFunction(name: string): NodeFunction | undefined {
    return this.functions.get(name);
  }

  getFunctionNames(): string[] {
    return Array.from(this.functions.keys());
  }
}

export class FunctionNotFoundError extends Error {}

export class FunctionRepository {
  private libraries = new Map<string, FunctionLibrary>();

  static of(...libraries: FunctionLibrary[]): FunctionRepository {
    const repo = new FunctionRepository();
    for (const lib of libraries) repo.add(lib);
    return repo;
  }

  add(library: FunctionLibrary): void {
    this.libraries.set(library.namespace, library);
  }

  remove(namespace: string): void {
    this.libraries.delete(namespace);
  }

  has(namespace: string): boolean {
    return this.libraries.has(namespace);
  }

  getLibrary(namespace: string): FunctionLibrary | undefined {
    return this.libraries.get(namespace);
  }

  getLibraries(): FunctionLibrary[] {
    return Array.from(this.libraries.values());
  }

  /** A repository with this one's libraries plus the given ones (later ones win). */
  combine(...others: FunctionRepository[]): FunctionRepository {
    const repo = new FunctionRepository();
    for (const lib of this.libraries.values()) repo.add(lib);
    for (const other of others) for (const lib of other.libraries.values()) repo.add(lib);
    return repo;
  }

  hasFunction(identifier: string): boolean {
    return this.findFunction(identifier) !== undefined;
  }

  findFunction(identifier: string): NodeFunction | undefined {
    const i = identifier.indexOf("/");
    if (i < 0) return undefined;
    const library = this.libraries.get(identifier.slice(0, i));
    return library?.getFunction(identifier.slice(i + 1));
  }

  getFunction(identifier: string): NodeFunction {
    const fn = this.findFunction(identifier);
    if (!fn) {
      const i = identifier.indexOf("/");
      const namespace = i < 0 ? identifier : identifier.slice(0, i);
      if (!this.libraries.has(namespace)) throw new FunctionNotFoundError(`Function library '${namespace}' is not loaded (function ${identifier}).`);
      throw new FunctionNotFoundError(`Function '${identifier}' does not exist.`);
    }
    return fn;
  }
}
