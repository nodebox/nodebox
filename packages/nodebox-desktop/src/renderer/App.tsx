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

  // Re-evaluate whenever library or frame changes
  useEffect(() => {
    if (!library) return;
    let cancelled = false;
    setEvaluating(true);

    const platform = new TestPlatform();
    evaluate({ library, frame: currentFrame, platform }).then((result) => {
      if (!cancelled) {
        setRenderResult(result.paths, result.texts, result.output, result.errors);
      }
    }).catch((err) => {
      if (!cancelled) {
        setRenderResult([], [], [], [{ nodePath: '', message: String(err) }]);
      }
    });

    return () => { cancelled = true; };
  }, [library, currentFrame, setRenderResult, setEvaluating]);

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
