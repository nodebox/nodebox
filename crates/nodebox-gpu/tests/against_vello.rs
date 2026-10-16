//! Draws the same scenes with nodebox-gpu and with Vello and compares the pixels.
//!
//! Needs a GPU adapter; a software one (lavapipe, llvmpipe) is enough. Without
//! one the test says so and passes.

use nodebox_core::geometry::{Color, Contour, Path, Point, Transform};
use nodebox_core::ops::{copy, resample, star, CopyOrder};
use wgpu::TextureFormat;

mod common;
use common::{compare, draw_gpu, draw_vello, dump, gpu, Frame};

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
        let frame = Frame::centered(scale);
        let a = draw_vello(&gpu, &mut vello, &paths, frame);
        let (b, _) = draw_gpu(&gpu, &mut ours, &paths, frame);
        dump(name, &[("vello", &a), ("gpu", &b)]);
        let (mean, off) = compare(&a, &b);
        println!("{name:<28} mean difference {mean:.3}/255, {:.3}% of pixels off by more than 32/255", off * 100.0);
        if mean > 0.5 || off > 0.004 {
            failures.push(name);
        }
    }
    assert!(failures.is_empty(), "differ from Vello: {failures:?}");
}
