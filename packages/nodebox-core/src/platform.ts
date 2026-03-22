export interface PlatformInfo {
  osName: string;
  isWeb: boolean;
  isMobile: boolean;
  hasFilesystem: boolean;
  hasNativeDialogs: boolean;
}

export interface ProjectContext {
  root: string | null;
  projectFile: string | null;
  frame: number;
}

export interface DirectoryEntry {
  name: string;
  isDirectory: boolean;
}

export interface FileFilter {
  name: string;
  extensions: string[];
}

export type LogLevel = 'error' | 'warn' | 'info' | 'debug';

export class PlatformError extends Error {
  constructor(
    public code: 'unsupported' | 'not_found' | 'permission_denied' | 'sandbox_violation' | 'network' | 'io' | 'other',
    message: string,
  ) {
    super(message);
    this.name = 'PlatformError';
  }
}

export interface Platform {
  platformInfo(): PlatformInfo;

  // File I/O (sandboxed to project directory)
  readTextFile(ctx: ProjectContext, path: string): Promise<string>;
  readBinaryFile(ctx: ProjectContext, path: string): Promise<Uint8Array>;
  writeFile(ctx: ProjectContext, path: string, data: Uint8Array): Promise<void>;
  listDirectory(ctx: ProjectContext, path: string): Promise<DirectoryEntry[]>;

  // Libraries
  loadLibrary(name: string): Promise<string>;
  loadAppResource(name: string): Promise<Uint8Array>;

  // Fonts
  listFonts(): Promise<string[]>;
  getFontBytes(fontFamily: string): Promise<Uint8Array>;

  // Network
  httpGet(url: string): Promise<Uint8Array>;

  // Logging
  log(level: LogLevel, message: string): void;
}

// Sandbox validation — reusable across all platform implementations
export function validateSandboxPath(path: string): void {
  if (path.startsWith('/')) {
    throw new PlatformError('sandbox_violation', `Absolute paths not allowed: ${path}`);
  }
  if (path.includes('..')) {
    throw new PlatformError('sandbox_violation', `Parent directory traversal not allowed: ${path}`);
  }
  // Windows drive letters
  if (/^[a-zA-Z]:/.test(path)) {
    throw new PlatformError('sandbox_violation', `Windows drive letters not allowed: ${path}`);
  }
}

// TestPlatform — in-memory implementation for unit tests
export class TestPlatform implements Platform {
  private files: Map<string, string> = new Map();
  private binaryFiles: Map<string, Uint8Array> = new Map();
  private libraries: Map<string, string> = new Map();
  private resources: Map<string, Uint8Array> = new Map();
  private fonts: Map<string, Uint8Array> = new Map();
  public logs: Array<{ level: LogLevel; message: string }> = [];

  addFile(path: string, content: string): void {
    this.files.set(path, content);
  }

  addBinaryFile(path: string, content: Uint8Array): void {
    this.binaryFiles.set(path, content);
  }

  addLibrary(name: string, content: string): void {
    this.libraries.set(name, content);
  }

  addResource(name: string, content: Uint8Array): void {
    this.resources.set(name, content);
  }

  addFont(name: string, bytes: Uint8Array): void {
    this.fonts.set(name, bytes);
  }

  platformInfo(): PlatformInfo {
    return {
      osName: 'test',
      isWeb: false,
      isMobile: false,
      hasFilesystem: true,
      hasNativeDialogs: false,
    };
  }

  async readTextFile(ctx: ProjectContext, path: string): Promise<string> {
    validateSandboxPath(path);
    const content = this.files.get(path);
    if (content === undefined) throw new PlatformError('not_found', `File not found: ${path}`);
    return content;
  }

  async readBinaryFile(ctx: ProjectContext, path: string): Promise<Uint8Array> {
    validateSandboxPath(path);
    const content = this.binaryFiles.get(path);
    if (content === undefined) throw new PlatformError('not_found', `Binary file not found: ${path}`);
    return content;
  }

  async writeFile(ctx: ProjectContext, path: string, data: Uint8Array): Promise<void> {
    validateSandboxPath(path);
    this.binaryFiles.set(path, data);
  }

  async listDirectory(ctx: ProjectContext, path: string): Promise<DirectoryEntry[]> {
    validateSandboxPath(path);
    return [];
  }

  async loadLibrary(name: string): Promise<string> {
    const content = this.libraries.get(name);
    if (content === undefined) throw new PlatformError('not_found', `Library not found: ${name}`);
    return content;
  }

  async loadAppResource(name: string): Promise<Uint8Array> {
    const content = this.resources.get(name);
    if (content === undefined) throw new PlatformError('not_found', `Resource not found: ${name}`);
    return content;
  }

  async listFonts(): Promise<string[]> {
    return [...this.fonts.keys()];
  }

  async getFontBytes(fontFamily: string): Promise<Uint8Array> {
    const bytes = this.fonts.get(fontFamily);
    if (!bytes) throw new PlatformError('not_found', `Font not found: ${fontFamily}`);
    return bytes;
  }

  async httpGet(url: string): Promise<Uint8Array> {
    throw new PlatformError('unsupported', 'HTTP not supported in test platform');
  }

  log(level: LogLevel, message: string): void {
    this.logs.push({ level, message });
  }
}
