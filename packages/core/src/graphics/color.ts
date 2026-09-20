import { clamp } from "./math";

/**
 * An RGBA color with components in 0..1, plus derived HSB values (nodebox.graphics.Color).
 * Immutable. Serializes to "#rrggbbaa", the .ndbx format.
 */
export class Color {
  static readonly BLACK = new Color(0, 0, 0, 1);
  static readonly WHITE = new Color(1, 1, 1, 1);

  readonly r: number;
  readonly g: number;
  readonly b: number;
  readonly a: number;
  readonly h: number;
  readonly s: number;
  readonly v: number;

  constructor(r = 0, g = 0, b = 0, a = 1) {
    this.r = clamp(r, 0, 1);
    this.g = clamp(g, 0, 1);
    this.b = clamp(b, 0, 1);
    this.a = clamp(a, 0, 1);
    const [h, s, v] = rgbToHsb(this.r, this.g, this.b);
    this.h = h;
    this.s = s;
    this.v = v;
  }

  static gray(v: number, a = 1): Color {
    return new Color(v, v, v, a);
  }

  static fromHSB(hue: number, saturation: number, brightness: number, alpha = 1): Color {
    const [r, g, b] = hsbToRgb(clamp(hue, 0, 1), clamp(saturation, 0, 1), clamp(brightness, 0, 1));
    return new Color(r, g, b, alpha);
  }

  /** Parse #rgb, #rgba, #rrggbb or #rrggbbaa. */
  static parse(value: string): Color {
    if (!value.startsWith("#")) throw new Error(`The given value '${value}' is not of the format #112233.`);
    const hex = value.slice(1);
    let r: number, g: number, b: number, a = 255;
    if (hex.length === 3 || hex.length === 4) {
      r = parseInt(hex[0] + hex[0], 16);
      g = parseInt(hex[1] + hex[1], 16);
      b = parseInt(hex[2] + hex[2], 16);
      if (hex.length === 4) a = parseInt(hex[3] + hex[3], 16);
    } else if (hex.length === 6 || hex.length === 8) {
      r = parseInt(hex.slice(0, 2), 16);
      g = parseInt(hex.slice(2, 4), 16);
      b = parseInt(hex.slice(4, 6), 16);
      if (hex.length === 8) a = parseInt(hex.slice(6, 8), 16);
    } else {
      throw new Error(`The given value '${value}' is not of the format #112233.`);
    }
    if ([r, g, b, a].some((c) => Number.isNaN(c))) {
      throw new Error(`The given value '${value}' is not of the format #112233.`);
    }
    return new Color(r / 255, g / 255, b / 255, a / 255);
  }

  static valueOf(value: string): Color {
    return Color.parse(value);
  }

  static isColor(value: unknown): value is Color {
    return value instanceof Color;
  }

  get red(): number {
    return this.r;
  }
  get green(): number {
    return this.g;
  }
  get blue(): number {
    return this.b;
  }
  get alpha(): number {
    return this.a;
  }
  get hue(): number {
    return this.h;
  }
  get saturation(): number {
    return this.s;
  }
  get brightness(): number {
    return this.v;
  }

  isVisible(): boolean {
    return this.a > 0;
  }

  withAlpha(a: number): Color {
    return new Color(this.r, this.g, this.b, a);
  }

  equals(other: unknown): boolean {
    return other instanceof Color && other.r === this.r && other.g === this.g && other.b === this.b && other.a === this.a;
  }

  /** "#rrggbbaa", the .ndbx serialization. */
  toString(): string {
    return "#" + hex2(this.r) + hex2(this.g) + hex2(this.b) + hex2(this.a);
  }

  toHex(): string {
    return this.toString();
  }

  toCSS(): string {
    if (!this.isVisible()) return "none";
    if (this.a === 1) return "#" + hex2(this.r) + hex2(this.g) + hex2(this.b);
    return `rgba(${Math.round(this.r * 255)},${Math.round(this.g * 255)},${Math.round(this.b * 255)},${this.a.toFixed(2)})`;
  }

  toJSON(): { r: number; g: number; b: number; a: number } {
    return { r: this.r, g: this.g, b: this.b, a: this.a };
  }
}

function hex2(v: number): string {
  return Math.round(v * 255)
    .toString(16)
    .padStart(2, "0");
}

function hsbToRgb(h: number, s: number, v: number): [number, number, number] {
  if (s === 0) return [v, v, v];
  if (h === 1.0) h = 0.999998;
  h = h / (60.0 / 360);
  const i = Math.floor(h);
  const f = h - i;
  const p = v * (1 - s);
  const q = v * (1 - s * f);
  const t = v * (1 - s * (1 - f));
  switch (i) {
    case 0:
      return [v, t, p];
    case 1:
      return [q, v, p];
    case 2:
      return [p, v, t];
    case 3:
      return [p, q, v];
    case 4:
      return [t, p, v];
    default:
      return [v, p, q];
  }
}

function rgbToHsb(r: number, g: number, b: number): [number, number, number] {
  let h = 0;
  let s = 0;
  const v = Math.max(r, g, b);
  const d = v - Math.min(r, g, b);
  if (v !== 0) s = d / v;
  if (s !== 0) {
    if (r === v) h = 0 + (g - b) / d;
    else if (g === v) h = 2 + (b - r) / d;
    else h = 4 + (r - g) / d;
  }
  h = h * (60.0 / 360);
  if (h < 0) h = h + 1;
  return [h, s, v];
}
