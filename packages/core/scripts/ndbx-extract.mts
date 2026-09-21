// Write top-level nodes of a (large) document into small documents of their own, for quick
// debugging: each output file's root contains the node and renders it.
//   node --max-old-space-size=12000 --import tsx scripts/ndbx-extract.mts file.ndbx outDir name1 name2 ...
import fs from "node:fs";
import path from "node:path";
import { cloneNode, createLibrary, getChild, openNdbx, saveNdbx } from "../src/index.ts";

const [file, outDir, ...names] = process.argv.slice(2);
const { library } = openNdbx(fs.readFileSync(file, "utf-8"), { file: path.resolve(file) });
fs.mkdirSync(outDir, { recursive: true });
for (const name of names) {
  const node = getChild(library.root, name);
  if (!node) {
    console.log(`no node ${name}`);
    continue;
  }
  const extract = createLibrary(name);
  extract.properties = { ...library.properties };
  extract.functionLinks = library.functionLinks.map((l) => ({ ...l }));
  extract.root.children.push(cloneNode(node));
  extract.root.renderedChild = name;
  const out = path.join(outDir, `${name}.ndbx`);
  fs.writeFileSync(out, saveNdbx(extract));
  console.log(`${out}: ${(fs.statSync(out).size / 1024).toFixed(0)} kB`);
}
