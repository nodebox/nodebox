//! Platform abstraction layer for NodeBox.
//!
//! The Port system provides a unified interface for platform-specific I/O operations,
//! enabling the same core logic to run across desktop (macOS, Windows, Linux),
//! web (WASM), and mobile (iOS, Android) platforms.
//!
//! # Design Principles
//!
//! 1. **Single trait with runtime capability checking** - One `Port` trait;
//!    unsupported operations return `Err(PortError::Unsupported)`
//! 2. **Synchronous API** - All operations are blocking
//! 3. **Explicit context passing** - `ProjectContext` passed to operations; no global state
//! 4. **Sandboxed file access** - Files accessible only within project directory,
//!    its subdirectories, and explicit library paths
//!
//! # Example
//!
//! ```no_run
//! use nodebox_port::{Port, ProjectContext, RelativePath, DesktopPort};
//! use std::path::PathBuf;
//!
//! let port = DesktopPort::new();
//! let ctx = ProjectContext::new("/path/to/project", "project.ndbx");
//!
//! // Read a file relative to project root using the convenience method
//! let svg_content = port.read_text_file(&ctx, "assets/logo.svg");
//!
//! // Or use the lower-level API with RelativePath
//! let path = RelativePath::new("assets/image.png").unwrap();
//! let data = port.read_file(&ctx, &path);
//! ```

#[cfg(not(target_arch = "wasm32"))]
mod desktop;

#[cfg(not(target_arch = "wasm32"))]
pub use desktop::DesktopPort;

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
}

impl ProjectContext {
    /// Create context for a new unsaved project.
    pub fn new_unsaved() -> Self {
        Self {
            root: None,
            project_file: None,
        }
    }

    /// Create context for a saved project.
    pub fn new(root: impl Into<PathBuf>, project_file: impl Into<String>) -> Self {
        Self {
            root: Some(root.into()),
            project_file: Some(project_file.into()),
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
    /// Returns `PortError::SandboxViolation` if:
    /// - The path contains ".." components
    /// - The path starts with "/" (absolute path)
    /// - The path starts with a Windows drive letter (e.g., "C:")
    pub fn new(path: impl AsRef<Path>) -> Result<Self, PortError> {
        let path = path.as_ref();

        // Check for absolute paths
        if path.is_absolute() {
            return Err(PortError::SandboxViolation);
        }

        // Check for Windows-style absolute paths (C:\, D:\, etc.)
        if let Some(s) = path.to_str() {
            if s.len() >= 2 {
                let chars: Vec<char> = s.chars().take(2).collect();
                if chars[0].is_ascii_alphabetic() && chars[1] == ':' {
                    return Err(PortError::SandboxViolation);
                }
            }
        }

        // Check for ".." components that could escape the sandbox
        for component in path.components() {
            if let std::path::Component::ParentDir = component {
                return Err(PortError::SandboxViolation);
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
    /// Returns `PortError::SandboxViolation` if the resulting path would escape the sandbox.
    pub fn join(&self, path: impl AsRef<Path>) -> Result<Self, PortError> {
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
pub enum PortError {
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

impl From<std::io::Error> for PortError {
    fn from(err: std::io::Error) -> Self {
        match err.kind() {
            std::io::ErrorKind::NotFound => PortError::NotFound,
            std::io::ErrorKind::PermissionDenied => PortError::PermissionDenied,
            _ => PortError::IoError(err.to_string()),
        }
    }
}

/// The main Port trait for platform abstraction.
///
/// Implementations of this trait provide platform-specific behavior for
/// file I/O, dialogs, clipboard, network, and other operations.
pub trait Port: Send + Sync {
    // === Platform Info ===

    /// Get information about the current platform.
    fn platform_info(&self) -> PlatformInfo;

    // === File Operations ===

    /// Read a file from the project directory.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context containing the root directory
    /// * `path` - Path relative to project root
    ///
    /// # Errors
    ///
    /// * `PortError::NotFound` - File does not exist
    /// * `PortError::PermissionDenied` - No permission to read file
    /// * `PortError::IoError` - Other I/O error
    fn read_file(&self, ctx: &ProjectContext, path: &RelativePath) -> Result<Vec<u8>, PortError>;

    /// Write a file to the project directory.
    ///
    /// Creates parent directories if they don't exist.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context containing the root directory
    /// * `path` - Path relative to project root
    /// * `data` - Data to write
    ///
    /// # Errors
    ///
    /// * `PortError::PermissionDenied` - No permission to write file
    /// * `PortError::IoError` - Other I/O error
    fn write_file(
        &self,
        ctx: &ProjectContext,
        path: &RelativePath,
        data: &[u8],
    ) -> Result<(), PortError>;

    /// List contents of a directory within the project.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context containing the root directory
    /// * `path` - Path relative to project root
    ///
    /// # Errors
    ///
    /// * `PortError::NotFound` - Directory does not exist
    /// * `PortError::PermissionDenied` - No permission to read directory
    /// * `PortError::IoError` - Other I/O error
    fn list_directory(
        &self,
        ctx: &ProjectContext,
        path: &RelativePath,
    ) -> Result<Vec<DirectoryEntry>, PortError>;

    // === Convenience File Operations ===

    /// Read a text file (UTF-8) from the project directory.
    ///
    /// This is a convenience method that validates the path is relative and within
    /// the project sandbox, then reads the file as UTF-8 text.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context containing the root directory
    /// * `path` - Path string relative to project root
    ///
    /// # Errors
    ///
    /// * `PortError::Unsupported` - Project has no root directory (unsaved project)
    /// * `PortError::SandboxViolation` - Path contains "..", is absolute, etc.
    /// * `PortError::NotFound` - File does not exist
    /// * `PortError::IoError` - Other I/O error or invalid UTF-8
    fn read_text_file(&self, ctx: &ProjectContext, path: &str) -> Result<String, PortError>;

    /// Read a binary file from the project directory.
    ///
    /// This is a convenience method that validates the path is relative and within
    /// the project sandbox, then reads the file as binary data.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context containing the root directory
    /// * `path` - Path string relative to project root
    ///
    /// # Errors
    ///
    /// * `PortError::Unsupported` - Project has no root directory (unsaved project)
    /// * `PortError::SandboxViolation` - Path contains "..", is absolute, etc.
    /// * `PortError::NotFound` - File does not exist
    /// * `PortError::IoError` - Other I/O error
    fn read_binary_file(&self, ctx: &ProjectContext, path: &str) -> Result<Vec<u8>, PortError>;

    /// Load an application resource (icons, fonts, etc.)
    ///
    /// These are bundled with the application, not project-specific.
    /// Resources are located relative to the application executable or bundle.
    ///
    /// # Arguments
    ///
    /// * `name` - Resource path relative to resources directory (e.g., "icons/add.png")
    ///
    /// # Errors
    ///
    /// * `PortError::NotFound` - Resource does not exist
    /// * `PortError::IoError` - Other I/O error
    fn load_app_resource(&self, name: &str) -> Result<Vec<u8>, PortError>;

    // === Project File (special handling) ===

    /// Read the project file.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context containing the root directory and project file name
    ///
    /// # Errors
    ///
    /// * `PortError::NotFound` - Project file does not exist
    /// * `PortError::IoError` - Other I/O error
    fn read_project(&self, ctx: &ProjectContext) -> Result<Vec<u8>, PortError>;

    /// Write the project file.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context containing the root directory and project file name
    /// * `data` - Project data to write
    ///
    /// # Errors
    ///
    /// * `PortError::PermissionDenied` - No permission to write file
    /// * `PortError::IoError` - Other I/O error
    fn write_project(&self, ctx: &ProjectContext, data: &[u8]) -> Result<(), PortError>;

    // === Library Access ===

    /// Load a library by name.
    ///
    /// Libraries are located in platform-specific directories:
    /// - macOS: `~/Library/Application Support/net.nodebox.NodeBox/libraries/`
    /// - Windows: `%APPDATA%\NodeBox\libraries\`
    /// - Linux: `~/.local/share/nodebox/libraries/`
    ///
    /// # Arguments
    ///
    /// * `name` - Name of the library (without extension)
    ///
    /// # Errors
    ///
    /// * `PortError::LibraryNotFound` - Library does not exist
    /// * `PortError::IoError` - Other I/O error
    fn load_library(&self, name: &str) -> Result<Vec<u8>, PortError>;

    // === Network ===

    /// Perform an HTTP GET request.
    ///
    /// # Arguments
    ///
    /// * `url` - URL to fetch
    ///
    /// # Errors
    ///
    /// * `PortError::NetworkError` - Network request failed
    /// * `PortError::Unsupported` - Network not available on this platform
    fn http_get(&self, url: &str) -> Result<Vec<u8>, PortError>;

    // === Dialogs (Project-level, return absolute paths) ===

    /// Show dialog to open a project file (no sandbox restriction).
    ///
    /// This is for opening .ndbx project files and returns an absolute path.
    /// Use this when the user wants to open an existing project.
    ///
    /// # Arguments
    ///
    /// * `filters` - File type filters to show
    ///
    /// # Returns
    ///
    /// * `Ok(Some(path))` - User selected a file
    /// * `Ok(None)` - User cancelled
    /// * `Err(PortError::Unsupported)` - Dialogs not available
    fn show_open_project_dialog(
        &self,
        filters: &[FileFilter],
    ) -> Result<Option<PathBuf>, PortError>;

    /// Show dialog to choose where to save a new project.
    ///
    /// This is for "Save As" operations and returns an absolute path.
    /// Use this to establish the project location for new/unsaved projects.
    ///
    /// # Arguments
    ///
    /// * `filters` - File type filters to show
    /// * `default_name` - Optional default filename
    ///
    /// # Returns
    ///
    /// * `Ok(Some(path))` - User selected a save location
    /// * `Ok(None)` - User cancelled
    /// * `Err(PortError::Unsupported)` - Dialogs not available
    fn show_save_project_dialog(
        &self,
        filters: &[FileFilter],
        default_name: Option<&str>,
    ) -> Result<Option<PathBuf>, PortError>;

    // === Dialogs (Asset-level, sandboxed to project) ===

    /// Show "Open File" dialog for importing assets.
    ///
    /// The selected file must be within the project directory. If the user
    /// selects a file outside the project, returns `Err(PortError::SandboxViolation)`.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context (required for sandbox validation)
    /// * `filters` - File type filters to show
    ///
    /// # Returns
    ///
    /// * `Ok(Some(path))` - User selected a valid file within project
    /// * `Ok(None)` - User cancelled
    /// * `Err(PortError::SandboxViolation)` - Selected file is outside project directory
    /// * `Err(PortError::Unsupported)` - Dialogs not available
    fn show_open_file_dialog(
        &self,
        ctx: &ProjectContext,
        filters: &[FileFilter],
    ) -> Result<Option<RelativePath>, PortError>;

    /// Show "Save File" dialog for exporting assets.
    ///
    /// The selected location must be within the project directory. If the user
    /// selects a location outside the project, returns `Err(PortError::SandboxViolation)`.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context (required for sandbox validation)
    /// * `filters` - File type filters to show
    /// * `default_name` - Optional default filename
    ///
    /// # Returns
    ///
    /// * `Ok(Some(path))` - User selected a valid location within project
    /// * `Ok(None)` - User cancelled
    /// * `Err(PortError::SandboxViolation)` - Selected location is outside project directory
    /// * `Err(PortError::Unsupported)` - Dialogs not available
    fn show_save_file_dialog(
        &self,
        ctx: &ProjectContext,
        filters: &[FileFilter],
        default_name: Option<&str>,
    ) -> Result<Option<RelativePath>, PortError>;

    /// Show a "Select Folder" dialog for selecting a directory within the project.
    ///
    /// The selected folder must be within the project directory. If the user
    /// selects a folder outside the project, returns `Err(PortError::SandboxViolation)`.
    ///
    /// # Arguments
    ///
    /// * `ctx` - Project context (required for sandbox validation)
    ///
    /// # Returns
    ///
    /// * `Ok(Some(path))` - User selected a valid folder within project
    /// * `Ok(None)` - User cancelled
    /// * `Err(PortError::SandboxViolation)` - Selected folder is outside project directory
    /// * `Err(PortError::Unsupported)` - Dialogs not available
    fn show_select_folder_dialog(
        &self,
        ctx: &ProjectContext,
    ) -> Result<Option<RelativePath>, PortError>;

    /// Show a confirmation dialog with OK and Cancel buttons.
    ///
    /// # Arguments
    ///
    /// * `title` - Dialog title
    /// * `message` - Dialog message
    ///
    /// # Returns
    ///
    /// * `Ok(true)` - User clicked OK
    /// * `Ok(false)` - User clicked Cancel
    /// * `Err(PortError::Unsupported)` - Dialogs not available
    fn show_confirm_dialog(&self, title: &str, message: &str) -> Result<bool, PortError>;

    /// Show a message dialog with custom buttons.
    ///
    /// # Arguments
    ///
    /// * `title` - Dialog title
    /// * `message` - Dialog message
    /// * `buttons` - Button labels (2-3 buttons)
    ///
    /// # Returns
    ///
    /// * `Ok(Some(index))` - User clicked button at index (0-based)
    /// * `Ok(None)` - User dismissed without selection
    /// * `Err(PortError::Unsupported)` - Dialogs not available
    fn show_message_dialog(
        &self,
        title: &str,
        message: &str,
        buttons: &[&str],
    ) -> Result<Option<usize>, PortError>;

    // === Clipboard ===

    /// Read text from the clipboard.
    ///
    /// # Returns
    ///
    /// * `Ok(Some(text))` - Clipboard contains text
    /// * `Ok(None)` - Clipboard is empty or doesn't contain text
    /// * `Err(PortError::Unsupported)` - Clipboard not available
    fn clipboard_read_text(&self) -> Result<Option<String>, PortError>;

    /// Write text to the clipboard.
    ///
    /// # Arguments
    ///
    /// * `text` - Text to write to clipboard
    ///
    /// # Errors
    ///
    /// * `Err(PortError::Unsupported)` - Clipboard not available
    /// * `Err(PortError::Other)` - Failed to write to clipboard
    fn clipboard_write_text(&self, text: &str) -> Result<(), PortError>;

    // === Logging ===

    /// Log a message at the specified level.
    ///
    /// Messages are routed to the appropriate destination:
    /// - Desktop: stderr or log file
    /// - Web: console.log/console.error
    /// - Mobile: NSLog/android.util.Log
    fn log(&self, level: LogLevel, message: &str);

    // === Performance ===

    /// Create a performance mark.
    ///
    /// Used for profiling and performance analysis.
    fn performance_mark(&self, name: &str);

    /// Create a performance mark with additional details.
    ///
    /// # Arguments
    ///
    /// * `name` - Mark name
    /// * `details` - Additional details (JSON string)
    fn performance_mark_with_details(&self, name: &str, details: &str);

    // === Configuration ===

    /// Get the configuration directory for storing app settings.
    ///
    /// Platform-specific locations:
    /// - macOS: `~/Library/Application Support/net.nodebox.NodeBox/`
    /// - Windows: `%APPDATA%\NodeBox\`
    /// - Linux: `~/.config/nodebox/`
    ///
    /// # Errors
    ///
    /// * `Err(PortError::Unsupported)` - No config directory on this platform
    fn get_config_dir(&self) -> Result<PathBuf, PortError>;
}

/// A minimal Port implementation for testing.
///
/// This port returns `Unsupported` for most operations, making it suitable
/// for tests that don't need actual file or dialog operations.
pub struct TestPort;

impl TestPort {
    /// Create a new TestPort.
    pub fn new() -> Self {
        Self
    }
}

impl Default for TestPort {
    fn default() -> Self {
        Self::new()
    }
}

impl Port for TestPort {
    fn platform_info(&self) -> PlatformInfo {
        PlatformInfo {
            os_name: "test".to_string(),
            is_web: false,
            is_mobile: false,
            has_filesystem: false,
            has_native_dialogs: false,
        }
    }

    fn read_file(&self, _ctx: &ProjectContext, _path: &RelativePath) -> Result<Vec<u8>, PortError> {
        Err(PortError::Unsupported)
    }

    fn write_file(
        &self,
        _ctx: &ProjectContext,
        _path: &RelativePath,
        _data: &[u8],
    ) -> Result<(), PortError> {
        Err(PortError::Unsupported)
    }

    fn list_directory(
        &self,
        _ctx: &ProjectContext,
        _path: &RelativePath,
    ) -> Result<Vec<DirectoryEntry>, PortError> {
        Err(PortError::Unsupported)
    }

    fn read_text_file(&self, _ctx: &ProjectContext, _path: &str) -> Result<String, PortError> {
        Err(PortError::Unsupported)
    }

    fn read_binary_file(&self, _ctx: &ProjectContext, _path: &str) -> Result<Vec<u8>, PortError> {
        Err(PortError::Unsupported)
    }

    fn load_app_resource(&self, _name: &str) -> Result<Vec<u8>, PortError> {
        Err(PortError::Unsupported)
    }

    fn read_project(&self, _ctx: &ProjectContext) -> Result<Vec<u8>, PortError> {
        Err(PortError::Unsupported)
    }

    fn write_project(&self, _ctx: &ProjectContext, _data: &[u8]) -> Result<(), PortError> {
        Err(PortError::Unsupported)
    }

    fn load_library(&self, _name: &str) -> Result<Vec<u8>, PortError> {
        Err(PortError::Unsupported)
    }

    fn http_get(&self, _url: &str) -> Result<Vec<u8>, PortError> {
        Err(PortError::Unsupported)
    }

    fn show_open_project_dialog(
        &self,
        _filters: &[FileFilter],
    ) -> Result<Option<PathBuf>, PortError> {
        Err(PortError::Unsupported)
    }

    fn show_save_project_dialog(
        &self,
        _filters: &[FileFilter],
        _default_name: Option<&str>,
    ) -> Result<Option<PathBuf>, PortError> {
        Err(PortError::Unsupported)
    }

    fn show_open_file_dialog(
        &self,
        _ctx: &ProjectContext,
        _filters: &[FileFilter],
    ) -> Result<Option<RelativePath>, PortError> {
        Err(PortError::Unsupported)
    }

    fn show_save_file_dialog(
        &self,
        _ctx: &ProjectContext,
        _filters: &[FileFilter],
        _default_name: Option<&str>,
    ) -> Result<Option<RelativePath>, PortError> {
        Err(PortError::Unsupported)
    }

    fn show_select_folder_dialog(
        &self,
        _ctx: &ProjectContext,
    ) -> Result<Option<RelativePath>, PortError> {
        Err(PortError::Unsupported)
    }

    fn show_confirm_dialog(&self, _title: &str, _message: &str) -> Result<bool, PortError> {
        Err(PortError::Unsupported)
    }

    fn show_message_dialog(
        &self,
        _title: &str,
        _message: &str,
        _buttons: &[&str],
    ) -> Result<Option<usize>, PortError> {
        Err(PortError::Unsupported)
    }

    fn clipboard_read_text(&self) -> Result<Option<String>, PortError> {
        Err(PortError::Unsupported)
    }

    fn clipboard_write_text(&self, _text: &str) -> Result<(), PortError> {
        Err(PortError::Unsupported)
    }

    fn log(&self, _level: LogLevel, _message: &str) {}

    fn performance_mark(&self, _name: &str) {}

    fn performance_mark_with_details(&self, _name: &str, _details: &str) {}

    fn get_config_dir(&self) -> Result<PathBuf, PortError> {
        Err(PortError::Unsupported)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_relative_path_valid() {
        // Simple paths should be valid
        assert!(RelativePath::new("file.txt").is_ok());
        assert!(RelativePath::new("subdir/file.txt").is_ok());
        assert!(RelativePath::new("a/b/c/d.txt").is_ok());
        assert!(RelativePath::new("").is_ok());
    }

    #[test]
    fn test_relative_path_rejects_parent_dir() {
        // Paths with ".." should be rejected
        assert!(matches!(
            RelativePath::new(".."),
            Err(PortError::SandboxViolation)
        ));
        assert!(matches!(
            RelativePath::new("../file.txt"),
            Err(PortError::SandboxViolation)
        ));
        assert!(matches!(
            RelativePath::new("subdir/../other.txt"),
            Err(PortError::SandboxViolation)
        ));
        assert!(matches!(
            RelativePath::new("a/b/../../c.txt"),
            Err(PortError::SandboxViolation)
        ));
    }

    #[test]
    fn test_relative_path_rejects_absolute() {
        // Absolute paths should be rejected
        assert!(matches!(
            RelativePath::new("/etc/passwd"),
            Err(PortError::SandboxViolation)
        ));
        assert!(matches!(
            RelativePath::new("/home/user/file.txt"),
            Err(PortError::SandboxViolation)
        ));
    }

    #[test]
    fn test_relative_path_rejects_windows_absolute() {
        // Windows-style absolute paths should be rejected
        assert!(matches!(
            RelativePath::new("C:/Users/file.txt"),
            Err(PortError::SandboxViolation)
        ));
        assert!(matches!(
            RelativePath::new("D:\\Documents\\file.txt"),
            Err(PortError::SandboxViolation)
        ));
    }

    #[test]
    fn test_relative_path_join() {
        let base = RelativePath::new("subdir").unwrap();

        // Valid joins
        let joined = base.join("file.txt").unwrap();
        assert_eq!(joined.as_path(), Path::new("subdir/file.txt"));

        // Invalid joins (with ..)
        assert!(matches!(
            base.join("../escape.txt"),
            Err(PortError::SandboxViolation)
        ));
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
        assert_eq!(
            ctx.project_path(),
            Some(PathBuf::from("/home/user/project/myproject.ndbx"))
        );
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
        assert!(matches!(PortError::from(not_found), PortError::NotFound));

        let permission = std::io::Error::new(std::io::ErrorKind::PermissionDenied, "denied");
        assert!(matches!(
            PortError::from(permission),
            PortError::PermissionDenied
        ));

        let other = std::io::Error::new(std::io::ErrorKind::Other, "something else");
        assert!(matches!(PortError::from(other), PortError::IoError(_)));
    }

    #[test]
    fn test_port_error_display() {
        assert_eq!(
            format!("{}", PortError::Unsupported),
            "operation not supported on this platform"
        );
        assert_eq!(format!("{}", PortError::NotFound), "not found");
        assert_eq!(format!("{}", PortError::PermissionDenied), "permission denied");
        assert_eq!(
            format!("{}", PortError::SandboxViolation),
            "path escapes project sandbox"
        );
        assert_eq!(
            format!("{}", PortError::NetworkError("timeout".to_string())),
            "network error: timeout"
        );
        assert_eq!(
            format!("{}", PortError::LibraryNotFound("math".to_string())),
            "library not found: math"
        );
    }

    #[test]
    fn test_platform_info_current() {
        let info = PlatformInfo::current();
        // Just verify it doesn't panic and returns something reasonable
        assert!(!info.os_name.is_empty());
    }
}
