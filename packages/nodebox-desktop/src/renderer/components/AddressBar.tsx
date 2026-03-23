import React from 'react';
import { useStore } from '../state/store';

export function AddressBar() {
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const setCurrentNetworkPath = useStore((s) => s.setCurrentNetworkPath);
  const parts = currentNetworkPath.split('/').filter(Boolean);

  return (
    <div style={{ display: 'flex', alignItems: 'center', height: 28, padding: '0 8px', background: '#27272a', borderBottom: '1px solid #18181b', fontSize: 11, color: '#f4f4f5', flexShrink: 0 }}>
      {parts.map((part, i) => {
        const path = parts.slice(0, i + 1).join('/');
        return (
          <React.Fragment key={path}>
            {i > 0 && <span style={{ margin: '0 4px', color: '#9f9fa9' }}>›</span>}
            <button
              onClick={() => setCurrentNetworkPath(path)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '2px 4px', fontSize: 11, fontFamily: 'inherit', color: i === parts.length - 1 ? '#f4f4f5' : '#9f9fa9' }}
            >
              {part}
            </button>
          </React.Fragment>
        );
      })}
    </div>
  );
}
