//! Application state management.

use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use nodebox_core::geometry::{Path as GeoPath, Color};
use nodebox_core::node::{Node, NodeLibrary, Port};
use crate::eval::NodeOutput;

/// Severity level for a notification.
#[derive(Debug, Clone, PartialEq)]
pub enum NotificationLevel {
    /// Informational notice.
    #[allow(dead_code)]
    Info,
    /// Warning about potential issues.
    Warning,
}

/// A dismissible notification shown to the user.
#[derive(Debug, Clone)]
pub struct Notification {
    /// Unique identifier for this notification.
    pub id: u64,
    /// The message to display.
    pub message: String,
    /// Severity level.
    pub level: NotificationLevel,
}

/// The main application state.
pub struct AppState {
    /// Current file path (if saved).
    pub current_file: Option<PathBuf>,

    /// Whether the document has unsaved changes.
    pub dirty: bool,

    /// Whether to show the about dialog.
    pub show_about: bool,

    /// The current geometry to render.
    pub geometry: Vec<GeoPath>,

    /// Currently selected node (if any).
    pub selected_node: Option<String>,

    /// Canvas background color.
    pub background_color: Color,

    /// The node library (document).
    /// Wrapped in Arc for cheap cloning when dispatching renders.
    /// Use `Arc::make_mut` for copy-on-write mutation.
    pub library: Arc<NodeLibrary>,

    /// Per-node error messages (node_name -> error message).
    pub node_errors: HashMap<String, String>,

    /// The raw output of the rendered node (for non-geometry data display).
    pub node_output: NodeOutput,

    /// Active notifications (dismissible banners).
    pub notifications: Vec<Notification>,

    /// Counter for generating unique notification IDs.
    notification_counter: u64,
}

impl Default for AppState {
    fn default() -> Self {
        Self::new()
    }
}

impl AppState {
    /// Create a new application state with demo content.
    ///
    /// Note: Geometry starts empty - the render worker will evaluate with
    /// the proper Port and populate it.
    pub fn new() -> Self {
        let library = Arc::new(Self::create_demo_library());

        Self {
            current_file: None,
            dirty: false,
            show_about: false,
            geometry: Vec::new(), // Render worker will populate
            selected_node: None,
            background_color: Color::rgb(232.0 / 255.0, 232.0 / 255.0, 232.0 / 255.0),
            library,
            node_errors: HashMap::new(),
            node_output: NodeOutput::None,
            notifications: Vec::new(),
            notification_counter: 0,
        }
    }

    /// Create a demo node library with a single rect node.
    fn create_demo_library() -> NodeLibrary {
        let mut library = NodeLibrary::new("demo");

        let rect_node = Node::new("rect1")
            .with_prototype("corevector.rect")
            .with_function("corevector/rect")
            .with_category("geometry")
            .with_position(1.0, 1.0)
            .with_input(Port::point("position", nodebox_core::geometry::Point::ZERO))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0))
            .with_input(Port::point("roundness", nodebox_core::geometry::Point::ZERO));

        library.root = Node::network("root")
            .with_child(rect_node)
            .with_rendered_child("rect1");

        library
    }

    /// Create a new empty document.
    pub fn new_document(&mut self) {
        self.current_file = None;
        self.dirty = false;
        self.geometry.clear();
        self.node_output = NodeOutput::None;
        self.selected_node = None;
        self.node_errors.clear();
        self.notifications.clear();
    }

    /// Load a file.
    ///
    /// Note: Geometry is cleared - the render worker will evaluate with
    /// the proper Port and populate it.
    pub fn load_file(&mut self, path: &Path) -> Result<(), String> {
        // Parse the .ndbx file with warnings (old format versions load best-effort)
        let (mut library, warnings) =
            nodebox_core::ndbx::parse_file_with_warnings(path).map_err(|e| e.to_string())?;

        // Ensure all nodes have their default ports populated
        populate_default_ports(&mut library.root);

        // Update state
        self.library = Arc::new(library);
        self.background_color = self.library.background_color();
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        self.selected_node = None;
        self.geometry.clear(); // Render worker will populate
        self.node_output = NodeOutput::None;
        self.node_errors.clear();

        // Surface any warnings as notifications
        self.notifications.clear();
        for warning in warnings {
            self.add_notification(warning, NotificationLevel::Warning);
        }

        Ok(())
    }

    /// Add a notification and return its ID.
    pub fn add_notification(&mut self, message: String, level: NotificationLevel) -> u64 {
        self.notification_counter += 1;
        let id = self.notification_counter;
        self.notifications.push(Notification { id, message, level });
        id
    }

    /// Dismiss (remove) a notification by ID.
    pub fn dismiss_notification(&mut self, id: u64) {
        self.notifications.retain(|n| n.id != id);
    }

    /// Save the current document.
    pub fn save_file(&mut self, path: &Path) -> Result<(), String> {
        nodebox_core::ndbx::serialize_to_file(&self.library, path)
            .map_err(|e| e.to_string())?;
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        Ok(())
    }

    /// Export to SVG.
    /// Uses document width/height and centered coordinate system.
    pub fn export_svg(&self, path: &Path, width: f64, height: f64) -> Result<(), String> {
        let options = nodebox_core::svg::SvgOptions::new(width, height)
            .with_centered(true)
            .with_background(Some(self.background_color));
        let svg = nodebox_core::svg::render_to_svg_with_options(&self.geometry, &options);
        std::fs::write(path, svg).map_err(|e| e.to_string())
    }
}

pub use nodebox_core::node::populate_default_ports;
