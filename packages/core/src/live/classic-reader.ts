// Read a classic NodeBox Live project into the core model.
//
// The two models line up almost one to one, because the core evaluator inherited NodeBox 3's list
// matching and the classic runtime's `cycleMap` is the same rule: the longest input decides how
// many times a node runs, shorter inputs cycle, a `takesList` parameter gets the whole list, an
// empty input means no invocation, and a `returnsList` function's results are concatenated.
//
// Classic parameters are positional and untyped at run time: values reach the function exactly as
// they were stored. Their declared types are kept as "classic:<type>" so that none of NodeBox 3's
// Java type conversions apply to them, while the editor can still show the right widget.

import { Color } from "../graphics/color";
import { Point } from "../graphics/point";
import { NodeRepository } from "../model/library";
import { addChild, createNetworkNode, createRootNode, extendNode, getInput } from "../model/node";
import { CLASSIC_TYPE_PREFIX, createPort } from "../model/port";
import { Library, MenuItem, Node, Port, PortValue, PortWidget } from "../model/types";
import { ClassicFunction, ClassicNode, ClassicParameter, ClassicProject, ClassicPortType } from "./classic-types";

export interface ClassicReadOptions {
  /** The namespace the project's functions live in, e.g. "g". Defaults to the project's own id. */
  namespace?: string;
  /** Where prototypes from other projects ("b5bulge.fx") are resolved. */
  repository?: NodeRepository;
}

export interface ClassicReadResult {
  library: Library;
  warnings: string[];
}

/** The widget the editor shows for a classic parameter type. */
const WIDGETS: Record<string, PortWidget> = {
  int: "int",
  float: "float",
  ratio: "float",
  string: "string",
  html: "text",
  boolean: "toggle",
  point: "point",
  pointRatio: "point",
  color: "color",
  file: "file",
  image: "image",
  shape: "none",
  object: "none",
  data: "data",
  list: "none",
  geometry: "none",
};

export function classicPortType(type: ClassicPortType | undefined): string {
  return `${CLASSIC_TYPE_PREFIX}${type || "object"}`;
}

function menuItems(parameter: ClassicParameter): MenuItem[] {
  return (parameter.choices ?? []).map((choice) => {
    if (typeof choice === "string") return { key: choice, label: choice };
    if (Array.isArray(choice)) return { key: choice[0], label: choice[1] ?? choice[0] };
    return { key: choice.key, label: choice.label };
  });
}

function parameterToPort(parameter: ClassicParameter): Port {
  const type = classicPortType(parameter.type);
  const items = menuItems(parameter);
  return createPort(parameter.name, type, {
    // A parameter that takes a list is handed the whole list, exactly like a list-range port.
    range: parameter.takesList ? "list" : "value",
    widget: items.length > 0 ? "menu" : (WIDGETS[parameter.type] ?? "none"),
    label: parameter.label ?? "",
    description: parameter.description ?? "",
    value: (parameter.value ?? null) as PortValue,
    // Bounds are a hint for the editor's controls; classic never clamped a value while rendering.
    min: isEnforced(parameter.enforceMinimum) ? parameter.minimum : undefined,
    max: isEnforced(parameter.enforceMaximum) ? parameter.maximum : undefined,
    menuItems: items,
  });
}

function isEnforced(flag: boolean | string | undefined): boolean {
  return flag === true || flag === "true";
}

function outputTypeOf(fn: ClassicFunction): ClassicPortType | undefined {
  return fn.outputType ?? fn.returnType;
}

/** A code function becomes a prototype node: positional inputs and one output. */
function codeFunctionToNode(fn: ClassicFunction, namespace: string): Node {
  const node = createRootNode();
  node.name = fn.name;
  node.prototype = "core.node";
  node.function = `${namespace}/${fn.name}`;
  node.category = fn.category ?? "";
  node.description = fn.ref ?? "";
  node.image = "";
  node.inputs = (fn.parameters ?? []).map(parameterToPort);
  node.outputType = classicPortType(outputTypeOf(fn));
  node.outputRange = fn.returnsList ? "list" : "value";
  node.outputs = [createPort("output", node.outputType, { range: node.outputRange })];
  node.meta = { classicExample: fn.example, classicAsync: fn.async, classicStateful: fn.stateful };
  return node;
}

/** A network function becomes a network node; its parameters become published ports (inlets). */
function networkFunctionToNode(fn: ClassicFunction, namespace: string): Node {
  const node = extendNode(createNetworkNode(), "core.network", fn.name);
  node.function = `${namespace}/${fn.name}`;
  node.category = fn.category ?? "";
  node.description = fn.ref ?? "";
  node.outputType = classicPortType(outputTypeOf(fn));
  node.outputRange = fn.returnsList ? "list" : "value";
  node.meta = { classicBackground: fn.background, classicHeight: fn.height, classicStateful: fn.stateful };
  return node;
}

function applyNodeOverrides(instance: Node, child: ClassicNode): void {
  if (child.outputType !== undefined) instance.outputType = classicPortType(child.outputType);
  if (child.returnsList !== undefined) instance.outputRange = child.returnsList ? "list" : "value";
  for (const output of instance.outputs) {
    output.type = instance.outputType;
    output.range = instance.outputRange;
  }
  if (child.masterList) instance.masterInput = child.masterList;
}

function fillNetwork(
  node: Node,
  fn: ClassicFunction,
  resolve: (fullName: string) => Node | undefined,
  warnings: string[],
): void {
  for (const child of fn.nodes ?? []) {
    const prototype = resolve(child.fn);
    let instance: Node;
    if (prototype) {
      instance = extendNode(prototype, child.fn, child.name);
    } else {
      warnings.push(`Function ${child.fn} could not be found (node ${child.name}).`);
      instance = createRootNode();
      instance.name = child.name;
      instance.prototype = child.fn;
      instance.function = child.fn.replace(".", "/");
      instance.meta.missingPrototype = true;
    }
    instance.position = new Point(child.x, child.y);
    for (const [name, value] of Object.entries(child.values ?? {})) {
      const port = getInput(instance, name);
      if (port) port.value = value as PortValue;
      else warnings.push(`Node ${child.name} has a value for unknown parameter ${name}.`);
    }
    applyNodeOverrides(instance, child);
    addChild(node, instance);
  }

  // The network's own parameters are inlets. A connection from an inlet publishes the child port
  // it feeds; unlike NodeBox 3, one inlet can feed several children.
  for (const parameter of fn.parameters ?? []) {
    const port = parameterToPort(parameter);
    const targets = (fn.connections ?? [])
      .filter((c) => c.inlet === parameter.name)
      .map((c) => `${c.input}.${c.parameter}`);
    if (targets.length > 0) {
      port.childReference = targets[0];
      if (targets.length > 1) port.childReferences = targets.slice(1);
    }
    node.inputs.push(port);
  }

  for (const connection of fn.connections ?? []) {
    if (connection.inlet) continue;
    if (!connection.output) continue;
    node.connections.push({
      outputNode: connection.output,
      inputNode: connection.input,
      inputPort: connection.parameter,
    });
  }

  for (const sticky of fn.stickies ?? []) {
    node.stickies.push({
      id: `${node.name}-sticky-${node.stickies.length}`,
      x: sticky.x,
      y: sticky.y,
      width: sticky.width ?? 200,
      height: sticky.height ?? 100,
      text: sticky.text ?? "",
      fontSize: 12,
      backgroundColor: Color.WHITE,
      fontColor: Color.BLACK,
    });
  }

  if (fn.renderedNode) node.renderedChild = fn.renderedNode;
  // Only the type follows the rendered child; whether the network's results concatenate is its own
  // `returnsList`, exactly as for a code function.
  const rendered = node.children.find((c) => c.name === node.renderedChild);
  if (rendered && outputTypeOf(fn) === undefined) node.outputType = rendered.outputType;
  node.outputs = [
    createPort("output", node.outputType, {
      range: node.outputRange,
      childReference: node.renderedChild ? `${node.renderedChild}.output` : undefined,
    }),
  ];
}

/**
 * Read a classic project. The library is named after the project's namespace, so that the
 * "namespace.name" identifiers in its nodes resolve through a NodeRepository unchanged.
 */
export function parseClassicProject(project: ClassicProject, options: ClassicReadOptions = {}): ClassicReadResult {
  const namespace = options.namespace ?? project.id ?? "self";
  const repository = options.repository;
  const warnings: string[] = [];
  const root = extendNode(createNetworkNode(), "core.network", "root");
  root.function = "core/zero";
  const library: Library = {
    name: namespace,
    root,
    functionLinks: [],
    devices: [],
    properties: {},
    dependencies: project.dependencies ?? {},
    assets: Object.fromEntries(
      Object.entries(project.assets ?? {}).map(([name, hash]) => [name, typeof hash === "string" ? hash : hash.hash]),
    ),
    title: project.title,
    color: project.color,
    sourceFormat: "classic",
    meta: {},
  };

  // Prototypes first, so a network can use functions defined later in the file.
  const own = new Map<string, Node>();
  for (const fn of project.functions ?? []) {
    const node = fn.type === "network" ? networkFunctionToNode(fn, namespace) : codeFunctionToNode(fn, namespace);
    own.set(fn.name, node);
    addChild(root, node);
  }
  const resolve = (fullName: string): Node | undefined => {
    const dot = fullName.indexOf(".");
    if (dot < 0) return own.get(fullName);
    const ns = fullName.slice(0, dot);
    const name = fullName.slice(dot + 1);
    if (ns === namespace) return own.get(name);
    return repository?.getNode(fullName);
  };
  // A network is instantiated by copying it, so fill the networks it uses before it. A cycle
  // (two networks that use each other) falls back to the order they appear in the file.
  const networks = (project.functions ?? []).filter((fn) => fn.type === "network");
  const localNetworkNames = new Set(networks.map((fn) => fn.name));
  const usedNetworks = (fn: ClassicFunction): string[] =>
    (fn.nodes ?? [])
      .map((child) => (child.fn.startsWith(`${namespace}.`) ? child.fn.slice(namespace.length + 1) : child.fn))
      .filter((name) => localNetworkNames.has(name) && name !== fn.name);
  const pending = [...networks];
  const filled = new Set<string>();
  while (pending.length > 0) {
    const ready = pending.filter((fn) => usedNetworks(fn).every((name) => filled.has(name)));
    const batch = ready.length > 0 ? ready : [pending[0]];
    for (const fn of batch) {
      fillNetwork(own.get(fn.name)!, fn, resolve, warnings);
      filled.add(fn.name);
      pending.splice(pending.indexOf(fn), 1);
    }
  }
  const main = (project.functions ?? []).find((fn) => fn.name === "main") ?? (project.functions ?? [])[0];
  if (main) root.renderedChild = main.name;
  return { library, warnings };
}

/** The names of the classic projects this one depends on, as "userId/projectId". */
export function classicDependencies(project: ClassicProject): string[] {
  return Object.keys(project.dependencies ?? {});
}
