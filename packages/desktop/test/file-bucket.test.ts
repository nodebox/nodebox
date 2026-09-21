import { afterEach, beforeEach, describe, expect, it } from "vitest";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { FileBucket } from "../src/file-bucket";

let dir: string;
beforeEach(async () => {
  dir = await fs.mkdtemp(path.join(os.tmpdir(), "ndbx-bucket-"));
});
afterEach(async () => {
  await fs.rm(dir, { recursive: true, force: true });
});

describe("FileBucket", () => {
  it("stores, lists and deletes objects like R2", async () => {
    const bucket = new FileBucket(dir);
    await bucket.put("users/a/profile.json", JSON.stringify({ login: "a" }), {
      httpMetadata: { contentType: "application/json" },
    });
    await bucket.put("users/a/p1/project.json", "{}");
    await bucket.put("users/b/profile.json", "{}");
    expect(await bucket.head("users/a/profile.json")).toMatchObject({ key: "users/a/profile.json" });
    expect(await bucket.head("users/missing")).toBeNull();
    const object = await bucket.get("users/a/profile.json");
    expect(await object!.json()).toEqual({ login: "a" });
    expect(object!.httpMetadata?.contentType).toBe("application/json");
    const listing = await bucket.list({ prefix: "users/", delimiter: "/" });
    expect(listing.delimitedPrefixes).toEqual(["users/a/", "users/b/"]);
    const all = await bucket.list({ prefix: "users/a/" });
    expect(all.objects.map((o) => o.key)).toEqual(["users/a/p1/project.json", "users/a/profile.json"]);
    await bucket.delete("users/a/profile.json");
    expect(await bucket.get("users/a/profile.json")).toBeNull();
    await expect(bucket.get("../outside")).rejects.toThrow();
  });
});
