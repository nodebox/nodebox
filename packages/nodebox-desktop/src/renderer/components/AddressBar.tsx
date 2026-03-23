import React from 'react';
import { useStore } from '../state/store';
import { ZINC_800, ZINC_900, ZINC_400, TEXT_DEFAULT, TEXT_DISABLED, FONT_SIZE_SMALL, ADDRESS_BAR_HEIGHT } from '../theme/tokens';

export function AddressBar() {
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const setCurrentNetworkPath = useStore((s) => s.setCurrentNetworkPath);
  const parts = currentNetworkPath.split('/').filter(Boolean);

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      height: ADDRESS_BAR_HEIGHT,
      padding: '0 8px',
      background: ZINC_800,
      borderBottom: `1px solid ${ZINC_900}`,
      fontSize: FONT_SIZE_SMALL,
      color: TEXT_DEFAULT,
      flexShrink: 0,
    }}>
      {parts.map((part, i) => {
        const path = parts.slice(0, i + 1).join('/');
        return (
          <React.Fragment key={path}>
            {i > 0 && <span style={{ margin: '0 4px', color: ZINC_400 }}>›</span>}
            <button
              onClick={() => setCurrentNetworkPath(path)}
              style={{
                background: 'none',
                border: 'none',
                color: i === parts.length - 1 ? TEXT_DEFAULT : TEXT_DISABLED,
                cursor: 'pointer',
                padding: '2px 4px',
                fontSize: FONT_SIZE_SMALL,
                fontFamily: 'inherit',
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
