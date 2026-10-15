# Appcast

NodeBox checks for updates once a day, and from *Help → Check for Updates*, by fetching
`https://secure.nodebox.net/app/nodebox/appcast.xml?v=<version>&p=<platform>`
(`Application.getAppcastURL`). Every release since 3.0.50 uses that URL, so it has to keep working.

This Cloudflare Worker builds that feed from the [GitHub releases](https://github.com/nodebox/nodebox/releases).
There is nothing to edit when you release: publish the GitHub release and running copies of NodeBox
offer it within about an hour.

## What ends up in the feed

- Published releases with a plain version tag (`v3.1.0`). Drafts, prereleases and tags like `v3.0a27` are skipped.
- Only releases with an installer for the platform in `?p=` (`nodebox.ui.Platform`): `1` is Windows
  (`.msi`, `.exe`), `2` is macOS (`.dmg`, `.zip`), anything else accepts either. So a Mac user is not
  offered 3.0.53, which only shipped an `.msi`, and a release isn't offered on a platform until its
  installer is attached.
- The newest three, newest first. The app only reads the first one: if it is newer than the running
  version, it shows the update dialog with the release notes, and *Download* opens that release's page.
- The release notes are the release's Markdown body, rendered as HTML. A release without notes shows
  the address of its page instead, so write the notes before you publish.

## Development

```sh
npm install
npm test && npm run typecheck
npm run env   # writes .env with GITHUB_TOKEN from 1Password
npm run dev   # http://localhost:8787/app/nodebox/appcast.xml?p=2
```

## GitHub rate limit

The Worker asks GitHub at most once an hour per Cloudflare data center, and revalidates with the
ETag (a `304` doesn't count against the limit). If GitHub fails, it keeps serving the last copy.

Set a `GITHUB_TOKEN` secret anyway: unauthenticated calls share a limit of 60 an hour with everything
else on the same egress IP, and `wrangler dev` already hit that limit while this was being built. A
fine-grained token with no permissions beyond read-only access to public repositories is enough.

The token is in 1Password at `op://NodeBox/NodeBox App/Appcast/GITHUB_TOKEN`. After the first deploy,
copy it into the Worker (needs the 1Password CLI signed in; the value goes over a pipe, nothing is printed):

```sh
npm run secrets
```

It expires after a year. An expired token gets a `401`, and the feed then lives on its cached copy
until the cache drops it, so replace it in 1Password and run `npm run secrets` again before then.

## Deploying

Cloudflare Workers Builds, connected to this repository:

- Root directory: `appcast`
- Build command: `npm ci && npm test && npm run typecheck`
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
