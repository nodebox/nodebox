import React, { useEffect } from 'react';
import { useStore } from './state/store';
import { useKeyboardShortcuts } from './hooks/useKeyboardShortcuts';
import { AppLayout } from './components/AppLayout';
import { evaluate } from 'nodebox-core';
import { TestPlatform } from 'nodebox-core';

export function App() {
  useKeyboardShortcuts();

  const library = useStore((s) => s.library);
  const currentFrame = useStore((s) => s.currentFrame);
  const playing = useStore((s) => s.playing);
  const setCurrentFrame = useStore((s) => s.setCurrentFrame);
  const startFrame = useStore((s) => s.startFrame);
  const endFrame = useStore((s) => s.endFrame);
  const setRenderResult = useStore((s) => s.setRenderResult);
  const setEvaluating = useStore((s) => s.setEvaluating);
  const setAutoDataMode = useStore((s) => s.setAutoDataMode);

  // Re-evaluate whenever library or frame changes
  useEffect(() => {
    if (!library) return;
    let cancelled = false;
    setEvaluating(true);

    const platform = new TestPlatform();
    evaluate({ library, frame: currentFrame, platform }).then((result) => {
      if (!cancelled) {
        setRenderResult(result.paths, result.texts, result.output, result.errors);
        // Auto-switch: only go to data if there IS non-visual output but no geometry.
        // Empty scene (no output at all) stays in visual.
        const hasGeometry = result.paths.length > 0;
        const hasDataOutput = result.output.length > 0;
        setAutoDataMode(hasGeometry || !hasDataOutput);
      }
    }).catch((err) => {
      if (!cancelled) {
        setRenderResult([], [], [], [{ nodePath: '', message: String(err) }]);
        setAutoDataMode(true); // error or empty → stay in visual
      }
    });

    return () => { cancelled = true; };
  }, [library, currentFrame, setRenderResult, setEvaluating, setAutoDataMode]);

  // Animation playback loop
  useEffect(() => {
    if (!playing) return;
    let lastTime = performance.now();
    let rafId: number;
    const fps = 30;
    const frameDuration = 1000 / fps;

    const tick = (now: number) => {
      const elapsed = now - lastTime;
      if (elapsed >= frameDuration) {
        lastTime = now - (elapsed % frameDuration);
        const state = useStore.getState();
        const nextFrame = state.currentFrame >= state.endFrame ? state.startFrame : state.currentFrame + 1;
        setCurrentFrame(nextFrame);
      }
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, [playing, setCurrentFrame]);

  return <AppLayout />;
}
