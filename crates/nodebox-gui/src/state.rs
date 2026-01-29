//! Application state management.

use std::path::{Path, PathBuf};
use nodebox_core::geometry::{Path as GeoPath, Color, Point};
use nodebox_core::node::{Node, NodeLibrary, Port, Connection};
use nodebox_ops;
use nodebox_svg::render_to_svg;

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
        // Create some demo geometry
        let mut geometry = Vec::new();

        // Add some demo shapes
        let mut circle = nodebox_ops::ellipse(Point::new(200.0, 200.0), 100.0, 100.0);
        circle.fill = Some(Color::rgb(0.9, 0.2, 0.2));
        geometry.push(circle);

        let mut rect = nodebox_ops::rect(Point::new(350.0, 200.0), 80.0, 80.0, Point::ZERO);
        rect.fill = Some(Color::rgb(0.2, 0.8, 0.3));
        geometry.push(rect);

        let mut star = nodebox_ops::star(Point::new(500.0, 200.0), 5, 50.0, 25.0);
        star.fill = Some(Color::rgb(0.2, 0.4, 0.9));
        geometry.push(star);

        let mut hex = nodebox_ops::polygon(Point::new(200.0, 350.0), 45.0, 6, true);
        hex.fill = Some(Color::rgb(0.8, 0.5, 0.2));
        geometry.push(hex);

        // Add a spiral of circles
        for i in 0..12 {
            let angle = i as f64 * 30.0 * std::f64::consts::PI / 180.0;
            let radius = 80.0 + i as f64 * 10.0;
            let x = 400.0 + radius * angle.cos();
            let y = 400.0 + radius * angle.sin();

            let hue = i as f64 / 12.0;
            let color = hsb_to_rgb(hue, 0.8, 0.9);

            let mut dot = GeoPath::ellipse(x, y, 15.0, 15.0);
            dot.fill = Some(color);
            geometry.push(dot);
        }

        // Create a demo node library
        let library = Self::create_demo_library();

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

    /// Create a demo node library with some example nodes.
    fn create_demo_library() -> NodeLibrary {
        let mut library = NodeLibrary::new("demo");

        // Create demo nodes (positions in grid units, vertical flow: top to bottom)
        // Layout:
        //   ellipse1 (0,0)     rect1 (4,0)
        //      ↓                  ↓
        //   colorize1 (0,1)   colorize2 (4,1)
        //         ↓              ↓
        //           merge1 (2,2)
        let ellipse_node = Node::new("ellipse1")
            .with_prototype("corevector.ellipse")
            .with_function("corevector/ellipse")
            .with_category("geometry")
            .with_position(0.0, 0.0)
            .with_input(Port::float("x", 200.0))
            .with_input(Port::float("y", 200.0))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0));

        let colorize_node = Node::new("colorize1")
            .with_prototype("corevector.colorize")
            .with_function("corevector/colorize")
            .with_category("color")
            .with_position(0.0, 1.0)
            .with_input(Port::geometry("shape"))
            .with_input(Port::color("fill", Color::rgb(0.9, 0.2, 0.2)))
            .with_input(Port::color("stroke", Color::BLACK))
            .with_input(Port::float("strokeWidth", 1.0));

        let rect_node = Node::new("rect1")
            .with_prototype("corevector.rect")
            .with_function("corevector/rect")
            .with_category("geometry")
            .with_position(4.0, 0.0)
            .with_input(Port::float("x", 350.0))
            .with_input(Port::float("y", 200.0))
            .with_input(Port::float("width", 80.0))
            .with_input(Port::float("height", 80.0));

        let colorize2_node = Node::new("colorize2")
            .with_prototype("corevector.colorize")
            .with_function("corevector/colorize")
            .with_category("color")
            .with_position(4.0, 1.0)
            .with_input(Port::geometry("shape"))
            .with_input(Port::color("fill", Color::rgb(0.2, 0.8, 0.3)))
            .with_input(Port::color("stroke", Color::BLACK))
            .with_input(Port::float("strokeWidth", 1.0));

        let merge_node = Node::new("merge1")
            .with_prototype("corevector.merge")
            .with_function("corevector/merge")
            .with_category("geometry")
            .with_position(2.0, 2.0)
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

/// Convert HSB to RGB color.
fn hsb_to_rgb(h: f64, s: f64, b: f64) -> Color {
    let h = h * 6.0;
    let i = h.floor() as i32;
    let f = h - i as f64;
    let p = b * (1.0 - s);
    let q = b * (1.0 - s * f);
    let t = b * (1.0 - s * (1.0 - f));

    let (r, g, b) = match i % 6 {
        0 => (b, t, p),
        1 => (q, b, p),
        2 => (p, b, t),
        3 => (p, q, b),
        4 => (t, p, b),
        _ => (b, p, q),
    };

    Color::rgb(r, g, b)
}
