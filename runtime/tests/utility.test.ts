import { expect, it } from "vitest";
import { fileExtension } from "../src/loaders.ts";
import { generateUniqueName } from "../src/identifiers.ts";
import { evalTemplate } from "../src/string-utils.ts";

it("can determine a unique node name.", () => {
  const nodeNames = ["value 1", "value 2", "negate 1"];
  expect(generateUniqueName("value", nodeNames)).toEqual("value 3");
  expect(generateUniqueName("negate", nodeNames)).toEqual("negate 2");
});

it("can find a file extension", () => {
  expect(fileExtension("foo.jpg")).toEqual(".jpg");
  expect(fileExtension("foo.JPEG")).toEqual(".JPEG");
  expect(fileExtension("foo")).toEqual("");
  expect(fileExtension(".gitignore")).toEqual(".gitignore");
  expect(fileExtension("a.b.c.png")).toEqual(".png");
  expect(fileExtension("hello.app/Contents/foo")).toEqual("");
});

it("can evaluate a template string", () => {
  const values = { name: "Alice", age: 42, nested: { value: 5 } };
  expect(evalTemplate("Hello, {{ name }}!", values)).toEqual("Hello, Alice!");
  expect(evalTemplate("Your age is {{ age }}.", values)).toEqual("Your age is 42.");
  expect(evalTemplate("Objects become {{ nested }}.", values)).toEqual("Objects become [object Object].");
  expect(() => evalTemplate("Hello, {{ notfound }}!", values)).toThrow('Missing template value for key "notfound"');
  expect(() => evalTemplate("Nested objects are not supported: {{ nested.value }}.", values)).toThrow(
    'Missing template value for key "nested.value"',
  );
});
