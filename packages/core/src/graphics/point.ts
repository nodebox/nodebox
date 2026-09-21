/**
 * A 2D point that also carries its role in a path (line-to, curve-to or curve control data),
 * exactly like nodebox.graphics.Point. Points are immutable.
 */
export class Point {
  static readonly LINE_TO = 1;
  static readonly CURVE_TO = 2;
  static readonly CURVE_DATA = 3;
  static readonly ZERO = new Point(0, 0);

  readonly x: number;
  readonly y: number;
  readonly type: number;

  constructor(x = 0, y = 0, type: number = Point.LINE_TO) {
    this.x = x;
    this.y = y;
    this.type = type;
  }

  /** Parse "12.3,45.6" (the .ndbx serialization). */
  static parse(s: string): Point {
    const args = s.split(",");
    if (args.length !== 2) throw new Error(`String '${s}' needs two components, i.e. 12.3,45.6`);
    // NodeBox 3 parses through Float, which rounds to single precision; we keep doubles.
    return new Point(parseFloat(args[0]), parseFloat(args[1]));
  }

  static valueOf(s: string): Point {
    return Point.parse(s);
  }

  static isPoint(value: unknown): value is Point {
    return value instanceof Point;
  }

  isLineTo(): boolean {
    return this.type === Point.LINE_TO;
  }

  isCurveTo(): boolean {
    return this.type === Point.CURVE_TO;
  }

  isCurveData(): boolean {
    return this.type === Point.CURVE_DATA;
  }

  isOnCurve(): boolean {
    return this.type !== Point.CURVE_DATA;
  }

  isOffCurve(): boolean {
    return this.type === Point.CURVE_DATA;
  }

  moved(dx: number, dy: number): Point {
    return new Point(this.x + dx, this.y + dy, this.type);
  }

  withType(type: number): Point {
    return new Point(this.x, this.y, type);
  }

  equals(other: unknown): boolean {
    return other instanceof Point && other.x === this.x && other.y === this.y && other.type === this.type;
  }

  /** The .ndbx serialization: two decimals, as NodeBox 3 writes it. */
  toString(): string {
    return `${this.x.toFixed(2)},${this.y.toFixed(2)}`;
  }

  toJSON(): { x: number; y: number } {
    return { x: this.x, y: this.y };
  }
}
