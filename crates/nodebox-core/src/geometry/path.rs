//! Path - a shape made of contours with fill and stroke styling.

use super::{Contour, Color, Transform, Rect, Point, PathPoint};

/// A path is a shape made of one or more contours with fill and stroke styling.
///
/// Paths are the primary visual element in NodeBox. They can have a fill color,
/// stroke color, and stroke width.
///
/// # Examples
///
/// ```
/// use nodebox_core::geometry::{Path, Color};
///
/// let mut path = Path::new();
/// path.move_to(0.0, 0.0);
/// path.line_to(100.0, 0.0);
/// path.line_to(100.0, 100.0);
/// path.close();
/// path.fill = Some(Color::rgb(1.0, 0.0, 0.0));
/// ```
#[derive(Clone, Debug, Default, PartialEq)]
pub struct Path {
    /// The contours that make up this path.
    pub contours: Vec<Contour>,
    /// The fill color (None for no fill).
    pub fill: Option<Color>,
    /// The stroke color (None for no stroke).
    pub stroke: Option<Color>,
    /// The stroke width.
    pub stroke_width: f64,
}

impl Path {
    /// Creates a new empty path with default black fill.
    pub fn new() -> Self {
        Path {
            contours: Vec::new(),
            fill: Some(Color::BLACK),
            stroke: None,
            stroke_width: 1.0,
        }
    }

    /// Creates a new path with no fill (stroke only).
    pub fn new_stroke() -> Self {
        Path {
            contours: Vec::new(),
            fill: None,
            stroke: Some(Color::BLACK),
            stroke_width: 1.0,
        }
    }

    /// Creates a path from existing contours.
    pub fn from_contours(contours: Vec<Contour>) -> Self {
        Path {
            contours,
            fill: Some(Color::BLACK),
            stroke: None,
            stroke_width: 1.0,
        }
    }

    /// Returns true if this path has no contours.
    #[inline]
    pub fn is_empty(&self) -> bool {
        self.contours.is_empty()
    }

    /// Ensures there is a current contour to add points to.
    fn ensure_contour(&mut self) {
        if self.contours.is_empty() {
            self.contours.push(Contour::new());
        }
    }

    /// Returns a mutable reference to the current (last) contour.
    fn current_contour(&mut self) -> &mut Contour {
        self.ensure_contour();
        self.contours.last_mut().unwrap()
    }

    /// Adds a move-to command, starting a new contour.
    pub fn move_to(&mut self, x: f64, y: f64) {
        // Start a new contour
        let mut contour = Contour::new();
        contour.move_to(x, y);
        self.contours.push(contour);
    }

    /// Adds a line-to command to the current contour.
    pub fn line_to(&mut self, x: f64, y: f64) {
        self.current_contour().line_to(x, y);
    }

    /// Adds a cubic Bezier curve to the current contour.
    pub fn curve_to(&mut self, x1: f64, y1: f64, x2: f64, y2: f64, x3: f64, y3: f64) {
        self.current_contour().curve_to(x1, y1, x2, y2, x3, y3);
    }

    /// Closes the current contour.
    pub fn close(&mut self) {
        if let Some(contour) = self.contours.last_mut() {
            contour.close();
        }
    }

    /// Adds a contour to this path.
    pub fn add_contour(&mut self, contour: Contour) {
        self.contours.push(contour);
    }

    /// Returns the bounding box of this path.
    ///
    /// Returns `None` if the path is empty.
    pub fn bounds(&self) -> Option<Rect> {
        let mut result: Option<Rect> = None;

        for contour in &self.contours {
            if let Some(bounds) = contour.bounds() {
                result = Some(match result {
                    Some(r) => r.union(&bounds),
                    None => bounds,
                });
            }
        }

        result
    }

    /// Returns a new path transformed by the given transform.
    pub fn transform(&self, t: &Transform) -> Path {
        Path {
            contours: self.contours.iter().map(|c| c.transform(t)).collect(),
            fill: self.fill,
            stroke: self.stroke,
            stroke_width: self.stroke_width,
        }
    }

    /// Returns the total number of points across all contours.
    pub fn point_count(&self) -> usize {
        self.contours.iter().map(|c| c.len()).sum()
    }

    /// Returns all points in the path as a flat list.
    pub fn all_points(&self) -> Vec<PathPoint> {
        self.contours.iter().flat_map(|c| c.points.iter().cloned()).collect()
    }

    /// Returns the centroid (center of bounding box) of this path.
    pub fn centroid(&self) -> Option<Point> {
        self.bounds().map(|b| b.center())
    }

    /// Creates a copy of this path with different styling.
    pub fn with_fill(mut self, fill: Option<Color>) -> Self {
        self.fill = fill;
        self
    }

    /// Creates a copy of this path with different stroke.
    pub fn with_stroke(mut self, stroke: Option<Color>) -> Self {
        self.stroke = stroke;
        self
    }

    /// Creates a copy of this path with different stroke width.
    pub fn with_stroke_width(mut self, width: f64) -> Self {
        self.stroke_width = width;
        self
    }

    /// Creates a rectangle path.
    pub fn rect(x: f64, y: f64, width: f64, height: f64) -> Path {
        let mut path = Path::new();
        path.move_to(x, y);
        path.line_to(x + width, y);
        path.line_to(x + width, y + height);
        path.line_to(x, y + height);
        path.close();
        path
    }

    /// Creates an ellipse path.
    pub fn ellipse(cx: f64, cy: f64, width: f64, height: f64) -> Path {
        // Approximate ellipse with 4 cubic Bezier curves
        // Magic number for circle approximation: 0.5522847498
        const KAPPA: f64 = 0.5522847498;

        let rx = width / 2.0;
        let ry = height / 2.0;
        let kx = rx * KAPPA;
        let ky = ry * KAPPA;

        let mut path = Path::new();
        path.move_to(cx + rx, cy);
        path.curve_to(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry);
        path.curve_to(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy);
        path.curve_to(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry);
        path.curve_to(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy);
        path.close();
        path
    }

    /// Creates a line path.
    pub fn line(x1: f64, y1: f64, x2: f64, y2: f64) -> Path {
        let mut path = Path::new_stroke();
        path.move_to(x1, y1);
        path.line_to(x2, y2);
        path
    }

    // ========================================================================
    // Bezier Curve Operations
    // ========================================================================

    /// Returns the total arc length of all contours.
    ///
    /// For bezier curves, this uses numerical approximation.
    pub fn length(&self) -> f64 {
        self.contours.iter().map(|c| c.length()).sum()
    }

    /// Returns the point at parameter `t` along the path.
    ///
    /// `t` ranges from 0.0 (start) to 1.0 (end). The parameter is distributed
    /// proportionally across all contours based on their lengths.
    ///
    /// # Examples
    ///
    /// ```
    /// use nodebox_core::geometry::Path;
    ///
    /// let path = Path::line(0.0, 0.0, 100.0, 0.0);
    /// let mid = path.point_at(0.5);
    /// assert!((mid.x - 50.0).abs() < 0.001);
    /// ```
    pub fn point_at(&self, t: f64) -> Point {
        if self.contours.is_empty() {
            return Point::ZERO;
        }

        // Clamp t to [0, 1]
        let t = t.clamp(0.0, 1.0);

        // Calculate contour lengths
        let contour_lengths: Vec<f64> = self.contours.iter().map(|c| c.length()).collect();
        let total_length: f64 = contour_lengths.iter().sum();

        if total_length == 0.0 {
            return self.contours[0].point_at(0.0);
        }

        // Find which contour contains the point at t
        let target_length = t * total_length;
        let mut accumulated = 0.0;

        for (i, contour) in self.contours.iter().enumerate() {
            let c_len = contour_lengths[i];
            if accumulated + c_len >= target_length || i == self.contours.len() - 1 {
                // t falls within this contour
                let local_t = if c_len > 0.0 {
                    (target_length - accumulated) / c_len
                } else {
                    0.0
                };
                return contour.point_at(local_t);
            }
            accumulated += c_len;
        }

        // Fallback
        self.contours.last().map(|c| c.point_at(1.0)).unwrap_or(Point::ZERO)
    }

    /// Generates `amount` evenly-spaced points along the path.
    ///
    /// Points are distributed across all contours proportionally.
    pub fn make_points(&self, amount: usize) -> Vec<Point> {
        if amount == 0 || self.contours.is_empty() {
            return Vec::new();
        }
        if amount == 1 {
            return vec![self.point_at(0.0)];
        }

        // For paths, treat all contours as one continuous curve
        let delta = 1.0 / (amount - 1) as f64;
        (0..amount)
            .map(|i| self.point_at(i as f64 * delta))
            .collect()
    }

    /// Creates a new path by resampling all contours with `amount` points each.
    ///
    /// Each contour is resampled independently.
    pub fn resample_by_amount(&self, amount: usize) -> Path {
        let contours: Vec<Contour> = self.contours
            .iter()
            .map(|c| c.resample_by_amount(amount))
            .collect();

        Path {
            contours,
            fill: self.fill,
            stroke: self.stroke,
            stroke_width: self.stroke_width,
        }
    }

    /// Creates a new path by resampling with segments of approximately `segment_length`.
    ///
    /// Each contour is resampled independently.
    pub fn resample_by_length(&self, segment_length: f64) -> Path {
        let contours: Vec<Contour> = self.contours
            .iter()
            .map(|c| c.resample_by_length(segment_length))
            .collect();

        Path {
            contours,
            fill: self.fill,
            stroke: self.stroke,
            stroke_width: self.stroke_width,
        }
    }

    /// Converts the path to a list of points (flattens all curves).
    ///
    /// This is equivalent to `make_points` but returns the on-curve points
    /// from all contours without resampling.
    pub fn to_points(&self) -> Vec<Point> {
        self.contours
            .iter()
            .flat_map(|c| c.on_curve_points())
            .collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_path_new() {
        let path = Path::new();
        assert!(path.is_empty());
        assert!(path.fill.is_some());
        assert!(path.stroke.is_none());
    }

    #[test]
    fn test_path_line() {
        let mut path = Path::new();
        path.move_to(0.0, 0.0);
        path.line_to(100.0, 0.0);
        path.line_to(100.0, 100.0);

        assert_eq!(path.contours.len(), 1);
        assert_eq!(path.point_count(), 3);
    }

    #[test]
    fn test_path_close() {
        let mut path = Path::new();
        path.move_to(0.0, 0.0);
        path.line_to(100.0, 0.0);
        path.line_to(100.0, 100.0);
        path.close();

        assert!(path.contours[0].closed);
    }

    #[test]
    fn test_path_bounds() {
        let mut path = Path::new();
        path.move_to(10.0, 20.0);
        path.line_to(110.0, 20.0);
        path.line_to(110.0, 70.0);
        path.line_to(10.0, 70.0);

        let bounds = path.bounds().unwrap();
        assert_eq!(bounds.x, 10.0);
        assert_eq!(bounds.y, 20.0);
        assert_eq!(bounds.width, 100.0);
        assert_eq!(bounds.height, 50.0);
    }

    #[test]
    fn test_path_transform() {
        let mut path = Path::new();
        path.move_to(0.0, 0.0);
        path.line_to(100.0, 0.0);

        let t = Transform::translate(10.0, 20.0);
        let transformed = path.transform(&t);

        let bounds = transformed.bounds().unwrap();
        assert_eq!(bounds.x, 10.0);
        assert_eq!(bounds.y, 20.0);
    }

    #[test]
    fn test_path_rect() {
        let path = Path::rect(10.0, 20.0, 100.0, 50.0);
        let bounds = path.bounds().unwrap();
        assert_eq!(bounds.x, 10.0);
        assert_eq!(bounds.y, 20.0);
        assert_eq!(bounds.width, 100.0);
        assert_eq!(bounds.height, 50.0);
        assert!(path.contours[0].closed);
    }

    #[test]
    fn test_path_ellipse() {
        let path = Path::ellipse(50.0, 50.0, 100.0, 60.0);
        let bounds = path.bounds().unwrap();
        // Bounds should be approximately centered at (50, 50) with size (100, 60)
        assert!((bounds.x - 0.0).abs() < 0.001);
        assert!((bounds.y - 20.0).abs() < 0.001);
        assert!((bounds.width - 100.0).abs() < 0.001);
        assert!((bounds.height - 60.0).abs() < 0.001);
    }

    #[test]
    fn test_path_line_factory() {
        let path = Path::line(10.0, 20.0, 110.0, 70.0);
        assert!(path.fill.is_none());
        assert!(path.stroke.is_some());
        assert_eq!(path.point_count(), 2);
    }

    #[test]
    fn test_path_with_fill() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0)
            .with_fill(Some(Color::rgb(1.0, 0.0, 0.0)));
        assert_eq!(path.fill, Some(Color::rgb(1.0, 0.0, 0.0)));
    }

    #[test]
    fn test_path_centroid() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let centroid = path.centroid().unwrap();
        assert_eq!(centroid, Point::new(50.0, 50.0));
    }

    // ========================================================================
    // Bezier Operations Tests
    // ========================================================================

    #[test]
    fn test_path_length_empty() {
        let path = Path::new();
        assert_eq!(path.length(), 0.0);
    }

    #[test]
    fn test_path_length_line() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);
        assert!((path.length() - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_path_length_rect() {
        // Rectangle perimeter (but not closed in this test)
        let mut path = Path::new();
        path.move_to(0.0, 0.0);
        path.line_to(100.0, 0.0);
        path.line_to(100.0, 100.0);
        path.line_to(0.0, 100.0);
        path.close();

        assert!((path.length() - 400.0).abs() < 0.001);
    }

    #[test]
    fn test_path_length_ellipse() {
        let path = Path::ellipse(0.0, 0.0, 100.0, 100.0);
        // Circumference of circle with diameter 100 is approximately 314.159
        assert!((path.length() - 314.159).abs() < 1.0);
    }

    #[test]
    fn test_path_point_at_empty() {
        let path = Path::new();
        assert_eq!(path.point_at(0.5), Point::ZERO);
    }

    #[test]
    fn test_path_point_at_line() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);

        let p0 = path.point_at(0.0);
        let p_half = path.point_at(0.5);
        let p1 = path.point_at(1.0);

        assert!((p0.x - 0.0).abs() < 0.001);
        assert!((p_half.x - 50.0).abs() < 0.001);
        assert!((p1.x - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_path_point_at_ellipse() {
        let path = Path::ellipse(0.0, 0.0, 100.0, 100.0);

        // At t=0, should be at (50, 0) - rightmost point
        let p0 = path.point_at(0.0);
        assert!((p0.x - 50.0).abs() < 0.001);
        assert!((p0.y - 0.0).abs() < 0.001);
    }

    #[test]
    fn test_path_make_points() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);

        let points = path.make_points(5);
        assert_eq!(points.len(), 5);
        assert!((points[0].x - 0.0).abs() < 0.001);
        assert!((points[2].x - 50.0).abs() < 0.001);
        assert!((points[4].x - 100.0).abs() < 0.001);
    }

    #[test]
    fn test_path_make_points_empty() {
        let path = Path::new();
        assert!(path.make_points(5).is_empty());
    }

    #[test]
    fn test_path_resample_by_amount() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);

        let resampled = path.resample_by_amount(10);
        assert_eq!(resampled.contours.len(), 1);
        assert_eq!(resampled.contours[0].points.len(), 10);
        assert!(resampled.fill.is_none()); // Preserves fill
        assert!(resampled.stroke.is_some()); // Preserves stroke
    }

    #[test]
    fn test_path_resample_by_length() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);

        let resampled = path.resample_by_length(20.0);
        // Should have approximately 5 segments (100/20)
        assert!(resampled.contours[0].points.len() >= 4);
    }

    #[test]
    fn test_path_to_points() {
        let mut path = Path::new();
        path.move_to(0.0, 0.0);
        path.line_to(100.0, 0.0);
        path.line_to(100.0, 100.0);

        let points = path.to_points();
        assert_eq!(points.len(), 3);
        assert_eq!(points[0], Point::new(0.0, 0.0));
        assert_eq!(points[1], Point::new(100.0, 0.0));
        assert_eq!(points[2], Point::new(100.0, 100.0));
    }

    #[test]
    fn test_path_to_points_with_curves() {
        let mut path = Path::new();
        path.move_to(0.0, 0.0);
        path.curve_to(10.0, 0.0, 20.0, 10.0, 20.0, 20.0);

        let points = path.to_points();
        // Only returns on-curve points (start and end of curve)
        assert_eq!(points.len(), 2);
        assert_eq!(points[0], Point::new(0.0, 0.0));
        assert_eq!(points[1], Point::new(20.0, 20.0));
    }
}
