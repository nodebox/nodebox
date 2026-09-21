// The node-authoring API NodeBox Live functions program against (packages/runtime/src/runtime-node.ts):
// `node.numberIn({...})` returns a parameter with `.value` and `.fn(row)`, `node.tableIn` a port,
// `node.onRender` does the work. This implementation is what the bridge hands to the module's
// initializer; values come from the unified evaluator instead of a Live Context.

import { Paint, Shape } from "@ndbx/g";
import { Color } from "../graphics/color";
import { Point } from "../graphics/point";
import { evaluateExpression } from "../runtime/expression";
import { isGShape } from "../graphics/to-g";
import { LiveChoice, LiveParameterType, LivePortType, LiveSection, LiveWidgetType } from "./types";
import { parseChoices, startCase } from "./source-analysis";

export type LiveValue = unknown;

export interface LiveGlobals {
  network: Record<string, unknown>;
  values?: Record<string, unknown>;
  [key: string]: unknown;
}

/** What a parameter knows about its current binding: a literal or an expression. */
export interface ParameterBinding {
  value?: unknown;
  expression?: string;
}

export class LiveParameter {
  node: LiveRuntimeNode;
  name: string;
  type: LiveParameterType;
  widget: LiveWidgetType;
  label: string;
  section?: string;
  defaultValue: unknown;
  choices?: LiveChoice[];
  min = -Infinity;
  max = Infinity;
  step = 1;
  binding: ParameterBinding = {};

  constructor(
    node: LiveRuntimeNode,
    name: string,
    type: LiveParameterType,
    defaultValue?: unknown,
    choices?: LiveChoice[],
  ) {
    this.node = node;
    this.name = name;
    this.type = type;
    this.widget = type as LiveWidgetType;
    this.label = startCase(name);
    this.defaultValue = defaultValue !== undefined ? defaultValue : defaultFor(type);
    this.choices = choices;
  }

  private convert(value: unknown): unknown {
    if (this.type === "COLOR") return toPaint(value);
    if (this.type === "POINT" && value instanceof Point) return { x: value.x, y: value.y };
    return value;
  }

  /** The literal value; an expression that is not `network.*` comes back as its source text, as in Live. */
  get value(): unknown {
    const { value, expression } = this.binding;
    if (expression !== undefined) {
      if (expression.startsWith("network.")) return this.convert(evaluateExpression(expression, this.node.globals));
      return expression;
    }
    if (value === undefined) return this.convert(this.defaultValue);
    return this.convert(value);
  }

  /** A per-row accessor: constants return themselves, expressions evaluate against the row. */
  get fn(): (d: Record<string, unknown>) => unknown {
    const { value, expression } = this.binding;
    if (expression !== undefined) {
      return (d) => this.convert(evaluateExpression(expression, { ...(d ?? {}), ...this.node.globals }));
    }
    const constant = value === undefined ? this.convert(this.defaultValue) : this.convert(value);
    return () => constant;
  }

  get timeDependent(): boolean {
    const e = this.binding.expression;
    return e !== undefined && /(\$FRAME|\$TIME|\$NOW|osc)/.test(e);
  }
}

export class LivePort {
  node: LiveRuntimeNode;
  name: string;
  type: LivePortType;
  label?: string;
  _value: unknown = null;

  constructor(node: LiveRuntimeNode, name: string, type: LivePortType, label?: string) {
    this.node = node;
    this.name = name;
    this.type = type;
    this.label = label;
  }

  get value(): unknown {
    return this._value;
  }

  set(value: unknown): void {
    this._value = value;
  }
}

/** The Live `Context` surface node sources touch: assets, the project and id generation. */
export interface LiveContextLike {
  assetMap: Map<string, unknown>;
  project: unknown;
  warnings: string[];
  generateId(): string;
  lookupItemByName(fqId: string): unknown;
  frame: number;
}

export class LiveRuntimeNode {
  cx: LiveContextLike;
  nodeId: string;
  inputPorts: LivePort[] = [];
  outputPorts: LivePort[] = [];
  parameters: LiveParameter[] = [];
  sections: LiveSection[] = [];
  globals: LiveGlobals = { network: {} };
  message?: string;
  errors: string[] = [];
  private _currentSection?: string;
  _timeDependent = false;

  constructor(cx: LiveContextLike, nodeId: string) {
    this.cx = cx;
    this.nodeId = nodeId;
  }

  get timeDependent(): boolean {
    return this._timeDependent || this.parameters.some((p) => p.timeDependent);
  }

  set timeDependent(v: boolean) {
    this._timeDependent = v;
  }

  private addParameter(p: LiveParameter): LiveParameter {
    p.section = this._currentSection;
    this.parameters.push(p);
    return p;
  }

  numberIn(args: {
    name: string;
    value?: unknown;
    min?: number;
    max?: number;
    step?: number;
    label?: string;
  }): LiveParameter {
    const p = new LiveParameter(this, args.name, "NUMBER", args.value);
    if (args.min !== undefined) p.min = args.min;
    if (args.max !== undefined) p.max = args.max;
    if (args.step !== undefined) p.step = args.step;
    if (args.label) p.label = args.label;
    return this.addParameter(p);
  }

  stringIn(args: {
    name: string;
    value?: unknown;
    widget?: LiveWidgetType;
    choices?: unknown;
    label?: string;
  }): LiveParameter {
    const p = new LiveParameter(this, args.name, "STRING", args.value);
    if (args.widget) p.widget = args.widget;
    if (args.choices) p.choices = parseChoices(args.choices);
    if (args.label) p.label = args.label;
    return this.addParameter(p);
  }

  booleanIn(args: { name: string; value?: unknown; label?: string }): LiveParameter {
    const p = new LiveParameter(this, args.name, "BOOLEAN", args.value);
    if (args.label) p.label = args.label;
    return this.addParameter(p);
  }

  pointIn(args: { name: string; value?: unknown; label?: string }): LiveParameter {
    const p = new LiveParameter(this, args.name, "POINT", args.value);
    if (args.label) p.label = args.label;
    return this.addParameter(p);
  }

  colorIn(args: { name: string; value?: unknown; label?: string }): LiveParameter {
    const p = new LiveParameter(this, args.name, "COLOR", args.value === undefined ? undefined : toPaint(args.value));
    if (args.label) p.label = args.label;
    return this.addParameter(p);
  }

  choiceIn(args: { name: string; value?: unknown; choices?: unknown; label?: string }): LiveParameter {
    const p = new LiveParameter(this, args.name, "CHOICE", args.value, parseChoices(args.choices));
    if (args.label) p.label = args.label;
    return this.addParameter(p);
  }

  fileIn(args: { name: string; value?: unknown; label?: string }): LiveParameter {
    const p = new LiveParameter(this, args.name, "FILE", args.value);
    if (args.label) p.label = args.label;
    return this.addParameter(p);
  }

  private addInput(name: string, type: LivePortType, label?: string): LivePort {
    const port = new LivePort(this, name, type, label);
    this.inputPorts.push(port);
    return port;
  }

  private addOutput(name: string, type: LivePortType, label?: string): LivePort {
    const port = new LivePort(this, name, type, label);
    this.outputPorts.push(port);
    return port;
  }

  tableIn(args: { name: string; label?: string }): LivePort {
    return this.addInput(args.name, "TABLE", args.label);
  }
  shapeIn(args: { name: string; label?: string }): LivePort {
    return this.addInput(args.name, "SHAPE", args.label);
  }
  specIn(args: { name: string; label?: string }): LivePort {
    return this.addInput(args.name, "SPEC", args.label);
  }
  tableOut(args: { name: string; label?: string }): LivePort {
    return this.addOutput(args.name, "TABLE", args.label);
  }
  shapeOut(args: { name: string; label?: string }): LivePort {
    return this.addOutput(args.name, "SHAPE", args.label);
  }
  specOut(args: { name: string; label?: string }): LivePort {
    return this.addOutput(args.name, "SPEC", args.label);
  }

  pushSection(args: { name: string; collapsed?: boolean }): void {
    if (this._currentSection) throw new Error(`Section ${this._currentSection} is not closed`);
    this.sections.push({ name: args.name, collapsed: args.collapsed ?? false });
    this._currentSection = args.name;
  }

  popSection(): void {
    this._currentSection = undefined;
  }

  error(message: string): void {
    this.errors.push(String(message));
  }

  onRender(_cx: LiveContextLike): void | Promise<void> {
    throw new Error("Not implemented");
  }

  onChange(_cx: LiveContextLike, _parameterName: string): void {}
}

function defaultFor(type: LiveParameterType): unknown {
  switch (type) {
    case "NUMBER":
      return 0;
    case "BOOLEAN":
      return false;
    case "POINT":
      return { x: 0, y: 0 };
    case "COLOR":
      return Paint.black();
    default:
      return "";
  }
}

/** Colors reach Live nodes as @ndbx/g paints, whatever form they arrive in. */
export function toPaint(value: unknown): unknown {
  if (value instanceof Color) return Paint.solid(value.r, value.g, value.b, value.a);
  if (value instanceof Paint) return value;
  if (typeof value === "string") {
    try {
      return Paint.parse(value);
    } catch {
      return value;
    }
  }
  if (value && typeof value === "object" && "r" in (value as object)) {
    const c = value as { r: number; g: number; b: number; a?: number };
    return Paint.solid(c.r, c.g, c.b, c.a ?? 1);
  }
  return value;
}

export function isShape(value: unknown): value is Shape {
  return isGShape(value);
}
