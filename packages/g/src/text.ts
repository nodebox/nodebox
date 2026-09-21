import { Shape, ShapeType, Bounds } from "./shape";
import { Paint } from "./paint";
import { Transform } from "./transform";

let _measureCtx: CanvasRenderingContext2D | null = null;
if (typeof document !== "undefined") {
  const _measureCanvas = document.createElement("canvas");
  _measureCtx = _measureCanvas.getContext("2d");
}

interface TextMetrics {
  width: number;
  height: number;
  descent: number;
}

export class Text extends Shape {
  x: number;
  y: number;
  text: string;
  fontSize: string;
  fontFamily: string;
  fontWeight: "normal" | "bold" | "bolder" | "lighter" | number;
  textAnchor: "start" | "end" | "middle";
  fill: Paint;

  private _cachedFont: string | null = null;
  private _cachedText: string | null = null;
  private _cachedMetrics: TextMetrics | null = null;

  constructor(
    text: string,
    x: number,
    y: number,
    fontSize: string,
    fontFamily: string,
    fontWeight: "normal" | "bold" | "bolder" | "lighter" | number = "normal",
    textAnchor: "start" | "end" | "middle" = "start",
    fill: Paint = Paint.black(),
  ) {
    super(ShapeType.Text);
    this.text = text;
    this.x = x;
    this.y = y;
    this.fontSize = fontSize;
    this.fontFamily = fontFamily;
    this.fontWeight = fontWeight || "normal";
    this.textAnchor = textAnchor || "start";
    this.fill = fill;
  }

  clone(): Shape {
    const text = new Text(
      this.text,
      this.x,
      this.y,
      this.fontSize,
      this.fontFamily,
      this.fontWeight,
      this.textAnchor,
      this.fill,
    );
    text._cloneAttributes(this);
    return text;
  }

  getBounds(): Bounds {
    return this.getTransformedBounds();
    //   const metrics = this.measureTextDimensions(this.text, this.fontSize, this.fontFamily);
    //   let left = this.x;
    //   let right = this.x;
    //   let top = this.y - metrics.height / (metrics.descent || 1);
    //   let bottom = this.y;
    //   let base = top + metrics.height;

    //   switch (this.textAnchor) {
    //     case "start":
    //       right = left + metrics.width;
    //       break;
    //     case "end":
    //       left = this.x - metrics.width;
    //       right = this.x;
    //       break;
    //     case "middle":
    //       left -= metrics.width / 2;
    //       right += metrics.width / 2;
    //       break;
    //   }

    //   const centerX = (left + right) / 2;
    //   const centerY = (top + bottom) / 2;

    //   return { left, right, top, bottom: base, centerX, centerY, base };
  }

  measureTextDimensions(text: string, fontSize: string, fontFamily: string) {
    if (!_measureCtx) {
      return { width: 0, height: 0, descent: 0 };
    }

    const font = `${fontSize}px ${fontFamily}`;
    if (this._cachedFont === font && this._cachedText === text && this._cachedMetrics) {
      return this._cachedMetrics;
    }

    if (this._cachedFont !== font) {
      _measureCtx.font = font;
      this._cachedFont = font;
    }

    const metrics = _measureCtx.measureText(text);
    this._cachedText = text;
    this._cachedMetrics = {
      width: metrics.width,
      height: parseFloat(fontSize) * 1.12,
      descent: (metrics.fontBoundingBoxAscent + metrics.fontBoundingBoxDescent) / metrics.fontBoundingBoxAscent,
    };

    return this._cachedMetrics;
  }

  getTransformedBounds(useTransform: boolean = true): Bounds {
    const metrics = this.measureTextDimensions(this.text, this.fontSize, this.fontFamily);
    // Adjust initial bounds based on textAnchor
    let left = this.x;
    let right = this.x;
    let top = this.y - metrics.height / (metrics.descent || 1); // Estimate ascent based on height and descent ratio
    let bottom = this.y;
    let base = top + metrics.height;

    switch (this.textAnchor) {
      case "start":
        right = left + metrics.width;
        break;
      case "end":
        left = this.x - metrics.width;
        right = this.x;
        break;
      case "middle":
        left -= metrics.width / 2;
        right += metrics.width / 2;
        break;
    }

    // Now transform all four corners of the bounding box
    let matrix;
    if (useTransform) {
      matrix = this.transform.matrix;
    } else {
      matrix = new Transform().matrix;
    }
    // Apply matrix transformation to each corner
    const topLeft = Transform.applyMatrixTransform(left, top, matrix);
    const topRight = Transform.applyMatrixTransform(right, top, matrix);
    const bottomLeft = Transform.applyMatrixTransform(left, base, matrix);
    const bottomRight = Transform.applyMatrixTransform(right, base, matrix);

    // Find the min/max for the transformed bounding box
    const transformedLeft = Math.min(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
    const transformedRight = Math.max(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
    const transformedTop = Math.min(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);
    const transformedBottom = Math.max(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);

    // Calculate the transformed center
    const transformedCenterX = (transformedLeft + transformedRight) / 2;
    const transformedCenterY = (transformedTop + transformedBottom) / 2;

    return {
      left: transformedLeft,
      right: transformedRight,
      top: transformedTop,
      bottom: transformedBottom,
      centerX: transformedCenterX,
      centerY: transformedCenterY,
    };
  }

  bakeTransform(parentMatrix?: Float32Array): void {
    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    const { x, y } = Transform.applyMatrixTransform(this.x, this.y, matrix);
    const scaleX = Math.hypot(matrix[0], matrix[1]);
    const scaleY = Math.hypot(matrix[2], matrix[3]);
    const scale = (scaleX + scaleY) / 2;

    this.x = x;
    this.y = y;

    const fontSizeMatch = this.fontSize.match(/^([\d.]+)([a-z%]*)$/);
    if (fontSizeMatch) {
      const fontSizeValue = parseFloat(fontSizeMatch[1]);
      const fontSizeUnit = fontSizeMatch[2] || "px";
      const newFontSizeValue = fontSizeValue * scale;
      this.fontSize = `${newFontSizeValue}${fontSizeUnit}`;
    }

    this.transform = new Transform();
  }

  toPathData(applyTransform?: boolean, parentMatrix?: Float32Array): string {
    return "";
  }
}
