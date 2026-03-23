import { describe, it, expect } from 'vitest';
import { evaluate } from '../../src/eval/evaluate';
import { createNodeLibrary } from '../../src/node/library';
import { createNode, addChild, setRenderedChild, addConnection } from '../../src/node/node';
import { createPort } from '../../src/node/port';
import { TestPlatform } from '../../src/platform';
import type { Port, Value } from '../../src/index';

// Simulate what the desktop app does when creating nodes from templates
function createNodeFromTemplate(
  name: string,
  functionName: string,
  outputType: string,
  inputs: Port[],
  position = { x: 0, y: 0 },
) {
  return {
    ...createNode(name),
    function: functionName,
    outputType,
    inputs,
    position,
  };
}

describe('Node Template Integration', () => {
  it('creates and evaluates a rect from template', async () => {
    const rect1 = createNodeFromTemplate('rect1', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 100 }),
      createPort('height', 'float', { type: 'float', value: 100 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);

    let root = createNode('root');
    root = addChild(root, rect1);
    root = setRenderedChild(root, 'rect1');

    const result = await evaluate({
      library: createNodeLibrary(root),
      frame: 1,
      platform: new TestPlatform(),
    });

    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
    expect(result.paths[0].contours.length).toBe(1);
    expect(result.paths[0].contours[0].closed).toBe(true);
  });

  it('creates and evaluates an ellipse from template', async () => {
    const ellipse1 = createNodeFromTemplate('ellipse1', 'corevector/ellipse', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 80 }),
      createPort('height', 'float', { type: 'float', value: 60 }),
    ]);

    let root = createNode('root');
    root = addChild(root, ellipse1);
    root = setRenderedChild(root, 'ellipse1');

    const result = await evaluate({
      library: createNodeLibrary(root),
      frame: 1,
      platform: new TestPlatform(),
    });

    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
  });

  it('creates a full pipeline: rect → colorize', async () => {
    const rect1 = createNodeFromTemplate('rect1', 'corevector/rect', 'geometry', [
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
      createPort('width', 'float', { type: 'float', value: 200 }),
      createPort('height', 'float', { type: 'float', value: 100 }),
      createPort('roundness', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);

    const colorize1 = createNodeFromTemplate('colorize1', 'corevector/colorize', 'geometry', [
      createPort('shape', 'geometry'),
      createPort('fill', 'color', { type: 'color', value: { r: 0, g: 0.5, b: 1, a: 1 } }),
      createPort('stroke', 'color', { type: 'color', value: { r: 0, g: 0, b: 0, a: 1 } }),
      createPort('strokeWidth', 'float', { type: 'float', value: 2 }),
    ]);

    let root = createNode('root');
    root = addChild(root, rect1);
    root = addChild(root, colorize1);
    root = addConnection(root, { outputNode: 'rect1', inputNode: 'colorize1', inputPort: 'shape' });
    root = setRenderedChild(root, 'colorize1');

    const result = await evaluate({
      library: createNodeLibrary(root),
      frame: 1,
      platform: new TestPlatform(),
    });

    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBe(1);
    expect(result.paths[0].fill).toEqual({ r: 0, g: 0.5, b: 1, a: 1 });
    expect(result.paths[0].stroke).toEqual({ r: 0, g: 0, b: 0, a: 1 });
    expect(result.paths[0].strokeWidth).toBe(2);
  });

  it('grid produces list of points', async () => {
    const grid1 = createNodeFromTemplate('grid1', 'corevector/grid', 'point', [
      createPort('columns', 'int', { type: 'int', value: 3 }),
      createPort('rows', 'int', { type: 'int', value: 2 }),
      createPort('width', 'float', { type: 'float', value: 100 }),
      createPort('height', 'float', { type: 'float', value: 100 }),
      createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
    ]);
    grid1.outputRange = 'list';

    let root = createNode('root');
    root = addChild(root, grid1);
    root = setRenderedChild(root, 'grid1');

    const result = await evaluate({
      library: createNodeLibrary(root),
      frame: 1,
      platform: new TestPlatform(),
    });

    expect(result.errors).toEqual([]);
    expect(result.output.length).toBe(6); // 3 cols × 2 rows
    expect(result.output[0].type).toBe('point');
  });

  it('polygon with different sides', async () => {
    for (const sides of [3, 5, 8]) {
      const poly = createNodeFromTemplate('poly1', 'corevector/polygon', 'geometry', [
        createPort('position', 'point', { type: 'point', value: { x: 0, y: 0 } }),
        createPort('radius', 'float', { type: 'float', value: 50 }),
        createPort('sides', 'int', { type: 'int', value: sides }),
        createPort('align', 'boolean', { type: 'boolean', value: true }),
      ]);

      let root = createNode('root');
      root = addChild(root, poly);
      root = setRenderedChild(root, 'poly1');

      const result = await evaluate({
        library: createNodeLibrary(root),
        frame: 1,
        platform: new TestPlatform(),
      });

      expect(result.errors).toEqual([]);
      expect(result.paths.length).toBe(1);
      expect(result.paths[0].contours[0].points.length).toBe(sides);
    }
  });
});
