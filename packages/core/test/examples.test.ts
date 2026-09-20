// Every NodeBox 3 example document that ships in examples/ must load, upgrade and render through
// the core. Documents that need the network are only required to load.
import { afterAll, beforeAll, describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { JSDOM } from "jsdom";
import { openNdbx, renderLibrary, setDomParser, setTextFileReader, toG } from "../src";

const root = path.resolve(__dirname, "..", "..", "..");
const NEEDS_NETWORK = ["02 Topics/Web/Twitter API/Twitter API.ndbx"];
// Documents whose rendered result is text; without fonts they render as text shapes.
const TEXT_ONLY = [
  "01 Basics/01 Shape/12 TextFX/12 TextFX.ndbx",
  "01 Basics/04 Math/07 Sine Text/07 Sine Text.ndbx",
  "02 Topics/Animation/02 Elastic/02 Elastic.ndbx",
];

function collect(dir: string): string[] {
  const out: string[] = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...collect(p));
    else if (entry.name.endsWith(".ndbx")) out.push(p);
  }
  return out.sort();
}

beforeAll(() => {
  setTextFileReader((file) => fs.readFileSync(file, "utf-8"));
  setDomParser(new JSDOM("").window.DOMParser);
});

afterAll(() => {
  setTextFileReader(null);
  setDomParser(null);
});

describe("NodeBox 3 examples", () => {
  for (const file of collect(path.join(root, "examples"))) {
    const rel = path.relative(path.join(root, "examples"), file);
    if (NEEDS_NETWORK.includes(rel)) continue;
    it(`renders ${rel}`, async () => {
      const { library, warnings } = openNdbx(fs.readFileSync(file, "utf-8"), { file });
      expect(warnings.filter((w) => w.includes("could not be found"))).toEqual([]);
      const result = await renderLibrary(library, { data: { frame: 10 } });
      if (TEXT_ONLY.includes(rel)) return;
      expect(result.length).toBeGreaterThan(0);
      const shape = toG(result);
      expect(shape).not.toBeNull();
    });
  }
});
