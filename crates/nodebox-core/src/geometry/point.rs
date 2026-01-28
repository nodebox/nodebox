//! Point types for 2D geometry
//!
//! This module provides [`Point`] for basic 2D coordinates and [`PathPoint`]
//! for points with curve type information used in paths.

use std::fmt;
use std::ops::{Add, Sub, Mul, Neg};
use std::str::FromStr;

/// A 2D point with x and y coordinates.
///
/// Points are immutable and Copy. All "mutation" operations return new points.
///
/// # Examples
///
/// ```
/// use nodebox_core::Point;
///
/// let p = Point::new(10.0, 20.0);
/// let moved = p.translate(5.0, -5.0);
/// assert_eq!(moved, Point::new(15.0, 15.0));
/// ```
#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct Point {
    pub x: f64,
    pub y: f64,
}

impl Point {
    /// The origin point (0, 0).
    pub const ZERO: Point = Point { x: 0.0, y: 0.0 };

    /// Creates a new point with the given coordinates.
    #[inline]
    pub const fn new(x: f64, y: f64) -> Self {
        Point { x, y }
    }

    /// Returns a new point translated by (dx, dy).
    #[inline]
    pub fn translate(self, dx: f64, dy: f64) -> Self {
        Point::new(self.x + dx, self.y + dy)
    }

    /// Returns the distance from this point to another point.
    #[inline]
    pub fn distance_to(self, other: Point) -> f64 {
        let dx = other.x - self.x;
        let dy = other.y - self.y;
        (dx * dx + dy * dy).sqrt()
    }

    /// Returns the angle from this point to another point in radians.
    #[inline]
    pub fn angle_to(self, other: Point) -> f64 {
        let dx = other.x - self.x;
        let dy = other.y - self.y;
        dy.atan2(dx)
    }

    /// Returns a point at the given angle and distance from this point.
    #[inline]
    pub fn point_at(self, angle: f64, distance: f64) -> Point {
        Point::new(
            self.x + distance * angle.cos(),
            self.y + distance * angle.sin(),
        )
    }

    /// Linearly interpolates between this point and another.
    #[inline]
    pub fn lerp(self, other: Point, t: f64) -> Point {
        Point::new(
            self.x + (other.x - self.x) * t,
            self.y + (other.y - self.y) * t,
        )
    }
}

impl Add for Point {
    type Output = Point;

    #[inline]
    fn add(self, other: Point) -> Point {
        Point::new(self.x + other.x, self.y + other.y)
    }
}

impl Sub for Point {
    type Output = Point;

    #[inline]
    fn sub(self, other: Point) -> Point {
        Point::new(self.x - other.x, self.y - other.y)
    }
}

impl Mul<f64> for Point {
    type Output = Point;

    #[inline]
    fn mul(self, scalar: f64) -> Point {
        Point::new(self.x * scalar, self.y * scalar)
    }
}

impl Neg for Point {
    type Output = Point;

    #[inline]
    fn neg(self) -> Point {
        Point::new(-self.x, -self.y)
    }
}

impl fmt::Display for Point {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{:.2},{:.2}", self.x, self.y)
    }
}

/// Error type for parsing points from strings.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ParsePointError {
    pub message: String,
}

impl fmt::Display for ParsePointError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.message)
    }
}

impl std::error::Error for ParsePointError {}

impl FromStr for Point {
    type Err = ParsePointError;

    /// Parses a point from a string like "12.3,45.6".
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let parts: Vec<&str> = s.split(',').collect();
        if parts.len() != 2 {
            return Err(ParsePointError {
                message: format!("String '{}' needs two components, e.g., '12.3,45.6'", s),
            });
        }

        let x = parts[0].trim().parse::<f64>().map_err(|_| ParsePointError {
            message: format!("Invalid x coordinate: '{}'", parts[0]),
        })?;
        let y = parts[1].trim().parse::<f64>().map_err(|_| ParsePointError {
            message: format!("Invalid y coordinate: '{}'", parts[1]),
        })?;

        Ok(Point::new(x, y))
    }
}

/// The type of a point in a path, indicating how to draw to this point.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Hash)]
pub enum PointType {
    /// Draw a straight line to this point.
    #[default]
    LineTo,
    /// This is the endpoint of a cubic Bezier curve.
    CurveTo,
    /// This is a control point for a cubic Bezier curve (not on the curve itself).
    CurveData,
}

impl PointType {
    /// Returns true if this point is on the curve (LineTo or CurveTo).
    #[inline]
    pub fn is_on_curve(self) -> bool {
        !matches!(self, PointType::CurveData)
    }

    /// Returns true if this point is off the curve (a control point).
    #[inline]
    pub fn is_off_curve(self) -> bool {
        matches!(self, PointType::CurveData)
    }
}

/// A point in a path with type information.
///
/// PathPoint combines a [`Point`] with a [`PointType`] to indicate
/// how the path should be drawn to this point.
#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct PathPoint {
    pub point: Point,
    pub point_type: PointType,
}

impl PathPoint {
    /// Creates a new path point.
    #[inline]
    pub const fn new(x: f64, y: f64, point_type: PointType) -> Self {
        PathPoint {
            point: Point { x, y },
            point_type,
        }
    }

    /// Creates a LineTo path point.
    #[inline]
    pub const fn line_to(x: f64, y: f64) -> Self {
        PathPoint::new(x, y, PointType::LineTo)
    }

    /// Creates a CurveTo path point (endpoint of a Bezier curve).
    #[inline]
    pub const fn curve_to(x: f64, y: f64) -> Self {
        PathPoint::new(x, y, PointType::CurveTo)
    }

    /// Creates a CurveData path point (control point).
    #[inline]
    pub const fn curve_data(x: f64, y: f64) -> Self {
        PathPoint::new(x, y, PointType::CurveData)
    }

    /// Returns the x coordinate.
    #[inline]
    pub fn x(&self) -> f64 {
        self.point.x
    }

    /// Returns the y coordinate.
    #[inline]
    pub fn y(&self) -> f64 {
        self.point.y
    }

    /// Returns a new path point translated by (dx, dy).
    #[inline]
    pub fn translate(self, dx: f64, dy: f64) -> Self {
        PathPoint {
            point: self.point.translate(dx, dy),
            point_type: self.point_type,
        }
    }
}

impl From<Point> for PathPoint {
    fn from(point: Point) -> Self {
        PathPoint {
            point,
            point_type: PointType::LineTo,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_point_creation() {
        let p = Point::new(10.0, 20.0);
        assert_eq!(p.x, 10.0);
        assert_eq!(p.y, 20.0);
    }

    #[test]
    fn test_point_zero() {
        assert_eq!(Point::ZERO, Point::new(0.0, 0.0));
    }

    #[test]
    fn test_point_translate() {
        let p = Point::new(10.0, 20.0);
        let moved = p.translate(5.0, -5.0);
        assert_eq!(moved, Point::new(15.0, 15.0));
    }

    #[test]
    fn test_point_add() {
        let p1 = Point::new(10.0, 20.0);
        let p2 = Point::new(5.0, 3.0);
        assert_eq!(p1 + p2, Point::new(15.0, 23.0));
    }

    #[test]
    fn test_point_sub() {
        let p1 = Point::new(10.0, 20.0);
        let p2 = Point::new(5.0, 3.0);
        assert_eq!(p1 - p2, Point::new(5.0, 17.0));
    }

    #[test]
    fn test_point_mul() {
        let p = Point::new(10.0, 20.0);
        assert_eq!(p * 2.0, Point::new(20.0, 40.0));
    }

    #[test]
    fn test_point_neg() {
        let p = Point::new(10.0, -20.0);
        assert_eq!(-p, Point::new(-10.0, 20.0));
    }

    #[test]
    fn test_point_distance() {
        let p1 = Point::new(0.0, 0.0);
        let p2 = Point::new(3.0, 4.0);
        assert!((p1.distance_to(p2) - 5.0).abs() < 1e-10);
    }

    #[test]
    fn test_point_angle() {
        let p1 = Point::ZERO;
        let p2 = Point::new(1.0, 0.0);
        assert!((p1.angle_to(p2) - 0.0).abs() < 1e-10);

        let p3 = Point::new(0.0, 1.0);
        assert!((p1.angle_to(p3) - std::f64::consts::FRAC_PI_2).abs() < 1e-10);
    }

    #[test]
    fn test_point_lerp() {
        let p1 = Point::new(0.0, 0.0);
        let p2 = Point::new(10.0, 20.0);
        assert_eq!(p1.lerp(p2, 0.0), p1);
        assert_eq!(p1.lerp(p2, 1.0), p2);
        assert_eq!(p1.lerp(p2, 0.5), Point::new(5.0, 10.0));
    }

    #[test]
    fn test_point_display() {
        let p = Point::new(12.345, 67.891);
        assert_eq!(format!("{}", p), "12.35,67.89");
    }

    #[test]
    fn test_point_parse() {
        let p: Point = "10.5,20.5".parse().unwrap();
        assert_eq!(p, Point::new(10.5, 20.5));

        let p2: Point = "10, 20".parse().unwrap();
        assert_eq!(p2, Point::new(10.0, 20.0));
    }

    #[test]
    fn test_point_parse_error() {
        let result: Result<Point, _> = "invalid".parse();
        assert!(result.is_err());

        let result2: Result<Point, _> = "1,2,3".parse();
        assert!(result2.is_err());
    }

    #[test]
    fn test_point_type() {
        assert!(PointType::LineTo.is_on_curve());
        assert!(PointType::CurveTo.is_on_curve());
        assert!(!PointType::CurveData.is_on_curve());
        assert!(PointType::CurveData.is_off_curve());
    }

    #[test]
    fn test_path_point_creation() {
        let pp = PathPoint::line_to(10.0, 20.0);
        assert_eq!(pp.x(), 10.0);
        assert_eq!(pp.y(), 20.0);
        assert_eq!(pp.point_type, PointType::LineTo);

        let pp2 = PathPoint::curve_to(30.0, 40.0);
        assert_eq!(pp2.point_type, PointType::CurveTo);

        let pp3 = PathPoint::curve_data(50.0, 60.0);
        assert_eq!(pp3.point_type, PointType::CurveData);
    }

    #[test]
    fn test_path_point_translate() {
        let pp = PathPoint::line_to(10.0, 20.0);
        let moved = pp.translate(5.0, 5.0);
        assert_eq!(moved.x(), 15.0);
        assert_eq!(moved.y(), 25.0);
        assert_eq!(moved.point_type, PointType::LineTo);
    }
}
