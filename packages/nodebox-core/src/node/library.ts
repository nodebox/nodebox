import type { Node } from './node.js';

export interface NodeLibrary {
  formatVersion: number;
  uuid: string;
  root: Node;
  properties: Record<string, string>;
  functionLinks: string[];
}

export function createNodeLibrary(root: Node): NodeLibrary {
  return {
    formatVersion: 21,
    uuid: generateUUID(),
    root,
    properties: {
      canvasWidth: '1000',
      canvasHeight: '1000',
    },
    functionLinks: [],
  };
}

export function getCanvasWidth(lib: NodeLibrary): number {
  return parseInt(lib.properties.canvasWidth ?? '1000', 10);
}

export function getCanvasHeight(lib: NodeLibrary): number {
  return parseInt(lib.properties.canvasHeight ?? '1000', 10);
}

// Build a flat map of path → node for efficient lookup during evaluation
export function flattenNodeMap(node: Node, basePath = ''): Map<string, Node> {
  const map = new Map<string, Node>();
  const path = basePath ? `${basePath}/${node.name}` : node.name;
  map.set(path, node);
  for (const child of node.children) {
    const childMap = flattenNodeMap(child, path);
    for (const [k, v] of childMap) {
      map.set(k, v);
    }
  }
  return map;
}

function generateUUID(): string {
  // Simple UUID v4 generator
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
