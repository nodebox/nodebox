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
  viewerMode: ViewerMode;         // what's currently displayed
  userViewerMode: ViewerMode;     // what the user manually chose
  autoDataMode: boolean;          // true when auto-switched to data for non-visual output
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
  setViewerMode: (mode: ViewerMode) => void;       // user-initiated switch
  setAutoDataMode: (hasGeometry: boolean) => void;  // called after each evaluation
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
    userViewerMode: 'visual',
    autoDataMode: false,
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
    // User manually picks a mode — this is the "sticky" preference
    setViewerMode: (mode) => set((s: UISlice) => {
      s.viewerMode = mode;
      s.userViewerMode = mode;
      s.autoDataMode = false;
    }),
    // Called after each evaluation with whether output has geometry.
    // Auto-switches to data when no geometry, restores user mode when geometry returns.
    setAutoDataMode: (hasGeometry) => set((s: UISlice) => {
      if (!hasGeometry && s.userViewerMode === 'visual') {
        // No geometry + user was in visual → auto-switch to data
        s.viewerMode = 'data';
        s.autoDataMode = true;
      } else if (hasGeometry && s.autoDataMode) {
        // Geometry returned + we auto-switched → restore visual
        s.viewerMode = 'visual';
        s.autoDataMode = false;
      }
      // If user manually chose 'data', do nothing — respect their choice
    }),
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
