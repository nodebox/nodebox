//! Vello-based viewer widget for GPU-accelerated vector rendering.
//!
//! This module provides a viewer widget that uses Vello for GPU rendering
//! and displays the result in egui.
//!
//! With egui 0.33+ and vello 0.7+, both use wgpu 27, allowing us to share
//! the GPU device and textures directly without CPU copies.

use eframe::egui;
use egui_wgpu::RenderState;
use nodebox_core::geometry::{Color, Path};
use vello::kurbo::Affine;
use vello::peniko::Color as PenikoColor;
use vello::wgpu;
use vello::{AaConfig, RenderParams, Renderer, RendererOptions, Scene};

use crate::vello_convert::convert_paths;

/// Error type for Vello viewer operations.
#[derive(Debug)]
pub enum VelloViewerError {
    /// Failed to create Vello renderer.
    RendererCreation(String),
    /// Failed to render.
    RenderFailed(String),
}

impl std::fmt::Display for VelloViewerError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            VelloViewerError::RendererCreation(e) => write!(f, "Failed to create renderer: {}", e),
            VelloViewerError::RenderFailed(e) => write!(f, "Render failed: {}", e),
        }
    }
}

impl std::error::Error for VelloViewerError {}

/// Cached GPU resources for a specific texture size.
struct CachedTexture {
    /// Width of the cached texture.
    width: u32,
    /// Height of the cached texture.
    height: u32,
    /// Render target texture (shared with egui).
    /// Note: We keep the texture alive so the view remains valid.
    #[allow(dead_code)]
    texture: wgpu::Texture,
    /// Render target texture view.
    texture_view: wgpu::TextureView,
    /// egui texture ID for displaying this texture.
    egui_texture_id: egui::TextureId,
}

impl CachedTexture {
    fn new(
        device: &wgpu::Device,
        renderer: &mut egui_wgpu::Renderer,
        width: u32,
        height: u32,
    ) -> Self {
        // Create texture with both STORAGE_BINDING (for Vello) and TEXTURE_BINDING (for egui)
        let texture = device.create_texture(&wgpu::TextureDescriptor {
            label: Some("vello_shared_texture"),
            size: wgpu::Extent3d {
                width,
                height,
                depth_or_array_layers: 1,
            },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8Unorm,
            usage: wgpu::TextureUsages::STORAGE_BINDING
                | wgpu::TextureUsages::TEXTURE_BINDING
                | wgpu::TextureUsages::COPY_DST,
            view_formats: &[],
        });

        let texture_view = texture.create_view(&wgpu::TextureViewDescriptor::default());

        // Register this texture with egui so it can display it directly
        let egui_texture_id = renderer.register_native_texture(
            device,
            &texture_view,
            wgpu::FilterMode::Linear,
        );

        CachedTexture {
            width,
            height,
            texture,
            texture_view,
            egui_texture_id,
        }
    }

    fn matches_size(&self, width: u32, height: u32) -> bool {
        self.width == width && self.height == height
    }
}

/// Cache key for determining when to re-render.
#[derive(Clone, Debug, PartialEq)]
struct CacheKey {
    width: u32,
    height: u32,
    pan_x: i32, // Stored as fixed-point to avoid float comparison issues
    pan_y: i32,
    zoom: i32,         // Stored as fixed-point (zoom * 1000)
    geometry_hash: u64,
    scale_factor: i32, // pixels_per_point * 100
    bg_color: [u8; 4], // Background color as RGBA bytes
}

impl CacheKey {
    fn new(
        width: u32,
        height: u32,
        pan_x: f32,
        pan_y: f32,
        zoom: f32,
        geometry_hash: u64,
        scale_factor: f32,
        background_color: &Color,
    ) -> Self {
        CacheKey {
            width,
            height,
            pan_x: (pan_x * 10.0) as i32,
            pan_y: (pan_y * 10.0) as i32,
            zoom: (zoom * 1000.0) as i32,
            geometry_hash,
            scale_factor: (scale_factor * 100.0) as i32,
            bg_color: [
                (background_color.r * 255.0) as u8,
                (background_color.g * 255.0) as u8,
                (background_color.b * 255.0) as u8,
                (background_color.a * 255.0) as u8,
            ],
        }
    }
}

/// Vello-based viewer for GPU-accelerated vector rendering.
///
/// Uses egui's wgpu device directly for zero-copy texture sharing.
pub struct VelloViewer {
    /// Vello renderer (lazily initialized with egui's device).
    renderer: Option<Renderer>,
    /// Cached GPU texture (shared with egui).
    cached_texture: Option<CachedTexture>,
    /// Cache key for the current render.
    cache_key: Option<CacheKey>,
    /// Background color.
    background_color: Color,
    /// Whether initialization failed.
    init_failed: bool,
    /// Error message if initialization failed.
    init_error: Option<String>,
}

impl Default for VelloViewer {
    fn default() -> Self {
        Self::new()
    }
}

impl VelloViewer {
    /// Create a new Vello viewer.
    pub fn new() -> Self {
        VelloViewer {
            renderer: None,
            cached_texture: None,
            cache_key: None,
            background_color: Color::WHITE,
            init_failed: false,
            init_error: None,
        }
    }

    /// Set the background color.
    pub fn set_background_color(&mut self, color: Color) {
        self.background_color = color;
    }

    /// Ensure the Vello renderer is initialized with egui's device.
    fn ensure_renderer(&mut self, device: &wgpu::Device) -> bool {
        if self.init_failed {
            return false;
        }

        if self.renderer.is_some() {
            return true;
        }

        match Renderer::new(device, RendererOptions::default()) {
            Ok(renderer) => {
                self.renderer = Some(renderer);
                true
            }
            Err(e) => {
                log::error!("Failed to create Vello renderer: {:?}", e);
                self.init_failed = true;
                self.init_error = Some(format!("{:?}", e));
                false
            }
        }
    }

    /// Render paths to a scene.
    fn build_scene(&self, paths: &[Path], transform: Affine) -> Scene {
        let mut scene = Scene::new();
        let vello_paths = convert_paths(paths);

        for vp in &vello_paths {
            // Draw fill
            if let Some(fill_color) = vp.style.fill {
                scene.fill(
                    vello::peniko::Fill::NonZero,
                    transform,
                    &vello::peniko::Brush::Solid(fill_color),
                    None,
                    &vp.bezpath,
                );
            }

            // Draw stroke
            if let Some(stroke_color) = vp.style.stroke {
                let stroke = vello::kurbo::Stroke::new(vp.style.stroke_width);
                scene.stroke(
                    &stroke,
                    transform,
                    &vello::peniko::Brush::Solid(stroke_color),
                    None,
                    &vp.bezpath,
                );
            }

            // Default stroke if no fill or stroke
            if vp.style.fill.is_none() && vp.style.stroke.is_none() {
                let stroke = vello::kurbo::Stroke::new(1.0);
                scene.stroke(
                    &stroke,
                    transform,
                    &vello::peniko::Brush::Solid(PenikoColor::BLACK),
                    None,
                    &vp.bezpath,
                );
            }
        }

        scene
    }

    /// Render and display in egui using the shared wgpu device.
    ///
    /// Parameters:
    /// - `render_state`: egui's wgpu render state (provides device/queue)
    /// - `ui`: The egui UI context
    /// - `paths`: The geometry to render
    /// - `pan`: Pan offset in logical pixels
    /// - `zoom`: Zoom level (1.0 = 100%)
    /// - `rect`: The screen rectangle to render into
    /// - `geometry_hash`: Hash of the geometry for cache invalidation
    ///
    /// Returns true if GPU rendering was used, false if fallback is needed.
    pub fn render(
        &mut self,
        render_state: &RenderState,
        ui: &mut egui::Ui,
        paths: &[Path],
        pan: egui::Vec2,
        zoom: f32,
        rect: egui::Rect,
        geometry_hash: u64,
    ) -> bool {
        let device = &render_state.device;
        let queue = &render_state.queue;

        // Get scale factor for HiDPI support
        let scale_factor = ui.ctx().pixels_per_point();

        // Calculate texture dimensions in physical pixels
        let physical_width = (rect.width() * scale_factor).max(1.0) as u32;
        let physical_height = (rect.height() * scale_factor).max(1.0) as u32;

        // Create cache key
        let new_cache_key = CacheKey::new(
            physical_width,
            physical_height,
            pan.x,
            pan.y,
            zoom,
            geometry_hash,
            scale_factor,
            &self.background_color,
        );

        // Check if we need to re-render
        let needs_render = self.cache_key.as_ref() != Some(&new_cache_key);

        if needs_render {
            // Ensure Vello renderer is initialized
            if !self.ensure_renderer(device) {
                if let Some(ref err) = self.init_error {
                    ui.painter().rect_filled(
                        rect,
                        0.0,
                        egui::Color32::from_rgb(40, 40, 40),
                    );
                    ui.painter().text(
                        rect.center(),
                        egui::Align2::CENTER_CENTER,
                        format!("GPU rendering unavailable: {}", err),
                        egui::FontId::default(),
                        egui::Color32::RED,
                    );
                }
                return false;
            }

            // Get or create cached texture for this size
            let needs_new_texture = self
                .cached_texture
                .as_ref()
                .map(|t| !t.matches_size(physical_width, physical_height))
                .unwrap_or(true);

            if needs_new_texture {
                // Unregister old texture if exists
                if let Some(old_texture) = self.cached_texture.take() {
                    render_state
                        .renderer
                        .write()
                        .free_texture(&old_texture.egui_texture_id);
                }

                // Create new shared texture
                self.cached_texture = Some(CachedTexture::new(
                    device,
                    &mut render_state.renderer.write(),
                    physical_width,
                    physical_height,
                ));
            }

            let cached_texture = self.cached_texture.as_ref().unwrap();

            // Build transform in texture-local coordinates
            let center_x = physical_width as f64 / 2.0;
            let center_y = physical_height as f64 / 2.0;
            let scaled_pan_x = pan.x as f64 * scale_factor as f64;
            let scaled_pan_y = pan.y as f64 * scale_factor as f64;
            let physical_zoom = zoom as f64 * scale_factor as f64;

            let transform = Affine::translate((center_x + scaled_pan_x, center_y + scaled_pan_y))
                * Affine::scale(physical_zoom);

            // Build scene
            let scene = self.build_scene(paths, transform);
            let bg = crate::vello_convert::color_to_peniko(&self.background_color);

            // Render with Vello directly to the shared texture
            let params = RenderParams {
                base_color: bg,
                width: physical_width,
                height: physical_height,
                antialiasing_method: AaConfig::Msaa16,
            };

            let renderer = self.renderer.as_mut().unwrap();
            if let Err(e) = renderer.render_to_texture(
                device,
                queue,
                &scene,
                &cached_texture.texture_view,
                &params,
            ) {
                log::error!("Vello render failed: {:?}", e);
                return false;
            }

            self.cache_key = Some(new_cache_key);
        }

        // Display the texture directly (no CPU copy needed!)
        if let Some(ref cached_texture) = self.cached_texture {
            ui.painter().image(
                cached_texture.egui_texture_id,
                rect,
                egui::Rect::from_min_max(egui::pos2(0.0, 0.0), egui::pos2(1.0, 1.0)),
                egui::Color32::WHITE,
            );
            true
        } else {
            false
        }
    }

    /// Force a re-render on the next frame.
    pub fn invalidate(&mut self) {
        self.cache_key = None;
    }

    /// Check if GPU rendering is available.
    pub fn is_available(&self) -> bool {
        !self.init_failed
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_vello_viewer_new() {
        let viewer = VelloViewer::new();
        assert!(viewer.renderer.is_none());
        assert!(!viewer.init_failed);
    }

    #[test]
    fn test_vello_viewer_set_background() {
        let mut viewer = VelloViewer::new();
        viewer.set_background_color(Color::rgb(0.5, 0.5, 0.5));
        assert_eq!(viewer.background_color.r, 0.5);
    }

    #[test]
    fn test_cache_key_equality() {
        let white = Color::WHITE;
        let key1 = CacheKey::new(100, 100, 0.0, 0.0, 1.0, 12345, 2.0, &white);
        let key2 = CacheKey::new(100, 100, 0.0, 0.0, 1.0, 12345, 2.0, &white);
        let key3 = CacheKey::new(100, 100, 1.0, 0.0, 1.0, 12345, 2.0, &white);

        assert_eq!(key1, key2);
        assert_ne!(key1, key3);

        // Different background color should invalidate cache
        let red = Color::rgb(1.0, 0.0, 0.0);
        let key4 = CacheKey::new(100, 100, 0.0, 0.0, 1.0, 12345, 2.0, &red);
        assert_ne!(key1, key4);
    }
}
