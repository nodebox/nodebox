import { Play, Pause, SkipBack } from 'lucide-react';
import { useStore } from '../state/store';
import { DragValue } from './DragValue';

export function AnimationBar() {
  const frame = useStore((s) => s.frame);
  const isPlaying = useStore((s) => s.isPlaying);
  const play = useStore((s) => s.play);
  const stop = useStore((s) => s.stop);
  const setFrame = useStore((s) => s.setFrame);

  return (
    <div
      className="flex items-center gap-1 px-2 bg-zinc-800 border-t border-zinc-500 text-[11px]"
      style={{ height: 28 }}
    >
      {/* Frame counter */}
      <div
        data-testid="frame-counter"
        style={{ minWidth: 50, height: 26 }}
      >
        <DragValue
          value={frame}
          onChange={(v) => setFrame(Math.max(1, Math.round(v)))}
          min={1}
          speed={1}
          format={(v) => String(Math.round(v))}
        />
      </div>

      {/* Play/Pause button */}
      <button
        onClick={() => (isPlaying ? stop() : play())}
        className="flex items-center justify-center text-zinc-100 bg-transparent border-none cursor-pointer"
        style={{ width: 28, height: 28 }}
      >
        {isPlaying ? <Pause size={14} /> : <Play size={14} />}
      </button>

      {/* Rewind button */}
      <button
        onClick={() => setFrame(1)}
        className="flex items-center justify-center text-zinc-100 bg-transparent border-none cursor-pointer"
        style={{ width: 28, height: 28 }}
      >
        <SkipBack size={14} />
      </button>
    </div>
  );
}
