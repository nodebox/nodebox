import { it, expect } from "vitest";
import { cleanFilename } from "../src/file-utils";

it("removes special characters and replaces spaces with dashes", () => {
  expect(cleanFilename("my file @#$%.jpg")).toBe("my-file-.jpg");
});

it("throws an error for unsupported file types", () => {
  expect(() => cleanFilename("document.exe")).toThrow("File type not allowed");
});

it("returns a cleaned filename for allowed file types", () => {
  expect(cleanFilename("picture.png")).toBe("picture.png");
  expect(cleanFilename("example file with spaces and symbols!.txt")).toBe("example-file-with-spaces-and-symbols.txt");
  expect(cleanFilename("weirdfile~name!.json")).toBe("weirdfilename.json");
});

it("truncates the filename to 255 characters including extension", () => {
  const longFilename = "a".repeat(260) + ".jpg";
  expect(cleanFilename(longFilename)).toHaveLength(256);
  expect(cleanFilename(longFilename)).toMatch(/^a{252}.jpg$/);
});

it("handles filenames without an extension correctly", () => {
  expect(() => cleanFilename("noextensionfile")).toThrow("File type not allowed");
});

it("supports filenames with uppercase extensions", () => {
  expect(cleanFilename("photo.PNG")).toBe("photo.png");
});

it("handles filenames with multiple dots correctly", () => {
  expect(cleanFilename("complex.name.with.dots.gif")).toBe("complex.name.with.dots.gif");
});
