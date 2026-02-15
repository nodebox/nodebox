//! Version upgrades for .ndbx files.
//!
//! This module handles upgrading older .ndbx file formats to the current version.
//! Old versions (< 21) are loaded best-effort with a warning.

use crate::node::NodeLibrary;

use super::error::NdbxError;

/// The current format version used when saving .ndbx files.
pub const CURRENT_FORMAT_VERSION: u32 = 22;

/// The minimum format version we can load without warnings (Java's latest).
pub const MIN_SUPPORTED_VERSION: u32 = 21;

/// Result of upgrading a NodeLibrary, with optional warnings.
#[derive(Debug, Default)]
pub struct UpgradeResult {
    /// Non-fatal warnings encountered during upgrade.
    pub warnings: Vec<String>,
}

/// Upgrades a NodeLibrary to the current format version.
///
/// Old versions (< 21) are upgraded best-effort with a warning.
/// Future versions (> current) return an error since they may contain
/// structures we cannot parse.
pub fn upgrade(library: &mut NodeLibrary) -> Result<UpgradeResult, NdbxError> {
    let mut result = UpgradeResult::default();

    match library.format_version {
        v if v < MIN_SUPPORTED_VERSION => {
            result.warnings.push(format!(
                "This file uses format version {}, which is older than the supported \
                 version {}. Some features may not work correctly.",
                v, MIN_SUPPORTED_VERSION
            ));
            library.format_version = CURRENT_FORMAT_VERSION;
            Ok(result)
        }
        21 => {
            // Upgrade from Java version 21 to Rust version 22
            // Currently a no-op (format is compatible)
            library.format_version = CURRENT_FORMAT_VERSION;
            Ok(result)
        }
        22 => Ok(result), // Already current
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

        let result = upgrade(&mut library).unwrap();
        assert_eq!(library.format_version, 22);
        assert!(result.warnings.is_empty());
    }

    #[test]
    fn test_upgrade_v22_no_change() {
        let mut library = NodeLibrary::default();
        library.format_version = 22;

        let result = upgrade(&mut library).unwrap();
        assert_eq!(library.format_version, 22);
        assert!(result.warnings.is_empty());
    }

    #[test]
    fn test_upgrade_old_version_warns() {
        let mut library = NodeLibrary::default();
        library.format_version = 20;

        let result = upgrade(&mut library).unwrap();
        assert!(!result.warnings.is_empty(), "Old version should produce a warning");
        assert_eq!(library.format_version, CURRENT_FORMAT_VERSION);
    }

    #[test]
    fn test_upgrade_very_old_version_warns() {
        let mut library = NodeLibrary::default();
        library.format_version = 17;

        let result = upgrade(&mut library).unwrap();
        assert!(!result.warnings.is_empty());
        assert!(result.warnings[0].contains("17"));
        assert_eq!(library.format_version, CURRENT_FORMAT_VERSION);
    }

    #[test]
    fn test_upgrade_future_version_error() {
        let mut library = NodeLibrary::default();
        library.format_version = 99;

        let result = upgrade(&mut library);
        assert!(result.is_err());
    }
}
