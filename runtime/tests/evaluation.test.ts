import { expect, it } from "vitest";
import { evaluateNetwork, findRenderedNodeOutputs } from "../src";
import { connectNodeToNode, createNode, setRenderedNode, setValue } from "../src/mutation.js";
import { createTestContext } from "./util.js";

it("can evaluate a simple function", async () => {
  const { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value")!;
  setValue(cx, network, value1, "value", { type: "VALUE", value: 5 });
  setRenderedNode(cx, network, value1);
  const item = cx.lookupItemByName("test/math/value");
  await evaluateNetwork(cx, network);
  const outputs = findRenderedNodeOutputs(cx, network);
  expect(outputs.size).toEqual(1);
  expect(Array.from(outputs.keys())).toEqual(["out"]);
  const output = outputs.get("out");
  expect(output).toEqual([{ value: 5 }]);
});

it("can evaluate connected functions", async () => {
  const { cx, network } = createTestContext();
  const value1 = createNode(cx, network, "test/math/value")!;
  setValue(cx, network, value1, "value", { type: "VALUE", value: 5 });
  const negate1 = createNode(cx, network, "test/math/negate");
  const valueFn = cx.lookupItemById(value1.fn);
  const negateFn = cx.lookupItemById(negate1.fn);
  const valueOut = valueFn.outputPorts[0];
  const negateIn = negateFn.inputPorts[0];
  connectNodeToNode(cx, network, value1, valueOut, negate1, negateIn);
  setRenderedNode(cx, network, negate1);
  await evaluateNetwork(cx, network);
  const outputs = findRenderedNodeOutputs(cx, network);
  const output = outputs.get("out");
  expect(output).toEqual([{ value: -5 }]);
});

it("can evaluate an expression", async () => {
  const { cx, network } = createTestContext();
  network.width = 1234;
  const value1 = createNode(cx, network, "test/math/value")!;
  setValue(cx, network, value1, "value", { type: "EXPRESSION", expression: "network.width" });
  setRenderedNode(cx, network, value1);
  await evaluateNetwork(cx, network);
  const outputs = findRenderedNodeOutputs(cx, network);
  const output = outputs.get("out");
  expect(output).toEqual([{ value: 1234 }]);
});

it.skip("can cycle lists", async () => {
  let result: any[];
  let plan = createTestContext();
  plan = createNetwork(plan, "main");
  plan = createNode(plan, "main", "math.makeNumbers");
  plan = createNode(plan, "main", "math.negate");
  plan = connect(plan, "main", "makeNumbers1", "negate1", "v");
  plan = setRenderedNode(plan, "main", "negate1");
  result = await evalFunction(plan, "test.main");
  expect(result).toEqual([-11, -22, -33]);
});

it.skip("can cycle two lists", async () => {
  let result: any[];
  let plan = createTestContext();
  plan = createNetwork(plan, "main");
  plan = createNode(plan, "main", "math.makeNumbers");
  plan = createNode(plan, "main", "math.makeNumbers");
  plan = setValue(plan, "main", "makeNumbers1", "s", "1;2;3;4");
  plan = setValue(plan, "main", "makeNumbers2", "s", "0;1000");
  plan = createNode(plan, "main", "math.add");
  plan = connect(plan, "main", "makeNumbers1", "add1", "a");
  plan = connect(plan, "main", "makeNumbers2", "add1", "b");
  plan = setRenderedNode(plan, "main", "add1");
  result = await evalFunction(plan, "test.main");
  // 1 + 0; 2 + 1000; 3 + 0; 4 + 1000
  expect(result).toEqual([1, 1002, 3, 1004]);
});

it.skip("can evaluate inlets", async () => {
  let result: any[];
  let plan = createTestContext();
  plan = createNetwork(plan, "main");
  plan = createNode(plan, "main", "math.add");
  plan = setRenderedNode(plan, "main", "add1");

  result = await evalFunction(plan, "test.main");
  expect(result).toEqual([8]);

  plan = addParameter(plan, "main", { name: "a", type: "float", value: 20, takesList: false });
  plan = addParameter(plan, "main", { name: "b", type: "float", value: 40, takesList: false });
  plan = connectInlet(plan, "main", 0, "add1", "a");
  plan = connectInlet(plan, "main", 1, "add1", "b");

  result = await evalFunction(plan, "test.main");
  expect(result).toEqual([60]);
});
