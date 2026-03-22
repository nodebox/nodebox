import { describe, it, expect } from 'vitest';
import { validateSandboxPath, PlatformError, TestPlatform } from '../../src/platform.js';

describe('Sandbox validation', () => {
  it('allows relative paths', () => {
    expect(() => validateSandboxPath('data/file.csv')).not.toThrow();
    expect(() => validateSandboxPath('file.txt')).not.toThrow();
    expect(() => validateSandboxPath('nested/deep/file.txt')).not.toThrow();
  });

  it('rejects absolute paths', () => {
    expect(() => validateSandboxPath('/etc/passwd')).toThrow(PlatformError);
    expect(() => validateSandboxPath('/tmp/data.csv')).toThrow(PlatformError);
  });

  it('rejects parent directory traversal', () => {
    expect(() => validateSandboxPath('../secret.txt')).toThrow(PlatformError);
    expect(() => validateSandboxPath('data/../../../etc/passwd')).toThrow(PlatformError);
    expect(() => validateSandboxPath('a/b/../../c')).toThrow(PlatformError);
  });

  it('rejects Windows drive letters', () => {
    expect(() => validateSandboxPath('C:\\Windows\\system.ini')).toThrow(PlatformError);
    expect(() => validateSandboxPath('D:file.txt')).toThrow(PlatformError);
  });

  it('PlatformError has correct code', () => {
    try {
      validateSandboxPath('/absolute');
      expect.fail('Should throw');
    } catch (e) {
      expect(e).toBeInstanceOf(PlatformError);
      expect((e as PlatformError).code).toBe('sandbox_violation');
    }
  });
});

describe('TestPlatform', () => {
  it('reads added files', async () => {
    const platform = new TestPlatform();
    platform.addFile('data.csv', 'a,b,c');
    const ctx = { root: '/test', projectFile: null, frame: 1 };
    const content = await platform.readTextFile(ctx, 'data.csv');
    expect(content).toBe('a,b,c');
  });

  it('throws on missing file', async () => {
    const platform = new TestPlatform();
    const ctx = { root: '/test', projectFile: null, frame: 1 };
    await expect(platform.readTextFile(ctx, 'missing.csv')).rejects.toThrow(PlatformError);
  });

  it('enforces sandbox on file reads', async () => {
    const platform = new TestPlatform();
    const ctx = { root: '/test', projectFile: null, frame: 1 };
    await expect(platform.readTextFile(ctx, '../secret')).rejects.toThrow(PlatformError);
    await expect(platform.readTextFile(ctx, '/etc/passwd')).rejects.toThrow(PlatformError);
  });

  it('loads libraries', async () => {
    const platform = new TestPlatform();
    platform.addLibrary('math', '<ndbx/>');
    const content = await platform.loadLibrary('math');
    expect(content).toBe('<ndbx/>');
  });

  it('logs messages', () => {
    const platform = new TestPlatform();
    platform.log('info', 'hello');
    platform.log('error', 'bad');
    expect(platform.logs).toEqual([
      { level: 'info', message: 'hello' },
      { level: 'error', message: 'bad' },
    ]);
  });

  it('lists fonts', async () => {
    const platform = new TestPlatform();
    platform.addFont('Inter', new Uint8Array([1, 2, 3]));
    const fonts = await platform.listFonts();
    expect(fonts).toEqual(['Inter']);
  });
});
