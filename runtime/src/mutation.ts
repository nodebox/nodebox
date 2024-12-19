import {
  Project,
  Item,
  Network,
  Node,
  Port,
  PortType,
  ParameterValue,
  ExpressionValue,
  Inlet,
  Outlet,
  ConnectionType,
  NodeToNodeConnection,
  InletToNodeConnection,
  NodeToOutletConnection,
  Point,
  FunctionItem,
  Sticky,
  NetworkItem,
} from "./types";
import Context from "./context";
import { generateUniqueName } from "./identifiers";
import { findNodeById, findInputPort, findOutputPort, findOutputConnectionsForNode } from "./queries";
import { CURRENT_FORMAT_VERSION } from "./loaders";

const FUNCTION_SOURCE_TEMPLATE = `/**
 * Function Template
 *
 * Use this as a demonstration of a function.
 * @category Graphics
 */

import { Rect, Paint } from "@ndbx/g";

export default function (node) {
  const shapeOut = node.shapeOut({ name: "out" });

  node.onRender = () => {
    const rect = new Rect(0, 0, 100, 100);
    rect.fill = Paint.solid(1, 0, 0);
    shapeOut.set(rect);
  };
}
`;

class Rect {
  x: number;
  y: number;
  width: number;
  height: number;
  constructor(x: number, y: number, width: number, height: number) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  containsPoint(x: number, y: number) {
    return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height;
  }

  grow(dx: number, dy: number) {
    this.x -= dx;
    this.y -= dy;
    this.width += 2 * dx;
    this.height += 2 * dy;
  }

  union(other: Rect) {
    const x = Math.min(this.x, other.x);
    const y = Math.min(this.y, other.y);
    const width = Math.max(this.x + this.width, other.x + other.width) - x;
    const height = Math.max(this.y + this.height, other.y + other.height) - y;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }
}

export function createProject(title: string): Project {
  return {
    __gallery: undefined,
    id: "xxx",
    formatVersion: CURRENT_FORMAT_VERSION,
    title,
    dependencies: {},
    assets: {},
    items: [],
  };
}

export function createNetwork(cx: Context, project: Project, name: string): Network {
  const network = {
    type: "NETWORK",
    id: cx.generateId(),
    name,
    category: "",
    description: "",
    canvasSize: "fixed",
    padding: 10,
    width: 1000,
    height: 1000,
    background: { r: 0.15, g: 0.15, b: 0.15, a: 1 },
    children: [],
    connections: [],
    renderedNode: null,
    inputPorts: [],
    outputPorts: [],
    parameters: [],
    sections: [],
  } as Network;
  project.items.push(network);
  return network;
}

export function createFunction(cx: Context, project: Project, name: string): FunctionItem {
  const item = {
    type: "FUNCTION",
    id: cx.generateId(),
    name,
    category: "",
    description: "",
    width: 1000,
    height: 1000,
    background: { r: 0.15, g: 0.15, b: 0.15, a: 1 },
    inputPorts: [],
    outputPorts: [],
    parameters: [],
    sections: [],
    source: FUNCTION_SOURCE_TEMPLATE,
  } as FunctionItem;
  project.items.push(item);
  return item;
}

export function getNodes(network: Network): Node[] {
  return network.children.filter((child) => child.type === "NODE") as Node[];
}

export function createSticky(cx: Context, network: Network): Sticky {
  const sticky: Sticky = {
    id: cx.generateId(),
    type: "STICKY",
    x: 0,
    y: 0,
    width: 190,
    height: 110,
    backgroundColor: {
      r: 251 / 255,
      g: 232 / 255,
      b: 143 / 255,
      a: 0.8,
    },
    text: "Double click to edit.",
    fontSize: 18,
    fontColor: {
      r: 253 / 255,
      g: 246 / 255,
      b: 211 / 255,
      a: 1,
    },
  };
  network.children.push(sticky);
  return sticky;
}

export function createNode(cx: Context, network: Network, fn: string, position?: Point): Node | undefined {
  const item = cx.lookupItemByName(fn);
  if (!item) return;
  const name = generateUniqueName(
    item.name,
    getNodes(network).map((node) => node.name),
  );
  const node = {
    type: "NODE",
    id: cx.generateId(),
    x: position?.x ?? 0,
    y: position?.y ?? 0,
    name,
    fn,
    values: {},
  } as Node;
  network.children.push(node);
  return node;
}
// (cx.value as Context, draftNetwork, node, fn)
export function replaceNode(cx: Context, network: Network, node: Node, fn: string): Node | undefined {
  const item = cx.lookupItemByName(fn);
  if (!item) return;
  const name = generateUniqueName(
    item.name,
    getNodes(network).map((node) => node.name),
  );
  const nodeToUpdate = network.children.find((c) => c.id === node.id) as Node;
  if (nodeToUpdate) {
    nodeToUpdate.fn = fn;
    nodeToUpdate.name = name;
  }
  markDirty(cx, network, nodeToUpdate);
  return node;
}

export function createOutlet(cx: Context, network: Network, name: string, type: PortType, position?: Point): Outlet {
  if (network.outputPorts?.some((port) => port.name === name)) {
    throw new Error(`Output port with name ${name} already exists.`);
  }
  const outlet: Outlet = {
    type: "OUTLET",
    id: cx.generateId(),
    x: position?.x ?? 0,
    y: position?.y ?? 0,
    portName: name,
    portType: type,
  };
  network.children.push(outlet);
  if (!network.outputPorts) {
    network.outputPorts = [];
  }
  network.outputPorts.push({ name, type });
  return outlet;
}

export function setValue(cx: Context, network: Network, node: Node, parameterName: string, value: ParameterValue) {
  if (node.values === undefined) {
    node.values = {};
  }
  node.values[parameterName] = value;
  markDirty(cx, network, node);
}

export function deleteItems(cx: Context, network: Network, ids: string[]) {
  const childrenToDelete = network.children.filter((child) => ids.includes(child.id));
  const outletsToDelete = childrenToDelete.filter((child) => child.type === "OUTLET") as Outlet[];
  const outletNames = outletsToDelete.map((outlet) => outlet.portName);
  network.children = network.children.filter((child) => !ids.includes(child.id));
  const connectionsToDelete = network.connections.filter(
    (c) =>
      (c.type === ConnectionType.NodeToNode && (ids.includes(c.inNode) || ids.includes(c.outNode))) ||
      (c.type === ConnectionType.InletToNode && ids.includes(c.inNode)) ||
      (c.type === ConnectionType.NodeToOutlet && (ids.includes(c.outNode) || outletNames.includes(c.outlet))),
  );
  network.connections = network.connections.filter((c) => !connectionsToDelete.includes(c));
  if (network.renderedNode && ids.includes(network.renderedNode)) {
    network.renderedNode = null;
  }
  for (const conn of connectionsToDelete) {
    if (conn.type === ConnectionType.NodeToNode) {
      const node = findNodeById(network, conn.inNode);
      markDirty(cx, network, node);
    }
  }
  network.outputPorts = network.outputPorts.filter((port) => !outletNames.includes(port.name));
}

export function duplicateItems(cx: Context, network: Network, ids: string[]): string[] {
  if (ids.length === 0) return [];
  const items = network.children.filter((child) => ids.includes(child.id));
  // Internal connections are connections between selected nodes.
  // Both inNode and outNode should be replaced by the newly duplicated nodes.
  const internalConnections = network.connections.filter((c) => {
    return c.type === ConnectionType.NodeToNode && ids.includes(c.outNode) && ids.includes(c.inNode);
  }) as NodeToNodeConnection[];
  // Input connections are connections between outside nodes and selected nodes.
  // The inNode should be replaced by the newly duplicated node.
  const inputConnections = network.connections.filter((c) => {
    return c.type === ConnectionType.NodeToNode && !ids.includes(c.outNode) && ids.includes(c.inNode);
  }) as NodeToNodeConnection[];
  return addItemsAndConnections(cx, network, items, internalConnections, inputConnections);
}

export function addItemsAndConnections(
  cx: Context,
  network: Network,
  items: NetworkItem[],
  internalConnections: NodeToNodeConnection[],
  inputConnections?: NodeToNodeConnection[],
) {
  const newIds = new Map<string, string>();
  if (items) {
    for (const item of items) {
      if (item.type === "NODE") {
        const node = item as Node;
        const newNode = createNode(cx, network, node.fn, { x: node.x + 10, y: node.y + 10 });
        if (newNode) {
          newIds.set(node.id, newNode.id);
          if (node.values) {
            for (const [key, value] of Object.entries(node.values)) {
              setValue(cx, network, newNode, key, value);
            }
          }
        }
      } else if (item.type === "STICKY") {
        const sticky = item as Sticky;
        const newSticky = createSticky(cx, network);
        newIds.set(sticky.id, newSticky.id);
        newSticky.x = sticky.x + 10;
        newSticky.y = sticky.y + 10;
        newSticky.backgroundColor = sticky.backgroundColor;
        newSticky.text = sticky.text;
        newSticky.fontSize = sticky.fontSize;
        newSticky.fontColor = sticky.fontColor;
        newSticky.height = sticky.height;
        newSticky.width = sticky.width;
      }
    }
    // Update internal connections to point to the new nodes.
    if (internalConnections)
      for (const conn of internalConnections) {
        if (conn.type == ConnectionType.NodeToNode) {
          const newInNodeId = newIds.get(conn.inNode)!;
          const newOutNodeId = newIds.get(conn.outNode)!;
          network.connections.push({
            type: ConnectionType.NodeToNode,
            outNode: newOutNodeId,
            outPort: conn.outPort,
            inNode: newInNodeId,
            inPort: conn.inPort,
          });
        }
      }
    if (inputConnections)
      for (const conn of inputConnections) {
        const newInNodeId = newIds.get(conn.inNode)!;
        network.connections.push({
          type: ConnectionType.NodeToNode,
          outNode: conn.outNode,
          outPort: conn.outPort,
          inNode: newInNodeId,
          inPort: conn.inPort,
        });
      }
  }
  return Array.from(newIds.values());
}

export function connectNodeToNode(
  cx: Context,
  network: Network,
  outNode: Node,
  outPort: Port,
  inNode: Node,
  inPort: Port,
) {
  // Check if the input port is already connected; disconnect it if so.
  const existingConnection = network.connections.find(
    (c) =>
      (c.type === ConnectionType.NodeToNode || c.type === ConnectionType.InletToNode) &&
      c.inNode === inNode.id &&
      c.inPort === inPort.name,
  );
  if (existingConnection) {
    network.connections = network.connections.filter((c) => c !== existingConnection);
  }

  network.connections.push({
    type: ConnectionType.NodeToNode,
    outNode: outNode.id,
    outPort: outPort.name,
    inNode: inNode.id,
    inPort: inPort.name,
  });

  markDirty(cx, network, inNode);
}

export function connectInletToNode(network: Network, inlet: Inlet, inNode: Node, inPort: Port) {
  const existingConnection = network.connections.find(
    (c) => c.type === ConnectionType.InletToNode && c.inNode === inNode.id && c.inPort === inPort.name,
  );
  if (existingConnection) {
    network.connections = network.connections.filter((c) => c !== existingConnection);
  }

  network.connections.push({
    type: ConnectionType.InletToNode,
    inlet: inlet.portName,
    inNode: inNode.id,
    inPort: inPort.name,
  });
}

export function connectNodeToOutlet(network: Network, outNode: Node, outPort: Port, outlet: Outlet) {
  const existingConnection = network.connections.find(
    (c) => c.type === ConnectionType.NodeToOutlet && c.outlet === outlet.portName,
  );
  if (existingConnection) {
    network.connections = network.connections.filter((c) => c !== existingConnection);
  }

  network.connections.push({
    type: ConnectionType.NodeToOutlet,
    outNode: outNode.id,
    outPort: outPort.name,
    outlet: outlet.portName,
  });
}

export function disconnect(cx: Context, network: Network, inputNodeId: string, inputPortName: string) {
  network.connections = network.connections.filter(
    (c) =>
      !(
        (c.type === ConnectionType.InletToNode || c.type === ConnectionType.NodeToNode) &&
        c.inNode === inputNodeId &&
        c.inPort === inputPortName
      ),
  );

  cx.portValues.delete(`${inputNodeId}/${inputPortName}`);
  markDirty(cx, network, findNodeById(network, inputNodeId));
}

export function setRenderedNode(cx: Context, network: Network, node: Node) {
  network.renderedNode = node.id;
  // FIXME: cx is currently not used, but this should trigger a dependency update (dirty propagation)
  cx = cx;
}

export function publishPortToOutlet(
  cx: Context,
  network: Network,
  node: Node,
  port: Port,
  outletName?: string,
): Outlet {
  outletName = outletName ?? port.name;
  const outlet = createOutlet(cx, network, outletName, port.type);
  network.connections.push({
    type: ConnectionType.NodeToOutlet,
    outNode: node.id,
    outPort: port.name,
    outlet: outlet.portName,
  });
  return outlet;
}

export function groupIntoNetwork(
  cx: Context,
  project: Project,
  network: Network,
  ids: string[],
  newNetworkName: string,
) {
  if (ids.length === 0) return;

  const items = network.children.filter((child) => ids.includes(child.id));
  if (items.find((item) => item.type === "INLET" || item.type === "OUTLET")) {
    console.warn("Cannot group inlets or outlets.");
    return;
  }

  // Internal connections are connections between selected nodes.
  const internalConnections = network.connections.filter((c) => {
    return c.type === ConnectionType.NodeToNode && ids.includes(c.outNode) && ids.includes(c.inNode);
  }) as NodeToNodeConnection[];
  // Input connections are connections between nodes in the original network and selected nodes.
  // They should be converted to inlets.
  const inputConnections = network.connections.filter((c) => {
    return c.type === ConnectionType.NodeToNode && !ids.includes(c.outNode) && ids.includes(c.inNode);
  }) as NodeToNodeConnection[];
  // Output connections are connections between selected nodes and nodes in the original network.
  // They should be converted to outlets.
  const outputConnections = network.connections.filter((c) => {
    return c.type === ConnectionType.NodeToNode && ids.includes(c.outNode) && !ids.includes(c.inNode);
  }) as NodeToNodeConnection[];

  // Create a new network.
  const newNetwork = createNetwork(cx, project, newNetworkName);

  // Add the items.
  // const nodes = nodeNames.map((name) => findLocalNode(network, name));
  // Find the top-left item and shift all items so that it is at (30, 100).
  const firstItem = items[0];
  const itemBounds = new Rect(firstItem.x, firstItem.y, 110, 30);
  items.forEach((item) => {
    itemBounds.union(new Rect(item.x, item.y, 110, 30));
  });
  const dx = -itemBounds.x + 30;
  const dy = -itemBounds.y + 100;

  newNetwork.children = items.map((item) => ({ ...item, x: item.x + dx, y: item.y + dy }));

  // Add the internal connections.
  newNetwork.connections = internalConnections;

  // Add the input connections as inlets.
  // - Create a new input port on the network that can serve as the input connection.
  // - Create a new Inlet in the network.
  // - Create a connection between the inlet and the node.
  // The inlet map is used to ensure that inlets are only created once. It is keyed on the ID of the output node and its port.
  const inletMap = new Map<string, Inlet>();
  inputConnections.forEach((c) => {
    const inletKey = `${c.outNode}:${c.outPort}`;
    let inlet: Inlet;
    if (!inletMap.has(inletKey)) {
      const node = findNodeById(network, c.inNode)!;
      const port = findInputPort(cx, node, c.inPort)!;
      const portName = generateUniqueName(
        port.name,
        newNetwork.inputPorts.map((p) => p.name),
      );
      const newPort: Port = {
        name: portName,
        type: port.type,
      };
      newNetwork.inputPorts.push(newPort);
      inlet = { id: cx.generateId(), type: "INLET", x: 30 + inletMap.size * 150, y: 30, portName, portType: port.type };
      newNetwork.children.push(inlet);
      inletMap.set(inletKey, inlet);
    } else {
      inlet = inletMap.get(inletKey)!;
    }
    const conn: InletToNodeConnection = {
      type: ConnectionType.InletToNode,
      inlet: inlet.id,
      inNode: c.inNode,
      inPort: c.inPort,
    };
    newNetwork.connections.push(conn);
  });

  // Add the output connections as outlets.
  // - Create a new output port on the network that can serve as the output connection.
  // - Create a new Outlet in the network.
  // - Create a connection between the network and the outlet.
  const outletMap = new Map<string, Outlet>();
  outputConnections.forEach((c) => {
    const outletKey = `${c.inNode}:${c.inPort}`;
    let outlet: Outlet;
    if (!outletMap.has(outletKey)) {
      const node = findNodeById(network, c.outNode)!;
      const port = findOutputPort(cx, node, c.outPort)!;
      const portName = generateUniqueName(
        port.name,
        newNetwork.outputPorts.map((p) => p.name),
      );
      const newPort: Port = {
        name: portName,
        type: port.type,
      };
      newNetwork.outputPorts.push(newPort);
      outlet = {
        id: cx.generateId(),
        type: "OUTLET",
        x: 30 + outletMap.size * 150,
        y: itemBounds.height + 150,
        portName,
        portType: port.type,
      };
      newNetwork.children.push(outlet);
      outletMap.set(outletKey, outlet);
    } else {
      outlet = outletMap.get(outletKey)!;
    }
    const conn: NodeToOutletConnection = {
      type: ConnectionType.NodeToOutlet,
      outNode: c.outNode,
      outPort: c.outPort,
      outlet: outlet.id,
    };
    newNetwork.connections.push(conn);
  });

  // Delete the old items and connections.
  network.children = network.children.filter((item) => !ids.includes(item.id));
  // network.connections = network.connections.filter((c) => c.output && !nodeNames.includes(c.output));
  // network.connections = network.connections.filter((c) => !nodeNames.includes(c.input));
  network.connections = network.connections.filter((c) => {
    if (c.type === ConnectionType.NodeToNode) {
      return !ids.includes(c.outNode) && !ids.includes(c.inNode);
    } else if (c.type === ConnectionType.InletToNode) {
      return !ids.includes(c.inNode);
    } else if (c.type === ConnectionType.NodeToOutlet) {
      return !ids.includes(c.outNode);
    }
  });

  // Add the new network to the old network.
  project.items.push(newNetwork);
  const fnName = `self/self/${newNetworkName}`;
  const node: Node = {
    id: cx.generateId(),
    type: "NODE",
    x: itemBounds.x,
    y: itemBounds.y,
    name: newNetworkName,
    fn: fnName,
    values: {},
  };
  network.children.push(node);

  // Add input connections
  inputConnections.forEach((c) => {
    const inlet = inletMap.get(`${c.outNode}:${c.outPort}`)!;
    const conn: NodeToNodeConnection = {
      type: ConnectionType.NodeToNode,
      outNode: c.outNode,
      outPort: c.outPort,
      inNode: node.id,
      inPort: inlet.portName,
    };
    network.connections.push(conn);
  });

  // Add output connections
  outputConnections.forEach((c) => {
    const outlet = outletMap.get(`${c.inNode}:${c.inPort}`)!;
    const conn: NodeToNodeConnection = {
      type: ConnectionType.NodeToNode,
      outNode: node.id,
      outPort: outlet.portName,
      inNode: c.inNode,
      inPort: c.inPort,
    };
    network.connections.push(conn);
  });

  // Set the rendered node.
  if (outputConnections.length >= 1) {
    // If there is an output connection, make that the rendered node in the new network.
    newNetwork.renderedNode = outputConnections[0].outNode!;
  } else if (network.renderedNode && ids.includes(network.renderedNode)) {
    // If the selection includes the rendered node, make that the rendered node in the new network.
    // Also set the rendered node of the old network to the name of the new network, since that replaces the rendered node.
    newNetwork.renderedNode = network.renderedNode;
    network.renderedNode = node.id;
  } else {
    // If there are no outputs connected, and no rendered node set, just pick the first node.
    newNetwork.renderedNode = ids[0];
  }
}

/**
 * Mark this node, and all downstream nodes, dirty.
 * @param cx The context.
 * @param network The network.
 * @param node The node to mark dirty. If undefined, does nothing.
 */
export function markDirty(cx: Context, network: Network, node: Node | undefined) {
  if (!node) return;
  const runtimeNode = cx.runtimeNodes.get(node.id);
  if (runtimeNode) {
    runtimeNode.dirty = true;
  }
  const connections = findOutputConnectionsForNode(network, node);
  connections.forEach((c) => {
    if (c.type === ConnectionType.NodeToNode) {
      const node = findNodeById(network, c.inNode);
      if (node) {
        markDirty(cx, network, node);
      }
    } else {
      // FIXME: Wherever the network is used, find its runtime node and mark it as dirty.
    }
  });
}

export function markProjectDirty(cx: Context, project: Project) {
  project.items.forEach((item) => {
    if (item.type === "NETWORK") {
      const network = item;
      network.children.forEach((child) => {
        if (child.type === "NODE") {
          const node = child;
          markDirty(cx, network, node as Node);
        }
      });
    }
  });
}

export function markFunctionDirty(cx: Context, project: Project, fqId: string) {
  cx.initializers.delete(fqId);
  cx.runtimeNodes.delete(fqId);
  project.items.forEach((item) => {
    if (item.type === "NETWORK") {
      const network = item;
      network.children.forEach((child) => {
        if (child.type === "NODE") {
          const node = child as Node;
          if (node.fn === fqId) {
            cx.runtimeNodes.delete(node.id);
            markDirty(cx, network, node as Node);
          }
        }
      });
    }
  });
}

export function markItemParameterDirty(cx: Context, item: Item, parameterName: string) {
  // Note that this doesn't set the value on the network!
  // This function is here to mark all nodes dirty that use this value from the network.
  if (item.type !== "NETWORK") return;
  const network = item as Network;
  for (const item of network.children) {
    if (item.type !== "NODE") continue;
    const node = item as Node;
    if (!node.values) continue;
    for (const value of Object.values(node.values)) {
      if (value.type === "EXPRESSION") {
        const expr = value as ExpressionValue;
        if (expr.expression.includes(`network.${parameterName}`)) {
          markDirty(cx, network, node);
          break;
        }
      }
    }
  }
}
