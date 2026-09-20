// NodeBox 3 documents inside the NodeBox Live editor.
//
// The editor works with Live projects whose nodes reference items in the project or in dependency
// projects. To show a .ndbx document, its built-in prototypes are exposed as "native" items of
// dependency projects named "nodebox/<library>" (nodebox/corevector/rect, nodebox/math/add, …),
// with their parameters and ports spelled out instead of derived from JavaScript source, and every
// subnetwork is hoisted into a network item of its own. The core evaluator renders such projects;
// the Live engine cannot, because native items have no source.

import { Color } from "../graphics/color";
import { Point } from "../graphics/point";
import { NodeRepository } from "../model/library";
import { getChild, uniqueName } from "../model/node";
import { isPublishedPort, portLabel } from "../model/port";
import { Library, Node, Port } from "../model/types";
import { builtinNodeRepository } from "../libraries";
import {
  LIVE_FORMAT_VERSION,
  LiveConnection,
  LiveFunctionItem,
  LiveItem,
  LiveNetworkItem,
  LiveParameter,
  LiveParameterValue,
  LivePort,
  LivePortType,
  LiveProject,
} from "./types";

export const NATIVE_USER_ID = "nodebox";

/** A function item whose behaviour lives in the core rather than in `source`. */
export interface NativeFunctionItem extends LiveFunctionItem {
  native: true;
  /** The "library.node" prototype id in the built-in node repository. */
  prototype: string;
}

export function isNativeItem(item: unknown): item is NativeFunctionItem {
  return typeof item === "object" && item !== null && (item as { native?: unknown }).native === true;
}

/** The dependency key of a built-in library: "nodebox/corevector". */
export function nativeProjectKey(libraryName: string): string {
  return `${NATIVE_USER_ID}/${libraryName}`;
}

/** Live projects for the built-in NodeBox 3 libraries, keyed by "nodebox/<library>". */
export function nativeLibraryProjects(
  repository: NodeRepository = builtinNodeRepository(),
): Record<string, LiveProject> {
  const projects: Record<string, LiveProject> = {};
  for (const library of repository.getLibraries()) {
    if (library.sourceFormat === "live") continue;
    const items: LiveItem[] = [];
    let id = 1;
    for (const node of library.root.children) {
      if (node.meta.importCoreNode) continue;
      items.push(nativeItem(node, `${library.name}.${node.name}`, `0:${id++}`));
    }
    projects[nativeProjectKey(library.name)] = {
      id: library.name,
      formatVersion: LIVE_FORMAT_VERSION,
      title: library.name,
      description: library.root.description,
      dependencies: {},
      assets: {},
      items,
    };
  }
  return projects;
}

function nativeItem(node: Node, prototype: string, id: string): NativeFunctionItem {
  const inputPorts: LivePort[] = [];
  const parameters: LiveParameter[] = [];
  for (const port of node.inputs) {
    if (isDataPort(port)) inputPorts.push({ name: port.name, type: livePortTypeFor(port), label: portLabel(port) });
    else parameters.push(portToLiveParameter(port));
  }
  const outputPorts: LivePort[] =
    node.outputs.length > 0
      ? node.outputs.map((p) => ({ name: p.name, type: livePortTypeFor(p), label: portLabel(p) }))
      : [
          {
            name: "output",
            type: node.outputRange === "list" ? "TABLE" : outputTypeFor(node.outputType),
            label: "Output",
          },
        ];
  return {
    type: "FUNCTION",
    native: true,
    prototype,
    id,
    name: node.name,
    category: node.category,
    description: node.description,
    inputPorts,
    outputPorts,
    parameters,
    sections: [],
    source: "",
  };
}

/** Ports that carry data between nodes rather than a literal: everything but the standard types with a widget. */
function isDataPort(port: Port): boolean {
  return !["int", "float", "string", "boolean", "point", "color"].includes(port.type) || port.widget === "none";
}

function livePortTypeFor(port: Port): LivePortType {
  if (port.type === "geometry" || port.type === "shape") return "SHAPE";
  if (port.type === "spec") return "SPEC";
  if (port.range === "list" || port.type === "list" || port.type === "data" || port.type === "table") return "TABLE";
  return "SERIES";
}

function outputTypeFor(type: string): LivePortType {
  if (type === "geometry" || type === "shape") return "SHAPE";
  if (type === "spec") return "SPEC";
  return "SERIES";
}

export function portToLiveParameter(port: Port): LiveParameter {
  const type =
    port.type === "float" || port.type === "int"
      ? "NUMBER"
      : port.type === "boolean"
        ? "BOOLEAN"
        : port.type === "point"
          ? "POINT"
          : port.type === "color"
            ? "COLOR"
            : port.widget === "file" || port.widget === "image"
              ? "FILE"
              : port.menuItems.length > 0 || port.widget === "menu"
                ? "CHOICE"
                : "STRING";
  const widget = port.widget === "text" ? "TEXT" : type === "CHOICE" ? "CHOICE" : (type as LiveParameter["widget"]);
  return {
    name: port.name,
    type,
    widget,
    label: portLabel(port),
    section: port.section,
    defaultValue: toLiveLiteral(port),
    choices: port.menuItems.length > 0 ? port.menuItems.map((m) => ({ name: m.key, label: m.label })) : undefined,
    min: port.min ?? -Infinity,
    max: port.max ?? Infinity,
    step: port.step ?? (port.type === "int" ? 1 : 1),
  };
}

function toLiveLiteral(port: Port): LiveParameter["defaultValue"] {
  const v = port.value;
  if (v instanceof Color) return { r: v.r, g: v.g, b: v.b, a: v.a };
  if (v instanceof Point) return { x: v.x, y: v.y };
  if (v === null || v === undefined) return "";
  return v;
}

export interface ConvertOptions {
  /** The repository the document's prototypes come from (default: the built-in libraries). */
  repository?: NodeRepository;
  /** Node positions in .ndbx are grid units; the editor uses pixels. Default 48 px per unit. */
  gridSize?: number;
}

/**
 * Convert a library loaded from .ndbx into a Live project the editor can show. Nodes with
 * "library.node" prototypes reference the native items; subnetworks become network items.
 */
export function libraryToLiveProject(library: Library, options: ConvertOptions = {}): LiveProject {
  const repository = options.repository ?? builtinNodeRepository();
  const gridSize = options.gridSize ?? 48;
  const items: LiveItem[] = [];
  const itemNames = new Set<string>();
  let nextId = 1;
  const freshId = () => `0:${nextId++}`;
  const dependencies: Record<string, string> = {};

  const freeName = (name: string): string => {
    const base = name || "network";
    const itemName = itemNames.has(base) ? uniqueName(base.replace(/\d+$/, "") || base, itemNames) : base;
    itemNames.add(itemName);
    return itemName;
  };

  const hoist = (network: Node, name: string): string => {
    const itemName = freeName(name);
    const children: LiveNetworkItem[] = [];
    const connections: LiveConnection[] = [];
    const ids = new Map<string, string>();
    for (const child of network.children) {
      const id = freshId();
      ids.set(child.name, id);
      let fn: string;
      if (child.isNetwork) {
        fn = `self/self/${hoist(child, child.name)}`;
      } else if (child.prototype && child.prototype.includes(".") && repository.getNode(child.prototype)) {
        const [lib, nodeName] = [
          child.prototype.slice(0, child.prototype.indexOf(".")),
          child.prototype.slice(child.prototype.indexOf(".") + 1),
        ];
        dependencies[nativeProjectKey(lib)] = "dev";
        fn = `${nativeProjectKey(lib)}/${nodeName}`;
      } else if (child.prototype && child.prototype.includes("/")) {
        fn = child.prototype;
      } else {
        // A node defined in the document itself (a prototype sibling or a custom function node).
        fn = `self/self/${hoistFunction(child)}`;
      }
      children.push({
        type: "NODE",
        id,
        name: child.name,
        x: Math.round(child.position.x * gridSize),
        y: Math.round(child.position.y * gridSize),
        fn,
        values: valuesOf(child),
      });
    }
    for (const c of network.connections) {
      const outNode = ids.get(c.outputNode);
      const inNode = ids.get(c.inputNode);
      if (!outNode || !inNode) continue;
      connections.push({
        type: "NODE_TO_NODE",
        outNode,
        outPort: c.outputPort ?? "output",
        inNode,
        inPort: c.inputPort,
      });
    }
    const inputPorts: LivePort[] = [];
    const parameters: LiveParameter[] = [];
    let inletY = 0;
    for (const port of network.inputs) {
      if (isPublishedPort(port)) {
        const inletId = freshId();
        const type = livePortTypeFor(port);
        children.push({
          type: "INLET",
          id: inletId,
          x: -gridSize * 3,
          y: (inletY += gridSize),
          portName: port.name,
          portType: type,
        });
        inputPorts.push({
          name: port.name,
          type,
          label: portLabel(port),
          __ndbx: { type: port.type, range: port.range },
        });
        const [childName, childPort] = port.childReference!.split(".");
        const inNode = ids.get(childName);
        if (inNode) connections.push({ type: "INLET_TO_NODE", inlet: inletId, inNode, inPort: childPort });
      } else {
        parameters.push(portToLiveParameter(port));
      }
    }
    const outputPorts: LivePort[] = [];
    const renderedNode = network.renderedChild ? (ids.get(network.renderedChild) ?? null) : null;
    if (renderedNode) {
      const outletId = freshId();
      const child = getChild(network, network.renderedChild)!;
      const type: LivePortType =
        network.outputRange === "list" && network.outputType !== "geometry"
          ? "TABLE"
          : outputTypeFor(network.outputType || child.outputType);
      children.push({ type: "OUTLET", id: outletId, x: gridSize * 8, y: gridSize, portName: "output", portType: type });
      outputPorts.push({ name: "output", type, label: "Output" });
      connections.push({ type: "NODE_TO_OUTLET", outNode: renderedNode, outPort: "output", outlet: outletId });
    }
    for (const sticky of network.stickies) {
      children.push({
        type: "STICKY",
        id: sticky.id || freshId(),
        x: sticky.x,
        y: sticky.y,
        width: sticky.width,
        height: sticky.height,
        backgroundColor: colorOf(sticky.backgroundColor),
        text: sticky.text,
        fontSize: sticky.fontSize,
        fontColor: colorOf(sticky.fontColor),
      });
    }
    if (network.comment) {
      children.push({
        type: "STICKY",
        id: freshId(),
        x: 0,
        y: -gridSize * 2,
        width: 320,
        height: 40,
        backgroundColor: { r: 1, g: 1, b: 0.85, a: 0.9 },
        text: network.comment,
        fontSize: 12,
        fontColor: { r: 0.1, g: 0.1, b: 0.1, a: 1 },
      });
    }
    items.push({
      type: "NETWORK",
      id: freshId(),
      name: itemName,
      category: network.category ?? "",
      description: network.description ?? "",
      canvasSize: "fixed",
      padding: 10,
      width: Number(library.properties.canvasWidth) || 1000,
      height: Number(library.properties.canvasHeight) || 1000,
      background: { r: 1, g: 1, b: 1, a: 1 },
      children,
      connections,
      renderedNode,
      inputPorts,
      outputPorts,
      parameters,
      sections: [],
      // NodeBox 3 canvases are centered on the origin; the viewer honours "origin".
      __ndbx: { outputType: network.outputType, outputRange: network.outputRange, origin: "center" },
    });
    return itemName;
  };

  const hoistFunction = (node: Node): string => {
    const itemName = freeName(node.name);
    const item = nativeItem(node, node.function, freshId());
    item.name = itemName;
    (item as NativeFunctionItem & { function: string }).function = node.function;
    items.push(item);
    return itemName;
  };

  const mainName = hoist(library.root, "Main");
  // The root goes first so the editor opens on it.
  const mainIndex = items.findIndex((i) => i.name === mainName);
  if (mainIndex > 0) items.unshift(items.splice(mainIndex, 1)[0]);
  return {
    id: library.name,
    formatVersion: LIVE_FORMAT_VERSION,
    title: library.title ?? library.name,
    dependencies,
    assets: {},
    items,
  };
}

function valuesOf(node: Node): Record<string, LiveParameterValue> | undefined {
  const values: Record<string, LiveParameterValue> = {};
  for (const port of node.inputs) {
    if (isDataPort(port)) continue;
    if (port.expression !== undefined) values[port.name] = { type: "EXPRESSION", expression: port.expression };
    else values[port.name] = { type: "VALUE", value: toLiveLiteral(port) };
  }
  return Object.keys(values).length > 0 ? values : undefined;
}

function colorOf(c: Color) {
  return { r: c.r, g: c.g, b: c.b, a: c.a };
}
