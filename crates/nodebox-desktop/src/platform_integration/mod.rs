//! Thin per-platform integration adapters for app lifecycle events.
//!
//! These adapters handle the things winit does not surface — most notably
//! how the OS delivers "open this document" requests — and forward them to
//! the portable app through a channel polled each frame.

#[cfg(target_os = "macos")]
mod macos;

use std::path::PathBuf;
use std::sync::mpsc::Receiver;
#[cfg(target_os = "macos")]
use std::sync::Mutex;

/// Receiver handoff from `init` (pre event loop) to the app constructor.
#[cfg(target_os = "macos")]
static OPEN_FILE_RX: Mutex<Option<Receiver<PathBuf>>> = Mutex::new(None);

/// Initialize platform lifecycle integration.
///
/// Must be called before the event loop starts (i.e. before
/// `eframe::run_native`). On macOS this arranges for the
/// `application:openURLs:` Apple event callback to be hooked as soon as the
/// application finishes launching, so that documents opened via Finder
/// (double-click, drag onto dock icon, or `open file.ndbx`) reach the app —
/// including the document that triggered the launch. On Windows and Linux
/// the OS passes document paths via argv, which is handled at startup, so
/// this is a no-op.
pub fn init() {
    #[cfg(target_os = "macos")]
    {
        let rx = macos::init_open_file_handler();
        *OPEN_FILE_RX.lock().unwrap() = Some(rx);
    }
}

/// Take the open-file receiver produced by `init`.
///
/// Called once by the app constructor; returns `None` on platforms without
/// an open-file handler or if `init` was not called.
pub fn take_open_file_receiver() -> Option<Receiver<PathBuf>> {
    #[cfg(target_os = "macos")]
    {
        OPEN_FILE_RX.lock().unwrap().take()
    }
    #[cfg(not(target_os = "macos"))]
    {
        None
    }
}
