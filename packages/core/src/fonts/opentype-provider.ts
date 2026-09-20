/// <reference path="./opentype.d.ts" />
// A font provider backed by opentype.js. Hosts register fonts by family (and style) name; text
// nodes then get real outlines and metrics, as NodeBox 3 had through AWT.

import * as opentype from "opentype.js";
import { Path } from "../graphics/path";
import { FontProvider, setFontProvider } from "../graphics/text";

export interface RegisteredFont {
  family: string;
  style: string;
  font: opentype.Font;
}

export class OpenTypeFontProvider implements FontProvider {
  private fonts: RegisteredFont[] = [];
  private fallback: opentype.Font | null = null;
  private cache = new Map<string, opentype.Font | null>();

  /** Register a parsed font. The first registered font is the fallback for unknown names. */
  register(font: opentype.Font, family?: string, style?: string): void {
    const names = font.names as Record<string, Record<string, string> | undefined>;
    const fam = family ?? nameOf(names.fontFamily) ?? "Unknown";
    const sty = style ?? nameOf(names.fontSubfamily) ?? "Regular";
    this.fonts.push({ family: fam, style: sty, font });
    if (!this.fallback) this.fallback = font;
    this.cache.clear();
  }

  /** Parse a TrueType/OpenType file (ArrayBuffer) and register it. */
  registerBuffer(buffer: ArrayBuffer, family?: string, style?: string): opentype.Font {
    const font = opentype.parse(buffer);
    this.register(font, family, style);
    return font;
  }

  setFallback(font: opentype.Font): void {
    this.fallback = font;
    this.cache.clear();
  }

  families(): string[] {
    return Array.from(new Set(this.fonts.map((f) => f.family)));
  }

  hasFont(fontName: string): boolean {
    return this.lookup(fontName, false) !== null;
  }

  /** Resolve "Verdana", "Verdana-Bold", "Helvetica Bold Italic" and friends. */
  private lookup(fontName: string, useFallback = true): opentype.Font | null {
    const key = `${fontName}\u0000${useFallback}`;
    if (this.cache.has(key)) return this.cache.get(key)!;
    const { family, style } = parseFontName(fontName);
    const norm = (s: string) => s.toLowerCase().replace(/[\s_-]+/g, "");
    let best: RegisteredFont | undefined;
    const candidates = this.fonts.filter((f) => norm(f.family) === norm(family));
    if (candidates.length > 0) {
      best = candidates.find((f) => norm(f.style) === norm(style)) ?? candidates.find((f) => norm(f.style) === "regular") ?? candidates[0];
    }
    const result = best ? best.font : useFallback ? this.fallback : null;
    this.cache.set(key, result);
    return result;
  }

  outline(text: string, fontName: string, fontSize: number): Path {
    const font = this.lookup(fontName);
    const path = new Path();
    if (!font) return path;
    for (const cmd of glyphCommands(font, text, fontSize)) {
      switch (cmd.type) {
        case "M":
          path.moveto(cmd.x, cmd.y);
          break;
        case "L":
          path.lineto(cmd.x, cmd.y);
          break;
        case "C":
          path.curveto(cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x, cmd.y);
          break;
        case "Q": {
          const prev = lastPoint(path);
          const c1x = prev.x + (2 / 3) * (cmd.x1 - prev.x);
          const c1y = prev.y + (2 / 3) * (cmd.y1 - prev.y);
          const c2x = cmd.x + (2 / 3) * (cmd.x1 - cmd.x);
          const c2y = cmd.y + (2 / 3) * (cmd.y1 - cmd.y);
          path.curveto(c1x, c1y, c2x, c2y, cmd.x, cmd.y);
          break;
        }
        case "Z":
          path.close();
          break;
      }
    }
    path.newContour();
    return path;
  }

  width(text: string, fontName: string, fontSize: number): number {
    const font = this.lookup(fontName);
    if (!font) return text.length * fontSize * 0.5;
    try {
      return font.getAdvanceWidth(text, fontSize);
    } catch {
      return advanceWidth(font, text, fontSize);
    }
  }
}

/**
 * The outline commands for a string. opentype.js's shaping fails on some GSUB tables; fall back
 * to laying out the glyphs one by one without substitutions.
 */
function glyphCommands(font: opentype.Font, text: string, fontSize: number): opentype.PathCommand[] {
  try {
    return font.getPath(text, 0, 0, fontSize).commands;
  } catch {
    const commands: opentype.PathCommand[] = [];
    const scale = fontSize / font.unitsPerEm;
    let x = 0;
    for (const char of text) {
      const glyph = font.charToGlyph(char);
      commands.push(...glyph.getPath(x, 0, fontSize).commands);
      x += (glyph.advanceWidth ?? 0) * scale;
    }
    return commands;
  }
}

function advanceWidth(font: opentype.Font, text: string, fontSize: number): number {
  const scale = fontSize / font.unitsPerEm;
  let width = 0;
  for (const char of text) width += (font.charToGlyph(char).advanceWidth ?? 0) * scale;
  return width;
}

function lastPoint(path: Path): { x: number; y: number } {
  const contour = path.contours[path.contours.length - 1];
  const p = contour?.points[contour.points.length - 1];
  return p ? { x: p.x, y: p.y } : { x: 0, y: 0 };
}

function nameOf(record: Record<string, string> | undefined): string | undefined {
  if (!record) return undefined;
  return record.en ?? Object.values(record)[0];
}

/** Split a Java-style font name into family and style: "Verdana-Bold" -> Verdana / Bold. */
export function parseFontName(fontName: string): { family: string; style: string } {
  const name = fontName.trim();
  const dash = name.lastIndexOf("-");
  if (dash > 0) return { family: name.slice(0, dash), style: name.slice(dash + 1) };
  const m = /^(.*?)\s+(Bold Italic|Bold Oblique|Bold|Italic|Oblique|Light|Medium|Black|Regular|Thin|Heavy)$/i.exec(name);
  if (m) return { family: m[1], style: m[2] };
  return { family: name, style: "Regular" };
}

/** Create a provider, register the given font buffers and install it as the global font provider. */
export function installOpenTypeFonts(buffers: { buffer: ArrayBuffer; family?: string; style?: string }[]): OpenTypeFontProvider {
  const provider = new OpenTypeFontProvider();
  for (const { buffer, family, style } of buffers) provider.registerBuffer(buffer, family, style);
  setFontProvider(provider);
  return provider;
}
