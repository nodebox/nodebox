// TypeScript types mirroring crates/nodebox-core/src/node/
// Field names match Rust serde serialization (snake_case).

import type { Point } from './geometry';
import type { Value } from './value';

export type PortType =
  | 'Int'
  | 'Float'
  | 'String'
  | 'Boolean'
  | 'Point'
  | 'Color'
  | 'Geometry'
  | 'Data'
  | 'List'
  | 'Context'
  | 'State';

export type Widget =
  | 'None'
  | 'Int'
  | 'Float'
  | 'Angle'
  | 'String'
  | 'Text'
  | 'Password'
  | 'Toggle'
  | 'Color'
  | 'Point'
  | 'Menu'
  | 'File'
  | 'Font'
  | 'Image'
  | 'Data'
  | 'Seed'
  | 'Gradient';

export type PortRange = 'Value' | 'List';

export interface MenuItem {
  key: string;
  label: string;
}

export interface Port {
  name: string;
  port_type: PortType;
  label: string | null;
  description: string | null;
  widget: Widget;
  range: PortRange;
  value: Value;
  min: number | null;
  max: number | null;
  menu_items: MenuItem[];
}

export interface Connection {
  output_node: string;
  input_node: string;
  input_port: string;
}

export interface Node {
  name: string;
  prototype: string | null;
  function: string | null;
  category: string;
  description: string | null;
  comment: string | null;
  position: Point;
  inputs: Port[];
  output_type: PortType;
  output_range: PortRange;
  is_network: boolean;
  children: Node[];
  rendered_child: string | null;
  connections: Connection[];
  handle: string | null;
  always_rendered: boolean;
}

export interface NodeLibrary {
  name: string;
  root: Node;
  properties: Record<string, string>;
  uuid: string | null;
  format_version: number;
}

export function createDefaultNode(name: string): Node {
  return {
    name,
    prototype: null,
    function: null,
    category: '',
    description: null,
    comment: null,
    position: { x: 0, y: 0 },
    inputs: [],
    output_type: 'Geometry',
    output_range: 'Value',
    is_network: false,
    children: [],
    rendered_child: null,
    connections: [],
    handle: null,
    always_rendered: false,
  };
}

export function createDefaultLibrary(): NodeLibrary {
  const rect1: Node = {
    ...createDefaultNode('rect1'),
    prototype: 'corevector.rect',
    category: 'geometry',
    position: { x: 1, y: 1 },
    inputs: [
      {
        name: 'position',
        port_type: 'Point',
        label: 'position',
        description: null,
        widget: 'Point',
        range: 'Value',
        value: { Point: { x: 0, y: 0 } },
        min: null,
        max: null,
        menu_items: [],
      },
      {
        name: 'width',
        port_type: 'Float',
        label: 'width',
        description: null,
        widget: 'Float',
        range: 'Value',
        value: { Float: 100 },
        min: null,
        max: null,
        menu_items: [],
      },
      {
        name: 'height',
        port_type: 'Float',
        label: 'height',
        description: null,
        widget: 'Float',
        range: 'Value',
        value: { Float: 100 },
        min: null,
        max: null,
        menu_items: [],
      },
      {
        name: 'roundness',
        port_type: 'Point',
        label: 'roundness',
        description: null,
        widget: 'Point',
        range: 'Value',
        value: { Point: { x: 0, y: 0 } },
        min: null,
        max: null,
        menu_items: [],
      },
    ],
    output_type: 'Geometry',
  };

  return {
    name: '',
    root: {
      ...createDefaultNode('root'),
      is_network: true,
      output_range: 'List',
      children: [rect1],
      rendered_child: 'rect1',
    },
    properties: {
      canvasWidth: '1000',
      canvasHeight: '1000',
    },
    uuid: null,
    format_version: 21,
  };
}
