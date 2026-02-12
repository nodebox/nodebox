//! App-level E2E tests using NodeBoxApp directly.
//!
//! These tests use the actual NodeBoxApp with update_for_testing(), which
//! simulates complete frame cycles including change detection, history
//! management, and synchronous evaluation — the same pipeline as the real GUI.

mod common;

use nodebox_core::geometry::{Color, Point};
use nodebox_core::node::{Connection, Node, Port};
use nodebox_gui::{populate_default_ports, NodeBoxApp};

// ============================================================================
// Full app lifecycle: create → render → delete → empty
// ============================================================================

#[test]
fn test_app_create_render_delete_lifecycle() {
    let mut app = NodeBoxApp::new_for_testing_empty();

    // Create a rect node
    app.state_mut().library.root.children.push(
        Node::new("rect1")
            .with_prototype("corevector.rect")
            .with_input(Port::point("position", Point::ZERO))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0)),
    );
    app.state_mut().library.root.rendered_child = Some("rect1".to_string());

    // Simulate frame update (detects change, saves history, evaluates)
    app.update_for_testing();

    // Verify geometry was produced
    assert_eq!(
        app.state().geometry.len(),
        1,
        "Should have geometry after creating and rendering a node"
    );
    assert!(
        app.history().can_undo(),
        "Should be able to undo after creating a node"
    );

    // Delete the node
    let name = "rect1";
    app.state_mut()
        .library
        .root
        .children
        .retain(|n| n.name != name);
    app.state_mut()
        .library
        .root
        .connections
        .retain(|c| c.output_node != name && c.input_node != name);
    if app
        .state()
        .library
        .root
        .rendered_child
        .as_deref()
        == Some(name)
    {
        app.state_mut().library.root.rendered_child = None;
    }

    // Simulate frame update
    app.update_for_testing();

    // Canvas should be empty
    assert!(
        app.state().geometry.is_empty(),
        "Canvas should be empty after deleting the node"
    );
}

// ============================================================================
// History integration through app
// ============================================================================

#[test]
fn test_app_history_tracks_changes() {
    let mut app = NodeBoxApp::new_for_testing_empty();

    assert!(
        !app.history().can_undo(),
        "Fresh app should have no undo history"
    );

    // Add a node
    app.state_mut().library.root.children.push(
        Node::new("ellipse1")
            .with_prototype("corevector.ellipse")
            .with_input(Port::point("position", Point::ZERO))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0)),
    );
    app.state_mut().library.root.rendered_child = Some("ellipse1".to_string());

    app.update_for_testing();
    assert!(
        app.history().can_undo(),
        "Should have undo history after a change"
    );

    // Modify a parameter
    app.state_mut()
        .library
        .root
        .child_mut("ellipse1")
        .unwrap()
        .input_mut("width")
        .unwrap()
        .value = nodebox_core::Value::Float(200.0);

    app.update_for_testing();
    assert_eq!(
        app.history().undo_count(),
        2,
        "Should have 2 undo entries after 2 changes"
    );
}

// ============================================================================
// Dirty flag tracking
// ============================================================================

#[test]
fn test_app_dirty_flag_set_on_change() {
    let mut app = NodeBoxApp::new_for_testing_empty();

    assert!(
        !app.state().dirty,
        "Fresh app should not be dirty"
    );

    // Add a node
    app.state_mut().library.root.children.push(
        Node::new("rect1")
            .with_prototype("corevector.rect")
            .with_input(Port::point("position", Point::ZERO))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0)),
    );

    app.update_for_testing();
    assert!(
        app.state().dirty,
        "App should be dirty after modifying the library"
    );
}

// ============================================================================
// Multiple sequential operations
// ============================================================================

#[test]
fn test_app_multiple_operations() {
    let mut app = NodeBoxApp::new_for_testing_empty();

    // Create 3 nodes
    for name in &["rect1", "ellipse1", "polygon1"] {
        app.state_mut().library.root.children.push(
            Node::new(*name)
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
    }
    app.state_mut().library.root.rendered_child = Some("rect1".to_string());
    app.update_for_testing();

    assert_eq!(app.state().library.root.children.len(), 3);
    assert_eq!(app.state().geometry.len(), 1);

    // Delete 2 nodes (keep rect1)
    app.state_mut()
        .library
        .root
        .children
        .retain(|n| n.name == "rect1");
    app.update_for_testing();

    assert_eq!(app.state().library.root.children.len(), 1);
    assert_eq!(
        app.state().geometry.len(),
        1,
        "Rendered node (rect1) still exists, geometry should remain"
    );
}

// ============================================================================
// Connected pipeline through app
// ============================================================================

#[test]
fn test_app_connected_pipeline() {
    let mut app = NodeBoxApp::new_for_testing_empty();

    // Build ellipse → colorize pipeline
    app.state_mut().library.root = Node::network("root")
        .with_child(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        )
        .with_child(
            Node::new("colorize1")
                .with_prototype("corevector.colorize")
                .with_input(Port::geometry("shape"))
                .with_input(Port::color("fill", Color::rgb(0.0, 0.0, 1.0))),
        )
        .with_connection(Connection::new("ellipse1", "colorize1", "shape"))
        .with_rendered_child("colorize1");
    populate_default_ports(&mut app.state_mut().library.root);

    app.update_for_testing();

    assert_eq!(
        app.state().geometry.len(),
        1,
        "Pipeline should produce one shape"
    );
    assert!(
        app.state().geometry[0].fill.is_some(),
        "Shape should have a fill color from colorize"
    );
}

// ============================================================================
// Evaluation after switching rendered node
// ============================================================================

#[test]
fn test_app_switch_rendered_node() {
    let mut app = NodeBoxApp::new_for_testing_empty();

    // Two nodes with different sizes
    app.state_mut().library.root = Node::network("root")
        .with_child(
            Node::new("small_rect")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 50.0))
                .with_input(Port::float("height", 50.0)),
        )
        .with_child(
            Node::new("big_rect")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 200.0))
                .with_input(Port::float("height", 200.0)),
        )
        .with_rendered_child("small_rect");

    app.update_for_testing();
    let small_bounds = app.state().geometry[0].bounds().unwrap();

    // Switch rendered node
    app.state_mut().library.root.rendered_child = Some("big_rect".to_string());
    app.update_for_testing();
    let big_bounds = app.state().geometry[0].bounds().unwrap();

    assert!(
        big_bounds.width > small_bounds.width,
        "Big rect should produce wider geometry"
    );
}
