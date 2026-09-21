// An R2Bucket look-alike over a directory, so the server's store runs unchanged inside the
// desktop app. Keys map to paths under the root; metadata is kept in a sidecar file per object.

import fs from "node:fs/promises";
import { createReadStream, existsSync } from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";

export interface BucketObject {
  key: string;
  size: number;
  httpMetadata?: { contentType?: string };
  body: ReadableStream;
  text(): Promise<string>;
  json<T = unknown>(): Promise<T>;
  arrayBuffer(): Promise<ArrayBuffer>;
}

export interface ListOptions {
  prefix?: string;
  delimiter?: string;
  cursor?: string;
  limit?: number;
}

export interface ListResult {
  objects: { key: string; size: number }[];
  delimitedPrefixes: string[];
  truncated: boolean;
  cursor?: string;
}

const METADATA_SUFFIX = ".r2meta.json";

export class FileBucket {
  readonly root: string;

  constructor(root: string) {
    this.root = root;
  }

  private pathFor(key: string): string {
    const root = path.resolve(this.root);
    const full = path.resolve(root, key);
    if (full !== root && !full.startsWith(root + path.sep)) throw new Error(`Key escapes the bucket: ${key}`);
    return full;
  }

  async get(key: string): Promise<BucketObject | null> {
    const file = this.pathFor(key);
    let stat;
    try {
      stat = await fs.stat(file);
    } catch {
      return null;
    }
    if (!stat.isFile()) return null;
    const metadata = await this.readMetadata(file);
    return {
      key,
      size: stat.size,
      httpMetadata: metadata,
      get body() {
        return Readable.toWeb(createReadStream(file)) as ReadableStream;
      },
      text: () => fs.readFile(file, "utf-8"),
      json: async <T>() => JSON.parse(await fs.readFile(file, "utf-8")) as T,
      arrayBuffer: async () => {
        const buffer = await fs.readFile(file);
        return buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength);
      },
    };
  }

  async head(key: string): Promise<{ key: string; size: number } | null> {
    try {
      const stat = await fs.stat(this.pathFor(key));
      return stat.isFile() ? { key, size: stat.size } : null;
    } catch {
      return null;
    }
  }

  async put(key: string, body: unknown, options: { httpMetadata?: { contentType?: string } } = {}): Promise<void> {
    const file = this.pathFor(key);
    await fs.mkdir(path.dirname(file), { recursive: true });
    await fs.writeFile(file, await toBuffer(body));
    if (options.httpMetadata) await fs.writeFile(file + METADATA_SUFFIX, JSON.stringify(options.httpMetadata));
  }

  async delete(key: string | string[]): Promise<void> {
    for (const k of Array.isArray(key) ? key : [key]) {
      const file = this.pathFor(k);
      await fs.rm(file, { force: true });
      await fs.rm(file + METADATA_SUFFIX, { force: true });
    }
  }

  async list(options: ListOptions = {}): Promise<ListResult> {
    const prefix = options.prefix ?? "";
    const keys = await this.allKeys();
    const matching = keys.filter((k) => k.startsWith(prefix)).sort();
    if (!options.delimiter) {
      return { objects: await Promise.all(matching.map((k) => this.head(k).then((h) => ({ key: k, size: h?.size ?? 0 })))), delimitedPrefixes: [], truncated: false };
    }
    const objects: { key: string; size: number }[] = [];
    const prefixes = new Set<string>();
    for (const key of matching) {
      const rest = key.slice(prefix.length);
      const i = rest.indexOf(options.delimiter);
      if (i < 0) objects.push({ key, size: (await this.head(key))?.size ?? 0 });
      else prefixes.add(prefix + rest.slice(0, i + options.delimiter.length));
    }
    return { objects, delimitedPrefixes: Array.from(prefixes).sort(), truncated: false };
  }

  private async allKeys(): Promise<string[]> {
    if (!existsSync(this.root)) return [];
    const keys: string[] = [];
    const walk = async (dir: string) => {
      for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) await walk(full);
        else if (!entry.name.endsWith(METADATA_SUFFIX)) keys.push(path.relative(this.root, full).split(path.sep).join("/"));
      }
    };
    await walk(this.root);
    return keys;
  }

  private async readMetadata(file: string): Promise<{ contentType?: string } | undefined> {
    try {
      return JSON.parse(await fs.readFile(file + METADATA_SUFFIX, "utf-8"));
    } catch {
      return { contentType: contentTypeFor(file) };
    }
  }
}

async function toBuffer(body: unknown): Promise<Buffer> {
  if (body === null || body === undefined) return Buffer.alloc(0);
  if (typeof body === "string") return Buffer.from(body, "utf-8");
  if (body instanceof ArrayBuffer) return Buffer.from(body);
  if (ArrayBuffer.isView(body)) return Buffer.from(body.buffer, body.byteOffset, body.byteLength);
  if (body instanceof Blob) return Buffer.from(await body.arrayBuffer());
  if (typeof (body as ReadableStream).getReader === "function") {
    const chunks: Uint8Array[] = [];
    const reader = (body as ReadableStream<Uint8Array>).getReader();
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      if (value) chunks.push(value);
    }
    return Buffer.concat(chunks);
  }
  return Buffer.from(String(body), "utf-8");
}

export function contentTypeFor(file: string): string {
  const ext = path.extname(file).toLowerCase();
  const types: Record<string, string> = {
    ".json": "application/json",
    ".js": "text/javascript",
    ".mjs": "text/javascript",
    ".css": "text/css",
    ".html": "text/html",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".gif": "image/gif",
    ".webp": "image/webp",
    ".ico": "image/x-icon",
    ".csv": "text/csv",
    ".txt": "text/plain",
    ".md": "text/markdown",
    ".woff": "font/woff",
    ".woff2": "font/woff2",
    ".ttf": "font/ttf",
    ".map": "application/json",
    ".geojson": "application/geo+json",
    ".xml": "application/xml",
    ".ndbx": "application/xml",
  };
  return types[ext] ?? "application/octet-stream";
}
