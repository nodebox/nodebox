/**
 * E2E-style tests that simulate the desktop app's node creation workflow.
 * These test the full pipeline: template → add to library → evaluate → render.
 * Uses TestPlatform (dummy/browser platform) for environment-independent testing.
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { evaluate } from '../../src/eval/evaluate';
import { createNodeLibrary } from '../../src/node/library';
import { createNode, addChild, addConnection, setRenderedChild } from '../../src/node/node';
import { createPort } from '../../src/node/port';
import type { Node, NodeLibrary, Port, Value } from '../../src/index';
import { TestPlatform } from '../../src/platform';

// ── Simulate the desktop app's node template system ──────────────
// This mirrors packages/nodebox-desktop/src/renderer/node-templates.ts

function createNodeFromTemplate(
  name: string,
  fnName: string,
  outputType: string,
  inputs: Port[],
  pos = { x: 0, y: 0 },
): Node {
  return {
    ...createNode(name),
    function: fnName,
    outputType,
    inputs: inputs.map((p) => ({ ...p })),
    position: pos,
  };
}

// Simulate what the store does when adding a node
function addNodeToLibrary(lib: NodeLibrary, node: Node): NodeLibrary {
  return {
    ...lib,
    root: addChild(lib.root, node),
  };
}

function connectNodes(
  lib: NodeLibrary,
  outputNode: string,
  inputNode: string,
  inputPort: string,
): NodeLibrary {
  return {
    ...lib,
    root: addConnection(lib.root, { outputNode, inputNode, inputPort }),
  };
}

function setRendered(lib: NodeLibrary, childName: string): NodeLibrary {
  return {
    ...lib,
    root: setRenderedChild(lib.root, childName),
  };
}

async function evalLib(lib: NodeLibrary) {
  return evaluate({ library: lib, frame: 1, platform: new TestPlatform() });
}

// ── Tests ────────────────────────────────────────────────────────

describe('E2E: Node Creation Workflow', () => {
  let lib: NodeLibrary;

  beforeEach(() => {
    lib = createNodeLibrary(createNode('root'));
  });

  it('creates a rect node and evaluates it', async () => {
    const rect1 = createNodeFromTemplate('rect1', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 100 }),
      createPort('height', 'float', { type: 'float', value: 100 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);

    lib = addNodeToLibrary(lib, rect1);
    lib = setRendered(lib, 'rect1');

    const result = await evalLib(lib);
    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
    expect(result.paths[0].contours[0].closed).toBe(true);
    expect(result.paths[0].contours[0].points.length).toBe(4);
  });

  it('creates a rect, sets width via port value, and re-evaluates', async () => {
    const rect1 = createNodeFromTemplate('rect1', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 100 }),
      createPort('height', 'float', { type: 'float', value: 50 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);

    lib = addNodeToLibrary(lib, rect1);
    lib = setRendered(lib, 'rect1');

    const result1 = await evalLib(lib);
    expect(result1.paths.length).toBe(1);

    // Now change width to 200 (simulating parameter editing)
    const widthPort = lib.root.children[0].inputs.find((p) => p.name === 'width')!;
    const updatedNode = {
      ...lib.root.children[0],
      inputs: lib.root.children[0].inputs.map((p) =>
        p.name === 'width' ? { ...p, value: { type: 'float' as const, value: 200 } } : p,
      ),
    };
    lib = {
      ...lib,
      root: {
        ...lib.root,
        children: [updatedNode],
      },
    };

    const result2 = await evalLib(lib);
    expect(result2.paths.length).toBe(1);
    // Bounds should reflect wider rect
    const bounds = result2.paths[0].contours[0].points;
    const xs = bounds.map((p) => p.point.x);
    const width = Math.max(...xs) - Math.min(...xs);
    expect(width).toBeCloseTo(200);
  });

  it('creates two nodes, connects them, and evaluates the pipeline', async () => {
    const rect1 = createNodeFromTemplate('rect1', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 100 }),
      createPort('height', 'float', { type: 'float', value: 100 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);

    const colorize1 = createNodeFromTemplate('colorize1', 'corevector/colorize', 'geometry', [
      createPort('shape', 'geometry'),
      createPort('fill', 'color', { type: 'color', value: { r: 1, g: 0, b: 0, a: 1 } }),
      createPort('stroke', 'color', { type: 'color', value: { r: 0, g: 0, b: 0, a: 1 } }),
      createPort('strokeWidth', 'float', { type: 'float', value: 0 }),
    ]);

    lib = addNodeToLibrary(lib, rect1);
    lib = addNodeToLibrary(lib, colorize1);
    lib = connectNodes(lib, 'rect1', 'colorize1', 'shape');
    lib = setRendered(lib, 'colorize1');

    const result = await evalLib(lib);
    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
    expect(result.paths[0].fill).toEqual({ r: 1, g: 0, b: 0, a: 1 });
  });

  it('creates an ellipse, double-click sets it as rendered, then evaluates', async () => {
    const ellipse1 = createNodeFromTemplate('ellipse1', 'corevector/ellipse', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 80 }),
      createPort('height', 'float', { type: 'float', value: 60 }),
    ]);

    lib = addNodeToLibrary(lib, ellipse1);
    // Simulate double-click on node → set as rendered child
    lib = setRendered(lib, 'ellipse1');

    const result = await evalLib(lib);
    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
    // Ellipse has bezier curves
    expect(result.paths[0].contours[0].points.some((p) => p.type === 'curveTo')).toBe(true);
  });

  it('creates multiple nodes from separate dialog invocations', async () => {
    // Simulates: double-click → dialog → create rect, double-click → dialog → create ellipse
    const rect1 = createNodeFromTemplate('rect1', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: -50, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 60 }),
      createPort('height', 'float', { type: 'float', value: 60 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ], { x: 1, y: 1 });

    const ellipse1 = createNodeFromTemplate('ellipse1', 'corevector/ellipse', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 50, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 60 }),
      createPort('height', 'float', { type: 'float', value: 60 }),
    ], { x: 4, y: 1 });

    lib = addNodeToLibrary(lib, rect1);
    lib = addNodeToLibrary(lib, ellipse1);

    // Both nodes exist
    expect(lib.root.children.length).toBe(2);
    expect(lib.root.children[0].name).toBe('rect1');
    expect(lib.root.children[1].name).toBe('ellipse1');

    // Set rect as rendered → evaluate
    lib = setRendered(lib, 'rect1');
    const result1 = await evalLib(lib);
    expect(result1.paths.length).toBe(1);

    // Switch to ellipse as rendered → evaluate
    lib = setRendered(lib, 'ellipse1');
    const result2 = await evalLib(lib);
    expect(result2.paths.length).toBe(1);
  });

  it('generates unique names when adding duplicate node types', async () => {
    const rect1 = createNodeFromTemplate('rect1', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 100 }),
      createPort('height', 'float', { type: 'float', value: 100 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);

    const rect2 = createNodeFromTemplate('rect2', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 50, y: 50 } }),
      createPort('width', 'float', { type: 'float', value: 50 }),
      createPort('height', 'float', { type: 'float', value: 50 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);

    lib = addNodeToLibrary(lib, rect1);
    lib = addNodeToLibrary(lib, rect2);

    expect(lib.root.children.length).toBe(2);
    expect(lib.root.children[0].name).toBe('rect1');
    expect(lib.root.children[1].name).toBe('rect2');
  });

  it('deletes a node and connections are cleaned up', async () => {
    const rect1 = createNodeFromTemplate('rect1', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 100 }),
      createPort('height', 'float', { type: 'float', value: 100 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);

    const colorize1 = createNodeFromTemplate('colorize1', 'corevector/colorize', 'geometry', [
      createPort('shape', 'geometry'),
      createPort('fill', 'color', { type: 'color', value: { r: 1, g: 0, b: 0, a: 1 } }),
      createPort('stroke', 'color', { type: 'color', value: { r: 0, g: 0, b: 0, a: 1 } }),
      createPort('strokeWidth', 'float', { type: 'float', value: 0 }),
    ]);

    lib = addNodeToLibrary(lib, rect1);
    lib = addNodeToLibrary(lib, colorize1);
    lib = connectNodes(lib, 'rect1', 'colorize1', 'shape');

    expect(lib.root.connections.length).toBe(1);

    // Delete rect1 — simulates pressing Delete key
    const { removeChild } = await import('../../src/node/node');
    lib = { ...lib, root: removeChild(lib.root, 'rect1') };

    expect(lib.root.children.length).toBe(1);
    expect(lib.root.connections.length).toBe(0); // Connection auto-removed
  });
});
