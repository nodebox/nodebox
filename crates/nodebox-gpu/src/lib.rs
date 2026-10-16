//! A small GPU renderer for NodeBox paths.
//!
//! Fills, and strokes expanded to outlines with kurbo, are drawn straight from
//! their quadratic curves with Eric Lengyel's Slug algorithm ("GPU-Centered
//! Font Rendering Directly from Glyph Outlines", JCGT 2017). The shader comes
//! from Frederik's bybike experiment, generalised from glyphs to any path.
//!
//! Paths that share a shape share its curves: each distinct shape goes into
//! the curve atlas once, and every path that draws it is one instance with its
//! own placement and color. Everything is drawn in one instanced draw call, in
//! paint order.
//!
//! [`prepare`] does the CPU work for a list of paths; [`Renderer`] uploads it
//! and draws it with any view.

mod prepare;
mod renderer;

pub use prepare::{prepare, Prepared};
pub use renderer::{Renderer, TooLarge, View};
