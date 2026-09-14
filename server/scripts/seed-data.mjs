#!/usr/bin/env node

/**
 * Copy the seed data (core, skel, template, example, test users) into the R2 bucket.
 *
 * Usage:
 *   node scripts/seed-data.mjs --local    # into the local wrangler dev bucket
 *   node scripts/seed-data.mjs --remote   # into the production bucket
 *   SEED_USERS=core,example node scripts/seed-data.mjs --local   # only some users
 *
 * --remote uses the R2 S3-compatible API and needs these variables in .env:
 *   R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY
 */

import { spawnSync } from "child_process";
import { readFileSync } from "fs";
import { dirname, join, relative, resolve } from "path";
import { readdirSync, statSync } from "fs";
import { fileURLToPath } from "url";
import { S3Client, PutObjectCommand } from "@aws-sdk/client-s3";
import dotenv from "dotenv";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
dotenv.config({ path: join(root, ".env") });

const args = process.argv.slice(2);
const isRemote = args.includes("--remote");
const DATA_DIR = join(root, "data");
const USERS = (process.env.SEED_USERS ?? "core,skel,template,example,test").split(",");
const BUCKET = "nodeboxlive";

function contentTypeFor(filePath) {
  if (filePath.endsWith(".json")) return "application/json";
  if (filePath.endsWith(".js")) return "application/javascript";
  if (filePath.endsWith(".svg")) return "image/svg+xml";
  if (filePath.endsWith(".png")) return "image/png";
  if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
  return "application/octet-stream";
}

function* walk(dir) {
  for (const name of readdirSync(dir)) {
    if (name.startsWith(".")) continue;
    const full = join(dir, name);
    if (statSync(full).isDirectory()) yield* walk(full);
    else yield full;
  }
}

function putLocal(key, filePath) {
  const result = spawnSync(
    "npx",
    [
      "wrangler",
      "r2",
      "object",
      "put",
      `${BUCKET}/${key}`,
      "--file",
      filePath,
      "--local",
      "--ct",
      contentTypeFor(filePath),
    ],
    { cwd: root, stdio: "inherit" },
  );
  if (result.status !== 0) throw new Error(`Upload failed for ${key}`);
}

function createRemoteClient() {
  const { R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY } = process.env;
  if (!(R2_ACCOUNT_ID && R2_ACCESS_KEY_ID && R2_SECRET_ACCESS_KEY)) {
    console.error("Missing R2_ACCOUNT_ID, R2_ACCESS_KEY_ID or R2_SECRET_ACCESS_KEY in .env");
    process.exit(1);
  }
  return new S3Client({
    region: "auto",
    endpoint: `https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com`,
    credentials: { accessKeyId: R2_ACCESS_KEY_ID, secretAccessKey: R2_SECRET_ACCESS_KEY },
  });
}

const s3 = isRemote ? createRemoteClient() : undefined;

for (const userId of USERS) {
  for (const filePath of walk(join(DATA_DIR, userId))) {
    const key = `users/${relative(DATA_DIR, filePath)}`;
    console.log(`Uploading ${key}`);
    if (isRemote) {
      await s3.send(
        new PutObjectCommand({
          Bucket: BUCKET,
          Key: key,
          Body: readFileSync(filePath),
          ContentType: contentTypeFor(filePath),
        }),
      );
    } else {
      putLocal(key, filePath);
    }
  }
}
