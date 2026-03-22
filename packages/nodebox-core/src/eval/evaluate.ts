import type { Node, Connection } from '../node/node.js';
import type { Port, PortRange } from '../node/port.js';
import type { Value } from '../node/value.js';
import type { NodeLibrary } from '../node/library.js';
import type { Path } from '../geometry/path.js';
import type { Text } from '../geometry/text.js';
import type { Platform } from '../platform.js';
import type { FunctionRegistry } from './function-registry.js';
import { flattenNodeMap } from '../node/library.js';
import { isPublishedPort, getChildNodeName, getChildPortName, isFileWidget } from '../node/port.js';
import { convert, clampValue } from './type-conversions.js';
import { createDefaultRegistry } from './register-ops.js';

export interface EvalError {
  nodePath: string;
  message: string;
}

export interface EvalResult {
  paths: Path[];
  texts: Text[];
  output: Value[];
  errors: EvalError[];
}

export interface EvalOptions {
  library: NodeLibrary;
  frame: number;
  platform: Platform;
  functionRegistry?: FunctionRegistry;
}

export interface EvalContextData {
  frame: number;
  mouse: { x: number; y: number } | null;
}

export async function evaluate(options: EvalOptions): Promise<EvalResult> {
  const { library, frame, platform, functionRegistry } = options;
  const registry = functionRegistry ?? createDefaultRegistry();
  const nodeMap = flattenNodeMap(library.root);
  const context: EvalContextData = { frame, mouse: null };
  const renderResults = new Map<string, Value[]>();
  const nodeArgResults = new Map<string, Value[]>();
  const errors: EvalError[] = [];

  function getNode(path: string): Node | undefined {
    return nodeMap.get(path);
  }

  function getChildPath(networkPath: string, childName: string): string {
    return networkPath.endsWith('/') ? networkPath + childName : networkPath + '/' + childName;
  }

  function findOutputNode(network: Node, inputNodeName: string, inputPortName: string): Node | undefined {
    for (const conn of network.connections) {
      if (conn.inputNode === inputNodeName && conn.inputPort === inputPortName) {
        return network.children.find(c => c.name === conn.outputNode);
      }
    }
    return undefined;
  }

  function wrappingGet<T>(list: T[], index: number): T {
    return list[index % list.length];
  }

  function buildArgumentMaps(portArguments: Map<Port, Value[]>): Map<Port, Value>[] {
    // Check for empty lists
    for (const [port, values] of portArguments) {
      if (port.range !== 'list' && values.length === 0) return [];
    }

    let maxSize = 0;
    for (const [port, values] of portArguments) {
      if (port.range === 'list') {
        maxSize = Math.max(maxSize, 1);
      } else {
        maxSize = Math.max(maxSize, values.length);
      }
    }
    if (maxSize === 0) return [];

    const maps: Map<Port, Value>[] = [];
    for (let i = 0; i < maxSize; i++) {
      const map = new Map<Port, Value>();
      for (const [port, values] of portArguments) {
        if (port.range === 'list') {
          map.set(port, { type: 'list', value: values });
        } else {
          map.set(port, wrappingGet(values, i));
        }
      }
      maps.push(map);
    }
    return maps;
  }

  function getPortValue(nodePath: string, port: Port): Value {
    if (port.type === 'context') {
      return { type: 'data', value: context as unknown as Record<string, unknown> };
    }
    return port.value;
  }

  function postProcessResult(nodePath: string, result: Value | Value[]): Value[] {
    const node = getNode(nodePath);
    if (!node) return [];

    if (node.outputRange === 'list') {
      if (Array.isArray(result)) return result;
      return [result];
    }

    if (Array.isArray(result)) {
      if (result.length === 0) return result;
      return result;
    }

    if (result === null || result === undefined) return [];
    if ((result as Value).type === 'null') return [];
    return [result];
  }

  function renderNode(nodePath: string, argumentMap?: Map<Port, Value>): Value[] {
    const node = getNode(nodePath);
    if (!node) {
      errors.push({ nodePath, message: `Node not found: ${nodePath}` });
      return [];
    }

    // Only cache when called without arguments (top-level render)
    // When called with arguments (from list matching), each call produces different results
    const hasArgs = argumentMap && argumentMap.size > 0;

    let result: Value | Value[];
    if (node.children.length > 0) {
      // Network node
      if (node.renderedChild) {
        result = renderChild(nodePath, node.renderedChild, argumentMap ?? new Map());
      } else {
        result = [];
      }
      // Always rendered children
      for (const child of node.children) {
        if (child.alwaysRendered) {
          const childPath = getChildPath(nodePath, child.name);
          if (!renderResults.has(childPath)) {
            renderNode(childPath);
          }
        }
      }
    } else {
      // Leaf node — invoke function
      result = invokeNode(nodePath, argumentMap ?? new Map());
    }

    const results = postProcessResult(nodePath, result);
    if (!hasArgs) {
      renderResults.set(nodePath, results);
    }
    return results;
  }

  function renderChild(
    networkPath: string,
    childName: string,
    networkArgumentMap: Map<Port, Value>,
  ): Value[] {
    const network = getNode(networkPath);
    if (!network) return [];
    const child = network.children.find(c => c.name === childName);
    if (!child) return [];

    // Cache key
    const cacheKey = `${networkPath}/${childName}:${serializeArgs(networkArgumentMap)}`;
    const cached = nodeArgResults.get(cacheKey);
    if (cached) return cached;

    const resultsList: Value[] = [];

    if (child.inputs.length === 0) {
      const childPath = getChildPath(networkPath, child.name);
      return renderNode(childPath);
    }

    // Evaluate each port
    const portArguments = new Map<Port, Value[]>();
    for (const port of child.inputs) {
      let result = evaluatePort(networkPath, child, port, networkArgumentMap);
      result = convertResultsForPort(port, result);
      result = clampResultsForPort(port, result);
      portArguments.set(port, result);
    }

    // Published port overrides from network arguments
    for (const [networkPort, value] of networkArgumentMap) {
      if (isPublishedPort(networkPort)) {
        const childNodeName = getChildNodeName(networkPort);
        const childPortName = getChildPortName(networkPort);
        if (childNodeName === child.name && childPortName) {
          const childPort = child.inputs.find(p => p.name === childPortName);
          if (childPort) {
            const values = Array.isArray(value) ? value : (value.type === 'list' ? (value as any).value : [value]);
            portArguments.set(childPort, values);
          }
        }
      }
    }

    const argumentMaps = buildArgumentMaps(portArguments);
    const childPath = getChildPath(networkPath, child.name);
    for (const argMap of argumentMaps) {
      const results = renderNode(childPath, argMap);
      resultsList.push(...results);
    }

    nodeArgResults.set(cacheKey, resultsList);
    return resultsList;
  }

  function evaluatePort(
    networkPath: string,
    child: Node,
    childPort: Port,
    networkArgumentMap: Map<Port, Value>,
  ): Value[] {
    const network = getNode(networkPath);
    if (!network) return [];

    // Check if this port is a published port on the parent network
    const parentNetwork = getNode(networkPath);
    if (parentNetwork) {
      for (const netPort of parentNetwork.inputs) {
        if (isPublishedPort(netPort)) {
          const childNodeName = getChildNodeName(netPort);
          const childPortName = getChildPortName(netPort);
          if (childNodeName === child.name && childPortName === childPort.name) {
            // Check if there's data from the network's arguments
            const argValue = networkArgumentMap.get(netPort);
            if (argValue !== undefined) {
              if (argValue.type === 'list') return (argValue as any).value;
              return [argValue];
            }
          }
        }
      }
    }

    // Check for upstream connection
    const outputNode = findOutputNode(network, child.name, childPort.name);
    if (outputNode) {
      return renderChild(networkPath, outputNode.name, networkArgumentMap);
    }

    // No connection — use port default value
    const childPath = getChildPath(networkPath, child.name);
    const value = getPortValue(childPath, childPort);
    if (value.type === 'null') return [];
    return [value];
  }

  function convertResultsForPort(port: Port, values: Value[]): Value[] {
    return values.map(v => convert(v, port.type));
  }

  function clampResultsForPort(port: Port, values: Value[]): Value[] {
    if (port.minimumValue === null && port.maximumValue === null) return values;
    return values.map(v => clampValue(v, port.minimumValue, port.maximumValue));
  }

  function invokeNode(nodePath: string, argumentMap: Map<Port, Value>): Value | Value[] {
    const node = getNode(nodePath);
    if (!node) return { type: 'null' };

    const fnName = node.function ?? 'core/zero';
    const fn = registry.get(fnName);
    if (!fn) {
      errors.push({ nodePath, message: `Function not found: ${fnName}` });
      return { type: 'null' };
    }

    const args: Value[] = [];
    for (const port of node.inputs) {
      if (argumentMap.has(port)) {
        args.push(argumentMap.get(port)!);
      } else if (port.type === 'context') {
        args.push({ type: 'data', value: context as unknown as Record<string, unknown> });
      } else if (port.range === 'value') {
        args.push(getPortValue(nodePath, port));
      } else {
        args.push({ type: 'list', value: [] });
      }
    }

    try {
      return fn(...args);
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      errors.push({ nodePath, message: msg });
      return { type: 'null' };
    }
  }

  function serializeArgs(map: Map<Port, Value>): string {
    if (map.size === 0) return '';
    const parts: string[] = [];
    for (const [port, val] of map) {
      parts.push(`${port.name}=${JSON.stringify(val)}`);
    }
    return parts.join('&');
  }

  // Start evaluation
  const rootPath = library.root.name;
  let output: Value[] = [];
  if (library.root.renderedChild) {
    output = renderChild(rootPath, library.root.renderedChild, new Map());
  }

  // Extract paths from geometry values
  const paths: Path[] = [];
  for (const v of output) {
    if (v.type === 'geometry') {
      paths.push(...v.value);
    }
  }

  return { paths, texts: [], output, errors };
}
