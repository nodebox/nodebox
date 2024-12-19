export class Point {
  x: number;
  y: number;

  constructor(x: number, y: number) {
    this.x = x;
    this.y = y;
  }

  clone(): Point {
    return new Point(this.x, this.y);
  }

  getBounds() {
    return {
      left: this.x,
      right: this.x,
      top: this.y,
      bottom: this.y,
      centerX: this.x,
      centerY: this.y,
    };
  }
}
