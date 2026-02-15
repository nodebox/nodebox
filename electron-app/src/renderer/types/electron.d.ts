// Type declarations for the contextBridge API exposed by preload.

interface FontInfo {
  family: string;
  postscriptName: string;
  path: string;
}

interface FileResult {
  path: string;
  content: string;
}

interface SaveResult {
  path: string;
}

interface ElectronAPI {
  newFile(): Promise<null>;
  openFile(): Promise<FileResult | null>;
  saveFile(data: string): Promise<SaveResult | null>;
  saveFileAs(data: string): Promise<SaveResult | null>;
  exportSvg(data: string): Promise<SaveResult | null>;
  exportPng(data: Uint8Array): Promise<SaveResult | null>;
  getFontList(): Promise<FontInfo[]>;
  getFontBytes(name: string): Promise<Uint8Array | null>;
  onMenuAction(callback: (action: string) => void): void;
}

declare global {
  interface Window {
    electronAPI: ElectronAPI;
  }
}

export {};
