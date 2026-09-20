#!/usr/bin/env node

/**
 * Who still reads the DigitalOcean Spaces bucket? Summarises the access logs by referrer,
 * user agent and project, so embeds on old runtime versions can be found and nudged to upgrade.
 *
 * Needs the Spaces key in .env.spaces and the log bucket set up with:
 *   aws s3api put-bucket-logging --bucket nodeboxlive --bucket-logging-status
 *     '{"LoggingEnabled":{"TargetBucket":"nodeboxlive-logs","TargetPrefix":"access/"}}'
 *
 * Usage:
 *   node scripts/spaces-usage.mjs            # summary of all logs so far
 *   node scripts/spaces-usage.mjs --since 2026-10-01
 */

import { spawnSync } from "node:child_process";
import { mkdtempSync, readdirSync, readFileSync, rmSync, statSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { gunzipSync } from "node:zlib";
import dotenv from "dotenv";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
dotenv.config({ path: join(root, ".env.spaces") });

const args = process.argv.slice(2);
const sinceIndex = args.indexOf("--since");
const since = sinceIndex >= 0 ? new Date(args[sinceIndex + 1]) : new Date(0);

const LOG_BUCKET = "nodeboxlive-logs";
const OWN_HOSTS = ["nodebox.live", "localhost"];

const dir = mkdtempSync(join(tmpdir(), "spaces-logs-"));
const sync = spawnSync(
  "aws",
  ["s3", "sync", `s3://${LOG_BUCKET}/access/`, dir, "--endpoint-url", process.env.AWS_S3_ENDPOINT, "--quiet"],
  { env: { ...process.env, AWS_DEFAULT_REGION: "us-east-1" }, stdio: ["ignore", "inherit", "inherit"] },
);
if (sync.status !== 0) {
  console.error("Could not download the logs. Check .env.spaces and that aws is installed.");
  process.exit(1);
}

const byReferrer = new Map();
const byUserAgent = new Map();
const byProject = new Map();
let total = 0;

function count(map, key) {
  map.set(key, (map.get(key) ?? 0) + 1);
}

function record({ time, key, referrer, userAgent }) {
  if (time < since) return;
  if (!key || !key.startsWith("users/")) return;
  total += 1;
  const host = referrerHost(referrer);
  count(byReferrer, host);
  count(byUserAgent, (userAgent || "-").slice(0, 80));
  const [, userId, projectId] = key.split("/");
  count(byProject, `${userId}/${projectId}`);
}

function referrerHost(referrer) {
  if (!referrer || referrer === "-") return "(no referrer)";
  try {
    const host = new URL(referrer).hostname;
    return OWN_HOSTS.some((own) => host === own || host.endsWith(`.${own}`)) ? `(own site) ${host}` : host;
  } catch {
    return referrer;
  }
}

// Amazon S3 server access log format: quoted request, referrer and user agent, bracketed time.
const S3_LINE = /^\S+ \S+ \[([^\]]+)\] \S+ \S+ \S+ \S+ (\S+) "[^"]*" \S+ \S+ \S+ \S+ \S+ \S+ "([^"]*)" "([^"]*)"/;

function parseS3Line(line) {
  const m = S3_LINE.exec(line);
  if (!m) return;
  const [, rawTime, key, referrer, userAgent] = m;
  const time = new Date(rawTime.replace(/^(\d+)\/(\w+)\/(\d+):(\d+:\d+:\d+) (.*)$/, "$1 $2 $3 $4 $5"));
  record({ time, key: decodeURIComponent(key), referrer, userAgent });
}

// Amazon CloudFront format (CDN requests): tab separated, with a "#Fields:" header line.
function parseCloudFront(text) {
  const lines = text.split("\n");
  const fieldsLine = lines.find((l) => l.startsWith("#Fields:"));
  if (!fieldsLine) return false;
  const fields = fieldsLine.replace("#Fields:", "").trim().split(/\s+/);
  const col = (name) => fields.indexOf(name);
  for (const line of lines) {
    if (!line || line.startsWith("#")) continue;
    const parts = line.split("\t");
    const time = new Date(`${parts[col("date")]}T${parts[col("time")]}Z`);
    const key = decodeURIComponent(parts[col("cs-uri-stem")] ?? "").replace(/^\//, "");
    record({
      time,
      key,
      referrer: decodeURIComponent(parts[col("cs(Referer)")] ?? "-"),
      userAgent: decodeURIComponent(parts[col("cs(User-Agent)")] ?? "-"),
    });
  }
  return true;
}

let files = 0;
for (const name of readdirSync(dir, { recursive: true })) {
  if (!statSync(join(dir, name)).isFile()) continue;
  const raw = readFileSync(join(dir, name));
  const text = name.endsWith(".gz") ? gunzipSync(raw).toString("utf8") : raw.toString("utf8");
  files += 1;
  if (parseCloudFront(text)) continue;
  for (const line of text.split("\n")) parseS3Line(line);
}
rmSync(dir, { recursive: true, force: true });

function printTop(title, map, limit = 20) {
  console.log(`\n${title}`);
  const rows = [...map.entries()].sort((a, b) => b[1] - a[1]).slice(0, limit);
  for (const [key, n] of rows) console.log(`${String(n).padStart(8)}  ${key}`);
}

console.log(`${files} log files, ${total} requests for users/ objects since ${since.toISOString().slice(0, 10)}`);
printTop("By referrer host (sites that embed from the old bucket):", byReferrer);
printTop("By project:", byProject);
printTop("By user agent:", byUserAgent, 10);
