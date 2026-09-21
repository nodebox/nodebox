import { config } from "@ndbx/runtime";

// The web app always talks to the server it was served from. The absolute defaults in the
// runtime package are for the embeddable player on other sites.
config.apiRoot = document.location.origin;
config.publishedUrlTemplate = "/api/published/{{ userId }}/{{ projectId }}";
config.libUrlTemplate = `${document.location.origin}/api/fn/{{ userId }}/{{ projectId }}/{{ file }}.js`;

if (import.meta.env.DEV) {
  config.assetsUrlTemplate = "/api/projects/{{ userId }}/{{ projectId }}/{{ version }}/assets/{{ hash }}";
  config.bareImportReplacer = (name: string) => import.meta.resolve(name);
}

export let apiRoot: string = config.apiRoot;
export let assetsUrlTemplate: string = config.assetsUrlTemplate;
