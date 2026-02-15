import { useEffect } from 'react';
import { useStore } from './state/store';
import { useKeyboardShortcuts } from './hooks/useKeyboardShortcuts';
import { AppLayout } from './components/AppLayout';
import type { MenuAction } from '../shared/ipc-channels';

export function App() {
  useKeyboardShortcuts();

  const toggleHandles = useStore((s) => s.toggleHandles);
  const togglePoints = useStore((s) => s.togglePoints);
  const toggleOrigin = useStore((s) => s.toggleOrigin);
  const toggleCanvasBorder = useStore((s) => s.toggleCanvasBorder);
  const setAboutDialogVisible = useStore((s) => s.setAboutDialogVisible);

  useEffect(() => {
    if (!window.electronAPI) return;

    window.electronAPI.onMenuAction((action: string) => {
      const menuAction = action as MenuAction;
      switch (menuAction) {
        case 'view:toggle-handles':
          toggleHandles();
          break;
        case 'view:toggle-points':
          togglePoints();
          break;
        case 'view:toggle-origin':
          toggleOrigin();
          break;
        case 'view:toggle-canvas-border':
          toggleCanvasBorder();
          break;
        case 'help:about':
          setAboutDialogVisible(true);
          break;
      }
    });
  }, [
    toggleHandles,
    togglePoints,
    toggleOrigin,
    toggleCanvasBorder,
    setAboutDialogVisible,
  ]);

  return <AppLayout />;
}
