import { Point } from "../graphics/point";
import { clonePort, createPort, isPublishedPort, portsEqual, withValue } from "./port";
import { Connection, Node, Port, PortRange, PortType, Sticky } from "./types";

export const NODE_NAME_PATTERN = /^[a-zA-Z_][a-zA-Z0-9_]{0,29}$/;
/** NodeBox Live names allow spaces; the unified model accepts both. */
export const RELAXED_NODE_NAME_PATTERN = /^[^\s/.][^/.]{0,63}$/;
export const RESERVED_NAMES = /^(node|network)$/;

/** The base node every other node extends, like nodebox.node.Node.ROOT. */
export function createRootNode(): Node {
  return {
    name: "node",
    prototype: null,
    comment: "",
    category: "",
    description: "Base node to be extended for custom nodes.",
    image: "node.png",
    function: "core/zero",
    position: Point.ZERO,
    inputs: [],
    outputType: "float",
    outputRange: "value",
    outputs: [],
    isNetwork: false,
    children: [],
    renderedChild: "",
    connections: [],
    handle: "",
    alwaysRendered: false,
    stickies: [],
    meta: {},
  };
}

/** An empty subnetwork, like nodebox.node.Node.NETWORK. */
export function createNetworkNode(): Node {
  const node = createRootNode();
  node.name = "network";
  node.prototype = "core.node";
  node.image = "network.png";
  node.description = "Create an empty subnetwork.";
  node.outputRange = "list";
  node.isNetwork = true;
  return node;
}

/** The nodes `<importCoreNode ref="ROOT|NETWORK"/>` refers to, and what "core.node" / "core.network" resolve to. */
export const CORE_NODES: Record<string, () => Node> = {
  ROOT: createRootNode,
  NETWORK: createNetworkNode,
};

/**
 * Create a node that extends the given prototype: all attributes and ports are copied, so the
 * prototype chain is flattened at load time. `prototypeId` is what gets written back to the file.
 */
export function extendNode(prototype: Node, prototypeId: string | null, name?: string): Node {
  return {
    ...prototype,
    name: name ?? prototype.name,
    prototype: prototypeId,
    position: prototype.position,
    inputs: prototype.inputs.map(clonePort),
    outputs: prototype.outputs.map(clonePort),
    // Networks start out with copies of the prototype's children so subnetwork templates work.
    children: prototype.children.map(cloneNode),
    connections: prototype.connections.map((c) => ({ ...c })),
    stickies: prototype.stickies.map((s) => ({ ...s })),
    meta: {},
  };
}

export function cloneNode(node: Node): Node {
  return {
    ...node,
    inputs: node.inputs.map(clonePort),
    outputs: node.outputs.map(clonePort),
    children: node.children.map(cloneNode),
    connections: node.connections.map((c) => ({ ...c })),
    stickies: node.stickies.map((s) => ({ ...s })),
    meta: { ...node.meta },
  };
}

export function validateName(name: string, relaxed = false): void {
  const pattern = relaxed ? RELAXED_NODE_NAME_PATTERN : NODE_NAME_PATTERN;
  if (!pattern.test(name)) throw new Error(`Invalid node name '${name}'.`);
  if (name.startsWith("__")) throw new Error(`Names starting with double underscore are reserved: '${name}'.`);
  if (RESERVED_NAMES.test(name)) throw new Error(`'${name}' is a reserved name.`);
}

const NUMBER_AT_THE_END = /^(.*?)(\d*)$/;

/** "rect1", "rect2", … the first name with the given prefix not in use. */
export function uniqueName(prefix: string, existingNames: Iterable<string>): string {
  const names = new Set(existingNames);
  const m = NUMBER_AT_THE_END.exec(prefix)!;
  const base = m[1];
  let counter = m[2] ? parseInt(m[2], 10) : 1;
  for (;;) {
    const suggested = `${base}${counter}`;
    if (!names.has(suggested)) return suggested;
    counter++;
  }
}

export function getChild(node: Node, name: string): Node | undefined {
  return node.children.find((c) => c.name === name);
}

export function hasChild(node: Node, name: string): boolean {
  return node.children.some((c) => c.name === name);
}

export function getInput(node: Node, name: string): Port | undefined {
  return node.inputs.find((p) => p.name === name);
}

export function hasInput(node: Node, name: string): boolean {
  return node.inputs.some((p) => p.name === name);
}

export function getOutput(node: Node, name?: string): Port | undefined {
  if (name === undefined || name === "output") return node.outputs[0] ?? implicitOutput(node);
  return node.outputs.find((p) => p.name === name);
}

/** NodeBox 3 nodes describe their single output with outputType/outputRange; present it as a port. */
export function implicitOutput(node: Node): Port {
  return createPort("output", node.outputType, { range: node.outputRange });
}

/** All outputs, with the implicit one synthesized for NodeBox 3 style nodes. */
export function outputPorts(node: Node): Port[] {
  return node.outputs.length > 0 ? node.outputs : [implicitOutput(node)];
}

export function primaryOutputName(node: Node): string {
  return node.outputs.length > 0 ? node.outputs[0].name : "output";
}

export function hasListOutputRange(node: Node): boolean {
  return node.outputRange === "list";
}

export function hasRenderedChild(node: Node): boolean {
  return node.renderedChild !== "" && hasChild(node, node.renderedChild);
}

export function getRenderedChild(node: Node): Node | undefined {
  return node.renderedChild ? getChild(node, node.renderedChild) : undefined;
}

export function setInputValue(node: Node, portName: string, value: unknown): void {
  const port = getInput(node, portName);
  if (!port) throw new Error(`Node ${node.name} has no input ${portName}.`);
  Object.assign(port, withValue(port, value));
  // A published port keeps the child port in sync, as NodeBox 3 does.
  if (node.isNetwork && isPublishedPort(port)) {
    const [childName, childPort] = splitReference(port.childReference!);
    const child = getChild(node, childName);
    if (child && hasInput(child, childPort)) setInputValue(child, childPort, value);
  }
}

export function splitReference(reference: string): [string, string] {
  const i = reference.indexOf(".");
  if (i < 0) throw new Error(`Invalid port reference '${reference}'.`);
  return [reference.slice(0, i), reference.slice(i + 1)];
}

export function addChild(node: Node, child: Node): Node {
  if (child.name === "root") throw new Error("A child node cannot be named 'root'.");
  if (hasChild(node, child.name))
    child.name = uniqueName(
      child.name,
      node.children.map((c) => c.name),
    );
  node.isNetwork = true;
  node.children.push(child);
  return child;
}

export function removeChild(node: Node, childName: string): void {
  node.children = node.children.filter((c) => c.name !== childName);
  node.connections = node.connections.filter((c) => c.outputNode !== childName && c.inputNode !== childName);
  node.inputs = node.inputs.filter((p) => !isPublishedPort(p) || splitReference(p.childReference!)[0] !== childName);
  node.outputs = node.outputs.map((p) =>
    p.childReference && splitReference(p.childReference)[0] === childName ? { ...p, childReference: undefined } : p,
  );
  if (node.renderedChild === childName) node.renderedChild = "";
}

export function renameChild(node: Node, oldName: string, newName: string): void {
  const child = getChild(node, oldName);
  if (!child) throw new Error(`No child named ${oldName}.`);
  if (oldName === newName) return;
  if (hasChild(node, newName)) throw new Error(`A child named ${newName} already exists.`);
  child.name = newName;
  for (const c of node.connections) {
    if (c.outputNode === oldName) c.outputNode = newName;
    if (c.inputNode === oldName) c.inputNode = newName;
  }
  for (const p of [...node.inputs, ...node.outputs]) {
    if (p.childReference && splitReference(p.childReference)[0] === oldName)
      p.childReference = `${newName}.${splitReference(p.childReference)[1]}`;
  }
  if (node.renderedChild === oldName) node.renderedChild = newName;
}

export function getConnection(node: Node, inputNode: string, inputPort: string): Connection | undefined {
  return node.connections.find((c) => c.inputNode === inputNode && c.inputPort === inputPort);
}

export function getConnectionsTo(node: Node, childName: string): Connection[] {
  return node.connections.filter((c) => c.inputNode === childName);
}

export function getConnectionsFrom(node: Node, childName: string): Connection[] {
  return node.connections.filter((c) => c.outputNode === childName);
}

/** Connect an output of one child to an input of another. Inputs take a single connection. */
export function connect(
  network: Node,
  outputNode: string,
  inputNode: string,
  inputPort: string,
  outputPort?: string,
): Connection {
  if (!hasChild(network, outputNode)) throw new Error(`Node ${outputNode} does not exist.`);
  const input = getChild(network, inputNode);
  if (!input) throw new Error(`Node ${inputNode} does not exist.`);
  if (!hasInput(input, inputPort)) throw new Error(`Node ${inputNode} has no input ${inputPort}.`);
  if (network.inputs.some((p) => p.childReference === `${inputNode}.${inputPort}`))
    throw new Error(`Port ${inputNode}.${inputPort} is published and cannot be connected.`);
  // A value port takes one connection; a list port collects them in the order they were made.
  const port = getInput(input, inputPort);
  if (!port || port.range !== "list")
    network.connections = network.connections.filter((c) => !(c.inputNode === inputNode && c.inputPort === inputPort));
  const connection: Connection = { outputNode, inputNode, inputPort };
  if (outputPort !== undefined && outputPort !== "output") connection.outputPort = outputPort;
  network.connections.push(connection);
  return connection;
}

export function disconnect(network: Node, inputNode: string, inputPort?: string): void {
  network.connections = network.connections.filter(
    (c) => !(c.inputNode === inputNode && (inputPort === undefined || c.inputPort === inputPort)),
  );
}

export function isConnected(network: Node, childName: string, portName?: string): boolean {
  return network.connections.some(
    (c) =>
      (c.inputNode === childName && (portName === undefined || c.inputPort === portName)) ||
      (portName === undefined && c.outputNode === childName),
  );
}

/** Make a child's input available on the network itself. Disconnects the child port first. */
export function publish(network: Node, childName: string, childPortName: string, publishedName: string): Port {
  const child = getChild(network, childName);
  if (!child) throw new Error(`Node ${childName} does not exist.`);
  const childPort = getInput(child, childPortName);
  if (!childPort) throw new Error(`Node ${childName} has no input ${childPortName}.`);
  if (hasInput(network, publishedName)) throw new Error(`Port ${publishedName} already exists on the network.`);
  const reference = `${childName}.${childPortName}`;
  if (network.inputs.some((p) => p.childReference === reference)) throw new Error(`${reference} is already published.`);
  disconnect(network, childName, childPortName);
  const port: Port = { ...clonePort(childPort), name: publishedName, label: "", childReference: reference };
  network.inputs.push(port);
  return port;
}

export function unpublish(network: Node, publishedName: string): void {
  network.inputs = network.inputs.filter((p) => p.name !== publishedName);
}

export function getPublishedPorts(node: Node): Port[] {
  return node.inputs.filter(isPublishedPort);
}

export function getPortByChildReference(node: Node, childName: string, childPortName: string): Port | undefined {
  const reference = `${childName}.${childPortName}`;
  return node.inputs.find((p) => p.childReference === reference);
}

/** Add an output port to a network that forwards a child's output (a NodeBox Live outlet). */
export function addOutput(
  node: Node,
  name: string,
  type: PortType,
  range: PortRange = "value",
  childReference?: string,
): Port {
  const port = createPort(name, type, { range, childReference });
  node.outputs.push(port);
  return port;
}

export function addSticky(node: Node, sticky: Sticky): void {
  node.stickies.push(sticky);
}

/** Whether values of one type can flow into a port of another (NodeBox 3's Node.isCompatible). */
export function isCompatible(outputType: PortType, inputType: PortType): boolean {
  if (outputType === inputType) return true;
  if (inputType === "string") return true;
  if (outputType === "int" && inputType === "float") return true;
  if (outputType === "float" && inputType === "int") return true;
  if ((outputType === "int" || outputType === "float") && inputType === "point") return true;
  return false;
}

/** Whether a port differs from the same port on the prototype (decides what the .ndbx writer emits). */
export function portDiffersFromPrototype(port: Port, prototypePort: Port | undefined): boolean {
  return prototypePort === undefined || !portsEqual(port, prototypePort);
}

/** The absolute path of a child within a network path: "/" + "rect1" -> "/rect1". */
export function childPath(networkPath: string, childName: string): string {
  return networkPath.endsWith("/") ? `${networkPath}${childName}` : `${networkPath}/${childName}`;
}
