import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { compareVersions, notesHtml, offeredReleases, Platform, renderAppcast, rssDate, versionOf, type Release } from "../src/appcast.ts";

// GitHub's answer for nodebox/nodebox on 2026-10-14, trimmed to the fields the appcast uses.
const releases: Release[] = JSON.parse(readFileSync(new URL("releases.json", import.meta.url), "utf8"));

function release(fields: Partial<Release>): Release {
  return {
    tag_name: "v9.0.0",
    name: null,
    body: "Notes.",
    html_url: "https://github.com/nodebox/nodebox/releases/tag/v9.0.0",
    published_at: "2030-01-01T00:00:00Z",
    draft: false,
    prerelease: false,
    assets: [{ name: "NodeBox-9.0.0.dmg" }, { name: "NodeBox-9.0.0.msi" }],
    ...fields,
  };
}

test("versions compare by number", () => {
  assert.ok(compareVersions("3.1.0", "3.0.53") > 0);
  assert.ok(compareVersions("3.0.9", "3.0.53") < 0);
  assert.equal(compareVersions("3.1", "3.1.0"), 0);
});

test("only plain version tags count", () => {
  assert.equal(versionOf(release({ tag_name: "v3.1.0" })), "3.1.0");
  assert.equal(versionOf(release({ tag_name: "3.2.0" })), "3.2.0");
  assert.equal(versionOf(release({ tag_name: "v3.0a27" })), null);
});

test("pubDate is in the format the app parses", () => {
  assert.equal(rssDate("2026-06-14T20:50:01Z"), "Sun, 14 Jun 2026 20:50:01 +0000");
  assert.equal(rssDate("2022-02-08T09:03:05Z"), "Tue, 08 Feb 2022 09:03:05 +0000");
});

test("each platform gets the releases it has an installer for", () => {
  const versions = (platform: number) => offeredReleases(releases, platform).map(versionOf);
  // 3.0.53 only shipped an .msi.
  assert.deepEqual(versions(Platform.MAC), ["3.1.0", "3.0.52", "3.0.51", "3.0.50"]);
  assert.deepEqual(versions(Platform.WIN), ["3.1.0", "3.0.53", "3.0.52", "3.0.51", "3.0.50"]);
  assert.deepEqual(versions(Platform.OTHER), versions(Platform.WIN));
});

test("drafts, prereleases and releases without installers are left out", () => {
  const all = [
    release({ tag_name: "v4.0.0", draft: true }),
    release({ tag_name: "v3.9.0", prerelease: true }),
    release({ tag_name: "v3.8.0", assets: [] }),
    release({ tag_name: "v3.7.0", assets: [{ name: "NodeBox-3.7.0.msi" }] }),
    ...releases,
  ];
  assert.equal(versionOf(offeredReleases(all, Platform.MAC)[0]), "3.1.0");
  assert.equal(versionOf(offeredReleases(all, Platform.WIN)[0]), "3.7.0");
});

test("release notes are Markdown rendered as HTML", () => {
  const html = notesHtml(release({ body: "We fixed:\r\n\r\n* the `make_table` node\r\n* <script>alert(1)</script>" }));
  assert.equal(html, "<p>We fixed:</p>\n<ul>\n<li>the <code>make_table</code> node</li>\n<li>&lt;script&gt;alert(1)&lt;/script&gt;</li>\n</ul>");
});

test("a release without notes points to its page", () => {
  assert.equal(notesHtml(releases[0]), "<p>Release notes: https://github.com/nodebox/nodebox/releases/tag/v3.1.0</p>");
});

test("the feed has the newest releases first and links to the newest one", () => {
  const xml = renderAppcast(releases, Platform.MAC)!;
  const versions = [...xml.matchAll(/<appcast:version>(.*?)<\/appcast:version>/g)].map((m) => m[1]);
  assert.deepEqual(versions, ["3.1.0", "3.0.52", "3.0.51"]);
  // The app takes the last <link> in the document as the download link, so there is only one.
  assert.deepEqual([...xml.matchAll(/<link>(.*?)<\/link>/g)].map((m) => m[1]), ["https://github.com/nodebox/nodebox/releases/tag/v3.1.0"]);
  assert.match(xml, /<pubDate>Sun, 14 Jun 2026 20:50:01 \+0000<\/pubDate>/);
});

test("release notes can't end the description early", () => {
  const xml = renderAppcast([release({ body: "`]]>` and ]]>" })], Platform.MAC)!;
  assert.match(xml, /<description><!\[CDATA\[<p><code>]]&gt;<\/code> and ]]&gt;<\/p>]]><\/description>/);
});

test("no feed without releases", () => {
  assert.equal(renderAppcast([], Platform.MAC), null);
});
