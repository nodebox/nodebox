export interface UISlice {
  currentNetworkPath: string;
  showPoints: boolean;
  showOrigin: boolean;
  showBounds: boolean;
  showHandles: boolean;
  nodeSelectionDialogOpen: boolean;
  setCurrentNetworkPath: (path: string) => void;
  toggleShowPoints: () => void;
  toggleShowOrigin: () => void;
  toggleShowBounds: () => void;
  toggleShowHandles: () => void;
  setNodeSelectionDialogOpen: (open: boolean) => void;
}

export function createUISlice(set: any): UISlice {
  return {
    currentNetworkPath: 'root',
    showPoints: false,
    showOrigin: true,
    showBounds: false,
    showHandles: true,
    nodeSelectionDialogOpen: false,

    setCurrentNetworkPath: (path) => set((state: UISlice) => {
      state.currentNetworkPath = path;
    }),
    toggleShowPoints: () => set((state: UISlice) => { state.showPoints = !state.showPoints; }),
    toggleShowOrigin: () => set((state: UISlice) => { state.showOrigin = !state.showOrigin; }),
    toggleShowBounds: () => set((state: UISlice) => { state.showBounds = !state.showBounds; }),
    toggleShowHandles: () => set((state: UISlice) => { state.showHandles = !state.showHandles; }),
    setNodeSelectionDialogOpen: (open) => set((state: UISlice) => {
      state.nodeSelectionDialogOpen = open;
    }),
  };
}
