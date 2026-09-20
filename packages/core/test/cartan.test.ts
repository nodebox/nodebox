// The Cartan Node Library (https://support.nodebox.net, "Cartan Node Library 3.7") is the largest
// third-party NodeBox 3 document around: 205 nodes and their demos, 915k nodes in one 274 MB file.
// It is not in the repository; point NODEBOX_CARTAN_LIBRARY at the .ndbx to run this check.
import { describe, expect, it } from "vitest";
import fs from "node:fs";
import { createContext, openNdbx, setTextFileReader } from "../src";
import { installBundledFonts } from "../src/fonts/node";

const file = process.env.NODEBOX_CARTAN_LIBRARY;
const present = file !== undefined && fs.existsSync(file);

describe("Cartan Node Library", () => {
  it.skipIf(!present)(
    "loads with every prototype resolved and renders its pure subnetworks",
    async () => {
      setTextFileReader((f) => fs.readFileSync(f, "utf-8"));
      installBundledFonts();
      const { library, warnings } = openNdbx(fs.readFileSync(file!, "utf-8"), { file });
      expect(warnings.filter((w) => w.includes("could not be found"))).toEqual([]);
      expect(library.root.children.length).toBeGreaterThan(400);
      // A few demos that only use built-in nodes.
      for (const name of ["spiral_", "gear_", "hexagons_", "arrow_", "donut_", "waveform_"]) {
        const context = createContext(library);
        const result = await context.render(`/${name}`);
        expect(result.length, name).toBeGreaterThan(0);
      }
    },
    600000,
  );
});
