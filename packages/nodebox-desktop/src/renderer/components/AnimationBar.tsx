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
    <div className="flex items-center h-7 px-1 bg-zinc-800 border-t border-zinc-900 text-[13px] text-zinc-100 shrink-0">
      <DragValue value={currentFrame} onChange={(v) => setCurrentFrame(Math.max(1, Math.round(v)))} step={1} min={1} precision={0} />
      <div className="w-1" />
      <IconButton icon={playing ? '\u23F8' : '\u25B6'} tooltip={playing ? 'Stop' : 'Play'} onClick={playing ? pause : play} />
      <IconButton icon={'\u23EE'} tooltip="Rewind" onClick={stop} />
    </div>
  );
}

function IconButton({ icon, tooltip, onClick }: { icon: string; tooltip: string; onClick: () => void }) {
  return (
    <button onClick={onClick} title={tooltip} className="w-7 h-7 flex items-center justify-center bg-transparent border-none text-zinc-100 hover:bg-zinc-700 hover:text-zinc-50 cursor-pointer text-[13px] font-[inherit]">
      {icon}
    </button>
  );
}
