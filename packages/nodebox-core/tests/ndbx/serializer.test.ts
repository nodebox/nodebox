import { describe, it, expect } from 'vitest';
import { parseNdbx, clearLibraryCache } from '../../src/ndbx/parser.js';
import { serializeNdbx } from '../../src/ndbx/serializer.js';

describe('NDBX Serializer', () => {
  beforeEach(() => clearLibraryCache());

  it('serializes and re-parses a library (round-trip)', () => {
    const xml = `<?xml version="1.0" encoding="UTF-8"?>
      <ndbx formatVersion="21" type="file" uuid="test-uuid">
        <property name="canvasWidth" value="1000"/>
        <property name="canvasHeight" value="1000"/>
        <node name="root" renderedChild="rect1">
          <node name="rect1" function="corevector/rect" position="1.00,2.00">
            <port name="width" type="float" value="200.0" widget="float"/>
            <port name="height" type="float" value="150.0" widget="float"/>
          </node>
          <node name="colorize1" function="corevector/colorize" position="1.00,4.00">
            <port name="fill" type="color" value="#ff0000ff" widget="color"/>
          </node>
          <conn input="colorize1.shape" output="rect1"/>
        </node>
      </ndbx>`;

    const lib1 = parseNdbx(xml);
    const serialized = serializeNdbx(lib1);
    const lib2 = parseNdbx(serialized);

    expect(lib2.root.name).toBe(lib1.root.name);
    expect(lib2.root.renderedChild).toBe(lib1.root.renderedChild);
    expect(lib2.root.children.length).toBe(lib1.root.children.length);
    expect(lib2.root.connections.length).toBe(lib1.root.connections.length);
    expect(lib2.root.children[0].name).toBe('rect1');
    expect(lib2.root.children[0].inputs[0].value).toEqual({ type: 'float', value: 200 });
  });

  it('serializes properties', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <property name="canvasWidth" value="2000"/>
      <node name="root"/>
    </ndbx>`;

    const lib = parseNdbx(xml);
    const serialized = serializeNdbx(lib);
    expect(serialized).toContain('canvasWidth');
    expect(serialized).toContain('2000');
  });

  it('serializes menu items', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <node name="root">
        <node name="test1">
          <port name="type" type="string" value="pie" widget="menu">
            <menu key="pie" label="Pie"/>
            <menu key="chord" label="Chord"/>
          </port>
        </node>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    const serialized = serializeNdbx(lib);
    expect(serialized).toContain('menu key="pie"');
    expect(serialized).toContain('menu key="chord"');
  });

  it('escapes XML special characters', () => {
    const xml = `<ndbx formatVersion="21" type="file">
      <node name="root">
        <node name="test1" description="A &amp; B"/>
      </node>
    </ndbx>`;

    const lib = parseNdbx(xml);
    const serialized = serializeNdbx(lib);
    expect(serialized).toContain('&amp;');
  });
});
