import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";
import { Group, Shape } from "@ndbx/g";
import {
  Library,
  NodeContext,
  analyzeFunctionSource,
  builtinFunctionRepository,
  getChild,
  libraryFromFunctionSources,
  liveFunctions,
  openLive,
  saveLive,
  toG,
} from "../src";

const root = path.resolve(__dirname, "..", "..", "..");

function functionSources(dir: string): Record<string, string> {
  const sources: Record<string, string> = {};
  for (const file of fs.readdirSync(dir)) {
    if (file.endsWith(".js")) sources[file.replace(/\.js$/, "")] = fs.readFileSync(path.join(dir, file), "utf-8");
  }
  return sources;
}

// Node's ESM loader needs a file URL for "@ndbx/g"; the browser gets an import map instead.
const require = createRequire(import.meta.url);
const resolveBareImport = (specifier: string) => pathToFileURL(require.resolve(specifier)).href;

let coreG: Library | undefined;
function coreGLibrary(): Library {
  if (!coreG)
    coreG = libraryFromFunctionSources("core/g", functionSources(path.join(root, "packages/runtime/functions/g")), "g");
  return coreG;
}

describe("Live source analysis", () => {
  it("derives parameters and ports from a node source", () => {
    const source = fs.readFileSync(path.join(root, "packages/runtime/functions/g/rect.js"), "utf-8");
    const sig = analyzeFunctionSource(source);
    expect(sig.inputPorts.map((p) => p.name)).toEqual(["table"]);
    expect(sig.parameters.map((p) => p.name)).toEqual(["x", "y", "width", "height", "fill", "stroke", "strokeWidth"]);
    expect(sig.parameters[2].min).toBe(0);
    expect(sig.outputPorts).toEqual([{ name: "Out", type: "SHAPE", label: undefined }]);
    expect(sig.category).toBe("Graphics");
    expect(sig.description).toBe("Draw a rectangle.");
  });

  it("handles sections and choices", () => {
    const source = fs.readFileSync(path.join(root, "packages/runtime/functions/g/filter-data.js"), "utf-8");
    const sig = analyzeFunctionSource(source);
    expect(sig.sections.map((s) => s.name)).toEqual(["General", "Data format"]);
    expect(sig.parameters.find((p) => p.name === "operator")!.choices!.length).toBe(14);
    expect(sig.parameters.find((p) => p.name === "format")!.section).toBe("Data format");
    expect(sig.parameters.find((p) => p.name === "format")!.choices![1]).toEqual({ name: "geojson", label: "GeoJSON" });
  });
});

describe("Live projects", () => {
  it("loads core/g as a library of prototypes", () => {
    const lib = coreGLibrary();
    const rect = getChild(lib.root, "Rect")!;
    expect(rect.function).toBe("live/core/g/Rect");
    expect(rect.inputs.map((p) => `${p.name}:${p.type}:${p.range}`)).toEqual([
      "table:table:list",
      "x:float:value",
      "y:float:value",
      "width:float:value",
      "height:float:value",
      "fill:color:value",
      "stroke:color:value",
      "strokeWidth:float:value",
    ]);
    expect(rect.outputs.map((p) => `${p.name}:${p.type}:${p.range}`)).toEqual(["Out:shape:value"]);
    expect(rect.inputs[3].min).toBe(0);
  });

  it("loads the welcome project and renders it", async () => {
    const json = fs.readFileSync(path.join(root, "packages/server/data/skel/welcome/project.json"), "utf-8");
    const { library, warnings } = openLive(json, { projectKey: "example/welcome", dependencies: [coreGLibrary()] });
    expect(warnings).toEqual([]);
    const main = getChild(library.root, "Main")!;
    expect(main.isNetwork).toBe(true);
    expect(main.children.map((c) => c.name)).toEqual(["Rect 1", "Sample 1"]);
    expect(main.stickies.length).toBeGreaterThan(0);
    expect(main.connections).toEqual([
      { outputNode: "Sample 1", outputPort: "out", inputNode: "Rect 1", inputPort: "table" },
    ]);
    const rect = getChild(main, "Rect 1")!;
    expect(rect.inputs.find((p) => p.name === "x")!.expression).toBe("value");
    expect(rect.inputs.find((p) => p.name === "width")!.value).toBe(10);
    expect(library.root.renderedChild).toBe("Main");

    const functions = builtinFunctionRepository().combine(
      liveFunctions([library, coreGLibrary()], { resolveBareImport }),
    );
    const ctx = new NodeContext(library, functions);
    const result = await ctx.render("/");
    expect(result).toHaveLength(1);
    const shape = result[0] as Shape;
    expect(shape.type).toBe("GROUP");
    expect((shape as Group).children).toHaveLength(11);
    const bounds = shape.getBounds();
    // Eleven 10x10 rects at x = 0, 10, ... 100, with a half-pixel stroke around them.
    expect(bounds.left).toBeCloseTo(-0.5);
    expect(bounds.right).toBeCloseTo(110.5);
  });

  it("round-trips the welcome project", () => {
    const json = fs.readFileSync(path.join(root, "packages/server/data/skel/welcome/project.json"), "utf-8");
    const original = JSON.parse(json);
    const { library } = openLive(json, { projectKey: "example/welcome", dependencies: [coreGLibrary()] });
    const written = saveLive(library);
    expect(written.title).toBe(original.title);
    expect(written.items.map((i) => i.name)).toEqual(original.items.map((i: { name: string }) => i.name));
    const main = written.items[0] as {
      children: { type: string; id: string }[];
      connections: unknown[];
      renderedNode: string;
    };
    const originalMain = original.items[0];
    expect(main.children.map((c) => c.id).sort()).toEqual(
      originalMain.children.map((c: { id: string }) => c.id).sort(),
    );
    expect(main.connections).toEqual(originalMain.connections);
    expect(main.renderedNode).toBe(originalMain.renderedNode);
    const rect = main.children.find((c) => c.type === "NODE" && c.id === "eayR3GkrlEu:0") as unknown as {
      values: Record<string, unknown>;
    };
    expect(rect.values.x).toEqual({ type: "EXPRESSION", expression: "value" });
    expect(rect.values.width).toEqual({ type: "VALUE", value: 10 });
  });

  it("feeds NodeBox 3 geometry into Live nodes and back", async () => {
    // A NodeBox 3 ellipse flows into a Live "Transform" node; its output converts to geometry again.
    const json = JSON.stringify({
      formatVersion: 4,
      title: "mix",
      dependencies: { "core/g": "dev" },
      assets: {},
      items: [
        {
          type: "NETWORK",
          id: "0:1",
          name: "Main",
          children: [
            {
              type: "NODE",
              id: "0:2",
              name: "Transform 1",
              x: 0,
              y: 0,
              fn: "core/g/Transform",
              values: { "translate x": { type: "VALUE", value: 50 } },
            },
          ],
          connections: [],
          renderedNode: "0:2",
        },
      ],
    });
    const { library } = openLive(json, { dependencies: [coreGLibrary()] });
    const main = getChild(library.root, "Main")!;
    const { builtinNodeRepository, extendNode, connect, Point } = await import("../src");
    const ellipse = extendNode(
      builtinNodeRepository().getNode("corevector.ellipse")!,
      "corevector.ellipse",
      "ellipse1",
    );
    main.children.push(ellipse);
    connect(main, "ellipse1", "Transform 1", "shapes");
    const functions = builtinFunctionRepository().combine(
      liveFunctions([library, coreGLibrary()], { resolveBareImport }),
    );
    const ctx = new NodeContext(library, functions);
    const result = await ctx.render("/");
    const shape = toG(result)!;
    const b = shape.getBounds();
    expect(b.centerX).toBeCloseTo(50, 0);
    expect(b.centerY).toBeCloseTo(0, 0);
    void Point;
  });
});
