//! NodeBox CLI - Command-line interface for NodeBox Rust
//!
//! A simple tool to experiment with NodeBox geometry operations.

use nodebox_core::geometry::{Path, Color, Point, Geometry, font};
use nodebox_ops::*;
use nodebox_svg::render_to_svg;
use std::io::{self, Write};

fn main() {
    let args: Vec<String> = std::env::args().collect();

    if args.len() > 1 {
        // Command mode
        match args[1].as_str() {
            "demo" => run_demo(&args[2..]),
            "text" => run_text_to_svg(&args[2..]),
            "help" | "--help" | "-h" => print_help(),
            "version" | "--version" | "-V" => print_version(),
            _ => {
                eprintln!("Unknown command: {}", args[1]);
                eprintln!("Run 'nodebox help' for usage.");
                std::process::exit(1);
            }
        }
    } else {
        // Interactive mode
        run_interactive();
    }
}

fn print_help() {
    println!(r#"
NodeBox CLI - Generative design toolkit

USAGE:
    nodebox [COMMAND] [OPTIONS]

COMMANDS:
    demo <name>     Generate a demo SVG (shapes, spiral, text, bezier)
    text <string>   Convert text to SVG path
    help            Show this help message
    version         Show version info

EXAMPLES:
    nodebox demo shapes > shapes.svg
    nodebox text "Hello World" > hello.svg
    nodebox                          # Interactive mode

Run without arguments for interactive mode.
"#);
}

fn print_version() {
    println!("NodeBox CLI v{}", env!("CARGO_PKG_VERSION"));
    println!("Built with Rust");
}

fn run_demo(args: &[String]) {
    let demo_name = args.get(0).map(|s| s.as_str()).unwrap_or("shapes");

    let svg = match demo_name {
        "shapes" => demo_shapes(),
        "spiral" => demo_spiral(),
        "text" => demo_text(),
        "bezier" => demo_bezier(),
        "all" => {
            // Output all demos to files
            std::fs::create_dir_all("output").ok();
            std::fs::write("output/shapes.svg", demo_shapes()).ok();
            std::fs::write("output/spiral.svg", demo_spiral()).ok();
            std::fs::write("output/text.svg", demo_text()).ok();
            std::fs::write("output/bezier.svg", demo_bezier()).ok();
            eprintln!("Generated: output/shapes.svg, spiral.svg, text.svg, bezier.svg");
            return;
        }
        _ => {
            eprintln!("Unknown demo: {}", demo_name);
            eprintln!("Available: shapes, spiral, text, bezier, all");
            std::process::exit(1);
        }
    };

    println!("{}", svg);
}

fn run_text_to_svg(args: &[String]) {
    let text = args.join(" ");
    if text.is_empty() {
        eprintln!("Usage: nodebox text <your text here>");
        std::process::exit(1);
    }

    let mut paths = Vec::new();

    match font::text_to_path(&text, "sans-serif", 72.0, Point::new(20.0, 100.0)) {
        Ok(mut path) => {
            path.fill = Some(Color::BLACK);

            // Calculate bounds for SVG size
            let bounds = path.bounds().unwrap_or(nodebox_core::geometry::Rect::new(0.0, 0.0, 400.0, 150.0));
            paths.push(path);

            let svg = render_to_svg(&paths, bounds.x + bounds.width + 40.0, bounds.y + bounds.height + 40.0);
            println!("{}", svg);
        }
        Err(e) => {
            eprintln!("Error rendering text: {}", e);
            std::process::exit(1);
        }
    }
}

fn run_interactive() {
    println!("NodeBox Interactive Mode");
    println!("Type 'help' for commands, 'quit' to exit.\n");

    let mut paths: Vec<Path> = Vec::new();

    loop {
        print!("nodebox> ");
        io::stdout().flush().unwrap();

        let mut input = String::new();
        if io::stdin().read_line(&mut input).is_err() {
            break;
        }

        let input = input.trim();
        if input.is_empty() {
            continue;
        }

        let parts: Vec<&str> = input.split_whitespace().collect();
        let cmd = parts[0];
        let args = &parts[1..];

        match cmd {
            "quit" | "exit" | "q" => break,

            "help" | "?" => print_interactive_help(),

            "clear" => {
                paths.clear();
                println!("Canvas cleared.");
            }

            "list" => {
                println!("{} paths on canvas:", paths.len());
                for (i, p) in paths.iter().enumerate() {
                    let bounds = p.bounds();
                    let fill = p.fill.map(|c| format!("#{:02x}{:02x}{:02x}",
                        (c.r * 255.0) as u8, (c.g * 255.0) as u8, (c.b * 255.0) as u8))
                        .unwrap_or_else(|| "none".to_string());
                    match bounds {
                        Some(b) => println!("  [{}] bounds: ({:.0},{:.0}) {}x{}, fill: {}",
                            i, b.x, b.y, b.width, b.height, fill),
                        None => println!("  [{}] empty", i),
                    }
                }
            }

            "ellipse" | "circle" => {
                let (x, y, w, h) = parse_rect_args(args, 100.0, 100.0, 80.0, 80.0);
                let mut p = Path::ellipse(x, y, w, h);
                p.fill = Some(Color::BLACK);
                paths.push(p);
                println!("Added ellipse at ({}, {}), size {}x{}", x, y, w, h);
            }

            "rect" | "rectangle" => {
                let (x, y, w, h) = parse_rect_args(args, 50.0, 50.0, 100.0, 80.0);
                let p = nodebox_ops::rect(Point::new(x, y), w, h, Point::ZERO);
                paths.push(p);
                println!("Added rect at ({}, {}), size {}x{}", x, y, w, h);
            }

            "star" => {
                let (x, y, _, _) = parse_rect_args(args, 100.0, 100.0, 0.0, 0.0);
                let points: u32 = args.get(4).and_then(|s| s.parse().ok()).unwrap_or(5);
                let outer: f64 = args.get(5).and_then(|s| s.parse().ok()).unwrap_or(50.0);
                let inner: f64 = args.get(6).and_then(|s| s.parse().ok()).unwrap_or(25.0);
                let p = nodebox_ops::star(Point::new(x, y), points, outer, inner);
                paths.push(p);
                println!("Added {}-point star at ({}, {})", points, x, y);
            }

            "polygon" => {
                let (x, y, _, _) = parse_rect_args(args, 100.0, 100.0, 0.0, 0.0);
                let sides: u32 = args.get(2).and_then(|s| s.parse().ok()).unwrap_or(6);
                let radius: f64 = args.get(3).and_then(|s| s.parse().ok()).unwrap_or(50.0);
                let p = nodebox_ops::polygon(Point::new(x, y), radius, sides, true);
                paths.push(p);
                println!("Added {}-sided polygon at ({}, {})", sides, x, y);
            }

            "text" => {
                let text = args.join(" ");
                if text.is_empty() {
                    println!("Usage: text <your text>");
                    continue;
                }
                match font::text_to_path(&text, "sans-serif", 48.0, Point::new(20.0, 100.0)) {
                    Ok(p) => {
                        paths.push(p);
                        println!("Added text: \"{}\"", text);
                    }
                    Err(e) => println!("Error: {}", e),
                }
            }

            "color" => {
                if paths.is_empty() {
                    println!("No paths to color. Add a shape first.");
                    continue;
                }
                let r: f64 = args.get(0).and_then(|s| s.parse().ok()).unwrap_or(0.0);
                let g: f64 = args.get(1).and_then(|s| s.parse().ok()).unwrap_or(0.0);
                let b: f64 = args.get(2).and_then(|s| s.parse().ok()).unwrap_or(0.0);
                if let Some(p) = paths.last_mut() {
                    p.fill = Some(Color::rgb(r, g, b));
                    println!("Set color to ({}, {}, {})", r, g, b);
                }
            }

            "save" => {
                let filename = args.get(0).unwrap_or(&"output.svg");
                let svg = render_to_svg(&paths, 500.0, 500.0);
                match std::fs::write(filename, &svg) {
                    Ok(_) => println!("Saved to {}", filename),
                    Err(e) => println!("Error saving: {}", e),
                }
            }

            "show" => {
                let svg = render_to_svg(&paths, 500.0, 500.0);
                println!("{}", svg);
            }

            _ => println!("Unknown command: {}. Type 'help' for commands.", cmd),
        }
    }

    println!("Goodbye!");
}

fn print_interactive_help() {
    println!(r#"
Interactive Commands:
  ellipse [x y w h]     Add ellipse (default: 100 100 80 80)
  rect [x y w h]        Add rectangle
  star [x y] [pts] [outer] [inner]  Add star
  polygon [x y] [sides] [radius]    Add polygon
  text <string>         Add text as path

  color r g b           Set color of last shape (0.0-1.0)
  clear                 Remove all shapes
  list                  Show all shapes

  save [filename]       Save to SVG file
  show                  Print SVG to stdout

  help                  Show this help
  quit                  Exit
"#);
}

fn parse_rect_args(args: &[&str], dx: f64, dy: f64, dw: f64, dh: f64) -> (f64, f64, f64, f64) {
    let x = args.get(0).and_then(|s| s.parse().ok()).unwrap_or(dx);
    let y = args.get(1).and_then(|s| s.parse().ok()).unwrap_or(dy);
    let w = args.get(2).and_then(|s| s.parse().ok()).unwrap_or(dw);
    let h = args.get(3).and_then(|s| s.parse().ok()).unwrap_or(dh);
    (x, y, w, h)
}

// Demo generators
fn demo_shapes() -> String {
    let mut paths = Vec::new();

    let mut circle = Path::ellipse(100.0, 100.0, 80.0, 80.0);
    circle.fill = Some(Color::rgb(0.9, 0.2, 0.2));
    paths.push(circle);

    let mut rect = nodebox_ops::rect(Point::new(180.0, 60.0), 100.0, 80.0, Point::ZERO);
    rect.fill = Some(Color::rgb(0.2, 0.8, 0.3));
    paths.push(rect);

    let mut star = nodebox_ops::star(Point::new(350.0, 100.0), 5, 50.0, 25.0);
    star.fill = Some(Color::rgb(0.2, 0.4, 0.9));
    paths.push(star);

    let mut hex = nodebox_ops::polygon(Point::new(480.0, 100.0), 45.0, 6, true);
    hex.fill = Some(Color::rgb(0.7, 0.3, 0.8));
    paths.push(hex);

    render_to_svg(&paths, 550.0, 200.0)
}

fn demo_spiral() -> String {
    use std::f64::consts::PI;

    let mut paths = Vec::new();
    let center = Point::new(250.0, 250.0);

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

    render_to_svg(&paths, 500.0, 500.0)
}

fn demo_text() -> String {
    let mut paths = Vec::new();

    if let Ok(mut path) = font::text_to_path("NodeBox", "sans-serif", 72.0, Point::new(50.0, 120.0)) {
        path.fill = Some(Color::rgb(0.2, 0.2, 0.2));
        paths.push(path);
    }

    if let Ok(mut path) = font::text_to_path("Rust", "sans-serif", 48.0, Point::new(50.0, 180.0)) {
        path.fill = Some(Color::rgb(0.8, 0.3, 0.1));
        paths.push(path);
    }

    let mut line = Path::line(50.0, 195.0, 200.0, 195.0);
    line.stroke = Some(Color::rgb(0.8, 0.3, 0.1));
    line.stroke_width = 3.0;
    paths.push(line);

    render_to_svg(&paths, 400.0, 220.0)
}

fn demo_bezier() -> String {
    let mut paths = Vec::new();

    let ellipse = Path::ellipse(150.0, 150.0, 200.0, 150.0);

    let mut outline = ellipse.clone();
    outline.fill = None;
    outline.stroke = Some(Color::rgba(0.5, 0.5, 0.5, 0.3));
    outline.stroke_width = 1.0;
    paths.push(outline);

    let sample_points = ellipse.make_points(24);

    for (i, p) in sample_points.iter().enumerate() {
        let hue = i as f64 / 24.0;
        let color = hsb_to_rgb(hue, 0.8, 0.9);

        let mut dot = Path::ellipse(p.x, p.y, 12.0, 12.0);
        dot.fill = Some(color);
        paths.push(dot);
    }

    for i in 0..sample_points.len() {
        let p1 = sample_points[i];
        let p2 = sample_points[(i + 1) % sample_points.len()];

        let mut line = Path::line(p1.x, p1.y, p2.x, p2.y);
        line.stroke = Some(Color::rgba(0.3, 0.3, 0.3, 0.5));
        line.stroke_width = 1.0;
        paths.push(line);
    }

    render_to_svg(&paths, 300.0, 300.0)
}

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
