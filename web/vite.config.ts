import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

function resolveImport(name: string) {
  if (process.env.NODE_ENV !== "production") {
    return import.meta.resolve(name).replace("file:///", "/@fs/");
  } else {
    return `https://esm.sh/${name}`;
  }
}

// NodeBox nodes use ESM imports to load different libraries.
// To support local dependencies (e.g. "@nodebox/g") we need to inject an import map into the HTML.
// In production, we can refer to the published packages using ESM URLs (e.g. "https://esm.sh/@nodebox/g"),
// but in development, we need to resolve the local imports to the local file system.
// Vite puts these under the "/@fs/" path.
// We cannot generate the import map statically because the local imports are resolved at runtime.
// This plugin dynamically generates the import map and injects it into the HTML.
function htmlImportMapInject() {
  return {
    name: "html-importmap-inject",
    transformIndexHtml(html: string) {
      // Dynamically generate the import map
      const importMap = {
        imports: {
          "@ndbx/g": resolveImport("@ndbx/g"),
        },
      };

      // Inject the import map script before the other scripts
      const importMapScript = `<script type="importmap">${JSON.stringify(importMap)}</script>`;
      return html.replace(/<head>(.*?)<\/head>/s, `<head>$1${importMapScript}</head>`);
    },
  };
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [["module:@preact/signals-react-transform"]],
      },
    }),
    htmlImportMapInject(),
  ],
  build: {
    sourcemap: true,
    chunkSizeWarningLimit: 5000,
  },
  server: {
    proxy: {
      "/admin": "http://localhost:3000",
      "/api": "http://localhost:3000",
      "/guide": "http://localhost:3000",
      // Note that `/embed/` ends with a slash, to avoid conflicting with the /embed-preview route
      "/embed/": "http://localhost:3000",
    },
  },
});
