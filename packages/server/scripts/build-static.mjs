#!/usr/bin/env node

/**
 * Assemble the static assets folder served by the worker:
 *   - the web app build (packages/web/dist)
 *   - guide media under /guide/media
 *   - guide markdown under /_guide, read by the worker to render /guide/:slug
 *
 * Usage:
 *   node scripts/build-static.mjs
 */

import { cpSync, existsSync, mkdirSync, readdirSync, rmSync } from "fs";
import { dirname, join, resolve } from "path";
import { fileURLToPath } from "url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const webDist = resolve(root, "../web/dist");
const guide = resolve(root, "../../doc/guide");
const out = join(root, "static");

if (!existsSync(webDist)) {
  console.error(`Web build not found at ${webDist}. Run "npm run web:build" first.`);
  process.exit(1);
}

rmSync(out, { recursive: true, force: true });
mkdirSync(out, { recursive: true });
cpSync(webDist, out, { recursive: true });
cpSync(join(guide, "media"), join(out, "guide", "media"), { recursive: true });

const guideOut = join(out, "_guide");
mkdirSync(guideOut, { recursive: true });
for (const name of readdirSync(guide)) {
  if (name.endsWith(".md")) cpSync(join(guide, name), join(guideOut, name));
}
console.log(`Static assets assembled in ${out}`);
