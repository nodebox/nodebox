// Render a top-level node of a document (errors are caught) and print the intermediate results of
// every node under the given path prefixes, as evaluated in context, to see what a failing node got.
//   node --max-old-space-size=12000 --import tsx scripts/ndbx-inspect.mts file.ndbx /demo_ /demo_/sub/network ...
import fs from "node:fs";
import path from "node:path";
import { NodeRenderError, createContext, openNdbx, setTextFileReader } from "../src/index.ts";

const [file, ...specs] = process.argv.slice(2);
setTextFileReader((f) => fs.readFileSync(f, "utf-8"));
const { library } = openNdbx(fs.readFileSync(file, "utf-8"), { file: path.resolve(file) });

function summarize(v: unknown): string {
  if (v === null || v === undefined) return "null";
  if (typeof v === "number" || typeof v === "boolean") return String(v);
  if (typeof v === "string") return JSON.stringify(v.length > 40 ? v.slice(0, 40) + "…" : v);
  if (Array.isArray(v)) return `[${v.slice(0, 5).map(summarize).join(", ")}${v.length > 5 ? `, …${v.length}` : ""}]`;
  const name = (v as object).constructor?.name ?? "Object";
  if (name === "Object") return JSON.stringify(v).slice(0, 80);
  if (name === "Point") return String(v);
  if (name === "Path" || name === "Geometry") return `<${name} ${(v as { pointCount: number }).pointCount} pts>`;
  return `<${name}>`;
}

// Each spec is "/demo" or "/demo:/prefix1,/prefix2".
for (const spec of specs) {
  const [demo, prefixList] = spec.split(":");
  const prefixes = prefixList ? prefixList.split(",") : [demo];
  const context = createContext(library, { data: { frame: 12 } });
  let status = "OK";
  try {
    const result = await context.render(demo);
    status = `OK ${result.length} results`;
  } catch (e) {
    status = e instanceof NodeRenderError ? `FAIL at ${e.nodePath}: ${(e.cause as Error).message}` : `FAIL ${(e as Error).message}`;
  }
  console.log(`\n${demo}: ${status}`);
  const entries = [...context.renderResults.entries()].filter(([p]) => prefixes.some((prefix) => p === prefix || p.startsWith(prefix + "/")));
  for (const [nodePath, results] of entries.sort((a, b) => a[0].localeCompare(b[0]))) {
    const node = context.getNodeForPath(nodePath)!;
    const parent = context.getNodeForPath(nodePath.slice(0, nodePath.lastIndexOf("/")) || "/")!;
    const ports = node.inputs.map((p) => {
      const conn = parent.connections.find((c) => c.inputNode === node.name && c.inputPort === p.name);
      return `${p.name}${p.range === "list" ? "[]" : ""}=${conn ? "<" + conn.outputNode + ">" : p.childReference ? "->" + p.childReference : summarize(p.value)}`;
    });
    const out = results.get("output") ?? [];
    console.log(`  ${nodePath} [${node.prototype}] (${ports.join(", ")}) -> ${out.length}: ${summarize(out)}`);
  }
}
