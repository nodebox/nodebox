export type ViewerMode = 'visual' | 'data';

export interface UISlice {
  networkSplitRatio: number;
  parameterPanelWidth: number;
  showHandles: boolean;
  showPoints: boolean;
  showOrigin: boolean;
  showCanvasBorder: boolean;
  showPointNumbers: boolean;
  nodeDialogVisible: boolean;
  aboutDialogVisible: boolean;
  viewerMode: ViewerMode;

  setNetworkSplitRatio: (ratio: number) => void;
  setParameterPanelWidth: (width: number) => void;
  toggleHandles: () => void;
  togglePoints: () => void;
  toggleOrigin: () => void;
  toggleCanvasBorder: () => void;
  togglePointNumbers: () => void;
  setNodeDialogVisible: (visible: boolean) => void;
  setAboutDialogVisible: (visible: boolean) => void;
  setViewerMode: (mode: ViewerMode) => void;
  viewerZoom: number;
  setViewerZoom: (zoom: number) => void;
  viewerZoomAction: 'in' | 'out' | 'reset' | null;
  requestViewerZoom: (action: 'in' | 'out' | 'reset') => void;
  clearViewerZoomAction: () => void;
}

export const createUISlice = (
  set: (fn: (state: UISlice) => void) => void,
): UISlice => ({
  networkSplitRatio: 0.65,
  parameterPanelWidth: 450,
  showHandles: true,
  showPoints: false,
  showOrigin: true,
  showCanvasBorder: true,
  showPointNumbers: false,
  nodeDialogVisible: false,
  aboutDialogVisible: false,
  viewerMode: 'visual',

  setNetworkSplitRatio: (ratio) =>
    set((state) => {
      state.networkSplitRatio = ratio;
    }),

  setParameterPanelWidth: (width) =>
    set((state) => {
      state.parameterPanelWidth = width;
    }),

  toggleHandles: () =>
    set((state) => {
      state.showHandles = !state.showHandles;
    }),

  togglePoints: () =>
    set((state) => {
      state.showPoints = !state.showPoints;
    }),

  toggleOrigin: () =>
    set((state) => {
      state.showOrigin = !state.showOrigin;
    }),

  toggleCanvasBorder: () =>
    set((state) => {
      state.showCanvasBorder = !state.showCanvasBorder;
    }),

  togglePointNumbers: () =>
    set((state) => {
      state.showPointNumbers = !state.showPointNumbers;
    }),

  setNodeDialogVisible: (visible) =>
    set((state) => {
      state.nodeDialogVisible = visible;
    }),

  setAboutDialogVisible: (visible) =>
    set((state) => {
      state.aboutDialogVisible = visible;
    }),

  setViewerMode: (mode) =>
    set((state) => {
      state.viewerMode = mode;
    }),

  viewerZoom: 1.0,
  setViewerZoom: (zoom) =>
    set((state) => {
      state.viewerZoom = zoom;
    }),

  viewerZoomAction: null,
  requestViewerZoom: (action) =>
    set((state) => {
      state.viewerZoomAction = action;
    }),
  clearViewerZoomAction: () =>
    set((state) => {
      state.viewerZoomAction = null;
    }),
});
