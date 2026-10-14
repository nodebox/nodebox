# Appcast

NodeBox checks for updates once a day, and from *Help → Check for Updates*, by fetching
`https://secure.nodebox.net/app/nodebox/appcast.xml?v=<version>&p=<platform>`
(`Application.getAppcastURL`). Every release since 3.0.50 uses that URL, so it has to keep working.

The feed is a static file, [`public/app/nodebox/appcast.xml`](public/app/nodebox/appcast.xml),
served by a Cloudflare Worker that has only static assets. The query string is ignored.

## Adding a release

When you bump `nodebox.version` (see [`docs/RELEASING.md`](../docs/RELEASING.md)), add an item
for that version at the **top** of the feed, in the same commit. `PublishedAppcastTest` fails
until the newest item matches `version.properties`.

What the app's parser (`nodebox.versioncheck.AppcastHandler`) needs:

- The app only looks at the first item. If its version is newer than the running app, it shows
  the update dialog with that item's description, and *Download* opens the channel `<link>`.
- No `<link>` inside an item: the parser keeps the last `<link>` in the file as the download link.
- `<appcast:version>` with that exact prefix, and a `pubDate` like `Sun, 14 Jun 2026 20:50:01 +0000`
  (English day and month names). An item with a bad date is unreadable and the check fails.
- The description is HTML in a `CDATA` block. The dialog renders a `<ul>` of `<li>`s well.

Once the commit reaches `master`, every running NodeBox will offer the update within a day,
so merge it when the GitHub Release (and the download page) has the new version.

## Deploying

Cloudflare Workers Builds, connected to this repository:

- Root directory: `appcast`
- Build command: none
- Deploy command: `npx wrangler deploy`
- Build watch paths: include `appcast/*`, so other pushes to `master` don't redeploy it.

## Going live

`secure.nodebox.net` is a DNS-only CNAME to `nodebox.github.io`, which has nothing there.
To serve the feed from this Worker:

1. In the `nodebox.net` zone, delete the `secure` CNAME record.
2. Add `"routes": [{ "pattern": "secure.nodebox.net", "custom_domain": true }]` to
   `wrangler.jsonc` and push, or add the custom domain in the Worker's settings.
3. Check it the way the app sees it:
   ```sh
   curl -i -A 'Java/14.0.1' 'https://secure.nodebox.net/app/nodebox/appcast.xml?v=3.0.52&p=2'
   ```
   It should be a `200` with the XML. A `403` or a challenge page means Bot Fight Mode or a
   security rule is blocking Java's user agent; the app can't solve a challenge.
