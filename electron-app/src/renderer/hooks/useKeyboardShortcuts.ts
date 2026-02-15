import { useEffect } from 'react';
import { useStore } from '../state/store';

export function useKeyboardShortcuts() {
  const clearSelection = useStore((s) => s.clearSelection);

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      // Escape: clear selection / close dialogs
      if (e.key === 'Escape') {
        clearSelection();
        useStore.getState().setNodeDialogVisible(false);
        useStore.getState().setAboutDialogVisible(false);
      }

      // Space: toggle playback
      if (e.key === ' ' && !(e.target instanceof HTMLInputElement)) {
        e.preventDefault();
        const { isPlaying, play, stop } = useStore.getState();
        if (isPlaying) stop();
        else play();
      }

      // Tab: open node dialog
      if (e.key === 'Tab' && !(e.target instanceof HTMLInputElement)) {
        e.preventDefault();
        useStore.getState().setNodeDialogVisible(true);
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [clearSelection]);
}
