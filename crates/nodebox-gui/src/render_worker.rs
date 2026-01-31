//! Background render worker for non-blocking network evaluation.

use std::sync::mpsc;
use std::thread;
use nodebox_core::geometry::Path as GeoPath;
use nodebox_core::node::NodeLibrary;

/// Unique identifier for a render request.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct RenderRequestId(u64);

/// A request sent to the render worker.
pub enum RenderRequest {
    /// Evaluate the network and return geometry.
    Evaluate { id: RenderRequestId, library: NodeLibrary },
    /// Shut down the worker thread.
    Shutdown,
}

/// A result returned from the render worker.
pub enum RenderResult {
    /// Evaluation succeeded.
    Success { id: RenderRequestId, geometry: Vec<GeoPath> },
    /// Evaluation failed.
    Error { id: RenderRequestId, message: String },
}

/// Tracks the state of pending and completed renders.
pub struct RenderState {
    next_id: u64,
    latest_dispatched_id: Option<RenderRequestId>,
    /// Whether a render is currently in progress.
    pub is_rendering: bool,
}

impl RenderState {
    /// Create a new render state.
    pub fn new() -> Self {
        Self {
            next_id: 0,
            latest_dispatched_id: None,
            is_rendering: false,
        }
    }

    /// Dispatch a new render request and return its ID.
    pub fn dispatch_new(&mut self) -> RenderRequestId {
        let id = RenderRequestId(self.next_id);
        self.next_id += 1;
        self.latest_dispatched_id = Some(id);
        self.is_rendering = true;
        id
    }

    /// Check if the given ID is the most recently dispatched.
    pub fn is_current(&self, id: RenderRequestId) -> bool {
        self.latest_dispatched_id == Some(id)
    }

    /// Mark the current render as complete.
    pub fn complete(&mut self) {
        self.is_rendering = false;
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

    /// Request a render of the given library.
    pub fn request_render(&self, id: RenderRequestId, library: NodeLibrary) {
        if let Some(ref tx) = self.request_tx {
            let _ = tx.send(RenderRequest::Evaluate { id, library });
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
    loop {
        match request_rx.recv() {
            Ok(RenderRequest::Evaluate { id, library }) => {
                // Drain to the latest request (skip stale ones)
                let (final_id, final_library) = drain_to_latest(id, library, &request_rx);

                // Evaluate the network
                let geometry = crate::eval::evaluate_network(&final_library);
                let _ = result_tx.send(RenderResult::Success {
                    id: final_id,
                    geometry,
                });
            }
            Ok(RenderRequest::Shutdown) | Err(_) => break,
        }
    }
}

/// Drain any pending requests and return the most recent one.
fn drain_to_latest(
    mut id: RenderRequestId,
    mut library: NodeLibrary,
    rx: &mpsc::Receiver<RenderRequest>,
) -> (RenderRequestId, NodeLibrary) {
    while let Ok(req) = rx.try_recv() {
        match req {
            RenderRequest::Evaluate {
                id: new_id,
                library: new_lib,
            } => {
                id = new_id;
                library = new_lib;
            }
            RenderRequest::Shutdown => break,
        }
    }
    (id, library)
}
