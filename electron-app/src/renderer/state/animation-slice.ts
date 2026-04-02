export interface AnimationSlice {
  frame: number;
  frameStart: number;
  frameEnd: number;
  isPlaying: boolean;

  setFrame: (frame: number) => void;
  play: () => void;
  stop: () => void;
  setRange: (start: number, end: number) => void;
}

export const createAnimationSlice = (
  set: (fn: (state: AnimationSlice) => void) => void,
): AnimationSlice => ({
  frame: 1,
  frameStart: 1,
  frameEnd: 100,
  isPlaying: false,

  setFrame: (frame) =>
    set((state) => {
      state.frame = frame;
    }),

  play: () =>
    set((state) => {
      state.isPlaying = true;
    }),

  stop: () =>
    set((state) => {
      state.isPlaying = false;
    }),

  setRange: (start, end) =>
    set((state) => {
      state.frameStart = start;
      state.frameEnd = end;
    }),
});
