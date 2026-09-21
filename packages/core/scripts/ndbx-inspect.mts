// Render a top-level node of a document (errors are caught) and print the intermediate results of
// every node under the given path prefixes, as evaluated in context, to see what a failing node got.
//   node --max-old-space-size=12000 --import tsx scripts/ndbx-inspect.mts file.ndbx /demo_ /demo_/sub/network ...
import fs from "node:fs";
import path from "node:path";
import { NodeRenderError, createContext, openNdbx, setTextFileReader } from "../src/index.ts";
import { installBundledFonts } from "../src/fonts/node.ts";

const argv = process.argv.slice(2);
// --override node.port=value forces a port value (numbers and booleans are parsed).
const overrides: Record<string, unknown> = {};
for (let i = argv.length - 1; i >= 0; i--) {
  if (argv[i] === "--override" && argv[i + 1]) {
    const [key, raw] = argv[i + 1].split("=");
    overrides[key] = raw === "true" ? true : raw === "false" ? false : Number.isNaN(Number(raw)) ? raw : Number(raw);
    argv.splice(i, 2);
  }
}
const [file, ...specs] = argv;
setTextFileReader((f) => fs.readFileSync(f, "utf-8"));
installBundledFonts();
const { library } = openNdbx(fs.readFileSync(file, "utf-8"), { file: path.resolve(file) });

function summarize(v: unknown): string {
  if (v === null || v === undefined) return "null";
  if (typeof v === "number" || typeof v === "boolean") return String(v);
  if (typeof v === "string") return JSON.stringify(v.length > 200 ? v.slice(0, 200) + "…" : v);
  if (Array.isArray(v)) return `[${v.slice(0, 5).map(summarize).join(", ")}${v.length > 5 ? `, …${v.length}` : ""}]`;
  const name = (v as object).constructor?.name ?? "Object";
  if (name === "Object") return JSON.stringify(v).slice(0, 80);
  if (name === "Point") {
    const p = v as { x: number; y: number; type: number };
    return `${process.env.NDBX_INSPECT_POINTS ? ("?LCD"[p.type] ?? "?") : ""}${p.x.toFixed(2)},${p.y.toFixed(2)}`;
  }
  if (name === "Path" || name === "Geometry") {
    const shape = v as { pointCount: number; points: { x: number; y: number; type: number }[] };
    // NDBX_INSPECT_POINTS=1 prints the points of small paths (M/L/C for the point type).
    if (process.env.NDBX_INSPECT_POINTS && shape.pointCount <= 24)
      return `<${name} ${shape.points.map((p) => `${"?LCD"[p.type] ?? "?"}${p.x.toFixed(2)},${p.y.toFixed(2)}`).join(" ")}>`;
    return `<${name} ${shape.pointCount} pts>`;
  }
  return `<${name}>`;
}

// Each spec is "/demo" or "/demo:/prefix1#depth,/prefix2" (depth limits how deep below the prefix to print).
for (const spec of specs) {
  const [demo, prefixList] = spec.split(":");
  const prefixes = (prefixList ? prefixList.split(",") : [demo]).map((p) => {
    const [prefix, depth] = p.split("#");
    return { prefix, depth: depth ? parseInt(depth, 10) : Infinity };
  });
  const context = createContext(library, { data: { frame: 12 }, portOverrides: overrides });
  let status = "OK";
  try {
    const result = await context.render(demo);
    status = `OK ${result.length} results`;
  } catch (e) {
    status =
      e instanceof NodeRenderError
        ? `FAIL at ${e.nodePath}: ${(e.cause as Error).message}`
        : `FAIL ${(e as Error).message}`;
  }
  console.log(`\n${demo}: ${status}`);
  const entries = [...context.renderResults.entries()].filter(([p]) =>
    prefixes.some(
      ({ prefix, depth }) =>
        p === prefix || (p.startsWith(prefix + "/") && p.slice(prefix.length + 1).split("/").length <= depth),
    ),
  );
  for (const [nodePath, results] of entries.sort((a, b) => a[0].localeCompare(b[0]))) {
    const node = context.getNodeForPath(nodePath)!;
    const parent = context.getNodeForPath(nodePath.slice(0, nodePath.lastIndexOf("/")) || "/")!;
    const ports = node.inputs.map((p) => {
      const conn = parent.connections.find((c) => c.inputNode === node.name && c.inputPort === p.name);
      return `${p.name}${p.range === "list" ? "[]" : ""}=${conn ? "<" + conn.outputNode + ">" : p.childReference ? "->" + p.childReference : summarize(p.value)}`;
    });
    const out = results.get("output") ?? [];
    // NDBX_INSPECT_PLAIN=1 prints the same shape as the Java reference harness (path -> count: values).
    if (process.env.NDBX_INSPECT_PLAIN) console.log(`  ${nodePath} -> ${out.length}: ${summarize(out)}`);
    else console.log(`  ${nodePath} [${node.prototype}] (${ports.join(", ")}) -> ${out.length}: ${summarize(out)}`);
  }
}
