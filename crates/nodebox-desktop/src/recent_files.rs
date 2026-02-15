//! Recent files management for the "Open Recent" menu functionality.

use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// Maximum number of recent files to store.
const MAX_RECENT_FILES: usize = 10;

/// Recent files storage filename.
const RECENT_FILES_FILENAME: &str = "recent_files.json";

/// Manages a list of recently opened files.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RecentFiles {
    files: Vec<PathBuf>,
}

impl Default for RecentFiles {
    fn default() -> Self {
        Self::new()
    }
}

impl RecentFiles {
    /// Create a new empty recent files list.
    pub fn new() -> Self {
        Self { files: Vec::new() }
    }

    /// Load recent files from disk, filtering out non-existent files.
    pub fn load() -> Self {
        let Some(path) = Self::config_path() else {
            return Self::new();
        };

        let Ok(contents) = std::fs::read_to_string(&path) else {
            return Self::new();
        };

        let Ok(mut recent) = serde_json::from_str::<RecentFiles>(&contents) else {
            return Self::new();
        };

        // Filter out non-existent files
        recent.files.retain(|p| p.exists());

        recent
    }

    /// Save recent files to disk.
    pub fn save(&self) {
        let Some(path) = Self::config_path() else {
            log::warn!("Could not determine config path for recent files");
            return;
        };

        // Ensure parent directory exists
        if let Some(parent) = path.parent() {
            if let Err(e) = std::fs::create_dir_all(parent) {
                log::warn!("Failed to create config directory: {}", e);
                return;
            }
        }

        let Ok(json) = serde_json::to_string_pretty(&self) else {
            log::warn!("Failed to serialize recent files");
            return;
        };

        if let Err(e) = std::fs::write(&path, json) {
            log::warn!("Failed to save recent files: {}", e);
        }
    }

    /// Add a file to the recent files list.
    /// If the file already exists in the list, it's moved to the top.
    /// The list is trimmed to MAX_RECENT_FILES entries.
    pub fn add_file(&mut self, path: PathBuf) {
        // Canonicalize path if possible for consistent comparison
        let path = path.canonicalize().unwrap_or(path);

        // Remove existing entry if present
        self.files.retain(|p| {
            p.canonicalize().unwrap_or_else(|_| p.clone()) != path
        });

        // Add to the front
        self.files.insert(0, path);

        // Trim to max size
        self.files.truncate(MAX_RECENT_FILES);
    }

    /// Get the list of recent files, filtering out non-existent files.
    pub fn files(&self) -> Vec<PathBuf> {
        self.files.iter().filter(|p| p.exists()).cloned().collect()
    }

    /// Clear all recent files.
    pub fn clear(&mut self) {
        self.files.clear();
    }

    /// Get the path to the config file.
    fn config_path() -> Option<PathBuf> {
        let proj_dirs = directories::ProjectDirs::from("net", "nodebox", "NodeBox")?;
        Some(proj_dirs.config_dir().join(RECENT_FILES_FILENAME))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs::File;
    use tempfile::tempdir;

    #[test]
    fn test_add_file_moves_to_top() {
        let dir = tempdir().unwrap();
        let file1 = dir.path().join("file1.ndbx");
        let file2 = dir.path().join("file2.ndbx");
        let file3 = dir.path().join("file3.ndbx");

        // Create the files so they exist
        File::create(&file1).unwrap();
        File::create(&file2).unwrap();
        File::create(&file3).unwrap();

        // Canonicalize for comparison (handles /var vs /private/var on macOS)
        let file1_canon = file1.canonicalize().unwrap();
        let file2_canon = file2.canonicalize().unwrap();
        let file3_canon = file3.canonicalize().unwrap();

        let mut recent = RecentFiles::new();
        recent.add_file(file1.clone());
        recent.add_file(file2.clone());
        recent.add_file(file3.clone());

        // file3 should be at top
        let files = recent.files();
        assert_eq!(files[0], file3_canon);
        assert_eq!(files[1], file2_canon);
        assert_eq!(files[2], file1_canon);

        // Adding file1 again should move it to top
        recent.add_file(file1.clone());
        let files = recent.files();
        assert_eq!(files[0], file1_canon);
        assert_eq!(files[1], file3_canon);
        assert_eq!(files[2], file2_canon);
    }

    #[test]
    fn test_max_files_limit() {
        let dir = tempdir().unwrap();
        let mut recent = RecentFiles::new();

        // Add more than MAX_RECENT_FILES
        for i in 0..15 {
            let path = dir.path().join(format!("file{}.ndbx", i));
            File::create(&path).unwrap();
            recent.add_file(path);
        }

        assert_eq!(recent.files().len(), MAX_RECENT_FILES);
    }

    #[test]
    fn test_clear() {
        let dir = tempdir().unwrap();
        let file1 = dir.path().join("file1.ndbx");
        File::create(&file1).unwrap();

        let mut recent = RecentFiles::new();
        recent.add_file(file1);
        assert_eq!(recent.files().len(), 1);

        recent.clear();
        assert_eq!(recent.files().len(), 0);
    }

    #[test]
    fn test_nonexistent_files_filtered() {
        let dir = tempdir().unwrap();
        let file1 = dir.path().join("file1.ndbx");
        let file2 = dir.path().join("file2.ndbx");

        // Only create file1
        File::create(&file1).unwrap();

        let mut recent = RecentFiles::new();
        recent.files = vec![file1.clone(), file2.clone()];

        // files() should filter out file2
        let files = recent.files();
        assert_eq!(files.len(), 1);
        assert_eq!(files[0], file1);
    }
}
