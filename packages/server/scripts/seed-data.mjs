#!/usr/bin/env node

/**
 * Copy the seed data (core, skel, template, example, test users) into the R2 bucket.
 *
 * Usage:
 *   node scripts/seed-data.mjs --local    # into the local wrangler dev bucket
 *   node scripts/seed-data.mjs --remote   # into the production bucket
 *   SEED_USERS=core,example node scripts/seed-data.mjs --local   # only some users
 *
 * --remote needs R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY in .env.
 */

import { readdirSync, statSync } from "fs";
import { join, relative } from "path";
import { openBucket, root } from "./bucket.mjs";

const DATA_DIR = join(root, "data");
const USERS = (process.env.SEED_USERS ?? "core,skel,template,example,test").split(",");

function* walk(dir) {
  for (const name of readdirSync(dir)) {
    if (name.startsWith(".")) continue;
    const full = join(dir, name);
    if (statSync(full).isDirectory()) yield* walk(full);
    else yield full;
  }
}

const bucket = openBucket(process.argv.slice(2));

for (const userId of USERS) {
  for (const filePath of walk(join(DATA_DIR, userId))) {
    const key = `users/${relative(DATA_DIR, filePath)}`;
    console.log(`Uploading ${key}`);
    await bucket.putFile(key, filePath);
  }
}
