//! NodeBox GUI - Native graphical interface for NodeBox
//!
//! This library provides the core components for creating a visual environment
//! for generative designs using NodeBox's node-based workflow.
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

mod address_bar;
mod animation_bar;
pub mod app;
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
mod pan_zoom;
mod panels;
mod render_worker;
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
pub use history::History;
pub use state::{populate_default_ports, AppState};

// Re-export commonly used types from dependencies
pub use nodebox_core::geometry::{Color, Path, Point};
pub use nodebox_core::node::{Connection, Node, NodeLibrary, Port};
pub use nodebox_core::Value;

// Re-export GPU rendering types when feature is enabled
#[cfg(feature = "gpu-rendering")]
pub use vello_convert::{convert_paths, VelloPath};
#[cfg(feature = "gpu-rendering")]
pub use vello_renderer::{VelloConfig, VelloError, VelloRenderer, ViewTransform};
#[cfg(feature = "gpu-rendering")]
pub use vello_viewer::VelloViewer;

mod native_menu;
mod recent_files;

use native_menu::NativeMenuHandle;
use std::path::PathBuf;

/// Run the NodeBox GUI application.
pub fn run() -> eframe::Result<()> {
    // Initialize logging
    env_logger::init();

    // Initialize native menu bar (macOS)
    // Must be done before eframe starts, and menu handle is passed to the app
    let native_menu = NativeMenuHandle::new();

    // Get initial file from command line arguments
    let initial_file: Option<PathBuf> = std::env::args()
        .nth(1)
        .map(PathBuf::from)
        .filter(|p| p.extension().map_or(false, |ext| ext == "ndbx"));

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
        Box::new(move |cc| Ok(Box::new(NodeBoxApp::new_with_file(cc, initial_file, Some(native_menu))))),
    )
}
