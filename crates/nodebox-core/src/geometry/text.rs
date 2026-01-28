//! Text rendering type.

use super::{Point, Color, Rect, Transform};

/// Text alignment options.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub enum TextAlign {
    #[default]
    Left,
    Center,
    Right,
}

/// A text element with position, font, and styling.
///
/// Text rendering requires font support which is platform-dependent.
/// This struct holds the text properties; actual rendering happens elsewhere.
#[derive(Clone, Debug, PartialEq)]
pub struct Text {
    /// The text content.
    pub text: String,
    /// The position (baseline start).
    pub position: Point,
    /// Font family name.
    pub font_family: String,
    /// Font size in points.
    pub font_size: f64,
    /// Text alignment.
    pub align: TextAlign,
    /// Fill color.
    pub fill: Option<Color>,
}

impl Default for Text {
    fn default() -> Self {
        Text {
            text: String::new(),
            position: Point::ZERO,
            font_family: "sans-serif".to_string(),
            font_size: 12.0,
            align: TextAlign::Left,
            fill: Some(Color::BLACK),
        }
    }
}

impl Text {
    /// Creates a new text element.
    pub fn new(text: impl Into<String>, x: f64, y: f64) -> Self {
        Text {
            text: text.into(),
            position: Point::new(x, y),
            ..Default::default()
        }
    }

    /// Creates a text with specified font.
    pub fn with_font(text: impl Into<String>, x: f64, y: f64, font_family: impl Into<String>, font_size: f64) -> Self {
        Text {
            text: text.into(),
            position: Point::new(x, y),
            font_family: font_family.into(),
            font_size,
            ..Default::default()
        }
    }

    /// Sets the text alignment.
    pub fn with_align(mut self, align: TextAlign) -> Self {
        self.align = align;
        self
    }

    /// Sets the fill color.
    pub fn with_fill(mut self, fill: Option<Color>) -> Self {
        self.fill = fill;
        self
    }

    /// Returns an approximate bounding box.
    ///
    /// Note: Accurate text bounds require font metrics which aren't available here.
    /// This returns an estimate based on character count and font size.
    pub fn bounds(&self) -> Option<Rect> {
        if self.text.is_empty() {
            return None;
        }

        // Rough estimate: each character is about 0.6 * font_size wide
        let char_width = self.font_size * 0.6;
        let width = self.text.len() as f64 * char_width;
        let height = self.font_size;

        let x = match self.align {
            TextAlign::Left => self.position.x,
            TextAlign::Center => self.position.x - width / 2.0,
            TextAlign::Right => self.position.x - width,
        };

        Some(Rect::new(x, self.position.y - height, width, height))
    }

    /// Transforms the text position.
    pub fn transform(&self, t: &Transform) -> Text {
        Text {
            position: t.transform_point(self.position),
            ..self.clone()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_text_new() {
        let t = Text::new("Hello", 10.0, 20.0);
        assert_eq!(t.text, "Hello");
        assert_eq!(t.position, Point::new(10.0, 20.0));
    }

    #[test]
    fn test_text_with_font() {
        let t = Text::with_font("Hello", 10.0, 20.0, "Arial", 24.0);
        assert_eq!(t.font_family, "Arial");
        assert_eq!(t.font_size, 24.0);
    }

    #[test]
    fn test_text_bounds() {
        let t = Text::new("Hello", 0.0, 20.0);
        let bounds = t.bounds().unwrap();
        assert!(bounds.width > 0.0);
        assert!(bounds.height > 0.0);
    }

    #[test]
    fn test_text_empty_bounds() {
        let t = Text::new("", 0.0, 0.0);
        assert!(t.bounds().is_none());
    }

    #[test]
    fn test_text_transform() {
        let t = Text::new("Hello", 10.0, 20.0);
        let transform = Transform::translate(5.0, 5.0);
        let transformed = t.transform(&transform);
        assert_eq!(transformed.position, Point::new(15.0, 25.0));
        assert_eq!(transformed.text, "Hello");
    }
}
