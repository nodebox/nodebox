//! Version upgrades for .ndbx files.
//!
//! This module handles upgrading older .ndbx file formats to the current version.
//! Currently supports upgrading from Java's version 21 to Rust's version 22.

use nodebox_core::node::NodeLibrary;

use crate::error::NdbxError;

/// The current format version used when saving .ndbx files.
pub const CURRENT_FORMAT_VERSION: u32 = 22;

/// The minimum format version we can load (Java's latest).
pub const MIN_SUPPORTED_VERSION: u32 = 21;

/// Upgrades a NodeLibrary to the current format version.
///
/// # Errors
///
/// Returns an error if the format version is too old (< 21) or too new (> 22).
pub fn upgrade(library: &mut NodeLibrary) -> Result<(), NdbxError> {
    match library.format_version {
        v if v < MIN_SUPPORTED_VERSION => Err(NdbxError::UnsupportedVersion(v)),
        21 => {
            // Upgrade from Java version 21 to Rust version 22
            // Currently a no-op (format is compatible)
            library.format_version = CURRENT_FORMAT_VERSION;
            Ok(())
        }
        22 => Ok(()), // Already current
        v => Err(NdbxError::UnsupportedVersion(v)),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_upgrade_v21_to_v22() {
        let mut library = NodeLibrary::default();
        library.format_version = 21;

        upgrade(&mut library).unwrap();
        assert_eq!(library.format_version, 22);
    }

    #[test]
    fn test_upgrade_v22_no_change() {
        let mut library = NodeLibrary::default();
        library.format_version = 22;

        upgrade(&mut library).unwrap();
        assert_eq!(library.format_version, 22);
    }

    #[test]
    fn test_upgrade_old_version_error() {
        let mut library = NodeLibrary::default();
        library.format_version = 20;

        let result = upgrade(&mut library);
        assert!(result.is_err());
    }

    #[test]
    fn test_upgrade_future_version_error() {
        let mut library = NodeLibrary::default();
        library.format_version = 99;

        let result = upgrade(&mut library);
        assert!(result.is_err());
    }
}
