//! UI panels for the NodeBox application.

use eframe::egui::{self, Color32, Sense, TextStyle};
use nodebox_core::node::{PortType, Widget};
use nodebox_core::Value;
use crate::state::AppState;
use crate::theme;

/// The parameter editor panel with Rerun-style minimal UI.
pub struct ParameterPanel {
    /// Fixed width for labels.
    label_width: f32,
    /// Track which port is being edited (node_name, port_name, edit_text, needs_select_all)
    editing: Option<(String, String, String, bool)>,
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
            editing: None,
        }
    }

    /// Show the parameter panel.
    pub fn show(&mut self, ui: &mut egui::Ui, state: &mut AppState) {
        // Apply minimal styling for the panel
        ui.style_mut().spacing.item_spacing = egui::vec2(8.0, 2.0);

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
                // Selection header - light gray bar with node name left, type right
                let header_rect = ui.available_rect_before_wrap();
                let header_rect = egui::Rect::from_min_size(
                    header_rect.min,
                    egui::vec2(header_rect.width(), theme::PARAMETER_ROW_HEIGHT),
                );
                ui.painter().rect_filled(
                    header_rect,
                    0.0,
                    Color32::from_rgb(75, 75, 75),
                );
                ui.allocate_ui_at_rect(header_rect, |ui| {
                    ui.horizontal_centered(|ui| {
                        ui.add_space(8.0);
                        // Node name on left
                        ui.label(
                            egui::RichText::new(node_display_name.as_deref().unwrap_or(&node.name))
                                .color(theme::TEXT_BRIGHT)
                                .size(11.0),
                        );
                        // Push type to the right
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            ui.add_space(8.0);
                            if let Some(ref proto) = node_prototype {
                                ui.label(
                                    egui::RichText::new(proto)
                                        .color(theme::TEXT_DISABLED)
                                        .size(11.0),
                                );
                            }
                        });
                    });
                });
                ui.add_space(theme::PARAMETER_ROW_HEIGHT + 8.0);

                // Clone node_name for use in closure
                let node_name_clone = node_name.clone();

                // Show input ports in a scrollable area
                egui::ScrollArea::vertical()
                    .auto_shrink([false, false])
                    .show(ui, |ui| {
                        for port in &mut node.inputs {
                            let is_connected = connected_ports.contains(&port.name);
                            self.show_port_row(ui, port, is_connected, &node_name_clone);
                        }
                    });
            } else {
                self.show_no_selection(ui, Some(&format!("Node '{}' not found.", node_name)));
            }
        } else {
            // No node selected - show document properties
            self.show_document_properties(ui, state);
        }
    }

    /// Show a single port row with label and value editor.
    fn show_port_row(
        &mut self,
        ui: &mut egui::Ui,
        port: &mut nodebox_core::node::Port,
        is_connected: bool,
        node_name: &str,
    ) {
        ui.horizontal(|ui| {
            ui.set_height(theme::PARAMETER_ROW_HEIGHT);

            // Fixed-width label, right-aligned (non-selectable)
            ui.allocate_ui_with_layout(
                egui::Vec2::new(self.label_width, theme::PARAMETER_ROW_HEIGHT),
                egui::Layout::right_to_left(egui::Align::Center),
                |ui| {
                    ui.add_space(8.0);
                    // Use painter to draw text directly (non-selectable)
                    let galley = ui.painter().layout_no_wrap(
                        port.name.clone(),
                        egui::FontId::proportional(11.0),
                        theme::TEXT_NORMAL,
                    );
                    let rect = ui.available_rect_before_wrap();
                    let pos = egui::pos2(
                        rect.right() - galley.size().x - 8.0,
                        rect.center().y - galley.size().y / 2.0,
                    );
                    ui.painter().galley(pos, galley, theme::TEXT_NORMAL);
                },
            );

            // Value editor
            if is_connected {
                // Non-selectable "connected" text
                let galley = ui.painter().layout_no_wrap(
                    "connected".to_string(),
                    egui::FontId::proportional(11.0),
                    theme::TEXT_DISABLED,
                );
                let rect = ui.available_rect_before_wrap();
                let pos = egui::pos2(rect.left(), rect.center().y - galley.size().y / 2.0);
                ui.painter().galley(pos, galley, theme::TEXT_DISABLED);
            } else {
                self.show_port_editor(ui, port, node_name);
            }
        });
    }

    /// Show the editor widget for a port value - minimal style with no borders.
    fn show_port_editor(&mut self, ui: &mut egui::Ui, port: &mut nodebox_core::node::Port, node_name: &str) {
        let port_key = (node_name.to_string(), port.name.clone());

        // Check if we're editing this port
        let is_editing = self.editing.as_ref()
            .map(|(n, p, _, _)| n == node_name && p == &port.name)
            .unwrap_or(false);

        match port.widget {
            Widget::Float | Widget::Angle => {
                if let Value::Float(ref mut value) = port.value {
                    self.show_drag_value_float(ui, value, port.min, port.max, 1.0, &port_key, is_editing);
                }
            }
            Widget::Int => {
                if let Value::Int(ref mut value) = port.value {
                    self.show_drag_value_int(ui, value, &port_key, is_editing);
                }
            }
            Widget::Toggle => {
                if let Value::Boolean(ref mut value) = port.value {
                    // Non-selectable clickable boolean
                    let text = if *value { "true" } else { "false" };
                    let galley = ui.painter().layout_no_wrap(
                        text.to_string(),
                        egui::FontId::proportional(11.0),
                        theme::VALUE_TEXT,
                    );
                    let rect = ui.available_rect_before_wrap();
                    let text_rect = egui::Rect::from_min_size(
                        egui::pos2(rect.left(), rect.center().y - galley.size().y / 2.0),
                        galley.size(),
                    );

                    let response = ui.allocate_rect(text_rect, Sense::click());
                    ui.painter().galley(text_rect.min, galley, theme::VALUE_TEXT);

                    if response.clicked() {
                        *value = !*value;
                    }
                    if response.hovered() {
                        ui.ctx().set_cursor_icon(egui::CursorIcon::PointingHand);
                    }
                }
            }
            Widget::String | Widget::Text => {
                if let Value::String(ref mut value) = port.value {
                    if is_editing {
                        // Show text input
                        let (mut edit_text, needs_select) = self.editing.as_ref()
                            .map(|(_, _, t, sel)| (t.clone(), *sel))
                            .unwrap_or_else(|| (value.clone(), true));

                        let output = egui::TextEdit::singleline(&mut edit_text)
                            .font(TextStyle::Body)
                            .text_color(theme::VALUE_TEXT)
                            .desired_width(120.0)
                            .frame(true)
                            .show(ui);

                        // Select all on first frame
                        if needs_select {
                            if let Some((_, _, _, ref mut sel)) = self.editing {
                                *sel = false;
                            }
                            // Set cursor to select all
                            let text_len = edit_text.chars().count();
                            let mut state = output.state.clone();
                            state.cursor.set_char_range(Some(egui::text::CCursorRange::two(
                                egui::text::CCursor::new(0),
                                egui::text::CCursor::new(text_len),
                            )));
                            state.store(ui.ctx(), output.response.id);
                        }

                        // Update edit text
                        if let Some((_, _, ref mut t, _)) = self.editing {
                            *t = edit_text.clone();
                        }

                        // Commit on enter or focus lost
                        if output.response.lost_focus() {
                            if ui.input(|i| i.key_pressed(egui::Key::Escape)) {
                                self.editing = None;
                            } else {
                                *value = edit_text;
                                self.editing = None;
                            }
                        }

                        // Request focus on first frame
                        output.response.request_focus();
                    } else {
                        // Show as clickable text
                        let display = if value.is_empty() { "\"\"" } else { value.as_str() };
                        let galley = ui.painter().layout_no_wrap(
                            display.to_string(),
                            egui::FontId::proportional(11.0),
                            theme::VALUE_TEXT,
                        );
                        let rect = ui.available_rect_before_wrap();
                        let text_rect = egui::Rect::from_min_size(
                            egui::pos2(rect.left(), rect.center().y - galley.size().y / 2.0),
                            galley.size(),
                        );

                        let response = ui.allocate_rect(text_rect, Sense::click());
                        ui.painter().galley(text_rect.min, galley, theme::VALUE_TEXT);

                        if response.clicked() {
                            self.editing = Some((port_key.0, port_key.1, value.clone(), true));
                        }
                        if response.hovered() {
                            ui.ctx().set_cursor_icon(egui::CursorIcon::Text);
                        }
                    }
                }
            }
            Widget::Color => {
                if let Value::Color(ref mut color) = port.value {
                    let mut rgba = [
                        (color.r * 255.0) as u8,
                        (color.g * 255.0) as u8,
                        (color.b * 255.0) as u8,
                        (color.a * 255.0) as u8,
                    ];
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
                    let key_x = (port_key.0.clone(), format!("{}_x", port_key.1));
                    let key_y = (port_key.0.clone(), format!("{}_y", port_key.1));
                    let is_editing_x = self.editing.as_ref()
                        .map(|(n, p, _, _)| n == &key_x.0 && p == &key_x.1)
                        .unwrap_or(false);
                    let is_editing_y = self.editing.as_ref()
                        .map(|(n, p, _, _)| n == &key_y.0 && p == &key_y.1)
                        .unwrap_or(false);
                    self.show_drag_value_float(ui, &mut point.x, None, None, 1.0, &key_x, is_editing_x);
                    ui.add_space(4.0);
                    self.show_drag_value_float(ui, &mut point.y, None, None, 1.0, &key_y, is_editing_y);
                }
            }
            _ => {
                // For geometry and other non-editable types, show type info (non-selectable)
                let type_str = match port.port_type {
                    PortType::Geometry => "Geometry",
                    _ => port.port_type.as_str(),
                };
                let galley = ui.painter().layout_no_wrap(
                    type_str.to_string(),
                    egui::FontId::proportional(11.0),
                    theme::TEXT_DISABLED,
                );
                let rect = ui.available_rect_before_wrap();
                let pos = egui::pos2(rect.left(), rect.center().y - galley.size().y / 2.0);
                ui.painter().galley(pos, galley, theme::TEXT_DISABLED);
            }
        }
    }

    /// Show a minimal drag value for floats - non-selectable, draggable, click to edit.
    fn show_drag_value_float(
        &mut self,
        ui: &mut egui::Ui,
        value: &mut f64,
        min: Option<f64>,
        max: Option<f64>,
        speed: f64,
        port_key: &(String, String),
        is_editing: bool,
    ) {
        if is_editing {
            // Show text input for direct editing
            let (mut edit_text, needs_select) = self.editing.as_ref()
                .map(|(_, _, t, sel)| (t.clone(), *sel))
                .unwrap_or_else(|| (format!("{:.2}", value), true));

            let output = egui::TextEdit::singleline(&mut edit_text)
                .font(TextStyle::Body)
                .text_color(theme::VALUE_TEXT)
                .desired_width(60.0)
                .frame(true)
                .show(ui);

            // Select all on first frame
            if needs_select {
                if let Some((_, _, _, ref mut sel)) = self.editing {
                    *sel = false;
                }
                let text_len = edit_text.chars().count();
                let mut state = output.state.clone();
                state.cursor.set_char_range(Some(egui::text::CCursorRange::two(
                    egui::text::CCursor::new(0),
                    egui::text::CCursor::new(text_len),
                )));
                state.store(ui.ctx(), output.response.id);
            }

            // Update edit text
            if let Some((_, _, ref mut t, _)) = self.editing {
                *t = edit_text.clone();
            }

            // Commit on enter or focus lost
            if output.response.lost_focus() {
                if ui.input(|i| i.key_pressed(egui::Key::Escape)) {
                    self.editing = None;
                } else if let Ok(new_val) = edit_text.parse::<f64>() {
                    let mut clamped = new_val;
                    if let Some(min_val) = min {
                        clamped = clamped.max(min_val);
                    }
                    if let Some(max_val) = max {
                        clamped = clamped.min(max_val);
                    }
                    *value = clamped;
                    self.editing = None;
                } else {
                    self.editing = None;
                }
            }

            output.response.request_focus();
        } else {
            // Show as draggable text (non-selectable)
            let text = format!("{:.2}", value);
            let galley = ui.painter().layout_no_wrap(
                text.clone(),
                egui::FontId::proportional(11.0),
                theme::VALUE_TEXT,
            );
            let rect = ui.available_rect_before_wrap();
            let text_rect = egui::Rect::from_min_size(
                egui::pos2(rect.left(), rect.center().y - galley.size().y / 2.0),
                galley.size(),
            );

            let response = ui.allocate_rect(text_rect, Sense::click_and_drag());
            ui.painter().galley(text_rect.min, galley, theme::VALUE_TEXT);

            if response.dragged() {
                // Modifier keys: Shift = x10, Alt = /100
                let modifier = ui.input(|i| {
                    if i.modifiers.shift {
                        10.0
                    } else if i.modifiers.alt {
                        0.01
                    } else {
                        1.0
                    }
                });
                let delta = response.drag_delta().x as f64 * speed * modifier;
                *value += delta;
                if let Some(min_val) = min {
                    *value = value.max(min_val);
                }
                if let Some(max_val) = max {
                    *value = value.min(max_val);
                }
            }

            if response.hovered() {
                ui.ctx().set_cursor_icon(egui::CursorIcon::ResizeHorizontal);
            }

            // Click to edit
            if response.clicked() {
                self.editing = Some((port_key.0.clone(), port_key.1.clone(), format!("{:.2}", value), true));
            }
        }
    }

    /// Show a minimal drag value for ints - non-selectable, draggable, click to edit.
    fn show_drag_value_int(&mut self, ui: &mut egui::Ui, value: &mut i64, port_key: &(String, String), is_editing: bool) {
        if is_editing {
            // Show text input for direct editing
            let (mut edit_text, needs_select) = self.editing.as_ref()
                .map(|(_, _, t, sel)| (t.clone(), *sel))
                .unwrap_or_else(|| (format!("{}", value), true));

            let output = egui::TextEdit::singleline(&mut edit_text)
                .font(TextStyle::Body)
                .text_color(theme::VALUE_TEXT)
                .desired_width(60.0)
                .frame(true)
                .show(ui);

            // Select all on first frame
            if needs_select {
                if let Some((_, _, _, ref mut sel)) = self.editing {
                    *sel = false;
                }
                let text_len = edit_text.chars().count();
                let mut state = output.state.clone();
                state.cursor.set_char_range(Some(egui::text::CCursorRange::two(
                    egui::text::CCursor::new(0),
                    egui::text::CCursor::new(text_len),
                )));
                state.store(ui.ctx(), output.response.id);
            }

            if let Some((_, _, ref mut t, _)) = self.editing {
                *t = edit_text.clone();
            }

            if output.response.lost_focus() {
                if ui.input(|i| i.key_pressed(egui::Key::Escape)) {
                    self.editing = None;
                } else if let Ok(new_val) = edit_text.parse::<i64>() {
                    *value = new_val;
                    self.editing = None;
                } else {
                    self.editing = None;
                }
            }

            output.response.request_focus();
        } else {
            let text = format!("{}", value);
            let galley = ui.painter().layout_no_wrap(
                text.clone(),
                egui::FontId::proportional(11.0),
                theme::VALUE_TEXT,
            );
            let rect = ui.available_rect_before_wrap();
            let text_rect = egui::Rect::from_min_size(
                egui::pos2(rect.left(), rect.center().y - galley.size().y / 2.0),
                galley.size(),
            );

            let response = ui.allocate_rect(text_rect, Sense::click_and_drag());
            ui.painter().galley(text_rect.min, galley, theme::VALUE_TEXT);

            if response.dragged() {
                // Modifier keys: Shift = x10, Alt = /100
                let modifier = ui.input(|i| {
                    if i.modifiers.shift {
                        10.0
                    } else if i.modifiers.alt {
                        0.01
                    } else {
                        1.0
                    }
                });
                let delta = response.drag_delta().x as f64 * modifier;
                *value += delta as i64;
            }

            if response.hovered() {
                ui.ctx().set_cursor_icon(egui::CursorIcon::ResizeHorizontal);
            }

            if response.clicked() {
                self.editing = Some((port_key.0.clone(), port_key.1.clone(), format!("{}", value), true));
            }
        }
    }

    /// Show document properties when no node is selected.
    fn show_no_selection(&mut self, ui: &mut egui::Ui, error: Option<&str>) {
        if let Some(err) = error {
            ui.vertical_centered(|ui| {
                ui.add_space(30.0);
                ui.label(
                    egui::RichText::new(err)
                        .color(Color32::from_rgb(255, 100, 100))
                        .size(12.0),
                );
            });
        } else {
            // Show document properties header
            let header_rect = ui.available_rect_before_wrap();
            let header_rect = egui::Rect::from_min_size(
                header_rect.min,
                egui::vec2(header_rect.width(), theme::PARAMETER_ROW_HEIGHT),
            );
            ui.painter().rect_filled(
                header_rect,
                0.0,
                Color32::from_rgb(75, 75, 75),
            );
            ui.allocate_ui_at_rect(header_rect, |ui| {
                ui.horizontal_centered(|ui| {
                    ui.add_space(8.0);
                    ui.label(
                        egui::RichText::new("Document")
                            .color(theme::TEXT_BRIGHT)
                            .size(11.0),
                    );
                });
            });
            ui.add_space(theme::PARAMETER_ROW_HEIGHT + 8.0);

            // Document properties will be shown in show() method
            // This is just for the error/no-document case
            ui.vertical_centered(|ui| {
                ui.add_space(10.0);
                ui.label(
                    egui::RichText::new("Select a node to edit parameters")
                        .color(theme::TEXT_DISABLED)
                        .size(11.0),
                );
            });
        }
    }

    /// Show document properties panel (canvas size, etc.).
    pub fn show_document_properties(&mut self, ui: &mut egui::Ui, state: &mut AppState) {
        // Apply minimal styling for the panel
        ui.style_mut().spacing.item_spacing = egui::vec2(8.0, 2.0);

        // Document properties header
        let header_rect = ui.available_rect_before_wrap();
        let header_rect = egui::Rect::from_min_size(
            header_rect.min,
            egui::vec2(header_rect.width(), theme::PARAMETER_ROW_HEIGHT),
        );
        ui.painter().rect_filled(
            header_rect,
            0.0,
            Color32::from_rgb(75, 75, 75),
        );
        ui.allocate_ui_at_rect(header_rect, |ui| {
            ui.horizontal_centered(|ui| {
                ui.add_space(8.0);
                ui.label(
                    egui::RichText::new("Document")
                        .color(theme::TEXT_BRIGHT)
                        .size(11.0),
                );
            });
        });
        ui.add_space(theme::PARAMETER_ROW_HEIGHT + 8.0);

        // Canvas width
        ui.horizontal(|ui| {
            ui.set_height(theme::PARAMETER_ROW_HEIGHT);

            // Label
            ui.allocate_ui_with_layout(
                egui::Vec2::new(self.label_width, theme::PARAMETER_ROW_HEIGHT),
                egui::Layout::right_to_left(egui::Align::Center),
                |ui| {
                    ui.add_space(8.0);
                    let galley = ui.painter().layout_no_wrap(
                        "canvasWidth".to_string(),
                        egui::FontId::proportional(11.0),
                        theme::TEXT_NORMAL,
                    );
                    let rect = ui.available_rect_before_wrap();
                    let pos = egui::pos2(
                        rect.right() - galley.size().x - 8.0,
                        rect.center().y - galley.size().y / 2.0,
                    );
                    ui.painter().galley(pos, galley, theme::TEXT_NORMAL);
                },
            );

            // Value
            let mut width = state.library.canvas_width();
            let key = ("__document__".to_string(), "canvasWidth".to_string());
            let is_editing = self.editing.as_ref()
                .map(|(n, p, _, _)| n == &key.0 && p == &key.1)
                .unwrap_or(false);
            self.show_drag_value_float(ui, &mut width, Some(1.0), None, 1.0, &key, is_editing);

            // Update the property if changed
            let new_width = (width as i64).to_string();
            if state.library.property("canvasWidth") != Some(&new_width) {
                state.library.set_property("canvasWidth", new_width);
            }
        });

        // Canvas height
        ui.horizontal(|ui| {
            ui.set_height(theme::PARAMETER_ROW_HEIGHT);

            // Label
            ui.allocate_ui_with_layout(
                egui::Vec2::new(self.label_width, theme::PARAMETER_ROW_HEIGHT),
                egui::Layout::right_to_left(egui::Align::Center),
                |ui| {
                    ui.add_space(8.0);
                    let galley = ui.painter().layout_no_wrap(
                        "canvasHeight".to_string(),
                        egui::FontId::proportional(11.0),
                        theme::TEXT_NORMAL,
                    );
                    let rect = ui.available_rect_before_wrap();
                    let pos = egui::pos2(
                        rect.right() - galley.size().x - 8.0,
                        rect.center().y - galley.size().y / 2.0,
                    );
                    ui.painter().galley(pos, galley, theme::TEXT_NORMAL);
                },
            );

            // Value
            let mut height = state.library.canvas_height();
            let key = ("__document__".to_string(), "canvasHeight".to_string());
            let is_editing = self.editing.as_ref()
                .map(|(n, p, _, _)| n == &key.0 && p == &key.1)
                .unwrap_or(false);
            self.show_drag_value_float(ui, &mut height, Some(1.0), None, 1.0, &key, is_editing);

            // Update the property if changed
            let new_height = (height as i64).to_string();
            if state.library.property("canvasHeight") != Some(&new_height) {
                state.library.set_property("canvasHeight", new_height);
            }
        });
    }
}
