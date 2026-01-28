//! Error types for NDBX parsing.

use thiserror::Error;

/// Result type for NDBX operations.
pub type Result<T> = std::result::Result<T, NdbxError>;

/// Errors that can occur when parsing NDBX files.
#[derive(Debug, Error)]
pub enum NdbxError {
    /// XML parsing error.
    #[error("XML parsing error: {0}")]
    Xml(#[from] quick_xml::Error),

    /// I/O error when reading file.
    #[error("I/O error: {0}")]
    Io(#[from] std::io::Error),

    /// Invalid or missing attribute.
    #[error("Invalid attribute '{name}' on element '{element}': {reason}")]
    InvalidAttribute {
        element: String,
        name: String,
        reason: String,
    },

    /// Missing required attribute.
    #[error("Missing required attribute '{name}' on element '{element}'")]
    MissingAttribute { element: String, name: String },

    /// Unexpected element.
    #[error("Unexpected element '{found}' in '{parent}'")]
    UnexpectedElement { found: String, parent: String },

    /// Invalid format version.
    #[error("Unsupported format version: {0}")]
    UnsupportedVersion(u32),

    /// Parse error for values.
    #[error("Failed to parse '{value}' as {expected_type}: {reason}")]
    ParseValue {
        value: String,
        expected_type: String,
        reason: String,
    },

    /// UTF-8 decoding error.
    #[error("UTF-8 decoding error: {0}")]
    Utf8(#[from] std::str::Utf8Error),
}

impl NdbxError {
    /// Creates an invalid attribute error.
    pub fn invalid_attr(element: &str, name: &str, reason: &str) -> Self {
        NdbxError::InvalidAttribute {
            element: element.to_string(),
            name: name.to_string(),
            reason: reason.to_string(),
        }
    }

    /// Creates a missing attribute error.
    pub fn missing_attr(element: &str, name: &str) -> Self {
        NdbxError::MissingAttribute {
            element: element.to_string(),
            name: name.to_string(),
        }
    }

    /// Creates a parse value error.
    pub fn parse_value(value: &str, expected_type: &str, reason: &str) -> Self {
        NdbxError::ParseValue {
            value: value.to_string(),
            expected_type: expected_type.to_string(),
            reason: reason.to_string(),
        }
    }
}
