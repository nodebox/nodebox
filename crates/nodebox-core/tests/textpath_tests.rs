//! Tests for text_to_path_from_bytes (the WASM-compatible path).
//!
//! These test the same Inter.ttf fixture but via ttf-parser (no system-fonts feature needed),
//! which is the code path used in the Electron/WASM evaluator.

use nodebox_core::geometry::font;
use nodebox_core::geometry::Point;
use std::path::PathBuf;

fn inter_font_bytes() -> Vec<u8> {
    let path = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("tests")
        .join("fixtures")
        .join("Inter.ttf");
    std::fs::read(path).expect("Inter.ttf fixture should exist")
}

#[test]
fn test_from_bytes_single_letter() {
    let bytes = inter_font_bytes();
    let path = font::text_to_path_from_bytes("A", &bytes, 72.0, Point::ZERO)
        .expect("Should convert 'A' to path");

    assert!(!path.is_empty(), "Path should have contours");
    let bounds = path.bounds().expect("Path should have bounds");
    assert!(bounds.width > 0.0, "Path should have width");
    assert!(bounds.height > 0.0, "Path should have height");
}

#[test]
fn test_from_bytes_hello() {
    let bytes = inter_font_bytes();
    let path = font::text_to_path_from_bytes("hello", &bytes, 24.0, Point::ZERO)
        .expect("Should convert 'hello' to path");

    assert!(!path.is_empty());
    let bounds = path.bounds().expect("Path should have bounds");
    assert!(bounds.width > bounds.height, "Text should be wider than tall");
}

#[test]
fn test_from_bytes_position_offset() {
    let bytes = inter_font_bytes();
    let path_origin = font::text_to_path_from_bytes("X", &bytes, 48.0, Point::ZERO)
        .expect("at origin");
    let path_offset = font::text_to_path_from_bytes("X", &bytes, 48.0, Point::new(100.0, 200.0))
        .expect("at offset");

    let b0 = path_origin.bounds().unwrap();
    let b1 = path_offset.bounds().unwrap();
    assert!((b1.x - b0.x - 100.0).abs() < 1.0, "X offset should be ~100");
    assert!((b1.y - b0.y - 200.0).abs() < 1.0, "Y offset should be ~200");
}

#[test]
fn test_from_bytes_font_size_scaling() {
    let bytes = inter_font_bytes();
    let small = font::text_to_path_from_bytes("X", &bytes, 24.0, Point::ZERO).unwrap();
    let large = font::text_to_path_from_bytes("X", &bytes, 72.0, Point::ZERO).unwrap();

    let bs = small.bounds().unwrap();
    let bl = large.bounds().unwrap();
    let ratio = bl.height / bs.height;
    assert!(
        (ratio - 3.0).abs() < 0.1,
        "72pt should be 3x taller than 24pt, got ratio: {ratio}"
    );
}

#[test]
fn test_from_bytes_empty_text() {
    let bytes = inter_font_bytes();
    let path = font::text_to_path_from_bytes("", &bytes, 48.0, Point::ZERO).unwrap();
    assert!(path.is_empty(), "Empty text should produce empty path");
}

#[test]
fn test_from_bytes_space_only() {
    let bytes = inter_font_bytes();
    // Space has no outline but a valid glyph; should not crash
    let path = font::text_to_path_from_bytes(" ", &bytes, 48.0, Point::ZERO).unwrap();
    assert!(path.is_empty(), "Space should produce empty path (no outlines)");
}

#[test]
fn test_from_bytes_contours_are_closed() {
    let bytes = inter_font_bytes();
    let path = font::text_to_path_from_bytes("O", &bytes, 72.0, Point::ZERO).unwrap();

    for contour in &path.contours {
        assert!(contour.closed, "Font contours should be closed");
    }
}

#[test]
fn test_from_bytes_invalid_font_bytes() {
    let result = font::text_to_path_from_bytes("hello", &[0, 1, 2, 3], 24.0, Point::ZERO);
    assert!(result.is_err(), "Invalid font bytes should fail");
}
