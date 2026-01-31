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
            .with_input(Port::point("position", nodebox_core::geometry::Point::new(-100.0, -50.0)))
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
            .with_input(Port::point("position", nodebox_core::geometry::Point::new(100.0, 50.0)))
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
        // Parse the .ndbx file
        let mut library = nodebox_ndbx::parse_file(path).map_err(|e| e.to_string())?;

        // Ensure all nodes have their default ports populated
        populate_default_ports(&mut library.root);

        // Update state
        self.library = library;
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        self.selected_node = None;

        // Evaluate the network
        self.geometry = eval::evaluate_network(&self.library);

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

/// Populate default ports for nodes based on their prototype.
///
/// When loading .ndbx files, only non-default port values are stored.
/// This function adds the missing default ports that nodes need for
/// connections to work properly.
pub fn populate_default_ports(node: &mut Node) {
    // Recursively process children first
    for child in &mut node.children {
        populate_default_ports(child);
    }

    // Add default ports based on prototype
    if let Some(ref proto) = node.prototype {
        match proto.as_str() {
            // Geometry generators - port names match corevector.ndbx library
            "corevector.ellipse" => {
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "width", || Port::float("width", 100.0));
                ensure_port(node, "height", || Port::float("height", 100.0));
            }
            "corevector.rect" => {
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "width", || Port::float("width", 100.0));
                ensure_port(node, "height", || Port::float("height", 100.0));
                ensure_port(node, "roundness", || Port::point("roundness", nodebox_core::geometry::Point::ZERO));
            }
            "corevector.line" => {
                ensure_port(node, "point1", || Port::point("point1", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "point2", || Port::point("point2", nodebox_core::geometry::Point::new(100.0, 100.0)));
                ensure_port(node, "points", || Port::int("points", 2));
            }
            "corevector.polygon" => {
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "radius", || Port::float("radius", 100.0));
                ensure_port(node, "sides", || Port::int("sides", 3));
                ensure_port(node, "align", || Port::boolean("align", false));
            }
            "corevector.star" => {
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "points", || Port::int("points", 20));
                ensure_port(node, "outer", || Port::float("outer", 200.0));
                ensure_port(node, "inner", || Port::float("inner", 100.0));
            }
            "corevector.arc" => {
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "width", || Port::float("width", 100.0));
                ensure_port(node, "height", || Port::float("height", 100.0));
                ensure_port(node, "start_angle", || Port::float("start_angle", 0.0));
                ensure_port(node, "degrees", || Port::float("degrees", 45.0));
                ensure_port(node, "type", || Port::string("type", "pie"));
            }
            // Filters
            "corevector.colorize" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "fill", || Port::color("fill", Color::WHITE));
                ensure_port(node, "stroke", || Port::color("stroke", Color::BLACK));
                ensure_port(node, "strokeWidth", || Port::float("strokeWidth", 1.0));
            }
            "corevector.translate" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "translate", || Port::point("translate", nodebox_core::geometry::Point::ZERO));
            }
            "corevector.rotate" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "angle", || Port::float("angle", 0.0));
                ensure_port(node, "origin", || Port::point("origin", nodebox_core::geometry::Point::ZERO));
            }
            "corevector.scale" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "scale", || Port::point("scale", nodebox_core::geometry::Point::new(100.0, 100.0)));
                ensure_port(node, "origin", || Port::point("origin", nodebox_core::geometry::Point::ZERO));
            }
            "corevector.copy" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "copies", || Port::int("copies", 1));
                ensure_port(node, "order", || Port::string("order", "tsr"));
                ensure_port(node, "translate", || Port::point("translate", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "rotate", || Port::float("rotate", 0.0));
                ensure_port(node, "scale", || Port::point("scale", nodebox_core::geometry::Point::new(100.0, 100.0)));
            }
            "corevector.align" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "halign", || Port::string("halign", "center"));
                ensure_port(node, "valign", || Port::string("valign", "middle"));
            }
            "corevector.fit" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "width", || Port::float("width", 300.0));
                ensure_port(node, "height", || Port::float("height", 300.0));
                ensure_port(node, "keep_proportions", || Port::boolean("keep_proportions", true));
            }
            "corevector.resample" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "points", || Port::int("points", 10));
            }
            "corevector.wiggle" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "scope", || Port::string("scope", "points"));
                ensure_port(node, "offset", || Port::point("offset", nodebox_core::geometry::Point::new(10.0, 10.0)));
                ensure_port(node, "seed", || Port::int("seed", 0));
            }
            // Combine operations
            "corevector.merge" | "corevector.combine" => {
                ensure_port(node, "shapes", || Port::geometry("shapes"));
            }
            "list.combine" => {
                ensure_port(node, "list1", || Port::geometry("list1"));
                ensure_port(node, "list2", || Port::geometry("list2"));
                ensure_port(node, "list3", || Port::geometry("list3"));
                ensure_port(node, "list4", || Port::geometry("list4"));
                ensure_port(node, "list5", || Port::geometry("list5"));
            }
            // Grid
            "corevector.grid" => {
                ensure_port(node, "columns", || Port::int("columns", 10));
                ensure_port(node, "rows", || Port::int("rows", 10));
                ensure_port(node, "width", || Port::float("width", 300.0));
                ensure_port(node, "height", || Port::float("height", 300.0));
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
            }
            // Connect
            "corevector.connect" => {
                ensure_port(node, "points", || Port::geometry("points"));
                ensure_port(node, "closed", || Port::boolean("closed", false));
            }
            // Point
            "corevector.point" | "corevector.makePoint" => {
                ensure_port(node, "x", || Port::float("x", 0.0));
                ensure_port(node, "y", || Port::float("y", 0.0));
            }
            _ => {}
        }
    }
}

/// Ensure a port exists on a node, adding it with the default if missing.
fn ensure_port<F>(node: &mut Node, name: &str, default: F)
where
    F: FnOnce() -> Port,
{
    if node.input(name).is_none() {
        node.inputs.push(default());
    }
}

