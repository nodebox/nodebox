import { X, AlertTriangle } from 'lucide-react';
import { useStore } from '../state/store';

export function NotificationBanner() {
  const notifications = useStore((s) => s.notifications);
  const dismissNotification = useStore((s) => s.dismissNotification);

  if (notifications.length === 0) return null;

  return (
    <>
      {notifications.map((n) => (
        <div
          key={n.id}
          className="flex items-center gap-2 px-2 bg-zinc-700 border-b border-zinc-600"
          style={{ height: 28, fontSize: 11 }}
        >
          <AlertTriangle size={12} className="text-zinc-400 shrink-0" />
          <span className="text-zinc-300 flex-1 truncate">{n.message}</span>
          <button
            className="text-zinc-400 hover:text-zinc-200 cursor-pointer bg-transparent border-none p-0 shrink-0"
            onClick={() => dismissNotification(n.id)}
          >
            <X size={14} />
          </button>
        </div>
      ))}
    </>
  );
}
