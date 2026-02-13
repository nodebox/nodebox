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
    // Geometry generators
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
    // Transform nodes
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
    // Color nodes
    NodeTemplate {
        name: "colorize",
        prototype: "corevector.colorize",
        category: "color",
        description: "Set fill and stroke colors",
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
    // Import nodes
    NodeTemplate {
        name: "import_svg",
        prototype: "corevector.import_svg",
        category: "geometry",
        description: "Import an SVG file as geometry",
    },
    // Math nodes
    NodeTemplate {
        name: "number",
        prototype: "math.number",
        category: "math",
        description: "Create a number value",
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
        name: "compare",
        prototype: "math.compare",
        category: "math",
        description: "Compare two values",
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
    // String nodes
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
    // List nodes
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
    // Color nodes
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
    // Core nodes
    NodeTemplate {
        name: "frame",
        prototype: "core.frame",
        category: "core",
        description: "Get the current animation frame",
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
            let categories = ["geometry", "transform", "color", "math", "string", "list", "core"];
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
        "colorize" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::color("fill", Color::rgb(0.5, 0.5, 0.5)))
                .with_input(Port::color("stroke", Color::BLACK))
                .with_input(Port::float("strokeWidth", 1.0));
        }
        "merge" | "group" => {
            node = node.with_input(Port::geometry("shapes"));
        }
        "resample" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::menu("method", "length", vec![
                    MenuItem::new("length", "By length"),
                    MenuItem::new("amount", "By amount"),
                ]))
                .with_input(Port::float("length", 10.0))
                .with_input(Port::int("points", 10))
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
        "import_svg" => {
            node = node
                .with_input(Port::string("file", "").with_widget(Widget::File))
                .with_input(Port::boolean("centered", true))
                .with_input(Port::point("position", Point::ZERO));
        }
        // Math nodes
        "number" => {
            node = node.with_input(Port::float("value", 0.0));
        }
        "add" | "subtract" | "multiply" | "divide" => {
            node = node
                .with_input(Port::float("value1", 0.0))
                .with_input(Port::float("value2", 0.0));
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
        // String nodes
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
        // List nodes
        "count" | "first" | "reverse" | "shuffle" | "slice" => {
            node = node.with_input(Port::geometry("list").with_port_range(PortRange::List));
            match template.name {
                "shuffle" => { node = node.with_input(Port::int("seed", 0)); }
                "slice" => {
                    node = node
                        .with_input(Port::int("start_index", 0))
                        .with_input(Port::int("size", 10))
                        .with_input(Port::boolean("invert", false));
                }
                _ => {}
            }
        }
        // Color nodes
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
        // Core nodes
        "frame" => {
            // No input ports; outputs the current frame number
        }
        _ => {}
    }

    node
}
