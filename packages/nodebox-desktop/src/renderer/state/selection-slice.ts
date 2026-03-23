export interface SelectionSlice {
  selectedNodes: string[];
  activeNode: string | null;

  selectNode: (name: string) => void;
  selectNodes: (names: string[]) => void;
  toggleNode: (name: string) => void;
  deselectAll: () => void;
  setActiveNode: (name: string | null) => void;
}

export function createSelectionSlice(set: any): SelectionSlice {
  return {
    selectedNodes: [],
    activeNode: null,

    selectNode: (name) => set((s: SelectionSlice) => {
      s.selectedNodes = [name];
      s.activeNode = name;
    }),
    selectNodes: (names) => set((s: SelectionSlice) => {
      s.selectedNodes = names;
      s.activeNode = names.length > 0 ? names[names.length - 1] : null;
    }),
    toggleNode: (name) => set((s: SelectionSlice) => {
      const idx = s.selectedNodes.indexOf(name);
      if (idx >= 0) {
        s.selectedNodes.splice(idx, 1);
      } else {
        s.selectedNodes.push(name);
      }
      s.activeNode = s.selectedNodes.length > 0 ? s.selectedNodes[s.selectedNodes.length - 1] : null;
    }),
    deselectAll: () => set((s: SelectionSlice) => {
      s.selectedNodes = [];
      s.activeNode = null;
    }),
    setActiveNode: (name) => set((s: SelectionSlice) => { s.activeNode = name; }),
  };
}
