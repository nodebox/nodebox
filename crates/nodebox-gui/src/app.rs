//! Main application state and update loop.

use eframe::egui;
use crate::canvas::CanvasViewer;
use crate::history::History;
use crate::network_view::NetworkView;
use crate::node_library::NodeLibraryBrowser;
use crate::panels::ParameterPanel;
use crate::state::AppState;

/// The main NodeBox application.
pub struct NodeBoxApp {
    state: AppState,
    canvas: CanvasViewer,
    network_view: NetworkView,
    node_library: NodeLibraryBrowser,
    parameters: ParameterPanel,
    history: History,
    show_node_library: bool,
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
            canvas: CanvasViewer::new(),
            network_view: NetworkView::new(),
            node_library: NodeLibraryBrowser::new(),
            parameters: ParameterPanel::new(),
            history: History::new(),
            show_node_library: true,
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
            // Create a copy of the previous state before the change
            // Note: We need to save the state BEFORE the change happened
            // This simple implementation saves after change detection, which
            // means the first undo might not work perfectly. For a production
            // system, we'd want to save before each operation.
            self.history.save_state(&self.state.library);
            self.previous_library_hash = current_hash;
            self.state.dirty = true;
        }
    }
}

impl eframe::App for NodeBoxApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        // Top menu bar
        egui::TopBottomPanel::top("menu_bar").show(ctx, |ui| {
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
                        // Delete selected nodes via network view
                        ui.close_menu();
                    }
                });

                ui.menu_button("View", |ui| {
                    if ui.button("Zoom In").clicked() {
                        self.canvas.zoom_in();
                        ui.close_menu();
                    }
                    if ui.button("Zoom Out").clicked() {
                        self.canvas.zoom_out();
                        ui.close_menu();
                    }
                    if ui.button("Fit to Window").clicked() {
                        self.canvas.fit_to_window();
                        ui.close_menu();
                    }
                    ui.separator();
                    if ui.checkbox(&mut self.show_node_library, "Node Library").clicked() {
                        ui.close_menu();
                    }
                });

                ui.menu_button("Help", |ui| {
                    if ui.button("About NodeBox").clicked() {
                        self.state.show_about = true;
                        ui.close_menu();
                    }
                });
            });
        });

        // Left panel: Node graph
        egui::SidePanel::left("node_graph")
            .default_width(400.0)
            .min_width(300.0)
            .resizable(true)
            .show(ctx, |ui| {
                ui.heading("Network");
                ui.separator();
                self.network_view.show(ui, &mut self.state.library);

                // Update selected node from network view
                let selected = self.network_view.selected_nodes();
                if selected.len() == 1 {
                    self.state.selected_node = selected.iter().next().cloned();
                } else if selected.is_empty() {
                    self.state.selected_node = None;
                }
            });

        // Right panel: Parameters
        egui::SidePanel::right("parameters")
            .default_width(250.0)
            .resizable(true)
            .show(ctx, |ui| {
                ui.heading("Parameters");
                ui.separator();
                self.parameters.show(ui, &mut self.state);
            });

        // Node library panel (optional)
        if self.show_node_library {
            egui::TopBottomPanel::bottom("node_library")
                .default_height(150.0)
                .resizable(true)
                .show(ctx, |ui| {
                    ui.horizontal(|ui| {
                        ui.heading("Node Library");
                        if ui.small_button("×").clicked() {
                            self.show_node_library = false;
                        }
                    });
                    ui.separator();
                    self.node_library.show(ui, &mut self.state.library);
                });
        }

        // Bottom status bar
        egui::TopBottomPanel::bottom("status_bar").show(ctx, |ui| {
            ui.horizontal(|ui| {
                if let Some(ref path) = self.state.current_file {
                    ui.label(format!("File: {}", path.display()));
                } else {
                    ui.label("Untitled");
                }
                ui.separator();
                ui.label(format!("Zoom: {:.0}%", self.canvas.zoom() * 100.0));
                ui.separator();
                ui.label(format!("Nodes: {}", self.state.library.root.children.len()));
            });
        });

        // Update canvas handles when selection changes
        self.canvas.update_handles_for_node(self.state.selected_node.as_deref(), &self.state);

        // Central panel: Canvas viewer
        egui::CentralPanel::default().show(ctx, |ui| {
            // Get the rect before showing canvas
            let rect = ui.available_rect_before_wrap();

            self.canvas.show(ui, &self.state);

            // Handle interaction with canvas handles
            if let Some((param_name, new_position)) = self.canvas.handle_interaction(ui, rect) {
                // Update the parameter in the selected node
                if let Some(ref node_name) = self.state.selected_node {
                    if let Some(node) = self.state.library.root.child_mut(node_name) {
                        // Update the appropriate parameter
                        match param_name.as_str() {
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
                                // For rect size handle
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
                                if let Some(port) = node.input_mut(&param_name) {
                                    port.value = nodebox_core::Value::Point(new_position);
                                }
                            }
                            _ => {}
                        }
                    }
                }
            }
        });

        // About dialog
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
            // Save: Ctrl+S
            if i.modifiers.command && i.key_pressed(egui::Key::S) {
                // Note: Can't call self.save_file() here due to borrow checker
                // Would need to refactor to handle this
            }
        });

        // Check for state changes and auto-save to history
        self.auto_save_history();
    }
}

impl NodeBoxApp {
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
}
