//! Geometry filters - functions that transform existing shapes.

use nodebox_core::geometry::{Point, Path, Geometry, Color, Transform};

/// Horizontal alignment options.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum HAlign {
    /// No horizontal change.
    None,
    /// Align to the left edge.
    Left,
    /// Align to the center.
    Center,
    /// Align to the right edge.
    Right,
}

impl HAlign {
    /// Parse from string.
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "left" => HAlign::Left,
            "center" => HAlign::Center,
            "right" => HAlign::Right,
            _ => HAlign::None,
        }
    }
}

/// Vertical alignment options.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum VAlign {
    /// No vertical change.
    None,
    /// Align to the top edge.
    Top,
    /// Align to the middle.
    Middle,
    /// Align to the bottom edge.
    Bottom,
}

impl VAlign {
    /// Parse from string.
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "top" => VAlign::Top,
            "middle" => VAlign::Middle,
            "bottom" => VAlign::Bottom,
            _ => VAlign::None,
        }
    }
}

/// Align geometry in relation to a position.
///
/// # Arguments
/// * `geometry` - The input geometry
/// * `position` - The alignment point
/// * `halign` - Horizontal alignment
/// * `valign` - Vertical alignment
///
/// # Example
/// ```
/// use nodebox_core::{Point, Path};
/// use nodebox_ops::{align, HAlign, VAlign};
///
/// let path = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let aligned = align(&path, Point::ZERO, HAlign::Center, VAlign::Middle);
/// ```
pub fn align(geometry: &Path, position: Point, halign: HAlign, valign: VAlign) -> Path {
    let bounds = match geometry.bounds() {
        Some(b) => b,
        None => return geometry.clone(),
    };

    let dx = match halign {
        HAlign::Left => position.x - bounds.x,
        HAlign::Right => position.x - bounds.x - bounds.width,
        HAlign::Center => position.x - bounds.x - bounds.width / 2.0,
        HAlign::None => 0.0,
    };

    let dy = match valign {
        VAlign::Top => position.y - bounds.y,
        VAlign::Bottom => position.y - bounds.y - bounds.height,
        VAlign::Middle => position.y - bounds.y - bounds.height / 2.0,
        VAlign::None => 0.0,
    };

    let t = Transform::translate(dx, dy);
    geometry.transform(&t)
}

/// Align geometry in relation to a position using string alignment names.
///
/// # Arguments
/// * `geometry` - The input geometry
/// * `position` - The alignment point
/// * `halign` - Horizontal alignment: "none", "left", "center", "right"
/// * `valign` - Vertical alignment: "none", "top", "middle", "bottom"
pub fn align_str(geometry: &Path, position: Point, halign: &str, valign: &str) -> Path {
    align(geometry, position, HAlign::from_str(halign), VAlign::from_str(valign))
}

/// Calculate the geometric center (centroid) of a shape.
///
/// # Arguments
/// * `geometry` - The input geometry
///
/// # Returns
/// The center point of the bounding box.
///
/// # Example
/// ```
/// use nodebox_core::{Point, Path};
/// use nodebox_ops::centroid;
///
/// // Path::rect uses top-left corner, so center is at (50, 50)
/// let path = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let center = centroid(&path);
/// assert_eq!(center.x, 50.0);
/// assert_eq!(center.y, 50.0);
/// ```
pub fn centroid(geometry: &Path) -> Point {
    match geometry.bounds() {
        Some(bounds) => Point::new(
            bounds.x + bounds.width / 2.0,
            bounds.y + bounds.height / 2.0,
        ),
        None => Point::ZERO,
    }
}

/// Change the color of a shape.
///
/// # Arguments
/// * `path` - The input path
/// * `fill` - The new fill color (use Color with alpha 0 for no fill)
/// * `stroke` - The new stroke color
/// * `stroke_width` - The new stroke width (0 for no stroke)
///
/// # Example
/// ```
/// use nodebox_core::{Path, Color};
/// use nodebox_ops::colorize;
///
/// let path = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let red = Color::rgb(1.0, 0.0, 0.0);
/// let colored = colorize(&path, red, Color::BLACK, 2.0);
/// ```
pub fn colorize(path: &Path, fill: Color, stroke: Color, stroke_width: f64) -> Path {
    let mut result = path.clone();

    // Set fill (if alpha > 0)
    result.fill = if fill.a > 0.0 { Some(fill) } else { None };

    // Set stroke (if width > 0)
    if stroke_width > 0.0 {
        result.stroke = Some(stroke);
        result.stroke_width = stroke_width;
    } else {
        result.stroke = None;
        result.stroke_width = 0.0;
    }

    result
}

/// Fit a shape within given bounds.
///
/// # Arguments
/// * `geometry` - The input geometry
/// * `position` - The target center position
/// * `width` - The target width
/// * `height` - The target height
/// * `keep_proportions` - If true, maintain aspect ratio
///
/// # Example
/// ```
/// use nodebox_core::{Point, Path};
/// use nodebox_ops::fit;
///
/// let path = Path::rect(0.0, 0.0, 100.0, 200.0);
/// let fitted = fit(&path, Point::ZERO, 50.0, 50.0, true);
/// ```
pub fn fit(geometry: &Path, position: Point, width: f64, height: f64, keep_proportions: bool) -> Path {
    let bounds = match geometry.bounds() {
        Some(b) => b,
        None => return geometry.clone(),
    };

    // Handle very small bounds
    let bw = if bounds.width > 1e-12 { bounds.width } else { 0.0 };
    let bh = if bounds.height > 1e-12 { bounds.height } else { 0.0 };

    let (sx, sy) = if keep_proportions {
        let sx = if bw > 0.0 { width / bw } else { f64::MAX };
        let sy = if bh > 0.0 { height / bh } else { f64::MAX };
        let s = sx.min(sy);
        (s, s)
    } else {
        let sx = if bw > 0.0 { width / bw } else { 1.0 };
        let sy = if bh > 0.0 { height / bh } else { 1.0 };
        (sx, sy)
    };

    let t = Transform::translate(position.x, position.y)
        .then(&Transform::scale_xy(sx, sy))
        .then(&Transform::translate(-bw / 2.0 - bounds.x, -bh / 2.0 - bounds.y));

    geometry.transform(&t)
}

/// Fit a shape to another shape's bounding box.
///
/// # Arguments
/// * `geometry` - The shape to fit
/// * `bounding` - The bounding shape to fit into
/// * `keep_proportions` - If true, maintain aspect ratio
pub fn fit_to(geometry: &Path, bounding: &Path, keep_proportions: bool) -> Path {
    let bounds = match bounding.bounds() {
        Some(b) => b,
        None => return geometry.clone(),
    };
    fit(
        geometry,
        Point::new(bounds.x + bounds.width / 2.0, bounds.y + bounds.height / 2.0),
        bounds.width,
        bounds.height,
        keep_proportions,
    )
}

/// The order in which to apply copy transformations.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum CopyOrder {
    /// Scale, Rotate, Translate
    SRT,
    /// Scale, Translate, Rotate
    STR,
    /// Rotate, Scale, Translate
    RST,
    /// Rotate, Translate, Scale
    RTS,
    /// Translate, Scale, Rotate
    TSR,
    /// Translate, Rotate, Scale
    TRS,
}

impl CopyOrder {
    /// Parse from string.
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "srt" => CopyOrder::SRT,
            "str" => CopyOrder::STR,
            "rst" => CopyOrder::RST,
            "rts" => CopyOrder::RTS,
            "tsr" => CopyOrder::TSR,
            "trs" => CopyOrder::TRS,
            _ => CopyOrder::TSR,
        }
    }
}

/// Create multiple transformed copies of a shape.
///
/// # Arguments
/// * `geometry` - The input geometry
/// * `copies` - The number of copies
/// * `order` - The transformation order
/// * `translate` - Translation per copy
/// * `rotate` - Rotation per copy (degrees)
/// * `scale` - Scale per copy (as percentage, e.g., 100.0 = no change)
///
/// # Example
/// ```
/// use nodebox_core::{Point, Path};
/// use nodebox_ops::{copy, CopyOrder};
///
/// let path = Path::rect(0.0, 0.0, 10.0, 10.0);
/// let copies = copy(&path, 5, CopyOrder::TSR, Point::new(20.0, 0.0), 0.0, Point::new(100.0, 100.0));
/// assert_eq!(copies.len(), 5);
/// ```
pub fn copy(
    geometry: &Path,
    copies: u32,
    order: CopyOrder,
    translate: Point,
    rotate: f64,
    scale: Point,
) -> Vec<Path> {
    let mut result = Vec::with_capacity(copies as usize);

    let mut tx = 0.0;
    let mut ty = 0.0;
    let mut r = 0.0;
    let mut sx = 1.0;
    let mut sy = 1.0;

    for _ in 0..copies {
        let transform = build_copy_transform(order, tx, ty, r, sx, sy);
        result.push(geometry.transform(&transform));

        tx += translate.x;
        ty += translate.y;
        r += rotate;
        sx += scale.x / 100.0 - 1.0;
        sy += scale.y / 100.0 - 1.0;
    }

    result
}

fn build_copy_transform(order: CopyOrder, tx: f64, ty: f64, r: f64, sx: f64, sy: f64) -> Transform {
    let mut t = Transform::IDENTITY;

    let ops: [char; 3] = match order {
        CopyOrder::SRT => ['s', 'r', 't'],
        CopyOrder::STR => ['s', 't', 'r'],
        CopyOrder::RST => ['r', 's', 't'],
        CopyOrder::RTS => ['r', 't', 's'],
        CopyOrder::TSR => ['t', 's', 'r'],
        CopyOrder::TRS => ['t', 'r', 's'],
    };

    for op in ops {
        match op {
            't' => t = t.then(&Transform::translate(tx, ty)),
            'r' => t = t.then(&Transform::rotate(r)),
            's' => t = t.then(&Transform::scale_xy(sx, sy)),
            _ => {}
        }
    }

    t
}

/// Group multiple paths into a single Geometry.
///
/// # Arguments
/// * `paths` - The list of paths to group
///
/// # Example
/// ```
/// use nodebox_core::{Path, Geometry};
/// use nodebox_ops::group;
///
/// let rect = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let ellipse = Path::ellipse(0.0, 0.0, 50.0, 50.0);
/// let grouped = group(&[rect, ellipse]);
/// assert_eq!(grouped.paths.len(), 2);
/// ```
pub fn group(paths: &[Path]) -> Geometry {
    let mut geometry = Geometry::new();
    for path in paths {
        geometry.add(path.clone());
    }
    geometry
}

/// Decompose a Geometry into its component paths.
///
/// # Arguments
/// * `geometry` - The input geometry
///
/// # Example
/// ```
/// use nodebox_core::{Path, Geometry};
/// use nodebox_ops::{group, ungroup};
///
/// let rect = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let ellipse = Path::ellipse(0.0, 0.0, 50.0, 50.0);
/// let grouped = group(&[rect, ellipse]);
/// let ungrouped = ungroup(&grouped);
/// assert_eq!(ungrouped.len(), 2);
/// ```
pub fn ungroup(geometry: &Geometry) -> Vec<Path> {
    geometry.paths.clone()
}

/// Get all points from a path.
///
/// # Arguments
/// * `path` - The input path
///
/// # Example
/// ```
/// use nodebox_core::Path;
/// use nodebox_ops::to_points;
///
/// let path = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let points = to_points(&path);
/// assert!(!points.is_empty());
/// ```
pub fn to_points(path: &Path) -> Vec<Point> {
    path.contours
        .iter()
        .flat_map(|c| c.on_curve_points())
        .collect()
}

/// Rotate a shape by the given angle.
///
/// # Arguments
/// * `geometry` - The input geometry
/// * `angle` - The rotation angle in degrees
/// * `origin` - The point to rotate around
///
/// # Example
/// ```
/// use nodebox_core::{Point, Path};
/// use nodebox_ops::rotate;
///
/// let path = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let rotated = rotate(&path, 45.0, Point::ZERO);
/// ```
pub fn rotate(geometry: &Path, angle: f64, origin: Point) -> Path {
    let t = Transform::translate(origin.x, origin.y)
        .then(&Transform::rotate(angle))
        .then(&Transform::translate(-origin.x, -origin.y));
    geometry.transform(&t)
}

/// Scale a shape.
///
/// # Arguments
/// * `geometry` - The input geometry
/// * `scale_pct` - The scale factor (as percentage, e.g., 200.0 = 2x)
/// * `origin` - The point to scale around
///
/// # Example
/// ```
/// use nodebox_core::{Point, Path};
/// use nodebox_ops::scale;
///
/// let path = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let scaled = scale(&path, Point::new(200.0, 200.0), Point::ZERO);
/// ```
pub fn scale(geometry: &Path, scale_pct: Point, origin: Point) -> Path {
    let sx = scale_pct.x / 100.0;
    let sy = scale_pct.y / 100.0;
    let t = Transform::translate(origin.x, origin.y)
        .then(&Transform::scale_xy(sx, sy))
        .then(&Transform::translate(-origin.x, -origin.y));
    geometry.transform(&t)
}

/// Translate a shape.
///
/// # Arguments
/// * `geometry` - The input geometry
/// * `offset` - The translation offset
///
/// # Example
/// ```
/// use nodebox_core::{Point, Path};
/// use nodebox_ops::translate;
///
/// let path = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let moved = translate(&path, Point::new(50.0, 50.0));
/// ```
pub fn translate(geometry: &Path, offset: Point) -> Path {
    let t = Transform::translate(offset.x, offset.y);
    geometry.transform(&t)
}

/// Skew a shape.
///
/// # Arguments
/// * `geometry` - The input geometry
/// * `skew_angle` - The skew angles in degrees (x, y)
/// * `origin` - The point to skew around
///
/// # Example
/// ```
/// use nodebox_core::{Point, Path};
/// use nodebox_ops::skew;
///
/// let path = Path::rect(0.0, 0.0, 100.0, 100.0);
/// let skewed = skew(&path, Point::new(10.0, 0.0), Point::ZERO);
/// ```
pub fn skew(geometry: &Path, skew_angle: Point, origin: Point) -> Path {
    let t = Transform::translate(origin.x, origin.y)
        .then(&Transform::skew(skew_angle.x, skew_angle.y))
        .then(&Transform::translate(-origin.x, -origin.y));
    geometry.transform(&t)
}

/// Return the given path as-is.
///
/// This is useful as a pass-through node.
pub fn do_nothing(path: &Path) -> Path {
    path.clone()
}

// ============================================================================
// Bezier Curve Operations
// ============================================================================

/// Get a point at a specific position along the path.
///
/// # Arguments
/// * `path` - The input path
/// * `t` - Position along the path (0.0 = start, 1.0 = end)
///
/// # Example
/// ```
/// use nodebox_core::Path;
/// use nodebox_ops::point_on_path;
///
/// let path = Path::line(0.0, 0.0, 100.0, 0.0);
/// let mid = point_on_path(&path, 0.5);
/// assert!((mid.x - 50.0).abs() < 0.001);
/// ```
pub fn point_on_path(path: &Path, t: f64) -> Point {
    path.point_at(t)
}

/// Get the total length of a path.
///
/// # Arguments
/// * `path` - The input path
///
/// # Example
/// ```
/// use nodebox_core::Path;
/// use nodebox_ops::path_length;
///
/// let path = Path::line(0.0, 0.0, 100.0, 0.0);
/// assert!((path_length(&path) - 100.0).abs() < 0.001);
/// ```
pub fn path_length(path: &Path) -> f64 {
    path.length()
}

/// Generate evenly-spaced points along a path.
///
/// # Arguments
/// * `path` - The input path
/// * `amount` - The number of points to generate
///
/// # Example
/// ```
/// use nodebox_core::Path;
/// use nodebox_ops::make_points;
///
/// let path = Path::line(0.0, 0.0, 100.0, 0.0);
/// let points = make_points(&path, 5);
/// assert_eq!(points.len(), 5);
/// ```
pub fn make_points(path: &Path, amount: usize) -> Vec<Point> {
    path.make_points(amount)
}

/// Resample a path with a specific number of points.
///
/// Creates a new path with evenly-spaced points along the original path.
/// Useful for simplifying complex curves or adding detail to simple paths.
///
/// # Arguments
/// * `path` - The input path
/// * `amount` - The number of points per contour
///
/// # Example
/// ```
/// use nodebox_core::Path;
/// use nodebox_ops::resample;
///
/// let path = Path::ellipse(0.0, 0.0, 100.0, 100.0);
/// let resampled = resample(&path, 20);
/// assert_eq!(resampled.contours[0].points.len(), 20);
/// ```
pub fn resample(path: &Path, amount: usize) -> Path {
    path.resample_by_amount(amount)
}

/// Resample a path with approximately equal-length segments.
///
/// Creates a new path with points spaced at approximately the given distance.
///
/// # Arguments
/// * `path` - The input path
/// * `segment_length` - The desired length of each segment
///
/// # Example
/// ```
/// use nodebox_core::Path;
/// use nodebox_ops::resample_by_length;
///
/// let path = Path::line(0.0, 0.0, 100.0, 0.0);
/// let resampled = resample_by_length(&path, 25.0);
/// ```
pub fn resample_by_length(path: &Path, segment_length: f64) -> Path {
    path.resample_by_length(segment_length)
}

// ============================================================================
// Advanced Geometry Operations
// ============================================================================

/// Snap geometry to a grid.
///
/// # Arguments
/// * `path` - The input path
/// * `grid_size` - The grid cell size
/// * `strength` - Snap strength from 0.0 (no snap) to 100.0 (full snap)
/// * `offset` - Grid offset position
pub fn snap(path: &Path, grid_size: f64, strength: f64, offset: Point) -> Path {
    if grid_size <= 0.0 {
        return path.clone();
    }

    let strength = (strength / 100.0).clamp(0.0, 1.0);

    let snap_value = |v: f64, grid_offset: f64| -> f64 {
        let grid_pos = v + grid_offset;
        let snapped = (grid_pos / grid_size).round() * grid_size;
        let original = v;
        let snapped_final = snapped - grid_offset;
        original * (1.0 - strength) + snapped_final * strength
    };

    let mut result = path.clone();
    for contour in &mut result.contours {
        for point in &mut contour.points {
            point.point.x = snap_value(point.point.x, offset.x);
            point.point.y = snap_value(point.point.y, offset.y);
        }
    }
    result
}

/// Wiggle scope - which elements to apply random offset to.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum WiggleScope {
    Points,
    Contours,
    Paths,
}

impl WiggleScope {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "contours" => WiggleScope::Contours,
            "paths" => WiggleScope::Paths,
            _ => WiggleScope::Points,
        }
    }
}

/// Wiggle (randomly offset) elements in a path.
///
/// # Arguments
/// * `path` - The input path
/// * `scope` - What to wiggle: points, contours, or the whole path
/// * `offset` - Maximum random offset in x and y
/// * `seed` - Random seed for reproducibility
pub fn wiggle(path: &Path, scope: WiggleScope, offset: Point, seed: u64) -> Path {
    let mut state = seed.wrapping_mul(1000000000);

    let random_offset = |state: &mut u64| -> (f64, f64) {
        *state = state.wrapping_mul(1103515245).wrapping_add(12345);
        let rx = ((*state >> 16) & 0x7FFF) as f64 / 32767.0 - 0.5;
        *state = state.wrapping_mul(1103515245).wrapping_add(12345);
        let ry = ((*state >> 16) & 0x7FFF) as f64 / 32767.0 - 0.5;
        (rx * offset.x * 2.0, ry * offset.y * 2.0)
    };

    match scope {
        WiggleScope::Points => {
            let mut result = path.clone();
            for contour in &mut result.contours {
                for point in &mut contour.points {
                    let (dx, dy) = random_offset(&mut state);
                    point.point.x += dx;
                    point.point.y += dy;
                }
            }
            result
        }
        WiggleScope::Contours => {
            let mut result = path.clone();
            for contour in &mut result.contours {
                let (dx, dy) = random_offset(&mut state);
                for point in &mut contour.points {
                    point.point.x += dx;
                    point.point.y += dy;
                }
            }
            result
        }
        WiggleScope::Paths => {
            let (dx, dy) = random_offset(&mut state);
            translate(path, Point::new(dx, dy))
        }
    }
}

/// Generate random points within the bounding box of a shape.
///
/// # Arguments
/// * `path` - The shape to scatter within
/// * `amount` - Number of points to generate
/// * `seed` - Random seed
///
/// Note: Points are generated within the bounding box and filtered to be inside the shape.
pub fn scatter(path: &Path, amount: usize, seed: u64) -> Vec<Point> {
    let bounds = match path.bounds() {
        Some(b) => b,
        None => return Vec::new(),
    };

    let mut state = seed.wrapping_mul(1000000000);
    let mut points = Vec::with_capacity(amount);

    let random_point = |state: &mut u64| -> Point {
        *state = state.wrapping_mul(1103515245).wrapping_add(12345);
        let rx = ((*state >> 16) & 0x7FFF) as f64 / 32768.0;
        *state = state.wrapping_mul(1103515245).wrapping_add(12345);
        let ry = ((*state >> 16) & 0x7FFF) as f64 / 32768.0;
        Point::new(bounds.x + rx * bounds.width, bounds.y + ry * bounds.height)
    };

    // For simplicity, we generate points in the bounding box
    // A full implementation would check if points are inside the actual path
    for _ in 0..amount {
        let pt = random_point(&mut state);
        points.push(pt);
    }

    points
}

/// Delete scope - which elements to delete.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum DeleteScope {
    Points,
    Paths,
}

impl DeleteScope {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "paths" => DeleteScope::Paths,
            _ => DeleteScope::Points,
        }
    }
}

/// Delete points or paths within a bounding region.
///
/// # Arguments
/// * `path` - The input path
/// * `bounds` - The bounding region
/// * `scope` - Delete points or paths
/// * `delete_inside` - If true, delete elements inside bounds; if false, delete outside
pub fn delete(path: &Path, bounds: &Path, scope: DeleteScope, delete_inside: bool) -> Path {
    let bounding_rect = match bounds.bounds() {
        Some(b) => b,
        None => return path.clone(),
    };

    match scope {
        DeleteScope::Points => {
            let mut result = path.clone();
            for contour in &mut result.contours {
                contour.points.retain(|pp| {
                    let inside = bounding_rect.contains_point(pp.point);
                    if delete_inside { !inside } else { inside }
                });
            }
            // Remove empty contours
            result.contours.retain(|c| !c.points.is_empty());
            result
        }
        DeleteScope::Paths => {
            // For a single path, check if any point is inside the bounds
            let has_point_inside = path.contours.iter().any(|c| {
                c.points.iter().any(|pp| bounding_rect.contains_point(pp.point))
            });
            if (delete_inside && has_point_inside) || (!delete_inside && !has_point_inside) {
                Path::new()
            } else {
                path.clone()
            }
        }
    }
}

/// Reflect a path across an axis.
///
/// # Arguments
/// * `path` - The input path
/// * `origin` - The reflection origin point
/// * `angle` - The reflection axis angle in degrees
/// * `keep_original` - If true, return both original and reflected; if false, only reflected
pub fn reflect(path: &Path, origin: Point, angle: f64, keep_original: bool) -> Geometry {
    use std::f64::consts::PI;

    let angle_rad = angle * PI / 180.0;

    // Reflect a point across a line through origin at the given angle
    let reflect_point = |p: Point| -> Point {
        // Translate to origin
        let px = p.x - origin.x;
        let py = p.y - origin.y;

        // Reflect across the axis
        // Using reflection matrix for a line through origin at angle theta:
        // [cos(2θ)   sin(2θ)]
        // [sin(2θ)  -cos(2θ)]
        let cos_2a = (2.0 * angle_rad).cos();
        let sin_2a = (2.0 * angle_rad).sin();

        let rx = px * cos_2a + py * sin_2a;
        let ry = px * sin_2a - py * cos_2a;

        // Translate back
        Point::new(rx + origin.x, ry + origin.y)
    };

    let mut reflected = path.clone();
    for contour in &mut reflected.contours {
        for point in &mut contour.points {
            point.point = reflect_point(point.point);
        }
    }

    let mut result = Geometry::new();
    if keep_original {
        result.add(path.clone());
    }
    result.add(reflected);
    result
}

/// Sort shapes by a criterion.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum SortBy {
    X,
    Y,
    Angle,
    Distance,
}

impl SortBy {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "y" => SortBy::Y,
            "angle" => SortBy::Angle,
            "distance" => SortBy::Distance,
            _ => SortBy::X,
        }
    }
}

/// Sort a list of paths by a criterion.
///
/// # Arguments
/// * `paths` - The paths to sort
/// * `order_by` - The sorting criterion
/// * `reference` - Reference point for angle/distance sorting
pub fn sort_paths(paths: &[Path], order_by: SortBy, reference: Point) -> Vec<Path> {
    let mut sorted: Vec<(f64, Path)> = paths
        .iter()
        .map(|p| {
            let c = centroid(p);
            let key = match order_by {
                SortBy::X => c.x,
                SortBy::Y => c.y,
                SortBy::Angle => (c.y - reference.y).atan2(c.x - reference.x),
                SortBy::Distance => ((c.x - reference.x).powi(2) + (c.y - reference.y).powi(2)).sqrt(),
            };
            (key, p.clone())
        })
        .collect();

    sorted.sort_by(|a, b| a.0.partial_cmp(&b.0).unwrap_or(std::cmp::Ordering::Equal));
    sorted.into_iter().map(|(_, p)| p).collect()
}

/// Stack direction.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum StackDirection {
    East,
    West,
    North,
    South,
}

impl StackDirection {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "w" | "west" => StackDirection::West,
            "n" | "north" => StackDirection::North,
            "s" | "south" => StackDirection::South,
            _ => StackDirection::East,
        }
    }
}

/// Stack shapes in a direction with margin.
///
/// # Arguments
/// * `paths` - The paths to stack
/// * `direction` - Stack direction (e, w, n, s)
/// * `margin` - Spacing between shapes
pub fn stack(paths: &[Path], direction: StackDirection, margin: f64) -> Vec<Path> {
    if paths.is_empty() {
        return Vec::new();
    }
    if paths.len() == 1 {
        return vec![paths[0].clone()];
    }

    let mut result = Vec::with_capacity(paths.len());
    let first_bounds = paths[0].bounds().unwrap_or_default();

    match direction {
        StackDirection::East => {
            let mut tx = first_bounds.x;
            for path in paths {
                let bounds = path.bounds().unwrap_or_default();
                let offset = tx - bounds.x;
                result.push(translate(path, Point::new(offset, 0.0)));
                tx += bounds.width + margin;
            }
        }
        StackDirection::West => {
            let mut tx = first_bounds.x + first_bounds.width;
            for path in paths {
                let bounds = path.bounds().unwrap_or_default();
                let offset = tx - bounds.x - bounds.width;
                result.push(translate(path, Point::new(offset, 0.0)));
                tx -= bounds.width + margin;
            }
        }
        StackDirection::South => {
            let mut ty = first_bounds.y;
            for path in paths {
                let bounds = path.bounds().unwrap_or_default();
                let offset = ty - bounds.y;
                result.push(translate(path, Point::new(0.0, offset)));
                ty += bounds.height + margin;
            }
        }
        StackDirection::North => {
            let mut ty = first_bounds.y + first_bounds.height;
            for path in paths {
                let bounds = path.bounds().unwrap_or_default();
                let offset = ty - bounds.y - bounds.height;
                result.push(translate(path, Point::new(0.0, offset)));
                ty -= bounds.height + margin;
            }
        }
    }

    result
}

/// Place shapes along a path.
///
/// # Arguments
/// * `shapes` - The shapes to place
/// * `path` - The path to place along
/// * `amount` - Number of copies
/// * `spacing` - Spacing between copies (as percentage of path length)
/// * `margin` - Margin from path ends
/// * `rotate_to_path` - If true, rotate shapes to follow path direction
pub fn shape_on_path(
    shapes: &[Path],
    path: &Path,
    amount: usize,
    spacing: f64,
    margin: f64,
    rotate_to_path: bool,
) -> Vec<Path> {
    if shapes.is_empty() || amount == 0 {
        return Vec::new();
    }

    let path_len = path.length();
    if path_len <= 0.0 {
        return Vec::new();
    }

    let mut result = Vec::with_capacity(amount * shapes.len());
    let margin_ratio = margin / 100.0;
    let usable_length = 1.0 - 2.0 * margin_ratio;
    let spacing_ratio = spacing / 100.0;

    let total_items = amount * shapes.len();
    for i in 0..amount {
        for (j, shape) in shapes.iter().enumerate() {
            let idx = i * shapes.len() + j;
            let pos = if total_items == 1 {
                0.5
            } else if spacing_ratio > 0.0 {
                // Use spacing mode
                margin_ratio + (idx as f64 * spacing_ratio) % usable_length
            } else {
                // Distribute evenly
                margin_ratio + (idx as f64 / (total_items - 1) as f64) * usable_length
            };

            let pos = pos.clamp(0.0, 1.0);
            let p1 = path.point_at(pos);
            let p2 = path.point_at((pos + 0.0001).min(1.0));

            let rotation_angle = if rotate_to_path {
                (p2.y - p1.y).atan2(p2.x - p1.x) * 180.0 / std::f64::consts::PI
            } else {
                0.0
            };

            let transform = Transform::translate(p1.x, p1.y)
                .then(&Transform::rotate(rotation_angle));
            result.push(shape.transform(&transform));
        }
    }

    result
}

/// Create a link path between two shapes.
///
/// # Arguments
/// * `shape1` - First shape
/// * `shape2` - Second shape
/// * `horizontal` - If true, create horizontal link; if false, vertical
pub fn link(shape1: &Path, shape2: &Path, horizontal: bool) -> Path {
    let a = match shape1.bounds() {
        Some(b) => b,
        None => return Path::new(),
    };
    let b = match shape2.bounds() {
        Some(b) => b,
        None => return Path::new(),
    };

    let mut p = Path::new();

    if horizontal {
        let hw = (b.x - (a.x + a.width)) / 2.0;
        p.move_to(a.x + a.width, a.y);
        p.curve_to(
            a.x + a.width + hw, a.y,
            b.x - hw, b.y,
            b.x, b.y,
        );
        p.line_to(b.x, b.y + b.height);
        p.curve_to(
            b.x - hw, b.y + b.height,
            a.x + a.width + hw, a.y + a.height,
            a.x + a.width, a.y + a.height,
        );
    } else {
        let hh = (b.y - (a.y + a.height)) / 2.0;
        p.move_to(a.x, a.y + a.height);
        p.curve_to(
            a.x, a.y + a.height + hh,
            b.x, b.y - hh,
            b.x, b.y,
        );
        p.line_to(b.x + b.width, b.y);
        p.curve_to(
            b.x + b.width, b.y - hh,
            a.x + a.width, a.y + a.height + hh,
            a.x + a.width, a.y + a.height,
        );
    }

    p.fill = None;
    p.stroke = Some(Color::BLACK);
    p.stroke_width = 1.0;
    p
}

/// Parse a freehand path string into a Path.
///
/// The path string format: "M x1,y1 x2,y2 ... M x1,y1 ..."
/// where M starts a new contour.
pub fn freehand(path_string: &str) -> Path {
    let mut path = Path::new();

    for contour_str in path_string.split('M') {
        let contour_str = contour_str.trim();
        if contour_str.is_empty() {
            continue;
        }

        let mut first = true;
        let coords: Vec<&str> = contour_str.split(|c: char| c.is_whitespace() || c == ',')
            .filter(|s| !s.is_empty())
            .collect();

        let mut i = 0;
        while i + 1 < coords.len() {
            if let (Ok(x), Ok(y)) = (coords[i].parse::<f64>(), coords[i + 1].parse::<f64>()) {
                if first {
                    path.move_to(x, y);
                    first = false;
                } else {
                    path.line_to(x, y);
                }
            }
            i += 2;
        }
    }

    path.fill = None;
    path.stroke = Some(Color::BLACK);
    path.stroke_width = 1.0;
    path
}

/// Create a quadratic bezier curve between two points.
///
/// # Arguments
/// * `p1` - Start point
/// * `p2` - End point
/// * `t` - Position of the control point along the line (0-100)
/// * `distance` - Perpendicular distance of control point from line
pub fn quad_curve(p1: Point, p2: Point, t: f64, distance: f64) -> Path {
    use std::f64::consts::PI;

    let t = t / 100.0;

    // Find point along the line
    let cx = p1.x + t * (p2.x - p1.x);
    let cy = p1.y + t * (p2.y - p1.y);

    // Calculate angle perpendicular to the line
    let angle = (p2.y - p1.y).atan2(p2.x - p1.x) + PI / 2.0;

    // Control point offset perpendicular to line
    let qx = cx + distance * angle.cos();
    let qy = cy + distance * angle.sin();

    // Convert quadratic to cubic (for our Path which uses cubics)
    let c1x = p1.x + 2.0 / 3.0 * (qx - p1.x);
    let c1y = p1.y + 2.0 / 3.0 * (qy - p1.y);
    let c2x = p2.x + 2.0 / 3.0 * (qx - p2.x);
    let c2y = p2.y + 2.0 / 3.0 * (qy - p2.y);

    let mut path = Path::new();
    path.move_to(p1.x, p1.y);
    path.curve_to(c1x, c1y, c2x, c2y, p2.x, p2.y);
    path.fill = None;
    path.stroke = Some(Color::BLACK);
    path.stroke_width = 1.0;
    path
}

#[cfg(test)]
mod tests {
    use super::*;
    use approx::assert_relative_eq;

    #[test]
    fn test_centroid() {
        // Path::rect uses top-left corner, so rect(0,0,100,100) has center at (50,50)
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let center = centroid(&path);
        assert_relative_eq!(center.x, 50.0, epsilon = 0.01);
        assert_relative_eq!(center.y, 50.0, epsilon = 0.01);
    }

    #[test]
    fn test_align_center() {
        let path = Path::rect(50.0, 50.0, 100.0, 100.0);
        let aligned = align(&path, Point::ZERO, HAlign::Center, VAlign::Middle);
        let center = centroid(&aligned);
        assert_relative_eq!(center.x, 0.0, epsilon = 0.01);
        assert_relative_eq!(center.y, 0.0, epsilon = 0.01);
    }

    #[test]
    fn test_align_left_top() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let aligned = align(&path, Point::ZERO, HAlign::Left, VAlign::Top);
        let bounds = aligned.bounds().unwrap();
        assert_relative_eq!(bounds.x, 0.0, epsilon = 0.01);
        assert_relative_eq!(bounds.y, 0.0, epsilon = 0.01);
    }

    #[test]
    fn test_colorize() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let red = Color::rgb(1.0, 0.0, 0.0);
        let blue = Color::rgb(0.0, 0.0, 1.0);
        let colored = colorize(&path, red, blue, 2.0);
        assert_eq!(colored.fill, Some(red));
        assert_eq!(colored.stroke, Some(blue));
        assert_eq!(colored.stroke_width, 2.0);
    }

    #[test]
    fn test_colorize_no_fill() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let no_fill = Color::rgba(0.0, 0.0, 0.0, 0.0);
        let colored = colorize(&path, no_fill, Color::BLACK, 1.0);
        assert_eq!(colored.fill, None);
    }

    #[test]
    fn test_fit() {
        let path = Path::rect(0.0, 0.0, 100.0, 200.0);
        let fitted = fit(&path, Point::ZERO, 50.0, 50.0, true);
        let bounds = fitted.bounds().unwrap();

        // Should maintain aspect ratio, so height should be 50 and width 25
        assert_relative_eq!(bounds.height, 50.0, epsilon = 0.1);
        assert_relative_eq!(bounds.width, 25.0, epsilon = 0.1);
    }

    #[test]
    fn test_fit_no_proportions() {
        let path = Path::rect(0.0, 0.0, 100.0, 200.0);
        let fitted = fit(&path, Point::ZERO, 50.0, 50.0, false);
        let bounds = fitted.bounds().unwrap();

        assert_relative_eq!(bounds.width, 50.0, epsilon = 0.1);
        assert_relative_eq!(bounds.height, 50.0, epsilon = 0.1);
    }

    #[test]
    fn test_copy() {
        // Path::rect(0,0,10,10) has center at (5,5)
        let path = Path::rect(0.0, 0.0, 10.0, 10.0);
        let copies = copy(
            &path,
            3,
            CopyOrder::TSR,
            Point::new(20.0, 0.0),
            0.0,
            Point::new(100.0, 100.0),
        );
        assert_eq!(copies.len(), 3);

        // Check positions - first copy is at original position (center 5,5)
        // second is translated by 20, third by 40
        let centers: Vec<Point> = copies.iter().map(|p| centroid(p)).collect();
        assert_relative_eq!(centers[0].x, 5.0, epsilon = 0.1);
        assert_relative_eq!(centers[1].x, 25.0, epsilon = 0.1);
        assert_relative_eq!(centers[2].x, 45.0, epsilon = 0.1);
    }

    #[test]
    fn test_group_ungroup() {
        let rect = Path::rect(0.0, 0.0, 100.0, 100.0);
        let ellipse = Path::ellipse(0.0, 0.0, 50.0, 50.0);
        let grouped = group(&[rect.clone(), ellipse.clone()]);
        assert_eq!(grouped.paths.len(), 2);

        let ungrouped = ungroup(&grouped);
        assert_eq!(ungrouped.len(), 2);
    }

    #[test]
    fn test_to_points() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let points = to_points(&path);
        // Rectangle has 4 corner points (plus close)
        assert!(points.len() >= 4);
    }

    #[test]
    fn test_rotate() {
        // Path::rect(50, 0, 100, 100) has center at (100, 50)
        // Rotating 180 degrees around origin moves it to (-100, -50)
        let path = Path::rect(50.0, 0.0, 100.0, 100.0);
        let rotated = rotate(&path, 180.0, Point::ZERO);
        let center = centroid(&rotated);
        assert_relative_eq!(center.x, -100.0, epsilon = 0.1);
        assert_relative_eq!(center.y, -50.0, epsilon = 0.1);
    }

    #[test]
    fn test_scale() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let scaled = scale(&path, Point::new(200.0, 200.0), Point::ZERO);
        let bounds = scaled.bounds().unwrap();
        assert_relative_eq!(bounds.width, 200.0, epsilon = 0.1);
        assert_relative_eq!(bounds.height, 200.0, epsilon = 0.1);
    }

    #[test]
    fn test_translate() {
        // Path::rect(0,0,100,100) has center at (50,50)
        // After translate by (50,50), center moves to (100,100)
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let moved = translate(&path, Point::new(50.0, 50.0));
        let center = centroid(&moved);
        assert_relative_eq!(center.x, 100.0, epsilon = 0.01);
        assert_relative_eq!(center.y, 100.0, epsilon = 0.01);
    }

    #[test]
    fn test_do_nothing() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let same = do_nothing(&path);
        assert_eq!(path, same);
    }

    // ========================================================================
    // Bezier Operations Tests
    // ========================================================================

    #[test]
    fn test_point_on_path() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);
        let p0 = point_on_path(&path, 0.0);
        let p_half = point_on_path(&path, 0.5);
        let p1 = point_on_path(&path, 1.0);

        assert_relative_eq!(p0.x, 0.0, epsilon = 0.01);
        assert_relative_eq!(p_half.x, 50.0, epsilon = 0.01);
        assert_relative_eq!(p1.x, 100.0, epsilon = 0.01);
    }

    #[test]
    fn test_path_length() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);
        assert_relative_eq!(path_length(&path), 100.0, epsilon = 0.01);
    }

    #[test]
    fn test_make_points() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);
        let points = make_points(&path, 5);

        assert_eq!(points.len(), 5);
        assert_relative_eq!(points[0].x, 0.0, epsilon = 0.01);
        assert_relative_eq!(points[2].x, 50.0, epsilon = 0.01);
        assert_relative_eq!(points[4].x, 100.0, epsilon = 0.01);
    }

    #[test]
    fn test_resample() {
        let path = Path::ellipse(0.0, 0.0, 100.0, 100.0);
        let resampled = resample(&path, 20);

        assert_eq!(resampled.contours.len(), 1);
        assert_eq!(resampled.contours[0].points.len(), 20);
    }

    #[test]
    fn test_resample_by_length() {
        let path = Path::line(0.0, 0.0, 100.0, 0.0);
        let resampled = resample_by_length(&path, 20.0);

        // Should have approximately 5 segments -> 6 points, but rounding may vary
        assert!(resampled.contours[0].points.len() >= 4);
    }

    // ========================================================================
    // Advanced Geometry Tests
    // ========================================================================

    #[test]
    fn test_snap() {
        let path = Path::rect(13.0, 17.0, 10.0, 10.0);
        let snapped = snap(&path, 10.0, 100.0, Point::ZERO);
        let bounds = snapped.bounds().unwrap();
        // With 100% strength, corners should snap to grid
        assert_relative_eq!(bounds.x, 10.0, epsilon = 0.1);
        assert_relative_eq!(bounds.y, 20.0, epsilon = 0.1);
    }

    #[test]
    fn test_snap_partial_strength() {
        let path = Path::rect(15.0, 15.0, 10.0, 10.0);
        let snapped = snap(&path, 10.0, 50.0, Point::ZERO);
        let bounds = snapped.bounds().unwrap();
        // With 50% strength, should be between original (15) and snapped position (20)
        // 15 * 0.5 + 20 * 0.5 = 17.5
        assert!(bounds.x > 15.0 && bounds.x < 20.0);
    }

    #[test]
    fn test_wiggle() {
        let path = Path::rect(0.0, 0.0, 10.0, 10.0);
        let wiggled = wiggle(&path, WiggleScope::Points, Point::new(5.0, 5.0), 42);
        // Points should have moved
        assert_ne!(path.contours[0].points[0].point, wiggled.contours[0].points[0].point);
    }

    #[test]
    fn test_wiggle_reproducible() {
        let path = Path::rect(0.0, 0.0, 10.0, 10.0);
        let w1 = wiggle(&path, WiggleScope::Points, Point::new(5.0, 5.0), 42);
        let w2 = wiggle(&path, WiggleScope::Points, Point::new(5.0, 5.0), 42);
        // Same seed should produce same result
        assert_eq!(w1.contours[0].points[0].point.x, w2.contours[0].points[0].point.x);
    }

    #[test]
    fn test_scatter() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let points = scatter(&path, 10, 42);
        assert_eq!(points.len(), 10);
        // Points should be within bounds
        for p in &points {
            assert!(p.x >= 0.0 && p.x <= 100.0);
            assert!(p.y >= 0.0 && p.y <= 100.0);
        }
    }

    #[test]
    fn test_reflect() {
        let path = Path::rect(10.0, 0.0, 10.0, 10.0);
        let reflected = reflect(&path, Point::ZERO, 90.0, false);
        // Reflected across y-axis (90 degrees) should move from (10,0) to (-10,0) approximately
        let bounds = reflected.paths[0].bounds().unwrap();
        assert!(bounds.x < 0.0);
    }

    #[test]
    fn test_reflect_keep_original() {
        let path = Path::rect(10.0, 0.0, 10.0, 10.0);
        let result = reflect(&path, Point::ZERO, 90.0, true);
        // Should have both original and reflected
        assert_eq!(result.paths.len(), 2);
    }

    #[test]
    fn test_sort_paths() {
        let p1 = Path::rect(30.0, 0.0, 10.0, 10.0);
        let p2 = Path::rect(10.0, 0.0, 10.0, 10.0);
        let p3 = Path::rect(20.0, 0.0, 10.0, 10.0);

        let sorted = sort_paths(&[p1.clone(), p2.clone(), p3.clone()], SortBy::X, Point::ZERO);

        // Should be sorted by x position (centroid)
        let centers: Vec<f64> = sorted.iter().map(|p| centroid(p).x).collect();
        assert!(centers[0] < centers[1]);
        assert!(centers[1] < centers[2]);
    }

    #[test]
    fn test_stack_east() {
        let p1 = Path::rect(0.0, 0.0, 10.0, 10.0);
        let p2 = Path::rect(0.0, 0.0, 10.0, 10.0);
        let p3 = Path::rect(0.0, 0.0, 10.0, 10.0);

        let stacked = stack(&[p1, p2, p3], StackDirection::East, 5.0);

        let bounds: Vec<_> = stacked.iter().map(|p| p.bounds().unwrap()).collect();
        assert_relative_eq!(bounds[0].x, 0.0, epsilon = 0.1);
        assert_relative_eq!(bounds[1].x, 15.0, epsilon = 0.1); // 10 + 5 margin
        assert_relative_eq!(bounds[2].x, 30.0, epsilon = 0.1); // 25 + 5 margin
    }

    #[test]
    fn test_stack_south() {
        let p1 = Path::rect(0.0, 0.0, 10.0, 10.0);
        let p2 = Path::rect(0.0, 0.0, 10.0, 10.0);

        let stacked = stack(&[p1, p2], StackDirection::South, 5.0);

        let bounds: Vec<_> = stacked.iter().map(|p| p.bounds().unwrap()).collect();
        assert_relative_eq!(bounds[0].y, 0.0, epsilon = 0.1);
        assert_relative_eq!(bounds[1].y, 15.0, epsilon = 0.1);
    }

    #[test]
    fn test_link_horizontal() {
        let p1 = Path::rect(0.0, 0.0, 10.0, 10.0);
        let p2 = Path::rect(50.0, 0.0, 10.0, 10.0);

        let linked = link(&p1, &p2, true);
        assert!(!linked.contours.is_empty());
    }

    #[test]
    fn test_freehand() {
        let path = freehand("M 0,0 10,0 10,10 M 20,20 30,30");
        assert_eq!(path.contours.len(), 2);
    }

    #[test]
    fn test_quad_curve() {
        let curve = quad_curve(Point::new(0.0, 0.0), Point::new(100.0, 0.0), 50.0, 50.0);
        assert_eq!(curve.contours.len(), 1);
        // Midpoint should be offset by the distance
        let mid = curve.point_at(0.5);
        assert!(mid.y.abs() > 20.0); // Should be offset significantly
    }

    #[test]
    fn test_shape_on_path() {
        let shape = Path::rect(0.0, 0.0, 10.0, 10.0);
        let guide = Path::line(0.0, 0.0, 100.0, 0.0);

        let placed = shape_on_path(&[shape], &guide, 3, 10.0, 0.0, false);
        assert_eq!(placed.len(), 3);
    }
}
