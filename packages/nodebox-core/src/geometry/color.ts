export interface Color {
  r: number;
  g: number;
  b: number;
  a: number;
}

export const BLACK: Color = { r: 0, g: 0, b: 0, a: 1 };
export const WHITE: Color = { r: 1, g: 1, b: 1, a: 1 };
export const TRANSPARENT: Color = { r: 0, g: 0, b: 0, a: 0 };

export function createColor(r: number, g: number, b: number, a = 1): Color {
  return { r, g, b, a };
}

export function colorFromHex(hex: string): Color {
  const h = hex.startsWith('#') ? hex.slice(1) : hex;
  if (h.length === 6) {
    return {
      r: parseInt(h.slice(0, 2), 16) / 255,
      g: parseInt(h.slice(2, 4), 16) / 255,
      b: parseInt(h.slice(4, 6), 16) / 255,
      a: 1,
    };
  }
  if (h.length === 8) {
    return {
      r: parseInt(h.slice(0, 2), 16) / 255,
      g: parseInt(h.slice(2, 4), 16) / 255,
      b: parseInt(h.slice(4, 6), 16) / 255,
      a: parseInt(h.slice(6, 8), 16) / 255,
    };
  }
  throw new Error(`Invalid hex color: ${hex}`);
}

export function colorToHex(color: Color): string {
  const r = Math.round(color.r * 255).toString(16).padStart(2, '0');
  const g = Math.round(color.g * 255).toString(16).padStart(2, '0');
  const b = Math.round(color.b * 255).toString(16).padStart(2, '0');
  const a = Math.round(color.a * 255).toString(16).padStart(2, '0');
  return `#${r}${g}${b}${a}`;
}

export function colorFromHSB(h: number, s: number, b: number, a = 1): Color {
  // h in 0-360, s in 0-1, b in 0-1
  const hNorm = ((h % 360) + 360) % 360;
  const c = b * s;
  const x = c * (1 - Math.abs((hNorm / 60) % 2 - 1));
  const m = b - c;
  let r1: number, g1: number, b1: number;
  if (hNorm < 60) { r1 = c; g1 = x; b1 = 0; }
  else if (hNorm < 120) { r1 = x; g1 = c; b1 = 0; }
  else if (hNorm < 180) { r1 = 0; g1 = c; b1 = x; }
  else if (hNorm < 240) { r1 = 0; g1 = x; b1 = c; }
  else if (hNorm < 300) { r1 = x; g1 = 0; b1 = c; }
  else { r1 = c; g1 = 0; b1 = x; }
  return { r: r1 + m, g: g1 + m, b: b1 + m, a };
}

export function colorToCSS(color: Color): string {
  const r = Math.round(color.r * 255);
  const g = Math.round(color.g * 255);
  const b = Math.round(color.b * 255);
  if (color.a === 1) {
    return `rgb(${r}, ${g}, ${b})`;
  }
  return `rgba(${r}, ${g}, ${b}, ${color.a.toFixed(3)})`;
}

export function colorEquals(a: Color, b: Color, epsilon = 1e-10): boolean {
  return Math.abs(a.r - b.r) < epsilon
    && Math.abs(a.g - b.g) < epsilon
    && Math.abs(a.b - b.b) < epsilon
    && Math.abs(a.a - b.a) < epsilon;
}
