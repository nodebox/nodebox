import { babel } from "@rollup/plugin-babel";
import commonjs from "@rollup/plugin-commonjs";
import { nodeResolve } from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";
import { globSync } from "glob";
import fs from "fs";
import path from "path";

function ensureDirectory(...segments) {
  const dir = path.join(...segments);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
  return dir;
}

const functionsToJSON = () => {
  return {
    name: "functions-to-json",

    buildStart() {
      const pattern = path.resolve("functions", "**/*.js");
      const files = globSync(pattern, { windowsPathsNoEscape: true });
      files.forEach((file) => {
        this.addWatchFile(file);
      });
    },

    async generateBundle(libraryName) {
      function generateProject(projectName) {
        const project = { formatVersion: 1, title: projectName, dependencies: {}, assets: {}, items: [] };
        const pattern = path.resolve("functions", projectName, "*.js");
        const files = globSync(pattern, { windowsPathsNoEscape: true });
        let id = 1;
        files
          .sort((a, b) => a.localeCompare(b))
          .forEach((file) => {
            let slug = path.basename(file, ".js");
            const name = slug.replace(/-/g, " ").replace(/\b\w/g, (char) => char.toUpperCase());
            const source = fs.readFileSync(file, "utf-8");
            project.items.push({ type: "FUNCTION", id: `0:${id++}`, name, source });
          });
        const projectDirectory = ensureDirectory("..", "server", "data", "core", projectName);
        const projectFile = path.join(projectDirectory, "project.json");
        fs.writeFileSync(projectFile, JSON.stringify(project, null, 2));
        const publishedDirectory = ensureDirectory("..", "server", "data", "core", projectName, "versions");
        const publishedProjectFile = path.join(publishedDirectory, "published.json");
        fs.writeFileSync(publishedProjectFile, JSON.stringify(project, null, 2));
        const libDirectory = ensureDirectory("..", "server", "data", "core", projectName, "lib");
        const utilitySource = fs.readFileSync(path.resolve("functions", projectName, "utilities.js"));
        fs.writeFileSync(path.join(libDirectory, "utilities.js"), utilitySource);
      }

      generateProject("g");
      generateProject("plot");
    },
  };
};

export default {
  input: "src/index.ts",
  output: [
    {
      file: "dist/ndbx.esm.js",
      format: "esm",
      sourcemap: true,
    },
  ],
  plugins: [
    nodeResolve({ browser: true }), // Resolves third-party modules in node_modules
    babel({ babelHelpers: "bundled" }), // Transpiles JavaScript using Babel
    commonjs({
      requireReturnsDefault: "auto",
    }),
    typescript(),
    functionsToJSON(),
  ],
  // The classic NodeBox Live packages stay outside the bundle: they are loaded on demand, and only
  // by a project in the classic format.
  external: ["react", "react-dom", "@ndbx/core", "g.js", "opentype-classic", "lodash"],
};
