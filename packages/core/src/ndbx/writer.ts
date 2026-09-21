// Writes the unified model as a .ndbx document, a port of nodebox.node.NDBXWriter: attributes and
// ports are written only where they differ from the prototype, children sorted by name with
// prototypes before their instances, connections last.

import { CURRENT_NDBX_FORMAT_VERSION, NodeRepository } from "../model/library";
import { getChild, getInput, portDiffersFromPrototype } from "../model/node";
import { formatValue, isPublishedPort, valuesEqual } from "../model/port";
import { Library, Node, Port } from "../model/types";
import { isStandardType } from "../model/types";
import { defaultRepository } from "./reader";
import { XmlDocument, XmlElement, XmlText, serializeXml } from "./xml";

export interface WriteOptions {
  repository?: NodeRepository;
  /** Also write extensions (named outputs, expressions, stickies) that NodeBox 3 does not read. Default true. */
  extensions?: boolean;
}

export function writeNdbx(library: Library, options: WriteOptions = {}): string {
  return serializeXml(toXmlDocument(library, options), {
    declaration: (library.meta.xmlDeclaration as string | undefined) ?? '<?xml version="1.0" encoding="UTF-8" standalone="no"?>',
  });
}

export function toXmlDocument(library: Library, options: WriteOptions = {}): XmlDocument {
  const repository = options.repository ?? defaultRepository();
  const extensions = options.extensions ?? true;
  const root = new XmlElement("ndbx");
  root.setAttribute("type", "file");
  root.setAttribute("formatVersion", CURRENT_NDBX_FORMAT_VERSION);
  if (library.uuid) root.setAttribute("uuid", library.uuid);
  for (const key of Object.keys(library.properties).sort()) {
    root.appendChild(
      new XmlElement("property", [
        ["name", key],
        ["value", library.properties[key]],
      ]),
    );
  }
  for (const link of library.functionLinks) {
    if (link.namespace === "core" && link.language === "java") continue;
    if (link.language === "javascript" && link.source !== undefined) {
      if (!extensions) continue;
      const e = new XmlElement("link", [
        ["rel", "functions"],
        ["href", link.href],
      ]);
      e.appendChild(new XmlText(link.source, true));
      root.appendChild(e);
      continue;
    }
    root.appendChild(
      new XmlElement("link", [
        ["rel", "functions"],
        ["href", link.href],
      ]),
    );
  }
  for (const device of library.devices) {
    const e = new XmlElement("device", [
      ["name", device.name],
      ["type", device.type],
    ]);
    for (const key of Object.keys(device.properties).sort()) {
      e.appendChild(
        new XmlElement("property", [
          ["name", key],
          ["value", device.properties[key]],
        ]),
      );
    }
    root.appendChild(e);
  }
  root.appendChild(writeNode(library.root, null, repository, extensions));
  return { root };
}

function prototypeOf(node: Node, parent: Node | null, repository: NodeRepository): Node | undefined {
  if (node.prototype === null) return undefined;
  if (node.prototype.includes(".")) return repository.getNode(node.prototype);
  return parent ? getChild(parent, node.prototype) : undefined;
}

export function writeNode(node: Node, parent: Node | null, repository: NodeRepository, extensions: boolean): XmlElement {
  const e = new XmlElement("node");
  const proto = prototypeOf(node, parent, repository);
  const differs = (key: keyof Node) => proto === undefined || proto[key] !== node[key];

  if (node.meta.importCoreNode) {
    const imp = new XmlElement("importCoreNode", [["ref", String(node.meta.importCoreNode)]]);
    return imp;
  }
  if (node.prototype !== null && node.prototype !== "core.node") e.setAttribute("prototype", node.prototype);
  e.setAttribute("name", node.name);
  if (differs("comment") && node.comment) e.setAttribute("comment", node.comment);
  if (differs("category") && node.category) e.setAttribute("category", node.category);
  if (differs("description") && node.description) e.setAttribute("description", node.description);
  if (differs("outputType")) e.setAttribute("outputType", node.outputType);
  if (differs("outputRange")) e.setAttribute("outputRange", node.outputRange);
  if (differs("image") && node.image) e.setAttribute("image", node.image);
  if (differs("function")) e.setAttribute("function", node.function);
  if (differs("handle") && node.handle) e.setAttribute("handle", node.handle);
  if (proto === undefined || !proto.position.equals(node.position)) e.setAttribute("position", node.position.toString());
  if (node.renderedChild) e.setAttribute("renderedChild", node.renderedChild);
  if (node.alwaysRendered && differs("alwaysRendered")) e.setAttribute("alwaysRendered", "true");
  for (const [key, value] of Object.entries(node.meta)) {
    if (key.startsWith("attr:")) e.setAttribute(key.slice(5), String(value));
  }

  // Children: sorted by name, but a child whose prototype is a sibling goes before that sibling's instances.
  const written = new Set<string>();
  const sorted = [...node.children].sort((a, b) => a.name.localeCompare(b.name));
  const writeOrdered = (child: Node) => {
    if (written.has(child.name)) return;
    if (child.prototype !== null && !child.prototype.includes(".")) {
      const sibling = getChild(node, child.prototype);
      if (sibling && sibling !== child) writeOrdered(sibling);
    }
    written.add(child.name);
    e.appendChild(writeNode(child, node, repository, extensions));
  };
  for (const child of sorted) writeOrdered(child);

  for (const port of node.inputs) {
    const protoPort = proto ? getInput(proto, port.name) : undefined;
    if (!portDiffersFromPrototype(port, protoPort) && !(extensions && port.expression)) continue;
    e.appendChild(writePort(port, protoPort, extensions));
  }
  if (extensions) {
    for (const port of node.outputs) e.appendChild(writePort(port, undefined, extensions, "output"));
    for (const sticky of node.stickies) {
      const s = new XmlElement("sticky", [
        ["id", sticky.id],
        ["x", String(sticky.x)],
        ["y", String(sticky.y)],
        ["width", String(sticky.width)],
        ["height", String(sticky.height)],
        ["backgroundColor", sticky.backgroundColor.toString()],
        ["fontColor", sticky.fontColor.toString()],
        ["fontSize", String(sticky.fontSize)],
      ]);
      s.appendChild(new XmlText(sticky.text));
      e.appendChild(s);
    }
  }
  for (const c of node.connections) {
    const conn = new XmlElement("conn", [
      ["output", c.outputNode],
      ["input", `${c.inputNode}.${c.inputPort}`],
    ]);
    if (c.outputPort && c.outputPort !== "output" && extensions) conn.setAttribute("outputPort", c.outputPort);
    e.appendChild(conn);
  }
  return e;
}

function writePort(port: Port, protoPort: Port | undefined, extensions: boolean, tag = "port"): XmlElement {
  const e = new XmlElement(tag);
  e.setAttribute("name", port.name);
  e.setAttribute("type", port.type);
  const differs = (key: keyof Port) => protoPort === undefined || protoPort[key] !== port[key];
  if (differs("label") && port.label) e.setAttribute("label", port.label);
  if (isPublishedPort(port) && (protoPort === undefined || protoPort.childReference !== port.childReference))
    e.setAttribute("childReference", port.childReference!);
  if (port.childReferences && port.childReferences.length > 0)
    e.setAttribute("childReferences", port.childReferences.join(" "));
  if (differs("widget")) e.setAttribute("widget", port.widget);
  if (differs("range")) e.setAttribute("range", port.range);
  if (isStandardType(port.type) && port.value !== null) e.setAttribute("value", formatValue(port.type, port.value));
  if (differs("description") && port.description) e.setAttribute("description", port.description);
  if (port.min !== undefined && (protoPort === undefined || !valuesEqual(protoPort.min ?? null, port.min)))
    e.setAttribute("min", formatValue("float", port.min));
  if (port.max !== undefined && (protoPort === undefined || !valuesEqual(protoPort.max ?? null, port.max)))
    e.setAttribute("max", formatValue("float", port.max));
  if (extensions) {
    if (port.expression) e.setAttribute("expression", port.expression);
    if (port.section) e.setAttribute("section", port.section);
    if (port.step !== undefined && port.step !== 1) e.setAttribute("step", String(port.step));
  }
  const protoItems = protoPort?.menuItems ?? [];
  const sameMenu =
    protoItems.length === port.menuItems.length &&
    protoItems.every((m, i) => m.key === port.menuItems[i].key && m.label === port.menuItems[i].label);
  if (!sameMenu) {
    for (const item of port.menuItems) {
      e.appendChild(
        new XmlElement("menu", [
          ["key", item.key],
          ["label", item.label],
        ]),
      );
    }
  }
  return e;
}
