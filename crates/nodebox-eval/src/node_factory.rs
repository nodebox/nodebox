//! Node creation from templates.

use nodebox_core::geometry::{Color, Point};
use nodebox_core::node::{MenuItem, Node, NodeLibrary, Port, PortRange, PortType, Widget};
use crate::node_templates::NodeTemplate;

/// Check if the first input port of a node template is directly compatible
/// with the given output type. Uses strict rules (no string conversion,
/// no number->point promotion) -- only same-type, List wildcard, and Int<->Float.
pub fn template_has_compatible_input(template: &NodeTemplate, output_type: &PortType) -> bool {
    let temp_lib = NodeLibrary::new("_temp");
    let node = create_node_from_template(template, &temp_lib, Point::ZERO);
    node.inputs
        .first()
        .is_some_and(|port| is_directly_compatible(output_type, &port.port_type))
}

/// Strict type compatibility for dialog filtering.
/// Only allows: same type, List wildcard, and Int<->Float.
/// Excludes the broad everything->String and Number->Point rules.
fn is_directly_compatible(output_type: &PortType, input_type: &PortType) -> bool {
    if output_type == input_type {
        return true;
    }
    // List input accepts any type
    if matches!(input_type, PortType::List) {
        return true;
    }
    // List output connects to any input
    if matches!(output_type, PortType::List) {
        return true;
    }
    // Int <-> Float
    if matches!(output_type, PortType::Int) && matches!(input_type, PortType::Float) {
        return true;
    }
    if matches!(output_type, PortType::Float) && matches!(input_type, PortType::Int) {
        return true;
    }
    false
}

/// Create a new node from a template.
pub fn create_node_from_template(template: &NodeTemplate, library: &NodeLibrary, position: Point) -> Node {
    // Generate unique name
    let base_name = template.name;
    let name = library.root.unique_child_name(base_name);

    // Create node with appropriate ports based on prototype
    // Derive function from prototype: "corevector.ellipse" -> "corevector/ellipse"
    let function = template.prototype.replacen('.', "/", 1);
    let mut node = Node::new(&name)
        .with_prototype(template.prototype)
        .with_function(function)
        .with_category(template.category)
        .with_position(position.x, position.y);

    // Add ports based on node type.
    // Every arm must call .with_output_type() -- the debug_assert below catches omissions.
    let mut matched = true;
    match template.name {
        // ========================
        // Geometry generators
        // ========================
        "ellipse" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_output_type(PortType::Geometry);
        }
        "rect" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_input(Port::point("roundness", Point::ZERO))
                .with_output_type(PortType::Geometry);
        }
        "line" => {
            node = node
                .with_input(Port::point("point1", Point::ZERO))
                .with_input(Port::point("point2", Point::new(100.0, 100.0)))
                .with_input(Port::int("points", 2).with_min(0.0))
                .with_output_type(PortType::Geometry);
        }
        "line_angle" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("angle", 0.0))
                .with_input(Port::float("distance", 100.0))
                .with_input(Port::int("points", 2).with_min(2.0))
                .with_output_type(PortType::Geometry);
        }
        "polygon" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("radius", 100.0))
                .with_input(Port::int("sides", 3).with_min(3.0))
                .with_input(Port::boolean("align", false))
                .with_output_type(PortType::Geometry);
        }
        "star" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::int("points", 20).with_min(2.0))
                .with_input(Port::float("outer", 200.0))
                .with_input(Port::float("inner", 100.0))
                .with_output_type(PortType::Geometry);
        }
        "arc" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_input(Port::float("start_angle", 0.0))
                .with_input(Port::float("degrees", 45.0))
                .with_input(Port::menu("type", "pie", vec![
                    MenuItem::new("pie", "Pie"),
                    MenuItem::new("chord", "Chord"),
                    MenuItem::new("open", "Open"),
                ]))
                .with_output_type(PortType::Geometry);
        }
        "quad_curve" => {
            node = node
                .with_input(Port::point("point1", Point::ZERO))
                .with_input(Port::point("point2", Point::new(100.0, 0.0)))
                .with_input(Port::float("t", 50.0))
                .with_input(Port::float("distance", 50.0))
                .with_output_type(PortType::Geometry);
        }
        "grid" => {
            node = node
                .with_input(Port::int("columns", 10).with_min(1.0))
                .with_input(Port::int("rows", 10).with_min(1.0))
                .with_input(Port::float("width", 300.0))
                .with_input(Port::float("height", 300.0))
                .with_input(Port::point("position", Point::ZERO))
                .with_output_type(PortType::Point)
                .with_output_range(PortRange::List);
        }
        "textpath" => {
            node = node
                .with_input(Port::string("text", "hello"))
                .with_input(Port::string("font_name", "Verdana").with_widget(Widget::Font))
                .with_input(Port::float("font_size", 24.0))
                .with_input(Port::menu("align", "CENTER", vec![
                    MenuItem::new("LEFT", "Left"),
                    MenuItem::new("CENTER", "Center"),
                    MenuItem::new("RIGHT", "Right"),
                    MenuItem::new("JUSTIFY", "Justify"),
                ]))
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 0.0))
                .with_output_type(PortType::Geometry);
        }
        "connect" => {
            node = node
                .with_input(Port::new("points", PortType::Point).with_port_range(PortRange::List))
                .with_input(Port::boolean("closed", false))
                .with_output_type(PortType::Geometry);
        }
        "make_point" => {
            node = node
                .with_input(Port::float("x", 0.0))
                .with_input(Port::float("y", 0.0))
                .with_output_type(PortType::Point);
        }
        "freehand" => {
            node = node
                .with_input(Port::string("path", ""))
                .with_output_type(PortType::Geometry);
        }
        // Combine / structural
        "merge" | "group" => {
            node = node
                .with_input(Port::geometry("shapes"))
                .with_output_type(PortType::Geometry);
        }
        "ungroup" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_output_type(PortType::Geometry);
        }
        "null" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_output_type(PortType::Geometry);
        }
        // Modify / filter geometry
        "resample" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::menu("method", "length", vec![
                    MenuItem::new("length", "By length"),
                    MenuItem::new("amount", "By amount"),
                ]))
                .with_input(Port::float("length", 10.0).with_min(1.0))
                .with_input(Port::int("points", 10).with_min(1.0))
                .with_input(Port::boolean("per_contour", false))
                .with_output_type(PortType::Geometry);
        }
        "wiggle" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::menu("scope", "points", vec![
                    MenuItem::new("points", "Points"),
                    MenuItem::new("contours", "Contours"),
                    MenuItem::new("paths", "Paths"),
                ]))
                .with_input(Port::point("offset", Point::new(10.0, 10.0)))
                .with_input(Port::int("seed", 0))
                .with_output_type(PortType::Geometry);
        }
        "align" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::menu("halign", "center", vec![
                    MenuItem::new("left", "Left"),
                    MenuItem::new("center", "Center"),
                    MenuItem::new("right", "Right"),
                ]))
                .with_input(Port::menu("valign", "middle", vec![
                    MenuItem::new("top", "Top"),
                    MenuItem::new("middle", "Middle"),
                    MenuItem::new("bottom", "Bottom"),
                ]))
                .with_output_type(PortType::Geometry);
        }
        "fit" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_input(Port::boolean("keep_proportions", true))
                .with_output_type(PortType::Geometry);
        }
        "fit_to" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::geometry("bounding"))
                .with_input(Port::boolean("keep_proportions", true))
                .with_output_type(PortType::Geometry);
        }
        "snap" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::float("distance", 10.0))
                .with_input(Port::float("strength", 1.0))
                .with_input(Port::point("position", Point::ZERO))
                .with_output_type(PortType::Geometry);
        }
        "centroid" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_output_type(PortType::Point);
        }
        "point_on_path" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::float("t", 0.0))
                .with_output_type(PortType::Point);
        }
        "scatter" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::int("amount", 10))
                .with_input(Port::int("seed", 0))
                .with_output_type(PortType::Point)
                .with_output_range(PortRange::List);
        }
        "delete" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::geometry("bounding"))
                .with_input(Port::menu("scope", "points", vec![
                    MenuItem::new("points", "Points"),
                    MenuItem::new("paths", "Paths"),
                ]))
                .with_input(Port::menu("operation", "selected", vec![
                    MenuItem::new("selected", "Selected"),
                    MenuItem::new("non-selected", "Non-selected"),
                ]))
                .with_output_type(PortType::Geometry);
        }
        "sort" => {
            node = node
                .with_input(Port::geometry("shapes"))
                .with_input(Port::menu("order_by", "x", vec![
                    MenuItem::new("x", "X"),
                    MenuItem::new("y", "Y"),
                    MenuItem::new("distance", "Distance"),
                    MenuItem::new("angle", "Angle"),
                ]))
                .with_input(Port::point("position", Point::ZERO))
                .with_output_type(PortType::Geometry);
        }
        "stack" => {
            node = node
                .with_input(Port::geometry("shapes"))
                .with_input(Port::menu("direction", "east", vec![
                    MenuItem::new("east", "East"),
                    MenuItem::new("west", "West"),
                    MenuItem::new("north", "North"),
                    MenuItem::new("south", "South"),
                ]))
                .with_input(Port::float("margin", 0.0))
                .with_output_type(PortType::Geometry);
        }
        "link" => {
            node = node
                .with_input(Port::geometry("shape1"))
                .with_input(Port::geometry("shape2"))
                .with_input(Port::menu("orientation", "horizontal", vec![
                    MenuItem::new("horizontal", "Horizontal"),
                    MenuItem::new("vertical", "Vertical"),
                ]))
                .with_output_type(PortType::Geometry);
        }
        "shape_on_path" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::geometry("path"))
                .with_input(Port::int("amount", 1))
                .with_input(Port::float("spacing", 20.0))
                .with_input(Port::float("margin", 0.0))
                .with_output_type(PortType::Geometry);
        }
        // Import
        "import_svg" => {
            node = node
                .with_input(Port::string("file", "").with_widget(Widget::File))
                .with_input(Port::boolean("centered", true))
                .with_input(Port::point("position", Point::ZERO))
                .with_output_type(PortType::Geometry);
        }
        // ========================
        // Transform nodes
        // ========================
        "translate" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("translate", Point::ZERO))
                .with_output_type(PortType::Geometry);
        }
        "rotate" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::float("angle", 0.0))
                .with_input(Port::point("origin", Point::ZERO))
                .with_output_type(PortType::Geometry);
        }
        "scale" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("scale", Point::new(100.0, 100.0)))
                .with_input(Port::point("origin", Point::ZERO))
                .with_output_type(PortType::Geometry);
        }
        "copy" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::int("copies", 1).with_min(0.0))
                .with_input(Port::menu("order", "tsr", vec![
                    MenuItem::new("srt", "Scale Rot Trans"),
                    MenuItem::new("str", "Scale Trans Rot"),
                    MenuItem::new("rst", "Rot Scale Trans"),
                    MenuItem::new("rtr", "Rot Trans Scale"),
                    MenuItem::new("tsr", "Trans Scale Rot"),
                    MenuItem::new("trs", "Trans Rot Scale"),
                ]))
                .with_input(Port::point("translate", Point::ZERO))
                .with_input(Port::float("rotate", 0.0))
                .with_input(Port::point("scale", Point::new(100.0, 100.0)))
                .with_output_type(PortType::Geometry);
        }
        "distribute" => {
            node = node
                .with_input(Port::geometry("shapes").with_port_range(PortRange::List))
                .with_input(Port::menu("horizontal", "none", vec![
                    MenuItem::new("none", "No Change"),
                    MenuItem::new("left", "Left"),
                    MenuItem::new("center", "Center"),
                    MenuItem::new("right", "Right"),
                ]))
                .with_input(Port::menu("vertical", "none", vec![
                    MenuItem::new("none", "No Change"),
                    MenuItem::new("top", "Top"),
                    MenuItem::new("middle", "Middle"),
                    MenuItem::new("bottom", "Bottom"),
                ]))
                .with_output_range(PortRange::List);
        }
        "skew" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("skew", Point::ZERO))
                .with_input(Port::point("origin", Point::ZERO))
                .with_output_type(PortType::Geometry);
        }
        "reflect" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("angle", 0.0))
                .with_input(Port::boolean("keep_original", true))
                .with_output_type(PortType::Geometry);
        }
        // ========================
        // Color nodes
        // ========================
        "colorize" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::color("fill", Color::rgb(0.5, 0.5, 0.5)))
                .with_input(Port::color("stroke", Color::BLACK))
                .with_input(Port::float("strokeWidth", 1.0))
                .with_output_type(PortType::Geometry);
        }
        "rgb_color" => {
            node = node
                .with_input(Port::float("red", 0.0))
                .with_input(Port::float("green", 0.0))
                .with_input(Port::float("blue", 0.0))
                .with_input(Port::float("alpha", 255.0))
                .with_input(Port::float("range", 255.0))
                .with_output_type(PortType::Color);
        }
        "hsb_color" => {
            node = node
                .with_input(Port::float("hue", 0.0))
                .with_input(Port::float("saturation", 0.0))
                .with_input(Port::float("brightness", 0.0))
                .with_input(Port::float("alpha", 255.0))
                .with_input(Port::float("range", 255.0))
                .with_output_type(PortType::Color);
        }
        "gray_color" => {
            node = node
                .with_input(Port::float("gray", 0.0))
                .with_input(Port::float("alpha", 255.0))
                .with_input(Port::float("range", 255.0))
                .with_output_type(PortType::Color);
        }
        "color" => {
            node = node
                .with_input(Port::color("color", Color::BLACK))
                .with_output_type(PortType::Color);
        }
        // ========================
        // Math nodes
        // ========================
        "number" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_output_type(PortType::Float);
        }
        "integer" => {
            node = node
                .with_input(Port::int("value", 0))
                .with_output_type(PortType::Int);
        }
        "boolean" => {
            node = node
                .with_input(Port::boolean("value", false))
                .with_output_type(PortType::Boolean);
        }
        "add" | "subtract" | "multiply" | "divide" => {
            node = node
                .with_input(Port::float("value1", 0.0))
                .with_input(Port::float("value2", 0.0))
                .with_output_type(PortType::Float);
        }
        "mod" => {
            node = node
                .with_input(Port::float("value1", 0.0))
                .with_input(Port::float("value2", 1.0))
                .with_output_type(PortType::Float);
        }
        "negate" | "abs" | "sqrt" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_output_type(PortType::Float);
        }
        "pow" => {
            node = node
                .with_input(Port::float("value1", 0.0))
                .with_input(Port::float("value2", 2.0))
                .with_output_type(PortType::Float);
        }
        "log" => {
            node = node
                .with_input(Port::float("value", 1.0))
                .with_output_type(PortType::Float);
        }
        "ceil" | "floor" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_output_type(PortType::Float);
        }
        "round" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_output_type(PortType::Int);
        }
        "sin" | "cos" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_output_type(PortType::Float);
        }
        "radians" => {
            node = node
                .with_input(Port::float("degrees", 0.0))
                .with_output_type(PortType::Float);
        }
        "degrees" => {
            node = node
                .with_input(Port::float("radians", 0.0))
                .with_output_type(PortType::Float);
        }
        "pi" | "e" => {
            node = node.with_output_type(PortType::Float);
        }
        "even" | "odd" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_output_type(PortType::Boolean);
        }
        "compare" => {
            node = node
                .with_input(Port::float("value1", 0.0))
                .with_input(Port::float("value2", 0.0))
                .with_input(Port::menu("comparator", "<", vec![
                    MenuItem::new("<", "Less Than"),
                    MenuItem::new(">", "Greater Than"),
                    MenuItem::new("<=", "Less or Equal"),
                    MenuItem::new(">=", "Greater or Equal"),
                    MenuItem::new("==", "Equal"),
                    MenuItem::new("!=", "Not Equal"),
                ]))
                .with_output_type(PortType::Boolean);
        }
        "logical" => {
            node = node
                .with_input(Port::boolean("boolean1", false))
                .with_input(Port::boolean("boolean2", false))
                .with_input(Port::menu("comparator", "or", vec![
                    MenuItem::new("or", "Or"),
                    MenuItem::new("and", "And"),
                ]))
                .with_output_type(PortType::Boolean);
        }
        "angle" | "distance" => {
            node = node
                .with_input(Port::point("point1", Point::ZERO))
                .with_input(Port::point("point2", Point::new(100.0, 100.0)))
                .with_output_type(PortType::Float);
        }
        "coordinates" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("angle", 0.0))
                .with_input(Port::float("distance", 100.0))
                .with_output_type(PortType::Point);
        }
        "math_reflect" => {
            node = node
                .with_input(Port::point("point1", Point::ZERO))
                .with_input(Port::point("point2", Point::new(100.0, 100.0)))
                .with_input(Port::float("angle", 0.0))
                .with_input(Port::float("distance", 1.0))
                .with_output_type(PortType::Point);
        }
        "random_numbers" => {
            node = node
                .with_input(Port::int("amount", 10))
                .with_input(Port::float("start", 0.0))
                .with_input(Port::float("end", 100.0))
                .with_input(Port::int("seed", 0))
                .with_output_type(PortType::Float)
                .with_output_range(PortRange::List);
        }
        "range" => {
            node = node
                .with_input(Port::float("start", 0.0))
                .with_input(Port::float("end", 10.0))
                .with_input(Port::float("step", 1.0))
                .with_output_type(PortType::Float)
                .with_output_range(PortRange::List);
        }
        "sample" => {
            node = node
                .with_input(Port::int("amount", 10))
                .with_input(Port::float("start", 0.0))
                .with_input(Port::float("end", 100.0))
                .with_output_type(PortType::Float)
                .with_output_range(PortRange::List);
        }
        "wave" => {
            node = node
                .with_input(Port::float("min", 0.0))
                .with_input(Port::float("max", 100.0))
                .with_input(Port::float("period", 60.0))
                .with_input(Port::float("offset", 0.0))
                .with_input(Port::menu("type", "sine", vec![
                    MenuItem::new("sine", "Sine"),
                    MenuItem::new("square", "Square"),
                    MenuItem::new("triangle", "Triangle"),
                    MenuItem::new("sawtooth", "Sawtooth"),
                ]))
                .with_output_type(PortType::Float);
        }
        "convert_range" => {
            node = node
                .with_input(Port::float("value", 50.0))
                .with_input(Port::float("source_start", 0.0))
                .with_input(Port::float("source_end", 100.0))
                .with_input(Port::float("target_start", 0.0))
                .with_input(Port::float("target_end", 1.0))
                .with_input(Port::menu("method", "clamp", vec![
                    MenuItem::new("clamp", "Clamp"),
                    MenuItem::new("wrap", "Wrap"),
                    MenuItem::new("mirror", "Mirror"),
                    MenuItem::new("ignore", "Ignore"),
                ]))
                .with_output_type(PortType::Float);
        }
        "sum" | "average" | "max" | "min" => {
            node = node
                .with_input(Port::new("values", PortType::Float).with_port_range(PortRange::List))
                .with_output_type(PortType::Float);
        }
        "make_numbers" => {
            node = node
                .with_input(Port::string("string", "11;22;33"))
                .with_input(Port::string("separator", ";"))
                .with_output_type(PortType::Float)
                .with_output_range(PortRange::List);
        }
        "running_total" => {
            node = node
                .with_input(Port::new("values", PortType::Float).with_port_range(PortRange::List))
                .with_output_type(PortType::Float)
                .with_output_range(PortRange::List);
        }
        // ========================
        // String nodes
        // ========================
        "string" => {
            node = node
                .with_input(Port::string("value", ""))
                .with_output_type(PortType::String);
        }
        "concatenate" => {
            node = node
                .with_input(Port::string("string1", ""))
                .with_input(Port::string("string2", ""))
                .with_input(Port::string("string3", ""))
                .with_input(Port::string("string4", ""))
                .with_output_type(PortType::String);
        }
        "make_strings" => {
            node = node
                .with_input(Port::string("string", "Alpha;Beta;Gamma"))
                .with_input(Port::string("separator", ";"))
                .with_output_type(PortType::String)
                .with_output_range(PortRange::List);
        }
        "length" | "word_count" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_output_type(PortType::Int);
        }
        "change_case" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::menu("method", "uppercase", vec![
                    MenuItem::new("uppercase", "Uppercase"),
                    MenuItem::new("lowercase", "Lowercase"),
                    MenuItem::new("titlecase", "Title Case"),
                ]))
                .with_output_type(PortType::String);
        }
        "format_number" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_input(Port::string("format", "%.2f"))
                .with_output_type(PortType::String);
        }
        "trim" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_output_type(PortType::String);
        }
        "replace" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("old", ""))
                .with_input(Port::string("new", ""))
                .with_output_type(PortType::String);
        }
        "sub_string" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::int("start", 0))
                .with_input(Port::int("end", 4))
                .with_input(Port::boolean("end_offset", false))
                .with_output_type(PortType::String);
        }
        "character_at" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::int("index", 0))
                .with_output_type(PortType::String);
        }
        "as_binary_string" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("digit_separator", ""))
                .with_input(Port::string("byte_separator", " "))
                .with_output_type(PortType::String);
        }
        "contains" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("contains", ""))
                .with_output_type(PortType::Boolean);
        }
        "ends_with" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("ends_with", ""))
                .with_output_type(PortType::Boolean);
        }
        "starts_with" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("starts_with", ""))
                .with_output_type(PortType::Boolean);
        }
        "equals" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("equals", ""))
                .with_input(Port::boolean("case_sensitive", false))
                .with_output_type(PortType::Boolean);
        }
        "characters" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_output_type(PortType::String)
                .with_output_range(PortRange::List);
        }
        "random_character" => {
            node = node
                .with_input(Port::string("characters", "abcdefghijklmnopqrstuvwxyz"))
                .with_input(Port::int("amount", 10))
                .with_input(Port::int("seed", 0))
                .with_output_type(PortType::String)
                .with_output_range(PortRange::List);
        }
        "as_binary_list" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_output_type(PortType::String)
                .with_output_range(PortRange::List);
        }
        "as_number_list" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::int("radix", 10))
                .with_input(Port::boolean("padding", true))
                .with_output_type(PortType::String)
                .with_output_range(PortRange::List);
        }
        // ========================
        // List nodes
        // ========================
        "count" | "first" | "reverse" | "shuffle" | "slice" => {
            node = node.with_input(Port::new("list", PortType::List).with_port_range(PortRange::List));
            match template.name {
                "count" => {
                    node = node.with_output_type(PortType::Int);
                }
                "first" => {
                    node = node.with_output_type(PortType::List);
                }
                "reverse" => {
                    node = node
                        .with_output_type(PortType::List)
                        .with_output_range(PortRange::List);
                }
                "shuffle" => {
                    node = node
                        .with_input(Port::int("seed", 0))
                        .with_output_type(PortType::List)
                        .with_output_range(PortRange::List);
                }
                "slice" => {
                    node = node
                        .with_input(Port::int("start_index", 0))
                        .with_input(Port::int("size", 10))
                        .with_input(Port::boolean("invert", false))
                        .with_output_type(PortType::List)
                        .with_output_range(PortRange::List);
                }
                _ => {}
            }
        }
        "second" | "last" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_output_type(PortType::List);
        }
        "rest" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "shift" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::int("amount", 1))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "repeat" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::int("amount", 1))
                .with_input(Port::boolean("per_item", false))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "list_sort" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "pick" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::int("amount", 5))
                .with_input(Port::int("seed", 0))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "cull" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::new("booleans", PortType::Boolean).with_port_range(PortRange::List))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "take_every" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::int("n", 1))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "distinct" => {
            node = node
                .with_input(Port::new("list", PortType::List).with_port_range(PortRange::List))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "switch" => {
            node = node
                .with_input(Port::int("index", 0))
                .with_input(Port::new("input1", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::new("input2", PortType::List).with_port_range(PortRange::List))
                .with_output_type(PortType::List)
                .with_output_range(PortRange::List);
        }
        "combine" => {
            node = node
                .with_input(Port::geometry("list1"))
                .with_input(Port::geometry("list2"))
                .with_input(Port::geometry("list3"))
                .with_output_type(PortType::Geometry);
        }
        "keys" => {
            node = node
                .with_input(Port::new("maps", PortType::List).with_port_range(PortRange::List))
                .with_output_type(PortType::Data)
                .with_output_range(PortRange::List);
        }
        "zip_map" => {
            node = node
                .with_input(Port::new("keys", PortType::String).with_port_range(PortRange::List))
                .with_input(Port::new("values", PortType::List).with_port_range(PortRange::List))
                .with_output_type(PortType::Data);
        }
        // ========================
        // Core nodes
        // ========================
        "frame" => {
            node = node.with_output_type(PortType::Float);
        }
        // ========================
        // Data nodes
        // ========================
        "import_text" => {
            node = node
                .with_input(Port::string("file", "").with_widget(Widget::File))
                .with_output_type(PortType::String)
                .with_output_range(PortRange::List);
        }
        "import_csv" => {
            node = node
                .with_input(Port::string("file", "").with_widget(Widget::File))
                .with_input(Port::menu("delimiter", "comma", vec![
                    MenuItem::new("comma", "Comma"),
                    MenuItem::new("semicolon", "Semicolon"),
                    MenuItem::new("colon", "Colon"),
                    MenuItem::new("tab", "Tab"),
                    MenuItem::new("space", "Space"),
                ]))
                .with_input(Port::menu("quotes", "double", vec![
                    MenuItem::new("double", "\""),
                    MenuItem::new("single", "'"),
                ]))
                .with_input(Port::menu("number_separator", "period", vec![
                    MenuItem::new("period", "."),
                    MenuItem::new("comma", ","),
                ]))
                .with_output_type(PortType::Data)
                .with_output_range(PortRange::List);
        }
        "make_table" => {
            node = node
                .with_input(Port::string("headers", "alpha;beta"))
                .with_input(Port::new("list1", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::new("list2", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::new("list3", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::new("list4", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::new("list5", PortType::List).with_port_range(PortRange::List))
                .with_input(Port::new("list6", PortType::List).with_port_range(PortRange::List))
                .with_output_type(PortType::Data)
                .with_output_range(PortRange::List);
        }
        "lookup" => {
            node = node
                .with_input(Port::new("list", PortType::Data))
                .with_input(Port::string("key", "x"))
                .with_output_type(PortType::Data);
        }
        "filter_data" => {
            node = node
                .with_input(Port::new("data", PortType::Data).with_port_range(PortRange::List))
                .with_input(Port::string("key", "name"))
                .with_input(Port::menu("op", "=", vec![
                    MenuItem::new("=", "= Equal To"),
                    MenuItem::new("!=", "!= Not Equal To"),
                    MenuItem::new(">", "> Greater Than"),
                    MenuItem::new(">=", ">= Greater or Equal"),
                    MenuItem::new("<", "< Smaller Than"),
                    MenuItem::new("<=", "<= Smaller or Equal"),
                ]))
                .with_input(Port::string("value", ""))
                .with_output_type(PortType::Data)
                .with_output_range(PortRange::List);
        }
        // ========================
        // Network nodes
        // ========================
        "http_get" => {
            node = node
                .with_input(Port::string("url", ""))
                .with_output_type(PortType::String);
        }
        "encode_url" => {
            node = node
                .with_input(Port::string("value", ""))
                .with_output_type(PortType::String);
        }
        _ => {
            matched = false;
        }
    }

    debug_assert!(
        matched,
        "Node template '{}' is registered but has no match arm in create_node_from_template. \
         Add an arm with .with_output_type() to set the correct output type.",
        template.name
    );

    node
}
