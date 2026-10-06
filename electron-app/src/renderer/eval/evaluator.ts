import type { NodeLibrary } from '../types/node';
import type { EvalResult } from '../types/eval-result';
import { isWasmReady, evaluateLibrary, registerFont, takeMissingFonts } from './wasm';

const EMPTY_RESULT: EvalResult = {
  paths: [],
  output: { type: 'none', isMultiple: false, values: [] },
  errors: [],
};

/**
 * Scan the library for ports with the File widget and collect file paths
 * that need to be read before evaluation.
 */
function collectFilePaths(library: NodeLibrary): string[] {
  const paths: string[] = [];
  for (const child of library.root.children) {
    for (const port of child.inputs) {
      if (port.widget === 'File' && typeof port.value === 'object' && 'String' in port.value) {
        const filePath = port.value.String;
        if (filePath && !paths.includes(filePath)) {
          paths.push(filePath);
        }
      }
    }
  }
  return paths;
}

/**
 * Read file contents via Electron IPC for the given relative paths.
 * Returns a map of relative path -> file content.
 */
async function readFiles(
  filePaths: string[],
  projectDir: string,
): Promise<Record<string, string>> {
  const files: Record<string, string> = {};
  if (!window.electronAPI) return files;
  const results = await Promise.all(
    filePaths.map(async (relativePath) => {
      const result = await window.electronAPI.readAssetFile(relativePath, projectDir);
      if ('content' in result) {
        return [relativePath, result.content] as const;
      }
      return null;
    }),
  );
  for (const entry of results) {
    if (entry) files[entry[0]] = entry[1];
  }
  return files;
}

/** A font as the Local Font Access API describes it. */
interface LocalFont {
  family: string;
  style: string;
  postscriptName: string;
  blob(): Promise<Blob>;
}

const requestedFonts = new Set<string>();
let localFonts: Promise<LocalFont[]> | null = null;

/**
 * Register the system fonts for `names` with the WASM module, which has no
 * system fonts of its own. Each name is looked up one time. Returns true if a
 * font was registered, so the caller evaluates again.
 */
async function loadMissingFonts(names: string[]): Promise<boolean> {
  const wanted = names.filter((name) => !requestedFonts.has(name));
  const query = (window as unknown as { queryLocalFonts?: () => Promise<LocalFont[]> }).queryLocalFonts;
  if (wanted.length === 0 || !query) return false;
  for (const name of wanted) requestedFonts.add(name);

  localFonts ??= query.call(window).catch(() => []);
  const fonts = await localFonts;
  let registered = false;
  for (const name of wanted) {
    const lower = name.toLowerCase();
    // .ndbx files store PostScript names such as "Verdana-Bold".
    const font =
      fonts.find((f) => f.postscriptName.toLowerCase() === lower) ??
      fonts.find((f) => f.family.toLowerCase() === lower && f.style === 'Regular') ??
      fonts.find((f) => f.family.toLowerCase() === lower);
    if (!font) continue;
    registerFont(new Uint8Array(await (await font.blob()).arrayBuffer()));
    registered = true;
  }
  return registered;
}

export async function evaluate(
  library: NodeLibrary,
  frame: number,
  projectDir: string | null,
): Promise<EvalResult> {
  if (!isWasmReady()) return EMPTY_RESULT;

  // Collect files needed by File widget ports and read them via IPC
  let filesJson = '{}';
  if (projectDir) {
    const filePaths = collectFilePaths(library);
    if (filePaths.length > 0) {
      const files = await readFiles(filePaths, projectDir);
      filesJson = JSON.stringify(files);
    }
  }

  const libraryJson = JSON.stringify(library);
  let json = evaluateLibrary(libraryJson, filesJson, frame);
  if (await loadMissingFonts(takeMissingFonts())) {
    json = evaluateLibrary(libraryJson, filesJson, frame);
  }
  return JSON.parse(json) as EvalResult;
}
