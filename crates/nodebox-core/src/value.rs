//! Runtime value types for node evaluation.
//!
//! The [`Value`] enum represents all possible values that can flow through
//! the node graph during evaluation.

use std::collections::HashMap;
use crate::geometry::{Point, Color, Path, Geometry};

/// A dynamic value that can flow through the node graph.
///
/// Values are produced by node operations and consumed by connected nodes.
/// The type system ensures compatibility at connection time.
#[derive(Clone, Debug, PartialEq)]
pub enum Value {
    /// No value (null).
    Null,
    /// Integer value.
    Int(i64),
    /// Floating-point value.
    Float(f64),
    /// String value.
    String(String),
    /// Boolean value.
    Boolean(bool),
    /// 2D point.
    Point(Point),
    /// RGBA color.
    Color(Color),
    /// A single path.
    Path(Path),
    /// A collection of paths.
    Geometry(Geometry),
    /// A list of values (can be heterogeneous).
    List(Vec<Value>),
    /// A key-value map.
    Map(HashMap<String, Value>),
}

impl Default for Value {
    fn default() -> Self {
        Value::Null
    }
}

impl Value {
    /// Returns true if this is a null value.
    #[inline]
    pub fn is_null(&self) -> bool {
        matches!(self, Value::Null)
    }

    /// Attempts to get this value as an integer.
    pub fn as_int(&self) -> Option<i64> {
        match self {
            Value::Int(i) => Some(*i),
            Value::Float(f) => Some(*f as i64),
            Value::Boolean(b) => Some(if *b { 1 } else { 0 }),
            _ => None,
        }
    }

    /// Attempts to get this value as a float.
    pub fn as_float(&self) -> Option<f64> {
        match self {
            Value::Float(f) => Some(*f),
            Value::Int(i) => Some(*i as f64),
            Value::Boolean(b) => Some(if *b { 1.0 } else { 0.0 }),
            _ => None,
        }
    }

    /// Attempts to get this value as a string.
    pub fn as_string(&self) -> Option<&str> {
        match self {
            Value::String(s) => Some(s),
            _ => None,
        }
    }

    /// Attempts to get this value as a boolean.
    pub fn as_bool(&self) -> Option<bool> {
        match self {
            Value::Boolean(b) => Some(*b),
            Value::Int(i) => Some(*i != 0),
            Value::Float(f) => Some(*f != 0.0),
            _ => None,
        }
    }

    /// Attempts to get this value as a point.
    pub fn as_point(&self) -> Option<&Point> {
        match self {
            Value::Point(p) => Some(p),
            _ => None,
        }
    }

    /// Attempts to get this value as a color.
    pub fn as_color(&self) -> Option<&Color> {
        match self {
            Value::Color(c) => Some(c),
            _ => None,
        }
    }

    /// Attempts to get this value as a path.
    pub fn as_path(&self) -> Option<&Path> {
        match self {
            Value::Path(p) => Some(p),
            _ => None,
        }
    }

    /// Attempts to get this value as a geometry.
    pub fn as_geometry(&self) -> Option<&Geometry> {
        match self {
            Value::Geometry(g) => Some(g),
            Value::Path(_) => None, // Could convert, but would need ownership
            _ => None,
        }
    }

    /// Attempts to get this value as a list.
    pub fn as_list(&self) -> Option<&[Value]> {
        match self {
            Value::List(l) => Some(l),
            _ => None,
        }
    }

    /// Converts this value to a geometry, if possible.
    pub fn into_geometry(self) -> Option<Geometry> {
        match self {
            Value::Geometry(g) => Some(g),
            Value::Path(p) => Some(Geometry::from_path(p)),
            _ => None,
        }
    }

    /// Converts this value to a list.
    ///
    /// If already a list, returns it. Otherwise wraps in a single-element list.
    pub fn into_list(self) -> Vec<Value> {
        match self {
            Value::List(l) => l,
            Value::Null => Vec::new(),
            other => vec![other],
        }
    }

    /// Converts this value to a string representation.
    pub fn to_string_value(&self) -> String {
        match self {
            Value::Null => "null".to_string(),
            Value::Int(i) => i.to_string(),
            Value::Float(f) => f.to_string(),
            Value::String(s) => s.clone(),
            Value::Boolean(b) => b.to_string(),
            Value::Point(p) => format!("{}", p),
            Value::Color(c) => c.to_hex(),
            Value::Path(_) => "[Path]".to_string(),
            Value::Geometry(g) => format!("[Geometry: {} paths]", g.len()),
            Value::List(l) => format!("[List: {} items]", l.len()),
            Value::Map(m) => format!("[Map: {} keys]", m.len()),
        }
    }

    /// Returns the type name of this value.
    pub fn type_name(&self) -> &'static str {
        match self {
            Value::Null => "null",
            Value::Int(_) => "int",
            Value::Float(_) => "float",
            Value::String(_) => "string",
            Value::Boolean(_) => "boolean",
            Value::Point(_) => "point",
            Value::Color(_) => "color",
            Value::Path(_) => "path",
            Value::Geometry(_) => "geometry",
            Value::List(_) => "list",
            Value::Map(_) => "map",
        }
    }
}

// Conversions from primitive types
impl From<i64> for Value {
    fn from(v: i64) -> Self {
        Value::Int(v)
    }
}

impl From<i32> for Value {
    fn from(v: i32) -> Self {
        Value::Int(v as i64)
    }
}

impl From<f64> for Value {
    fn from(v: f64) -> Self {
        Value::Float(v)
    }
}

impl From<f32> for Value {
    fn from(v: f32) -> Self {
        Value::Float(v as f64)
    }
}

impl From<String> for Value {
    fn from(v: String) -> Self {
        Value::String(v)
    }
}

impl From<&str> for Value {
    fn from(v: &str) -> Self {
        Value::String(v.to_string())
    }
}

impl From<bool> for Value {
    fn from(v: bool) -> Self {
        Value::Boolean(v)
    }
}

impl From<Point> for Value {
    fn from(v: Point) -> Self {
        Value::Point(v)
    }
}

impl From<Color> for Value {
    fn from(v: Color) -> Self {
        Value::Color(v)
    }
}

impl From<Path> for Value {
    fn from(v: Path) -> Self {
        Value::Path(v)
    }
}

impl From<Geometry> for Value {
    fn from(v: Geometry) -> Self {
        Value::Geometry(v)
    }
}

impl<T: Into<Value>> From<Vec<T>> for Value {
    fn from(v: Vec<T>) -> Self {
        Value::List(v.into_iter().map(Into::into).collect())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_value_null() {
        let v = Value::Null;
        assert!(v.is_null());
    }

    #[test]
    fn test_value_int() {
        let v = Value::from(42i64);
        assert_eq!(v.as_int(), Some(42));
        assert_eq!(v.as_float(), Some(42.0));
    }

    #[test]
    fn test_value_float() {
        let v = Value::from(3.14f64);
        assert_eq!(v.as_float(), Some(3.14));
        assert_eq!(v.as_int(), Some(3));
    }

    #[test]
    fn test_value_string() {
        let v = Value::from("hello");
        assert_eq!(v.as_string(), Some("hello"));
    }

    #[test]
    fn test_value_bool() {
        let v = Value::from(true);
        assert_eq!(v.as_bool(), Some(true));
        assert_eq!(v.as_int(), Some(1));
    }

    #[test]
    fn test_value_point() {
        let p = Point::new(10.0, 20.0);
        let v = Value::from(p);
        assert_eq!(v.as_point(), Some(&p));
    }

    #[test]
    fn test_value_geometry() {
        let geo = Geometry::from_path(Path::rect(0.0, 0.0, 100.0, 100.0));
        let v = Value::from(geo.clone());
        assert_eq!(v.as_geometry(), Some(&geo));
    }

    #[test]
    fn test_value_list() {
        let v = Value::from(vec![1i64, 2, 3]);
        let list = v.as_list().unwrap();
        assert_eq!(list.len(), 3);
        assert_eq!(list[0].as_int(), Some(1));
    }

    #[test]
    fn test_value_into_list() {
        let v = Value::from(42i64);
        let list = v.into_list();
        assert_eq!(list.len(), 1);

        let v2 = Value::Null;
        let list2 = v2.into_list();
        assert!(list2.is_empty());
    }

    #[test]
    fn test_value_type_name() {
        assert_eq!(Value::Null.type_name(), "null");
        assert_eq!(Value::from(42i64).type_name(), "int");
        assert_eq!(Value::from(3.14f64).type_name(), "float");
        assert_eq!(Value::from("hello").type_name(), "string");
    }

    #[test]
    fn test_value_to_string() {
        assert_eq!(Value::from(42i64).to_string_value(), "42");
        assert_eq!(Value::from(3.14f64).to_string_value(), "3.14");
        assert_eq!(Value::from("hello").to_string_value(), "hello");
    }
}
