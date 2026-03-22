import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';
import type { LibrarySlice } from './library-slice.js';
import type { SelectionSlice } from './selection-slice.js';
import type { HistorySlice } from './history-slice.js';
import type { UISlice } from './ui-slice.js';
import type { AnimationSlice } from './animation-slice.js';
import type { RenderSlice } from './render-slice.js';
import { createLibrarySlice } from './library-slice.js';
import { createSelectionSlice } from './selection-slice.js';
import { createHistorySlice } from './history-slice.js';
import { createUISlice } from './ui-slice.js';
import { createAnimationSlice } from './animation-slice.js';
import { createRenderSlice } from './render-slice.js';

export type AppState = LibrarySlice & SelectionSlice & HistorySlice
  & UISlice & AnimationSlice & RenderSlice;

export const useStore = create<AppState>()(
  immer((set, get, api) => ({
    ...createLibrarySlice(set, get),
    ...createSelectionSlice(set),
    ...createHistorySlice(set, get),
    ...createUISlice(set),
    ...createAnimationSlice(set),
    ...createRenderSlice(set),
  })),
);
