import type { Color } from '../geometry/color.js';
import { colorFromHSB } from '../geometry/color.js';

export function color(c: Color): Color {
  return c;
}

export function grayColor(gray: number, alpha: number, colorRange: number): Color {
  const r = colorRange > 0 ? gray / colorRange : 0;
  const a = colorRange > 0 ? alpha / colorRange : 0;
  return { r, g: r, b: r, a };
}

export function hsbColor(hue: number, saturation: number, brightness: number, alpha: number, colorRange: number): Color {
  const h = colorRange > 0 ? (hue / colorRange) * 360 : 0;
  const s = colorRange > 0 ? saturation / colorRange : 0;
  const b = colorRange > 0 ? brightness / colorRange : 0;
  const a = colorRange > 0 ? alpha / colorRange : 0;
  return colorFromHSB(h, s, b, a);
}

export function rgbColor(red: number, green: number, blue: number, alpha: number, colorRange: number): Color {
  const r = colorRange > 0 ? red / colorRange : 0;
  const g = colorRange > 0 ? green / colorRange : 0;
  const b = colorRange > 0 ? blue / colorRange : 0;
  const a = colorRange > 0 ? alpha / colorRange : 0;
  return { r, g, b, a };
}
