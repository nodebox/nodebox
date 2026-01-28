//! UI panels for the NodeBox application.

use eframe::egui;
use nodebox_core::node::{PortType, Widget};
use nodebox_core::Value;
use crate::state::AppState;

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
        if let Some(ref node_name) = state.selected_node.clone() {
            // Find the node in the library
            if let Some(node) = state.library.root.child_mut(node_name) {
                ui.heading(&node.name);
                if let Some(ref proto) = node.prototype {
                    ui.label(format!("Type: {}", proto));
                }
                ui.add_space(10.0);
                ui.separator();

                // Show input ports
                egui::ScrollArea::vertical().show(ui, |ui| {
                    for port in &mut node.inputs {
                        ui.add_space(5.0);

                        match port.widget {
                            Widget::Float | Widget::Angle => {
                                if let Value::Float(ref mut value) = port.value {
                                    ui.horizontal(|ui| {
                                        ui.label(&port.name);
                                        let mut drag = egui::DragValue::new(value).speed(1.0);
                                        if let Some(min) = port.min {
                                            drag = drag.range(min..=f64::MAX);
                                        }
                                        if let Some(max) = port.max {
                                            drag = drag.range(f64::MIN..=max);
                                        }
                                        ui.add(drag);
                                    });
                                }
                            }
                            Widget::Int => {
                                if let Value::Int(ref mut value) = port.value {
                                    ui.horizontal(|ui| {
                                        ui.label(&port.name);
                                        ui.add(egui::DragValue::new(value).speed(1.0));
                                    });
                                }
                            }
                            Widget::Toggle => {
                                if let Value::Boolean(ref mut value) = port.value {
                                    ui.checkbox(value, &port.name);
                                }
                            }
                            Widget::String | Widget::Text => {
                                if let Value::String(ref mut value) = port.value {
                                    ui.horizontal(|ui| {
                                        ui.label(&port.name);
                                        ui.text_edit_singleline(value);
                                    });
                                }
                            }
                            Widget::Color => {
                                if let Value::Color(ref mut color) = port.value {
                                    ui.horizontal(|ui| {
                                        ui.label(&port.name);
                                        let mut rgba = [
                                            color.r as f32,
                                            color.g as f32,
                                            color.b as f32,
                                            color.a as f32,
                                        ];
                                        if ui.color_edit_button_rgba_unmultiplied(&mut rgba).changed() {
                                            color.r = rgba[0] as f64;
                                            color.g = rgba[1] as f64;
                                            color.b = rgba[2] as f64;
                                            color.a = rgba[3] as f64;
                                        }
                                    });
                                }
                            }
                            Widget::Point => {
                                if let Value::Point(ref mut point) = port.value {
                                    ui.horizontal(|ui| {
                                        ui.label(&port.name);
                                    });
                                    ui.horizontal(|ui| {
                                        ui.label("  X:");
                                        ui.add(egui::DragValue::new(&mut point.x).speed(1.0));
                                        ui.label("Y:");
                                        ui.add(egui::DragValue::new(&mut point.y).speed(1.0));
                                    });
                                }
                            }
                            _ => {
                                // For geometry and other non-editable types, just show the name
                                match port.port_type {
                                    PortType::Geometry => {
                                        ui.label(format!("{}: (connected)", port.name));
                                    }
                                    _ => {
                                        ui.label(format!("{}: {}", port.name, port.port_type.as_str()));
                                    }
                                }
                            }
                        }
                    }
                });
            } else {
                ui.label(format!("Node '{}' not found.", node_name));
            }
        } else {
            ui.label("Select a node to edit its parameters.");
        }
    }
}
