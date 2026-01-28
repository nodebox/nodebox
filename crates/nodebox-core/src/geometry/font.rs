//! Font loading and text-to-path conversion using font-kit.
//!
//! This module provides functionality to convert text to vector paths
//! using system fonts.

use std::path::Path as FilePath;
use std::sync::Arc;

use font_kit::family_name::FamilyName;
use font_kit::font::Font;
use font_kit::hinting::HintingOptions;
use font_kit::outline::OutlineSink;
use font_kit::properties::Properties;
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

/// Loads a font by family name.
///
/// Searches system fonts for a matching family. Falls back to default
/// sans-serif if the requested font is not found.
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

/// A sink for receiving path commands from font glyph outlines.
struct PathSink {
    contours: Vec<Contour>,
    current_contour: Contour,
    current_point: Point,
    scale: f64,
    offset_x: f64,
    offset_y: f64,
}

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

/// Convert text to a vector path.
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
pub fn list_font_families() -> Vec<String> {
    let source = SystemSource::new();
    source
        .all_families()
        .unwrap_or_default()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_load_font_sans_serif() {
        let result = load_font("sans-serif");
        assert!(result.is_ok(), "Should be able to load sans-serif");
    }

    #[test]
    fn test_load_font_serif() {
        let result = load_font("serif");
        assert!(result.is_ok(), "Should be able to load serif");
    }

    #[test]
    fn test_load_font_monospace() {
        let result = load_font("monospace");
        assert!(result.is_ok(), "Should be able to load monospace");
    }

    #[test]
    fn test_load_font_fallback() {
        // Even a non-existent font should fall back to sans-serif
        let result = load_font("ThisFontDoesNotExist12345");
        assert!(result.is_ok(), "Should fall back to default font");
    }

    #[test]
    fn test_text_to_path_simple() {
        let result = text_to_path("A", "sans-serif", 72.0, Point::new(0.0, 100.0));
        assert!(result.is_ok(), "Should convert 'A' to path");

        let path = result.unwrap();
        assert!(!path.is_empty(), "Path should not be empty");
    }

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

    #[test]
    fn test_text_to_path_empty() {
        let result = text_to_path("", "sans-serif", 48.0, Point::ZERO);
        assert!(result.is_ok());

        let path = result.unwrap();
        assert!(path.is_empty(), "Empty text should produce empty path");
    }

    #[test]
    fn test_list_font_families() {
        let families = list_font_families();
        // On most systems there should be at least a few fonts
        assert!(!families.is_empty() || cfg!(target_os = "linux"),
            "Should have some font families (may be empty on minimal Linux)");
    }

    #[test]
    fn test_text_position() {
        let result = text_to_path("A", "sans-serif", 72.0, Point::new(100.0, 200.0));
        assert!(result.is_ok());

        let path = result.unwrap();
        let bounds = path.bounds().unwrap();

        // The path should be positioned around the given position
        assert!(bounds.x >= 90.0, "Path should be near the x position");
    }
}
