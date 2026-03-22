import { describe, it, expect } from 'vitest';
import {
  createNode, isNetwork, hasRenderedChild, getChild, getPort,
  addChild, removeChild, addConnection, removeConnection,
  setPortValue, setRenderedChild, setNodePosition,
  getConnection, getConnectionsTo, getConnectionsFrom,
} from '../../src/node/node.js';
import { createPort } from '../../src/node/port.js';
import { floatValue, intValue } from '../../src/node/value.js';

describe('Node', () => {
  it('creates a basic node', () => {
    const n = createNode('rect1');
    expect(n.name).toBe('rect1');
    expect(n.children).toEqual([]);
    expect(n.connections).toEqual([]);
    expect(n.inputs).toEqual([]);
    expect(n.outputType).toBe('geometry');
    expect(n.alwaysRendered).toBe(false);
    expect(n.comment).toBeNull();
  });

  it('detects network nodes', () => {
    const n = createNode('root');
    expect(isNetwork(n)).toBe(false);
    const withChild = addChild(n, createNode('child1'));
    expect(isNetwork(withChild)).toBe(true);
  });

  it('detects rendered child', () => {
    const n = createNode('root');
    expect(hasRenderedChild(n)).toBe(false);
    expect(hasRenderedChild(setRenderedChild(n, 'child1'))).toBe(true);
  });

  it('adds and removes children', () => {
    let n = createNode('root');
    n = addChild(n, createNode('a'));
    n = addChild(n, createNode('b'));
    expect(n.children.length).toBe(2);
    expect(getChild(n, 'a')?.name).toBe('a');

    n = removeChild(n, 'a');
    expect(n.children.length).toBe(1);
    expect(getChild(n, 'a')).toBeUndefined();
  });

  it('removing child also removes its connections', () => {
    let n = createNode('root');
    n = addChild(n, createNode('a'));
    n = addChild(n, createNode('b'));
    n = addConnection(n, { outputNode: 'a', inputNode: 'b', inputPort: 'shape' });
    expect(n.connections.length).toBe(1);
    n = removeChild(n, 'a');
    expect(n.connections.length).toBe(0);
  });

  it('adds and removes connections', () => {
    let n = createNode('root');
    n = addConnection(n, { outputNode: 'a', inputNode: 'b', inputPort: 'shape' });
    expect(n.connections.length).toBe(1);
    expect(getConnection(n, 'b', 'shape')).toBeDefined();

    n = removeConnection(n, 'b', 'shape');
    expect(n.connections.length).toBe(0);
  });

  it('replaces existing connection to same input', () => {
    let n = createNode('root');
    n = addConnection(n, { outputNode: 'a', inputNode: 'c', inputPort: 'shape' });
    n = addConnection(n, { outputNode: 'b', inputNode: 'c', inputPort: 'shape' });
    expect(n.connections.length).toBe(1);
    expect(n.connections[0].outputNode).toBe('b');
  });

  it('gets connections to/from a child', () => {
    let n = createNode('root');
    n = addConnection(n, { outputNode: 'a', inputNode: 'b', inputPort: 'shape' });
    n = addConnection(n, { outputNode: 'a', inputNode: 'c', inputPort: 'shape' });
    expect(getConnectionsFrom(n, 'a').length).toBe(2);
    expect(getConnectionsTo(n, 'b').length).toBe(1);
  });

  it('sets port value', () => {
    let n = createNode('rect1');
    n = { ...n, inputs: [createPort('width', 'float', floatValue(100))] };
    n = setPortValue(n, 'width', floatValue(200));
    expect(getPort(n, 'width')?.value).toEqual(floatValue(200));
  });

  it('sets node position', () => {
    const n = setNodePosition(createNode('a'), { x: 10, y: 20 });
    expect(n.position).toEqual({ x: 10, y: 20 });
  });
});
