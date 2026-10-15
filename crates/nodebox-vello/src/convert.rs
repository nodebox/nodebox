//! Geometry conversion from nodebox-core types to Vello/kurbo types.
//!
//! This module provides conversions between NodeBox's internal geometry
//! representation and Vello's kurbo-based types for GPU rendering.

use std::collections::HashMap;
use std::sync::Arc;

use nodebox_core::geometry::{Color, Contour, Path, PointType, Shape, Transform};
use vello::kurbo::{Affine, BezPath, Point as KurboPoint};
use vello::peniko::Color as PenikoColor;

/// Convert a NodeBox Point to a kurbo Point.
#[inline]
pub fn point_to_kurbo(p: &nodebox_core::geometry::Point) -> KurboPoint {
    KurboPoint::new(p.x, p.y)
}

/// Convert a NodeBox Color to a peniko Color.
#[inline]
pub fn color_to_peniko(c: &Color) -> PenikoColor {
    // peniko Color uses from_rgba8 with u8 components
    PenikoColor::from_rgba8(
        (c.r * 255.0) as u8,
        (c.g * 255.0) as u8,
        (c.b * 255.0) as u8,
        (c.a * 255.0) as u8,
    )
}

/// Convert a NodeBox Transform to a kurbo Affine transform.
pub fn transform_to_affine(t: &Transform) -> Affine {
    // NodeBox Transform stores [m00, m10, m01, m11, m02, m12]
    // which matches kurbo Affine's [a, b, c, d, e, f] layout
    let m = t.as_array();
    Affine::new(m)
}

/// Convert a NodeBox Contour to a kurbo BezPath.
pub fn contour_to_bezpath(contour: &Contour) -> BezPath {
    let mut path = BezPath::new();

    if contour.points.is_empty() {
        return path;
    }

    let mut i = 0;
    let points = &contour.points;

    // Move to the first point
    if let Some(first) = points.first() {
        path.move_to(point_to_kurbo(&first.point));
        i = 1;
    }

    while i < points.len() {
        let pp = &points[i];

        match pp.point_type {
            PointType::LineTo => {
                path.line_to(point_to_kurbo(&pp.point));
                i += 1;
            }
            PointType::CurveData => {
                // Cubic bezier: CurveData (ctrl1), CurveData (ctrl2), CurveTo (end)
                if i + 2 < points.len() {
                    let ctrl1 = &points[i];
                    let ctrl2 = &points[i + 1];
                    let end = &points[i + 2];

                    // Verify the structure is correct
                    if ctrl1.point_type == PointType::CurveData
                        && ctrl2.point_type == PointType::CurveData
                        && end.point_type == PointType::CurveTo
                    {
                        path.curve_to(
                            point_to_kurbo(&ctrl1.point),
                            point_to_kurbo(&ctrl2.point),
                            point_to_kurbo(&end.point),
                        );
                        i += 3;
                        continue;
                    }
                }
                // Fallback: treat as line if structure is invalid
                path.line_to(point_to_kurbo(&pp.point));
                i += 1;
            }
            PointType::CurveTo => {
                // Standalone CurveTo without preceding CurveData - treat as line
                path.line_to(point_to_kurbo(&pp.point));
                i += 1;
            }
            PointType::QuadData => {
                // Quadratic bezier: QuadData (ctrl), QuadTo (end)
                if i + 1 < points.len() {
                    let ctrl = &points[i];
                    let end = &points[i + 1];

                    // Verify the structure is correct
                    if ctrl.point_type == PointType::QuadData
                        && end.point_type == PointType::QuadTo
                    {
                        path.quad_to(
                            point_to_kurbo(&ctrl.point),
                            point_to_kurbo(&end.point),
                        );
                        i += 2;
                        continue;
                    }
                }
                // Fallback: treat as line if structure is invalid
                path.line_to(point_to_kurbo(&pp.point));
                i += 1;
            }
            PointType::QuadTo => {
                // Standalone QuadTo without preceding QuadData - treat as line
                path.line_to(point_to_kurbo(&pp.point));
                i += 1;
            }
        }
    }

    // Close the path if the contour is closed
    if contour.closed {
        path.close_path();
    }

    path
}

/// Convert a NodeBox Path to a kurbo BezPath, with its placement applied.
///
/// This combines all contours into a single BezPath with multiple subpaths.
pub fn path_to_bezpath(path: &Path) -> BezPath {
    let mut bez = shape_to_bezpath(path.shape());
    bez.apply_affine(transform_to_affine(&path.placement()));
    bez
}

/// Convert a NodeBox Shape to a kurbo BezPath, in the shape's own coordinates.
pub fn shape_to_bezpath(shape: &Shape) -> BezPath {
    let mut bez = BezPath::new();
    for contour in shape.contours() {
        bez.extend(contour_to_bezpath(contour).iter());
    }
    bez
}

/// Style information extracted from a NodeBox Path for Vello rendering.
#[derive(Clone, Debug)]
pub struct PathStyle {
    /// Fill color (None for no fill).
    pub fill: Option<PenikoColor>,
    /// Stroke color (None for no stroke).
    pub stroke: Option<PenikoColor>,
    /// Stroke width.
    pub stroke_width: f64,
}

impl PathStyle {
    /// Extract style information from a NodeBox Path.
    pub fn from_path(path: &Path) -> Self {
        PathStyle {
            fill: path.fill.map(|c| color_to_peniko(&c)),
            stroke: path.stroke.map(|c| color_to_peniko(&c)),
            stroke_width: path.stroke_width,
        }
    }
}

/// A converted path ready for Vello rendering.
#[derive(Clone, Debug)]
pub struct VelloPath {
    /// The shape's geometry, in the shape's own coordinates. Paths that share
    /// a shape share this.
    pub bezpath: Arc<BezPath>,
    /// Places the shape: the path's placement as an Affine.
    pub placement: Affine,
    /// Style information for rendering.
    pub style: PathStyle,
}

impl VelloPath {
    /// Convert a NodeBox Path to a VelloPath.
    pub fn from_nodebox_path(path: &Path) -> Self {
        VelloPath {
            bezpath: Arc::new(shape_to_bezpath(path.shape())),
            placement: transform_to_affine(&path.placement()),
            style: PathStyle::from_path(path),
        }
    }

    /// The geometry with the placement applied.
    pub fn placed_bezpath(&self) -> BezPath {
        let mut bez = (*self.bezpath).clone();
        bez.apply_affine(self.placement);
        bez
    }

    /// How to stroke this path so the stroke width is not scaled by the
    /// placement (NodeBox strokes the placed points): a transform to append
    /// to the view transform, and the width to stroke with in shape
    /// coordinates. None when the placement scales unevenly, skews or mirrors, so the
    /// caller strokes [`VelloPath::placed_bezpath`] instead.
    pub fn stroke_in_shape_space(&self, width: f64) -> Option<(Affine, f64)> {
        let [a, b, c, d, _, _] = self.placement.as_coeffs();
        let scale = (a * d - b * c).abs().sqrt();
        let tolerance = 1e-9 * scale.max(1.0);
        // Rotation and uniform scale keep a stroke's shape. Reflections do
        // too, but Vello draws their joins slightly differently, so they are
        // stroked placed.
        let similar = (a - d).abs() < tolerance && (b + c).abs() < tolerance;
        (similar && scale > 1e-12).then(|| (self.placement, width / scale))
    }
}

/// Convert a slice of NodeBox Paths to VelloPaths. Paths that share a shape
/// share one converted BezPath.
pub fn convert_paths(paths: &[Path]) -> Vec<VelloPath> {
    let mut converted: HashMap<*const Shape, Arc<BezPath>> = HashMap::new();
    paths
        .iter()
        .map(|path| {
            let shape = path.shape();
            let bezpath = converted
                .entry(Arc::as_ptr(shape))
                .or_insert_with(|| Arc::new(shape_to_bezpath(shape)))
                .clone();
            VelloPath {
                bezpath,
                placement: transform_to_affine(&path.placement()),
                style: PathStyle::from_path(path),
            }
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use nodebox_core::geometry::Point;

    #[test]
    fn test_point_conversion() {
        let nb_point = Point::new(10.0, 20.0);
        let kurbo_point = point_to_kurbo(&nb_point);
        assert_eq!(kurbo_point.x, 10.0);
        assert_eq!(kurbo_point.y, 20.0);
    }

    #[test]
    fn test_color_conversion() {
        let nb_color = Color::rgba(1.0, 0.5, 0.25, 0.8);
        let peniko_color = color_to_peniko(&nb_color);
        // peniko::Color stores RGBA components
        let comps = peniko_color.to_rgba8();
        assert_eq!(comps.r, 255);
        assert!((comps.g as f64 / 255.0 - 0.5).abs() < 0.01);
        assert!((comps.b as f64 / 255.0 - 0.25).abs() < 0.01);
        assert!((comps.a as f64 / 255.0 - 0.8).abs() < 0.01);
    }

    #[test]
    fn test_line_path_conversion() {
        let path = Path::line(0.0, 0.0, 100.0, 100.0);
        let bezpath = path_to_bezpath(&path);

        // Should have MoveTo and LineTo
        let elements: Vec<_> = bezpath.iter().collect();
        assert_eq!(elements.len(), 2);
    }

    #[test]
    fn test_rect_path_conversion() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let bezpath = path_to_bezpath(&path);

        // Should have MoveTo, 3 LineTo, and ClosePath
        let elements: Vec<_> = bezpath.iter().collect();
        assert_eq!(elements.len(), 5);
    }

    #[test]
    fn test_ellipse_path_conversion() {
        let path = Path::ellipse(50.0, 50.0, 100.0, 100.0);
        let bezpath = path_to_bezpath(&path);

        // Ellipse is 4 cubic beziers + close
        // MoveTo + 4 CurveTo + ClosePath = 6 elements
        let elements: Vec<_> = bezpath.iter().collect();
        assert_eq!(elements.len(), 6);
    }

    #[test]
    fn test_vello_path_style() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0)
            .with_fill(Some(Color::rgb(1.0, 0.0, 0.0)))
            .with_stroke(Some(Color::rgb(0.0, 0.0, 1.0)))
            .with_stroke_width(2.0);

        let vello_path = VelloPath::from_nodebox_path(&path);

        assert!(vello_path.style.fill.is_some());
        assert!(vello_path.style.stroke.is_some());
        assert_eq!(vello_path.style.stroke_width, 2.0);
    }

    #[test]
    fn test_copies_share_one_bezpath() {
        let path = Path::rect(0.0, 0.0, 10.0, 10.0);
        let copies = vec![
            path.transform(&Transform::translate(10.0, 0.0)),
            path.transform(&Transform::rotate(45.0)),
        ];
        let converted = convert_paths(&copies);
        assert!(Arc::ptr_eq(&converted[0].bezpath, &converted[1].bezpath));
        assert_eq!(converted[0].placed_bezpath(), path_to_bezpath(&copies[0]));
    }

    #[test]
    fn test_stroke_in_shape_space() {
        let path = Path::rect(0.0, 0.0, 10.0, 10.0);
        let rotated = VelloPath::from_nodebox_path(&path.transform(&Transform::rotate(30.0).then(&Transform::scale(2.0))));
        let (_, width) = rotated.stroke_in_shape_space(4.0).unwrap();
        assert!((width - 2.0).abs() < 1e-9);

        let mirrored = VelloPath::from_nodebox_path(&path.transform(&Transform::scale_xy(-3.0, 3.0)));
        assert!(mirrored.stroke_in_shape_space(3.0).is_none());

        let squashed = VelloPath::from_nodebox_path(&path.transform(&Transform::scale_xy(2.0, 1.0)));
        assert!(squashed.stroke_in_shape_space(1.0).is_none());
    }
}
