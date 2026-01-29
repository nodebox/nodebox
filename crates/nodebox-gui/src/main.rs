//! NodeBox GUI - Native graphical interface for NodeBox
//!
//! This application provides a visual environment for creating generative designs
//! using NodeBox's node-based workflow.

mod address_bar;
mod animation_bar;
mod app;
mod canvas;
mod export;
mod handles;
mod history;
mod network_view;
mod node_library;
mod node_selection_dialog;
mod panels;
mod state;
mod theme;
mod timeline;
mod viewer_pane;

use app::NodeBoxApp;

fn main() -> eframe::Result<()> {
    // Initialize logging
    env_logger::init();

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
        Box::new(|cc| Ok(Box::new(NodeBoxApp::new(cc)))),
    )
}
