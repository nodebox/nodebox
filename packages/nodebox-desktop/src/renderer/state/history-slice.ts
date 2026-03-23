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

    // Capture the current (immutable) library via get() BEFORE entering the immer draft.
    // Immer guarantees get() returns a frozen snapshot, so no cloning is needed.
    pushHistory: () => {
      const snapshot = get().library;
      if (!snapshot) return;
      set((state: any) => {
        state.undoStack.push(snapshot);
        if (state.undoStack.length > MAX_HISTORY) {
          state.undoStack.shift();
        }
        state.redoStack = [];
      });
    },

    undo: () => {
      const { undoStack, library } = get();
      if (undoStack.length === 0) return;
      set((state: any) => {
        if (library) {
          state.redoStack.push(library);
        }
        state.library = state.undoStack.pop();
      });
    },

    redo: () => {
      const { redoStack, library } = get();
      if (redoStack.length === 0) return;
      set((state: any) => {
        if (library) {
          state.undoStack.push(library);
        }
        state.library = state.redoStack.pop();
      });
    },

    canUndo: () => get().undoStack.length > 0,
    canRedo: () => get().redoStack.length > 0,
  };
}
