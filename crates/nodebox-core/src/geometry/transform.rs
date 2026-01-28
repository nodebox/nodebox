//! 2D affine transformation matrix.

use std::f64::consts::PI;
use std::fmt;
use std::ops::Mul;
use super::{Point, PathPoint, Rect};

/// A 2D affine transformation matrix.
///
/// The matrix is stored as [m00, m10, m01, m11, m02, m12] where:
/// ```text
/// | m00 m01 m02 |   | scale_x  shear_x  translate_x |
/// | m10 m11 m12 | = | shear_y  scale_y  translate_y |
/// | 0   0   1   |   | 0        0        1           |
/// ```
///
/// Transforming a point (x, y):
/// ```text
/// x' = m00*x + m01*y + m02
/// y' = m10*x + m11*y + m12
/// ```
///
/// # Examples
///
/// ```
/// use nodebox_core::geometry::{Transform, Point};
///
/// let t = Transform::translate(10.0, 20.0);
/// let p = t.transform_point(Point::new(5.0, 5.0));
/// assert_eq!(p, Point::new(15.0, 25.0));
/// ```
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Transform {
    /// Matrix elements [m00, m10, m01, m11, m02, m12]
    m: [f64; 6],
}

impl Default for Transform {
    fn default() -> Self {
        Transform::IDENTITY
    }
}

impl Transform {
    /// The identity transform (no transformation).
    pub const IDENTITY: Transform = Transform {
        m: [1.0, 0.0, 0.0, 1.0, 0.0, 0.0],
    };

    /// Creates a new transform from matrix elements.
    ///
    /// The elements are [m00, m10, m01, m11, m02, m12] matching Java's AffineTransform order.
    #[inline]
    pub const fn new(m00: f64, m10: f64, m01: f64, m11: f64, m02: f64, m12: f64) -> Self {
        Transform { m: [m00, m10, m01, m11, m02, m12] }
    }

    /// Creates a translation transform.
    #[inline]
    pub fn translate(tx: f64, ty: f64) -> Self {
        Transform::new(1.0, 0.0, 0.0, 1.0, tx, ty)
    }

    /// Creates a rotation transform (angle in degrees).
    #[inline]
    pub fn rotate(degrees: f64) -> Self {
        Transform::rotate_radians(degrees * PI / 180.0)
    }

    /// Creates a rotation transform (angle in radians).
    #[inline]
    pub fn rotate_radians(radians: f64) -> Self {
        let cos = radians.cos();
        let sin = radians.sin();
        Transform::new(cos, sin, -sin, cos, 0.0, 0.0)
    }

    /// Creates a uniform scaling transform.
    #[inline]
    pub fn scale(s: f64) -> Self {
        Transform::scale_xy(s, s)
    }

    /// Creates a non-uniform scaling transform.
    #[inline]
    pub fn scale_xy(sx: f64, sy: f64) -> Self {
        Transform::new(sx, 0.0, 0.0, sy, 0.0, 0.0)
    }

    /// Creates a skew transform (angles in degrees).
    #[inline]
    pub fn skew(kx: f64, ky: f64) -> Self {
        let kx_rad = kx * PI / 180.0;
        let ky_rad = ky * PI / 180.0;
        Transform::new(1.0, ky_rad.tan(), -kx_rad.tan(), 1.0, 0.0, 0.0)
    }

    /// Returns the matrix elements as an array [m00, m10, m01, m11, m02, m12].
    #[inline]
    pub fn as_array(&self) -> [f64; 6] {
        self.m
    }

    /// Returns the translation component (tx, ty).
    #[inline]
    pub fn translation(&self) -> (f64, f64) {
        (self.m[4], self.m[5])
    }

    /// Concatenates another transform to this one.
    ///
    /// The result is equivalent to applying `self` first, then `other`.
    /// In matrix terms, this computes `other * self`.
    #[inline]
    pub fn then(&self, other: &Transform) -> Transform {
        // To apply self first, then other, we compute: other * self
        // [A B TX]   [a b tx]   [Aa+Bc Ab+Bd Atx+Bty+TX]
        // [C D TY] * [c d ty] = [Ca+Dc Cb+Dd Ctx+Dty+TY]
        // [0 0 1 ]   [0 0 1 ]   [0     0     1         ]
        let a = self.m[0];
        let b = self.m[2];
        let c = self.m[1];
        let d = self.m[3];
        let tx = self.m[4];
        let ty = self.m[5];

        let aa = other.m[0];
        let bb = other.m[2];
        let cc = other.m[1];
        let dd = other.m[3];
        let txx = other.m[4];
        let tyy = other.m[5];

        Transform::new(
            aa * a + bb * c,
            cc * a + dd * c,
            aa * b + bb * d,
            cc * b + dd * d,
            aa * tx + bb * ty + txx,
            cc * tx + dd * ty + tyy,
        )
    }

    /// Prepends another transform to this one.
    ///
    /// The result is equivalent to applying `other` first, then `self`.
    #[inline]
    pub fn pre(&self, other: &Transform) -> Transform {
        other.then(self)
    }

    /// Returns the inverse of this transform, if it exists.
    pub fn inverse(&self) -> Option<Transform> {
        let a = self.m[0];
        let b = self.m[2];
        let c = self.m[1];
        let d = self.m[3];
        let tx = self.m[4];
        let ty = self.m[5];

        let det = a * d - b * c;
        if det.abs() < 1e-10 {
            return None;
        }

        let inv_det = 1.0 / det;
        Some(Transform::new(
            d * inv_det,
            -c * inv_det,
            -b * inv_det,
            a * inv_det,
            (b * ty - d * tx) * inv_det,
            (c * tx - a * ty) * inv_det,
        ))
    }

    /// Transforms a point.
    #[inline]
    pub fn transform_point(&self, p: Point) -> Point {
        Point::new(
            self.m[0] * p.x + self.m[2] * p.y + self.m[4],
            self.m[1] * p.x + self.m[3] * p.y + self.m[5],
        )
    }

    /// Transforms a path point (preserving type).
    #[inline]
    pub fn transform_path_point(&self, pp: PathPoint) -> PathPoint {
        PathPoint {
            point: self.transform_point(pp.point),
            point_type: pp.point_type,
        }
    }

    /// Transforms a vector (direction only, ignoring translation).
    #[inline]
    pub fn transform_vector(&self, p: Point) -> Point {
        Point::new(
            self.m[0] * p.x + self.m[2] * p.y,
            self.m[1] * p.x + self.m[3] * p.y,
        )
    }

    /// Transforms a slice of points.
    pub fn transform_points(&self, points: &[Point]) -> Vec<Point> {
        points.iter().map(|p| self.transform_point(*p)).collect()
    }

    /// Transforms a slice of path points.
    pub fn transform_path_points(&self, points: &[PathPoint]) -> Vec<PathPoint> {
        points.iter().map(|p| self.transform_path_point(*p)).collect()
    }

    /// Transforms a rectangle.
    ///
    /// Note: This returns the bounding box of the transformed rectangle,
    /// which may be larger if the transform includes rotation or skew.
    pub fn transform_rect(&self, r: Rect) -> Rect {
        // Transform all four corners and compute bounding box
        let corners = [
            self.transform_point(Point::new(r.x, r.y)),
            self.transform_point(Point::new(r.x + r.width, r.y)),
            self.transform_point(Point::new(r.x + r.width, r.y + r.height)),
            self.transform_point(Point::new(r.x, r.y + r.height)),
        ];

        let min_x = corners.iter().map(|p| p.x).fold(f64::INFINITY, f64::min);
        let max_x = corners.iter().map(|p| p.x).fold(f64::NEG_INFINITY, f64::max);
        let min_y = corners.iter().map(|p| p.y).fold(f64::INFINITY, f64::min);
        let max_y = corners.iter().map(|p| p.y).fold(f64::NEG_INFINITY, f64::max);

        Rect::new(min_x, min_y, max_x - min_x, max_y - min_y)
    }

    /// Returns true if this is approximately the identity transform.
    pub fn is_identity(&self) -> bool {
        const EPS: f64 = 1e-10;
        (self.m[0] - 1.0).abs() < EPS
            && self.m[1].abs() < EPS
            && self.m[2].abs() < EPS
            && (self.m[3] - 1.0).abs() < EPS
            && self.m[4].abs() < EPS
            && self.m[5].abs() < EPS
    }
}

impl Mul for Transform {
    type Output = Transform;

    #[inline]
    fn mul(self, rhs: Transform) -> Transform {
        self.then(&rhs)
    }
}

impl fmt::Display for Transform {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "Transform([{:.4}, {:.4}, {:.4}, {:.4}, {:.4}, {:.4}])",
            self.m[0], self.m[1], self.m[2], self.m[3], self.m[4], self.m[5]
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const EPS: f64 = 1e-10;

    fn approx_eq(a: f64, b: f64) -> bool {
        (a - b).abs() < EPS
    }

    fn point_approx_eq(a: Point, b: Point) -> bool {
        approx_eq(a.x, b.x) && approx_eq(a.y, b.y)
    }

    #[test]
    fn test_identity() {
        let t = Transform::IDENTITY;
        let p = Point::new(10.0, 20.0);
        assert_eq!(t.transform_point(p), p);
        assert!(t.is_identity());
    }

    #[test]
    fn test_translate() {
        let t = Transform::translate(10.0, 20.0);
        let p = t.transform_point(Point::new(5.0, 5.0));
        assert_eq!(p, Point::new(15.0, 25.0));
    }

    #[test]
    fn test_scale() {
        let t = Transform::scale(2.0);
        let p = t.transform_point(Point::new(10.0, 20.0));
        assert_eq!(p, Point::new(20.0, 40.0));
    }

    #[test]
    fn test_scale_xy() {
        let t = Transform::scale_xy(2.0, 3.0);
        let p = t.transform_point(Point::new(10.0, 20.0));
        assert_eq!(p, Point::new(20.0, 60.0));
    }

    #[test]
    fn test_rotate_90() {
        let t = Transform::rotate(90.0);
        let p = t.transform_point(Point::new(10.0, 0.0));
        assert!(point_approx_eq(p, Point::new(0.0, 10.0)));
    }

    #[test]
    fn test_rotate_180() {
        let t = Transform::rotate(180.0);
        let p = t.transform_point(Point::new(10.0, 20.0));
        assert!(point_approx_eq(p, Point::new(-10.0, -20.0)));
    }

    #[test]
    fn test_rotate_radians() {
        let t = Transform::rotate_radians(PI / 2.0);
        let p = t.transform_point(Point::new(10.0, 0.0));
        assert!(point_approx_eq(p, Point::new(0.0, 10.0)));
    }

    #[test]
    fn test_concatenate() {
        // Scale then translate
        let t1 = Transform::scale(2.0);
        let t2 = Transform::translate(10.0, 10.0);
        let t = t1.then(&t2);

        let p = t.transform_point(Point::new(5.0, 5.0));
        // (5,5) * 2 = (10, 10), then + (10, 10) = (20, 20)
        assert_eq!(p, Point::new(20.0, 20.0));
    }

    #[test]
    fn test_prepend() {
        // Translate then scale
        let t1 = Transform::scale(2.0);
        let t2 = Transform::translate(10.0, 10.0);
        let t = t1.pre(&t2);

        let p = t.transform_point(Point::new(5.0, 5.0));
        // (5,5) + (10, 10) = (15, 15), then * 2 = (30, 30)
        assert_eq!(p, Point::new(30.0, 30.0));
    }

    #[test]
    fn test_mul_operator() {
        let t1 = Transform::scale(2.0);
        let t2 = Transform::translate(10.0, 10.0);
        let t = t1 * t2;

        let p = t.transform_point(Point::new(5.0, 5.0));
        assert_eq!(p, Point::new(20.0, 20.0));
    }

    #[test]
    fn test_inverse() {
        let t = Transform::translate(10.0, 20.0);
        let inv = t.inverse().unwrap();

        let p = Point::new(15.0, 25.0);
        let p_transformed = t.transform_point(inv.transform_point(p));
        assert!(point_approx_eq(p, p_transformed));
    }

    #[test]
    fn test_inverse_scale() {
        let t = Transform::scale(2.0);
        let inv = t.inverse().unwrap();

        let p = Point::new(20.0, 40.0);
        let p_transformed = inv.transform_point(p);
        assert_eq!(p_transformed, Point::new(10.0, 20.0));
    }

    #[test]
    fn test_inverse_singular() {
        // A scale of 0 has no inverse
        let t = Transform::scale(0.0);
        assert!(t.inverse().is_none());
    }

    #[test]
    fn test_transform_vector() {
        let t = Transform::translate(100.0, 100.0);
        let v = t.transform_vector(Point::new(10.0, 0.0));
        // Translation should not affect vectors
        assert_eq!(v, Point::new(10.0, 0.0));
    }

    #[test]
    fn test_transform_points() {
        let t = Transform::translate(10.0, 20.0);
        let points = vec![Point::new(0.0, 0.0), Point::new(5.0, 5.0)];
        let transformed = t.transform_points(&points);
        assert_eq!(transformed[0], Point::new(10.0, 20.0));
        assert_eq!(transformed[1], Point::new(15.0, 25.0));
    }

    #[test]
    fn test_transform_rect() {
        let t = Transform::translate(10.0, 20.0);
        let r = Rect::new(0.0, 0.0, 100.0, 50.0);
        let transformed = t.transform_rect(r);
        assert_eq!(transformed.x, 10.0);
        assert_eq!(transformed.y, 20.0);
        assert_eq!(transformed.width, 100.0);
        assert_eq!(transformed.height, 50.0);
    }

    #[test]
    fn test_transform_rect_rotated() {
        let t = Transform::rotate(90.0);
        let r = Rect::new(0.0, 0.0, 100.0, 50.0);
        let transformed = t.transform_rect(r);

        // After 90° rotation, width and height swap
        assert!(approx_eq(transformed.width, 50.0));
        assert!(approx_eq(transformed.height, 100.0));
    }

    #[test]
    fn test_skew() {
        let t = Transform::skew(45.0, 0.0);
        let p = t.transform_point(Point::new(0.0, 10.0));
        // With 45° skew in x, y=10 shifts x by -10
        assert!(approx_eq(p.x, -10.0));
        assert!(approx_eq(p.y, 10.0));
    }

    #[test]
    fn test_path_point_transform() {
        let t = Transform::translate(10.0, 20.0);
        let pp = PathPoint::curve_to(5.0, 5.0);
        let transformed = t.transform_path_point(pp);

        assert_eq!(transformed.x(), 15.0);
        assert_eq!(transformed.y(), 25.0);
        assert_eq!(transformed.point_type, pp.point_type);
    }
}
