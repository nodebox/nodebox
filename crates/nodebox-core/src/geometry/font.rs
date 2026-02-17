//! Font loading and text-to-path conversion.
//!
//! This module provides functionality to convert text to vector paths.
//!
//! - When the `system-fonts` feature is enabled, system fonts can be loaded
//!   using font-kit (desktop platforms).
//! - The `text_to_path_from_bytes()` function uses ttf-parser and works on
//!   all platforms including WASM.

#[cfg(feature = "system-fonts")]
use std::path::Path as FilePath;
#[cfg(feature = "system-fonts")]
use std::sync::Arc;

#[cfg(feature = "system-fonts")]
use font_kit::family_name::FamilyName;
#[cfg(feature = "system-fonts")]
use font_kit::font::Font;
#[cfg(feature = "system-fonts")]
use font_kit::hinting::HintingOptions;
#[cfg(feature = "system-fonts")]
use font_kit::outline::OutlineSink;
#[cfg(feature = "system-fonts")]
use font_kit::properties::Properties;
#[cfg(feature = "system-fonts")]
use font_kit::source::SystemSource;

use super::{Contour, Path, Point};

/// Error type for font operations.
#[derive(Debug, Clone)]
pub enum FontError {
    /// The requested font family was not found.
    FontNotFound(String),
    /// Failed to load the font.
    LoadError(String),
    /// Failed to get glyph outline.
    GlyphError(String),
}

impl std::fmt::Display for FontError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            FontError::FontNotFound(name) => write!(f, "Font not found: {}", name),
            FontError::LoadError(msg) => write!(f, "Failed to load font: {}", msg),
            FontError::GlyphError(msg) => write!(f, "Glyph error: {}", msg),
        }
    }
}

impl std::error::Error for FontError {}

// ===========================================================================
// System font functions (desktop only, requires font-kit)
// ===========================================================================

/// Loads a font by family name.
///
/// Searches system fonts for a matching family. Falls back to default
/// sans-serif if the requested font is not found.
#[cfg(feature = "system-fonts")]
pub fn load_font(family_name: &str) -> Result<Font, FontError> {
    let source = SystemSource::new();

    // Try to find the exact font family
    let family = match family_name.to_lowercase().as_str() {
        "sans-serif" | "sans" => FamilyName::SansSerif,
        "serif" => FamilyName::Serif,
        "monospace" | "mono" => FamilyName::Monospace,
        _ => FamilyName::Title(family_name.to_string()),
    };

    let handle = source
        .select_best_match(&[family.clone()], &Properties::new())
        .or_else(|_| {
            // Fallback to sans-serif
            source.select_best_match(&[FamilyName::SansSerif], &Properties::new())
        })
        .map_err(|e| FontError::FontNotFound(format!("{}: {}", family_name, e)))?;

    handle
        .load()
        .map_err(|e| FontError::LoadError(e.to_string()))
}

/// Loads a font from a file path.
///
/// This is useful for testing with specific font files.
#[cfg(feature = "system-fonts")]
pub fn load_font_from_path(path: impl AsRef<FilePath>) -> Result<Font, FontError> {
    let path = path.as_ref();

    if !path.exists() {
        return Err(FontError::FontNotFound(
            path.display().to_string()
        ));
    }

    let data = std::fs::read(path)
        .map_err(|e| FontError::LoadError(format!("Failed to read font file: {}", e)))?;

    Font::from_bytes(Arc::new(data), 0)
        .map_err(|e| FontError::LoadError(format!("Failed to parse font: {}", e)))
}

/// A sink for receiving path commands from font glyph outlines (font-kit).
#[cfg(feature = "system-fonts")]
struct PathSink {
    contours: Vec<Contour>,
    current_contour: Contour,
    current_point: Point,
    scale: f64,
    offset_x: f64,
    offset_y: f64,
}

#[cfg(feature = "system-fonts")]
impl PathSink {
    fn new(scale: f64, offset_x: f64, offset_y: f64) -> Self {
        PathSink {
            contours: Vec::new(),
            current_contour: Contour::new(),
            current_point: Point::ZERO,
            scale,
            offset_x,
            offset_y,
        }
    }

    fn transform_point(&self, x: f32, y: f32) -> Point {
        Point::new(
            x as f64 * self.scale + self.offset_x,
            // Flip Y since font coordinates are bottom-up
            -y as f64 * self.scale + self.offset_y,
        )
    }

    fn finish(mut self) -> Vec<Contour> {
        // Add the last contour if it has points
        if !self.current_contour.is_empty() {
            self.contours.push(self.current_contour);
        }
        self.contours
    }
}

#[cfg(feature = "system-fonts")]
impl OutlineSink for PathSink {
    fn move_to(&mut self, to: pathfinder_geometry::vector::Vector2F) {
        // Start a new contour
        if !self.current_contour.is_empty() {
            self.contours.push(std::mem::take(&mut self.current_contour));
        }
        let p = self.transform_point(to.x(), to.y());
        self.current_contour.move_to(p.x, p.y);
        self.current_point = p;
    }

    fn line_to(&mut self, to: pathfinder_geometry::vector::Vector2F) {
        let p = self.transform_point(to.x(), to.y());
        self.current_contour.line_to(p.x, p.y);
        self.current_point = p;
    }

    fn quadratic_curve_to(
        &mut self,
        ctrl: pathfinder_geometry::vector::Vector2F,
        to: pathfinder_geometry::vector::Vector2F,
    ) {
        // Convert quadratic to cubic bezier
        // Cubic control points are: P1 = P0 + 2/3 * (C - P0), P2 = P + 2/3 * (C - P)
        let ctrl = self.transform_point(ctrl.x(), ctrl.y());
        let to = self.transform_point(to.x(), to.y());

        let ctrl1 = Point::new(
            self.current_point.x + 2.0 / 3.0 * (ctrl.x - self.current_point.x),
            self.current_point.y + 2.0 / 3.0 * (ctrl.y - self.current_point.y),
        );
        let ctrl2 = Point::new(
            to.x + 2.0 / 3.0 * (ctrl.x - to.x),
            to.y + 2.0 / 3.0 * (ctrl.y - to.y),
        );

        self.current_contour
            .curve_to(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, to.x, to.y);
        self.current_point = to;
    }

    fn cubic_curve_to(
        &mut self,
        ctrl: pathfinder_geometry::line_segment::LineSegment2F,
        to: pathfinder_geometry::vector::Vector2F,
    ) {
        let ctrl0 = self.transform_point(ctrl.from_x(), ctrl.from_y());
        let ctrl1 = self.transform_point(ctrl.to_x(), ctrl.to_y());
        let to = self.transform_point(to.x(), to.y());

        self.current_contour
            .curve_to(ctrl0.x, ctrl0.y, ctrl1.x, ctrl1.y, to.x, to.y);
        self.current_point = to;
    }

    fn close(&mut self) {
        self.current_contour.close();
        self.contours.push(std::mem::take(&mut self.current_contour));
    }
}

/// Convert text to a vector path using system fonts.
///
/// # Arguments
/// * `text` - The text to convert
/// * `font_family` - The font family name (e.g., "Arial", "Helvetica")
/// * `font_size` - The font size in points
/// * `position` - The starting position (baseline)
///
/// # Returns
/// A Path containing the outlines of all glyphs in the text.
///
/// # Example
/// ```ignore
/// use nodebox_core::geometry::{font, Point};
///
/// let path = font::text_to_path("Hello", "Arial", 72.0, Point::new(0.0, 100.0));
/// ```
#[cfg(feature = "system-fonts")]
pub fn text_to_path(
    text: &str,
    font_family: &str,
    font_size: f64,
    position: Point,
) -> Result<Path, FontError> {
    let font = load_font(font_family)?;

    // Get font metrics
    let metrics = font.metrics();
    let units_per_em = metrics.units_per_em as f64;
    let scale = font_size / units_per_em;

    let mut path = Path::new();
    let mut x = position.x;
    let y = position.y;

    for ch in text.chars() {
        let glyph_id = font.glyph_for_char(ch);

        if let Some(glyph_id) = glyph_id {
            // Get glyph advance width
            let advance = font
                .advance(glyph_id)
                .map_err(|e| FontError::GlyphError(e.to_string()))?;

            // Get glyph outline
            let mut sink = PathSink::new(scale, x, y);

            font.outline(glyph_id, HintingOptions::None, &mut sink)
                .map_err(|e| FontError::GlyphError(e.to_string()))?;

            let contours = sink.finish();
            for contour in contours {
                path.add_contour(contour);
            }

            // Advance x position
            x += advance.x() as f64 * scale;
        } else {
            // No glyph for this character, advance by estimated width
            x += font_size * 0.5;
        }
    }

    Ok(path)
}

/// Convert text to path using a font loaded from a file.
///
/// This is useful for testing with specific font files for deterministic results.
#[cfg(feature = "system-fonts")]
pub fn text_to_path_with_font(
    text: &str,
    font: &Font,
    font_size: f64,
    position: Point,
) -> Result<Path, FontError> {
    // Get font metrics
    let metrics = font.metrics();
    let units_per_em = metrics.units_per_em as f64;
    let scale = font_size / units_per_em;

    let mut path = Path::new();
    let mut x = position.x;
    let y = position.y;

    for ch in text.chars() {
        let glyph_id = font.glyph_for_char(ch);

        if let Some(glyph_id) = glyph_id {
            // Get glyph advance width
            let advance = font
                .advance(glyph_id)
                .map_err(|e| FontError::GlyphError(e.to_string()))?;

            // Get glyph outline
            let mut sink = PathSink::new(scale, x, y);

            font.outline(glyph_id, HintingOptions::None, &mut sink)
                .map_err(|e| FontError::GlyphError(e.to_string()))?;

            let contours = sink.finish();
            for contour in contours {
                path.add_contour(contour);
            }

            // Advance x position
            x += advance.x() as f64 * scale;
        } else {
            // No glyph for this character, advance by estimated width
            x += font_size * 0.5;
        }
    }

    Ok(path)
}

/// List available font families on the system.
#[cfg(feature = "system-fonts")]
pub fn list_font_families() -> Vec<String> {
    let source = SystemSource::new();
    source
        .all_families()
        .unwrap_or_default()
}

// ===========================================================================
// ttf-parser based functions (always available, works on WASM)
// ===========================================================================

/// An outline builder that collects glyph outlines into Contours.
struct TtfOutlineBuilder {
    contours: Vec<Contour>,
    current_contour: Contour,
    current_point: Point,
    scale: f64,
    offset_x: f64,
    offset_y: f64,
}

impl TtfOutlineBuilder {
    fn new(scale: f64, offset_x: f64, offset_y: f64) -> Self {
        TtfOutlineBuilder {
            contours: Vec::new(),
            current_contour: Contour::new(),
            current_point: Point::ZERO,
            scale,
            offset_x,
            offset_y,
        }
    }

    fn transform_point(&self, x: f32, y: f32) -> Point {
        Point::new(
            x as f64 * self.scale + self.offset_x,
            // Flip Y since font coordinates are bottom-up
            -y as f64 * self.scale + self.offset_y,
        )
    }

    fn finish(mut self) -> Vec<Contour> {
        if !self.current_contour.is_empty() {
            self.contours.push(self.current_contour);
        }
        self.contours
    }
}

impl ttf_parser::OutlineBuilder for TtfOutlineBuilder {
    fn move_to(&mut self, x: f32, y: f32) {
        if !self.current_contour.is_empty() {
            self.contours.push(std::mem::take(&mut self.current_contour));
        }
        let p = self.transform_point(x, y);
        self.current_contour.move_to(p.x, p.y);
        self.current_point = p;
    }

    fn line_to(&mut self, x: f32, y: f32) {
        let p = self.transform_point(x, y);
        self.current_contour.line_to(p.x, p.y);
        self.current_point = p;
    }

    fn quad_to(&mut self, x1: f32, y1: f32, x: f32, y: f32) {
        // Convert quadratic to cubic bezier
        let ctrl = self.transform_point(x1, y1);
        let to = self.transform_point(x, y);

        let ctrl1 = Point::new(
            self.current_point.x + 2.0 / 3.0 * (ctrl.x - self.current_point.x),
            self.current_point.y + 2.0 / 3.0 * (ctrl.y - self.current_point.y),
        );
        let ctrl2 = Point::new(
            to.x + 2.0 / 3.0 * (ctrl.x - to.x),
            to.y + 2.0 / 3.0 * (ctrl.y - to.y),
        );

        self.current_contour
            .curve_to(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, to.x, to.y);
        self.current_point = to;
    }

    fn curve_to(&mut self, x1: f32, y1: f32, x2: f32, y2: f32, x: f32, y: f32) {
        let ctrl0 = self.transform_point(x1, y1);
        let ctrl1 = self.transform_point(x2, y2);
        let to = self.transform_point(x, y);

        self.current_contour
            .curve_to(ctrl0.x, ctrl0.y, ctrl1.x, ctrl1.y, to.x, to.y);
        self.current_point = to;
    }

    fn close(&mut self) {
        self.current_contour.close();
        self.contours.push(std::mem::take(&mut self.current_contour));
    }
}

/// Convert text to a vector path using raw font bytes (ttf-parser).
///
/// This function does not require system font support and works on all
/// platforms including WASM.
///
/// # Arguments
/// * `text` - The text to convert
/// * `font_bytes` - The raw font file bytes (TTF or OTF)
/// * `font_size` - The font size in points
/// * `position` - The starting position (baseline)
///
/// # Returns
/// A Path containing the outlines of all glyphs in the text.
pub fn text_to_path_from_bytes(
    text: &str,
    font_bytes: &[u8],
    font_size: f64,
    position: Point,
) -> Result<Path, FontError> {
    let face = ttf_parser::Face::parse(font_bytes, 0)
        .map_err(|e| FontError::LoadError(format!("Failed to parse font: {}", e)))?;

    let units_per_em = face.units_per_em() as f64;
    let scale = font_size / units_per_em;

    let mut path = Path::new();
    let mut x = position.x;
    let y = position.y;

    for ch in text.chars() {
        let glyph_id = face.glyph_index(ch);

        if let Some(glyph_id) = glyph_id {
            // Get glyph advance width
            let advance = face
                .glyph_hor_advance(glyph_id)
                .unwrap_or(0) as f64;

            // Get glyph outline
            let mut builder = TtfOutlineBuilder::new(scale, x, y);

            // outline_glyph returns None if the glyph has no outline (e.g. space)
            let _ = face.outline_glyph(glyph_id, &mut builder);

            let contours = builder.finish();
            for contour in contours {
                path.add_contour(contour);
            }

            // Advance x position
            x += advance * scale;
        } else {
            // No glyph for this character, advance by estimated width
            x += font_size * 0.5;
        }
    }

    Ok(path)
}

/// Place text along a path, following the path's curvature.
///
/// Each character is individually positioned and rotated to follow the path.
/// This replicates the algorithm from NodeBox Java's `pyvector/text_on_path`.
///
/// # Arguments
/// * `text` - The text string to place along the path
/// * `shape` - The path to follow
/// * `font_bytes` - Raw font file bytes (TTF or OTF)
/// * `font_size` - Font size in points
/// * `alignment` - `"leading"` (left-to-right) or `"trailing"` (right-to-left)
/// * `margin` - Starting position on the path as a percentage (0–100)
/// * `baseline_offset` - Vertical offset from the path baseline
pub fn text_on_path(
    text: &str,
    shape: &Path,
    font_bytes: &[u8],
    font_size: f64,
    alignment: &str,
    margin: f64,
    baseline_offset: f64,
) -> Result<Path, FontError> {
    use super::Transform;

    if text.is_empty() || shape.length() <= 0.0 {
        return Ok(Path::new());
    }

    let face = ttf_parser::Face::parse(font_bytes, 0)
        .map_err(|e| FontError::LoadError(format!("Failed to parse font: {}", e)))?;

    let units_per_em = face.units_per_em() as f64;
    let scale = font_size / units_per_em;

    // Calculate per-character advance widths
    let char_data: Vec<(char, f64)> = text
        .chars()
        .map(|ch| {
            let advance = face
                .glyph_index(ch)
                .and_then(|gid| face.glyph_hor_advance(gid))
                .unwrap_or(0) as f64
                * scale;
            (ch, advance)
        })
        .collect();

    let string_width: f64 = char_data.iter().map(|(_, w)| w).sum();
    if string_width <= 0.0 {
        return Ok(Path::new());
    }

    let shape_length = shape.length();
    let dw = string_width / shape_length;

    // Handle trailing alignment: walk backwards to find start position
    let effective_margin = if alignment == "trailing" {
        let mut t = (99.9 - margin) / 100.0;
        let mut first = true;
        for &(_, char_width) in &char_data {
            if first {
                first = false;
            } else {
                t -= char_width / string_width * dw;
            }
            t = t.rem_euclid(1.0);
        }
        t * 100.0
    } else {
        margin
    };

    let mut result = Path::new();
    let mut t = 0.0_f64;
    let mut first = true;

    for &(ch, char_width) in &char_data {
        if first {
            t = effective_margin / 100.0;
            first = false;
        } else {
            t += char_width / string_width * dw;
        }

        // Wrap around (matches Python: t = t % 1.0)
        t = t.rem_euclid(1.0);

        // Get point and tangent direction on the path
        let pt1 = shape.point_at(t);
        let pt2 = shape.point_at((t + 0.0000001).min(1.0));

        // Angle from pt2 to pt1 (matching Python: angle(pt2.x, pt2.y, pt1.x, pt1.y))
        let angle_deg = (pt1.y - pt2.y).atan2(pt1.x - pt2.x).to_degrees();

        // Render single character at (-char_width, -baseline_offset)
        let mut builder = TtfOutlineBuilder::new(scale, -char_width, -baseline_offset);
        if let Some(glyph_id) = face.glyph_index(ch) {
            let _ = face.outline_glyph(glyph_id, &mut builder);
        }
        let contours = builder.finish();

        // Transform: rotate to follow path tangent, then translate to path point
        let transform =
            Transform::rotate(angle_deg - 180.0).then(&Transform::translate(pt1.x, pt1.y));

        for contour in contours {
            result.add_contour(contour.transform(&transform));
        }
    }

    Ok(result)
}

/// Bundled Inter font bytes (always available, works on all platforms).
///
/// This is used as a fallback when the platform cannot provide font bytes,
/// ensuring that textpath nodes always work, even on WASM.
pub static BUNDLED_FONT_BYTES: &[u8] = include_bytes!("../../resources/Inter.ttf");

#[cfg(test)]
mod tests {
    use super::*;

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_load_font_sans_serif() {
        let result = load_font("sans-serif");
        assert!(result.is_ok(), "Should be able to load sans-serif");
    }

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_load_font_serif() {
        let result = load_font("serif");
        assert!(result.is_ok(), "Should be able to load serif");
    }

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_load_font_monospace() {
        let result = load_font("monospace");
        assert!(result.is_ok(), "Should be able to load monospace");
    }

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_load_font_fallback() {
        // Even a non-existent font should fall back to sans-serif
        let result = load_font("ThisFontDoesNotExist12345");
        assert!(result.is_ok(), "Should fall back to default font");
    }

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_text_to_path_simple() {
        let result = text_to_path("A", "sans-serif", 72.0, Point::new(0.0, 100.0));
        assert!(result.is_ok(), "Should convert 'A' to path");

        let path = result.unwrap();
        assert!(!path.is_empty(), "Path should not be empty");
    }

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_text_to_path_hello() {
        let result = text_to_path("Hello", "sans-serif", 48.0, Point::new(0.0, 100.0));
        assert!(result.is_ok(), "Should convert 'Hello' to path");

        let path = result.unwrap();
        assert!(!path.is_empty(), "Path should have contours");

        // Check bounds
        let bounds = path.bounds();
        assert!(bounds.is_some(), "Path should have bounds");
    }

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_text_to_path_empty() {
        let result = text_to_path("", "sans-serif", 48.0, Point::ZERO);
        assert!(result.is_ok());

        let path = result.unwrap();
        assert!(path.is_empty(), "Empty text should produce empty path");
    }

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_list_font_families() {
        let families = list_font_families();
        // On most systems there should be at least a few fonts
        assert!(!families.is_empty() || cfg!(target_os = "linux"),
            "Should have some font families (may be empty on minimal Linux)");
    }

    #[cfg(feature = "system-fonts")]
    #[test]
    fn test_text_position() {
        let result = text_to_path("A", "sans-serif", 72.0, Point::new(100.0, 200.0));
        assert!(result.is_ok());

        let path = result.unwrap();
        let bounds = path.bounds().unwrap();

        // The path should be positioned around the given position
        assert!(bounds.x >= 90.0, "Path should be near the x position");
    }

    #[test]
    fn test_text_to_path_from_bytes_empty() {
        // We can't easily embed a font file in tests, but we can test error handling
        let result = text_to_path_from_bytes("Hello", &[], 48.0, Point::ZERO);
        assert!(result.is_err(), "Empty font bytes should fail");
    }
}
