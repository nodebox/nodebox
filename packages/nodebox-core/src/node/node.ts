import type { Point } from '../geometry/point.js';
import type { Port, PortRange } from './port.js';

export interface Connection {
  outputNode: string;   // source node name
  inputNode: string;    // target node name
  inputPort: string;    // target port name
}

export interface Node {
  name: string;
  prototype: string | null;
  function: string | null;
  category: string | null;
  description: string | null;
  image: string | null;
  position: Point;
  comment: string | null;
  inputs: Port[];
  outputType: string;
  outputRange: PortRange;
  children: Node[];
  connections: Connection[];
  renderedChild: string | null;
  handle: string | null;
  alwaysRendered: boolean;
}

export function createNode(name: string): Node {
  return {
    name,
    prototype: null,
    function: null,
    category: null,
    description: null,
    image: null,
    position: { x: 0, y: 0 },
    comment: null,
    inputs: [],
    outputType: 'geometry',
    outputRange: 'value',
    children: [],
    connections: [],
    renderedChild: null,
    handle: null,
    alwaysRendered: false,
  };
}

export function isNetwork(node: Node): boolean {
  return node.children.length > 0;
}

export function hasRenderedChild(node: Node): boolean {
  return node.renderedChild !== null;
}

export function getChild(node: Node, name: string): Node | undefined {
  return node.children.find(c => c.name === name);
}

export function getPort(node: Node, name: string): Port | undefined {
  return node.inputs.find(p => p.name === name);
}

export function getConnection(node: Node, inputNode: string, inputPort: string): Connection | undefined {
  return node.connections.find(c => c.inputNode === inputNode && c.inputPort === inputPort);
}

export function getConnectionsTo(node: Node, childName: string): Connection[] {
  return node.connections.filter(c => c.inputNode === childName);
}

export function getConnectionsFrom(node: Node, childName: string): Connection[] {
  return node.connections.filter(c => c.outputNode === childName);
}

export function addChild(parent: Node, child: Node): Node {
  return { ...parent, children: [...parent.children, child] };
}

export function removeChild(parent: Node, childName: string): Node {
  return {
    ...parent,
    children: parent.children.filter(c => c.name !== childName),
    connections: parent.connections.filter(
      c => c.inputNode !== childName && c.outputNode !== childName,
    ),
  };
}

export function addConnection(parent: Node, connection: Connection): Node {
  // Remove existing connection to same input port
  const connections = parent.connections.filter(
    c => !(c.inputNode === connection.inputNode && c.inputPort === connection.inputPort),
  );
  return { ...parent, connections: [...connections, connection] };
}

export function removeConnection(parent: Node, inputNode: string, inputPort: string): Node {
  return {
    ...parent,
    connections: parent.connections.filter(
      c => !(c.inputNode === inputNode && c.inputPort === inputPort),
    ),
  };
}

export function setPortValue(node: Node, portName: string, value: import('./value.js').Value): Node {
  return {
    ...node,
    inputs: node.inputs.map(p => p.name === portName ? { ...p, value } : p),
  };
}

export function setRenderedChild(node: Node, childName: string | null): Node {
  return { ...node, renderedChild: childName };
}

export function setNodePosition(node: Node, position: Point): Node {
  return { ...node, position };
}
