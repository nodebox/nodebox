//! NDBX file format parser and serializer for NodeBox.
//!
//! Parses `.ndbx` files (XML-based) into NodeBox's internal
//! node graph representation, and serializes them back to XML.

mod error;
mod parser;
mod serializer;
mod upgrades;

pub use error::{NdbxError, Result};
pub use parser::{parse, parse_file, parse_file_with_warnings};
pub use serializer::{serialize, serialize_to_file};
pub use upgrades::{upgrade, UpgradeResult, CURRENT_FORMAT_VERSION, MIN_SUPPORTED_VERSION};
