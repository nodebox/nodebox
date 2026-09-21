export function startCase(str: string): string {
  if (!str) return "";

  const words = str
    .replace(/([A-Z])/g, " $1")
    .trim()
    .split(" ")
    .map((word) => {
      return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
    });

  return words.join(" ");
}

/**
 * Replaces {{key}} in a template string with values[key].
 *
 * @param template  The string containing {{ placeholders }}.
 * @param values    A map from placeholder names to their replacement strings.
 * @returns         A new string with all placeholders substituted.
 */
export function evalTemplate(template: string, values: Record<string, any>): string {
  return template.replace(/\{\{\s*([^{}\s]+)\s*\}\}/g, (_match, key) => {
    if (!(key in values)) {
      throw new Error(`Missing template value for key "${key}"`);
    }
    return String(values[key]);
  });
}
