// Evaluation result types from the WASM engine.

import type { Contour, Color } from './geometry';

export interface PathRenderData {
  contours: Contour[];
  fill: Color | null;
  stroke: Color | null;
  stroke_width: number;
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
  output: OutputInfo;
  errors: NodeError[];
}
