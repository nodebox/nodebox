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
    <div className="flex items-center h-7 px-1 bg-zinc-800 border-t border-zinc-900 text-[13px] text-zinc-100 shrink-0 gap-0.5">
      <div className="w-20">
        <DragValue value={currentFrame} onChange={(v) => setCurrentFrame(Math.max(1, Math.round(v)))} step={1} min={1} precision={0} />
      </div>
      <BarButton icon={playing ? '\u23F8' : '\u25B6'} label={playing ? 'Pause' : 'Play'} onClick={playing ? pause : play} />
      <BarButton icon={'\u23EE'} label="Rewind" onClick={stop} />
    </div>
  );
}

function BarButton({ icon, label, onClick }: { icon: string; label: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-1 h-6 px-2 bg-transparent hover:bg-zinc-700 border-none text-zinc-100 hover:text-zinc-50 cursor-pointer text-[11px] font-[inherit]"
    >
      <span className="text-[13px]">{icon}</span>
      <span>{label}</span>
    </button>
  );
}
