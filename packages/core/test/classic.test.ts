// The demos and tutorials of the original NodeBox Live, rendered on the core engine.
//
// classic-results.json records how many results each one produces. Those numbers come from the
// original runtime (src/client/ndbx.js on the nodeboxdead branch), run over the same projects in
// Node and compared node by node; see scripts/classic-render.mts and docs/classic-nodebox-live.md.
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { describe, expect, it } from "vitest";
import { NodeContext } from "../src/runtime/context";
import { openClassicProject } from "../src/live/classic-document";
import { isClassicProject } from "../src/live/classic-types";
import type { ClassicProject } from "../src/live/classic-types";

const require = createRequire(import.meta.url);
// g.js probes for a canvas when it loads and falls back gracefully when there is none.
(globalThis as Record<string, unknown>).document ??= {
  createElement: () => ({ width: 0, height: 0, style: {}, getContext: () => null }),
};
// Script assets run in the global scope, the way a <script> tag did, and expect a browser there.
(globalThis as Record<string, unknown>).window ??= globalThis;

const gPackage = require("g.js");
gPackage._toSVG = gPackage.toSVG;

const dataDir = path.resolve(__dirname, "..", "..", "server", "data");
const fontFile = path.resolve(__dirname, "..", "..", "server", "static", "fonts", "FiraSans-Regular.woff");
const expected: Record<string, number> = JSON.parse(
  fs.readFileSync(path.join(__dirname, "classic-results.json"), "utf-8"),
);

const TEXT_EXTENSIONS = new Set([
  ".csv",
  ".html",
  ".ini",
  ".json",
  ".geojson",
  ".log",
  ".md",
  ".rtf",
  ".svg",
  ".txt",
  ".tsv",
  ".xhtml",
  ".xml",
  ".yaml",
]);

function toArrayBuffer(buffer: Buffer): ArrayBuffer {
  return buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength) as ArrayBuffer;
}

function loadProject(key: string): ClassicProject {
  const [userId, projectId] = key.split("/");
  const project = JSON.parse(fs.readFileSync(path.join(dataDir, userId, projectId, "project.json"), "utf-8"));
  project.id = projectId;
  return project;
}

function loadAssets(key: string, project: ClassicProject, scripts: string[]): Record<string, unknown> {
  const [userId, projectId] = key.split("/");
  const blobs = path.join(dataDir, userId, projectId, "blobs");
  const assets: Record<string, unknown> = {};
  for (const [fileName, hash] of Object.entries(project.assets ?? {})) {
    const file = path.join(blobs, typeof hash === "string" ? hash : hash.hash);
    if (!fs.existsSync(file)) continue;
    const ext = path.extname(fileName).toLowerCase();
    if (TEXT_EXTENSIONS.has(ext)) assets[fileName] = fs.readFileSync(file, "utf-8");
    else if (ext === ".js") scripts.push(fs.readFileSync(file, "utf-8"));
    else assets[fileName] = toArrayBuffer(fs.readFileSync(file));
  }
  return assets;
}

async function render(key: string): Promise<unknown[]> {
  const project = loadProject(key);
  const scripts: string[] = [];
  const assets = loadAssets(key, project, scripts);
  if (fs.existsSync(fontFile)) assets["default-font"] = toArrayBuffer(fs.readFileSync(fontFile));
  // A dependency that is no longer a classic project (core/g was rewritten in a later format) is
  // left out, so that the classic library built into the core is used for it instead.
  const dependencies = Object.keys(project.dependencies ?? {})
    .map((depKey) => ({ key: depKey, project: loadProject(depKey) }))
    .filter((dependency) => isClassicProject(dependency.project));
  for (const dependency of dependencies) Object.assign(assets, loadAssets(dependency.key, dependency.project, scripts));
  const document = openClassicProject(key, project, {
    dependencies,
    assets,
    namespaces: { g: gPackage },
    // g.js was written against opentype.js 1.x; 2.x builds different outlines.
    globals: { _: require("lodash"), opentype: require("opentype-classic") },
  });
  for (const script of scripts) document.runtime.loadScript(script);
  document.runtime.ndbx._currentFrame = 1;
  const context = new NodeContext(document.library, document.functions, { data: { frame: 1 } });
  return context.renderEntryPoint("/main");
}

describe("classic NodeBox Live projects", () => {
  it("reads a project into a library whose networks keep their inlets", () => {
    const project = loadProject("tutorial/b5bulge");
    const document = openClassicProject("tutorial/b5bulge", project, { namespaces: { g: gPackage } });
    const fx = document.library.root.children.find((child) => child.name === "fx")!;
    expect(fx.isNetwork).toBe(true);
    expect(fx.inputs.map((port) => port.name)).toContain("shape");
    // Every parameter keeps the type it was declared with, so no NodeBox 3 conversion applies.
    expect(fx.inputs.every((port) => port.type.startsWith("classic:"))).toBe(true);
    expect(document.warnings.filter((w) => w.includes("could not be found"))).toEqual([]);
  });

  it("publishes an inlet to every child port it feeds", () => {
    const project = loadProject("tutorial/b4closest");
    const document = openClassicProject("tutorial/b4closest", project, { namespaces: { g: gPackage } });
    const search = document.library.root.children.find((child) => child.name === "search")!;
    const points = search.inputs.find((port) => port.name === "points")!;
    expect(points.childReference).toBe("slice1.l");
    expect(points.childReferences).toEqual(["shapeSort1.shapes"]);
  });

  for (const [key, count] of Object.entries(expected)) {
    it(`renders ${key}`, async () => {
      expect((await render(key)).length).toBe(count);
    }, 60000);
  }
});
