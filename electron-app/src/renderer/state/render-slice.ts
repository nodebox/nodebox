import type { EvalResult } from '../types/eval-result';

export interface RenderSlice {
  renderResult: EvalResult | null;
  isRendering: boolean;
  renderError: string | null;

  setRenderResult: (result: EvalResult) => void;
  setRendering: (rendering: boolean) => void;
  setRenderError: (error: string | null) => void;
}

export const createRenderSlice = (
  set: (fn: (state: RenderSlice) => void) => void,
): RenderSlice => ({
  renderResult: null,
  isRendering: false,
  renderError: null,

  setRenderResult: (result) =>
    set((state) => {
      state.renderResult = result;
      state.isRendering = false;
      state.renderError = null;
    }),

  setRendering: (rendering) =>
    set((state) => {
      state.isRendering = rendering;
    }),

  setRenderError: (error) =>
    set((state) => {
      state.renderError = error;
      state.isRendering = false;
    }),
});
