//! Rectangle type for bounding boxes and dimensions.

use std::fmt;
use super::Point;

/// A rectangle defined by position and size.
///
/// The rectangle is defined by its top-left corner (x, y) and dimensions (width, height).
/// Width and height can be negative; use [`Rect::normalized`] to get a rectangle with
/// positive dimensions.
///
/// # Examples
///
/// ```
/// use nodebox_core::geometry::{Rect, Point};
///
/// let r = Rect::new(10.0, 20.0, 100.0, 50.0);
/// assert_eq!(r.center(), Point::new(60.0, 45.0));
/// ```
#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct Rect {
    pub x: f64,
    pub y: f64,
    pub width: f64,
    pub height: f64,
}

impl Rect {
    /// An empty rectangle at the origin.
    pub const ZERO: Rect = Rect { x: 0.0, y: 0.0, width: 0.0, height: 0.0 };

    /// Creates a new rectangle with the given position and size.
    #[inline]
    pub const fn new(x: f64, y: f64, width: f64, height: f64) -> Self {
        Rect { x, y, width, height }
    }

    /// Creates a rectangle centered at (cx, cy) with the given dimensions.
    #[inline]
    pub fn centered(cx: f64, cy: f64, width: f64, height: f64) -> Self {
        Rect::new(cx - width / 2.0, cy - height / 2.0, width, height)
    }

    /// Creates a rectangle from two corner points.
    #[inline]
    pub fn from_corners(p1: Point, p2: Point) -> Self {
        let x = p1.x.min(p2.x);
        let y = p1.y.min(p2.y);
        let width = (p2.x - p1.x).abs();
        let height = (p2.y - p1.y).abs();
        Rect::new(x, y, width, height)
    }

    /// Returns the top-left corner of the rectangle.
    #[inline]
    pub fn position(&self) -> Point {
        Point::new(self.x, self.y)
    }

    /// Returns the center point of the rectangle.
    #[inline]
    pub fn center(&self) -> Point {
        Point::new(self.x + self.width / 2.0, self.y + self.height / 2.0)
    }

    /// Returns the minimum x coordinate (left edge after normalization).
    #[inline]
    pub fn min_x(&self) -> f64 {
        self.x.min(self.x + self.width)
    }

    /// Returns the maximum x coordinate (right edge after normalization).
    #[inline]
    pub fn max_x(&self) -> f64 {
        self.x.max(self.x + self.width)
    }

    /// Returns the minimum y coordinate (top edge after normalization).
    #[inline]
    pub fn min_y(&self) -> f64 {
        self.y.min(self.y + self.height)
    }

    /// Returns the maximum y coordinate (bottom edge after normalization).
    #[inline]
    pub fn max_y(&self) -> f64 {
        self.y.max(self.y + self.height)
    }

    /// Returns true if the rectangle has zero or negative area.
    #[inline]
    pub fn is_empty(&self) -> bool {
        let n = self.normalized();
        n.width <= 0.0 || n.height <= 0.0
    }

    /// Returns a normalized rectangle with positive width and height.
    ///
    /// If width or height is negative, the rectangle is flipped so that
    /// (x, y) is the top-left corner.
    pub fn normalized(&self) -> Rect {
        let mut x = self.x;
        let mut y = self.y;
        let mut width = self.width;
        let mut height = self.height;

        if width < 0.0 {
            x += width;
            width = -width;
        }
        if height < 0.0 {
            y += height;
            height = -height;
        }

        Rect::new(x, y, width, height)
    }

    /// Returns the union of this rectangle with another.
    ///
    /// The result is the smallest rectangle that contains both rectangles.
    pub fn union(&self, other: &Rect) -> Rect {
        let r1 = self.normalized();
        let r2 = other.normalized();

        let x = r1.x.min(r2.x);
        let y = r1.y.min(r2.y);
        let width = (r1.x + r1.width).max(r2.x + r2.width) - x;
        let height = (r1.y + r1.height).max(r2.y + r2.height) - y;

        Rect::new(x, y, width, height)
    }

    /// Returns true if this rectangle intersects with another.
    pub fn intersects(&self, other: &Rect) -> bool {
        let r1 = self.normalized();
        let r2 = other.normalized();

        r1.x < r2.x + r2.width
            && r1.x + r1.width > r2.x
            && r1.y < r2.y + r2.height
            && r1.y + r1.height > r2.y
    }

    /// Returns the intersection of this rectangle with another.
    ///
    /// Returns `None` if the rectangles don't intersect.
    pub fn intersection(&self, other: &Rect) -> Option<Rect> {
        if !self.intersects(other) {
            return None;
        }

        let r1 = self.normalized();
        let r2 = other.normalized();

        let x = r1.x.max(r2.x);
        let y = r1.y.max(r2.y);
        let width = (r1.x + r1.width).min(r2.x + r2.width) - x;
        let height = (r1.y + r1.height).min(r2.y + r2.height) - y;

        Some(Rect::new(x, y, width, height))
    }

    /// Returns true if this rectangle contains the given point.
    pub fn contains_point(&self, p: Point) -> bool {
        let r = self.normalized();
        p.x >= r.x && p.x <= r.x + r.width && p.y >= r.y && p.y <= r.y + r.height
    }

    /// Returns true if this rectangle fully contains another rectangle.
    pub fn contains_rect(&self, other: &Rect) -> bool {
        let r1 = self.normalized();
        let r2 = other.normalized();

        r2.x >= r1.x
            && r2.x + r2.width <= r1.x + r1.width
            && r2.y >= r1.y
            && r2.y + r2.height <= r1.y + r1.height
    }

    /// Returns a new rectangle expanded by the given amount on all sides.
    #[inline]
    pub fn inset(&self, dx: f64, dy: f64) -> Rect {
        Rect::new(
            self.x + dx,
            self.y + dy,
            self.width - 2.0 * dx,
            self.height - 2.0 * dy,
        )
    }

    /// Returns a new rectangle translated by (dx, dy).
    #[inline]
    pub fn translate(&self, dx: f64, dy: f64) -> Rect {
        Rect::new(self.x + dx, self.y + dy, self.width, self.height)
    }
}

impl fmt::Display for Rect {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "Rect({}, {}, {}, {})", self.x, self.y, self.width, self.height)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_rect_creation() {
        let r = Rect::new(10.0, 20.0, 100.0, 50.0);
        assert_eq!(r.x, 10.0);
        assert_eq!(r.y, 20.0);
        assert_eq!(r.width, 100.0);
        assert_eq!(r.height, 50.0);
    }

    #[test]
    fn test_rect_centered() {
        let r = Rect::centered(50.0, 50.0, 100.0, 60.0);
        assert_eq!(r.x, 0.0);
        assert_eq!(r.y, 20.0);
        assert_eq!(r.width, 100.0);
        assert_eq!(r.height, 60.0);
        assert_eq!(r.center(), Point::new(50.0, 50.0));
    }

    #[test]
    fn test_rect_from_corners() {
        let r = Rect::from_corners(Point::new(10.0, 20.0), Point::new(110.0, 70.0));
        assert_eq!(r.x, 10.0);
        assert_eq!(r.y, 20.0);
        assert_eq!(r.width, 100.0);
        assert_eq!(r.height, 50.0);

        // Reversed corners should give same result
        let r2 = Rect::from_corners(Point::new(110.0, 70.0), Point::new(10.0, 20.0));
        assert_eq!(r2, r);
    }

    #[test]
    fn test_rect_center() {
        let r = Rect::new(10.0, 20.0, 100.0, 50.0);
        assert_eq!(r.center(), Point::new(60.0, 45.0));
    }

    #[test]
    fn test_rect_normalized() {
        let r = Rect::new(100.0, 50.0, -80.0, -30.0);
        let n = r.normalized();
        assert_eq!(n.x, 20.0);
        assert_eq!(n.y, 20.0);
        assert_eq!(n.width, 80.0);
        assert_eq!(n.height, 30.0);
    }

    #[test]
    fn test_rect_is_empty() {
        // Tests matching Java RectTest.testEmpty()
        // Non-empty rectangles
        assert!(!Rect::new(100.0, 100.0, 100.0, 100.0).is_empty());
        assert!(!Rect::new(1.0, 1.0, 1.0, 1.0).is_empty());
        assert!(!Rect::new(0.0, 0.0, 1.0, 1.0).is_empty());
        // Negative dimensions are NOT empty (they normalize to positive)
        assert!(!Rect::new(0.0, 0.0, -100.0, -200.0).is_empty());

        // Empty rectangles (zero width OR height)
        assert!(Rect::new(0.0, 0.0, 0.0, 0.0).is_empty());
        assert!(Rect::new(-10.0, 0.0, 0.0, 10.0).is_empty());
        assert!(Rect::new(-10.0, 0.0, 200.0, 0.0).is_empty());
        assert!(Rect::new(20.0, 30.0, 10.0, 0.0).is_empty());
    }

    /// Tests matching Java RectTest.testUnited()
    #[test]
    fn test_rect_union_java() {
        let r1 = Rect::new(10.0, 20.0, 30.0, 40.0);
        let r2 = Rect::new(40.0, 30.0, 50.0, 30.0);
        let u = r1.union(&r2);
        assert_eq!(u.x, 10.0);
        assert_eq!(u.y, 20.0);
        assert_eq!(u.width, 40.0 + 50.0 - 10.0); // 80
        assert_eq!(u.height, 30.0 + 30.0 - 20.0); // 40

        let r3 = Rect::new(10.0, 20.0, 30.0, 40.0);
        let r4 = Rect::new(10.0, 120.0, 30.0, 40.0);
        let u2 = r3.union(&r4);
        assert_eq!(u2.x, 10.0);
        assert_eq!(u2.y, 20.0);
        assert_eq!(u2.width, 30.0);
        assert_eq!(u2.height, 120.0 + 40.0 - 20.0); // 140
    }

    /// Tests matching Java RectTest.testContains()
    #[test]
    fn test_rect_contains_java() {
        let r = Rect::new(10.0, 20.0, 30.0, 40.0);
        // Point containment
        assert!(r.contains_point(Point::new(10.0, 20.0)));
        assert!(r.contains_point(Point::new(11.0, 22.0)));
        assert!(r.contains_point(Point::new(40.0, 60.0)));
        assert!(!r.contains_point(Point::new(0.0, 0.0)));
        assert!(!r.contains_point(Point::new(-11.0, -22.0)));
        assert!(!r.contains_point(Point::new(100.0, 200.0)));
        assert!(!r.contains_point(Point::new(15.0, 200.0)));
        assert!(!r.contains_point(Point::new(200.0, 25.0)));

        // Rect containment
        assert!(r.contains_rect(&Rect::new(10.0, 20.0, 30.0, 40.0)));
        assert!(r.contains_rect(&Rect::new(15.0, 25.0, 5.0, 5.0)));
        assert!(!r.contains_rect(&Rect::new(15.0, 25.0, 30.0, 40.0)));
        assert!(!r.contains_rect(&Rect::new(1.0, 2.0, 3.0, 4.0)));
        assert!(!r.contains_rect(&Rect::new(15.0, 25.0, 300.0, 400.0)));
        assert!(!r.contains_rect(&Rect::new(15.0, 25.0, 5.0, 400.0)));
        assert!(!r.contains_rect(&Rect::new(15.0, 25.0, 400.0, 5.0)));
    }

    #[test]
    fn test_rect_union() {
        let r1 = Rect::new(0.0, 0.0, 50.0, 50.0);
        let r2 = Rect::new(25.0, 25.0, 50.0, 50.0);
        let u = r1.union(&r2);
        assert_eq!(u.x, 0.0);
        assert_eq!(u.y, 0.0);
        assert_eq!(u.width, 75.0);
        assert_eq!(u.height, 75.0);
    }

    #[test]
    fn test_rect_intersects() {
        let r1 = Rect::new(0.0, 0.0, 50.0, 50.0);
        let r2 = Rect::new(25.0, 25.0, 50.0, 50.0);
        let r3 = Rect::new(100.0, 100.0, 50.0, 50.0);

        assert!(r1.intersects(&r2));
        assert!(!r1.intersects(&r3));
    }

    #[test]
    fn test_rect_intersection() {
        let r1 = Rect::new(0.0, 0.0, 50.0, 50.0);
        let r2 = Rect::new(25.0, 25.0, 50.0, 50.0);
        let i = r1.intersection(&r2).unwrap();
        assert_eq!(i.x, 25.0);
        assert_eq!(i.y, 25.0);
        assert_eq!(i.width, 25.0);
        assert_eq!(i.height, 25.0);

        let r3 = Rect::new(100.0, 100.0, 50.0, 50.0);
        assert!(r1.intersection(&r3).is_none());
    }

    #[test]
    fn test_rect_contains_point() {
        let r = Rect::new(10.0, 20.0, 100.0, 50.0);
        assert!(r.contains_point(Point::new(50.0, 40.0)));
        assert!(r.contains_point(Point::new(10.0, 20.0))); // Corner
        assert!(r.contains_point(Point::new(110.0, 70.0))); // Corner
        assert!(!r.contains_point(Point::new(0.0, 0.0)));
        assert!(!r.contains_point(Point::new(200.0, 200.0)));
    }

    #[test]
    fn test_rect_contains_rect() {
        let r1 = Rect::new(0.0, 0.0, 100.0, 100.0);
        let r2 = Rect::new(10.0, 10.0, 50.0, 50.0);
        let r3 = Rect::new(50.0, 50.0, 100.0, 100.0);

        assert!(r1.contains_rect(&r2));
        assert!(!r1.contains_rect(&r3));
    }

    #[test]
    fn test_rect_inset() {
        let r = Rect::new(0.0, 0.0, 100.0, 100.0);
        let inset = r.inset(10.0, 20.0);
        assert_eq!(inset.x, 10.0);
        assert_eq!(inset.y, 20.0);
        assert_eq!(inset.width, 80.0);
        assert_eq!(inset.height, 60.0);
    }

    #[test]
    fn test_rect_translate() {
        let r = Rect::new(10.0, 20.0, 100.0, 50.0);
        let moved = r.translate(5.0, -5.0);
        assert_eq!(moved.x, 15.0);
        assert_eq!(moved.y, 15.0);
        assert_eq!(moved.width, 100.0);
        assert_eq!(moved.height, 50.0);
    }

    #[test]
    fn test_rect_display() {
        let r = Rect::new(10.0, 20.0, 100.0, 50.0);
        assert_eq!(format!("{}", r), "Rect(10, 20, 100, 50)");
    }
}
