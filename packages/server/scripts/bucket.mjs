/**
 * Read and write objects in the R2 bucket from a Node script.
 *
 * --local talks to the wrangler dev bucket through the wrangler CLI.
 * --remote talks to the production bucket through the R2 S3-compatible API and needs
 *   R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY in packages/server/.env.
 */

import { spawnSync } from "child_process";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "fs";
import { tmpdir } from "os";
import { dirname, join, resolve } from "path";
import { fileURLToPath } from "url";
import { GetObjectCommand, PutObjectCommand, S3Client } from "@aws-sdk/client-s3";
import dotenv from "dotenv";

export const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
export const BUCKET = "nodeboxlive";

dotenv.config({ path: join(root, ".env") });

export function contentTypeFor(filePath) {
  if (filePath.endsWith(".json")) return "application/json";
  if (filePath.endsWith(".js")) return "application/javascript";
  if (filePath.endsWith(".svg")) return "image/svg+xml";
  if (filePath.endsWith(".png")) return "image/png";
  if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
  return "application/octet-stream";
}

function wrangler(args) {
  const result = spawnSync("npx", ["wrangler", ...args], { cwd: root, stdio: ["ignore", "pipe", "pipe"] });
  if (result.status !== 0) throw new Error(result.stderr.toString() || `wrangler ${args.join(" ")} failed`);
  return result;
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

export function openBucket(args) {
  const isRemote = args.includes("--remote");
  const isLocal = args.includes("--local");
  if (isRemote === isLocal) {
    console.error("Pass exactly one of --local or --remote");
    process.exit(1);
  }
  const s3 = isRemote ? createRemoteClient() : undefined;

  async function putFile(key, filePath) {
    if (s3) {
      await s3.send(
        new PutObjectCommand({
          Bucket: BUCKET,
          Key: key,
          Body: readFileSync(filePath),
          ContentType: contentTypeFor(filePath),
        }),
      );
    } else {
      wrangler([
        "r2",
        "object",
        "put",
        `${BUCKET}/${key}`,
        "--file",
        filePath,
        "--local",
        "--ct",
        contentTypeFor(filePath),
      ]);
    }
  }

  async function getText(key) {
    if (s3) {
      const { Body } = await s3.send(new GetObjectCommand({ Bucket: BUCKET, Key: key }));
      return Body.transformToString();
    }
    const dir = mkdtempSync(join(tmpdir(), "r2-"));
    try {
      const file = join(dir, "object");
      wrangler(["r2", "object", "get", `${BUCKET}/${key}`, "--file", file, "--local"]);
      return readFileSync(file, "utf8");
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  }

  async function putText(key, text, contentType = "application/json") {
    if (s3) {
      await s3.send(new PutObjectCommand({ Bucket: BUCKET, Key: key, Body: text, ContentType: contentType }));
      return;
    }
    const dir = mkdtempSync(join(tmpdir(), "r2-"));
    try {
      const file = join(dir, "object");
      writeFileSync(file, text);
      wrangler(["r2", "object", "put", `${BUCKET}/${key}`, "--file", file, "--local", "--ct", contentType]);
    } finally {
      rmSync(dir, { recursive: true, force: true });
    }
  }

  return { isRemote, putFile, getText, putText };
}
