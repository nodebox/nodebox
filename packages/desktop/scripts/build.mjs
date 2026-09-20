// Bundle the Electron main and preload scripts. The renderer is the web app build (packages/web/dist),
// served by the same Hono application that runs on Cloudflare, here on localhost with a file store.
import { build } from "esbuild";
import { existsSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "..");
const webDist = resolve(root, "../web/dist");
if (!existsSync(webDist)) {
  console.warn(`Web build not found at ${webDist}; run "npm run web:build" before starting the desktop app.`);
}

const shared = {
  bundle: true,
  platform: "node",
  target: "node20",
  format: "cjs",
  sourcemap: true,
  external: ["electron"],
  // The server package still carries its pre-Cloudflare index.js next to index.ts.
  resolveExtensions: [".ts", ".tsx", ".mjs", ".js", ".json"],
  loader: { ".html": "text" },
  define: { "process.env.NODE_ENV": '"production"' },
  logLevel: "info",
};

await build({ ...shared, entryPoints: [resolve(root, "src/main.ts")], outfile: resolve(root, "dist/main.cjs") });
await build({ ...shared, entryPoints: [resolve(root, "src/preload.ts")], outfile: resolve(root, "dist/preload.cjs") });
