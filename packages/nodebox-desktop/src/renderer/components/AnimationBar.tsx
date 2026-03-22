import React from 'react';
import { useStore } from '../state/store.js';

export function AnimationBar() {
  const currentFrame = useStore((s) => s.currentFrame);
  const startFrame = useStore((s) => s.startFrame);
  const endFrame = useStore((s) => s.endFrame);
  const playing = useStore((s) => s.playing);
  const setCurrentFrame = useStore((s) => s.setCurrentFrame);
  const play = useStore((s) => s.play);
  const pause = useStore((s) => s.pause);
  const stop = useStore((s) => s.stop);

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      height: 32,
      padding: '0 8px',
      background: '#2a2a2a',
      borderTop: '1px solid #333',
      gap: 8,
      fontSize: 12,
      color: '#ccc',
    }}>
      <button onClick={stop} style={buttonStyle}>Stop</button>
      <button onClick={playing ? pause : play} style={buttonStyle}>
        {playing ? 'Pause' : 'Play'}
      </button>
      <input
        type="range"
        min={startFrame}
        max={endFrame}
        value={currentFrame}
        onChange={(e) => setCurrentFrame(parseInt(e.target.value, 10))}
        style={{ flex: 1 }}
      />
      <span style={{ minWidth: 60, textAlign: 'right' }}>
        Frame {currentFrame}
      </span>
    </div>
  );
}

const buttonStyle: React.CSSProperties = {
  background: '#444',
  border: '1px solid #555',
  color: '#ccc',
  padding: '2px 8px',
  cursor: 'pointer',
  fontSize: 11,
  borderRadius: 3,
};
