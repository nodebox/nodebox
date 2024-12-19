import {
  Item,
  Network,
  Node,
  ConnectionType,
  FunctionItem,
  ParameterValue,
  LiteralValue,
  ContextGlobals,
} from "./types";
import Context from "./context";
import RuntimeNode, { createExpressionContext } from "./runtime-node";
import { findInputConnection, findOutletConnection } from "./queries";
import { loadItem } from "./loaders";

export async function evaluateItem(
  cx: Context,
  fqName: string,
  item: Item,
  values?: Record<string, LiteralValue>,
): Promise<RuntimeNode | undefined | null> {
  if (item.type === "NETWORK") {
    return await evaluateNetwork(cx, item as Network, values);
  } else if (item.type === "FUNCTION") {
    return await evaluateFunction(cx, fqName, values);
  } else {
    throw new Error(`Invalid item: ${item}`);
  }
}

export async function evaluateNetwork(
  cx: Context,
  network: Network,
  values?: Record<string, LiteralValue>,
): Promise<RuntimeNode | null> {
  let toVisit = [];
  const renderedNode = network.children.find((node) => node.id === network.renderedNode);
  if (renderedNode) {
    toVisit.push(renderedNode as Node);
    console.assert(renderedNode.type === "NODE");
  }
  if (Array.isArray(network.outputPorts)) {
    for (const outPort of network.outputPorts) {
      const conn = findOutletConnection(network, outPort.name);
      if (conn) {
        const outNode = network.children.find((n) => n.id === conn.outNode) as Node;
        if (outNode) {
          toVisit.push(outNode);
        }
      }
    }
  }
  if (toVisit.length === 0) {
    return null;
  }

  const visitedNodes = new Set<string>();
  const visitNode = async (node: Node): Promise<any> => {
    const nodeFn = cx.lookupItemByName(node.fn);
    if (!nodeFn) return;
    if (visitedNodes.has(node.id)) {
      return;
    }
    visitedNodes.add(node.id);
    for (const port of nodeFn.inputPorts) {
      const conn = findInputConnection(network, node, port.name);
      if (!conn) {
        continue;
      }
      if (conn.type === ConnectionType.InletToNode) {
        const inlet = network.inputPorts.find((p) => p.name === conn.inlet);
        if (!inlet) {
          throw new Error(`Inlet not found: ${conn.inlet}`);
        }
        // FIXME: What do we do here? Do we get the value from the calling network?
        throw new Error(`Unimplemented: Inlet value`);
      } else {
        const outNode = network.children.find((n) => n.id === conn.outNode) as Node;
        if (!outNode) {
          throw new Error(`Output node not found: ${conn.outNode}`);
        }
        await visitNode(outNode);
        const portValue = cx.portValues.get(`${conn.outNode}/${conn.outPort}`);
        if (portValue) {
          cx.portValues.set(`${node.id}/${port.name}`, portValue);
        }
      }
    }
    await evaluateNode(cx, node, createExpressionContext(cx, { network, values }));
  };

  for (const node of toVisit) {
    await visitNode(node);
  }
  if (Array.isArray(network.outputPorts)) {
    for (const outPort of network?.outputPorts) {
      const conn = findOutletConnection(network, outPort.name);
      if (!conn) {
        continue;
      }
      const portValue = cx.portValues.get(`${conn.outNode}/${conn.outPort}`);
      if (portValue) {
        cx.portValues.set(`${network.id}/${outPort.name}`, portValue);
      }
    }
  }
  if (renderedNode) {
    return cx.runtimeNodes.get(renderedNode.id)!;
  } else {
    return null;
  }
}

async function evaluateNode(cx: Context, node: Node, globals: ContextGlobals) {
  // The given node is the simple JSON-serialized node that just contains the node ID and values.
  // In order to run this node, we need an "initialized" node that contains the onRender function.
  // Let's check if we have that node by looking in the `cx.runtimeNodes` map.
  let runtimeNode = cx.runtimeNodes.get(node.id);
  if (!runtimeNode) {
    runtimeNode = await createRuntimeNodeForItem(cx, node.id, node.fn);
    if (runtimeNode !== undefined) cx.runtimeNodes.set(node.id, runtimeNode);
  }
  if (runtimeNode && runtimeNode.dirty) {
    runtimeNode.values = structuredClone(node.values || {});
    runtimeNode.globals = globals;
    await runtimeNode.onRender(cx);
    runtimeNode.dirty = runtimeNode.timeDependent ? true : false;
    runtimeNode.outputPorts.forEach((port) => {
      cx.portValues.set(`${node.id}/${port.name}`, port._value);
    });
  }
}

/**
 * Given a fully qualified item name (`userId/projectId/itemName`), create a runtime node for that item.
 * @param {Context cx The context object.
 * @param {string} nodeId The id of the node. Used for looking up port values in the context.
 * @param {string} fqName The fully qualified item name.
 * @returns {RuntimeNode} The runtime node for the given item.
 */
export async function createRuntimeNodeForItem(
  cx: Context,
  nodeId: string,
  fqName: string,
): Promise<RuntimeNode | undefined> {
  // We do this by looking up the function item in the project and then loading the module.
  const nodeFn = cx.lookupItemByName(fqName);
  if (!nodeFn) return undefined;
  if (nodeFn.type === "FUNCTION") {
    await loadItem(cx, nodeFn, fqName);
    const initializerFn = cx.initializers.get(fqName);
    if (!initializerFn) {
      throw new Error(`Function not found: ${fqName}`);
    }
    // Create a runtime node object.
    const runtimeNode = new RuntimeNode(cx, nodeId, nodeFn);
    // Run the initializer function on the node object to initialize the ports and parameters.
    initializerFn(runtimeNode);
    return runtimeNode;
  } else {
    const runtimeNode = new RuntimeNode(cx, nodeId, nodeFn);
    return runtimeNode;
  }
}

export async function evaluateFunction(
  cx: Context,
  fqName: string,
  values?: Record<string, LiteralValue>,
): Promise<RuntimeNode | undefined> {
  let runtimeNode = cx.runtimeNodes.get(fqName);
  if (!runtimeNode) {
    // We use the fully qualified name here twice, because we don't have a node ID.
    // This means that when we're executing this function in isolation, and looking up the value of an input port,
    // we will not find anything in the context, because the port value is stored as `nodeId/portName`.
    runtimeNode = await createRuntimeNodeForItem(cx, fqName, fqName);
    if (!runtimeNode) return undefined;
    cx.runtimeNodes.set(fqName, runtimeNode);
  }
  if (values) {
    runtimeNode.values = Object.fromEntries(Object.entries(values).map(([k, v]) => [k, { type: "VALUE", value: v }]));
  }
  await runtimeNode.onRender(cx);
  return runtimeNode;
}

export async function evaluateRuntimeNode(runtimeNode: RuntimeNode, values?: Record<string, ParameterValue>) {
  if (values) {
    runtimeNode.values = structuredClone(values);
  }
  await runtimeNode.onRender(runtimeNode.cx);
}

export async function sendChangeEvent(cx: Context, nodeId: string, parameterName: string) {
  // FIXME: This needs to be changed once we're merging subnetworks, because we can't use
  // global node IDs anymore.
  const runtimeNode = cx.runtimeNodes.get(nodeId);
  if (!runtimeNode) return;
  runtimeNode.onChange(cx, parameterName);
}
