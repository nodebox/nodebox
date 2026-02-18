import { contextBridge, ipcRenderer } from 'electron';
import { IPC } from '../shared/ipc-channels';

const electronAPI = {
  // File operations
  newFile: () => ipcRenderer.invoke(IPC.FILE_NEW),
  openFile: () => ipcRenderer.invoke(IPC.FILE_OPEN),
  saveFile: (data: string) => ipcRenderer.invoke(IPC.FILE_SAVE, data),
  saveFileAs: (data: string) => ipcRenderer.invoke(IPC.FILE_SAVE_AS, data),
  exportSvg: (data: string) => ipcRenderer.invoke(IPC.EXPORT_SVG, data),
  exportPng: (data: Uint8Array) => ipcRenderer.invoke(IPC.EXPORT_PNG, data),
  openAssetFile: (filters: { name: string; extensions: string[] }[], projectDir: string) =>
    ipcRenderer.invoke(IPC.ASSET_OPEN, { filters, projectDir }),

  // Fonts
  getFontList: () => ipcRenderer.invoke(IPC.FONT_LIST),
  getFontBytes: (name: string) => ipcRenderer.invoke(IPC.FONT_BYTES, name),

  // Menu actions
  onMenuAction: (callback: (action: string) => void) => {
    ipcRenderer.on(IPC.MENU_ACTION, (_event, action) => callback(action));
  },
};

contextBridge.exposeInMainWorld('electronAPI', electronAPI);

export type ElectronAPI = typeof electronAPI;
