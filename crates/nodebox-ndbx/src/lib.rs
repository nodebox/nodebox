//! NDBX file format parser for NodeBox.
//!
//! This crate parses `.ndbx` files (XML-based) into NodeBox's internal
//! node graph representation.
//!
//! # Example
//!
//! ```no_run
//! use nodebox_ndbx::parse_file;
//!
//! let library = parse_file("examples/my_project.ndbx").unwrap();
//! println!("Loaded library: {}", library.name);
//! ```

mod parser;
mod error;

pub use error::{NdbxError, Result};
pub use parser::{parse, parse_file};
