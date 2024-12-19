import { Point } from "./point";
import { Bounds } from "./shape";

export class Transform {
  matrix: Float32Array;
  bounds?: Bounds;
  "pivot-mode"?: string;
  "pivot-point"?: string;

  static matrixIsIdentity(matrix: Float32Array): boolean {
    return (
      matrix[0] === 1 && matrix[1] === 0 && matrix[2] === 0 && matrix[3] === 1 && matrix[4] === 0 && matrix[5] === 0
    );
  }

  static multiplyMatrices(m1: Float32Array, m2: Float32Array): Float32Array {
    const [a1, b1, c1, d1, e1, f1] = m1;
    const [a2, b2, c2, d2, e2, f2] = m2;

    return new Float32Array([
      a1 * a2 + c1 * b2,
      b1 * a2 + d1 * b2,
      a1 * c2 + c1 * d2,
      b1 * c2 + d1 * d2,
      a1 * e2 + c1 * f2 + e1,
      b1 * e2 + d1 * f2 + f1,
    ]);
  }

  constructor(matrix?: Float32Array) {
    this.matrix = matrix ? new Float32Array(matrix) : new Float32Array([1, 0, 0, 1, 0, 0]);
  }

  static fromSvgTransform(s: string): Transform {
    function makeMatrix(type: string, values: number[]): number[] {
      switch (type) {
        case "translate":
          return [1, 0, 0, 1, values[0], values[1] || 0];
        case "rotate":
          const angle = -(values[0] * Math.PI) / 180;
          return [Math.cos(angle), -Math.sin(angle), Math.sin(angle), Math.cos(angle), 0, 0];
        case "scale":
          return [values[0], 0, 0, values[1] || values[0], 0, 0];
        default:
          return [1, 0, 0, 1, 0, 0];
      }
    }

    function combine(m1: number[], m2: number[]): number[] {
      return [
        m1[0] * m2[0] + m1[2] * m2[1],
        m1[1] * m2[0] + m1[3] * m2[1],
        m1[0] * m2[2] + m1[2] * m2[3],
        m1[1] * m2[2] + m1[3] * m2[3],
        m1[0] * m2[4] + m1[2] * m2[5] + m1[4],
        m1[1] * m2[4] + m1[3] * m2[5] + m1[5],
      ];
    }

    // Initialize the transformation matrix as identity matrix
    let matrix = [1, 0, 0, 1, 0, 0]; // Initial identity matrix
    const transforms = s.match(/(\w+\((\-?\d+\.?\d*(e[\+\-]?\d+)?,?\s*)+\))/gi);
    if (transforms) {
      transforms.forEach((trans) => {
        const [type, values] = trans.match(/(\w+)\((.*)\)/)!.slice(1);
        const nums = values.split(/[\s,]+/).map(Number);
        matrix = combine(matrix, makeMatrix(type, nums));
      });
    }

    return new Transform(new Float32Array(matrix)); // Use the updated constructor
  }

  clone(): Transform {
    return new Transform(this.matrix); // Use the updated constructor
  }

  isIdentity(): boolean {
    return (
      this.matrix[0] === 1 &&
      this.matrix[1] === 0 &&
      this.matrix[2] === 0 &&
      this.matrix[3] === 1 &&
      this.matrix[4] === 0 &&
      this.matrix[5] === 0
    );
  }

  translate(tx: number, ty: number) {
    this.matrix[4] += tx;
    this.matrix[5] += ty;
    return this;
  }

  rotate(angle: number, around: Point = new Point(0, 0)) {
    const cos = Math.cos(angle);
    const sin = Math.sin(angle);
    const [a, b, c, d, e, f] = this.matrix; // Assuming the matrix is 3x2 for translation purposes

    // Translate point to origin (subtract around.x and around.y)
    const tx = around.x;
    const ty = around.y;

    // Apply rotation
    this.matrix[0] = a * cos - b * sin;
    this.matrix[1] = a * sin + b * cos;
    this.matrix[2] = c * cos - d * sin;
    this.matrix[3] = c * sin + d * cos;

    // Apply translation back to the original point
    this.matrix[4] += tx - (tx * cos - ty * sin);
    this.matrix[5] += ty - (tx * sin + ty * cos);

    return this;
  }

  rotateDegrees(angle: number, around: Point = new Point(0, 0)) {
    angle = (angle * Math.PI) / 180;
    return this.rotate(angle, around);
  }

  scale(sx: number, sy: number, around: Point = new Point(0, 0)) {
    const tx = around.x;
    const ty = around.y;

    this.translate(-tx, -ty);

    this.matrix[0] *= sx;
    this.matrix[1] *= sy;
    this.matrix[2] *= sx;
    this.matrix[3] *= sy;
    this.matrix[4] *= sx;
    this.matrix[5] *= sy;

    this.translate(tx, ty);

    return this;
  }

  combine(other: Transform) {
    const a = this.matrix;
    const b = other.matrix;
    const result = [
      a[0] * b[0] + a[2] * b[1],
      a[1] * b[0] + a[3] * b[1],
      a[0] * b[2] + a[2] * b[3],
      a[1] * b[2] + a[3] * b[3],
      a[0] * b[4] + a[2] * b[5] + a[4],
      a[1] * b[4] + a[3] * b[5] + a[5],
    ];
    this.matrix.set(result);

    this["pivot-mode"] = other["pivot-mode"];
    this["pivot-point"] = other["pivot-point"];
    return this;
  }

  multiply(other: Float32Array): Float32Array {
    const [a1, b1, c1, d1, e1, f1] = this.matrix;
    const [a2, b2, c2, d2, e2, f2] = other;

    return new Float32Array([
      a1 * a2 + c1 * b2,
      b1 * a2 + d1 * b2,
      a1 * c2 + c1 * d2,
      b1 * c2 + d1 * d2,
      a1 * e2 + c1 * f2 + e1,
      b1 * e2 + d1 * f2 + f1,
    ]);
  }

  transform(tx: number, ty: number, sx: number, sy: number, angle: number, around: Point = new Point(0, 0)) {
    return this.translate(tx, ty).scale(sx, sy, around).rotate(angle, around);
  }

  transformPoint(point: Point): Point {
    const { x, y } = point;
    const { matrix } = this;
    return new Point(matrix[0] * x + matrix[2] * y + matrix[4], matrix[1] * x + matrix[3] * y + matrix[5]);
  }

  toMatrixString(): string {
    const m = this.matrix;
    return `matrix(${m[0]},${m[1]},${m[2]},${m[3]},${m[4]},${m[5]})`;
  }

  static applyMatrixTransform(x: number, y: number, matrix: Float32Array): { x: number; y: number } {
    const [a, b, c, d, e, f] = matrix;

    return {
      x: a * x + c * y + e,
      y: b * x + d * y + f,
    };
  }
}
