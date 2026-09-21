import { writeFileSync, mkdirSync } from "fs";
import { join } from "path";
import { format } from "prettier";
const LIVE_PLOT_PROJECT_URL = `https://new.nodebox.live/api/projects/jonasverstraete/dep2407plot/dev`;
const LIVE_GALLERY_URL = `https://new.nodebox.live/api/projects/example`;
const PLOT_DIRECTORY = "packages/runtime/functions/plot";
const EXAMPLES_DIRECTORY = "packages/server/data/example";
mkdirSync(PLOT_DIRECTORY, { recursive: true });

async function convertCorePlotProject() {
  const response = await fetch(LIVE_PLOT_PROJECT_URL);
  const data = await response.json();
  if (data.status !== "ok") {
    throw new Error("Failed to download project.json file.");
  }

  const project = data.project;
  for (const item of project.items) {
    console.assert(item.type === "FUNCTION", `Expected item ${item.name} to be a function, got "${item.type}".`);
    const slug = item.name.replace(/[^a-z0-9]/gi, "-").toLowerCase();
    const filePath = join(PLOT_DIRECTORY, `${slug}.js`);
    console.log(item.name, "->", filePath);
    const formattedSource = await format(item.source, {
      parser: "babel",
      printWidth: 120,
      tabWidth: 2,
      useTabs: false,
    });
    writeFileSync(filePath, formattedSource);
  }
}

async function convertExampleProjects() {
  const response = await fetch(LIVE_GALLERY_URL);
  const data = await response.json();
  if (data.status !== "ok") {
    throw new Error("Failed to download example projects.");
  }

  const projects = data.projects;
  for (const project of projects) {
    await convertExampleProject(project.id, `${LIVE_GALLERY_URL}/${project.id}/dev`);
  }
  const profile = {
    login: "example",
    projects,
  };
  writeFileSync(join(EXAMPLES_DIRECTORY, "profile.json"), JSON.stringify(profile, null, 2));
}

async function convertExampleProject(id, url) {
  console.log(`Converting ${id} example project...`);
  const response = await fetch(url);
  const data = await response.json();
  if (data.status !== "ok") {
    throw new Error("Failed to download project.json file.");
  }

  const project = data.project;
  const dependencies = Object.keys(project.dependencies);
  console.assert(dependencies.includes("core/g"), `Expected project to depend on "core/g".`);
  dependencies.splice(dependencies.indexOf("core/g"), 1);
  // There should be one other dependency left.
  console.assert(
    dependencies.length === 1,
    `Expected project to have one other dependency, got ${dependencies.length}.`,
  );
  const plotDependency = dependencies[0];
  console.assert(plotDependency.includes("plot"), `Expected project to depend on plot library.`);
  project.dependencies = { "core/g": "dev", "core/plot": "dev" };
  const projectDirectory = join(EXAMPLES_DIRECTORY, id);
  mkdirSync(projectDirectory, { recursive: true });
  const projectFile = join(projectDirectory, "project.json");
  // Go through all networks in the project
  for (const item of project.items) {
    if (item.type !== "NETWORK") continue;
    // Replace all nodes that depend on the plot dependency with the new dependency.
    for (const node of item.children) {
      if (node.type !== "NODE") continue;
      if (node.fn.startsWith(plotDependency)) {
        node.fn = node.fn.replace(plotDependency, "core/plot");
      }
    }
  }
  writeFileSync(projectFile, JSON.stringify(project, null, 2));
}

await convertCorePlotProject();
await convertExampleProjects();
