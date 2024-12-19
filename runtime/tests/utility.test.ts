import { expect, it } from "vitest";
import { fileExtension } from "../src/loaders.ts";
import { generateUniqueName } from "../src/identifiers.ts";

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
