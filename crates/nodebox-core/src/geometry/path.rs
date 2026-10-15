//! Path - a shape made of contours with fill and stroke styling.

use std::borrow::Cow;
use std::sync::{Arc, OnceLock};

use super::{Contour, Color, Transform, Rect, Point, PathPoint};

/// The points of a path, in the path's own coordinates.
///
/// A shape never changes once it is shared: any number of paths can point to the
/// same shape, each with its own placement and style, and anything computed from
/// the shape (its bounds, or a renderer's outline) can be cached against it.
/// To change the points, build a new shape.
#[derive(Clone, Debug, Default)]
pub struct Shape {
    contours: Vec<Contour>,
    bounds: OnceLock<Option<Rect>>,
}

impl Shape {
    /// Creates a shape from contours.
    pub fn new(contours: Vec<Contour>) -> Self {
        Shape { contours, bounds: OnceLock::new() }
    }

    /// The contours, in the shape's own coordinates.
    #[inline]
    pub fn contours(&self) -> &[Contour] {
        &self.contours
    }

    /// The bounding box of all points, control points included. Computed once.
    pub fn bounds(&self) -> Option<Rect> {
        *self.bounds.get_or_init(|| {
            self.contours.iter().filter_map(|c| c.bounds()).reduce(|a, b| a.union(&b))
        })
    }
}

impl PartialEq for Shape {
    fn eq(&self, other: &Self) -> bool {
        self.contours == other.contours
    }
}

/// A path is a shape made of one or more contours with fill and stroke styling.
///
/// Paths are the primary visual element in NodeBox. They can have a fill color,
/// stroke color, and stroke width.
///
/// A path is a shared, immutable [`Shape`] plus a placement (a transform) and a
/// style. Cloning a path, moving it, rotating it or copying it a thousand times
/// does not copy its points.
///
/// # Points are always placed
///
/// Every method that hands out points, starting with [`Path::contours`], returns
/// them with the placement applied: the points as they are drawn. Only
/// [`Path::shape`] and [`Path::placement`] expose the shape's own coordinates,
/// for renderers and caches that want to share work between copies.
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
#[derive(Clone, Debug, Default)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(from = "PathData", into = "PathData"))]
pub struct Path {
    shape: Arc<Shape>,
    placement: Transform,
    /// The fill color (None for no fill).
    pub fill: Option<Color>,
    /// The stroke color (None for no stroke).
    pub stroke: Option<Color>,
    /// The stroke width.
    pub stroke_width: f64,
}

/// How a path is written to and read from serde: placed contours and a style.
#[cfg(feature = "serde")]
#[derive(serde::Serialize, serde::Deserialize)]
struct PathData {
    contours: Vec<Contour>,
    fill: Option<Color>,
    stroke: Option<Color>,
    stroke_width: f64,
}

#[cfg(feature = "serde")]
impl From<PathData> for Path {
    fn from(d: PathData) -> Self {
        Path {
            shape: Arc::new(Shape::new(d.contours)),
            placement: Transform::IDENTITY,
            fill: d.fill,
            stroke: d.stroke,
            stroke_width: d.stroke_width,
        }
    }
}

#[cfg(feature = "serde")]
impl From<Path> for PathData {
    fn from(p: Path) -> Self {
        PathData { fill: p.fill, stroke: p.stroke, stroke_width: p.stroke_width, contours: p.into_contours() }
    }
}

/// Two paths are equal when they draw the same: same style, same placed points.
impl PartialEq for Path {
    fn eq(&self, other: &Self) -> bool {
        self.fill == other.fill
            && self.stroke == other.stroke
            && self.stroke_width == other.stroke_width
            && ((Arc::ptr_eq(&self.shape, &other.shape) && self.placement == other.placement)
                || self.contours() == other.contours())
    }
}

impl Path {
    /// Creates a new empty path with default black fill.
    pub fn new() -> Self {
        Path {
            fill: Some(Color::BLACK),
            stroke: None,
            stroke_width: 1.0,
            ..Default::default()
        }
    }

    /// Creates a new path with no fill (stroke only).
    pub fn new_stroke() -> Self {
        Path {
            fill: None,
            stroke: Some(Color::BLACK),
            stroke_width: 1.0,
            ..Default::default()
        }
    }

    /// Creates a path from existing contours.
    pub fn from_contours(contours: Vec<Contour>) -> Self {
        Path {
            shape: Arc::new(Shape::new(contours)),
            ..Path::new()
        }
    }

    /// Creates a path that draws `shape` placed by `placement`.
    pub fn from_shape(shape: Arc<Shape>, placement: Transform) -> Self {
        Path { shape, placement, ..Path::new() }
    }

    /// Returns a path with these (placed) contours and this path's style.
    pub fn with_contours(&self, contours: Vec<Contour>) -> Path {
        Path {
            shape: Arc::new(Shape::new(contours)),
            placement: Transform::IDENTITY,
            fill: self.fill,
            stroke: self.stroke,
            stroke_width: self.stroke_width,
        }
    }

    /// The shared shape, in its own coordinates. Use [`Path::contours`] unless
    /// you apply [`Path::placement`] yourself.
    #[inline]
    pub fn shape(&self) -> &Arc<Shape> {
        &self.shape
    }

    /// The transform that places the shape.
    #[inline]
    pub fn placement(&self) -> Transform {
        self.placement
    }

    /// The contours, placed. Borrowed from the shape when the placement is the
    /// identity, computed otherwise; bind the result once instead of calling
    /// this in a loop.
    pub fn contours(&self) -> Cow<'_, [Contour]> {
        if self.placement == Transform::IDENTITY {
            Cow::Borrowed(&self.shape.contours)
        } else {
            Cow::Owned(self.shape.contours.iter().map(|c| c.transform(&self.placement)).collect())
        }
    }

    /// The contours, placed, taken out of the path. Does not copy when the path
    /// is the only owner of its shape and has no placement.
    pub fn into_contours(self) -> Vec<Contour> {
        if self.placement == Transform::IDENTITY {
            match Arc::try_unwrap(self.shape) {
                Ok(shape) => shape.contours,
                Err(shape) => shape.contours.clone(),
            }
        } else {
            self.contours().into_owned()
        }
    }

    /// The contours, placed, for editing in place.
    ///
    /// Applies the placement to the points first, and copies the shape if any
    /// other path shares it, so no other path changes. Meant for building a
    /// path; to derive a path from another one, use [`Path::with_contours`].
    pub fn contours_mut(&mut self) -> &mut Vec<Contour> {
        if self.placement != Transform::IDENTITY {
            let placed = self.contours().into_owned();
            self.shape = Arc::new(Shape::new(placed));
            self.placement = Transform::IDENTITY;
        }
        let shape = Arc::make_mut(&mut self.shape);
        shape.bounds = OnceLock::new();
        &mut shape.contours
    }

    /// Returns true if this path has no contours.
    #[inline]
    pub fn is_empty(&self) -> bool {
        self.shape.contours.is_empty()
    }

    /// Returns the number of contours.
    #[inline]
    pub fn contour_count(&self) -> usize {
        self.shape.contours.len()
    }

    /// Returns a mutable reference to the current (last) contour.
    fn current_contour(&mut self) -> &mut Contour {
        let contours = self.contours_mut();
        if contours.is_empty() {
            contours.push(Contour::new());
        }
        contours.last_mut().unwrap()
    }

    /// Adds a move-to command, starting a new contour.
    pub fn move_to(&mut self, x: f64, y: f64) {
        // Start a new contour
        let mut contour = Contour::new();
        contour.move_to(x, y);
        self.contours_mut().push(contour);
    }

    /// Adds a line-to command to the current contour.
    pub fn line_to(&mut self, x: f64, y: f64) {
        self.current_contour().line_to(x, y);
    }

    /// Adds a cubic Bezier curve to the current contour.
    pub fn curve_to(&mut self, x1: f64, y1: f64, x2: f64, y2: f64, x3: f64, y3: f64) {
        self.current_contour().curve_to(x1, y1, x2, y2, x3, y3);
    }

    /// Adds a quadratic Bezier curve to the current contour.
    pub fn quad_to(&mut self, cx: f64, cy: f64, x: f64, y: f64) {
        self.current_contour().quad_to(cx, cy, x, y);
    }

    /// Closes the current contour.
    pub fn close(&mut self) {
        if let Some(contour) = self.contours_mut().last_mut() {
            contour.close();
        }
    }

    /// Adds a contour to this path.
    pub fn add_contour(&mut self, contour: Contour) {
        self.contours_mut().push(contour);
    }

    /// Returns the bounding box of this path, as placed.
    ///
    /// Returns `None` if the path is empty.
    pub fn bounds(&self) -> Option<Rect> {
        let shape_bounds = self.shape.bounds()?;
        let [_, m10, m01, ..] = self.placement.as_array();
        if m10 == 0.0 && m01 == 0.0 {
            // Translation and scale map the bounding box onto the bounding box.
            return Some(self.placement.transform_rect(shape_bounds));
        }
        let mut min = Point::new(f64::INFINITY, f64::INFINITY);
        let mut max = Point::new(f64::NEG_INFINITY, f64::NEG_INFINITY);
        for pp in self.shape.contours.iter().flat_map(|c| &c.points) {
            let p = self.placement.transform_point(pp.point);
            min = Point::new(min.x.min(p.x), min.y.min(p.y));
            max = Point::new(max.x.max(p.x), max.y.max(p.y));
        }
        Some(Rect::new(min.x, min.y, max.x - min.x, max.y - min.y))
    }

    /// Returns this path with `t` applied after its placement. The shape is
    /// shared, not copied. The stroke width does not change.
    pub fn transform(&self, t: &Transform) -> Path {
        Path {
            shape: Arc::clone(&self.shape),
            placement: self.placement.then(t),
            fill: self.fill,
            stroke: self.stroke,
            stroke_width: self.stroke_width,
        }
    }

    /// Returns the total number of points across all contours.
    pub fn point_count(&self) -> usize {
        self.shape.contours.iter().map(|c| c.len()).sum()
    }

    /// Returns all points in the path as a flat list.
    pub fn all_points(&self) -> Vec<PathPoint> {
        self.contours().iter().flat_map(|c| c.points.iter().cloned()).collect()
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
        self.contours().iter().map(|c| c.length()).sum()
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
        let contours = self.contours();
        if contours.is_empty() {
            return Point::ZERO;
        }

        // Clamp t to [0, 1]
        let t = t.clamp(0.0, 1.0);

        // Calculate contour lengths
        let contour_lengths: Vec<f64> = contours.iter().map(|c| c.length()).collect();
        let total_length: f64 = contour_lengths.iter().sum();

        if total_length == 0.0 {
            return contours[0].point_at(0.0);
        }

        // Find which contour contains the point at t
        let target_length = t * total_length;
        let mut accumulated = 0.0;

        for (i, contour) in contours.iter().enumerate() {
            let c_len = contour_lengths[i];
            if accumulated + c_len >= target_length || i == contours.len() - 1 {
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
        contours.last().map(|c| c.point_at(1.0)).unwrap_or(Point::ZERO)
    }

    /// Generates `amount` evenly-spaced points along the path.
    ///
    /// Points are distributed across all contours proportionally.
    pub fn make_points(&self, amount: usize) -> Vec<Point> {
        if amount == 0 || self.is_empty() {
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
        self.with_contours(self.contours().iter().map(|c| c.resample_by_amount(amount)).collect())
    }

    /// Creates a new path by resampling with segments of approximately `segment_length`.
    ///
    /// Each contour is resampled independently.
    pub fn resample_by_length(&self, segment_length: f64) -> Path {
        self.with_contours(self.contours().iter().map(|c| c.resample_by_length(segment_length)).collect())
    }

    /// Converts the path to a list of points (flattens all curves).
    ///
    /// This is equivalent to `make_points` but returns the on-curve points
    /// from all contours without resampling.
    pub fn to_points(&self) -> Vec<Point> {
        self.contours()
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

        assert_eq!(path.contour_count(), 1);
        assert_eq!(path.point_count(), 3);
    }

    #[test]
    fn test_path_close() {
        let mut path = Path::new();
        path.move_to(0.0, 0.0);
        path.line_to(100.0, 0.0);
        path.line_to(100.0, 100.0);
        path.close();

        assert!(path.contours()[0].closed);
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
        assert!(path.contours()[0].closed);
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
        assert_eq!(resampled.contour_count(), 1);
        assert_eq!(resampled.contours()[0].points.len(), 10);
        assert!(resampled.fill.is_none()); // Preserves fill
        assert!(resampled.stroke.is_some()); // Preserves stroke
    }

    #[test]
    fn test_path_resample_by_length() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);

        let resampled = path.resample_by_length(20.0);
        // Should have approximately 5 segments (100/20)
        assert!(resampled.contours()[0].points.len() >= 4);
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

    // ========================================================================
    // Shared shapes
    // ========================================================================

    #[test]
    fn test_transform_shares_shape() {
        let path = Path::rect(0.0, 0.0, 10.0, 10.0);
        let moved = path.transform(&Transform::translate(5.0, 0.0));
        assert!(Arc::ptr_eq(path.shape(), moved.shape()));
        assert_eq!(moved.contours()[0].points[0].point, Point::new(5.0, 0.0));
        // The original is untouched.
        assert_eq!(path.contours()[0].points[0].point, Point::new(0.0, 0.0));
    }

    #[test]
    fn test_editing_a_copy_leaves_the_original() {
        let path = Path::rect(0.0, 0.0, 10.0, 10.0);
        let mut copy = path.clone();
        copy.line_to(20.0, 20.0);
        assert!(!Arc::ptr_eq(path.shape(), copy.shape()));
        assert_eq!(path.point_count(), 4);
        assert_eq!(copy.point_count(), 5);
    }

    #[test]
    fn test_editing_a_placed_path_edits_placed_points() {
        let mut path = Path::rect(0.0, 0.0, 10.0, 10.0).transform(&Transform::translate(100.0, 0.0));
        path.line_to(0.0, 0.0);
        assert_eq!(path.placement(), Transform::IDENTITY);
        let points = path.all_points();
        assert_eq!(points[0].point, Point::new(100.0, 0.0));
        assert_eq!(points[4].point, Point::new(0.0, 0.0));
    }

    #[test]
    fn test_placed_equals_baked() {
        let path = Path::ellipse(0.0, 0.0, 10.0, 20.0);
        let t = Transform::rotate(30.0).then(&Transform::translate(3.0, 4.0));
        let placed = path.transform(&t);
        let baked = path.with_contours(path.contours().iter().map(|c| c.transform(&t)).collect());
        assert!(!Arc::ptr_eq(placed.shape(), baked.shape()));
        assert_eq!(placed, baked);
    }

    #[test]
    fn test_bounds_follow_placement() {
        let path = Path::rect(0.0, 0.0, 10.0, 20.0);
        let t = Transform::scale_xy(2.0, -1.0);
        assert_eq!(path.transform(&t).bounds(), Some(Rect::new(0.0, -20.0, 20.0, 20.0)));

        let rotated = path.transform(&Transform::rotate(90.0)).bounds().unwrap();
        assert!((rotated.x + 20.0).abs() < 1e-9);
        assert!((rotated.y - 0.0).abs() < 1e-9);
        assert!((rotated.width - 20.0).abs() < 1e-9);
        assert!((rotated.height - 10.0).abs() < 1e-9);
    }

    #[cfg(feature = "serde")]
    #[test]
    fn test_serde_writes_placed_points() {
        let path = Path::rect(0.0, 0.0, 10.0, 10.0).transform(&Transform::translate(5.0, 0.0));
        let json = serde_json::to_string(&path).unwrap();
        let back: Path = serde_json::from_str(&json).unwrap();
        assert_eq!(back.placement(), Transform::IDENTITY);
        assert_eq!(back, path);
    }
}
