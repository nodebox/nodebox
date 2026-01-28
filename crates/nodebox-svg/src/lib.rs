//! SVG rendering for NodeBox.
//!
//! This crate converts NodeBox geometry to SVG format.
//!
//! # Example
//!
//! ```
//! use nodebox_core::geometry::{Path, Color};
//! use nodebox_svg::render_to_svg;
//!
//! let path = Path::ellipse(100.0, 100.0, 80.0, 80.0);
//! let svg = render_to_svg(&[path], 200.0, 200.0);
//! println!("{}", svg);
//! ```

mod renderer;

pub use renderer::*;
