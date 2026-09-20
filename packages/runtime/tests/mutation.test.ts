import { expect, it } from "vitest";
import { Context, Network, Node, Sticky, getValue, findNodeByName, NodeToNodeConnection } from "../src";
import { createTestContext } from "./util.js";
import {
  createProject,
  createNetwork,
  createNode,
  setValue,
  connectNodeToNode,
  setRenderedNode,
  duplicateItems,
  createSticky,
  publishPortToOutlet,
  deleteItems,
} from "../src/mutation.js";

it("can create a network", () => {
  const project = createProject("test");
  let cx = new Context(project, new Map(), new Map());
  const n1 = createNetwork(cx, project, "net");
  expect(n1.name).toEqual("net");
  expect(n1.children.length).toEqual(0);
  expect(n1.connections.length).toEqual(0);
  expect(n1.parameters.length).toEqual(0);
  expect(n1.renderedNode).toBeNull();
});

it("can serialize the project", () => {
  let { cx } = createTestContext();
  const json = JSON.stringify(cx.project);
  const project = JSON.parse(json);
  expect(project.title).toEqual("test");
});

it("can create nodes", () => {
  const { cx, network } = createTestContext();
  const node = createNode(cx, network, "test/math/negate");
  expect(node.name).toEqual("negate 1");
});

it("can delete nodes", () => {
  const { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  const negate1 = createNode(cx, network, "test/math/negate");
  _connectNodeToNode(cx, network, value1, "out", negate1, "table");
  setRenderedNode(cx, network, negate1);
  expect(value1.name).toEqual("value 1");
  expect(negate1.name).toEqual("negate 1");
  expect(network.children.length).toEqual(2);
  expect(network.connections.length).toEqual(1);
  deleteItems(cx, network, [negate1.id]);
  expect(network.children.length).toEqual(1);
  expect(network.connections.length).toEqual(0);
  expect(findNodeByName(network, "value 1")).toBeDefined();
  expect(findNodeByName(network, "negate 1")).toBeUndefined();
  expect(network.renderedNode).toBeNull();
});

it("can change values", () => {
  let { cx, network } = createTestContext();
  const node = createNode(cx, network, "test/math/value");
  expect(node.name).toEqual("value 1");
  const originalValue = getValue(cx, node, "value");
  expect(originalValue).toEqual(0);
  setValue(cx, network, node, "value", { type: "VALUE", value: 42 });
  const newValue = getValue(cx, node, "value");
  expect(newValue).toEqual(42);
});

function _connectNodeToNode(
  cx: Context,
  network: Network,
  outNode: Node,
  outPortName: string,
  inNode: Node,
  inPortName: string,
) {
  const outItem = cx.lookupItemByName(outNode.fn);
  const inItem = cx.lookupItemByName(inNode.fn);
  const outPort = outItem.outputPorts.find((p) => p.name === outPortName);
  expect(outPort).toBeDefined();
  const inPort = inItem.inputPorts.find((p) => p.name === inPortName);
  expect(inPort).toBeDefined();
  connectNodeToNode(cx, network, outNode, outPort!, inNode, inPort!);
}

it("can connect nodes", () => {
  let { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  const value2 = createNode(cx, network, "test/math/value");
  const negate1 = createNode(cx, network, "test/math/negate");
  expect(network.connections.length).toEqual(0);
  value1.fn;
  _connectNodeToNode(cx, network, value1, "out", negate1, "table");
  expect(network.connections.length).toEqual(1);
  // Replace the existing connection to negate1 with a connection from value2.
  // This should remove the connection from value1 to negate1.
  _connectNodeToNode(cx, network, value2, "out", negate1, "table");
  expect(network.connections.length).toEqual(1);
});

it("can duplicate a single node", () => {
  let { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  setValue(cx, network, value1, "value", { type: "VALUE", value: 42 });
  expect(network.children.length).toEqual(1);
  duplicateItems(cx, network, [value1.id]);
  expect(network.children.length).toEqual(2);
  const newNode = network.children[1] as Node;
  expect(newNode.type).toEqual("NODE");
  expect(newNode.name).toEqual("value 2");
  expect(getValue(cx, newNode, "value")).toEqual(42);
});

it("can duplicate multiple nodes", () => {
  // We have a network with a value node going into a negate node.
  // We're copying both nodes.
  // A new connection should be made between the new value and the new negate node.
  let { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  const negate1 = createNode(cx, network, "test/math/negate");
  _connectNodeToNode(cx, network, value1, "out", negate1, "table");
  expect(network.children.length).toEqual(2);
  expect(network.connections.length).toEqual(1);
  duplicateItems(cx, network, [value1.id, negate1.id]);
  expect(network.children.length).toEqual(4);
  expect(network.connections.length).toEqual(2);
  const newValue = network.children[2] as Node;
  const newNegate = network.children[3] as Node;
  const newConnection = network.connections[1] as NodeToNodeConnection;
  expect(newConnection.outNode).toEqual(newValue.id);
  expect(newConnection.inNode).toEqual(newNegate.id);
});

it("when duplicating, can keep connections between outside and inside nodes", () => {
  // We have a network with a value node going into a negate node.
  // We're only copying the negate node.
  // A new connection should be made between value1 and the new negate node.
  let { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  const negate1 = createNode(cx, network, "test/math/negate");
  _connectNodeToNode(cx, network, value1, "out", negate1, "table");
  expect(network.children.length).toEqual(2);
  expect(network.connections.length).toEqual(1);
  duplicateItems(cx, network, [negate1.id]);
  expect(network.children.length).toEqual(3);
  expect(network.connections.length).toEqual(2);
  const newNegate = network.children[2] as Node;
  const newConnection = network.connections[1] as NodeToNodeConnection;
  expect(newConnection.outNode).toEqual(value1.id);
  expect(newConnection.inNode).toEqual(newNegate.id);
});

it("duplicate also duplicates stickies", () => {
  let { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  const sticky1 = createSticky(cx, network);
  sticky1.text = "1234";
  expect(network.children.length).toEqual(2);
  duplicateItems(cx, network, [value1.id, sticky1.id]);
  expect(network.children.length).toEqual(4);
  const newSticky = network.children[3] as Sticky;
  expect(newSticky.text).toEqual("1234");
});

it("can create an outlet on a network", () => {
  let { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  const valueOut = cx.lookupItemById(value1.fn).outputPorts[0];
  expect(network.outputPorts.length).toEqual(0);
  expect(network.connections.length).toEqual(0);
  publishPortToOutlet(cx, network, value1, valueOut);
  expect(network.outputPorts.length).toEqual(1);
  expect(network.connections.length).toEqual(1);
});

it("can't create two outlets with the same name on a network", () => {
  let { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  const value1Out = cx.lookupItemById(value1.fn).outputPorts[0];
  const value2 = createNode(cx, network, "test/math/value");
  const value2Out = cx.lookupItemById(value2.fn).outputPorts[0];
  publishPortToOutlet(cx, network, value1, value1Out);
  // This port has the same name as the first one, so it should throw.
  expect(() => publishPortToOutlet(cx, network, value2, value2Out)).toThrow();
  // We can optionally provide a different name, which should work.
  publishPortToOutlet(cx, network, value2, value2Out, "out2");
  expect(network.outputPorts.map((p) => p.name)).toEqual(["out", "out2"]);
});

it("deleting an outlet deletes the connections and output ports", () => {
  let { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  const value1Out = cx.lookupItemById(value1.fn).outputPorts[0];
  const outlet = publishPortToOutlet(cx, network, value1, value1Out);
  expect(network.connections.length).toEqual(1);
  expect(network.outputPorts.length).toEqual(1);
  deleteItems(cx, network, [outlet.id]);
  expect(network.connections.length).toEqual(0);
  expect(network.outputPorts.length).toEqual(0);
});

it.skip("can group nodes into a network", async () => {
  // Network looks like this:
  // value1         value2
  // |                |
  // |                v
  // |              negate1
  // +--------+  +----+
  //          v  v
  //          add1
  //            |
  //            v
  //          negate2

  // We're going to group add1 and negate1 into a network called negateAdd.
  // That means the connection negate1->add1 is an internal connection.
  // The value1->add1 and value2->negate1 connections are input connections.
  // The add1->negate2 connection is an output connection.
  async function assertGroupedNetwork(nodeNames: string[], expectedParameterNames: string[], expectedOutput: any[]) {
    let result: any[];
    let plan = createTestContext();
    plan = createNode(plan, "test", "math.value");
    plan = createNode(plan, "test", "math.value");
    plan = createNode(plan, "test", "math.negate");
    plan = createNode(plan, "test", "math.negate");
    plan = createNode(plan, "test", "math.add");
    plan = setValue(plan, "test", "value1", "v", 60);
    plan = setValue(plan, "test", "value2", "v", 20);
    plan = connect(plan, "test", "value1", "add1", "a");
    plan = connect(plan, "test", "value2", "negate1", "v");
    plan = connect(plan, "test", "negate1", "add1", "b");
    plan = connect(plan, "test", "add1", "negate2", "v");
    plan = setRenderedNode(plan, "test", "negate2");

    result = await evalFunction(plan, "test.test");
    expect(result).toEqual(expectedOutput);

    plan = groupIntoNetwork(plan, "test", nodeNames, "negateAdd");
    reloadMainProjectFunctions(plan);

    const negateAddNetwork = findLocalFunction(plan, "negateAdd") as Network;
    expect(negateAddNetwork).toBeDefined();
    // expect(negateAddNetwork.nodes.length).toEqual(2);
    // expect(negateAddNetwork.renderedNode).toEqual("add1");
    expect(negateAddNetwork.parameters.map((p) => p.name)).toEqual(expectedParameterNames);

    const testNetwork = findLocalFunction(plan, "test") as Network;
    expect(testNetwork).toBeDefined();
    console.log(testNetwork.nodes.map((n) => n.name));
    // expect(testNetwork.nodes.length).toEqual(5);

    result = await evalFunction(plan, "test.test");
    // result = await evalFunction(plan, "test.negateAdd");
    expect(result).toEqual(expectedOutput);
  }

  await assertGroupedNetwork(["add1", "negate1"], ["a", "v"], [-40]);
  await assertGroupedNetwork(["add1", "negate1", "negate2"], ["a", "v"], [-40]);
  await assertGroupedNetwork(["negate1"], ["v"], [-40]);
  await assertGroupedNetwork(["add1"], ["a", "b"], [-40]);
});
