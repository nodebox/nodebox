import type { PortType } from '../types/node';

export type ViewerMode = 'visual' | 'data';

export interface PendingConnection {
  fromNode: string;
  outputType: PortType;
}

export interface Notification {
  id: number;
  message: string;
  level: 'info' | 'warning' | 'error';
}

export interface UISlice {
  networkSplitRatio: number;
  parameterPanelWidth: number;
  showHandles: boolean;
  showPoints: boolean;
  showOrigin: boolean;
  showCanvasBorder: boolean;
  showPointNumbers: boolean;
  nodeDialogVisible: boolean;
  nodeDialogPosition: { x: number; y: number } | null;
  pendingConnection: PendingConnection | null;
  aboutDialogVisible: boolean;
  viewerMode: ViewerMode;
  notifications: Notification[];

  setNetworkSplitRatio: (ratio: number) => void;
  setParameterPanelWidth: (width: number) => void;
  toggleHandles: () => void;
  togglePoints: () => void;
  toggleOrigin: () => void;
  toggleCanvasBorder: () => void;
  togglePointNumbers: () => void;
  setNodeDialogVisible: (visible: boolean) => void;
  setNodeDialogPosition: (position: { x: number; y: number } | null) => void;
  setPendingConnection: (pending: PendingConnection | null) => void;
  setAboutDialogVisible: (visible: boolean) => void;
  setViewerMode: (mode: ViewerMode) => void;
  viewerZoom: number;
  setViewerZoom: (zoom: number) => void;
  viewerZoomAction: 'in' | 'out' | 'reset' | null;
  requestViewerZoom: (action: 'in' | 'out' | 'reset') => void;
  clearViewerZoomAction: () => void;
  addNotification: (message: string, level?: 'info' | 'warning' | 'error') => void;
  dismissNotification: (id: number) => void;
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
  nodeDialogPosition: null,
  pendingConnection: null,
  aboutDialogVisible: false,
  viewerMode: 'visual',
  notifications: [],

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
      if (!visible) {
        state.pendingConnection = null;
      }
    }),

  setNodeDialogPosition: (position) =>
    set((state) => {
      state.nodeDialogPosition = position;
    }),

  setPendingConnection: (pending) =>
    set((state) => {
      state.pendingConnection = pending;
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

  addNotification: (message, level = 'warning') =>
    set((state) => {
      state.notifications.push({ id: Date.now(), message, level });
    }),

  dismissNotification: (id) =>
    set((state) => {
      state.notifications = state.notifications.filter((n) => n.id !== id);
    }),
});
