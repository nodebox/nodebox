import { Shape, ShapeType } from "./shape";
import { Paint } from "./paint";
import { Transform } from "./transform";

export class Ellipse extends Shape {
  cx: number;
  cy: number;
  rx: number;
  ry: number;

  constructor(cx: number, cy: number, rx: number, ry: number) {
    super(ShapeType.Ellipse);
    this.cx = cx;
    this.cy = cy;
    this.rx = rx;
    this.ry = ry;
    this.fill = Paint.black();
  }

  clone(): Shape {
    const ellipse = new Ellipse(this.cx, this.cy, this.rx, this.ry);
    ellipse._cloneAttributes(this);
    return ellipse;
  }

  getBounds() {
    const strokeWidth = this.strokeWidth || 0;
    // Calculate the key points on the ellipse boundary
    const points = [
      { x: this.cx + this.rx + strokeWidth / 2, y: this.cy }, // Right-most point
      { x: this.cx - this.rx - strokeWidth / 2, y: this.cy }, // Left-most point
      { x: this.cx, y: this.cy + this.ry + strokeWidth / 2 }, // Bottom-most point
      { x: this.cx, y: this.cy - this.ry - strokeWidth / 2 }, // Top-most point
    ];

    // Apply the transformation matrix to each of these points
    const transformedPoints = points.map((point) =>
      Transform.applyMatrixTransform(point.x, point.y, this.transform.matrix),
    );

    // Find the min and max of the transformed points for the bounding box
    const left = Math.min(...transformedPoints.map((p) => p.x));
    const right = Math.max(...transformedPoints.map((p) => p.x));
    const top = Math.min(...transformedPoints.map((p) => p.y));
    const bottom = Math.max(...transformedPoints.map((p) => p.y));

    // Calculate the center based on the new bounding box
    const centerX = (left + right) / 2;
    const centerY = (top + bottom) / 2;

    return { left, right, top, bottom, centerX, centerY };
  }

  _transformed(
    applyTransform?: boolean,
    parentMatrix?: Float32Array,
  ): { cx: number; cy: number; rx: number; ry: number } {
    if (!applyTransform) {
      return { cx: this.cx, cy: this.cy, rx: this.rx, ry: this.ry };
    }
    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    if (Transform.matrixIsIdentity(matrix)) {
      return { cx: this.cx, cy: this.cy, rx: this.rx, ry: this.ry };
    }
    const { x, y } = Transform.applyMatrixTransform(this.cx, this.cy, matrix);
    const scaleX = Math.hypot(matrix[0], matrix[1]);
    const scaleY = Math.hypot(matrix[2], matrix[3]);
    return { cx: x, cy: y, rx: this.rx * scaleX, ry: this.ry * scaleY };
  }

  bakeTransform(parentMatrix?: Float32Array): void {
    const { cx, cy, rx, ry } = this._transformed(true, parentMatrix);
    this.cx = cx;
    this.cy = cy;
    this.rx = rx;
    this.ry = ry;
    this.transform = new Transform();
  }

  toPathData(applyTransform?: boolean, parentMatrix?: Float32Array): string {
    const kappa = 0.5522848;
    const { cx, cy, rx, ry } = this._transformed(applyTransform, parentMatrix);
    const x0 = cx - rx;
    const y0 = cy;
    const x1 = cx;
    const y1 = cy - ry;
    const x2 = cx + rx;
    const y2 = cy;
    const x3 = cx;
    const y3 = cy + ry;
    return `M ${x0} ${y0} C ${x0} ${y0 - ry * kappa} ${x1 - rx * kappa} ${y1} ${x1} ${y1} C ${x1 + rx * kappa} ${y1} ${x2} ${y2 - ry * kappa} ${x2} ${y2} C ${x2} ${y2 + ry * kappa} ${x3 + rx * kappa} ${y3} ${x3} ${y3} C ${x3 - rx * kappa} ${y3} ${x0} ${y0 + ry * kappa} ${x0} ${y0}`;
  }
}
