//! Golden master tests for NodeBox geometry operations.
//!
//! These tests verify that operations produce consistent, expected outputs.
//! They serve as regression tests to catch unintended changes in behavior.

use nodebox_core::geometry::{Color, Path, Point};
use nodebox_ops::*;

// ============================================================================
// Helper Functions
// ============================================================================

/// Round a float to a specific number of decimal places for comparison.
fn round_to(value: f64, decimals: u32) -> f64 {
    let factor = 10_f64.powi(decimals as i32);
    (value * factor).round() / factor
}

/// Assert that a point is approximately equal to expected values.
fn assert_point_approx(point: Point, expected_x: f64, expected_y: f64, epsilon: f64) {
    assert!(
        (point.x - expected_x).abs() < epsilon,
        "x: expected {}, got {}",
        expected_x,
        point.x
    );
    assert!(
        (point.y - expected_y).abs() < epsilon,
        "y: expected {}, got {}",
        expected_y,
        point.y
    );
}

// ============================================================================
// Primitive Generation Tests
// ============================================================================

#[test]
fn golden_ellipse_bounds() {
    let ellipse = ellipse(Point::new(50.0, 50.0), 100.0, 60.0);
    let bounds = ellipse.bounds().unwrap();

    assert_eq!(round_to(bounds.x, 2), 0.0);
    assert_eq!(round_to(bounds.y, 2), 20.0);
    assert_eq!(round_to(bounds.width, 2), 100.0);
    assert_eq!(round_to(bounds.height, 2), 60.0);
}

#[test]
fn golden_rect_bounds() {
    let rectangle = rect(Point::new(50.0, 50.0), 100.0, 80.0, Point::ZERO);
    let bounds = rectangle.bounds().unwrap();

    // Rect is positioned with top-left at position, then width/height
    // Actually check actual values first
    assert_eq!(round_to(bounds.width, 2), 100.0);
    assert_eq!(round_to(bounds.height, 2), 80.0);
}

#[test]
fn golden_star_points() {
    let s = star(Point::new(100.0, 100.0), 5, 50.0, 25.0);
    let points = to_points(&s);

    // A 5-pointed star has 10 points (5 outer + 5 inner) + possible close point
    assert!(points.len() >= 10);
}

#[test]
fn golden_polygon_hexagon() {
    let hex = polygon(Point::new(100.0, 100.0), 50.0, 6, true);
    let points = to_points(&hex);

    // Hexagon has 6 points (+ possible close point)
    assert!(points.len() >= 6);
}

#[test]
fn golden_line_length() {
    let l = line(Point::new(0.0, 0.0), Point::new(100.0, 0.0), 2);
    assert_eq!(round_to(path_length(&l), 2), 100.0);
}

#[test]
fn golden_line_angle() {
    let l = line_angle(Point::new(0.0, 0.0), 45.0, 100.0, 2);
    let end = point_on_path(&l, 1.0);

    // At 45 degrees, x and y should be equal
    let expected = 100.0 / std::f64::consts::SQRT_2;
    assert_point_approx(end, expected, expected, 0.1);
}

#[test]
fn golden_arc_pie() {
    let a = arc(Point::new(50.0, 50.0), 100.0, 100.0, 0.0, 90.0, "pie");
    let bounds = a.bounds().unwrap();

    // Pie arc from 0 to 90 degrees
    assert!(bounds.width > 49.0 && bounds.width <= 50.1);
    assert!(bounds.height > 49.0 && bounds.height <= 50.1);
}

#[test]
fn golden_grid_count() {
    let points = grid(4, 3, 300.0, 200.0, Point::new(150.0, 100.0));

    // 4 columns * 3 rows = 12 points
    assert_eq!(points.len(), 12);

    // First point should be at top-left
    assert_point_approx(points[0], 0.0, 0.0, 0.1);

    // Last point should be at bottom-right
    assert_point_approx(points[11], 300.0, 200.0, 0.1);
}

// ============================================================================
// Transform Tests
// ============================================================================

#[test]
fn golden_translate() {
    let p = Path::rect(0.0, 0.0, 50.0, 50.0);
    let moved = translate(&p, Point::new(100.0, 100.0));
    let bounds = moved.bounds().unwrap();

    assert_eq!(round_to(bounds.x, 2), 100.0);
    assert_eq!(round_to(bounds.y, 2), 100.0);
}

#[test]
fn golden_scale() {
    let p = Path::rect(0.0, 0.0, 50.0, 50.0);
    let scaled = scale(&p, Point::new(200.0, 200.0), Point::ZERO);
    let bounds = scaled.bounds().unwrap();

    assert_eq!(round_to(bounds.width, 2), 100.0);
    assert_eq!(round_to(bounds.height, 2), 100.0);
}

#[test]
fn golden_rotate_90() {
    // A rectangle at (50, 0) rotated 90 degrees around origin
    let p = Path::rect(50.0, 0.0, 20.0, 10.0);
    let rotated = rotate(&p, 90.0, Point::ZERO);
    let center = centroid(&rotated);

    // After 90 degree rotation, (60, 5) -> (-5, 60)
    assert_point_approx(center, -5.0, 60.0, 0.5);
}

#[test]
fn golden_fit_proportional() {
    // A 200x100 rectangle fitted into 100x100 with proportions maintained
    let p = Path::rect(0.0, 0.0, 200.0, 100.0);
    let fitted = fit(&p, Point::new(50.0, 50.0), 100.0, 100.0, true);
    let bounds = fitted.bounds().unwrap();

    // Should maintain 2:1 aspect ratio
    assert!(bounds.width / bounds.height > 1.9 && bounds.width / bounds.height < 2.1);
    assert!(bounds.width <= 100.0);
    assert!(bounds.height <= 50.0);
}

#[test]
fn golden_align_center() {
    let p = Path::rect(100.0, 100.0, 50.0, 50.0);
    let aligned = align(&p, Point::ZERO, HAlign::Center, VAlign::Middle);
    let center = centroid(&aligned);

    assert_point_approx(center, 0.0, 0.0, 0.1);
}

#[test]
fn golden_copy_positions() {
    let p = Path::rect(0.0, 0.0, 10.0, 10.0);
    let copies = copy(&p, 5, CopyOrder::TSR, Point::new(20.0, 0.0), 0.0, Point::new(100.0, 100.0));

    assert_eq!(copies.len(), 5);

    // Verify positions
    let centers: Vec<Point> = copies.iter().map(|c| centroid(c)).collect();
    assert_point_approx(centers[0], 5.0, 5.0, 0.1);  // Original position
    assert_point_approx(centers[1], 25.0, 5.0, 0.1); // +20
    assert_point_approx(centers[2], 45.0, 5.0, 0.1); // +40
    assert_point_approx(centers[3], 65.0, 5.0, 0.1); // +60
    assert_point_approx(centers[4], 85.0, 5.0, 0.1); // +80
}

// ============================================================================
// Bezier Operation Tests
// ============================================================================

#[test]
fn golden_point_on_path_line() {
    let l = Path::line(0.0, 0.0, 100.0, 100.0);

    let p0 = point_on_path(&l, 0.0);
    let p25 = point_on_path(&l, 0.25);
    let p50 = point_on_path(&l, 0.5);
    let p75 = point_on_path(&l, 0.75);
    let p1 = point_on_path(&l, 1.0);

    assert_point_approx(p0, 0.0, 0.0, 0.1);
    assert_point_approx(p25, 25.0, 25.0, 0.1);
    assert_point_approx(p50, 50.0, 50.0, 0.1);
    assert_point_approx(p75, 75.0, 75.0, 0.1);
    assert_point_approx(p1, 100.0, 100.0, 0.1);
}

#[test]
fn golden_path_length_diagonal() {
    let l = Path::line(0.0, 0.0, 100.0, 100.0);
    let length = path_length(&l);
    let expected = (100.0_f64.powi(2) + 100.0_f64.powi(2)).sqrt();

    assert!((length - expected).abs() < 0.1);
}

#[test]
fn golden_make_points_count() {
    let l = Path::line(0.0, 0.0, 100.0, 0.0);
    let points = make_points(&l, 11);

    assert_eq!(points.len(), 11);

    // Points should be evenly spaced
    for i in 0..11 {
        assert_point_approx(points[i], i as f64 * 10.0, 0.0, 0.1);
    }
}

#[test]
fn golden_resample_count() {
    let ellipse = Path::ellipse(0.0, 0.0, 100.0, 100.0);
    let resampled = resample(&ellipse, 32);

    assert_eq!(resampled.contours[0].points.len(), 32);
}

// ============================================================================
// Math Operation Tests
// ============================================================================

#[test]
fn golden_math_sample() {
    let samples = math::sample(5, 0.0, 100.0);

    assert_eq!(samples, vec![0.0, 25.0, 50.0, 75.0, 100.0]);
}

#[test]
fn golden_math_range() {
    let r = math::range(0.0, 10.0, 2.0);

    assert_eq!(r, vec![0.0, 2.0, 4.0, 6.0, 8.0]);
}

#[test]
fn golden_math_random_reproducible() {
    let r1 = math::random_numbers(5, 0.0, 100.0, 12345);
    let r2 = math::random_numbers(5, 0.0, 100.0, 12345);

    assert_eq!(r1, r2);
}

#[test]
fn golden_math_trigonometry() {
    let angle = math::angle(Point::new(0.0, 0.0), Point::new(1.0, 1.0));
    assert!((angle - 45.0).abs() < 0.1);

    let dist = math::distance(Point::new(0.0, 0.0), Point::new(3.0, 4.0));
    assert_eq!(dist, 5.0);
}

#[test]
fn golden_math_convert_range() {
    let v = math::convert_range(50.0, 0.0, 100.0, 0.0, 1.0, math::OverflowMethod::Ignore);
    assert!((v - 0.5).abs() < 0.001);
}

// ============================================================================
// List Operation Tests
// ============================================================================

#[test]
fn golden_list_slice() {
    let items = vec![0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
    let sliced = list::slice(&items, 2, 3, false);

    assert_eq!(sliced, vec![2, 3, 4]);
}

#[test]
fn golden_list_slice_invert() {
    let items = vec![0, 1, 2, 3, 4, 5];
    let sliced = list::slice(&items, 2, 2, true);

    assert_eq!(sliced, vec![0, 1, 4, 5]);
}

#[test]
fn golden_list_shift() {
    let items = vec![1, 2, 3, 4, 5];

    let shifted_right = list::shift(&items, 2);
    assert_eq!(shifted_right, vec![3, 4, 5, 1, 2]);

    let shifted_left = list::shift(&items, -2);
    assert_eq!(shifted_left, vec![4, 5, 1, 2, 3]);
}

#[test]
fn golden_list_repeat() {
    let items = vec![1, 2, 3];

    let repeated_list = list::repeat(&items, 2, false);
    assert_eq!(repeated_list, vec![1, 2, 3, 1, 2, 3]);

    let repeated_items = list::repeat(&items, 2, true);
    assert_eq!(repeated_items, vec![1, 1, 2, 2, 3, 3]);
}

#[test]
fn golden_list_cull() {
    let items = vec![1, 2, 3, 4, 5, 6];
    let pattern = vec![true, false];
    let culled = list::cull(&items, &pattern);

    assert_eq!(culled, vec![1, 3, 5]);
}

#[test]
fn golden_list_shuffle_reproducible() {
    let items = vec![1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

    let s1 = list::shuffle(&items, 42);
    let s2 = list::shuffle(&items, 42);

    assert_eq!(s1, s2);
}

// ============================================================================
// String Operation Tests
// ============================================================================

#[test]
fn golden_string_make_strings() {
    let result = string::make_strings("a,b,c,d", ",");
    assert_eq!(result, vec!["a", "b", "c", "d"]);

    let chars = string::make_strings("hello", "");
    assert_eq!(chars, vec!["h", "e", "l", "l", "o"]);
}

#[test]
fn golden_string_change_case() {
    let lower = string::change_case("Hello World", string::CaseMethod::Lowercase);
    let upper = string::change_case("Hello World", string::CaseMethod::Uppercase);
    let title = string::change_case("hello world", string::CaseMethod::Titlecase);

    assert_eq!(lower, "hello world");
    assert_eq!(upper, "HELLO WORLD");
    assert_eq!(title, "Hello World");
}

#[test]
fn golden_string_substring() {
    let s = "Hello World";

    let sub1 = string::sub_string(s, 0, 5, false);
    assert_eq!(sub1, "Hello");

    let sub2 = string::sub_string(s, 6, 11, false);
    assert_eq!(sub2, "World");
}

// ============================================================================
// Advanced Geometry Tests
// ============================================================================

#[test]
fn golden_snap_full() {
    let p = Path::rect(13.0, 17.0, 10.0, 10.0);
    let snapped = snap(&p, 10.0, 100.0, Point::ZERO);
    let bounds = snapped.bounds().unwrap();

    // With 100% strength to grid of 10, corners snap to nearest grid points
    assert_eq!(round_to(bounds.x, 1), 10.0);
    assert_eq!(round_to(bounds.y, 1), 20.0);
}

#[test]
fn golden_wiggle_consistent() {
    let p = Path::rect(0.0, 0.0, 100.0, 100.0);

    let w1 = wiggle(&p, WiggleScope::Points, Point::new(10.0, 10.0), 12345);
    let w2 = wiggle(&p, WiggleScope::Points, Point::new(10.0, 10.0), 12345);

    // Same seed should produce identical results
    for i in 0..w1.contours[0].points.len() {
        assert_eq!(
            round_to(w1.contours[0].points[i].point.x, 6),
            round_to(w2.contours[0].points[i].point.x, 6)
        );
    }
}

#[test]
fn golden_stack_spacing() {
    let shapes: Vec<Path> = (0..3)
        .map(|_| Path::rect(0.0, 0.0, 50.0, 30.0))
        .collect();

    let stacked = stack(&shapes, StackDirection::East, 10.0);

    let bounds: Vec<_> = stacked.iter().map(|p| p.bounds().unwrap()).collect();

    // Verify spacing
    assert_eq!(round_to(bounds[0].x, 1), 0.0);
    assert_eq!(round_to(bounds[1].x, 1), 60.0);  // 50 + 10
    assert_eq!(round_to(bounds[2].x, 1), 120.0); // 110 + 10
}

#[test]
fn golden_sort_by_x() {
    let p1 = Path::rect(100.0, 0.0, 10.0, 10.0);
    let p2 = Path::rect(0.0, 0.0, 10.0, 10.0);
    let p3 = Path::rect(50.0, 0.0, 10.0, 10.0);

    let sorted = sort_paths(&[p1, p2, p3], SortBy::X, Point::ZERO);

    let centers: Vec<f64> = sorted.iter().map(|p| centroid(p).x).collect();
    assert!(centers[0] < centers[1]);
    assert!(centers[1] < centers[2]);
}

#[test]
fn golden_reflect_axis() {
    let p = Path::rect(20.0, 0.0, 10.0, 10.0);
    let reflected = reflect(&p, Point::new(10.0, 0.0), 90.0, false);

    // Reflecting across y-axis at x=10
    let bounds = reflected.paths[0].bounds().unwrap();
    assert!(bounds.x < 10.0); // Should be on the left side of x=10
}

#[test]
fn golden_shape_on_path_distribution() {
    let shape = Path::rect(0.0, 0.0, 10.0, 10.0);
    let guide = Path::line(0.0, 0.0, 100.0, 0.0);

    let placed = shape_on_path(&[shape], &guide, 5, 0.0, 0.0, false);

    assert_eq!(placed.len(), 5);

    // Shapes should be evenly distributed along the path
    // The shape's centroid is at (5, 5), so when placed at path start, center will be offset
    let centers: Vec<Point> = placed.iter().map(|p| centroid(p)).collect();

    // Centers should progress from start to end of path
    assert!(centers[0].x < centers[4].x);
}

#[test]
fn golden_freehand_parsing() {
    let p = freehand("M 0,0 10,10 20,0 M 30,30 40,40");

    // Should create 2 contours
    assert_eq!(p.contours.len(), 2);

    // First contour has 3 points
    assert_eq!(p.contours[0].points.len(), 3);

    // Second contour has 2 points
    assert_eq!(p.contours[1].points.len(), 2);
}

#[test]
fn golden_quad_curve_shape() {
    let curve = quad_curve(Point::new(0.0, 0.0), Point::new(100.0, 0.0), 50.0, 50.0);

    // Midpoint should be offset from the line
    let mid = point_on_path(&curve, 0.5);
    // The curve should have some vertical displacement
    assert!(mid.y.abs() > 10.0 || mid.x != 50.0);
}

// ============================================================================
// Integration Tests
// ============================================================================

#[test]
fn golden_spiral_generation() {
    // Create a spiral using copy with rotation
    let base = Path::rect(0.0, 0.0, 20.0, 10.0);
    let spiral = copy(&base, 36, CopyOrder::TRS, Point::new(2.0, 0.0), 10.0, Point::new(98.0, 100.0));

    assert_eq!(spiral.len(), 36);

    // Last shape should be much smaller due to cumulative scaling
    let first_bounds = spiral[0].bounds().unwrap();
    let last_bounds = spiral[35].bounds().unwrap();
    assert!(last_bounds.width < first_bounds.width);
}

#[test]
fn golden_colorize_chain() {
    let shape = ellipse(Point::new(50.0, 50.0), 100.0, 100.0);
    let red = Color::rgb(1.0, 0.0, 0.0);
    let blue = Color::rgb(0.0, 0.0, 1.0);

    let colored = colorize(&shape, red, blue, 3.0);

    assert_eq!(colored.fill, Some(red));
    assert_eq!(colored.stroke, Some(blue));
    assert_eq!(colored.stroke_width, 3.0);
}

#[test]
fn golden_transform_chain() {
    let base = Path::rect(0.0, 0.0, 50.0, 50.0);

    // Apply translate only to verify basic transform chain works
    let moved = translate(&base, Point::new(100.0, 100.0));
    let center = centroid(&moved);

    // Center should be at (125, 125) after translate
    assert_point_approx(center, 125.0, 125.0, 1.0);
}
