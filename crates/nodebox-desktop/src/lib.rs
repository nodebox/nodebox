//! NodeBox Desktop - Native graphical interface for NodeBox
//!
//! This library provides the desktop GUI and platform implementation for
//! generative designs using NodeBox's node-based workflow.
//!
//! # Testing
//!
//! This crate supports testing through:
//! - State-based unit tests (fast, no GPU)
//! - Integration tests with egui_kittest
//!
//! Use `NodeBoxApp::new_for_testing()` to create a testable app instance.
//!
//! # GPU Rendering
//!
//! When the `gpu-rendering` feature is enabled, this crate provides GPU-accelerated
//! vector rendering via Vello. The following modules become available:
//!
//! - `vello_convert` - Geometry conversion from nodebox-core to Vello types
//! - `vello_renderer` - High-level Vello renderer wrapper

#[cfg(not(target_arch = "wasm32"))]
mod desktop_platform;
#[cfg(not(target_arch = "wasm32"))]
pub use desktop_platform::DesktopPlatform;

mod address_bar;
mod animation_bar;
pub mod app;
pub mod document;
mod canvas;
mod components;
pub mod eval;
mod export;
pub mod handles;
pub mod history;
mod icon_cache;
mod network_view;
mod node_library;
mod node_selection_dialog;
mod notification_banner;
mod pan_zoom;
mod parameter_panel;
pub mod render_worker;
pub mod state;
mod theme;
mod timeline;
mod viewer_pane;

// GPU rendering modules (feature-gated)
#[cfg(feature = "gpu-rendering")]
pub mod vello_convert;
#[cfg(feature = "gpu-rendering")]
pub mod vello_renderer;
#[cfg(feature = "gpu-rendering")]
pub mod vello_viewer;

// Re-export key types for testing and external use
pub use app::NodeBoxApp;
pub use document::{AppRequest, Document, DocumentEnv};
pub use history::{History, SelectionSnapshot};
pub use state::{populate_default_ports, AppState, Notification, NotificationLevel};

// Re-export commonly used types from dependencies
pub use nodebox_core::geometry::{Color, Path, Point};
pub use nodebox_core::node::{Connection, Node, NodeLibrary, Port};
pub use nodebox_core::platform::Platform;
pub use nodebox_core::Value;

// Re-export GPU rendering types when feature is enabled
#[cfg(feature = "gpu-rendering")]
pub use vello_convert::{convert_paths, VelloPath};
#[cfg(feature = "gpu-rendering")]
pub use vello_renderer::{VelloConfig, VelloError, VelloRenderer, ViewTransform};
#[cfg(feature = "gpu-rendering")]
pub use vello_viewer::VelloViewer;

mod native_menu;
mod platform_integration;
mod recent_files;

use std::path::PathBuf;
use std::sync::Arc;

/// Run the NodeBox GUI application.
///
/// This is a convenience function that creates a DesktopPlatform and runs the app.
/// For more control, use `NodeBoxApp::new_with_port` directly.
pub fn run() -> eframe::Result<()> {
    // Initialize logging
    env_logger::init();

    // Set up platform lifecycle integration (e.g. macOS open-file events).
    // Must happen before the event loop starts.
    platform_integration::init();

    // Create the desktop platform for file operations
    let port: Arc<dyn nodebox_core::platform::Platform> = Arc::new(crate::DesktopPlatform::new());

    // Get initial files from command line arguments (each opens a window)
    let initial_files: Vec<PathBuf> = std::env::args()
        .skip(1)
        .map(PathBuf::from)
        .filter(|p| p.extension().is_some_and(|ext| ext == "ndbx"))
        .collect();

    // Native options
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([1280.0, 800.0])
            .with_min_inner_size([800.0, 600.0])
            .with_title("NodeBox"),
        ..Default::default()
    };

    // Run the application
    eframe::run_native(
        "NodeBox",
        options,
        Box::new(move |cc| Ok(Box::new(NodeBoxApp::new_with_port(cc, port.clone(), initial_files.clone())))),
    )
}
