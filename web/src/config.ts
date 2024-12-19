export let apiRoot: string;
export let assetsRoot: string;

if (import.meta.env.PROD) {
  apiRoot = "";
  assetsRoot = "https://nodeboxlive.s3.amazonaws.com";
} else {
  apiRoot = "";
  assetsRoot = "https://nodeboxtest.s3.amazonaws.com";
}
