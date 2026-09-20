// A port of nodebox.function.NetworkFunctions (namespace "network"), on top of fetch.

import { JavaScriptLibrary } from "../runtime/function-repository";

interface CachedResponse {
  timeFetched: number;
  response: Record<string, unknown>;
}

const responseCache = new Map<string, CachedResponse>();

function nowSeconds(): number {
  return Math.floor(Date.now() / 1000);
}

export async function httpGet(url: string, username: string, password: string, refreshTimeSeconds: number): Promise<Record<string, unknown>> {
  const cacheKey = `${url}\u0000${username}\u0000${password}`;
  const cached = responseCache.get(cacheKey);
  if (cached && nowSeconds() - cached.timeFetched <= refreshTimeSeconds) return cached.response;
  const response = await doGet(url, username, password);
  responseCache.set(cacheKey, { timeFetched: nowSeconds(), response });
  return response;
}

async function doGet(url: string, username: string, password: string): Promise<Record<string, unknown>> {
  if (!/^https?:\/\//.test(url)) throw new Error('URL should start with "http://" or "https://".');
  const headers: Record<string, string> = {};
  if (username && username.trim() !== "") {
    const credentials = `${username}:${password ?? ""}`;
    headers.Authorization = `Basic ${toBase64(credentials)}`;
  }
  try {
    const res = await fetch(url, { headers });
    const body = await res.text();
    const responseHeaders: Record<string, string> = {};
    res.headers.forEach((value, key) => {
      responseHeaders[key] = value;
    });
    return { body, statusCode: res.status, headers: responseHeaders };
  } catch {
    // No response at all: report a request timeout, as NodeBox 3 does.
    return { body: "", statusCode: 408 };
  }
}

function toBase64(s: string): string {
  const bytes = new TextEncoder().encode(s);
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary);
}

/** A JSONPath subset: $, .name, ['name'], [index], [*], .* and ..name (recursive descent). */
export function jsonPath(data: unknown, query: string): unknown[] {
  let q = query.trim();
  if (q.startsWith("$")) q = q.slice(1);
  const tokens: (string | number | "*" | { descend: string })[] = [];
  const re = /\.\.([A-Za-z_$][\w$]*|\*)|\.([A-Za-z_$][\w$]*|\*)|\[\s*'([^']*)'\s*\]|\[\s*"([^"]*)"\s*\]|\[\s*(\*|-?\d+)\s*\]/g;
  let m: RegExpExecArray | null;
  let last = 0;
  while ((m = re.exec(q)) !== null) {
    if (m.index !== last) throw new Error(`Invalid JSONPath: ${query}`);
    last = re.lastIndex;
    if (m[1] !== undefined) tokens.push({ descend: m[1] });
    else if (m[2] !== undefined) tokens.push(m[2] === "*" ? "*" : m[2]);
    else if (m[3] !== undefined) tokens.push(m[3]);
    else if (m[4] !== undefined) tokens.push(m[4]);
    else if (m[5] !== undefined) tokens.push(m[5] === "*" ? "*" : parseInt(m[5], 10));
  }
  if (last !== q.length) throw new Error(`Invalid JSONPath: ${query}`);
  let current: unknown[] = [data];
  for (const token of tokens) {
    const next: unknown[] = [];
    for (const item of current) {
      if (typeof token === "object") {
        collectDescendants(item, token.descend, next);
      } else if (token === "*") {
        if (Array.isArray(item)) next.push(...item);
        else if (item && typeof item === "object") next.push(...Object.values(item));
      } else if (typeof token === "number") {
        if (Array.isArray(item)) {
          const idx = token < 0 ? item.length + token : token;
          if (idx >= 0 && idx < item.length) next.push(item[idx]);
        }
      } else if (item && typeof item === "object" && !Array.isArray(item) && token in (item as object)) {
        next.push((item as Record<string, unknown>)[token]);
      }
    }
    current = next;
  }
  return current;
}

function collectDescendants(item: unknown, key: string, into: unknown[]): void {
  if (item === null || typeof item !== "object") return;
  if (Array.isArray(item)) {
    for (const child of item) collectDescendants(child, key, into);
    return;
  }
  const obj = item as Record<string, unknown>;
  if (key === "*") into.push(...Object.values(obj));
  else if (key in obj) into.push(obj[key]);
  for (const child of Object.values(obj)) collectDescendants(child, key, into);
}

export function queryJSON(json: unknown, query: string): unknown[] {
  if (json !== null && typeof json === "object" && !Array.isArray(json)) {
    const map = json as Record<string, unknown>;
    if ("body" in map) return queryJSON(map.body, query);
    throw new Error("Cannot parse JSON input.");
  }
  if (typeof json === "string") {
    const data = JSON.parse(json);
    const results = jsonPath(data, query);
    // A single non-list result stays a one-element list; a matched array is returned as-is.
    if (results.length === 1 && Array.isArray(results[0])) return results[0];
    return results;
  }
  throw new Error("Cannot parse JSON input.");
}

export function encodeURL(s: string): string {
  // java.net.URLEncoder: spaces become '+', and '*' stays as is.
  return encodeURIComponent(s ?? "")
    .replace(/%20/g, "+")
    .replace(/[!'()~]/g, (c) => `%${c.charCodeAt(0).toString(16).toUpperCase()}`)
    .replace(/%2A/g, "*");
}

export const networkLibrary = new JavaScriptLibrary("network", { httpGet, queryJSON, encodeURL }, { impure: ["httpGet"] });
