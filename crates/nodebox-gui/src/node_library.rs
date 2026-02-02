//! Node library browser for creating new nodes.
//!
//! Note: This module is work-in-progress and not yet integrated.

#![allow(dead_code)]

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
    pub fn show(&mut self, ui: &mut egui::Ui, library: &mut NodeLibrary) -> Option<String> {
        let mut created_node = None;

        // Search box
        ui.horizontal(|ui| {
            ui.label("Search:");
            ui.text_edit_singleline(&mut self.search_text);
        });
        ui.add_space(5.0);

        // Category filter buttons
        ui.horizontal_wrapped(|ui| {
            let categories = ["geometry", "transform", "color"];
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
                        library.root.children.push(node);
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
    let mut node = Node::new(&name)
        .with_prototype(template.prototype)
        .with_function(format!("corevector/{}", template.name))
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
                .with_input(Port::int("points", 2));
        }
        "polygon" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("radius", 100.0))
                .with_input(Port::int("sides", 3))
                .with_input(Port::boolean("align", false));
        }
        "star" => {
            node = node
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::int("points", 20))
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
                .with_input(Port::int("columns", 10))
                .with_input(Port::int("rows", 10))
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
                .with_input(Port::int("copies", 1))
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
        "import_svg" => {
            node = node
                .with_input(Port::string("file", "").with_widget(Widget::File))
                .with_input(Port::boolean("centered", true))
                .with_input(Port::point("position", Point::ZERO));
        }
        _ => {}
    }

    node
}
