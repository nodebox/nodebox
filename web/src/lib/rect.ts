import { Color } from "@ndbx/runtime";

export default class Rect {
  x: number;
  y: number;
  width: number;
  height: number;
  backgroundColor: Color;
  constructor(x: number, y: number, width: number, height: number, backgroundColor?: Color) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.backgroundColor = backgroundColor || { r: 1, g: 1, b: 1, a: 0 };
  }

  static fromPoints(point1: { x: number; y: number }, point2: { x: number; y: number }) {
    const x = Math.min(point1.x, point2.x);
    const y = Math.min(point1.y, point2.y);
    const width = Math.abs(point1.x - point2.x);
    const height = Math.abs(point1.y - point2.y);
    return new Rect(x, y, width, height);
  }

  containsPoint(x: number, y: number) {
    return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height;
  }

  intersects(other: Rect) {
    return (
      this.x < other.x + other.width &&
      this.x + this.width > other.x &&
      this.y < other.y + other.height &&
      this.y + this.height > other.y
    );
  }

  grow(dx: number, dy: number) {
    this.x -= dx;
    this.y -= dy;
    this.width += 2 * dx;
    this.height += 2 * dy;
  }

  updateWithPoint({ x, y }: { x: number; y: number }) {
    const minX = Math.min(this.x, x);
    const minY = Math.min(this.y, y);
    const maxX = Math.max(this.x, x);
    const maxY = Math.max(this.y, y);

    // Update the rectangle dimensions
    this.x = minX;
    this.y = minY;
    this.width = maxX - minX;
    this.height = maxY - minY;
  }
}
