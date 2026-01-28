//! Contour - a sequence of points forming a path segment.

use super::{PathPoint, Point, Transform, Rect};

/// A contour is a sequence of connected points that can be open or closed.
///
/// Contours are the building blocks of paths. A closed contour forms a loop,
/// while an open contour is just a line.
///
/// # Examples
///
/// ```
/// use nodebox_core::geometry::{Contour, PathPoint};
///
/// let mut contour = Contour::new();
/// contour.move_to(0.0, 0.0);
/// contour.line_to(100.0, 0.0);
/// contour.line_to(100.0, 100.0);
/// contour.close();
/// ```
#[derive(Clone, Debug, Default, PartialEq)]
pub struct Contour {
    /// The points in this contour.
    pub points: Vec<PathPoint>,
    /// Whether this contour is closed (forms a loop).
    pub closed: bool,
}

impl Contour {
    /// Creates a new empty contour.
    pub fn new() -> Self {
        Contour {
            points: Vec::new(),
            closed: false,
        }
    }

    /// Creates a contour from a list of points.
    pub fn from_points(points: Vec<PathPoint>, closed: bool) -> Self {
        Contour { points, closed }
    }

    /// Returns true if this contour has no points.
    #[inline]
    pub fn is_empty(&self) -> bool {
        self.points.is_empty()
    }

    /// Returns the number of points in this contour.
    #[inline]
    pub fn len(&self) -> usize {
        self.points.len()
    }

    /// Adds a move-to point (starts a new subpath at this location).
    pub fn move_to(&mut self, x: f64, y: f64) {
        self.points.push(PathPoint::line_to(x, y));
    }

    /// Adds a line-to point.
    pub fn line_to(&mut self, x: f64, y: f64) {
        self.points.push(PathPoint::line_to(x, y));
    }

    /// Adds a cubic Bezier curve to (x3, y3) with control points (x1, y1) and (x2, y2).
    pub fn curve_to(&mut self, x1: f64, y1: f64, x2: f64, y2: f64, x3: f64, y3: f64) {
        self.points.push(PathPoint::curve_data(x1, y1));
        self.points.push(PathPoint::curve_data(x2, y2));
        self.points.push(PathPoint::curve_to(x3, y3));
    }

    /// Closes this contour.
    pub fn close(&mut self) {
        self.closed = true;
    }

    /// Returns the bounding box of this contour.
    ///
    /// Returns `None` if the contour is empty.
    pub fn bounds(&self) -> Option<Rect> {
        if self.points.is_empty() {
            return None;
        }

        let mut min_x = f64::INFINITY;
        let mut min_y = f64::INFINITY;
        let mut max_x = f64::NEG_INFINITY;
        let mut max_y = f64::NEG_INFINITY;

        for pp in &self.points {
            min_x = min_x.min(pp.point.x);
            min_y = min_y.min(pp.point.y);
            max_x = max_x.max(pp.point.x);
            max_y = max_y.max(pp.point.y);
        }

        Some(Rect::new(min_x, min_y, max_x - min_x, max_y - min_y))
    }

    /// Returns a new contour transformed by the given transform.
    pub fn transform(&self, t: &Transform) -> Contour {
        Contour {
            points: t.transform_path_points(&self.points),
            closed: self.closed,
        }
    }

    /// Returns the number of "on-curve" points (excluding control points).
    pub fn point_count(&self) -> usize {
        self.points.iter().filter(|p| p.point_type.is_on_curve()).count()
    }

    /// Returns just the on-curve points as simple Points.
    pub fn on_curve_points(&self) -> Vec<Point> {
        self.points
            .iter()
            .filter(|p| p.point_type.is_on_curve())
            .map(|p| p.point)
            .collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_contour_new() {
        let c = Contour::new();
        assert!(c.is_empty());
        assert!(!c.closed);
    }

    #[test]
    fn test_contour_line() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(100.0, 100.0);

        assert_eq!(c.len(), 3);
        assert!(!c.closed);
    }

    #[test]
    fn test_contour_closed() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(100.0, 100.0);
        c.close();

        assert!(c.closed);
    }

    #[test]
    fn test_contour_curve() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.curve_to(10.0, 0.0, 20.0, 10.0, 20.0, 20.0);

        assert_eq!(c.len(), 4); // 1 move + 2 control + 1 curve-to
        assert_eq!(c.point_count(), 2); // Only on-curve points
    }

    #[test]
    fn test_contour_bounds() {
        let mut c = Contour::new();
        c.move_to(10.0, 20.0);
        c.line_to(110.0, 20.0);
        c.line_to(110.0, 70.0);

        let bounds = c.bounds().unwrap();
        assert_eq!(bounds.x, 10.0);
        assert_eq!(bounds.y, 20.0);
        assert_eq!(bounds.width, 100.0);
        assert_eq!(bounds.height, 50.0);
    }

    #[test]
    fn test_contour_empty_bounds() {
        let c = Contour::new();
        assert!(c.bounds().is_none());
    }

    #[test]
    fn test_contour_transform() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let t = Transform::translate(10.0, 20.0);
        let transformed = c.transform(&t);

        assert_eq!(transformed.points[0].x(), 10.0);
        assert_eq!(transformed.points[0].y(), 20.0);
        assert_eq!(transformed.points[1].x(), 110.0);
        assert_eq!(transformed.points[1].y(), 20.0);
    }

    #[test]
    fn test_contour_on_curve_points() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.curve_to(10.0, 0.0, 20.0, 10.0, 20.0, 20.0);

        let on_curve = c.on_curve_points();
        assert_eq!(on_curve.len(), 2);
        assert_eq!(on_curve[0], Point::new(0.0, 0.0));
        assert_eq!(on_curve[1], Point::new(20.0, 20.0));
    }
}
