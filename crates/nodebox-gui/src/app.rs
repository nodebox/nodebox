//! Main application state and update loop.

use eframe::egui;
use crate::canvas::CanvasViewer;
use crate::panels::{NodeGraphPanel, ParameterPanel};
use crate::state::AppState;

/// The main NodeBox application.
pub struct NodeBoxApp {
    state: AppState,
    canvas: CanvasViewer,
    node_graph: NodeGraphPanel,
    parameters: ParameterPanel,
}

impl NodeBoxApp {
    /// Create a new NodeBox application instance.
    pub fn new(_cc: &eframe::CreationContext<'_>) -> Self {
        Self {
            state: AppState::new(),
            canvas: CanvasViewer::new(),
            node_graph: NodeGraphPanel::new(),
            parameters: ParameterPanel::new(),
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
                    if ui.button("Undo").clicked() {
                        // TODO: Implement undo
                        ui.close_menu();
                    }
                    if ui.button("Redo").clicked() {
                        // TODO: Implement redo
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
            .default_width(300.0)
            .resizable(true)
            .show(ctx, |ui| {
                ui.heading("Network");
                ui.separator();
                self.node_graph.show(ui, &mut self.state);
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
            });
        });

        // Central panel: Canvas viewer
        egui::CentralPanel::default().show(ctx, |ui| {
            self.canvas.show(ui, &self.state);
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
