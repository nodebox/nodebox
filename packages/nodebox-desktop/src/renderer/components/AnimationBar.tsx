import React from 'react';
import { useStore } from '../state/store';
import { DragValue } from './DragValue';
import { ZINC_700, ZINC_800, ZINC_900, TEXT_DEFAULT, TEXT_STRONG, FONT_SIZE_BASE, ANIMATION_BAR_HEIGHT, ZINC_500 } from '../theme/tokens';

export function AnimationBar() {
  const currentFrame = useStore((s) => s.currentFrame);
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
      padding: '0 4px',
      background: ZINC_800,
      borderTop: `1px solid ${ZINC_900}`,
      gap: 0,
      fontSize: FONT_SIZE_BASE,
      color: TEXT_DEFAULT,
      flexShrink: 0,
    }}>
      {/* Frame number (draggable) */}
      <DragValue
        value={currentFrame}
        onChange={(v) => setCurrentFrame(Math.max(1, Math.round(v)))}
        step={1}
        min={1}
        precision={0}
      />

      <div style={{ width: 4 }} />

      {/* Play/Pause */}
      <IconButton
        icon={playing ? '\u23F8' : '\u25B6'}
        tooltip={playing ? 'Stop' : 'Play'}
        onClick={playing ? () => { pause(); } : play}
      />

      {/* Rewind */}
      <IconButton
        icon={'\u23EE'}
        tooltip="Rewind"
        onClick={stop}
      />
    </div>
  );
}

function IconButton({ icon, tooltip, onClick }: { icon: string; tooltip: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      title={tooltip}
      style={{
        width: ANIMATION_BAR_HEIGHT,
        height: ANIMATION_BAR_HEIGHT,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'transparent',
        border: 'none',
        color: TEXT_DEFAULT,
        cursor: 'pointer',
        fontSize: FONT_SIZE_BASE,
        fontFamily: 'inherit',
      }}
      onMouseEnter={(e) => { e.currentTarget.style.background = ZINC_700; e.currentTarget.style.color = TEXT_STRONG; }}
      onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = TEXT_DEFAULT; }}
    >
      {icon}
    </button>
  );
}
