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
}
