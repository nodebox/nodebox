//! Draws the same scenes with nodebox-gpu and with Vello and compares the pixels.
//!
//! Needs a GPU adapter; a software one (lavapipe, llvmpipe) is enough. Without
//! one the test says so and passes.

use nodebox_core::geometry::{Color, Contour, Path, Point, Transform};
use nodebox_core::ops::{copy, resample, star, CopyOrder};
use wgpu::TextureFormat;

const SIZE: u32 = 512;

struct Gpu {
    device: wgpu::Device,
    queue: wgpu::Queue,
}

fn gpu() -> Option<Gpu> {
    let instance = wgpu::Instance::default();
    let adapter = pollster::block_on(instance.request_adapter(&Default::default())).ok()?;
    let (device, queue) = pollster::block_on(adapter.request_device(&Default::default())).ok()?;
    Some(Gpu { device, queue })
}

fn target(gpu: &Gpu, usage: wgpu::TextureUsages) -> wgpu::Texture {
    gpu.device.create_texture(&wgpu::TextureDescriptor {
        label: None,
        size: wgpu::Extent3d { width: SIZE, height: SIZE, depth_or_array_layers: 1 },
        mip_level_count: 1,
        sample_count: 1,
        dimension: wgpu::TextureDimension::D2,
        format: TextureFormat::Rgba8Unorm,
        usage: usage | wgpu::TextureUsages::COPY_SRC,
        view_formats: &[],
    })
}

fn read(gpu: &Gpu, texture: &wgpu::Texture) -> Vec<u8> {
    let buffer = gpu.device.create_buffer(&wgpu::BufferDescriptor {
        label: None,
        size: u64::from(SIZE * SIZE * 4),
        usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::MAP_READ,
        mapped_at_creation: false,
    });
    let mut encoder = gpu.device.create_command_encoder(&Default::default());
    encoder.copy_texture_to_buffer(
        texture.as_image_copy(),
        wgpu::TexelCopyBufferInfo {
            buffer: &buffer,
            layout: wgpu::TexelCopyBufferLayout { offset: 0, bytes_per_row: Some(SIZE * 4), rows_per_image: None },
        },
        wgpu::Extent3d { width: SIZE, height: SIZE, depth_or_array_layers: 1 },
    );
    gpu.queue.submit([encoder.finish()]);
    buffer.slice(..).map_async(wgpu::MapMode::Read, |_| {});
    gpu.device.poll(wgpu::PollType::wait_indefinitely()).unwrap();
    let pixels = buffer.slice(..).get_mapped_range().to_vec();
    pixels
}

fn draw_vello(gpu: &Gpu, renderer: &mut vello::Renderer, paths: &[Path], scale: f64) -> Vec<u8> {
    let half = f64::from(SIZE) / 2.0;
    let scene = nodebox_vello::build_scene(paths, vello::kurbo::Affine::translate((half, half)) * vello::kurbo::Affine::scale(scale));
    let texture = target(gpu, wgpu::TextureUsages::STORAGE_BINDING);
    let params = vello::RenderParams {
        base_color: vello::peniko::Color::WHITE,
        width: SIZE,
        height: SIZE,
        antialiasing_method: vello::AaConfig::Area,
    };
    renderer
        .render_to_texture(&gpu.device, &gpu.queue, &scene, &texture.create_view(&Default::default()), &params)
        .unwrap();
    read(gpu, &texture)
}

fn draw_gpu(gpu: &Gpu, renderer: &mut nodebox_gpu::Renderer, paths: &[Path], scale: f64) -> Vec<u8> {
    let half = f64::from(SIZE) / 2.0;
    let texture = target(gpu, wgpu::TextureUsages::RENDER_ATTACHMENT);
    let view = nodebox_gpu::View { width: SIZE, height: SIZE, scale, offset: (half, half) };
    let prepared = nodebox_gpu::prepare(paths);
    renderer.render(&gpu.device, &gpu.queue, &prepared, view, &texture.create_view(&Default::default()), wgpu::Color::WHITE);
    read(gpu, &texture)
}

/// Mean difference per channel, and the share of pixels off by more than 1/8.
fn compare(a: &[u8], b: &[u8]) -> (f64, f64) {
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

fn styled(path: Path, fill: Option<Color>, stroke: Option<Color>, width: f64) -> Path {
    path.with_fill(fill).with_stroke(stroke).with_stroke_width(width)
}

fn scenes() -> Vec<(&'static str, Vec<Path>, f64)> {
    let blue = Some(Color::rgba(0.2, 0.4, 0.8, 0.7));
    let black = Some(Color::BLACK);
    let shape = styled(resample(&star(Point::ZERO, 5, 40.0, 18.0), 30), blue, black, 2.0);

    // A square with a square hole: the inner contour runs the other way.
    let mut hole = Path::rect(-100.0, -100.0, 200.0, 200.0);
    hole.add_contour(Contour::from_points(
        Path::rect(-50.0, -50.0, 100.0, 100.0).contours()[0].points.iter().rev().cloned().collect(),
        true,
    ));
    // Two overlapping circles in one path: nonzero fills the overlap once.
    let mut overlap = Path::ellipse(-30.0, 0.0, 120.0, 120.0);
    overlap.add_contour(Path::ellipse(30.0, 0.0, 120.0, 120.0).contours()[0].clone());

    let mut spiral = Path::new_stroke();
    for i in 0..2000 {
        let a = i as f64 * 0.05;
        let r = 2.0 + a * 6.0;
        let (x, y) = (a.cos() * r, a.sin() * r);
        if i == 0 { spiral.move_to(x, y) } else { spiral.line_to(x, y) }
    }
    let mut unstyled = Path::line(-200.0, 200.0, 200.0, -200.0);
    unstyled.stroke = None;

    vec![
        ("copies, rotated and scaled", copy(&shape, 24, CopyOrder::TSR, Point::new(12.0, 6.0), 15.0, Point::new(97.0, 97.0)), 1.0),
        ("copies, scaled unevenly", copy(&shape, 12, CopyOrder::TSR, Point::new(15.0, 8.0), 0.0, Point::new(110.0, 90.0)), 1.0),
        ("mirrored and skewed", vec![
            shape.transform(&Transform::scale_xy(-2.0, 2.0)),
            shape.transform(&Transform::skew(20.0, 0.0).then(&Transform::translate(-150.0, -150.0))),
        ], 1.0),
        ("a hole", vec![styled(hole, blue, None, 0.0)], 1.0),
        ("overlapping contours", vec![styled(overlap, blue, black, 1.0)], 1.0),
        ("a spiral of 2,000 lines", vec![spiral], 1.0),
        ("thin strokes", (0..20).map(|i| styled(Path::line(-200.0, -190.0 + i as f64 * 20.0, 200.0, -170.0 + i as f64 * 20.0), None, black, 0.1 + i as f64 * 0.1)).collect(), 1.0),
        ("an unstyled line", vec![unstyled], 1.0),
        ("zoomed in 8 times", vec![shape.clone()], 8.0),
    ]
}

#[test]
fn draws_like_vello() {
    let Some(gpu) = gpu() else {
        eprintln!("No GPU adapter: skipping the comparison with Vello.");
        return;
    };
    let mut vello = vello::Renderer::new(&gpu.device, vello::RendererOptions::default()).unwrap();
    let mut ours = nodebox_gpu::Renderer::new(&gpu.device, TextureFormat::Rgba8Unorm);
    let mut failures = Vec::new();
    for (name, paths, scale) in scenes() {
        let a = draw_vello(&gpu, &mut vello, &paths, scale);
        let b = draw_gpu(&gpu, &mut ours, &paths, scale);
        if let Ok(dir) = std::env::var("NODEBOX_GPU_DUMP") {
            let slug = name.replace([' ', ','], "-");
            std::fs::write(format!("{dir}/{slug}.vello.rgba"), &a).unwrap();
            std::fs::write(format!("{dir}/{slug}.gpu.rgba"), &b).unwrap();
        }
        let (mean, off) = compare(&a, &b);
        println!("{name:<28} mean difference {mean:.3}/255, {:.3}% of pixels off by more than 32/255", off * 100.0);
        if mean > 0.5 || off > 0.004 {
            failures.push(name);
        }
    }
    assert!(failures.is_empty(), "differ from Vello: {failures:?}");
}
