//! SVG rendering implementation.

use nodebox_core::geometry::{Path, Geometry, Color, Contour, PointType, Text, TextAlign, Canvas, Grob};
use std::fmt::Write;

/// Options for SVG rendering.
#[derive(Clone, Debug)]
pub struct SvgOptions {
    /// Width of the SVG canvas.
    pub width: f64,
    /// Height of the SVG canvas.
    pub height: f64,
    /// Background color (None for transparent).
    pub background: Option<Color>,
    /// Precision for coordinate output (decimal places).
    pub precision: usize,
    /// Whether to include the XML declaration.
    pub xml_declaration: bool,
    /// Whether to include a viewBox attribute.
    pub include_viewbox: bool,
}

impl Default for SvgOptions {
    fn default() -> Self {
        SvgOptions {
            width: 500.0,
            height: 500.0,
            background: Some(Color::WHITE),
            precision: 2,
            xml_declaration: true,
            include_viewbox: true,
        }
    }
}

impl SvgOptions {
    /// Creates options with the given dimensions.
    pub fn new(width: f64, height: f64) -> Self {
        SvgOptions {
            width,
            height,
            ..Default::default()
        }
    }

    /// Sets the background color.
    pub fn with_background(mut self, color: Option<Color>) -> Self {
        self.background = color;
        self
    }

    /// Sets whether to include XML declaration.
    pub fn with_xml_declaration(mut self, include: bool) -> Self {
        self.xml_declaration = include;
        self
    }
}

/// Renders a list of paths to SVG format.
///
/// # Arguments
/// * `paths` - The paths to render
/// * `width` - Canvas width
/// * `height` - Canvas height
///
/// # Example
/// ```
/// use nodebox_core::geometry::Path;
/// use nodebox_svg::render_to_svg;
///
/// let circle = Path::ellipse(100.0, 100.0, 80.0, 80.0);
/// let svg = render_to_svg(&[circle], 200.0, 200.0);
/// assert!(svg.contains("<svg"));
/// assert!(svg.contains("</svg>"));
/// ```
pub fn render_to_svg(paths: &[Path], width: f64, height: f64) -> String {
    let options = SvgOptions::new(width, height);
    render_to_svg_with_options(paths, &options)
}

/// Renders paths to SVG with custom options.
pub fn render_to_svg_with_options(paths: &[Path], options: &SvgOptions) -> String {
    let mut svg = String::new();

    // XML declaration
    if options.xml_declaration {
        writeln!(svg, r#"<?xml version="1.0" encoding="UTF-8"?>"#).unwrap();
    }

    // SVG opening tag
    write!(svg, r#"<svg xmlns="http://www.w3.org/2000/svg""#).unwrap();
    write!(svg, r#" width="{}" height="{}""#, options.width, options.height).unwrap();

    if options.include_viewbox {
        write!(svg, r#" viewBox="0 0 {} {}""#, options.width, options.height).unwrap();
    }

    writeln!(svg, ">").unwrap();

    // Background
    if let Some(bg) = options.background {
        writeln!(
            svg,
            r#"  <rect width="100%" height="100%" fill="{}"/>"#,
            color_to_svg(&bg)
        ).unwrap();
    }

    // Render paths
    for path in paths {
        render_path(&mut svg, path, options.precision);
    }

    writeln!(svg, "</svg>").unwrap();

    svg
}

/// Renders a Geometry (collection of paths) to SVG.
pub fn render_geometry_to_svg(geometry: &Geometry, width: f64, height: f64) -> String {
    render_to_svg(&geometry.paths, width, height)
}

/// Renders a Canvas to SVG.
pub fn render_canvas_to_svg(canvas: &Canvas) -> String {
    let options = SvgOptions::new(canvas.width, canvas.height)
        .with_background(canvas.background);

    let mut svg = String::new();

    // XML declaration
    if options.xml_declaration {
        writeln!(svg, r#"<?xml version="1.0" encoding="UTF-8"?>"#).unwrap();
    }

    // SVG opening tag
    write!(svg, r#"<svg xmlns="http://www.w3.org/2000/svg""#).unwrap();
    write!(svg, r#" width="{}" height="{}""#, options.width, options.height).unwrap();

    if options.include_viewbox {
        write!(svg, r#" viewBox="0 0 {} {}""#, options.width, options.height).unwrap();
    }

    writeln!(svg, ">").unwrap();

    // Background
    if let Some(bg) = options.background {
        writeln!(
            svg,
            r#"  <rect width="100%" height="100%" fill="{}"/>"#,
            color_to_svg(&bg)
        ).unwrap();
    }

    // Render all grobs
    for grob in &canvas.items {
        render_grob(&mut svg, grob, options.precision);
    }

    writeln!(svg, "</svg>").unwrap();

    svg
}

fn render_grob(svg: &mut String, grob: &Grob, precision: usize) {
    match grob {
        Grob::Path(path) => render_path(svg, path, precision),
        Grob::Text(text) => render_text(svg, text),
        Grob::Geometry(geometry) => {
            for path in &geometry.paths {
                render_path(svg, path, precision);
            }
        }
        Grob::Canvas(canvas) => {
            // Render nested canvas items
            for item in &canvas.items {
                render_grob(svg, item, precision);
            }
        }
    }
}

fn render_path(svg: &mut String, path: &Path, precision: usize) {
    if path.contours.is_empty() {
        return;
    }

    let path_data = path_to_svg_data(path, precision);
    if path_data.is_empty() {
        return;
    }

    write!(svg, r#"  <path d="{}""#, path_data).unwrap();

    // Fill
    match &path.fill {
        Some(color) if color.a > 0.0 => {
            write!(svg, r#" fill="{}""#, color_to_svg(color)).unwrap();
            if color.a < 1.0 {
                write!(svg, r#" fill-opacity="{:.2}""#, color.a).unwrap();
            }
        }
        _ => {
            write!(svg, r#" fill="none""#).unwrap();
        }
    }

    // Stroke
    if let Some(color) = &path.stroke {
        if color.a > 0.0 && path.stroke_width > 0.0 {
            write!(svg, r#" stroke="{}""#, color_to_svg(color)).unwrap();
            write!(svg, r#" stroke-width="{}""#, path.stroke_width).unwrap();
            if color.a < 1.0 {
                write!(svg, r#" stroke-opacity="{:.2}""#, color.a).unwrap();
            }
        }
    }

    writeln!(svg, "/>").unwrap();
}

fn render_text(svg: &mut String, text: &Text) {
    if text.text.is_empty() {
        return;
    }

    write!(svg, r#"  <text x="{:.2}" y="{:.2}""#, text.position.x, text.position.y).unwrap();

    // Font
    write!(svg, r#" font-family="{}""#, escape_xml(&text.font_family)).unwrap();
    write!(svg, r#" font-size="{}""#, text.font_size).unwrap();

    // Alignment
    let anchor = match text.align {
        TextAlign::Left => "start",
        TextAlign::Center => "middle",
        TextAlign::Right => "end",
    };
    write!(svg, r#" text-anchor="{}""#, anchor).unwrap();

    // Fill
    if let Some(color) = &text.fill {
        write!(svg, r#" fill="{}""#, color_to_svg(color)).unwrap();
    }

    write!(svg, ">{}</text>\n", escape_xml(&text.text)).unwrap();
}

fn path_to_svg_data(path: &Path, precision: usize) -> String {
    let mut data = String::new();

    for contour in &path.contours {
        contour_to_svg_data(&mut data, contour, precision);
    }

    data
}

fn contour_to_svg_data(data: &mut String, contour: &Contour, precision: usize) {
    if contour.points.is_empty() {
        return;
    }

    let mut i = 0;
    let points = &contour.points;

    // First point is always a move
    if !points.is_empty() {
        write!(data, "M{:.prec$},{:.prec$}", points[0].point.x, points[0].point.y, prec = precision).unwrap();
        i = 1;
    }

    while i < points.len() {
        let pt = &points[i];

        match pt.point_type {
            PointType::LineTo => {
                write!(data, " L{:.prec$},{:.prec$}", pt.point.x, pt.point.y, prec = precision).unwrap();
                i += 1;
            }
            PointType::CurveData => {
                // Cubic bezier: ctrl1, ctrl2, end
                if i + 2 < points.len() {
                    let ctrl1 = &points[i];
                    let ctrl2 = &points[i + 1];
                    let end = &points[i + 2];

                    write!(
                        data,
                        " C{:.prec$},{:.prec$} {:.prec$},{:.prec$} {:.prec$},{:.prec$}",
                        ctrl1.point.x, ctrl1.point.y,
                        ctrl2.point.x, ctrl2.point.y,
                        end.point.x, end.point.y,
                        prec = precision
                    ).unwrap();

                    i += 3;
                } else {
                    // Malformed curve data, skip
                    i += 1;
                }
            }
            PointType::CurveTo => {
                // This shouldn't happen without preceding CurveData
                write!(data, " L{:.prec$},{:.prec$}", pt.point.x, pt.point.y, prec = precision).unwrap();
                i += 1;
            }
        }
    }

    if contour.closed {
        data.push_str(" Z");
    }
}

fn color_to_svg(color: &Color) -> String {
    if color.a == 0.0 {
        return "none".to_string();
    }

    // Convert to 0-255 range
    let r = (color.r * 255.0).round() as u8;
    let g = (color.g * 255.0).round() as u8;
    let b = (color.b * 255.0).round() as u8;

    format!("#{:02x}{:02x}{:02x}", r, g, b)
}

fn escape_xml(s: &str) -> String {
    s.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
        .replace('"', "&quot;")
        .replace('\'', "&apos;")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_render_empty() {
        let svg = render_to_svg(&[], 100.0, 100.0);
        assert!(svg.contains("<svg"));
        assert!(svg.contains("</svg>"));
    }

    #[test]
    fn test_render_rect() {
        let rect = Path::rect(10.0, 10.0, 80.0, 60.0);
        let svg = render_to_svg(&[rect], 100.0, 100.0);

        assert!(svg.contains("<path"));
        assert!(svg.contains("M10"));
        assert!(svg.contains("Z"));
    }

    #[test]
    fn test_render_ellipse() {
        let ellipse = Path::ellipse(50.0, 50.0, 80.0, 60.0);
        let svg = render_to_svg(&[ellipse], 100.0, 100.0);

        assert!(svg.contains("<path"));
        assert!(svg.contains("C")); // Contains curves
    }

    #[test]
    fn test_render_line() {
        let line = Path::line(10.0, 10.0, 90.0, 90.0);
        let svg = render_to_svg(&[line], 100.0, 100.0);

        assert!(svg.contains("<path"));
        assert!(svg.contains("M10"));
        assert!(svg.contains("L90"));
    }

    #[test]
    fn test_render_with_fill() {
        let mut rect = Path::rect(10.0, 10.0, 80.0, 60.0);
        rect.fill = Some(Color::rgb(1.0, 0.0, 0.0));
        let svg = render_to_svg(&[rect], 100.0, 100.0);

        assert!(svg.contains(r##"fill="#ff0000""##));
    }

    #[test]
    fn test_render_with_stroke() {
        let mut rect = Path::rect(10.0, 10.0, 80.0, 60.0);
        rect.fill = None;
        rect.stroke = Some(Color::rgb(0.0, 0.0, 1.0));
        rect.stroke_width = 2.0;
        let svg = render_to_svg(&[rect], 100.0, 100.0);

        assert!(svg.contains(r#"fill="none""#));
        assert!(svg.contains(r##"stroke="#0000ff""##));
        assert!(svg.contains(r#"stroke-width="2""#));
    }

    #[test]
    fn test_render_no_xml_declaration() {
        let options = SvgOptions::new(100.0, 100.0)
            .with_xml_declaration(false);
        let svg = render_to_svg_with_options(&[], &options);

        assert!(!svg.contains("<?xml"));
        assert!(svg.contains("<svg"));
    }

    #[test]
    fn test_render_transparent_background() {
        let options = SvgOptions::new(100.0, 100.0)
            .with_background(None);
        let svg = render_to_svg_with_options(&[], &options);

        // Should not have background rect
        assert!(!svg.contains(r#"<rect width="100%""#));
    }

    #[test]
    fn test_color_to_svg() {
        assert_eq!(color_to_svg(&Color::rgb(1.0, 0.0, 0.0)), "#ff0000");
        assert_eq!(color_to_svg(&Color::rgb(0.0, 1.0, 0.0)), "#00ff00");
        assert_eq!(color_to_svg(&Color::rgb(0.0, 0.0, 1.0)), "#0000ff");
        assert_eq!(color_to_svg(&Color::WHITE), "#ffffff");
        assert_eq!(color_to_svg(&Color::BLACK), "#000000");
    }

    #[test]
    fn test_escape_xml() {
        assert_eq!(escape_xml("Hello & World"), "Hello &amp; World");
        assert_eq!(escape_xml("<test>"), "&lt;test&gt;");
    }

    #[test]
    fn test_render_multiple_paths() {
        let rect = Path::rect(10.0, 10.0, 30.0, 30.0);
        let ellipse = Path::ellipse(70.0, 70.0, 40.0, 40.0);
        let svg = render_to_svg(&[rect, ellipse], 100.0, 100.0);

        // Should have two path elements
        let path_count = svg.matches("<path").count();
        assert_eq!(path_count, 2);
    }

    #[test]
    fn test_render_geometry() {
        let mut geom = Geometry::new();
        geom.add(Path::rect(10.0, 10.0, 30.0, 30.0));
        geom.add(Path::ellipse(70.0, 70.0, 40.0, 40.0));

        let svg = render_geometry_to_svg(&geom, 100.0, 100.0);
        let path_count = svg.matches("<path").count();
        assert_eq!(path_count, 2);
    }

    #[test]
    fn test_render_canvas() {
        let mut canvas = Canvas::new(200.0, 200.0);
        canvas.background = Some(Color::rgb(0.9, 0.9, 0.9));
        canvas.add(Path::ellipse(100.0, 100.0, 80.0, 80.0));

        let svg = render_canvas_to_svg(&canvas);
        assert!(svg.contains("width=\"200\""));
        assert!(svg.contains("height=\"200\""));
        assert!(svg.contains("<path"));
    }
}
