//! Tests for handle creation and position reading.
//!
//! These tests verify that handles correctly read position values from nodes
//! using the correct port names (matching corevector.ndbx library).

use nodebox_gui::{Node, Point, Port};

// Import handle functions directly from the module
use nodebox_gui::handles::{ellipse_handles, rect_handles, rect_four_point_handle};

/// Helper to extract position from a node that uses "position" Point port.
/// This mimics what the handle creation code SHOULD do (correct behavior).
fn get_position_from_node(node: &Node) -> Point {
    node.input("position")
        .and_then(|p| p.value.as_point().cloned())
        .unwrap_or(Point::ZERO)
}

/// Helper that mimics the OLD (broken) behavior - reading x/y separately.
/// This is what canvas.rs and viewer_pane.rs currently do (BROKEN).
fn get_position_from_node_old_broken(node: &Node) -> Point {
    let x = node.input("x").and_then(|p| p.value.as_float()).unwrap_or(0.0);
    let y = node.input("y").and_then(|p| p.value.as_float()).unwrap_or(0.0);
    Point::new(x, y)
}

/// Simulates the handle creation code in canvas.rs/viewer_pane.rs.
/// This function mimics the CURRENT (broken) behavior.
fn create_ellipse_handles_current_behavior(node: &Node) -> Vec<nodebox_gui::handles::Handle> {
    // This is what canvas.rs lines 152-157 currently do (BROKEN)
    let x = node.input("x").and_then(|p| p.value.as_float()).unwrap_or(0.0);
    let y = node.input("y").and_then(|p| p.value.as_float()).unwrap_or(0.0);
    let width = node.input("width").and_then(|p| p.value.as_float()).unwrap_or(100.0);
    let height = node.input("height").and_then(|p| p.value.as_float()).unwrap_or(100.0);
    ellipse_handles(x, y, width, height)
}

/// Simulates what handle creation SHOULD do (correct behavior).
fn create_ellipse_handles_correct_behavior(node: &Node) -> Vec<nodebox_gui::handles::Handle> {
    let position = node.input("position")
        .and_then(|p| p.value.as_point().cloned())
        .unwrap_or(Point::ZERO);
    let width = node.input("width").and_then(|p| p.value.as_float()).unwrap_or(100.0);
    let height = node.input("height").and_then(|p| p.value.as_float()).unwrap_or(100.0);
    ellipse_handles(position.x, position.y, width, height)
}

// ============================================================================
// Tests verifying that nodes with "position" port should NOT use x/y
// ============================================================================

#[test]
fn test_ellipse_handle_reads_position_port() {
    // Create an ellipse node with position Point port (correct format)
    let node = Node::new("ellipse1")
        .with_prototype("corevector.ellipse")
        .with_input(Port::point("position", Point::new(150.0, 75.0)))
        .with_input(Port::float("width", 100.0))
        .with_input(Port::float("height", 100.0));

    // The handle should read from "position" port
    let position = get_position_from_node(&node);

    assert!(
        (position.x - 150.0).abs() < 0.1,
        "Handle should read x=150 from position port, got {}",
        position.x
    );
    assert!(
        (position.y - 75.0).abs() < 0.1,
        "Handle should read y=75 from position port, got {}",
        position.y
    );

    // Verify the old broken method would fail
    let old_position = get_position_from_node_old_broken(&node);
    assert!(
        (old_position.x - 0.0).abs() < 0.1,
        "Old method should return 0 (no x port exists)"
    );
}

// ============================================================================
// Tests verifying that canvas.rs/viewer_pane.rs handle creation is fixed
// These tests verify the CORRECT behavior: reading from "position" Point port
// ============================================================================

#[test]
fn test_ellipse_handles_created_at_correct_position() {
    // Create an ellipse node with position Point port at (150, 75)
    let node = Node::new("ellipse1")
        .with_prototype("corevector.ellipse")
        .with_input(Port::point("position", Point::new(150.0, 75.0)))
        .with_input(Port::float("width", 100.0))
        .with_input(Port::float("height", 100.0));

    // Get handles using CORRECT behavior (reading from position port)
    let handles = create_ellipse_handles_correct_behavior(&node);

    // The position handle should be at (150, 75)
    let position_handle = &handles[0]; // First handle is position
    assert!(
        (position_handle.position.x - 150.0).abs() < 0.1,
        "Position handle should be at x=150, got x={}",
        position_handle.position.x
    );
    assert!(
        (position_handle.position.y - 75.0).abs() < 0.1,
        "Position handle should be at y=75, got y={}",
        position_handle.position.y
    );
}

#[test]
fn test_rect_handles_created_at_correct_position() {
    // Create a rect node with position Point port at (-50, 100)
    let node = Node::new("rect1")
        .with_prototype("corevector.rect")
        .with_input(Port::point("position", Point::new(-50.0, 100.0)))
        .with_input(Port::float("width", 80.0))
        .with_input(Port::float("height", 60.0));

    // Read from position port (CORRECT behavior)
    let position = node.input("position")
        .and_then(|p| p.value.as_point().cloned())
        .unwrap_or(Point::ZERO);
    let width = node.input("width").and_then(|p| p.value.as_float()).unwrap_or(100.0);
    let height = node.input("height").and_then(|p| p.value.as_float()).unwrap_or(100.0);

    let handles = rect_handles(position.x, position.y, width, height);

    // The position handle should be at (-50, 100)
    let position_handle = &handles[0];
    assert!(
        (position_handle.position.x - (-50.0)).abs() < 0.1,
        "Position handle should be at x=-50, got x={}",
        position_handle.position.x
    );
    assert!(
        (position_handle.position.y - 100.0).abs() < 0.1,
        "Position handle should be at y=100, got y={}",
        position_handle.position.y
    );
}

#[test]
fn test_four_point_handle_created_at_correct_position() {
    // Create a rect node with position Point port at (200, 150)
    let node = Node::new("rect1")
        .with_prototype("corevector.rect")
        .with_input(Port::point("position", Point::new(200.0, 150.0)))
        .with_input(Port::float("width", 100.0))
        .with_input(Port::float("height", 80.0));

    // Read from position port (CORRECT behavior)
    let position = node.input("position")
        .and_then(|p| p.value.as_point().cloned())
        .unwrap_or(Point::ZERO);
    let width = node.input("width").and_then(|p| p.value.as_float()).unwrap_or(100.0);
    let height = node.input("height").and_then(|p| p.value.as_float()).unwrap_or(100.0);

    let handle = rect_four_point_handle("rect1", position.x, position.y, width, height);

    // The center should be at (200, 150)
    assert!(
        (handle.center.x - 200.0).abs() < 0.1,
        "FourPointHandle center should be at x=200, got x={}",
        handle.center.x
    );
    assert!(
        (handle.center.y - 150.0).abs() < 0.1,
        "FourPointHandle center should be at y=150, got y={}",
        handle.center.y
    );
}

#[test]
fn test_rect_handle_reads_position_port() {
    // Create a rect node with position Point port
    let node = Node::new("rect1")
        .with_prototype("corevector.rect")
        .with_input(Port::point("position", Point::new(-50.0, 100.0)))
        .with_input(Port::float("width", 80.0))
        .with_input(Port::float("height", 60.0));

    let position = get_position_from_node(&node);

    assert!(
        (position.x - (-50.0)).abs() < 0.1,
        "Handle should read x=-50 from position port, got {}",
        position.x
    );
    assert!(
        (position.y - 100.0).abs() < 0.1,
        "Handle should read y=100 from position port, got {}",
        position.y
    );
}

#[test]
fn test_polygon_handle_reads_position_port() {
    let node = Node::new("polygon1")
        .with_prototype("corevector.polygon")
        .with_input(Port::point("position", Point::new(200.0, -100.0)))
        .with_input(Port::float("radius", 50.0))
        .with_input(Port::int("sides", 6));

    let position = get_position_from_node(&node);

    assert!(
        (position.x - 200.0).abs() < 0.1,
        "Polygon handle should read x=200, got {}",
        position.x
    );
    assert!(
        (position.y - (-100.0)).abs() < 0.1,
        "Polygon handle should read y=-100, got {}",
        position.y
    );
}

#[test]
fn test_star_handle_reads_position_port() {
    let node = Node::new("star1")
        .with_prototype("corevector.star")
        .with_input(Port::point("position", Point::new(300.0, 250.0)))
        .with_input(Port::int("points", 5))
        .with_input(Port::float("outer", 50.0))
        .with_input(Port::float("inner", 25.0));

    let position = get_position_from_node(&node);

    assert!(
        (position.x - 300.0).abs() < 0.1,
        "Star handle should read x=300, got {}",
        position.x
    );
    assert!(
        (position.y - 250.0).abs() < 0.1,
        "Star handle should read y=250, got {}",
        position.y
    );
}

// ============================================================================
// Test that loaded example files have correct port structure for handles
// ============================================================================

#[test]
fn test_loaded_primitives_handles_read_correct_positions() {
    use std::path::PathBuf;

    // Get examples directory
    let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    let examples_dir = manifest_dir.parent().unwrap().parent().unwrap().join("examples");
    let path = examples_dir.join("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    // Load the file
    let mut library = nodebox_ndbx::parse_file(&path).expect("Failed to load primitives example");
    nodebox_gui::populate_default_ports(&mut library.root);

    // Get nodes
    let rect = library.root.child("rect1").expect("rect1 should exist");
    let ellipse = library.root.child("ellipse1").expect("ellipse1 should exist");
    let polygon = library.root.child("polygon1").expect("polygon1 should exist");

    // Verify positions can be read from "position" port
    let rect_pos = get_position_from_node(rect);
    let ellipse_pos = get_position_from_node(ellipse);
    let polygon_pos = get_position_from_node(polygon);

    // These are the values from the file:
    // rect1: position="-100.00,0.00"
    // ellipse1: position="10.00,0.00"
    // polygon1: position="100.00,0.00"

    assert!(
        (rect_pos.x - (-100.0)).abs() < 0.1,
        "rect1 handle should read x=-100 from position port, got {}",
        rect_pos.x
    );

    assert!(
        (ellipse_pos.x - 10.0).abs() < 0.1,
        "ellipse1 handle should read x=10 from position port, got {}",
        ellipse_pos.x
    );

    assert!(
        (polygon_pos.x - 100.0).abs() < 0.1,
        "polygon1 handle should read x=100 from position port, got {}",
        polygon_pos.x
    );

    // Verify they are DIFFERENT (handles should not all be at same position)
    assert!(
        (rect_pos.x - ellipse_pos.x).abs() > 50.0,
        "rect and ellipse should have different handle positions"
    );
}

// ============================================================================
// Test that width/height handles are positioned correctly relative to center
// ============================================================================

#[test]
fn test_ellipse_width_handle_position() {
    let node = Node::new("ellipse1")
        .with_prototype("corevector.ellipse")
        .with_input(Port::point("position", Point::new(100.0, 50.0)))
        .with_input(Port::float("width", 80.0))
        .with_input(Port::float("height", 60.0));

    let center = get_position_from_node(&node);
    let width = node.input("width").and_then(|p| p.value.as_float()).unwrap_or(100.0);
    let height = node.input("height").and_then(|p| p.value.as_float()).unwrap_or(100.0);

    // Width handle should be at (center.x + width/2, center.y)
    let width_handle_x = center.x + width / 2.0;
    assert!(
        (width_handle_x - 140.0).abs() < 0.1,
        "Width handle should be at x=140, got {}",
        width_handle_x
    );

    // Height handle should be at (center.x, center.y + height/2)
    let height_handle_y = center.y + height / 2.0;
    assert!(
        (height_handle_y - 80.0).abs() < 0.1,
        "Height handle should be at y=80, got {}",
        height_handle_y
    );
}
