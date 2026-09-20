// Write a library that came from a NodeBox Live project (or was built in the unified model) back to
// project.json. Ids kept in `meta` are reused; new items and nodes get fresh ids.

import { Color } from "../graphics/color";
import { Point } from "../graphics/point";
import { splitReference } from "../model/node";
import { isPublishedPort } from "../model/port";
import { Library, Node, Port } from "../model/types";
import { LIVE_FORMAT_VERSION, LiveColor, LiveConnection, LiveItem, LiveLiteralValue, LiveNetworkItem, LiveParameter, LiveParameterValue, LivePortType, LiveProject } from "./types";

export function writeLiveProject(library: Library): LiveProject {
  const projectKey = String(library.meta.projectKey ?? "self/self");
  let nextId = 1;
  const freshId = () => `0:${nextId++}`;
  const items: LiveItem[] = [];
  for (const node of library.root.children) {
    if (node.function.startsWith("live/") && !node.isNetwork && node.meta.liveSource !== undefined) {
      items.push({
        type: "FUNCTION",
        id: String(node.meta.liveId ?? freshId()),
        name: node.name,
        category: node.category,
        description: node.description,
        width: (node.meta.liveWidth as number | undefined) ?? 1000,
        height: (node.meta.liveHeight as number | undefined) ?? 1000,
        background: (node.meta.liveBackground as LiveColor | undefined) ?? { r: 0.15, g: 0.15, b: 0.15, a: 1 },
        source: String(node.meta.liveSource),
      });
    } else if (node.isNetwork) {
      items.push(networkToItem(node, projectKey, freshId));
    }
  }
  const project: LiveProject = {
    formatVersion: (library.meta.liveFormatVersion as number | undefined) ?? LIVE_FORMAT_VERSION,
    title: library.title ?? library.name,
    dependencies: { ...library.dependencies },
    assets: { ...library.assets },
    items,
  };
  if (library.meta.liveId !== undefined) project.id = String(library.meta.liveId);
  if (library.description) project.description = library.description;
  if (library.color) project.color = library.color;
  if (library.meta.liveScope !== undefined) project.scope = library.meta.liveScope as string;
  if (library.meta.liveIsPublished !== undefined) project.isPublished = library.meta.liveIsPublished as boolean;
  if (library.meta.livePublishDate !== undefined) project.publishDate = library.meta.livePublishDate as string;
  if (library.meta.liveGallery !== undefined) project.__gallery = library.meta.liveGallery;
  return project;
}

function networkToItem(node: Node, projectKey: string, freshId: () => string): LiveItem {
  const ids = new Map<string, string>();
  const children: LiveNetworkItem[] = [];
  for (const child of node.children) {
    const id = String(child.meta.liveId ?? freshId());
    ids.set(child.name, id);
    children.push({
      type: "NODE",
      id,
      name: child.name,
      x: child.position.x,
      y: child.position.y,
      fn: child.prototype && child.prototype.includes("/") ? child.prototype : `${projectKey}/${child.prototype ?? child.name}`,
      values: valuesOf(child),
    });
  }
  const inletMeta = (node.meta.liveInlets as { id: string; x: number; y: number; portName: string; portType: LivePortType }[] | undefined) ?? [];
  const outletMeta = (node.meta.liveOutlets as { id: string; x: number; y: number; portName: string; portType: LivePortType }[] | undefined) ?? [];
  const connections: LiveConnection[] = [];
  for (const c of node.connections) {
    const outNode = ids.get(c.outputNode);
    const inNode = ids.get(c.inputNode);
    if (!outNode || !inNode) continue;
    connections.push({ type: "NODE_TO_NODE", outNode, outPort: c.outputPort ?? "output", inNode, inPort: c.inputPort });
  }
  const inputPorts: { name: string; type: LivePortType }[] = [];
  const parameters: LiveParameter[] = [];
  for (const port of node.inputs) {
    if (isPublishedPort(port)) {
      const meta = inletMeta.find((i) => i.portName === port.name);
      const inlet = { type: "INLET" as const, id: meta?.id ?? freshId(), x: meta?.x ?? 0, y: meta?.y ?? 0, portName: port.name, portType: meta?.portType ?? toLivePortType(port) };
      children.push(inlet);
      inputPorts.push({ name: port.name, type: inlet.portType });
      const [childName, childPort] = splitReference(port.childReference!);
      const inNode = ids.get(childName);
      if (inNode) connections.push({ type: "INLET_TO_NODE", inlet: inlet.id, inNode, inPort: childPort });
      const extra = (node.meta.liveExtraInletTargets as Record<string, string[]> | undefined)?.[port.name] ?? [];
      for (const ref of extra) {
        const [n, p] = splitReference(ref);
        const target = ids.get(n);
        if (target) connections.push({ type: "INLET_TO_NODE", inlet: inlet.id, inNode: target, inPort: p });
      }
    } else if (inletMeta.some((i) => i.portName === port.name)) {
      const meta = inletMeta.find((i) => i.portName === port.name)!;
      children.push({ type: "INLET", id: meta.id, x: meta.x, y: meta.y, portName: port.name, portType: meta.portType });
      inputPorts.push({ name: port.name, type: meta.portType });
    } else {
      parameters.push(portToParameter(port));
    }
  }
  const outputPorts: { name: string; type: LivePortType }[] = [];
  for (const port of node.outputs) {
    const meta = outletMeta.find((o) => o.portName === port.name);
    const outlet = { type: "OUTLET" as const, id: meta?.id ?? freshId(), x: meta?.x ?? 0, y: meta?.y ?? 0, portName: port.name, portType: meta?.portType ?? toLivePortType(port) };
    children.push(outlet);
    outputPorts.push({ name: port.name, type: outlet.portType });
    if (port.childReference) {
      const [childName, childPort] = splitReference(port.childReference);
      const outNode = ids.get(childName);
      if (outNode) connections.push({ type: "NODE_TO_OUTLET", outNode, outPort: childPort, outlet: outlet.id });
    }
  }
  for (const sticky of node.stickies) {
    children.push({
      type: "STICKY",
      id: sticky.id || freshId(),
      x: sticky.x,
      y: sticky.y,
      width: sticky.width,
      height: sticky.height,
      backgroundColor: colorToLive(sticky.backgroundColor),
      text: sticky.text,
      fontSize: sticky.fontSize,
      fontColor: colorToLive(sticky.fontColor),
    });
  }
  const item: LiveItem = {
    type: "NETWORK",
    id: String(node.meta.liveId ?? freshId()),
    name: node.name,
    category: node.category,
    description: node.description,
    canvasSize: (node.meta.liveCanvasSize as string | undefined) ?? "fixed",
    padding: (node.meta.livePadding as number | undefined) ?? 10,
    width: (node.meta.liveWidth as number | undefined) ?? 1000,
    height: (node.meta.liveHeight as number | undefined) ?? 1000,
    background: (node.meta.liveBackground as LiveColor | undefined) ?? { r: 0.15, g: 0.15, b: 0.15, a: 1 },
    children,
    connections,
    renderedNode: node.renderedChild ? (ids.get(node.renderedChild) ?? null) : null,
    inputPorts,
    outputPorts,
    parameters,
    sections: (node.meta.liveSections as LiveParameter[] | undefined) ? (node.meta.liveSections as never) : [],
  };
  if (node.meta.liveGallery !== undefined) item.__gallery = node.meta.liveGallery;
  return item;
}

function toLivePortType(port: Port): LivePortType {
  switch (port.type) {
    case "shape":
    case "geometry":
      return "SHAPE";
    case "spec":
      return "SPEC";
    case "table":
    case "list":
      return "TABLE";
    default:
      return port.range === "list" ? "TABLE" : "SERIES";
  }
}

function valuesOf(node: Node): Record<string, LiveParameterValue> | undefined {
  const values: Record<string, LiveParameterValue> = {};
  const changed = (node.meta.liveValueNames as string[] | undefined) ?? undefined;
  for (const port of node.inputs) {
    if (port.type === "table" || port.type === "shape" || port.type === "spec" || port.type === "list" || port.type === "geometry") continue;
    if (port.expression !== undefined) {
      values[port.name] = { type: "EXPRESSION", expression: port.expression };
    } else if (changed === undefined || changed.includes(port.name)) {
      values[port.name] = { type: "VALUE", value: valueToLive(port) };
    }
  }
  return Object.keys(values).length > 0 ? values : undefined;
}

function valueToLive(port: Port): LiveLiteralValue {
  const v = port.value;
  if (v instanceof Color) return colorToLive(v);
  if (v instanceof Point) return { x: v.x, y: v.y };
  if (v === null) return "";
  return v as LiveLiteralValue;
}

function colorToLive(c: Color): LiveColor {
  return { r: c.r, g: c.g, b: c.b, a: c.a };
}

function portToParameter(port: Port): LiveParameter {
  const type = port.type === "float" || port.type === "int" ? "NUMBER" : port.type === "boolean" ? "BOOLEAN" : port.type === "point" ? "POINT" : port.type === "color" ? "COLOR" : port.widget === "file" ? "FILE" : port.menuItems.length > 0 ? "CHOICE" : "STRING";
  return {
    name: port.name,
    type,
    widget: port.widget === "text" ? "TEXT" : (type as LiveParameter["widget"]),
    label: port.label || port.name,
    section: port.section,
    defaultValue: valueToLive(port),
    choices: port.menuItems.length > 0 ? port.menuItems.map((m) => ({ name: m.key, label: m.label })) : undefined,
    min: port.min ?? -Infinity,
    max: port.max ?? Infinity,
    step: port.step ?? 1,
  };
}
