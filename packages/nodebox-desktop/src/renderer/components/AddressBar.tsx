import React from 'react';
import { useStore } from '../state/store';

export function AddressBar() {
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const setCurrentNetworkPath = useStore((s) => s.setCurrentNetworkPath);
  const parts = currentNetworkPath.split('/').filter(Boolean);

  return (
    <div className="flex items-center h-7 px-2 bg-zinc-800 border-b border-zinc-900 text-[11px] text-zinc-100 shrink-0">
      {parts.map((part, i) => {
        const path = parts.slice(0, i + 1).join('/');
        return (
          <React.Fragment key={path}>
            {i > 0 && <span className="mx-1 text-zinc-400">›</span>}
            <button
              onClick={() => setCurrentNetworkPath(path)}
              className={`bg-transparent border-none cursor-pointer px-1 py-0.5 text-[11px] font-[inherit] ${i === parts.length - 1 ? 'text-zinc-100' : 'text-zinc-400'}`}
            >
              {part}
            </button>
          </React.Fragment>
        );
      })}
    </div>
  );
}
