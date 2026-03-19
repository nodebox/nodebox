//! WASM platform implementation for the Electron app.
//!
//! Provides a minimal Platform implementation for WASM that returns
//! `Unsupported` for most operations. The Electron app handles
//! file I/O and dialogs on the JavaScript side.

use std::collections::HashMap;
use std::path::PathBuf;
use nodebox_core::platform::{
    DirectoryEntry, FileFilter, FontInfo, LogLevel, Platform, PlatformError, PlatformInfo,
    ProjectContext, RelativePath,
};

/// WASM platform implementation.
///
/// Most operations return `Unsupported` since the Electron app
/// handles file I/O, dialogs, and clipboard on the JavaScript side.
/// The WASM module is primarily used for evaluation.
///
/// File contents can be pre-loaded from the JS side and served via
/// `read_text_file`. This avoids WASM→JS callbacks during evaluation.
pub struct WasmPlatform {
    files: HashMap<String, String>,
}

impl WasmPlatform {
    pub fn new(files: HashMap<String, String>) -> Self {
        Self { files }
    }
}

impl Platform for WasmPlatform {
    fn platform_info(&self) -> PlatformInfo {
        PlatformInfo {
            os_name: "web".to_string(),
            is_web: true,
            is_mobile: false,
            has_filesystem: false,
            has_native_dialogs: false,
        }
    }

    fn read_file(&self, _ctx: &ProjectContext, _path: &RelativePath) -> Result<Vec<u8>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn write_file(&self, _ctx: &ProjectContext, _path: &RelativePath, _data: &[u8]) -> Result<(), PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn list_directory(&self, _ctx: &ProjectContext, _path: &RelativePath) -> Result<Vec<DirectoryEntry>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn read_text_file(&self, _ctx: &ProjectContext, path: &str) -> Result<String, PlatformError> {
        self.files
            .get(path)
            .cloned()
            .ok_or(PlatformError::Unsupported)
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

    fn show_open_project_dialog(&self, _filters: &[FileFilter]) -> Result<Option<PathBuf>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_save_project_dialog(&self, _filters: &[FileFilter], _default_name: Option<&str>) -> Result<Option<PathBuf>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_open_file_dialog(&self, _ctx: &ProjectContext, _filters: &[FileFilter]) -> Result<Option<RelativePath>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_save_file_dialog(&self, _ctx: &ProjectContext, _filters: &[FileFilter], _default_name: Option<&str>) -> Result<Option<RelativePath>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_select_folder_dialog(&self, _ctx: &ProjectContext) -> Result<Option<RelativePath>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_confirm_dialog(&self, _title: &str, _message: &str) -> Result<bool, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn show_message_dialog(&self, _title: &str, _message: &str, _buttons: &[&str]) -> Result<Option<usize>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn clipboard_read_text(&self) -> Result<Option<String>, PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn clipboard_write_text(&self, _text: &str) -> Result<(), PlatformError> {
        Err(PlatformError::Unsupported)
    }

    fn log(&self, _level: LogLevel, _message: &str) {
        // Could use web_sys::console::log_1 here, but keeping it simple
    }

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
