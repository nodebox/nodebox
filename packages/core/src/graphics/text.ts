import { Color } from "./color";
import { Path } from "./path";
import { Point } from "./point";
import { Rect } from "./rect";
import { Transform } from "./transform";

export type TextAlign = "LEFT" | "RIGHT" | "CENTER" | "JUSTIFY";

/**
 * Turns text into outlines and measures it. NodeBox 3 used AWT fonts for this; in the TypeScript
 * core the host (desktop app, browser, tests) installs a provider, typically backed by opentype.js
 * or the canvas measureText API. Without a provider, text keeps rendering as text but has no outline.
 */
export interface FontProvider {
  /** Outline of a single line of text with its baseline origin at (0, 0). */
  outline(text: string, fontName: string, fontSize: number): Path;
  /** Advance width of the text, in the same units as fontSize. */
  width(text: string, fontName: string, fontSize: number): number;
  /** Whether the font can be resolved by this provider. */
  hasFont?(fontName: string): boolean;
  /** Ascent of the font at this size (the line advance is ascent * lineHeight, as in AWT). */
  ascent?(fontName: string, fontSize: number): number;
  /** Advance width without kerning: what AWT's LineBreakMeasurer compares against the box width. */
  measure?(text: string, fontName: string, fontSize: number): number;
}

function ascentOf(fontName: string, fontSize: number): number {
  return fontProvider?.ascent?.(fontName, fontSize) ?? fontSize * 0.93;
}

let fontProvider: FontProvider | null = null;

export function setFontProvider(provider: FontProvider | null): void {
  fontProvider = provider;
}

export function getFontProvider(): FontProvider | null {
  return fontProvider;
}

/**
 * A block of text with a font, size, alignment and an optional box for wrapping (nodebox.graphics.Text).
 * Unlike paths, a Text keeps a transform, because its outline is only known once a font is available.
 */
export class Text {
  text: string;
  baseLineX: number;
  baseLineY: number;
  width: number;
  height: number;
  fontName = "Helvetica";
  fontSize = 24;
  lineHeight = 1.2;
  align: TextAlign = "CENTER";
  fillColor: Color | null = Color.BLACK;
  transform = new Transform();

  constructor(text: string, x = 0, y = 0, width = 0, height = 0) {
    this.text = text;
    this.baseLineX = x;
    this.baseLineY = y;
    this.width = width;
    this.height = height;
  }

  static isText(value: unknown): value is Text {
    return value instanceof Text;
  }

  get fill(): Color | null {
    return this.fillColor;
  }
  set fill(c: Color | null) {
    this.fillColor = c;
  }

  translate(tx: number, ty: number): void {
    this.transform.translate(tx, ty);
  }

  rotate(degrees: number): void {
    this.transform.rotate(degrees);
  }

  scale(sx: number, sy = sx): void {
    this.transform.scale(sx, sy);
  }

  isEmpty(): boolean {
    return this.text.length === 0;
  }

  /**
   * The lines of text after wrapping at the box width (0 means no wrapping). Like AWT's
   * LineBreakMeasurer: break after spaces, and inside a word only when the word alone is too wide.
   */
  lines(): string[] {
    const paragraphs = this.text.split(/\r?\n/);
    if (this.width <= 0 || !fontProvider) return paragraphs;
    const provider = fontProvider;
    const measure = provider.measure?.bind(provider) ?? provider.width.bind(provider);
    const fits = (s: string) => measure(s.replace(/ +$/, ""), this.fontName, this.fontSize) <= this.width;
    // Lines keep their trailing spaces: the layout's advance (used for centering) includes them.
    const lines: string[] = [];
    for (const paragraph of paragraphs) {
      const chunks = paragraph.match(/[^ ]* */g)?.filter((c) => c.length > 0) ?? [];
      let line = "";
      for (const chunk of chunks) {
        if (fits(line + chunk)) {
          line += chunk;
          continue;
        }
        if (line !== "") {
          lines.push(line);
          line = "";
        }
        // The chunk alone is too wide: take as many characters as fit, at least one per line.
        let rest = chunk;
        while (!fits(rest)) {
          let n = 1;
          while (n < rest.length && fits(rest.slice(0, n + 1))) n++;
          lines.push(rest.slice(0, n));
          rest = rest.slice(n);
        }
        line = rest;
      }
      lines.push(line);
    }
    return lines;
  }

  /** Horizontal offset of a line's start relative to the baseline x, following the alignment. */
  private lineOffset(lineWidth: number): number {
    // Without a box the text hangs from its origin: centered or right-aligned around x (as in Java).
    const boxWidth = this.width > 0 ? this.width : 0;
    switch (this.align) {
      case "RIGHT":
        return boxWidth - lineWidth;
      case "CENTER":
        return (boxWidth - lineWidth) / 2;
      default:
        return 0;
    }
  }

  /** The outline of the text as a path, or an empty path when no font provider is installed. */
  getPath(): Path {
    const path = new Path();
    path.fillColor = this.fillColor;
    if (!fontProvider) return path;
    const lines = this.lines();
    let y = this.baseLineY;
    for (const line of lines) {
      const lineWidth = fontProvider.width(line, this.fontName, this.fontSize);
      const x = this.baseLineX + this.lineOffset(lineWidth);
      const outline = fontProvider.outline(line, this.fontName, this.fontSize);
      path.extend(outline.transformed(Transform.translated(x, y)));
      y += ascentOf(this.fontName, this.fontSize) * this.lineHeight;
    }
    path.newContour();
    return this.transform.isIdentity() ? path : path.transformed(this.transform);
  }

  get path(): Path {
    return this.getPath();
  }

  /**
   * The metrics as NodeBox 3 reports them: the union of each line's ink bounds relative to the
   * line's own origin (Java unions TextLayout.getBounds() without compensating x and y).
   */
  getMetrics(): Rect {
    const lines = this.lines();
    if (lines.length === 0 || lines.every((line) => line === "")) return new Rect();
    let width = this.width;
    if (fontProvider) {
      // TextLayout.getBounds() spans from the layout origin to the ink, so include x = 0.
      let union = new Rect(0, 0, 0, 0);
      for (const line of lines) union = union.united(fontProvider.outline(line, this.fontName, this.fontSize).getBounds());
      return union;
    } else {
      // Rough estimate: an average glyph is about half an em wide.
      for (const line of lines) width = Math.max(width, line.length * this.fontSize * 0.5);
    }
    const ascent = ascentOf(this.fontName, this.fontSize);
    const height = lines.length * ascent * this.lineHeight;
    return new Rect(this.baseLineX, this.baseLineY - ascent, width, height);
  }

  get bounds(): Rect {
    return this.getBounds();
  }

  getBounds(): Rect {
    if (fontProvider) return this.getPath().getBounds();
    return this.transform.mapRect(this.getMetrics()).normalized();
  }

  transformed(t: Transform): Text {
    const text = this.clone();
    text.transform = t.clone().concatenate(this.transform);
    return text;
  }

  clone(): Text {
    const t = new Text(this.text, this.baseLineX, this.baseLineY, this.width, this.height);
    t.fontName = this.fontName;
    t.fontSize = this.fontSize;
    t.lineHeight = this.lineHeight;
    t.align = this.align;
    t.fillColor = this.fillColor;
    t.transform = this.transform.clone();
    return t;
  }

  toString(): string {
    return `<Text ${JSON.stringify(this.text)}>`;
  }
}

export function parseTextAlign(s: string): TextAlign {
  const upper = s.toUpperCase();
  if (upper === "LEFT" || upper === "RIGHT" || upper === "CENTER" || upper === "JUSTIFY") return upper;
  return "CENTER";
}

export { Point };
