// TypeScript types mirroring crates/nodebox-core/src/geometry/

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

export type PointType = 'moveTo' | 'lineTo' | 'curveTo' | 'curveData';

export interface PathPoint {
  x: number;
  y: number;
  pointType: PointType;
}

export interface Contour {
  points: PathPoint[];
  closed: boolean;
}

export interface Path {
  contours: Contour[];
  fill: Color | null;
  stroke: Color | null;
  strokeWidth: number;
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
