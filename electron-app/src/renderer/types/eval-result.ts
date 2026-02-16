// Evaluation result types from the WASM engine.

import type { Contour, Color, Point } from './geometry';

export interface PathRenderData {
  contours: Contour[];
  fill: Color | null;
  stroke: Color | null;
  strokeWidth: number;
  /** When false, handles/points are not drawn (e.g. font glyph outlines). */
  editable?: boolean;
}

export interface TextRenderData {
  text: string;
  position: Point;
  fontFamily: string;
  fontSize: number;
  align: 'left' | 'center' | 'right';
  fill: Color | null;
}

export interface OutputInfo {
  type: string;
  isMultiple: boolean;
  values: string[];
}

export interface NodeError {
  nodeName: string;
  message: string;
}

export interface EvalResult {
  paths: PathRenderData[];
  texts: TextRenderData[];
  output: OutputInfo;
  errors: NodeError[];
}
