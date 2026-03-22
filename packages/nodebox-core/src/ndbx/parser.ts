import type { Node, Connection } from '../node/node.js';
import type { Port, PortType, Widget, PortRange, MenuItem } from '../node/port.js';
import type { Value } from '../node/value.js';
import type { NodeLibrary } from '../node/library.js';
import { CURRENT_FORMAT_VERSION, upgradeNdbx } from './upgrades.js';

export type LibraryLoader = (name: string) => NodeLibrary | undefined;

// Cache for loaded prototype libraries
const libraryCache = new Map<string, NodeLibrary>();

export function parseNdbx(xml: string, libraryLoader?: LibraryLoader): NodeLibrary {
  const doc = parseXml(xml);
  const root = doc.documentElement;

  if (root.tagName !== 'ndbx') {
    throw new Error(`Expected root element 'ndbx', got '${root.tagName}'`);
  }

  const formatVersion = parseInt(root.getAttribute('formatVersion') ?? String(CURRENT_FORMAT_VERSION), 10);
  const uuid = root.getAttribute('uuid') ?? '';

  // Upgrade if needed
  let xmlToUse = xml;
  if (formatVersion < CURRENT_FORMAT_VERSION) {
    const result = upgradeNdbx(xml, formatVersion);
    xmlToUse = result.xml;
    // Re-parse upgraded XML
    const upgradedDoc = parseXml(xmlToUse);
    return parseNdbxDocument(upgradedDoc, uuid, libraryLoader);
  }

  return parseNdbxDocument(doc, uuid, libraryLoader);
}

function parseNdbxDocument(doc: Document, uuid: string, libraryLoader?: LibraryLoader): NodeLibrary {
  const root = doc.documentElement;
  const properties: Record<string, string> = {};
  const functionLinks: string[] = [];

  // Parse properties and links
  for (let i = 0; i < root.childNodes.length; i++) {
    const child = root.childNodes[i];
    if (child.nodeType !== 1) continue; // Element nodes only
    const el = child as Element;
    if (el.tagName === 'property') {
      const name = el.getAttribute('name') ?? '';
      const value = el.getAttribute('value') ?? '';
      properties[name] = value;
    } else if (el.tagName === 'link') {
      const href = el.getAttribute('href') ?? '';
      functionLinks.push(href);
    }
  }

  // Find the root node element
  let rootNode: Node | null = null;
  for (let i = 0; i < root.childNodes.length; i++) {
    const child = root.childNodes[i];
    if (child.nodeType !== 1) continue;
    const el = child as Element;
    if (el.tagName === 'node') {
      rootNode = parseNodeElement(el, libraryLoader);
      break;
    }
  }

  if (!rootNode) {
    throw new Error('No root node found in ndbx');
  }

  return {
    formatVersion: CURRENT_FORMAT_VERSION,
    uuid,
    root: rootNode,
    properties,
    functionLinks,
  };
}

function parseNodeElement(el: Element, libraryLoader?: LibraryLoader): Node {
  const name = el.getAttribute('name') ?? '';
  const prototypeRef = el.getAttribute('prototype') ?? null;
  const func = el.getAttribute('function') ?? null;
  const category = el.getAttribute('category') ?? null;
  const description = el.getAttribute('description') ?? null;
  const image = el.getAttribute('image') ?? null;
  const posStr = el.getAttribute('position') ?? '0,0';
  const outputType = el.getAttribute('outputType') ?? null;
  const outputRange = (el.getAttribute('outputRange') as PortRange) ?? null;
  const renderedChild = el.getAttribute('renderedChild') ?? null;
  const handle = el.getAttribute('handle') ?? null;
  const comment = el.getAttribute('comment') ?? null;
  const alwaysRendered = el.getAttribute('alwaysRendered') === 'true';

  // Parse position
  const posParts = posStr.split(',');
  const position = {
    x: parseFloat(posParts[0]) || 0,
    y: parseFloat(posParts[1]) || 0,
  };

  // Resolve prototype
  let protoNode: Node | null = null;
  if (prototypeRef) {
    protoNode = resolvePrototype(prototypeRef, libraryLoader);
  }

  // Parse child elements
  const ports: Port[] = [];
  const children: Node[] = [];
  const connections: Connection[] = [];

  for (let i = 0; i < el.childNodes.length; i++) {
    const child = el.childNodes[i];
    if (child.nodeType !== 1) continue;
    const childEl = child as Element;

    if (childEl.tagName === 'port') {
      ports.push(parsePortElement(childEl));
    } else if (childEl.tagName === 'node') {
      children.push(parseNodeElement(childEl, libraryLoader));
    } else if (childEl.tagName === 'conn') {
      connections.push(parseConnectionElement(childEl));
    } else if (childEl.tagName === 'importCoreNode') {
      const ref = childEl.getAttribute('ref');
      if (ref === 'ROOT') {
        // ROOT is the base template — add no-op node with core/zero function
        children.push(createRootNode());
      } else if (ref === 'NETWORK') {
        children.push(createNetworkNode());
      }
    }
  }

  // Merge with prototype
  const node: Node = {
    name,
    prototype: prototypeRef,
    function: func ?? protoNode?.function ?? null,
    category: category ?? protoNode?.category ?? null,
    description: description ?? protoNode?.description ?? null,
    image: image ?? protoNode?.image ?? null,
    position,
    comment,
    inputs: mergeInputs(protoNode?.inputs ?? [], ports),
    outputType: outputType ?? protoNode?.outputType ?? 'geometry',
    outputRange: outputRange ?? protoNode?.outputRange ?? 'value',
    children,
    connections,
    renderedChild,
    handle: handle ?? protoNode?.handle ?? null,
    alwaysRendered,
  };

  return node;
}

function parsePortElement(el: Element): Port {
  const name = el.getAttribute('name') ?? '';
  const type = (el.getAttribute('type') ?? 'float') as PortType;
  const label = el.getAttribute('label') ?? null;
  const description = el.getAttribute('description') ?? null;
  const widget = (el.getAttribute('widget') ?? null) as Widget | null;
  const range = (el.getAttribute('range') ?? 'value') as PortRange;
  const valueStr = el.getAttribute('value') ?? null;
  const minStr = el.getAttribute('min') ?? null;
  const maxStr = el.getAttribute('max') ?? null;
  const childReference = el.getAttribute('childReference') ?? null;

  // Parse menu items
  const menuItems: MenuItem[] = [];
  for (let i = 0; i < el.childNodes.length; i++) {
    const child = el.childNodes[i];
    if (child.nodeType !== 1) continue;
    const menuEl = child as Element;
    if (menuEl.tagName === 'menu') {
      menuItems.push({
        key: menuEl.getAttribute('key') ?? '',
        label: menuEl.getAttribute('label') ?? '',
      });
    }
  }

  return {
    name,
    type,
    label,
    description,
    widget: widget ?? defaultWidgetForType(type),
    range,
    value: parsePortValue(valueStr, type),
    minimumValue: minStr !== null ? parseFloat(minStr) : null,
    maximumValue: maxStr !== null ? parseFloat(maxStr) : null,
    menuItems,
    childReference,
  };
}

function parseConnectionElement(el: Element): Connection {
  const input = el.getAttribute('input') ?? '';
  const output = el.getAttribute('output') ?? '';

  // input format: "nodeName.portName"
  const dotIdx = input.indexOf('.');
  const inputNode = dotIdx >= 0 ? input.substring(0, dotIdx) : input;
  const inputPort = dotIdx >= 0 ? input.substring(dotIdx + 1) : '';

  return {
    outputNode: output,
    inputNode,
    inputPort,
  };
}

function parsePortValue(str: string | null, type: PortType): Value {
  if (str === null || str === '') {
    return getDefaultValue(type);
  }
  switch (type) {
    case 'int': return { type: 'int', value: parseInt(str, 10) || 0 };
    case 'float': return { type: 'float', value: parseFloat(str) || 0 };
    case 'boolean': return { type: 'boolean', value: str === 'true' };
    case 'string': return { type: 'string', value: str };
    case 'point': {
      const parts = str.split(',');
      return {
        type: 'point',
        value: {
          x: parseFloat(parts[0]) || 0,
          y: parseFloat(parts[1]) || 0,
        },
      };
    }
    case 'color': {
      // Parse "#rrggbbaa" format
      const hex = str.startsWith('#') ? str.slice(1) : str;
      if (hex.length === 8) {
        return {
          type: 'color',
          value: {
            r: parseInt(hex.slice(0, 2), 16) / 255,
            g: parseInt(hex.slice(2, 4), 16) / 255,
            b: parseInt(hex.slice(4, 6), 16) / 255,
            a: parseInt(hex.slice(6, 8), 16) / 255,
          },
        };
      }
      if (hex.length === 6) {
        return {
          type: 'color',
          value: {
            r: parseInt(hex.slice(0, 2), 16) / 255,
            g: parseInt(hex.slice(2, 4), 16) / 255,
            b: parseInt(hex.slice(4, 6), 16) / 255,
            a: 1,
          },
        };
      }
      return { type: 'null' };
    }
    case 'geometry':
    case 'list':
    case 'data':
    case 'context':
      return { type: 'null' };
    default:
      return { type: 'string', value: str };
  }
}

function getDefaultValue(type: PortType): Value {
  switch (type) {
    case 'int': return { type: 'int', value: 0 };
    case 'float': return { type: 'float', value: 0 };
    case 'boolean': return { type: 'boolean', value: false };
    case 'string': return { type: 'string', value: '' };
    case 'point': return { type: 'point', value: { x: 0, y: 0 } };
    case 'color': return { type: 'color', value: { r: 0, g: 0, b: 0, a: 1 } };
    default: return { type: 'null' };
  }
}

function defaultWidgetForType(type: PortType): Widget {
  switch (type) {
    case 'int': return 'int';
    case 'float': return 'float';
    case 'string': return 'string';
    case 'boolean': return 'toggle';
    case 'point': return 'point';
    case 'color': return 'color';
    default: return 'none';
  }
}

function mergeInputs(protoInputs: Port[], overridePorts: Port[]): Port[] {
  // Start with proto's ports, then override by name
  const result: Port[] = protoInputs.map(p => ({ ...p }));

  for (const override of overridePorts) {
    const existing = result.findIndex(p => p.name === override.name);
    if (existing >= 0) {
      // Merge: override only the fields that are set
      result[existing] = {
        ...result[existing],
        ...override,
        // Keep proto values for fields not specified in override
        label: override.label ?? result[existing].label,
        description: override.description ?? result[existing].description,
      };
    } else {
      result.push(override);
    }
  }

  return result;
}

function resolvePrototype(ref: string, libraryLoader?: LibraryLoader): Node | null {
  const dotIdx = ref.indexOf('.');
  if (dotIdx < 0) return null;

  const libName = ref.substring(0, dotIdx);
  const nodeName = ref.substring(dotIdx + 1);

  // Try loading from cache
  let lib = libraryCache.get(libName);
  if (!lib && libraryLoader) {
    lib = libraryLoader(libName);
    if (lib) libraryCache.set(libName, lib);
  }

  if (!lib) return null;

  // Find node in library's root children
  return findNodeInChildren(lib.root, nodeName);
}

function findNodeInChildren(node: Node, name: string): Node | null {
  for (const child of node.children) {
    if (child.name === name) return child;
  }
  return null;
}

function createRootNode(): Node {
  return {
    name: 'ROOT',
    prototype: null,
    function: 'core/zero',
    category: null,
    description: 'Base node',
    image: null,
    position: { x: 0, y: 0 },
    comment: null,
    inputs: [],
    outputType: 'geometry',
    outputRange: 'value',
    children: [],
    connections: [],
    renderedChild: null,
    handle: null,
    alwaysRendered: false,
  };
}

function createNetworkNode(): Node {
  return {
    name: 'NETWORK',
    prototype: null,
    function: null,
    category: null,
    description: 'Network node',
    image: null,
    position: { x: 0, y: 0 },
    comment: null,
    inputs: [],
    outputType: 'geometry',
    outputRange: 'value',
    children: [],
    connections: [],
    renderedChild: null,
    handle: null,
    alwaysRendered: false,
  };
}

// Map function references from Java/Python to TypeScript
export function mapFunctionRef(ref: string): string {
  // Java: "corevector/rect" stays the same
  // Python: function="pyvector/compound" → "corevector/compound"
  if (ref.startsWith('pyvector/')) {
    return 'corevector/' + ref.substring('pyvector/'.length);
  }
  return ref;
}

function parseXml(xml: string): Document {
  if (typeof DOMParser !== 'undefined') {
    return new DOMParser().parseFromString(xml, 'text/xml');
  }
  // For Node.js, try @xmldom/xmldom
  try {
    const { DOMParser: XmlDOMParser } = require('@xmldom/xmldom');
    return new XmlDOMParser().parseFromString(xml, 'text/xml');
  } catch {
    throw new Error('XML parsing requires DOMParser (browser) or @xmldom/xmldom (Node.js)');
  }
}

// Allow clearing the cache for testing
export function clearLibraryCache(): void {
  libraryCache.clear();
}
