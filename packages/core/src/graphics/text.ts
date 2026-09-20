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

  /** The lines of text after wrapping at the box width (0 means no wrapping). */
  lines(): string[] {
    const paragraphs = this.text.split(/\r?\n/);
    if (this.width <= 0 || !fontProvider) return paragraphs;
    const lines: string[] = [];
    for (const paragraph of paragraphs) {
      const words = paragraph.split(" ");
      let line = "";
      for (const word of words) {
        const candidate = line ? `${line} ${word}` : word;
        if (line && fontProvider.width(candidate, this.fontName, this.fontSize) > this.width) {
          lines.push(line);
          line = word;
        } else {
          line = candidate;
        }
      }
      lines.push(line);
    }
    return lines;
  }

  /** Horizontal offset of a line's start relative to the baseline x, following the alignment. */
  private lineOffset(lineWidth: number): number {
    const boxWidth = this.width > 0 ? this.width : lineWidth;
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
      y += this.fontSize * this.lineHeight;
    }
    path.newContour();
    return this.transform.isIdentity() ? path : path.transformed(this.transform);
  }

  get path(): Path {
    return this.getPath();
  }

  /** The metrics as NodeBox 3 reports them: the union of the line boxes, before the transform. */
  getMetrics(): Rect {
    const lines = this.lines();
    let width = this.width;
    if (fontProvider) {
      for (const line of lines) width = Math.max(width, fontProvider.width(line, this.fontName, this.fontSize));
    } else {
      // Rough estimate: an average glyph is about half an em wide.
      for (const line of lines) width = Math.max(width, line.length * this.fontSize * 0.5);
    }
    const ascent = this.fontSize * 0.8;
    const height = lines.length * this.fontSize * this.lineHeight;
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
