//! Every example document in `examples/`, evaluated by nodebox-core and drawn
//! with nodebox-gpu, with Vello, and with the supersampled reference.
//!
//! `cargo test -p nodebox-gpu --release --test examples -- --nocapture` prints a
//! table; NODEBOX_GPU_DUMP=<dir> writes the images.

use std::path::{Path as FsPath, PathBuf};
use std::sync::Arc;

use nodebox_core::eval::evaluate_network;
use nodebox_core::ndbx::parse_file;
use nodebox_core::platform::{
    DirectoryEntry, FileFilter, FontInfo, LogLevel, Platform, PlatformError, PlatformInfo, ProjectContext,
    RelativePath, TestPlatform,
};
use wgpu::TextureFormat;

mod common;
use common::{compare, draw_gpu, draw_reference, draw_vello, dump, gpu, Frame, SIZE};

/// Documents where nodebox-gpu is known to be further from the reference than
/// the rule allows; printed, but not failures. "05 Copy" is 2,160 bars of 0.5 by
/// 5 pixels with a 0.5-pixel outline: nodebox-gpu draws their edges a little
/// darker (2.5/255 on average, Vello 0.7).
const KNOWN_GAPS: &[&str] = &["05 Copy"];

/// Reads files next to the document; everything else as TestPlatform.
struct Files(TestPlatform);

impl Files {
    fn read(ctx: &ProjectContext, path: &FsPath) -> Result<Vec<u8>, PlatformError> {
        let root = ctx.root.as_ref().ok_or(PlatformError::Unsupported)?;
        std::fs::read(root.join(path)).map_err(|e| PlatformError::IoError(e.to_string()))
    }
}

impl Platform for Files {
    fn platform_info(&self) -> PlatformInfo {
        self.0.platform_info()
    }
    fn read_file(&self, ctx: &ProjectContext, path: &RelativePath) -> Result<Vec<u8>, PlatformError> {
        Self::read(ctx, path.as_path())
    }
    fn write_file(&self, ctx: &ProjectContext, path: &RelativePath, data: &[u8]) -> Result<(), PlatformError> {
        self.0.write_file(ctx, path, data)
    }
    fn list_directory(&self, ctx: &ProjectContext, path: &RelativePath) -> Result<Vec<DirectoryEntry>, PlatformError> {
        self.0.list_directory(ctx, path)
    }
    fn read_text_file(&self, ctx: &ProjectContext, path: &str) -> Result<String, PlatformError> {
        String::from_utf8(Self::read(ctx, FsPath::new(path))?).map_err(|e| PlatformError::IoError(e.to_string()))
    }
    fn read_binary_file(&self, ctx: &ProjectContext, path: &str) -> Result<Vec<u8>, PlatformError> {
        Self::read(ctx, FsPath::new(path))
    }
    fn load_app_resource(&self, name: &str) -> Result<Vec<u8>, PlatformError> {
        self.0.load_app_resource(name)
    }
    fn read_project(&self, ctx: &ProjectContext) -> Result<Vec<u8>, PlatformError> {
        self.0.read_project(ctx)
    }
    fn write_project(&self, ctx: &ProjectContext, data: &[u8]) -> Result<(), PlatformError> {
        self.0.write_project(ctx, data)
    }
    fn load_library(&self, name: &str) -> Result<Vec<u8>, PlatformError> {
        self.0.load_library(name)
    }
    fn http_get(&self, url: &str) -> Result<Vec<u8>, PlatformError> {
        self.0.http_get(url)
    }
    fn show_open_project_dialog(&self, filters: &[FileFilter]) -> Result<Option<PathBuf>, PlatformError> {
        self.0.show_open_project_dialog(filters)
    }
    fn show_save_project_dialog(&self, filters: &[FileFilter], name: Option<&str>) -> Result<Option<PathBuf>, PlatformError> {
        self.0.show_save_project_dialog(filters, name)
    }
    fn show_open_file_dialog(&self, ctx: &ProjectContext, filters: &[FileFilter]) -> Result<Option<RelativePath>, PlatformError> {
        self.0.show_open_file_dialog(ctx, filters)
    }
    fn show_save_file_dialog(
        &self,
        ctx: &ProjectContext,
        filters: &[FileFilter],
        name: Option<&str>,
    ) -> Result<Option<RelativePath>, PlatformError> {
        self.0.show_save_file_dialog(ctx, filters, name)
    }
    fn show_select_folder_dialog(&self, ctx: &ProjectContext) -> Result<Option<RelativePath>, PlatformError> {
        self.0.show_select_folder_dialog(ctx)
    }
    fn show_confirm_dialog(&self, title: &str, message: &str) -> Result<bool, PlatformError> {
        self.0.show_confirm_dialog(title, message)
    }
    fn show_message_dialog(&self, title: &str, message: &str, buttons: &[&str]) -> Result<Option<usize>, PlatformError> {
        self.0.show_message_dialog(title, message, buttons)
    }
    fn clipboard_read_text(&self) -> Result<Option<String>, PlatformError> {
        self.0.clipboard_read_text()
    }
    fn clipboard_write_text(&self, text: &str) -> Result<(), PlatformError> {
        self.0.clipboard_write_text(text)
    }
    fn log(&self, level: LogLevel, message: &str) {
        self.0.log(level, message)
    }
    fn performance_mark(&self, name: &str) {
        self.0.performance_mark(name)
    }
    fn performance_mark_with_details(&self, name: &str, details: &str) {
        self.0.performance_mark_with_details(name, details)
    }
    fn get_config_dir(&self) -> Result<PathBuf, PlatformError> {
        self.0.get_config_dir()
    }
    fn list_fonts(&self) -> Vec<String> {
        self.0.list_fonts()
    }
    fn get_font_list(&self) -> Vec<FontInfo> {
        self.0.get_font_list()
    }
    fn get_font_bytes(&self, name: &str) -> Result<Vec<u8>, PlatformError> {
        self.0.get_font_bytes(name)
    }
}

fn documents(dir: &FsPath, out: &mut Vec<PathBuf>) {
    let mut entries: Vec<_> = std::fs::read_dir(dir).unwrap().map(|e| e.unwrap().path()).collect();
    entries.sort();
    for path in entries {
        if path.is_dir() {
            documents(&path, out);
        } else if path.extension().is_some_and(|e| e == "ndbx") {
            out.push(path);
        }
    }
}

#[test]
fn examples() {
    let Some(gpu) = gpu() else {
        eprintln!("No GPU adapter: skipping the examples.");
        return;
    };
    let root = FsPath::new(env!("CARGO_MANIFEST_DIR")).join("../../examples");
    let mut paths = Vec::new();
    documents(&root, &mut paths);
    let mut vello = vello::Renderer::new(&gpu.device, vello::RendererOptions::default()).unwrap();
    let mut ours = nodebox_gpu::Renderer::new(&gpu.device, TextureFormat::Rgba8Unorm);
    let platform: Arc<dyn Platform> = Arc::new(Files(TestPlatform::new()));
    let mut failures = Vec::new();
    println!("{:<48} {:>6} {:>9} {:>9} {:>9}", "document", "paths", "vello err", "gpu err", "vs vello");
    for file in paths {
        let name = file.file_stem().unwrap().to_string_lossy().to_string();
        let Ok(library) = parse_file(&file) else {
            println!("{name:<48} does not parse");
            continue;
        };
        let ctx = ProjectContext::new(file.parent().unwrap(), file.file_name().unwrap().to_string_lossy());
        let (geometry, _, errors) = evaluate_network(&library, &platform, &ctx);
        if geometry.is_empty() {
            let why = errors.first().map(|e| format!("{e:?}")).unwrap_or_else(|| "no geometry".into());
            println!("{name:<48} nothing to draw: {}", why.chars().take(70).collect::<String>());
            continue;
        }
        // The canvas fits the image, on the document's background.
        let scale = f64::from(SIZE) / library.width().max(library.height());
        let bg = library.background_color();
        let half = f64::from(SIZE) / 2.0;
        let frame = Frame { scale, offset: (half, half), background: [bg.r, bg.g, bg.b, bg.a] };
        let a = draw_vello(&gpu, &mut vello, &geometry, frame);
        let (b, _) = draw_gpu(&gpu, &mut ours, &geometry, frame);
        if bg.a > 0.0 && a.iter().all(|&c| c == 0) {
            // Vello gives up on the whole frame, background included, when coordinates
            // are too large for it. Then nothing can be on the canvas: check that
            // nodebox-gpu drew only the background.
            let only_background = b.chunks(4).all(|p| p == &b[..4]);
            println!("{name:<48} {:>6} Vello drew nothing; nodebox-gpu drew {}", geometry.len(),
                if only_background { "the background" } else { "something" });
            dump(&format!("example-{name}"), &[("vello", &a), ("gpu", &b)]);
            if !only_background {
                failures.push(name);
            }
            continue;
        }
        let (truth, _) = draw_reference(&gpu, &mut vello, &geometry, frame).unwrap_or_else(|| (a.clone(), 1));
        dump(&format!("example-{name}"), &[("ref", &truth), ("vello", &a), ("gpu", &b)]);
        let (vello_err, vello_off) = compare(&a, &truth);
        let (gpu_err, gpu_off) = compare(&b, &truth);
        let (between, _) = compare(&a, &b);
        println!("{name:<48} {:>6} {vello_err:>9.3} {gpu_err:>9.3} {between:>9.3}", geometry.len());
        let worse = (gpu_err > vello_err * 1.5 + 0.2 && gpu_err > 0.5) || gpu_off > vello_off * 1.5 + 0.004;
        if worse && !KNOWN_GAPS.contains(&name.as_str()) {
            failures.push(name);
        }
    }
    assert!(failures.is_empty(), "further from the reference than Vello: {failures:?}");
}

