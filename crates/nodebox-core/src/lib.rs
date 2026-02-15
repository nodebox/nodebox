//! NodeBox Core Library
//!
//! This crate provides the core types, operations, and platform abstraction for NodeBox:
//! - Geometry primitives (Point, Rect, Path, etc.)
//! - Node graph model (Node, Port, Connection)
//! - Runtime value types
//! - Geometry operations (generators, filters, math, etc.)
//! - NDBX file format (parse/serialize)
//! - SVG rendering
//! - Platform abstraction (Platform trait)

pub mod geometry;
pub mod node;
pub mod value;
pub mod ops;
pub mod ndbx;
pub mod svg;
pub mod platform;

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
