import { describe, it, expect } from 'vitest';
import { evaluate } from '../../src/eval/evaluate.js';
import { createFunctionRegistry } from '../../src/eval/function-registry.js';
import { createDefaultRegistry } from '../../src/eval/register-ops.js';
import { createNode, addChild, addConnection, setRenderedChild } from '../../src/node/node.js';
import { createPort, type Port } from '../../src/node/port.js';
import { createNodeLibrary } from '../../src/node/library.js';
import { floatValue, type Value } from '../../src/node/value.js';
import { TestPlatform } from '../../src/platform.js';

describe('List Matching', () => {
  it('evaluates with longest list matching', async () => {
    // Build: gen_list (returns [1, 2, 3]) -> adder (adds 10)
    // adder should be invoked 3 times with wrapping
    const registry = createDefaultRegistry();

    // A function that returns a list of 3 values
    registry.register('test/list3', () => [
      { type: 'float', value: 1 },
      { type: 'float', value: 2 },
      { type: 'float', value: 3 },
    ] as Value[]);

    // A function that adds two numbers
    registry.register('test/add', (a: Value, b: Value) => ({
      type: 'float',
      value: (a.type === 'float' ? a.value : 0) + (b.type === 'float' ? b.value : 0),
    }));

    let list3 = createNode('list3');
    list3 = { ...list3, function: 'test/list3', outputRange: 'list' };

    let adder = createNode('add1');
    adder = {
      ...adder,
      function: 'test/add',
      inputs: [
        createPort('a', 'float', floatValue(0)),
        createPort('b', 'float', floatValue(10)),
      ],
    };

    let root = createNode('root');
    root = addChild(root, list3);
    root = addChild(root, adder);
    root = addConnection(root, { outputNode: 'list3', inputNode: 'add1', inputPort: 'a' });
    root = setRenderedChild(root, 'add1');

    const result = await evaluate({
      library: createNodeLibrary(root),
      frame: 1,
      platform: new TestPlatform(),
      functionRegistry: registry,
    });

    // adder called 3 times: 1+10=11, 2+10=12, 3+10=13
    expect(result.output).toEqual([
      { type: 'float', value: 11 },
      { type: 'float', value: 12 },
      { type: 'float', value: 13 },
    ]);
  });

  it('handles list-range ports (gets full list)', async () => {
    const registry = createDefaultRegistry();
    let called = false;

    registry.register('test/list3', () => [
      { type: 'float', value: 1 },
      { type: 'float', value: 2 },
      { type: 'float', value: 3 },
    ] as Value[]);

    registry.register('test/count', (list: Value) => {
      called = true;
      if (list.type === 'list') {
        return { type: 'int', value: list.value.length };
      }
      return { type: 'int', value: 0 };
    });

    let list3 = createNode('list3');
    list3 = { ...list3, function: 'test/list3', outputRange: 'list' };

    const listPort: Port = { ...createPort('list', 'list'), range: 'list' };
    let counter = createNode('count1');
    counter = { ...counter, function: 'test/count', inputs: [listPort] };

    let root = createNode('root');
    root = addChild(root, list3);
    root = addChild(root, counter);
    root = addConnection(root, { outputNode: 'list3', inputNode: 'count1', inputPort: 'list' });
    root = setRenderedChild(root, 'count1');

    const result = await evaluate({
      library: createNodeLibrary(root),
      frame: 1,
      platform: new TestPlatform(),
      functionRegistry: registry,
    });

    expect(called).toBe(true);
    expect(result.output).toEqual([{ type: 'int', value: 3 }]);
  });
});
