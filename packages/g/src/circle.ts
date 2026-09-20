import { Shape, ShapeType } from "./shape";
import { Paint } from "./paint";
import { Transform } from "./transform";

export class Circle extends Shape {
  cx: number;
  cy: number;
  radius: number;

  constructor(cx: number, cy: number, radius: number) {
    super(ShapeType.Circle);
    this.cx = cx;
    this.cy = cy;
    this.radius = radius;
    this.fill = Paint.black();
  }

  clone(): Shape {
    const circle = new Circle(this.cx, this.cy, this.radius);
    circle._cloneAttributes(this);
    return circle;
  }

  getBounds() {
    const strokeWidth = this.strokeWidth || 0;
    // Calculate the key points on the ellipse boundary
    const points = [
      { x: this.cx + this.radius + strokeWidth / 2, y: this.cy }, // Right-most point
      { x: this.cx - this.radius - strokeWidth / 2, y: this.cy }, // Left-most point
      { x: this.cx, y: this.cy + this.radius + strokeWidth / 2 }, // Bottom-most point
      { x: this.cx, y: this.cy - this.radius - strokeWidth / 2 }, // Top-most point
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

  _transformed(applyTransform?: boolean, parentMatrix?: Float32Array): { cx: number; cy: number; radius: number } {
    if (!applyTransform) {
      return { cx: this.cx, cy: this.cy, radius: this.radius };
    }
    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    if (Transform.matrixIsIdentity(matrix)) {
      return { cx: this.cx, cy: this.cy, radius: this.radius };
    }
    const { x, y } = Transform.applyMatrixTransform(this.cx, this.cy, matrix);
    const scaleX = Math.hypot(matrix[0], matrix[1]);
    const scaleY = Math.hypot(matrix[2], matrix[3]);
    const scale = (scaleX + scaleY) / 2;
    return { cx: x, cy: y, radius: this.radius * scale };
  }

  bakeTransform(parentMatrix?: Float32Array): void {
    const { cx, cy, radius } = this._transformed(true, parentMatrix);
    this.cx = cx;
    this.cy = cy;
    this.radius = radius;
    this.transform = new Transform();
  }

  toPathData(applyTransform?: boolean, parentMatrix?: Float32Array): string {
    const { cx, cy, radius } = this._transformed(applyTransform, parentMatrix);
    return `M ${cx - radius}, ${cy} a ${radius}, ${radius} 0 1, 0 ${radius * 2}, 0 a ${radius}, ${radius} 0 1, 0 ${-radius * 2}, 0`;
  }
}
