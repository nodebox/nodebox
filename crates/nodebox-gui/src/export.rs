//! Export functionality for PNG and PDF.

use std::path::Path;
use nodebox_core::geometry::{Path as GeoPath, Color, PointType};
use tiny_skia::{Pixmap, Paint, PathBuilder, Stroke, Transform, FillRule, LineCap, LineJoin};

/// Export geometry to PNG.
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

    // Draw each path
    for geo_path in geometry {
        draw_path(&mut pixmap, geo_path);
    }

    // Save to file
    pixmap.save_png(path).map_err(|e| e.to_string())
}

/// Draw a geometry path to the pixmap.
fn draw_path(pixmap: &mut Pixmap, geo_path: &GeoPath) {
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
                Transform::identity(),
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
                Transform::identity(),
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
                Transform::identity(),
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
