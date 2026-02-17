//! Command-line renderer for NodeBox .ndbx files.
//!
//! Usage:
//!   nodebox-render <input.ndbx> <output.svg>
//!   nodebox-render --all <examples-dir> <output-dir>

use std::path::{Path, PathBuf};
use std::sync::Arc;

use nodebox_core::ndbx;
use nodebox_core::platform::{
    DirectoryEntry, FileFilter, FontInfo, LogLevel, Platform, PlatformError, PlatformInfo,
    ProjectContext, RelativePath,
};
use nodebox_core::svg::{render_to_svg_with_options, SvgOptions};
use nodebox_eval::eval::evaluate_network;

/// Minimal CLI platform that supports file I/O (sandboxed to project directory).
/// Returns Unsupported for dialogs, clipboard, and other GUI operations.
struct CliPlatform;

impl Platform for CliPlatform {
    fn platform_info(&self) -> PlatformInfo {
        PlatformInfo::current()
    }

    fn read_file(
        &self,
        ctx: &ProjectContext,
        path: &RelativePath,
    ) -> Result<Vec<u8>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let full_path = root.join(path.as_path());
        std::fs::read(&full_path).map_err(PlatformError::from)
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

    fn read_text_file(&self, ctx: &ProjectContext, path: &str) -> Result<String, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let relative = RelativePath::new(path)?;
        let full_path = root.join(relative.as_path());
        let bytes = std::fs::read(&full_path).map_err(PlatformError::from)?;
        String::from_utf8(bytes)
            .map_err(|_| PlatformError::IoError("Invalid UTF-8".to_string()))
    }

    fn read_binary_file(
        &self,
        ctx: &ProjectContext,
        path: &str,
    ) -> Result<Vec<u8>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        let relative = RelativePath::new(path)?;
        let full_path = root.join(relative.as_path());
        std::fs::read(&full_path).map_err(PlatformError::from)
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

fn main() {
    let args: Vec<String> = std::env::args().collect();

    if args.len() < 3 {
        eprintln!("Usage: nodebox-render <input.ndbx> <output.svg>");
        eprintln!("       nodebox-render --all <examples-dir> <output-dir>");
        std::process::exit(1);
    }

    if args[1] == "--all" {
        if args.len() < 4 {
            eprintln!("Usage: nodebox-render --all <examples-dir> <output-dir>");
            std::process::exit(1);
        }
        render_all(Path::new(&args[2]), Path::new(&args[3]));
    } else {
        render_one(Path::new(&args[1]), Path::new(&args[2]));
    }
}

fn render_one(input: &Path, output: &Path) {
    // Parse the .ndbx file
    let library = match ndbx::parse_file(input) {
        Ok(lib) => lib,
        Err(e) => {
            eprintln!(
                "FAIL {}: {}",
                input.file_name().unwrap_or_default().to_string_lossy(),
                e
            );
            return;
        }
    };

    // Set up platform and project context
    let platform: Arc<dyn Platform> = Arc::new(CliPlatform);
    let project_context = match input.parent() {
        Some(parent) => ProjectContext {
            root: Some(parent.to_path_buf()),
            project_file: Some(
                input
                    .file_name()
                    .unwrap_or_default()
                    .to_string_lossy()
                    .to_string(),
            ),
            frame: 1,
        },
        None => ProjectContext::new_unsaved(),
    };

    // Evaluate the network
    let (paths, _output, errors) = evaluate_network(&library, &platform, &project_context);

    if !errors.is_empty() {
        for err in &errors {
            eprintln!("  WARN {}: {}", err.node_name, err.message);
        }
    }

    // Get canvas dimensions
    let width = library.width();
    let height = library.height();

    // Render to SVG with centered viewBox to match Java output
    let options = SvgOptions {
        width,
        height,
        background: None,
        precision: 2,
        xml_declaration: true,
        include_viewbox: true,
        centered: true,
    };

    let svg = render_to_svg_with_options(&paths, &options);

    // Create output directory and write file
    if let Some(parent) = output.parent() {
        std::fs::create_dir_all(parent).ok();
    }

    match std::fs::write(output, &svg) {
        Ok(()) => {
            println!(
                "OK {}",
                input.file_name().unwrap_or_default().to_string_lossy()
            );
        }
        Err(e) => {
            eprintln!(
                "FAIL {}: write error: {}",
                input.file_name().unwrap_or_default().to_string_lossy(),
                e
            );
        }
    }
}

fn render_all(examples_dir: &Path, output_dir: &Path) {
    if !examples_dir.exists() {
        eprintln!(
            "Cannot find examples directory: {}",
            examples_dir.display()
        );
        std::process::exit(1);
    }

    std::fs::create_dir_all(output_dir).expect("Failed to create output directory");
    render_directory(examples_dir, examples_dir, output_dir);
}

fn render_directory(dir: &Path, base_dir: &Path, output_dir: &Path) {
    let mut entries: Vec<PathBuf> = std::fs::read_dir(dir)
        .expect("Failed to read directory")
        .filter_map(|e| e.ok().map(|e| e.path()))
        .collect();

    entries.sort();

    for entry in entries {
        if entry.is_dir() {
            render_directory(&entry, base_dir, output_dir);
        } else if entry.extension().map_or(false, |ext| ext == "ndbx") {
            let relative = entry.strip_prefix(base_dir).unwrap();
            let svg_name = relative.with_extension("svg");
            let out_file = output_dir.join(svg_name);
            render_one(&entry, &out_file);
        }
    }
}
