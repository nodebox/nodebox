import { describe, it, expect } from 'vitest';
import { parseNdbx, clearLibraryCache } from '../../src/ndbx/parser.js';

describe('NDBX Parser', () => {
  beforeEach(() => clearLibraryCache());

  it('parses a minimal ndbx file', () => {
    const xml = `<?xml version="1.0" encoding="UTF-8"?>
      <ndbx formatVersion="21" type="file" uuid="test-uuid">
        <node name="root" renderedChild="rect1">
          <node name="rect1" position="1.00,2.00" function="corevector/rect">
            <port name="width" type="float" value="200.0"/>
            <port name="height" type="float" value="150.0"/>
          </node>
        </node>
      </ndbx>`;

    const lib = parseNdbx(xml);
    expect(lib.formatVersion).toBe(21);
    expect(lib.uuid).toBe('test-uuid');
    expect(lib.root.name).toBe('root');
    expect(lib.root.renderedChild).toBe('rect1');
    expect(lib.root.children.length).toBe(1);

    const rect = lib.root.children[0];
    expect(rect.name).toBe('rect1');
    expect(rect.function).toBe('corevector/rect');
    expect(rect.position).toEqual({ x: 1, y: 2 });
    expect(rect.inputs.length).toBe(2);
    expect(rect.inputs[0].name).toBe('width');
    expect(rect.inputs[0].value).toEqual({ type: 'float', value: 200 });
  });

  it('parses properties', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <property name="canvasWidth" value="2000"/>
      <property name="canvasHeight" value="1500"/>
      <node name="root"/>
    </ndbx>`;

    const lib = parseNdbx(xml);
    expect(lib.properties.canvasWidth).toBe('2000');
    expect(lib.properties.canvasHeight).toBe('1500');
  });

  it('parses function links', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <link href="python:pyvector.py" rel="functions"/>
      <link href="java:nodebox.function.CoreVectorFunctions" rel="functions"/>
      <node name="root"/>
    </ndbx>`;

    const lib = parseNdbx(xml);
    expect(lib.functionLinks).toEqual([
      'python:pyvector.py',
      'java:nodebox.function.CoreVectorFunctions',
    ]);
  });

  it('parses connections', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <node name="root" renderedChild="colorize1">
        <node name="rect1"/>
        <node name="colorize1"/>
        <conn input="colorize1.shape" output="rect1"/>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    expect(lib.root.connections.length).toBe(1);
    const conn = lib.root.connections[0];
    expect(conn.outputNode).toBe('rect1');
    expect(conn.inputNode).toBe('colorize1');
    expect(conn.inputPort).toBe('shape');
  });

  it('parses port types correctly', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <node name="root">
        <node name="test1">
          <port name="count" type="int" value="5"/>
          <port name="ratio" type="float" value="3.14"/>
          <port name="flag" type="boolean" value="true"/>
          <port name="text" type="string" value="hello"/>
          <port name="pos" type="point" value="10.00,20.00"/>
          <port name="col" type="color" value="#ff0000ff"/>
        </node>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    const node = lib.root.children[0];
    expect(node.inputs[0].value).toEqual({ type: 'int', value: 5 });
    expect(node.inputs[1].value).toEqual({ type: 'float', value: 3.14 });
    expect(node.inputs[2].value).toEqual({ type: 'boolean', value: true });
    expect(node.inputs[3].value).toEqual({ type: 'string', value: 'hello' });
    expect(node.inputs[4].value).toEqual({ type: 'point', value: { x: 10, y: 20 } });
    expect(node.inputs[5].value).toEqual({
      type: 'color',
      value: { r: 1, g: 0, b: 0, a: 1 },
    });
  });

  it('parses menu items', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <node name="root">
        <node name="test1">
          <port name="type" type="string" value="pie" widget="menu">
            <menu key="pie" label="Pie"/>
            <menu key="chord" label="Chord"/>
            <menu key="open" label="Open"/>
          </port>
        </node>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    const port = lib.root.children[0].inputs[0];
    expect(port.menuItems.length).toBe(3);
    expect(port.menuItems[0]).toEqual({ key: 'pie', label: 'Pie' });
  });

  it('parses port constraints', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <node name="root">
        <node name="test1">
          <port name="width" type="float" value="100.0" min="0.0" max="500.0"/>
        </node>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    const port = lib.root.children[0].inputs[0];
    expect(port.minimumValue).toBe(0);
    expect(port.maximumValue).toBe(500);
  });

  it('parses published ports (childReference)', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <node name="root">
        <node name="mesh" renderedChild="line1">
          <node name="line1"/>
          <node name="slice1"/>
          <port name="list" type="list" range="list" childReference="slice1/list"/>
          <port name="start_index" type="int" value="1" childReference="slice1/start_index"/>
        </node>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    const mesh = lib.root.children[0];
    expect(mesh.inputs.length).toBe(2);
    expect(mesh.inputs[0].childReference).toBe('slice1/list');
    expect(mesh.inputs[1].childReference).toBe('slice1/start_index');
  });

  it('parses nested networks', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <node name="root" renderedChild="outer">
        <node name="outer" renderedChild="inner">
          <node name="inner" renderedChild="leaf">
            <node name="leaf"/>
          </node>
        </node>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    expect(lib.root.children[0].children[0].children[0].name).toBe('leaf');
  });

  it('parses importCoreNode', () => {
    const xml = `<ndbx type="file">
      <node name="root">
        <importCoreNode ref="ROOT"/>
        <importCoreNode ref="NETWORK"/>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    expect(lib.root.children.length).toBe(2);
    expect(lib.root.children[0].name).toBe('ROOT');
    expect(lib.root.children[1].name).toBe('NETWORK');
  });

  it('resolves prototypes from loaded libraries', () => {
    // First, parse a "library" that defines the rect prototype
    const libXml = `<ndbx formatVersion="21" type="file">
      <node name="root">
        <node name="generator" function="corevector/generator" outputType="geometry"/>
        <node name="rect" function="corevector/rect" prototype="generator" outputType="geometry">
          <port name="position" type="point" value="0.00,0.00"/>
          <port name="width" type="float" value="100.0"/>
          <port name="height" type="float" value="100.0"/>
          <port name="roundness" type="point" value="0.00,0.00"/>
        </node>
      </node>
    </ndbx>`;

    const corevectorLib = parseNdbx(libXml);

    // Now parse a file that references this library
    const fileXml = `<ndbx formatVersion="21" type="file">
      <node name="root" renderedChild="rect1">
        <node name="rect1" prototype="corevector.rect">
          <port name="width" type="float" value="200.0"/>
        </node>
      </node>
    </ndbx>`;

    const lib = parseNdbx(fileXml, (name) => {
      if (name === 'corevector') return corevectorLib;
      return undefined;
    });

    const rect1 = lib.root.children[0];
    expect(rect1.function).toBe('corevector/rect');
    // Width overridden
    const widthPort = rect1.inputs.find(p => p.name === 'width');
    expect(widthPort?.value).toEqual({ type: 'float', value: 200 });
    // Height inherited from prototype
    const heightPort = rect1.inputs.find(p => p.name === 'height');
    expect(heightPort?.value).toEqual({ type: 'float', value: 100 });
  });
});
