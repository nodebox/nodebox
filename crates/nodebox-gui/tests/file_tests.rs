//! Tests for loading and evaluating .ndbx files.
//!
//! Note: Old .ndbx files (version < 21) from the Java implementation are loaded
//! best-effort with a warning. These tests verify that behavior and test evaluation
//! with programmatically created libraries.

use std::path::PathBuf;
use std::sync::Arc;

use nodebox_core::geometry::{Color, Point};
use nodebox_core::node::{Connection, Node, NodeLibrary, Port};
use nodebox_gui::eval::evaluate_network;
use nodebox_gui::{populate_default_ports, AppState};
use nodebox_port::{Port as PortTrait, ProjectContext, TestPort};

/// Create a test port and project context for evaluation tests.
fn test_port_and_context() -> (Arc<dyn PortTrait>, ProjectContext) {
    (Arc::new(TestPort::new()), ProjectContext::new_unsaved())
}

/// Get the path to the examples directory.
fn examples_dir() -> PathBuf {
    let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    manifest_dir
        .parent()
        .unwrap()
        .parent()
        .unwrap()
        .join("examples")
}

/// Get the path to the libraries directory.
fn libraries_dir() -> PathBuf {
    let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    manifest_dir
        .parent()
        .unwrap()
        .parent()
        .unwrap()
        .join("libraries")
}

// ============================================================================
// Version compatibility tests
// ============================================================================

#[test]
fn test_old_version_files_load_with_warning() {
    // The Primitives example has formatVersion="17" which is below our minimum (21)
    // but should load best-effort with a warning
    let path = examples_dir().join("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");
    if !path.exists() {
        println!("Skipping test - example file not found");
        return;
    }

    let result = nodebox_ndbx::parse_file_with_warnings(&path);
    assert!(result.is_ok(), "Old version files should load best-effort");

    let (library, warnings) = result.unwrap();
    assert!(!warnings.is_empty(), "Should have a warning about old format version");
    assert!(warnings[0].contains("17"), "Warning should mention the file's version");
    assert_eq!(library.format_version, 22, "Should be upgraded to current version");
}

#[test]
fn test_library_files_can_be_loaded() {
    // Library files (corevector, etc.) have no formatVersion attribute,
    // which defaults to 21 and is supported
    let path = libraries_dir().join("corevector/corevector.ndbx");
    if !path.exists() {
        println!("Skipping test - library file not found");
        return;
    }

    let library = nodebox_ndbx::parse_file(&path).expect("Library files should load");

    // After upgrade, format version should be 22
    assert_eq!(library.format_version, 22);

    // Check that key nodes exist
    assert!(library.root.child("rect").is_some(), "Missing rect node");
    assert!(
        library.root.child("ellipse").is_some(),
        "Missing ellipse node"
    );
}

// ============================================================================
// Evaluation tests with programmatically created libraries
// ============================================================================

/// Create a test library similar to the Primitives example
fn create_primitives_library() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.set_width(1000.0);
    library.set_height(1000.0);

    // Create nodes similar to the Primitives example
    let rect1 = Node::new("rect1")
        .with_prototype("corevector.rect")
        .with_position(1.0, 1.0)
        .with_input(Port::point("position", Point::new(-100.0, 0.0)))
        .with_input(Port::float("width", 100.0))
        .with_input(Port::float("height", 100.0));

    let ellipse1 = Node::new("ellipse1")
        .with_prototype("corevector.ellipse")
        .with_position(4.0, 1.0)
        .with_input(Port::point("position", Point::new(10.0, 0.0)))
        .with_input(Port::float("width", 100.0))
        .with_input(Port::float("height", 100.0));

    let polygon1 = Node::new("polygon1")
        .with_prototype("corevector.polygon")
        .with_position(7.0, 1.0)
        .with_input(Port::point("position", Point::new(100.0, 0.0)))
        .with_input(Port::float("radius", 60.0))
        .with_input(Port::int("sides", 6));

    let colorize1 = Node::new("colorize1")
        .with_prototype("corevector.colorize")
        .with_position(1.0, 3.0)
        .with_input(Port::color("fill", Color::rgba(0.82, 0.42, 0.15, 1.0)));

    let colorize2 = Node::new("colorize2")
        .with_prototype("corevector.colorize")
        .with_position(4.0, 3.0)
        .with_input(Port::color("fill", Color::rgba(0.31, 0.62, 0.96, 1.0)));

    let colorize3 = Node::new("colorize3")
        .with_prototype("corevector.colorize")
        .with_position(7.0, 3.0)
        .with_input(Port::color("fill", Color::rgba(0.0, 0.10, 0.18, 1.0)));

    let combine1 = Node::new("combine1")
        .with_prototype("list.combine")
        .with_position(3.0, 5.0);

    library.root = Node::network("root")
        .with_child(rect1)
        .with_child(ellipse1)
        .with_child(polygon1)
        .with_child(colorize1)
        .with_child(colorize2)
        .with_child(colorize3)
        .with_child(combine1)
        .with_connection(Connection::new("rect1", "colorize1", "shape"))
        .with_connection(Connection::new("ellipse1", "colorize2", "shape"))
        .with_connection(Connection::new("polygon1", "colorize3", "shape"))
        .with_connection(Connection::new("colorize1", "combine1", "list1"))
        .with_connection(Connection::new("colorize2", "combine1", "list2"))
        .with_connection(Connection::new("colorize3", "combine1", "list3"))
        .with_rendered_child("combine1");

    // Populate default ports so connections work properly
    populate_default_ports(&mut library.root);

    library
}

#[test]
fn test_evaluate_primitives() {
    let library = create_primitives_library();

    // Create a library with just the rect node rendered
    let mut test_library = library.clone();
    test_library.root.rendered_child = Some("rect1".to_string());

    let (port, ctx) = test_port_and_context();
    let (paths, _output, _errors) = evaluate_network(&test_library, &port, &ctx);
    assert_eq!(paths.len(), 1, "rect1 should produce one path");

    // Test ellipse
    test_library.root.rendered_child = Some("ellipse1".to_string());
    let (port, ctx) = test_port_and_context();
    let (paths, _output, _errors) = evaluate_network(&test_library, &port, &ctx);
    assert_eq!(paths.len(), 1, "ellipse1 should produce one path");

    // Test polygon
    test_library.root.rendered_child = Some("polygon1".to_string());
    let (port, ctx) = test_port_and_context();
    let (paths, _output, _errors) = evaluate_network(&test_library, &port, &ctx);
    assert_eq!(paths.len(), 1, "polygon1 should produce one path");
}

#[test]
fn test_evaluate_primitives_full() {
    let library = create_primitives_library();

    // The rendered child is "combine1" which uses list.combine
    let (port, ctx) = test_port_and_context();
    let (paths, _output, _errors) = evaluate_network(&library, &port, &ctx);

    // Should have 3 shapes: rect, ellipse, polygon (each colorized)
    assert_eq!(paths.len(), 3, "combine1 should produce 3 colorized paths");

    // All paths should have fills (they go through colorize nodes)
    for path in &paths {
        assert!(path.fill.is_some(), "Each path should have a fill color");
    }
}

#[test]
fn test_evaluate_colorized_primitives() {
    let library = create_primitives_library();

    let mut test_library = library.clone();

    // Test colorized rect (colorize1 <- rect1)
    test_library.root.rendered_child = Some("colorize1".to_string());
    let (port, ctx) = test_port_and_context();
    let (paths, _output, _errors) = evaluate_network(&test_library, &port, &ctx);

    assert_eq!(paths.len(), 1, "colorize1 should produce one path");
    assert!(paths[0].fill.is_some(), "colorized path should have fill");
}

// ============================================================================
// Position port tests - verify shapes respect the "position" Point port
// ============================================================================

#[test]
fn test_primitives_shapes_at_different_positions() {
    // This test verifies that shapes are at DIFFERENT positions, not all at the origin.
    let library = create_primitives_library();

    // Evaluate rect1 alone
    let mut test_library = library.clone();
    test_library.root.rendered_child = Some("rect1".to_string());
    let (port, ctx) = test_port_and_context();
    let (rect_paths, _output, _errors) = evaluate_network(&test_library, &port, &ctx);
    assert_eq!(rect_paths.len(), 1, "rect1 should produce one path");
    let rect_bounds = rect_paths[0].bounds().unwrap();
    let rect_center_x = rect_bounds.x + rect_bounds.width / 2.0;

    // Evaluate ellipse1 alone
    test_library.root.rendered_child = Some("ellipse1".to_string());
    let (ellipse_paths, _output, _errors) = evaluate_network(&test_library, &port, &ctx);
    assert_eq!(ellipse_paths.len(), 1, "ellipse1 should produce one path");
    let ellipse_bounds = ellipse_paths[0].bounds().unwrap();
    let ellipse_center_x = ellipse_bounds.x + ellipse_bounds.width / 2.0;

    // Evaluate polygon1 alone
    test_library.root.rendered_child = Some("polygon1".to_string());
    let (polygon_paths, _output, _errors) = evaluate_network(&test_library, &port, &ctx);
    assert_eq!(polygon_paths.len(), 1, "polygon1 should produce one path");
    let polygon_bounds = polygon_paths[0].bounds().unwrap();
    let polygon_center_x = polygon_bounds.x + polygon_bounds.width / 2.0;

    // Verify they are at DIFFERENT x positions as defined
    // rect1 should be at x=-100, ellipse1 at x=10, polygon1 at x=100
    assert!(
        (rect_center_x - (-100.0)).abs() < 10.0,
        "rect1 center X should be near -100, got {}",
        rect_center_x
    );
    assert!(
        (ellipse_center_x - 10.0).abs() < 10.0,
        "ellipse1 center X should be near 10, got {}",
        ellipse_center_x
    );
    assert!(
        (polygon_center_x - 100.0).abs() < 10.0,
        "polygon1 center X should be near 100, got {}",
        polygon_center_x
    );

    // They should NOT all be at the same position (the bug we're catching)
    assert!(
        (rect_center_x - ellipse_center_x).abs() > 50.0,
        "rect1 and ellipse1 should be at different positions! rect={}, ellipse={}",
        rect_center_x,
        ellipse_center_x
    );
    assert!(
        (ellipse_center_x - polygon_center_x).abs() > 50.0,
        "ellipse1 and polygon1 should be at different positions! ellipse={}, polygon={}",
        ellipse_center_x,
        polygon_center_x
    );
}

#[test]
fn test_position_port_is_point_type() {
    // Verify that created nodes have "position" port with Point type
    let library = create_primitives_library();

    let rect = library.root.child("rect1").expect("rect1 should exist");
    let position_port = rect.input("position");
    assert!(
        position_port.is_some(),
        "rect1 should have a 'position' port"
    );
    if let Some(port) = position_port {
        match &port.value {
            nodebox_core::Value::Point(p) => {
                assert!(
                    (p.x - (-100.0)).abs() < 0.1,
                    "rect1 position.x should be -100, got {}",
                    p.x
                );
            }
            other => panic!("rect1 position should be Point type, got {:?}", other),
        }
    }

    let ellipse = library.root.child("ellipse1").expect("ellipse1 should exist");
    let position_port = ellipse.input("position");
    assert!(
        position_port.is_some(),
        "ellipse1 should have a 'position' port"
    );

    let polygon = library.root.child("polygon1").expect("polygon1 should exist");
    let position_port = polygon.input("position");
    assert!(
        position_port.is_some(),
        "polygon1 should have a 'position' port"
    );
}

// ============================================================================
// AppState tests
// ============================================================================

#[test]
fn test_app_state_new() {
    let state = AppState::new();

    // Initially has demo content
    assert!(!state.library.root.children.is_empty());
    assert!(!state.dirty);
    assert!(state.current_file.is_none());
}

#[test]
fn test_app_state_load_file_old_version_warns() {
    let mut state = AppState::new();

    // Old version files should load best-effort with notifications
    let path = examples_dir().join("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");
    if !path.exists() {
        println!("Skipping test - example file not found");
        return;
    }

    let result = state.load_file(&path);
    assert!(
        result.is_ok(),
        "load_file should succeed for old version files (best-effort)"
    );
    assert!(
        !state.notifications.is_empty(),
        "Should have notification warning about old format"
    );
}

#[test]
fn test_app_state_load_file_nonexistent() {
    let mut state = AppState::new();

    let path = examples_dir().join("nonexistent.ndbx");
    let result = state.load_file(&path);

    assert!(
        result.is_err(),
        "load_file should fail for nonexistent file"
    );
}

// ============================================================================
// Round-trip serialization test
// ============================================================================

#[test]
fn test_save_and_reload() {
    // Create a library
    let original = create_primitives_library();

    // Serialize to string
    let xml = nodebox_ndbx::serialize(&original);

    // Parse back
    let reloaded = nodebox_ndbx::parse(&xml).expect("Should be able to parse serialized content");

    // Verify key properties
    assert_eq!(reloaded.format_version, 22);
    assert_eq!(reloaded.root.name, "root");
    assert_eq!(
        reloaded.root.rendered_child,
        Some("combine1".to_string())
    );
    assert_eq!(reloaded.root.children.len(), 7);
    assert_eq!(reloaded.root.connections.len(), 6);

    // Verify a specific node
    let rect = reloaded.root.child("rect1").expect("Missing rect1");
    assert_eq!(rect.prototype, Some("corevector.rect".to_string()));
}

// ============================================================================
// Bulk loading test for library files (which have no version and default to 21)
// ============================================================================

#[test]
fn test_load_all_library_files() {
    let libs = libraries_dir();
    if !libs.exists() {
        println!("Libraries directory not found, skipping test");
        return;
    }

    let mut loaded = 0;
    let mut failed = Vec::new();

    // Walk all library directories
    if let Ok(entries) = std::fs::read_dir(&libs) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.is_dir() {
                // Look for .ndbx file in the directory
                if let Ok(files) = std::fs::read_dir(&path) {
                    for file in files.flatten() {
                        if file
                            .path()
                            .extension()
                            .map_or(false, |e| e == "ndbx")
                        {
                            match nodebox_ndbx::parse_file(file.path()) {
                                Ok(library) => {
                                    // Basic sanity check
                                    assert!(!library.root.name.is_empty());
                                    assert_eq!(
                                        library.format_version, 22,
                                        "Library should be upgraded to v22"
                                    );
                                    loaded += 1;
                                }
                                Err(e) => {
                                    failed.push((file.path(), e.to_string()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    println!("Loaded {} library files", loaded);

    if !failed.is_empty() {
        println!("Failed to load {} files:", failed.len());
        for (path, err) in &failed {
            println!("  {}: {}", path.display(), err);
        }
    }

    assert!(loaded > 0, "Should have loaded at least one library file");
}
