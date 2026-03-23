import { useEffect } from 'react';
import { useStore } from '../state/store';

export function useKeyboardShortcuts() {
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      const tag = (e.target as HTMLElement)?.tagName;
      const isEditing = tag === 'INPUT' || tag === 'TEXTAREA';

      if (e.key === 'Escape') {
        useStore.getState().setNodeSelectionDialogOpen(false);
      }

      if (e.key === 'Tab' && !isEditing) {
        e.preventDefault();
        useStore.getState().setNodeSelectionDialogOpen(true);
      }

      if ((e.key === 'Delete' || e.key === 'Backspace') && !isEditing) {
        e.preventDefault();
        const state = useStore.getState();
        const { selectedNodes } = state;
        if (selectedNodes.length === 0) return;
        state.pushHistory();
        for (const name of selectedNodes) {
          state.removeNodeFromNetwork(state.currentNetworkPath, name);
        }
        state.deselectAll();
      }

      // Spacebar = play/pause
      if (e.key === ' ' && !isEditing && !e.repeat) {
        // Space is handled by usePanZoom for canvas interaction
        // Only play/pause when not holding space for panning
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);
}
