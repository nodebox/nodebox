import type { NodeLibrary, Connection } from '../types/node';
import type { Value } from '../types/value';
import { createDefaultLibrary } from '../types/node';

export interface LibrarySlice {
  library: NodeLibrary;
  filePath: string | null;
  isDirty: boolean;

  setLibrary: (library: NodeLibrary) => void;
  addNode: (
    parentPath: string,
    node: NodeLibrary['root'],
  ) => void;
  removeNode: (parentPath: string, nodeName: string) => void;
  setPortValue: (
    nodePath: string,
    portName: string,
    value: Value,
  ) => void;
  addConnection: (parentPath: string, connection: Connection) => void;
  removeConnection: (
    parentPath: string,
    inputNode: string,
    inputPort: string,
  ) => void;
  setRenderedChild: (parentPath: string, childName: string | null) => void;
  setNodePosition: (nodeName: string, position: { x: number; y: number }) => void;
  setCanvasProperty: (key: string, value: string) => void;
  setFilePath: (path: string | null) => void;
  markClean: () => void;
}

export const createLibrarySlice = (
  set: (fn: (state: LibrarySlice) => void) => void,
): LibrarySlice => ({
  library: createDefaultLibrary(),
  filePath: null,
  isDirty: false,

  setLibrary: (library) =>
    set((state) => {
      state.library = library;
      state.isDirty = false;
    }),

  addNode: (_parentPath, node) =>
    set((state) => {
      state.library.root.children.push(node);
      state.isDirty = true;
    }),

  removeNode: (_parentPath, nodeName) =>
    set((state) => {
      const root = state.library.root;
      root.children = root.children.filter((n) => n.name !== nodeName);
      root.connections = root.connections.filter(
        (c) => c.outputNode !== nodeName && c.inputNode !== nodeName,
      );
      if (root.renderedChild === nodeName) {
        root.renderedChild = null;
      }
      state.isDirty = true;
    }),

  setPortValue: (nodeName, portName, value) =>
    set((state) => {
      const node = state.library.root.children.find((n) => n.name === nodeName);
      if (node) {
        const port = node.inputs.find((p) => p.name === portName);
        if (port) {
          port.value = value;
          state.isDirty = true;
        }
      }
    }),

  addConnection: (_parentPath, connection) =>
    set((state) => {
      const root = state.library.root;
      root.connections = root.connections.filter(
        (c) =>
          !(
            c.inputNode === connection.inputNode &&
            c.inputPort === connection.inputPort
          ),
      );
      root.connections.push(connection);
      state.isDirty = true;
    }),

  removeConnection: (_parentPath, inputNode, inputPort) =>
    set((state) => {
      state.library.root.connections =
        state.library.root.connections.filter(
          (c) => !(c.inputNode === inputNode && c.inputPort === inputPort),
        );
      state.isDirty = true;
    }),

  setRenderedChild: (_parentPath, childName) =>
    set((state) => {
      state.library.root.renderedChild = childName;
      state.isDirty = true;
    }),

  setNodePosition: (nodeName, position) =>
    set((state) => {
      const node = state.library.root.children.find((n) => n.name === nodeName);
      if (node) {
        node.position = position;
        state.isDirty = true;
      }
    }),

  setCanvasProperty: (key, value) =>
    set((state) => {
      state.library.properties[key] = value;
      state.isDirty = true;
    }),

  setFilePath: (path) =>
    set((state) => {
      state.filePath = path;
    }),

  markClean: () =>
    set((state) => {
      state.isDirty = false;
    }),
});
