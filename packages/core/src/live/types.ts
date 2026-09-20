// The NodeBox Live project format, as stored in project.json (packages/runtime/src/types.ts).

export interface LiveProject {
  id?: string;
  formatVersion?: number;
  title: string;
  description?: string;
  color?: string | LiveColor;
  dependencies: Record<string, string>;
  assets: Record<string, string>;
  items: LiveItem[];
  scope?: string;
  isPublished?: boolean;
  publishDate?: string;
  __gallery?: unknown;
}

export type LiveItem = LiveNetwork | LiveFunctionItem;

export interface LiveColor {
  r: number;
  g: number;
  b: number;
  a: number;
}

export interface LivePoint {
  x: number;
  y: number;
}

export interface LiveNetwork {
  type: "NETWORK";
  id: string;
  name: string;
  category?: string;
  description?: string;
  canvasSize?: string;
  padding?: number;
  width?: number;
  height?: number;
  background?: LiveColor;
  children: LiveNetworkItem[];
  connections: LiveConnection[];
  renderedNode?: string | null;
  inputPorts?: LivePort[];
  outputPorts?: LivePort[];
  parameters?: LiveParameter[];
  sections?: LiveSection[];
  __gallery?: unknown;
}

export type LiveNetworkItem = LiveNode | LiveInlet | LiveOutlet | LiveSticky;

export interface LiveNode {
  type: "NODE";
  id: string;
  name: string;
  x: number;
  y: number;
  fn: string;
  values?: Record<string, LiveParameterValue>;
}

export interface LiveInlet {
  type: "INLET";
  id: string;
  x: number;
  y: number;
  portName: string;
  portType: LivePortType;
}

export interface LiveOutlet {
  type: "OUTLET";
  id: string;
  x: number;
  y: number;
  portName: string;
  portType: LivePortType;
}

export interface LiveSticky {
  type: "STICKY";
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
  backgroundColor: LiveColor;
  text: string;
  fontSize: number;
  fontColor: LiveColor;
}

export interface LiveFunctionItem {
  type: "FUNCTION";
  id: string;
  name: string;
  category?: string;
  description?: string;
  width?: number;
  height?: number;
  background?: LiveColor;
  inputPorts?: LivePort[];
  outputPorts?: LivePort[];
  parameters?: LiveParameter[];
  sections?: LiveSection[];
  source: string;
}

export type LiveConnection =
  | { type: "NODE_TO_NODE"; outNode: string; outPort: string; inNode: string; inPort: string }
  | { type: "INLET_TO_NODE"; inlet: string; inNode: string; inPort: string }
  | { type: "NODE_TO_OUTLET"; outNode: string; outPort: string; outlet: string };

export type LivePortType = "INVALID" | "SERIES" | "SHAPE" | "SPEC" | "TABLE";

export interface LivePort {
  name: string;
  type: LivePortType;
  label?: string;
}

export type LiveParameterType = "NUMBER" | "STRING" | "BOOLEAN" | "POINT" | "COLOR" | "FILE" | "CHOICE";
export type LiveWidgetType = "NUMBER" | "STRING" | "TEXT" | "BOOLEAN" | "POINT" | "COLOR" | "FILE" | "CHOICE";

export interface LiveParameter {
  name: string;
  type: LiveParameterType;
  widget: LiveWidgetType;
  label: string;
  section?: string;
  defaultValue: LiveLiteralValue;
  choices?: LiveChoice[];
  min: number;
  max: number;
  step: number;
}

export type LiveLiteralValue = number | string | boolean | LivePoint | LiveColor;

export type LiveParameterValue = { type: "VALUE"; value: LiveLiteralValue } | { type: "EXPRESSION"; expression: string };

export interface LiveChoice {
  name: string;
  label: string;
}

export interface LiveSection {
  name: string;
  collapsed: boolean;
}

export const LIVE_FORMAT_VERSION = 4;
