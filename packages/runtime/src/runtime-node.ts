import {
  Item,
  Port as IPort,
  PortType,
  Parameter as IParameter,
  ParameterType,
  ParameterValue,
  Color,
  Choice,
  PortValue,
  LiteralValue,
  WidgetType,
  ContextGlobals,
  Section,
} from "./types";
import { startCase } from "./string-utils";
import Context from "./context";
import { evaluateExpression } from "./expression";
import { Paint } from "@ndbx/g";

type ParameterChoices = string[] | string[][];

export function parseChoices(choices: string[] | string[][]): Choice[] {
  if (choices.length === 0) return [];
  if (typeof choices[0] === "string") {
    return (choices as string[]).map((name) => ({ name, label: startCase(name) }));
  } else {
    return (choices as string[][]).map(([name, label]) => ({ name, label }));
  }
}

export function defaultValueForType(type: ParameterType): LiteralValue {
  if (type === ParameterType.Number) {
    return 0;
  } else if (type === ParameterType.String || type === ParameterType.File) {
    return "";
  } else if (type === ParameterType.Boolean) {
    return false;
  } else if (type === ParameterType.Point) {
    return { x: 0, y: 0 };
  } else if (type === ParameterType.Color) {
    return { r: 0, g: 0, b: 0, a: 1 };
  } else {
    throw new Error(`Invalid parameter type: ${type}`);
  }
}

export function defaultWidgetForType(type: ParameterType): WidgetType {
  return type as unknown as WidgetType;
}

export function createExpressionContext(_cx: Context, globals: ContextGlobals): ContextGlobals {
  const networkProxy = new Proxy(globals.network, {
    get(target, prop: string) {
      if (target.hasOwnProperty(prop)) {
        return (target as Record<string, any>)[prop];
      }
      const param = target.parameters.find((p) => p.name === prop);
      if (param) {
        if (globals.values && globals.values.hasOwnProperty(prop)) {
          return globals.values[prop];
        } else {
          return param.defaultValue;
        }
      }
    },
  });
  return { network: networkProxy, values: globals.values };
}

class Parameter implements IParameter {
  node: RuntimeNode;
  type: ParameterType;
  widget: WidgetType;
  name: string;
  label: string;
  section?: string;
  defaultValue: LiteralValue;
  choices?: Choice[];
  min: number;
  max: number;
  step: number;

  constructor(
    node: RuntimeNode,
    name: string,
    type: ParameterType,
    defaultValue: LiteralValue | undefined = undefined,
    choices?: Choice[],
  ) {
    this.node = node;
    this.type = type;
    this.widget = defaultWidgetForType(type);
    this.name = name;
    this.label = startCase(name);
    this.defaultValue = typeof defaultValue !== "undefined" ? defaultValue : defaultValueForType(type);
    this.choices = choices;
    this.min = -Infinity;
    this.max = Infinity;
    this.step = 1;
    this.section = undefined;
  }

  get value() {
    const nodeValue = this.node.values?.[this.name];
    if (nodeValue !== undefined) {
      if (nodeValue.type === "VALUE") {
        if (this.type === ParameterType.Color) {
          return Paint.parse(nodeValue.value as unknown as Color);
        }
        return nodeValue.value;
      } else {
        // HACK This is to make network expressions work, without breaking the example plot projects
        if (nodeValue.expression.startsWith("network.")) {
          const ctx = this.node.globals;
          let result = evaluateExpression(nodeValue.expression, ctx as ContextGlobals);
          if (this.type === ParameterType.Color) {
            result = Paint.parse(result as unknown as Color);
          }
          return result;
        } else {
          return nodeValue.expression;
        }
      }
    } else {
      return this.defaultValue;
    }
  }

  get fn() {
    return (d: Record<string, any>) => {
      const nodeValue = this.node.values?.[this.name];
      if (nodeValue === undefined) return this.defaultValue;
      if (nodeValue.type === "VALUE") {
        if (this.type === ParameterType.Color) {
          return Paint.parse(nodeValue.value as unknown as Color);
        }
        return nodeValue.value;
      } else {
        const ctx = { ...d, ...this.node.globals };
        let result = evaluateExpression(nodeValue.expression, ctx as ContextGlobals);
        if (this.type === ParameterType.Color) {
          result = Paint.parse(result as unknown as Color);
        }
        return result;
      }
    };
  }

  get timeDependent() {
    const nodeValue = this.node.values?.[this.name];
    if (nodeValue === undefined) return false;
    return nodeValue.type === "EXPRESSION" && /(\$FRAME|\$TIME|\$NOW|osc)/.test(nodeValue.expression);
  }
}

class Port implements IPort {
  node: RuntimeNode;
  type: PortType;
  name: string;
  _value: PortValue;

  constructor(node: RuntimeNode, name: string, type: PortType) {
    this.node = node;
    this.name = name;
    this.type = type;
    this._value = null;
  }

  get value(): PortValue {
    const portValue = this.node.cx.portValues.get(`${this.node.nodeId}/${this.name}`);
    if (portValue !== undefined) return portValue;
    return this._value;
  }

  set(value: PortValue) {
    this._value = value;
  }
}

export default class RuntimeNode {
  cx: Context;
  nodeId: string;
  nodeFn: Item;
  inputPorts: Port[];
  outputPorts: Port[];
  parameters: Parameter[];
  values: Record<string, ParameterValue>;
  _timeDependent: boolean;
  dirty: boolean;
  globals: ContextGlobals | null;
  sections: Section[];
  _currentSection?: string;
  message?: string;

  constructor(cx: Context, nodeId: string, nodeFn: Item) {
    this.cx = cx;
    this.nodeId = nodeId;
    this.nodeFn = nodeFn;
    this.inputPorts = [];
    this.outputPorts = [];
    this.parameters = [];
    this.values = {};
    this._timeDependent = false;
    this.dirty = true;
    this.globals = null;
    this.sections = [];
    this._currentSection = undefined;
  }

  get timeDependent() {
    if (this._timeDependent) {
      return true;
    }
    return this.parameters.some((p) => p.timeDependent);
  }

  set timeDependent(value) {
    this._timeDependent = value;
  }

  numberIn({
    name,
    value,
    min,
    max,
    step,
  }: {
    name: string;
    value: LiteralValue | undefined;
    min?: number;
    max?: number;
    step?: number;
  }): Parameter {
    const parameter = new Parameter(this, name, ParameterType.Number, value);
    parameter.section = this._currentSection;
    if (min !== undefined) {
      parameter.min = min;
    }
    if (max !== undefined) {
      parameter.max = max;
    }
    if (step !== undefined) {
      parameter.step = step;
    }
    this.parameters.push(parameter);
    return parameter;
  }

  stringIn({
    name,
    value,
    widget,
    choices,
  }: {
    name: string;
    value: LiteralValue | undefined;
    widget?: WidgetType;
    choices?: ParameterChoices;
  }): Parameter {
    const parameter = new Parameter(this, name, ParameterType.String, value);
    parameter.section = this._currentSection;
    if (widget) {
      parameter.widget = widget;
    }
    if (choices) {
      let realChoices: Choice[] = parseChoices(choices);
      parameter.choices = realChoices;
    }
    this.parameters.push(parameter);
    return parameter;
  }

  booleanIn({ name, value }: { name: string; value: LiteralValue | undefined }): Parameter {
    const parameter = new Parameter(this, name, ParameterType.Boolean, value);
    parameter.section = this._currentSection;
    this.parameters.push(parameter);
    return parameter;
  }

  colorIn({ name, value }: { name: string; value: LiteralValue | undefined }): Parameter {
    if (typeof value === "string") {
      value = Paint.parse(value) as unknown as Color;
    }
    const parameter = new Parameter(this, name, ParameterType.Color, value);
    parameter.section = this._currentSection;
    this.parameters.push(parameter);
    return parameter;
  }

  choiceIn({ name, value, choices }: { name: string; value: LiteralValue | undefined; choices: Choice[] }): Parameter {
    const parameter = new Parameter(this, name, ParameterType.Choice, value, choices);
    parameter.section = this._currentSection;
    this.parameters.push(parameter);
    return parameter;
  }

  fileIn({ name, value }: { name: string; value: LiteralValue | undefined }): Parameter {
    const parameter = new Parameter(this, name, ParameterType.File, value);
    parameter.section = this._currentSection;
    this.parameters.push(parameter);
    return parameter;
  }

  tableIn({ name }: { name: string }): Port {
    const port = new Port(this, name, PortType.Table);
    this.inputPorts.push(port);
    return port;
  }

  shapeIn({ name }: { name: string }): Port {
    const port = new Port(this, name, PortType.Shape);
    this.inputPorts.push(port);
    return port;
  }

  specIn({ name }: { name: string }): Port {
    const port = new Port(this, name, PortType.Spec);
    this.inputPorts.push(port);
    return port;
  }

  tableOut({ name }: { name: string }): Port {
    const port = new Port(this, name, PortType.Table);
    this.outputPorts.push(port);
    return port;
  }

  shapeOut({ name }: { name: string }): Port {
    const port = new Port(this, name, PortType.Shape);
    this.outputPorts.push(port);
    return port;
  }

  specOut({ name }: { name: string }): Port {
    const port = new Port(this, name, PortType.Spec);
    this.outputPorts.push(port);
    return port;
  }

  pushSection({ name, collapsed = false }: { name: string; collapsed: boolean }): void {
    if (this._currentSection) {
      throw new Error(`Section ${this._currentSection} is not closed`);
    }
    this.sections.push({ name, collapsed });
    this._currentSection = name;
  }

  popSection() {
    this._currentSection = undefined;
  }

  onRender(_cx: Context) {
    throw new Error("Not implemented");
  }

  onChange(_cx: Context, _parameterName: string) {}
}
