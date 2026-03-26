import type { Value } from './value.js';
import { NULL_VALUE } from './value.js';

export type PortType = 'int' | 'float' | 'string' | 'boolean' | 'point'
  | 'color' | 'geometry' | 'list' | 'data' | 'context';

export type Widget = 'none' | 'int' | 'float' | 'angle' | 'string' | 'text'
  | 'password' | 'toggle' | 'color' | 'point' | 'menu'
  | 'file' | 'font' | 'image' | 'data' | 'seed' | 'gradient';

export type PortRange = 'value' | 'list';

export interface MenuItem {
  key: string;
  label: string;
}

export interface Port {
  name: string;
  type: PortType;
  label: string | null;
  description: string | null;
  widget: Widget;
  range: PortRange;
  value: Value;
  minimumValue: number | null;
  maximumValue: number | null;
  menuItems: MenuItem[];
  childReference: string | null; // for published ports: "childNode/childPort"
}

export function createPort(name: string, type: PortType, value: Value = NULL_VALUE): Port {
  return {
    name,
    type,
    label: null,
    description: null,
    widget: defaultWidgetForType(type),
    range: 'value',
    value,
    minimumValue: null,
    maximumValue: null,
    menuItems: [],
    childReference: null,
  };
}

export function defaultWidgetForType(type: PortType): Widget {
  switch (type) {
    case 'int': return 'int';
    case 'float': return 'float';
    case 'string': return 'string';
    case 'boolean': return 'toggle';
    case 'point': return 'point';
    case 'color': return 'color';
    case 'geometry': return 'none';
    case 'list': return 'none';
    case 'data': return 'none';
    case 'context': return 'none';
  }
}

export function isFileWidget(port: Port): boolean {
  return port.widget === 'file' || port.widget === 'image';
}

export function isPublishedPort(port: Port): boolean {
  return port.childReference !== null;
}

export function getChildNodeName(port: Port): string | null {
  if (!port.childReference) return null;
  // .ndbx uses dot separator (e.g. "slice1.list"), code may use slash
  const sep = port.childReference.includes('.') ? '.' : '/';
  const parts = port.childReference.split(sep);
  return parts[0] ?? null;
}

export function getChildPortName(port: Port): string | null {
  if (!port.childReference) return null;
  const sep = port.childReference.includes('.') ? '.' : '/';
  const parts = port.childReference.split(sep);
  return parts[1] ?? null;
}

export function clampPortValue(port: Port, value: number): number {
  let result = value;
  if (port.minimumValue !== null) result = Math.max(port.minimumValue, result);
  if (port.maximumValue !== null) result = Math.min(port.maximumValue, result);
  return result;
}
