// The project format of the original NodeBox Live (the "nodeboxdead" branch): a project is a list
// of functions, each either JavaScript code with declared parameters or a network of nodes that
// call other functions. Parameters are positional, and the runtime does the list matching.

/** int, float, ratio, string, html, boolean, point, pointRatio, color, shape, file, image, object. */
export type ClassicPortType = string;

export interface ClassicParameter {
  name: string;
  type: ClassicPortType;
  /** The default value, as plain JSON: a point is a `{x, y}`, a color a CSS string. */
  value?: unknown;
  /** The parameter receives the whole list instead of one element per invocation. */
  takesList?: boolean;
  minimum?: number;
  maximum?: number;
  /** Some projects store these as the strings "true" and "false". */
  enforceMinimum?: boolean | string;
  enforceMaximum?: boolean | string;
  /** A choice is a key, a [key, label] pair, or an object with both. */
  choices?: (string | [string, string] | { key: string; label: string })[];
  label?: string;
  description?: string;
}

export interface ClassicNode {
  name: string;
  x: number;
  y: number;
  /** The function this node calls, as "namespace.name", e.g. "g.rect". */
  fn: string;
  values?: Record<string, unknown>;
  /** The parameter whose list length decides how many times the node runs. */
  masterList?: string;
  /** Overrides of the function's own declarations. */
  outputType?: ClassicPortType;
  returnsList?: boolean;
  stateful?: boolean;
  cached?: boolean;
}

export interface ClassicConnection {
  /** The source node, for a node-to-node connection. */
  output?: string;
  /** The source inlet, for a connection from one of the network's own parameters. */
  inlet?: string;
  input: string;
  parameter: string;
}

export interface ClassicSticky {
  x: number;
  y: number;
  width?: number;
  height?: number;
  text?: string;
}

export interface ClassicFunction {
  name: string;
  type: "code" | "network";
  outputType?: ClassicPortType;
  /** An older spelling of outputType. */
  returnType?: ClassicPortType;
  returnsList?: boolean;
  async?: boolean;
  stateful?: boolean;
  category?: string;
  /** Reference documentation, in Markdown. */
  ref?: string;
  example?: string;
  /** The source of a code function. It assigns the function into its project's namespace. */
  source?: string;
  parameters?: ClassicParameter[];
  /** Networks only. */
  nodes?: ClassicNode[];
  connections?: ClassicConnection[];
  renderedNode?: string;
  stickies?: ClassicSticky[];
  background?: string;
  height?: number;
}

export interface ClassicProject {
  /** The namespace of the project's functions, e.g. "g" for core/g. */
  id?: string;
  title?: string;
  color?: string;
  dependencies?: Record<string, string>;
  assets?: Record<string, string | { hash: string }>;
  functions: ClassicFunction[];
}

/** Whether this JSON is a classic project rather than one of the later `items` formats. */
export function isClassicProject(project: unknown): project is ClassicProject {
  if (project === null || typeof project !== "object") return false;
  const candidate = project as { functions?: unknown; items?: unknown };
  return Array.isArray(candidate.functions) && candidate.items === undefined;
}
