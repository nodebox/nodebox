// The same Hono application that runs on Cloudflare, served on localhost inside the desktop app,
// with projects on disk and a single local user.

import { serve } from "@hono/node-server";
import { createStore } from "@ndbx/server/src/store.ts";
import { signToken } from "@ndbx/server/src/auth.ts";
import app from "@ndbx/server/src/index.ts";
import bcrypt from "bcryptjs";
import { randomBytes } from "node:crypto";
import fs from "node:fs/promises";
import { existsSync } from "node:fs";
import path from "node:path";
import { FileBucket } from "./file-bucket";
import { createStaticFetcher } from "./static-fetcher";

export const LOCAL_USER_ID = "local";

export interface LocalServerOptions {
  /** Where projects, profiles and assets live (the bucket root). */
  dataDir: string;
  /** The web app build, plus the guide (markdown under _guide, media under guide/media). */
  staticDirs: string[];
  /** Seed data copied into the bucket on first run: the core libraries, templates and examples. */
  seedDir?: string;
  port?: number;
  host?: string;
}

export interface LocalServer {
  url: string;
  token: string;
  bucket: FileBucket;
  store: ReturnType<typeof createStore>;
  close(): Promise<void>;
}

export async function startLocalServer(options: LocalServerOptions): Promise<LocalServer> {
  const bucketRoot = path.join(options.dataDir, "bucket");
  await fs.mkdir(bucketRoot, { recursive: true });
  const bucket = new FileBucket(bucketRoot);
  if (options.seedDir) await seed(bucket, options.seedDir);
  const secret = await jwtSecret(options.dataDir);
  const store = createStore(bucket as never);
  await ensureLocalUser(store);
  const token = await signToken({ userId: LOCAL_USER_ID, membership: null }, secret);

  const env = {
    BUCKET: bucket,
    ASSETS: createStaticFetcher(options.staticDirs),
    JWT_SECRET: secret,
    ASSETS_URL: "",
  };
  const host = options.host ?? "127.0.0.1";
  // The server binds asynchronously, so the port an ephemeral bind (port 0) picked is only known
  // once it is listening. Asking for the address before that yields port 0 and an unreachable URL.
  const { server, port } = await new Promise<{ server: ReturnType<typeof serve>; port: number }>((resolve) => {
    const started = serve(
      { fetch: (request) => app.fetch(request, env), hostname: host, port: options.port ?? 0 },
      (info) => resolve({ server: started, port: info.port }),
    );
  });
  return {
    url: `http://${host}:${port}`,
    token,
    bucket,
    store,
    close: () => new Promise((resolve) => server.close(() => resolve())),
  };
}

/** Copy the seed users (core, skel, template, example, …) into the bucket when they are missing. */
async function seed(bucket: FileBucket, seedDir: string): Promise<void> {
  if (!existsSync(seedDir)) return;
  for (const entry of await fs.readdir(seedDir, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue;
    const target = path.join(bucket.root, "users", entry.name);
    if (existsSync(target)) continue;
    await fs.cp(path.join(seedDir, entry.name), target, { recursive: true });
  }
}

async function jwtSecret(dataDir: string): Promise<string> {
  const file = path.join(dataDir, "secret");
  try {
    return (await fs.readFile(file, "utf-8")).trim();
  } catch {
    const secret = randomBytes(32).toString("hex");
    await fs.writeFile(file, secret, { mode: 0o600 });
    return secret;
  }
}

async function ensureLocalUser(store: ReturnType<typeof createStore>): Promise<void> {
  if (await store.userExists(LOCAL_USER_ID)) return;
  // The desktop never asks for this password: the token is minted directly.
  const hash = await bcrypt.hash(randomBytes(16).toString("hex"), 10);
  await store.createUser(LOCAL_USER_ID, "local@localhost", hash);
}
