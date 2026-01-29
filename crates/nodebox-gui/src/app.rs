//! Main application state and update loop.

use eframe::egui::{self, Pos2, Rect, Vec2};
use nodebox_core::geometry::Point;
use crate::address_bar::AddressBar;
use crate::animation_bar::AnimationBar;
use crate::history::History;
use crate::network_view::{NetworkAction, NetworkView};
use crate::node_selection_dialog::NodeSelectionDialog;
use crate::panels::ParameterPanel;
use crate::state::AppState;
use crate::theme;
use crate::viewer_pane::ViewerPane;

/// The main NodeBox application.
pub struct NodeBoxApp {
    state: AppState,
    address_bar: AddressBar,
    viewer_pane: ViewerPane,
    network_view: NetworkView,
    parameters: ParameterPanel,
    animation_bar: AnimationBar,
    node_dialog: NodeSelectionDialog,
    history: History,
    /// Previous library state for detecting changes.
    previous_library_hash: u64,
}

impl NodeBoxApp {
    /// Create a new NodeBox application instance.
    pub fn new(_cc: &eframe::CreationContext<'_>) -> Self {
        let state = AppState::new();
        let hash = Self::hash_library(&state.library);
        Self {
            state,
            address_bar: AddressBar::new(),
            viewer_pane: ViewerPane::new(),
            network_view: NetworkView::new(),
            parameters: ParameterPanel::new(),
            animation_bar: AnimationBar::new(),
            node_dialog: NodeSelectionDialog::new(),
            history: History::new(),
            previous_library_hash: hash,
        }
    }

    /// Compute a simple hash of the library for change detection.
    fn hash_library(library: &nodebox_core::node::NodeLibrary) -> u64 {
        use std::hash::{Hash, Hasher};
        use std::collections::hash_map::DefaultHasher;
        let mut hasher = DefaultHasher::new();
        // Hash the number of children and their names/positions
        library.root.children.len().hash(&mut hasher);
        for child in &library.root.children {
            child.name.hash(&mut hasher);
            (child.position.x as i64).hash(&mut hasher);
            (child.position.y as i64).hash(&mut hasher);
            child.inputs.len().hash(&mut hasher);
        }
        library.root.connections.len().hash(&mut hasher);
        hasher.finish()
    }

    /// Save current state to history if it changed.
    fn auto_save_history(&mut self) {
        let current_hash = Self::hash_library(&self.state.library);
        if current_hash != self.previous_library_hash {
            self.history.save_state(&self.state.library);
            self.previous_library_hash = current_hash;
            self.state.dirty = true;
        }
    }

    /// Show the menu bar.
    fn show_menu_bar(&mut self, ui: &mut egui::Ui, ctx: &egui::Context) {
        egui::menu::bar(ui, |ui| {
            ui.menu_button("File", |ui| {
                if ui.button("New").clicked() {
                    self.state.new_document();
                    ui.close_menu();
                }
                if ui.button("Open...").clicked() {
                    self.open_file();
                    ui.close_menu();
                }
                if ui.button("Save").clicked() {
                    self.save_file();
                    ui.close_menu();
                }
                if ui.button("Save As...").clicked() {
                    self.save_file_as();
                    ui.close_menu();
                }
                ui.separator();
                if ui.button("Export SVG...").clicked() {
                    self.export_svg();
                    ui.close_menu();
                }
                if ui.button("Export PNG...").clicked() {
                    self.export_png();
                    ui.close_menu();
                }
                ui.separator();
                if ui.button("Quit").clicked() {
                    ctx.send_viewport_cmd(egui::ViewportCommand::Close);
                }
            });

            ui.menu_button("Edit", |ui| {
                let undo_text = if self.history.can_undo() {
                    format!("Undo ({})", self.history.undo_count())
                } else {
                    "Undo".to_string()
                };
                if ui.add_enabled(self.history.can_undo(), egui::Button::new(undo_text)).clicked() {
                    if let Some(previous) = self.history.undo(&self.state.library) {
                        self.state.library = previous;
                        self.previous_library_hash = Self::hash_library(&self.state.library);
                    }
                    ui.close_menu();
                }
                let redo_text = if self.history.can_redo() {
                    format!("Redo ({})", self.history.redo_count())
                } else {
                    "Redo".to_string()
                };
                if ui.add_enabled(self.history.can_redo(), egui::Button::new(redo_text)).clicked() {
                    if let Some(next) = self.history.redo(&self.state.library) {
                        self.state.library = next;
                        self.previous_library_hash = Self::hash_library(&self.state.library);
                    }
                    ui.close_menu();
                }
                ui.separator();
                if ui.button("Delete Selected").clicked() {
                    ui.close_menu();
                }
            });

            ui.menu_button("View", |ui| {
                if ui.button("Zoom In").clicked() {
                    self.viewer_pane.zoom_in();
                    ui.close_menu();
                }
                if ui.button("Zoom Out").clicked() {
                    self.viewer_pane.zoom_out();
                    ui.close_menu();
                }
                if ui.button("Fit to Window").clicked() {
                    self.viewer_pane.fit_to_window();
                    ui.close_menu();
                }
                ui.separator();
                ui.checkbox(&mut self.viewer_pane.show_handles, "Show Handles");
                ui.checkbox(&mut self.viewer_pane.show_points, "Show Points");
                ui.checkbox(&mut self.viewer_pane.show_origin, "Show Origin");
                ui.checkbox(&mut self.viewer_pane.show_bounds, "Show Bounds");
            });

            ui.menu_button("Help", |ui| {
                if ui.button("About NodeBox").clicked() {
                    self.state.show_about = true;
                    ui.close_menu();
                }
            });
        });
    }
}

impl eframe::App for NodeBoxApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        // 1. Menu bar (top-most)
        egui::TopBottomPanel::top("menu_bar").show(ctx, |ui| {
            self.show_menu_bar(ui, ctx);
        });

        // 2. Address bar (below menu)
        egui::TopBottomPanel::top("address_bar")
            .exact_height(theme::ADDRESS_BAR_HEIGHT)
            .show(ctx, |ui| {
                // Update address bar message with current state
                let node_count = self.state.library.root.children.len();
                let msg = format!("Nodes: {} | Zoom: {:.0}%", node_count, self.viewer_pane.zoom() * 100.0);
                self.address_bar.set_message(msg);

                if let Some(_clicked_path) = self.address_bar.show(ui) {
                    // Future: navigate to sub-network
                }
            });

        // 3. Animation bar (bottom)
        egui::TopBottomPanel::bottom("animation_bar")
            .exact_height(theme::ANIMATION_BAR_HEIGHT)
            .show(ctx, |ui| {
                let _event = self.animation_bar.show(ui);
            });

        // Update animation playback
        if self.animation_bar.is_playing() {
            self.animation_bar.update();
            ctx.request_repaint();
        }

        // 4. Right side panel containing Parameters (top) and Network (bottom)
        egui::SidePanel::right("right_panel")
            .default_width(450.0)
            .min_width(300.0)
            .resizable(true)
            .show(ctx, |ui| {
                let available = ui.available_rect_before_wrap();
                let split_ratio = 0.35; // 35% parameters, 65% network
                let split_y = available.height() * split_ratio;

                // Top: Parameters pane
                let params_rect = Rect::from_min_size(
                    available.min,
                    Vec2::new(available.width(), split_y - 1.0),
                );

                ui.allocate_new_ui(egui::UiBuilder::new().max_rect(params_rect), |ui| {
                    ui.set_clip_rect(params_rect);
                    ui.heading("Parameters");
                    ui.separator();
                    self.parameters.show(ui, &mut self.state);
                });

                // Separator line
                let sep_y = available.min.y + split_y;
                ui.painter().line_segment(
                    [
                        Pos2::new(available.min.x, sep_y),
                        Pos2::new(available.max.x, sep_y),
                    ],
                    egui::Stroke::new(1.0, theme::DARK_BACKGROUND),
                );

                // Bottom: Network pane
                let network_rect = Rect::from_min_max(
                    Pos2::new(available.min.x, available.min.y + split_y + 1.0),
                    available.max,
                );

                ui.allocate_new_ui(egui::UiBuilder::new().max_rect(network_rect), |ui| {
                    ui.set_clip_rect(network_rect);
                    // Network header
                    ui.horizontal(|ui| {
                        ui.label("Network");
                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            if ui.button("+ New Node").clicked() {
                                self.node_dialog.open(Point::new(0.0, 0.0));
                            }
                        });
                    });
                    ui.separator();

                    // Network view
                    let action = self.network_view.show(ui, &mut self.state.library);

                    // Handle network actions
                    match action {
                        NetworkAction::OpenNodeDialog(pos) => {
                            self.node_dialog.open(pos);
                        }
                        NetworkAction::None => {}
                    }

                    // Update selected node from network view
                    let selected = self.network_view.selected_nodes();
                    if selected.len() == 1 {
                        self.state.selected_node = selected.iter().next().cloned();
                    } else if selected.is_empty() {
                        self.state.selected_node = None;
                    }
                });
            });

        // 5. Central panel: Viewer (left side, takes remaining space)
        egui::CentralPanel::default().show(ctx, |ui| {
            // Update handles for selected node
            self.viewer_pane.update_handles_for_node(
                self.state.selected_node.as_deref(),
                &self.state,
            );
            self.viewer_pane.show(ui, &self.state);

            // Handle viewer interaction
            let viewer_rect = ui.min_rect();
            if let Some((param_name, new_position)) = self.viewer_pane.handle_interaction(ui, viewer_rect) {
                self.handle_parameter_change(&param_name, new_position);
            }
        });

        // 6. Node selection dialog
        if self.node_dialog.visible {
            if let Some(new_node) = self.node_dialog.show(ctx, &self.state.library) {
                let node_name = new_node.name.clone();
                self.state.library.root.children.push(new_node);
                // Select the new node
                self.state.selected_node = Some(node_name);
            }
        }

        // 7. About dialog
        if self.state.show_about {
            egui::Window::new("About NodeBox")
                .collapsible(false)
                .resizable(false)
                .anchor(egui::Align2::CENTER_CENTER, [0.0, 0.0])
                .show(ctx, |ui| {
                    ui.vertical_centered(|ui| {
                        ui.heading("NodeBox");
                        ui.label("Version 4.0 (Rust)");
                        ui.add_space(10.0);
                        ui.label("A node-based generative design tool");
                        ui.add_space(10.0);
                        ui.hyperlink_to("Visit website", "https://www.nodebox.net");
                        ui.add_space(10.0);
                        if ui.button("Close").clicked() {
                            self.state.show_about = false;
                        }
                    });
                });
        }

        // Handle keyboard shortcuts
        ctx.input(|i| {
            // Undo: Ctrl+Z (or Cmd+Z on Mac)
            if i.modifiers.command && i.key_pressed(egui::Key::Z) && !i.modifiers.shift {
                if let Some(previous) = self.history.undo(&self.state.library) {
                    self.state.library = previous;
                    self.previous_library_hash = Self::hash_library(&self.state.library);
                }
            }
            // Redo: Ctrl+Shift+Z or Ctrl+Y
            if (i.modifiers.command && i.modifiers.shift && i.key_pressed(egui::Key::Z))
                || (i.modifiers.command && i.key_pressed(egui::Key::Y))
            {
                if let Some(next) = self.history.redo(&self.state.library) {
                    self.state.library = next;
                    self.previous_library_hash = Self::hash_library(&self.state.library);
                }
            }
        });

        // Check for state changes and auto-save to history
        self.auto_save_history();
    }
}

impl NodeBoxApp {
    /// Handle parameter change from viewer handles.
    fn handle_parameter_change(&mut self, param_name: &str, new_position: Point) {
        if let Some(ref node_name) = self.state.selected_node {
            if let Some(node) = self.state.library.root.child_mut(node_name) {
                match param_name {
                    "position" => {
                        if let Some(port) = node.input_mut("x") {
                            port.value = nodebox_core::Value::Float(new_position.x);
                        }
                        if let Some(port) = node.input_mut("y") {
                            port.value = nodebox_core::Value::Float(new_position.y);
                        }
                    }
                    "width" => {
                        if let Some(port) = node.input_mut("x") {
                            let x = port.value.as_float().unwrap_or(0.0);
                            let new_width = (new_position.x - x) * 2.0;
                            if let Some(width_port) = node.input_mut("width") {
                                width_port.value = nodebox_core::Value::Float(new_width.abs());
                            }
                        }
                    }
                    "height" => {
                        if let Some(port) = node.input_mut("y") {
                            let y = port.value.as_float().unwrap_or(0.0);
                            let new_height = (new_position.y - y) * 2.0;
                            if let Some(height_port) = node.input_mut("height") {
                                height_port.value = nodebox_core::Value::Float(new_height.abs());
                            }
                        }
                    }
                    "size" => {
                        if let Some(port) = node.input_mut("x") {
                            let x = port.value.as_float().unwrap_or(0.0);
                            if let Some(width_port) = node.input_mut("width") {
                                width_port.value = nodebox_core::Value::Float((new_position.x - x).abs());
                            }
                        }
                        if let Some(port) = node.input_mut("y") {
                            let y = port.value.as_float().unwrap_or(0.0);
                            if let Some(height_port) = node.input_mut("height") {
                                height_port.value = nodebox_core::Value::Float((new_position.y - y).abs());
                            }
                        }
                    }
                    "point1" | "point2" => {
                        if let Some(port) = node.input_mut(param_name) {
                            port.value = nodebox_core::Value::Point(new_position);
                        }
                    }
                    _ => {}
                }
            }
        }
    }

    fn open_file(&mut self) {
        if let Some(path) = rfd::FileDialog::new()
            .add_filter("NodeBox Files", &["ndbx"])
            .pick_file()
        {
            if let Err(e) = self.state.load_file(&path) {
                log::error!("Failed to load file: {}", e);
            }
        }
    }

    fn save_file(&mut self) {
        if let Some(ref path) = self.state.current_file.clone() {
            if let Err(e) = self.state.save_file(path) {
                log::error!("Failed to save file: {}", e);
            }
        } else {
            self.save_file_as();
        }
    }

    fn save_file_as(&mut self) {
        if let Some(path) = rfd::FileDialog::new()
            .add_filter("NodeBox Files", &["ndbx"])
            .save_file()
        {
            if let Err(e) = self.state.save_file(&path) {
                log::error!("Failed to save file: {}", e);
            }
        }
    }

    fn export_svg(&mut self) {
        if let Some(path) = rfd::FileDialog::new()
            .add_filter("SVG Files", &["svg"])
            .save_file()
        {
            if let Err(e) = self.state.export_svg(&path) {
                log::error!("Failed to export SVG: {}", e);
            }
        }
    }

    fn export_png(&mut self) {
        if let Some(path) = rfd::FileDialog::new()
            .add_filter("PNG Files", &["png"])
            .save_file()
        {
            // Calculate bounds and export dimensions
            let (min_x, min_y, max_x, max_y) = crate::export::calculate_bounds(&self.state.geometry);
            let padding = 20.0;
            let width = ((max_x - min_x + padding * 2.0).max(100.0)) as u32;
            let height = ((max_y - min_y + padding * 2.0).max(100.0)) as u32;

            if let Err(e) = crate::export::export_png(
                &self.state.geometry,
                &path,
                width,
                height,
                self.state.background_color,
            ) {
                log::error!("Failed to export PNG: {}", e);
            }
        }
    }
}
