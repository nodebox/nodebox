//! Desktop (macOS, Windows, Linux) implementation of the Platform trait.

use nodebox_core::platform::{
    DirectoryEntry, FileFilter, LogLevel, PlatformInfo, Platform, PlatformError, ProjectContext,
    RelativePath,
};
use std::path::{Path, PathBuf};

/// Desktop implementation of the Platform trait.
///
/// Uses native filesystem, rfd for dialogs, arboard for clipboard, and ureq for HTTP.
#[derive(Debug, Default)]
pub struct DesktopPlatform;

impl DesktopPlatform {
    /// Create a new DesktopPlatform instance.
    pub fn new() -> Self {
        Self
    }

    /// Get the library directory for the current platform.
    fn library_dir(&self) -> Result<PathBuf, PlatformError> {
        if let Some(proj_dirs) =
            directories::ProjectDirs::from("net", "nodebox", "NodeBox")
        {
            Ok(proj_dirs.data_dir().join("libraries"))
        } else {
            Err(PlatformError::Other(
                "Could not determine library directory".to_string(),
            ))
        }
    }

    /// Validate that a path is within the project directory and convert to RelativePath.
    ///
    /// This is a security function that ensures file access is sandboxed to the project.
    fn validate_within_project(
        ctx: &ProjectContext,
        path: &Path,
    ) -> Result<RelativePath, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;

        // Canonicalize both paths to resolve symlinks and normalize
        let canonical_root = root.canonicalize().map_err(|e| {
            PlatformError::IoError(format!("Failed to canonicalize project root: {}", e))
        })?;
        let canonical_path = path.canonicalize().map_err(|e| {
            PlatformError::IoError(format!("Failed to canonicalize selected path: {}", e))
        })?;

        // Check if the selected path is within the project root
        if canonical_path.starts_with(&canonical_root) {
            let relative = canonical_path
                .strip_prefix(&canonical_root)
                .map_err(|_| PlatformError::SandboxViolation)?;
            RelativePath::new(relative)
        } else {
            Err(PlatformError::SandboxViolation)
        }
    }

    /// Validate that a path (which may not exist yet) would be within the project directory.
    ///
    /// This is used for save dialogs where the file doesn't exist yet, so we can't canonicalize it.
    fn validate_save_path_within_project(
        ctx: &ProjectContext,
        path: &Path,
    ) -> Result<RelativePath, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;

        // Canonicalize the project root
        let canonical_root = root.canonicalize().map_err(|e| {
            PlatformError::IoError(format!("Failed to canonicalize project root: {}", e))
        })?;

        // For the save path, canonicalize the parent directory (which should exist)
        // and then append the filename
        if let Some(parent) = path.parent() {
            if let Some(file_name) = path.file_name() {
                // Try to canonicalize the parent. If it doesn't exist, try its parent, etc.
                let canonical_parent = if parent.exists() {
                    parent.canonicalize().map_err(|e| {
                        PlatformError::IoError(format!(
                            "Failed to canonicalize parent directory: {}",
                            e
                        ))
                    })?
                } else {
                    // Parent doesn't exist - this is a new nested directory
                    // Check if any ancestor is within the project
                    let mut ancestor = parent.to_path_buf();
                    loop {
                        if ancestor.exists() {
                            break ancestor.canonicalize().map_err(|e| {
                                PlatformError::IoError(format!(
                                    "Failed to canonicalize ancestor: {}",
                                    e
                                ))
                            })?;
                        }
                        if let Some(p) = ancestor.parent() {
                            ancestor = p.to_path_buf();
                        } else {
                            return Err(PlatformError::SandboxViolation);
                        }
                    }
                };

                // Check if the canonical parent is within or equals the project root
                if canonical_parent.starts_with(&canonical_root) {
                    // Build the relative path from project root to the save location
                    let relative_parent = if canonical_parent == canonical_root {
                        PathBuf::new()
                    } else {
                        canonical_parent
                            .strip_prefix(&canonical_root)
                            .map_err(|_| PlatformError::SandboxViolation)?
                            .to_path_buf()
                    };

                    // Append any non-existent directory components from the original path
                    let original_parent = parent.to_path_buf();
                    let existing_ancestor = if parent.exists() {
                        parent.canonicalize().ok()
                    } else {
                        let mut a = parent.to_path_buf();
                        while !a.exists() {
                            if let Some(p) = a.parent() {
                                a = p.to_path_buf();
                            } else {
                                break;
                            }
                        }
                        a.canonicalize().ok()
                    };

                    let full_relative = if let Some(existing) = existing_ancestor {
                        if let Ok(suffix) = original_parent.strip_prefix(
                            existing
                                .strip_prefix(&canonical_root)
                                .map(|p| canonical_root.join(p))
                                .unwrap_or(existing.clone()),
                        ) {
                            relative_parent.join(suffix).join(file_name)
                        } else {
                            relative_parent.join(file_name)
                        }
                    } else {
                        relative_parent.join(file_name)
                    };

                    RelativePath::new(full_relative)
                } else {
                    Err(PlatformError::SandboxViolation)
                }
            } else {
                Err(PlatformError::SandboxViolation)
            }
        } else {
            // Path has no parent - just a filename, which is fine
            RelativePath::new(path)
        }
    }
}

impl Platform for DesktopPlatform {
    fn platform_info(&self) -> PlatformInfo {
        PlatformInfo::current()
    }

    fn read_file(&self, ctx: &ProjectContext, path: &RelativePath) -> Result<Vec<u8>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let full_path = root.join(path.as_path());
        std::fs::read(&full_path).map_err(PlatformError::from)
    }

    fn write_file(
        &self,
        ctx: &ProjectContext,
        path: &RelativePath,
        data: &[u8],
    ) -> Result<(), PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let full_path = root.join(path.as_path());

        // Create parent directories if needed
        if let Some(parent) = full_path.parent() {
            std::fs::create_dir_all(parent)?;
        }

        std::fs::write(&full_path, data).map_err(PlatformError::from)
    }

    fn list_directory(
        &self,
        ctx: &ProjectContext,
        path: &RelativePath,
    ) -> Result<Vec<DirectoryEntry>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let full_path = root.join(path.as_path());

        let entries = std::fs::read_dir(&full_path)?
            .filter_map(|entry| {
                entry.ok().map(|e| {
                    DirectoryEntry::new(
                        e.file_name().to_string_lossy().to_string(),
                        e.file_type().map(|ft| ft.is_dir()).unwrap_or(false),
                    )
                })
            })
            .collect();

        Ok(entries)
    }

    fn read_text_file(&self, ctx: &ProjectContext, path: &str) -> Result<String, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let relative = RelativePath::new(path)?;
        let full_path = root.join(relative.as_path());
        let bytes = std::fs::read(&full_path)?;
        String::from_utf8(bytes).map_err(|_| PlatformError::IoError("Invalid UTF-8".to_string()))
    }

    fn read_binary_file(&self, ctx: &ProjectContext, path: &str) -> Result<Vec<u8>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let relative = RelativePath::new(path)?;
        let full_path = root.join(relative.as_path());
        std::fs::read(&full_path).map_err(PlatformError::from)
    }

    fn load_app_resource(&self, name: &str) -> Result<Vec<u8>, PlatformError> {
        // Locate resources relative to executable or in standard location
        let exe_dir = std::env::current_exe()
            .ok()
            .and_then(|p| p.parent().map(|p| p.to_path_buf()));

        let resource_dirs = [
            exe_dir.as_ref().map(|d| d.join("resources")),
            exe_dir.as_ref().map(|d| d.join("../Resources")), // macOS app bundle
            Some(PathBuf::from("resources")),                 // Development fallback
        ];

        for dir in resource_dirs.iter().flatten() {
            let path = dir.join(name);
            if path.exists() {
                return std::fs::read(&path).map_err(PlatformError::from);
            }
        }

        Err(PlatformError::NotFound)
    }

    fn read_project(&self, ctx: &ProjectContext) -> Result<Vec<u8>, PlatformError> {
        let path = ctx.project_path().ok_or(PlatformError::Unsupported)?;
        std::fs::read(&path).map_err(PlatformError::from)
    }

    fn write_project(&self, ctx: &ProjectContext, data: &[u8]) -> Result<(), PlatformError> {
        let path = ctx.project_path().ok_or(PlatformError::Unsupported)?;

        // Create parent directories if needed
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }

        std::fs::write(&path, data).map_err(PlatformError::from)
    }

    fn load_library(&self, name: &str) -> Result<Vec<u8>, PlatformError> {
        let library_dir = self.library_dir()?;
        let library_path = library_dir.join(format!("{}.ndbx", name));

        if !library_path.exists() {
            return Err(PlatformError::LibraryNotFound(name.to_string()));
        }

        std::fs::read(&library_path).map_err(PlatformError::from)
    }

    fn http_get(&self, url: &str) -> Result<Vec<u8>, PlatformError> {
        let response = ureq::get(url)
            .call()
            .map_err(|e| PlatformError::NetworkError(e.to_string()))?;

        let mut bytes = Vec::new();
        response
            .into_reader()
            .read_to_end(&mut bytes)
            .map_err(|e| PlatformError::NetworkError(e.to_string()))?;

        Ok(bytes)
    }

    fn show_open_project_dialog(
        &self,
        filters: &[FileFilter],
    ) -> Result<Option<PathBuf>, PlatformError> {
        let mut dialog = rfd::FileDialog::new();

        for filter in filters {
            let extensions: Vec<&str> = filter.extensions.iter().map(|s| s.as_str()).collect();
            dialog = dialog.add_filter(&filter.name, &extensions);
        }

        Ok(dialog.pick_file())
    }

    fn show_save_project_dialog(
        &self,
        filters: &[FileFilter],
        default_name: Option<&str>,
    ) -> Result<Option<PathBuf>, PlatformError> {
        let mut dialog = rfd::FileDialog::new();

        for filter in filters {
            let extensions: Vec<&str> = filter.extensions.iter().map(|s| s.as_str()).collect();
            dialog = dialog.add_filter(&filter.name, &extensions);
        }

        if let Some(name) = default_name {
            dialog = dialog.set_file_name(name);
        }

        Ok(dialog.save_file())
    }

    fn show_open_file_dialog(
        &self,
        ctx: &ProjectContext,
        filters: &[FileFilter],
    ) -> Result<Option<RelativePath>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let mut dialog = rfd::FileDialog::new();

        // Start in the project directory
        dialog = dialog.set_directory(root);

        for filter in filters {
            let extensions: Vec<&str> = filter.extensions.iter().map(|s| s.as_str()).collect();
            dialog = dialog.add_filter(&filter.name, &extensions);
        }

        match dialog.pick_file() {
            Some(path) => {
                // Validate that the selected file is within the project directory
                let relative = Self::validate_within_project(ctx, &path)?;
                Ok(Some(relative))
            }
            None => Ok(None),
        }
    }

    fn show_save_file_dialog(
        &self,
        ctx: &ProjectContext,
        filters: &[FileFilter],
        default_name: Option<&str>,
    ) -> Result<Option<RelativePath>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let mut dialog = rfd::FileDialog::new();

        // Start in the project directory
        dialog = dialog.set_directory(root);

        for filter in filters {
            let extensions: Vec<&str> = filter.extensions.iter().map(|s| s.as_str()).collect();
            dialog = dialog.add_filter(&filter.name, &extensions);
        }

        if let Some(name) = default_name {
            dialog = dialog.set_file_name(name);
        }

        match dialog.save_file() {
            Some(path) => {
                // Validate that the selected location is within the project directory
                let relative = Self::validate_save_path_within_project(ctx, &path)?;
                Ok(Some(relative))
            }
            None => Ok(None),
        }
    }

    fn show_select_folder_dialog(
        &self,
        ctx: &ProjectContext,
    ) -> Result<Option<RelativePath>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let mut dialog = rfd::FileDialog::new();

        // Start in the project directory
        dialog = dialog.set_directory(root);

        match dialog.pick_folder() {
            Some(path) => {
                // Validate that the selected folder is within the project directory
                let relative = Self::validate_within_project(ctx, &path)?;
                Ok(Some(relative))
            }
            None => Ok(None),
        }
    }

    fn show_confirm_dialog(&self, title: &str, message: &str) -> Result<bool, PlatformError> {
        let result = rfd::MessageDialog::new()
            .set_title(title)
            .set_description(message)
            .set_buttons(rfd::MessageButtons::OkCancel)
            .show();

        Ok(result == rfd::MessageDialogResult::Ok)
    }

    fn show_message_dialog(
        &self,
        title: &str,
        message: &str,
        buttons: &[&str],
    ) -> Result<Option<usize>, PlatformError> {
        // rfd doesn't support custom button labels directly
        // Map to available button types based on count
        let button_type = match buttons.len() {
            2 => rfd::MessageButtons::OkCancel,
            3 => rfd::MessageButtons::YesNoCancel,
            _ => rfd::MessageButtons::Ok,
        };

        let result = rfd::MessageDialog::new()
            .set_title(title)
            .set_description(message)
            .set_buttons(button_type)
            .show();

        // Map result back to button index
        let index = match result {
            rfd::MessageDialogResult::Ok | rfd::MessageDialogResult::Yes => Some(0),
            rfd::MessageDialogResult::No => Some(1),
            rfd::MessageDialogResult::Cancel => {
                if buttons.len() == 2 {
                    Some(1)
                } else {
                    Some(2)
                }
            }
            rfd::MessageDialogResult::Custom(_) => None,
        };

        Ok(index)
    }

    fn clipboard_read_text(&self) -> Result<Option<String>, PlatformError> {
        let mut clipboard =
            arboard::Clipboard::new().map_err(|e| PlatformError::Other(e.to_string()))?;

        match clipboard.get_text() {
            Ok(text) => Ok(Some(text)),
            Err(arboard::Error::ContentNotAvailable) => Ok(None),
            Err(e) => Err(PlatformError::Other(e.to_string())),
        }
    }

    fn clipboard_write_text(&self, text: &str) -> Result<(), PlatformError> {
        let mut clipboard =
            arboard::Clipboard::new().map_err(|e| PlatformError::Other(e.to_string()))?;

        clipboard
            .set_text(text)
            .map_err(|e| PlatformError::Other(e.to_string()))
    }

    fn log(&self, level: LogLevel, message: &str) {
        match level {
            LogLevel::Error => log::error!("{}", message),
            LogLevel::Warn => log::warn!("{}", message),
            LogLevel::Info => log::info!("{}", message),
            LogLevel::Debug => log::debug!("{}", message),
        }
    }

    fn performance_mark(&self, name: &str) {
        log::trace!("[PERF] {}", name);
    }

    fn performance_mark_with_details(&self, name: &str, details: &str) {
        log::trace!("[PERF] {} - {}", name, details);
    }

    fn get_config_dir(&self) -> Result<PathBuf, PlatformError> {
        if let Some(proj_dirs) =
            directories::ProjectDirs::from("net", "nodebox", "NodeBox")
        {
            Ok(proj_dirs.config_dir().to_path_buf())
        } else {
            Err(PlatformError::Other(
                "Could not determine config directory".to_string(),
            ))
        }
    }

    fn list_fonts(&self) -> Vec<String> {
        let source = font_kit::source::SystemSource::new();
        let mut families = source.all_families().unwrap_or_default();
        families.sort();
        families
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;

    fn create_test_context() -> (TempDir, ProjectContext) {
        let temp_dir = TempDir::new().unwrap();
        let ctx = ProjectContext::new(temp_dir.path(), "test.ndbx");
        (temp_dir, ctx)
    }

    #[test]
    fn test_platform_info() {
        let port = DesktopPlatform::new();
        let info = port.platform_info();

        assert!(info.has_filesystem);
        assert!(info.has_native_dialogs);
        assert!(!info.is_web);
    }

    #[test]
    fn test_read_write_file() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        let path = RelativePath::new("test.txt").unwrap();
        let data = b"Hello, World!";

        // Write file
        port.write_file(&ctx, &path, data).unwrap();

        // Read file back
        let read_data = port.read_file(&ctx, &path).unwrap();
        assert_eq!(read_data, data);
    }

    #[test]
    fn test_write_creates_directories() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        let path = RelativePath::new("subdir/nested/file.txt").unwrap();
        let data = b"nested content";

        port.write_file(&ctx, &path, data).unwrap();

        let read_data = port.read_file(&ctx, &path).unwrap();
        assert_eq!(read_data, data);
    }

    #[test]
    fn test_read_nonexistent_file() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        let path = RelativePath::new("nonexistent.txt").unwrap();
        let result = port.read_file(&ctx, &path);

        assert!(matches!(result, Err(PlatformError::NotFound)));
    }

    #[test]
    fn test_list_directory() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        // Create some files and directories
        port.write_file(&ctx, &RelativePath::new("file1.txt").unwrap(), b"1")
            .unwrap();
        port.write_file(&ctx, &RelativePath::new("file2.txt").unwrap(), b"2")
            .unwrap();
        port.write_file(&ctx, &RelativePath::new("subdir/nested.txt").unwrap(), b"3")
            .unwrap();

        // List root directory
        let root = RelativePath::new("").unwrap();
        let entries = port.list_directory(&ctx, &root).unwrap();

        let names: Vec<_> = entries.iter().map(|e| &e.name).collect();
        assert!(names.contains(&&"file1.txt".to_string()));
        assert!(names.contains(&&"file2.txt".to_string()));
        assert!(names.contains(&&"subdir".to_string()));

        // Check that subdir is marked as directory
        let subdir = entries.iter().find(|e| e.name == "subdir").unwrap();
        assert!(subdir.is_directory);
    }

    #[test]
    fn test_read_write_project() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        let data = b"<ndbx>project content</ndbx>";

        port.write_project(&ctx, data).unwrap();

        let read_data = port.read_project(&ctx).unwrap();
        assert_eq!(read_data, data);
    }

    #[test]
    fn test_load_library_not_found() {
        let port = DesktopPlatform::new();
        let result = port.load_library("nonexistent_library_xyz");

        assert!(matches!(result, Err(PlatformError::LibraryNotFound(_))));
    }

    #[test]
    fn test_get_config_dir() {
        let port = DesktopPlatform::new();
        let result = port.get_config_dir();

        assert!(result.is_ok());
        let path = result.unwrap();
        // Should end with nodebox or NodeBox
        let path_str = path.to_string_lossy().to_lowercase();
        assert!(path_str.contains("nodebox"));
    }

    #[test]
    fn test_log_levels() {
        let port = DesktopPlatform::new();

        // Just verify these don't panic
        port.log(LogLevel::Error, "test error");
        port.log(LogLevel::Warn, "test warning");
        port.log(LogLevel::Info, "test info");
        port.log(LogLevel::Debug, "test debug");
    }

    #[test]
    fn test_performance_marks() {
        let port = DesktopPlatform::new();

        // Just verify these don't panic
        port.performance_mark("test-mark");
        port.performance_mark_with_details("test-mark", r#"{"key": "value"}"#);
    }

    #[test]
    fn test_validate_within_project_valid_path() {
        let (_temp_dir, ctx) = create_test_context();

        // Create a file inside the project
        let file_path = ctx.root.as_ref().unwrap().join("test_file.txt");
        std::fs::write(&file_path, "test content").unwrap();

        // Validation should succeed
        let result = DesktopPlatform::validate_within_project(&ctx, &file_path);
        assert!(result.is_ok());
        let relative = result.unwrap();
        assert_eq!(relative.as_path(), Path::new("test_file.txt"));
    }

    #[test]
    fn test_validate_within_project_nested_path() {
        let (_temp_dir, ctx) = create_test_context();

        // Create a nested file inside the project
        let nested_dir = ctx.root.as_ref().unwrap().join("subdir");
        std::fs::create_dir_all(&nested_dir).unwrap();
        let file_path = nested_dir.join("nested_file.txt");
        std::fs::write(&file_path, "nested content").unwrap();

        // Validation should succeed
        let result = DesktopPlatform::validate_within_project(&ctx, &file_path);
        assert!(result.is_ok());
        let relative = result.unwrap();
        assert_eq!(relative.as_path(), Path::new("subdir/nested_file.txt"));
    }

    #[test]
    fn test_validate_within_project_outside_path() {
        let temp_dir = TempDir::new().unwrap();
        let project_dir = temp_dir.path().join("project");
        std::fs::create_dir_all(&project_dir).unwrap();
        let ctx = ProjectContext::new(&project_dir, "test.ndbx");

        // Create a file outside the project directory (sibling)
        let outside_file = temp_dir.path().join("outside.txt");
        std::fs::write(&outside_file, "outside content").unwrap();

        // Validation should fail with SandboxViolation
        let result = DesktopPlatform::validate_within_project(&ctx, &outside_file);
        assert!(matches!(result, Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_validate_within_project_parent_traversal() {
        let temp_dir = TempDir::new().unwrap();
        let project_dir = temp_dir.path().join("project");
        std::fs::create_dir_all(&project_dir).unwrap();
        let ctx = ProjectContext::new(&project_dir, "test.ndbx");

        // Create a file at the parent level
        let parent_file = temp_dir.path().join("parent.txt");
        std::fs::write(&parent_file, "parent content").unwrap();

        // Validation should fail
        let result = DesktopPlatform::validate_within_project(&ctx, &parent_file);
        assert!(matches!(result, Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_validate_save_path_within_project_existing_file() {
        let (_temp_dir, ctx) = create_test_context();

        // Save path to an existing location (the file doesn't exist but parent does)
        let save_path = ctx.root.as_ref().unwrap().join("new_file.txt");

        let result = DesktopPlatform::validate_save_path_within_project(&ctx, &save_path);
        assert!(result.is_ok());
        let relative = result.unwrap();
        assert_eq!(relative.as_path(), Path::new("new_file.txt"));
    }

    #[test]
    fn test_validate_save_path_within_project_nested() {
        let (_temp_dir, ctx) = create_test_context();

        // Create a subdirectory
        let subdir = ctx.root.as_ref().unwrap().join("assets");
        std::fs::create_dir_all(&subdir).unwrap();

        // Save path to the existing subdirectory
        let save_path = subdir.join("new_image.png");

        let result = DesktopPlatform::validate_save_path_within_project(&ctx, &save_path);
        assert!(result.is_ok());
        let relative = result.unwrap();
        assert_eq!(relative.as_path(), Path::new("assets/new_image.png"));
    }

    #[test]
    fn test_validate_save_path_outside_project() {
        let temp_dir = TempDir::new().unwrap();
        let project_dir = temp_dir.path().join("project");
        std::fs::create_dir_all(&project_dir).unwrap();
        let ctx = ProjectContext::new(&project_dir, "test.ndbx");

        // Try to save outside the project
        let outside_path = temp_dir.path().join("outside.txt");

        let result = DesktopPlatform::validate_save_path_within_project(&ctx, &outside_path);
        assert!(matches!(result, Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_read_text_file() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        // Create a text file
        let file_path = ctx.root.as_ref().unwrap().join("test.txt");
        std::fs::write(&file_path, "Hello, World!").unwrap();

        // Read it through the port
        let content = port.read_text_file(&ctx, "test.txt").unwrap();
        assert_eq!(content, "Hello, World!");
    }

    #[test]
    fn test_read_text_file_nested() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        // Create a nested text file
        let subdir = ctx.root.as_ref().unwrap().join("assets");
        std::fs::create_dir_all(&subdir).unwrap();
        std::fs::write(subdir.join("data.txt"), "nested content").unwrap();

        // Read it through the port
        let content = port.read_text_file(&ctx, "assets/data.txt").unwrap();
        assert_eq!(content, "nested content");
    }

    #[test]
    fn test_read_text_file_invalid_utf8() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        // Create a binary file (invalid UTF-8)
        let file_path = ctx.root.as_ref().unwrap().join("binary.dat");
        std::fs::write(&file_path, &[0xFF, 0xFE, 0x00, 0x01]).unwrap();

        // Should fail with IoError for invalid UTF-8
        let result = port.read_text_file(&ctx, "binary.dat");
        assert!(matches!(result, Err(PlatformError::IoError(_))));
    }

    #[test]
    fn test_read_text_file_rejects_sandbox_violation() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        // Try to read with ".." in path
        let result = port.read_text_file(&ctx, "../escape.txt");
        assert!(matches!(result, Err(PlatformError::SandboxViolation)));

        // Try with absolute path
        let result = port.read_text_file(&ctx, "/etc/passwd");
        assert!(matches!(result, Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_read_text_file_unsaved_project() {
        let port = DesktopPlatform::new();
        let ctx = ProjectContext::new_unsaved();

        // Should fail because project is unsaved
        let result = port.read_text_file(&ctx, "test.txt");
        assert!(matches!(result, Err(PlatformError::Unsupported)));
    }

    #[test]
    fn test_read_binary_file() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        // Create a binary file
        let data = vec![0x89, 0x50, 0x4E, 0x47]; // PNG magic bytes
        let file_path = ctx.root.as_ref().unwrap().join("image.png");
        std::fs::write(&file_path, &data).unwrap();

        // Read it through the port
        let content = port.read_binary_file(&ctx, "image.png").unwrap();
        assert_eq!(content, data);
    }

    #[test]
    fn test_read_binary_file_rejects_sandbox_violation() {
        let (_temp_dir, ctx) = create_test_context();
        let port = DesktopPlatform::new();

        // Try to read with ".." in path
        let result = port.read_binary_file(&ctx, "../escape.bin");
        assert!(matches!(result, Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_load_app_resource_not_found() {
        let port = DesktopPlatform::new();

        // Resources that don't exist should return NotFound
        let result = port.load_app_resource("nonexistent/icon.png");
        assert!(matches!(result, Err(PlatformError::NotFound)));
    }
}
