import type { Path, Text, Value, EvalError } from 'nodebox-core';

export interface RenderSlice {
  paths: Path[];
  texts: Text[];
  output: Value[];
  evalErrors: EvalError[];
  evaluating: boolean;
  setRenderResult: (paths: Path[], texts: Text[], output: Value[], errors: EvalError[]) => void;
  setEvaluating: (evaluating: boolean) => void;
}

export function createRenderSlice(set: any): RenderSlice {
  return {
    paths: [],
    texts: [],
    output: [],
    evalErrors: [],
    evaluating: false,

    setRenderResult: (paths, texts, output, errors) => set((state: RenderSlice) => {
      state.paths = paths;
      state.texts = texts;
      state.output = output;
      state.evalErrors = errors;
      state.evaluating = false;
    }),

    setEvaluating: (evaluating) => set((state: RenderSlice) => {
      state.evaluating = evaluating;
    }),
  };
}
