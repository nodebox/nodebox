import { Platform, renderAppcast, type Release } from "./appcast.ts";

interface Env {
  // Optional. Unauthenticated GitHub API calls share a rate limit with everything else on Cloudflare's egress IPs.
  GITHUB_TOKEN?: string;
}

const APPCAST_PATH = "/app/nodebox/appcast.xml";
const RELEASES_URL = "https://api.github.com/repos/nodebox/nodebox/releases?per_page=30";
// The releases are kept in this data center's cache under this key.
const CACHE_KEY = "https://appcast.cache/nodebox/releases";
// After this, ask GitHub again. Until GitHub answers, the older copy is still served.
const FRESH_SECONDS = 60 * 60;
const KEEP_SECONDS = 30 * 24 * 60 * 60;

export default {
  async fetch(request, env, ctx): Promise<Response> {
    const url = new URL(request.url);
    if (url.pathname !== APPCAST_PATH) return new Response("Not found\n", { status: 404 });
    if (request.method !== "GET" && request.method !== "HEAD") return new Response(null, { status: 405, headers: { Allow: "GET, HEAD" } });

    const releases = await getReleases(env, ctx);
    const platform = Number(url.searchParams.get("p")) || Platform.OTHER;
    const xml = releases && renderAppcast(releases, platform);
    if (!xml) return new Response("No releases available\n", { status: 503, headers: { "Retry-After": "600" } });
    return new Response(request.method === "HEAD" ? null : xml, {
      headers: { "Content-Type": "application/xml; charset=utf-8", "Cache-Control": "public, max-age=300" },
    });
  },
} satisfies ExportedHandler<Env>;

async function getReleases(env: Env, ctx: ExecutionContext): Promise<Release[] | null> {
  const cache = caches.default;
  const cached = await cache.match(CACHE_KEY);
  const fetchedAt = Number(cached?.headers.get("X-Fetched-At") ?? 0);
  if (cached && Date.now() - fetchedAt < FRESH_SECONDS * 1000) return cached.json();

  const headers: Record<string, string> = {
    Accept: "application/vnd.github+json",
    "User-Agent": "nodebox-appcast",
    "X-GitHub-Api-Version": "2022-11-28",
  };
  if (env.GITHUB_TOKEN) headers.Authorization = `Bearer ${env.GITHUB_TOKEN}`;
  // A 304 doesn't count against the rate limit.
  const etag = cached?.headers.get("ETag");
  if (etag) headers["If-None-Match"] = etag;

  let response: Response;
  try {
    response = await fetch(RELEASES_URL, { headers });
  } catch (err) {
    console.error("GitHub releases request failed", err);
    return cached ? cached.json() : null;
  }

  if (response.status === 304 && cached) {
    const body = await cached.text();
    ctx.waitUntil(store(cache, body, etag));
    return JSON.parse(body);
  }
  if (!response.ok) {
    console.error("GitHub releases request failed", response.status, await response.text());
    return cached ? cached.json() : null;
  }
  const body = await response.text();
  ctx.waitUntil(store(cache, body, response.headers.get("ETag")));
  return JSON.parse(body);
}

function store(cache: Cache, body: string, etag: string | null | undefined): Promise<void> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    "Cache-Control": `max-age=${KEEP_SECONDS}`,
    "X-Fetched-At": String(Date.now()),
  };
  if (etag) headers.ETag = etag;
  return cache.put(CACHE_KEY, new Response(body, { headers }));
}
