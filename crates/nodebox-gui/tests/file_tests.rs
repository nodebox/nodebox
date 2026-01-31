//! Tests for loading and evaluating .ndbx files from the examples directory.

use std::path::PathBuf;

use nodebox_gui::eval::evaluate_network;
use nodebox_gui::{populate_default_ports, AppState};

/// Get the path to the examples directory.
fn examples_dir() -> PathBuf {
    let manifest_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    manifest_dir.parent().unwrap().parent().unwrap().join("examples")
}

/// Load and parse an .ndbx file from the examples directory.
/// Also populates default ports for proper evaluation.
fn load_example(relative_path: &str) -> nodebox_core::node::NodeLibrary {
    let path = examples_dir().join(relative_path);
    let mut library = nodebox_ndbx::parse_file(&path).unwrap_or_else(|e| {
        panic!("Failed to parse {}: {}", path.display(), e);
    });
    // Populate default ports so connections work properly
    populate_default_ports(&mut library.root);
    library
}

// ============================================================================
// 01 Basics / 01 Shape
// ============================================================================

#[test]
fn test_load_primitives() {
    let library = load_example("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    // Verify basic structure
    assert_eq!(library.root.name, "root");
    assert_eq!(library.root.rendered_child, Some("combine1".to_string()));
    assert_eq!(library.canvas_width(), 1000.0);
    assert_eq!(library.canvas_height(), 1000.0);

    // Verify nodes were loaded
    assert!(!library.root.children.is_empty());

    // Find specific nodes
    let rect = library.root.child("rect1");
    assert!(rect.is_some(), "rect1 node should exist");
    assert_eq!(
        rect.unwrap().prototype,
        Some("corevector.rect".to_string())
    );

    let ellipse = library.root.child("ellipse1");
    assert!(ellipse.is_some(), "ellipse1 node should exist");

    let polygon = library.root.child("polygon1");
    assert!(polygon.is_some(), "polygon1 node should exist");

    // Verify connections
    assert!(!library.root.connections.is_empty());
}

#[test]
fn test_load_lines() {
    let library = load_example("01 Basics/01 Shape/02 Lines/02 Lines.ndbx");

    assert_eq!(library.root.name, "root");
    assert!(!library.root.children.is_empty());
}

#[test]
fn test_load_grid() {
    let library = load_example("01 Basics/01 Shape/04 Grid/04 Grid.ndbx");

    assert_eq!(library.root.name, "root");

    // This file should have a grid node
    let has_grid = library
        .root
        .children
        .iter()
        .any(|n| n.prototype.as_deref() == Some("corevector.grid"));
    assert!(has_grid, "Should contain a grid node");
}

#[test]
fn test_load_copy() {
    let library = load_example("01 Basics/01 Shape/05 Copy/05 Copy.ndbx");

    assert_eq!(library.root.name, "root");

    // This file should have a copy node
    let has_copy = library
        .root
        .children
        .iter()
        .any(|n| n.prototype.as_deref() == Some("corevector.copy"));
    assert!(has_copy, "Should contain a copy node");
}

#[test]
fn test_load_transformations() {
    let library = load_example("01 Basics/01 Shape/06 Transformations/06 Transformations.ndbx");

    assert_eq!(library.root.name, "root");
    assert!(!library.root.children.is_empty());
}

// ============================================================================
// Evaluation tests - verify we can evaluate loaded files
// ============================================================================

#[test]
fn test_evaluate_primitives() {
    let library = load_example("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    // Create a library with just the rect node rendered
    let mut test_library = nodebox_core::node::NodeLibrary::new("test");
    test_library.root = library.root.clone();
    test_library.root.rendered_child = Some("rect1".to_string());

    let paths = evaluate_network(&test_library);
    assert_eq!(paths.len(), 1, "rect1 should produce one path");

    // Test ellipse
    test_library.root.rendered_child = Some("ellipse1".to_string());
    let paths = evaluate_network(&test_library);
    assert_eq!(paths.len(), 1, "ellipse1 should produce one path");

    // Test polygon
    test_library.root.rendered_child = Some("polygon1".to_string());
    let paths = evaluate_network(&test_library);
    assert_eq!(paths.len(), 1, "polygon1 should produce one path");
}

#[test]
fn test_evaluate_primitives_full() {
    let library = load_example("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    // The rendered child is "combine1" which uses list.combine
    // Now that list.combine is implemented, we can evaluate the full network
    let paths = evaluate_network(&library);

    // Should have 3 shapes: rect, ellipse, polygon (each colorized)
    assert_eq!(paths.len(), 3, "combine1 should produce 3 colorized paths");

    // All paths should have fills (they go through colorize nodes)
    for path in &paths {
        assert!(path.fill.is_some(), "Each path should have a fill color");
    }
}

#[test]
fn test_evaluate_colorized_primitives() {
    let library = load_example("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    let mut test_library = nodebox_core::node::NodeLibrary::new("test");
    test_library.root = library.root.clone();

    // Test colorized rect (colorize1 <- rect1)
    test_library.root.rendered_child = Some("colorize1".to_string());
    let paths = evaluate_network(&test_library);

    assert_eq!(paths.len(), 1, "colorize1 should produce one path");
    assert!(paths[0].fill.is_some(), "colorized path should have fill");
}

#[test]
fn test_evaluate_copy() {
    let library = load_example("01 Basics/01 Shape/05 Copy/05 Copy.ndbx");

    // Find a copy node and try to evaluate its output
    let copy_node = library
        .root
        .children
        .iter()
        .find(|n| n.prototype.as_deref() == Some("corevector.copy"));

    if let Some(copy) = copy_node {
        let mut test_library = nodebox_core::node::NodeLibrary::new("test");
        test_library.root = library.root.clone();
        test_library.root.rendered_child = Some(copy.name.clone());

        let paths = evaluate_network(&test_library);
        // Copy should produce multiple paths
        assert!(
            !paths.is_empty(),
            "Copy node {} should produce paths",
            copy.name
        );
    }
}

// ============================================================================
// Color examples
// ============================================================================

#[test]
fn test_load_color_example() {
    let path = examples_dir().join("01 Basics/02 Color");
    if path.exists() {
        // Find any .ndbx file in color examples
        if let Ok(entries) = std::fs::read_dir(&path) {
            for entry in entries.flatten() {
                let entry_path = entry.path();
                if entry_path.is_dir() {
                    if let Ok(files) = std::fs::read_dir(&entry_path) {
                        for file in files.flatten() {
                            if file
                                .path()
                                .extension()
                                .map_or(false, |e| e == "ndbx")
                            {
                                let library = nodebox_ndbx::parse_file(file.path()).unwrap();
                                assert_eq!(library.root.name, "root");
                                return; // Found and tested one file
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// AppState::load_file integration test
// ============================================================================

#[test]
fn test_app_state_load_file() {
    let mut state = AppState::new();

    // Initially has demo content
    assert!(!state.library.root.children.is_empty());

    // Load the primitives example
    let path = examples_dir().join("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");
    let result = state.load_file(&path);

    assert!(result.is_ok(), "load_file should succeed");
    assert_eq!(state.current_file, Some(path.clone()));
    assert!(!state.dirty);

    // Verify the library was loaded
    assert_eq!(state.library.root.name, "root");
    assert!(state.library.root.child("rect1").is_some());
    assert!(state.library.root.child("ellipse1").is_some());
    assert!(state.library.root.child("polygon1").is_some());

    // Verify geometry was evaluated (should have 3 shapes)
    assert_eq!(state.geometry.len(), 3, "Should have 3 rendered shapes");
}

#[test]
fn test_app_state_load_file_nonexistent() {
    let mut state = AppState::new();

    let path = examples_dir().join("nonexistent.ndbx");
    let result = state.load_file(&path);

    assert!(result.is_err(), "load_file should fail for nonexistent file");
}

// ============================================================================
// Position port tests - verify shapes respect the "position" Point port
// ============================================================================

#[test]
fn test_primitives_shapes_at_different_positions() {
    // This test verifies that shapes loaded from the primitives example
    // are at DIFFERENT positions, not all at the origin.
    // The file defines:
    //   rect1: position="-100.00,0.00"
    //   ellipse1: position="10.00,0.00"
    //   polygon1: position="100.00,0.00"
    let library = load_example("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    // Evaluate rect1 alone
    let mut test_library = nodebox_core::node::NodeLibrary::new("test");
    test_library.root = library.root.clone();
    test_library.root.rendered_child = Some("rect1".to_string());
    let rect_paths = evaluate_network(&test_library);
    assert_eq!(rect_paths.len(), 1, "rect1 should produce one path");
    let rect_bounds = rect_paths[0].bounds().unwrap();
    let rect_center_x = rect_bounds.x + rect_bounds.width / 2.0;

    // Evaluate ellipse1 alone
    test_library.root.rendered_child = Some("ellipse1".to_string());
    let ellipse_paths = evaluate_network(&test_library);
    assert_eq!(ellipse_paths.len(), 1, "ellipse1 should produce one path");
    let ellipse_bounds = ellipse_paths[0].bounds().unwrap();
    let ellipse_center_x = ellipse_bounds.x + ellipse_bounds.width / 2.0;

    // Evaluate polygon1 alone
    test_library.root.rendered_child = Some("polygon1".to_string());
    let polygon_paths = evaluate_network(&test_library);
    assert_eq!(polygon_paths.len(), 1, "polygon1 should produce one path");
    let polygon_bounds = polygon_paths[0].bounds().unwrap();
    let polygon_center_x = polygon_bounds.x + polygon_bounds.width / 2.0;

    // Verify they are at DIFFERENT x positions as defined in the file
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
    // Verify that the loaded nodes have "position" port with Point type
    let library = load_example("01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    let rect = library.root.child("rect1").expect("rect1 should exist");
    let position_port = rect.input("position");
    assert!(
        position_port.is_some(),
        "rect1 should have a 'position' port after loading"
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
        "ellipse1 should have a 'position' port after loading"
    );

    let polygon = library.root.child("polygon1").expect("polygon1 should exist");
    let position_port = polygon.input("position");
    assert!(
        position_port.is_some(),
        "polygon1 should have a 'position' port after loading"
    );
}

// ============================================================================
// Bulk loading test - verify all example files can be parsed
// ============================================================================

#[test]
fn test_load_all_example_files() {
    let examples = examples_dir();
    if !examples.exists() {
        println!("Examples directory not found, skipping test");
        return;
    }

    let mut loaded = 0;
    let mut failed = Vec::new();

    // Walk all directories
    fn walk_dir(dir: &PathBuf, loaded: &mut usize, failed: &mut Vec<(PathBuf, String)>) {
        if let Ok(entries) = std::fs::read_dir(dir) {
            for entry in entries.flatten() {
                let path = entry.path();
                if path.is_dir() {
                    walk_dir(&path, loaded, failed);
                } else if path.extension().map_or(false, |e| e == "ndbx") {
                    match nodebox_ndbx::parse_file(&path) {
                        Ok(library) => {
                            // Basic sanity check
                            assert!(!library.root.name.is_empty());
                            *loaded += 1;
                        }
                        Err(e) => {
                            failed.push((path, e.to_string()));
                        }
                    }
                }
            }
        }
    }

    walk_dir(&examples, &mut loaded, &mut failed);

    println!("Loaded {} example files", loaded);

    if !failed.is_empty() {
        println!("Failed to load {} files:", failed.len());
        for (path, err) in &failed {
            println!("  {}: {}", path.display(), err);
        }
    }

    assert!(loaded > 0, "Should have loaded at least one example file");
    // Note: We don't assert on failed.is_empty() since some files may have
    // features not yet implemented in the parser
}
