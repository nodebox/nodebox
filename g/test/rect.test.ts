import { expect, it } from "vitest";
import { Path, pathDataToSegments, Point, Rect } from "../src";

it("can convert to path data", () => {
  const path = new Rect(10, 20, 30, 40);
  expect(path.toPathData()).toEqual(`M 10, 20 h 30 v 40 h -30 Z`);
});
