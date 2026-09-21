import { it, expect } from "vitest";
import { ParameterType } from "../src";
import { formatParameterValue, parseParameterValue } from "../src/type-conversion";

it("can convert from a string to a value", () => {
  expect(parseParameterValue("42", ParameterType.Number)).toEqual(42);
  expect(parseParameterValue("foo", ParameterType.Number)).toEqual(0);
  expect(parseParameterValue("", ParameterType.Number)).toEqual(0);

  expect(parseParameterValue("true", ParameterType.Boolean)).toBe(true);
  expect(parseParameterValue("false", ParameterType.Boolean)).toBe(false);
  expect(parseParameterValue("foo", ParameterType.Boolean)).toBe(false);
  expect(parseParameterValue("", ParameterType.Boolean)).toBe(false);

  expect(parseParameterValue("3, 5", ParameterType.Point)).toEqual({ x: 3, y: 5 });
  expect(parseParameterValue("7", ParameterType.Point)).toEqual({ x: 7, y: 7 });
  expect(parseParameterValue("foo", ParameterType.Point)).toEqual({ x: 0, y: 0 });
  expect(parseParameterValue("", ParameterType.Point)).toEqual({ x: 0, y: 0 });
});

it("can convert from a value to a string", () => {
  expect(formatParameterValue(42, ParameterType.Number)).toEqual("42");
  expect(formatParameterValue(true, ParameterType.Boolean)).toEqual("true");
  expect(formatParameterValue(false, ParameterType.Boolean)).toEqual("false");
  expect(formatParameterValue({ x: 3, y: 5 }, ParameterType.Point)).toEqual("3, 5");
});
