import React from 'react';
import { useStore } from '../state/store';

export function NotificationBanner() {
  const notifications = useStore((s) => s.notifications);
  const dismissNotification = useStore((s) => s.dismissNotification);

  if (notifications.length === 0) return null;

  return (
    <div>
      {notifications.map((n) => (
        <div key={n.id} className={`flex items-center px-3 py-1 text-[11px] text-zinc-50 border-b ${n.level === 'error' ? 'bg-[#3a1a1a] border-error/25' : 'bg-[#3a3a1a] border-[#eab308]/25'}`}>
          <span className="flex-1">{n.message}</span>
          <button onClick={() => dismissNotification(n.id)} className="bg-transparent border-none text-zinc-50 cursor-pointer text-sm px-1">×</button>
        </div>
      ))}
    </div>
  );
}
