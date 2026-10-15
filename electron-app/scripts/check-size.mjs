// Fails when the packaged app is larger than the budget, so size creep shows up
// in the release build instead of in a user's Downloads folder.
//
// Usage: node scripts/check-size.mjs
// Run after electron-builder; it measures every unpacked app under release/.
//
// Electron alone sets the floor. Electron 33 with only the English locale is
// about 204 MB on macOS, 234 MB on Linux and 240 MB on Windows, and NodeBox adds
// about 5 MB. The budgets leave about 10 MB of room above today's build.

import { readdirSync, lstatSync } from 'fs';
import { join } from 'path';
import { fileURLToPath } from 'url';

const budgetsMb = { mac: 220, linux: 250, win: 255 };
const releaseDir = fileURLToPath(new URL('../release/', import.meta.url));

function sizeOf(path) {
  const stat = lstatSync(path);
  if (stat.isSymbolicLink()) return 0;
  if (!stat.isDirectory()) return stat.size;
  return readdirSync(path).reduce((sum, name) => sum + sizeOf(join(path, name)), 0);
}

function findApps(dir) {
  const apps = [];
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (!lstatSync(path).isDirectory()) continue;
    if (name.endsWith('.app') || name.endsWith('-unpacked')) apps.push(path);
    else if (name.startsWith('mac')) apps.push(...findApps(path));
  }
  return apps;
}

const apps = findApps(releaseDir);
if (apps.length === 0) {
  console.error(`No packaged app found in ${releaseDir}`);
  process.exit(1);
}

let failed = false;
for (const app of apps) {
  const name = app.slice(releaseDir.length);
  const platform = Object.keys(budgetsMb).find((p) => name.startsWith(p));
  const budget = budgetsMb[platform];
  const mb = sizeOf(app) / 1e6;
  const ok = mb <= budget;
  failed ||= !ok;
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${mb.toFixed(1)} MB of ${budget} MB  ${name}`);
}
if (failed) process.exit(1);
