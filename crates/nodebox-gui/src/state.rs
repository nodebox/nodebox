//! Application state management.

use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use nodebox_core::geometry::{Path as GeoPath, Color, Point};
use nodebox_core::node::{Node, NodeLibrary, MenuItem, Port, PortRange, Widget};
use crate::eval::NodeOutput;

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
        self.library = Arc::new(library);
        self.background_color = self.library.background_color();
        self.current_file = Some(path.to_path_buf());
        self.dirty = false;
        self.selected_node = None;
        self.geometry.clear(); // Render worker will populate
        self.node_output = NodeOutput::None;
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
            // ========================
            // Math nodes
            // ========================
            "math.number" => {
                ensure_port(node, "value", || Port::float("value", 0.0));
            }
            "math.integer" => {
                ensure_port(node, "value", || Port::int("value", 0));
            }
            "math.boolean" => {
                ensure_port(node, "value", || Port::boolean("value", false));
            }
            "math.add" | "math.subtract" | "math.multiply" | "math.divide" | "math.mod" | "math.pow" => {
                ensure_port(node, "value1", || Port::float("value1", 0.0));
                ensure_port(node, "value2", || Port::float("value2", 0.0));
            }
            "math.negate" | "math.abs" | "math.sqrt" | "math.log" | "math.ceil" | "math.floor" | "math.round" | "math.sin" | "math.cos" | "math.even" | "math.odd" => {
                ensure_port(node, "value", || Port::float("value", 0.0));
            }
            "math.radians" => {
                ensure_port(node, "degrees", || Port::float("degrees", 0.0));
            }
            "math.degrees" => {
                ensure_port(node, "radians", || Port::float("radians", 0.0));
            }
            "math.compare" => {
                ensure_port(node, "value1", || Port::float("value1", 0.0));
                ensure_port(node, "value2", || Port::float("value2", 0.0));
                ensure_port(node, "comparator", || Port::menu("comparator", "<", vec![
                    MenuItem::new("<", "Less Than"),
                    MenuItem::new(">", "Greater Than"),
                    MenuItem::new("<=", "Less or Equal"),
                    MenuItem::new(">=", "Greater or Equal"),
                    MenuItem::new("==", "Equal"),
                    MenuItem::new("!=", "Not Equal"),
                ]));
            }
            "math.logical" => {
                ensure_port(node, "boolean1", || Port::boolean("boolean1", false));
                ensure_port(node, "boolean2", || Port::boolean("boolean2", false));
                ensure_port(node, "comparator", || Port::menu("comparator", "or", vec![
                    MenuItem::new("or", "Or"),
                    MenuItem::new("and", "And"),
                    MenuItem::new("xor", "Xor"),
                ]));
            }
            "math.angle" | "math.distance" => {
                ensure_port(node, "point1", || Port::point("point1", Point::ZERO));
                ensure_port(node, "point2", || Port::point("point2", Point::new(100.0, 100.0)));
            }
            "math.coordinates" => {
                ensure_port(node, "position", || Port::point("position", Point::ZERO));
                ensure_port(node, "angle", || Port::float("angle", 0.0));
                ensure_port(node, "distance", || Port::float("distance", 100.0));
            }
            "math.reflect" => {
                ensure_port(node, "point1", || Port::point("point1", Point::ZERO));
                ensure_port(node, "point2", || Port::point("point2", Point::new(100.0, 100.0)));
                ensure_port(node, "angle", || Port::float("angle", 0.0));
                ensure_port(node, "distance", || Port::float("distance", 1.0));
            }
            "math.sum" | "math.average" | "math.max" | "math.min" | "math.running_total" => {
                ensure_port(node, "values", || Port::float("values", 0.0).with_port_range(PortRange::List));
            }
            "math.convert_range" => {
                ensure_port(node, "value", || Port::float("value", 50.0));
                ensure_port(node, "source_start", || Port::float("source_start", 0.0));
                ensure_port(node, "source_end", || Port::float("source_end", 100.0));
                ensure_port(node, "target_start", || Port::float("target_start", 0.0));
                ensure_port(node, "target_end", || Port::float("target_end", 1.0));
                ensure_port(node, "method", || Port::menu("method", "clamp", vec![
                    MenuItem::new("clamp", "Clamp"),
                    MenuItem::new("wrap", "Wrap"),
                    MenuItem::new("mirror", "Mirror"),
                    MenuItem::new("ignore", "Ignore"),
                ]));
            }
            "math.wave" => {
                ensure_port(node, "min", || Port::float("min", 0.0));
                ensure_port(node, "max", || Port::float("max", 100.0));
                ensure_port(node, "period", || Port::float("period", 60.0));
                ensure_port(node, "offset", || Port::float("offset", 0.0));
                ensure_port(node, "type", || Port::menu("type", "sine", vec![
                    MenuItem::new("sine", "Sine"),
                    MenuItem::new("square", "Square"),
                    MenuItem::new("triangle", "Triangle"),
                    MenuItem::new("sawtooth", "Sawtooth"),
                ]));
            }
            "math.make_numbers" => {
                ensure_port(node, "string", || Port::string("string", "11;22;33"));
                ensure_port(node, "separator", || Port::string("separator", ";"));
            }
            "math.random_numbers" => {
                ensure_port(node, "amount", || Port::int("amount", 10));
                ensure_port(node, "start", || Port::float("start", 0.0));
                ensure_port(node, "end", || Port::float("end", 100.0));
                ensure_port(node, "seed", || Port::int("seed", 0));
            }
            "math.sample" => {
                ensure_port(node, "amount", || Port::int("amount", 10));
                ensure_port(node, "start", || Port::float("start", 0.0));
                ensure_port(node, "end", || Port::float("end", 100.0));
            }
            "math.range" => {
                ensure_port(node, "start", || Port::float("start", 0.0));
                ensure_port(node, "end", || Port::float("end", 10.0));
                ensure_port(node, "step", || Port::float("step", 1.0));
            }

            // ========================
            // String nodes
            // ========================
            "string.string" => {
                ensure_port(node, "value", || Port::string("value", ""));
            }
            "string.length" | "string.word_count" | "string.trim" | "string.characters" => {
                ensure_port(node, "string", || Port::string("string", ""));
            }
            "string.concatenate" => {
                ensure_port(node, "string1", || Port::string("string1", ""));
                ensure_port(node, "string2", || Port::string("string2", ""));
                ensure_port(node, "string3", || Port::string("string3", ""));
                ensure_port(node, "string4", || Port::string("string4", ""));
                ensure_port(node, "string5", || Port::string("string5", ""));
                ensure_port(node, "string6", || Port::string("string6", ""));
                ensure_port(node, "string7", || Port::string("string7", ""));
            }
            "string.change_case" => {
                ensure_port(node, "string", || Port::string("string", "default"));
                ensure_port(node, "method", || Port::menu("method", "uppercase", vec![
                    MenuItem::new("lowercase", "Lower Case"),
                    MenuItem::new("uppercase", "Upper Case"),
                    MenuItem::new("titlecase", "Title Case"),
                ]));
            }
            "string.format_number" => {
                ensure_port(node, "value", || Port::float("value", 0.0));
                ensure_port(node, "format", || Port::string("format", "%.2f"));
            }
            "string.replace" => {
                ensure_port(node, "string", || Port::string("string", ""));
                ensure_port(node, "old", || Port::string("old", ""));
                ensure_port(node, "new", || Port::string("new", ""));
            }
            "string.sub_string" => {
                ensure_port(node, "string", || Port::string("string", ""));
                ensure_port(node, "start", || Port::int("start", 0));
                ensure_port(node, "end", || Port::int("end", 4));
                ensure_port(node, "end_offset", || Port::boolean("end_offset", false));
            }
            "string.character_at" => {
                ensure_port(node, "string", || Port::string("string", ""));
                ensure_port(node, "index", || Port::int("index", 0));
            }
            "string.as_binary_string" => {
                ensure_port(node, "string", || Port::string("string", ""));
                ensure_port(node, "digit_separator", || Port::string("digit_separator", ""));
                ensure_port(node, "byte_separator", || Port::string("byte_separator", " "));
            }
            "string.as_binary_list" | "string.as_number_list" => {
                ensure_port(node, "string", || Port::string("string", ""));
            }
            "string.contains" => {
                ensure_port(node, "string", || Port::string("string", ""));
                ensure_port(node, "contains", || Port::string("contains", ""));
            }
            "string.ends_with" => {
                ensure_port(node, "string", || Port::string("string", ""));
                ensure_port(node, "ends_with", || Port::string("ends_with", ""));
            }
            "string.starts_with" => {
                ensure_port(node, "string", || Port::string("string", ""));
                ensure_port(node, "starts_with", || Port::string("starts_with", ""));
            }
            "string.equals" => {
                ensure_port(node, "string", || Port::string("string", ""));
                ensure_port(node, "equals", || Port::string("equals", ""));
                ensure_port(node, "case_sensitive", || Port::boolean("case_sensitive", false));
            }
            "string.make_strings" => {
                ensure_port(node, "string", || Port::string("string", "Alpha;Beta;Gamma"));
                ensure_port(node, "separator", || Port::string("separator", ";"));
            }
            "string.random_character" => {
                ensure_port(node, "characters", || Port::string("characters", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"));
                ensure_port(node, "amount", || Port::int("amount", 10));
                ensure_port(node, "seed", || Port::int("seed", 0));
            }

            // ========================
            // List nodes
            // ========================
            "list.count" | "list.first" | "list.second" | "list.last" | "list.rest" | "list.reverse" | "list.distinct" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
            }
            "list.slice" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
                ensure_port(node, "start_index", || Port::int("start_index", 0));
                ensure_port(node, "size", || Port::int("size", 10));
                ensure_port(node, "invert", || Port::boolean("invert", false));
            }
            "list.shift" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
                ensure_port(node, "amount", || Port::int("amount", 1));
            }
            "list.repeat" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
                ensure_port(node, "amount", || Port::int("amount", 1));
                ensure_port(node, "per_item", || Port::boolean("per_item", false));
            }
            "list.sort" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
                ensure_port(node, "key", || Port::string("key", ""));
            }
            "list.shuffle" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
                ensure_port(node, "seed", || Port::int("seed", 0));
            }
            "list.pick" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
                ensure_port(node, "amount", || Port::int("amount", 5));
                ensure_port(node, "seed", || Port::int("seed", 0));
            }
            "list.cull" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
                ensure_port(node, "booleans", || Port::boolean("booleans", true).with_port_range(PortRange::List));
            }
            "list.take_every" => {
                ensure_port(node, "list", || Port::geometry("list").with_port_range(PortRange::List));
                ensure_port(node, "n", || Port::int("n", 1));
            }
            "list.switch" => {
                ensure_port(node, "input1", || Port::geometry("input1").with_port_range(PortRange::List));
                ensure_port(node, "input2", || Port::geometry("input2").with_port_range(PortRange::List));
                ensure_port(node, "index", || Port::int("index", 0));
            }

            // ========================
            // Color nodes
            // ========================
            "color.color" => {
                ensure_port(node, "color", || Port::color("color", Color::BLACK));
            }
            "color.gray_color" => {
                ensure_port(node, "gray", || Port::float("gray", 0.0));
                ensure_port(node, "alpha", || Port::float("alpha", 255.0));
                ensure_port(node, "range", || Port::float("range", 255.0));
            }
            "color.rgb_color" => {
                ensure_port(node, "red", || Port::float("red", 0.0));
                ensure_port(node, "green", || Port::float("green", 0.0));
                ensure_port(node, "blue", || Port::float("blue", 0.0));
                ensure_port(node, "alpha", || Port::float("alpha", 255.0));
                ensure_port(node, "range", || Port::float("range", 255.0));
            }
            "color.hsb_color" => {
                ensure_port(node, "hue", || Port::float("hue", 0.0));
                ensure_port(node, "saturation", || Port::float("saturation", 0.0));
                ensure_port(node, "brightness", || Port::float("brightness", 0.0));
                ensure_port(node, "alpha", || Port::float("alpha", 255.0));
                ensure_port(node, "range", || Port::float("range", 255.0));
            }

            // ========================
            // Network nodes
            // ========================
            "network.http_get" => {
                ensure_port(node, "url", || Port::string("url", ""));
            }
            "network.encode_url" => {
                ensure_port(node, "value", || Port::string("value", ""));
            }

            // ========================
            // Data nodes
            // ========================
            "data.import_text" | "data.import_csv" => {
                ensure_port(node, "file", || Port::string("file", "").with_widget(Widget::File));
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

