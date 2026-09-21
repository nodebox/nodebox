// The evaluator: a port of nodebox.node.NodeContext with its list-matching semantics, extended with
// named outputs, expressions and asynchronous functions so NodeBox Live nodes run through it too.
//
// The rules, in short:
//   * a network renders its rendered child (or the children behind its named outputs);
//   * a node is invoked once per element of its longest value-range input; shorter value-range
//     inputs cycle; list-range inputs receive the whole list; an empty value-range input means no
//     invocations at all; the results of all invocations are concatenated;
//   * values are converted to the port type after evaluation and clamped to the port's min/max;
//   * values entering a network through published ports override the referenced child ports.

import { Point } from "../graphics/point";
import { Library, Node, Port, PortValue } from "../model/types";
import {
  childPath,
  getChild,
  getInput,
  hasListOutputRange,
  hasRenderedChild,
  outputPorts,
  primaryOutputName,
  splitReference,
} from "../model/node";
import { flattenedNodeMap } from "../model/library";
import {
  clampValue,
  hasListRange,
  hasValueRange,
  isClassicPort,
  isClassicShapePort,
  isFileWidget,
  isPublishedPort,
  publishedTargets,
} from "../model/port";
import { evaluateExpression } from "./expression";
import { FunctionRepository, NodeFunction } from "./function-repository";
import { convertValues, listType, valueType } from "./values";

/** Results of one node, per output name. */
export type NodeResults = Map<string, unknown[]>;

export interface ContextOptions {
  /** Render data such as the frame number, the mouse position and device messages. */
  data?: Record<string, unknown>;
  /** "nodeName.portName" -> value; overrides port values without touching the document (handles use this). */
  portOverrides?: Record<string, unknown>;
  /** Resolve a relative file path against the document's location. */
  resolvePath?: (path: string) => string;
  /** State that lives across renders, e.g. NodeBox Live runtime node instances. */
  persistent?: Map<string, unknown>;
}

export const DEFAULT_CONTEXT_DATA: Record<string, unknown> = { frame: 1.0 };

/** A single invocation's arguments: port name -> value. */
type ArgumentMap = Map<string, unknown>;

export class NodeRenderError extends Error {
  constructor(
    public nodePath: string,
    public node: Node,
    public cause: unknown,
  ) {
    super(`Error rendering ${nodePath}: ${cause instanceof Error ? cause.message : String(cause)}`);
    this.name = "NodeRenderError";
  }
}

export class NodeContext {
  readonly library: Library;
  readonly functionRepository: FunctionRepository;
  readonly data: Record<string, unknown>;
  readonly portOverrides: Record<string, unknown>;
  readonly persistent: Map<string, unknown>;
  /** The last results per node path, for the viewer and for inspecting intermediate values. */
  readonly renderResults = new Map<string, NodeResults>();
  readonly resolvePath: (path: string) => string;
  private readonly nodeMap: Map<string, Node>;
  private readonly memo = new Map<string, NodeResults>();
  private readonly outputNodeCache = new Map<Node, Map<string, Node>>();

  constructor(library: Library, functionRepository: FunctionRepository, options: ContextOptions = {}) {
    this.library = library;
    this.functionRepository = functionRepository;
    this.data = { ...DEFAULT_CONTEXT_DATA, ...(options.data ?? {}) };
    this.portOverrides = options.portOverrides ?? {};
    this.persistent = options.persistent ?? new Map();
    this.resolvePath = options.resolvePath ?? defaultResolvePath(library.file);
    this.nodeMap = flattenedNodeMap(library);
  }

  get frame(): number {
    const f = this.data.frame;
    return typeof f === "number" ? f : 1.0;
  }

  getNodeForPath(path: string): Node | undefined {
    return this.nodeMap.get(path);
  }

  /** Render a node by path and return its primary output. */
  async render(nodePath = "/"): Promise<unknown[]> {
    const results = await this.renderNode(nodePath);
    const node = this.nodeMap.get(nodePath)!;
    return results.get(primaryOutputName(node)) ?? [];
  }

  /**
   * Render a network as the thing being shown, and return its rendered child's results unchanged.
   * Rendering the network node itself would apply the network's own output range, which is what
   * happens when it is used as a node inside another network, not when it is the entry point.
   */
  async renderEntryPoint(nodePath = "/"): Promise<unknown[]> {
    const node = this.nodeMap.get(nodePath);
    if (!node || !node.isNetwork || !hasRenderedChild(node)) return this.render(nodePath);
    const child = getChild(node, node.renderedChild)!;
    const childResults = await this.renderChild(nodePath, child, new Map());
    return childResults.get(primaryOutputName(child)) ?? [];
  }

  /**
   * Render the node: a network renders its rendered child (and the children behind its outputs),
   * anything else invokes its function.
   */
  async renderNode(nodePath: string, argumentMap: ArgumentMap = new Map()): Promise<NodeResults> {
    const node = this.nodeMap.get(nodePath);
    if (!node) throw new Error(`Node ${nodePath} does not exist.`);
    let results: NodeResults;
    if (node.isNetwork) {
      results = new Map();
      // Named outputs first: each forwards a child's output.
      for (const output of node.outputs) {
        if (!output.childReference) continue;
        const [childName, childPort] = splitReference(output.childReference);
        const child = getChild(node, childName);
        if (!child) continue;
        const childResults = await this.renderChild(nodePath, child, argumentMap);
        // The child's list is one result of the network; the output range decides whether it unwraps.
        results.set(output.name, [childResults.get(childPort || primaryOutputName(child)) ?? []]);
      }
      if (hasRenderedChild(node)) {
        const child = getChild(node, node.renderedChild)!;
        const childResults = await this.renderChild(nodePath, child, argumentMap);
        const primary = primaryOutputName(node);
        if (!results.has(primary)) results.set(primary, [childResults.get(primaryOutputName(child)) ?? []]);
      }
      if (results.size === 0) results.set(primaryOutputName(node), []);
      results = this.postProcessResults(node, results);
    } else {
      const raw = await this.invokeNode(nodePath, node, argumentMap);
      results = this.postProcessResults(node, raw);
    }
    this.renderResults.set(nodePath, results);
    return results;
  }

  /** Render every alwaysRendered child of a network that has not rendered yet (side-effect nodes). */
  async renderAlwaysRenderedNodes(networkPath: string): Promise<void> {
    const network = this.nodeMap.get(networkPath);
    if (!network || !network.isNetwork) return;
    for (const child of network.children) {
      if (!child.alwaysRendered) continue;
      const path = childPath(networkPath, child.name);
      if (!this.renderResults.has(path)) await this.renderNode(path);
    }
  }

  private postProcessResults(node: Node, raw: NodeResults): NodeResults {
    const results: NodeResults = new Map();
    for (const port of outputPorts(node)) {
      const value = raw.get(port.name);
      results.set(port.name, postProcess(node, port, value));
    }
    return results;
  }

  /** Render a child of a network, applying list matching over its evaluated inputs. */
  async renderChild(
    networkPath: string,
    child: Node,
    networkArgumentMap: ArgumentMap = new Map(),
  ): Promise<NodeResults> {
    const network = this.nodeMap.get(networkPath)!;
    const memoKey = memoKeyFor(networkPath, child.name, networkArgumentMap);
    const stored = this.memo.get(memoKey);
    if (stored) return stored;
    const childNodePath = childPath(networkPath, child.name);

    if (child.inputs.length === 0) {
      const results = await this.renderNode(childNodePath);
      this.memo.set(memoKey, results);
      return results;
    }

    const portArguments = new Map<Port, unknown[]>();
    // A classic list parameter that is not connected receives its stored value as it is, not a
    // one-element list: only a connection hands such a parameter a list.
    const scalarPorts = new Set<Port>();
    // A list of points reaching a classic shape parameter is one argument, not one point per
    // invocation: g.js functions such as wigglePoints take the points of a shape as a whole.
    const wholeListPorts = new Set<Port>();
    for (const port of child.inputs) {
      const raw = await this.evaluatePort(networkPath, child, port, networkArgumentMap);
      if (!raw.connected && isClassicPort(port) && hasListRange(port)) scalarPorts.add(port);
      if (raw.connected && isClassicShapePort(port) && hasValueRange(port) && isPointLike(raw.values[0])) {
        wholeListPorts.add(port);
      }
      let values: unknown[];
      try {
        values = this.convertResultsForPort(port, raw.values, raw.sourceType);
      } catch (e) {
        throw new NodeRenderError(
          childNodePath,
          child,
          new Error(`Cannot convert the value for port ${port.name}: ${e instanceof Error ? e.message : e}`),
        );
      }
      values = clampResultsForPort(port, values);
      portArguments.set(port, values);
    }

    // Values entering through the network's published ports override the referenced child ports.
    for (const [portName, value] of networkArgumentMap) {
      const networkPort = getInput(network, portName);
      if (!networkPort || !isPublishedPort(networkPort)) continue;
      for (const target of publishedTargets(networkPort)) {
        if (target.node !== child.name) continue;
        const childPort = getInput(child, target.port);
        if (!childPort) continue;
        portArguments.set(childPort, Array.isArray(value) ? value : [value]);
        // The value now comes from outside the network, so it is no longer the port's own default.
        scalarPorts.delete(childPort);
      }
    }

    const results: NodeResults = new Map();
    const masterPort = child.masterInput ? getInput(child, child.masterInput) : undefined;
    for (const argumentMap of buildArgumentMaps(portArguments, { masterPort, scalarPorts, wholeListPorts })) {
      const invocationResults = await this.renderNode(childNodePath, argumentMap);
      for (const [name, list] of invocationResults) {
        const existing = results.get(name);
        if (existing) existing.push(...list);
        else results.set(name, [...list]);
      }
    }
    if (results.size === 0) for (const port of outputPorts(child)) results.set(port.name, []);
    // Classic NodeBox Live: when a value-range shape node returns a list of points from each
    // invocation, the node's result is the first of those lists rather than all of them joined.
    for (const port of outputPorts(child)) {
      if (!isClassicShapePort(port) || !hasValueRange(port)) continue;
      const list = results.get(port.name);
      if (list && Array.isArray(list[0]) && isPointLike(list[0][0])) results.set(port.name, list[0]);
    }
    this.memo.set(memoKey, results);
    return results;
  }

  private async evaluatePort(
    networkPath: string,
    child: Node,
    childPort: Port,
    networkArgumentMap: ArgumentMap,
  ): Promise<{ values: unknown[]; sourceType: string | undefined; connected: boolean }> {
    const network = this.nodeMap.get(networkPath)!;
    // A value port takes one connection; a list port takes as many as are wired, in order.
    const connections = this.findConnections(network, child, childPort);
    if (connections.length > 0) {
      const values: unknown[] = [];
      let sourceType: string | undefined;
      let connected = false;
      for (const connection of connections) {
        const outputNode = getChild(network, connection.outputNode);
        if (!outputNode) continue;
        const outputResults = await this.renderChild(networkPath, outputNode, networkArgumentMap);
        const outputName = connection.outputPort ?? primaryOutputName(outputNode);
        let part = outputResults.get(outputName) ?? [];
        if (isFileWidget(childPort)) part = part.map((v) => this.resolvePath(String(v)));
        values.push(...part);
        sourceType ??= this.sourceTypeOf(networkPath, outputNode, outputName);
        connected = true;
      }
      if (connected) return { values, sourceType, connected: true };
    }
    const value = this.getPortValue(childPath(networkPath, child.name), child, childPort);
    // A classic port's null is a value the function receives; a NodeBox 3 port's null is no value.
    if (isClassicPort(childPort)) return { values: [value ?? null], sourceType: childPort.type, connected: false };
    const empty = value === null || value === undefined;
    return { values: empty ? [] : [value], sourceType: childPort.type, connected: false };
  }

  /**
   * The declared type of a node's output, followed through list nodes and networks: JavaScript has
   * one number type, so whether a value was a Java Long or Double (which decides how it converts to
   * a string) has to be read off the node that produced it.
   */
  private sourceTypeOf(networkPath: string, node: Node, outputName: string, depth = 0): string | undefined {
    const outputPort = outputPorts(node).find((p) => p.name === outputName);
    const type = outputPort?.type;
    if (depth > 24) return type;
    // A network's declared output type is a default ("float"); its rendered child knows better.
    if (node.isNetwork) {
      const child = getChild(node, node.renderedChild);
      if (!child) return type;
      return this.sourceTypeOf(childPath(networkPath, node.name), child, primaryOutputName(child), depth + 1) ?? type;
    }
    // List nodes and "null" pass their elements through; look at what feeds them.
    if (node.function.startsWith("list/") || node.function === "corevector/doNothing") {
      const network = this.nodeMap.get(networkPath);
      if (!network) return type;
      for (const port of node.inputs) {
        if (port.type !== "list" && port.type !== "geometry" && port.type !== "data") continue;
        const connection = network.connections.find((c) => c.inputNode === node.name && c.inputPort === port.name);
        if (!connection) continue;
        const upstream = getChild(network, connection.outputNode);
        if (!upstream) continue;
        const resolved = this.sourceTypeOf(
          networkPath,
          upstream,
          connection.outputPort ?? primaryOutputName(upstream),
          depth + 1,
        );
        if (resolved !== undefined) return resolved;
      }
      // Fed through a published port of the enclosing network: unknown here.
      return undefined;
    }
    return type;
  }

  private findConnections(network: Node, inputNode: Node, inputPort: Port) {
    let lookup = this.outputNodeCache.get(network);
    if (!lookup) {
      lookup = new Map();
      for (const c of network.connections)
        lookup.set(`${c.inputNode} ${c.inputPort}`, getChild(network, c.outputNode)!);
      this.outputNodeCache.set(network, lookup);
    }
    if (!lookup.has(`${inputNode.name} ${inputPort.name}`)) return [];
    const matches = network.connections.filter((c) => c.inputNode === inputNode.name && c.inputPort === inputPort.name);
    return hasListRange(inputPort) ? matches : matches.slice(0, 1);
  }

  /**
   * The value of an unconnected port: an override if one was given, a context port yields the
   * context, a file widget resolves the path, an expression is evaluated, else the literal value.
   */
  getPortValue(nodePath: string, node: Node, port: Port): unknown {
    const overrideKey = `${node.name}.${port.name}`;
    const override = this.portOverrides[overrideKey];
    if (port.type === "context") return this;
    let value: unknown = override !== undefined ? override : port.value;
    if (port.expression && override === undefined && !this.functionHandlesExpressions(node)) {
      value = this.evaluatePortExpression(nodePath, port);
    }
    if (isFileWidget(port) && typeof value === "string" && value !== "") return this.resolvePath(value);
    return value;
  }

  private functionHandlesExpressions(node: Node): boolean {
    return this.functionRepository.findFunction(node.function)?.handlesExpressions === true;
  }

  evaluatePortExpression(nodePath: string, port: Port): unknown {
    const scope: Record<string, unknown> = {
      ...this.data,
      frame: this.frame,
      $FRAME: this.frame,
      $TIME: this.data.time ?? 0,
    };
    // Published values of the enclosing network, as NodeBox Live's `network.<param>`.
    const parentPath = nodePath.slice(0, nodePath.lastIndexOf("/")) || "/";
    const parent = this.nodeMap.get(parentPath);
    if (parent) {
      const network: Record<string, unknown> = {};
      for (const p of parent.inputs) network[p.name] = p.value;
      scope.network = network;
    }
    try {
      return evaluateExpression(port.expression!, scope);
    } catch (e) {
      throw new Error(`Expression '${port.expression}' on port ${port.name}: ${e instanceof Error ? e.message : e}`);
    }
  }

  private convertResultsForPort(port: Port, values: unknown[], sourceType: string | undefined): unknown[] {
    if (values.length === 0) return values;
    // A numeric source says whether its numbers were integers; otherwise integral values count as such.
    // A declared "int" port can still hand out doubles (a network whose child computes floats),
    // and a Java Long never has a fraction, so the values themselves get the last word.
    const allIntegers = values.every((v) => typeof v !== "number" || Number.isInteger(v));
    let numberHint: "int" | "float";
    if (sourceType === "float") numberHint = "float";
    else if (sourceType === "int") numberHint = allIntegers ? "int" : "float";
    else numberHint = allIntegers ? "int" : "float";
    const type = listType(values, numberHint);
    // A list of points going into a value-range geometry port is one argument: the point list.
    if (type === "point" && port.type === "geometry" && hasValueRange(port)) return [values];
    return convertValues(type, port.type, values);
  }

  private async invokeNode(nodePath: string, node: Node, argumentMap: ArgumentMap): Promise<NodeResults> {
    const args: unknown[] = [];
    for (const port of node.inputs) {
      if (argumentMap.has(port.name)) args.push(argumentMap.get(port.name));
      else if (hasValueRange(port)) args.push(this.getPortValue(nodePath, node, port));
      else args.push([]);
    }
    let fn: NodeFunction;
    try {
      fn = this.functionRepository.getFunction(node.function);
    } catch (e) {
      throw new NodeRenderError(nodePath, node, e);
    }
    let result: unknown;
    try {
      result = await fn.invoke(args, { context: this, node, nodePath });
    } catch (e) {
      if (e instanceof NodeRenderError) throw e;
      throw new NodeRenderError(nodePath, node, e);
    }
    return toResults(node, result);
  }
}

/** Wrap a function result into per-output lists. */
export class Outputs {
  values: Map<string, unknown>;
  constructor(values: Record<string, unknown> | Map<string, unknown>) {
    this.values = values instanceof Map ? values : new Map(Object.entries(values));
  }
}

function toResults(node: Node, result: unknown): NodeResults {
  const results: NodeResults = new Map();
  if (result instanceof Outputs) {
    for (const [name, value] of result.values) results.set(name, value === undefined ? [] : [value]);
    return results;
  }
  results.set(primaryOutputName(node), [result]);
  return results;
}

/** Apply the output range: list outputs flatten one level, value outputs wrap. */
function postProcess(node: Node, port: Port, raw: unknown[] | undefined): unknown[] {
  if (raw === undefined || raw.length === 0) return [];
  const out: unknown[] = [];
  const listRange = node.outputs.length > 0 ? hasListRange(port) : hasListOutputRange(node);
  for (const result of raw) {
    if (listRange) {
      if (Array.isArray(result)) out.push(...result);
      else if (result !== null && result !== undefined) out.push(result);
    } else if (Array.isArray(result)) {
      // A value-range node returning a list of points from a geometry output passes them through.
      if (result.length === 0) continue;
      if (listType(result) === "point" && port.type === "geometry") out.push(...result);
      else out.push(result);
    } else if (result !== null && result !== undefined) {
      out.push(result);
    }
  }
  return out;
}

/** Anything with an x and a y: the duck typing classic NodeBox Live used to spot a point. */
function isPointLike(value: unknown): boolean {
  return (
    typeof value === "object" &&
    value !== null &&
    (value as { x?: unknown }).x !== undefined &&
    (value as { y?: unknown }).y !== undefined
  );
}

function clampResultsForPort(port: Port, values: unknown[]): unknown[] {
  if (port.min === undefined && port.max === undefined) return values;
  return values.map((v) => clampValue(port, v as PortValue));
}

/**
 * The list-matching rule. Given {alpha: [1 2 3 4 5], beta: ["a" "b"], gamma: [true]} yields five
 * argument maps, beta cycling. A list-range port always contributes its whole list.
 */
/**
 * One argument map per invocation: the longest input decides how many there are, shorter inputs
 * cycle, a list-range port gets its whole list, and an empty input means no invocation at all.
 * A master port (classic NodeBox Live's `masterList`) sets the count instead of the longest input.
 */
export interface ArgumentMapOptions {
  /** The port whose length sets the number of invocations (classic NodeBox Live's `masterList`). */
  masterPort?: Port;
  /** List ports that receive their single value as it is, rather than as a list. */
  scalarPorts?: ReadonlySet<Port>;
  /** Value ports that receive their whole list as one argument, like a list port. */
  wholeListPorts?: ReadonlySet<Port>;
}

export function* buildArgumentMaps(
  argumentsPerPort: Map<Port, unknown[]>,
  options: ArgumentMapOptions = {},
): Generator<ArgumentMap> {
  const { masterPort, scalarPorts, wholeListPorts } = options;
  let minSize = Number.MAX_SAFE_INTEGER;
  let maxSize = 0;
  const whole = (port: Port) => hasListRange(port) || wholeListPorts?.has(port) === true;
  for (const [port, values] of argumentsPerPort) {
    const size = whole(port) ? 1 : values.length;
    minSize = Math.min(minSize, size);
    maxSize = Math.max(maxSize, size);
  }
  if (minSize === 0 || argumentsPerPort.size === 0) return;
  if (masterPort && argumentsPerPort.has(masterPort)) {
    maxSize = whole(masterPort) ? 1 : argumentsPerPort.get(masterPort)!.length;
  }
  for (let i = 0; i < maxSize; i++) {
    const map: ArgumentMap = new Map();
    for (const [port, values] of argumentsPerPort) {
      if (scalarPorts?.has(port)) map.set(port.name, values[0]);
      else map.set(port.name, whole(port) ? values : values[i % values.length]);
    }
    yield map;
  }
}

function memoKeyFor(networkPath: string, childName: string, argumentMap: ArgumentMap): string {
  if (argumentMap.size === 0) return `${networkPath}\u0000${childName}`;
  const parts: string[] = [];
  for (const [name, value] of argumentMap) parts.push(`${name}=${identity(value)}`);
  return `${networkPath}\u0000${childName}\u0000${parts.join("\u0001")}`;
}

// Object identities for memo keys: equal objects in the same render are the same instances.
const identities = new WeakMap<object, number>();
let nextIdentity = 1;
function identity(value: unknown): string {
  if (value === null || value === undefined) return "null";
  if (typeof value === "object" || typeof value === "function") {
    let id = identities.get(value as object);
    if (id === undefined) {
      id = nextIdentity++;
      identities.set(value as object, id);
    }
    return `#${id}`;
  }
  if (value instanceof Point) return value.toString();
  return `${typeof value}:${String(value)}`;
}

function defaultResolvePath(file?: string): (path: string) => string {
  return (path: string) => {
    if (!file || /^([a-zA-Z]:[\\/]|\/|[a-z]+:\/\/)/.test(path)) return path;
    const dir = file.replace(/[\\/][^\\/]*$/, "");
    return dir ? `${dir}/${path}` : path;
  };
}

export { valueType };
