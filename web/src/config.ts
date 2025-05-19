import { config } from "@ndbx/runtime";

if (import.meta.env.DEV) {
  config.apiRoot = document.location.origin;
  config.publishedUrlTemplate = "/api/published/{{ userId }}/{{ projectId }}";
  config.assetsUrlTemplate = "/api/projects/{{ userId }}/{{ projectId }}/{{ version }}/assets/{{ hash }}";
  config.bareImportReplacer = (name: string) => import.meta.resolve(name);
}

export let apiRoot: string = config.apiRoot;
export let assetsUrlTemplate: string = config.assetsUrlTemplate;
