import { Shape } from "@ndbx/g";

//// Interfaces /////////////////////////////////////////////////////////////

export interface Project {
  __gallery?: Gallery;
  /**
   * The project as it was stored, for a project in the classic NodeBox Live format. The editor
   * works with the converted `items`; the core engine renders from this.
   */
  __classicSource?: { key: string; project: unknown };
  id: string;
  formatVersion: number;
  title: string;
  description?: string;
  color?: Color;
  dependencies: Record<string, string>;
  assets: Record<string, string>;
  items: Item[];
  scope?: string;
  isPublished?: boolean;
  publishDate?: string;
}
export interface ProjectTitle {
  title: string;
}
export interface Gallery {
  keywords: string[];
  keyword: string;
  subKeyword: string;
  hash?: string; // prevent keywords being saved
}
export type Item = Network | FunctionItem;
export function item_isFunctionItem(item: Item): item is FunctionItem {
  return (item as FunctionItem).type === "FUNCTION";
}
export function item_isNetwork(item: Item): item is Network {
  return (item as Network).type === "NETWORK";
}

export type NetworkItemType = Node | Inlet | Outlet | Sticky;

export interface Network {
  type: "NETWORK";
  id: string;
  name: string;
  category: string;
  description: string;
  canvasSize: string;
  padding: number;
  width: number;
  height: number;
  background: Color;
  children: NetworkItem[];
  connections: Connection[];
  renderedNode: string | null;
  inputPorts: Port[];
  outputPorts: Port[];
  parameters: Parameter[];
  sections: Section[];
  __gallery?: Gallery;
}

export interface NetworkItem {
  id: string;
  type: string;
  x: number;
  y: number;
}

export interface Node extends NetworkItem {
  type: "NODE";
  name: string;
  fn: string;
  values?: Record<string, ParameterValue>;
}

export interface Inlet extends NetworkItem {
  type: "INLET";
  portName: string;
  portType: PortType;
}

export interface Outlet extends NetworkItem {
  type: "OUTLET";
  portName: string;
  portType: PortType;
}

export interface Sticky extends NetworkItem {
  type: "STICKY";
  width: number;
  height: number;
  backgroundColor: Color;
  text: string;
  fontSize: number;
  fontColor: Color;
}

export interface FunctionItem {
  type: "FUNCTION";
  id: string;
  name: string;
  category: string;
  description: string;
  width: number;
  height: number;
  background: Color;
  inputPorts: Port[];
  outputPorts: Port[];
  parameters: Parameter[];
  sections: Section[];
  source: string;
}

export interface BaseConnection {
  type: ConnectionType;
}

export interface NodeToNodeConnection extends BaseConnection {
  type: ConnectionType.NodeToNode;
  outNode: string;
  outPort: string;
  inNode: string;
  inPort: string;
}

export interface InletToNodeConnection extends BaseConnection {
  type: ConnectionType.InletToNode;
  inlet: string;
  inNode: string;
  inPort: string;
}

export interface NodeToOutletConnection extends BaseConnection {
  type: ConnectionType.NodeToOutlet;
  outNode: string;
  outPort: string;
  outlet: string;
}

export type Connection = NodeToNodeConnection | InletToNodeConnection | NodeToOutletConnection;

export enum ConnectionType {
  NodeToNode = "NODE_TO_NODE",
  InletToNode = "INLET_TO_NODE",
  NodeToOutlet = "NODE_TO_OUTLET",
}

export interface Point {
  x: number;
  y: number;
}

export interface Dimension {
  height: number;
  width: number;
}

export interface Color {
  r: number;
  g: number;
  b: number;
  a: number;
}

export interface Port {
  name: string;
  type: PortType;
}

export enum PortType {
  Invalid = "INVALID",
  Series = "SERIES",
  Shape = "SHAPE",
  Spec = "SPEC",
  Table = "TABLE",
}

export type Table = Record<string, unknown>[];

export type PortValue = Record<string, unknown>[] | Shape | React.ReactElement | null;

export interface Parameter {
  name: string;
  type: ParameterType;
  widget: WidgetType;
  label: string;
  section?: string;
  defaultValue: LiteralValue;
  choices?: Choice[];
  min: number;
  max: number;
  step: number;
}

export enum ParameterType {
  Number = "NUMBER",
  String = "STRING",
  Boolean = "BOOLEAN",
  Point = "POINT",
  Color = "COLOR",
  File = "FILE",
  Choice = "CHOICE",
}

export enum WidgetType {
  Number = "NUMBER",
  String = "STRING",
  Text = "TEXT",
  Boolean = "BOOLEAN",
  Point = "POINT",
  Color = "COLOR",
  File = "FILE",
  Choice = "CHOICE",
}

export type LiteralValue = number | string | boolean | Point | Color;

export interface StaticValue {
  type: "VALUE";
  value: LiteralValue;
}

export interface ExpressionValue {
  type: "EXPRESSION";
  expression: string;
}

export type ParameterValue = StaticValue | ExpressionValue;

export interface Choice {
  name: string;
  label: string;
}

export interface ContextGlobals {
  network: Network;
  values?: Record<string, LiteralValue>;
}

export interface Section {
  name: string;
  collapsed: boolean;
}
