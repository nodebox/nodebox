export interface SelectionSlice {
  selectedNodes: string[];
  selectNode: (name: string, additive?: boolean) => void;
  selectNodes: (names: string[]) => void;
  deselectAll: () => void;
}

export function createSelectionSlice(set: any): SelectionSlice {
  return {
    selectedNodes: [],
    selectNode: (name, additive = false) => set((state: SelectionSlice) => {
      if (additive) {
        if (state.selectedNodes.includes(name)) {
          state.selectedNodes = state.selectedNodes.filter(n => n !== name);
        } else {
          state.selectedNodes.push(name);
        }
      } else {
        state.selectedNodes = [name];
      }
    }),
    selectNodes: (names) => set((state: SelectionSlice) => {
      state.selectedNodes = names;
    }),
    deselectAll: () => set((state: SelectionSlice) => {
      state.selectedNodes = [];
    }),
  };
}
