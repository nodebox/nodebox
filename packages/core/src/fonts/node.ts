// The fonts bundled with @ndbx/core, for Node hosts (tests, scripts, the Electron main process).
// Browsers fetch the same files from wherever the host serves them and call installOpenTypeFonts.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { installOpenTypeFonts, OpenTypeFontProvider } from "./opentype-provider";

/** The directory that holds the bundled TrueType files (DejaVu Sans, Bitstream Vera license). */
export function bundledFontDirectory(): string {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..", "fonts");
}

export function bundledFontFiles(): string[] {
  const dir = bundledFontDirectory();
  // The regular face comes first: the first registered font is the fallback for unknown names.
  return fs
    .readdirSync(dir)
    .filter((name) => /\.(ttf|otf)$/i.test(name))
    .sort((a, b) => Number(a.includes("-")) - Number(b.includes("-")) || a.localeCompare(b))
    .map((name) => path.join(dir, name));
}

/**
 * Install the bundled fonts as the font provider. The first file (DejaVu Sans) is the fallback for
 * font names the provider does not know, so NodeBox 3 documents that ask for Verdana still render.
 */
export function installBundledFonts(extraFiles: string[] = []): OpenTypeFontProvider {
  const files = [...bundledFontFiles(), ...extraFiles];
  return installOpenTypeFonts(files.map((file) => ({ buffer: toArrayBuffer(fs.readFileSync(file)) })));
}

function toArrayBuffer(buffer: Buffer): ArrayBuffer {
  return buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength) as ArrayBuffer;
}
