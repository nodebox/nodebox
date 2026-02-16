import type { Node, Connection } from '../types/node';
import type { Value } from '../types/value';
import type { EvalResult } from '../types/eval-result';
import type { PathRenderData } from '../types/eval-result';
import type { Path, Point, Color } from '../types/geometry';
import {
  rectPath,
  ellipsePath,
  colorizePath,
  strokePath,
  translatePath,
} from './generators';

function getPortValue(node: Node, portName: string): Value {
  const port = node.inputs.find((p) => p.name === portName);
  if (port) return port.value;
  return { type: 'null' };
}

function toFloat(v: Value): number {
  if (v.type === 'float' || v.type === 'int') return v.value;
  return 0;
}

function toPoint(v: Value): Point {
  if (v.type === 'point') return v.value;
  return { x: 0, y: 0 };
}

function toColor(v: Value): Color {
  if (v.type === 'color') return v.value;
  return { r: 0, g: 0, b: 0, a: 1 };
}

function toPath(v: Value): Path | null {
  if (v.type === 'path') return v.value;
  return null;
}

function resolveInputValue(
  node: Node,
  portName: string,
  children: Node[],
  connections: Connection[],
  evaluated: Map<string, Value>,
): Value {
  // Check if there's a connection feeding this port
  const conn = connections.find(
    (c) => c.inputNode === node.name && c.inputPort === portName,
  );
  if (conn) {
    return evaluateNode(conn.outputNode, children, connections, evaluated);
  }
  return getPortValue(node, portName);
}

function evaluateNode(
  nodeName: string,
  children: Node[],
  connections: Connection[],
  evaluated: Map<string, Value>,
): Value {
  // Memoize
  const cached = evaluated.get(nodeName);
  if (cached) return cached;

  const node = children.find((n) => n.name === nodeName);
  if (!node) return { type: 'null' };

  const resolve = (portName: string) =>
    resolveInputValue(node, portName, children, connections, evaluated);

  let result: Value = { type: 'null' };

  switch (node.prototype) {
    case 'corevector.rect': {
      const position = toPoint(resolve('position'));
      const width = toFloat(resolve('width'));
      const height = toFloat(resolve('height'));
      const roundness = toPoint(resolve('roundness'));
      result = { type: 'path', value: rectPath(position, width, height, roundness) };
      break;
    }
    case 'corevector.ellipse': {
      const position = toPoint(resolve('position'));
      const width = toFloat(resolve('width'));
      const height = toFloat(resolve('height'));
      result = { type: 'path', value: ellipsePath(position, width, height) };
      break;
    }
    case 'corevector.colorize': {
      const shape = toPath(resolve('shape'));
      const fill = toColor(resolve('fill'));
      if (shape) {
        result = { type: 'path', value: colorizePath(shape, fill) };
      }
      break;
    }
    case 'corevector.stroke': {
      const shape = toPath(resolve('shape'));
      const color = toColor(resolve('color'));
      const width = toFloat(resolve('strokeWidth'));
      if (shape) {
        result = { type: 'path', value: strokePath(shape, color, width) };
      }
      break;
    }
    case 'corevector.translate': {
      const shape = toPath(resolve('shape'));
      const offset = toPoint(resolve('translate'));
      if (shape) {
        result = { type: 'path', value: translatePath(shape, offset) };
      }
      break;
    }
    default:
      break;
  }

  evaluated.set(nodeName, result);
  return result;
}

function pathToRenderData(path: Path): PathRenderData {
  return {
    contours: path.contours,
    fill: path.fill,
    stroke: path.stroke,
    strokeWidth: path.strokeWidth,
  };
}

export function evaluate(
  renderedChild: string | null,
  children: Node[],
  connections: Connection[],
): EvalResult {
  if (!renderedChild) {
    return { paths: [], texts: [], output: { type: 'none', isMultiple: false, values: [] }, errors: [] };
  }

  const evaluated = new Map<string, Value>();
  const result = evaluateNode(renderedChild, children, connections, evaluated);

  const paths: PathRenderData[] = [];
  if (result.type === 'path') {
    paths.push(pathToRenderData(result.value));
  } else if (result.type === 'list') {
    for (const item of result.value) {
      if (item.type === 'path') {
        paths.push(pathToRenderData(item.value));
      }
    }
  }

  return {
    paths,
    texts: [],
    output: {
      type: 'geometry',
      isMultiple: false,
      values: paths.map((_, i) => `Path ${i}`),
    },
    errors: [],
  };
}
