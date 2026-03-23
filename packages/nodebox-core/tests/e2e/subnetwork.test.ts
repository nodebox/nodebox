/**
 * E2E tests for subnetwork evaluation with published ports.
 * This is the key compositional mechanism in NodeBox.
 */
import { describe, it, expect } from 'vitest';
import { evaluate } from '../../src/eval/evaluate';
import { createNodeLibrary } from '../../src/node/library';
import { createNode, addChild, addConnection, setRenderedChild } from '../../src/node/node';
import { createPort } from '../../src/node/port';
import type { Node, Port } from '../../src/index';
import { TestPlatform } from '../../src/platform';

async function evalLib(root: Node) {
  return evaluate({ library: createNodeLibrary(root), frame: 1, platform: new TestPlatform() });
}

describe('E2E: Subnetwork Evaluation', () => {
  it('evaluates a simple subnetwork with rendered child', async () => {
    // Build: root → subnet (contains rect1 as rendered child)
    let rect1 = createNode('rect1');
    rect1 = {
      ...rect1,
      function: 'corevector/rect',
      inputs: [
        createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
        createPort('width', 'float', { type: 'float', value: 50 }),
        createPort('height', 'float', { type: 'float', value: 50 }),
        createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      ],
    };

    let subnet = createNode('subnet');
    subnet = addChild(subnet, rect1);
    subnet = setRenderedChild(subnet, 'rect1');

    let root = createNode('root');
    root = addChild(root, subnet);
    root = setRenderedChild(root, 'subnet');

    const result = await evalLib(root);
    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
  });

  it('evaluates nested subnetworks (2 levels deep)', async () => {
    let rect1 = createNode('rect1');
    rect1 = {
      ...rect1,
      function: 'corevector/rect',
      inputs: [
        createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
        createPort('width', 'float', { type: 'float', value: 30 }),
        createPort('height', 'float', { type: 'float', value: 30 }),
        createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      ],
    };

    let inner = createNode('inner');
    inner = addChild(inner, rect1);
    inner = setRenderedChild(inner, 'rect1');

    let outer = createNode('outer');
    outer = addChild(outer, inner);
    outer = setRenderedChild(outer, 'inner');

    let root = createNode('root');
    root = addChild(root, outer);
    root = setRenderedChild(root, 'outer');

    const result = await evalLib(root);
    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
  });

  it('subnetwork with connected children', async () => {
    // subnet contains: rect1 → colorize1 (rendered)
    let rect1 = createNode('rect1');
    rect1 = {
      ...rect1,
      function: 'corevector/rect',
      inputs: [
        createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
        createPort('width', 'float', { type: 'float', value: 80 }),
        createPort('height', 'float', { type: 'float', value: 80 }),
        createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      ],
    };

    let colorize1 = createNode('colorize1');
    colorize1 = {
      ...colorize1,
      function: 'corevector/colorize',
      inputs: [
        createPort('shape', 'geometry'),
        createPort('fill', 'color', { type: 'color', value: { r: 0, g: 1, b: 0, a: 1 } }),
        createPort('stroke', 'color', { type: 'color', value: { r: 0, g: 0, b: 0, a: 1 } }),
        createPort('strokeWidth', 'float', { type: 'float', value: 0 }),
      ],
    };

    let subnet = createNode('subnet');
    subnet = addChild(subnet, rect1);
    subnet = addChild(subnet, colorize1);
    subnet = addConnection(subnet, { outputNode: 'rect1', inputNode: 'colorize1', inputPort: 'shape' });
    subnet = setRenderedChild(subnet, 'colorize1');

    let root = createNode('root');
    root = addChild(root, subnet);
    root = setRenderedChild(root, 'subnet');

    const result = await evalLib(root);
    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
    expect(result.paths[0].fill).toEqual({ r: 0, g: 1, b: 0, a: 1 });
  });
});
