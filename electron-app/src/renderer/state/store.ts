import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';
import { createLibrarySlice, type LibrarySlice } from './library-slice';
import { createSelectionSlice, type SelectionSlice } from './selection-slice';
import { createHistorySlice, type HistorySlice } from './history-slice';
import { createUISlice, type UISlice } from './ui-slice';
import { createAnimationSlice, type AnimationSlice } from './animation-slice';
import { createRenderSlice, type RenderSlice } from './render-slice';

export type AppState = LibrarySlice &
  SelectionSlice &
  HistorySlice &
  UISlice &
  AnimationSlice &
  RenderSlice;

export const useStore = create<AppState>()(
  immer((set, get) => ({
    ...createLibrarySlice(set as never),
    ...createSelectionSlice(set as never),
    ...createHistorySlice(set as never, get as never),
    ...createUISlice(set as never),
    ...createAnimationSlice(set as never),
    ...createRenderSlice(set as never),
  })),
);
