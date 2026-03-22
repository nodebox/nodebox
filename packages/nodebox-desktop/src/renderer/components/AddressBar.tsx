import React from 'react';
import { useStore } from '../state/store.js';

export function AddressBar() {
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const setCurrentNetworkPath = useStore((s) => s.setCurrentNetworkPath);

  const parts = currentNetworkPath.split('/').filter(Boolean);

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      height: 28,
      padding: '0 8px',
      background: '#2a2a2a',
      borderBottom: '1px solid #333',
      fontSize: 12,
      color: '#ccc',
    }}>
      {parts.map((part, i) => {
        const path = parts.slice(0, i + 1).join('/');
        return (
          <React.Fragment key={path}>
            {i > 0 && <span style={{ margin: '0 4px', color: '#555' }}>/</span>}
            <button
              onClick={() => setCurrentNetworkPath(path)}
              style={{
                background: 'none',
                border: 'none',
                color: i === parts.length - 1 ? '#fff' : '#888',
                cursor: 'pointer',
                padding: '2px 4px',
                fontSize: 12,
              }}
            >
              {part}
            </button>
          </React.Fragment>
        );
      })}
    </div>
  );
}
