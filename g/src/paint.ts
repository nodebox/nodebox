import { HSLColor, RGBColor, color, rgb } from "d3-color";

interface ColorLike {
  r: number;
  g: number;
  b: number;
  a?: number;
}

interface GradientLike {
  gradient: string;
  x1?: number;
  y1?: number;
  x2?: number;
  y2?: number;
  r1?: number;
  r2?: number;
  stops: ColorStop[];
}

// The paint types for gradients match the SVG element names <linearGradient> and <radialGradient>
export type PaintType = "unset" | "none" | "solid" | "linearGradient" | "radialGradient";

export interface ColorStop {
  offset: number; // Between 0 and 1
  color: SolidPaint;
}

function fToHex(value: number): string {
  return Math.round(value * 255)
    .toString(16)
    .padStart(2, "0");
}

export abstract class Paint {
  abstract readonly type: PaintType;
  abstract toString(): string;
  abstract clone(): Paint;

  static unset(): UnsetPaint {
    return new UnsetPaint();
  }

  static none(): NonePaint {
    return new NonePaint();
  }

  static solid(r: number, g: number, b: number, a: number = 1): SolidPaint {
    return new SolidPaint(r, g, b, a);
  }

  static transparent(): SolidPaint {
    return new SolidPaint(0, 0, 0, 0);
  }

  static black(): SolidPaint {
    return new SolidPaint(0, 0, 0);
  }

  static white(): SolidPaint {
    return new SolidPaint(1, 1, 1);
  }

  static lightGray(): SolidPaint {
    return new SolidPaint(0.75, 0.75, 0.75);
  }

  static darkGray(): SolidPaint {
    return new SolidPaint(0.15, 0.15, 0.15);
  }

  static linearGradient(
    stops: ColorStop[],
    x1: number = 0,
    y1: number = 0,
    x2: number = 1,
    y2: number = 0,
  ): LinearGradientPaint {
    return new LinearGradientPaint(stops, x1, y1, x2, y2);
  }

  static radialGradient(
    stops: ColorStop[],
    cx: number = 0.5,
    cy: number = 0.5,
    r: number = 0.5,
    fx?: number,
    fy?: number,
  ): RadialGradientPaint {
    return new RadialGradientPaint(stops, cx, cy, r, fx, fy);
  }

  // Parse from string (SVG format)
  static parse(value: string | SolidPaint | ColorLike | GradientLike): Paint {
    if (value === "none") {
      return Paint.none();
    }

    if (value instanceof SolidPaint) {
      return value;
    }

    if (typeof value === "object" && "gradient" in value) {
      const gradient = value as GradientLike;
      const stops: ColorStop[] = gradient.stops.map((stop) => ({
        offset: stop.offset,
        color: SolidPaint.parse(stop.color) as SolidPaint,
      }));
      if (gradient.gradient === "linear") {
        let { x1 = 0, y1 = 0, x2 = 1, y2 = 0 } = gradient;
        return new LinearGradientPaint(stops, x1, y1, x2, y2);
      } else if (gradient.gradient === "radial") {
        const { x1 = 0, y1 = 0, r1 = 0, x2 = 1, y2 = 0, r2 = 1 } = gradient;
        // FIXME: Conversion from Vega gradients to SVG gradients
        return new RadialGradientPaint(stops, x1, y1, r1, x2, y2);
      } else {
        console.error(`Unknown gradient type: ${value.gradient}`);
        return Paint.none();
      }
    }

    if (typeof value === "object" && "r" in value && "g" in value && "b" in value) {
      const { r, g, b, a = 1 } = value;
      return new SolidPaint(r, g, b, a);
    }

    if (typeof value === "string") {
      if (value === "transparent") {
        return Paint.transparent();
      }
      let clr = color(value);
      if (clr === null) {
        return new SolidPaint(0, 0, 0);
      } else if ("r" in clr) {
        clr = clr as RGBColor;
        return new SolidPaint(clr.r / 255, clr.g / 255, clr.b / 255, clr.opacity);
      } else if ("h" in clr) {
        clr = clr as HSLColor;
        clr = rgb(clr);
        return new SolidPaint(clr.r / 255, clr.g / 255, clr.b / 255, clr.opacity);
      } else {
        // This shouldn't happen, so return a solid purple color (this is a reference to the "missing texture" in game assets)
        return new SolidPaint(1, 0, 1);
      }
    }
    return new SolidPaint(0, 0, 0);
  }

  abstract isTransparent(): boolean;

  abstract toObject(): Record<string, unknown>;
}

export class UnsetPaint extends Paint {
  readonly type: PaintType = "unset";

  toString(): string {
    return "unset";
  }

  clone(): UnsetPaint {
    return new UnsetPaint();
  }

  isTransparent(): boolean {
    return true;
  }

  toObject(): Record<string, unknown> {
    return { type: "unset" };
  }
}

export class NonePaint extends Paint {
  readonly type: PaintType = "none";

  toString(): string {
    return "none";
  }

  clone(): NonePaint {
    return new NonePaint();
  }

  isTransparent(): boolean {
    return true;
  }

  toObject(): Record<string, unknown> {
    return { type: "none" };
  }
}

export class SolidPaint extends Paint {
  readonly type: PaintType = "solid";

  constructor(
    public r: number,
    public g: number,
    public b: number,
    public a: number = 1,
  ) {
    super();
  }

  clone(): SolidPaint {
    return new SolidPaint(this.r, this.g, this.b, this.a);
  }

  toString(): string {
    const r = Math.round(this.r * 255);
    const g = Math.round(this.g * 255);
    const b = Math.round(this.b * 255);
    if (this.a === 1.0) {
      return `#${fToHex(this.r)}${fToHex(this.g)}${fToHex(this.b)}`;
    } else if (this.r === 0.0 && this.g === 0.0 && this.b === 0 && this.a === 0.0) {
      return "transparent";
    } else {
      return `rgba(${r}, ${g}, ${b}, ${this.a})`;
    }
  }

  toHex(withOpacity: Boolean = false): string {
    const r = fToHex(this.r);
    const g = fToHex(this.g);
    const b = fToHex(this.b);
    const a = fToHex(this.a);
    return withOpacity ? `#${r}${g}${b}${a}` : `#${r}${g}${b}`;
  }

  isTransparent(): boolean {
    return this.a === 0;
  }

  toObject(): Record<string, unknown> {
    return { type: "solid", r: this.r, g: this.g, b: this.b, a: this.a };
  }
}

export type Color = SolidPaint;
export const Color = SolidPaint;

export class LinearGradientPaint extends Paint {
  readonly id: string = `gradient-${Math.floor(Math.random() * 1e8)}`;
  readonly type: PaintType = "linearGradient";

  constructor(
    public stops: ColorStop[],
    public x1: number = 0,
    public y1: number = 0,
    public x2: number = 1,
    public y2: number = 0,
  ) {
    super();
  }

  toString(): string {
    const coords = `${this.x1} ${this.y1} ${this.x2} ${this.y2}`;
    const stops = this.stops.map((stop) => `${stop.color.toString()} ${stop.offset * 100}%`).join(", ");
    return `linearGradient(${coords}, ${stops})`;
  }

  clone(): LinearGradientPaint {
    return new LinearGradientPaint(
      this.stops.map((stop) => ({
        offset: stop.offset,
        color: stop.color.clone() as SolidPaint,
      })),
      this.x1,
      this.y1,
      this.x2,
      this.y2,
    );
  }

  isTransparent(): boolean {
    return this.stops.every((stop) => stop.color.isTransparent());
  }

  toObject(): Record<string, unknown> {
    return { type: "linearGradient", stops: this.stops, x1: this.x1, y1: this.y1, x2: this.x2, y2: this.y2 };
  }
}

export class RadialGradientPaint extends Paint {
  readonly id: string = `gradient-${Math.floor(Math.random() * 1e8)}`;
  readonly type: PaintType = "radialGradient";

  constructor(
    public stops: ColorStop[],
    public cx: number = 0.5,
    public cy: number = 0.5,
    public r: number = 0.5,
    public fx?: number,
    public fy?: number,
  ) {
    super();
  }

  toString(): string {
    const focal = this.fx !== undefined && this.fy !== undefined ? `${this.fx} ${this.fy} ` : "";
    const coords = `${this.cx} ${this.cy} ${this.r} ${focal}`;
    const stops = this.stops.map((stop) => `${stop.color.toString()} ${stop.offset * 100}%`).join(", ");
    return `radialGradient(${coords}, ${stops})`;
  }

  clone(): RadialGradientPaint {
    return new RadialGradientPaint(
      this.stops.map((stop) => ({
        offset: stop.offset,
        color: stop.color.clone() as SolidPaint,
      })),
      this.cx,
      this.cy,
      this.r,
      this.fx,
      this.fy,
    );
  }

  isTransparent(): boolean {
    return this.stops.every((stop) => stop.color.isTransparent());
  }

  toObject(): Record<string, unknown> {
    return { type: "radialGradient", stops: this.stops, cx: this.cx, cy: this.cy, r: this.r, fx: this.fx, fy: this.fy };
  }
}

// static fromHex(hex: string): Color {
//   if (hex.length === 7 && hex[0] === "#") {
//     const r = parseInt(hex.slice(1, 3), 16);
//     const g = parseInt(hex.slice(3, 5), 16);
//     const b = parseInt(hex.slice(5, 7), 16);
//     return new Color(r / 255, g / 255, b / 255);
//   } else if (hex.length === 9 && hex[0] === "#") {
//     const r = parseInt(hex.slice(1, 3), 16);
//     const g = parseInt(hex.slice(3, 5), 16);
//     const b = parseInt(hex.slice(5, 7), 16);
//     const a = parseInt(hex.slice(7, 9), 16);
//     return new Color(r / 255, g / 255, b / 255, a / 255);
//   }
//   return new Color(0, 0, 0);
// }

// toHex(withOpacity: Boolean = true): string {
//   const r = fToHex(this.r);
//   const g = fToHex(this.g);
//   const b = fToHex(this.b);
//   const a = fToHex(this.a);
//   return withOpacity ? `#${r}${g}${b}${a}` : `#${r}${g}${b}`;
// }
