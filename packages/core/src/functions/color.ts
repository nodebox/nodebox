// A port of nodebox.function.ColorFunctions (namespace "color").

import { Color } from "../graphics/color";
import { JavaScriptLibrary } from "../runtime/function-repository";

export function color(c: Color): Color {
  return c;
}

export function gray(g: number, alpha: number, range: number): Color {
  range = Math.max(range, 1);
  return new Color(g / range, g / range, g / range, alpha / range);
}

export function rgb(red: number, green: number, blue: number, alpha: number, range: number): Color {
  range = Math.max(range, 1);
  return new Color(red / range, green / range, blue / range, alpha / range);
}

export function hsb(hue: number, saturation: number, brightness: number, alpha: number, range: number): Color {
  range = Math.max(range, 1);
  return Color.fromHSB(hue / range, saturation / range, brightness / range, alpha / range);
}

export const colorLibrary = new JavaScriptLibrary("color", { color, gray, rgb, hsb });
