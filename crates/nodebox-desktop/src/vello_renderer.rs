//! Vello renderer wrapper for GPU-accelerated vector rendering.
//!
//! This module provides a high-level interface for rendering NodeBox geometry
//! using Vello's GPU compute pipeline.

use vello::kurbo::{Affine, Stroke};
use vello::peniko::{Brush, Color as PenikoColor, Fill};
use vello::wgpu::{
    Device, Queue, Texture, TextureDescriptor, TextureDimension, TextureFormat, TextureUsages,
};
use vello::{AaConfig, RenderParams, Renderer, RendererOptions, Scene};

use crate::vello_convert::{convert_paths, VelloPath};
use nodebox_core::geometry::{Color, Path};

/// Error type for Vello renderer operations.
#[derive(Debug)]
pub enum VelloError {
    /// Failed to create the Vello renderer.
    RendererCreation(String),
    /// Failed to render the scene.
    RenderFailed(String),
    /// No GPU device available.
    NoDevice,
}

impl std::fmt::Display for VelloError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            VelloError::RendererCreation(msg) => write!(f, "Failed to create renderer: {}", msg),
            VelloError::RenderFailed(msg) => write!(f, "Render failed: {}", msg),
            VelloError::NoDevice => write!(f, "No GPU device available"),
        }
    }
}

impl std::error::Error for VelloError {}

/// Configuration for the Vello renderer.
#[derive(Clone, Debug)]
pub struct VelloConfig {
    /// Width of the render target in pixels.
    pub width: u32,
    /// Height of the render target in pixels.
    pub height: u32,
    /// Background color for the canvas.
    pub background_color: Color,
    /// Anti-aliasing configuration.
    pub antialiasing: AaConfig,
}

impl Default for VelloConfig {
    fn default() -> Self {
        VelloConfig {
            width: 800,
            height: 600,
            background_color: Color::WHITE,
            antialiasing: AaConfig::Msaa16,
        }
    }
}

/// GPU-accelerated vector renderer using Vello.
pub struct VelloRenderer {
    /// The Vello renderer instance.
    renderer: Renderer,
    /// Current render configuration.
    config: VelloConfig,
    /// Render target texture.
    target_texture: Option<Texture>,
}

impl VelloRenderer {
    /// Create a new Vello renderer with the given device and configuration.
    pub fn new(device: &Device, config: VelloConfig) -> Result<Self, VelloError> {
        let renderer = Renderer::new(
            device,
            RendererOptions {
                // vello 0.7 uses default options with optional pipeline cache
                ..Default::default()
            },
        )
        .map_err(|e| VelloError::RendererCreation(format!("{:?}", e)))?;

        Ok(VelloRenderer {
            renderer,
            config,
            target_texture: None,
        })
    }

    /// Resize the render target.
    pub fn resize(&mut self, device: &Device, width: u32, height: u32) {
        if width == 0 || height == 0 {
            return;
        }

        self.config.width = width;
        self.config.height = height;

        // Recreate the target texture
        self.target_texture = Some(Self::create_texture(device, width, height));
    }

    /// Create a render target texture.
    fn create_texture(device: &Device, width: u32, height: u32) -> Texture {
        device.create_texture(&TextureDescriptor {
            label: Some("vello_target"),
            size: vello::wgpu::Extent3d {
                width,
                height,
                depth_or_array_layers: 1,
            },
            mip_level_count: 1,
            sample_count: 1,
            dimension: TextureDimension::D2,
            format: TextureFormat::Rgba8Unorm,
            usage: TextureUsages::STORAGE_BINDING
                | TextureUsages::TEXTURE_BINDING
                | TextureUsages::COPY_SRC,
            view_formats: &[],
        })
    }

    /// Build a Vello scene from NodeBox paths.
    pub fn build_scene(&self, paths: &[Path], transform: Affine) -> Scene {
        let mut scene = Scene::new();
        let vello_paths = convert_paths(paths);

        for vp in &vello_paths {
            self.draw_path(&mut scene, vp, transform);
        }

        scene
    }

    /// Draw a single path to the scene.
    fn draw_path(&self, scene: &mut Scene, vp: &VelloPath, transform: Affine) {
        // Draw fill
        if let Some(fill_color) = vp.style.fill {
            scene.fill(
                Fill::NonZero,
                transform,
                &Brush::Solid(fill_color),
                None,
                &vp.bezpath,
            );
        }

        // Draw stroke
        if let Some(stroke_color) = vp.style.stroke {
            let stroke = Stroke::new(vp.style.stroke_width);
            scene.stroke(
                &stroke,
                transform,
                &Brush::Solid(stroke_color),
                None,
                &vp.bezpath,
            );
        }

        // If no fill or stroke, draw default black stroke
        if vp.style.fill.is_none() && vp.style.stroke.is_none() {
            let stroke = Stroke::new(1.0);
            scene.stroke(
                &stroke,
                transform,
                &Brush::Solid(PenikoColor::BLACK),
                None,
                &vp.bezpath,
            );
        }
    }

    /// Render paths to the target texture.
    ///
    /// Returns the texture for display.
    pub fn render(
        &mut self,
        device: &Device,
        queue: &Queue,
        paths: &[Path],
        transform: Affine,
    ) -> Result<&Texture, VelloError> {
        // Ensure we have a target texture
        if self.target_texture.is_none() {
            self.target_texture =
                Some(Self::create_texture(device, self.config.width, self.config.height));
        }

        let texture = self.target_texture.as_ref().unwrap();
        let view = texture.create_view(&vello::wgpu::TextureViewDescriptor::default());

        // Build the scene
        let scene = self.build_scene(paths, transform);

        // Convert background color
        let bg = crate::vello_convert::color_to_peniko(&self.config.background_color);

        // Render parameters
        let params = RenderParams {
            base_color: bg,
            width: self.config.width,
            height: self.config.height,
            antialiasing_method: self.config.antialiasing,
        };

        // Render to texture
        self.renderer
            .render_to_texture(device, queue, &scene, &view, &params)
            .map_err(|e| VelloError::RenderFailed(format!("{:?}", e)))?;

        Ok(texture)
    }

    /// Get the current render target dimensions.
    pub fn dimensions(&self) -> (u32, u32) {
        (self.config.width, self.config.height)
    }

    /// Get the current configuration.
    pub fn config(&self) -> &VelloConfig {
        &self.config
    }

    /// Set the background color.
    pub fn set_background_color(&mut self, color: Color) {
        self.config.background_color = color;
    }
}

/// Builder for creating view transforms.
///
/// Combines pan, zoom, and canvas centering into a single affine transform.
pub struct ViewTransform {
    /// Pan offset (in screen pixels).
    pub pan: (f64, f64),
    /// Zoom level (1.0 = 100%).
    pub zoom: f64,
    /// Center point of the viewport (in screen pixels).
    pub center: (f64, f64),
}

impl ViewTransform {
    /// Create a new view transform.
    pub fn new(pan: (f64, f64), zoom: f64, center: (f64, f64)) -> Self {
        ViewTransform { pan, zoom, center }
    }

    /// Convert to a kurbo Affine transform.
    ///
    /// The transform maps world coordinates to screen coordinates:
    /// 1. Scale by zoom factor
    /// 2. Translate by pan offset
    /// 3. Translate to center of viewport
    pub fn to_affine(&self) -> Affine {
        Affine::translate((self.center.0 + self.pan.0, self.center.1 + self.pan.1))
            * Affine::scale(self.zoom)
    }
}

impl Default for ViewTransform {
    fn default() -> Self {
        ViewTransform {
            pan: (0.0, 0.0),
            zoom: 1.0,
            center: (0.0, 0.0),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use vello::kurbo::Point;

    #[test]
    fn test_view_transform_identity() {
        let vt = ViewTransform::new((0.0, 0.0), 1.0, (0.0, 0.0));
        let affine = vt.to_affine();

        // Should be approximately identity
        let coeffs = affine.as_coeffs();
        assert!((coeffs[0] - 1.0).abs() < 0.001);
        assert!((coeffs[3] - 1.0).abs() < 0.001);
    }

    #[test]
    fn test_view_transform_zoom() {
        let vt = ViewTransform::new((0.0, 0.0), 2.0, (0.0, 0.0));
        let affine = vt.to_affine();

        // Point at (10, 10) should map to (20, 20)
        let p = affine * Point::new(10.0, 10.0);
        assert!((p.x - 20.0).abs() < 0.001);
        assert!((p.y - 20.0).abs() < 0.001);
    }

    #[test]
    fn test_view_transform_pan() {
        let vt = ViewTransform::new((100.0, 50.0), 1.0, (0.0, 0.0));
        let affine = vt.to_affine();

        // Point at (0, 0) should map to (100, 50)
        let p = affine * Point::new(0.0, 0.0);
        assert!((p.x - 100.0).abs() < 0.001);
        assert!((p.y - 50.0).abs() < 0.001);
    }

    #[test]
    fn test_vello_config_default() {
        let config = VelloConfig::default();
        assert_eq!(config.width, 800);
        assert_eq!(config.height, 600);
    }
}
