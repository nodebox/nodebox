#!/usr/bin/env node

/**
 * Write a .dev.vars with local-only values, so wrangler dev runs without 1Password or live secrets.
 *
 * Usage:
 *   node scripts/setup-dev-vars.mjs
 */

import { writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(fileURLToPath(new URL("..", import.meta.url)));

const localDevVars = {
  JWT_SECRET: "local-dev-only-jwt-secret",
};

const lines = Object.entries(localDevVars).map(([key, value]) => `${key}=${value}`);
await writeFile(resolve(root, ".dev.vars"), `${lines.join("\n")}\n`, "utf8");
console.log("Wrote .dev.vars with local-only development values.");
