//! Export functionality for PNG and PDF.

use std::path::Path;
use nodebox_core::geometry::{Path as GeoPath, Color, PointType};
use tiny_skia::{Pixmap, Paint, PathBuilder, Stroke, Transform, FillRule, LineCap, LineJoin};

/// Export geometry to PNG.
/// The canvas is defined by width x height centered at origin,
/// so geometry needs to be translated by (width/2, height/2).
pub fn export_png(
    geometry: &[GeoPath],
    path: &Path,
    width: u32,
    height: u32,
    background: Color,
) -> Result<(), String> {
    // Create pixmap
    let mut pixmap = Pixmap::new(width, height)
        .ok_or_else(|| "Failed to create pixmap".to_string())?;

    // Fill background
    let bg_color = tiny_skia::Color::from_rgba(
        background.r as f32,
        background.g as f32,
        background.b as f32,
        background.a as f32,
    ).unwrap_or(tiny_skia::Color::WHITE);
    pixmap.fill(bg_color);

    // Translate to center the canvas (geometry is at origin, canvas is centered)
    let transform = Transform::from_translate(width as f32 / 2.0, height as f32 / 2.0);

    // Draw each path
    for geo_path in geometry {
        draw_path_with_transform(&mut pixmap, geo_path, transform);
    }

    // Save to file
    pixmap.save_png(path).map_err(|e| e.to_string())
}

/// Draw a geometry path to the pixmap with a transform.
fn draw_path_with_transform(pixmap: &mut Pixmap, geo_path: &GeoPath, transform: Transform) {
    for contour in &geo_path.contours {
        if contour.points.is_empty() {
            continue;
        }

        // Build the path
        let mut builder = PathBuilder::new();
        let mut first = true;

        let mut i = 0;
        while i < contour.points.len() {
            let pp = &contour.points[i];
            let x = pp.point.x as f32;
            let y = pp.point.y as f32;

            match pp.point_type {
                PointType::LineTo => {
                    if first {
                        builder.move_to(x, y);
                        first = false;
                    } else {
                        builder.line_to(x, y);
                    }
                    i += 1;
                }
                PointType::CurveTo => {
                    // Cubic bezier: current point is first control point
                    if i + 2 < contour.points.len() {
                        let ctrl1 = &contour.points[i];
                        let ctrl2 = &contour.points[i + 1];
                        let end = &contour.points[i + 2];

                        if first {
                            builder.move_to(ctrl1.point.x as f32, ctrl1.point.y as f32);
                            first = false;
                        }

                        builder.cubic_to(
                            ctrl1.point.x as f32, ctrl1.point.y as f32,
                            ctrl2.point.x as f32, ctrl2.point.y as f32,
                            end.point.x as f32, end.point.y as f32,
                        );
                        i += 3;
                    } else {
                        i += 1;
                    }
                }
                PointType::CurveData => {
                    // Skip curve data points, they're handled with CurveTo
                    i += 1;
                }
                PointType::QuadTo => {
                    // Quadratic bezier: current point is control point
                    if i + 1 < contour.points.len() {
                        let ctrl = &contour.points[i];
                        let end = &contour.points[i + 1];

                        if first {
                            builder.move_to(ctrl.point.x as f32, ctrl.point.y as f32);
                            first = false;
                        }

                        builder.quad_to(
                            ctrl.point.x as f32, ctrl.point.y as f32,
                            end.point.x as f32, end.point.y as f32,
                        );
                        i += 2;
                    } else {
                        i += 1;
                    }
                }
                PointType::QuadData => {
                    // Skip quad data points, they're handled with QuadTo
                    i += 1;
                }
            }
        }

        if contour.closed {
            builder.close();
        }

        let Some(skia_path) = builder.finish() else {
            continue;
        };

        // Draw fill
        if let Some(fill_color) = geo_path.fill {
            let mut paint = Paint::default();
            paint.set_color(color_to_skia(fill_color));
            paint.anti_alias = true;

            pixmap.fill_path(
                &skia_path,
                &paint,
                FillRule::Winding,
                transform,
                None,
            );
        }

        // Draw stroke
        if let Some(stroke_color) = geo_path.stroke {
            let mut paint = Paint::default();
            paint.set_color(color_to_skia(stroke_color));
            paint.anti_alias = true;

            let stroke = Stroke {
                width: geo_path.stroke_width as f32,
                line_cap: LineCap::Round,
                line_join: LineJoin::Round,
                ..Default::default()
            };

            pixmap.stroke_path(
                &skia_path,
                &paint,
                &stroke,
                transform,
                None,
            );
        } else if geo_path.fill.is_none() {
            // Default stroke if no fill and no stroke
            let mut paint = Paint::default();
            paint.set_color_rgba8(0, 0, 0, 255);
            paint.anti_alias = true;

            let stroke = Stroke {
                width: 1.0,
                line_cap: LineCap::Round,
                line_join: LineJoin::Round,
                ..Default::default()
            };

            pixmap.stroke_path(
                &skia_path,
                &paint,
                &stroke,
                transform,
                None,
            );
        }
    }
}

/// Convert a NodeBox color to a tiny-skia color.
fn color_to_skia(color: Color) -> tiny_skia::Color {
    tiny_skia::Color::from_rgba(
        color.r as f32,
        color.g as f32,
        color.b as f32,
        color.a as f32,
    ).unwrap_or(tiny_skia::Color::BLACK)
}

/// Calculate the bounds of geometry.
/// Note: Currently only used in tests, but kept for future use.
#[allow(dead_code)]
pub fn calculate_bounds(geometry: &[GeoPath]) -> (f64, f64, f64, f64) {
    let mut min_x = f64::MAX;
    let mut min_y = f64::MAX;
    let mut max_x = f64::MIN;
    let mut max_y = f64::MIN;

    for geo in geometry {
        if let Some(bounds) = geo.bounds() {
            min_x = min_x.min(bounds.x);
            min_y = min_y.min(bounds.y);
            max_x = max_x.max(bounds.x + bounds.width);
            max_y = max_y.max(bounds.y + bounds.height);
        }
    }

    // Ensure we have valid bounds
    if min_x == f64::MAX {
        min_x = 0.0;
        min_y = 0.0;
        max_x = 100.0;
        max_y = 100.0;
    }

    (min_x, min_y, max_x, max_y)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Read;
    use tempfile::NamedTempFile;

    #[test]
    fn test_export_png_with_canvas_dimensions() {
        // Create a simple rect at origin (centered)
        let rect = GeoPath::rect(-25.0, -25.0, 50.0, 50.0);

        // Create a temp file for the export
        let temp_file = NamedTempFile::new().unwrap();
        let path = temp_file.path();

        // Export with 200x100 canvas dimensions
        // The rect is at origin (-25, -25, 50, 50) so it should be centered in the output
        let result = export_png(&[rect], path, 200, 100, Color::WHITE);
        assert!(result.is_ok(), "Export should succeed");

        // Verify the file was created and has content
        let mut file = std::fs::File::open(path).unwrap();
        let mut buffer = [0u8; 8];
        file.read_exact(&mut buffer).unwrap();

        // PNG files start with the signature: 137 80 78 71 13 10 26 10
        assert_eq!(buffer[0], 137);
        assert_eq!(buffer[1], 80); // 'P'
        assert_eq!(buffer[2], 78); // 'N'
        assert_eq!(buffer[3], 71); // 'G'
    }

    #[test]
    fn test_export_png_geometry_centered() {
        // Create a rect centered at origin (which should appear in center of canvas)
        let mut rect = GeoPath::rect(-50.0, -50.0, 100.0, 100.0);
        rect.fill = Some(Color::rgb(1.0, 0.0, 0.0)); // Red fill

        let temp_file = NamedTempFile::new().unwrap();
        let path = temp_file.path();

        // Export with 200x200 canvas
        // The rect (-50,-50 to 50,50) should appear centered in the 200x200 output
        // After translation by (100,100), the rect should be at (50,50 to 150,150)
        let result = export_png(&[rect], path, 200, 200, Color::WHITE);
        assert!(result.is_ok());

        // Read the PNG to verify its dimensions
        let file_data = std::fs::read(path).unwrap();
        assert!(file_data.len() > 100, "PNG should have substantial content");

        // Check PNG dimensions in IHDR chunk
        // IHDR starts at offset 8 (after signature) + 4 (length) + 4 (IHDR tag)
        // Width is at offset 16, height at offset 20 (both big-endian u32)
        let width = u32::from_be_bytes([file_data[16], file_data[17], file_data[18], file_data[19]]);
        let height = u32::from_be_bytes([file_data[20], file_data[21], file_data[22], file_data[23]]);
        assert_eq!(width, 200);
        assert_eq!(height, 200);
    }

    #[test]
    fn test_export_png_empty_geometry() {
        let temp_file = NamedTempFile::new().unwrap();
        let path = temp_file.path();

        // Export with no geometry - should still create valid PNG with just background
        let result = export_png(&[], path, 100, 100, Color::rgb(0.5, 0.5, 0.5));
        assert!(result.is_ok());

        // Verify it's a valid PNG
        let file_data = std::fs::read(path).unwrap();
        assert_eq!(file_data[1], 80); // 'P' in PNG signature
    }

    #[test]
    fn test_calculate_bounds_single_geometry() {
        let rect = GeoPath::rect(10.0, 20.0, 30.0, 40.0);
        let (min_x, min_y, max_x, max_y) = calculate_bounds(&[rect]);

        assert_eq!(min_x, 10.0);
        assert_eq!(min_y, 20.0);
        assert_eq!(max_x, 40.0);
        assert_eq!(max_y, 60.0);
    }

    #[test]
    fn test_calculate_bounds_empty() {
        let (min_x, min_y, max_x, max_y) = calculate_bounds(&[]);

        // Empty geometry should return default bounds
        assert_eq!(min_x, 0.0);
        assert_eq!(min_y, 0.0);
        assert_eq!(max_x, 100.0);
        assert_eq!(max_y, 100.0);
    }
}
