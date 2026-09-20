import { Shape, ShapeType } from "./shape";
import { Paint } from "./paint";
import { Transform } from "./transform";

export class Rect extends Shape {
  x: number;
  y: number;
  width: number;
  height: number;

  constructor(x: number, y: number, width: number, height: number) {
    super(ShapeType.Rect);
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.fill = Paint.black();
  }

  clone(): Rect {
    const rect = new Rect(this.x, this.y, this.width, this.height);
    rect._cloneAttributes(this);
    return rect;
  }

  getBounds() {
    const strokeWidth = this.strokeWidth || 0;

    const left_ = this.x - strokeWidth / 2;
    const right_ = this.x + this.width + strokeWidth / 2;
    const top_ = this.y - strokeWidth / 2;
    const bottom_ = this.y + this.height + strokeWidth / 2;

    const topLeft = Transform.applyMatrixTransform(left_, top_, this.transform.matrix);
    const topRight = Transform.applyMatrixTransform(right_, top_, this.transform.matrix);
    const bottomLeft = Transform.applyMatrixTransform(left_, bottom_, this.transform.matrix);
    const bottomRight = Transform.applyMatrixTransform(right_, bottom_, this.transform.matrix);

    // Find the min and max of the transformed corners for bounding box
    const left = Math.min(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
    const right = Math.max(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
    const top = Math.min(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);
    const bottom = Math.max(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);

    // Center points can be calculated based on the new bounding box
    const centerX = (left + right) / 2;
    const centerY = (top + bottom) / 2;

    return { left, right, top, bottom, centerX, centerY };
  }

  _transformed(
    applyTransform?: boolean,
    parentMatrix?: Float32Array,
  ): { x: number; y: number; width: number; height: number } {
    if (!applyTransform) {
      return { x: this.x, y: this.y, width: this.width, height: this.height };
    }
    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    const topLeft = Transform.applyMatrixTransform(this.x, this.y, matrix);
    const topRight = Transform.applyMatrixTransform(this.x + this.width, this.y, matrix);
    const bottomLeft = Transform.applyMatrixTransform(this.x, this.y + this.height, matrix);
    const bottomRight = Transform.applyMatrixTransform(this.x + this.width, this.y + this.height, matrix);

    const left = Math.min(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
    const right = Math.max(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
    const top = Math.min(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);
    const bottom = Math.max(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);
    return { x: left, y: top, width: right - left, height: bottom - top };
  }

  bakeTransform(parentMatrix?: Float32Array): void {
    const { x, y, width, height } = this._transformed(true, parentMatrix);
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.transform = new Transform();
  }

  toPathData(applyTransform?: boolean, parentMatrix?: Float32Array): string {
    const { x, y, width, height } = this._transformed(applyTransform, parentMatrix);
    return `M ${x}, ${y} h ${width} v ${height} h ${-width} Z`;
  }
}
