// Compare a Java reference trace (scripts/java-reference) with a TypeScript trace from
// ndbx-inspect.mts (NDBX_INSPECT_PLAIN=1): both print "  /path -> count: [values]" lines.
//
//   node --import tsx scripts/ndbx-compare-traces.mts demo.java.log demo.ts.log [--limit N] [--kinds count,values]

import fs from "node:fs";

const args = process.argv.slice(2);
const [javaFile, tsFile] = args;
const limitIndex = args.indexOf("--limit");
const limit = limitIndex >= 0 ? parseInt(args[limitIndex + 1], 10) : 15;
const kindsIndex = args.indexOf("--kinds");
const kinds = kindsIndex >= 0 ? new Set(args[kindsIndex + 1].split(",")) : null;

type Entry = { count: number; values: string };

function parse(file: string): Map<string, Entry> {
  const out = new Map<string, Entry>();
  for (const line of fs.readFileSync(file, "utf-8").split("\n")) {
    const m = /^ {2}(\/\S+) -> (\d+): (.*)$/.exec(line);
    if (m) out.set(m[1], { count: parseInt(m[2], 10), values: m[3] });
  }
  return out;
}

/** Strip formatting differences between Java's toString and the TypeScript summaries. */
function normalize(s: string): string {
  return s
    .replace(/\?/g, "…")
    .replace(/nodebox\.graphics\.Contour@[0-9a-f]+/g, "<Contour>")
    .replace(/(-?\d+(?:\.\d+)?)[eE]([-+]?\d+)/g, (m) => String(parseFloat(m)))
    .replace(/(-?\d+)\.0\b/g, "$1")
    .replace(/(-?\d+\.\d\d)\d+/g, "$1")
    .replace(/(?<![\w.])-0(\.00?)?\b/g, "0")
    .replace(/\{([^}]*)\}/g, (_, inner: string) => `{${inner.replace(/["=:, ]/g, "")}}`)
    .replace(/\s+/g, "");
}

function kindOf(a: Entry, b: Entry): string {
  if (a.count !== b.count) return "count";
  const na = normalize(a.values);
  const nb = normalize(b.values);
  if (na === nb) return "same";
  if (/<(Path|Geometry) \d+ pts>/.test(a.values) && na.replace(/\d+pts/g, "N") === nb.replace(/\d+pts/g, "N"))
    return "pointcount";
  if (a.values.includes("{")) return "maps";
  if (a.values.includes("<Path") || a.values.includes("<Geometry")) return "geometry";
  if (/^\[-?\d/.test(a.values)) return "numbers";
  return "values";
}

const java = parse(javaFile);
const ts = parse(tsFile);
const diffs: { path: string; kind: string; a?: Entry; b?: Entry }[] = [];
const paths = [...new Set([...java.keys(), ...ts.keys()])].sort(
  (p, q) => p.split("/").length - q.split("/").length || p.localeCompare(q),
);
for (const path of paths) {
  const a = java.get(path);
  const b = ts.get(path);
  if (!a || !b) diffs.push({ path, kind: "missing", a, b });
  else {
    const kind = kindOf(a, b);
    if (kind !== "same") diffs.push({ path, kind, a, b });
  }
}
const tally: Record<string, number> = {};
for (const d of diffs) tally[d.kind] = (tally[d.kind] ?? 0) + 1;
console.log(`${java.size} java nodes, ${ts.size} ts nodes, ${diffs.length} differences ${JSON.stringify(tally)}`);
let shown = 0;
for (const d of diffs) {
  if (kinds && !kinds.has(d.kind)) continue;
  if (shown++ >= limit) break;
  console.log(
    `${d.kind.padEnd(12)} ${d.path}\n    java: ${d.a?.values.slice(0, 150) ?? "-"}\n    ts:   ${d.b?.values.slice(0, 150) ?? "-"}`,
  );
}
