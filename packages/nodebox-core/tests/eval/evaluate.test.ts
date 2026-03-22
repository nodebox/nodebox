import { describe, it, expect } from 'vitest';
import { evaluate, type EvalOptions } from '../../src/eval/evaluate.js';
import { createFunctionRegistry } from '../../src/eval/function-registry.js';
import { createDefaultRegistry } from '../../src/eval/register-ops.js';
import { createNode, addChild, addConnection, setRenderedChild } from '../../src/node/node.js';
import { createPort } from '../../src/node/port.js';
import { createNodeLibrary } from '../../src/node/library.js';
import { floatValue, intValue, type Value } from '../../src/node/value.js';
import { TestPlatform } from '../../src/platform.js';

function makeEvalOptions(root: ReturnType<typeof createNode>, registry?: ReturnType<typeof createFunctionRegistry>): EvalOptions {
  return {
    library: createNodeLibrary(root),
    frame: 1,
    platform: new TestPlatform(),
    functionRegistry: registry,
  };
}

describe('Evaluation Engine', () => {
  it('evaluates a single leaf node', async () => {
    const registry = createDefaultRegistry();
    registry.register('test/constant', () => ({ type: 'float', value: 42 }));

    let node = createNode('const1');
    node = { ...node, function: 'test/constant' };
    let root = createNode('root');
    root = addChild(root, node);
    root = setRenderedChild(root, 'const1');

    const result = await evaluate(makeEvalOptions(root, registry));
    expect(result.output).toEqual([{ type: 'float', value: 42 }]);
    expect(result.errors).toEqual([]);
  });

  it('evaluates core/zero for default function', async () => {
    let node = createNode('zero1');
    // No function set — defaults to core/zero
    let root = createNode('root');
    root = addChild(root, node);
    root = setRenderedChild(root, 'zero1');

    const result = await evaluate(makeEvalOptions(root));
    expect(result.output).toEqual([{ type: 'float', value: 0.0 }]);
  });

  it('evaluates core/frame', async () => {
    let node = createNode('frame1');
    node = {
      ...node,
      function: 'core/frame',
      inputs: [createPort('context', 'context')],
    };
    let root = createNode('root');
    root = addChild(root, node);
    root = setRenderedChild(root, 'frame1');

    const result = await evaluate({
      ...makeEvalOptions(root),
      frame: 42,
    });
    expect(result.output).toEqual([{ type: 'float', value: 42 }]);
  });

  it('evaluates connected nodes', async () => {
    const registry = createDefaultRegistry();
    registry.register('test/double', (v: Value) => ({
      type: 'float',
      value: (v.type === 'float' ? v.value : 0) * 2,
    }));
    registry.register('test/const5', () => ({ type: 'float', value: 5 }));

    let const5 = createNode('const5');
    const5 = { ...const5, function: 'test/const5' };

    let doubler = createNode('double1');
    doubler = {
      ...doubler,
      function: 'test/double',
      inputs: [createPort('value', 'float', floatValue(0))],
    };

    let root = createNode('root');
    root = addChild(root, const5);
    root = addChild(root, doubler);
    root = addConnection(root, { outputNode: 'const5', inputNode: 'double1', inputPort: 'value' });
    root = setRenderedChild(root, 'double1');

    const result = await evaluate(makeEvalOptions(root, registry));
    expect(result.output).toEqual([{ type: 'float', value: 10 }]);
  });

  it('reports errors for missing functions', async () => {
    let node = createNode('bad1');
    node = { ...node, function: 'nonexistent/func' };
    let root = createNode('root');
    root = addChild(root, node);
    root = setRenderedChild(root, 'bad1');

    const result = await evaluate(makeEvalOptions(root));
    expect(result.errors.length).toBe(1);
    expect(result.errors[0].message).toContain('nonexistent/func');
  });

  it('uses port default when not connected', async () => {
    const registry = createDefaultRegistry();
    registry.register('test/identity', (v: Value) => v);

    let node = createNode('id1');
    node = {
      ...node,
      function: 'test/identity',
      inputs: [createPort('value', 'float', floatValue(99))],
    };
    let root = createNode('root');
    root = addChild(root, node);
    root = setRenderedChild(root, 'id1');

    const result = await evaluate(makeEvalOptions(root, registry));
    expect(result.output).toEqual([{ type: 'float', value: 99 }]);
  });

  it('handles node that returns null', async () => {
    const registry = createDefaultRegistry();
    registry.register('test/null', () => ({ type: 'null' }));

    let node = createNode('n1');
    node = { ...node, function: 'test/null' };
    let root = createNode('root');
    root = addChild(root, node);
    root = setRenderedChild(root, 'n1');

    const result = await evaluate(makeEvalOptions(root, registry));
    expect(result.output).toEqual([]);
  });

  it('catches function exceptions', async () => {
    const registry = createDefaultRegistry();
    registry.register('test/throw', () => { throw new Error('boom'); });

    let node = createNode('t1');
    node = { ...node, function: 'test/throw' };
    let root = createNode('root');
    root = addChild(root, node);
    root = setRenderedChild(root, 't1');

    const result = await evaluate(makeEvalOptions(root, registry));
    expect(result.errors[0].message).toBe('boom');
  });
});
