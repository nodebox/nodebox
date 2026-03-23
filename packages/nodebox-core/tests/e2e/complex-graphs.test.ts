/**
 * E2E tests for complex multi-node graph evaluation.
 * Tests realistic NodeBox workflows.
 */
import { describe, it, expect } from 'vitest';
import { evaluate } from '../../src/eval/evaluate';
import { createNodeLibrary } from '../../src/node/library';
import { createNode, addChild, addConnection, setRenderedChild } from '../../src/node/node';
import { createPort } from '../../src/node/port';
import { TestPlatform } from '../../src/platform';
import type { Node } from '../../src/index';

function n(name: string, fn: string, outputType: string, inputs: any[], opts?: Partial<Node>): Node {
  return { ...createNode(name), function: fn, outputType, inputs: inputs.map(([pn, pt, pv]: any) => createPort(pn, pt, pv)), ...opts };
}

const pt = (x: number, y: number) => ({ type: 'point' as const, value: { x, y } });
const f = (v: number) => ({ type: 'float' as const, value: v });
const i = (v: number) => ({ type: 'int' as const, value: v });
const s = (v: string) => ({ type: 'string' as const, value: v });
const b = (v: boolean) => ({ type: 'boolean' as const, value: v });
const col = (r: number, g: number, b: number, a: number) => ({ type: 'color' as const, value: { r, g, b, a } });

async function evalRoot(root: Node) {
  return evaluate({ library: createNodeLibrary(root), frame: 1, platform: new TestPlatform() });
}

describe('E2E: Complex Graph Evaluation', () => {
  it('rect → copy → colorize pipeline', async () => {
    const rect1 = n('rect1', 'corevector/rect', 'geometry', [
      ['position', 'point', pt(0, 0)], ['width', 'float', f(20)], ['height', 'float', f(20)], ['roundness', 'point', pt(0, 0)],
    ]);
    const copy1 = n('copy1', 'corevector/copy', 'geometry', [
      ['shape', 'geometry', { type: 'null' }], ['copies', 'int', i(3)], ['order', 'string', s('tsr')],
      ['translate', 'point', pt(30, 0)], ['rotate', 'float', f(0)], ['scale', 'point', pt(100, 100)],
    ], { outputRange: 'list' });
    const colorize1 = n('colorize1', 'corevector/colorize', 'geometry', [
      ['shape', 'geometry', { type: 'null' }], ['fill', 'color', col(1, 0, 0, 1)],
      ['stroke', 'color', col(0, 0, 0, 1)], ['strokeWidth', 'float', f(0)],
    ]);

    let root = createNode('root');
    root = addChild(root, rect1);
    root = addChild(root, copy1);
    root = addChild(root, colorize1);
    root = addConnection(root, { outputNode: 'rect1', inputNode: 'copy1', inputPort: 'shape' });
    root = addConnection(root, { outputNode: 'copy1', inputNode: 'colorize1', inputPort: 'shape' });
    root = setRenderedChild(root, 'colorize1');

    const result = await evalRoot(root);
    expect(result.errors).toEqual([]);
    // copy produces 3, colorize processes each → 3 red paths
    expect(result.paths.length).toBe(3);
    for (const path of result.paths) {
      expect(path.fill).toEqual({ r: 1, g: 0, b: 0, a: 1 });
    }
  });

  it('grid → connect → colorize pipeline', async () => {
    const grid1 = n('grid1', 'corevector/grid', 'point', [
      ['columns', 'int', i(3)], ['rows', 'int', i(3)], ['width', 'float', f(100)],
      ['height', 'float', f(100)], ['position', 'point', pt(0, 0)],
    ], { outputRange: 'list' });
    const connect1 = n('connect1', 'corevector/connect', 'geometry', [
      ['points', 'list', { type: 'null' }], ['closed', 'boolean', b(false)],
    ]);
    // Mark points port as list range
    connect1.inputs[0].range = 'list';

    const colorize1 = n('colorize1', 'corevector/colorize', 'geometry', [
      ['shape', 'geometry', { type: 'null' }], ['fill', 'color', col(0, 0, 1, 1)],
      ['stroke', 'color', col(0, 0, 0, 1)], ['strokeWidth', 'float', f(2)],
    ]);

    let root = createNode('root');
    root = addChild(root, grid1);
    root = addChild(root, connect1);
    root = addChild(root, colorize1);
    root = addConnection(root, { outputNode: 'grid1', inputNode: 'connect1', inputPort: 'points' });
    root = addConnection(root, { outputNode: 'connect1', inputNode: 'colorize1', inputPort: 'shape' });
    root = setRenderedChild(root, 'colorize1');

    const result = await evalRoot(root);
    expect(result.errors).toEqual([]);
    expect(result.paths.length).toBeGreaterThanOrEqual(1);
    expect(result.paths[0].fill).toEqual({ r: 0, g: 0, b: 1, a: 1 });
    expect(result.paths[0].strokeWidth).toBe(2);
  });

  it('ellipse → wiggle with seed', async () => {
    const ellipse1 = n('ellipse1', 'corevector/ellipse', 'geometry', [
      ['position', 'point', pt(0, 0)], ['width', 'float', f(100)], ['height', 'float', f(100)],
    ]);
    const wiggle1 = n('wiggle1', 'corevector/wiggle', 'geometry', [
      ['shape', 'geometry', { type: 'null' }], ['scope', 'string', s('points')],
      ['offset', 'point', pt(5, 5)], ['seed', 'int', i(42)],
    ], { outputRange: 'list' });

    let root = createNode('root');
    root = addChild(root, ellipse1);
    root = addChild(root, wiggle1);
    root = addConnection(root, { outputNode: 'ellipse1', inputNode: 'wiggle1', inputPort: 'shape' });
    root = setRenderedChild(root, 'wiggle1');

    const r1 = await evalRoot(root);
    const r2 = await evalRoot(root);
    expect(r1.errors).toEqual([]);
    expect(r1.paths.length).toBe(1);
    // Same seed → same result (deterministic)
    expect(r1.paths[0].contours[0].points[0].point.x).toBe(r2.paths[0].contours[0].points[0].point.x);
  });

  it('polygon → resample → centroid', async () => {
    const polygon1 = n('polygon1', 'corevector/polygon', 'geometry', [
      ['position', 'point', pt(0, 0)], ['radius', 'float', f(50)], ['sides', 'int', i(6)], ['align', 'boolean', b(true)],
    ]);
    const resample1 = n('resample1', 'corevector/resample', 'geometry', [
      ['shape', 'geometry', { type: 'null' }], ['method', 'string', s('amount')],
      ['length', 'float', f(10)], ['points', 'int', i(30)], ['perContour', 'boolean', b(false)],
    ]);
    const centroid1 = n('centroid1', 'corevector/centroid', 'point', [
      ['shape', 'geometry', { type: 'null' }],
    ]);

    let root = createNode('root');
    root = addChild(root, polygon1);
    root = addChild(root, resample1);
    root = addChild(root, centroid1);
    root = addConnection(root, { outputNode: 'polygon1', inputNode: 'resample1', inputPort: 'shape' });
    root = addConnection(root, { outputNode: 'resample1', inputNode: 'centroid1', inputPort: 'shape' });
    root = setRenderedChild(root, 'centroid1');

    const result = await evalRoot(root);
    expect(result.errors).toEqual([]);
    expect(result.output.length).toBe(1);
    expect(result.output[0].type).toBe('point');
    // Centroid of a regular polygon centered at origin should be near (0,0)
    const c = (result.output[0] as any).value;
    expect(Math.abs(c.x)).toBeLessThan(5);
    expect(Math.abs(c.y)).toBeLessThan(5);
  });

  it('math chain: number → multiply → add', async () => {
    const num1 = n('num1', 'math/number', 'float', [['value', 'float', f(5)]]);
    const num2 = n('num2', 'math/number', 'float', [['value', 'float', f(3)]]);
    const mul1 = n('mul1', 'math/multiply', 'float', [['value1', 'float', f(0)], ['value2', 'float', f(0)]]);
    const add1 = n('add1', 'math/add', 'float', [['value1', 'float', f(0)], ['value2', 'float', f(10)]]);

    let root = createNode('root');
    root = addChild(root, num1);
    root = addChild(root, num2);
    root = addChild(root, mul1);
    root = addChild(root, add1);
    root = addConnection(root, { outputNode: 'num1', inputNode: 'mul1', inputPort: 'value1' });
    root = addConnection(root, { outputNode: 'num2', inputNode: 'mul1', inputPort: 'value2' });
    root = addConnection(root, { outputNode: 'mul1', inputNode: 'add1', inputPort: 'value1' });
    root = setRenderedChild(root, 'add1');

    const result = await evalRoot(root);
    expect(result.errors).toEqual([]);
    // 5 * 3 = 15, 15 + 10 = 25
    expect(result.output).toEqual([{ type: 'float', value: 25 }]);
  });

  it('frame-based animation: frame → multiply', async () => {
    const frame1 = n('frame1', 'core/frame', 'float', [['context', 'context', { type: 'null' }]]);
    const mul1 = n('mul1', 'math/multiply', 'float', [['value1', 'float', f(0)], ['value2', 'float', f(10)]]);

    let root = createNode('root');
    root = addChild(root, frame1);
    root = addChild(root, mul1);
    root = addConnection(root, { outputNode: 'frame1', inputNode: 'mul1', inputPort: 'value1' });
    root = setRenderedChild(root, 'mul1');

    const lib = createNodeLibrary(root);

    // Frame 1: 1 * 10 = 10
    const r1 = await evaluate({ library: lib, frame: 1, platform: new TestPlatform() });
    expect(r1.output).toEqual([{ type: 'float', value: 10 }]);

    // Frame 5: 5 * 10 = 50
    const r5 = await evaluate({ library: lib, frame: 5, platform: new TestPlatform() });
    expect(r5.output).toEqual([{ type: 'float', value: 50 }]);
  });
});
