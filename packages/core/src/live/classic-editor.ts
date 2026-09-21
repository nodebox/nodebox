// Classic NodeBox Live projects, as the editor's own project format.
//
// The editor works with `items`: networks whose children are nodes, inlets, outlets and stickies,
// and function items with parameters and ports. A classic project is a flat list of functions, and
// the two line up closely enough to convert directly from the JSON, without resolving prototypes
// first: a classic parameter becomes both an input port (it can be connected) and a parameter (it
// has a widget), which is what the classic editor showed as well.
//
// The conversion is for display and editing only; rendering goes through `openClassicProject`.

import { classicPortType } from "./classic-reader";
import { ClassicFunction, ClassicNode, ClassicParameter, ClassicProject } from "./classic-types";
import {
  LIVE_FORMAT_VERSION,
  LiveConnection,
  LiveFunctionItem,
  LiveItem,
  LiveNetwork,
  LiveNetworkItem,
  LiveParameter,
  LiveParameterValue,
  LivePort,
  LivePortType,
  LiveProject,
} from "./types";

/** The classic viewer drew on a 1000 by 1000 canvas centred on the origin. */
const CLASSIC_CANVAS = 1000;

export interface ClassicEditorOptions {
  /** "userId/projectId" of the project itself; its own functions become "self/self/<name>". */
  key?: string;
}

/** A classic item carries the function it came from, so an editor can tell it apart. */
export interface ClassicItemMeta {
  __classic: {
    /** The namespace the function lives in at run time, e.g. "g". */
    namespace: string;
    /** The classic declarations the conversion cannot express: async, stateful, examples. */
    async?: boolean;
    stateful?: boolean;
    example?: string;
    outputType?: string;
    returnsList?: boolean;
  };
}

/**
 * Convert a classic project into the editor's project format. Node references ("g.rect") resolve
 * through the project's dependencies, so that a node in the editor points at the same item the
 * core engine renders.
 */
export function classicProjectToLiveProject(project: ClassicProject, options: ClassicEditorOptions = {}): LiveProject {
  const namespace = project.id ?? namespaceOf(options.key) ?? "self";
  const dependencies = project.dependencies ?? {};
  // "g.rect" is the g namespace, which is the second half of a dependency key such as "core/g".
  const byNamespace = new Map<string, string>();
  for (const key of Object.keys(dependencies)) {
    const slash = key.indexOf("/");
    byNamespace.set(slash < 0 ? key : key.slice(slash + 1), key);
  }
  const own = new Set((project.functions ?? []).map((fn) => fn.name));

  const resolve = (fullName: string): string => {
    const dot = fullName.indexOf(".");
    if (dot < 0) return `self/self/${fullName}`;
    const ns = fullName.slice(0, dot);
    const name = fullName.slice(dot + 1);
    if (ns === namespace || (own.has(name) && !byNamespace.has(ns))) return `self/self/${name}`;
    const key = byNamespace.get(ns);
    return key ? `${key}/${name}` : `${ns}/${ns}/${name}`;
  };

  let nextId = 1;
  const freshId = () => `c:${nextId++}`;
  const items: LiveItem[] = (project.functions ?? []).map((fn) =>
    fn.type === "network" ? networkItem(fn, freshId, resolve, namespace) : functionItem(fn, freshId(), namespace),
  );
  // The editor opens on the first item, and classic rendered "main".
  const mainIndex = items.findIndex((item) => item.name === "main");
  if (mainIndex > 0) items.unshift(items.splice(mainIndex, 1)[0]);

  return {
    id: namespace,
    formatVersion: LIVE_FORMAT_VERSION,
    title: project.title ?? namespace,
    color: typeof project.color === "string" ? project.color : undefined,
    dependencies,
    assets: Object.fromEntries(
      Object.entries(project.assets ?? {}).map(([name, hash]) => [name, typeof hash === "string" ? hash : hash.hash]),
    ),
    items,
  };
}

function namespaceOf(key: string | undefined): string | undefined {
  if (!key) return undefined;
  const slash = key.indexOf("/");
  return slash < 0 ? key : key.slice(slash + 1);
}

function functionItem(fn: ClassicFunction, id: string, namespace: string): LiveFunctionItem & ClassicItemMeta {
  return {
    type: "FUNCTION",
    id,
    name: fn.name,
    category: fn.category ?? "",
    description: fn.ref ?? "",
    inputPorts: (fn.parameters ?? []).map(parameterPort),
    outputPorts: [{ name: "output", type: outputPortType(fn), label: "Output" }],
    parameters: (fn.parameters ?? []).filter(hasWidget).map(parameterWidget),
    sections: [],
    source: fn.source ?? "",
    __classic: {
      namespace,
      async: fn.async,
      stateful: fn.stateful,
      example: fn.example,
      outputType: fn.outputType ?? fn.returnType,
      returnsList: fn.returnsList,
    },
  };
}

function networkItem(
  fn: ClassicFunction,
  freshId: () => string,
  resolve: (fullName: string) => string,
  namespace: string,
): LiveNetwork & ClassicItemMeta {
  const children: LiveNetworkItem[] = [];
  const connections: LiveConnection[] = [];
  const ids = new Map<string, string>();

  for (const node of fn.nodes ?? []) {
    const id = freshId();
    ids.set(node.name, id);
    children.push({
      type: "NODE",
      id,
      name: node.name,
      x: node.x,
      y: node.y,
      fn: resolve(node.fn),
      values: nodeValues(node),
    });
  }

  // A network's parameters are its inlets. One classic inlet can feed several child ports, which
  // is simply several connections from the same inlet.
  const inputPorts: LivePort[] = [];
  let inletY = 0;
  for (const parameter of fn.parameters ?? []) {
    const inletId = freshId();
    const type = portType(parameter);
    children.push({ type: "INLET", id: inletId, x: -120, y: (inletY += 40), portName: parameter.name, portType: type });
    inputPorts.push({ name: parameter.name, type, label: labelOf(parameter) });
    for (const connection of fn.connections ?? []) {
      if (connection.inlet !== parameter.name) continue;
      const inNode = ids.get(connection.input);
      if (inNode) connections.push({ type: "INLET_TO_NODE", inlet: inletId, inNode, inPort: connection.parameter });
    }
  }

  for (const connection of fn.connections ?? []) {
    if (connection.inlet || !connection.output) continue;
    const outNode = ids.get(connection.output);
    const inNode = ids.get(connection.input);
    if (outNode && inNode) {
      connections.push({ type: "NODE_TO_NODE", outNode, outPort: "output", inNode, inPort: connection.parameter });
    }
  }

  const outputPorts: LivePort[] = [];
  const renderedNode = fn.renderedNode ? (ids.get(fn.renderedNode) ?? null) : null;
  if (renderedNode) {
    const outletId = freshId();
    const type = outputPortType(fn);
    children.push({ type: "OUTLET", id: outletId, x: 400, y: 40, portName: "output", portType: type });
    outputPorts.push({ name: "output", type, label: "Output" });
    connections.push({ type: "NODE_TO_OUTLET", outNode: renderedNode, outPort: "output", outlet: outletId });
  }

  for (const sticky of fn.stickies ?? []) {
    children.push({
      type: "STICKY",
      id: freshId(),
      x: sticky.x,
      y: sticky.y,
      width: sticky.width ?? 200,
      height: sticky.height ?? 100,
      backgroundColor: { r: 1, g: 1, b: 0.85, a: 0.9 },
      text: sticky.text ?? "",
      fontSize: 12,
      fontColor: { r: 0.1, g: 0.1, b: 0.1, a: 1 },
    });
  }

  return {
    type: "NETWORK",
    id: freshId(),
    name: fn.name,
    category: fn.category ?? "",
    description: fn.ref ?? "",
    canvasSize: "fixed",
    padding: 10,
    width: CLASSIC_CANVAS,
    height: fn.height ?? CLASSIC_CANVAS,
    background: { r: 1, g: 1, b: 1, a: 1 },
    children,
    connections,
    renderedNode,
    inputPorts,
    outputPorts,
    parameters: (fn.parameters ?? []).filter(hasWidget).map(parameterWidget),
    sections: [],
    // The classic viewer translated to the middle of the canvas before drawing.
    __ndbx: {
      outputType: classicPortType(fn.outputType ?? fn.returnType),
      outputRange: fn.returnsList ? "list" : "value",
      origin: "center",
    },
    __classic: {
      namespace,
      stateful: fn.stateful,
      outputType: fn.outputType ?? fn.returnType,
      returnsList: fn.returnsList,
    },
  };
}

function nodeValues(node: ClassicNode): Record<string, LiveParameterValue> | undefined {
  const entries = Object.entries(node.values ?? {});
  if (entries.length === 0) return undefined;
  return Object.fromEntries(entries.map(([name, value]) => [name, { type: "VALUE", value } as LiveParameterValue]));
}

/** Every classic parameter is connectable, so every one of them is also an input port. */
function parameterPort(parameter: ClassicParameter): LivePort {
  return {
    name: parameter.name,
    type: portType(parameter),
    label: labelOf(parameter),
    __ndbx: { type: classicPortType(parameter.type), range: parameter.takesList ? "list" : "value" },
  };
}

function portType(parameter: ClassicParameter): LivePortType {
  if (parameter.type === "shape" || parameter.type === "geometry") return "SHAPE";
  if (parameter.takesList || parameter.type === "list" || parameter.type === "data") return "TABLE";
  return "SERIES";
}

function outputPortType(fn: ClassicFunction): LivePortType {
  const type = fn.outputType ?? fn.returnType;
  if (type === "shape" || type === "geometry") return "SHAPE";
  if (fn.returnsList || type === "list" || type === "data") return "TABLE";
  return "SERIES";
}

/** A shape or an opaque object has no control; classic showed those as a port only. */
function hasWidget(parameter: ClassicParameter): boolean {
  return !["shape", "geometry", "object", "list", "data"].includes(parameter.type);
}

function parameterWidget(parameter: ClassicParameter): LiveParameter {
  const choices = (parameter.choices ?? []).map((choice) =>
    typeof choice === "string"
      ? { name: choice, label: choice }
      : Array.isArray(choice)
        ? { name: choice[0], label: choice[1] ?? choice[0] }
        : { name: choice.key, label: choice.label },
  );
  const type: LiveParameter["type"] =
    choices.length > 0
      ? "CHOICE"
      : parameter.type === "int" || parameter.type === "float" || parameter.type === "ratio"
        ? "NUMBER"
        : parameter.type === "boolean"
          ? "BOOLEAN"
          : parameter.type === "point" || parameter.type === "pointRatio"
            ? "POINT"
            : parameter.type === "color"
              ? "COLOR"
              : parameter.type === "file" || parameter.type === "image"
                ? "FILE"
                : "STRING";
  return {
    name: parameter.name,
    type,
    widget: parameter.type === "html" ? "TEXT" : type === "CHOICE" ? "CHOICE" : (type as LiveParameter["widget"]),
    label: labelOf(parameter),
    defaultValue: (parameter.value ?? "") as LiveParameter["defaultValue"],
    choices: choices.length > 0 ? choices : undefined,
    // Classic never clamped while rendering; the bounds only steer the control.
    min: isEnforced(parameter.enforceMinimum) ? (parameter.minimum ?? -Infinity) : -Infinity,
    max: isEnforced(parameter.enforceMaximum) ? (parameter.maximum ?? Infinity) : Infinity,
    step: parameter.type === "int" ? 1 : parameter.type === "ratio" ? 0.01 : 1,
  };
}

function isEnforced(flag: boolean | string | undefined): boolean {
  return flag === true || flag === "true";
}

function labelOf(parameter: ClassicParameter): string {
  if (parameter.label) return parameter.label;
  const spaced = parameter.name.replace(/([a-z0-9])([A-Z])/g, "$1 $2");
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}
