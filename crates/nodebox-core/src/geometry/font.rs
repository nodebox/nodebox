//! Font loading and text-to-path conversion.
//!
//! Fonts are parsed in pure Rust (`fontdb` for lookup, `ttf-parser` for
//! outlines), so the same code runs natively and in WebAssembly.
//!
//! Natively the font database holds the system fonts. In WebAssembly there are
//! no system fonts: the host passes font files to [`register_font`]. A name
//! that could not be matched is recorded and returned by
//! [`take_missing_fonts`], so the host can fetch that font and evaluate again.

use std::collections::BTreeSet;
use std::path::Path as FilePath;
use std::sync::{Arc, Mutex, MutexGuard, OnceLock};

use fontdb::{Database, Family, Query, ID};
use ttf_parser::{Face, OutlineBuilder};

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

/// A font file held in memory, with the index of one face in it.
#[derive(Debug, Clone)]
pub struct Font {
    data: Arc<Vec<u8>>,
    index: u32,
}

impl Font {
    /// Creates a font from the bytes of a font file. `index` selects a face in a collection.
    pub fn from_bytes(data: Vec<u8>, index: u32) -> Result<Self, FontError> {
        Face::parse(&data, index)
            .map_err(|e| FontError::LoadError(format!("Failed to parse font: {}", e)))?;
        Ok(Font {
            data: Arc::new(data),
            index,
        })
    }

    fn face(&self) -> Result<Face<'_>, FontError> {
        Face::parse(&self.data, self.index)
            .map_err(|e| FontError::LoadError(format!("Failed to parse font: {}", e)))
    }
}

struct FontStore {
    database: Database,
    /// Faces added with `register_font`. They are matched before system fonts.
    registered: Vec<ID>,
    /// Names that were requested and matched no face.
    missing: BTreeSet<String>,
}

fn store() -> MutexGuard<'static, FontStore> {
    static STORE: OnceLock<Mutex<FontStore>> = OnceLock::new();
    STORE
        .get_or_init(|| {
            #[allow(unused_mut)]
            let mut database = Database::new();
            #[cfg(not(target_arch = "wasm32"))]
            database.load_system_fonts();
            Mutex::new(FontStore {
                database,
                registered: Vec::new(),
                missing: BTreeSet::new(),
            })
        })
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
}

impl FontStore {
    /// Finds the face for `name`, which is a generic family (`sans-serif`), a
    /// PostScript name (`Verdana-Bold`, as stored in .ndbx files) or a family
    /// name (`Verdana`). Falls back to sans-serif, then to any face.
    fn find(&mut self, name: &str) -> Option<ID> {
        let query = |family| {
            self.database.query(&Query {
                families: &[family],
                ..Query::default()
            })
        };
        let generic = match name.to_lowercase().as_str() {
            "sans-serif" | "sans" => Some(Family::SansSerif),
            "serif" => Some(Family::Serif),
            "monospace" | "mono" => Some(Family::Monospace),
            _ => None,
        };
        let found = match generic {
            Some(family) => query(family),
            None => {
                let matches = |face: &&fontdb::FaceInfo| {
                    face.post_script_name.eq_ignore_ascii_case(name)
                        || face.families.iter().any(|(family, _)| family.eq_ignore_ascii_case(name))
                };
                let registered = self
                    .registered
                    .iter()
                    .filter_map(|&id| self.database.face(id))
                    .find(matches);
                registered
                    .or_else(|| {
                        self.database
                            .faces()
                            .find(|face| face.post_script_name.eq_ignore_ascii_case(name))
                    })
                    .map(|face| face.id)
                    .or_else(|| query(Family::Name(name)))
            }
        };
        if found.is_none() && generic.is_none() {
            self.missing.insert(name.to_string());
        }
        found
            .or_else(|| query(Family::SansSerif))
            .or_else(|| self.database.faces().next().map(|face| face.id))
    }
}

/// Adds the faces of a font file to the font database and returns how many were added.
pub fn register_font(data: Vec<u8>) -> usize {
    let mut store = store();
    let ids = store
        .database
        .load_font_source(fontdb::Source::Binary(Arc::new(data)));
    store.registered.extend(ids.iter().copied());
    ids.len()
}

/// Returns the font names that matched no face since the last call, and forgets them.
pub fn take_missing_fonts() -> Vec<String> {
    std::mem::take(&mut store().missing).into_iter().collect()
}

/// Returns the bytes of the font file that holds the best match for `name`.
///
/// A host uses this to hand a font to a WebAssembly client, which passes the
/// bytes to [`register_font`].
pub fn font_data(name: &str) -> Option<Vec<u8>> {
    let mut store = store();
    let id = store.find(name)?;
    store.database.with_face_data(id, |data, _| data.to_vec())
}

/// Loads a font by name.
///
/// Searches the font database for a matching face. Falls back to default
/// sans-serif if the requested font is not found.
pub fn load_font(family_name: &str) -> Result<Font, FontError> {
    let mut store = store();
    let id = store
        .find(family_name)
        .ok_or_else(|| FontError::FontNotFound(family_name.to_string()))?;
    store
        .database
        .with_face_data(id, |data, index| Font::from_bytes(data.to_vec(), index))
        .ok_or_else(|| FontError::LoadError(family_name.to_string()))?
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

    Font::from_bytes(data, 0)
}

/// Receives the outline of one glyph and turns it into contours.
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

impl OutlineBuilder for PathSink {
    fn move_to(&mut self, x: f32, y: f32) {
        // Start a new contour
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
        // Cubic control points are: P1 = P0 + 2/3 * (C - P0), P2 = P + 2/3 * (C - P)
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

/// Lays out `text` on one line, glyph by glyph, starting at `position` on the baseline.
fn layout(face: &Face, text: &str, font_size: f64, position: Point) -> Path {
    let scale = font_size / face.units_per_em() as f64;

    let mut path = Path::new();
    let mut x = position.x;
    let y = position.y;

    for ch in text.chars() {
        if let Some(glyph_id) = face.glyph_index(ch) {
            // A glyph without an outline, such as a space, only advances.
            let mut sink = PathSink::new(scale, x, y);
            face.outline_glyph(glyph_id, &mut sink);
            for contour in sink.finish() {
                path.add_contour(contour);
            }

            x += face.glyph_hor_advance(glyph_id).unwrap_or(0) as f64 * scale;
        } else {
            // No glyph for this character, advance by estimated width
            x += font_size * 0.5;
        }
    }

    path
}

/// The advance width of `text` on one line, as [`layout`] places it.
fn advance(face: &Face, text: &str, font_size: f64) -> f64 {
    let scale = font_size / face.units_per_em() as f64;
    text.chars()
        .map(|ch| match face.glyph_index(ch) {
            Some(glyph_id) => face.glyph_hor_advance(glyph_id).unwrap_or(0) as f64 * scale,
            None => font_size * 0.5,
        })
        .sum()
}

/// Measure the advance width of `text` set on one line.
pub fn text_width(text: &str, font_family: &str, font_size: f64) -> Result<f64, FontError> {
    let mut store = store();
    let id = store
        .find(font_family)
        .ok_or_else(|| FontError::FontNotFound(font_family.to_string()))?;
    store
        .database
        .with_face_data(id, |data, index| {
            let face = Face::parse(data, index)
                .map_err(|e| FontError::LoadError(format!("Failed to parse font: {}", e)))?;
            Ok(advance(&face, text, font_size))
        })
        .ok_or_else(|| FontError::LoadError(font_family.to_string()))?
}

/// Convert text to a vector path.
///
/// # Arguments
/// * `text` - The text to convert
/// * `font_family` - The font name: a PostScript name ("Verdana-Bold"), a
///   family name ("Arial") or a generic family ("sans-serif")
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
    let mut store = store();
    let id = store
        .find(font_family)
        .ok_or_else(|| FontError::FontNotFound(font_family.to_string()))?;
    store
        .database
        .with_face_data(id, |data, index| {
            let face = Face::parse(data, index)
                .map_err(|e| FontError::LoadError(format!("Failed to parse font: {}", e)))?;
            Ok(layout(&face, text, font_size, position))
        })
        .ok_or_else(|| FontError::LoadError(font_family.to_string()))?
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
    Ok(layout(&font.face()?, text, font_size, position))
}

/// List the font families in the font database.
pub fn list_font_families() -> Vec<String> {
    let families: BTreeSet<String> = store()
        .database
        .faces()
        .flat_map(|face| face.families.iter().map(|(name, _)| name.clone()))
        .collect();
    families.into_iter().collect()
}

/// Bundled Inter font bytes (always available, works on all platforms).
///
/// This is used as a fallback when the platform cannot provide font bytes,
/// ensuring that textpath nodes always work, even on WASM.
pub static BUNDLED_FONT_BYTES: &[u8] = include_bytes!("../../resources/Inter.ttf");

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
