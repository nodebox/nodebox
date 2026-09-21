import {
  Project,
  Item,
  Network,
  Node,
  Port,
  PortValue,
  Parameter,
  Connection,
  ConnectionType,
  NetworkItem,
  ParameterValue,
  LiteralValue,
  NodeToOutletConnection,
} from "./types";
import Context from "./context";

export function findItemById(project: Project, id: string): Item | undefined {
  return project.items.find((item) => item.id === id);
}

export function findItemByName(project: Project, name: string): Item | undefined {
  return project.items.find((item) => item.name === name);
}

export function findNetworkItemById(network: Network, id: string): NetworkItem | undefined {
  return network.children.find((item) => item.id === id);
}

export function findNodeById(network: Network, id: string): Node | undefined {
  return network.children.find((child) => child.type === "NODE" && child.id === id) as Node;
}

export function findNodeByName(network: Network, name: string): Node | undefined {
  return network.children.find((child) => child.type === "NODE" && (child as Node).name === name) as Node;
}

export function findInputPort(cx: Context, node: Node, portName: string): Port | undefined {
  const fn = cx.lookupItemByName(node.fn);
  if (!fn) return undefined;
  const port = fn.inputPorts.find((p) => p.name === portName);
  return port;
}

export function findOutputPort(cx: Context, node: Node, portName: string): Port | undefined {
  const fn = cx.lookupItemByName(node.fn);
  if (!fn) return undefined;
  const port = fn.outputPorts.find((p) => p.name === portName);
  return port;
}

export function findParameter(cx: Context, node: Node, parameterName: string): Parameter | undefined {
  const fn = cx.lookupItemByName(node.fn);
  if (!fn) return undefined;
  const parameter = fn.parameters.find((p) => p.name === parameterName);
  return parameter;
}

export function findInputConnection(network: Network, inNode: Node, inPort: string): Connection | undefined {
  return network.connections.find(
    (c) =>
      (c.type === ConnectionType.InletToNode || c.type === ConnectionType.NodeToNode) &&
      c.inNode === inNode.id &&
      c.inPort === inPort,
  );
}

export function findOutputConnectionsForNode(network: Network, outNode: Node): Connection[] {
  return network.connections.filter(
    (c) => (c.type === ConnectionType.NodeToNode || c.type === ConnectionType.NodeToOutlet) && c.outNode === outNode.id,
  );
}

export function findOutputConnections(network: Network, outNode: Node, outPort: string): Connection[] {
  return network.connections.filter(
    (c) =>
      (c.type === ConnectionType.NodeToNode || c.type === ConnectionType.NodeToOutlet) &&
      c.outNode === outNode.id &&
      c.outPort === outPort,
  );
}

export function findOutletConnection(network: Network, outlet: string): NodeToOutletConnection | undefined {
  return network.connections.find(
    (c) => c.type === ConnectionType.NodeToOutlet && c.outlet === outlet,
  ) as NodeToOutletConnection;
}

export function findRenderedNodeOutputs(cx: Context, network: Network): Map<string, PortValue> {
  const renderedNode = network.children.find((node) => node.id === network.renderedNode);
  if (!renderedNode) {
    return new Map();
  }
  console.assert(renderedNode.type === "NODE");
  const runtimeNode = cx.runtimeNodes.get(renderedNode.id);
  const result = new Map<string, PortValue>();
  if (!runtimeNode) {
    return new Map();
  }
  runtimeNode.outputPorts.forEach((port) => {
    const portValue = cx.portValues.get(`${renderedNode.id}/${port.name}`);
    if (typeof portValue !== "undefined") {
      result.set(port.name, portValue);
    }
  });
  return result;
}

export function getParameterValue(cx: Context, node: Node, parameterName: string): ParameterValue | undefined {
  const result = node.values?.[parameterName];
  if (typeof result !== "undefined") {
    return result;
  }
  const item = cx.lookupItemByName(node.fn);
  if (!item) return undefined;
  const parameter = item.parameters.find((p) => p.name === parameterName);
  if (!parameter) {
    throw new Error(`Parameter not found: ${parameterName}`);
  }
  return { type: "VALUE", value: parameter.defaultValue };
}

export function getValue(cx: Context, node: Node, parameterName: string): LiteralValue | undefined {
  const result = node.values?.[parameterName];
  if (typeof result !== "undefined") {
    if (result.type === "VALUE") {
      return result.value;
    } else {
      throw new Error(`Expression evaluation not implemented`);
    }
  }
  const item = cx.lookupItemByName(node.fn);
  if (!item) {
    return undefined;
  } else {
    const parameter = item.parameters.find((p) => p.name === parameterName);
    if (!parameter) {
      throw new Error(`Parameter not found: ${parameterName}`);
    }
    return parameter.defaultValue;
  }
}

export function getNetworkOutput(cx: Context, network: Network, portName: string): PortValue | undefined {
  const portValue = cx.portValues.get(`${network.id}/${portName}`);
  return portValue;
}
