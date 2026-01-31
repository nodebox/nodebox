//! UI panels for the NodeBox application.

use eframe::egui::{self, Color32};
use nodebox_core::node::{PortType, Widget};
use nodebox_core::Value;
use crate::state::AppState;
use crate::theme;

/// The parameter editor panel.
pub struct ParameterPanel {
    /// Fixed width for labels.
    label_width: f32,
}

impl Default for ParameterPanel {
    fn default() -> Self {
        Self::new()
    }
}

impl ParameterPanel {
    /// Create a new parameter panel.
    pub fn new() -> Self {
        Self {
            label_width: theme::LABEL_WIDTH,
        }
    }

    /// Show the parameter panel.
    pub fn show(&mut self, ui: &mut egui::Ui, state: &mut AppState) {
        if let Some(ref node_name) = state.selected_node.clone() {
            // First, collect connected ports while we only have immutable borrow
            let connected_ports: std::collections::HashSet<String> = state
                .library
                .root
                .connections
                .iter()
                .filter(|c| c.input_node == *node_name)
                .map(|c| c.input_port.clone())
                .collect();

            // Also collect display info before mutable borrow
            let (node_display_name, node_prototype) = {
                if let Some(node) = state.library.root.child(&node_name) {
                    (Some(node.name.clone()), node.prototype.clone())
                } else {
                    (None, None)
                }
            };

            // Find the node in the library for mutation
            if let Some(node) = state.library.root.child_mut(&node_name) {
                // Node header with name
                ui.horizontal(|ui| {
                    ui.add_space(4.0);
                    ui.label(
                        egui::RichText::new(node_display_name.as_deref().unwrap_or(&node.name))
                            .color(theme::TEXT_BRIGHT)
                            .size(14.0)
                            .strong(),
                    );
                });

                if let Some(ref proto) = node_prototype {
                    ui.horizontal(|ui| {
                        ui.add_space(4.0);
                        ui.label(
                            egui::RichText::new(proto)
                                .color(theme::TEXT_DISABLED)
                                .size(11.0),
                        );
                    });
                }
                ui.add_space(4.0);
                ui.separator();

                // Show input ports in a scrollable area
                egui::ScrollArea::vertical()
                    .auto_shrink([false, false])
                    .show(ui, |ui| {
                        for port in &mut node.inputs {
                            let is_connected = connected_ports.contains(&port.name);
                            self.show_port_row(ui, port, is_connected);
                        }
                    });
            } else {
                self.show_no_selection(ui, Some(&format!("Node '{}' not found.", node_name)));
            }
        } else {
            self.show_no_selection(ui, None);
        }
    }

    /// Show a single port row with label and value editor.
    fn show_port_row(
        &self,
        ui: &mut egui::Ui,
        port: &mut nodebox_core::node::Port,
        is_connected: bool,
    ) {
        ui.horizontal(|ui| {
            // Fixed-width label
            ui.allocate_ui_with_layout(
                egui::Vec2::new(self.label_width, 20.0),
                egui::Layout::left_to_right(egui::Align::Center),
                |ui| {
                    ui.add_space(4.0);
                    ui.label(
                        egui::RichText::new(&port.name)
                            .color(theme::TEXT_NORMAL)
                            .size(11.0),
                    );
                },
            );

            // Value editor
            if is_connected {
                ui.label(
                    egui::RichText::new("<connected>")
                        .color(theme::TEXT_DISABLED)
                        .size(11.0)
                        .italics(),
                );
            } else {
                self.show_port_editor(ui, port);
            }
        });
    }

    /// Show the editor widget for a port value.
    fn show_port_editor(&self, ui: &mut egui::Ui, port: &mut nodebox_core::node::Port) {
        match port.widget {
            Widget::Float | Widget::Angle => {
                if let Value::Float(ref mut value) = port.value {
                    let mut drag = egui::DragValue::new(value).speed(1.0);
                    if let Some(min) = port.min {
                        drag = drag.range(min..=f64::MAX);
                    }
                    if let Some(max) = port.max {
                        drag = drag.range(f64::MIN..=max);
                    }
                    ui.add(drag);
                }
            }
            Widget::Int => {
                if let Value::Int(ref mut value) = port.value {
                    ui.add(egui::DragValue::new(value).speed(1.0));
                }
            }
            Widget::Toggle => {
                if let Value::Boolean(ref mut value) = port.value {
                    ui.checkbox(value, "");
                }
            }
            Widget::String | Widget::Text => {
                if let Value::String(ref mut value) = port.value {
                    ui.add(egui::TextEdit::singleline(value).desired_width(80.0));
                }
            }
            Widget::Color => {
                if let Value::Color(ref mut color) = port.value {
                    // Convert 0-1 floats to 0-255 bytes for sRGB color picker
                    let mut rgba = [
                        (color.r * 255.0) as u8,
                        (color.g * 255.0) as u8,
                        (color.b * 255.0) as u8,
                        (color.a * 255.0) as u8,
                    ];
                    // Use sRGB color picker since our Color values are in sRGB space
                    if ui.color_edit_button_srgba_unmultiplied(&mut rgba).changed() {
                        color.r = rgba[0] as f64 / 255.0;
                        color.g = rgba[1] as f64 / 255.0;
                        color.b = rgba[2] as f64 / 255.0;
                        color.a = rgba[3] as f64 / 255.0;
                    }
                }
            }
            Widget::Point => {
                if let Value::Point(ref mut point) = port.value {
                    ui.label(
                        egui::RichText::new("X:")
                            .color(theme::TEXT_DISABLED)
                            .size(10.0),
                    );
                    ui.add(egui::DragValue::new(&mut point.x).speed(1.0));
                    ui.label(
                        egui::RichText::new("Y:")
                            .color(theme::TEXT_DISABLED)
                            .size(10.0),
                    );
                    ui.add(egui::DragValue::new(&mut point.y).speed(1.0));
                }
            }
            _ => {
                // For geometry and other non-editable types, show type info
                match port.port_type {
                    PortType::Geometry => {
                        ui.label(
                            egui::RichText::new("Geometry")
                                .color(theme::TEXT_DISABLED)
                                .size(11.0),
                        );
                    }
                    _ => {
                        ui.label(
                            egui::RichText::new(port.port_type.as_str())
                                .color(theme::TEXT_DISABLED)
                                .size(11.0),
                        );
                    }
                }
            }
        }
    }

    /// Show message when no node is selected.
    fn show_no_selection(&self, ui: &mut egui::Ui, error: Option<&str>) {
        ui.vertical_centered(|ui| {
            ui.add_space(30.0);
            if let Some(err) = error {
                ui.label(
                    egui::RichText::new(err)
                        .color(Color32::from_rgb(255, 100, 100))
                        .size(12.0),
                );
            } else {
                ui.label(
                    egui::RichText::new("Select a node to edit parameters")
                        .color(theme::TEXT_DISABLED)
                        .size(11.0),
                );
            }
        });
    }
}
