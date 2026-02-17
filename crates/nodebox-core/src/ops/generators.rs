//! Geometry generators - functions that create new shapes.

use std::f64::consts::PI;
use crate::geometry::{Point, Path, Color, Contour};

/// Create an ellipse at the given position.
///
/// # Arguments
/// * `position` - The center position of the ellipse
/// * `width` - The ellipse width
/// * `height` - The ellipse height
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::ellipse;
///
/// let path = ellipse(Point::ZERO, 100.0, 50.0);
/// assert!(!path.contours.is_empty());
/// ```
pub fn ellipse(position: Point, width: f64, height: f64) -> Path {
    Path::ellipse(position.x, position.y, width, height)
}

/// Create a rectangle at the given position.
///
/// # Arguments
/// * `position` - The center position of the rectangle
/// * `width` - The rectangle width
/// * `height` - The rectangle height
/// * `roundness` - The corner roundness as (rx, ry). Use (0, 0) for sharp corners.
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::rect;
///
/// let path = rect(Point::ZERO, 100.0, 100.0, Point::ZERO);
/// assert!(!path.contours.is_empty());
/// ```
pub fn rect(position: Point, width: f64, height: f64, roundness: Point) -> Path {
    if roundness == Point::ZERO {
        // Center the rect at position (same as rounded_rect)
        Path::rect(position.x - width / 2.0, position.y - height / 2.0, width, height)
    } else {
        rounded_rect(position, width, height, roundness.x, roundness.y)
    }
}

/// Create a rounded rectangle at the given position.
fn rounded_rect(position: Point, width: f64, height: f64, rx: f64, ry: f64) -> Path {
    let x = position.x - width / 2.0;
    let y = position.y - height / 2.0;

    // Clamp roundness to half of width/height
    let rx = rx.min(width / 2.0).max(0.0);
    let ry = ry.min(height / 2.0).max(0.0);

    // Kappa for bezier circle approximation
    const KAPPA: f64 = 0.5522847498;
    let kx = rx * KAPPA;
    let ky = ry * KAPPA;

    let mut contour = Contour::new();

    // Start at top-left, after the rounded corner
    contour.move_to(x + rx, y);

    // Top edge
    contour.line_to(x + width - rx, y);

    // Top-right corner
    contour.curve_to(
        x + width - rx + kx, y,
        x + width, y + ry - ky,
        x + width, y + ry,
    );

    // Right edge
    contour.line_to(x + width, y + height - ry);

    // Bottom-right corner
    contour.curve_to(
        x + width, y + height - ry + ky,
        x + width - rx + kx, y + height,
        x + width - rx, y + height,
    );

    // Bottom edge
    contour.line_to(x + rx, y + height);

    // Bottom-left corner
    contour.curve_to(
        x + rx - kx, y + height,
        x, y + height - ry + ky,
        x, y + height - ry,
    );

    // Left edge
    contour.line_to(x, y + ry);

    // Top-left corner
    contour.curve_to(
        x, y + ry - ky,
        x + rx - kx, y,
        x + rx, y,
    );

    contour.close();

    Path::from_contours(vec![contour])
}

/// Create a line between two points.
///
/// # Arguments
/// * `p1` - The starting point
/// * `p2` - The ending point
/// * `points` - The number of points along the line (minimum 2)
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::line;
///
/// let path = line(Point::ZERO, Point::new(100.0, 100.0), 2);
/// assert_eq!(path.contours[0].points.len(), 2);
/// ```
pub fn line(p1: Point, p2: Point, points: u32) -> Path {
    let points = points.max(2);
    let mut path = if points == 2 {
        Path::line(p1.x, p1.y, p2.x, p2.y)
    } else {
        // Create line with intermediate points
        let mut contour = Contour::new();
        for i in 0..points {
            let t = i as f64 / (points - 1) as f64;
            let x = p1.x + (p2.x - p1.x) * t;
            let y = p1.y + (p2.y - p1.y) * t;
            if i == 0 {
                contour.move_to(x, y);
            } else {
                contour.line_to(x, y);
            }
        }
        Path::from_contours(vec![contour])
    };

    // Lines typically have no fill, just stroke
    path.fill = None;
    path.stroke = Some(Color::BLACK);
    path.stroke_width = 1.0;
    path
}

/// Create a line from a starting point using an angle and distance.
///
/// # Arguments
/// * `point` - The starting point
/// * `angle` - The angle in degrees (0 = right, 90 = down)
/// * `distance` - The length of the line
/// * `points` - The number of points along the line (minimum 2)
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::line_angle;
///
/// let path = line_angle(Point::ZERO, 45.0, 100.0, 2);
/// assert!(!path.contours.is_empty());
/// ```
pub fn line_angle(point: Point, angle: f64, distance: f64, points: u32) -> Path {
    let p2 = coordinates(point, angle, distance);
    line(point, p2, points)
}

/// Calculate a point at a given angle and distance from another point.
///
/// # Arguments
/// * `point` - The origin point
/// * `angle` - The angle in degrees (0 = right, 90 = down)
/// * `distance` - The distance from the origin
pub fn coordinates(point: Point, angle: f64, distance: f64) -> Point {
    let rad = angle * PI / 180.0;
    Point::new(
        point.x + distance * rad.cos(),
        point.y + distance * rad.sin(),
    )
}

/// Create an arc at the given position.
///
/// # Arguments
/// * `position` - The center position of the arc
/// * `width` - The arc width
/// * `height` - The arc height
/// * `start_angle` - The starting angle in degrees (0 = right, 90 = down)
/// * `degrees` - The arc extent in degrees
/// * `arc_type` - The type of arc: "pie", "chord", or "open"
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::arc;
///
/// let path = arc(Point::ZERO, 100.0, 100.0, 0.0, 90.0, "pie");
/// assert!(!path.contours.is_empty());
/// ```
pub fn arc(position: Point, width: f64, height: f64, start_angle: f64, degrees: f64, arc_type: &str) -> Path {
    let rx = width / 2.0;
    let ry = height / 2.0;

    // Convert angles to radians
    let start_rad = start_angle * PI / 180.0;
    let _end_rad = start_rad + degrees * PI / 180.0;

    let mut contour = Contour::new();

    // Calculate start point
    let start_x = position.x + rx * start_rad.cos();
    let start_y = position.y + ry * start_rad.sin();

    // For pie type, start at center
    if arc_type == "pie" {
        contour.move_to(position.x, position.y);
        contour.line_to(start_x, start_y);
    } else {
        contour.move_to(start_x, start_y);
    }

    // Draw arc segments using bezier approximation
    // Split into segments for better approximation
    let segments = ((degrees.abs() / 45.0).ceil() as usize).max(1);
    let segment_angle = degrees / segments as f64;

    for i in 0..segments {
        let a1 = start_rad + (i as f64 * segment_angle) * PI / 180.0;
        let a2 = start_rad + ((i + 1) as f64 * segment_angle) * PI / 180.0;
        arc_bezier_segment(&mut contour, position, rx, ry, a1, a2);
    }

    // Close based on type
    match arc_type {
        "pie" => {
            contour.line_to(position.x, position.y);
            contour.close();
        }
        "chord" => {
            contour.close();
        }
        _ => {} // "open" - don't close
    }

    Path::from_contours(vec![contour])
}

/// Add a bezier segment approximating an arc.
fn arc_bezier_segment(contour: &mut Contour, center: Point, rx: f64, ry: f64, a1: f64, a2: f64) {
    let da = a2 - a1;
    let alpha = (4.0 / 3.0) * (da / 4.0).tan();

    let x1 = center.x + rx * a1.cos();
    let y1 = center.y + ry * a1.sin();
    let x2 = center.x + rx * a2.cos();
    let y2 = center.y + ry * a2.sin();

    let c1x = x1 - alpha * rx * a1.sin();
    let c1y = y1 + alpha * ry * a1.cos();
    let c2x = x2 + alpha * rx * a2.sin();
    let c2y = y2 - alpha * ry * a2.cos();

    contour.curve_to(c1x, c1y, c2x, c2y, x2, y2);
}

/// Create a multi-sided polygon.
///
/// # Arguments
/// * `position` - The center position of the polygon
/// * `radius` - The radius (size) of the polygon
/// * `sides` - The number of sides (minimum 3)
/// * `align` - If true, aligns the bottom edge to the X axis
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::polygon;
///
/// let path = polygon(Point::ZERO, 50.0, 6, false);
/// // Hexagon has 6 points (one per side)
/// assert_eq!(path.contours[0].points.len(), 6);
/// ```
pub fn polygon(position: Point, radius: f64, sides: u32, align: bool) -> Path {
    let sides = sides.max(3);
    let angle_step = 2.0 * PI / sides as f64;

    // Match Java pyvector.polygon:
    // align=false: start at angle 0 (right)
    // align=true: rotate so one edge is horizontal
    let start_angle = if align {
        // Java: x0, y0 = coordinates(x, y, r, 0)
        //       x1, y1 = coordinates(x, y, r, a)
        //       da = -angle(x1, y1, x0, y0)
        let sin_a = angle_step.sin();
        let cos_a = angle_step.cos();
        // -atan2(y0-y1, x0-x1) = -atan2(-r*sin(a), r*(1-cos(a)))
        -(-sin_a).atan2(1.0 - cos_a)
    } else {
        0.0
    };

    let mut contour = Contour::new();

    for i in 0..sides {
        let angle = start_angle + (i as f64) * angle_step;
        let x = position.x + radius * angle.cos();
        let y = position.y + radius * angle.sin();

        if i == 0 {
            contour.move_to(x, y);
        } else {
            contour.line_to(x, y);
        }
    }
    contour.close();

    Path::from_contours(vec![contour])
}

/// Create a star shape.
///
/// # Arguments
/// * `position` - The center position of the star
/// * `points` - The number of points (arms) on the star
/// * `outer` - The outer radius
/// * `inner` - The inner radius
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::star;
///
/// let path = star(Point::ZERO, 5, 50.0, 25.0);
/// // 5-pointed star has 10 points (2*5, matching Java)
/// assert_eq!(path.contours[0].points.len(), 10);
/// ```
pub fn star(position: Point, points: u32, outer: f64, inner: f64) -> Path {
    // Match Java pyvector.star:
    // - Uses outer/2 and inner/2 as radii (parameters represent diameters)
    // - Uses sin for x and cos for y (90° rotated from standard)
    // - Starts at bottom (y + outer/2)
    let points = points.max(2);
    let outer_r = outer / 2.0;
    let inner_r = inner / 2.0;

    let mut contour = Contour::new();

    // First point: moveto at (x, y + outer_r)
    contour.move_to(position.x, position.y + outer_r);

    // Remaining points
    for i in 1..(points * 2) {
        let angle = (i as f64) * PI / points as f64;
        let radius = if i % 2 != 0 { inner_r } else { outer_r };
        let x = position.x + radius * angle.sin();
        let y = position.y + radius * angle.cos();
        contour.line_to(x, y);
    }
    contour.close();

    Path::from_contours(vec![contour])
}

/// Create a grid of points.
///
/// # Arguments
/// * `columns` - The number of columns
/// * `rows` - The number of rows
/// * `width` - The total width of the grid
/// * `height` - The total height of the grid
/// * `position` - The center position of the grid
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::grid;
///
/// let points = grid(3, 3, 100.0, 100.0, Point::ZERO);
/// assert_eq!(points.len(), 9);
/// ```
pub fn grid(columns: u32, rows: u32, width: f64, height: f64, position: Point) -> Vec<Point> {
    let columns = columns.max(1);
    let rows = rows.max(1);

    let (column_size, left) = if columns > 1 {
        (width / (columns - 1) as f64, position.x - width / 2.0)
    } else {
        (0.0, position.x)
    };

    let (row_size, top) = if rows > 1 {
        (height / (rows - 1) as f64, position.y - height / 2.0)
    } else {
        (0.0, position.y)
    };

    let mut points = Vec::with_capacity((columns * rows) as usize);

    for row in 0..rows {
        for col in 0..columns {
            let x = left + col as f64 * column_size;
            let y = top + row as f64 * row_size;
            points.push(Point::new(x, y));
        }
    }

    points
}

/// Connect a list of points into a path.
///
/// # Arguments
/// * `points` - The list of points to connect
/// * `closed` - If true, close the path
///
/// # Example
/// ```
/// use nodebox_core::Point;
/// use nodebox_core::ops::connect;
///
/// let points = vec![Point::ZERO, Point::new(100.0, 0.0), Point::new(50.0, 100.0)];
/// let path = connect(&points, true);
/// assert!(path.contours[0].closed);
/// ```
pub fn connect(points: &[Point], closed: bool) -> Path {
    if points.is_empty() {
        return Path::new();
    }

    let mut contour = Contour::new();
    for (i, pt) in points.iter().enumerate() {
        if i == 0 {
            contour.move_to(pt.x, pt.y);
        } else {
            contour.line_to(pt.x, pt.y);
        }
    }

    if closed {
        contour.close();
    }

    let mut path = Path::from_contours(vec![contour]);
    path.fill = None;
    path.stroke = Some(Color::BLACK);
    path.stroke_width = 1.0;
    path
}

/// Create a point with the given x, y coordinates.
///
/// This is a simple wrapper for creating points, useful as a node function.
///
/// # Example
/// ```
/// use nodebox_core::ops::make_point;
///
/// let p = make_point(10.0, 20.0);
/// assert_eq!(p.x, 10.0);
/// assert_eq!(p.y, 20.0);
/// ```
pub fn make_point(x: f64, y: f64) -> Point {
    Point::new(x, y)
}

#[cfg(test)]
mod tests {
    use super::*;
    use approx::assert_relative_eq;
    use crate::PointType;

    #[test]
    fn test_ellipse() {
        let path = ellipse(Point::ZERO, 100.0, 50.0);
        assert_eq!(path.contours.len(), 1);
        assert!(path.contours[0].closed);

        let bounds = path.bounds().unwrap();
        assert_relative_eq!(bounds.width, 100.0, epsilon = 0.01);
        assert_relative_eq!(bounds.height, 50.0, epsilon = 0.01);
    }

    #[test]
    fn test_rect_simple() {
        let path = rect(Point::ZERO, 100.0, 50.0, Point::ZERO);
        assert_eq!(path.contours.len(), 1);
        assert!(path.contours[0].closed);

        let bounds = path.bounds().unwrap();
        assert_relative_eq!(bounds.width, 100.0, epsilon = 0.01);
        assert_relative_eq!(bounds.height, 50.0, epsilon = 0.01);
    }

    #[test]
    fn test_rect_centered_at_position() {
        // Regression test: rect should be centered at the given position
        let path = rect(Point::new(50.0, 30.0), 100.0, 60.0, Point::ZERO);
        let bounds = path.bounds().unwrap();

        // A 100x60 rect centered at (50, 30) should have bounds:
        // x: 50 - 50 = 0, y: 30 - 30 = 0
        // width: 100, height: 60
        assert_relative_eq!(bounds.x, 0.0, epsilon = 0.01);
        assert_relative_eq!(bounds.y, 0.0, epsilon = 0.01);
        assert_relative_eq!(bounds.width, 100.0, epsilon = 0.01);
        assert_relative_eq!(bounds.height, 60.0, epsilon = 0.01);
    }

    #[test]
    fn test_rect_rounded() {
        let path = rect(Point::ZERO, 100.0, 50.0, Point::new(10.0, 10.0));
        assert_eq!(path.contours.len(), 1);
        assert!(path.contours[0].closed);

        // Rounded rect should have curves
        let has_curves = path.contours[0].points.iter()
            .any(|p| matches!(p.point_type, PointType::CurveTo | PointType::CurveData));
        assert!(has_curves);
    }

    #[test]
    fn test_line() {
        let path = line(Point::ZERO, Point::new(100.0, 0.0), 2);
        assert_eq!(path.contours.len(), 1);
        assert!(!path.contours[0].closed);
        assert_eq!(path.contours[0].points.len(), 2);
    }

    #[test]
    fn test_line_with_points() {
        let path = line(Point::ZERO, Point::new(100.0, 0.0), 5);
        assert_eq!(path.contours[0].points.len(), 5);

        // Check intermediate points
        assert_relative_eq!(path.contours[0].points[2].point.x, 50.0, epsilon = 0.01);
    }

    #[test]
    fn test_line_angle() {
        let path = line_angle(Point::ZERO, 0.0, 100.0, 2);
        let end = &path.contours[0].points[1].point;
        assert_relative_eq!(end.x, 100.0, epsilon = 0.01);
        assert_relative_eq!(end.y, 0.0, epsilon = 0.01);
    }

    #[test]
    fn test_coordinates() {
        // 0 degrees = right
        let p = coordinates(Point::ZERO, 0.0, 100.0);
        assert_relative_eq!(p.x, 100.0, epsilon = 0.01);
        assert_relative_eq!(p.y, 0.0, epsilon = 0.01);

        // 90 degrees = down
        let p = coordinates(Point::ZERO, 90.0, 100.0);
        assert_relative_eq!(p.x, 0.0, epsilon = 0.01);
        assert_relative_eq!(p.y, 100.0, epsilon = 0.01);
    }

    #[test]
    fn test_polygon_triangle() {
        let path = polygon(Point::ZERO, 50.0, 3, false);
        // Triangle: 3 points (matching Java: no extra close point)
        assert_eq!(path.contours[0].points.len(), 3);
        assert!(path.contours[0].closed);
    }

    #[test]
    fn test_polygon_hexagon() {
        let path = polygon(Point::ZERO, 50.0, 6, false);
        // Hexagon: 6 points (matching Java: no extra close point)
        assert_eq!(path.contours[0].points.len(), 6);
    }

    #[test]
    fn test_star() {
        let path = star(Point::ZERO, 5, 50.0, 25.0);
        // 5-pointed star: 10 points (matching Java: no extra close point)
        assert_eq!(path.contours[0].points.len(), 10);
    }

    #[test]
    fn test_grid() {
        let points = grid(3, 3, 100.0, 100.0, Point::ZERO);
        assert_eq!(points.len(), 9);

        // Check corners
        assert_relative_eq!(points[0].x, -50.0, epsilon = 0.01);
        assert_relative_eq!(points[0].y, -50.0, epsilon = 0.01);
        assert_relative_eq!(points[8].x, 50.0, epsilon = 0.01);
        assert_relative_eq!(points[8].y, 50.0, epsilon = 0.01);
    }

    #[test]
    fn test_grid_single_column() {
        let points = grid(1, 3, 100.0, 100.0, Point::ZERO);
        assert_eq!(points.len(), 3);

        // All should be at center x
        for p in &points {
            assert_relative_eq!(p.x, 0.0, epsilon = 0.01);
        }
    }

    #[test]
    fn test_connect() {
        let pts = vec![
            Point::new(0.0, 0.0),
            Point::new(100.0, 0.0),
            Point::new(50.0, 100.0),
        ];
        let path = connect(&pts, true);
        assert!(path.contours[0].closed);
        assert_eq!(path.contours[0].points.len(), 3);
    }

    #[test]
    fn test_connect_open() {
        let pts = vec![Point::ZERO, Point::new(100.0, 0.0)];
        let path = connect(&pts, false);
        assert!(!path.contours[0].closed);
    }

    #[test]
    fn test_make_point() {
        let p = make_point(10.0, 20.0);
        assert_eq!(p.x, 10.0);
        assert_eq!(p.y, 20.0);
    }

    #[test]
    fn test_arc_pie() {
        let path = arc(Point::ZERO, 100.0, 100.0, 0.0, 90.0, "pie");
        assert!(path.contours[0].closed);
    }

    #[test]
    fn test_arc_open() {
        let path = arc(Point::ZERO, 100.0, 100.0, 0.0, 90.0, "open");
        assert!(!path.contours[0].closed);
    }
}
