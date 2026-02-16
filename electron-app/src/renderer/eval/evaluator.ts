import type { NodeLibrary } from '../types/node';
import type { EvalResult } from '../types/eval-result';
import { isWasmReady, evaluateLibrary } from './wasm';

const EMPTY_RESULT: EvalResult = {
  paths: [],
  texts: [],
  output: { type: 'none', isMultiple: false, values: [] },
  errors: [],
};

export function evaluate(library: NodeLibrary, frame: number): EvalResult {
  if (!isWasmReady()) return EMPTY_RESULT;
  const json = evaluateLibrary(JSON.stringify(library), frame);
  return JSON.parse(json) as EvalResult;
}
