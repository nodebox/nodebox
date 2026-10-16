//! Shared by the comparison tests: a GPU device, both renderers, and the pixel comparison.

#![allow(dead_code)]

use std::time::{Duration, Instant};

use nodebox_core::geometry::Path;
use wgpu::TextureFormat;

pub const SIZE: u32 = 512;

pub struct Gpu {
    pub device: wgpu::Device,
    pub queue: wgpu::Queue,
}

pub fn gpu() -> Option<Gpu> {
    let instance = wgpu::Instance::default();
    let adapter = pollster::block_on(instance.request_adapter(&Default::default())).ok()?;
    let (device, queue) = pollster::block_on(adapter.request_device(&Default::default())).ok()?;
    Some(Gpu { device, queue })
}

pub fn target(gpu: &Gpu, usage: wgpu::TextureUsages, size: u32) -> wgpu::Texture {
    gpu.device.create_texture(&wgpu::TextureDescriptor {
        label: None,
        size: wgpu::Extent3d { width: size, height: size, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: TextureFormat::Rgba8Unorm,
        usage: usage | wgpu::TextureUsages::COPY_SRC,
        view_formats: &[],
    })
}

pub fn read(gpu: &Gpu, texture: &wgpu::Texture, size: u32) -> Vec<u8> {
    let buffer = gpu.device.create_buffer(&wgpu::BufferDescriptor {
        label: None,
        size: u64::from(size) * u64::from(size) * 4,
        usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::MAP_READ,
        mapped_at_creation: false,
    });
    let mut encoder = gpu.device.create_command_encoder(&Default::default());
    encoder.copy_texture_to_buffer(
        texture.as_image_copy(),
        wgpu::TexelCopyBufferInfo {
            buffer: &buffer,
            layout: wgpu::TexelCopyBufferLayout { offset: 0, bytes_per_row: Some(size * 4), rows_per_image: None },
        },
        wgpu::Extent3d { width: size, height: size, depth_or_array_layers: 1 },
    );
    gpu.queue.submit([encoder.finish()]);
    buffer.slice(..).map_async(wgpu::MapMode::Read, |_| {});
    gpu.device.poll(wgpu::PollType::wait_indefinitely()).unwrap();
    let pixels = buffer.slice(..).get_mapped_range().to_vec();
    pixels
}

/// Where the document lands: `scale` pixels per unit, the origin at `offset`,
/// on `background` (straight RGBA; alpha 0 is a transparent target).
#[derive(Clone, Copy)]
pub struct Frame {
    pub scale: f64,
    pub offset: (f64, f64),
    pub background: [f64; 4],
}

impl Frame {
    pub const WHITE: [f64; 4] = [1.0, 1.0, 1.0, 1.0];

    /// The origin in the middle, on white.
    pub fn centered(scale: f64) -> Self {
        let half = f64::from(SIZE) / 2.0;
        Frame { scale, offset: (half, half), background: Self::WHITE }
    }
}

pub fn draw_vello(gpu: &Gpu, renderer: &mut vello::Renderer, paths: &[Path], frame: Frame) -> Vec<u8> {
    draw_vello_at(gpu, renderer, paths, frame, 1)
}

/// Vello at `factor` times the resolution.
fn draw_vello_at(gpu: &Gpu, renderer: &mut vello::Renderer, paths: &[Path], frame: Frame, factor: u32) -> Vec<u8> {
    let f = f64::from(factor);
    let transform = vello::kurbo::Affine::scale(f)
        * vello::kurbo::Affine::translate(frame.offset)
        * vello::kurbo::Affine::scale(frame.scale);
    let scene = nodebox_vello::build_scene(paths, transform);
    let size = SIZE * factor;
    let texture = target(gpu, wgpu::TextureUsages::STORAGE_BINDING, size);
    let [r, g, b, a] = frame.background;
    let params = vello::RenderParams {
        base_color: vello::peniko::Color::new([r as f32, g as f32, b as f32, a as f32]),
        width: size,
        height: size,
        antialiasing_method: vello::AaConfig::Area,
    };
    renderer
        .render_to_texture(&gpu.device, &gpu.queue, &scene, &texture.create_view(&Default::default()), &params)
        .unwrap();
    read(gpu, &texture, size)
}

/// What an ideal renderer would draw: Vello at 8 times the resolution, each
/// 8x8 block averaged. Strokes keep their width in document units, so the
/// picture is the same, only sampled 64 times per pixel. Vello draws nothing
/// when a scene outgrows its buffers, which happens at this size first; then
/// the reference is drawn at 4 times, and None if that fails too.
pub fn draw_reference(gpu: &Gpu, renderer: &mut vello::Renderer, paths: &[Path], frame: Frame) -> Option<(Vec<u8>, usize)> {
    [8, 4].into_iter().find_map(|f| {
        let out = supersampled(gpu, renderer, paths, frame, f);
        out.iter().any(|&c| c != 0).then_some((out, f))
    })
}

fn supersampled(gpu: &Gpu, renderer: &mut vello::Renderer, paths: &[Path], frame: Frame, factor: usize) -> Vec<u8> {
    let f = factor;
    let big = draw_vello_at(gpu, renderer, paths, frame, f as u32);
    let size = SIZE as usize;
    let mut out = vec![0u8; size * size * 4];
    for y in 0..size {
        for x in 0..size {
            // Average premultiplied, since Vello writes straight alpha.
            let mut sum = [0u32; 4];
            for dy in 0..f {
                let row = (y * f + dy) * size * f;
                for dx in 0..f {
                    let i = (row + x * f + dx) * 4;
                    let a = u32::from(big[i + 3]);
                    for c in 0..3 {
                        sum[c] += u32::from(big[i + c]) * a;
                    }
                    sum[3] += a;
                }
            }
            let o = (y * size + x) * 4;
            let n = (f * f) as u32;
            for c in 0..3 {
                out[o + c] = if sum[3] == 0 { 0 } else { ((sum[c] + sum[3] / 2) / sum[3]) as u8 };
            }
            out[o + 3] = ((sum[3] + n / 2) / n) as u8;
        }
    }
    out
}

/// Time spent preparing (CPU) and drawing (GPU, waited for).
#[derive(Clone, Copy, Default)]
pub struct Timing {
    pub prepare: Duration,
    pub draw: Duration,
}

pub fn draw_gpu(gpu: &Gpu, renderer: &mut nodebox_gpu::Renderer, paths: &[Path], frame: Frame) -> (Vec<u8>, Timing) {
    let texture = target(gpu, wgpu::TextureUsages::RENDER_ATTACHMENT, SIZE);
    let view = nodebox_gpu::View { width: SIZE, height: SIZE, scale: frame.scale, offset: frame.offset };
    let start = Instant::now();
    let prepared = nodebox_gpu::prepare(paths);
    let prepare = start.elapsed();
    let [r, g, b, a] = frame.background;
    // The target holds premultiplied color.
    let background = wgpu::Color { r: r * a, g: g * a, b: b * a, a };
    let start = Instant::now();
    renderer
        .render(&gpu.device, &gpu.queue, &prepared, view, &texture.create_view(&Default::default()), background)
        .unwrap();
    gpu.device.poll(wgpu::PollType::wait_indefinitely()).unwrap();
    let draw = start.elapsed();
    let mut pixels = read(gpu, &texture, SIZE);
    // Straight alpha, like Vello's output and a PNG file.
    for p in pixels.chunks_mut(4) {
        let a = u32::from(p[3]);
        if a > 0 && a < 255 {
            for c in &mut p[..3] {
                *c = ((u32::from(*c) * 255 + a / 2) / a).min(255) as u8;
            }
        }
    }
    (pixels, Timing { prepare, draw })
}

/// Mean difference per channel, and the share of pixels off by more than 1/8.
pub fn compare(a: &[u8], b: &[u8]) -> (f64, f64) {
    let mut total = 0u64;
    let mut off = 0usize;
    for (pa, pb) in a.chunks(4).zip(b.chunks(4)) {
        let d: Vec<u8> = pa.iter().zip(pb).map(|(x, y)| x.abs_diff(*y)).collect();
        total += d.iter().map(|&v| u64::from(v)).sum::<u64>();
        if d.iter().any(|&v| v > 32) {
            off += 1;
        }
    }
    (total as f64 / a.len() as f64, off as f64 / (a.len() / 4) as f64)
}

/// Writes both images when NODEBOX_GPU_DUMP names a directory.
pub fn dump(name: &str, images: &[(&str, &[u8])]) {
    if let Ok(dir) = std::env::var("NODEBOX_GPU_DUMP") {
        let slug: String = name.chars().map(|c| if c.is_ascii_alphanumeric() { c } else { '-' }).collect();
        for (kind, pixels) in images {
            std::fs::write(format!("{dir}/{slug}.{kind}.rgba"), pixels).unwrap();
        }
    }
}
