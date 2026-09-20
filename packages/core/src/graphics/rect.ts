import { Point } from "./point";

/** An axis-aligned rectangle (nodebox.graphics.Rect). Immutable. */
export class Rect {
  readonly x: number;
  readonly y: number;
  readonly width: number;
  readonly height: number;

  constructor(x = 0, y = 0, width = 0, height = 0) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  static centeredRect(cx: number, cy: number, width: number, height: number): Rect {
    return new Rect(cx - width / 2, cy - height / 2, width, height);
  }

  static corneredRect(x: number, y: number, width: number, height: number): Rect {
    return new Rect(x, y, width, height);
  }

  get position(): Point {
    return new Point(this.x, this.y);
  }

  get centroid(): Point {
    return new Point(this.x + this.width / 2, this.y + this.height / 2);
  }

  isEmpty(): boolean {
    const n = this.normalized();
    return n.width <= 0 || n.height <= 0;
  }

  normalized(): Rect {
    let { x, y, width, height } = this;
    if (width < 0) {
      x += width;
      width = -width;
    }
    if (height < 0) {
      y += height;
      height = -height;
    }
    return new Rect(x, y, width, height);
  }

  united(r: Rect): Rect {
    const r1 = this.normalized();
    const r2 = r.normalized();
    const x = Math.min(r1.x, r2.x);
    const y = Math.min(r1.y, r2.y);
    const width = Math.max(r1.x + r1.width, r2.x + r2.width) - x;
    const height = Math.max(r1.y + r1.height, r2.y + r2.height) - y;
    return new Rect(x, y, width, height);
  }

  intersects(r: Rect): boolean {
    const r1 = this.normalized();
    const r2 = r.normalized();
    return (
      Math.max(r1.x, r2.x) < Math.min(r1.x + r1.width, r2.x + r2.width) &&
      Math.max(r1.y, r2.y) < Math.min(r1.y + r1.height, r2.y + r2.height)
    );
  }

  contains(p: Point): boolean;
  contains(r: Rect): boolean;
  contains(other: Point | Rect): boolean {
    const r1 = this.normalized();
    if (other instanceof Point) {
      return other.x >= r1.x && other.x <= r1.x + r1.width && other.y >= r1.y && other.y <= r1.y + r1.height;
    }
    const r2 = other.normalized();
    return r2.x >= r1.x && r2.x + r2.width <= r1.x + r1.width && r2.y >= r1.y && r2.y + r2.height <= r1.y + r1.height;
  }

  equals(other: unknown): boolean {
    return (
      other instanceof Rect &&
      other.x === this.x &&
      other.y === this.y &&
      other.width === this.width &&
      other.height === this.height
    );
  }

  /** Destructure like the Python `x, y, w, h = shape.bounds`. */
  *[Symbol.iterator](): Iterator<number> {
    yield this.x;
    yield this.y;
    yield this.width;
    yield this.height;
  }

  toString(): string {
    return `Rect(${this.x}, ${this.y}, ${this.width}, ${this.height})`;
  }
}
