//! Operations for NodeBox.
//!
//! This crate provides functions for generating and manipulating geometry,
//! as well as math, list, and string operations.
//!
//! # Modules
//!
//! - [`generators`] - Functions that create new geometry (ellipse, rect, line, etc.)
//! - [`filters`] - Functions that transform existing geometry (align, colorize, fit, etc.)
//! - [`math`] - Mathematical operations (arithmetic, trigonometry, random, etc.)
//! - [`list`] - List manipulation operations (sort, filter, combine, etc.)
//! - [`string`] - String manipulation operations (case, split, format, etc.)
//! - [`parallel`] - Parallel versions of operations using Rayon

pub mod generators;
pub mod filters;
pub mod math;
pub mod list;
pub mod string;
pub mod parallel;

pub use generators::*;
pub use filters::*;
