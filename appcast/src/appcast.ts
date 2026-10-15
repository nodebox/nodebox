import MarkdownIt from "markdown-it";

// The parts of a GitHub release (GET /repos/{owner}/{repo}/releases) that the appcast uses.
export interface Release {
  tag_name: string;
  name: string | null;
  body: string | null;
  html_url: string;
  published_at: string | null;
  draft: boolean;
  prerelease: boolean;
  assets: { name: string }[];
}

// nodebox.ui.Platform.current_platform, sent by the app as ?p=.
export const Platform = { WIN: 1, MAC: 2, OTHER: 3 } as const;

// Only plain version tags: v3.1.0, not v3.0a27.
const VERSION_TAG = /^v?(\d+(?:\.\d+)*)$/;

const INSTALLERS: Record<number, RegExp> = {
  [Platform.WIN]: /\.(msi|exe)$/i,
  [Platform.MAC]: /\.(dmg|zip)$/i,
  [Platform.OTHER]: /\.(msi|exe|dmg|zip)$/i,
};

const markdown = new MarkdownIt({ html: false, linkify: true });

export function versionOf(release: Release): string | null {
  return VERSION_TAG.exec(release.tag_name)?.[1] ?? null;
}

export function compareVersions(a: string, b: string): number {
  const as = a.split(".").map(Number);
  const bs = b.split(".").map(Number);
  for (let i = 0; i < Math.max(as.length, bs.length); i++) {
    const diff = (as[i] ?? 0) - (bs[i] ?? 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

// The app parses pubDate with SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).
export function rssDate(iso: string): string {
  return new Date(iso).toUTCString().replace("GMT", "+0000");
}

// Release notes are Markdown on GitHub; the update dialog shows HTML.
export function notesHtml(release: Release): string {
  const body = release.body?.trim();
  // The dialog doesn't follow links, so show the address itself.
  if (!body) return `<p>Release notes: ${escapeXml(release.html_url)}</p>`;
  return markdown.render(body).trim();
}

// Published releases with an installer for the platform, newest first.
export function offeredReleases(releases: Release[], platform: number): Release[] {
  const installer = INSTALLERS[platform] ?? INSTALLERS[Platform.OTHER];
  return releases
    .filter((r) => !r.draft && !r.prerelease && r.published_at && versionOf(r))
    .filter((r) => r.assets.some((a) => installer.test(a.name)))
    .sort((a, b) => compareVersions(versionOf(b)!, versionOf(a)!));
}

// Renders the feed nodebox.versioncheck reads. It only looks at the first item, and takes the last
// <link> in the document as the download link, so items have no <link> of their own.
export function renderAppcast(releases: Release[], platform: number, limit = 3): string | null {
  const offered = offeredReleases(releases, platform).slice(0, limit);
  if (offered.length === 0) return null;
  const items = offered.map((r) => {
    const version = versionOf(r)!;
    return `    <item>
      <title>Version ${escapeXml(version)}</title>
      <description>${cdata(notesHtml(r))}</description>
      <appcast:version>${escapeXml(version)}</appcast:version>
      <pubDate>${rssDate(r.published_at!)}</pubDate>
    </item>`;
  });
  return `<?xml version="1.0" encoding="utf-8"?>
<rss version="2.0" xmlns:appcast="https://nodebox.net/xml-namespaces/appcast">
  <channel>
    <title>NodeBox Appcast</title>
    <description>Most recent changes with links to updates.</description>
    <language>en</language>
${items.join("\n")}
    <link>${escapeXml(offered[0].html_url)}</link>
  </channel>
</rss>
`;
}

function escapeXml(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// notesHtml escapes every ">" in text, so the HTML can't contain "]]>".
function cdata(s: string): string {
  return `<![CDATA[${s}]]>`;
}
