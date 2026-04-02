export interface SelectionSlice {
  selectedNodes: Set<string>;
  activeNode: string | null;

  selectNode: (name: string) => void;
  selectNodes: (names: string[]) => void;
  toggleNode: (name: string) => void;
  clearSelection: () => void;
  setActiveNode: (name: string | null) => void;
}

export const createSelectionSlice = (
  set: (fn: (state: SelectionSlice) => void) => void,
): SelectionSlice => ({
  selectedNodes: new Set<string>(),
  activeNode: null,

  selectNode: (name) =>
    set((state) => {
      state.selectedNodes = new Set([name]);
      state.activeNode = name;
    }),

  selectNodes: (names) =>
    set((state) => {
      state.selectedNodes = new Set(names);
      state.activeNode = names.length > 0 ? names[names.length - 1] : null;
    }),

  toggleNode: (name) =>
    set((state) => {
      const next = new Set(state.selectedNodes);
      if (next.has(name)) {
        next.delete(name);
      } else {
        next.add(name);
      }
      state.selectedNodes = next;
      state.activeNode = next.size > 0 ? [...next].pop()! : null;
    }),

  clearSelection: () =>
    set((state) => {
      state.selectedNodes = new Set();
      state.activeNode = null;
    }),

  setActiveNode: (name) =>
    set((state) => {
      state.activeNode = name;
    }),
});
