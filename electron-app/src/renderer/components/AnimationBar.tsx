import { Play, Pause, SkipBack } from 'lucide-react';
import { useStore } from '../state/store';
import {
  ANIMATION_BAR_BACKGROUND,
  ANIMATION_BAR_HEIGHT,
  TEXT_DEFAULT,
  FONT_SIZE_SMALL,
  BORDER_COLOR,
} from '../theme/tokens';

export function AnimationBar() {
  const frame = useStore((s) => s.frame);
  const isPlaying = useStore((s) => s.isPlaying);
  const play = useStore((s) => s.play);
  const stop = useStore((s) => s.stop);
  const setFrame = useStore((s) => s.setFrame);

  return (
    <div
      className="flex items-center gap-1 px-2"
      style={{
        height: ANIMATION_BAR_HEIGHT,
        background: ANIMATION_BAR_BACKGROUND,
        borderTop: `1px solid ${BORDER_COLOR}`,
        fontSize: FONT_SIZE_SMALL,
      }}
    >
      {/* Frame counter */}
      <span
        style={{
          minWidth: 50,
          textAlign: 'center',
          color: TEXT_DEFAULT,
          fontSize: FONT_SIZE_SMALL,
        }}
      >
        {frame}
      </span>

      {/* Play/Pause button */}
      <button
        onClick={() => (isPlaying ? stop() : play())}
        className="flex items-center justify-center"
        style={{
          width: 28,
          height: 28,
          color: TEXT_DEFAULT,
          background: 'transparent',
          border: 'none',
          cursor: 'pointer',
        }}
      >
        {isPlaying ? <Pause size={14} /> : <Play size={14} />}
      </button>

      {/* Rewind button */}
      <button
        onClick={() => setFrame(1)}
        className="flex items-center justify-center"
        style={{
          width: 28,
          height: 28,
          color: TEXT_DEFAULT,
          background: 'transparent',
          border: 'none',
          cursor: 'pointer',
        }}
      >
        <SkipBack size={14} />
      </button>
    </div>
  );
}
