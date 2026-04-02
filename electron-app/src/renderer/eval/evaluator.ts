import type { NodeLibrary } from '../types/node';
import type { EvalResult } from '../types/eval-result';
import { isWasmReady, evaluateLibrary } from './wasm';

const EMPTY_RESULT: EvalResult = {
  paths: [],
  texts: [],
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

  const json = evaluateLibrary(JSON.stringify(library), filesJson, frame);
  return JSON.parse(json) as EvalResult;
}
