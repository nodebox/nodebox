//! Generate golden SVG files for all NodeBox node operations.
//! These files can be compared with Java output to verify consistency.

use nodebox_core::geometry::{Color, Path, Point};
use nodebox_ops::*;
use nodebox_svg::{render_to_svg_with_options, SvgOptions};
use std::fs;
use std::path::PathBuf;

const WIDTH: f64 = 200.0;
const HEIGHT: f64 = 200.0;

fn main() {
    let out_dir = PathBuf::from("build/golden-svg");
    fs::create_dir_all(&out_dir).unwrap();

    println!("Generating golden SVG files...\n");

    // Generators
    generate_ellipse(&out_dir);
    generate_rect(&out_dir);
    generate_rect_rounded(&out_dir);
    generate_line(&out_dir);
    generate_line_points(&out_dir);
    generate_line_angle(&out_dir);
    generate_arc_pie(&out_dir);
    generate_arc_chord(&out_dir);
    generate_arc_open(&out_dir);
    generate_arc_270(&out_dir);
    generate_grid(&out_dir);
    generate_connect(&out_dir);
    generate_connect_open(&out_dir);
    generate_freehand(&out_dir);
    generate_polygon(&out_dir);
    generate_star(&out_dir);
    generate_quad_curve(&out_dir);

    // Filters
    generate_align_center(&out_dir);
    generate_align_left_top(&out_dir);
    generate_colorize(&out_dir);
    generate_copy(&out_dir);
    generate_copy_rotate(&out_dir);
    generate_fit(&out_dir);
    generate_fit_no_proportions(&out_dir);
    generate_group(&out_dir);
    generate_skew(&out_dir);
    generate_snap(&out_dir);
    generate_point_on_path(&out_dir);
    generate_translate(&out_dir);
    generate_scale(&out_dir);
    generate_rotate(&out_dir);
    generate_reflect(&out_dir);
    generate_wiggle(&out_dir);
    generate_stack(&out_dir);

    // Complex examples
    generate_spiral(&out_dir);
    generate_radial_pattern(&out_dir);
    generate_grid_of_shapes(&out_dir);
    generate_all_arc_types(&out_dir);

    println!("\nGenerated {} golden files in {:?}", count_svgs(&out_dir), out_dir);
}

fn count_svgs(dir: &PathBuf) -> usize {
    fs::read_dir(dir)
        .map(|entries| entries.filter(|e| e.as_ref().unwrap().path().extension().map_or(false, |e| e == "svg")).count())
        .unwrap_or(0)
}

fn write_svg(name: &str, paths: &[Path], out_dir: &PathBuf) {
    let options = SvgOptions::new(WIDTH, HEIGHT)
        .with_background(None)
        .with_xml_declaration(true);
    let svg = render_to_svg_with_options(paths, &options);
    let path = out_dir.join(format!("{}.svg", name));
    fs::write(&path, &svg).unwrap();
    println!("  {} - {} paths", name, paths.len());
}

// ============================================================================
// Generators
// ============================================================================

fn generate_ellipse(out_dir: &PathBuf) {
    let mut e = ellipse(Point::new(100.0, 100.0), 80.0, 60.0);
    e.fill = Some(Color::BLACK);
    write_svg("ellipse", &[e], out_dir);
}

fn generate_rect(out_dir: &PathBuf) {
    let mut r = rect(Point::new(100.0, 100.0), 80.0, 60.0, Point::ZERO);
    r.fill = Some(Color::BLACK);
    write_svg("rect", &[r], out_dir);
}

fn generate_rect_rounded(out_dir: &PathBuf) {
    let mut r = rect(Point::new(100.0, 100.0), 80.0, 60.0, Point::new(10.0, 10.0));
    r.fill = Some(Color::BLACK);
    write_svg("rect_rounded", &[r], out_dir);
}

fn generate_line(out_dir: &PathBuf) {
    let mut l = line(Point::new(20.0, 20.0), Point::new(180.0, 180.0), 2);
    l.fill = None;
    l.stroke = Some(Color::BLACK);
    l.stroke_width = 2.0;
    write_svg("line", &[l], out_dir);
}

fn generate_line_points(out_dir: &PathBuf) {
    let mut l = line(Point::new(20.0, 20.0), Point::new(180.0, 180.0), 5);
    l.fill = None;
    l.stroke = Some(Color::BLACK);
    l.stroke_width = 2.0;
    write_svg("line_points", &[l], out_dir);
}

fn generate_line_angle(out_dir: &PathBuf) {
    let mut l = line_angle(Point::new(100.0, 100.0), 45.0, 80.0, 2);
    l.fill = None;
    l.stroke = Some(Color::BLACK);
    l.stroke_width = 2.0;
    write_svg("line_angle", &[l], out_dir);
}

fn generate_arc_pie(out_dir: &PathBuf) {
    let mut a = arc(Point::new(100.0, 100.0), 80.0, 80.0, 0.0, 90.0, "pie");
    a.fill = Some(Color::BLACK);
    write_svg("arc_pie", &[a], out_dir);
}

fn generate_arc_chord(out_dir: &PathBuf) {
    let mut a = arc(Point::new(100.0, 100.0), 80.0, 80.0, 0.0, 90.0, "chord");
    a.fill = Some(Color::BLACK);
    write_svg("arc_chord", &[a], out_dir);
}

fn generate_arc_open(out_dir: &PathBuf) {
    let mut a = arc(Point::new(100.0, 100.0), 80.0, 80.0, 0.0, 90.0, "open");
    a.fill = None;
    a.stroke = Some(Color::BLACK);
    a.stroke_width = 3.0;
    write_svg("arc_open", &[a], out_dir);
}

fn generate_arc_270(out_dir: &PathBuf) {
    let mut a = arc(Point::new(100.0, 100.0), 80.0, 80.0, 0.0, 270.0, "pie");
    a.fill = Some(Color::BLACK);
    write_svg("arc_270", &[a], out_dir);
}

fn generate_grid(out_dir: &PathBuf) {
    let points = grid(4, 3, 160.0, 120.0, Point::new(100.0, 100.0));
    let mut paths = Vec::new();
    for p in points {
        let mut circle = Path::ellipse(p.x, p.y, 5.0, 5.0);
        circle.fill = Some(Color::BLACK);
        paths.push(circle);
    }
    write_svg("grid", &paths, out_dir);
}

fn generate_connect(out_dir: &PathBuf) {
    let points = vec![
        Point::new(20.0, 20.0),
        Point::new(100.0, 180.0),
        Point::new(180.0, 20.0),
    ];
    let mut p = connect(&points, true);
    p.fill = Some(Color::BLACK);
    write_svg("connect", &[p], out_dir);
}

fn generate_connect_open(out_dir: &PathBuf) {
    let points = vec![
        Point::new(20.0, 20.0),
        Point::new(100.0, 180.0),
        Point::new(180.0, 20.0),
    ];
    let mut p = connect(&points, false);
    p.fill = None;
    p.stroke = Some(Color::BLACK);
    p.stroke_width = 2.0;
    write_svg("connect_open", &[p], out_dir);
}

fn generate_freehand(out_dir: &PathBuf) {
    let mut p = freehand("M 0,100 50,50 100,100 M 100,100 150,150 200,100");
    p.fill = None;
    p.stroke = Some(Color::BLACK);
    p.stroke_width = 2.0;
    write_svg("freehand", &[p], out_dir);
}

fn generate_polygon(out_dir: &PathBuf) {
    let mut p = polygon(Point::new(100.0, 100.0), 50.0, 6, true);
    p.fill = Some(Color::BLACK);
    write_svg("polygon", &[p], out_dir);
}

fn generate_star(out_dir: &PathBuf) {
    let mut s = star(Point::new(100.0, 100.0), 5, 50.0, 25.0);
    s.fill = Some(Color::BLACK);
    write_svg("star", &[s], out_dir);
}

fn generate_quad_curve(out_dir: &PathBuf) {
    let mut c = quad_curve(Point::new(20.0, 100.0), Point::new(180.0, 100.0), 50.0, 50.0);
    c.fill = None;
    c.stroke = Some(Color::BLACK);
    c.stroke_width = 2.0;
    write_svg("quad_curve", &[c], out_dir);
}

// ============================================================================
// Filters
// ============================================================================

fn generate_align_center(out_dir: &PathBuf) {
    let r = rect(Point::new(150.0, 150.0), 40.0, 40.0, Point::ZERO);
    let mut aligned = align(&r, Point::ZERO, HAlign::Center, VAlign::Middle);
    // Move to center of canvas for display
    aligned = translate(&aligned, Point::new(100.0, 100.0));
    aligned.fill = Some(Color::BLACK);
    write_svg("align_center", &[aligned], out_dir);
}

fn generate_align_left_top(out_dir: &PathBuf) {
    let r = rect(Point::new(100.0, 100.0), 40.0, 40.0, Point::ZERO);
    let mut aligned = align(&r, Point::new(50.0, 50.0), HAlign::Left, VAlign::Top);
    aligned.fill = Some(Color::BLACK);
    write_svg("align_left_top", &[aligned], out_dir);
}

fn generate_colorize(out_dir: &PathBuf) {
    let e = ellipse(Point::new(100.0, 100.0), 80.0, 80.0);
    let colored = colorize(&e, Color::rgb(1.0, 0.0, 0.0), Color::rgb(0.0, 0.0, 1.0), 3.0);
    write_svg("colorize", &[colored], out_dir);
}

fn generate_copy(out_dir: &PathBuf) {
    let mut r = rect(Point::new(20.0, 100.0), 15.0, 15.0, Point::ZERO);
    r.fill = Some(Color::rgb(0.3, 0.5, 0.8));
    let copies = copy(&r, 8, CopyOrder::TSR, Point::new(22.0, 0.0), 0.0, Point::new(100.0, 100.0));
    write_svg("copy", &copies, out_dir);
}

fn generate_copy_rotate(out_dir: &PathBuf) {
    let mut r = rect(Point::new(100.0, 50.0), 10.0, 30.0, Point::ZERO);
    r.fill = Some(Color::rgb(0.8, 0.3, 0.3));
    let copies = copy(&r, 12, CopyOrder::TRS, Point::ZERO, 30.0, Point::new(100.0, 100.0));
    write_svg("copy_rotate", &copies, out_dir);
}

fn generate_fit(out_dir: &PathBuf) {
    let r = rect(Point::new(100.0, 100.0), 200.0, 100.0, Point::ZERO);
    let mut fitted = fit(&r, Point::new(100.0, 100.0), 80.0, 80.0, true);
    fitted.fill = Some(Color::BLACK);

    // Draw target bounds for reference
    let mut target = rect(Point::new(100.0, 100.0), 80.0, 80.0, Point::ZERO);
    target.fill = None;
    target.stroke = Some(Color::rgb(0.8, 0.8, 0.8));
    target.stroke_width = 1.0;

    write_svg("fit", &[target, fitted], out_dir);
}

fn generate_fit_no_proportions(out_dir: &PathBuf) {
    let r = rect(Point::new(100.0, 100.0), 200.0, 100.0, Point::ZERO);
    let mut fitted = fit(&r, Point::new(100.0, 100.0), 80.0, 80.0, false);
    fitted.fill = Some(Color::BLACK);
    write_svg("fit_no_proportions", &[fitted], out_dir);
}

fn generate_group(out_dir: &PathBuf) {
    let mut r = rect(Point::new(60.0, 100.0), 40.0, 40.0, Point::ZERO);
    let mut e = ellipse(Point::new(140.0, 100.0), 40.0, 40.0);
    r.fill = Some(Color::BLACK);
    e.fill = Some(Color::BLACK);
    write_svg("group", &[r, e], out_dir);
}

fn generate_skew(out_dir: &PathBuf) {
    let r = rect(Point::new(100.0, 100.0), 60.0, 60.0, Point::ZERO);
    let mut skewed = skew(&r, Point::new(20.0, 0.0), Point::new(100.0, 100.0));
    skewed.fill = Some(Color::BLACK);
    write_svg("skew", &[skewed], out_dir);
}

fn generate_snap(out_dir: &PathBuf) {
    let r = rect(Point::new(73.0, 67.0), 40.0, 40.0, Point::ZERO);
    let mut snapped = snap(&r, 20.0, 100.0, Point::ZERO);
    snapped.fill = Some(Color::BLACK);

    // Draw grid for reference
    let mut paths = Vec::new();
    for x in (0..=200).step_by(20) {
        let mut l = Path::line(x as f64, 0.0, x as f64, 200.0);
        l.fill = None;
        l.stroke = Some(Color::rgb(0.9, 0.9, 0.9));
        l.stroke_width = 0.5;
        paths.push(l);
    }
    for y in (0..=200).step_by(20) {
        let mut l = Path::line(0.0, y as f64, 200.0, y as f64);
        l.fill = None;
        l.stroke = Some(Color::rgb(0.9, 0.9, 0.9));
        l.stroke_width = 0.5;
        paths.push(l);
    }
    paths.push(snapped);
    write_svg("snap", &paths, out_dir);
}

fn generate_point_on_path(out_dir: &PathBuf) {
    let mut e = ellipse(Point::new(100.0, 100.0), 80.0, 80.0);
    e.fill = None;
    e.stroke = Some(Color::rgb(0.5, 0.5, 0.5));
    e.stroke_width = 1.0;

    let mut paths = vec![e.clone()];
    for i in 0..=10 {
        let t = i as f64 / 10.0;
        let p = point_on_path(&e, t * 100.0);
        let mut marker = Path::ellipse(p.x, p.y, 6.0, 6.0);
        marker.fill = Some(Color::rgb(1.0, 0.0, 0.0));
        paths.push(marker);
    }
    write_svg("point_on_path", &paths, out_dir);
}

fn generate_translate(out_dir: &PathBuf) {
    let r = Path::rect(0.0, 0.0, 50.0, 50.0);
    let mut moved = translate(&r, Point::new(100.0, 100.0));
    moved.fill = Some(Color::BLACK);
    write_svg("translate", &[moved], out_dir);
}

fn generate_scale(out_dir: &PathBuf) {
    let r = Path::rect(75.0, 75.0, 50.0, 50.0);
    let mut scaled = scale(&r, Point::new(200.0, 200.0), Point::new(100.0, 100.0));
    scaled.fill = Some(Color::BLACK);
    write_svg("scale", &[scaled], out_dir);
}

fn generate_rotate(out_dir: &PathBuf) {
    let mut r = Path::rect(100.0, 50.0, 60.0, 30.0);
    r.fill = Some(Color::BLACK);

    let mut rotated = rotate(&r, 45.0, Point::new(100.0, 100.0));
    rotated.fill = Some(Color::rgb(0.5, 0.5, 0.5));

    write_svg("rotate", &[r, rotated], out_dir);
}

fn generate_reflect(out_dir: &PathBuf) {
    let r = Path::rect(120.0, 80.0, 40.0, 40.0);
    let reflected = reflect(&r, Point::new(100.0, 100.0), 90.0, false);
    let mut paths: Vec<Path> = reflected.paths.into_iter().map(|mut p| {
        p.fill = Some(Color::BLACK);
        p
    }).collect();
    // Also show original
    let mut orig = r.clone();
    orig.fill = Some(Color::rgb(0.5, 0.5, 0.5));
    paths.push(orig);
    write_svg("reflect", &paths, out_dir);
}

fn generate_wiggle(out_dir: &PathBuf) {
    let r = Path::rect(50.0, 50.0, 100.0, 100.0);
    let mut wiggled = wiggle(&r, WiggleScope::Points, Point::new(10.0, 10.0), 12345);
    wiggled.fill = Some(Color::BLACK);
    write_svg("wiggle", &[wiggled], out_dir);
}

fn generate_stack(out_dir: &PathBuf) {
    let shapes: Vec<Path> = (0..3)
        .map(|_| {
            let mut r = Path::rect(0.0, 0.0, 50.0, 30.0);
            r.fill = Some(Color::BLACK);
            r
        })
        .collect();
    let stacked = stack(&shapes, StackDirection::East, 10.0);
    write_svg("stack", &stacked, out_dir);
}

// ============================================================================
// Complex Examples
// ============================================================================

fn generate_spiral(out_dir: &PathBuf) {
    let mut r = rect(Point::new(100.0, 20.0), 8.0, 3.0, Point::ZERO);
    r.fill = Some(Color::rgb(0.2, 0.4, 0.8));
    let copies = copy(&r, 50, CopyOrder::TRS, Point::new(0.0, 2.0), 12.0, Point::new(98.0, 100.0));
    write_svg("spiral", &copies, out_dir);
}

fn generate_radial_pattern(out_dir: &PathBuf) {
    let mut petal = ellipse(Point::new(100.0, 60.0), 20.0, 60.0);
    petal.fill = Some(Color::rgba(0.9, 0.4, 0.4, 0.7));
    let copies = copy(&petal, 8, CopyOrder::TRS, Point::ZERO, 45.0, Point::new(100.0, 100.0));

    let mut center = ellipse(Point::new(100.0, 100.0), 30.0, 30.0);
    center.fill = Some(Color::rgb(0.9, 0.8, 0.2));

    let mut paths = copies;
    paths.push(center);
    write_svg("radial_pattern", &paths, out_dir);
}

fn generate_grid_of_shapes(out_dir: &PathBuf) {
    let points = grid(5, 5, 160.0, 160.0, Point::new(100.0, 100.0));
    let mut paths = Vec::new();
    for (i, p) in points.iter().enumerate() {
        let hue = i as f64 / points.len() as f64;
        let color = hsb_to_rgb(hue, 0.7, 0.9);
        let mut shape = if i % 2 == 0 {
            rect(*p, 20.0, 20.0, Point::ZERO)
        } else {
            ellipse(*p, 20.0, 20.0)
        };
        shape.fill = Some(color);
        paths.push(shape);
    }
    write_svg("grid_of_shapes", &paths, out_dir);
}

fn generate_all_arc_types(out_dir: &PathBuf) {
    let mut arc_pie = arc(Point::new(50.0, 50.0), 60.0, 60.0, 0.0, 120.0, "pie");
    arc_pie.fill = Some(Color::rgb(0.8, 0.2, 0.2));

    let mut arc_chord = arc(Point::new(150.0, 50.0), 60.0, 60.0, 0.0, 120.0, "chord");
    arc_chord.fill = Some(Color::rgb(0.2, 0.8, 0.2));

    let mut arc_open = arc(Point::new(50.0, 150.0), 60.0, 60.0, 0.0, 120.0, "open");
    arc_open.fill = None;
    arc_open.stroke = Some(Color::rgb(0.2, 0.2, 0.8));
    arc_open.stroke_width = 3.0;

    let mut arc_elliptical = arc(Point::new(150.0, 150.0), 60.0, 40.0, 30.0, 270.0, "pie");
    arc_elliptical.fill = Some(Color::rgb(0.8, 0.5, 0.2));

    write_svg("all_arc_types", &[arc_pie, arc_chord, arc_open, arc_elliptical], out_dir);
}

// ============================================================================
// Helpers
// ============================================================================

fn hsb_to_rgb(h: f64, s: f64, b: f64) -> Color {
    let h = h * 6.0;
    let i = h.floor() as i32;
    let f = h - i as f64;
    let p = b * (1.0 - s);
    let q = b * (1.0 - s * f);
    let t = b * (1.0 - s * (1.0 - f));

    let (r, g, b_val) = match i % 6 {
        0 => (b, t, p),
        1 => (q, b, p),
        2 => (p, b, t),
        3 => (p, q, b),
        4 => (t, p, b),
        _ => (b, p, q),
    };

    Color::rgb(r, g, b_val)
}
