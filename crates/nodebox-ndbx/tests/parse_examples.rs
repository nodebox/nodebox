//! Integration tests for parsing actual NDBX files.

use nodebox_ndbx::{parse_file, NdbxError, MIN_SUPPORTED_VERSION};
use std::path::Path;

/// Test that parsing old format versions (< 21) returns an error.
#[test]
fn test_parse_old_version_returns_error() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../../examples/01 Basics/01 Shape/01 Primitives/01 Primitives.ndbx");

    if !path.exists() {
        println!("Skipping test - example file not found at: {:?}", path);
        return;
    }

    // This file has formatVersion="17" which is below our minimum supported version
    let result = parse_file(&path);
    assert!(result.is_err(), "Expected error for old format version");

    match result.unwrap_err() {
        NdbxError::UnsupportedVersion(v) => {
            assert!(v < MIN_SUPPORTED_VERSION, "Expected version < 21, got {}", v);
        }
        other => panic!("Expected UnsupportedVersion error, got: {:?}", other),
    }
}

/// Test parsing the corevector library.
/// Library files have no formatVersion attribute, which defaults to 21.
/// After loading, the library is upgraded to version 22.
#[test]
fn test_parse_corevector_library() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../../libraries/corevector/corevector.ndbx");

    if !path.exists() {
        println!("Skipping test - library file not found at: {:?}", path);
        return;
    }

    let library = parse_file(&path).expect("Failed to parse corevector.ndbx");

    // After parsing and upgrading, format version should be 22
    assert_eq!(library.format_version, 22, "Expected upgraded format version");

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

/// Test that parsing a very old demo file (version 0) returns an error.
#[test]
fn test_parse_old_demo_file_returns_error() {
    let path = Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../../src/test/files/demo.ndbx");

    if !path.exists() {
        println!("Skipping test - demo file not found at: {:?}", path);
        return;
    }

    // This file has an old/missing format version
    let result = parse_file(&path);
    assert!(result.is_err(), "Expected error for old format version");
}
