/// <reference path="./opentype.d.ts" />
// A font provider backed by opentype.js. Hosts register fonts by family (and style) name; text
// nodes then get real outlines and metrics, as NodeBox 3 had through AWT.

import * as opentype from "opentype.js";
import { Path } from "../graphics/path";
import { FontProvider, setFontProvider } from "../graphics/text";

// Node's ESM loader hands us the CommonJS build under "default"; bundlers give the namespace itself.
type OpenTypeModule = { parse: (buffer: ArrayBuffer) => opentype.Font };
const opentypeApi: OpenTypeModule =
  (opentype as unknown as { parse?: unknown }).parse !== undefined
    ? (opentype as unknown as OpenTypeModule)
    : (opentype as unknown as { default: OpenTypeModule }).default;

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
    const font = opentypeApi.parse(buffer);
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

  ascent(fontName: string, fontSize: number): number {
    const font = this.lookup(fontName);
    if (!font) return fontSize * 0.93;
    return (font.ascender / font.unitsPerEm) * fontSize;
  }

  width(text: string, fontName: string, fontSize: number): number {
    const font = this.lookup(fontName);
    if (!font) return text.length * fontSize * 0.5;
    return advanceWidth(font, text, fontSize, true);
  }

  measure(text: string, fontName: string, fontSize: number): number {
    const font = this.lookup(fontName);
    if (!font) return text.length * fontSize * 0.5;
    return advanceWidth(font, text, fontSize, false);
  }
}

/**
 * The outline commands for a string, laid out glyph by glyph without kerning or substitutions,
 * the way AWT's TextLayout did for NodeBox 3. TrueType outlines are walked exactly as FreeType's
 * FT_Outline_Decompose does (same start point, implied midpoints, closing line), in 26.6 fixed
 * point, so coordinates agree with the Java engine to the 1/64 pixel.
 */
function glyphCommands(font: opentype.Font, text: string, fontSize: number): opentype.PathCommand[] {
  const commands: opentype.PathCommand[] = [];
  const scale = fixedSize(fontSize) / font.unitsPerEm;
  let x = 0;
  let previous: opentype.Glyph | null = null;
  for (const char of text) {
    const glyph = font.charToGlyph(char);
    if (previous) x += kerning(font, previous, glyph) * scale;
    if (glyph.points && glyph.points.length > 0) commands.push(...trueTypeCommands(glyph.points, scale, x));
    else for (const cmd of glyph.getPath(0, 0, fontSize).commands) commands.push(shifted(quantized(cmd), x));
    x += (glyph.advanceWidth ?? 0) * scale;
    previous = glyph;
  }
  return commands;
}

/**
 * Pair kerning in font units. HarfBuzz (behind AWT since JDK 9) applies the kern feature by
 * default; opentype.js only reads simple GPOS pairs, so the legacy kern table comes first.
 */
function kerning(font: opentype.Font, left: opentype.Glyph, right: opentype.Glyph): number {
  const pair = font.kerningPairs?.[`${left.index},${right.index}`];
  if (pair !== undefined) return pair;
  try {
    return font.getKerningValue(left, right) || 0;
  } catch {
    return 0;
  }
}

interface FixedPoint {
  x: number;
  y: number;
  on: boolean;
}

/** Walk TrueType contours like FT_Outline_Decompose, in 26.6 units; emit pixel commands (y down). */
function trueTypeCommands(points: opentype.GlyphPoint[], scale: number, dx: number): opentype.PathCommand[] {
  const out: opentype.PathCommand[] = [];
  const fixed = (v: number) => Math.sign(v) * Math.round(Math.abs(v) * scale * 64);
  const px = (p: { x: number; y: number }) => ({ x: dx + p.x / 64, y: -p.y / 64 });
  const contours: FixedPoint[][] = [];
  let current: FixedPoint[] = [];
  for (const p of points) {
    current.push({ x: fixed(p.x), y: fixed(p.y), on: p.onCurve });
    if (p.lastPointOfContour) {
      contours.push(current);
      current = [];
    }
  }
  if (current.length > 0) contours.push(current);

  for (const contour of contours) {
    if (contour.length === 0) continue;
    let limit = contour.length;
    let index = 0;
    let vStart = contour[0];
    const vLast = contour[contour.length - 1];
    if (!vStart.on) {
      // A contour that starts off-curve begins at its last point (if on-curve) or the midpoint.
      if (vLast.on) {
        vStart = vLast;
        limit--;
      } else {
        vStart = { x: Math.trunc((vStart.x + vLast.x) / 2), y: Math.trunc((vStart.y + vLast.y) / 2), on: true };
      }
      index = -1;
    }
    const s = px(vStart);
    out.push({ type: "M", x: s.x, y: s.y } as opentype.PathCommand);
    let control: FixedPoint | null = null;
    for (index++; index < limit; index++) {
      const p = contour[index];
      if (p.on) {
        if (control === null) {
          const q = px(p);
          out.push({ type: "L", x: q.x, y: q.y } as opentype.PathCommand);
        } else {
          out.push(quad(px(control), px(p)));
          control = null;
        }
      } else if (control === null) {
        control = p;
      } else {
        const mid = { x: Math.trunc((control.x + p.x) / 2), y: Math.trunc((control.y + p.y) / 2), on: true };
        out.push(quad(px(control), px(mid)));
        control = p;
      }
    }
    if (control !== null) out.push(quad(px(control), s));
    else out.push({ type: "L", x: s.x, y: s.y } as opentype.PathCommand);
    out.push({ type: "Z" } as opentype.PathCommand);
  }
  return out;
}

function quad(c: { x: number; y: number }, p: { x: number; y: number }): opentype.PathCommand {
  return { type: "Q", x1: c.x, y1: c.y, x: p.x, y: p.y } as opentype.PathCommand;
}

function shifted(cmd: opentype.PathCommand, dx: number): opentype.PathCommand {
  const s = (v: number | undefined) => (v === undefined ? v : v + dx);
  return { ...cmd, x: s(cmd.x)!, x1: s(cmd.x1), x2: s(cmd.x2) } as opentype.PathCommand;
}

// Outlines without point data (CFF) are still snapped to the 1/64 pixel grid FreeType used.
function quantized(cmd: opentype.PathCommand): opentype.PathCommand {
  const q = (v: number | undefined) => (v === undefined ? v : Math.sign(v) * Math.round(Math.abs(v) * 64) / 64);
  return { ...cmd, x: q(cmd.x)!, y: q(cmd.y)!, x1: q(cmd.x1), y1: q(cmd.y1), x2: q(cmd.x2), y2: q(cmd.y2) } as opentype.PathCommand;
}

/** FT_Set_Char_Size takes the size in 26.6 fixed point, so fractional sizes truncate to 1/64. */
function fixedSize(fontSize: number): number {
  return Math.floor(fontSize * 64) / 64;
}

function advanceWidth(font: opentype.Font, text: string, fontSize: number, kerned: boolean): number {
  const scale = fixedSize(fontSize) / font.unitsPerEm;
  let width = 0;
  let previous: opentype.Glyph | null = null;
  for (const char of text) {
    const glyph = font.charToGlyph(char);
    if (previous && kerned) width += kerning(font, previous, glyph) * scale;
    width += (glyph.advanceWidth ?? 0) * scale;
    previous = glyph;
  }
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
