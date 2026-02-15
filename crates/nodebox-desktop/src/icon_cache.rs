//! Shared icon cache for loading and displaying node icons.

use eframe::egui::{self, Color32, ColorImage, Pos2, Rect, Stroke, TextureHandle, TextureOptions, Vec2};
use std::collections::{HashMap, HashSet};
use std::path::PathBuf;

/// Cache for node icons loaded from libraries directory.
pub struct IconCache {
    /// Loaded icon textures, keyed by function name (e.g., "corevector/ellipse").
    textures: HashMap<String, TextureHandle>,
    /// Path to the libraries directory.
    libraries_path: Option<PathBuf>,
    /// Set of icons that failed to load (don't retry).
    failed: HashSet<String>,
}

impl Default for IconCache {
    fn default() -> Self {
        Self::new()
    }
}

impl IconCache {
    /// Create a new icon cache.
    pub fn new() -> Self {
        // Try to find the libraries directory
        let libraries_path = Self::find_libraries_path();
        Self {
            textures: HashMap::new(),
            libraries_path,
            failed: HashSet::new(),
        }
    }

    /// Find the libraries directory (next to executable or in development paths).
    fn find_libraries_path() -> Option<PathBuf> {
        // Try relative to executable
        if let Ok(exe_path) = std::env::current_exe() {
            // In macOS bundle: NodeBox.app/Contents/MacOS/NodeBox
            // Libraries at: NodeBox.app/Contents/Resources/libraries
            if let Some(parent) = exe_path.parent() {
                let bundle_resources = parent.parent().map(|p| p.join("Resources/libraries"));
                if let Some(ref path) = bundle_resources {
                    if path.exists() {
                        return Some(path.clone());
                    }
                }
            }
        }

        // Try current directory
        let cwd_libs = PathBuf::from("libraries");
        if cwd_libs.exists() {
            return Some(cwd_libs);
        }

        // Try relative to project root (development)
        for path in ["./libraries", "../libraries", "../../libraries"] {
            let p = PathBuf::from(path);
            if p.exists() {
                return Some(p);
            }
        }

        None
    }

    /// Get or load an icon for the given function name.
    pub fn get_icon(&mut self, ctx: &egui::Context, function: &str) -> Option<&TextureHandle> {
        // Check if already loaded
        if self.textures.contains_key(function) {
            return self.textures.get(function);
        }

        // Check if previously failed
        if self.failed.contains(function) {
            return None;
        }

        // Try to load the icon
        if let Some(ref libs_path) = self.libraries_path {
            // Function is like "corevector/ellipse" -> "libraries/corevector/ellipse.png"
            let icon_path = libs_path.join(format!("{}.png", function));
            if icon_path.exists() {
                if let Ok(image_data) = std::fs::read(&icon_path) {
                    if let Ok(image) = image::load_from_memory(&image_data) {
                        let rgba = image.to_rgba8();
                        let size = [rgba.width() as usize, rgba.height() as usize];
                        let pixels = rgba.into_raw();
                        let color_image = ColorImage::from_rgba_unmultiplied(size, &pixels);
                        let texture = ctx.load_texture(
                            function,
                            color_image,
                            TextureOptions::LINEAR,
                        );
                        self.textures.insert(function.to_string(), texture);
                        return self.textures.get(function);
                    }
                }
            }
        }

        // Mark as failed
        self.failed.insert(function.to_string());
        None
    }

    /// Draw a node icon, loading from libraries if available.
    /// Returns true if an icon was drawn, false if fallback should be used.
    pub fn draw_icon(
        &mut self,
        ctx: &egui::Context,
        painter: &egui::Painter,
        pos: Pos2,
        size: f32,
        function: Option<&str>,
        category: &str,
    ) {
        let rect = Rect::from_min_size(pos, Vec2::splat(size));

        // Try to load icon from file if we have a function name
        if let Some(func) = function {
            if let Some(texture) = self.get_icon(ctx, func) {
                // Draw the texture
                let uv = Rect::from_min_max(Pos2::new(0.0, 0.0), Pos2::new(1.0, 1.0));
                let tint = Color32::WHITE;
                painter.image(texture.id(), rect, uv, tint);
                return;
            }
        }

        // Fallback to procedural icon based on category
        Self::draw_fallback_icon(painter, rect, category, 1.0);
    }

    /// Draw a node icon at a specific size with zoom factor.
    pub fn draw_icon_zoomed(
        &mut self,
        ctx: &egui::Context,
        painter: &egui::Painter,
        pos: Pos2,
        size: f32,
        zoom: f32,
        function: Option<&str>,
        category: &str,
    ) {
        let rect = Rect::from_min_size(pos, Vec2::splat(size));

        // Try to load icon from file if we have a function name
        if let Some(func) = function {
            if let Some(texture) = self.get_icon(ctx, func) {
                // Draw the texture
                let uv = Rect::from_min_max(Pos2::new(0.0, 0.0), Pos2::new(1.0, 1.0));
                let tint = Color32::WHITE;
                painter.image(texture.id(), rect, uv, tint);
                return;
            }
        }

        // Fallback to procedural icon based on category
        Self::draw_fallback_icon(painter, rect, category, zoom);
    }

    /// Draw a fallback procedural icon based on category.
    pub fn draw_fallback_icon(painter: &egui::Painter, rect: Rect, category: &str, zoom: f32) {
        let size = rect.width();
        let icon_color = Color32::from_rgba_unmultiplied(255, 255, 255, 200);

        match category.to_lowercase().as_str() {
            "corevector" | "geometry" => {
                // Diamond shape
                let center = rect.center();
                let r = size * 0.35;
                let points = vec![
                    Pos2::new(center.x, center.y - r),
                    Pos2::new(center.x + r, center.y),
                    Pos2::new(center.x, center.y + r),
                    Pos2::new(center.x - r, center.y),
                ];
                painter.add(egui::Shape::convex_polygon(points, icon_color, Stroke::NONE));
            }
            "transform" => {
                // Arrow shape
                let center = rect.center();
                let r = size * 0.3;
                let points = vec![
                    Pos2::new(center.x, center.y - r),
                    Pos2::new(center.x + r, center.y + r * 0.5),
                    Pos2::new(center.x, center.y),
                    Pos2::new(center.x - r, center.y + r * 0.5),
                ];
                painter.add(egui::Shape::convex_polygon(points, icon_color, Stroke::NONE));
            }
            "color" => {
                // Circle
                let center = rect.center();
                painter.circle_filled(center, size * 0.35, icon_color);
            }
            "math" => {
                // Plus sign
                let center = rect.center();
                let r = size * 0.3;
                let stroke = Stroke::new(2.0 * zoom, icon_color);
                painter.line_segment(
                    [
                        Pos2::new(center.x - r, center.y),
                        Pos2::new(center.x + r, center.y),
                    ],
                    stroke,
                );
                painter.line_segment(
                    [
                        Pos2::new(center.x, center.y - r),
                        Pos2::new(center.x, center.y + r),
                    ],
                    stroke,
                );
            }
            "list" => {
                // Three horizontal lines
                let stroke = Stroke::new(2.0 * zoom, icon_color);
                for i in 0..3 {
                    let y = rect.top() + size * (0.3 + 0.2 * i as f32);
                    painter.line_segment(
                        [
                            Pos2::new(rect.left() + size * 0.2, y),
                            Pos2::new(rect.right() - size * 0.2, y),
                        ],
                        stroke,
                    );
                }
            }
            "string" => {
                // "A" letter outline
                let center = rect.center();
                painter.text(
                    center,
                    egui::Align2::CENTER_CENTER,
                    "A",
                    egui::FontId::proportional(size * 0.6),
                    icon_color,
                );
            }
            "data" => {
                // Database cylinder (simplified as stacked ovals)
                let cx = rect.center().x;
                let top = rect.top() + size * 0.25;
                let bot = rect.bottom() - size * 0.25;
                let w = size * 0.35;
                let stroke = Stroke::new(1.5 * zoom, icon_color);
                painter.line_segment([Pos2::new(cx - w, top), Pos2::new(cx - w, bot)], stroke);
                painter.line_segment([Pos2::new(cx + w, top), Pos2::new(cx + w, bot)], stroke);
                painter.line_segment([Pos2::new(cx - w, top), Pos2::new(cx + w, top)], stroke);
                painter.line_segment([Pos2::new(cx - w, bot), Pos2::new(cx + w, bot)], stroke);
            }
            _ => {
                // Default: small filled rounded rect
                painter.rect_filled(rect.shrink(size * 0.2), 2.0 * zoom, icon_color);
            }
        }
    }
}
