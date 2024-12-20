import { babel } from "@rollup/plugin-babel";
import commonjs from "@rollup/plugin-commonjs";
import { nodeResolve } from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";
import { globSync } from "glob";
import fs from "fs";
import path from "path";

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
        const projectDirectory = path.join("..", "server", "data", "core", projectName);
        if (!fs.existsSync(projectDirectory)) {
          fs.mkdirSync(projectDirectory, { recursive: true });
        }
        const projectFile = path.join(projectDirectory, "project.json");
        fs.writeFileSync(projectFile, JSON.stringify(project, null, 2));
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
  external: ["react", "react-dom"],
};
