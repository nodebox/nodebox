//! Background render worker for non-blocking network evaluation.

use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc};
use std::thread;
use std::time::Instant;
use nodebox_core::geometry::Path as GeoPath;
use nodebox_core::node::NodeLibrary;
use nodebox_core::platform::{Platform, ProjectContext};
use crate::eval::{NodeError, NodeOutput};

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

/// Unique identifier for a render request.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct RenderRequestId(u64);

/// A request sent to the render worker.
pub enum RenderRequest {
    /// Evaluate the network and return geometry.
    Evaluate {
        id: RenderRequestId,
        library: Arc<NodeLibrary>,
        cancel_token: CancellationToken,
        port: Arc<dyn Platform>,
        project_context: ProjectContext,
    },
    /// Shut down the worker thread.
    Shutdown,
}

/// A result returned from the render worker.
#[allow(dead_code)]
pub enum RenderResult {
    /// Evaluation completed (may include errors).
    Success {
        id: RenderRequestId,
        geometry: Vec<GeoPath>,
        output: crate::eval::NodeOutput,
        errors: Vec<NodeError>,
    },
    /// Evaluation was cancelled before completion.
    Cancelled {
        id: RenderRequestId,
    },
    /// Evaluation failed completely (e.g., panic in worker).
    Error { id: RenderRequestId, message: String },
}

/// Tracks the state of pending and completed renders.
pub struct RenderState {
    next_id: u64,
    latest_dispatched_id: Option<RenderRequestId>,
    /// Whether a render is currently in progress.
    pub is_rendering: bool,
    /// When the current render started (for UI feedback).
    pub render_start_time: Option<Instant>,
    /// Current cancellation token (if render is in progress).
    current_cancel_token: Option<CancellationToken>,
}

impl RenderState {
    /// Create a new render state.
    pub fn new() -> Self {
        Self {
            next_id: 0,
            latest_dispatched_id: None,
            is_rendering: false,
            render_start_time: None,
            current_cancel_token: None,
        }
    }

    /// Dispatch a new render request and return its ID and cancellation token.
    pub fn dispatch_new(&mut self) -> (RenderRequestId, CancellationToken) {
        let id = RenderRequestId(self.next_id);
        self.next_id += 1;
        self.latest_dispatched_id = Some(id);
        self.is_rendering = true;
        self.render_start_time = Some(Instant::now());

        let token = CancellationToken::new();
        self.current_cancel_token = Some(token.clone());

        (id, token)
    }

    /// Check if the given ID is the most recently dispatched.
    pub fn is_current(&self, id: RenderRequestId) -> bool {
        self.latest_dispatched_id == Some(id)
    }

    /// Mark the current render as complete.
    pub fn complete(&mut self) {
        self.is_rendering = false;
        self.render_start_time = None;
        self.current_cancel_token = None;
    }

    /// Cancel the current render if one is in progress.
    pub fn cancel(&self) {
        if let Some(ref token) = self.current_cancel_token {
            token.cancel();
        }
    }

    /// Get elapsed time since render started, if rendering.
    pub fn elapsed(&self) -> Option<std::time::Duration> {
        self.render_start_time.map(|t| t.elapsed())
    }
}

impl Default for RenderState {
    fn default() -> Self {
        Self::new()
    }
}

/// Handle to the background render worker thread.
pub struct RenderWorkerHandle {
    request_tx: Option<mpsc::Sender<RenderRequest>>,
    result_rx: mpsc::Receiver<RenderResult>,
    thread_handle: Option<thread::JoinHandle<()>>,
}

impl RenderWorkerHandle {
    /// Spawn a new render worker thread.
    pub fn spawn() -> Self {
        let (request_tx, request_rx) = mpsc::channel();
        let (result_tx, result_rx) = mpsc::channel();

        let thread_handle = thread::spawn(move || {
            render_worker_loop(request_rx, result_tx);
        });

        Self {
            request_tx: Some(request_tx),
            result_rx,
            thread_handle: Some(thread_handle),
        }
    }

    /// Request a render of the given library with cancellation support.
    pub fn request_render(
        &self,
        id: RenderRequestId,
        library: Arc<NodeLibrary>,
        cancel_token: CancellationToken,
        port: Arc<dyn Platform>,
        project_context: ProjectContext,
    ) {
        if let Some(ref tx) = self.request_tx {
            let _ = tx.send(RenderRequest::Evaluate {
                id,
                library,
                cancel_token,
                port,
                project_context,
            });
        }
    }

    /// Try to receive a render result without blocking.
    pub fn try_recv_result(&self) -> Option<RenderResult> {
        self.result_rx.try_recv().ok()
    }

    /// Shut down the render worker thread.
    pub fn shutdown(&mut self) {
        // Send shutdown message
        if let Some(tx) = self.request_tx.take() {
            let _ = tx.send(RenderRequest::Shutdown);
        }
        // Wait for thread to finish
        if let Some(handle) = self.thread_handle.take() {
            let _ = handle.join();
        }
    }
}

impl Drop for RenderWorkerHandle {
    fn drop(&mut self) {
        self.shutdown();
    }
}

/// The main loop of the render worker thread.
fn render_worker_loop(
    request_rx: mpsc::Receiver<RenderRequest>,
    result_tx: mpsc::Sender<RenderResult>,
) {
    // Cache persists across renders - lives in worker thread only.
    // This avoids race conditions from cross-thread cache sharing.
    let mut node_cache: HashMap<String, NodeOutput> = HashMap::new();

    loop {
        match request_rx.recv() {
            Ok(RenderRequest::Evaluate { id, library, cancel_token, port, project_context }) => {
                // Drain to the latest request (skip stale ones)
                let (final_id, final_library, final_token, final_port, final_project_context) =
                    drain_to_latest(id, library, cancel_token, port, project_context, &request_rx);

                // Clear cache when library changes to ensure fresh evaluation.
                // Future optimization: use hash-based cache keys so unchanged nodes stay cached.
                node_cache.clear();

                // Evaluate the network with cancellation support
                let result = crate::eval::evaluate_network_cancellable(
                    &final_library,
                    &final_token,
                    &mut node_cache,
                    &final_port,
                    &final_project_context,
                );

                match result {
                    crate::eval::EvalOutcome::Completed { geometry, output, errors } => {
                        let _ = result_tx.send(RenderResult::Success {
                            id: final_id,
                            geometry,
                            output,
                            errors,
                        });
                    }
                    crate::eval::EvalOutcome::Cancelled => {
                        let _ = result_tx.send(RenderResult::Cancelled {
                            id: final_id,
                        });
                    }
                }
            }
            Ok(RenderRequest::Shutdown) | Err(_) => break,
        }
    }
}

/// Drain any pending requests and return the most recent one.
fn drain_to_latest(
    mut id: RenderRequestId,
    mut library: Arc<NodeLibrary>,
    mut cancel_token: CancellationToken,
    mut port: Arc<dyn Platform>,
    mut project_context: ProjectContext,
    rx: &mpsc::Receiver<RenderRequest>,
) -> (RenderRequestId, Arc<NodeLibrary>, CancellationToken, Arc<dyn Platform>, ProjectContext) {
    while let Ok(req) = rx.try_recv() {
        match req {
            RenderRequest::Evaluate {
                id: new_id,
                library: new_lib,
                cancel_token: new_token,
                port: new_port,
                project_context: new_ctx,
            } => {
                id = new_id;
                library = new_lib;
                cancel_token = new_token;
                port = new_port;
                project_context = new_ctx;
            }
            RenderRequest::Shutdown => break,
        }
    }
    (id, library, cancel_token, port, project_context)
}
