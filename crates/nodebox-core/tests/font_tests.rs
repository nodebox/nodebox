//! Font integration tests using the Inter font fixture.
//!
//! These tests use a bundled Inter.ttf font for platform-independent,
//! deterministic testing of text-to-path conversion.

use nodebox_core::geometry::font::{load_font_from_path, text_to_path_with_font};
use nodebox_core::geometry::Point;
use std::path::PathBuf;

fn get_inter_font_path() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("tests")
        .join("fixtures")
        .join("Inter.ttf")
}

#[test]
fn test_load_inter_font() {
    let font_path = get_inter_font_path();
    let result = load_font_from_path(&font_path);
    assert!(result.is_ok(), "Should be able to load Inter.ttf: {:?}", result.err());
}

#[test]
fn test_inter_single_letter() {
    let font_path = get_inter_font_path();
    let font = load_font_from_path(&font_path).expect("Failed to load Inter font");

    let path = text_to_path_with_font("A", &font, 72.0, Point::new(0.0, 100.0))
        .expect("Failed to convert 'A' to path");

    assert!(!path.is_empty(), "Path should have contours");

    let bounds = path.bounds().expect("Path should have bounds");
    assert!(bounds.width > 0.0, "Path should have width");
    assert!(bounds.height > 0.0, "Path should have height");
}

#[test]
fn test_inter_hello_world() {
    let font_path = get_inter_font_path();
    let font = load_font_from_path(&font_path).expect("Failed to load Inter font");

    let path = text_to_path_with_font("Hello World", &font, 48.0, Point::new(0.0, 100.0))
        .expect("Failed to convert text to path");

    assert!(!path.is_empty(), "Path should have contours");

    let bounds = path.bounds().expect("Path should have bounds");

    // "Hello World" should be wider than it is tall
    assert!(bounds.width > bounds.height, "Text should be wider than tall");

    // Check approximate dimensions for 48pt text
    // At 48pt, "Hello World" should be roughly 200-300 pixels wide
    assert!(bounds.width > 100.0, "Text should be at least 100 pixels wide");
    assert!(bounds.height > 20.0, "Text should be at least 20 pixels tall");
}

#[test]
fn test_inter_font_size_scaling() {
    let font_path = get_inter_font_path();
    let font = load_font_from_path(&font_path).expect("Failed to load Inter font");

    let path_small = text_to_path_with_font("X", &font, 24.0, Point::ZERO)
        .expect("Failed to convert at 24pt");
    let path_large = text_to_path_with_font("X", &font, 72.0, Point::ZERO)
        .expect("Failed to convert at 72pt");

    let bounds_small = path_small.bounds().expect("Should have bounds");
    let bounds_large = path_large.bounds().expect("Should have bounds");

    // The 72pt version should be approximately 3x larger than the 24pt version
    let scale_ratio = bounds_large.height / bounds_small.height;
    assert!(
        (scale_ratio - 3.0).abs() < 0.1,
        "72pt should be 3x taller than 24pt, got ratio: {}",
        scale_ratio
    );
}

#[test]
fn test_inter_position_offset() {
    let font_path = get_inter_font_path();
    let font = load_font_from_path(&font_path).expect("Failed to load Inter font");

    let path_origin = text_to_path_with_font("X", &font, 48.0, Point::ZERO)
        .expect("Failed at origin");
    let path_offset = text_to_path_with_font("X", &font, 48.0, Point::new(100.0, 200.0))
        .expect("Failed at offset");

    let bounds_origin = path_origin.bounds().expect("Should have bounds");
    let bounds_offset = path_offset.bounds().expect("Should have bounds");

    // The offset version should be displaced by (100, 200)
    let x_diff = bounds_offset.x - bounds_origin.x;
    let y_diff = bounds_offset.y - bounds_origin.y;

    assert!(
        (x_diff - 100.0).abs() < 1.0,
        "X should be offset by 100, got: {}",
        x_diff
    );
    assert!(
        (y_diff - 200.0).abs() < 1.0,
        "Y should be offset by 200, got: {}",
        y_diff
    );
}

#[test]
fn test_inter_empty_text() {
    let font_path = get_inter_font_path();
    let font = load_font_from_path(&font_path).expect("Failed to load Inter font");

    let path = text_to_path_with_font("", &font, 48.0, Point::ZERO)
        .expect("Should handle empty text");

    assert!(path.is_empty(), "Empty text should produce empty path");
}

#[test]
fn test_inter_numbers() {
    let font_path = get_inter_font_path();
    let font = load_font_from_path(&font_path).expect("Failed to load Inter font");

    let path = text_to_path_with_font("0123456789", &font, 36.0, Point::ZERO)
        .expect("Should convert numbers");

    assert!(!path.is_empty(), "Numbers should produce path");

    let bounds = path.bounds().expect("Should have bounds");
    assert!(bounds.width > 0.0, "Numbers should have width");
}

#[test]
fn test_inter_bezier_operations() {
    let font_path = get_inter_font_path();
    let font = load_font_from_path(&font_path).expect("Failed to load Inter font");

    let path = text_to_path_with_font("O", &font, 72.0, Point::new(0.0, 100.0))
        .expect("Should convert 'O'");

    // The letter O should have curves, so it should have measurable length
    let length = path.length();
    assert!(length > 0.0, "Path should have non-zero length");

    // Get some points along the path
    let points = path.make_points(10);
    assert_eq!(points.len(), 10, "Should generate 10 points");

    // Resample the path
    let resampled = path.resample_by_amount(20);
    assert!(!resampled.is_empty(), "Resampled path should not be empty");
}
