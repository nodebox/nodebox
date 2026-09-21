// Load NodeBox Live function sources as ES modules. Sources import "@ndbx/g" and other project
// items through "project:Name"; both are rewritten to URLs the host can import: a data: URL for
// project items (built recursively), and whatever the host says for bare specifiers.

export type Initializer = (node: unknown) => void;

export interface LoadedModule {
  initializer?: Initializer;
  exports: Record<string, unknown>;
}

export interface ModuleLoaderOptions {
  /** Resolve a bare import such as "@ndbx/g" or "https://esm.sh/d3" to an importable URL. */
  resolveBareImport?: (specifier: string) => string | Promise<string>;
  /** Find the source of "project:Name" within a project ("userId/projectId"). */
  resolveProjectImport?: (projectKey: string, itemName: string) => string | undefined;
}

const BARE_IMPORT_RE =
  /(\bimport\s*(?:[\w*{}\s,$]+\s*from\s*)?|\bexport\s*(?:[\w*{}\s,$]+\s*from\s*))(["'])([^"']+)\2/g;
const DYNAMIC_IMPORT_RE = /\bimport\(\s*(["'])([^"']+)\1\s*\)/g;

function defaultResolveBareImport(specifier: string): string {
  if (/^(https?:|data:|file:|blob:|\/|\.\.?\/)/.test(specifier)) return specifier;
  const meta = import.meta as { resolve?: (s: string) => string };
  if (typeof meta.resolve === "function") {
    try {
      const resolved = meta.resolve(specifier);
      // Bundlers may hand back the bare name again; only trust a real URL or path.
      if (resolved !== specifier && /^(https?:|data:|file:|blob:|\/)/.test(resolved)) return resolved;
    } catch {
      // fall through to the CDN
    }
  }
  return `https://esm.sh/${specifier}`;
}

export class LiveModuleLoader {
  private cache = new Map<string, Promise<LoadedModule>>();
  private urlCache = new Map<string, Promise<string>>();
  private options: ModuleLoaderOptions;

  constructor(options: ModuleLoaderOptions = {}) {
    this.options = options;
  }

  /** Import the module for a function item; results are cached per project item and source. */
  load(projectKey: string, itemName: string, source: string): Promise<LoadedModule> {
    const key = `${projectKey}/${itemName}\u0000${hash(source)}`;
    let promise = this.cache.get(key);
    if (!promise) {
      promise = this.importSource(projectKey, itemName, source);
      this.cache.set(key, promise);
    }
    return promise;
  }

  invalidate(projectKey?: string, itemName?: string): void {
    if (!projectKey) {
      this.cache.clear();
      this.urlCache.clear();
      return;
    }
    const prefix = itemName ? `${projectKey}/${itemName}\u0000` : `${projectKey}/`;
    for (const key of [...this.cache.keys()]) if (key.startsWith(prefix)) this.cache.delete(key);
    for (const key of [...this.urlCache.keys()]) if (key.startsWith(prefix)) this.urlCache.delete(key);
  }

  private async importSource(projectKey: string, itemName: string, source: string): Promise<LoadedModule> {
    const url = await this.moduleUrl(projectKey, itemName, source, new Set());
    const module = (await import(/* @vite-ignore */ url)) as Record<string, unknown>;
    const initializer = typeof module.default === "function" ? (module.default as Initializer) : undefined;
    return { initializer, exports: module };
  }

  /** The importable URL of a source, with its imports rewritten (recursively for project imports). */
  private moduleUrl(projectKey: string, itemName: string, source: string, stack: Set<string>): Promise<string> {
    const key = `${projectKey}/${itemName}\u0000${hash(source)}`;
    let promise = this.urlCache.get(key);
    if (!promise) {
      promise = this.buildUrl(projectKey, itemName, source, stack);
      this.urlCache.set(key, promise);
    }
    return promise;
  }

  private async buildUrl(projectKey: string, itemName: string, source: string, stack: Set<string>): Promise<string> {
    const id = `${projectKey}/${itemName}`;
    if (stack.has(id)) throw new Error(`Circular import between project items: ${[...stack, id].join(" -> ")}`);
    stack.add(id);
    const rewritten = await this.rewriteImports(projectKey, source, stack);
    stack.delete(id);
    return toDataUrl(rewritten);
  }

  private async rewriteImports(projectKey: string, source: string, stack: Set<string>): Promise<string> {
    const replacements = new Map<string, string>();
    const specifiers = new Set<string>();
    for (const m of source.matchAll(BARE_IMPORT_RE)) specifiers.add(m[3]);
    for (const m of source.matchAll(DYNAMIC_IMPORT_RE)) specifiers.add(m[2]);
    for (const specifier of specifiers) {
      if (specifier.startsWith("project:")) {
        const target = specifier.slice("project:".length);
        const targetSource = this.options.resolveProjectImport?.(projectKey, target);
        if (targetSource === undefined) throw new Error(`Cannot import "${specifier}": no item named ${target}.`);
        replacements.set(specifier, await this.moduleUrl(projectKey, target, targetSource, stack));
      } else {
        const resolve = this.options.resolveBareImport ?? defaultResolveBareImport;
        replacements.set(specifier, await resolve(specifier));
      }
    }
    let out = source.replace(BARE_IMPORT_RE, (match, head: string, quote: string, spec: string) =>
      replacements.has(spec) ? `${head}${quote}${replacements.get(spec)}${quote}` : match,
    );
    out = out.replace(DYNAMIC_IMPORT_RE, (match, quote: string, spec: string) =>
      replacements.has(spec) ? `import(${quote}${replacements.get(spec)}${quote})` : match,
    );
    return out;
  }
}

function toDataUrl(source: string): string {
  if (
    typeof Blob !== "undefined" &&
    typeof URL !== "undefined" &&
    typeof URL.createObjectURL === "function" &&
    isBrowser()
  ) {
    return URL.createObjectURL(new Blob([source], { type: "text/javascript" }));
  }
  return `data:text/javascript;charset=utf-8,${encodeURIComponent(source)}`;
}

function isBrowser(): boolean {
  return typeof window !== "undefined" && typeof document !== "undefined";
}

function hash(s: string): string {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return (h >>> 0).toString(36);
}
