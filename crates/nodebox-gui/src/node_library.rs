//! Node library browser for creating new nodes.
//!
//! Note: This module is work-in-progress and not yet integrated.

#![allow(dead_code)]

use std::sync::Arc;
use eframe::egui;
use nodebox_core::geometry::{Color, Point};
use nodebox_core::node::{MenuItem, Node, NodeLibrary, Port, PortRange, PortType, Widget};

/// Available node types that can be created.
pub struct NodeTemplate {
    pub name: &'static str,
    pub prototype: &'static str,
    pub category: &'static str,
    pub description: &'static str,
}

/// List of all available node templates.
pub const NODE_TEMPLATES: &[NodeTemplate] = &[
    // ========================
    // Geometry generators
    // ========================
    NodeTemplate {
        name: "ellipse",
        prototype: "corevector.ellipse",
        category: "geometry",
        description: "Create an ellipse or circle",
    },
    NodeTemplate {
        name: "rect",
        prototype: "corevector.rect",
        category: "geometry",
        description: "Create a rectangle",
    },
    NodeTemplate {
        name: "line",
        prototype: "corevector.line",
        category: "geometry",
        description: "Create a line between two points",
    },
    NodeTemplate {
        name: "line_angle",
        prototype: "corevector.line_angle",
        category: "geometry",
        description: "Create a line from a point, angle and distance",
    },
    NodeTemplate {
        name: "polygon",
        prototype: "corevector.polygon",
        category: "geometry",
        description: "Create a regular polygon",
    },
    NodeTemplate {
        name: "star",
        prototype: "corevector.star",
        category: "geometry",
        description: "Create a star shape",
    },
    NodeTemplate {
        name: "arc",
        prototype: "corevector.arc",
        category: "geometry",
        description: "Create an arc or pie slice",
    },
    NodeTemplate {
        name: "quad_curve",
        prototype: "corevector.quad_curve",
        category: "geometry",
        description: "Create a quadratic curve between two points",
    },
    NodeTemplate {
        name: "grid",
        prototype: "corevector.grid",
        category: "geometry",
        description: "Create a grid of points",
    },
    NodeTemplate {
        name: "textpath",
        prototype: "corevector.textpath",
        category: "geometry",
        description: "Convert text to a vector path",
    },
    NodeTemplate {
        name: "connect",
        prototype: "corevector.connect",
        category: "geometry",
        description: "Connect all points in a path",
    },
    NodeTemplate {
        name: "make_point",
        prototype: "corevector.make_point",
        category: "geometry",
        description: "Create a point from X/Y coordinates",
    },
    NodeTemplate {
        name: "freehand",
        prototype: "corevector.freehand",
        category: "geometry",
        description: "Draw directly on the canvas",
    },
    // Combine nodes
    NodeTemplate {
        name: "merge",
        prototype: "corevector.merge",
        category: "geometry",
        description: "Combine multiple shapes",
    },
    NodeTemplate {
        name: "group",
        prototype: "corevector.group",
        category: "geometry",
        description: "Group shapes together",
    },
    NodeTemplate {
        name: "ungroup",
        prototype: "corevector.ungroup",
        category: "geometry",
        description: "Decompose geometry into its paths",
    },
    // Modify nodes
    NodeTemplate {
        name: "resample",
        prototype: "corevector.resample",
        category: "geometry",
        description: "Resample path points",
    },
    NodeTemplate {
        name: "wiggle",
        prototype: "corevector.wiggle",
        category: "geometry",
        description: "Add random displacement to points",
    },
    NodeTemplate {
        name: "align",
        prototype: "corevector.align",
        category: "geometry",
        description: "Align a shape in relation to the origin",
    },
    NodeTemplate {
        name: "fit",
        prototype: "corevector.fit",
        category: "geometry",
        description: "Fit a shape within bounds",
    },
    NodeTemplate {
        name: "fit_to",
        prototype: "corevector.fit_to",
        category: "geometry",
        description: "Fit a shape to another shape",
    },
    NodeTemplate {
        name: "snap",
        prototype: "corevector.snap",
        category: "geometry",
        description: "Snap geometry to a grid",
    },
    NodeTemplate {
        name: "centroid",
        prototype: "corevector.centroid",
        category: "geometry",
        description: "Calculate the center point of a shape",
    },
    NodeTemplate {
        name: "point_on_path",
        prototype: "corevector.point_on_path",
        category: "geometry",
        description: "Calculate a point on a path",
    },
    NodeTemplate {
        name: "scatter",
        prototype: "corevector.scatter",
        category: "geometry",
        description: "Generate points within a shape",
    },
    NodeTemplate {
        name: "delete",
        prototype: "corevector.delete",
        category: "geometry",
        description: "Delete points or paths within a bounding shape",
    },
    NodeTemplate {
        name: "sort",
        prototype: "corevector.sort",
        category: "geometry",
        description: "Sort shapes by position or distance",
    },
    NodeTemplate {
        name: "stack",
        prototype: "corevector.stack",
        category: "geometry",
        description: "Arrange shapes in a layout",
    },
    NodeTemplate {
        name: "link",
        prototype: "corevector.link",
        category: "geometry",
        description: "Generate a visual link between two shapes",
    },
    NodeTemplate {
        name: "shape_on_path",
        prototype: "corevector.shape_on_path",
        category: "geometry",
        description: "Copy shapes along a path",
    },
    NodeTemplate {
        name: "null",
        prototype: "corevector.null",
        category: "geometry",
        description: "Pass through without changes",
    },
    // Import nodes
    NodeTemplate {
        name: "import_svg",
        prototype: "corevector.import_svg",
        category: "geometry",
        description: "Import an SVG file as geometry",
    },
    // ========================
    // Transform nodes
    // ========================
    NodeTemplate {
        name: "translate",
        prototype: "corevector.translate",
        category: "transform",
        description: "Move geometry by offset",
    },
    NodeTemplate {
        name: "rotate",
        prototype: "corevector.rotate",
        category: "transform",
        description: "Rotate geometry around a point",
    },
    NodeTemplate {
        name: "scale",
        prototype: "corevector.scale",
        category: "transform",
        description: "Scale geometry",
    },
    NodeTemplate {
        name: "copy",
        prototype: "corevector.copy",
        category: "transform",
        description: "Create multiple copies",
    },
    NodeTemplate {
        name: "distribute",
        prototype: "corevector.distribute",
        category: "transform",
        description: "Distribute shapes on a horizontal or vertical axis",
    },
    NodeTemplate {
        name: "skew",
        prototype: "corevector.skew",
        category: "transform",
        description: "Skew the shape",
    },
    NodeTemplate {
        name: "reflect",
        prototype: "corevector.reflect",
        category: "transform",
        description: "Mirror geometry around an axis",
    },
    // ========================
    // Color nodes
    // ========================
    NodeTemplate {
        name: "colorize",
        prototype: "corevector.colorize",
        category: "color",
        description: "Set fill and stroke colors",
    },
    NodeTemplate {
        name: "rgb_color",
        prototype: "color.rgb_color",
        category: "color",
        description: "Create a color from RGB components",
    },
    NodeTemplate {
        name: "hsb_color",
        prototype: "color.hsb_color",
        category: "color",
        description: "Create a color from HSB components",
    },
    NodeTemplate {
        name: "gray_color",
        prototype: "color.gray_color",
        category: "color",
        description: "Create a grayscale color",
    },
    NodeTemplate {
        name: "color",
        prototype: "color.color",
        category: "color",
        description: "Create a color value",
    },
    // ========================
    // Math nodes
    // ========================
    NodeTemplate {
        name: "number",
        prototype: "math.number",
        category: "math",
        description: "Create a number value",
    },
    NodeTemplate {
        name: "integer",
        prototype: "math.integer",
        category: "math",
        description: "Create an integer value",
    },
    NodeTemplate {
        name: "boolean",
        prototype: "math.boolean",
        category: "math",
        description: "Create a boolean value",
    },
    NodeTemplate {
        name: "add",
        prototype: "math.add",
        category: "math",
        description: "Add two numbers",
    },
    NodeTemplate {
        name: "subtract",
        prototype: "math.subtract",
        category: "math",
        description: "Subtract two numbers",
    },
    NodeTemplate {
        name: "multiply",
        prototype: "math.multiply",
        category: "math",
        description: "Multiply two numbers",
    },
    NodeTemplate {
        name: "divide",
        prototype: "math.divide",
        category: "math",
        description: "Divide two numbers",
    },
    NodeTemplate {
        name: "mod",
        prototype: "math.mod",
        category: "math",
        description: "Modulo of two numbers",
    },
    NodeTemplate {
        name: "negate",
        prototype: "math.negate",
        category: "math",
        description: "Negate a number",
    },
    NodeTemplate {
        name: "abs",
        prototype: "math.abs",
        category: "math",
        description: "Absolute value",
    },
    NodeTemplate {
        name: "sqrt",
        prototype: "math.sqrt",
        category: "math",
        description: "Square root",
    },
    NodeTemplate {
        name: "pow",
        prototype: "math.pow",
        category: "math",
        description: "Raise to a power",
    },
    NodeTemplate {
        name: "log",
        prototype: "math.log",
        category: "math",
        description: "Natural logarithm",
    },
    NodeTemplate {
        name: "ceil",
        prototype: "math.ceil",
        category: "math",
        description: "Round up to integer",
    },
    NodeTemplate {
        name: "floor",
        prototype: "math.floor",
        category: "math",
        description: "Round down to integer",
    },
    NodeTemplate {
        name: "round",
        prototype: "math.round",
        category: "math",
        description: "Round to nearest integer",
    },
    NodeTemplate {
        name: "sin",
        prototype: "math.sin",
        category: "math",
        description: "Sine function",
    },
    NodeTemplate {
        name: "cos",
        prototype: "math.cos",
        category: "math",
        description: "Cosine function",
    },
    NodeTemplate {
        name: "radians",
        prototype: "math.radians",
        category: "math",
        description: "Convert degrees to radians",
    },
    NodeTemplate {
        name: "degrees",
        prototype: "math.degrees",
        category: "math",
        description: "Convert radians to degrees",
    },
    NodeTemplate {
        name: "pi",
        prototype: "math.pi",
        category: "math",
        description: "The constant pi",
    },
    NodeTemplate {
        name: "e",
        prototype: "math.e",
        category: "math",
        description: "Euler's number",
    },
    NodeTemplate {
        name: "even",
        prototype: "math.even",
        category: "math",
        description: "Check if a number is even",
    },
    NodeTemplate {
        name: "odd",
        prototype: "math.odd",
        category: "math",
        description: "Check if a number is odd",
    },
    NodeTemplate {
        name: "compare",
        prototype: "math.compare",
        category: "math",
        description: "Compare two values",
    },
    NodeTemplate {
        name: "logical",
        prototype: "math.logical",
        category: "math",
        description: "Logical AND/OR of two booleans",
    },
    NodeTemplate {
        name: "angle",
        prototype: "math.angle",
        category: "math",
        description: "Angle between two points",
    },
    NodeTemplate {
        name: "distance",
        prototype: "math.distance",
        category: "math",
        description: "Distance between two points",
    },
    NodeTemplate {
        name: "coordinates",
        prototype: "math.coordinates",
        category: "math",
        description: "Point from angle and distance",
    },
    NodeTemplate {
        name: "math_reflect",
        prototype: "math.reflect",
        category: "math",
        description: "Reflect a point around another",
    },
    NodeTemplate {
        name: "random_numbers",
        prototype: "math.random_numbers",
        category: "math",
        description: "Generate a list of random numbers",
    },
    NodeTemplate {
        name: "range",
        prototype: "math.range",
        category: "math",
        description: "Generate a range of numbers",
    },
    NodeTemplate {
        name: "sample",
        prototype: "math.sample",
        category: "math",
        description: "Generate evenly-spaced samples",
    },
    NodeTemplate {
        name: "wave",
        prototype: "math.wave",
        category: "math",
        description: "Generate a wave value",
    },
    NodeTemplate {
        name: "convert_range",
        prototype: "math.convert_range",
        category: "math",
        description: "Map a value from one range to another",
    },
    NodeTemplate {
        name: "sum",
        prototype: "math.sum",
        category: "math",
        description: "Sum of a list of numbers",
    },
    NodeTemplate {
        name: "average",
        prototype: "math.average",
        category: "math",
        description: "Average of a list of numbers",
    },
    NodeTemplate {
        name: "max",
        prototype: "math.max",
        category: "math",
        description: "Maximum of a list of numbers",
    },
    NodeTemplate {
        name: "min",
        prototype: "math.min",
        category: "math",
        description: "Minimum of a list of numbers",
    },
    NodeTemplate {
        name: "make_numbers",
        prototype: "math.make_numbers",
        category: "math",
        description: "Parse numbers from a string",
    },
    NodeTemplate {
        name: "running_total",
        prototype: "math.running_total",
        category: "math",
        description: "Running total of a list",
    },
    // ========================
    // String nodes
    // ========================
    NodeTemplate {
        name: "string",
        prototype: "string.string",
        category: "string",
        description: "Create a string value",
    },
    NodeTemplate {
        name: "concatenate",
        prototype: "string.concatenate",
        category: "string",
        description: "Join strings together",
    },
    NodeTemplate {
        name: "make_strings",
        prototype: "string.make_strings",
        category: "string",
        description: "Split a string into a list",
    },
    NodeTemplate {
        name: "length",
        prototype: "string.length",
        category: "string",
        description: "Length of a string",
    },
    NodeTemplate {
        name: "word_count",
        prototype: "string.word_count",
        category: "string",
        description: "Count words in a string",
    },
    NodeTemplate {
        name: "change_case",
        prototype: "string.change_case",
        category: "string",
        description: "Change text case",
    },
    NodeTemplate {
        name: "format_number",
        prototype: "string.format_number",
        category: "string",
        description: "Format a number as string",
    },
    NodeTemplate {
        name: "trim",
        prototype: "string.trim",
        category: "string",
        description: "Remove leading/trailing whitespace",
    },
    NodeTemplate {
        name: "replace",
        prototype: "string.replace",
        category: "string",
        description: "Replace text in a string",
    },
    NodeTemplate {
        name: "sub_string",
        prototype: "string.sub_string",
        category: "string",
        description: "Extract part of a string",
    },
    NodeTemplate {
        name: "character_at",
        prototype: "string.character_at",
        category: "string",
        description: "Get character at index",
    },
    NodeTemplate {
        name: "as_binary_string",
        prototype: "string.as_binary_string",
        category: "string",
        description: "Convert string to binary",
    },
    NodeTemplate {
        name: "contains",
        prototype: "string.contains",
        category: "string",
        description: "Check if string contains text",
    },
    NodeTemplate {
        name: "ends_with",
        prototype: "string.ends_with",
        category: "string",
        description: "Check if string ends with text",
    },
    NodeTemplate {
        name: "starts_with",
        prototype: "string.starts_with",
        category: "string",
        description: "Check if string starts with text",
    },
    NodeTemplate {
        name: "equals",
        prototype: "string.equals",
        category: "string",
        description: "Check if two strings are equal",
    },
    NodeTemplate {
        name: "characters",
        prototype: "string.characters",
        category: "string",
        description: "Split string into characters",
    },
    NodeTemplate {
        name: "random_character",
        prototype: "string.random_character",
        category: "string",
        description: "Generate random characters",
    },
    NodeTemplate {
        name: "as_binary_list",
        prototype: "string.as_binary_list",
        category: "string",
        description: "Convert string to binary list",
    },
    NodeTemplate {
        name: "as_number_list",
        prototype: "string.as_number_list",
        category: "string",
        description: "Convert string to number list",
    },
    // ========================
    // List nodes
    // ========================
    NodeTemplate {
        name: "count",
        prototype: "list.count",
        category: "list",
        description: "Count items in a list",
    },
    NodeTemplate {
        name: "first",
        prototype: "list.first",
        category: "list",
        description: "Get the first item of a list",
    },
    NodeTemplate {
        name: "second",
        prototype: "list.second",
        category: "list",
        description: "Get the second item of a list",
    },
    NodeTemplate {
        name: "last",
        prototype: "list.last",
        category: "list",
        description: "Get the last item of a list",
    },
    NodeTemplate {
        name: "rest",
        prototype: "list.rest",
        category: "list",
        description: "Get all items except the first",
    },
    NodeTemplate {
        name: "reverse",
        prototype: "list.reverse",
        category: "list",
        description: "Reverse the order of a list",
    },
    NodeTemplate {
        name: "shuffle",
        prototype: "list.shuffle",
        category: "list",
        description: "Randomize list order",
    },
    NodeTemplate {
        name: "slice",
        prototype: "list.slice",
        category: "list",
        description: "Take a portion of a list",
    },
    NodeTemplate {
        name: "shift",
        prototype: "list.shift",
        category: "list",
        description: "Shift list items by offset",
    },
    NodeTemplate {
        name: "repeat",
        prototype: "list.repeat",
        category: "list",
        description: "Repeat list items",
    },
    NodeTemplate {
        name: "list_sort",
        prototype: "list.sort",
        category: "list",
        description: "Sort a list",
    },
    NodeTemplate {
        name: "pick",
        prototype: "list.pick",
        category: "list",
        description: "Pick random items from a list",
    },
    NodeTemplate {
        name: "cull",
        prototype: "list.cull",
        category: "list",
        description: "Filter list items by boolean pattern",
    },
    NodeTemplate {
        name: "take_every",
        prototype: "list.take_every",
        category: "list",
        description: "Take every Nth item",
    },
    NodeTemplate {
        name: "distinct",
        prototype: "list.distinct",
        category: "list",
        description: "Remove duplicate items",
    },
    NodeTemplate {
        name: "switch",
        prototype: "list.switch",
        category: "list",
        description: "Select from multiple inputs",
    },
    NodeTemplate {
        name: "combine",
        prototype: "list.combine",
        category: "list",
        description: "Combine multiple lists into one",
    },
    // ========================
    // Core nodes
    // ========================
    NodeTemplate {
        name: "frame",
        prototype: "core.frame",
        category: "core",
        description: "Get the current animation frame",
    },
    // ========================
    // Data nodes
    // ========================
    NodeTemplate {
        name: "import_text",
        prototype: "data.import_text",
        category: "data",
        description: "Import lines from a text file",
    },
    NodeTemplate {
        name: "import_csv",
        prototype: "data.import_csv",
        category: "data",
        description: "Import data from a CSV file",
    },
    // ========================
    // Network nodes
    // ========================
    NodeTemplate {
        name: "http_get",
        prototype: "network.http_get",
        category: "network",
        description: "Fetch content from a URL",
    },
    NodeTemplate {
        name: "encode_url",
        prototype: "network.encode_url",
        category: "network",
        description: "Percent-encode a URL string",
    },
];

/// The node library browser widget.
pub struct NodeLibraryBrowser {
    search_text: String,
    selected_category: Option<String>,
}

impl Default for NodeLibraryBrowser {
    fn default() -> Self {
        Self::new()
    }
}

impl NodeLibraryBrowser {
    pub fn new() -> Self {
        Self {
            search_text: String::new(),
            selected_category: None,
        }
    }

    /// Show the library browser and return the name of any node created.
    pub fn show(&mut self, ui: &mut egui::Ui, library: &mut Arc<NodeLibrary>) -> Option<String> {
        let mut created_node = None;

        // Search box
        ui.horizontal(|ui| {
            ui.label("Search:");
            ui.text_edit_singleline(&mut self.search_text);
        });
        ui.add_space(5.0);

        // Category filter buttons
        ui.horizontal_wrapped(|ui| {
            let categories = ["geometry", "transform", "color", "math", "string", "list", "core", "data", "network"];
            for cat in categories {
                let is_selected = self.selected_category.as_deref() == Some(cat);
                if ui.selectable_label(is_selected, cat).clicked() {
                    if is_selected {
                        self.selected_category = None;
                    } else {
                        self.selected_category = Some(cat.to_string());
                    }
                }
            }
            if ui.selectable_label(self.selected_category.is_none() && self.search_text.is_empty(), "all").clicked() {
                self.selected_category = None;
                self.search_text.clear();
            }
        });
        ui.separator();

        // Node list
        egui::ScrollArea::vertical().show(ui, |ui| {
            for template in NODE_TEMPLATES {
                // Filter by category
                if let Some(ref cat) = self.selected_category {
                    if template.category != cat {
                        continue;
                    }
                }

                // Filter by search text
                if !self.search_text.is_empty() {
                    let search = self.search_text.to_lowercase();
                    if !template.name.to_lowercase().contains(&search)
                        && !template.description.to_lowercase().contains(&search)
                    {
                        continue;
                    }
                }

                // Display node button
                ui.horizontal(|ui| {
                    if ui.button("+").clicked() {
                        // Calculate position (offset from last node or default)
                        let pos = if let Some(last_child) = library.root.children.last() {
                            Point::new(last_child.position.x + 180.0, last_child.position.y)
                        } else {
                            Point::new(50.0, 50.0)
                        };
                        // Create the node
                        let node = create_node_from_template(template, library, pos);
                        let node_name = node.name.clone();
                        Arc::make_mut(library).root.children.push(node);
                        created_node = Some(node_name);
                    }
                    ui.label(template.name);
                    ui.label(format!("({})", template.category)).on_hover_text(template.description);
                });
            }
        });

        created_node
    }
}

/// Check if the first input port of a node template is directly compatible
/// with the given output type. Uses strict rules (no string conversion,
/// no number→point promotion) — only same-type, List wildcard, and Int↔Float.
pub fn template_has_compatible_input(template: &NodeTemplate, output_type: &PortType) -> bool {
    let temp_lib = NodeLibrary::new("_temp");
    let node = create_node_from_template(template, &temp_lib, Point::ZERO);
    node.inputs
        .first()
        .is_some_and(|port| is_directly_compatible(output_type, &port.port_type))
}

/// Strict type compatibility for dialog filtering.
/// Only allows: same type, List wildcard, and Int↔Float.
/// Excludes the broad everything→String and Number→Point rules.
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

    // Add ports based on node type
    match template.name {
        // ========================
        // Geometry generators
        // ========================
        "ellipse" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0));
        }
        "rect" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_input(Port::point("roundness", Point::ZERO));
        }
        "line" => {
            node = node
                .with_input(Port::point("point1", Point::ZERO))
                .with_input(Port::point("point2", Point::new(100.0, 100.0)))
                .with_input(Port::int("points", 2).with_min(0.0));
        }
        "line_angle" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("angle", 0.0))
                .with_input(Port::float("distance", 100.0))
                .with_input(Port::int("points", 2).with_min(2.0));
        }
        "polygon" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("radius", 100.0))
                .with_input(Port::int("sides", 3).with_min(3.0))
                .with_input(Port::boolean("align", false));
        }
        "star" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::int("points", 20).with_min(2.0))
                .with_input(Port::float("outer", 200.0))
                .with_input(Port::float("inner", 100.0));
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
                ]));
        }
        "quad_curve" => {
            node = node
                .with_input(Port::point("point1", Point::ZERO))
                .with_input(Port::point("point2", Point::new(100.0, 0.0)))
                .with_input(Port::float("t", 50.0))
                .with_input(Port::float("distance", 50.0));
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
                .with_input(Port::float("width", 0.0));
        }
        "connect" => {
            node = node
                .with_input(Port::new("points", PortType::Point).with_port_range(PortRange::List))
                .with_input(Port::boolean("closed", false));
        }
        "make_point" => {
            node = node
                .with_input(Port::float("x", 0.0))
                .with_input(Port::float("y", 0.0))
                .with_output_type(PortType::Point);
        }
        "freehand" => {
            node = node.with_input(Port::string("path", ""));
        }
        // Combine / structural
        "merge" | "group" => {
            node = node.with_input(Port::geometry("shapes"));
        }
        "ungroup" => {
            node = node.with_input(Port::geometry("shape"));
        }
        "null" => {
            node = node.with_input(Port::geometry("shape"));
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
                .with_input(Port::boolean("per_contour", false));
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
                .with_input(Port::int("seed", 0));
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
                ]));
        }
        "fit" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_input(Port::boolean("keep_proportions", true));
        }
        "fit_to" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::geometry("bounding"))
                .with_input(Port::boolean("keep_proportions", true));
        }
        "snap" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::float("distance", 10.0))
                .with_input(Port::float("strength", 1.0))
                .with_input(Port::point("position", Point::ZERO));
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
                ]));
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
                .with_input(Port::point("position", Point::ZERO));
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
                .with_input(Port::float("margin", 0.0));
        }
        "link" => {
            node = node
                .with_input(Port::geometry("shape1"))
                .with_input(Port::geometry("shape2"))
                .with_input(Port::menu("orientation", "horizontal", vec![
                    MenuItem::new("horizontal", "Horizontal"),
                    MenuItem::new("vertical", "Vertical"),
                ]));
        }
        "shape_on_path" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::geometry("path"))
                .with_input(Port::int("amount", 1))
                .with_input(Port::float("spacing", 20.0))
                .with_input(Port::float("margin", 0.0));
        }
        // Import
        "import_svg" => {
            node = node
                .with_input(Port::string("file", "").with_widget(Widget::File))
                .with_input(Port::boolean("centered", true))
                .with_input(Port::point("position", Point::ZERO));
        }
        // ========================
        // Transform nodes
        // ========================
        "translate" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("translate", Point::ZERO));
        }
        "rotate" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::float("angle", 0.0))
                .with_input(Port::point("origin", Point::ZERO));
        }
        "scale" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("scale", Point::new(100.0, 100.0)))
                .with_input(Port::point("origin", Point::ZERO));
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
                .with_input(Port::point("scale", Point::new(100.0, 100.0)));
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
                .with_input(Port::point("origin", Point::ZERO));
        }
        "reflect" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("angle", 0.0))
                .with_input(Port::boolean("keep_original", true));
        }
        // ========================
        // Color nodes
        // ========================
        "colorize" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::color("fill", Color::rgb(0.5, 0.5, 0.5)))
                .with_input(Port::color("stroke", Color::BLACK))
                .with_input(Port::float("strokeWidth", 1.0));
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
            node = node.with_input(Port::float("value", 0.0));
        }
        "integer" => {
            node = node.with_input(Port::int("value", 0));
        }
        "boolean" => {
            node = node.with_input(Port::boolean("value", false));
        }
        "add" | "subtract" | "multiply" | "divide" => {
            node = node
                .with_input(Port::float("value1", 0.0))
                .with_input(Port::float("value2", 0.0));
        }
        "mod" => {
            node = node
                .with_input(Port::float("value1", 0.0))
                .with_input(Port::float("value2", 1.0));
        }
        "negate" | "abs" | "sqrt" => {
            node = node.with_input(Port::float("value", 0.0));
        }
        "pow" => {
            node = node
                .with_input(Port::float("value1", 0.0))
                .with_input(Port::float("value2", 2.0));
        }
        "log" => {
            node = node.with_input(Port::float("value", 1.0));
        }
        "ceil" | "floor" => {
            node = node.with_input(Port::float("value", 0.0));
        }
        "round" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_output_type(PortType::Int);
        }
        "sin" | "cos" => {
            node = node.with_input(Port::float("value", 0.0));
        }
        "radians" => {
            node = node.with_input(Port::float("degrees", 0.0));
        }
        "degrees" => {
            node = node.with_input(Port::float("radians", 0.0));
        }
        "pi" | "e" => {
            node = node.with_output_type(PortType::Float);
        }
        "even" | "odd" => {
            node = node.with_input(Port::float("value", 0.0));
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
                ]));
        }
        "logical" => {
            node = node
                .with_input(Port::boolean("boolean1", false))
                .with_input(Port::boolean("boolean2", false))
                .with_input(Port::menu("comparator", "or", vec![
                    MenuItem::new("or", "Or"),
                    MenuItem::new("and", "And"),
                ]));
        }
        "angle" | "distance" => {
            node = node
                .with_input(Port::point("point1", Point::ZERO))
                .with_input(Port::point("point2", Point::new(100.0, 100.0)));
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
                ]));
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
                ]));
        }
        "sum" | "average" | "max" | "min" => {
            node = node
                .with_input(Port::new("values", PortType::Float).with_port_range(PortRange::List));
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
            node = node.with_input(Port::string("value", ""));
        }
        "concatenate" => {
            node = node
                .with_input(Port::string("string1", ""))
                .with_input(Port::string("string2", ""))
                .with_input(Port::string("string3", ""))
                .with_input(Port::string("string4", ""));
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
                ]));
        }
        "format_number" => {
            node = node
                .with_input(Port::float("value", 0.0))
                .with_input(Port::string("format", "%.2f"));
        }
        "trim" => {
            node = node.with_input(Port::string("string", ""));
        }
        "replace" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("old", ""))
                .with_input(Port::string("new", ""));
        }
        "sub_string" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::int("start", 0))
                .with_input(Port::int("end", 4))
                .with_input(Port::boolean("end_offset", false));
        }
        "character_at" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::int("index", 0));
        }
        "as_binary_string" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("digit_separator", ""))
                .with_input(Port::string("byte_separator", " "));
        }
        "contains" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("contains", ""));
        }
        "ends_with" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("ends_with", ""));
        }
        "starts_with" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("starts_with", ""));
        }
        "equals" => {
            node = node
                .with_input(Port::string("string", ""))
                .with_input(Port::string("equals", ""))
                .with_input(Port::boolean("case_sensitive", false));
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
                .with_input(Port::new("input2", PortType::List).with_port_range(PortRange::List));
        }
        "combine" => {
            node = node
                .with_input(Port::geometry("list1"))
                .with_input(Port::geometry("list2"))
                .with_input(Port::geometry("list3"));
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
                .with_output_type(PortType::String)
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
        _ => {}
    }

    node
}
