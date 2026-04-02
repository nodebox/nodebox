import type { NodeLibrary } from '../types/node';
import { createDefaultLibrary } from '../types/node';

export interface HistorySlice {
  undoStack: NodeLibrary[];
  redoStack: NodeLibrary[];
  maxHistory: number;

  pushSnapshot: (library: NodeLibrary) => void;
  undo: () => NodeLibrary | null;
  redo: () => NodeLibrary | null;
  clearHistory: () => void;
}

export const createHistorySlice = (
  set: (fn: (state: HistorySlice) => void) => void,
  get: () => HistorySlice,
): HistorySlice => ({
  undoStack: [],
  redoStack: [],
  maxHistory: 50,

  pushSnapshot: (library) =>
    set((state) => {
      state.undoStack.push(structuredClone(library));
      if (state.undoStack.length > state.maxHistory) {
        state.undoStack.shift();
      }
      state.redoStack = [];
    }),

  undo: () => {
    const { undoStack } = get();
    if (undoStack.length === 0) return null;
    const snapshot = undoStack[undoStack.length - 1];
    set((state) => {
      const popped = state.undoStack.pop();
      if (popped) {
        state.redoStack.push(popped);
      }
    });
    return snapshot ?? createDefaultLibrary();
  },

  redo: () => {
    const { redoStack } = get();
    if (redoStack.length === 0) return null;
    const snapshot = redoStack[redoStack.length - 1];
    set((state) => {
      const popped = state.redoStack.pop();
      if (popped) {
        state.undoStack.push(popped);
      }
    });
    return snapshot ?? createDefaultLibrary();
  },

  clearHistory: () =>
    set((state) => {
      state.undoStack = [];
      state.redoStack = [];
    }),
});
