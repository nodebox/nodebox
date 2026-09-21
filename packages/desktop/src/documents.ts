// Opening and saving NodeBox documents on disk. A .ndbx file (NodeBox 3) becomes a project the
// editor can show, evaluated by the core engine; a project.json is a NodeBox Live project as is.

import fs from "node:fs/promises";
import path from "node:path";
import {
  builtinNodeRepository,
  detectFormat,
  libraryToLiveProject,
  openLive,
  openNdbx,
  saveNdbx,
  type LiveProject,
} from "@ndbx/core";

export interface OpenedDocument {
  format: "ndbx" | "live";
  project: LiveProject;
  warnings: string[];
  file: string;
}

export async function openDocument(file: string): Promise<OpenedDocument> {
  const text = await fs.readFile(file, "utf-8");
  const format = detectFormat(text);
  if (format === "ndbx") {
    const { library, warnings } = openNdbx(text, { file });
    const project = libraryToLiveProject(library);
    project.id = projectIdFor(file);
    project.title = path.basename(file, path.extname(file));
    return { format, project, warnings, file };
  }
  if (format === "live") {
    const project = JSON.parse(text) as LiveProject;
    project.id = project.id ?? projectIdFor(file);
    return { format, project, warnings: [], file };
  }
  throw new Error(`${file} is not a NodeBox document.`);
}

/** Save a project as .ndbx when it can be expressed as one, otherwise as project.json. */
export async function saveDocument(project: LiveProject, file: string, dependencies: LiveProject[] = []): Promise<"ndbx" | "live"> {
  if (file.toLowerCase().endsWith(".ndbx")) {
    const repository = builtinNodeRepository();
    const { library } = openLive(project, { projectKey: "self/self", repository, dependencies: [] });
    library.name = path.basename(file, ".ndbx");
    void dependencies;
    await fs.writeFile(file, saveNdbx(library, repository), "utf-8");
    return "ndbx";
  }
  await fs.writeFile(file, JSON.stringify(project, null, 2), "utf-8");
  return "live";
}

/** A project id the server accepts: lowercase letters, digits and dashes, 3 to 32 characters. */
export function projectIdFor(file: string): string {
  const base = path
    .basename(file, path.extname(file))
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 28);
  return (base.length >= 3 ? base : `doc-${base}`).padEnd(3, "0");
}
