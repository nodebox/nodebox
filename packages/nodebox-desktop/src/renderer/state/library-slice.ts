import type { NodeLibrary, Node, Connection, Value, Point } from 'nodebox-core';
import { createNode, addChild, removeChild, addConnection, removeConnection, setPortValue, setRenderedChild, setNodePosition } from 'nodebox-core';

export interface LibrarySlice {
  library: NodeLibrary | null;
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
    library: null,
    filePath: null,
    dirty: false,

    setLibrary: (library, filePath) => set((state: LibrarySlice) => {
      state.library = library;
      state.filePath = filePath ?? null;
      state.dirty = false;
    }),

    addNodeToNetwork: (networkPath, node) => set((state: LibrarySlice) => {
      if (!state.library) return;
      const network = findNode(state.library.root, networkPath);
      if (network) {
        network.children.push(node);
        state.dirty = true;
      }
    }),

    removeNodeFromNetwork: (networkPath, nodeName) => set((state: LibrarySlice) => {
      if (!state.library) return;
      const network = findNode(state.library.root, networkPath);
      if (network) {
        network.children = network.children.filter(c => c.name !== nodeName);
        network.connections = network.connections.filter(
          c => c.inputNode !== nodeName && c.outputNode !== nodeName,
        );
        state.dirty = true;
      }
    }),

    addConnectionToNetwork: (networkPath, conn) => set((state: LibrarySlice) => {
      if (!state.library) return;
      const network = findNode(state.library.root, networkPath);
      if (network) {
        // Remove existing connection to same input
        network.connections = network.connections.filter(
          c => !(c.inputNode === conn.inputNode && c.inputPort === conn.inputPort),
        );
        network.connections.push(conn);
        state.dirty = true;
      }
    }),

    removeConnectionFromNetwork: (networkPath, inputNode, inputPort) => set((state: LibrarySlice) => {
      if (!state.library) return;
      const network = findNode(state.library.root, networkPath);
      if (network) {
        network.connections = network.connections.filter(
          c => !(c.inputNode === inputNode && c.inputPort === inputPort),
        );
        state.dirty = true;
      }
    }),

    setPortValueAction: (nodePath, portName, value) => set((state: LibrarySlice) => {
      if (!state.library) return;
      const node = findNode(state.library.root, nodePath);
      if (node) {
        const port = node.inputs.find(p => p.name === portName);
        if (port) {
          port.value = value;
          state.dirty = true;
        }
      }
    }),

    setRenderedChildAction: (networkPath, childName) => set((state: LibrarySlice) => {
      if (!state.library) return;
      const network = findNode(state.library.root, networkPath);
      if (network) {
        network.renderedChild = childName;
        state.dirty = true;
      }
    }),

    setNodePositionAction: (networkPath, nodeName, position) => set((state: LibrarySlice) => {
      if (!state.library) return;
      const network = findNode(state.library.root, networkPath);
      if (network) {
        const child = network.children.find(c => c.name === nodeName);
        if (child) {
          child.position = position;
          state.dirty = true;
        }
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
