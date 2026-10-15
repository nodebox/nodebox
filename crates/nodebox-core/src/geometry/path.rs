//! Path - a shape made of contours with fill and stroke styling.

use super::{Contour, Color, Transform, Rect, Point, PathPoint, PointType, Segment};

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
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
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

    /// Adds a quadratic Bezier curve to the current contour.
    pub fn quad_to(&mut self, cx: f64, cy: f64, x: f64, y: f64) {
        self.current_contour().quad_to(cx, cy, x, y);
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

    /// Returns the bounding box of the curves themselves, as NodeBox 3's
    /// `Path.getBounds()` computes it: on-curve points plus the extrema of
    /// each cubic segment. [`Path::bounds`] uses the control points instead,
    /// which can be larger.
    ///
    /// Returns `None` if the path is empty.
    pub fn curve_bounds(&self) -> Option<Rect> {
        let mut min_x = f64::MAX;
        let mut min_y = f64::MAX;
        let mut max_x = -f64::MAX;
        let mut max_y = -f64::MAX;
        let mut any = false;
        for contour in &self.contours {
            let pts = &contour.points;
            for (i, pp) in pts.iter().enumerate() {
                let r = match pp.point_type {
                    PointType::LineTo => Rect::new(pp.point.x, pp.point.y, 0.0, 0.0),
                    PointType::CurveTo if i >= 3 => cubic_extrema(
                        pts[i - 3].point,
                        pts[i - 2].point,
                        pts[i - 1].point,
                        pp.point,
                    ),
                    PointType::QuadTo if i >= 2 => {
                        let (c1, c2) = quad_to_cubic(pts[i - 2].point, pts[i - 1].point, pp.point);
                        cubic_extrema(pts[i - 2].point, c1, c2, pp.point)
                    }
                    _ => continue,
                };
                any = true;
                min_x = min_x.min(r.x);
                min_y = min_y.min(r.y);
                max_x = max_x.max(r.x + r.width);
                max_y = max_y.max(r.y + r.height);
            }
        }
        any.then(|| Rect::new(min_x, min_y, max_x - min_x, max_y - min_y))
    }

    /// Returns true if `point` lies inside the path, using the non-zero
    /// winding rule. Open contours count as closed, as when filling.
    pub fn contains(&self, point: Point) -> bool {
        let mut winding = 0;
        for contour in &self.contours {
            let mut segments = contour.segments();
            if !contour.closed {
                if let (Some(first), Some(last)) = (contour.points.first(), contour.points.last()) {
                    if first.point != last.point {
                        segments.push(Segment::Line { start: last.point, end: first.point });
                    }
                }
            }
            for segment in segments {
                winding += match segment {
                    Segment::Line { start, end } => line_crossing(start, end, point),
                    Segment::Cubic { start, ctrl1, ctrl2, end } => {
                        cubic_crossings(start, ctrl1, ctrl2, end, point)
                    }
                    Segment::Quadratic { start, ctrl, end } => {
                        let (c1, c2) = quad_to_cubic(start, ctrl, end);
                        cubic_crossings(start, c1, c2, end, point)
                    }
                };
            }
        }
        winding != 0
    }
}

/// The two control points of the cubic that traces the same curve as a quad.
fn quad_to_cubic(start: Point, ctrl: Point, end: Point) -> (Point, Point) {
    (
        Point::new(start.x + 2.0 / 3.0 * (ctrl.x - start.x), start.y + 2.0 / 3.0 * (ctrl.y - start.y)),
        Point::new(end.x + 2.0 / 3.0 * (ctrl.x - end.x), end.y + 2.0 / 3.0 * (ctrl.y - end.y)),
    )
}

/// Bounds of one cubic segment, computed as NodeBox 3's `Path.Bezier.extrema()`
/// does, so `curve_bounds` returns the same numbers.
fn cubic_extrema(p1: Point, p2: Point, p3: Point, p4: Point) -> Rect {
    fn fuzzy_equal(a: f64, b: f64) -> bool {
        (a - b).abs() <= 0.000000000001 * a.abs().min(b.abs())
    }
    fn point_at(p1: Point, p2: Point, p3: Point, p4: Point, t: f64) -> Point {
        let m_t = 1.0 - t;
        let mut b = m_t * m_t;
        let mut c = t * t;
        let d = c * t;
        let a = b * m_t;
        b *= 3.0 * t;
        c *= 3.0 * m_t;
        Point::new(
            a * p1.x + b * p2.x + c * p3.x + d * p4.x,
            a * p1.y + b * p2.y + c * p3.y + d * p4.y,
        )
    }
    let (mut min_x, mut max_x) = if p1.x < p4.x { (p1.x, p4.x) } else { (p4.x, p1.x) };
    let (mut min_y, mut max_y) = if p1.y < p4.y { (p1.y, p4.y) } else { (p4.y, p1.y) };
    let mut check = |t: f64| {
        if (0.0..=1.0).contains(&t) {
            let p = point_at(p1, p2, p3, p4, t);
            if p.x < min_x {
                min_x = p.x;
            } else if p.x > max_x {
                max_x = p.x;
            }
            if p.y < min_y {
                min_y = p.y;
            } else if p.y > max_y {
                max_y = p.y;
            }
        }
    };
    // Roots of the derivative, per axis. The x axis accepts a zero
    // discriminant and the y axis does not, as in NodeBox 3.
    for (v1, v2, v3, v4, allow_zero) in [(p1.x, p2.x, p3.x, p4.x, true), (p1.y, p2.y, p3.y, p4.y, false)] {
        let a = 3.0 * (-v1 + 3.0 * v2 - 3.0 * v3 + v4);
        let b = 6.0 * (v1 - 2.0 * v2 + v3);
        let c = 3.0 * (-v1 + v2);
        if fuzzy_equal(a + 1.0, 1.0) {
            if !fuzzy_equal(b + 1.0, 1.0) {
                check(-c / b);
            }
        } else {
            let disc = b * b - 4.0 * a * c;
            if disc > 0.0 || (allow_zero && disc == 0.0) {
                let root = disc.sqrt();
                let rcp = 1.0 / (2.0 * a);
                check((-b + root) * rcp);
                check((-b - root) * rcp);
            }
        }
    }
    Rect::new(min_x, min_y, max_x - min_x, max_y - min_y)
}

/// Signed crossing of a horizontal ray from `p` towards +x with a line:
/// +1 going down the y axis, -1 going up, 0 if it misses. A segment
/// includes its lower y end and excludes its upper end, so shared endpoints
/// count once.
fn line_crossing(a: Point, b: Point, p: Point) -> i32 {
    let (dir, lo, hi) = if a.y < b.y {
        (1, a, b)
    } else if a.y > b.y {
        (-1, b, a)
    } else {
        return 0;
    };
    if p.y < lo.y || p.y >= hi.y {
        return 0;
    }
    let x = lo.x + (p.y - lo.y) * (hi.x - lo.x) / (hi.y - lo.y);
    if p.x < x { dir } else { 0 }
}

/// Signed crossings of a horizontal ray from `p` towards +x with a cubic.
/// The curve is split where it turns in y, and each monotonic piece is
/// solved for `y(t) = p.y` by bisection.
fn cubic_crossings(p0: Point, p1: Point, p2: Point, p3: Point, p: Point) -> i32 {
    let coord = |v0: f64, v1: f64, v2: f64, v3: f64, t: f64| {
        let m = 1.0 - t;
        m * m * m * v0 + 3.0 * m * m * t * v1 + 3.0 * m * t * t * v2 + t * t * t * v3
    };
    let y = |t: f64| coord(p0.y, p1.y, p2.y, p3.y, t);
    let x = |t: f64| coord(p0.x, p1.x, p2.x, p3.x, t);

    // Quick reject: the curve stays within the hull of its control points.
    let min_y = p0.y.min(p1.y).min(p2.y).min(p3.y);
    let max_y = p0.y.max(p1.y).max(p2.y).max(p3.y);
    let max_x = p0.x.max(p1.x).max(p2.x).max(p3.x);
    if p.y < min_y || p.y > max_y || p.x >= max_x {
        return 0;
    }

    // y'(t) / 3 = a t^2 + b t + c
    let a = -p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y;
    let b = 2.0 * (p0.y - 2.0 * p1.y + p2.y);
    let c = p1.y - p0.y;
    let mut ts = vec![0.0];
    let mut add_root = |t: f64| {
        if t > 0.0 && t < 1.0 {
            ts.push(t);
        }
    };
    if a.abs() < 1e-12 {
        if b.abs() > 1e-12 {
            add_root(-c / b);
        }
    } else {
        let disc = b * b - 4.0 * a * c;
        if disc >= 0.0 {
            let root = disc.sqrt();
            add_root((-b + root) / (2.0 * a));
            add_root((-b - root) / (2.0 * a));
        }
    }
    ts.push(1.0);
    ts.sort_by(|u, v| u.total_cmp(v));

    let mut winding = 0;
    for w in ts.windows(2) {
        let (t0, t1) = (w[0], w[1]);
        let (y0, y1) = (y(t0), y(t1));
        let (dir, lo_t, hi_t, lo_y, hi_y) = if y0 < y1 {
            (1, t0, t1, y0, y1)
        } else if y0 > y1 {
            (-1, t1, t0, y1, y0)
        } else {
            continue;
        };
        if p.y < lo_y || p.y >= hi_y {
            continue;
        }
        // Bisect between the t at the low end and the t at the high end.
        let (mut lo, mut hi) = (lo_t, hi_t);
        for _ in 0..64 {
            let mid = 0.5 * (lo + hi);
            if y(mid) < p.y {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        if p.x < x(0.5 * (lo + hi)) {
            winding += dir;
        }
    }
    winding
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
