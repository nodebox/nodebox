//! Generate an SVG file showcasing NodeBox capabilities.

use nodebox_core::geometry::{Path, Color, Point, Canvas, font};
use nodebox_svg::{render_to_svg, render_canvas_to_svg};
use std::f64::consts::PI;

fn main() {
    // Create output directory
    let out_dir = std::path::Path::new("output");
    std::fs::create_dir_all(out_dir).unwrap();

    // 1. Simple shapes
    generate_shapes_demo(out_dir);

    // 2. Spiral pattern
    generate_spiral_demo(out_dir);

    // 3. Text as paths
    generate_text_demo(out_dir);

    // 4. Bezier sampling
    generate_bezier_demo(out_dir);

    println!("\nSVG files generated in ./output/");
    println!("Open them in a browser to view!");
}

fn generate_shapes_demo(out_dir: &std::path::Path) {
    println!("Generating shapes.svg...");

    let mut paths = Vec::new();

    // Red circle
    let mut circle = Path::ellipse(100.0, 100.0, 80.0, 80.0);
    circle.fill = Some(Color::rgb(0.9, 0.2, 0.2));
    paths.push(circle);

    // Green rectangle
    let mut rect = Path::rect(180.0, 60.0, 100.0, 80.0);
    rect.fill = Some(Color::rgb(0.2, 0.8, 0.3));
    paths.push(rect);

    // Blue star
    let mut star = create_star(350.0, 100.0, 5, 50.0, 25.0);
    star.fill = Some(Color::rgb(0.2, 0.4, 0.9));
    paths.push(star);

    // Purple hexagon
    let mut hex = create_polygon(480.0, 100.0, 6, 45.0);
    hex.fill = Some(Color::rgb(0.7, 0.3, 0.8));
    paths.push(hex);

    let svg = render_to_svg(&paths, 550.0, 200.0);
    std::fs::write(out_dir.join("shapes.svg"), svg).unwrap();
}

fn generate_spiral_demo(out_dir: &std::path::Path) {
    println!("Generating spiral.svg...");

    let mut paths = Vec::new();
    let center = Point::new(250.0, 250.0);

    // Create concentric circles with rotating colors
    for i in 0..12 {
        let radius = 30.0 + i as f64 * 18.0;
        let hue = i as f64 / 12.0;
        let color = hsb_to_rgb(hue, 0.7, 0.9);

        let mut circle = Path::ellipse(center.x, center.y, radius * 2.0, radius * 2.0);
        circle.fill = None;
        circle.stroke = Some(color);
        circle.stroke_width = 3.0;
        paths.push(circle);
    }

    // Add radiating lines
    for i in 0..24 {
        let angle = i as f64 * PI / 12.0;
        let x1 = center.x + 25.0 * angle.cos();
        let y1 = center.y + 25.0 * angle.sin();
        let x2 = center.x + 230.0 * angle.cos();
        let y2 = center.y + 230.0 * angle.sin();

        let mut line = Path::line(x1, y1, x2, y2);
        line.stroke = Some(Color::rgba(0.3, 0.3, 0.3, 0.3));
        line.stroke_width = 1.0;
        paths.push(line);
    }

    let svg = render_to_svg(&paths, 500.0, 500.0);
    std::fs::write(out_dir.join("spiral.svg"), svg).unwrap();
}

fn generate_text_demo(out_dir: &std::path::Path) {
    println!("Generating text.svg...");

    let mut paths = Vec::new();

    // Convert text to paths
    match font::text_to_path("NodeBox", "sans-serif", 72.0, Point::new(50.0, 120.0)) {
        Ok(mut path) => {
            path.fill = Some(Color::rgb(0.2, 0.2, 0.2));
            paths.push(path);
        }
        Err(e) => println!("   Warning: Could not render text: {}", e),
    }

    // Add "Rust" below
    match font::text_to_path("Rust", "sans-serif", 48.0, Point::new(50.0, 180.0)) {
        Ok(mut path) => {
            path.fill = Some(Color::rgb(0.8, 0.3, 0.1));
            paths.push(path);
        }
        Err(e) => println!("   Warning: Could not render text: {}", e),
    }

    // Decorative underline
    let mut line = Path::line(50.0, 195.0, 200.0, 195.0);
    line.stroke = Some(Color::rgb(0.8, 0.3, 0.1));
    line.stroke_width = 3.0;
    paths.push(line);

    let svg = render_to_svg(&paths, 400.0, 220.0);
    std::fs::write(out_dir.join("text.svg"), svg).unwrap();
}

fn generate_bezier_demo(out_dir: &std::path::Path) {
    println!("Generating bezier.svg...");

    let mut paths = Vec::new();

    // Create an ellipse
    let ellipse = Path::ellipse(150.0, 150.0, 200.0, 150.0);

    // Draw the original ellipse (faint)
    let mut outline = ellipse.clone();
    outline.fill = None;
    outline.stroke = Some(Color::rgba(0.5, 0.5, 0.5, 0.3));
    outline.stroke_width = 1.0;
    paths.push(outline);

    // Sample points along the ellipse
    let sample_points = ellipse.make_points(24);

    // Draw dots at each sample point
    for (i, p) in sample_points.iter().enumerate() {
        let hue = i as f64 / 24.0;
        let color = hsb_to_rgb(hue, 0.8, 0.9);

        let mut dot = Path::ellipse(p.x, p.y, 12.0, 12.0);
        dot.fill = Some(color);
        paths.push(dot);
    }

    // Connect points with lines
    for i in 0..sample_points.len() {
        let p1 = sample_points[i];
        let p2 = sample_points[(i + 1) % sample_points.len()];

        let mut line = Path::line(p1.x, p1.y, p2.x, p2.y);
        line.stroke = Some(Color::rgba(0.3, 0.3, 0.3, 0.5));
        line.stroke_width = 1.0;
        paths.push(line);
    }

    // Show point_at at various positions
    let mut label_y = 320.0;
    for t in [0.0, 0.25, 0.5, 0.75] {
        let p = ellipse.point_at(t);
        let mut marker = Path::ellipse(p.x, p.y, 20.0, 20.0);
        marker.fill = None;
        marker.stroke = Some(Color::rgb(1.0, 0.0, 0.0));
        marker.stroke_width = 2.0;
        paths.push(marker);
    }

    let svg = render_to_svg(&paths, 300.0, 300.0);
    std::fs::write(out_dir.join("bezier.svg"), svg).unwrap();
}

// Helper: create a star shape
fn create_star(cx: f64, cy: f64, points: usize, outer_r: f64, inner_r: f64) -> Path {
    let mut path = Path::new();
    let angle_step = PI / points as f64;

    for i in 0..(points * 2) {
        let angle = -PI / 2.0 + i as f64 * angle_step;
        let r = if i % 2 == 0 { outer_r } else { inner_r };
        let x = cx + r * angle.cos();
        let y = cy + r * angle.sin();

        if i == 0 {
            path.move_to(x, y);
        } else {
            path.line_to(x, y);
        }
    }
    path.close();
    path
}

// Helper: create a regular polygon
fn create_polygon(cx: f64, cy: f64, sides: usize, radius: f64) -> Path {
    let mut path = Path::new();
    let angle_step = 2.0 * PI / sides as f64;

    for i in 0..sides {
        let angle = -PI / 2.0 + i as f64 * angle_step;
        let x = cx + radius * angle.cos();
        let y = cy + radius * angle.sin();

        if i == 0 {
            path.move_to(x, y);
        } else {
            path.line_to(x, y);
        }
    }
    path.close();
    path
}

// Helper: convert HSB to RGB
fn hsb_to_rgb(h: f64, s: f64, b: f64) -> Color {
    let h = h * 6.0;
    let i = h.floor() as i32;
    let f = h - i as f64;
    let p = b * (1.0 - s);
    let q = b * (1.0 - s * f);
    let t = b * (1.0 - s * (1.0 - f));

    let (r, g, b) = match i % 6 {
        0 => (b, t, p),
        1 => (q, b, p),
        2 => (p, b, t),
        3 => (p, q, b),
        4 => (t, p, b),
        _ => (b, p, q),
    };

    Color::rgb(r, g, b)
}
