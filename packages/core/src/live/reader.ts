// Read a NodeBox Live project.json into the unified model.
//
// A Live project becomes a library whose root holds one child per item: a function item becomes a
// prototype node with ports derived from its source; a network item becomes a network node whose
// children are instances of "userId/projectId/Item" prototypes. Inlets become published ports,
// outlets named outputs, parameters plain inputs, stickies stickies. Live ids are kept in `meta`
// so the writer can produce the same file again.

import { Color } from "../graphics/color";
import { Point } from "../graphics/point";
import { NodeRepository } from "../model/library";
import { addChild, createNetworkNode, createRootNode, extendNode, getInput, setInputValue } from "../model/node";
import { createPort } from "../model/port";
import { FunctionLink, Library, MenuItem, Node, Port, PortRange, PortType, PortWidget } from "../model/types";
import { analyzeFunctionSource } from "./source-analysis";
import {
  LIVE_FORMAT_VERSION,
  LiveColor,
  LiveFunctionItem,
  LiveInlet,
  LiveNetwork,
  LiveNode,
  LiveOutlet,
  LiveParameter,
  LiveParameterValue,
  LivePoint,
  LivePortType,
  LiveProject,
  LiveSticky,
} from "./types";

export interface LiveReadOptions {
  /** "userId/projectId" of this project; "self/self" ids resolve to it. Defaults to "self/self". */
  projectKey?: string;
  /** Where dependency prototypes ("core/g/Rect") are resolved. */
  repository?: NodeRepository;
  /** Upgrade older format versions (default true). */
  upgrade?: boolean;
}

export interface LiveReadResult {
  library: Library;
  warnings: string[];
}

export class LiveLoadError extends Error {}

/** The function namespace Live function items are published under: "live/<userId>/<projectId>/<Item>". */
export const LIVE_FUNCTION_NAMESPACE = "live";

export function liveFunctionId(projectKey: string, itemName: string): string {
  return `${LIVE_FUNCTION_NAMESPACE}/${projectKey}/${itemName}`;
}

export function parseLiveProject(json: string | LiveProject, options: LiveReadOptions = {}): LiveReadResult {
  const raw: LiveProject = typeof json === "string" ? JSON.parse(json) : json;
  const warnings: string[] = [];
  const project = options.upgrade === false ? raw : upgradeLiveProject(raw, warnings);
  const projectKey = options.projectKey ?? "self/self";
  const repository = options.repository ?? new NodeRepository();

  const root = extendNode(createNetworkNode(), "core.network", "root");
  const library: Library = {
    name: projectKey,
    root,
    properties: {},
    devices: [],
    functionLinks: [],
    dependencies: { ...(project.dependencies ?? {}) },
    assets: { ...(project.assets ?? {}) },
    title: project.title,
    description: project.description,
    color:
      typeof project.color === "string" ? project.color : project.color ? toColor(project.color).toString() : undefined,
    sourceFormat: "live",
    meta: {
      projectKey,
      liveId: project.id,
      liveFormatVersion: project.formatVersion ?? LIVE_FORMAT_VERSION,
      liveScope: project.scope,
      liveIsPublished: project.isPublished,
      livePublishDate: project.publishDate,
      liveGallery: project.__gallery,
      positionUnits: "pixels",
    },
  };
  // Two passes: prototypes first, so networks can instantiate items defined later in the file.
  const itemNodes = new Map<string, Node>();
  for (const item of project.items ?? []) {
    if (item.type === "FUNCTION") {
      const node = functionItemToNode(item, projectKey);
      library.functionLinks.push(functionLink(item, projectKey));
      itemNodes.set(item.name, node);
      addChild(root, node);
    } else if (item.type === "NETWORK") {
      const node = extendNode(createNetworkNode(), "core.network", item.name);
      node.meta.liveId = item.id;
      itemNodes.set(item.name, node);
      addChild(root, node);
    }
  }
  const resolvePrototype = (fn: string): Node | undefined => {
    const [userId, projectId, ...rest] = fn.split("/");
    const itemName = rest.join("/");
    if ((userId === "self" && projectId === "self") || `${userId}/${projectId}` === projectKey)
      return itemNodes.get(itemName);
    return repository.getNode(fn);
  };
  // Instances copy their prototype's children, so a network is filled only after the networks it
  // uses; a cycle (a network calling itself through another) falls back to file order.
  const networkNames = new Set((project.items ?? []).filter((i) => i.type === "NETWORK").map((i) => i.name));
  const localNetworkRefs = (item: LiveNetwork): string[] =>
    (item.children ?? [])
      .filter((c): c is LiveNode => c.type === "NODE")
      .map((c) => c.fn)
      .filter((fn) => fn.startsWith("self/self/") || fn.startsWith(`${projectKey}/`))
      .map((fn) => fn.split("/").slice(2).join("/"))
      .filter((name) => networkNames.has(name) && name !== item.name);
  const pending = (project.items ?? []).filter((i): i is LiveNetwork => i.type === "NETWORK");
  const filled = new Set<string>();
  while (pending.length > 0) {
    const ready = pending.filter((item) => localNetworkRefs(item).every((name) => filled.has(name)));
    const batch = ready.length > 0 ? ready : [pending[0]];
    for (const item of batch) {
      fillNetwork(itemNodes.get(item.name)!, item, resolvePrototype, warnings);
      filled.add(item.name);
      pending.splice(pending.indexOf(item), 1);
    }
  }
  const mainItem = (project.items ?? []).find((i) => i.type === "NETWORK");
  if (mainItem) root.renderedChild = mainItem.name;
  const main = mainItem && mainItem.type === "NETWORK" ? mainItem : undefined;
  if (main?.width) library.properties.canvasWidth = String(main.width);
  if (main?.height) library.properties.canvasHeight = String(main.height);
  return { library, warnings };
}

function functionLink(item: LiveFunctionItem, projectKey: string): FunctionLink {
  return {
    language: "javascript",
    href: `module:${projectKey}/${item.name}`,
    namespace: LIVE_FUNCTION_NAMESPACE,
    source: item.source,
  };
}

/** Turn a function item into a prototype node with NodeBox ports. */
export function functionItemToNode(item: LiveFunctionItem, projectKey: string): Node {
  const signature = analyzeFunctionSource(item.source);
  const node = createRootNode();
  node.name = item.name;
  node.prototype = "core.node";
  node.function = liveFunctionId(projectKey, item.name);
  node.category = item.category || signature.category || "";
  node.description = item.description || signature.description || "";
  node.image = "";
  node.inputs = [];
  for (const port of signature.inputPorts) node.inputs.push(livePortToPort(port.name, port.type, port.label));
  for (const parameter of signature.parameters) node.inputs.push(parameterToPort(parameter));
  node.outputs = signature.outputPorts.map((port) => livePortToPort(port.name, port.type, port.label));
  if (node.outputs.length > 0) {
    node.outputType = node.outputs[0].type;
    node.outputRange = node.outputs[0].range;
  }
  node.meta.liveId = item.id;
  node.meta.liveSource = item.source;
  node.meta.liveSections = signature.sections;
  node.meta.liveWidth = item.width;
  node.meta.liveHeight = item.height;
  node.meta.liveBackground = item.background;
  return node;
}

export function livePortType(type: LivePortType): { type: PortType; range: PortRange } {
  switch (type) {
    case "TABLE":
      return { type: "table", range: "list" };
    case "SERIES":
      return { type: "list", range: "list" };
    case "SHAPE":
      return { type: "shape", range: "value" };
    case "SPEC":
      return { type: "spec", range: "value" };
    default:
      return { type: "list", range: "list" };
  }
}

function livePortToPort(name: string, type: LivePortType, label?: string): Port {
  const { type: portType, range } = livePortType(type);
  return createPort(name, portType, { range, label: label ?? "", widget: "none" });
}

function parameterToPort(parameter: LiveParameter): Port {
  let type: PortType;
  let widget: PortWidget;
  switch (parameter.type) {
    case "NUMBER":
      type = "float";
      widget = "float";
      break;
    case "BOOLEAN":
      type = "boolean";
      widget = "toggle";
      break;
    case "POINT":
      type = "point";
      widget = "point";
      break;
    case "COLOR":
      type = "color";
      widget = "color";
      break;
    case "FILE":
      type = "string";
      widget = "file";
      break;
    case "CHOICE":
      type = "string";
      widget = "menu";
      break;
    default:
      type = "string";
      widget = parameter.widget === "TEXT" ? "text" : "string";
  }
  if (parameter.choices && parameter.choices.length > 0) widget = "menu";
  const port = createPort(parameter.name, type, {
    widget,
    label: parameter.label ?? "",
    section: parameter.section,
    menuItems: (parameter.choices ?? []).map((c): MenuItem => ({ key: c.name, label: c.label })),
  });
  if (Number.isFinite(parameter.min)) port.min = parameter.min;
  if (Number.isFinite(parameter.max)) port.max = parameter.max;
  if (parameter.step !== undefined && parameter.step !== 1) port.step = parameter.step;
  port.value = literalToValue(type, parameter.defaultValue);
  return port;
}

export function toColor(c: LiveColor | string): Color {
  if (typeof c === "string") return Color.parse(c.startsWith("#") ? c : `#${c}`);
  return new Color(c.r, c.g, c.b, c.a ?? 1);
}

function literalToValue(type: PortType, value: unknown): Port["value"] {
  if (value === null || value === undefined) return createPort("x", type).value;
  switch (type) {
    case "float":
    case "int":
      return typeof value === "number" ? value : Number(value) || 0;
    case "boolean":
      return Boolean(value);
    case "point":
      if (typeof value === "object" && value !== null && "x" in value) {
        const p = value as LivePoint;
        return new Point(Number(p.x), Number(p.y));
      }
      return Point.ZERO;
    case "color":
      if (typeof value === "object" && value !== null && "r" in value) return toColor(value as LiveColor);
      if (typeof value === "string") {
        // Live colors may be CSS names or hex; keep names in the meta by falling back to black.
        try {
          return toColor(value);
        } catch {
          return Color.BLACK;
        }
      }
      return Color.BLACK;
    default:
      return typeof value === "string" ? value : String(value);
  }
}

function fillNetwork(
  node: Node,
  item: LiveNetwork,
  resolvePrototype: (fn: string) => Node | undefined,
  warnings: string[],
): void {
  node.category = item.category ?? "";
  node.description = item.description ?? "";
  node.meta.liveId = item.id;
  node.meta.liveCanvasSize = item.canvasSize;
  node.meta.livePadding = item.padding;
  node.meta.liveWidth = item.width;
  node.meta.liveHeight = item.height;
  node.meta.liveBackground = item.background;
  node.meta.liveSections = item.sections ?? [];
  node.meta.liveGallery = item.__gallery;
  const idToName = new Map<string, string>();
  const inlets = new Map<string, LiveInlet>();
  const outlets = new Map<string, LiveOutlet>();
  for (const child of item.children ?? []) {
    if (child.type === "NODE") {
      const proto = resolvePrototype(child.fn);
      let instance: Node;
      if (proto) {
        instance = extendNode(proto, child.fn, child.name);
      } else {
        warnings.push(`Function ${child.fn} could not be found (node ${child.name}).`);
        instance = createRootNode();
        instance.name = child.name;
        instance.prototype = child.fn;
        instance.function = child.fn.includes("/")
          ? liveFunctionId(child.fn.split("/").slice(0, 2).join("/"), child.fn.split("/").slice(2).join("/"))
          : child.fn;
        instance.meta.missingPrototype = true;
      }
      instance.position = new Point(child.x, child.y);
      instance.meta.liveId = child.id;
      applyValues(instance, child.values ?? {});
      idToName.set(child.id, child.name);
      addChild(node, instance);
    } else if (child.type === "STICKY") {
      node.stickies.push(stickyFrom(child));
    } else if (child.type === "INLET") {
      inlets.set(child.id, child);
    } else if (child.type === "OUTLET") {
      outlets.set(child.id, child);
    }
  }
  const nameOf = (id: string) => idToName.get(id);
  // Network parameters are plain inputs on the network node, reachable as `network.<name>`.
  for (const parameter of item.parameters ?? []) {
    node.inputs.push(parameterToPort(parameter));
  }
  for (const c of item.connections ?? []) {
    if (c.type === "NODE_TO_NODE") {
      const outNode = nameOf(c.outNode);
      const inNode = nameOf(c.inNode);
      if (!outNode || !inNode) {
        warnings.push(`Connection refers to an unknown node (${c.outNode} -> ${c.inNode}).`);
        continue;
      }
      node.connections = node.connections.filter((x) => !(x.inputNode === inNode && x.inputPort === c.inPort));
      node.connections.push({ outputNode: outNode, outputPort: c.outPort, inputNode: inNode, inputPort: c.inPort });
    } else if (c.type === "INLET_TO_NODE") {
      const inlet = inlets.get(c.inlet) ?? [...inlets.values()].find((i) => i.portName === c.inlet);
      const inNode = nameOf(c.inNode);
      if (!inlet || !inNode) {
        warnings.push(`Inlet connection refers to an unknown inlet or node (${c.inlet} -> ${c.inNode}).`);
        continue;
      }
      const existing = node.inputs.find((p) => p.name === inlet.portName);
      const reference = `${inNode}.${c.inPort}`;
      if (existing && existing.childReference) {
        // One inlet feeding several ports: keep the extra targets so the writer can restore them.
        const extra = (node.meta.liveExtraInletTargets as Record<string, string[]>) ?? {};
        (extra[inlet.portName] ??= []).push(reference);
        node.meta.liveExtraInletTargets = extra;
        continue;
      }
      const { type, range } = livePortType(inlet.portType);
      const child = node.children.find((n) => n.name === inNode);
      const childPort = child ? getInput(child, c.inPort) : undefined;
      const port = createPort(inlet.portName, childPort?.type ?? type, {
        range: childPort?.range ?? range,
        widget: "none",
        childReference: reference,
      });
      if (childPort) port.value = childPort.value;
      if (existing) Object.assign(existing, port);
      else node.inputs.push(port);
    } else if (c.type === "NODE_TO_OUTLET") {
      const outlet = outlets.get(c.outlet) ?? [...outlets.values()].find((o) => o.portName === c.outlet);
      const outNode = nameOf(c.outNode);
      if (!outlet || !outNode) {
        warnings.push(`Outlet connection refers to an unknown outlet or node (${c.outNode} -> ${c.outlet}).`);
        continue;
      }
      const { type, range } = livePortType(outlet.portType);
      const existing = node.outputs.find((p) => p.name === outlet.portName);
      const port = createPort(outlet.portName, type, {
        range,
        widget: "none",
        childReference: `${outNode}.${c.outPort}`,
      });
      if (existing) Object.assign(existing, port);
      else node.outputs.push(port);
    }
  }
  // Inlets and outlets without connections still exist as ports.
  for (const inlet of inlets.values()) {
    if (!node.inputs.some((p) => p.name === inlet.portName)) {
      const { type, range } = livePortType(inlet.portType);
      node.inputs.push(createPort(inlet.portName, type, { range, widget: "none" }));
    }
  }
  // Ports converted from NodeBox 3 keep their exact type and range (a Live SHAPE port cannot say
  // "list of geometry").
  for (const port of item.inputPorts ?? []) {
    const original = port.__ndbx;
    const target = original && node.inputs.find((p) => p.name === port.name);
    if (original && target) {
      target.type = original.type as PortType;
      target.range = original.range as PortRange;
    }
  }
  for (const outlet of outlets.values()) {
    if (!node.outputs.some((p) => p.name === outlet.portName)) {
      const { type, range } = livePortType(outlet.portType);
      node.outputs.push(createPort(outlet.portName, type, { range, widget: "none" }));
    }
  }
  node.meta.liveInlets = [...inlets.values()].map((i) => ({
    id: i.id,
    x: i.x,
    y: i.y,
    portName: i.portName,
    portType: i.portType,
  }));
  node.meta.liveOutlets = [...outlets.values()].map((o) => ({
    id: o.id,
    x: o.x,
    y: o.y,
    portName: o.portName,
    portType: o.portType,
  }));
  if (node.outputs.length > 0) {
    node.outputType = node.outputs[0].type;
    node.outputRange = node.outputs[0].range;
  }
  if (item.__ndbx) {
    node.outputType = item.__ndbx.outputType as PortType;
    node.outputRange = item.__ndbx.outputRange as PortRange;
    for (const output of node.outputs) {
      output.type = node.outputType;
      output.range = node.outputRange;
    }
  }
  if (item.renderedNode) {
    const rendered = nameOf(item.renderedNode);
    if (rendered) node.renderedChild = rendered;
  }
}

function applyValues(node: Node, values: Record<string, LiveParameterValue>): void {
  for (const [name, value] of Object.entries(values)) {
    let port = getInput(node, name);
    if (!port) {
      // A value for a parameter the source no longer declares; keep it so nothing is lost on save.
      port = createPort(name, "string", { widget: "none", description: "(not declared by the function source)" });
      node.inputs.push(port);
    }
    if (value.type === "VALUE") {
      port.expression = undefined;
      setInputValue(node, name, literalToValue(port.type, value.value));
    } else if (value.type === "EXPRESSION") {
      port.expression = value.expression;
    }
  }
}

function stickyFrom(s: LiveSticky) {
  return {
    id: s.id,
    x: s.x,
    y: s.y,
    width: s.width,
    height: s.height,
    text: s.text,
    backgroundColor: toColor(s.backgroundColor),
    fontColor: toColor(s.fontColor),
    fontSize: s.fontSize,
  };
}

/** The NodeBox Live format upgrades (packages/runtime/src/upgrades.ts). */
export function upgradeLiveProject(project: LiveProject, warnings: string[] = []): LiveProject {
  let version = project.formatVersion ?? 1;
  if (version > LIVE_FORMAT_VERSION)
    throw new LiveLoadError("Invalid project format version. You might want to retry later on.");
  const p = structuredClone(project);
  const renameNodes = (from: string, to: string, renameName?: (name: string) => string) => {
    for (const item of p.items ?? []) {
      if (item.type !== "NETWORK") continue;
      for (const child of item.children ?? []) {
        if (child.type === "NODE" && child.fn === from) {
          child.fn = to;
          if (renameName) child.name = renameName(child.name);
        }
      }
    }
  };
  while (version < LIVE_FORMAT_VERSION) {
    if (version === 1) {
      renameNodes("core/g/load-csv", "core/g/import-data", (n) => n.replace(/load-csv/i, "import-data"));
      warnings.push("Renamed load-csv nodes to import-data.");
    } else if (version === 2) {
      for (const item of p.items ?? [])
        if (item.type === "NETWORK" && !Array.isArray(item.outputPorts)) item.outputPorts = [];
    } else if (version === 3) {
      for (const item of p.items ?? []) {
        if (item.type !== "NETWORK") continue;
        for (const child of item.children ?? []) {
          if (child.type !== "NODE" || !child.fn.startsWith("core/g/")) continue;
          const slug = child.fn.slice("core/g/".length);
          if (!slug.includes("-") && slug === slug.toLowerCase()) {
            const title = slugToTitle(slug);
            child.name = child.name.replace(slug, title);
            child.fn = `core/g/${title}`;
          } else if (slug.includes("-")) {
            const title = slugToTitle(slug);
            child.name = child.name.replace(slug, title);
            child.fn = `core/g/${title}`;
          }
        }
      }
    }
    version++;
  }
  p.formatVersion = LIVE_FORMAT_VERSION;
  return p;
}

export function slugToTitle(slug: string): string {
  return slug.replace(/-/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

/** Build a library (e.g. "core/g") from a set of function sources keyed by slug. */
export function libraryFromFunctionSources(
  projectKey: string,
  sources: Record<string, string>,
  title = projectKey,
): Library {
  const items: LiveFunctionItem[] = Object.keys(sources)
    .sort((a, b) => a.localeCompare(b))
    .map((slug, i) => ({ type: "FUNCTION", id: `0:${i + 1}`, name: slugToTitle(slug), source: sources[slug] }));
  const project: LiveProject = { formatVersion: LIVE_FORMAT_VERSION, title, dependencies: {}, assets: {}, items };
  return parseLiveProject(project, { projectKey, upgrade: false }).library;
}
