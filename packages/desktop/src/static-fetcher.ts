// The ASSETS binding of the worker, on the file system: serves the web app build with the
// index page as the fallback for client-side routes.

import fs from "node:fs/promises";
import path from "node:path";
import { contentTypeFor } from "./file-bucket";

export interface StaticFetcher {
  fetch(request: Request | string | URL): Promise<Response>;
}

export function createStaticFetcher(directories: string[]): StaticFetcher {
  async function find(pathname: string): Promise<string | null> {
    const relative = decodeURIComponent(pathname).replace(/^\/+/, "");
    for (const dir of directories) {
      const full = path.resolve(dir, relative);
      if (!full.startsWith(path.resolve(dir))) continue;
      try {
        const stat = await fs.stat(full);
        if (stat.isFile()) return full;
      } catch {
        // try the next directory
      }
    }
    return null;
  }

  return {
    async fetch(request: Request | string | URL): Promise<Response> {
      const url = request instanceof URL ? request : new URL(typeof request === "string" ? request : request.url);
      let file = await find(url.pathname);
      if (!file && !path.extname(url.pathname)) file = await find("/index.html");
      if (!file) return new Response("Not found", { status: 404 });
      const body = await fs.readFile(file);
      return new Response(body, { headers: { "content-type": contentTypeFor(file) } });
    },
  };
}
