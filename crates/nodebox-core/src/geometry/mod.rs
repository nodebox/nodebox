//! Geometry primitives for NodeBox
//!
//! This module contains the core geometric types:
//! - [`Point`] - 2D point with x, y coordinates
//! - [`Rect`] - Rectangle with position and size
//! - [`Transform`] - 2D affine transformation matrix
//! - [`Color`] - RGBA color
//! - [`Contour`] - A sequence of points forming a path segment
//! - [`Path`] - A shape made of contours with fill/stroke
//! - [`Geometry`] - A collection of paths
//! - [`Text`] - Text with font and styling
//! - [`Canvas`] - A drawing surface containing graphic objects
//! - [`font`] - Font loading and text-to-path conversion

mod point;
mod rect;
mod transform;
mod color;
mod contour;
mod path;
mod grob;
mod text;
mod canvas;
pub mod font;

pub use point::{Point, PointType, PathPoint};
pub use rect::Rect;
pub use transform::Transform;
pub use color::Color;
pub use contour::{Contour, Segment};
pub use path::Path;
pub use grob::{Geometry, Grob};
pub use text::{Text, TextAlign};
pub use canvas::Canvas;
