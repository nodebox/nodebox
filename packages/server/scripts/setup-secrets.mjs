#!/usr/bin/env node

/**
 * Hand the secrets in .env to the worker.
 *
 * Usage:
 *   node scripts/setup-secrets.mjs --local    # write .dev.vars for wrangler dev
 *   node scripts/setup-secrets.mjs --remote   # wrangler secret put for the deployed worker
 *
 * .env is generated from .env.template with `op inject` (see package.json "setup-secrets").
 */

import { spawn } from "node:child_process";
import { writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import dotenv from "dotenv";

const root = resolve(fileURLToPath(new URL("..", import.meta.url)));
dotenv.config({ path: resolve(root, ".env") });

// Only these reach the worker. The R2_* values stay in .env for the scripts.
const WORKER_SECRETS = ["JWT_SECRET"];

const missing = WORKER_SECRETS.filter((key) => !process.env[key]);
if (missing.length > 0) {
  console.error(`Missing in .env: ${missing.join(", ")}. Run "npm run setup-secrets" first.`);
  process.exit(1);
}

function escapeValue(value) {
  if (/[\s#"'`]/.test(value)) return `"${value.replace(/"/g, '\\"')}"`;
  return value;
}

function wranglerSecretPut(key, value) {
  return new Promise((resolvePromise, reject) => {
    const child = spawn("npx", ["wrangler", "secret", "put", key], {
      cwd: root,
      stdio: ["pipe", "inherit", "inherit"],
    });
    child.stdin.write(`${value}\n`);
    child.stdin.end();
    child.on("close", (code) =>
      code === 0 ? resolvePromise() : reject(new Error(`wrangler secret put ${key} failed`)),
    );
  });
}

const args = new Set(process.argv.slice(2));
const runRemote = args.has("--remote");
const runLocal = args.has("--local") || !runRemote;

if (runLocal) {
  const lines = WORKER_SECRETS.map((key) => `${key}=${escapeValue(process.env[key])}`);
  await writeFile(resolve(root, ".dev.vars"), `${lines.join("\n")}\n`, "utf8");
  console.log("Wrote .dev.vars for local development.");
}

if (runRemote) {
  for (const key of WORKER_SECRETS) await wranglerSecretPut(key, process.env[key]);
  console.log("Uploaded secrets to Cloudflare.");
}
