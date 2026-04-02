//! Node library browser for creating new nodes.
//!
//! Note: This module is work-in-progress and not yet integrated.

#![allow(dead_code)]

use std::sync::Arc;
use eframe::egui;
use nodebox_core::geometry::Point;
use nodebox_core::node::NodeLibrary;
// Re-export from nodebox-eval for other modules in this crate
pub use nodebox_eval::{NodeTemplate, NODE_TEMPLATES, create_node_from_template, template_has_compatible_input};

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
