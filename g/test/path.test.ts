import { expect, it } from "vitest";
import { Path, pathDataToSegments, Point } from "../src";

it("can parse simple path data", () => {
  const d = `M 10 20 L 30 40`;
  const path = Path.fromPathData(d);
  expect(path.verbs).toEqual(["M", "L"]);
  expect(path.points).toEqual([new Point(10, 20), new Point(30, 40)]);
});

it("can parse 'skipped' commands", () => {
  const d = `M10 10 L90 10 90 90 10 90 z`;
  const path = Path.fromPathData(d);
  expect(path.verbs).toEqual(["M", "L", "L", "L", "Z"]);
  expect(path.points).toEqual([new Point(10, 10), new Point(90, 10), new Point(90, 90), new Point(10, 90)]);
});

it("can parse shorthand", () => {
  const segments = pathDataToSegments(`m17.836 12.014-4.345.725 3.29-4.113a1 1 0 0 0-.227-1.457`);
  expect(segments).toEqual([
    { command: "m", params: [17.836, 12.014] },
    { command: "l", params: [-4.345, 0.725] },
    { command: "l", params: [3.29, -4.113] },
    { command: "a", params: [1, 1, 0, 0, 0, -0.227, -1.457] },
  ]);
});
