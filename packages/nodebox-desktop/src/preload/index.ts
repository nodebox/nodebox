import { contextBridge, ipcRenderer } from 'electron';
import { IPC } from '../shared/ipc-channels.js';

contextBridge.exposeInMainWorld('nodebox', {
  file: {
    open: () => ipcRenderer.invoke(IPC.FILE_OPEN),
    save: (filePath: string, content: string) => ipcRenderer.invoke(IPC.FILE_SAVE, { filePath, content }),
    saveAs: (content: string) => ipcRenderer.invoke(IPC.FILE_SAVE_AS, { content }),
    read: (basePath: string, relativePath: string) => ipcRenderer.invoke(IPC.FILE_READ, { basePath, relativePath }),
  },
  library: {
    load: (name: string) => ipcRenderer.invoke(IPC.LIBRARY_LOAD, name),
  },
  onMenuAction: (callback: (action: string) => void) => {
    const actions = ['new', 'open', 'save', 'save-as', 'export-svg', 'export-png',
      'undo', 'redo', 'delete', 'toggle-points', 'toggle-origin', 'toggle-bounds'];
    for (const action of actions) {
      ipcRenderer.on(`menu:${action}`, () => callback(action));
    }
  },
});
