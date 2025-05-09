export let apiRoot: string;
export let assetsUrlTemplate: string;

if (import.meta.env.PROD) {
  apiRoot = "";
  assetsUrlTemplate = "https://nodeboxlive.s3.amazonaws.com/{{ userId }}/{{ projectId }}/blobs/{{ hash }}";
} else {
  apiRoot = "";
  assetsUrlTemplate = "https://nodeboxtest.s3.amazonaws.com/{{ userId }}/{{ projectId }}/blobs/{{ hash }}";
}
