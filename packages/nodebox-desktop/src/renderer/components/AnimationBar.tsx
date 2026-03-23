import React from 'react';
import { useStore } from '../state/store';
import { DragValue } from './DragValue';

export function AnimationBar() {
  const currentFrame = useStore((s) => s.currentFrame);
  const playing = useStore((s) => s.playing);
  const setCurrentFrame = useStore((s) => s.setCurrentFrame);
  const play = useStore((s) => s.play);
  const pause = useStore((s) => s.pause);
  const stop = useStore((s) => s.stop);

  return (
    <div style={{ display: 'flex', alignItems: 'center', height: 28, padding: '0 4px', background: '#27272a', borderTop: '1px solid #18181b', fontSize: 13, color: '#f4f4f5', flexShrink: 0, gap: 2 }}>
      {/* Frame number (draggable) */}
      <div style={{ width: 80 }}>
        <DragValue value={currentFrame} onChange={(v) => setCurrentFrame(Math.max(1, Math.round(v)))} step={1} min={1} precision={0} />
      </div>

      {/* Play/Pause */}
      <BarButton icon={playing ? '\u23F8' : '\u25B6'} label={playing ? 'Pause' : 'Play'} onClick={playing ? pause : play} />

      {/* Rewind */}
      <BarButton icon={'\u23EE'} label="Rewind" onClick={stop} />
    </div>
  );
}

function BarButton({ icon, label, onClick }: { icon: string; label: string; onClick: () => void }) {
  const [hovered, setHovered] = React.useState(false);
  return (
    <button
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: 'flex', alignItems: 'center', gap: 4,
        height: 24, padding: '0 8px',
        background: hovered ? '#3f3f46' : 'transparent',
        border: 'none', color: hovered ? '#fafafa' : '#f4f4f5',
        cursor: 'pointer', fontSize: 11, fontFamily: 'inherit',
      }}
    >
      <span style={{ fontSize: 13 }}>{icon}</span>
      <span>{label}</span>
    </button>
  );
}
