import type { NodeLibrary, Node, Connection, Value, Point } from 'nodebox-core';
import { createNode, createNodeLibrary } from 'nodebox-core';

function createDefaultLibrary(): NodeLibrary {
  return createNodeLibrary(createNode('root'));
}

export interface LibrarySlice {
  library: NodeLibrary;
  filePath: string | null;
  dirty: boolean;
  setLibrary: (library: NodeLibrary, filePath?: string) => void;
  addNodeToNetwork: (networkPath: string, node: Node) => void;
  removeNodeFromNetwork: (networkPath: string, nodeName: string) => void;
  addConnectionToNetwork: (networkPath: string, conn: Connection) => void;
  removeConnectionFromNetwork: (networkPath: string, inputNode: string, inputPort: string) => void;
  setPortValueAction: (nodePath: string, portName: string, value: Value) => void;
  setRenderedChildAction: (networkPath: string, childName: string | null) => void;
  setNodePositionAction: (networkPath: string, nodeName: string, position: Point) => void;
  setDirty: (dirty: boolean) => void;
}

export function createLibrarySlice(set: any, get: any): LibrarySlice {
  return {
    library: createDefaultLibrary(),
    filePath: null,
    dirty: false,

    setLibrary: (library, filePath) => set((state: LibrarySlice) => {
      state.library = library;
      state.filePath = filePath ?? null;
      state.dirty = false;
    }),

    // All mutations use immer's draft proxy — mutate in-place, no spread/filter needed.
    // Use splice() for removals instead of filter() to stay on the proxied array.

    addNodeToNetwork: (networkPath, node) => set((state: LibrarySlice) => {
      const network = findNode(state.library.root, networkPath);
      if (!network) return;
      network.children.push(node);
      state.dirty = true;
    }),

    removeNodeFromNetwork: (networkPath, nodeName) => set((state: LibrarySlice) => {
      const network = findNode(state.library.root, networkPath);
      if (!network) return;
      const childIdx = network.children.findIndex(c => c.name === nodeName);
      if (childIdx >= 0) network.children.splice(childIdx, 1);
      // Reverse iterate for safe in-place splice
      for (let i = network.connections.length - 1; i >= 0; i--) {
        const c = network.connections[i];
        if (c.inputNode === nodeName || c.outputNode === nodeName) {
          network.connections.splice(i, 1);
        }
      }
      if (network.renderedChild === nodeName) network.renderedChild = null;
      state.dirty = true;
    }),

    addConnectionToNetwork: (networkPath, conn) => set((state: LibrarySlice) => {
      const network = findNode(state.library.root, networkPath);
      if (!network) return;
      for (let i = network.connections.length - 1; i >= 0; i--) {
        const c = network.connections[i];
        if (c.inputNode === conn.inputNode && c.inputPort === conn.inputPort) {
          network.connections.splice(i, 1);
        }
      }
      network.connections.push(conn);
      state.dirty = true;
    }),

    removeConnectionFromNetwork: (networkPath, inputNode, inputPort) => set((state: LibrarySlice) => {
      const network = findNode(state.library.root, networkPath);
      if (!network) return;
      for (let i = network.connections.length - 1; i >= 0; i--) {
        const c = network.connections[i];
        if (c.inputNode === inputNode && c.inputPort === inputPort) {
          network.connections.splice(i, 1);
        }
      }
      state.dirty = true;
    }),

    setPortValueAction: (nodePath, portName, value) => set((state: LibrarySlice) => {
      const node = findNode(state.library.root, nodePath);
      if (!node) return;
      const port = node.inputs.find(p => p.name === portName);
      if (port) {
        port.value = value;
        state.dirty = true;
      }
    }),

    setRenderedChildAction: (networkPath, childName) => set((state: LibrarySlice) => {
      const network = findNode(state.library.root, networkPath);
      if (!network) return;
      network.renderedChild = childName;
      state.dirty = true;
    }),

    setNodePositionAction: (networkPath, nodeName, position) => set((state: LibrarySlice) => {
      const network = findNode(state.library.root, networkPath);
      if (!network) return;
      const child = network.children.find(c => c.name === nodeName);
      if (child) {
        child.position = position;
        // No dirty flag for position — cosmetic, high-frequency during drag
      }
    }),

    setDirty: (dirty) => set((state: LibrarySlice) => { state.dirty = dirty; }),
  };
}

function findNode(root: Node, path: string): Node | null {
  const parts = path.split('/').filter(Boolean);
  let current: Node = root;
  // Skip the root name if it matches
  const startIdx = parts[0] === current.name ? 1 : 0;
  for (let i = startIdx; i < parts.length; i++) {
    const child = current.children.find(c => c.name === parts[i]);
    if (!child) return null;
    current = child;
  }
  return current;
}
