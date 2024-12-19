import { Color } from "@ndbx/runtime";

export function colorToHex(color: Color): string {
  const toHex = (v: number) => (v * 255).toString(16).padStart(2, "0");
  return `#${toHex(color.r)}${toHex(color.g)}${toHex(color.b)}`;
}

export function colorToCss(color: Color): string {
  const r = (v: number) => v * 255;
  return color.a === 1
    ? `rgb(${r(color.r)} ${r(color.g)} ${r(color.b)})`
    : `rgb(${r(color.r)} ${r(color.g)} ${r(color.b)} / ${color.a})`;
}

export function hexToColor(hex: string): Color {
  const r = parseInt(hex.substring(1, 3), 16) / 255;
  const g = parseInt(hex.substring(3, 5), 16) / 255;
  const b = parseInt(hex.substring(5, 7), 16) / 255;
  const a = hex.length === 9 ? parseInt(hex.substring(7, 9), 16) / 255 : 1;
  return { r, g, b, a };
}

export function colorIsTransparent(color: Color): boolean {
  if (!color || typeof color.a !== "number") {
    return true;
  }
  return color.a === 0;
}
