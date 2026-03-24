export interface AnimationSlice {
  currentFrame: number;
  fps: number;
  playing: boolean;
  setCurrentFrame: (frame: number) => void;
  setFps: (fps: number) => void;
  play: () => void;
  pause: () => void;
  stop: () => void;
}

export function createAnimationSlice(set: any): AnimationSlice {
  return {
    currentFrame: 1,
    fps: 30,
    playing: false,

    setCurrentFrame: (frame) => set((state: AnimationSlice) => {
      state.currentFrame = frame;
    }),
    setFps: (fps) => set((state: AnimationSlice) => { state.fps = fps; }),
    play: () => set((state: AnimationSlice) => { state.playing = true; }),
    pause: () => set((state: AnimationSlice) => { state.playing = false; }),
    stop: () => set((state: AnimationSlice) => {
      state.playing = false;
      state.currentFrame = 1;
    }),
  };
}
