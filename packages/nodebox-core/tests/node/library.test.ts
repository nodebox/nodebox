import { describe, it, expect } from 'vitest';
import { createNodeLibrary, flattenNodeMap, getCanvasWidth, getCanvasHeight } from '../../src/node/library.js';
import { createNode, addChild } from '../../src/node/node.js';

describe('NodeLibrary', () => {
  it('creates a library', () => {
    const root = createNode('root');
    const lib = createNodeLibrary(root);
    expect(lib.formatVersion).toBe(21);
    expect(lib.root.name).toBe('root');
    expect(lib.uuid).toMatch(/^[0-9a-f-]+$/);
  });

  it('gets canvas dimensions', () => {
    const lib = createNodeLibrary(createNode('root'));
    expect(getCanvasWidth(lib)).toBe(1000);
    expect(getCanvasHeight(lib)).toBe(1000);
  });

  it('flattens node map', () => {
    let root = createNode('root');
    let child1 = createNode('rect1');
    let child2 = createNode('ellipse1');
    root = addChild(root, child1);
    root = addChild(root, child2);

    const map = flattenNodeMap(root);
    expect(map.size).toBe(3);
    expect(map.has('root')).toBe(true);
    expect(map.has('root/rect1')).toBe(true);
    expect(map.has('root/ellipse1')).toBe(true);
  });

  it('flattens nested networks', () => {
    let root = createNode('root');
    let subnet = createNode('mesh');
    subnet = addChild(subnet, createNode('line1'));
    subnet = addChild(subnet, createNode('slice1'));
    root = addChild(root, subnet);

    const map = flattenNodeMap(root);
    expect(map.has('root/mesh/line1')).toBe(true);
    expect(map.has('root/mesh/slice1')).toBe(true);
  });
});
