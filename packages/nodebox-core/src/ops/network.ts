export function encodeUrl(value: string): string {
  return encodeURIComponent(value);
}

export function queryJson(json: string, query: string): unknown {
  try {
    const data = JSON.parse(json);
    // Simple dot-notation query (e.g., "results.0.name")
    const parts = query.split('.');
    let current: unknown = data;
    for (const part of parts) {
      if (current === null || current === undefined) return null;
      if (Array.isArray(current)) {
        const idx = parseInt(part, 10);
        current = isNaN(idx) ? null : current[idx];
      } else if (typeof current === 'object') {
        current = (current as Record<string, unknown>)[part];
      } else {
        return null;
      }
    }
    return current;
  } catch {
    return null;
  }
}
