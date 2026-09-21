// Reads .ndbx documents into the unified model, a port of nodebox.node.NodeLibrary's StAX parser
// (with the prototype flattening it performs) plus the upgrade step the client runs beforehand.

import { Point } from "../graphics/point";
import { CURRENT_NDBX_FORMAT_VERSION, NodeRepository } from "../model/library";
import { CORE_NODES, addChild, createRootNode, extendNode, getChild, getInput } from "../model/node";
import { clampValue, createPort, parseRange, parseValue, parseWidget } from "../model/port";
import { Device, FunctionLanguage, FunctionLink, Library, MenuItem, Node, Port } from "../model/types";
import { needsUpgrade, upgradeXml } from "./upgrades";
import { XmlElement, parseXml } from "./xml";

export interface ReadOptions {
  /** Where "library.node" prototypes are resolved; defaults to the built-in libraries. */
  repository?: NodeRepository;
  /** The library name; defaults to the file's base name or "untitled". */
  name?: string;
  /** The file path or URL the document came from, for resolving relative links. */
  file?: string;
  /** Run the format upgrades first (default true). */
  upgrade?: boolean;
  /** Fail on an unknown prototype instead of degrading to a bare node with a warning (default false). */
  strict?: boolean;
}

export interface ReadResult {
  library: Library;
  warnings: string[];
  /** The version the document had on disk before upgrading. */
  formatVersion?: string;
}

export class NdbxLoadError extends Error {}

/** Map from the Java function classes NodeBox 3 links to, to the namespaces they publish. */
export const JAVA_NAMESPACES: Record<string, string> = {
  "nodebox.function.CoreFunctions": "core",
  "nodebox.function.CoreVectorFunctions": "corevector",
  "nodebox.function.MathFunctions": "math",
  "nodebox.function.ListFunctions": "list",
  "nodebox.function.StringFunctions": "string",
  "nodebox.function.ColorFunctions": "color",
  "nodebox.function.DataFunctions": "data",
  "nodebox.function.NetworkFunctions": "network",
  "nodebox.function.DeviceFunctions": "device",
};

export function parseNdbx(xml: string, options: ReadOptions = {}): ReadResult {
  const warnings: string[] = [];
  let formatVersion: string | undefined;
  const m = /formatVersion=['"]([\d.]+)['"]/.exec(xml);
  if (m) formatVersion = m[1];
  if (options.upgrade !== false && needsUpgrade(xml)) {
    const upgraded = upgradeXml(xml);
    warnings.push(...upgraded.warnings);
    xml = upgraded.xml;
  }
  const doc = parseXml(xml);
  const root = doc.root;
  if (root.tagName !== "ndbx") throw new NdbxLoadError(`Only tag ndbx allowed, not ${root.tagName}`);
  const version = root.getAttribute("formatVersion");
  if (version !== undefined && version !== CURRENT_NDBX_FORMAT_VERSION) {
    throw new NdbxLoadError(`File uses version ${version}, current version is ${CURRENT_NDBX_FORMAT_VERSION}.`);
  }
  const name = options.name ?? libraryNameFromFile(options.file) ?? "untitled";
  const repository = options.repository ?? defaultRepository();
  const state: ParseState = { repository, warnings, strict: options.strict ?? false, name };

  const library: Library = {
    name,
    uuid: root.getAttribute("uuid"),
    file: options.file,
    root: createRootNode(),
    properties: {},
    devices: [],
    functionLinks: [],
    dependencies: {},
    assets: {},
    sourceFormat: "ndbx",
    meta: {},
  };
  if (doc.declaration) library.meta.xmlDeclaration = doc.declaration;

  for (const child of root.childElements) {
    switch (child.tagName) {
      case "property": {
        const key = child.getAttribute("name");
        const value = child.getAttribute("value");
        if (key !== undefined && value !== undefined) library.properties[key] = value;
        break;
      }
      case "link":
        library.functionLinks.push(parseLink(child));
        break;
      case "device":
        library.devices.push(parseDevice(child));
        break;
      case "node":
        library.root = parseNode(child, library.root, state);
        break;
      default:
        throw new NdbxLoadError(`Unknown tag ${child.tagName}`);
    }
  }
  return { library, warnings, formatVersion };
}

let builtinRepository: NodeRepository | undefined;

/** The repository the readers fall back to; set by the libraries module once the built-ins are loaded. */
export function setDefaultRepository(repository: NodeRepository): void {
  builtinRepository = repository;
}

export function defaultRepository(): NodeRepository {
  return builtinRepository ?? new NodeRepository();
}

function libraryNameFromFile(file?: string): string | undefined {
  if (!file) return undefined;
  const base = file.split(/[\\/]/).pop() ?? file;
  return base.replace(/\.ndbx$/i, "");
}

interface ParseState {
  repository: NodeRepository;
  warnings: string[];
  strict: boolean;
  /** The library being parsed, so a node can extend one defined earlier in the same file. */
  name: string;
  root?: Node;
}

export function parseLink(e: XmlElement): FunctionLink {
  const rel = e.getAttribute("rel");
  if (rel !== "functions") throw new NdbxLoadError(`Unsupported link relation '${rel}'.`);
  const href = e.getAttribute("href") ?? "";
  const m = /^([a-z]+):(.*)$/.exec(href);
  if (!m) throw new NdbxLoadError(`Invalid function library href '${href}'.`);
  const language = m[1] as FunctionLanguage;
  const path = m[2];
  if (!["java", "python", "clojure", "javascript"].includes(language))
    throw new NdbxLoadError(`Unsupported function library language '${language}'.`);
  return { language, href, namespace: namespaceForLink(language, path) };
}

export function namespaceForLink(language: FunctionLanguage, path: string): string {
  if (language === "java") {
    if (JAVA_NAMESPACES[path]) return JAVA_NAMESPACES[path];
    const simple = path.split(".").pop() ?? path;
    return simple.replace(/Functions$/, "").toLowerCase();
  }
  const base = path.split(/[\\/]/).pop() ?? path;
  return base.replace(/\.(py|clj|js|mjs)$/i, "");
}

function parseDevice(e: XmlElement): Device {
  const device: Device = { name: e.getAttribute("name") ?? "", type: e.getAttribute("type") ?? "", properties: {} };
  for (const p of e.childElementsWithName("property")) {
    const key = p.getAttribute("name");
    const value = p.getAttribute("value");
    if (key !== undefined && value !== undefined) device.properties[key] = value;
  }
  return device;
}

const NODE_ATTRIBUTES = [
  "prototype",
  "name",
  "comment",
  "category",
  "description",
  "image",
  "function",
  "outputType",
  "outputRange",
  "position",
  "renderedChild",
  "handle",
  "alwaysRendered",
];

function lookupPrototype(id: string, parent: Node, state: ParseState): Node | undefined {
  if (!id.includes(".")) return getChild(parent, id);
  const found = state.repository.getNode(id);
  if (found) return found;
  // The library is not in the repository while it is being read, so a node that extends one
  // defined earlier in the same file resolves against the root built so far.
  const dot = id.indexOf(".");
  if (state.root && id.slice(0, dot) === state.name) return getChild(state.root, id.slice(dot + 1));
  return undefined;
}

function createNode(
  e: XmlElement,
  extendFrom: Node,
  extendFromId: string | null,
  parent: Node,
  state: ParseState,
): Node {
  const prototypeId = e.getAttribute("prototype");
  let prototype: Node | undefined;
  let id = extendFromId;
  if (prototypeId !== undefined) {
    prototype = lookupPrototype(prototypeId, parent, state);
    id = prototypeId;
    if (!prototype) {
      const message = `Prototype ${prototypeId} could not be found.`;
      if (state.strict) throw new NdbxLoadError(message);
      state.warnings.push(message);
      prototype = e.firstChildElement("node") ? CORE_NODES.NETWORK() : CORE_NODES.ROOT();
    }
  } else {
    prototype = extendFrom;
  }
  const node = extendNode(prototype, id);
  if (prototypeId !== undefined && !lookupPrototype(prototypeId, parent, state)) node.meta.missingPrototype = true;

  const attr = (name: string) => e.getAttribute(name);
  if (attr("name") !== undefined) node.name = attr("name")!;
  if (attr("comment") !== undefined) node.comment = attr("comment")!;
  if (attr("category") !== undefined) node.category = attr("category")!;
  if (attr("description") !== undefined) node.description = attr("description")!;
  if (attr("image") !== undefined) node.image = attr("image")!;
  if (attr("function") !== undefined) node.function = attr("function")!;
  if (attr("outputType") !== undefined) node.outputType = attr("outputType")!;
  if (attr("outputRange") !== undefined) node.outputRange = parseRange(attr("outputRange")!.toLowerCase());
  if (attr("position") !== undefined) node.position = Point.parse(attr("position")!);
  if (attr("handle") !== undefined) node.handle = attr("handle")!;
  if (attr("alwaysRendered") !== undefined) node.alwaysRendered = attr("alwaysRendered") === "true";
  // Keep unknown attributes so foreign extensions survive a round trip.
  for (const [key, value] of e.attributes) {
    if (!NODE_ATTRIBUTES.includes(key)) node.meta[`attr:${key}`] = value;
  }
  return node;
}

function parseNode(e: XmlElement, parent: Node, state: ParseState): Node {
  const hasChildren = e.childElements.some((c) => c.tagName === "node" || c.tagName === "importCoreNode");
  const prototypeId = e.getAttribute("prototype");
  // A node without a prototype extends the root node, or the network node once it has children.
  const base = prototypeId === undefined && hasChildren ? CORE_NODES.NETWORK() : CORE_NODES.ROOT();
  const baseId = prototypeId === undefined && hasChildren ? "core.network" : null;
  const node = createNode(e, base, baseId, parent, state);
  if (prototypeId === undefined && !hasChildren) node.prototype = null;
  // The library's own root, so that a later node can extend one defined earlier in this file.
  if (state.root === undefined) state.root = node;

  for (const child of e.childElements) {
    switch (child.tagName) {
      case "node":
        addChild(node, parseNode(child, node, state));
        break;
      case "importCoreNode": {
        const ref = child.getAttribute("ref") ?? "";
        const factory = CORE_NODES[ref];
        if (!factory) throw new NdbxLoadError(`Core node '${ref}' does not exist.`);
        const coreNode = factory();
        coreNode.prototype = ref === "ROOT" ? null : "core.node";
        coreNode.meta.importCoreNode = ref;
        addChild(node, coreNode);
        break;
      }
      case "port": {
        const portName = child.getAttribute("name") ?? "";
        const existing = getInput(node, portName);
        if (existing) {
          Object.assign(existing, parsePort(child, existing));
        } else {
          node.inputs.push(parsePort(child, undefined));
        }
        break;
      }
      case "conn": {
        const output = child.getAttribute("output") ?? "";
        const input = child.getAttribute("input") ?? "";
        const i = input.indexOf(".");
        if (i < 0) throw new NdbxLoadError(`Invalid connection input '${input}'.`);
        const connection = { outputNode: output, inputNode: input.slice(0, i), inputPort: input.slice(i + 1) };
        const outputPort = child.getAttribute("outputPort");
        // A value port takes one connection, so a second replaces the first; a list port collects.
        const target = getChild(node, connection.inputNode);
        const targetPort = target && getInput(target, connection.inputPort);
        if (!targetPort || targetPort.range !== "list") {
          node.connections = node.connections.filter(
            (c) => !(c.inputNode === connection.inputNode && c.inputPort === connection.inputPort),
          );
        }
        node.connections.push(outputPort ? { ...connection, outputPort } : connection);
        break;
      }
      case "output": {
        // Extension: named outputs for NodeBox Live style nodes stored in .ndbx.
        node.outputs.push(parsePort(child, undefined));
        break;
      }
      case "sticky": {
        node.stickies.push(parseSticky(child));
        break;
      }
      default:
        throw new NdbxLoadError(`Unknown tag ${child.tagName}`);
    }
  }
  const renderedChild = e.getAttribute("renderedChild");
  if (renderedChild !== undefined) node.renderedChild = renderedChild;
  return node;
}

export function parsePort(e: XmlElement, prototype: Port | undefined): Port {
  const name = e.getAttribute("name") ?? "";
  const type = e.getAttribute("type");
  let port: Port;
  if (prototype === undefined) {
    if (type === undefined) throw new NdbxLoadError(`Port ${name} needs a type.`);
    port = createPort(name, type);
  } else {
    port = { ...prototype, menuItems: prototype.menuItems.map((m) => ({ ...m })) };
  }
  const attr = (n: string) => e.getAttribute(n);
  if (attr("label") !== undefined) port.label = attr("label")!;
  if (attr("childReference") !== undefined) port.childReference = attr("childReference")!;
  // A port that feeds several children, which NodeBox 3 never wrote but the core supports.
  const more = attr("childReferences");
  if (more !== undefined && more !== "") port.childReferences = more.split(/\s+/);
  if (attr("widget") !== undefined) port.widget = parseWidget(attr("widget")!);
  if (attr("range") !== undefined) port.range = parseRange(attr("range")!);
  if (attr("min") !== undefined) port.min = Number(attr("min"));
  if (attr("max") !== undefined) port.max = Number(attr("max"));
  if (attr("value") !== undefined) {
    if (!["int", "float", "string", "boolean", "point", "color"].includes(port.type))
      throw new NdbxLoadError(
        `Port ${name}: you can only set the value for one of the standard types, not ${port.type}.`,
      );
    port.value = clampValue(port, parseValue(port.type, attr("value")!));
  }
  if (attr("description") !== undefined) port.description = attr("description")!;
  if (attr("expression") !== undefined) port.expression = attr("expression")!;
  if (attr("section") !== undefined) port.section = attr("section")!;
  if (attr("step") !== undefined) port.step = Number(attr("step"));
  const items: MenuItem[] = [];
  for (const child of e.childElements) {
    if (child.tagName !== "menu") throw new NdbxLoadError(`Unknown tag ${child.tagName}`);
    const key = child.getAttribute("key") ?? "";
    items.push({ key, label: child.getAttribute("label") ?? key });
  }
  if (items.length > 0) port.menuItems = items;
  return port;
}

function parseSticky(e: XmlElement) {
  const num = (n: string, d = 0) => Number(e.getAttribute(n) ?? d);
  return {
    id: e.getAttribute("id") ?? "",
    x: num("x"),
    y: num("y"),
    width: num("width", 200),
    height: num("height", 100),
    text: e.textContent,
    backgroundColor: parseColorAttr(e.getAttribute("backgroundColor"), "#ffffe0ff"),
    fontColor: parseColorAttr(e.getAttribute("fontColor"), "#000000ff"),
    fontSize: num("fontSize", 12),
  };
}

function parseColorAttr(value: string | undefined, fallback: string) {
  return parseValue("color", value ?? fallback) as import("../graphics/color").Color;
}
