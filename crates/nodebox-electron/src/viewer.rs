//! The viewer: draws the geometry of the last evaluation with Vello, through
//! the page's WebGPU.
//!
//! The paths stay in WebAssembly memory between evaluation and drawing, so
//! drawing does not parse or copy them. All coordinates here are in physical
//! pixels; the page applies the device pixel ratio.

use std::cell::RefCell;

use nodebox_vello::build_scene;
use vello::kurbo::Affine;
use vello::peniko::Color;
use vello::wgpu;
use vello::{AaConfig, RenderParams, Renderer, RendererOptions};
use wasm_bindgen::prelude::*;

use crate::last_paths;

struct Viewer {
    device: wgpu::Device,
    queue: wgpu::Queue,
    surface: wgpu::Surface<'static>,
    config: wgpu::SurfaceConfiguration,
    renderer: Renderer,
    /// Vello renders into this Rgba8Unorm texture; it is then blitted to the canvas.
    target: wgpu::TextureView,
    blitter: wgpu::util::TextureBlitter,
}

thread_local! {
    static VIEWER: RefCell<Option<Viewer>> = const { RefCell::new(None) };
}

fn error(error: impl std::fmt::Debug) -> JsError {
    JsError::new(&format!("{error:?}"))
}

fn create_target(device: &wgpu::Device, width: u32, height: u32, readable: bool) -> wgpu::Texture {
    let mut usage = wgpu::TextureUsages::STORAGE_BINDING | wgpu::TextureUsages::TEXTURE_BINDING;
    if readable {
        usage |= wgpu::TextureUsages::COPY_SRC;
    }
    device.create_texture(&wgpu::TextureDescriptor {
        label: Some("vello target"),
        size: wgpu::Extent3d {
            width,
            height,
            depth_or_array_layers: 1,
        },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: wgpu::TextureFormat::Rgba8Unorm,
        usage,
        view_formats: &[],
    })
}

fn render_params(width: u32, height: u32, background: [u8; 3]) -> RenderParams {
    RenderParams {
        base_color: Color::from_rgba8(background[0], background[1], background[2], 255),
        width,
        height,
        antialiasing_method: AaConfig::Area,
    }
}

/// The transform from document coordinates to pixels: `scale`, then the origin at `offset`.
fn view_transform(offset_x: f64, offset_y: f64, scale: f64) -> Affine {
    Affine::translate((offset_x, offset_y)) * Affine::scale(scale)
}

/// Create the WebGPU device and the Vello renderer for `canvas`.
///
/// Fails when the page has no WebGPU adapter.
#[wasm_bindgen]
pub async fn viewer_init(canvas: web_sys::HtmlCanvasElement) -> Result<(), JsError> {
    let width = canvas.width().max(1);
    let height = canvas.height().max(1);

    let instance = wgpu::Instance::default();
    let surface = instance
        .create_surface(wgpu::SurfaceTarget::Canvas(canvas))
        .map_err(error)?;
    let adapter = instance
        .request_adapter(&wgpu::RequestAdapterOptions {
            compatible_surface: Some(&surface),
            ..Default::default()
        })
        .await
        .map_err(error)?;
    let (device, queue) = adapter
        .request_device(&wgpu::DeviceDescriptor::default())
        .await
        .map_err(error)?;

    let mut config = surface
        .get_default_config(&adapter, width, height)
        .ok_or_else(|| JsError::new("the canvas is not supported by the GPU adapter"))?;
    // Vello produces sRGB-encoded values, so the surface must not encode again.
    config.format = config.format.remove_srgb_suffix();
    surface.configure(&device, &config);

    let renderer = Renderer::new(&device, RendererOptions::default()).map_err(error)?;
    let target = create_target(&device, width, height, false)
        .create_view(&wgpu::TextureViewDescriptor::default());
    let blitter = wgpu::util::TextureBlitter::new(&device, config.format);

    VIEWER.with(|viewer| {
        *viewer.borrow_mut() = Some(Viewer {
            device,
            queue,
            surface,
            config,
            renderer,
            target,
            blitter,
        });
    });
    Ok(())
}

/// Draw the geometry of the last evaluation to the canvas.
///
/// `width` and `height` are the size of the canvas buffer. Does nothing until
/// `viewer_init` has finished.
#[wasm_bindgen]
#[allow(clippy::too_many_arguments)]
pub fn viewer_render(
    width: u32,
    height: u32,
    offset_x: f64,
    offset_y: f64,
    scale: f64,
    background_r: u8,
    background_g: u8,
    background_b: u8,
) -> Result<(), JsError> {
    VIEWER.with(|viewer| {
        let mut viewer = viewer.borrow_mut();
        let Some(viewer) = viewer.as_mut() else {
            return Ok(());
        };
        if width == 0 || height == 0 {
            return Ok(());
        }
        if viewer.config.width != width || viewer.config.height != height {
            viewer.config.width = width;
            viewer.config.height = height;
            viewer.surface.configure(&viewer.device, &viewer.config);
            viewer.target = create_target(&viewer.device, width, height, false)
                .create_view(&wgpu::TextureViewDescriptor::default());
        }

        let scene = build_scene(&last_paths(), view_transform(offset_x, offset_y, scale));
        let params = render_params(width, height, [background_r, background_g, background_b]);
        viewer
            .renderer
            .render_to_texture(&viewer.device, &viewer.queue, &scene, &viewer.target, &params)
            .map_err(error)?;

        let frame = viewer.surface.get_current_texture().map_err(error)?;
        let view = frame
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());
        let mut encoder = viewer
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor::default());
        viewer
            .blitter
            .copy(&viewer.device, &mut encoder, &viewer.target, &view);
        viewer.queue.submit([encoder.finish()]);
        frame.present();
        Ok(())
    })
}

/// Render the geometry of the last evaluation offscreen and return its pixels
/// as RGBA bytes, row by row. Used for PNG export and by tests.
#[wasm_bindgen]
#[allow(clippy::too_many_arguments)]
pub async fn viewer_render_pixels(
    width: u32,
    height: u32,
    offset_x: f64,
    offset_y: f64,
    scale: f64,
    background_r: u8,
    background_g: u8,
    background_b: u8,
) -> Result<Vec<u8>, JsError> {
    if width == 0 || height == 0 {
        return Err(JsError::new("the image has no pixels"));
    }
    // 256 is wgpu's required alignment for the rows of a texture copy.
    let padded_row = (width * 4).div_ceil(256) * 256;

    // The renderer is borrowed only while the work is submitted, not while
    // waiting for the GPU, so the canvas can keep drawing.
    let buffer = VIEWER.with(|viewer| -> Result<wgpu::Buffer, JsError> {
        let mut viewer = viewer.borrow_mut();
        let viewer = viewer
            .as_mut()
            .ok_or_else(|| JsError::new("the viewer is not initialized"))?;

        let texture = create_target(&viewer.device, width, height, true);
        let view = texture.create_view(&wgpu::TextureViewDescriptor::default());
        let scene = build_scene(&last_paths(), view_transform(offset_x, offset_y, scale));
        let params = render_params(width, height, [background_r, background_g, background_b]);
        viewer
            .renderer
            .render_to_texture(&viewer.device, &viewer.queue, &scene, &view, &params)
            .map_err(error)?;

        let buffer = viewer.device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("pixel readback"),
            size: u64::from(padded_row) * u64::from(height),
            usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::MAP_READ,
            mapped_at_creation: false,
        });
        let mut encoder = viewer
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor::default());
        encoder.copy_texture_to_buffer(
            texture.as_image_copy(),
            wgpu::TexelCopyBufferInfo {
                buffer: &buffer,
                layout: wgpu::TexelCopyBufferLayout {
                    offset: 0,
                    bytes_per_row: Some(padded_row),
                    rows_per_image: None,
                },
            },
            wgpu::Extent3d {
                width,
                height,
                depth_or_array_layers: 1,
            },
        );
        viewer.queue.submit([encoder.finish()]);
        Ok(buffer)
    })?;

    let (sender, receiver) = futures_channel::oneshot::channel();
    buffer.slice(..).map_async(wgpu::MapMode::Read, move |result| {
        let _ = sender.send(result);
    });
    receiver.await.map_err(error)?.map_err(error)?;

    let mapped = buffer.slice(..).get_mapped_range();
    let row = (width * 4) as usize;
    let mut pixels = Vec::with_capacity(row * height as usize);
    for line in mapped.chunks(padded_row as usize) {
        pixels.extend_from_slice(&line[..row]);
    }
    drop(mapped);
    buffer.unmap();
    Ok(pixels)
}
