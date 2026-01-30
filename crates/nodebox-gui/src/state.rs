//! Application state management.

use std::path::{Path, PathBuf};
use nodebox_core::geometry::{Path as GeoPath, Color};
use nodebox_core::node::{Node, NodeLibrary, Port, Connection};
use nodebox_svg::render_to_svg;
use crate::eval;

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
    pub library: NodeLibrary,
}

impl Default for AppState {
    fn default() -> Self {
        Self::new()
    }
}

impl AppState {
    /// Create a new application state with demo content.
    pub fn new() -> Self {
        // Create a demo node library
        let library = Self::create_demo_library();

        // Evaluate the network to get the initial geometry
        let geometry = eval::evaluate_network(&library);

        Self {
            current_file: None,
            dirty: false,
            show_about: false,
            geometry,
            selected_node: None,
            background_color: Color::WHITE,
            library,
        }
    }

    /// Re-evaluate the network and update the geometry.
    pub fn evaluate(&mut self) {
        self.geometry = eval::evaluate_network(&self.library);
    }

    /// Create a demo node library with some example nodes.
    fn create_demo_library() -> NodeLibrary {
        let mut library = NodeLibrary::new("demo");

        // Create demo nodes (positions in grid units, vertical flow: top to bottom)
        // Layout:
        //   ellipse1 (2,2)     rect1 (6,2)
        //      ↓                  ↓
        //   colorize1 (2,3)   colorize2 (6,3)
        //         ↓              ↓
        //           merge1 (4,4)
        let ellipse_node = Node::new("ellipse1")
            .with_prototype("corevector.ellipse")
            .with_function("corevector/ellipse")
            .with_category("geometry")
            .with_position(2.0, 2.0)
            .with_input(Port::float("x", -100.0))
            .with_input(Port::float("y", -50.0))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0));

        let colorize_node = Node::new("colorize1")
            .with_prototype("corevector.colorize")
            .with_function("corevector/colorize")
            .with_category("color")
            .with_position(2.0, 3.0)
            .with_input(Port::geometry("shape"))
            .with_input(Port::color("fill", Color::rgb(0.9, 0.2, 0.2)))
            .with_input(Port::color("stroke", Color::BLACK))
            .with_input(Port::float("strokeWidth", 1.0));

        let rect_node = Node::new("rect1")
            .with_prototype("corevector.rect")
            .with_function("corevector/rect")
            .with_category("geometry")
            .with_position(6.0, 2.0)
            .with_input(Port::float("x", 100.0))
            .with_input(Port::float("y", 50.0))
            .with_input(Port::float("width", 80.0))
            .with_input(Port::float("height", 80.0));

        let colorize2_node = Node::new("colorize2")
            .with_prototype("corevector.colorize")
            .with_function("corevector/colorize")
            .with_category("color")
            .with_position(6.0, 3.0)
            .with_input(Port::geometry("shape"))
            .with_input(Port::color("fill", Color::rgb(0.2, 0.8, 0.3)))
            .with_input(Port::color("stroke", Color::BLACK))
            .with_input(Port::float("strokeWidth", 1.0));

        let merge_node = Node::new("merge1")
            .with_prototype("corevector.merge")
            .with_function("corevector/merge")
            .with_category("geometry")
            .with_position(4.0, 4.0)
            .with_input(Port::geometry("shapes"));

        // Build the root network
        library.root = Node::network("root")
            .with_child(ellipse_node)
            .with_child(colorize_node)
            .with_child(rect_node)
            .with_child(colorize2_node)
            .with_child(merge_node)
            .with_connection(Connection::new("ellipse1", "colorize1", "shape"))
            .with_connection(Connection::new("rect1", "colorize2", "shape"))
            .with_connection(Connection::new("colorize1", "merge1", "shapes"))
            .with_connection(Connection::new("colorize2", "merge1", "shapes"))
            .with_rendered_child("merge1");

        library
    }

    /// Create a new empty document.
    pub fn new_document(&mut self) {
        self.current_file = None;
        self.dirty = false;
        self.geometry.clear();
        self.selected_node = None;
    }

    /// Load a file.
    pub fn load_file(&mut self, path: &Path) -> Result<(), String> {
        // For now, just update the path
        // TODO: Actually parse the .ndbx file and load nodes
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        Ok(())
    }

    /// Save the current document.
    pub fn save_file(&mut self, path: &Path) -> Result<(), String> {
        // TODO: Implement proper .ndbx saving
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        Ok(())
    }

    /// Export to SVG.
    pub fn export_svg(&self, path: &Path) -> Result<(), String> {
        // Calculate bounds
        let mut min_x = f64::MAX;
        let mut min_y = f64::MAX;
        let mut max_x = f64::MIN;
        let mut max_y = f64::MIN;

        for geo in &self.geometry {
            if let Some(bounds) = geo.bounds() {
                min_x = min_x.min(bounds.x);
                min_y = min_y.min(bounds.y);
                max_x = max_x.max(bounds.x + bounds.width);
                max_y = max_y.max(bounds.y + bounds.height);
            }
        }

        let width = (max_x - min_x + 40.0).max(100.0);
        let height = (max_y - min_y + 40.0).max(100.0);

        let svg = render_to_svg(&self.geometry, width, height);
        std::fs::write(path, svg).map_err(|e| e.to_string())
    }
}

