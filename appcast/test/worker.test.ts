import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { beforeEach, test } from "node:test";
import worker from "../src/index.ts";

const releasesJson = readFileSync(new URL("releases.json", import.meta.url), "utf8");
const APPCAST = "https://secure.nodebox.net/app/nodebox/appcast.xml?v=3.0.52&p=2";

// Stand-ins for the Workers cache and GitHub.
let cache: Map<string, Response>;
let github: (request: Request) => Response | Promise<Response>;
let githubRequests: Request[];
const pending: Promise<unknown>[] = [];
const ctx = { waitUntil: (p: Promise<unknown>) => void pending.push(p), passThroughOnException() {} } as unknown as ExecutionContext;

beforeEach(() => {
  cache = new Map();
  githubRequests = [];
  github = () => new Response(releasesJson, { headers: { ETag: '"v1"' } });
  Object.assign(globalThis, {
    caches: {
      default: {
        match: async (key: string) => cache.get(key)?.clone(),
        put: async (key: string, response: Response) => void cache.set(key, response),
      },
    },
    fetch: async (input: RequestInfo, init?: RequestInit) => {
      const request = new Request(input, init);
      githubRequests.push(request);
      return github(request);
    },
  });
});

async function get(url = APPCAST, env: Record<string, string> = {}, method = "GET"): Promise<Response> {
  const response = await worker.fetch(new Request(url, { method }) as never, env, ctx);
  await Promise.all(pending.splice(0));
  return response;
}

test("serves the appcast for the platform", async () => {
  const response = await get();
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("Content-Type"), "application/xml; charset=utf-8");
  const xml = await response.text();
  assert.match(xml, /<appcast:version>3\.1\.0<\/appcast:version>/);
  assert.doesNotMatch(xml, /3\.0\.53/); // No macOS installer.
  assert.match(await (await get(APPCAST.replace("p=2", "p=1"))).text(), /3\.0\.53/);
});

test("asks GitHub at most once an hour", async () => {
  await get();
  await get();
  assert.equal(githubRequests.length, 1);
  assert.equal(githubRequests[0].headers.get("User-Agent"), "nodebox-appcast");
});

test("revalidates with the ETag once the copy is old", async () => {
  await get();
  const stored = cache.values().next().value!;
  const key = cache.keys().next().value!;
  const headers = new Headers(stored.headers);
  headers.set("X-Fetched-At", String(Date.now() - 2 * 60 * 60 * 1000));
  cache.set(key, new Response(await stored.text(), { headers }));
  github = () => new Response(null, { status: 304 });

  const response = await get();
  assert.equal(response.status, 200);
  assert.equal(githubRequests[1].headers.get("If-None-Match"), '"v1"');
  assert.ok(Date.now() - Number(cache.get(key)!.headers.get("X-Fetched-At")) < 60 * 1000);
});

test("keeps serving the old copy while GitHub fails", async () => {
  await get();
  const key = cache.keys().next().value!;
  const stored = cache.get(key)!;
  const headers = new Headers(stored.headers);
  headers.set("X-Fetched-At", "0");
  cache.set(key, new Response(await stored.text(), { headers }));
  github = () => new Response("rate limited", { status: 403 });
  assert.equal((await get()).status, 200);
});

test("is unavailable when GitHub fails and nothing is cached", async () => {
  github = () => {
    throw new Error("offline");
  };
  assert.equal((await get()).status, 503);
});

test("sends the token when there is one", async () => {
  await get(APPCAST, { GITHUB_TOKEN: "secret" });
  assert.equal(githubRequests[0].headers.get("Authorization"), "Bearer secret");
});

test("only serves the appcast path", async () => {
  assert.equal((await get("https://secure.nodebox.net/")).status, 404);
  const head = await get(APPCAST, {}, "HEAD");
  assert.equal(head.status, 200);
  assert.equal(head.body, null);
});
