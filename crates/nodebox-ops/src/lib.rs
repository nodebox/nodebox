//! Geometry operations for NodeBox.
//!
//! This crate provides functions for generating and manipulating geometry,
//! corresponding to the core vector operations in NodeBox.
//!
//! # Modules
//!
//! - [`generators`] - Functions that create new geometry (ellipse, rect, line, etc.)
//! - [`filters`] - Functions that transform existing geometry (align, colorize, fit, etc.)

pub mod generators;
pub mod filters;

pub use generators::*;
pub use filters::*;
