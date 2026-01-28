//! NodeBox Core Library
//!
//! This crate provides the core types and traits for NodeBox:
//! - Geometry primitives (Point, Rect, Path, etc.)
//! - Node graph model (Node, Port, Connection)
//! - Runtime value types

pub mod geometry;
pub mod node;
pub mod value;

// Re-export commonly used types at the crate root
pub use geometry::{
    Point, PointType, PathPoint,
    Rect,
    Transform,
    Color,
    Contour,
    Path,
    Geometry, Grob,
    Text, TextAlign,
    Canvas,
};
pub use value::Value;
