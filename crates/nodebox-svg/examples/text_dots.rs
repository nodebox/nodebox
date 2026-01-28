//! Demo: Text to paths, resampled with dots
//!
//! This demonstrates the full pipeline:
//! 1. Convert text to vector paths using font-kit
//! 2. Resample each contour to get evenly-spaced points
//! 3. Draw dots at each sample point

use nodebox_core::geometry::{Path, Color, Point, font};
use nodebox_svg::render_to_svg;

fn main() {
    let text = std::env::args().nth(1).unwrap_or_else(|| "NodeBox".to_string());
    let dot_count: usize = std::env::args()
        .nth(2)
        .and_then(|s| s.parse().ok())
        .unwrap_or(50);

    eprintln!("Converting '{}' to paths with {} dots per contour...", text, dot_count);

    let mut paths = Vec::new();

    // Convert text to path
    let text_path = match font::text_to_path(&text, "sans-serif", 120.0, Point::new(50.0, 150.0)) {
        Ok(p) => p,
        Err(e) => {
            eprintln!("Error: {}", e);
            std::process::exit(1);
        }
    };

    // Draw the original text path (faint)
    let mut ghost = text_path.clone();
    ghost.fill = Some(Color::rgba(0.9, 0.9, 0.95, 1.0));
    ghost.stroke = None;
    paths.push(ghost);

    // For each contour, resample and draw dots
    let mut total_points = 0;
    let mut total_length = 0.0;

    for contour in &text_path.contours {
        let length = contour.length();
        total_length += length;

        // Scale dot count based on contour length
        let points_for_contour = ((length / 10.0) as usize).max(3).min(dot_count * 2);
        let points = contour.make_points(points_for_contour);
        total_points += points.len();

        // Draw dots with rainbow colors based on position along contour
        for (i, p) in points.iter().enumerate() {
            let t = i as f64 / points.len() as f64;
            let color = rainbow_color(t);

            let mut dot = Path::ellipse(p.x, p.y, 5.0, 5.0);
            dot.fill = Some(color);
            paths.push(dot);
        }

        // Also draw connecting lines (faint)
        for i in 0..points.len() {
            let p1 = points[i];
            let p2 = points[(i + 1) % points.len()];

            // Skip the closing line for open contours
            if !contour.closed && i == points.len() - 1 {
                continue;
            }

            let mut line = Path::line(p1.x, p1.y, p2.x, p2.y);
            line.stroke = Some(Color::rgba(0.3, 0.3, 0.4, 0.3));
            line.stroke_width = 0.5;
            paths.push(line);
        }
    }

    eprintln!("Total contours: {}", text_path.contours.len());
    eprintln!("Total path length: {:.1}", total_length);
    eprintln!("Total dots: {}", total_points);

    // Calculate bounds for SVG size
    let bounds = text_path.bounds().unwrap();
    let width = bounds.x + bounds.width + 100.0;
    let height = bounds.y + bounds.height + 50.0;

    let svg = render_to_svg(&paths, width, height);
    println!("{}", svg);

    eprintln!("\nPipe to a file: cargo run --example text_dots -- \"Hello\" > hello_dots.svg");
}

fn rainbow_color(t: f64) -> Color {
    // HSB to RGB with full saturation and brightness
    let h = t * 6.0;
    let i = h.floor() as i32;
    let f = h - i as f64;

    let (r, g, b) = match i % 6 {
        0 => (1.0, f, 0.0),
        1 => (1.0 - f, 1.0, 0.0),
        2 => (0.0, 1.0, f),
        3 => (0.0, 1.0 - f, 1.0),
        4 => (f, 0.0, 1.0),
        _ => (1.0, 0.0, 1.0 - f),
    };

    Color::rgb(r, g, b)
}
