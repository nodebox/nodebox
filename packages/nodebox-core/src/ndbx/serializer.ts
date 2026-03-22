import type { NodeLibrary } from '../node/library.js';
import type { Node, Connection } from '../node/node.js';
import type { Port } from '../node/port.js';
import type { Value } from '../node/value.js';
import { CURRENT_FORMAT_VERSION } from './upgrades.js';

export function serializeNdbx(library: NodeLibrary): string {
  const lines: string[] = [];
  lines.push(`<?xml version="1.0" encoding="UTF-8" standalone="no"?>`);

  const attrs: string[] = [`formatVersion="${CURRENT_FORMAT_VERSION}"`, `type="file"`];
  if (library.uuid) attrs.push(`uuid="${escXml(library.uuid)}"`);
  lines.push(`<ndbx ${attrs.join(' ')}>`);

  // Properties
  for (const [name, value] of Object.entries(library.properties)) {
    lines.push(`    <property name="${escXml(name)}" value="${escXml(value)}"/>`);
  }

  // Function links
  for (const link of library.functionLinks) {
    lines.push(`    <link href="${escXml(link)}" rel="functions"/>`);
  }

  // Root node
  serializeNode(library.root, lines, '    ');

  lines.push(`</ndbx>`);
  return lines.join('\n');
}

function serializeNode(node: Node, lines: string[], indent: string): void {
  const attrs: string[] = [`name="${escXml(node.name)}"`];
  if (node.prototype) attrs.push(`prototype="${escXml(node.prototype)}"`);
  if (node.function) attrs.push(`function="${escXml(node.function)}"`);
  if (node.category) attrs.push(`category="${escXml(node.category)}"`);
  if (node.description) attrs.push(`description="${escXml(node.description)}"`);
  if (node.image) attrs.push(`image="${escXml(node.image)}"`);
  if (node.position.x !== 0 || node.position.y !== 0) {
    attrs.push(`position="${node.position.x.toFixed(2)},${node.position.y.toFixed(2)}"`);
  }
  if (node.outputType !== 'geometry') attrs.push(`outputType="${escXml(node.outputType)}"`);
  if (node.outputRange !== 'value') attrs.push(`outputRange="${node.outputRange}"`);
  if (node.renderedChild) attrs.push(`renderedChild="${escXml(node.renderedChild)}"`);
  if (node.handle) attrs.push(`handle="${escXml(node.handle)}"`);
  if (node.comment) attrs.push(`comment="${escXml(node.comment)}"`);
  if (node.alwaysRendered) attrs.push(`alwaysRendered="true"`);

  const hasContent = node.inputs.length > 0 || node.children.length > 0 || node.connections.length > 0;

  if (!hasContent) {
    lines.push(`${indent}<node ${attrs.join(' ')}/>`);
    return;
  }

  lines.push(`${indent}<node ${attrs.join(' ')}>`);

  for (const port of node.inputs) {
    serializePort(port, lines, indent + '    ');
  }

  for (const child of node.children) {
    serializeNode(child, lines, indent + '    ');
  }

  for (const conn of node.connections) {
    serializeConnection(conn, lines, indent + '    ');
  }

  lines.push(`${indent}</node>`);
}

function serializePort(port: Port, lines: string[], indent: string): void {
  const attrs: string[] = [`name="${escXml(port.name)}"`, `type="${port.type}"`];

  if (port.label) attrs.push(`label="${escXml(port.label)}"`);
  if (port.description) attrs.push(`description="${escXml(port.description)}"`);
  if (port.widget !== 'none') attrs.push(`widget="${port.widget}"`);
  if (port.range !== 'value') attrs.push(`range="${port.range}"`);

  const valStr = valueToNdbxString(port.value, port.type);
  if (valStr !== null) attrs.push(`value="${escXml(valStr)}"`);

  if (port.minimumValue !== null) attrs.push(`min="${port.minimumValue}"`);
  if (port.maximumValue !== null) attrs.push(`max="${port.maximumValue}"`);
  if (port.childReference) attrs.push(`childReference="${escXml(port.childReference)}"`);

  const hasMenu = port.menuItems.length > 0;
  if (!hasMenu) {
    lines.push(`${indent}<port ${attrs.join(' ')}/>`);
  } else {
    lines.push(`${indent}<port ${attrs.join(' ')}>`);
    for (const item of port.menuItems) {
      lines.push(`${indent}    <menu key="${escXml(item.key)}" label="${escXml(item.label)}"/>`);
    }
    lines.push(`${indent}</port>`);
  }
}

function serializeConnection(conn: Connection, lines: string[], indent: string): void {
  const input = `${conn.inputNode}.${conn.inputPort}`;
  lines.push(`${indent}<conn input="${escXml(input)}" output="${escXml(conn.outputNode)}"/>`);
}

function valueToNdbxString(value: Value, _type: string): string | null {
  switch (value.type) {
    case 'int': return String(value.value);
    case 'float': return String(value.value);
    case 'boolean': return String(value.value);
    case 'string': return value.value;
    case 'point': return `${value.value.x.toFixed(2)},${value.value.y.toFixed(2)}`;
    case 'color': {
      const { r, g, b, a } = value.value;
      const toHex = (n: number) => Math.round(n * 255).toString(16).padStart(2, '0');
      return `#${toHex(r)}${toHex(g)}${toHex(b)}${toHex(a)}`;
    }
    case 'null': return null;
    default: return null;
  }
}

function escXml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}
