//! UI panels for the NodeBox application.

use std::sync::Arc;
use eframe::egui::{self, Sense, TextStyle};
use nodebox_core::geometry::Color;
use nodebox_core::node::{PortType, Widget};
use nodebox_core::Value;
use nodebox_port::{FileFilter, Port, PortError, ProjectContext};
use crate::components;
use crate::state::AppState;
use crate::theme;

/// The parameter editor panel with Rerun-style minimal UI.
pub struct ParameterPanel {
    /// Fixed width for labels.
    label_width: f32,
    /// Track which port is being edited (node_name, port_name, edit_text, needs_select_all)
    editing: Option<(String, String, String, bool)>,
    /// Ordered list of tabbable (node_name, port_name) keys, rebuilt every frame.
    tab_order: Vec<(String, String)>,
    /// Deferred tab target: set when Tab/Shift+Tab is pressed, consumed next frame.
    tab_target: Option<(String, String)>,
    /// When true, the current edit was initiated from a label click on a Point field.
    /// On commit, the value should be applied to both x and y.
    label_edit_apply_both: bool,
    /// Set to the committed value when a label-initiated Point edit commits.
    label_edit_committed_value: Option<f64>,
    /// Accumulates sub-pixel drag deltas so that default (non-Alt) drags snap to integers.
    drag_accumulator: f64,
}

impl Default for ParameterPanel {
    fn default() -> Self {
        Self::new()
    }
}

impl ParameterPanel {
    /// Create a new parameter panel.
    /// The label_width is set to theme::LABEL_WIDTH to align with the pane header separator.
    pub fn new() -> Self {
        Self {
            label_width: theme::LABEL_WIDTH,
            editing: None,
            tab_order: Vec::new(),
            tab_target: None,
            label_edit_apply_both: false,
            label_edit_committed_value: None,
            drag_accumulator: 0.0,
        }
    }

    /// Show the parameter panel.
    pub fn show(
        &mut self,
        ui: &mut egui::Ui,
        state: &mut AppState,
        port: &dyn Port,
        project_context: &ProjectContext,
    ) {
        // Zero spacing so header sits flush against content
        ui.style_mut().spacing.item_spacing = egui::vec2(0.0, 0.0);

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

            // Build tab order from the immutable view of the node's inputs
            if let Some(node) = state.library.root.child(&node_name) {
                self.tab_order = Self::build_tab_order(&node_name, &node.inputs, &connected_ports);

                // Activate pending tab target
                if let Some(ref target) = self.tab_target.take() {
                    if self.tab_order.iter().any(|k| k == target) {
                        let edit_text = Self::get_edit_text_for_target(&node.inputs, target);
                        self.editing = Some((target.0.clone(), target.1.clone(), edit_text, true));
                    }
                }
            }

            // Show header before mutable borrow
            self.show_parameters_header(
                ui,
                node_display_name.as_deref(),
                node_prototype.as_deref(),
            );

            // Restore row spacing for content
            ui.style_mut().spacing.item_spacing = egui::vec2(8.0, 2.0);

            // Find the node in the library for mutation
            if let Some(node) = Arc::make_mut(&mut state.library).root.child_mut(&node_name) {
                // Clone node_name for use in closure
                let node_name_clone = node_name.clone();

                // Show input ports in a scrollable area with two-tone background
                egui::ScrollArea::vertical()
                    .auto_shrink([false, false])
                    .show(ui, |ui| {
                        // Paint two-tone background
                        let full_rect = ui.max_rect();
                        // Left side (labels) - darker
                        ui.painter().rect_filled(
                            egui::Rect::from_min_max(
                                full_rect.min,
                                egui::pos2(full_rect.left() + self.label_width, full_rect.max.y),
                            ),
                            0.0,
                            theme::PORT_LABEL_BACKGROUND,
                        );
                        // Right side (values) - lighter
                        ui.painter().rect_filled(
                            egui::Rect::from_min_max(
                                egui::pos2(full_rect.left() + self.label_width, full_rect.min.y),
                                full_rect.max,
                            ),
                            0.0,
                            theme::PORT_VALUE_BACKGROUND,
                        );

                        ui.add_space(theme::PADDING);

                        for node_port in &mut node.inputs {
                            let is_connected = connected_ports.contains(&node_port.name);
                            self.show_port_row(
                                ui,
                                node_port,
                                is_connected,
                                &node_name_clone,
                                port,
                                project_context,
                            );
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

    /// Build the tab order for the given node's inputs.
    /// Returns a list of (node_name, port_key) pairs for all tabbable fields in order.
    /// Point fields produce two entries: (node, "name_x") and (node, "name_y").
    fn build_tab_order(
        node_name: &str,
        inputs: &[nodebox_core::node::Port],
        connected_ports: &std::collections::HashSet<String>,
    ) -> Vec<(String, String)> {
        let mut order = Vec::new();
        for port in inputs {
            if connected_ports.contains(&port.name) {
                continue;
            }
            match port.widget {
                Widget::Float | Widget::Angle | Widget::Int | Widget::String | Widget::Text => {
                    order.push((node_name.to_string(), port.name.clone()));
                }
                Widget::Point => {
                    order.push((node_name.to_string(), format!("{}_x", port.name)));
                    order.push((node_name.to_string(), format!("{}_y", port.name)));
                }
                _ => {}
            }
        }
        order
    }

    /// Find the next or previous tab stop in the tab order, wrapping around.
    fn next_tab_stop(
        tab_order: &[(String, String)],
        current: &(String, String),
        forward: bool,
    ) -> Option<(String, String)> {
        if tab_order.is_empty() {
            return None;
        }
        let pos = tab_order.iter().position(|k| k == current);
        let next_idx = match pos {
            Some(idx) => {
                if forward {
                    (idx + 1) % tab_order.len()
                } else if idx == 0 {
                    tab_order.len() - 1
                } else {
                    idx - 1
                }
            }
            None => 0,
        };
        Some(tab_order[next_idx].clone())
    }

    /// Get the text representation of a port value for pre-filling the edit field.
    fn get_edit_text_for_target(
        inputs: &[nodebox_core::node::Port],
        target: &(String, String),
    ) -> String {
        // Try exact port name match first
        if let Some(port) = inputs.iter().find(|p| p.name == target.1) {
            return match &port.value {
                Value::Float(v) => format!("{:.2}", v),
                Value::Int(v) => format!("{}", v),
                Value::String(v) => v.clone(),
                _ => String::new(),
            };
        }
        // Try Point suffix: target.1 ends with "_x" or "_y"
        if let Some(base) = target.1.strip_suffix("_x") {
            if let Some(port) = inputs.iter().find(|p| p.name == base) {
                if let Value::Point(ref pt) = port.value {
                    return format!("{:.2}", pt.x);
                }
            }
        }
        if let Some(base) = target.1.strip_suffix("_y") {
            if let Some(port) = inputs.iter().find(|p| p.name == base) {
                if let Value::Point(ref pt) = port.value {
                    return format!("{:.2}", pt.y);
                }
            }
        }
        String::new()
    }

    /// Detect whether a TextEdit lost focus due to Tab/Shift+Tab navigation.
    ///
    /// On some platforms (e.g. Linux/X11), Shift+Tab generates `ISO_Left_Tab`
    /// instead of `Key::Tab`, so scanning for `Key::Tab` events is unreliable.
    /// Instead, we infer Tab navigation by exclusion: if focus was lost and it
    /// wasn't from Escape, Enter, or a mouse click, it must be Tab/Shift+Tab.
    ///
    /// Call BEFORE `TextEdit::show()` to capture `Enter` state before the
    /// TextEdit potentially consumes the event.
    fn detect_enter_pressed(ui: &egui::Ui) -> bool {
        ui.input(|i| i.key_pressed(egui::Key::Enter))
    }

    /// Compute the drag speed modifier based on keyboard modifiers.
    /// Shift = 10x (coarse), Alt = 0.01x (fine), otherwise 1x.
    fn drag_modifier(ui: &egui::Ui) -> f64 {
        ui.input(|i| {
            if i.modifiers.shift {
                10.0
            } else if i.modifiers.alt {
                0.01
            } else {
                1.0
            }
        })
    }

    /// Draw a right-aligned label in a fixed-width column, optionally with drag-to-adjust
    /// and click-to-edit interaction.
    ///
    /// Returns (drag_delta_pixels, drag_started).
    fn show_draggable_label(
        &mut self,
        ui: &mut egui::Ui,
        label: &str,
        click_edit_state: Option<(String, String, String)>,
        set_apply_both: bool,
    ) -> (f32, bool) {
        let label_width = self.label_width;
        let mut drag_delta_x: f32 = 0.0;
        let mut drag_started = false;
        let label_owned = label.to_string();

        ui.allocate_ui_with_layout(
            egui::Vec2::new(label_width, theme::PARAMETER_ROW_HEIGHT),
            egui::Layout::right_to_left(egui::Align::Center),
            |ui| {
                ui.add_space(8.0);
                let bg_idx = ui.painter().add(egui::Shape::Noop);
                let galley = ui.painter().layout_no_wrap(
                    label_owned.clone(),
                    egui::FontId::proportional(11.0),
                    theme::TEXT_NORMAL,
                );
                let rect = ui.available_rect_before_wrap();
                let pos = egui::pos2(
                    rect.right() - galley.size().x - 8.0,
                    rect.center().y - galley.size().y / 2.0,
                );
                ui.painter().galley(pos, galley, theme::TEXT_NORMAL);

                if let Some(ref edit_state) = click_edit_state {
                    let full_rect = ui.max_rect();
                    let interact_id = ui.id().with(("label_drag", &label_owned));
                    let response = ui.interact(full_rect, interact_id, Sense::click_and_drag());
                    if response.hovered() || response.dragged() {
                        ui.painter().set(bg_idx, egui::Shape::rect_filled(
                            full_rect, 0.0, theme::FIELD_HOVER_BG,
                        ));
                        ui.ctx().set_cursor_icon(egui::CursorIcon::ResizeHorizontal);
                    }
                    if response.drag_started() {
                        drag_started = true;
                    }
                    if response.dragged() {
                        drag_delta_x = response.drag_delta().x;
                    }
                    if response.clicked() {
                        self.editing = Some((edit_state.0.clone(), edit_state.1.clone(), edit_state.2.clone(), true));
                        if set_apply_both {
                            self.label_edit_apply_both = true;
                        }
                    }
                }
            },
        );

        (drag_delta_x, drag_started)
    }

    /// Show a single port row with label and value editor.
    fn show_port_row(
        &mut self,
        ui: &mut egui::Ui,
        port: &mut nodebox_core::node::Port,
        is_connected: bool,
        node_name: &str,
        io_port: &dyn Port,
        project_context: &ProjectContext,
    ) {
        let is_label_draggable = !is_connected
            && matches!(port.widget, Widget::Float | Widget::Angle | Widget::Int | Widget::Point);
        let port_name = port.name.clone();
        let mut label_drag_delta_x: f32 = 0.0;
        let mut label_drag_started = false;

        // Pre-compute the editing state for label click (avoids borrowing port in the closure)
        let label_click_edit_state: Option<(String, String, String)> = if is_label_draggable {
            match (&port.widget, &port.value) {
                (Widget::Float | Widget::Angle, Value::Float(v)) => {
                    Some((node_name.to_string(), port.name.clone(), format!("{:.2}", v)))
                }
                (Widget::Int, Value::Int(v)) => {
                    Some((node_name.to_string(), port.name.clone(), format!("{}", v)))
                }
                (Widget::Point, Value::Point(p)) => {
                    Some((node_name.to_string(), format!("{}_x", port.name), format!("{:.2}", p.x)))
                }
                _ => None,
            }
        } else {
            None
        };
        let label_click_is_point = is_label_draggable && matches!(port.widget, Widget::Point);

        ui.horizontal(|ui| {
            ui.set_height(theme::PARAMETER_ROW_HEIGHT);

            // Fixed-width label, right-aligned (non-selectable)
            (label_drag_delta_x, label_drag_started) = self.show_draggable_label(
                ui, &port_name, label_click_edit_state, label_click_is_point,
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
                self.show_port_editor(ui, port, node_name, io_port, project_context);

                // Apply label-initiated Point edit to both x and y
                if let Some(committed_val) = self.label_edit_committed_value.take() {
                    if let Value::Point(ref mut point) = port.value {
                        point.y = committed_val;
                    }
                    self.label_edit_apply_both = false;
                } else if self.label_edit_apply_both && self.editing.is_none() {
                    // Edit was cancelled, clear the flag
                    self.label_edit_apply_both = false;
                }
            }
        });

        // Apply label drag delta to port value
        if label_drag_started {
            self.drag_accumulator = 0.0;
        }
        if label_drag_delta_x != 0.0 {
            let modifier = Self::drag_modifier(ui);
            self.drag_accumulator += label_drag_delta_x as f64 * modifier;

            let apply_delta = if ui.input(|i| i.modifiers.alt) {
                // Fine mode: apply full fractional delta
                let d = self.drag_accumulator;
                self.drag_accumulator = 0.0;
                d
            } else {
                // Integer mode: only apply integer portion
                let int_delta = self.drag_accumulator.trunc();
                self.drag_accumulator -= int_delta;
                int_delta
            };

            if apply_delta != 0.0 {
                match port.widget {
                    Widget::Float | Widget::Angle => {
                        if let Value::Float(ref mut value) = port.value {
                            *value += apply_delta;
                            if let Some(min_val) = port.min {
                                *value = value.max(min_val);
                            }
                            if let Some(max_val) = port.max {
                                *value = value.min(max_val);
                            }
                        }
                    }
                    Widget::Int => {
                        if let Value::Int(ref mut value) = port.value {
                            *value += apply_delta as i64;
                        }
                    }
                    Widget::Point => {
                        if let Value::Point(ref mut point) = port.value {
                            point.x += apply_delta;
                            point.y += apply_delta;
                        }
                    }
                    _ => {}
                }
            }
        }
    }

    /// Show the editor widget for a port value - minimal style with no borders.
    fn show_port_editor(
        &mut self,
        ui: &mut egui::Ui,
        port: &mut nodebox_core::node::Port,
        node_name: &str,
        io_port: &dyn Port,
        project_context: &ProjectContext,
    ) {
        let port_key = (node_name.to_string(), port.name.clone());

        // Check if we're editing this port
        let is_editing = self.editing.as_ref()
            .map(|(n, p, _, _)| n == node_name && p == &port.name)
            .unwrap_or(false);

        match port.widget {
            Widget::Float | Widget::Angle => {
                if let Value::Float(ref mut value) = port.value {
                    self.show_drag_value_float(ui, value, port.min, port.max, 1.0, &port_key, is_editing, theme::PADDING);
                }
            }
            Widget::Int => {
                if let Value::Int(ref mut value) = port.value {
                    self.show_drag_value_int(ui, value, port.min, port.max, &port_key, is_editing, theme::PADDING);
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

                        // Capture Enter state before TextEdit may consume it.
                        let enter_pressed = Self::detect_enter_pressed(ui);

                        let output = egui::TextEdit::singleline(&mut edit_text)
                            .font(TextStyle::Body)
                            .text_color(egui::Color32::WHITE)
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
                                // If focus was lost from keyboard Tab navigation
                                // (not Enter, not mouse click), advance to next/prev field.
                                let mouse_clicked = ui.input(|i| i.pointer.any_pressed());
                                if !enter_pressed && !mouse_clicked {
                                    let forward = !ui.input(|i| i.modifiers.shift);
                                    self.tab_target = Self::next_tab_stop(&self.tab_order, &port_key, forward);
                                }
                            }
                        }

                        // Request focus on first frame
                        if self.editing.is_some() {
                            output.response.request_focus();
                        }
                    } else {
                        // Show as clickable text
                        let display = if value.is_empty() { "\"\"" } else { value.as_str() };
                        let galley = ui.painter().layout_no_wrap(
                            display.to_string(),
                            egui::FontId::proportional(11.0),
                            egui::Color32::WHITE,
                        );
                        let rect = ui.available_rect_before_wrap();
                        let text_rect = egui::Rect::from_min_size(
                            egui::pos2(rect.left(), rect.center().y - galley.size().y / 2.0),
                            galley.size(),
                        );

                        let response = ui.allocate_rect(text_rect, Sense::click());
                        ui.painter().galley(text_rect.min, galley, egui::Color32::WHITE);

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
                    let available = ui.available_width() - theme::PADDING;
                    let old_spacing = ui.spacing().item_spacing.x;
                    ui.spacing_mut().item_spacing.x = 16.0;
                    let field_width = (available - 16.0) / 2.0;
                    ui.allocate_ui(egui::Vec2::new(field_width, theme::PARAMETER_ROW_HEIGHT), |ui| {
                        self.show_drag_value_float(ui, &mut point.x, None, None, 1.0, &key_x, is_editing_x, 0.0);
                    });
                    ui.allocate_ui(egui::Vec2::new(field_width, theme::PARAMETER_ROW_HEIGHT), |ui| {
                        self.show_drag_value_float(ui, &mut point.y, None, None, 1.0, &key_y, is_editing_y, 0.0);
                    });
                    ui.spacing_mut().item_spacing.x = old_spacing;
                }
            }
            Widget::Menu => {
                if let Value::String(ref mut value) = port.value {
                    // Find current label from menu_items
                    let current_label = port.menu_items.iter()
                        .find(|item| item.key == *value)
                        .map(|item| item.label.as_str())
                        .unwrap_or(value.as_str());

                    // Use smaller font to match other controls
                    let style = ui.style_mut();
                    style.override_font_id = Some(egui::FontId::proportional(theme::FONT_SIZE_SMALL));

                    // Use egui ComboBox
                    let combo_id = ui.make_persistent_id((&port_key.0, &port_key.1));
                    egui::ComboBox::from_id_salt(combo_id)
                        .selected_text(current_label)
                        .width(120.0)
                        .show_ui(ui, |ui| {
                            for item in &port.menu_items {
                                if ui.selectable_label(*value == item.key, &item.label).clicked() {
                                    *value = item.key.clone();
                                }
                            }
                        });
                }
            }
            Widget::File => {
                if let Value::String(ref mut path) = port.value {
                    // Show filename or placeholder
                    let display_text = if path.is_empty() {
                        "(none)".to_string()
                    } else {
                        // Extract just the filename from the path
                        std::path::Path::new(path)
                            .file_name()
                            .map(|s| s.to_string_lossy().to_string())
                            .unwrap_or_else(|| path.clone())
                    };

                    let galley = ui.painter().layout_no_wrap(
                        display_text,
                        egui::FontId::proportional(11.0),
                        if path.is_empty() { theme::TEXT_DISABLED } else { theme::VALUE_TEXT },
                    );
                    let rect = ui.available_rect_before_wrap();
                    let text_pos = egui::pos2(rect.left(), rect.center().y - galley.size().y / 2.0);
                    ui.painter().galley(text_pos, galley.clone(), theme::VALUE_TEXT);

                    // Add browse button after the text
                    let button_x = rect.left() + galley.size().x + 8.0;
                    let button_rect = egui::Rect::from_min_size(
                        egui::pos2(button_x, rect.center().y - 8.0),
                        egui::vec2(16.0, 16.0),
                    );

                    let button_response = ui.allocate_rect(button_rect, Sense::click());

                    // Draw folder icon or "..." button
                    let button_color = if button_response.hovered() {
                        theme::TEXT_BRIGHT
                    } else {
                        theme::TEXT_NORMAL
                    };
                    ui.painter().text(
                        button_rect.center(),
                        egui::Align2::CENTER_CENTER,
                        "…",
                        egui::FontId::proportional(14.0),
                        button_color,
                    );

                    if button_response.hovered() {
                        ui.ctx().set_cursor_icon(egui::CursorIcon::PointingHand);
                    }

                    if button_response.clicked() {
                        // Check if project is saved first
                        if !project_context.is_saved() {
                            // Show message to save project first
                            let _ = io_port.show_message_dialog(
                                "Save Project First",
                                "Please save your project before importing files.",
                                &["OK"],
                            );
                        } else {
                            // Use sandboxed file dialog through Port
                            match io_port.show_open_file_dialog(
                                project_context,
                                &[FileFilter::svg()],
                            ) {
                                Ok(Some(relative_path)) => {
                                    // Store the relative path
                                    *path = relative_path.to_string();
                                }
                                Ok(None) => {} // User cancelled
                                Err(PortError::SandboxViolation) => {
                                    // File is outside project directory
                                    let _ = io_port.show_message_dialog(
                                        "File Outside Project",
                                        "Please copy the file to your project folder first.",
                                        &["OK"],
                                    );
                                }
                                Err(e) => {
                                    log::error!("File dialog error: {}", e);
                                }
                            }
                        }
                    }
                }
            }
            Widget::Font => {
                if let Value::String(ref mut value) = port.value {
                    let style = ui.style_mut();
                    style.override_font_id = Some(egui::FontId::proportional(theme::FONT_SIZE_SMALL));

                    let combo_id = ui.make_persistent_id((&port_key.0, &port_key.1));
                    egui::ComboBox::from_id_salt(combo_id)
                        .selected_text(value.as_str())
                        .width(120.0)
                        .show_ui(ui, |ui| {
                            for family in io_port.list_fonts() {
                                if ui.selectable_label(*value == family, &family).clicked() {
                                    *value = family;
                                }
                            }
                        });
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
    /// `right_padding` is extra space to reserve on the right (e.g. PADDING for panel edge margin, 0.0 for Point widget fields).
    fn show_drag_value_float(
        &mut self,
        ui: &mut egui::Ui,
        value: &mut f64,
        min: Option<f64>,
        max: Option<f64>,
        speed: f64,
        port_key: &(String, String),
        is_editing: bool,
        right_padding: f32,
    ) {
        if is_editing {
            // Show text input for direct editing
            let (mut edit_text, needs_select) = self.editing.as_ref()
                .map(|(_, _, t, sel)| (t.clone(), *sel))
                .unwrap_or_else(|| (format!("{:.2}", value), true));

            // Capture Enter state before TextEdit may consume it.
            let enter_pressed = Self::detect_enter_pressed(ui);

            // Frameless TextEdit with manual background for pixel-perfect alignment
            let old_selection = ui.visuals().selection.clone();
            ui.visuals_mut().selection.stroke = egui::Stroke::new(0.0, egui::Color32::WHITE);
            ui.visuals_mut().selection.bg_fill = theme::TEXT_EDIT_SELECTION_BG;

            let bg_idx = ui.painter().add(egui::Shape::Noop);
            let output = egui::TextEdit::singleline(&mut edit_text)
                .font(egui::FontId::proportional(theme::FONT_SIZE_SMALL))
                .text_color(egui::Color32::WHITE)
                .desired_width(ui.available_width() - theme::PADDING - right_padding)
                .margin(egui::Margin::symmetric(4, 0))
                .frame(false)
                .show(ui);

            // Paint rounded background behind the text
            let bg_rect = output.response.rect.expand2(egui::vec2(0.0, 4.0));
            ui.painter().set(bg_idx, egui::Shape::rect_filled(
                bg_rect,
                egui::CornerRadius::same(theme::CORNER_RADIUS_SMALL as u8),
                theme::ZINC_700,
            ));

            ui.visuals_mut().selection = old_selection;

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
                } else {
                    if let Ok(new_val) = edit_text.parse::<f64>() {
                        let mut clamped = new_val;
                        if let Some(min_val) = min {
                            clamped = clamped.max(min_val);
                        }
                        if let Some(max_val) = max {
                            clamped = clamped.min(max_val);
                        }
                        *value = clamped;
                        if self.label_edit_apply_both {
                            self.label_edit_committed_value = Some(clamped);
                        }
                    }
                    self.editing = None;
                    // If focus was lost from keyboard Tab navigation
                    // (not Enter, not mouse click), advance to next/prev field.
                    let mouse_clicked = ui.input(|i| i.pointer.any_pressed());
                    if !enter_pressed && !mouse_clicked {
                        let forward = !ui.input(|i| i.modifiers.shift);
                        self.tab_target = Self::next_tab_stop(&self.tab_order, port_key, forward);
                    }
                }
            }

            if self.editing.is_some() {
                output.response.request_focus();
            }
        } else {
            // Non-interactive TextEdit for pixel-perfect alignment with editing state
            let mut display_text = format!("{:.2}", value);
            let bg_idx = ui.painter().add(egui::Shape::Noop);
            let te_output = egui::TextEdit::singleline(&mut display_text)
                .font(egui::FontId::proportional(theme::FONT_SIZE_SMALL))
                .text_color(egui::Color32::WHITE)
                .interactive(false)
                .frame(false)
                .margin(egui::Margin::symmetric(4, 0))
                .desired_width(ui.available_width() - theme::PADDING - right_padding)
                .show(ui);

            // Overlay click+drag sensing on the same rect
            let interact_id = ui.id().with(port_key);
            let response = ui.interact(te_output.response.rect, interact_id, Sense::click_and_drag());

            // Hover effect: subtle darkened background
            if response.hovered() || response.dragged() {
                let hover_rect = te_output.response.rect.expand2(egui::vec2(0.0, 4.0));
                ui.painter().set(bg_idx, egui::Shape::rect_filled(
                    hover_rect,
                    egui::CornerRadius::same(theme::CORNER_RADIUS_SMALL as u8),
                    theme::FIELD_HOVER_BG,
                ));
            }

            if response.drag_started() {
                self.drag_accumulator = 0.0;
            }
            if response.dragged() {
                let modifier = Self::drag_modifier(ui);
                self.drag_accumulator += response.drag_delta().x as f64 * speed * modifier;

                let apply_delta = if ui.input(|i| i.modifiers.alt) {
                    // Fine mode: apply full fractional delta
                    let d = self.drag_accumulator;
                    self.drag_accumulator = 0.0;
                    d
                } else {
                    // Integer mode: only apply integer portion
                    let int_delta = self.drag_accumulator.trunc();
                    self.drag_accumulator -= int_delta;
                    int_delta
                };

                if apply_delta != 0.0 {
                    *value += apply_delta;
                }
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
    fn show_drag_value_int(&mut self, ui: &mut egui::Ui, value: &mut i64, min: Option<f64>, max: Option<f64>, port_key: &(String, String), is_editing: bool, right_padding: f32) {
        if is_editing {
            // Show text input for direct editing
            let (mut edit_text, needs_select) = self.editing.as_ref()
                .map(|(_, _, t, sel)| (t.clone(), *sel))
                .unwrap_or_else(|| (format!("{}", value), true));

            // Capture Enter state before TextEdit may consume it.
            let enter_pressed = Self::detect_enter_pressed(ui);

            // Frameless TextEdit with manual background for pixel-perfect alignment
            let old_selection = ui.visuals().selection.clone();
            ui.visuals_mut().selection.stroke = egui::Stroke::new(0.0, egui::Color32::WHITE);
            ui.visuals_mut().selection.bg_fill = theme::TEXT_EDIT_SELECTION_BG;

            let bg_idx = ui.painter().add(egui::Shape::Noop);
            let output = egui::TextEdit::singleline(&mut edit_text)
                .font(egui::FontId::proportional(theme::FONT_SIZE_SMALL))
                .text_color(egui::Color32::WHITE)
                .desired_width(ui.available_width() - theme::PADDING - right_padding)
                .margin(egui::Margin::symmetric(4, 0))
                .frame(false)
                .show(ui);

            // Paint rounded background behind the text
            let bg_rect = output.response.rect.expand2(egui::vec2(0.0, 4.0));
            ui.painter().set(bg_idx, egui::Shape::rect_filled(
                bg_rect,
                egui::CornerRadius::same(theme::CORNER_RADIUS_SMALL as u8),
                theme::ZINC_700,
            ));

            ui.visuals_mut().selection = old_selection;

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
                } else {
                    if let Ok(new_val) = edit_text.parse::<i64>() {
                        let mut clamped = new_val;
                        if let Some(min_val) = min {
                            clamped = clamped.max(min_val as i64);
                        }
                        if let Some(max_val) = max {
                            clamped = clamped.min(max_val as i64);
                        }
                        *value = clamped;
                    }
                    self.editing = None;
                    // If focus was lost from keyboard Tab navigation
                    // (not Enter, not mouse click), advance to next/prev field.
                    let mouse_clicked = ui.input(|i| i.pointer.any_pressed());
                    if !enter_pressed && !mouse_clicked {
                        let forward = !ui.input(|i| i.modifiers.shift);
                        self.tab_target = Self::next_tab_stop(&self.tab_order, port_key, forward);
                    }
                }
            }

            if self.editing.is_some() {
                output.response.request_focus();
            }
        } else {
            // Non-interactive TextEdit for pixel-perfect alignment with editing state
            let mut display_text = format!("{}", value);
            let bg_idx = ui.painter().add(egui::Shape::Noop);
            let te_output = egui::TextEdit::singleline(&mut display_text)
                .font(egui::FontId::proportional(theme::FONT_SIZE_SMALL))
                .text_color(egui::Color32::WHITE)
                .interactive(false)
                .frame(false)
                .margin(egui::Margin::symmetric(4, 0))
                .desired_width(ui.available_width() - theme::PADDING - right_padding)
                .show(ui);

            // Overlay click+drag sensing on the same rect
            let interact_id = ui.id().with(port_key);
            let response = ui.interact(te_output.response.rect, interact_id, Sense::click_and_drag());

            // Hover effect: subtle darkened background
            if response.hovered() || response.dragged() {
                let hover_rect = te_output.response.rect.expand2(egui::vec2(0.0, 4.0));
                ui.painter().set(bg_idx, egui::Shape::rect_filled(
                    hover_rect,
                    egui::CornerRadius::same(theme::CORNER_RADIUS_SMALL as u8),
                    theme::FIELD_HOVER_BG,
                ));
            }

            if response.drag_started() {
                self.drag_accumulator = 0.0;
            }
            if response.dragged() {
                let modifier = Self::drag_modifier(ui);
                self.drag_accumulator += response.drag_delta().x as f64 * modifier;
                let int_delta = self.drag_accumulator.trunc() as i64;
                if int_delta != 0 {
                    *value += int_delta;
                    self.drag_accumulator -= int_delta as f64;
                }
                if let Some(min_val) = min {
                    *value = (*value).max(min_val as i64);
                }
                if let Some(max_val) = max {
                    *value = (*value).min(max_val as i64);
                }
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
            // Show header even for errors
            self.show_parameters_header(ui, None, None);
            ui.vertical_centered(|ui| {
                ui.add_space(30.0);
                ui.label(
                    egui::RichText::new(err)
                        .color(theme::ERROR_RED)
                        .size(12.0),
                );
            });
        } else {
            // Show merged header with "Document"
            self.show_parameters_header(ui, Some("Document"), None);

            // Hint text
            ui.vertical_centered(|ui| {
                ui.add_space(theme::PADDING);
                ui.label(
                    egui::RichText::new("Select a node to edit parameters")
                        .color(theme::TEXT_DISABLED)
                        .size(11.0),
                );
            });
        }
    }

    /// Show the merged parameters header: PARAMETERS | node_name ... prototype
    fn show_parameters_header(&self, ui: &mut egui::Ui, node_name: Option<&str>, prototype: Option<&str>) {
        let (header_rect, x) = components::draw_pane_header_with_title(ui, "Parameters");

        // Only show node info if we have a node name
        if let Some(name) = node_name {
            // Node name after separator
            ui.painter().text(
                egui::pos2(x, header_rect.center().y),
                egui::Align2::LEFT_CENTER,
                name,
                egui::FontId::proportional(10.0),
                theme::TEXT_BRIGHT,
            );

            // Prototype on right
            if let Some(proto) = prototype {
                ui.painter().text(
                    header_rect.right_center() - egui::vec2(theme::PADDING, 0.0),
                    egui::Align2::RIGHT_CENTER,
                    proto,
                    egui::FontId::proportional(10.0),
                    theme::TEXT_DISABLED,
                );
            }
        }
    }

    /// Show document properties panel (canvas size, etc.).
    pub fn show_document_properties(&mut self, ui: &mut egui::Ui, state: &mut AppState) {
        // Build tab order for document properties
        self.tab_order = vec![
            ("__document__".to_string(), "width".to_string()),
            ("__document__".to_string(), "height".to_string()),
        ];

        // Activate pending tab target
        if let Some(ref target) = self.tab_target.take() {
            if self.tab_order.iter().any(|k| k == target) {
                let edit_text = if target.1 == "width" {
                    format!("{:.2}", state.library.width())
                } else {
                    format!("{:.2}", state.library.height())
                };
                self.editing = Some((target.0.clone(), target.1.clone(), edit_text, true));
            }
        }


        // Merged header with "Document"
        self.show_parameters_header(ui, Some("Document"), None);

        // Restore row spacing for content
        ui.style_mut().spacing.item_spacing = egui::vec2(8.0, 2.0);

        // Paint two-tone background for the content area
        let content_rect = ui.available_rect_before_wrap();
        // Left side (labels) - darker
        ui.painter().rect_filled(
            egui::Rect::from_min_max(
                content_rect.min,
                egui::pos2(content_rect.left() + self.label_width, content_rect.max.y),
            ),
            0.0,
            theme::PORT_LABEL_BACKGROUND,
        );
        // Right side (values) - lighter
        ui.painter().rect_filled(
            egui::Rect::from_min_max(
                egui::pos2(content_rect.left() + self.label_width, content_rect.min.y),
                content_rect.max,
            ),
            0.0,
            theme::PORT_VALUE_BACKGROUND,
        );

        ui.add_space(theme::PADDING);

        // Width
        let current_width = state.library.width();
        let mut width_label_drag: f32 = 0.0;
        let mut width_drag_started = false;
        ui.horizontal(|ui| {
            ui.set_height(theme::PARAMETER_ROW_HEIGHT);

            (width_label_drag, width_drag_started) = self.show_draggable_label(
                ui, "width",
                Some(("__document__".to_string(), "width".to_string(), format!("{:.2}", current_width))),
                false,
            );

            // Value
            let mut width = current_width;
            let key = ("__document__".to_string(), "width".to_string());
            let is_editing = self.editing.as_ref()
                .map(|(n, p, _, _)| n == &key.0 && p == &key.1)
                .unwrap_or(false);
            self.show_drag_value_float(ui, &mut width, Some(1.0), None, 1.0, &key, is_editing, theme::PADDING);

            // Update the property if changed
            if (current_width - width).abs() > 0.001 {
                Arc::make_mut(&mut state.library).set_width(width);
            }
        });
        if width_drag_started {
            self.drag_accumulator = 0.0;
        }
        if width_label_drag != 0.0 {
            let modifier = Self::drag_modifier(ui);
            self.drag_accumulator += width_label_drag as f64 * modifier;

            let apply_delta = if ui.input(|i| i.modifiers.alt) {
                let d = self.drag_accumulator;
                self.drag_accumulator = 0.0;
                d
            } else {
                let int_delta = self.drag_accumulator.trunc();
                self.drag_accumulator -= int_delta;
                int_delta
            };

            if apply_delta != 0.0 {
                let new_width = (state.library.width() + apply_delta).max(1.0);
                Arc::make_mut(&mut state.library).set_width(new_width);
            }
        }

        // Height
        let current_height = state.library.height();
        let mut height_label_drag: f32 = 0.0;
        let mut height_drag_started = false;
        ui.horizontal(|ui| {
            ui.set_height(theme::PARAMETER_ROW_HEIGHT);

            (height_label_drag, height_drag_started) = self.show_draggable_label(
                ui, "height",
                Some(("__document__".to_string(), "height".to_string(), format!("{:.2}", current_height))),
                false,
            );

            // Value
            let mut height = current_height;
            let key = ("__document__".to_string(), "height".to_string());
            let is_editing = self.editing.as_ref()
                .map(|(n, p, _, _)| n == &key.0 && p == &key.1)
                .unwrap_or(false);
            self.show_drag_value_float(ui, &mut height, Some(1.0), None, 1.0, &key, is_editing, theme::PADDING);

            // Update the property if changed
            if (current_height - height).abs() > 0.001 {
                Arc::make_mut(&mut state.library).set_height(height);
            }
        });
        if height_drag_started {
            self.drag_accumulator = 0.0;
        }
        if height_label_drag != 0.0 {
            let modifier = Self::drag_modifier(ui);
            self.drag_accumulator += height_label_drag as f64 * modifier;

            let apply_delta = if ui.input(|i| i.modifiers.alt) {
                let d = self.drag_accumulator;
                self.drag_accumulator = 0.0;
                d
            } else {
                let int_delta = self.drag_accumulator.trunc();
                self.drag_accumulator -= int_delta;
                int_delta
            };

            if apply_delta != 0.0 {
                let new_height = (state.library.height() + apply_delta).max(1.0);
                Arc::make_mut(&mut state.library).set_height(new_height);
            }
        }

        // Background color
        ui.horizontal(|ui| {
            ui.set_height(theme::PARAMETER_ROW_HEIGHT);

            self.show_draggable_label(ui, "background", None, false);

            // Color widget
            let color = state.background_color;
            let mut rgba = [
                (color.r * 255.0) as u8,
                (color.g * 255.0) as u8,
                (color.b * 255.0) as u8,
                (color.a * 255.0) as u8,
            ];
            ui.color_edit_button_srgba_unmultiplied(&mut rgba);
            let new_color = Color::rgba(
                rgba[0] as f64 / 255.0,
                rgba[1] as f64 / 255.0,
                rgba[2] as f64 / 255.0,
                rgba[3] as f64 / 255.0,
            );
            if new_color != color {
                state.background_color = new_color;
                Arc::make_mut(&mut state.library).set_background_color(new_color);
            }
        });
    }
}
