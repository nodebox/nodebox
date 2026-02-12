//! Screenshot (snapshot) tests for nodebox-gui.
//!
//! These tests render UI components and compare them against baseline
//! screenshots stored in `tests/snapshots/`. This lets us visually verify
//! that layouts, colors, and alignment are correct.
//!
//! # Updating baselines
//!
//! Run with `UPDATE_SNAPSHOTS=true cargo test` to accept new/changed images.
//! Run with `UPDATE_SNAPSHOTS=force` to regenerate all baselines.

use egui_kittest::Harness;

/// Screenshot the parameter panel showing typical ellipse-style controls.
#[test]
fn screenshot_parameter_panel() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(260.0, 200.0))
        .build_ui(|ui| {
            ui.label("PARAMETERS");
            ui.separator();
            ui.horizontal(|ui| {
                ui.label("x");
                let mut value = 0.0f64;
                ui.add(egui::DragValue::new(&mut value).speed(1.0));
            });
            ui.horizontal(|ui| {
                ui.label("y");
                let mut value = 0.0f64;
                ui.add(egui::DragValue::new(&mut value).speed(1.0));
            });
            ui.horizontal(|ui| {
                ui.label("width");
                let mut value = 100.0f64;
                ui.add(egui::DragValue::new(&mut value).speed(1.0));
            });
            ui.horizontal(|ui| {
                ui.label("height");
                let mut value = 100.0f64;
                ui.add(egui::DragValue::new(&mut value).speed(1.0));
            });
        });

    harness.run();
    harness.snapshot("parameter_panel");
}

/// Screenshot the network view with a list of nodes.
#[test]
fn screenshot_network_view() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(300.0, 260.0))
        .build_ui(|ui| {
            ui.horizontal(|ui| {
                ui.label("Network");
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    if ui.button("+ New Node").clicked() {}
                });
            });
            ui.separator();

            egui::ScrollArea::vertical().show(ui, |ui| {
                for name in ["ellipse1", "colorize1", "rect1", "merge1"] {
                    let _ = ui.selectable_label(false, name);
                }
            });
        });

    harness.run();
    harness.snapshot("network_view");
}

/// Screenshot the menu bar.
#[test]
fn screenshot_menu_bar() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(600.0, 30.0))
        .build_ui(|ui| {
            egui::MenuBar::new().ui(ui, |ui| {
                ui.menu_button("File", |ui| {
                    if ui.button("New").clicked() {}
                    if ui.button("Open...").clicked() {}
                    if ui.button("Save").clicked() {}
                    if ui.button("Save As...").clicked() {}
                    ui.separator();
                    if ui.button("Export SVG...").clicked() {}
                    if ui.button("Export PNG...").clicked() {}
                    ui.separator();
                    if ui.button("Quit").clicked() {}
                });
                ui.menu_button("Edit", |ui| {
                    if ui.button("Undo").clicked() {}
                    if ui.button("Redo").clicked() {}
                });
                ui.menu_button("View", |ui| {
                    if ui.button("Zoom In").clicked() {}
                    if ui.button("Zoom Out").clicked() {}
                });
                ui.menu_button("Help", |ui| {
                    if ui.button("About NodeBox").clicked() {}
                });
            });
        });

    harness.run();
    harness.snapshot("menu_bar");
}

/// Screenshot the File menu in its open state.
#[test]
fn screenshot_file_menu_open() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(600.0, 300.0))
        .build_ui(|ui| {
            egui::MenuBar::new().ui(ui, |ui| {
                ui.menu_button("File", |ui| {
                    if ui.button("New").clicked() {}
                    if ui.button("Open...").clicked() {}
                    if ui.button("Save").clicked() {}
                    if ui.button("Save As...").clicked() {}
                    ui.separator();
                    if ui.button("Export SVG...").clicked() {}
                    if ui.button("Export PNG...").clicked() {}
                    ui.separator();
                    if ui.button("Quit").clicked() {}
                });
                ui.menu_button("Edit", |ui| {
                    if ui.button("Undo").clicked() {}
                    if ui.button("Redo").clicked() {}
                });
                ui.menu_button("View", |ui| {
                    if ui.button("Zoom In").clicked() {}
                    if ui.button("Zoom Out").clicked() {}
                });
                ui.menu_button("Help", |ui| {
                    if ui.button("About NodeBox").clicked() {}
                });
            });
        });

    // Open the File menu
    use egui_kittest::kittest::Queryable;
    harness.get_by_label("File").click();
    harness.run();

    harness.snapshot("file_menu_open");
}

/// Screenshot the node selection dialog with categories.
#[test]
fn screenshot_node_dialog() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(350.0, 420.0))
        .build_ui(|ui| {
            egui::Window::new("Create Node")
                .collapsible(false)
                .resizable(false)
                .show(ui.ctx(), |ui| {
                    ui.label("Search:");
                    let mut search = String::new();
                    ui.text_edit_singleline(&mut search);
                    ui.separator();

                    ui.collapsing("geometry", |ui| {
                        let _ = ui.selectable_label(false, "ellipse");
                        let _ = ui.selectable_label(false, "rect");
                        let _ = ui.selectable_label(false, "line");
                        let _ = ui.selectable_label(false, "polygon");
                        let _ = ui.selectable_label(false, "star");
                    });

                    ui.collapsing("color", |ui| {
                        let _ = ui.selectable_label(false, "colorize");
                    });

                    ui.collapsing("transform", |ui| {
                        let _ = ui.selectable_label(false, "translate");
                        let _ = ui.selectable_label(false, "rotate");
                        let _ = ui.selectable_label(false, "scale");
                    });
                });
        });

    harness.run();
    harness.snapshot("node_dialog");
}

/// Screenshot view option checkboxes in various states.
#[test]
fn screenshot_view_options() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(200.0, 140.0))
        .build_ui(|ui| {
            let mut show_handles = true;
            let mut show_points = false;
            let mut show_origin = true;
            let mut show_canvas = true;

            ui.checkbox(&mut show_handles, "Show Handles");
            ui.checkbox(&mut show_points, "Show Points");
            ui.checkbox(&mut show_origin, "Show Origin");
            ui.checkbox(&mut show_canvas, "Show Canvas");
        });

    harness.run();
    harness.snapshot("view_options");
}

/// Screenshot the About dialog.
#[test]
fn screenshot_about_dialog() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(350.0, 280.0))
        .build_ui(|ui| {
            egui::Window::new("About NodeBox")
                .collapsible(false)
                .resizable(false)
                .anchor(egui::Align2::CENTER_CENTER, [0.0, 0.0])
                .show(ui.ctx(), |ui| {
                    ui.vertical_centered(|ui| {
                        ui.heading("NodeBox");
                        ui.label("Version 4.0 (Rust)");
                        ui.add_space(10.0);
                        ui.label("A node-based generative design tool");
                        ui.add_space(10.0);
                        ui.hyperlink_to("Visit website", "https://www.nodebox.net");
                        ui.add_space(10.0);
                        if ui.button("Close").clicked() {}
                    });
                });
        });

    harness.run();
    harness.snapshot("about_dialog");
}

/// Screenshot undo/redo toolbar with different enabled states.
#[test]
fn screenshot_undo_redo_enabled() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(200.0, 50.0))
        .build_ui(|ui| {
            ui.horizontal(|ui| {
                ui.add_enabled(true, egui::Button::new("Undo"));
                ui.add_enabled(false, egui::Button::new("Redo"));
            });
        });

    harness.run();
    harness.snapshot("undo_redo_enabled");
}

/// Screenshot zoom controls at 100%.
#[test]
fn screenshot_zoom_controls() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(250.0, 50.0))
        .build_ui(|ui| {
            ui.horizontal(|ui| {
                if ui.button("Zoom In").clicked() {}
                ui.label("100%");
                if ui.button("Zoom Out").clicked() {}
            });
        });

    harness.run();
    harness.snapshot("zoom_controls");
}

/// Screenshot a node list with one item selected.
#[test]
fn screenshot_node_list_with_selection() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(200.0, 160.0))
        .build_ui(|ui| {
            let selected = "colorize1";
            for name in ["ellipse1", "colorize1", "rect1", "merge1"] {
                let _ = ui.selectable_label(name == selected, name);
            }
        });

    harness.run();
    harness.snapshot("node_list_with_selection");
}

/// Screenshot a multi-parameter panel resembling a real colorize node.
#[test]
fn screenshot_colorize_parameters() {
    let mut harness = Harness::builder()
        .with_size(egui::vec2(280.0, 250.0))
        .build_ui(|ui| {
            ui.label("PARAMETERS — colorize1");
            ui.separator();

            // Fill color row
            ui.horizontal(|ui| {
                ui.label("fill");
                let mut color = [1.0_f32, 0.0, 0.0];
                ui.color_edit_button_rgb(&mut color);
            });

            // Stroke color row
            ui.horizontal(|ui| {
                ui.label("stroke");
                let mut color = [0.0_f32, 0.0, 0.0];
                ui.color_edit_button_rgb(&mut color);
            });

            // Stroke width row
            ui.horizontal(|ui| {
                ui.label("strokeWidth");
                let mut value = 2.0f64;
                ui.add(egui::DragValue::new(&mut value).speed(0.1));
            });
        });

    harness.run();
    harness.snapshot("colorize_parameters");
}
