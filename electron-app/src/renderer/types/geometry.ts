// TypeScript types mirroring crates/nodebox-core/src/geometry/
// Field names match Rust serde serialization (snake_case).

export interface Point {
  x: number;
  y: number;
}

export interface Color {
  r: number;
  g: number;
  b: number;
  a: number;
}

export type PointType = 'LineTo' | 'CurveTo' | 'CurveData' | 'QuadTo' | 'QuadData';

export interface PathPoint {
  point: Point;
  point_type: PointType;
}

export interface Contour {
  points: PathPoint[];
  closed: boolean;
}

export interface Path {
  contours: Contour[];
  fill: Color | null;
  stroke: Color | null;
  stroke_width: number;
}

export type TextAlign = 'left' | 'center' | 'right';

export interface Text {
  text: string;
  position: Point;
  fontFamily: string;
  fontSize: number;
  align: TextAlign;
  fill: Color | null;
}

export interface Transform {
  m: [number, number, number, number, number, number];
}

export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}
