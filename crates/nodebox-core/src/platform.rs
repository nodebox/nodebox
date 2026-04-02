//! Platform abstraction layer for NodeBox.
//!
//! The Platform system provides a unified interface for platform-specific I/O operations,
//! enabling the same core logic to run across desktop (macOS, Windows, Linux),
//! web (WASM), and mobile (iOS, Android) platforms.
//!
//! # Design Principles
//!
//! 1. **Single trait with runtime capability checking** - One `Platform` trait;
//!    unsupported operations return `Err(PlatformError::Unsupported)`
//! 2. **Synchronous API** - All operations are blocking
//! 3. **Explicit context passing** - `ProjectContext` passed to operations; no global state
//! 4. **Sandboxed file access** - Files accessible only within project directory,
//!    its subdirectories, and explicit library paths

use std::path::{Path, PathBuf};
use thiserror::Error;

/// Information about the current platform.
#[derive(Debug, Clone)]
pub struct PlatformInfo {
    /// Operating system name: "macos", "windows", "linux", "web", "ios", "android"
    pub os_name: String,
    /// Whether running in a web browser (WASM)
    pub is_web: bool,
    /// Whether running on a mobile platform (iOS/Android)
    pub is_mobile: bool,
    /// Whether native filesystem access is available
    pub has_filesystem: bool,
    /// Whether OS-native dialogs are available
    pub has_native_dialogs: bool,
}

impl PlatformInfo {
    /// Create platform info for the current platform.
    #[cfg(target_os = "macos")]
    pub fn current() -> Self {
        Self {
            os_name: "macos".to_string(),
            is_web: false,
            is_mobile: false,
            has_filesystem: true,
            has_native_dialogs: true,
        }
    }

    #[cfg(target_os = "windows")]
    pub fn current() -> Self {
        Self {
            os_name: "windows".to_string(),
            is_web: false,
            is_mobile: false,
            has_filesystem: true,
            has_native_dialogs: true,
        }
    }

    #[cfg(target_os = "linux")]
    pub fn current() -> Self {
        Self {
            os_name: "linux".to_string(),
            is_web: false,
            is_mobile: false,
            has_filesystem: true,
            has_native_dialogs: true,
        }
    }

    #[cfg(target_arch = "wasm32")]
    pub fn current() -> Self {
        Self {
            os_name: "web".to_string(),
            is_web: true,
            is_mobile: false,
            has_filesystem: false,
            has_native_dialogs: false,
        }
    }

    #[cfg(target_os = "ios")]
    pub fn current() -> Self {
        Self {
            os_name: "ios".to_string(),
            is_web: false,
            is_mobile: true,
            has_filesystem: true,
            has_native_dialogs: true,
        }
    }

    #[cfg(target_os = "android")]
    pub fn current() -> Self {
        Self {
            os_name: "android".to_string(),
            is_web: false,
            is_mobile: true,
            has_filesystem: true,
            has_native_dialogs: true,
        }
    }

    // Fallback for other platforms
    #[cfg(not(any(
        target_os = "macos",
        target_os = "windows",
        target_os = "linux",
        target_os = "ios",
        target_os = "android",
        target_arch = "wasm32"
    )))]
    pub fn current() -> Self {
        Self {
            os_name: "unknown".to_string(),
            is_web: false,
            is_mobile: false,
            has_filesystem: true,
            has_native_dialogs: false,
        }
    }
}

/// Context for project-relative file operations.
#[derive(Debug, Clone)]
pub struct ProjectContext {
    /// Root directory of the project (contains the .ndbx file).
    /// None for unsaved projects.
    pub root: Option<PathBuf>,
    /// Name of the project file within root.
    /// None for unsaved projects.
    pub project_file: Option<String>,
    /// Current frame number for animation.
    pub frame: u32,
}

impl ProjectContext {
    /// Create context for a new unsaved project.
    pub fn new_unsaved() -> Self {
        Self {
            root: None,
            project_file: None,
            frame: 1,
        }
    }

    /// Create context for a saved project.
    pub fn new(root: impl Into<PathBuf>, project_file: impl Into<String>) -> Self {
        Self {
            root: Some(root.into()),
            project_file: Some(project_file.into()),
            frame: 1,
        }
    }

    /// Check if this project has been saved.
    pub fn is_saved(&self) -> bool {
        self.root.is_some()
    }

    /// Get the full path to the project file.
    /// Returns None for unsaved projects.
    pub fn project_path(&self) -> Option<PathBuf> {
        match (&self.root, &self.project_file) {
            (Some(root), Some(file)) => Some(root.join(file)),
            _ => None,
        }
    }
}

/// A path relative to project root that cannot escape the project directory.
///
/// This type ensures that file access is sandboxed within the project directory.
/// Paths containing ".." components or starting with "/" are rejected.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RelativePath {
    path: PathBuf,
}

impl RelativePath {
    /// Create a new relative path.
    ///
    /// # Errors
    ///
    /// Returns `PlatformError::SandboxViolation` if:
    /// - The path contains ".." components
    /// - The path starts with "/" (absolute path)
    /// - The path starts with a Windows drive letter (e.g., "C:")
    pub fn new(path: impl AsRef<Path>) -> Result<Self, PlatformError> {
        let path = path.as_ref();

        // Check for absolute paths
        if path.is_absolute() {
            return Err(PlatformError::SandboxViolation);
        }

        // Check for Windows-style absolute paths (C:\, D:\, etc.)
        if let Some(s) = path.to_str() {
            if s.len() >= 2 {
                let chars: Vec<char> = s.chars().take(2).collect();
                if chars[0].is_ascii_alphabetic() && chars[1] == ':' {
                    return Err(PlatformError::SandboxViolation);
                }
            }
        }

        // Check for ".." components that could escape the sandbox
        for component in path.components() {
            if let std::path::Component::ParentDir = component {
                return Err(PlatformError::SandboxViolation);
            }
        }

        Ok(Self {
            path: path.to_path_buf(),
        })
    }

    /// Get the path as a `Path` reference.
    pub fn as_path(&self) -> &Path {
        &self.path
    }

    /// Join this relative path with another path component.
    ///
    /// # Errors
    ///
    /// Returns `PlatformError::SandboxViolation` if the resulting path would escape the sandbox.
    pub fn join(&self, path: impl AsRef<Path>) -> Result<Self, PlatformError> {
        let joined = self.path.join(path);
        Self::new(joined)
    }
}

impl AsRef<Path> for RelativePath {
    fn as_ref(&self) -> &Path {
        &self.path
    }
}

impl std::fmt::Display for RelativePath {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.path.display())
    }
}

/// An entry in a directory listing.
#[derive(Debug, Clone)]
pub struct DirectoryEntry {
    /// Name of the file or directory
    pub name: String,
    /// Whether this entry is a directory
    pub is_directory: bool,
}

impl DirectoryEntry {
    /// Create a new directory entry.
    pub fn new(name: impl Into<String>, is_directory: bool) -> Self {
        Self {
            name: name.into(),
            is_directory,
        }
    }
}

/// Filter for file dialogs.
#[derive(Debug, Clone)]
pub struct FileFilter {
    /// Display name for the filter (e.g., "NodeBox Files")
    pub name: String,
    /// File extensions to filter (e.g., ["ndbx"])
    pub extensions: Vec<String>,
}

impl FileFilter {
    /// Create a new file filter.
    pub fn new(name: impl Into<String>, extensions: Vec<String>) -> Self {
        Self {
            name: name.into(),
            extensions,
        }
    }

    /// Create a filter for NodeBox files.
    pub fn nodebox() -> Self {
        Self::new("NodeBox Files", vec!["ndbx".to_string()])
    }

    /// Create a filter for SVG files.
    pub fn svg() -> Self {
        Self::new("SVG Files", vec!["svg".to_string()])
    }

    /// Create a filter for PNG files.
    pub fn png() -> Self {
        Self::new("PNG Files", vec!["png".to_string()])
    }

    /// Create a filter for CSV files.
    pub fn csv() -> Self {
        Self::new("CSV Files", vec!["csv".to_string(), "tsv".to_string()])
    }

    /// Create a filter for text files.
    pub fn text() -> Self {
        Self::new("Text Files", vec!["txt".to_string(), "text".to_string(), "csv".to_string(), "tsv".to_string(), "log".to_string()])
    }
}

/// Information about a font available on the system.
#[derive(Debug, Clone)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FontInfo {
    /// The font family name (e.g., "Helvetica").
    pub family: String,
    /// The PostScript name used for loading the font (e.g., "Helvetica-Bold").
    pub postscript_name: String,
}

/// Log level for the `log` method.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LogLevel {
    /// Error messages
    Error,
    /// Warning messages
    Warn,
    /// Informational messages
    Info,
    /// Debug messages
    Debug,
}

impl std::fmt::Display for LogLevel {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            LogLevel::Error => write!(f, "ERROR"),
            LogLevel::Warn => write!(f, "WARN"),
            LogLevel::Info => write!(f, "INFO"),
            LogLevel::Debug => write!(f, "DEBUG"),
        }
    }
}

/// Errors that can occur during Port operations.
#[derive(Debug, Error)]
pub enum PlatformError {
    /// Operation not supported on this platform
    #[error("operation not supported on this platform")]
    Unsupported,

    /// File or directory not found
    #[error("not found")]
    NotFound,

    /// Permission denied
    #[error("permission denied")]
    PermissionDenied,

    /// Path escapes sandbox (tried to access outside project dir)
    #[error("path escapes project sandbox")]
    SandboxViolation,

    /// Network request failed
    #[error("network error: {0}")]
    NetworkError(String),

    /// I/O error
    #[error("I/O error: {0}")]
    IoError(String),

    /// Library not found
    #[error("library not found: {0}")]
    LibraryNotFound(String),

    /// Other error
    #[error("{0}")]
    Other(String),
}

impl From<std::io::Error> for PlatformError {
    fn from(err: std::io::Error) -> Self {
        match err.kind() {
            std::io::ErrorKind::NotFound => PlatformError::NotFound,
            std::io::ErrorKind::PermissionDenied => PlatformError::PermissionDenied,
            _ => PlatformError::IoError(err.to_string()),
        }
    }
}

/// The main Platform trait for platform abstraction.
///
/// Implementations of this trait provide platform-specific behavior for
/// file I/O, dialogs, clipboard, network, and other operations.
pub trait Platform: Send + Sync {
    // === Platform Info ===

    /// Get information about the current platform.
    fn platform_info(&self) -> PlatformInfo;

    // === File Operations ===

    /// Read a file from the project directory.
    fn read_file(&self, ctx: &ProjectContext, path: &RelativePath) -> Result<Vec<u8>, PlatformError>;

    /// Write a file to the project directory.
    fn write_file(
        &self,
        ctx: &ProjectContext,
        path: &RelativePath,
        data: &[u8],
    ) -> Result<(), PlatformError>;

    /// List contents of a directory within the project.
    fn list_directory(
        &self,
        ctx: &ProjectContext,
        path: &RelativePath,
    ) -> Result<Vec<DirectoryEntry>, PlatformError>;

    // === Convenience File Operations ===

    /// Read a text file (UTF-8) from the project directory.
    fn read_text_file(&self, ctx: &ProjectContext, path: &str) -> Result<String, PlatformError>;

    /// Read a binary file from the project directory.
    fn read_binary_file(&self, ctx: &ProjectContext, path: &str) -> Result<Vec<u8>, PlatformError>;

    /// Load an application resource (icons, fonts, etc.)
    fn load_app_resource(&self, name: &str) -> Result<Vec<u8>, PlatformError>;

    // === Project File (special handling) ===

    /// Read the project file.
    fn read_project(&self, ctx: &ProjectContext) -> Result<Vec<u8>, PlatformError>;

    /// Write the project file.
    fn write_project(&self, ctx: &ProjectContext, data: &[u8]) -> Result<(), PlatformError>;

    // === Library Access ===

    /// Load a library by name.
    fn load_library(&self, name: &str) -> Result<Vec<u8>, PlatformError>;

    // === Network ===

    /// Perform an HTTP GET request.
    fn http_get(&self, url: &str) -> Result<Vec<u8>, PlatformError>;

    // === Dialogs (Project-level, return absolute paths) ===

    /// Show dialog to open a project file (no sandbox restriction).
    fn show_open_project_dialog(
        &self,
        filters: &[FileFilter],
    ) -> Result<Option<PathBuf>, PlatformError>;

    /// Show dialog to choose where to save a new project.
    fn show_save_project_dialog(
        &self,
        filters: &[FileFilter],
        default_name: Option<&str>,
    ) -> Result<Option<PathBuf>, PlatformError>;

    // === Dialogs (Asset-level, sandboxed to project) ===

    /// Show "Open File" dialog for importing assets.
    fn show_open_file_dialog(
        &self,
        ctx: &ProjectContext,
        filters: &[FileFilter],
    ) -> Result<Option<RelativePath>, PlatformError>;

    /// Show "Save File" dialog for exporting assets.
    fn show_save_file_dialog(
        &self,
        ctx: &ProjectContext,
        filters: &[FileFilter],
        default_name: Option<&str>,
    ) -> Result<Option<RelativePath>, PlatformError>;

    /// Show a "Select Folder" dialog for selecting a directory within the project.
    fn show_select_folder_dialog(
        &self,
        ctx: &ProjectContext,
    ) -> Result<Option<RelativePath>, PlatformError>;

    /// Show a confirmation dialog with OK and Cancel buttons.
    fn show_confirm_dialog(&self, title: &str, message: &str) -> Result<bool, PlatformError>;

    /// Show a message dialog with custom buttons.
    fn show_message_dialog(
        &self,
        title: &str,
        message: &str,
        buttons: &[&str],
    ) -> Result<Option<usize>, PlatformError>;

    // === Clipboard ===

    /// Read text from the clipboard.
    fn clipboard_read_text(&self) -> Result<Option<String>, PlatformError>;

    /// Write text to the clipboard.
    fn clipboard_write_text(&self, text: &str) -> Result<(), PlatformError>;

    // === Logging ===

    /// Log a message at the specified level.
    fn log(&self, level: LogLevel, message: &str);

    // === Performance ===

    /// Create a performance mark.
    fn performance_mark(&self, name: &str);

    /// Create a performance mark with additional details.
    fn performance_mark_with_details(&self, name: &str, details: &str);

    // === Configuration ===

    /// Get the configuration directory for storing app settings.
    fn get_config_dir(&self) -> Result<PathBuf, PlatformError>;

    /// List available font families on the system.
    fn list_fonts(&self) -> Vec<String>;

    /// Get a list of available fonts with family and PostScript names.
    fn get_font_list(&self) -> Vec<FontInfo>;

    /// Load font file bytes by PostScript name.
    fn get_font_bytes(&self, postscript_name: &str) -> Result<Vec<u8>, PlatformError>;
}

/// A minimal Platform implementation for testing.
///
/// Returns `Unsupported` for most operations, making it suitable
/// for tests that don't need actual file or dialog operations.
pub struct TestPlatform;

impl TestPlatform {
    /// Create a new TestPlatform.
    pub fn new() -> Self {
        Self
    }
}

impl Default for TestPlatform {
    fn default() -> Self {
        Self::new()
    }
}

impl Platform for TestPlatform {
    fn platform_info(&self) -> PlatformInfo {
        PlatformInfo {
            os_name: "test".to_string(),
            is_web: false,
            is_mobile: false,
            has_filesystem: false,
            has_native_dialogs: false,
        }
    }

    fn read_file(&self, _ctx: &ProjectContext, _path: &RelativePath) -> Result<Vec<u8>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn write_file(
        &self,
        _ctx: &ProjectContext,
        _path: &RelativePath,
        _data: &[u8],
    ) -> Result<(), PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn list_directory(
        &self,
        _ctx: &ProjectContext,
        _path: &RelativePath,
    ) -> Result<Vec<DirectoryEntry>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn read_text_file(&self, _ctx: &ProjectContext, _path: &str) -> Result<String, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn read_binary_file(&self, _ctx: &ProjectContext, _path: &str) -> Result<Vec<u8>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn load_app_resource(&self, _name: &str) -> Result<Vec<u8>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn read_project(&self, _ctx: &ProjectContext) -> Result<Vec<u8>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn write_project(&self, _ctx: &ProjectContext, _data: &[u8]) -> Result<(), PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn load_library(&self, _name: &str) -> Result<Vec<u8>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn http_get(&self, _url: &str) -> Result<Vec<u8>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_open_project_dialog(
        &self,
        _filters: &[FileFilter],
    ) -> Result<Option<PathBuf>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_save_project_dialog(
        &self,
        _filters: &[FileFilter],
        _default_name: Option<&str>,
    ) -> Result<Option<PathBuf>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_open_file_dialog(
        &self,
        _ctx: &ProjectContext,
        _filters: &[FileFilter],
    ) -> Result<Option<RelativePath>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_save_file_dialog(
        &self,
        _ctx: &ProjectContext,
        _filters: &[FileFilter],
        _default_name: Option<&str>,
    ) -> Result<Option<RelativePath>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_select_folder_dialog(
        &self,
        _ctx: &ProjectContext,
    ) -> Result<Option<RelativePath>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_confirm_dialog(&self, _title: &str, _message: &str) -> Result<bool, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_message_dialog(
        &self,
        _title: &str,
        _message: &str,
        _buttons: &[&str],
    ) -> Result<Option<usize>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn clipboard_read_text(&self) -> Result<Option<String>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn clipboard_write_text(&self, _text: &str) -> Result<(), PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn log(&self, _level: LogLevel, _message: &str) {}

    fn performance_mark(&self, _name: &str) {}

    fn performance_mark_with_details(&self, _name: &str, _details: &str) {}

    fn get_config_dir(&self) -> Result<PathBuf, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn list_fonts(&self) -> Vec<String> {
        Vec::new()
    }

    fn get_font_list(&self) -> Vec<FontInfo> {
        Vec::new()
    }

    fn get_font_bytes(&self, _postscript_name: &str) -> Result<Vec<u8>, PlatformError> {
        Err(PlatformError::Unsupported)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_relative_path_valid() {
        assert!(RelativePath::new("file.txt").is_ok());
        assert!(RelativePath::new("subdir/file.txt").is_ok());
        assert!(RelativePath::new("a/b/c/d.txt").is_ok());
        assert!(RelativePath::new("").is_ok());
    }

    #[test]
    fn test_relative_path_rejects_parent_dir() {
        assert!(matches!(RelativePath::new(".."), Err(PlatformError::SandboxViolation)));
        assert!(matches!(RelativePath::new("../file.txt"), Err(PlatformError::SandboxViolation)));
        assert!(matches!(RelativePath::new("subdir/../other.txt"), Err(PlatformError::SandboxViolation)));
        assert!(matches!(RelativePath::new("a/b/../../c.txt"), Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_relative_path_rejects_absolute() {
        assert!(matches!(RelativePath::new("/etc/passwd"), Err(PlatformError::SandboxViolation)));
        assert!(matches!(RelativePath::new("/home/user/file.txt"), Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_relative_path_rejects_windows_absolute() {
        assert!(matches!(RelativePath::new("C:/Users/file.txt"), Err(PlatformError::SandboxViolation)));
        assert!(matches!(RelativePath::new("D:\\Documents\\file.txt"), Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_relative_path_join() {
        let base = RelativePath::new("subdir").unwrap();
        let joined = base.join("file.txt").unwrap();
        assert_eq!(joined.as_path(), Path::new("subdir/file.txt"));
        assert!(matches!(base.join("../escape.txt"), Err(PlatformError::SandboxViolation)));
    }

    #[test]
    fn test_relative_path_display() {
        let path = RelativePath::new("subdir/file.txt").unwrap();
        assert_eq!(format!("{}", path), "subdir/file.txt");
    }

    #[test]
    fn test_project_context() {
        let ctx = ProjectContext::new("/home/user/project", "myproject.ndbx");
        assert_eq!(ctx.root, Some(PathBuf::from("/home/user/project")));
        assert_eq!(ctx.project_file, Some("myproject.ndbx".to_string()));
        assert!(ctx.is_saved());
        assert_eq!(ctx.project_path(), Some(PathBuf::from("/home/user/project/myproject.ndbx")));
    }

    #[test]
    fn test_project_context_unsaved() {
        let ctx = ProjectContext::new_unsaved();
        assert_eq!(ctx.root, None);
        assert_eq!(ctx.project_file, None);
        assert!(!ctx.is_saved());
        assert_eq!(ctx.project_path(), None);
    }

    #[test]
    fn test_directory_entry() {
        let file = DirectoryEntry::new("file.txt", false);
        assert_eq!(file.name, "file.txt");
        assert!(!file.is_directory);
        let dir = DirectoryEntry::new("subdir", true);
        assert_eq!(dir.name, "subdir");
        assert!(dir.is_directory);
    }

    #[test]
    fn test_file_filter() {
        let filter = FileFilter::new("Images", vec!["png".to_string(), "jpg".to_string()]);
        assert_eq!(filter.name, "Images");
        assert_eq!(filter.extensions, vec!["png", "jpg"]);
        let ndbx = FileFilter::nodebox();
        assert_eq!(ndbx.extensions, vec!["ndbx"]);
    }

    #[test]
    fn test_log_level_display() {
        assert_eq!(format!("{}", LogLevel::Error), "ERROR");
        assert_eq!(format!("{}", LogLevel::Warn), "WARN");
        assert_eq!(format!("{}", LogLevel::Info), "INFO");
        assert_eq!(format!("{}", LogLevel::Debug), "DEBUG");
    }

    #[test]
    fn test_port_error_from_io_error() {
        let not_found = std::io::Error::new(std::io::ErrorKind::NotFound, "not found");
        assert!(matches!(PlatformError::from(not_found), PlatformError::NotFound));
        let permission = std::io::Error::new(std::io::ErrorKind::PermissionDenied, "denied");
        assert!(matches!(PlatformError::from(permission), PlatformError::PermissionDenied));
        let other = std::io::Error::new(std::io::ErrorKind::Other, "something else");
        assert!(matches!(PlatformError::from(other), PlatformError::IoError(_)));
    }

    #[test]
    fn test_port_error_display() {
        assert_eq!(format!("{}", PlatformError::Unsupported), "operation not supported on this platform");
        assert_eq!(format!("{}", PlatformError::NotFound), "not found");
        assert_eq!(format!("{}", PlatformError::PermissionDenied), "permission denied");
        assert_eq!(format!("{}", PlatformError::SandboxViolation), "path escapes project sandbox");
        assert_eq!(format!("{}", PlatformError::NetworkError("timeout".to_string())), "network error: timeout");
        assert_eq!(format!("{}", PlatformError::LibraryNotFound("math".to_string())), "library not found: math");
    }

    #[test]
    fn test_platform_info_current() {
        let info = PlatformInfo::current();
        assert!(!info.os_name.is_empty());
    }
}
