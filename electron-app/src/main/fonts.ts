import { execFile } from 'child_process';
import { readFile } from 'fs/promises';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);

export interface FontInfo {
  family: string;
  postscriptName: string;
  path: string;
}

let cachedFonts: FontInfo[] | null = null;

export async function listFonts(): Promise<FontInfo[]> {
  if (cachedFonts) return cachedFonts;

  try {
    const { stdout } = await execFileAsync('system_profiler', [
      'SPFontsDataType',
      '-json',
    ]);
    const data = JSON.parse(stdout);
    const fonts: FontInfo[] = [];
    const items = data?.SPFontsDataType ?? [];

    for (const item of items) {
      const family = item._name ?? '';
      const path = item.path ?? '';
      const typefaces = item.typefaces ?? {};
      for (const [, face] of Object.entries(typefaces)) {
        const f = face as Record<string, string>;
        const postscriptName = f.postscriptname ?? '';
        if (postscriptName) {
          fonts.push({ family, postscriptName, path });
        }
      }
    }

    cachedFonts = fonts;
    return fonts;
  } catch {
    return [];
  }
}

export async function getFontBytes(
  postscriptName: string,
): Promise<Uint8Array | null> {
  const fonts = await listFonts();
  const font = fonts.find((f) => f.postscriptName === postscriptName);
  if (!font?.path) return null;

  try {
    const buffer = await readFile(font.path);
    return new Uint8Array(buffer);
  } catch {
    return null;
  }
}
