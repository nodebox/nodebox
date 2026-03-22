import type { Platform, PlatformInfo, ProjectContext, DirectoryEntry, LogLevel } from 'nodebox-core';
import { PlatformError, validateSandboxPath } from 'nodebox-core';

declare global {
  interface Window {
    nodebox: {
      file: {
        open: () => Promise<{ filePath: string; content: string } | null>;
        save: (filePath: string, content: string) => Promise<boolean>;
        saveAs: (content: string) => Promise<string | null>;
        read: (basePath: string, relativePath: string) => Promise<string>;
      };
      library: {
        load: (name: string) => Promise<string>;
      };
      onMenuAction: (callback: (action: string) => void) => void;
    };
  }
}

export class ElectronPlatform implements Platform {
  platformInfo(): PlatformInfo {
    return {
      osName: process.platform ?? 'unknown',
      isWeb: false,
      isMobile: false,
      hasFilesystem: true,
      hasNativeDialogs: true,
    };
  }

  async readTextFile(ctx: ProjectContext, path: string): Promise<string> {
    validateSandboxPath(path);
    if (!ctx.projectFile) throw new PlatformError('io', 'No project file set');
    return window.nodebox.file.read(ctx.projectFile, path);
  }

  async readBinaryFile(_ctx: ProjectContext, _path: string): Promise<Uint8Array> {
    throw new PlatformError('unsupported', 'Binary file reading not yet implemented');
  }

  async writeFile(_ctx: ProjectContext, _path: string, _data: Uint8Array): Promise<void> {
    throw new PlatformError('unsupported', 'File writing not yet implemented');
  }

  async listDirectory(_ctx: ProjectContext, _path: string): Promise<DirectoryEntry[]> {
    throw new PlatformError('unsupported', 'Directory listing not yet implemented');
  }

  async loadLibrary(name: string): Promise<string> {
    return window.nodebox.library.load(name);
  }

  async loadAppResource(_name: string): Promise<Uint8Array> {
    throw new PlatformError('unsupported', 'App resource loading not yet implemented');
  }

  async listFonts(): Promise<string[]> {
    return ['Inter']; // Bundled font + system fonts via future IPC
  }

  async getFontBytes(_fontFamily: string): Promise<Uint8Array> {
    throw new PlatformError('unsupported', 'Font loading not yet implemented');
  }

  async httpGet(url: string): Promise<Uint8Array> {
    const resp = await fetch(url);
    if (!resp.ok) throw new PlatformError('network', `HTTP ${resp.status}`);
    return new Uint8Array(await resp.arrayBuffer());
  }

  log(level: LogLevel, message: string): void {
    if (level === 'error') console.error(message);
    else if (level === 'warn') console.warn(message);
    else console.log(message);
  }
}
