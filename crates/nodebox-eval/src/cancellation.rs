//! Cooperative cancellation for render operations.

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;

/// Token for cooperative cancellation of render operations.
///
/// The token is shared between the main thread and the render worker.
/// When cancelled, the worker should check `is_cancelled()` at appropriate
/// boundaries (per-node and per-iteration) and return early.
#[derive(Clone)]
pub struct CancellationToken {
    cancelled: Arc<AtomicBool>,
}

impl CancellationToken {
    /// Create a new cancellation token in the non-cancelled state.
    pub fn new() -> Self {
        Self {
            cancelled: Arc::new(AtomicBool::new(false)),
        }
    }

    /// Request cancellation. This is thread-safe and can be called from any thread.
    pub fn cancel(&self) {
        self.cancelled.store(true, Ordering::SeqCst);
    }

    /// Check if cancellation has been requested.
    /// Call this at appropriate boundaries (before each node, during iterations).
    pub fn is_cancelled(&self) -> bool {
        self.cancelled.load(Ordering::SeqCst)
    }
}

impl Default for CancellationToken {
    fn default() -> Self {
        Self::new()
    }
}
