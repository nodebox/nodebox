// Check a .ndbx document against the core: load it, report unresolved prototypes and functions
// that no loaded library provides, and optionally render a node.
//
//   npx tsx scripts/ndbx-check.mts path/to/document.ndbx [--render [/node/path]] [--time]
//
// Run with a big heap for very large documents: node --max-old-space-size=8192 --import tsx ...

import fs from "node:fs";
import path from "node:path";
import { createContext, openNdbx, setTextFileReader, toG } from "../src/index.ts";

const args = process.argv.slice(2);
const file = args.find((a) => !a.startsWith("--"));
if (!file) {
  console.error("usage: ndbx-check <file.ndbx> [--render [/node/path]] [--time]");
  process.exit(2);
}
const renderIndex = args.indexOf("--render");
const renderPath =
  renderIndex >= 0 && args[renderIndex + 1] && args[renderIndex + 1].startsWith("/") ? args[renderIndex + 1] : "/";

setTextFileReader((f) => fs.readFileSync(f, "utf-8"));

const size = fs.statSync(file).size;
let t0 = Date.now();
const xml = fs.readFileSync(file, "utf-8");
console.log(`read ${(size / 1e6).toFixed(1)} MB in ${Date.now() - t0} ms`);
t0 = Date.now();
const { library, warnings, formatVersion } = openNdbx(xml, { file: path.resolve(file) });
console.log(`parsed (formatVersion ${formatVersion}) in ${Date.now() - t0} ms; ${warnings.length} warnings`);
for (const w of warnings.slice(0, 20)) console.log(`  warning: ${w}`);
if (warnings.length > 20) console.log(`  ... ${warnings.length - 20} more`);

// Walk the tree once: count nodes, prototypes and functions.
const prototypes = new Map<string, number>();
const functions = new Map<string, number>();
let nodeCount = 0;
let missingPrototypes = 0;
const visit = (node: typeof library.root) => {
  nodeCount++;
  if (node.meta.missingPrototype) missingPrototypes++;
  if (node.prototype) prototypes.set(node.prototype, (prototypes.get(node.prototype) ?? 0) + 1);
  if (!node.isNetwork) functions.set(node.function, (functions.get(node.function) ?? 0) + 1);
  for (const child of node.children) visit(child);
};
visit(library.root);
console.log(`${nodeCount} nodes, ${prototypes.size} distinct prototypes, ${missingPrototypes} with missing prototypes`);
console.log(`function links: ${library.functionLinks.map((l) => l.href).join(", ") || "(none)"}`);

const context = createContext(library);
const unavailable = [...functions.entries()].filter(([fn]) => !context.functionRepository.hasFunction(fn));
console.log(`${functions.size} distinct functions used; ${unavailable.length} not provided by any loaded library:`);
for (const [fn, count] of unavailable.sort((a, b) => b[1] - a[1])) console.log(`  ${fn} (${count} nodes)`);

if (renderIndex >= 0) {
  t0 = Date.now();
  try {
    const result = await context.render(renderPath);
    const shape = toG(result);
    console.log(
      `rendered ${renderPath}: ${result.length} results in ${Date.now() - t0} ms${shape ? `, bounds ${JSON.stringify(shape.getBounds())}` : ""}`,
    );
  } catch (e) {
    console.log(`render ${renderPath} failed after ${Date.now() - t0} ms: ${(e as Error).message}`);
  }
}
