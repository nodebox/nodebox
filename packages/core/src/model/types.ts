// The unified document model.
//
// It is NodeBox 3's NodeLibrary model (prototypes, typed ports, value/list ranges, published ports,
// rendered children) extended with what NodeBox Live needs on top: several named outputs per node,
// expression values on ports, stickies, and JavaScript function items. Both file formats map onto it
// without loss; the .ndbx and Live JSON readers and writers live in ../ndbx and ../live.

import { Color } from "../graphics/color";
import { Point } from "../graphics/point";

/** The standard port types can hold a literal value; every other type (list, geometry, data, …) carries null. */
export const STANDARD_TYPES = ["int", "float", "string", "boolean", "point", "color"] as const;
export type StandardType = (typeof STANDARD_TYPES)[number];

export type PortType =
  StandardType | "list" | "geometry" | "context" | "state" | "data" | "table" | "shape" | "spec" | string;

export type PortRange = "value" | "list";

export type PortWidget =
  | "none"
  | "angle"
  | "color"
  | "data"
  | "file"
  | "float"
  | "font"
  | "gradient"
  | "image"
  | "int"
  | "menu"
  | "seed"
  | "string"
  | "text"
  | "password"
  | "toggle"
  | "point";

export type LiteralValue = number | string | boolean | Point | Color;
/** Classic NodeBox Live keeps raw JSON in its parameters (a point is a plain `{x, y}`). */
export type ClassicValue = { readonly [key: string]: unknown };
export type PortValue = LiteralValue | ClassicValue | null;

export interface MenuItem {
  key: string;
  label: string;
}

export interface Port {
  name: string;
  type: PortType;
  label: string;
  description: string;
  widget: PortWidget;
  range: PortRange;
  /** The literal value; null for non-standard types. Integers are stored as JavaScript numbers. */
  value: PortValue;
  /** An expression that replaces the literal value at render time (NodeBox Live parameters). */
  expression?: string;
  /** "childNode.childPort" when this port forwards to a child of a network (a published port). */
  childReference?: string;
  /**
   * The further children a published port feeds, as "childNode.childPort". NodeBox 3 publishes a
   * port to exactly one child; a classic NodeBox Live inlet can feed several at once.
   */
  childReferences?: string[];
  min?: number;
  max?: number;
  step?: number;
  menuItems: MenuItem[];
  /** NodeBox Live groups parameters in collapsible sections. */
  section?: string;
}

export interface Connection {
  outputNode: string;
  /** The output port of the source node. Undefined means the node's single implicit output. */
  outputPort?: string;
  inputNode: string;
  inputPort: string;
}

export interface Sticky {
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
  text: string;
  backgroundColor: Color;
  fontColor: Color;
  fontSize: number;
}

export interface Node {
  name: string;
  /** Prototype id ("corevector.rect", "core.network", a sibling name) as written in the file, or null for the root node. */
  prototype: string | null;
  comment: string;
  category: string;
  description: string;
  image: string;
  /** Function id, "namespace/name". */
  function: string;
  position: Point;
  inputs: Port[];
  outputType: PortType;
  outputRange: PortRange;
  /**
   * Named outputs. NodeBox 3 nodes have none listed: their single output is described by outputType and
   * outputRange. NodeBox Live nodes list theirs; the first is the primary one.
   */
  outputs: Port[];
  isNetwork: boolean;
  children: Node[];
  /** The child whose result is the network's result, or "" for none. */
  renderedChild: string;
  connections: Connection[];
  handle: string;
  alwaysRendered: boolean;
  stickies: Sticky[];
  /** Format-specific extras that must survive a round trip (Live ids, canvas settings, …). */
  meta: Record<string, unknown>;
  /**
   * The input whose length decides how many times the node runs, instead of the longest input
   * (classic NodeBox Live's `masterList`). A value that is not a list means a single run.
   */
  masterInput?: string;
}

export interface Device {
  name: string;
  type: "osc" | "audioplayer" | "audioinput" | string;
  properties: Record<string, string>;
}

export type FunctionLanguage = "java" | "python" | "clojure" | "javascript";

/** A `<link rel="functions" href="python:foo.py"/>`, or a NodeBox Live function item held inline. */
export interface FunctionLink {
  language: FunctionLanguage;
  /** A path relative to the document for python/clojure/java, or "module:<name>" for inline JavaScript. */
  href: string;
  /** The namespace the functions are published under ("pyvector" for pyvector.py). */
  namespace: string;
  /** Inline source, for JavaScript function items stored in the document. */
  source?: string;
}

export interface Library {
  /** The library name: the .ndbx base name, or the Live project id. */
  name: string;
  uuid?: string;
  /** The file or URL the library was loaded from, used to resolve relative paths. */
  file?: string;
  root: Node;
  properties: Record<string, string>;
  devices: Device[];
  functionLinks: FunctionLink[];
  /** NodeBox Live dependencies: "core/g" -> "dev". */
  dependencies: Record<string, string>;
  /** NodeBox Live assets: file name -> blob id. */
  assets: Record<string, string>;
  title?: string;
  description?: string;
  color?: string;
  /** Which format the document came from; the writer of that format keeps its extras. */
  sourceFormat: "ndbx" | "live" | "classic" | "memory";
  meta: Record<string, unknown>;
}

export const DEFAULT_VALUES: Record<StandardType, LiteralValue> = {
  int: 0,
  float: 0,
  string: "",
  boolean: false,
  point: Point.ZERO,
  color: Color.BLACK,
};

export function isStandardType(type: string): type is StandardType {
  return (STANDARD_TYPES as readonly string[]).includes(type);
}
