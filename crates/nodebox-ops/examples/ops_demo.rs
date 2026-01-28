//! Demo of NodeBox geometry operations.

use nodebox_core::geometry::Point;
use nodebox_ops::*;

fn main() {
    println!("=== NodeBox Ops Demo ===\n");

    // 1. Generators
    println!("1. Shape Generators:");

    let star_shape = star(Point::ZERO, 5, 50.0, 25.0);
    println!("   Star (5 points): {} contours", star_shape.contours.len());

    let poly = polygon(Point::ZERO, 40.0, 6, true);
    println!("   Hexagon: perimeter = {:.2}", poly.length());

    let arc_shape = arc(Point::ZERO, 100.0, 60.0, 0.0, 270.0, "open");
    println!("   270° arc: length = {:.2}", arc_shape.length());

    // 2. Grid
    println!("\n2. Grid Generator:");
    let points = grid(4, 3, 150.0, 100.0, Point::ZERO);
    println!("   4x3 grid produces {} points:", points.len());
    for (i, p) in points.iter().enumerate() {
        println!("      [{:2}] ({:.0}, {:.0})", i, p.x, p.y);
    }

    // 3. Filters
    println!("\n3. Shape Filters:");

    let rect_shape = rect(Point::ZERO, 100.0, 60.0, Point::ZERO);
    println!("   Rectangle: {:?}", rect_shape.bounds().unwrap());

    let rotated = rotate(&rect_shape, 45.0, Point::ZERO);
    let rot_bounds = rotated.bounds().unwrap();
    println!("   Rotated 45°: ({:.1}, {:.1}) size {:.1}x{:.1}",
        rot_bounds.x, rot_bounds.y, rot_bounds.width, rot_bounds.height);

    let scaled = scale(&rect_shape, Point::new(200.0, 150.0), Point::ZERO);
    let scale_bounds = scaled.bounds().unwrap();
    println!("   Scaled 2x, 1.5x: size {:.0}x{:.0}",
        scale_bounds.width, scale_bounds.height);

    // 4. Copy operation
    println!("\n4. Copy with transforms:");
    let small_rect = rect(Point::ZERO, 20.0, 20.0, Point::ZERO);
    let copies = copy(
        &small_rect,
        5,
        CopyOrder::TSR,
        Point::new(30.0, 0.0),
        15.0,
        Point::new(100.0, 100.0)
    );
    println!("   5 copies with translate+rotate:");
    for (i, c) in copies.iter().enumerate() {
        let center = centroid(c);
        println!("      [{}] center at ({:.1}, {:.1})", i, center.x, center.y);
    }

    // 5. Bezier ops through filters
    println!("\n5. Bezier operations:");
    let circle = ellipse(Point::ZERO, 80.0, 80.0);
    println!("   Circle circumference: {:.2}", path_length(&circle));

    let pts = make_points(&circle, 12);
    println!("   12 points around circle:");
    for (i, p) in pts.iter().take(6).enumerate() {
        println!("      [{:2}] ({:6.1}, {:6.1})", i, p.x, p.y);
    }
    println!("      ... and 6 more");

    // 6. Connect points
    println!("\n6. Connect operation:");
    let random_points = vec![
        Point::new(0.0, 0.0),
        Point::new(30.0, 50.0),
        Point::new(70.0, 20.0),
        Point::new(100.0, 60.0),
    ];
    let connected = connect(&random_points, true);
    println!("   Connected 4 points (closed): length = {:.2}", connected.length());

    println!("\n=== Demo complete! ===");
}
