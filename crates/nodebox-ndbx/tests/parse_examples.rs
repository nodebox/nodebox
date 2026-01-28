//! Integration tests for parsing actual NDBX files.

use nodebox_ndbx::parse_file;
use std::path::Path;

/// Test parsing the Primitives example.
#[test]
fn test_parse_primitives_example() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../../examples/01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    if !path.exists() {
        println!("Skipping test - example file not found at: {:?}", path);
        return;
    }

    let library = parse_file(&path).expect("Failed to parse Primitives.ndbx");

    assert_eq!(library.format_version, 17);
    assert!(library.uuid.is_some());
    assert_eq!(library.canvas_width(), 1000.0);
    assert_eq!(library.canvas_height(), 1000.0);

    // Root should be a network with "combine1" as rendered child
    assert_eq!(library.root.name, "root");
    assert_eq!(library.root.rendered_child, Some("combine1".to_string()));

    // Should have several child nodes
    assert!(!library.root.children.is_empty());

    // Check some specific nodes exist
    let has_rect = library.root.children.iter().any(|n| n.name == "rect1");
    let has_ellipse = library.root.children.iter().any(|n| n.name == "ellipse1");
    let has_polygon = library.root.children.iter().any(|n| n.name == "polygon1");
    let has_combine = library.root.children.iter().any(|n| n.name == "combine1");

    assert!(has_rect, "Missing rect1 node");
    assert!(has_ellipse, "Missing ellipse1 node");
    assert!(has_polygon, "Missing polygon1 node");
    assert!(has_combine, "Missing combine1 node");

    // Check connections exist
    assert!(!library.root.connections.is_empty());
}

/// Test parsing the corevector library.
#[test]
fn test_parse_corevector_library() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../../libraries/corevector/corevector.ndbx");

    if !path.exists() {
        println!("Skipping test - library file not found at: {:?}", path);
        return;
    }

    let library = parse_file(&path).expect("Failed to parse corevector.ndbx");

    // Check that key nodes exist
    let child_names: Vec<&str> = library.root.children.iter().map(|n| n.name.as_str()).collect();

    assert!(child_names.contains(&"rect"), "Missing rect node");
    assert!(child_names.contains(&"ellipse"), "Missing ellipse node");
    assert!(child_names.contains(&"colorize"), "Missing colorize node");
    assert!(child_names.contains(&"grid"), "Missing grid node");

    // Check that rect has the expected ports
    let rect = library.root.children.iter().find(|n| n.name == "rect").unwrap();
    let rect_port_names: Vec<&str> = rect.inputs.iter().map(|p| p.name.as_str()).collect();

    assert!(rect_port_names.contains(&"position"), "rect missing position port");
    assert!(rect_port_names.contains(&"width"), "rect missing width port");
    assert!(rect_port_names.contains(&"height"), "rect missing height port");
}

/// Test parsing a simple demo file.
#[test]
fn test_parse_demo_file() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../../src/test/files/demo.ndbx");

    if !path.exists() {
        println!("Skipping test - demo file not found at: {:?}", path);
        return;
    }

    let library = parse_file(&path).expect("Failed to parse demo.ndbx");

    // Should parse without error and have a root node with a name
    assert!(!library.root.name.is_empty());
}
