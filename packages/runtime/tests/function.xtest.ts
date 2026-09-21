import { expect, it } from "vitest";
import { namespace, baseName, isValidFunctionName, isValidFullName, functionToString } from "../src/index.ts";

const math: any = {};

math.add = function (a, b) {
  return a + b;
};

math.add.meta = createCodeFn({
  ns: "math",
  name: "add",
  fn: math.add,
  parameters: [
    { name: "a", type: "float", value: 5 },
    { name: "b", type: "float", value: 3 },
  ],
});

function createCodeFn({ ns, name, fn, parameters, outputType = "float", returnsList = false, async = false }) {
  const fullName = `${ns}.${name}`;
  const source = functionToString(fullName, fn);
  const result = {
    name,
    type: "code",
    source,
    parameters,
    outputType,
    returnsList,
    async,
  };
  return result;
}

it("can determine a namespace", () => {
  expect(namespace("foo.bar")).toEqual("foo");
});

it("can determine a base name", () => {
  expect(baseName("foo.bar")).toEqual("bar");
});

it("can determine valid function names", () => {
  expect(isValidFunctionName("bar")).toBeTruthy();
  expect(isValidFunctionName("name_with_underscores")).toBeTruthy();
  expect(isValidFunctionName("1direction")).toBeFalsy();
  expect(isValidFunctionName("  spaces  ")).toBeFalsy();
  expect(isValidFunctionName("")).toBeFalsy();
  expect(isValidFunctionName("foo.bar")).toBeFalsy();
  expect(isValidFunctionName("name-with-dashes")).toBeFalsy();
  expect(isValidFunctionName("name_that_is_way_too_long_for_its_own_good")).toBeFalsy();
});

it("can determine valid full names", () => {
  expect(isValidFullName("foo.bar")).toBeTruthy();
  expect(isValidFullName("bar")).toBeFalsy();
  expect(isValidFullName("  foo.bar  ")).toBeFalsy();
});

it("can extract function source", () => {
  expect(math.add.meta.source.split("\n")[0]).toEqual("math.add = function(a, b) {");
});
