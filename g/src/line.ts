import { Shape, ShapeType } from "./shape";
import { Paint } from "./paint";
import { Transform } from "./transform";

export class Line extends Shape {
  x1: number;
  y1: number;
  x2: number;
  y2: number;

  constructor(x1: number, y1: number, x2: number, y2: number) {
    super(ShapeType.Line);
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    this.stroke = Paint.black();
  }

  clone(): Shape {
    const clonedLine = new Line(this.x1, this.y1, this.x2, this.y2);
    clonedLine._cloneAttributes(this);
    return clonedLine;
  }

  getBounds() {
    const strokeWidth = this.strokeWidth || 0;

    // Apply transformation to the line endpoints
    const start = Transform.applyMatrixTransform(this.x1, this.y1, this.transform.matrix);
    const end = Transform.applyMatrixTransform(this.x2, this.y2, this.transform.matrix);
    // Adjust bounds to account for stroke width
    const left = Math.min(start.x, end.x) - strokeWidth / 2;
    const right = Math.max(start.x, end.x) + strokeWidth / 2;
    const top = Math.min(start.y, end.y) - strokeWidth / 2;
    const bottom = Math.max(start.y, end.y) + strokeWidth / 2;

    // Calculate the center point
    const centerX = (left + right) / 2;
    const centerY = (top + bottom) / 2;

    return { left, right, top, bottom, centerX, centerY };
  }

  _transformed(
    applyTransform?: boolean,
    parentMatrix?: Float32Array,
  ): { start: { x: number; y: number }; end: { x: number; y: number } } {
    if (!applyTransform) {
      return { start: { x: this.x1, y: this.y1 }, end: { x: this.x2, y: this.y2 } };
    }
    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    const start = Transform.applyMatrixTransform(this.x1, this.y1, matrix);
    const end = Transform.applyMatrixTransform(this.x2, this.y2, matrix);
    return { start, end };
  }

  bakeTransform(parentMatrix?: Float32Array): void {
    const { start, end } = this._transformed(true, parentMatrix);
    this.x1 = start.x;
    this.y1 = start.y;
    this.x2 = end.x;
    this.y2 = end.y;
    this.transform = new Transform();
  }

  toPathData(applyTransform?: boolean, parentMatrix?: Float32Array): string {
    const { start, end } = this._transformed(applyTransform, parentMatrix);
    return `M ${start.x} ${start.y} L ${end.x} ${end.y}`;
  }
}
