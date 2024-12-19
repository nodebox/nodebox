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
