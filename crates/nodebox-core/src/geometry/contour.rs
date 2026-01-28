//! Contour - a sequence of points forming a path segment.
//!
//! This module provides bezier curve operations including:
//! - `point_at(t)` - Get a point at parameter t along the contour
//! - `length()` - Calculate the arc length of the contour
//! - `make_points(amount)` - Generate evenly spaced points
//! - `resample_by_amount()` / `resample_by_length()` - Create resampled contours

use super::{PathPoint, Point, PointType, Transform, Rect};

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

    // ========================================================================
    // Bezier Curve Operations
    // ========================================================================

    /// Returns the segments of this contour.
    ///
    /// A segment is either a line or a cubic bezier curve between two on-curve points.
    pub fn segments(&self) -> Vec<Segment> {
        if self.points.is_empty() {
            return Vec::new();
        }

        let mut segments = Vec::new();
        let mut i = 0;

        while i < self.points.len() {
            let start = self.points[i].point;

            // Look at the next point(s) to determine segment type
            if i + 1 < self.points.len() {
                let next = &self.points[i + 1];

                if next.point_type == PointType::CurveData {
                    // This is a cubic bezier: start, ctrl1, ctrl2, end
                    if i + 3 < self.points.len() {
                        let ctrl1 = self.points[i + 1].point;
                        let ctrl2 = self.points[i + 2].point;
                        let end = self.points[i + 3].point;
                        segments.push(Segment::Cubic { start, ctrl1, ctrl2, end });
                        i += 3;
                    } else {
                        // Malformed curve, skip to end
                        break;
                    }
                } else {
                    // Line segment
                    segments.push(Segment::Line { start, end: next.point });
                    i += 1;
                }
            } else {
                i += 1;
            }
        }

        // If closed, add segment from last point to first
        if self.closed && !self.points.is_empty() {
            let last_point = self.points.last().unwrap().point;
            let first_point = self.points[0].point;
            if last_point != first_point {
                segments.push(Segment::Line { start: last_point, end: first_point });
            }
        }

        segments
    }

    /// Returns the point at parameter `t` along the contour.
    ///
    /// `t` ranges from 0.0 (start) to 1.0 (end). For closed contours,
    /// t=1.0 returns to the start point.
    ///
    /// # Examples
    ///
    /// ```
    /// use nodebox_core::geometry::Contour;
    ///
    /// let mut c = Contour::new();
    /// c.move_to(0.0, 0.0);
    /// c.line_to(100.0, 0.0);
    ///
    /// let mid = c.point_at(0.5);
    /// assert!((mid.x - 50.0).abs() < 0.001);
    /// ```
    pub fn point_at(&self, t: f64) -> Point {
        let segments = self.segments();
        if segments.is_empty() {
            return if self.points.is_empty() {
                Point::ZERO
            } else {
                self.points[0].point
            };
        }

        // Clamp t to [0, 1]
        let t = t.clamp(0.0, 1.0);

        // Calculate segment lengths
        let segment_lengths: Vec<f64> = segments.iter().map(|s| s.length()).collect();
        let total_length: f64 = segment_lengths.iter().sum();

        if total_length == 0.0 {
            return self.points[0].point;
        }

        // Find which segment contains the point at t
        let target_length = t * total_length;
        let mut accumulated = 0.0;

        for (i, segment) in segments.iter().enumerate() {
            let seg_len = segment_lengths[i];
            if accumulated + seg_len >= target_length || i == segments.len() - 1 {
                // t falls within this segment
                let local_t = if seg_len > 0.0 {
                    (target_length - accumulated) / seg_len
                } else {
                    0.0
                };
                return segment.point_at(local_t);
            }
            accumulated += seg_len;
        }

        // Fallback to last point
        segments.last().map(|s| s.point_at(1.0)).unwrap_or(Point::ZERO)
    }

    /// Returns the total arc length of this contour.
    ///
    /// For bezier curves, this uses numerical approximation with 20 subdivisions
    /// per curve segment.
    pub fn length(&self) -> f64 {
        self.segments().iter().map(|s| s.length()).sum()
    }

    /// Generates `amount` evenly-spaced points along the contour.
    ///
    /// For closed contours, the last point will NOT duplicate the first.
    /// For open contours, both endpoints are included.
    ///
    /// # Examples
    ///
    /// ```
    /// use nodebox_core::geometry::Contour;
    ///
    /// let mut c = Contour::new();
    /// c.move_to(0.0, 0.0);
    /// c.line_to(100.0, 0.0);
    ///
    /// let points = c.make_points(5);
    /// assert_eq!(points.len(), 5);
    /// ```
    pub fn make_points(&self, amount: usize) -> Vec<Point> {
        if amount == 0 {
            return Vec::new();
        }
        if amount == 1 {
            return vec![self.point_at(0.0)];
        }

        let delta = if self.closed {
            1.0 / amount as f64
        } else {
            1.0 / (amount - 1) as f64
        };

        (0..amount)
            .map(|i| self.point_at(i as f64 * delta))
            .collect()
    }

    /// Creates a new contour by resampling this contour with `amount` points.
    ///
    /// The resulting contour consists only of line segments.
    pub fn resample_by_amount(&self, amount: usize) -> Contour {
        let points = self.make_points(amount);
        Contour::from_line_points(&points, self.closed)
    }

    /// Creates a new contour by resampling with segments of approximately `segment_length`.
    ///
    /// The actual segment length may vary slightly to ensure even distribution.
    pub fn resample_by_length(&self, segment_length: f64) -> Contour {
        if segment_length <= 0.0 {
            return Contour::new();
        }

        let total_length = self.length();
        if total_length == 0.0 {
            return Contour::new();
        }

        let amount = (total_length / segment_length).round().max(2.0) as usize;
        self.resample_by_amount(amount)
    }

    /// Creates a contour from simple line points (no curves).
    fn from_line_points(points: &[Point], closed: bool) -> Contour {
        if points.is_empty() {
            return Contour::new();
        }

        let path_points: Vec<PathPoint> = points
            .iter()
            .map(|p| PathPoint::line_to(p.x, p.y))
            .collect();

        Contour {
            points: path_points,
            closed,
        }
    }
}

/// A segment of a contour - either a line or a cubic bezier curve.
#[derive(Clone, Copy, Debug, PartialEq)]
pub enum Segment {
    /// A straight line segment.
    Line { start: Point, end: Point },
    /// A cubic bezier curve segment.
    Cubic {
        start: Point,
        ctrl1: Point,
        ctrl2: Point,
        end: Point,
    },
}

impl Segment {
    /// Number of subdivisions for bezier length approximation.
    const BEZIER_SUBDIVISIONS: usize = 20;

    /// Returns the point at parameter `t` (0.0 to 1.0) along this segment.
    pub fn point_at(&self, t: f64) -> Point {
        match self {
            Segment::Line { start, end } => start.lerp(*end, t),
            Segment::Cubic { start, ctrl1, ctrl2, end } => {
                Self::cubic_bezier_point(*start, *ctrl1, *ctrl2, *end, t)
            }
        }
    }

    /// Returns the approximate arc length of this segment.
    pub fn length(&self) -> f64 {
        match self {
            Segment::Line { start, end } => start.distance_to(*end),
            Segment::Cubic { start, ctrl1, ctrl2, end } => {
                Self::cubic_bezier_length(*start, *ctrl1, *ctrl2, *end)
            }
        }
    }

    /// Evaluates a cubic bezier curve using De Casteljau's algorithm.
    ///
    /// This is numerically stable and works for any t in [0, 1].
    fn cubic_bezier_point(p0: Point, p1: Point, p2: Point, p3: Point, t: f64) -> Point {
        // De Casteljau's algorithm
        // Level 1
        let q0 = p0.lerp(p1, t);
        let q1 = p1.lerp(p2, t);
        let q2 = p2.lerp(p3, t);

        // Level 2
        let r0 = q0.lerp(q1, t);
        let r1 = q1.lerp(q2, t);

        // Level 3 (final point)
        r0.lerp(r1, t)
    }

    /// Approximates the arc length of a cubic bezier using subdivision.
    fn cubic_bezier_length(p0: Point, p1: Point, p2: Point, p3: Point) -> f64 {
        let mut length = 0.0;
        let mut prev = p0;

        for i in 1..=Self::BEZIER_SUBDIVISIONS {
            let t = i as f64 / Self::BEZIER_SUBDIVISIONS as f64;
            let current = Self::cubic_bezier_point(p0, p1, p2, p3, t);
            length += prev.distance_to(current);
            prev = current;
        }

        length
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

    // ========================================================================
    // Bezier Operations Tests
    // ========================================================================

    #[test]
    fn test_segments_empty() {
        let c = Contour::new();
        assert!(c.segments().is_empty());
    }

    #[test]
    fn test_segments_single_point() {
        let mut c = Contour::new();
        c.move_to(10.0, 20.0);
        assert!(c.segments().is_empty());
    }

    #[test]
    fn test_segments_line() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let segments = c.segments();
        assert_eq!(segments.len(), 1);
        match &segments[0] {
            Segment::Line { start, end } => {
                assert_eq!(*start, Point::new(0.0, 0.0));
                assert_eq!(*end, Point::new(100.0, 0.0));
            }
            _ => panic!("Expected Line segment"),
        }
    }

    #[test]
    fn test_segments_multiple_lines() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(100.0, 100.0);

        let segments = c.segments();
        assert_eq!(segments.len(), 2);
    }

    #[test]
    fn test_segments_closed_triangle() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(50.0, 86.6);
        c.close();

        let segments = c.segments();
        // Should have 3 segments: 2 explicit + 1 closing
        assert_eq!(segments.len(), 3);
    }

    #[test]
    fn test_segments_curve() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.curve_to(10.0, 0.0, 20.0, 10.0, 20.0, 20.0);

        let segments = c.segments();
        assert_eq!(segments.len(), 1);
        match &segments[0] {
            Segment::Cubic { start, ctrl1, ctrl2, end } => {
                assert_eq!(*start, Point::new(0.0, 0.0));
                assert_eq!(*ctrl1, Point::new(10.0, 0.0));
                assert_eq!(*ctrl2, Point::new(20.0, 10.0));
                assert_eq!(*end, Point::new(20.0, 20.0));
            }
            _ => panic!("Expected Cubic segment"),
        }
    }

    #[test]
    fn test_point_at_empty() {
        let c = Contour::new();
        assert_eq!(c.point_at(0.5), Point::ZERO);
    }

    #[test]
    fn test_point_at_single_point() {
        let mut c = Contour::new();
        c.move_to(10.0, 20.0);
        assert_eq!(c.point_at(0.0), Point::new(10.0, 20.0));
        assert_eq!(c.point_at(0.5), Point::new(10.0, 20.0));
        assert_eq!(c.point_at(1.0), Point::new(10.0, 20.0));
    }

    #[test]
    fn test_point_at_line() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let p0 = c.point_at(0.0);
        let p_half = c.point_at(0.5);
        let p1 = c.point_at(1.0);

        assert!((p0.x - 0.0).abs() < 0.001);
        assert!((p_half.x - 50.0).abs() < 0.001);
        assert!((p1.x - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_point_at_line_y() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(0.0, 100.0);

        let p_quarter = c.point_at(0.25);
        assert!((p_quarter.x - 0.0).abs() < 0.001);
        assert!((p_quarter.y - 25.0).abs() < 0.001);
    }

    #[test]
    fn test_point_at_clamps() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        // t < 0 should clamp to start
        let p_neg = c.point_at(-0.5);
        assert!((p_neg.x - 0.0).abs() < 0.001);

        // t > 1 should clamp to end
        let p_over = c.point_at(1.5);
        assert!((p_over.x - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_point_at_bezier() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.curve_to(0.0, 50.0, 100.0, 50.0, 100.0, 0.0);

        // At t=0, should be at start
        let p0 = c.point_at(0.0);
        assert!((p0.x - 0.0).abs() < 0.001);
        assert!((p0.y - 0.0).abs() < 0.001);

        // At t=1, should be at end
        let p1 = c.point_at(1.0);
        assert!((p1.x - 100.0).abs() < 0.001);
        assert!((p1.y - 0.0).abs() < 0.001);

        // At t=0.5, should be at peak of the curve
        let p_half = c.point_at(0.5);
        assert!((p_half.x - 50.0).abs() < 1.0); // approximate
        assert!(p_half.y > 0.0); // Should be above the baseline
    }

    #[test]
    fn test_point_at_multiple_segments() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(100.0, 100.0);

        // Total length is 200, so t=0.5 should be at (100, 0)
        let p_half = c.point_at(0.5);
        assert!((p_half.x - 100.0).abs() < 0.001);
        assert!((p_half.y - 0.0).abs() < 0.001);

        // t=0.75 should be at (100, 50)
        let p_three_quarter = c.point_at(0.75);
        assert!((p_three_quarter.x - 100.0).abs() < 0.001);
        assert!((p_three_quarter.y - 50.0).abs() < 0.001);
    }

    #[test]
    fn test_length_empty() {
        let c = Contour::new();
        assert_eq!(c.length(), 0.0);
    }

    #[test]
    fn test_length_single_point() {
        let mut c = Contour::new();
        c.move_to(10.0, 20.0);
        assert_eq!(c.length(), 0.0);
    }

    #[test]
    fn test_length_horizontal_line() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        assert!((c.length() - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_length_vertical_line() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(0.0, 50.0);
        assert!((c.length() - 50.0).abs() < 0.001);
    }

    #[test]
    fn test_length_diagonal_line() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(3.0, 4.0);
        assert!((c.length() - 5.0).abs() < 0.001);
    }

    #[test]
    fn test_length_multiple_lines() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(100.0, 100.0);
        assert!((c.length() - 200.0).abs() < 0.001);
    }

    #[test]
    fn test_length_closed_square() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(100.0, 100.0);
        c.line_to(0.0, 100.0);
        c.close();

        assert!((c.length() - 400.0).abs() < 0.001);
    }

    #[test]
    fn test_length_bezier_approximate() {
        // A straight bezier should have length close to the line
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.curve_to(33.0, 0.0, 66.0, 0.0, 100.0, 0.0);

        // Should be approximately 100
        assert!((c.length() - 100.0).abs() < 1.0);
    }

    #[test]
    fn test_make_points_zero() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        assert!(c.make_points(0).is_empty());
    }

    #[test]
    fn test_make_points_one() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let points = c.make_points(1);
        assert_eq!(points.len(), 1);
        assert!((points[0].x - 0.0).abs() < 0.001);
    }

    #[test]
    fn test_make_points_two_open() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let points = c.make_points(2);
        assert_eq!(points.len(), 2);
        assert!((points[0].x - 0.0).abs() < 0.001);
        assert!((points[1].x - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_make_points_five_open() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let points = c.make_points(5);
        assert_eq!(points.len(), 5);
        assert!((points[0].x - 0.0).abs() < 0.001);
        assert!((points[1].x - 25.0).abs() < 0.001);
        assert!((points[2].x - 50.0).abs() < 0.001);
        assert!((points[3].x - 75.0).abs() < 0.001);
        assert!((points[4].x - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_make_points_closed() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(100.0, 100.0);
        c.line_to(0.0, 100.0);
        c.close();

        // For closed contour, points don't include duplicate endpoint
        let points = c.make_points(4);
        assert_eq!(points.len(), 4);

        // Each corner of the square
        assert!((points[0].x - 0.0).abs() < 0.001 && (points[0].y - 0.0).abs() < 0.001);
        assert!((points[1].x - 100.0).abs() < 0.001 && (points[1].y - 0.0).abs() < 0.001);
        assert!((points[2].x - 100.0).abs() < 0.001 && (points[2].y - 100.0).abs() < 0.001);
        assert!((points[3].x - 0.0).abs() < 0.001 && (points[3].y - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_resample_by_amount() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let resampled = c.resample_by_amount(5);
        assert_eq!(resampled.points.len(), 5);
        assert_eq!(resampled.closed, false);

        // All points should be LineTo type
        for p in &resampled.points {
            assert_eq!(p.point_type, PointType::LineTo);
        }
    }

    #[test]
    fn test_resample_by_amount_closed() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);
        c.line_to(100.0, 100.0);
        c.line_to(0.0, 100.0);
        c.close();

        let resampled = c.resample_by_amount(8);
        assert_eq!(resampled.points.len(), 8);
        assert!(resampled.closed);
    }

    #[test]
    fn test_resample_by_length() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        // 100 length, 25 segment length -> 4 segments -> 5 points (but we get 4 due to rounding)
        let resampled = c.resample_by_length(25.0);
        assert!(resampled.points.len() >= 4);
    }

    #[test]
    fn test_resample_by_length_zero() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let resampled = c.resample_by_length(0.0);
        assert!(resampled.is_empty());
    }

    #[test]
    fn test_resample_by_length_negative() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.line_to(100.0, 0.0);

        let resampled = c.resample_by_length(-10.0);
        assert!(resampled.is_empty());
    }

    #[test]
    fn test_resample_bezier() {
        let mut c = Contour::new();
        c.move_to(0.0, 0.0);
        c.curve_to(0.0, 50.0, 100.0, 50.0, 100.0, 0.0);

        // Resample the curve to 10 points
        let resampled = c.resample_by_amount(10);
        assert_eq!(resampled.points.len(), 10);

        // First and last points should match original endpoints
        assert!((resampled.points[0].x() - 0.0).abs() < 0.001);
        assert!((resampled.points[0].y() - 0.0).abs() < 0.001);
        assert!((resampled.points[9].x() - 100.0).abs() < 0.001);
        assert!((resampled.points[9].y() - 0.0).abs() < 0.001);
    }

    // ========================================================================
    // Segment Tests
    // ========================================================================

    #[test]
    fn test_segment_line_point_at() {
        let seg = Segment::Line {
            start: Point::new(0.0, 0.0),
            end: Point::new(100.0, 0.0),
        };

        let p0 = seg.point_at(0.0);
        let p_half = seg.point_at(0.5);
        let p1 = seg.point_at(1.0);

        assert_eq!(p0, Point::new(0.0, 0.0));
        assert_eq!(p_half, Point::new(50.0, 0.0));
        assert_eq!(p1, Point::new(100.0, 0.0));
    }

    #[test]
    fn test_segment_line_length() {
        let seg = Segment::Line {
            start: Point::new(0.0, 0.0),
            end: Point::new(3.0, 4.0),
        };
        assert!((seg.length() - 5.0).abs() < 0.001);
    }

    #[test]
    fn test_segment_cubic_endpoints() {
        let seg = Segment::Cubic {
            start: Point::new(0.0, 0.0),
            ctrl1: Point::new(0.0, 50.0),
            ctrl2: Point::new(100.0, 50.0),
            end: Point::new(100.0, 0.0),
        };

        let p0 = seg.point_at(0.0);
        let p1 = seg.point_at(1.0);

        assert_eq!(p0, Point::new(0.0, 0.0));
        assert_eq!(p1, Point::new(100.0, 0.0));
    }

    #[test]
    fn test_segment_cubic_midpoint() {
        // Symmetric curve - midpoint should be at x=50
        let seg = Segment::Cubic {
            start: Point::new(0.0, 0.0),
            ctrl1: Point::new(0.0, 50.0),
            ctrl2: Point::new(100.0, 50.0),
            end: Point::new(100.0, 0.0),
        };

        let p_half = seg.point_at(0.5);
        assert!((p_half.x - 50.0).abs() < 0.001);
        // Y should be 37.5 for t=0.5 with these control points
        // De Casteljau: 0.5 * (0.5 * (0.5*0 + 0.5*50) + 0.5 * (0.5*50 + 0.5*50)) +
        //              0.5 * (0.5 * (0.5*50 + 0.5*50) + 0.5 * (0.5*50 + 0.5*0))
        // = 0.5 * (0.5 * 25 + 0.5 * 50) + 0.5 * (0.5 * 50 + 0.5 * 25)
        // = 0.5 * 37.5 + 0.5 * 37.5 = 37.5
        assert!((p_half.y - 37.5).abs() < 0.001);
    }

    #[test]
    fn test_segment_straight_cubic_length() {
        // A "straight" cubic bezier
        let seg = Segment::Cubic {
            start: Point::new(0.0, 0.0),
            ctrl1: Point::new(33.333, 0.0),
            ctrl2: Point::new(66.666, 0.0),
            end: Point::new(100.0, 0.0),
        };

        // Length should be approximately 100
        assert!((seg.length() - 100.0).abs() < 1.0);
    }

    #[test]
    fn test_de_casteljau_algorithm() {
        // Verify the De Casteljau algorithm for a known curve
        // For a straight line from (0,0) to (1,1) expressed as a cubic bezier
        let seg = Segment::Cubic {
            start: Point::new(0.0, 0.0),
            ctrl1: Point::new(0.333, 0.333),
            ctrl2: Point::new(0.666, 0.666),
            end: Point::new(1.0, 1.0),
        };

        // At t=0.5, should be at (0.5, 0.5)
        let p = seg.point_at(0.5);
        assert!((p.x - 0.5).abs() < 0.01);
        assert!((p.y - 0.5).abs() < 0.01);
    }
}
