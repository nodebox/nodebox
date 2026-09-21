// The packages a classic NodeBox Live project renders against. They are plain JavaScript, and the
// classic runtime passes them to function sources as names rather than calling them from here.

declare module "g.js" {
  const g: Record<string, unknown>;
  export default g;
}

declare module "opentype-classic" {
  const opentype: Record<string, unknown>;
  export default opentype;
}
