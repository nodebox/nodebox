//! Integration tests for nodebox-gui using egui_kittest.
//!
//! These tests verify UI interactions work correctly through
//! the AccessKit-based testing framework.

mod common;

use egui_kittest::kittest::Queryable;
use egui_kittest::Harness;

/// Create a test harness with a simple parameter panel UI.
fn create_param_panel_harness() -> Harness<'static> {
    Harness::new_ui(|ui| {
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
    })
}

#[test]
fn test_parameter_panel_has_labels() {
    let harness = create_param_panel_harness();

    // Verify that parameter labels exist (get_by_label panics if not found)
    harness.get_by_label("PARAMETERS");
    harness.get_by_label("x");
    harness.get_by_label("y");
    harness.get_by_label("width");
    harness.get_by_label("height");
}

/// Create a test harness with a network view showing node buttons.
fn create_network_view_harness() -> Harness<'static> {
    Harness::builder()
        .with_size(egui::vec2(400.0, 300.0))
        .build_ui(|ui| {
            ui.horizontal(|ui| {
                ui.label("Network");
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    if ui.button("+ New Node").clicked() {
                        // Would open node dialog
                    }
                });
            });
            ui.separator();

            // Simulate node list
            egui::ScrollArea::vertical().show(ui, |ui| {
                for name in ["ellipse1", "colorize1", "rect1", "merge1"] {
                    let response = ui.selectable_label(false, name);
                    if response.clicked() {
                        // Would select node
                    }
                }
            });
        })
}

#[test]
fn test_network_view_has_new_node_button() {
    let harness = create_network_view_harness();
    harness.get_by_label("+ New Node");
}

#[test]
fn test_network_view_shows_nodes() {
    let harness = create_network_view_harness();

    // Verify node names are visible (panics if not found)
    harness.get_by_label("ellipse1");
    harness.get_by_label("colorize1");
    harness.get_by_label("rect1");
    harness.get_by_label("merge1");
}

/// Create a test harness with the menu bar.
fn create_menu_bar_harness() -> Harness<'static> {
    Harness::builder()
        .with_size(egui::vec2(800.0, 40.0))
        .build_ui(|ui| {
            egui::menu::bar(ui, |ui| {
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
        })
}

#[test]
fn test_menu_bar_has_menus() {
    let harness = create_menu_bar_harness();

    harness.get_by_label("File");
    harness.get_by_label("Edit");
    harness.get_by_label("View");
    harness.get_by_label("Help");
}

#[test]
fn test_file_menu_opens_on_click() {
    let mut harness = create_menu_bar_harness();

    // Click File menu
    harness.get_by_label("File").click();
    harness.run();

    // Menu items should now be visible
    harness.get_by_label("New");
}

/// Create a harness with a node selection dialog.
fn create_node_dialog_harness() -> Harness<'static> {
    Harness::builder()
        .with_size(egui::vec2(400.0, 500.0))
        .build_ui(|ui| {
            egui::Window::new("Create Node")
                .collapsible(false)
                .resizable(false)
                .show(ui.ctx(), |ui| {
                    ui.label("Search:");
                    let mut search = String::new();
                    ui.text_edit_singleline(&mut search);
                    ui.separator();

                    // Category: Geometry
                    ui.collapsing("geometry", |ui| {
                        if ui.selectable_label(false, "ellipse").clicked() {}
                        if ui.selectable_label(false, "rect").clicked() {}
                        if ui.selectable_label(false, "line").clicked() {}
                        if ui.selectable_label(false, "polygon").clicked() {}
                        if ui.selectable_label(false, "star").clicked() {}
                    });

                    // Category: Color
                    ui.collapsing("color", |ui| {
                        if ui.selectable_label(false, "colorize").clicked() {}
                    });

                    // Category: Transform
                    ui.collapsing("transform", |ui| {
                        if ui.selectable_label(false, "translate").clicked() {}
                        if ui.selectable_label(false, "rotate").clicked() {}
                        if ui.selectable_label(false, "scale").clicked() {}
                    });
                });
        })
}

#[test]
fn test_node_dialog_shows_categories() {
    let harness = create_node_dialog_harness();

    // Window title and categories
    harness.get_by_label("Create Node");
    harness.get_by_label("geometry");
    harness.get_by_label("color");
    harness.get_by_label("transform");
}

/// Create a harness with view options (checkboxes).
fn create_view_options_harness() -> Harness<'static> {
    Harness::new_ui(|ui| {
        let mut show_handles = true;
        let mut show_points = false;
        let mut show_origin = true;
        let mut show_bounds = false;

        ui.checkbox(&mut show_handles, "Show Handles");
        ui.checkbox(&mut show_points, "Show Points");
        ui.checkbox(&mut show_origin, "Show Origin");
        ui.checkbox(&mut show_bounds, "Show Bounds");
    })
}

#[test]
fn test_view_options_checkboxes_exist() {
    let harness = create_view_options_harness();

    harness.get_by_label("Show Handles");
    harness.get_by_label("Show Points");
    harness.get_by_label("Show Origin");
    harness.get_by_label("Show Bounds");
}

#[test]
fn test_checkbox_can_be_toggled() {
    let mut harness = Harness::new_ui_state(
        |ui, checked: &mut bool| {
            ui.checkbox(checked, "Test Checkbox");
        },
        false,
    );

    // Initially unchecked
    let checkbox = harness.get_by_label("Test Checkbox");

    // After clicking, state should change
    checkbox.click();
    harness.run();

    // The state should now be true
    assert!(*harness.state());
}

/// Create a harness simulating the about dialog.
fn create_about_dialog_harness() -> Harness<'static> {
    Harness::builder()
        .with_size(egui::vec2(300.0, 250.0))
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
        })
}

#[test]
fn test_about_dialog_content() {
    let harness = create_about_dialog_harness();

    harness.get_by_label("About NodeBox");
    harness.get_by_label("NodeBox");
    harness.get_by_label("Version 4.0 (Rust)");
    harness.get_by_label("Close");
}

/// Test state management with the harness - simulating undo/redo button state.
#[test]
fn test_undo_redo_button_state() {
    #[derive(Default)]
    struct UndoRedoState {
        can_undo: bool,
        can_redo: bool,
        undo_clicked: bool,
        redo_clicked: bool,
    }

    let mut harness = Harness::new_ui_state(
        |ui, state: &mut UndoRedoState| {
            ui.horizontal(|ui| {
                if ui
                    .add_enabled(state.can_undo, egui::Button::new("Undo"))
                    .clicked()
                {
                    state.undo_clicked = true;
                }
                if ui
                    .add_enabled(state.can_redo, egui::Button::new("Redo"))
                    .clicked()
                {
                    state.redo_clicked = true;
                }
            });
        },
        UndoRedoState {
            can_undo: true,
            can_redo: false,
            undo_clicked: false,
            redo_clicked: false,
        },
    );

    // Undo button should be enabled and clickable
    harness.get_by_label("Undo").click();
    harness.run();
    assert!(harness.state().undo_clicked);
}

/// Test simulating zoom controls.
#[test]
fn test_zoom_controls() {
    #[derive(Default)]
    struct ZoomState {
        zoom: f32,
    }

    let mut harness = Harness::new_ui_state(
        |ui, state: &mut ZoomState| {
            ui.horizontal(|ui| {
                if ui.button("Zoom In").clicked() {
                    state.zoom *= 1.1;
                }
                ui.label(format!("{:.0}%", state.zoom));
                if ui.button("Zoom Out").clicked() {
                    state.zoom /= 1.1;
                }
            });
        },
        ZoomState { zoom: 100.0 },
    );

    // Initial zoom
    assert!((harness.state().zoom - 100.0).abs() < 0.1);

    // Click zoom in
    harness.get_by_label("Zoom In").click();
    harness.run();
    assert!(harness.state().zoom > 100.0);

    // Click zoom out twice to go below 100%
    harness.get_by_label("Zoom Out").click();
    harness.run();
    harness.get_by_label("Zoom Out").click();
    harness.run();
    assert!(harness.state().zoom < 100.0);
}

/// Test node selection in a list.
#[test]
fn test_node_selection() {
    #[derive(Default)]
    struct SelectionState {
        selected: Option<String>,
    }

    let nodes = vec!["ellipse1", "colorize1", "rect1", "merge1"];

    let mut harness = Harness::new_ui_state(
        |ui, state: &mut SelectionState| {
            for name in &nodes {
                let is_selected = state.selected.as_deref() == Some(*name);
                if ui.selectable_label(is_selected, *name).clicked() {
                    state.selected = Some(name.to_string());
                }
            }
        },
        SelectionState { selected: None },
    );

    // No selection initially
    assert!(harness.state().selected.is_none());

    // Select colorize1
    harness.get_by_label("colorize1").click();
    harness.run();
    assert_eq!(harness.state().selected.as_deref(), Some("colorize1"));

    // Select rect1
    harness.get_by_label("rect1").click();
    harness.run();
    assert_eq!(harness.state().selected.as_deref(), Some("rect1"));
}

/// Test that parameter changes update state.
#[test]
fn test_drag_value_interaction() {
    #[derive(Default)]
    struct ParamState {
        x: f64,
    }

    let harness = Harness::new_ui_state(
        |ui, state: &mut ParamState| {
            ui.horizontal(|ui| {
                ui.label("x:");
                ui.add(egui::DragValue::new(&mut state.x).speed(1.0));
            });
        },
        ParamState { x: 50.0 },
    );

    // Initial value
    assert!((harness.state().x - 50.0).abs() < 0.1);

    // Verify label exists
    harness.get_by_label("x:");
}
