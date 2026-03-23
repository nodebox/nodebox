import React from 'react';
import { useStore } from '../state/store';
import { ERROR_RED, WARNING_YELLOW, ZINC_700, TEXT_STRONG, FONT_SIZE_SMALL } from '../theme/tokens';

export function NotificationBanner() {
  const notifications = useStore((s) => s.notifications);
  const dismissNotification = useStore((s) => s.dismissNotification);

  if (notifications.length === 0) return null;

  return (
    <div>
      {notifications.map((n) => (
        <div
          key={n.id}
          style={{
            display: 'flex',
            alignItems: 'center',
            padding: '4px 12px',
            background: n.level === 'error' ? '#3a1a1a' : n.level === 'warning' ? '#3a3a1a' : ZINC_700,
            borderBottom: `1px solid ${n.level === 'error' ? ERROR_RED : WARNING_YELLOW}40`,
            fontSize: FONT_SIZE_SMALL,
            color: TEXT_STRONG,
          }}
        >
          <span style={{ flex: 1 }}>{n.message}</span>
          <button
            onClick={() => dismissNotification(n.id)}
            style={{ background: 'transparent', border: 'none', color: TEXT_STRONG, cursor: 'pointer', fontSize: 14, padding: '0 4px' }}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
