import { babel } from "@rollup/plugin-babel";
import commonjs from "@rollup/plugin-commonjs";
import { nodeResolve } from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";

export default {
  input: "src/index.ts",
  output: [
    {
      file: "dist/g.esm.js",
      format: "esm",
      sourcemap: true,
    },
  ],
  plugins: [
    nodeResolve(), // Resolves third-party modules in node_modules
    babel({ babelHelpers: "bundled" }), // Transpiles JavaScript using Babel
    commonjs({
      requireReturnsDefault: "auto",
    }),
    typescript(),
  ],
};
