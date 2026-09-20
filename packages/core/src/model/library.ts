import { Rect } from "../graphics/rect";
import { CORE_NODES, childPath, createNetworkNode, extendNode, getChild } from "./node";
import { Library, Node } from "./types";

export const CURRENT_NDBX_FORMAT_VERSION = "22";

export function createLibrary(name: string, root?: Node): Library {
  const rootNode = root ?? extendNode(createNetworkNode(), "core.network", "root");
  return {
    name,
    root: rootNode,
    properties: {},
    devices: [],
    functionLinks: [],
    dependencies: {},
    assets: {},
    sourceFormat: "memory",
    meta: {},
  };
}

/** Every node in the library, keyed by absolute path: "/", "/rect1", "/net1/child2". */
export function flattenedNodeMap(library: Library): Map<string, Node> {
  const map = new Map<string, Node>();
  const visit = (node: Node, path: string) => {
    map.set(path, node);
    for (const child of node.children) visit(child, childPath(path, child.name));
  };
  visit(library.root, "/");
  return map;
}

export function getNodeForPath(library: Library, path: string): Node | undefined {
  if (path === "/" || path === "") return library.root;
  let node: Node | undefined = library.root;
  for (const part of path.split("/").filter((s) => s.length > 0)) {
    node = getChild(node, part);
    if (!node) return undefined;
  }
  return node;
}

export function parentPath(path: string): string {
  const i = path.lastIndexOf("/");
  return i <= 0 ? "/" : path.slice(0, i);
}

/** The canvas bounds from the canvasX/Y/Width/Height properties, centered like NodeBox 3's getBounds(). */
export function libraryBounds(library: Library): Rect {
  const num = (key: string) => Number(library.properties[key] ?? 0) || 0;
  return Rect.centeredRect(num("canvasX"), num("canvasY"), num("canvasWidth"), num("canvasHeight"));
}

/**
 * A set of libraries by name, used to resolve prototype ids such as "corevector.rect".
 * "core.node" and "core.network" resolve to the built-in root and network nodes, even without a
 * core library loaded, and "_root" resolves to the root node.
 */
export class NodeRepository {
  private libraries = new Map<string, Library>();

  static of(...libraries: Library[]): NodeRepository {
    const repo = new NodeRepository();
    for (const lib of libraries) repo.add(lib);
    return repo;
  }

  add(library: Library): void {
    this.libraries.set(library.name, library);
  }

  remove(name: string): void {
    this.libraries.delete(name);
  }

  has(name: string): boolean {
    return this.libraries.has(name);
  }

  getLibrary(name: string): Library | undefined {
    return this.libraries.get(name);
  }

  getLibraries(): Library[] {
    return Array.from(this.libraries.values());
  }

  /** Resolve "library.node" to the prototype node, or undefined when unknown. */
  getNode(identifier: string): Node | undefined {
    if (identifier === "_root") return CORE_NODES.ROOT();
    const i = identifier.indexOf(".");
    if (i < 0) return undefined;
    const libraryName = identifier.slice(0, i);
    const nodeName = identifier.slice(i + 1);
    const library = this.libraries.get(libraryName);
    if (library) {
      const node = getChild(library.root, nodeName);
      if (node) return node;
    }
    if (libraryName === "core") {
      if (nodeName === "node") return CORE_NODES.ROOT();
      if (nodeName === "network") return CORE_NODES.NETWORK();
    }
    return undefined;
  }

  /** All prototype nodes across the libraries, with their "library.node" ids. */
  getNodes(): { id: string; node: Node; library: Library }[] {
    const result: { id: string; node: Node; library: Library }[] = [];
    for (const library of this.libraries.values()) {
      for (const node of library.root.children) result.push({ id: `${library.name}.${node.name}`, node, library });
    }
    return result;
  }

  getCategories(): string[] {
    const categories = new Set<string>();
    for (const { node } of this.getNodes()) if (node.category) categories.add(node.category);
    return Array.from(categories).sort();
  }

  /** The "library.node" id of the prototype a node was created from, searching by identity or name. */
  nodeIdFor(node: Node): string | undefined {
    for (const { id, node: candidate } of this.getNodes()) if (candidate === node) return id;
    return undefined;
  }
}

export function nodePathsOf(library: Library): string[] {
  return Array.from(flattenedNodeMap(library).keys());
}

export function childPathOf(networkPath: string, childName: string): string {
  return childPath(networkPath, childName);
}
