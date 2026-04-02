import { describe, expect, it } from 'vitest';
import { createUISlice, type UISlice } from '../../src/renderer/state/ui-slice';

function createState(): UISlice {
  let state = {} as UISlice;
  const set = (fn: (draft: UISlice) => void) => {
    fn(state);
  };
  state = createUISlice(set);
  return state;
}

describe('ui-slice viewer mode sync', () => {
  it('keeps the initial visual state for visual output', () => {
    const state = createState();

    state.syncViewerModeForRender(true, true);

    expect(state.viewerMode).toBe('visual');
    expect(state.preferredGeometryViewerMode).toBe('visual');
    expect(state.visualViewerAvailable).toBe(true);
  });

  it('stores a manual data preference while visual output is available', () => {
    const state = createState();

    state.setViewerMode('data');

    expect(state.viewerMode).toBe('data');
    expect(state.preferredGeometryViewerMode).toBe('data');
  });

  it('forces data mode when switching to non-visual output', () => {
    const state = createState();

    state.syncViewerModeForRender(true, false);

    expect(state.viewerMode).toBe('data');
    expect(state.preferredGeometryViewerMode).toBe('visual');
    expect(state.visualViewerAvailable).toBe(false);
  });

  it('keeps data mode forced while staying on non-visual output', () => {
    const state = createState();

    state.syncViewerModeForRender(true, false);
    state.setViewerMode('data');
    state.syncViewerModeForRender(true, false);

    expect(state.viewerMode).toBe('data');
    expect(state.preferredGeometryViewerMode).toBe('visual');
    expect(state.visualViewerAvailable).toBe(false);
  });

  it('restores the saved visual preference when visual output returns', () => {
    const state = createState();

    state.syncViewerModeForRender(true, false);
    state.syncViewerModeForRender(true, true);

    expect(state.viewerMode).toBe('visual');
    expect(state.visualViewerAvailable).toBe(true);
  });

  it('restores a saved data preference when visual output returns', () => {
    const state = createState();

    state.setViewerMode('data');
    state.syncViewerModeForRender(true, false);
    state.syncViewerModeForRender(true, true);

    expect(state.viewerMode).toBe('data');
    expect(state.preferredGeometryViewerMode).toBe('data');
    expect(state.visualViewerAvailable).toBe(true);
  });

  it('keeps the current mode and re-enables visual when nothing is rendered', () => {
    const state = createState();

    state.setViewerMode('data');
    state.syncViewerModeForRender(true, false);
    state.syncViewerModeForRender(false, false);

    expect(state.viewerMode).toBe('data');
    expect(state.preferredGeometryViewerMode).toBe('data');
    expect(state.visualViewerAvailable).toBe(true);
  });
});
