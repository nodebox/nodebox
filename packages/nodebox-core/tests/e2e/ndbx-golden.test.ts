/**
 * Golden tests: parse real .ndbx example files, evaluate them, verify no crashes.
 * Tests the full pipeline: NDBX parse → prototype resolution → evaluation.
 * This is the "does it run?" complement to the golden-master SVG comparison tests.
 */
import { describe, it, expect } from 'vitest';
import { readFileSync, readdirSync, existsSync, statSync } from 'fs';
import { join, relative } from 'path';
import { parseNdbx, clearLibraryCache } from '../../src/ndbx/parser';
import { evaluate } from '../../src/eval/evaluate';
import { TestPlatform } from '../../src/platform';
import type { NodeLibrary } from '../../src/node/library';

const ROOT = join(__dirname, '..', '..', '..', '..');
const EXAMPLES_DIR = join(ROOT, 'examples');

/** Skip examples that need external resources. */
const SKIP_PATTERNS = ['device/', 'Web/', 'Geocoding/', 'Twitter'];

function shouldSkip(relPath: string): boolean {
  return SKIP_PATTERNS.some((p) => relPath.includes(p));
}

function loadLibraries() {
  const libs: Record<string, NodeLibrary> = {};
  for (const name of ['core', 'corevector', 'list', 'math', 'color', 'string', 'data', 'network']) {
    try {
      libs[name] = parseNdbx(readFileSync(join(ROOT, `libraries/${name}/${name}.ndbx`), 'utf-8'));
    } catch { /* some libs may not exist */ }
  }
  return (name: string) => libs[name];
}

/** Recursively find all .ndbx files. */
function findNdbxFiles(dir: string): string[] {
  const results: string[] = [];
  if (!existsSync(dir)) return results;
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    const stat = statSync(full);
    if (stat.isDirectory()) {
      results.push(...findNdbxFiles(full));
    } else if (entry.endsWith('.ndbx')) {
      results.push(full);
    }
  }
  return results;
}

describe('Parse and evaluate all .ndbx example files', () => {
  const loader = loadLibraries();
  const ndbxFiles = findNdbxFiles(EXAMPLES_DIR);

  for (const ndbxPath of ndbxFiles) {
    const relPath = relative(EXAMPLES_DIR, ndbxPath);

    if (shouldSkip(relPath)) {
      it.skip(`${relPath} (external dependency)`, () => {});
      continue;
    }

    it(`parses and evaluates: ${relPath}`, async () => {
      clearLibraryCache();
      const xml = readFileSync(ndbxPath, 'utf-8');
      const lib = parseNdbx(xml, loader);

      expect(lib.root.name).toBe('root');
      expect(lib.root.children.length).toBeGreaterThan(0);

      if (lib.root.renderedChild) {
        const result = await evaluate({
          library: lib,
          frame: 1,
          platform: new TestPlatform(),
        });

        expect(result).toBeDefined();
        expect(result.output).toBeDefined();
      }
    });
  }
});
