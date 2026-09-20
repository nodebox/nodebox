// Render the named top-level children of a document and print where each failure happened.
//   node --max-old-space-size=12000 --import tsx scripts/ndbx-debug-children.mts file.ndbx name1 name2 ...
import fs from "node:fs";
import path from "node:path";
import { NodeRenderError, createContext, openNdbx, setTextFileReader } from "../src/index.ts";

const [file, ...names] = process.argv.slice(2);
setTextFileReader((f) => fs.readFileSync(f, "utf-8"));
const { library } = openNdbx(fs.readFileSync(file, "utf-8"), { file: path.resolve(file) });
for (const name of names) {
  const context = createContext(library, { data: { frame: 12 } });
  try {
    const result = await context.render(`/${name}`);
    console.log(`OK    ${name}: ${result.length} results`);
  } catch (e) {
    if (e instanceof NodeRenderError) {
      const node = e.node;
      const cause = e.cause as Error;
      console.log(`FAIL  ${name}: at ${e.nodePath} [${node.prototype} -> ${node.function}] ${cause?.message}`);
      console.log(
        `      inputs: ${node.inputs.map((p) => `${p.name}:${p.type}${p.range === "list" ? "[]" : ""}`).join(", ")}`,
      );
      const frames = (cause?.stack ?? "")
        .split("\n")
        .slice(1, 5)
        .map((l) => l.trim().replace(/^at /, ""))
        .join(" | ");
      console.log(`      ${frames}`);
    } else {
      console.log(`FAIL  ${name}: ${(e as Error).stack?.split("\n").slice(0, 4).join(" | ")}`);
    }
  }
}
