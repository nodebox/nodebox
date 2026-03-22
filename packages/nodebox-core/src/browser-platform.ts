import type { Platform, PlatformInfo, ProjectContext, DirectoryEntry, LogLevel } from './platform.js';
import { PlatformError, validateSandboxPath } from './platform.js';

// Bundled library content — will be populated at build time or registered manually
const bundledLibraries = new Map<string, string>();
const bundledResources = new Map<string, Uint8Array>();
const bundledFonts = new Map<string, Uint8Array>();

export function registerBundledLibrary(name: string, content: string): void {
  bundledLibraries.set(name, content);
}

export function registerBundledResource(name: string, data: Uint8Array): void {
  bundledResources.set(name, data);
}

export function registerBundledFont(name: string, data: Uint8Array): void {
  bundledFonts.set(name, data);
}

export class BrowserPlatform implements Platform {
  platformInfo(): PlatformInfo {
    return {
      osName: 'browser',
      isWeb: true,
      isMobile: /Mobi|Android/i.test(typeof navigator !== 'undefined' ? navigator.userAgent : ''),
      hasFilesystem: false,
      hasNativeDialogs: false,
    };
  }

  async readTextFile(_ctx: ProjectContext, _path: string): Promise<string> {
    throw new PlatformError('unsupported', 'File system not available in browser');
  }

  async readBinaryFile(_ctx: ProjectContext, _path: string): Promise<Uint8Array> {
    throw new PlatformError('unsupported', 'File system not available in browser');
  }

  async writeFile(_ctx: ProjectContext, _path: string, _data: Uint8Array): Promise<void> {
    throw new PlatformError('unsupported', 'File system not available in browser');
  }

  async listDirectory(_ctx: ProjectContext, _path: string): Promise<DirectoryEntry[]> {
    throw new PlatformError('unsupported', 'File system not available in browser');
  }

  async loadLibrary(name: string): Promise<string> {
    const content = bundledLibraries.get(name);
    if (content) return content;
    throw new PlatformError('not_found', `Bundled library not found: ${name}`);
  }

  async loadAppResource(name: string): Promise<Uint8Array> {
    const data = bundledResources.get(name);
    if (data) return data;
    throw new PlatformError('not_found', `Bundled resource not found: ${name}`);
  }

  async listFonts(): Promise<string[]> {
    return [...bundledFonts.keys()];
  }

  async getFontBytes(fontFamily: string): Promise<Uint8Array> {
    const data = bundledFonts.get(fontFamily);
    if (data) return data;
    throw new PlatformError('not_found', `Font not available: ${fontFamily}`);
  }

  async httpGet(url: string): Promise<Uint8Array> {
    const resp = await fetch(url);
    if (!resp.ok) throw new PlatformError('network', `HTTP ${resp.status}: ${url}`);
    const buffer = await resp.arrayBuffer();
    return new Uint8Array(buffer);
  }

  log(level: LogLevel, message: string): void {
    if (level === 'error') console.error(message);
    else if (level === 'warn') console.warn(message);
    else if (level === 'debug') console.debug(message);
    else console.log(message);
  }
}
