import { expect, it } from "vitest";
import { evaluateExpression } from "../src";

it("can evaluate a simple expression", () => {
  const result = evaluateExpression("3 + 5", {});
  expect(result).toEqual(8);
});

it("can evaluate an expression with context", () => {
  const result = evaluateExpression("width * 2", { width: 100 });
  expect(result).toEqual(200);
});

it("can evaluate a string expression", () => {
  const result = evaluateExpression(`"a" + "b"`);
  expect(result).toEqual("ab");
});

it("can evaluate a dotted expression", () => {
  const result = evaluateExpression(`network.width`, { network: { width: 1234 } });
  expect(result).toEqual(1234);
});
