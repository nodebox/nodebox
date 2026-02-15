// TypeScript types mirroring crates/nodebox-core/src/node/

import type { Point } from './geometry';
import type { Value } from './value';

export type PortType =
  | 'int'
  | 'float'
  | 'string'
  | 'boolean'
  | 'point'
  | 'color'
  | 'geometry'
  | 'data'
  | 'list'
  | 'context'
  | 'state';

export type Widget =
  | 'none'
  | 'int'
  | 'float'
  | 'angle'
  | 'string'
  | 'text'
  | 'password'
  | 'toggle'
  | 'color'
  | 'point'
  | 'menu'
  | 'file'
  | 'font'
  | 'image'
  | 'data'
  | 'seed'
  | 'gradient';

export type PortRange = 'value' | 'list';

export interface MenuItem {
  key: string;
  label: string;
}

export interface Port {
  name: string;
  portType: PortType;
  label: string | null;
  description: string | null;
  widget: Widget;
  range: PortRange;
  value: Value;
  min: number | null;
  max: number | null;
  menuItems: MenuItem[];
}

export interface Connection {
  outputNode: string;
  inputNode: string;
  inputPort: string;
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
  outputType: PortType;
  outputRange: PortRange;
  isNetwork: boolean;
  children: Node[];
  renderedChild: string | null;
  connections: Connection[];
  handle: string | null;
  alwaysRendered: boolean;
}

export interface NodeLibrary {
  name: string;
  root: Node;
  properties: Record<string, string>;
  uuid: string | null;
  formatVersion: number;
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
    outputType: 'geometry',
    outputRange: 'value',
    isNetwork: false,
    children: [],
    renderedChild: null,
    connections: [],
    handle: null,
    alwaysRendered: false,
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
        portType: 'point',
        label: 'Position',
        description: null,
        widget: 'point',
        range: 'value',
        value: { type: 'point', value: { x: 0, y: 0 } },
        min: null,
        max: null,
        menuItems: [],
      },
      {
        name: 'width',
        portType: 'float',
        label: 'Width',
        description: null,
        widget: 'float',
        range: 'value',
        value: { type: 'float', value: 100 },
        min: null,
        max: null,
        menuItems: [],
      },
      {
        name: 'height',
        portType: 'float',
        label: 'Height',
        description: null,
        widget: 'float',
        range: 'value',
        value: { type: 'float', value: 100 },
        min: null,
        max: null,
        menuItems: [],
      },
      {
        name: 'roundness',
        portType: 'point',
        label: 'Roundness',
        description: null,
        widget: 'point',
        range: 'value',
        value: { type: 'point', value: { x: 0, y: 0 } },
        min: null,
        max: null,
        menuItems: [],
      },
    ],
    outputType: 'geometry',
  };

  return {
    name: '',
    root: {
      ...createDefaultNode('root'),
      isNetwork: true,
      outputRange: 'list',
      children: [rect1],
      renderedChild: 'rect1',
    },
    properties: {
      canvasWidth: '1000',
      canvasHeight: '1000',
    },
    uuid: null,
    formatVersion: 21,
  };
}
