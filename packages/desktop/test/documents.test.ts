import { describe, expect, it } from "vitest";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { openDocument, projectIdFor, saveDocument } from "../src/documents";

const root = path.resolve(__dirname, "..", "..", "..");

describe("documents", () => {
  it("opens a .ndbx as a project with native NodeBox 3 items", async () => {
    const file = path.join(root, "examples/01 Basics/08 Subnetworks/Mesh/Mesh.ndbx");
    const doc = await openDocument(file);
    expect(doc.format).toBe("ndbx");
    expect(doc.project.id).toBe("mesh");
    expect(doc.project.dependencies).toMatchObject({
      "nodebox/corevector": "dev",
      "nodebox/list": "dev",
      "nodebox/math": "dev",
    });
    const main = doc.project.items[0];
    expect(main.type).toBe("NETWORK");
    if (main.type !== "NETWORK") return;
    expect(main.children.filter((c) => c.type === "NODE").map((c) => (c as { fn: string }).fn)).toContain(
      "nodebox/corevector/colorize",
    );
    expect(main.children.filter((c) => c.type === "NODE").map((c) => (c as { fn: string }).fn)).toContain(
      "self/self/mesh",
    );
    const mesh = doc.project.items.find((i) => i.name === "mesh");
    expect(mesh?.type).toBe("NETWORK");
    if (mesh?.type !== "NETWORK") return;
    expect(mesh.inputPorts!.map((p) => p.name)).toEqual(["list", "start_index", "point2"]);
    expect(mesh.outputPorts!.map((p) => p.name)).toEqual(["output"]);
  });

  it("saves a project as .ndbx and as project.json", async () => {
    const dir = await fs.mkdtemp(path.join(os.tmpdir(), "ndbx-doc-"));
    const doc = await openDocument(path.join(root, "packages/server/data/skel/welcome/project.json"));
    expect(doc.format).toBe("live");
    const json = path.join(dir, "welcome.json");
    expect(await saveDocument(doc.project, json)).toBe("live");
    expect(JSON.parse(await fs.readFile(json, "utf-8")).title).toBe(doc.project.title);
    const ndbx = path.join(dir, "welcome.ndbx");
    expect(await saveDocument(doc.project, ndbx)).toBe("ndbx");
    expect(await fs.readFile(ndbx, "utf-8")).toContain('<ndbx type="file" formatVersion="22"');
    await fs.rm(dir, { recursive: true, force: true });
  });

  it("derives project ids from file names", () => {
    expect(projectIdFor("/tmp/My Document.ndbx")).toBe("my-document");
    expect(projectIdFor("/tmp/a.ndbx")).toBe("doc-a");
  });
});
