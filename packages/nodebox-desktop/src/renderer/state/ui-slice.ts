import type { PortType } from 'nodebox-core';

export type ViewerMode = 'visual' | 'data';

export interface PendingConnection {
  fromNode: string;
  outputType: string;
}

export interface Notification {
  id: number;
  message: string;
  level: 'info' | 'warning' | 'error';
}

export interface UISlice {
  currentNetworkPath: string;
  parameterPanelWidth: number;
  showHandles: boolean;
  showPoints: boolean;
  showPointNumbers: boolean;
  showOrigin: boolean;
  showCanvasBorder: boolean;
  nodeSelectionDialogOpen: boolean;
  nodeDialogPosition: { x: number; y: number } | null;
  pendingConnection: PendingConnection | null;
  viewerMode: ViewerMode;
  viewerZoom: number;
  viewerZoomAction: 'in' | 'out' | 'reset' | null;
  notifications: Notification[];

  setCurrentNetworkPath: (path: string) => void;
  setParameterPanelWidth: (width: number) => void;
  toggleHandles: () => void;
  togglePoints: () => void;
  togglePointNumbers: () => void;
  toggleOrigin: () => void;
  toggleCanvasBorder: () => void;
  setNodeSelectionDialogOpen: (open: boolean) => void;
  setNodeDialogPosition: (pos: { x: number; y: number } | null) => void;
  setPendingConnection: (pending: PendingConnection | null) => void;
  setViewerMode: (mode: ViewerMode) => void;
  setViewerZoom: (zoom: number) => void;
  requestViewerZoom: (action: 'in' | 'out' | 'reset') => void;
  clearViewerZoomAction: () => void;
  addNotification: (message: string, level?: 'info' | 'warning' | 'error') => void;
  dismissNotification: (id: number) => void;
}

export function createUISlice(set: any): UISlice {
  return {
    currentNetworkPath: 'root',
    parameterPanelWidth: 450,
    showHandles: true,
    showPoints: false,
    showPointNumbers: false,
    showOrigin: true,
    showCanvasBorder: true,
    nodeSelectionDialogOpen: false,
    nodeDialogPosition: null,
    pendingConnection: null,
    viewerMode: 'visual',
    viewerZoom: 1.0,
    viewerZoomAction: null,
    notifications: [],

    setCurrentNetworkPath: (path) => set((s: UISlice) => { s.currentNetworkPath = path; }),
    setParameterPanelWidth: (width) => set((s: UISlice) => { s.parameterPanelWidth = width; }),
    toggleHandles: () => set((s: UISlice) => { s.showHandles = !s.showHandles; }),
    togglePoints: () => set((s: UISlice) => { s.showPoints = !s.showPoints; }),
    togglePointNumbers: () => set((s: UISlice) => { s.showPointNumbers = !s.showPointNumbers; }),
    toggleOrigin: () => set((s: UISlice) => { s.showOrigin = !s.showOrigin; }),
    toggleCanvasBorder: () => set((s: UISlice) => { s.showCanvasBorder = !s.showCanvasBorder; }),
    setNodeSelectionDialogOpen: (open) => set((s: UISlice) => {
      s.nodeSelectionDialogOpen = open;
      if (!open) s.pendingConnection = null;
    }),
    setNodeDialogPosition: (pos) => set((s: UISlice) => { s.nodeDialogPosition = pos; }),
    setPendingConnection: (pending) => set((s: UISlice) => { s.pendingConnection = pending; }),
    setViewerMode: (mode) => set((s: UISlice) => { s.viewerMode = mode; }),
    setViewerZoom: (zoom) => set((s: UISlice) => { s.viewerZoom = zoom; }),
    requestViewerZoom: (action) => set((s: UISlice) => { s.viewerZoomAction = action; }),
    clearViewerZoomAction: () => set((s: UISlice) => { s.viewerZoomAction = null; }),
    addNotification: (message, level = 'warning') => set((s: UISlice) => {
      s.notifications.push({ id: Date.now(), message, level });
    }),
    dismissNotification: (id) => set((s: UISlice) => {
      const idx = s.notifications.findIndex((n) => n.id === id);
      if (idx >= 0) s.notifications.splice(idx, 1);
    }),
  };
}
