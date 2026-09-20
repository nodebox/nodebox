// Render every top-level child of a document that matches a name pattern and report which ones
// succeed. Used to check third-party libraries such as the Cartan Node Library, whose demo
// networks are the top-level nodes ending in "_".
//
//   node --max-old-space-size=12000 --import tsx scripts/ndbx-render-children.mts file.ndbx [regex] [--limit N]

import fs from "node:fs";
import path from "node:path";
import { createContext, openNdbx, setTextFileReader, toG } from "../src/index.ts";
import { installBundledFonts } from "../src/fonts/node.ts";

const args = process.argv.slice(2);
const file = args[0];
const pattern = new RegExp(args[1] && !args[1].startsWith("--") ? args[1] : "_$");
const limitIndex = args.indexOf("--limit");
const limit = limitIndex >= 0 ? parseInt(args[limitIndex + 1], 10) : Infinity;
setTextFileReader((f) => fs.readFileSync(f, "utf-8"));
installBundledFonts();

const { library } = openNdbx(fs.readFileSync(file, "utf-8"), { file: path.resolve(file) });
const names = library.root.children
  .map((c) => c.name)
  .filter((n) => pattern.test(n))
  .slice(0, limit);
let ok = 0;
let empty = 0;
let failed = 0;
const failures = new Map<string, string[]>();
for (const name of names) {
  const context = createContext(library, { data: { frame: 12 } });
  const t0 = Date.now();
  try {
    const result = await Promise.race([
      context.render(`/${name}`),
      new Promise<never>((_, reject) => setTimeout(() => reject(new Error("timeout (20 s)")), 20000)),
    ]);
    const shape = toG(result);
    const ms = Date.now() - t0;
    if (result.length === 0) {
      empty++;
      console.log(`EMPTY ${name} (${ms} ms)`);
    } else {
      ok++;
      console.log(`OK    ${name}: ${result.length} results${shape ? "" : " (no shape)"} (${ms} ms)`);
    }
  } catch (e) {
    failed++;
    const message = (e as Error).message.replace(/^Error rendering [^:]*: /, "").split("\n")[0];
    const key = message.replace(/'[^']*'/g, "'…'").slice(0, 80);
    failures.set(key, [...(failures.get(key) ?? []), name]);
    console.log(`FAIL  ${name}: ${message.slice(0, 200)}`);
  }
}
console.log(`\n${ok} rendered, ${empty} empty, ${failed} failed of ${names.length}`);
for (const [message, nodes] of [...failures.entries()].sort((a, b) => b[1].length - a[1].length)) {
  console.log(`  ${nodes.length}x ${message}: ${nodes.slice(0, 8).join(", ")}${nodes.length > 8 ? ", …" : ""}`);
}
