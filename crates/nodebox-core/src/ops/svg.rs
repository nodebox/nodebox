//! SVG import functionality.
//!
//! This module provides functions to import SVG content and convert it to NodeBox geometry.
//!
//! File reading is NOT done by this module. Callers are responsible for reading
//! SVG files through the Port system (for sandboxed file access) and passing
//! the string content here.

use crate::geometry::{Color, Contour, Geometry, Path, Point, Transform};
use usvg::{tiny_skia_path::PathSegment, Tree};

/// Import SVG from string content and convert it to NodeBox Geometry.
///
/// This is the ONLY version - no file path version exists.
/// Callers are responsible for reading the file through the Port system.
///
/// # Arguments
/// * `svg_content` - SVG content as a string
/// * `centered` - If true, center the geometry at the origin before applying position
/// * `position` - Position offset to apply after optional centering
///
/// # Returns
/// * `Ok(Geometry)` - The imported geometry
/// * `Err(String)` - Error message if import fails
///
/// # Examples
///
/// ```ignore
/// use nodebox_core::geometry::Point;
/// use nodebox_core::ops::import_svg;
///
/// let svg_content = r#"<svg><rect width="100" height="100"/></svg>"#;
/// let geometry = import_svg(svg_content, true, Point::ZERO)?;
/// ```
pub fn import_svg(svg_content: &str, centered: bool, position: Point) -> Result<Geometry, String> {
    // Empty content returns empty geometry
    if svg_content.is_empty() {
        return Ok(Geometry::new());
    }

    // Parse SVG using usvg with default options
    let options = usvg::Options::default();
    let tree = Tree::from_data(svg_content.as_bytes(), &options)
        .map_err(|e| format!("Failed to parse SVG: {}", e))?;

    // Convert the usvg tree to NodeBox geometry
    let mut geometry = convert_tree_to_geometry(&tree);

    // Apply centering if requested
    if centered {
        if let Some(bounds) = geometry.bounds() {
            let center = bounds.center();
            let centering_transform = Transform::translate(-center.x, -center.y);
            geometry = geometry.transform(&centering_transform);
        }
    }

    // Apply position offset
    if position.x != 0.0 || position.y != 0.0 {
        let position_transform = Transform::translate(position.x, position.y);
        geometry = geometry.transform(&position_transform);
    }

    Ok(geometry)
}

/// Convert a usvg Tree to NodeBox Geometry.
fn convert_tree_to_geometry(tree: &Tree) -> Geometry {
    let mut geometry = Geometry::new();

    // Process all nodes in the tree
    for node in tree.root().children() {
        process_node(&node, Transform::IDENTITY, &mut geometry);
    }

    geometry
}

/// Recursively process a usvg node.
fn process_node(node: &usvg::Node, parent_transform: Transform, geometry: &mut Geometry) {
    match node {
        usvg::Node::Group(group) => {
            // Combine parent transform with group transform
            let group_transform = usvg_transform_to_nodebox(&group.transform());
            let combined_transform = parent_transform.then(&group_transform);

            // Process children
            for child in group.children() {
                process_node(&child, combined_transform, geometry);
            }
        }
        usvg::Node::Path(path) => {
            if let Some(nodebox_path) = convert_path(path, &parent_transform) {
                geometry.add(nodebox_path);
            }
        }
        usvg::Node::Image(_) => {
            // Images are not supported, skip
        }
        usvg::Node::Text(_) => {
            // Text is not supported, skip (as per requirements)
        }
    }
}

/// Convert a usvg Transform to a NodeBox Transform.
fn usvg_transform_to_nodebox(t: &usvg::Transform) -> Transform {
    Transform::new(t.sx as f64, t.ky as f64, t.kx as f64, t.sy as f64, t.tx as f64, t.ty as f64)
}

/// Convert a usvg Path to a NodeBox Path.
fn convert_path(usvg_path: &usvg::Path, transform: &Transform) -> Option<Path> {
    let data = usvg_path.data();

    // Create contours from path data
    let contours = convert_path_data(data);

    if contours.is_empty() {
        return None;
    }

    // Create the NodeBox path
    let mut path = Path::from_contours(contours);

    // Apply fill
    if let Some(fill) = usvg_path.fill() {
        path.fill = usvg_paint_to_color(&fill.paint(), fill.opacity().get());
    } else {
        path.fill = None;
    }

    // Apply stroke
    if let Some(stroke) = usvg_path.stroke() {
        path.stroke = usvg_paint_to_color(&stroke.paint(), stroke.opacity().get());
        // Get the stroke width and scale it with the transform
        let base_width = stroke.width().get() as f64;
        // Apply transform scale to stroke width (use average of x and y scale)
        let transform_array = transform.as_array();
        let scale_x = (transform_array[0] * transform_array[0]
            + transform_array[1] * transform_array[1])
            .sqrt();
        let scale_y = (transform_array[2] * transform_array[2]
            + transform_array[3] * transform_array[3])
            .sqrt();
        let avg_scale = (scale_x + scale_y) / 2.0;
        path.stroke_width = base_width * avg_scale;
    } else {
        path.stroke = None;
    }

    // Apply the transform to the path
    let transformed_path = path.transform(transform);

    Some(transformed_path)
}

/// Convert usvg path data to NodeBox contours.
fn convert_path_data(data: &usvg::tiny_skia_path::Path) -> Vec<Contour> {
    let mut contours: Vec<Contour> = Vec::new();
    let mut current_contour: Option<Contour> = None;

    for segment in data.segments() {
        match segment {
            PathSegment::MoveTo(pt) => {
                // Start a new contour
                if let Some(contour) = current_contour.take() {
                    if !contour.is_empty() {
                        contours.push(contour);
                    }
                }
                let mut new_contour = Contour::new();
                new_contour.move_to(pt.x as f64, pt.y as f64);
                current_contour = Some(new_contour);
            }
            PathSegment::LineTo(pt) => {
                if let Some(ref mut contour) = current_contour {
                    contour.line_to(pt.x as f64, pt.y as f64);
                }
            }
            PathSegment::QuadTo(ctrl, end) => {
                if let Some(ref mut contour) = current_contour {
                    contour.quad_to(ctrl.x as f64, ctrl.y as f64, end.x as f64, end.y as f64);
                }
            }
            PathSegment::CubicTo(ctrl1, ctrl2, end) => {
                if let Some(ref mut contour) = current_contour {
                    contour.curve_to(
                        ctrl1.x as f64,
                        ctrl1.y as f64,
                        ctrl2.x as f64,
                        ctrl2.y as f64,
                        end.x as f64,
                        end.y as f64,
                    );
                }
            }
            PathSegment::Close => {
                if let Some(ref mut contour) = current_contour {
                    contour.close();
                }
            }
        }
    }

    // Push any remaining contour
    if let Some(contour) = current_contour {
        if !contour.is_empty() {
            contours.push(contour);
        }
    }

    contours
}

/// Convert a usvg Paint to an optional NodeBox Color.
fn usvg_paint_to_color(paint: &usvg::Paint, opacity: f32) -> Option<Color> {
    match paint {
        usvg::Paint::Color(c) => {
            // Convert u8 components (0-255) to f64 (0.0-1.0)
            let r = c.red as f64 / 255.0;
            let g = c.green as f64 / 255.0;
            let b = c.blue as f64 / 255.0;
            let a = opacity as f64;
            Some(Color::rgba(r, g, b, a))
        }
        usvg::Paint::LinearGradient(_) | usvg::Paint::RadialGradient(_) | usvg::Paint::Pattern(_) => {
            // Gradients and patterns are not supported, return None
            None
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_import_empty_content() {
        let result = import_svg("", false, Point::ZERO);
        assert!(result.is_ok());
        assert!(result.unwrap().is_empty());
    }

    #[test]
    fn test_import_invalid_svg() {
        let result = import_svg("not valid svg content", false, Point::ZERO);
        assert!(result.is_err());
        assert!(result.unwrap_err().contains("Failed to parse SVG"));
    }

    #[test]
    fn test_usvg_transform_identity() {
        let usvg_t = usvg::Transform::identity();
        let nodebox_t = usvg_transform_to_nodebox(&usvg_t);
        assert!(nodebox_t.is_identity());
    }

    #[test]
    fn test_usvg_transform_translate() {
        let usvg_t = usvg::Transform::from_translate(10.0, 20.0);
        let nodebox_t = usvg_transform_to_nodebox(&usvg_t);
        let p = nodebox_t.transform_point(Point::ZERO);
        assert!((p.x - 10.0).abs() < 0.001);
        assert!((p.y - 20.0).abs() < 0.001);
    }

    #[test]
    fn test_usvg_paint_to_color_solid() {
        let paint = usvg::Paint::Color(usvg::Color::new_rgb(255, 128, 0));
        let color = usvg_paint_to_color(&paint, 1.0);
        assert!(color.is_some());
        let c = color.unwrap();
        assert!((c.r - 1.0).abs() < 0.01);
        assert!((c.g - 128.0 / 255.0).abs() < 0.01);
        assert!((c.b - 0.0).abs() < 0.01);
        assert!((c.a - 1.0).abs() < 0.01);
    }

    #[test]
    fn test_usvg_paint_to_color_with_opacity() {
        let paint = usvg::Paint::Color(usvg::Color::new_rgb(255, 255, 255));
        let color = usvg_paint_to_color(&paint, 0.5);
        assert!(color.is_some());
        let c = color.unwrap();
        assert!((c.a - 0.5).abs() < 0.02);
    }

    #[test]
    fn test_import_svg_from_content() {
        let svg_content = r##"<?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                <rect x="10" y="10" width="80" height="80" fill="#ff0000"/>
                <circle cx="50" cy="50" r="25" fill="#00ff00"/>
            </svg>"##;

        let result = import_svg(svg_content, false, Point::ZERO);
        assert!(result.is_ok(), "Failed to import SVG: {:?}", result);

        let geometry = result.unwrap();
        assert!(!geometry.is_empty(), "Geometry should not be empty");

        // Should have at least 2 paths (rect and circle)
        assert!(
            geometry.len() >= 2,
            "Should have at least 2 paths, got {}",
            geometry.len()
        );
    }

    #[test]
    fn test_import_svg_centered() {
        let svg_content = r##"<?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                <rect x="0" y="0" width="100" height="100" fill="#000000"/>
            </svg>"##;

        // Import without centering
        let not_centered = import_svg(svg_content, false, Point::ZERO).unwrap();
        let bounds_not_centered = not_centered.bounds().unwrap();

        // Import with centering
        let centered = import_svg(svg_content, true, Point::ZERO).unwrap();
        let bounds_centered = centered.bounds().unwrap();

        // Centered version should have center at (0, 0)
        let center = bounds_centered.center();
        assert!(
            center.x.abs() < 1.0,
            "Centered X should be near 0, got {}",
            center.x
        );
        assert!(
            center.y.abs() < 1.0,
            "Centered Y should be near 0, got {}",
            center.y
        );

        // Not centered should have different bounds
        let not_centered_center = bounds_not_centered.center();
        assert!(
            (not_centered_center.x - 50.0).abs() < 1.0,
            "Not centered X should be near 50, got {}",
            not_centered_center.x
        );
    }

    #[test]
    fn test_import_svg_with_position() {
        let svg_content = r##"<?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                <rect x="0" y="0" width="100" height="100" fill="#000000"/>
            </svg>"##;

        let offset = Point::new(200.0, 300.0);
        let result = import_svg(svg_content, true, offset).unwrap();
        let bounds = result.bounds().unwrap();

        // Center should be at the offset position
        let center = bounds.center();
        assert!(
            (center.x - 200.0).abs() < 1.0,
            "Center X should be 200, got {}",
            center.x
        );
        assert!(
            (center.y - 300.0).abs() < 1.0,
            "Center Y should be 300, got {}",
            center.y
        );
    }

    #[test]
    fn test_import_svg_colors() {
        let svg_content = r##"<?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                <rect x="0" y="0" width="100" height="100" fill="#ff0000" stroke="#0000ff" stroke-width="2"/>
            </svg>"##;

        let result = import_svg(svg_content, false, Point::ZERO).unwrap();
        assert!(!result.is_empty());

        let path = &result.paths[0];

        // Check fill color (should be red)
        assert!(path.fill.is_some(), "Path should have fill");
        let fill = path.fill.unwrap();
        assert!(
            (fill.r - 1.0).abs() < 0.01,
            "Fill red should be 1.0, got {}",
            fill.r
        );
        assert!(fill.g < 0.01, "Fill green should be 0.0, got {}", fill.g);
        assert!(fill.b < 0.01, "Fill blue should be 0.0, got {}", fill.b);

        // Check stroke color (should be blue)
        assert!(path.stroke.is_some(), "Path should have stroke");
        let stroke = path.stroke.unwrap();
        assert!(stroke.r < 0.01, "Stroke red should be 0.0, got {}", stroke.r);
        assert!(
            stroke.g < 0.01,
            "Stroke green should be 0.0, got {}",
            stroke.g
        );
        assert!(
            (stroke.b - 1.0).abs() < 0.01,
            "Stroke blue should be 1.0, got {}",
            stroke.b
        );

        // Check stroke width
        assert!(
            (path.stroke_width - 2.0).abs() < 0.1,
            "Stroke width should be 2.0, got {}",
            path.stroke_width
        );
    }
}
