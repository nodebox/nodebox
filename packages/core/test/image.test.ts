// The pixel layer: the kernels, the rasterizer, and the print chain that is built from them.
import { describe, expect, it } from "vitest";
import { Color } from "../src/graphics/color";
import { Path } from "../src/graphics/path";
import { Point } from "../src/graphics/point";
import { Raster } from "../src/graphics/raster";
import { rasterizeFill, rasterizeStroke } from "../src/graphics/rasterize";
import * as image from "../src/functions/image";
import { builtinFunctionRepository } from "../src/functions";
import { builtinNodeRepository } from "../src/libraries";
import { NodeContext } from "../src/runtime/context";
import { addChild, connect, createNetworkNode, extendNode, setInputValue } from "../src/model/node";
import type { Library } from "../src/model/types";

function stats(raster: Raster) {
  let min = Infinity;
  let max = -Infinity;
  let total = 0;
  for (const v of raster.data) {
    if (v < min) min = v;
    if (v > max) max = v;
    total += v;
  }
  return { min, max, mean: total / raster.data.length };
}

describe("rasterizer", () => {
  it("fills a shape with coverage that matches its area", () => {
    const circle = new Path();
    circle.ellipse(0, 0, 100, 100);
    const raster = rasterizeFill(circle, { width: 200, height: 200, left: -100, top: -100, spanX: 200, spanY: 200 });
    const { min, max, mean } = stats(raster);
    expect(min).toBe(0);
    expect(max).toBeCloseTo(1, 5);
    // A circle of radius 50 in a 200 by 200 window covers pi*50^2 / 200^2 of it.
    expect(mean).toBeCloseTo((Math.PI * 50 * 50) / (200 * 200), 2);
  });

  it("leaves the inside of a shape with a hole empty, under the nonzero rule", () => {
    const ring = new Path();
    ring.ellipse(0, 0, 100, 100);
    // The same direction twice would fill the hole; reversed, the windings cancel.
    const inner = new Path();
    inner.ellipse(0, 0, 50, 50);
    for (const contour of inner.contours) contour.points.reverse();
    for (const contour of inner.contours) ring.add(contour);
    const raster = rasterizeFill(ring, { width: 200, height: 200, left: -100, top: -100, spanX: 200, spanY: 200 });
    expect(raster.at(100, 100)).toBeCloseTo(0, 5);
    expect(raster.at(100, 65)).toBeCloseTo(1, 5);
  });

  it("strokes a line with the width it was given", () => {
    const line = new Path();
    line.line(-40, 0, 40, 0);
    const raster = rasterizeStroke(line, 10, { width: 100, height: 100, left: -50, top: -50, spanX: 100, spanY: 100 });
    expect(raster.at(50, 50)).toBeCloseTo(1, 5);
    // The stroke is 10 wide around y = 0, which is rows 45 to 55.
    expect(raster.at(50, 45)).toBeCloseTo(1, 5);
    expect(raster.at(50, 44)).toBeCloseTo(0, 5);
  });
});

describe("pixel kernels", () => {
  it("keeps the generator of riso-drawing, so seeds give the same picture", () => {
    const rng = image.mulberry32(7);
    expect(rng()).toBeCloseTo(0.011704753153026104, 12);
    expect(rng()).toBeCloseTo(0.06195825757458806, 12);
  });

  it("grows a halftone dot past the corner of its cell", () => {
    const screen = image.screen(64, 64, 8, 0);
    expect(screen.at(0, 0)).toBeCloseTo((Math.PI * 0.5) / 0.9, 6);
    expect(screen.at(4, 4)).toBeCloseTo(0, 6);
  });

  it("compares a against b as a soft step", () => {
    const a = image.constant(4, 4, 0.5);
    const b = image.constant(4, 4, 0.5);
    expect(image.compare(a, b, 0.1).at(0, 0)).toBeCloseTo(0.5, 6);
    expect(image.compare(image.constant(4, 4, 1), b, 0.1).at(0, 0)).toBe(1);
    expect(image.compare(image.constant(4, 4, 0), b, 0.1).at(0, 0)).toBe(0);
  });

  it("turns coverage into the transmittance of an ink", () => {
    const inked = image.ink(image.constant(2, 2, 1), new Color(1, 0.28, 0.69));
    // The raster holds 32-bit floats, so the ink comes back rounded to that.
    expect(inked.at(0, 0, 0)).toBeCloseTo(1, 6);
    expect(inked.at(0, 0, 1)).toBeCloseTo(0.28, 6);
    expect(inked.at(0, 0, 2)).toBeCloseTo(0.69, 6);
    const blank = image.ink(image.constant(2, 2, 0), new Color(1, 0.28, 0.69));
    expect([blank.at(0, 0, 0), blank.at(0, 0, 1), blank.at(0, 0, 2)]).toEqual([1, 1, 1]);
  });

  it("multiplies a list of rasters and broadcasts one channel onto three", () => {
    const tone = image.constant(2, 2, 0.5);
    const rgb = image.ink(image.constant(2, 2, 1), new Color(1, 0, 0));
    const out = image.multiply([tone, rgb]);
    expect(out.channels).toBe(3);
    expect([out.at(0, 0, 0), out.at(0, 0, 1), out.at(0, 0, 2)]).toEqual([0.5, 0, 0]);
  });

  it("shifts a raster and leaves the edge it came from empty", () => {
    const solid = image.constant(8, 8, 1);
    const moved = image.shift(solid, new Point(2, 0), 0);
    expect(moved.at(4, 4)).toBeCloseTo(1, 5);
    expect(moved.at(0, 4)).toBe(0);
  });
});

describe("the print, as a network of those nodes", () => {
  function printDocument(size: number): Library {
    const repository = builtinNodeRepository();
    const root = extendNode(createNetworkNode(), "core.network", "root");
    const add = (prototype: string, name: string) =>
      addChild(root, extendNode(repository.getNode(prototype)!, prototype, name));
    for (const [ink, x, y] of [
      ["blue", -30, -18],
      ["pink", 30, -18],
      ["yellow", 0, 34],
    ] as const) {
      const shape = add("corevector.ellipse", `${ink}Shape`);
      setInputValue(shape, "position", new Point(x, y));
      setInputValue(shape, "width", 110);
      setInputValue(shape, "height", 110);
      const fill = add("image.fill", `${ink}Fill`);
      setInputValue(fill, "width", size);
      setInputValue(fill, "height", size);
      connect(root, `${ink}Shape`, `${ink}Fill`, "shape");
    }
    const print = add("image.print", "print");
    setInputValue(print, "width", size);
    setInputValue(print, "height", size);
    setInputValue(print, "fibres", 0);
    for (const ink of ["blue", "pink", "yellow"] as const) connect(root, `${ink}Fill`, "print", ink);
    root.renderedChild = "print";
    return {
      name: "print-test",
      root,
      functionLinks: [],
      devices: [],
      properties: {},
      dependencies: {},
      assets: {},
      sourceFormat: "memory",
      meta: {},
    };
  }

  it("prints three inks on paper", async () => {
    const size = 160;
    const context = new NodeContext(printDocument(size), builtinFunctionRepository());
    const results = await context.render("/");
    expect(results).toHaveLength(1);
    const raster = results[0] as Raster;
    expect(raster).toBeInstanceOf(Raster);
    expect([raster.width, raster.height, raster.channels]).toEqual([size, size, 3]);
    // The paper shows in the corner, each ink shows in its own lobe, and the three together are dark.
    const paper = [raster.at(4, 4, 0), raster.at(4, 4, 1), raster.at(4, 4, 2)];
    expect(paper[0]).toBeGreaterThan(0.85);
    expect(paper[2]).toBeGreaterThan(0.8);
    const middle =
      raster.at(size / 2, size / 2, 0) + raster.at(size / 2, size / 2, 1) + raster.at(size / 2, size / 2, 2);
    expect(middle).toBeLessThan(paper[0] + paper[1] + paper[2]);
  }, 30000);

  it("is built from the image blocks, not from one function", () => {
    const halftone = builtinNodeRepository().getNode("image.halftone")!;
    expect(halftone.isNetwork).toBe(true);
    expect(halftone.children.length).toBeGreaterThan(15);
    expect(halftone.children.every((child) => child.prototype?.startsWith("image."))).toBe(true);
    const print = builtinNodeRepository().getNode("image.print")!;
    expect(print.children.map((child) => child.prototype)).toContain("image.halftone");
  });

  it("gives a list port every connection that reaches it", () => {
    const halftone = builtinNodeRepository().getNode("image.halftone")!;
    const covered = halftone.connections.filter((c) => c.inputNode === "covered" && c.inputPort === "images");
    expect(covered).toHaveLength(5);
  });
});
