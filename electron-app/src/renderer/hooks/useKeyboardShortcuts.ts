import { useEffect } from 'react';
import { useStore } from '../state/store';

export function useKeyboardShortcuts() {
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      // Skip when an input or textarea is focused
      const tag = (e.target as HTMLElement)?.tagName;
      const isEditing = tag === 'INPUT' || tag === 'TEXTAREA';

      // Escape: close dialogs
      if (e.key === 'Escape') {
        useStore.getState().setNodeDialogVisible(false);
        useStore.getState().setAboutDialogVisible(false);
      }

      // Tab: open node dialog
      if (e.key === 'Tab' && !isEditing) {
        e.preventDefault();
        useStore.getState().setNodeDialogPosition(null);
        useStore.getState().setNodeDialogVisible(true);
      }

      // Delete/Backspace: delete selected nodes
      if ((e.key === 'Delete' || e.key === 'Backspace') && !isEditing) {
        e.preventDefault();
        const state = useStore.getState();
        const { selectedNodes } = state;
        if (selectedNodes.size === 0) return;

        state.pushSnapshot(state.library);
        for (const name of selectedNodes) {
          state.removeNode('root', name);
        }
        state.clearSelection();
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);
}
