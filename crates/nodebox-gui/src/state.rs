//! Application state management.

use std::collections::HashMap;
use std::path::{Path, PathBuf};
use nodebox_core::geometry::{Path as GeoPath, Color};
use nodebox_core::node::{Node, NodeLibrary, MenuItem, Port, PortRange, Widget};

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

    /// Per-node error messages (node_name -> error message).
    pub node_errors: HashMap<String, String>,
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
        let library = Self::create_demo_library();

        Self {
            current_file: None,
            dirty: false,
            show_about: false,
            geometry: Vec::new(), // Render worker will populate
            selected_node: None,
            background_color: Color::WHITE,
            library,
            node_errors: HashMap::new(),
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
        self.selected_node = None;
        self.node_errors.clear();
    }

    /// Load a file.
    ///
    /// Note: Geometry is cleared - the render worker will evaluate with
    /// the proper Port and populate it.
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
        self.geometry.clear(); // Render worker will populate
        self.node_errors.clear();

        Ok(())
    }

    /// Save the current document.
    pub fn save_file(&mut self, path: &Path) -> Result<(), String> {
        nodebox_ndbx::serialize_to_file(&self.library, path)
            .map_err(|e| e.to_string())?;
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        Ok(())
    }

    /// Export to SVG.
    /// Uses document width/height and centered coordinate system.
    pub fn export_svg(&self, path: &Path, width: f64, height: f64) -> Result<(), String> {
        let options = nodebox_svg::SvgOptions::new(width, height)
            .with_centered(true)
            .with_background(Some(self.background_color));
        let svg = nodebox_svg::render_to_svg_with_options(&self.geometry, &options);
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
                ensure_port(node, "type", || Port::menu("type", "pie", vec![
                    MenuItem::new("pie", "Pie"),
                    MenuItem::new("chord", "Chord"),
                    MenuItem::new("open", "Open"),
                ]));
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
                ensure_port(node, "order", || Port::menu("order", "tsr", vec![
                    MenuItem::new("srt", "Scale Rot Trans"),
                    MenuItem::new("str", "Scale Trans Rot"),
                    MenuItem::new("rst", "Rot Scale Trans"),
                    MenuItem::new("rtr", "Rot Trans Scale"),
                    MenuItem::new("tsr", "Trans Scale Rot"),
                    MenuItem::new("trs", "Trans Rot Scale"),
                ]));
                ensure_port(node, "translate", || Port::point("translate", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "rotate", || Port::float("rotate", 0.0));
                ensure_port(node, "scale", || Port::point("scale", nodebox_core::geometry::Point::new(100.0, 100.0)));
            }
            "corevector.align" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "halign", || Port::menu("halign", "center", vec![
                    MenuItem::new("none", "No Change"),
                    MenuItem::new("left", "Left"),
                    MenuItem::new("center", "Center"),
                    MenuItem::new("right", "Right"),
                ]));
                ensure_port(node, "valign", || Port::menu("valign", "middle", vec![
                    MenuItem::new("none", "No Change"),
                    MenuItem::new("top", "Top"),
                    MenuItem::new("middle", "Middle"),
                    MenuItem::new("bottom", "Bottom"),
                ]));
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
                ensure_port(node, "method", || Port::menu("method", "length", vec![
                    MenuItem::new("length", "By length"),
                    MenuItem::new("amount", "By amount"),
                ]));
                ensure_port(node, "length", || Port::float("length", 10.0));
                ensure_port(node, "points", || Port::int("points", 10));
                ensure_port(node, "per_contour", || Port::boolean("per_contour", false));
            }
            "corevector.wiggle" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "scope", || Port::menu("scope", "points", vec![
                    MenuItem::new("points", "Points"),
                    MenuItem::new("contours", "Contours"),
                    MenuItem::new("paths", "Paths"),
                ]));
                ensure_port(node, "offset", || Port::point("offset", nodebox_core::geometry::Point::new(10.0, 10.0)));
                ensure_port(node, "seed", || Port::int("seed", 0));
            }
            // Combine operations
            "corevector.merge" | "corevector.combine" => {
                // shapes port expects a list of shapes, not individual values to iterate over
                ensure_port(node, "shapes", || Port::geometry("shapes").with_port_range(PortRange::List));
            }
            "corevector.group" => {
                ensure_port(node, "shapes", || Port::geometry("shapes").with_port_range(PortRange::List));
            }
            "corevector.stack" => {
                ensure_port(node, "shapes", || Port::geometry("shapes").with_port_range(PortRange::List));
                ensure_port(node, "direction", || Port::menu("direction", "e", vec![
                    MenuItem::new("n", "North"),
                    MenuItem::new("e", "East"),
                    MenuItem::new("s", "South"),
                    MenuItem::new("w", "West"),
                ]));
                ensure_port(node, "margin", || Port::float("margin", 5.0));
            }
            "corevector.sort" => {
                ensure_port(node, "shapes", || Port::geometry("shapes").with_port_range(PortRange::List));
                ensure_port(node, "order_by", || Port::menu("order_by", "none", vec![
                    MenuItem::new("none", "No Change"),
                    MenuItem::new("x", "X"),
                    MenuItem::new("y", "Y"),
                    MenuItem::new("angle", "Angle to Point"),
                    MenuItem::new("distance", "Distance to Point"),
                ]));
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
            }
            "list.combine" => {
                // list.combine ports should be LIST-range so empty inputs don't block evaluation
                ensure_port(node, "list1", || Port::geometry("list1").with_port_range(PortRange::List));
                ensure_port(node, "list2", || Port::geometry("list2").with_port_range(PortRange::List));
                ensure_port(node, "list3", || Port::geometry("list3").with_port_range(PortRange::List));
                ensure_port(node, "list4", || Port::geometry("list4").with_port_range(PortRange::List));
                ensure_port(node, "list5", || Port::geometry("list5").with_port_range(PortRange::List));
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
                // points port expects a list of points, not individual values to iterate over
                ensure_port(node, "points", || Port::geometry("points").with_port_range(PortRange::List));
                ensure_port(node, "closed", || Port::boolean("closed", false));
            }
            // Point
            "corevector.point" | "corevector.makePoint" => {
                ensure_port(node, "x", || Port::float("x", 0.0));
                ensure_port(node, "y", || Port::float("y", 0.0));
            }
            "corevector.quad_curve" => {
                ensure_port(node, "point1", || Port::point("point1", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "point2", || Port::point("point2", nodebox_core::geometry::Point::new(100.0, 0.0)));
                ensure_port(node, "t", || Port::float("t", 50.0));
                ensure_port(node, "distance", || Port::float("distance", 50.0));
            }
            "corevector.compound" => {
                ensure_port(node, "shape1", || Port::geometry("shape1"));
                ensure_port(node, "shape2", || Port::geometry("shape2"));
                ensure_port(node, "function", || Port::menu("function", "united", vec![
                    MenuItem::new("united", "Union"),
                    MenuItem::new("subtracted", "Difference"),
                    MenuItem::new("intersected", "Intersection"),
                ]));
                ensure_port(node, "invert_difference", || Port::boolean("invert_difference", false));
            }
            "corevector.link" => {
                ensure_port(node, "shape1", || Port::geometry("shape1"));
                ensure_port(node, "shape2", || Port::geometry("shape2"));
                ensure_port(node, "orientation", || Port::menu("orientation", "horizontal", vec![
                    MenuItem::new("horizontal", "Horizontal"),
                    MenuItem::new("vertical", "Vertical"),
                ]));
            }
            "corevector.textpath" => {
                ensure_port(node, "text", || Port::string("text", "hello"));
                ensure_port(node, "font_name", || Port::string("font_name", "Verdana").with_widget(Widget::Font));
                ensure_port(node, "font_size", || Port::float("font_size", 24.0));
                ensure_port(node, "align", || Port::menu("align", "CENTER", vec![
                    MenuItem::new("LEFT", "Left"),
                    MenuItem::new("CENTER", "Center"),
                    MenuItem::new("RIGHT", "Right"),
                    MenuItem::new("JUSTIFY", "Justify"),
                ]));
                ensure_port(node, "position", || Port::point("position", nodebox_core::geometry::Point::ZERO));
                ensure_port(node, "width", || Port::float("width", 0.0));
            }
            "corevector.delete" => {
                ensure_port(node, "shape", || Port::geometry("shape"));
                ensure_port(node, "bounding", || Port::geometry("bounding"));
                ensure_port(node, "scope", || Port::menu("scope", "points", vec![
                    MenuItem::new("points", "Points"),
                    MenuItem::new("paths", "Paths"),
                ]));
                ensure_port(node, "operation", || Port::menu("operation", "selected", vec![
                    MenuItem::new("selected", "Delete Selected"),
                    MenuItem::new("non-selected", "Delete Non-selected"),
                ]));
            }
            "corevector.distribute" => {
                ensure_port(node, "shapes", || Port::geometry("shapes").with_port_range(PortRange::List));
                ensure_port(node, "horizontal", || Port::menu("horizontal", "none", vec![
                    MenuItem::new("none", "No Change"),
                    MenuItem::new("left", "Left"),
                    MenuItem::new("center", "Center"),
                    MenuItem::new("right", "Right"),
                ]));
                ensure_port(node, "vertical", || Port::menu("vertical", "none", vec![
                    MenuItem::new("none", "No Change"),
                    MenuItem::new("top", "Top"),
                    MenuItem::new("middle", "Middle"),
                    MenuItem::new("bottom", "Bottom"),
                ]));
            }
            "corevector.shape_on_path" => {
                ensure_port(node, "shape", || Port::geometry("shape").with_port_range(PortRange::List));
                ensure_port(node, "path", || Port::geometry("path"));
                ensure_port(node, "amount", || Port::int("amount", 1));
                ensure_port(node, "alignment", || Port::menu("alignment", "leading", vec![
                    MenuItem::new("leading", "Leading"),
                    MenuItem::new("trailing", "Trailing"),
                    MenuItem::new("distributed", "Distributed"),
                ]));
                ensure_port(node, "spacing", || Port::float("spacing", 20.0));
                ensure_port(node, "margin", || Port::float("margin", 0.0));
                ensure_port(node, "baseline_offset", || Port::float("baseline_offset", 0.0));
            }
            "corevector.text_on_path" => {
                ensure_port(node, "text", || Port::string("text", "text following a path"));
                ensure_port(node, "path", || Port::geometry("path"));
                ensure_port(node, "font_name", || Port::string("font_name", "Verdana").with_widget(Widget::Font));
                ensure_port(node, "font_size", || Port::float("font_size", 24.0));
                ensure_port(node, "alignment", || Port::menu("alignment", "leading", vec![
                    MenuItem::new("leading", "Leading"),
                    MenuItem::new("trailing", "Trailing"),
                ]));
                ensure_port(node, "margin", || Port::float("margin", 0.0));
                ensure_port(node, "baseline_offset", || Port::float("baseline_offset", 0.0));
            }
            _ => {}
        }
    }
}

/// Ensure a port exists on a node with the correct widget and menu items.
///
/// If the port doesn't exist, it's created with the default.
/// If the port exists but has Widget::String and the default has Widget::Menu,
/// update the widget type and menu items (preserving the existing value).
fn ensure_port<F>(node: &mut Node, name: &str, default: F)
where
    F: FnOnce() -> Port,
{
    if let Some(existing) = node.inputs.iter_mut().find(|p| p.name == name) {
        // Port exists - check if we need to update widget/menu items
        let default_port = default();
        if existing.widget == Widget::String && default_port.widget == Widget::Menu {
            // Update to menu widget and add menu items
            existing.widget = Widget::Menu;
            existing.menu_items = default_port.menu_items;
        }
    } else {
        // Port doesn't exist - add it
        node.inputs.push(default());
    }
}

