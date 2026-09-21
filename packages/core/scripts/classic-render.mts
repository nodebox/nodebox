// Render classic NodeBox Live projects on the core engine and print what every node produced, in
// the same shape as the reference harness that runs the original runtime. Diffing the two is how
// the classic reader was checked.
//
//   node --import tsx scripts/classic-render.mts <dataDir> demo/flocking tutorial/a0shapes ...
//   VERBOSE=1 prints every node; FRAME sets the frame (default 1).

import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { NodeContext } from "../src/runtime/context.ts";
import { openClassicProject } from "../src/live/classic-document.ts";
import { isClassicProject } from "../src/live/classic-types.ts";
import type { ClassicProject } from "../src/live/classic-types.ts";

const require = createRequire(import.meta.url);
// Script assets run in the global scope, as a <script> tag did, and expect a browser there.
(globalThis as any).window ??= globalThis;
// g.js probes for a canvas when it loads and falls back gracefully when there is none.
(globalThis as any).document ??= { createElement: () => ({ width: 0, height: 0, style: {}, getContext: () => null }) };
const gPackage = require("g.js");
gPackage._toSVG = gPackage.toSVG;

const [dataDir, ...targets] = process.argv.slice(2);
const frame = Number(process.env.FRAME ?? 1);
// Text nodes measure and outline with this font unless a project supplies its own.
const defaultFontFile =
  process.env.DEFAULT_FONT ?? path.resolve(import.meta.dirname, "../../server/static/fonts/FiraSans-Regular.woff");

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

function loadProject(key: string): ClassicProject {
  const [userId, projectId] = key.split("/");
  const project = JSON.parse(fs.readFileSync(path.join(dataDir, userId, projectId, "project.json"), "utf-8"));
  project.id = projectId;
  return project;
}

/** Assets reach the function sources as the original runtime delivered them. */
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
    else {
      const buffer = fs.readFileSync(file);
      assets[fileName] = buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength);
    }
  }
  return assets;
}

function shapeName(v: any): string | null {
  const name = v?.constructor?.name;
  return ["Path", "Group", "Point", "Color", "Text", "Img"].includes(name) ? name : null;
}

function pointCount(v: any): number {
  if (v instanceof gPackage.Path) return v.commands.length;
  if (v instanceof gPackage.Group) return v.shapes.reduce((n: number, s: any) => n + pointCount(s), 0);
  return 0;
}

function summarize(v: unknown, depth = 0): string {
  if (v === null || v === undefined) return "null";
  if (typeof v === "number") return Number.isInteger(v) ? String(v) : v.toFixed(4);
  if (typeof v === "boolean") return String(v);
  if (typeof v === "string") return JSON.stringify(v.length > 60 ? v.slice(0, 60) + "…" : v);
  if (Array.isArray(v)) {
    if (depth > 2) return `[…${v.length}]`;
    return `[${v
      .slice(0, 5)
      .map((e) => summarize(e, depth + 1))
      .join(", ")}${v.length > 5 ? `, …${v.length}` : ""}]`;
  }
  const name = shapeName(v);
  if (name === "Point") return `${(v as any).x.toFixed(2)},${(v as any).y.toFixed(2)}`;
  if (name === "Color") return `<Color ${String(v)}>`;
  if (name === "Path" || name === "Group") return `<${name} ${pointCount(v)} pts>`;
  if (name) return `<${name}>`;
  if (typeof v === "object") {
    const keys = Object.keys(v as object);
    const shown = keys.slice(0, 6).map((k) => `${k}=${summarize((v as any)[k], depth + 1)}`);
    return `{${shown.join(", ")}${keys.length > 6 ? ", …" : ""}}`;
  }
  return String(v);
}

for (const target of targets) {
  const [userId, projectId, fnName = "main"] = target.split("/");
  const key = `${userId}/${projectId}`;
  let document;
  const t0 = Date.now();
  try {
    const project = loadProject(key);
    const scripts: string[] = [];
    const assets = loadAssets(key, project, scripts);
    if (fs.existsSync(defaultFontFile)) {
      const font = fs.readFileSync(defaultFontFile);
      assets["default-font"] = font.buffer.slice(font.byteOffset, font.byteOffset + font.byteLength);
    }
    // A dependency that is no longer a classic project (core/g was rewritten in a later format) is
    // left out, so that the classic library built into the core is used for it instead.
    const dependencies = Object.keys(project.dependencies ?? {})
      .map((depKey) => ({ key: depKey, project: loadProject(depKey) }))
      .filter((dependency) => isClassicProject(dependency.project));
    for (const dependency of dependencies)
      Object.assign(assets, loadAssets(dependency.key, dependency.project, scripts));
    document = openClassicProject(key, project, {
      dependencies,
      assets,
      namespaces: { g: gPackage },
      // g.js was written against opentype.js 1.x; 2.x builds different outlines.
      globals: { _: require("lodash"), opentype: require("opentype-classic") },
    });
    for (const script of scripts) document.runtime.loadScript(script);
  } catch (e) {
    console.log(`FAIL  ${target}: could not load: ${(e as Error).message}`);
    continue;
  }
  if (process.env.WARNINGS && document.warnings.length > 0) {
    for (const warning of document.warnings.slice(0, 10)) console.log(`  warn: ${warning}`);
  }
  document.runtime.ndbx._currentFrame = frame;
  const context = new NodeContext(document.library, document.functions, { data: { frame } });
  try {
    const results = await context.renderEntryPoint(`/${fnName}`);
    console.log(`OK    ${target}: ${results.length} results ${summarize(results)} (${Date.now() - t0} ms)`);
  } catch (e) {
    console.log(`FAIL  ${target}: ${(e as Error).message}`);
  }
  if (process.env.VERBOSE) {
    const entries = [...context.renderResults.entries()].sort((a, b) => a[0].localeCompare(b[0]));
    for (const [nodePath, outputs] of entries) {
      const values = outputs.get("output") ?? [];
      console.log(`  ${nodePath} -> ${values.length}: ${summarize(values)}`);
    }
  }
}
