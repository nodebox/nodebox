import type { NodeLibrary } from 'nodebox-core';

const MAX_HISTORY = 50;

export interface HistorySlice {
  undoStack: NodeLibrary[];
  redoStack: NodeLibrary[];
  pushHistory: () => void;
  undo: () => void;
  redo: () => void;
  canUndo: () => boolean;
  canRedo: () => boolean;
}

export function createHistorySlice(set: any, get: any): HistorySlice {
  return {
    undoStack: [],
    redoStack: [],

    pushHistory: () => set((state: any) => {
      if (state.library) {
        // Deep clone via structuredClone
        state.undoStack.push(structuredClone(state.library));
        if (state.undoStack.length > MAX_HISTORY) {
          state.undoStack.shift();
        }
        state.redoStack = [];
      }
    }),

    undo: () => set((state: any) => {
      if (state.undoStack.length === 0) return;
      if (state.library) {
        state.redoStack.push(structuredClone(state.library));
      }
      state.library = state.undoStack.pop();
    }),

    redo: () => set((state: any) => {
      if (state.redoStack.length === 0) return;
      if (state.library) {
        state.undoStack.push(structuredClone(state.library));
      }
      state.library = state.redoStack.pop();
    }),

    canUndo: () => get().undoStack.length > 0,
    canRedo: () => get().redoStack.length > 0,
  };
}
