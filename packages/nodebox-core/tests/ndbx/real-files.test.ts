import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { join } from 'path';
import { parseNdbx, clearLibraryCache } from '../../src/ndbx/parser.js';

const ROOT = join(__dirname, '..', '..', '..', '..');

function loadFile(path: string): string {
  return readFileSync(join(ROOT, path), 'utf-8');
}

describe('Real NDBX Files', () => {
  beforeEach(() => clearLibraryCache());

  it('parses core.ndbx', () => {
    const xml = loadFile('libraries/core/core.ndbx');
    const lib = parseNdbx(xml);
    expect(lib.root.name).toBe('root');
    // Should have ROOT, NETWORK, frame nodes
    expect(lib.root.children.length).toBeGreaterThanOrEqual(2);
    const frame = lib.root.children.find(c => c.name === 'frame');
    expect(frame?.function).toBe('core/frame');
  });

  it('parses corevector.ndbx (library)', () => {
    const xml = loadFile('libraries/corevector/corevector.ndbx');
    const lib = parseNdbx(xml);
    expect(lib.root.name).toBe('root');
    // Should have many child nodes (rect, ellipse, etc.)
    expect(lib.root.children.length).toBeGreaterThan(30);

    const rectNode = lib.root.children.find(c => c.name === 'rect');
    expect(rectNode).toBeDefined();
    expect(rectNode?.function).toBe('corevector/rect');

    const ellipseNode = lib.root.children.find(c => c.name === 'ellipse');
    expect(ellipseNode).toBeDefined();
  });

  it('parses math.ndbx', () => {
    const xml = loadFile('libraries/math/math.ndbx');
    const lib = parseNdbx(xml);
    expect(lib.root.children.length).toBeGreaterThan(20);
    const addNode = lib.root.children.find(c => c.name === 'add');
    expect(addNode).toBeDefined();
  });

  it('parses list.ndbx', () => {
    const xml = loadFile('libraries/list/list.ndbx');
    const lib = parseNdbx(xml);
    expect(lib.root.children.length).toBeGreaterThan(10);
  });

  it('parses string.ndbx', () => {
    const xml = loadFile('libraries/string/string.ndbx');
    const lib = parseNdbx(xml);
    expect(lib.root.children.length).toBeGreaterThan(10);
  });

  it('parses color.ndbx', () => {
    const xml = loadFile('libraries/color/color.ndbx');
    const lib = parseNdbx(xml);
    expect(lib.root.children.length).toBeGreaterThanOrEqual(3);
  });

  it('parses example file with prototype resolution', () => {
    // Load libraries
    const coreXml = loadFile('libraries/core/core.ndbx');
    const coreLib = parseNdbx(coreXml);
    const cvXml = loadFile('libraries/corevector/corevector.ndbx');
    const cvLib = parseNdbx(cvXml);
    const listXml = loadFile('libraries/list/list.ndbx');
    const listLib = parseNdbx(listXml);

    const loader = (name: string) => {
      if (name === 'core') return coreLib;
      if (name === 'corevector') return cvLib;
      if (name === 'list') return listLib;
      return undefined;
    };

    const exXml = loadFile('examples/01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx');
    const lib = parseNdbx(exXml, loader);

    expect(lib.root.renderedChild).toBe('combine1');
    expect(lib.root.children.length).toBe(7);

    // rect1 should have inherited ports from prototype
    const rect1 = lib.root.children.find(c => c.name === 'rect1');
    expect(rect1).toBeDefined();
    expect(rect1?.function).toBe('corevector/rect');
    // Should have width/height ports from prototype
    expect(rect1?.inputs.some(p => p.name === 'position')).toBe(true);
  });
});
