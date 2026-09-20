// Minimal typings for the parts of opentype.js 2.x the font provider uses.
declare module "opentype.js" {
  export interface PathCommand {
    type: "M" | "L" | "C" | "Q" | "Z";
    x: number;
    y: number;
    x1: number;
    y1: number;
    x2: number;
    y2: number;
  }
  export interface GlyphPath {
    commands: PathCommand[];
  }
  export interface Glyph {
    advanceWidth?: number;
    getPath(x: number, y: number, fontSize: number): GlyphPath;
  }
  export interface Font {
    names: Record<string, Record<string, string> | undefined>;
    unitsPerEm: number;
    ascender: number;
    descender: number;
    getPath(text: string, x: number, y: number, fontSize: number, options?: Record<string, unknown>): GlyphPath;
    getAdvanceWidth(text: string, fontSize: number, options?: Record<string, unknown>): number;
    charToGlyph(char: string): Glyph;
    stringToGlyphs(text: string): Glyph[];
  }
  export function parse(buffer: ArrayBuffer): Font;
}
