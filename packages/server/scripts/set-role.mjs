#!/usr/bin/env node

/**
 * Set the role of a user. Admins can open /admin and reset passwords for other users.
 *
 * Usage:
 *   node scripts/set-role.mjs <userId> admin --local
 *   node scripts/set-role.mjs <userId> user --remote
 */

import { openBucket } from "./bucket.mjs";

const ROLES = ["user", "admin"];
const args = process.argv.slice(2);
const [userId, role] = args.filter((arg) => !arg.startsWith("--"));

if (!userId || !ROLES.includes(role)) {
  console.error("Usage: node scripts/set-role.mjs <userId> <user|admin> --local|--remote");
  process.exit(1);
}

const bucket = openBucket(args);
const key = `users/${userId}/profile.json`;

let profile;
try {
  profile = JSON.parse(await bucket.getText(key));
} catch (e) {
  console.error(`User "${userId}" not found (${key})`);
  process.exit(1);
}

if (role === "user") delete profile.role;
else profile.role = role;

await bucket.putText(key, JSON.stringify(profile, null, 2));
console.log(`${userId} is now ${role} (${bucket.isRemote ? "remote" : "local"})`);
