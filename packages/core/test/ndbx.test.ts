import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import {
  builtinNodeRepository,
  getChild,
  getInput,
  openNdbx,
  parseNdbx,
  parseXml,
  renderLibrary,
  saveNdbx,
  serializeXml,
  upgradeXml,
} from "../src";

const root = path.resolve(__dirname, "..", "..", "..");

function read(rel: string): string {
  return fs.readFileSync(path.join(root, rel), "utf-8");
}

describe("xml", () => {
  it("round-trips a document", () => {
    const xml = '<?xml version="1.0"?>\n<a x="1 &amp; 2"><b/><c>text &lt; </c><!-- hi --><![CDATA[<raw>]]></a>';
    const doc = parseXml(xml);
    expect(doc.root.tagName).toBe("a");
    expect(doc.root.getAttribute("x")).toBe("1 & 2");
    expect(doc.root.childElements.map((e) => e.tagName)).toEqual(["b", "c"]);
    const out = serializeXml(doc);
    expect(out).toContain('x="1 &amp; 2"');
    expect(parseXml(out).root.firstChildElement("c")!.textContent).toBe("text < ");
  });
});

describe("builtin libraries", () => {
  it("load with their nodes", () => {
    const repo = builtinNodeRepository();
    expect(repo.getNode("corevector.rect")).toBeDefined();
    expect(repo.getNode("core.network")!.isNetwork).toBe(true);
    const rect = repo.getNode("corevector.rect")!;
    expect(rect.inputs.map((p) => p.name)).toEqual(["position", "width", "height", "roundness"]);
    expect(rect.function).toBe("corevector/rect");
    const colorize = repo.getNode("corevector.colorize")!;
    // The filter prototype contributes the shape port; the node adds its own.
    expect(colorize.inputs.map((p) => p.name)).toEqual(["shape", "fill", "stroke", "strokeWidth"]);
    expect(getInput(colorize, "strokeWidth")!.min).toBe(0);
    expect(repo.getNode("math.wave")!.inputs.find((p) => p.name === "type")!.menuItems.length).toBeGreaterThan(0);
    expect(repo.getNodes().length).toBeGreaterThan(140);
  });
});

describe("upgrades", () => {
  const files = fs
    .readdirSync(path.join(root, "src/test/files"))
    .filter((f) => /^upgrade-v\d+\.ndbx$/.test(f) && f !== "upgrade-v999.ndbx")
    .sort((a, b) => parseInt(a.match(/\d+/)![0], 10) - parseInt(b.match(/\d+/)![0], 10));

  for (const file of files) {
    it(`upgrades ${file}`, () => {
      const result = upgradeXml(read(`src/test/files/${file}`));
      expect(result.toVersion).toBe("22");
      const { library } = parseNdbx(result.xml, { name: "up" });
      expect(library.root).toBeDefined();
    });
  }

  it("rejects NodeBox 2 files and files from the future", () => {
    expect(() => upgradeXml(read("src/test/files/upgrade-v0.9.ndbx"))).toThrow(/NodeBox 2/);
    expect(() => upgradeXml(read("src/test/files/upgrade-v999.ndbx"))).toThrow(/too new/);
  });

  it("rotates positions from version 1", () => {
    const result = upgradeXml(read("src/test/files/upgrade-v1.ndbx"));
    expect(result.warnings.some((w) => w.includes("rotated"))).toBe(true);
  });

  it("converts copy scale to percentages (20 -> 21)", () => {
    const xml = `<ndbx formatVersion="20" type="file"><node name="root" prototype="core.network"><node name="copy1" prototype="corevector.copy"><port name="scale" type="point" value="0.50,-0.50"/></node></node></ndbx>`;
    const result = upgradeXml(xml, "21");
    expect(result.xml).toContain('value="150.00,50.00"');
  });

  it("renames ports and prototypes (2 -> 4)", () => {
    const xml = `<ndbx formatVersion="2" type="file"><node name="root" prototype="core.network" renderedChild="to_integer1"><node name="to_integer1" prototype="math.to_integer"/><node name="to_points1" prototype="corevector.to_points"><port name="shape" type="geometry"/></node><conn input="to_points1.shape" output="to_integer1"/></node></ndbx>`;
    const result = upgradeXml(xml, "4");
    expect(result.xml).toContain('prototype="math.round"');
    expect(result.xml).toContain('renderedChild="round1"');
    expect(result.xml).toContain('prototype="corevector.point"');
    expect(result.xml).toContain('input="point1.value"');
    expect(result.xml).toContain('output="round1"');
  });
});

describe("ndbx documents", () => {
  const examples = collectNdbx(path.join(root, "examples"));

  it("finds the example documents", () => {
    expect(examples.length).toBeGreaterThan(40);
  });

  for (const file of examples) {
    it(`loads ${path.relative(root, file)}`, () => {
      const { library, warnings } = openNdbx(fs.readFileSync(file, "utf-8"), { file });
      expect(warnings.filter((w) => w.includes("could not be found"))).toEqual([]);
      expect(library.root.isNetwork).toBe(true);
      expect(library.root.children.length).toBeGreaterThan(0);
    });
  }

  it("round-trips a document through the writer", () => {
    const file = path.join(root, "examples/01 Basics/08 Subnetworks/Mesh/Mesh.ndbx");
    const { library } = openNdbx(fs.readFileSync(file, "utf-8"), { file });
    const xml = saveNdbx(library);
    const { library: again } = openNdbx(xml, { name: "Mesh" });
    expect(again.root.children.map((c) => c.name).sort()).toEqual(library.root.children.map((c) => c.name).sort());
    const mesh = getChild(again.root, "mesh")!;
    expect(mesh.inputs.filter((p) => p.childReference).map((p) => p.name)).toEqual(["list", "start_index", "point2"]);
    expect(again.root.connections).toHaveLength(library.root.connections.length);
    expect(again.properties).toEqual({ canvasHeight: "1000", canvasWidth: "1000" });
    expect(saveNdbx(again)).toBe(xml);
  });

  it("renders the Mesh example", async () => {
    const file = path.join(root, "examples/01 Basics/08 Subnetworks/Mesh/Mesh.ndbx");
    const { library } = openNdbx(fs.readFileSync(file, "utf-8"), { file });
    const result = await renderLibrary(library);
    expect(result.length).toBeGreaterThan(0);
  });
});

function collectNdbx(dir: string): string[] {
  const out: string[] = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...collectNdbx(p));
    else if (entry.name.endsWith(".ndbx")) out.push(p);
  }
  return out.sort();
}
