// Evaluation result types from the WASM engine.

import type { Contour, Color, Point } from './geometry';

export interface PathRenderData {
  contours: Contour[];
  fill: Color | null;
  stroke: Color | null;
  stroke_width: number;
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
