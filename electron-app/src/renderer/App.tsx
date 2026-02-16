import { useEffect } from 'react';
import { useStore } from './state/store';
import { useKeyboardShortcuts } from './hooks/useKeyboardShortcuts';
import { AppLayout } from './components/AppLayout';
import { evaluate } from './eval/evaluator';
import { onWasmReady } from './eval/wasm';
import type { MenuAction } from '../shared/ipc-channels';

export function App() {
  useKeyboardShortcuts();

  const library = useStore((s) => s.library);
  const frame = useStore((s) => s.frame);
  const isPlaying = useStore((s) => s.isPlaying);
  const setFrame = useStore((s) => s.setFrame);
  const setRenderResult = useStore((s) => s.setRenderResult);
  const toggleHandles = useStore((s) => s.toggleHandles);
  const togglePoints = useStore((s) => s.togglePoints);
  const toggleOrigin = useStore((s) => s.toggleOrigin);
  const toggleCanvasBorder = useStore((s) => s.toggleCanvasBorder);
  const setAboutDialogVisible = useStore((s) => s.setAboutDialogVisible);

  // Run the evaluator whenever the library or frame changes
  useEffect(() => {
    const result = evaluate(library, frame);
    setRenderResult(result);
  }, [library, frame, setRenderResult]);

  // Re-evaluate once WASM is initialized (for textpath nodes)
  useEffect(() => {
    onWasmReady(() => {
      const s = useStore.getState();
      const result = evaluate(s.library, s.frame);
      s.setRenderResult(result);
    });
  }, []);

  // Animation playback loop
  useEffect(() => {
    if (!isPlaying) return;
    let lastTime = performance.now();
    let rafId: number;
    const fps = 30;
    const frameDuration = 1000 / fps;

    const tick = (now: number) => {
      const elapsed = now - lastTime;
      if (elapsed >= frameDuration) {
        lastTime = now - (elapsed % frameDuration);
        const state = useStore.getState();
        const nextFrame = state.frame >= state.frameEnd ? state.frameStart : state.frame + 1;
        setFrame(nextFrame);
      }
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, [isPlaying, setFrame]);

  useEffect(() => {
    if (!window.electronAPI) return;

    window.electronAPI.onMenuAction((action: string) => {
      const menuAction = action as MenuAction;
      switch (menuAction) {
        case 'view:toggle-handles':
          toggleHandles();
          break;
        case 'view:toggle-points':
          togglePoints();
          break;
        case 'view:toggle-origin':
          toggleOrigin();
          break;
        case 'view:toggle-canvas-border':
          toggleCanvasBorder();
          break;
        case 'help:about':
          setAboutDialogVisible(true);
          break;
      }
    });
  }, [
    toggleHandles,
    togglePoints,
    toggleOrigin,
    toggleCanvasBorder,
    setAboutDialogVisible,
  ]);

  return <AppLayout />;
}
