import { expect, it } from "vitest";
import {
  Context,
  Network,
  evaluateItem,
  evaluateNetwork,
  findRenderedNodeOutputs,
  getValue,
  PortType,
  PortValue,
  getNetworkOutput,
  ParameterType,
  WidgetType,
  createRuntimeNodeForItem,
  evaluateRuntimeNode,
} from "../src";
import {
  connectNodeToNode,
  connectNodeToOutlet,
  createNode,
  createOutlet,
  setRenderedNode,
  setValue,
  markDirty,
  publishPortToOutlet,
  markItemParameterDirty,
} from "../src/mutation.js";
import { createTestContext } from "./util.js";

function networkNumberIn(network: Network, name: string, defaultValue: number) {
  network.parameters.push({
    name,
    type: ParameterType.Number,
    widget: WidgetType.Number,
    label: name,
    defaultValue,
    min: 0,
    max: 10,
    step: 1,
  });
}

async function expectNetworkResult(
  cx: Context,
  network: Network,
  values: Record<string, number>,
  portName: string,
  expectedResult: PortValue,
) {
  for (const [key, value] of Object.entries(values)) {
    markItemParameterDirty(cx, network, key);
  }
  await evaluateNetwork(cx, network, values);
  const result = getNetworkOutput(cx, network, portName);
  expect(result).toEqual(expectedResult);
}

it("can eval a network with an outlet", async () => {
  const { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  setValue(cx, network, value1, "value", { type: "VALUE", value: 5 });
  const negate1 = createNode(cx, network, "test/math/negate");
  const valueFn = cx.lookupItemById(value1.fn);
  const negateFn = cx.lookupItemById(negate1.fn);
  const valueOut = valueFn.outputPorts[0];
  const negateIn = negateFn.inputPorts[0];
  const negateOut = negateFn.outputPorts[0];
  connectNodeToNode(cx, network, value1, valueOut, negate1, negateIn);
  expect(network.outputPorts).toEqual([]);
  publishPortToOutlet(cx, network, negate1, negateOut);
  expect(network.outputPorts).toEqual([{ name: "out", type: PortType.Table }]);

  const valueBeforeRender = getNetworkOutput(cx, network, "out");
  expect(valueBeforeRender).toBeUndefined();

  // We explicitly don't set a renderedNode.
  await evaluateNetwork(cx, network);
  const value = getNetworkOutput(cx, network, "out");
  expect(value).toEqual([{ value: -5 }]);
});

it("can eval a network with parameters", async () => {
  const { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  setValue(cx, network, value1, "value", { type: "EXPRESSION", expression: "network.valueFromNetwork" });
  const valueFn = cx.lookupItemById(value1.fn);
  const valueOut = valueFn.outputPorts[0];
  publishPortToOutlet(cx, network, value1, valueOut);
  networkNumberIn(network, "valueFromNetwork", 5);

  {
    // Test without providing values
    await evaluateNetwork(cx, network);
    const result = getNetworkOutput(cx, network, "out");
    expect(result).toEqual([{ value: 5 }]);
  }
  markItemParameterDirty(cx, network, "valueFromNetwork");
  {
    // Test with providing values
    await evaluateNetwork(cx, network, { valueFromNetwork: 42 });
    const result = getNetworkOutput(cx, network, "out");
    expect(result).toEqual([{ value: 42 }]);
  }
});

it("can do dirty propagation", async () => {
  const { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  setValue(cx, network, value1, "value", { type: "EXPRESSION", expression: "network.foo" });
  const valueFn = cx.lookupItemById(value1.fn);
  const valueOut = valueFn.outputPorts[0];
  publishPortToOutlet(cx, network, value1, valueOut);
  networkNumberIn(network, "foo", 5);
  await expectNetworkResult(cx, network, { foo: 1234 }, "out", [{ value: 1234 }]);
  await expectNetworkResult(cx, network, { foo: 1111 }, "out", [{ value: 1111 }]);
});

it.skip("can support embedded values", async () => {
  const { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value");
  setValue(cx, network, value1, "value", { type: "EXPRESSION", expression: "network.valueFromNetwork" });
  const valueFn = cx.lookupItemById(value1.fn);
  const valueOut = valueFn.outputPorts[0];
  publishPortToOutlet(cx, network, value1, valueOut);
  network.parameters.push({
    name: "valueFromNetwork",
    type: ParameterType.Number,
    widget: WidgetType.Number,
    label: "Value From Network",
    defaultValue: 5,
    min: 0,
    max: 10,
    step: 1,
  });

  const results = evaluateItem(cx, "self/self/test", network, { valueFromNetwork: 42 });

  const runtimeNode = await createRuntimeNodeForItem(cx, "self/self/test", "self/self/test");
  await evaluateRuntimeNode(runtimeNode, { valueFromNetwork: { type: "VALUE", value: 42 } });

  const outPort = runtimeNode.outputPorts[0];
  expect(outPort.value).toEqual(42);
});
