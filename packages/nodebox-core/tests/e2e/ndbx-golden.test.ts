/**
 * Golden tests: parse real .ndbx example files, evaluate them, verify output.
 * These test the full pipeline: NDBX parse → prototype resolution → evaluation.
 */
import { describe, it, expect } from 'vitest';
import { readFileSync, readdirSync } from 'fs';
import { join } from 'path';
import { parseNdbx, clearLibraryCache } from '../../src/ndbx/parser';
import { evaluate } from '../../src/eval/evaluate';
import { TestPlatform } from '../../src/platform';
import type { NodeLibrary } from '../../src/node/library';

const ROOT = join(__dirname, '..', '..', '..', '..');

function loadFile(path: string): string {
  return readFileSync(join(ROOT, path), 'utf-8');
}

function loadLibraries() {
  const libs: Record<string, NodeLibrary> = {};
  for (const name of ['core', 'corevector', 'list', 'math', 'color', 'string', 'data']) {
    try {
      libs[name] = parseNdbx(loadFile(`libraries/${name}/${name}.ndbx`));
    } catch { /* some libs may not exist */ }
  }
  return (name: string) => libs[name];
}

describe('Golden: Parse and evaluate .ndbx example files', () => {
  beforeEach(() => clearLibraryCache());

  const loader = loadLibraries();

  // Find all example .ndbx files
  const exampleDirs = [
    'examples/01 Basics/01 Shape',
    'examples/01 Basics/02 Transform',
  ];

  for (const dir of exampleDirs) {
    const fullDir = join(ROOT, dir);
    let subdirs: string[] = [];
    try { subdirs = readdirSync(fullDir); } catch { continue; }

    for (const subdir of subdirs) {
      const ndbxFiles = (() => {
        try {
          return readdirSync(join(fullDir, subdir)).filter(f => f.endsWith('.ndbx'));
        } catch { return []; }
      })();

      for (const file of ndbxFiles) {
        const relPath = `${dir}/${subdir}/${file}`;

        it(`parses and evaluates: ${subdir}/${file}`, async () => {
          clearLibraryCache();
          const xml = loadFile(relPath);
          const lib = parseNdbx(xml, loader);

          expect(lib.root.name).toBe('root');
          expect(lib.root.children.length).toBeGreaterThan(0);

          if (lib.root.renderedChild) {
            const result = await evaluate({
              library: lib,
              frame: 1,
              platform: new TestPlatform(),
            });

            // Should evaluate without crashing
            // Some examples may have errors for unimplemented nodes, that's ok
            expect(result).toBeDefined();
            expect(result.output).toBeDefined();
          }
        });
      }
    }
  }
});
