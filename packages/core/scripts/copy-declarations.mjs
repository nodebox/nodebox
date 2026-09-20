// Hand-written ambient declarations are not emitted by tsc; copy them next to the generated ones.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
for (const file of ["fonts/opentype.d.ts"]) {
  fs.mkdirSync(path.dirname(path.join(root, "dist", file)), { recursive: true });
  fs.copyFileSync(path.join(root, "src", file), path.join(root, "dist", file));
}
