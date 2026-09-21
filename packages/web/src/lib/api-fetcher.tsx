export default async function apiFetcher(url: string) {
  const res = await fetch(url);
  const json = await res.json();
  if (json.status !== "ok") {
    throw new Error(json.error.message);
  }
  return json;
}
