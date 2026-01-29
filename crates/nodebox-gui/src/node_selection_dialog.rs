//! Modal node selection dialog with search and category filtering.

use eframe::egui::{self, Color32, Key, Rect, Vec2};
use nodebox_core::geometry::{Color, Point};
use nodebox_core::node::{Node, NodeLibrary, Port};
use crate::theme;

/// Available node types that can be created.
#[derive(Clone)]
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
];

/// Categories for filtering nodes.
const CATEGORIES: &[&str] = &["All", "geometry", "transform", "color"];

/// The modal node selection dialog.
pub struct NodeSelectionDialog {
    /// Whether the dialog is visible.
    pub visible: bool,
    /// Search query string.
    search_query: String,
    /// Selected category (None = All).
    selected_category: Option<String>,
    /// Filtered list of node indices.
    filtered_indices: Vec<usize>,
    /// Currently selected index in filtered list.
    selected_index: usize,
    /// Position where the node should be created.
    create_position: Point,
    /// Whether search input should be focused.
    focus_search: bool,
}

impl Default for NodeSelectionDialog {
    fn default() -> Self {
        Self::new()
    }
}

impl NodeSelectionDialog {
    /// Create a new node selection dialog.
    pub fn new() -> Self {
        let mut dialog = Self {
            visible: false,
            search_query: String::new(),
            selected_category: None,
            filtered_indices: Vec::new(),
            selected_index: 0,
            create_position: Point::ZERO,
            focus_search: false,
        };
        dialog.update_filtered_list();
        dialog
    }

    /// Open the dialog at the given position.
    pub fn open(&mut self, position: Point) {
        self.visible = true;
        self.search_query.clear();
        self.selected_category = None;
        self.selected_index = 0;
        self.create_position = position;
        self.focus_search = true;
        self.update_filtered_list();
    }

    /// Close the dialog.
    pub fn close(&mut self) {
        self.visible = false;
        self.search_query.clear();
    }

    /// Update the filtered list based on search query and category.
    fn update_filtered_list(&mut self) {
        self.filtered_indices.clear();
        let query = self.search_query.to_lowercase();

        for (i, template) in NODE_TEMPLATES.iter().enumerate() {
            // Filter by category
            if let Some(ref cat) = self.selected_category {
                if template.category != cat {
                    continue;
                }
            }

            // Filter by search query
            if !query.is_empty() {
                let matches = self.fuzzy_match(template, &query);
                if !matches {
                    continue;
                }
            }

            self.filtered_indices.push(i);
        }

        // Reset selection if out of bounds
        if self.selected_index >= self.filtered_indices.len() {
            self.selected_index = 0;
        }
    }

    /// Perform fuzzy matching on a template.
    fn fuzzy_match(&self, template: &NodeTemplate, query: &str) -> bool {
        let name = template.name.to_lowercase();
        let desc = template.description.to_lowercase();

        // Exact start match
        if name.starts_with(query) {
            return true;
        }

        // Contains match
        if name.contains(query) || desc.contains(query) {
            return true;
        }

        // First letters match (e.g., "rc" matches "rect create")
        let name_chars: Vec<char> = name.chars().collect();
        let query_chars: Vec<char> = query.chars().collect();

        if query_chars.len() <= name_chars.len() {
            let mut qi = 0;
            for &nc in &name_chars {
                if qi < query_chars.len() && nc == query_chars[qi] {
                    qi += 1;
                }
            }
            if qi == query_chars.len() {
                return true;
            }
        }

        false
    }

    /// Show the dialog. Returns the selected template if one was chosen.
    pub fn show(&mut self, ctx: &egui::Context, library: &NodeLibrary) -> Option<Node> {
        if !self.visible {
            return None;
        }

        let mut result = None;
        let mut should_close = false;

        // Handle keyboard input first
        ctx.input(|i| {
            if i.key_pressed(Key::Escape) {
                should_close = true;
            }
            if i.key_pressed(Key::ArrowDown) {
                if !self.filtered_indices.is_empty() {
                    self.selected_index = (self.selected_index + 1) % self.filtered_indices.len();
                }
            }
            if i.key_pressed(Key::ArrowUp) {
                if !self.filtered_indices.is_empty() {
                    if self.selected_index == 0 {
                        self.selected_index = self.filtered_indices.len() - 1;
                    } else {
                        self.selected_index -= 1;
                    }
                }
            }
        });

        // Modal window
        egui::Window::new("New Node")
            .collapsible(false)
            .resizable(false)
            .fixed_size([400.0, 350.0])
            .anchor(egui::Align2::CENTER_CENTER, [0.0, 0.0])
            .show(ctx, |ui| {
                // Search input
                ui.horizontal(|ui| {
                    ui.label("Search:");
                    let response = ui.add(
                        egui::TextEdit::singleline(&mut self.search_query)
                            .desired_width(300.0)
                            .hint_text("Type to search..."),
                    );

                    // Focus search on open
                    if self.focus_search {
                        response.request_focus();
                        self.focus_search = false;
                    }

                    // Update filter when search changes
                    if response.changed() {
                        self.update_filtered_list();
                    }

                    // Handle Enter key on search input
                    if response.lost_focus() && ui.input(|i| i.key_pressed(Key::Enter)) {
                        if let Some(&idx) = self.filtered_indices.get(self.selected_index) {
                            let template = &NODE_TEMPLATES[idx];
                            result = Some(create_node_from_template(template, library, self.create_position));
                            should_close = true;
                        }
                    }
                });

                ui.add_space(8.0);

                // Category buttons
                ui.horizontal(|ui| {
                    for &cat in CATEGORIES {
                        let is_selected = if cat == "All" {
                            self.selected_category.is_none()
                        } else {
                            self.selected_category.as_deref() == Some(cat)
                        };

                        if ui.selectable_label(is_selected, cat).clicked() {
                            if cat == "All" {
                                self.selected_category = None;
                            } else {
                                self.selected_category = Some(cat.to_string());
                            }
                            self.update_filtered_list();
                        }
                    }
                });

                ui.separator();

                // Node list
                egui::ScrollArea::vertical()
                    .max_height(250.0)
                    .show(ui, |ui| {
                        for (list_idx, &template_idx) in self.filtered_indices.iter().enumerate() {
                            let template = &NODE_TEMPLATES[template_idx];
                            let is_selected = list_idx == self.selected_index;

                            // Create a frame for the item
                            let response = ui.allocate_response(
                                Vec2::new(ui.available_width(), 32.0),
                                egui::Sense::click(),
                            );

                            // Background
                            let bg_color = if is_selected {
                                theme::SELECTED_ITEM
                            } else if response.hovered() {
                                theme::HOVERED_ITEM
                            } else {
                                Color32::TRANSPARENT
                            };

                            if bg_color != Color32::TRANSPARENT {
                                ui.painter().rect_filled(response.rect, 2.0, bg_color);
                            }

                            // Icon (colored by category)
                            let icon_rect = Rect::from_min_size(
                                response.rect.min + Vec2::new(8.0, 6.0),
                                Vec2::splat(20.0),
                            );
                            let icon_color = category_color(template.category);
                            ui.painter().rect_filled(icon_rect, 3.0, icon_color);

                            // Name
                            ui.painter().text(
                                response.rect.min + Vec2::new(36.0, 8.0),
                                egui::Align2::LEFT_TOP,
                                template.name,
                                egui::FontId::proportional(12.0),
                                theme::TEXT_BRIGHT,
                            );

                            // Description
                            ui.painter().text(
                                response.rect.min + Vec2::new(36.0, 22.0),
                                egui::Align2::LEFT_TOP,
                                template.description,
                                egui::FontId::proportional(10.0),
                                theme::TEXT_DISABLED,
                            );

                            // Handle click
                            if response.clicked() {
                                self.selected_index = list_idx;
                            }

                            // Handle double-click
                            if response.double_clicked() {
                                result = Some(create_node_from_template(template, library, self.create_position));
                                should_close = true;
                            }
                        }

                        if self.filtered_indices.is_empty() {
                            ui.vertical_centered(|ui| {
                                ui.add_space(50.0);
                                ui.label(
                                    egui::RichText::new("No matching nodes found.")
                                        .color(theme::TEXT_DISABLED),
                                );
                            });
                        }
                    });

                ui.separator();

                // Buttons
                ui.horizontal(|ui| {
                    if ui.button("Create").clicked() {
                        if let Some(&idx) = self.filtered_indices.get(self.selected_index) {
                            let template = &NODE_TEMPLATES[idx];
                            result = Some(create_node_from_template(template, library, self.create_position));
                            should_close = true;
                        }
                    }
                    if ui.button("Cancel").clicked() {
                        should_close = true;
                    }
                });
            });

        if should_close {
            self.close();
        }

        result
    }
}

/// Get color for a category.
fn category_color(category: &str) -> Color32 {
    match category {
        "geometry" => Color32::from_rgb(20, 20, 20),
        "transform" => Color32::from_rgb(119, 154, 173),
        "color" => Color32::from_rgb(94, 85, 112),
        _ => Color32::from_rgb(52, 85, 129),
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
                .with_input(Port::float("x", 0.0))
                .with_input(Port::float("y", 0.0))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0));
        }
        "rect" => {
            node = node
                .with_input(Port::float("x", 0.0))
                .with_input(Port::float("y", 0.0))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_input(Port::float("roundness", 0.0));
        }
        "line" => {
            node = node
                .with_input(Port::point("point1", Point::new(0.0, 0.0)))
                .with_input(Port::point("point2", Point::new(100.0, 100.0)));
        }
        "polygon" => {
            node = node
                .with_input(Port::float("x", 0.0))
                .with_input(Port::float("y", 0.0))
                .with_input(Port::float("radius", 50.0))
                .with_input(Port::int("sides", 6));
        }
        "star" => {
            node = node
                .with_input(Port::float("x", 0.0))
                .with_input(Port::float("y", 0.0))
                .with_input(Port::int("points", 5))
                .with_input(Port::float("outerRadius", 50.0))
                .with_input(Port::float("innerRadius", 25.0));
        }
        "arc" => {
            node = node
                .with_input(Port::float("x", 0.0))
                .with_input(Port::float("y", 0.0))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_input(Port::float("startAngle", 0.0))
                .with_input(Port::float("degrees", 360.0));
        }
        "grid" => {
            node = node
                .with_input(Port::int("rows", 5))
                .with_input(Port::int("columns", 5))
                .with_input(Port::float("width", 200.0))
                .with_input(Port::float("height", 200.0));
        }
        "translate" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::float("tx", 0.0))
                .with_input(Port::float("ty", 0.0));
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
                .with_input(Port::float("sx", 100.0))
                .with_input(Port::float("sy", 100.0));
        }
        "copy" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::int("copies", 10))
                .with_input(Port::float("tx", 0.0))
                .with_input(Port::float("ty", 0.0))
                .with_input(Port::float("rotate", 0.0))
                .with_input(Port::float("scale", 100.0));
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
                .with_input(Port::int("points", 50));
        }
        "wiggle" => {
            node = node
                .with_input(Port::geometry("shape"))
                .with_input(Port::float("offset", 10.0))
                .with_input(Port::int("seed", 0));
        }
        _ => {}
    }

    node
}
