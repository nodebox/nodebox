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
  rotatePath,
  scalePath,
  copyPaths,
  linePath,
  polygonPath,
  starPath,
  gridPoints,
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
    case 'corevector.rotate': {
      const shape = toPath(resolve('shape'));
      const angle = toFloat(resolve('angle'));
      const origin = toPoint(resolve('origin'));
      if (shape) {
        result = { type: 'path', value: rotatePath(shape, angle, origin) };
      }
      break;
    }
    case 'corevector.scale': {
      const shape = toPath(resolve('shape'));
      const scaleVal = toPoint(resolve('scale'));
      const origin = toPoint(resolve('origin'));
      if (shape) {
        result = { type: 'path', value: scalePath(shape, scaleVal.x, scaleVal.y, origin) };
      }
      break;
    }
    case 'corevector.copy': {
      const shape = toPath(resolve('shape'));
      const copies = toFloat(resolve('copies'));
      const translate = toPoint(resolve('translate'));
      const rotate = toFloat(resolve('rotate'));
      const scale = toPoint(resolve('scale'));
      if (shape) {
        const paths = copyPaths(shape, Math.round(copies), translate, rotate, scale);
        result = { type: 'list', value: paths.map((p) => ({ type: 'path' as const, value: p })) };
      }
      break;
    }
    case 'corevector.line': {
      const point1 = toPoint(resolve('point1'));
      const point2 = toPoint(resolve('point2'));
      result = { type: 'path', value: linePath(point1, point2) };
      break;
    }
    case 'corevector.polygon': {
      const position = toPoint(resolve('position'));
      const radius = toFloat(resolve('radius'));
      const sides = toFloat(resolve('sides'));
      const rotation = toFloat(resolve('align'));
      result = { type: 'path', value: polygonPath(position, radius, Math.round(sides), rotation) };
      break;
    }
    case 'corevector.star': {
      const position = toPoint(resolve('position'));
      const points = toFloat(resolve('points'));
      const outer = toFloat(resolve('outer'));
      const inner = toFloat(resolve('inner'));
      result = { type: 'path', value: starPath(position, Math.round(points), outer, inner) };
      break;
    }
    case 'corevector.grid': {
      const rows = toFloat(resolve('rows'));
      const columns = toFloat(resolve('columns'));
      const width = toFloat(resolve('width'));
      const height = toFloat(resolve('height'));
      const position = toPoint(resolve('position'));
      const pts = gridPoints(rows, columns, width, height, position);
      result = { type: 'list', value: pts.map((p) => ({ type: 'point' as const, value: p })) };
      break;
    }
    case 'corevector.make_point': {
      const x = toFloat(resolve('x'));
      const y = toFloat(resolve('y'));
      result = { type: 'point', value: { x, y } };
      break;
    }
    case 'math.add': {
      const v1 = toFloat(resolve('value1'));
      const v2 = toFloat(resolve('value2'));
      result = { type: 'float', value: v1 + v2 };
      break;
    }
    case 'math.subtract': {
      const v1 = toFloat(resolve('value1'));
      const v2 = toFloat(resolve('value2'));
      result = { type: 'float', value: v1 - v2 };
      break;
    }
    case 'math.multiply': {
      const v1 = toFloat(resolve('value1'));
      const v2 = toFloat(resolve('value2'));
      result = { type: 'float', value: v1 * v2 };
      break;
    }
    case 'math.divide': {
      const v1 = toFloat(resolve('value1'));
      const v2 = toFloat(resolve('value2'));
      result = { type: 'float', value: v2 !== 0 ? v1 / v2 : 0 };
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
