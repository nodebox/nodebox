// The function side of classic NodeBox Live: a namespace object per project, holding plain
// JavaScript functions that nodes call positionally.
//
// A classic project's code functions are source strings that assign themselves into their
// project's namespace ("g.frame = function () { ... }"). The original runtime evaluated them
// against the browser's global object; here they are evaluated against namespace objects passed in
// as arguments, so projects cannot reach or clobber anything else. Library functions with no
// source (most of core/g) come from a namespace the host supplies, such as the g.js package.

import { FunctionLibrary, FunctionRepository, JavaScriptLibrary } from "../runtime/function-repository";
import { ClassicProject } from "./classic-types";

export type ClassicNamespace = Record<string, unknown>;

/** The `ndbx` object a classic function source sees. */
export interface ClassicGlobals {
  /** The frame being rendered; `g.frame` returns it. */
  _currentFrame: number;
  _startTime: number;
  _mousePosition: { x: number; y: number };
  /** Loaded assets, by file name. */
  assets: Record<string, unknown>;
  findAsset(name: string): unknown;
  elapsedSeconds(): number;
  frame(): number;
  randomGenerator(seed: number): (min?: number, max?: number) => number;
  randomNumber(min: number, max: number, seed?: number): number;
  randomInt(min: number, max: number, seed?: number): number;
  /** Scratch space the sources keep their own caches in. */
  _state: Record<string, unknown>;
  _fonts: Record<string, unknown>;
  _fetchCache: Record<string, unknown>;
  _assetCache: Record<string, unknown>;
}

export function createClassicGlobals(): ClassicGlobals {
  const globals: ClassicGlobals = {
    _currentFrame: 1,
    _startTime: Date.now(),
    _mousePosition: { x: 0, y: 0 },
    assets: {},
    findAsset: (name) => globals.assets[name],
    elapsedSeconds: () => (Date.now() - globals._startTime) / 1000,
    frame: () => globals._currentFrame,
    randomGenerator,
    randomNumber: (min, max, seed) =>
      seed === undefined ? min + Math.random() * (max - min) : randomGenerator(seed)(min, max),
    randomInt: (min, max, seed) => Math.round(globals.randomNumber(min, max, seed)),
    _state: {},
    _fonts: {},
    _fetchCache: {},
    _assetCache: {},
  };
  return globals;
}

/**
 * The seeded generator classic NodeBox Live used, kept bit for bit: a linear congruential
 * generator, with negative seeds folded onto positive ones.
 */
export function randomGenerator(seed: number): (min?: number, max?: number) => number {
  if (seed < 0) {
    const generator = randomGenerator(Math.abs(seed));
    for (let i = 0; i < 23; i += 1) generator();
    return randomGenerator(generator(0, 10000));
  }
  let state = seed;
  return (min = 0, max = 1) => {
    state = (state * 9301 + 49297) % 233280;
    return min + (state / 233280) * (max - min);
  };
}

export interface ClassicRuntimeOptions {
  /**
   * Namespaces the projects start from, by project id: the libraries whose functions have no
   * source of their own, such as `{ g: gPackage }` for core/g.
   */
  namespaces?: Record<string, ClassicNamespace>;
  /** Further names the function sources see, such as `_` for lodash or `opentype`. */
  globals?: Record<string, unknown>;
}

/**
 * The namespaces of a set of classic projects, and the function repository over them. Sources are
 * evaluated once, when their project is added.
 */
export class ClassicRuntime {
  readonly ndbx: ClassicGlobals = createClassicGlobals();
  /**
   * The object classic sources reach for as `window`. They were written for a browser and use it
   * as a place to keep caches; a host that has a real window can pass one through `globals`.
   */
  readonly window: Record<string, unknown> = {};
  private readonly namespaces = new Map<string, ClassicNamespace>();
  private readonly extraGlobals: Record<string, unknown>;

  constructor(options: ClassicRuntimeOptions = {}) {
    this.extraGlobals = options.globals ?? {};
    for (const [name, namespace] of Object.entries(options.namespaces ?? {})) {
      // Copy, so evaluating a source cannot change the package the host passed in.
      this.namespaces.set(name, { ...namespace });
    }
  }

  namespace(name: string): ClassicNamespace {
    let namespace = this.namespaces.get(name);
    if (!namespace) {
      namespace = {};
      this.namespaces.set(name, namespace);
    }
    return namespace;
  }

  namespaceNames(): string[] {
    return [...this.namespaces.keys()];
  }

  /** Evaluate every code function of a project into its namespace. Returns the errors met. */
  addProject(project: ClassicProject, namespaceName?: string): string[] {
    const name = namespaceName ?? project.id ?? "self";
    this.namespace(name);
    const warnings: string[] = [];
    for (const fn of project.functions ?? []) {
      if (fn.type !== "code" || !fn.source) continue;
      try {
        this.evaluate(fn.source);
      } catch (e) {
        warnings.push(`Could not load ${name}.${fn.name}: ${(e as Error).message}`);
      }
    }
    return warnings;
  }

  /** Run a source against the namespaces, the way the classic runtime ran it against globals. */
  evaluate(source: string): void {
    const bindings = new Map<string, unknown>([["window", this.window], ...Object.entries(this.extraGlobals)]);
    // A namespace wins over a global of the same name: that is the object the source writes into.
    bindings.set("ndbx", this.ndbx);
    for (const [name, namespace] of this.namespaces) bindings.set(name, namespace);
    new Function(...bindings.keys(), source)(...bindings.values());
  }

  /**
   * Run a script asset the way a `<script>` tag did: its top-level names become globals, so that
   * function sources loaded later can call them. This is the one place a classic project reaches
   * outside its sandbox, and it is what the original runtime did with a `.js` asset.
   */
  loadScript(source: string): void {
    (0, eval)(source);
  }

  library(name: string): FunctionLibrary {
    return new JavaScriptLibrary(name, this.namespace(name) as Record<string, (...args: any[]) => unknown>);
  }

  repository(): FunctionRepository {
    const repository = new FunctionRepository();
    for (const name of this.namespaces.keys()) repository.add(this.library(name));
    return repository;
  }
}
