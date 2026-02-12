//! Undo/redo history management.

use std::sync::Arc;
use nodebox_core::node::NodeLibrary;

/// Maximum number of undo states to keep.
const MAX_HISTORY: usize = 50;

/// The undo/redo history manager.
pub struct History {
    /// Past states (undo stack).
    undo_stack: Vec<Arc<NodeLibrary>>,
    /// Future states (redo stack).
    redo_stack: Vec<Arc<NodeLibrary>>,
    /// The last saved state (to track changes).
    #[allow(dead_code)]
    last_saved_state: Option<Arc<NodeLibrary>>,
}

impl Default for History {
    fn default() -> Self {
        Self::new()
    }
}

impl History {
    /// Create a new empty history.
    pub fn new() -> Self {
        Self {
            undo_stack: Vec::new(),
            redo_stack: Vec::new(),
            last_saved_state: None,
        }
    }

    /// Check if undo is available.
    pub fn can_undo(&self) -> bool {
        !self.undo_stack.is_empty()
    }

    /// Check if redo is available.
    pub fn can_redo(&self) -> bool {
        !self.redo_stack.is_empty()
    }

    /// Save the current state before making changes.
    /// Call this BEFORE modifying the library.
    pub fn save_state(&mut self, library: &Arc<NodeLibrary>) {
        self.undo_stack.push(Arc::clone(library));

        // Clear redo stack when new changes are made
        self.redo_stack.clear();

        // Limit history size
        while self.undo_stack.len() > MAX_HISTORY {
            self.undo_stack.remove(0);
        }
    }

    /// Undo the last change, returning the previous state.
    /// Call this to restore the library to its previous state.
    pub fn undo(&mut self, current: &Arc<NodeLibrary>) -> Option<Arc<NodeLibrary>> {
        if let Some(previous) = self.undo_stack.pop() {
            // Save current state for redo
            self.redo_stack.push(Arc::clone(current));
            Some(previous)
        } else {
            None
        }
    }

    /// Redo the last undone change, returning the restored state.
    pub fn redo(&mut self, current: &Arc<NodeLibrary>) -> Option<Arc<NodeLibrary>> {
        if let Some(next) = self.redo_stack.pop() {
            // Save current state for undo
            self.undo_stack.push(Arc::clone(current));
            Some(next)
        } else {
            None
        }
    }

    /// Mark the current state as saved.
    #[allow(dead_code)]
    pub fn mark_saved(&mut self, library: &Arc<NodeLibrary>) {
        self.last_saved_state = Some(Arc::clone(library));
    }

    /// Check if the library has unsaved changes since the last save.
    #[allow(dead_code)]
    pub fn has_unsaved_changes(&self, library: &NodeLibrary) -> bool {
        match &self.last_saved_state {
            Some(saved) => saved.as_ref() != library,
            None => true, // Never saved, so always has changes
        }
    }

    /// Clear all history.
    #[allow(dead_code)]
    pub fn clear(&mut self) {
        self.undo_stack.clear();
        self.redo_stack.clear();
    }

    /// Get the number of undo states available.
    pub fn undo_count(&self) -> usize {
        self.undo_stack.len()
    }

    /// Get the number of redo states available.
    pub fn redo_count(&self) -> usize {
        self.redo_stack.len()
    }
}
