import React from 'react';
import { useStore } from '../state/store';
import { ZINC_800, ZINC_900, TEXT_DEFAULT, TEXT_DISABLED, FONT_SIZE_SMALL, ANIMATION_BAR_HEIGHT, ZINC_500 } from '../theme/tokens';

const btnStyle: React.CSSProperties = {
  background: 'transparent',
  border: 'none',
  color: TEXT_DEFAULT,
  cursor: 'pointer',
  fontSize: 14,
  padding: '0 4px',
  fontFamily: 'inherit',
};

export function AnimationBar() {
  const currentFrame = useStore((s) => s.currentFrame);
  const startFrame = useStore((s) => s.startFrame);
  const endFrame = useStore((s) => s.endFrame);
  const fps = useStore((s) => s.fps);
  const playing = useStore((s) => s.playing);
  const setCurrentFrame = useStore((s) => s.setCurrentFrame);
  const play = useStore((s) => s.play);
  const pause = useStore((s) => s.pause);
  const stop = useStore((s) => s.stop);

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      height: ANIMATION_BAR_HEIGHT,
      padding: '0 8px',
      background: ZINC_800,
      borderTop: `1px solid ${ZINC_900}`,
      gap: 4,
      fontSize: FONT_SIZE_SMALL,
      color: TEXT_DEFAULT,
      flexShrink: 0,
    }}>
      <button onClick={() => setCurrentFrame(startFrame)} style={btnStyle} title="First frame">⏮</button>
      <button onClick={() => setCurrentFrame(Math.max(startFrame, currentFrame - 10))} style={btnStyle} title="Back 10">⏪</button>
      <button onClick={playing ? pause : play} style={btnStyle} title={playing ? 'Pause' : 'Play'}>
        {playing ? '⏸' : '▶'}
      </button>
      <button onClick={() => setCurrentFrame(Math.min(endFrame, currentFrame + 10))} style={btnStyle} title="Forward 10">⏩</button>
      <button onClick={() => setCurrentFrame(endFrame)} style={btnStyle} title="Last frame">⏭</button>
      <button onClick={stop} style={btnStyle} title="Stop">⏹</button>

      <div style={{ width: 1, height: 16, background: ZINC_500, margin: '0 4px' }} />

      <span style={{ color: TEXT_DISABLED }}>Frame</span>
      <input
        type="number"
        value={currentFrame}
        onChange={(e) => setCurrentFrame(parseInt(e.target.value, 10) || 1)}
        style={{ width: 40, background: 'transparent', border: 'none', color: TEXT_DEFAULT, textAlign: 'center', fontSize: FONT_SIZE_SMALL, fontFamily: 'inherit' }}
      />
      <span style={{ color: TEXT_DISABLED }}>/{endFrame}</span>

      <div style={{ width: 1, height: 16, background: ZINC_500, margin: '0 4px' }} />

      <span style={{ color: TEXT_DISABLED }}>FPS</span>
      <span>{fps}</span>

      <div style={{ flex: 1 }} />

      <label style={{ display: 'flex', alignItems: 'center', gap: 4, cursor: 'pointer' }}>
        <input type="checkbox" defaultChecked />
        <span style={{ fontSize: FONT_SIZE_SMALL }}>Loop</span>
      </label>
    </div>
  );
}
