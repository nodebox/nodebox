//! UI panels for the NodeBox application.

use eframe::egui;
use crate::state::AppState;

/// The node graph panel.
pub struct NodeGraphPanel {
    // Node graph state will go here
}

impl Default for NodeGraphPanel {
    fn default() -> Self {
        Self::new()
    }
}

impl NodeGraphPanel {
    /// Create a new node graph panel.
    pub fn new() -> Self {
        Self {}
    }

    /// Show the node graph panel.
    pub fn show(&mut self, ui: &mut egui::Ui, state: &mut AppState) {
        ui.label("Node graph editor will appear here.");
        ui.add_space(10.0);

        // Placeholder node list
        egui::ScrollArea::vertical().show(ui, |ui| {
            ui.collapsing("Geometry", |ui| {
                if ui.selectable_label(state.selected_node == Some("ellipse".to_string()), "ellipse").clicked() {
                    state.selected_node = Some("ellipse".to_string());
                }
                if ui.selectable_label(state.selected_node == Some("rect".to_string()), "rect").clicked() {
                    state.selected_node = Some("rect".to_string());
                }
                if ui.selectable_label(state.selected_node == Some("star".to_string()), "star").clicked() {
                    state.selected_node = Some("star".to_string());
                }
                if ui.selectable_label(state.selected_node == Some("polygon".to_string()), "polygon").clicked() {
                    state.selected_node = Some("polygon".to_string());
                }
            });

            ui.collapsing("Transform", |ui| {
                if ui.selectable_label(state.selected_node == Some("translate".to_string()), "translate").clicked() {
                    state.selected_node = Some("translate".to_string());
                }
                if ui.selectable_label(state.selected_node == Some("rotate".to_string()), "rotate").clicked() {
                    state.selected_node = Some("rotate".to_string());
                }
                if ui.selectable_label(state.selected_node == Some("scale".to_string()), "scale").clicked() {
                    state.selected_node = Some("scale".to_string());
                }
            });

            ui.collapsing("Color", |ui| {
                if ui.selectable_label(state.selected_node == Some("colorize".to_string()), "colorize").clicked() {
                    state.selected_node = Some("colorize".to_string());
                }
            });
        });
    }
}

/// The parameter editor panel.
pub struct ParameterPanel {
    // Parameter panel state will go here
}

impl Default for ParameterPanel {
    fn default() -> Self {
        Self::new()
    }
}

impl ParameterPanel {
    /// Create a new parameter panel.
    pub fn new() -> Self {
        Self {}
    }

    /// Show the parameter panel.
    pub fn show(&mut self, ui: &mut egui::Ui, state: &mut AppState) {
        if let Some(ref node_name) = state.selected_node {
            ui.label(format!("Selected: {}", node_name));
            ui.add_space(10.0);

            // Show placeholder parameters based on node type
            match node_name.as_str() {
                "ellipse" => {
                    ui.horizontal(|ui| {
                        ui.label("Position:");
                    });
                    ui.horizontal(|ui| {
                        ui.label("X:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                        ui.label("Y:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Width:");
                        ui.add(egui::DragValue::new(&mut 100.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Height:");
                        ui.add(egui::DragValue::new(&mut 100.0).speed(1.0));
                    });
                }
                "rect" => {
                    ui.horizontal(|ui| {
                        ui.label("Position:");
                    });
                    ui.horizontal(|ui| {
                        ui.label("X:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                        ui.label("Y:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Width:");
                        ui.add(egui::DragValue::new(&mut 100.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Height:");
                        ui.add(egui::DragValue::new(&mut 80.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Roundness:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                    });
                }
                "star" => {
                    ui.horizontal(|ui| {
                        ui.label("Position:");
                    });
                    ui.horizontal(|ui| {
                        ui.label("X:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                        ui.label("Y:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Points:");
                        ui.add(egui::DragValue::new(&mut 5_i32).speed(1.0).range(3..=20));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Outer Radius:");
                        ui.add(egui::DragValue::new(&mut 50.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Inner Radius:");
                        ui.add(egui::DragValue::new(&mut 25.0).speed(1.0));
                    });
                }
                "polygon" => {
                    ui.horizontal(|ui| {
                        ui.label("Position:");
                    });
                    ui.horizontal(|ui| {
                        ui.label("X:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                        ui.label("Y:");
                        ui.add(egui::DragValue::new(&mut 0.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Radius:");
                        ui.add(egui::DragValue::new(&mut 50.0).speed(1.0));
                    });
                    ui.horizontal(|ui| {
                        ui.label("Sides:");
                        ui.add(egui::DragValue::new(&mut 6_i32).speed(1.0).range(3..=20));
                    });
                }
                "translate" | "rotate" | "scale" | "colorize" => {
                    ui.label("Transform parameters will appear here.");
                }
                _ => {
                    ui.label("Unknown node type.");
                }
            }
        } else {
            ui.label("Select a node to edit its parameters.");
        }
    }
}
