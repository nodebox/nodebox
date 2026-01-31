//! NodeBox GUI - Native graphical interface for NodeBox
//!
//! This application provides a visual environment for creating generative designs
//! using NodeBox's node-based workflow.

mod address_bar;
mod animation_bar;
mod app;
mod canvas;
mod eval;
mod export;
mod handles;
mod history;
mod native_menu;
mod network_view;
mod node_library;
mod node_selection_dialog;
mod pan_zoom;
mod panels;
mod render_worker;
mod state;
mod theme;
mod timeline;
mod viewer_pane;

use app::NodeBoxApp;
use native_menu::NativeMenuHandle;
use std::path::PathBuf;

fn main() -> eframe::Result<()> {
    // Initialize logging
    env_logger::init();

    // Initialize native menu bar (macOS)
    // Must be done before eframe starts, and menu handle must be kept alive
    let _native_menu = NativeMenuHandle::new();

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
        Box::new(move |cc| Ok(Box::new(NodeBoxApp::new_with_file(cc, initial_file)))),
    )
}
