//! NDBX file format parser and serializer for NodeBox.
//!
//! This crate parses `.ndbx` files (XML-based) into NodeBox's internal
//! node graph representation, and serializes them back to XML.
//!
//! # Example
//!
//! ```no_run
//! use nodebox_ndbx::{parse_file, serialize_to_file};
//!
//! let library = parse_file("examples/my_project.ndbx").unwrap();
//! println!("Loaded library: {}", library.name);
//!
//! // Save the library back to a file
//! serialize_to_file(&library, "output.ndbx").unwrap();
//! ```

mod error;
mod parser;
mod serializer;
mod upgrades;

pub use error::{NdbxError, Result};
pub use parser::{parse, parse_file, parse_file_with_warnings};
pub use serializer::{serialize, serialize_to_file};
pub use upgrades::{upgrade, UpgradeResult, CURRENT_FORMAT_VERSION, MIN_SUPPORTED_VERSION};
