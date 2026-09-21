# @ndbx/desktop

NodeBox for the desktop: the NodeBox editor (`packages/web`) inside an Electron window, served by the
same Hono application that runs on Cloudflare (`packages/server`), with projects stored on disk under
the user's application data folder. NodeBox 3 documents (`.ndbx`) open through `@ndbx/core`, which
converts them to projects the editor can show and evaluates them with the core engine.

```bash
npm run build          # from the repository root: builds g, core, runtime, web and server
npm run desktop:dev    # bundles the main and preload scripts and starts Electron
npm run desktop:build  # bundles without starting
npm --workspace @ndbx/desktop run package   # electron-builder: dmg/zip, nsis, AppImage
```

The File menu opens `.ndbx` and `project.json` documents and saves projects back as either format.
Files passed on the command line (or double-clicked, once the app is packaged) open on start.
