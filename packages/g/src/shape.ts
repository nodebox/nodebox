import { Transform } from "./transform";
import { Paint, SolidPaint, LinearGradientPaint, RadialGradientPaint } from "./paint";
import { ClipPath } from "./clip-path";
import { Context } from "./context";

export enum ShapeType {
  Ellipse = "ELLIPSE",
  Circle = "CIRCLE",
  Line = "LINE",
  Rect = "RECT",
  Path = "PATH",
  Group = "GROUP",
  Text = "TEXT",
}

export interface Bounds {
  left: number;
  right: number;
  top: number;
  bottom: number;
  centerX: number;
  centerY: number;
  base?: number;
}

export interface CanvasSize {
  left: number;
  top: number;
  width: number;
  height: number;
}
export type SvgAttributes = { [key: string]: string | number };

export abstract class Shape {
  id?: string;
  type: ShapeType;
  transform: Transform;
  fill: Paint;
  stroke: Paint;
  strokeWidth: number;
  opacity: number;
  tags: string[];
  clipPath?: ClipPath;

  constructor(type: ShapeType) {
    this.type = type;
    this.transform = new Transform();
    this.fill = Paint.unset();
    this.stroke = Paint.unset();
    this.strokeWidth = 1;
    this.opacity = 1;
    this.tags = [];
  }

  abstract clone(): Shape;

  applyTransform(m: Transform) {
    this.transform.combine(m);
  }

  clip(clipPath: Shape) {
    const id = `clip-${Math.floor(Math.random() * 1e8)}`;
    this.clipPath = new ClipPath(id, clipPath);
  }

  _cloneAttributes(other: Shape) {
    if (typeof other.fill === "string" || other.fill === undefined) {
      this.fill = other.fill;
    } else {
      this.fill = other.fill.clone();
    }

    if (typeof other.stroke === "string" || other.stroke === undefined) {
      this.stroke = other.stroke;
    } else {
      this.stroke = other.stroke.clone();
    }

    this.strokeWidth = other.strokeWidth;
    this.opacity = other.opacity;
    this.transform.matrix.set(other.transform.matrix);
    this.tags = other.tags;
  }

  _getAttributes(context?: Context): SvgAttributes {
    return {
      ...this._getFill(context),
      ...this._getStroke(context),
      ...this._getTransform(),
      ...this._getOpacity(),
      ...this._getTags(),
      ...this._getClipPath(),
    };
  }

  _getFill(context?: Context): SvgAttributes {
    if (!this.fill) {
      return { fill: "none" };
    }
    switch (this.fill.type) {
      case "unset":
        return {};
      case "none":
        return { fill: "none" };
      case "solid":
        const solid = this.fill as SolidPaint;
        const attrs: SvgAttributes = { fill: solid.toHex() };
        if (solid.a < 1) {
          attrs.fillOpacity = solid.a;
        }
        return attrs;
      case "linearGradient":
        const linearGradient = this.fill as LinearGradientPaint;
        context && context.addLinearGradient(linearGradient);
        return { fill: `url(#${linearGradient.id})` };
      case "radialGradient":
        const radialGradient = this.fill as RadialGradientPaint;
        context && context.addRadialGradient(radialGradient);
        return { fill: `url(#${radialGradient.id})` };
    }
  }

  _getStroke(context?: Context): SvgAttributes {
    if (!this.stroke) {
      return { stroke: "none" };
    }
    switch (this.stroke.type) {
      case "unset":
        return {};
      case "none":
        return { stroke: "none" };
      case "solid":
        const solid = this.stroke as SolidPaint;
        const attrs: SvgAttributes = { stroke: solid.toHex(false), strokeWidth: this.strokeWidth };
        if (solid.a < 1) {
          attrs.strokeOpacity = solid.a;
        }
        return attrs;
      case "linearGradient":
        const linearGradient = this.stroke as LinearGradientPaint;
        context && context.addLinearGradient(linearGradient);
        return { stroke: `url(#${linearGradient.id})`, strokeWidth: this.strokeWidth };
      case "radialGradient":
        const radialGradient = this.stroke as RadialGradientPaint;
        context && context.addRadialGradient(radialGradient);
        return { stroke: `url(#${radialGradient.id})`, strokeWidth: this.strokeWidth };
    }
  }

  _getTransform(): SvgAttributes {
    if (this.transform.isIdentity()) {
      return {};
    }
    return { transform: this.transform.toMatrixString() };
  }

  _getOpacity(): SvgAttributes {
    if (this.opacity >= 1) {
      return {};
    }
    return { opacity: this.opacity };
  }

  _getTags(): SvgAttributes {
    if (!Array.isArray(this.tags) || this.tags.length === 0) {
      return {};
    }
    return { className: this.tags.join(" ") };
  }

  _getClipPath(): SvgAttributes {
    if (!this.clipPath) {
      return {};
    }
    return { clipPath: `url(#${this.clipPath.id})` };
  }

  _updateAttributes(updates: { [key: string]: any }) {
    Object.entries(updates).forEach(([key, value]) => {
      if (value !== undefined) {
        (this as any)[key] = value;
      }
    });
  }

  getBounds(): Bounds {
    return {
      left: 0,
      right: 0,
      top: 0,
      bottom: 0,
      centerX: 0,
      centerY: 0,
    };
  }

  // Applies the Transform object and sets the transform to the identity matrix.
  // Optionally takes a parent transform matrix to apply to the current transform.
  abstract bakeTransform(parentMatrix?: Float32Array): void;

  abstract toPathData(applyTransform?: boolean, parentMatrix?: Float32Array): string;

  matchesTags(tagsIn: string[]): boolean {
    if (tagsIn.length === 0 || (tagsIn.length === 1 && tagsIn[0] === "")) {
      return true;
    }
    const posTagMatch = tagsIn.filter((tag) => !tag.startsWith("!"));
    const negTagMatch = tagsIn.filter((tag) => tag.startsWith("!")).map((tag) => tag.replace("!", ""));

    if (posTagMatch.length > 0 && !posTagMatch.some((tag) => this.tags.includes(tag))) {
      return false;
    }

    if (negTagMatch.some((tag) => this.tags.includes(tag))) {
      return false;
    }

    return false;
  }
}
