import { expect, it } from "vitest";
import { ParameterType, findNodeByName, findParameter, getValue } from "../src";
import { createTestContext } from "./util.js";
import { createNode, setValue } from "../src/mutation.js";

it("can find nodes", () => {
  let { cx, network } = createTestContext();
  expect(network.name).toEqual("test");
  createNode(cx, network, "test/math/value");
  const existingNode = findNodeByName(network, "value 1");
  expect(existingNode).toBeDefined();
  expect(existingNode!.name).toEqual("value 1");
  const nonExistantNode = findNodeByName(network, "nonExistant");
  expect(nonExistantNode).not.toBeDefined();
});

it("can find a parameter", () => {
  const { cx, network } = createTestContext();
  const node = createNode(cx, network, "test/math/make-numbers");
  const param = findParameter(cx, node, "template");
  expect(param).toBeDefined();
  expect(param?.type).toEqual(ParameterType.String);
  expect(param?.defaultValue).toEqual("11;22;33");
  expect(findParameter(cx, node, "xxx")).toBeUndefined();
});

it("can get a parameter value", () => {
  const { cx, network } = createTestContext();
  const node = createNode(cx, network, "test/math/make-numbers");
  const param = findParameter(cx, node, "template");
  expect(param).toBeDefined();
  expect(getValue(cx, node, "template")).toEqual("11;22;33");
  setValue(cx, network, node, "template", { type: "VALUE", value: "1;2;3;4" });
  expect(getValue(cx, node, "template")).toEqual("1;2;3;4");
});
