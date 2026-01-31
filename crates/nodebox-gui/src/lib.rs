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

mod address_bar;
mod animation_bar;
pub mod app;
mod canvas;
pub mod eval;
mod export;
mod handles;
pub mod history;
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

// Re-export key types for testing and external use
pub use app::NodeBoxApp;
pub use history::History;
pub use state::AppState;

// Re-export commonly used types from dependencies
pub use nodebox_core::geometry::{Color, Path, Point};
pub use nodebox_core::node::{Connection, Node, NodeLibrary, Port};
pub use nodebox_core::Value;
