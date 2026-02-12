//! End-to-end workflow tests for NodeBox GUI.
//!
//! These tests simulate complete user workflows through the AppState and
//! evaluate_network path: creating nodes, connecting them, evaluating,
//! modifying parameters, undoing/redoing, and verifying the final state.
//!
//! Unlike integration_tests.rs (which tests mocked UI widgets), these tests
//! exercise the actual data pipeline that the real GUI uses.

mod common;

use std::sync::Arc;

use nodebox_core::geometry::{Color, Point};
use nodebox_core::node::{Connection, Node, NodeLibrary, Port};
use nodebox_gui::eval::evaluate_network;
use nodebox_gui::{populate_default_ports, AppState, History};
use nodebox_port::{Port as PortTrait, ProjectContext, TestPort};

/// Helper: create a test port and project context for evaluation.
fn test_env() -> (Arc<dyn PortTrait>, ProjectContext) {
    (Arc::new(TestPort::new()), ProjectContext::new_unsaved())
}

/// Helper: evaluate the network in a state, assert no errors, return geometry.
fn evaluate(state: &AppState) -> Vec<nodebox_core::geometry::Path> {
    let (port, ctx) = test_env();
    let (geometry, _output, errors) = evaluate_network(&state.library, &port, &ctx);
    assert!(
        errors.is_empty(),
        "Unexpected evaluation errors: {:?}",
        errors.iter().map(|e| &e.message).collect::<Vec<_>>()
    );
    geometry
}

// ============================================================================
// Scenario 1: Create a node, set it rendered, verify geometry is produced
// ============================================================================

#[test]
fn test_create_node_set_rendered_produces_geometry() {
    let mut state = AppState::new();
    // Start with a fresh empty library
    state.library = NodeLibrary::new("test");

    // Create a rect node
    state.library.root.children.push(
        Node::new("rect1")
            .with_prototype("corevector.rect")
            .with_input(Port::point("position", Point::ZERO))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0)),
    );

    // Set it as rendered
    state.library.root.rendered_child = Some("rect1".to_string());

    // Evaluate and verify geometry is produced
    let geometry = evaluate(&state);
    assert_eq!(geometry.len(), 1, "Rect should produce one path");
}

// ============================================================================
// Scenario 2: Create + render + delete → canvas empty (user's example)
// ============================================================================

#[test]
fn test_create_render_delete_canvas_empty() {
    let mut state = AppState::new();
    state.library = NodeLibrary::new("test");

    // 1. Create a node
    state.library.root.children.push(
        Node::new("ellipse1")
            .with_prototype("corevector.ellipse")
            .with_input(Port::point("position", Point::ZERO))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0)),
    );

    // 2. Set it rendered
    state.library.root.rendered_child = Some("ellipse1".to_string());

    // 3. Verify geometry exists
    let geometry = evaluate(&state);
    assert_eq!(geometry.len(), 1, "Should have one shape before deletion");

    // 4. Delete the node (same logic as network view delete handler)
    let name = "ellipse1";
    state.library.root.children.retain(|n| n.name != name);
    state
        .library
        .root
        .connections
        .retain(|c| c.output_node != name && c.input_node != name);
    if state.library.root.rendered_child.as_deref() == Some(name) {
        state.library.root.rendered_child = None;
    }

    // 5. Evaluate — canvas should be empty and nothing crashes
    let geometry = evaluate(&state);
    assert!(
        geometry.is_empty(),
        "Canvas should be empty after deleting the only node"
    );
    assert!(state.library.root.children.is_empty());
}

// ============================================================================
// Scenario 3: Multi-node pipeline with connections
// ============================================================================

#[test]
fn test_multi_node_pipeline_colorize() {
    let mut state = AppState::new();
    state.library = NodeLibrary::new("test");

    // Create ellipse → colorize pipeline
    state.library.root = Node::network("root")
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
                .with_input(Port::color("fill", Color::rgb(1.0, 0.0, 0.0))),
        )
        .with_connection(Connection::new("ellipse1", "colorize1", "shape"))
        .with_rendered_child("colorize1");

    populate_default_ports(&mut state.library.root);

    let geometry = evaluate(&state);
    assert_eq!(geometry.len(), 1, "Pipeline should produce one path");
    assert!(
        geometry[0].fill.is_some(),
        "Colorized shape should have a fill color"
    );
}

// ============================================================================
// Scenario 4: Undo/redo through a complete workflow
// ============================================================================

#[test]
fn test_undo_redo_full_workflow() {
    let mut state = AppState::new();
    state.library = NodeLibrary::new("test");
    let mut history = History::new();

    // Save empty state (the state before adding a node)
    history.save_state(&state.library);

    // Step 1: Add a rect with width=100
    state.library.root.children.push(
        Node::new("rect1")
            .with_prototype("corevector.rect")
            .with_input(Port::point("position", Point::ZERO))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0)),
    );
    state.library.root.rendered_child = Some("rect1".to_string());

    // Save state with width=100 (before changing width)
    history.save_state(&state.library);

    // Step 2: Change width to 200 (current state, not saved)
    state
        .library
        .root
        .child_mut("rect1")
        .unwrap()
        .input_mut("width")
        .unwrap()
        .value = nodebox_core::Value::Float(200.0);

    let geometry = evaluate(&state);
    assert_eq!(geometry.len(), 1);

    // Step 3: Undo — should restore to saved state (width=100)
    let restored = history.undo(&state.library).unwrap();
    state.library = restored;

    let width = state
        .library
        .root
        .child("rect1")
        .unwrap()
        .input("width")
        .unwrap()
        .value
        .as_float()
        .unwrap();
    assert!(
        (width - 100.0).abs() < 0.1,
        "Undo should restore width to 100, got {}",
        width
    );

    // Step 4: Redo — should go back to width=200
    let next = history.redo(&state.library).unwrap();
    state.library = next;

    let width = state
        .library
        .root
        .child("rect1")
        .unwrap()
        .input("width")
        .unwrap()
        .value
        .as_float()
        .unwrap();
    assert!(
        (width - 200.0).abs() < 0.1,
        "Redo should restore width to 200, got {}",
        width
    );
}

// ============================================================================
// Scenario 5: Parameter change updates geometry
// ============================================================================

#[test]
fn test_parameter_change_updates_geometry() {
    let mut state = AppState::new();
    state.library = NodeLibrary::new("test");

    state.library.root.children.push(
        Node::new("rect1")
            .with_prototype("corevector.rect")
            .with_input(Port::point("position", Point::ZERO))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0)),
    );
    state.library.root.rendered_child = Some("rect1".to_string());

    let geom1 = evaluate(&state);
    let bounds1 = geom1[0].bounds().unwrap();

    // Change width from 100 to 200
    state
        .library
        .root
        .child_mut("rect1")
        .unwrap()
        .input_mut("width")
        .unwrap()
        .value = nodebox_core::Value::Float(200.0);

    let geom2 = evaluate(&state);
    let bounds2 = geom2[0].bounds().unwrap();

    assert!(
        bounds2.width > bounds1.width,
        "Width should increase: before={}, after={}",
        bounds1.width,
        bounds2.width
    );
}

// ============================================================================
// Scenario 6: Save/load round-trip preserves network and geometry
// ============================================================================

#[test]
fn test_save_load_roundtrip_preserves_geometry() {
    let mut state = AppState::new();
    state.library = NodeLibrary::new("test");

    // Build a network
    state.library.root.children.push(
        Node::new("ellipse1")
            .with_prototype("corevector.ellipse")
            .with_input(Port::point("position", Point::new(50.0, 50.0)))
            .with_input(Port::float("width", 120.0))
            .with_input(Port::float("height", 80.0)),
    );
    state.library.root.rendered_child = Some("ellipse1".to_string());

    let geom_before = evaluate(&state);

    // Save to temp file
    let tmp = tempfile::NamedTempFile::new().unwrap();
    state.save_file(tmp.path()).unwrap();

    // Load into fresh state
    let mut state2 = AppState::new();
    state2.load_file(tmp.path()).unwrap();

    let geom_after = evaluate(&state2);

    assert_eq!(
        geom_before.len(),
        geom_after.len(),
        "Round-trip should preserve geometry count"
    );
}

// ============================================================================
// Scenario 7: Delete middle node from chain cleans up connections
// ============================================================================

#[test]
fn test_delete_middle_node_cleans_connections() {
    let mut state = AppState::new();
    state.library = NodeLibrary::new("test");

    // ellipse → colorize (rendered)
    state.library.root = Node::network("root")
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
                .with_input(Port::color("fill", Color::rgb(1.0, 0.0, 0.0))),
        )
        .with_connection(Connection::new("ellipse1", "colorize1", "shape"))
        .with_rendered_child("colorize1");
    populate_default_ports(&mut state.library.root);

    assert_eq!(state.library.root.connections.len(), 1);

    // Delete ellipse1 — connection should be cleaned up
    let name = "ellipse1";
    state.library.root.children.retain(|n| n.name != name);
    state
        .library
        .root
        .connections
        .retain(|c| c.output_node != name && c.input_node != name);

    assert!(
        state.library.root.connections.is_empty(),
        "Connection should be removed when source node is deleted"
    );
    assert_eq!(state.library.root.children.len(), 1); // only colorize1 remains

    // Evaluation should not crash even though colorize has a dangling input
    let (port, ctx) = test_env();
    let (_geometry, _output, _errors) = evaluate_network(&state.library, &port, &ctx);
}

// ============================================================================
// Scenario 8: Switch rendered node produces different geometry
// ============================================================================

#[test]
fn test_switch_rendered_node() {
    let mut state = AppState::new();
    state.library = NodeLibrary::new("test");

    // Two nodes at different positions
    state.library.root = Node::network("root")
        .with_child(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::new(-100.0, 0.0)))
                .with_input(Port::float("width", 50.0))
                .with_input(Port::float("height", 50.0)),
        )
        .with_child(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::point("position", Point::new(100.0, 0.0)))
                .with_input(Port::float("width", 200.0))
                .with_input(Port::float("height", 200.0)),
        )
        .with_rendered_child("rect1");

    // Render rect1 — small shape at left
    let geom_rect = evaluate(&state);
    assert_eq!(geom_rect.len(), 1);
    let rect_bounds = geom_rect[0].bounds().unwrap();

    // Switch to ellipse1 — larger shape at right
    state.library.root.rendered_child = Some("ellipse1".to_string());
    let geom_ellipse = evaluate(&state);
    assert_eq!(geom_ellipse.len(), 1);
    let ellipse_bounds = geom_ellipse[0].bounds().unwrap();

    assert!(
        ellipse_bounds.width > rect_bounds.width,
        "Ellipse (width=200) should be wider than rect (width=50)"
    );
}

// ============================================================================
// Scenario 9: Create multiple nodes, delete all, verify empty
// ============================================================================

#[test]
fn test_create_multiple_delete_all_empty() {
    let mut state = AppState::new();
    state.library = NodeLibrary::new("test");

    // Create 3 nodes
    for name in &["rect1", "ellipse1", "polygon1"] {
        state.library.root.children.push(
            Node::new(*name)
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        );
    }
    state.library.root.rendered_child = Some("rect1".to_string());

    let geometry = evaluate(&state);
    assert_eq!(geometry.len(), 1);
    assert_eq!(state.library.root.children.len(), 3);

    // Delete all nodes
    state.library.root.children.clear();
    state.library.root.connections.clear();
    state.library.root.rendered_child = None;

    let geometry = evaluate(&state);
    assert!(
        geometry.is_empty(),
        "Canvas should be empty after deleting all nodes"
    );
    assert!(state.library.root.children.is_empty());
}
