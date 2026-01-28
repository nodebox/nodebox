//! RGBA color type with HSB conversion support.

use std::fmt;
use std::str::FromStr;

/// An RGBA color with components in the range 0.0 to 1.0.
///
/// Colors are immutable. All color components are stored as f64 in the range [0.0, 1.0].
///
/// # Examples
///
/// ```
/// use nodebox_core::Color;
///
/// let red = Color::rgb(1.0, 0.0, 0.0);
/// let semi_transparent = Color::rgba(1.0, 0.0, 0.0, 0.5);
/// let gray = Color::gray(0.5);
/// ```
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Color {
    /// Red component (0.0 to 1.0)
    pub r: f64,
    /// Green component (0.0 to 1.0)
    pub g: f64,
    /// Blue component (0.0 to 1.0)
    pub b: f64,
    /// Alpha component (0.0 = transparent, 1.0 = opaque)
    pub a: f64,
}

impl Default for Color {
    fn default() -> Self {
        Color::BLACK
    }
}

impl Color {
    /// Black color.
    pub const BLACK: Color = Color { r: 0.0, g: 0.0, b: 0.0, a: 1.0 };

    /// White color.
    pub const WHITE: Color = Color { r: 1.0, g: 1.0, b: 1.0, a: 1.0 };

    /// Transparent (invisible) color.
    pub const TRANSPARENT: Color = Color { r: 0.0, g: 0.0, b: 0.0, a: 0.0 };

    /// Creates a new color with the given RGBA components.
    ///
    /// Components are clamped to the range [0.0, 1.0].
    #[inline]
    pub fn rgba(r: f64, g: f64, b: f64, a: f64) -> Self {
        Color {
            r: clamp(r),
            g: clamp(g),
            b: clamp(b),
            a: clamp(a),
        }
    }

    /// Creates a new opaque color with the given RGB components.
    #[inline]
    pub fn rgb(r: f64, g: f64, b: f64) -> Self {
        Color::rgba(r, g, b, 1.0)
    }

    /// Creates a grayscale color with the given value.
    #[inline]
    pub fn gray(v: f64) -> Self {
        Color::rgba(v, v, v, 1.0)
    }

    /// Creates a grayscale color with alpha.
    #[inline]
    pub fn gray_alpha(v: f64, a: f64) -> Self {
        Color::rgba(v, v, v, a)
    }

    /// Creates a color from HSB (Hue, Saturation, Brightness) values.
    ///
    /// All components should be in the range [0.0, 1.0].
    /// Hue of 0.0 and 1.0 are both red.
    pub fn hsb(h: f64, s: f64, b: f64) -> Self {
        Color::hsba(h, s, b, 1.0)
    }

    /// Creates a color from HSBA (Hue, Saturation, Brightness, Alpha) values.
    pub fn hsba(h: f64, s: f64, v: f64, a: f64) -> Self {
        let h = clamp(h);
        let s = clamp(s);
        let v = clamp(v);
        let a = clamp(a);

        if s == 0.0 {
            return Color::rgba(v, v, v, a);
        }

        let h = if h >= 1.0 { 0.999998 } else { h };
        let h = h / (60.0 / 360.0);
        let i = h.floor() as i32;
        let f = h - h.floor();
        let p = v * (1.0 - s);
        let q = v * (1.0 - s * f);
        let t = v * (1.0 - s * (1.0 - f));

        let (r, g, b) = match i {
            0 => (v, t, p),
            1 => (q, v, p),
            2 => (p, v, t),
            3 => (p, q, v),
            4 => (t, p, v),
            _ => (v, p, q),
        };

        Color::rgba(r, g, b, a)
    }

    /// Creates a color from a hex string like "#FF0000" or "#FF0000FF".
    ///
    /// Supports formats: "#RGB", "#RGBA", "#RRGGBB", "#RRGGBBAA"
    pub fn from_hex(hex: &str) -> Result<Self, ParseColorError> {
        let hex = hex.trim_start_matches('#');

        match hex.len() {
            3 => {
                // #RGB
                let r = u8::from_str_radix(&hex[0..1], 16).map_err(|_| ParseColorError)?;
                let g = u8::from_str_radix(&hex[1..2], 16).map_err(|_| ParseColorError)?;
                let b = u8::from_str_radix(&hex[2..3], 16).map_err(|_| ParseColorError)?;
                Ok(Color::rgb(
                    (r * 17) as f64 / 255.0,
                    (g * 17) as f64 / 255.0,
                    (b * 17) as f64 / 255.0,
                ))
            }
            4 => {
                // #RGBA
                let r = u8::from_str_radix(&hex[0..1], 16).map_err(|_| ParseColorError)?;
                let g = u8::from_str_radix(&hex[1..2], 16).map_err(|_| ParseColorError)?;
                let b = u8::from_str_radix(&hex[2..3], 16).map_err(|_| ParseColorError)?;
                let a = u8::from_str_radix(&hex[3..4], 16).map_err(|_| ParseColorError)?;
                Ok(Color::rgba(
                    (r * 17) as f64 / 255.0,
                    (g * 17) as f64 / 255.0,
                    (b * 17) as f64 / 255.0,
                    (a * 17) as f64 / 255.0,
                ))
            }
            6 => {
                // #RRGGBB
                let r = u8::from_str_radix(&hex[0..2], 16).map_err(|_| ParseColorError)?;
                let g = u8::from_str_radix(&hex[2..4], 16).map_err(|_| ParseColorError)?;
                let b = u8::from_str_radix(&hex[4..6], 16).map_err(|_| ParseColorError)?;
                Ok(Color::rgb(
                    r as f64 / 255.0,
                    g as f64 / 255.0,
                    b as f64 / 255.0,
                ))
            }
            8 => {
                // #RRGGBBAA
                let r = u8::from_str_radix(&hex[0..2], 16).map_err(|_| ParseColorError)?;
                let g = u8::from_str_radix(&hex[2..4], 16).map_err(|_| ParseColorError)?;
                let b = u8::from_str_radix(&hex[4..6], 16).map_err(|_| ParseColorError)?;
                let a = u8::from_str_radix(&hex[6..8], 16).map_err(|_| ParseColorError)?;
                Ok(Color::rgba(
                    r as f64 / 255.0,
                    g as f64 / 255.0,
                    b as f64 / 255.0,
                    a as f64 / 255.0,
                ))
            }
            _ => Err(ParseColorError),
        }
    }

    /// Returns true if the color is visible (alpha > 0).
    #[inline]
    pub fn is_visible(&self) -> bool {
        self.a > 0.0
    }

    /// Returns the hue component (0.0 to 1.0).
    pub fn hue(&self) -> f64 {
        self.to_hsb().0
    }

    /// Returns the saturation component (0.0 to 1.0).
    pub fn saturation(&self) -> f64 {
        self.to_hsb().1
    }

    /// Returns the brightness component (0.0 to 1.0).
    pub fn brightness(&self) -> f64 {
        self.to_hsb().2
    }

    /// Converts to HSB (Hue, Saturation, Brightness) components.
    pub fn to_hsb(&self) -> (f64, f64, f64) {
        let v = self.r.max(self.g).max(self.b);
        let d = v - self.r.min(self.g).min(self.b);

        let s = if v != 0.0 { d / v } else { 0.0 };

        let h = if s != 0.0 {
            let h = if self.r == v {
                (self.g - self.b) / d
            } else if self.g == v {
                2.0 + (self.b - self.r) / d
            } else {
                4.0 + (self.r - self.g) / d
            };

            let h = h * (60.0 / 360.0);
            if h < 0.0 { h + 1.0 } else { h }
        } else {
            0.0
        };

        (h, s, v)
    }

    /// Returns a new color with modified alpha.
    #[inline]
    pub fn with_alpha(self, a: f64) -> Self {
        Color::rgba(self.r, self.g, self.b, a)
    }

    /// Linearly interpolates between this color and another.
    pub fn lerp(self, other: Color, t: f64) -> Color {
        Color::rgba(
            self.r + (other.r - self.r) * t,
            self.g + (other.g - self.g) * t,
            self.b + (other.b - self.b) * t,
            self.a + (other.a - self.a) * t,
        )
    }

    /// Converts to CSS color string for SVG output.
    ///
    /// Returns "none" for invisible colors, "#RRGGBB" for opaque colors,
    /// or "rgba(r,g,b,a)" for semi-transparent colors.
    pub fn to_css(&self) -> String {
        if !self.is_visible() {
            return "none".to_string();
        }

        let r256 = (self.r * 255.0).round() as u8;
        let g256 = (self.g * 255.0).round() as u8;
        let b256 = (self.b * 255.0).round() as u8;

        if (self.a - 1.0).abs() < 1e-10 {
            format!("#{:02X}{:02X}{:02X}", r256, g256, b256)
        } else {
            format!("rgba({},{},{},{:.2})", r256, g256, b256, self.a)
        }
    }

    /// Converts to a hex string like "#RRGGBBAA".
    pub fn to_hex(&self) -> String {
        let r256 = (self.r * 255.0).round() as u8;
        let g256 = (self.g * 255.0).round() as u8;
        let b256 = (self.b * 255.0).round() as u8;
        let a256 = (self.a * 255.0).round() as u8;
        format!("#{:02X}{:02X}{:02X}{:02X}", r256, g256, b256, a256)
    }
}

/// Error type for parsing colors.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct ParseColorError;

impl fmt::Display for ParseColorError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "invalid color format")
    }
}

impl std::error::Error for ParseColorError {}

impl FromStr for Color {
    type Err = ParseColorError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        Color::from_hex(s)
    }
}

impl fmt::Display for Color {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.to_hex())
    }
}

/// Clamps a value to the range [0.0, 1.0].
#[inline]
fn clamp(v: f64) -> f64 {
    if v < 0.0 {
        0.0
    } else if v > 1.0 {
        1.0
    } else {
        v
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_color_rgb() {
        let c = Color::rgb(1.0, 0.5, 0.25);
        assert_eq!(c.r, 1.0);
        assert_eq!(c.g, 0.5);
        assert_eq!(c.b, 0.25);
        assert_eq!(c.a, 1.0);
    }

    #[test]
    fn test_color_rgba() {
        let c = Color::rgba(1.0, 0.5, 0.25, 0.8);
        assert_eq!(c.r, 1.0);
        assert_eq!(c.g, 0.5);
        assert_eq!(c.b, 0.25);
        assert_eq!(c.a, 0.8);
    }

    #[test]
    fn test_color_clamping() {
        let c = Color::rgba(2.0, -0.5, 0.5, 1.5);
        assert_eq!(c.r, 1.0);
        assert_eq!(c.g, 0.0);
        assert_eq!(c.b, 0.5);
        assert_eq!(c.a, 1.0);
    }

    #[test]
    fn test_color_gray() {
        let c = Color::gray(0.5);
        assert_eq!(c.r, 0.5);
        assert_eq!(c.g, 0.5);
        assert_eq!(c.b, 0.5);
        assert_eq!(c.a, 1.0);
    }

    #[test]
    fn test_color_constants() {
        assert_eq!(Color::BLACK, Color::rgb(0.0, 0.0, 0.0));
        assert_eq!(Color::WHITE, Color::rgb(1.0, 1.0, 1.0));
    }

    #[test]
    fn test_color_hsb_red() {
        let c = Color::hsb(0.0, 1.0, 1.0);
        assert!((c.r - 1.0).abs() < 0.001);
        assert!(c.g.abs() < 0.001);
        assert!(c.b.abs() < 0.001);
    }

    #[test]
    fn test_color_hsb_green() {
        let c = Color::hsb(1.0 / 3.0, 1.0, 1.0);
        assert!(c.r.abs() < 0.001);
        assert!((c.g - 1.0).abs() < 0.001);
        assert!(c.b.abs() < 0.001);
    }

    #[test]
    fn test_color_hsb_blue() {
        let c = Color::hsb(2.0 / 3.0, 1.0, 1.0);
        assert!(c.r.abs() < 0.001);
        assert!(c.g.abs() < 0.001);
        assert!((c.b - 1.0).abs() < 0.001);
    }

    #[test]
    fn test_color_to_hsb() {
        let c = Color::rgb(1.0, 0.0, 0.0);
        let (h, s, b) = c.to_hsb();
        assert!(h.abs() < 0.001);
        assert!((s - 1.0).abs() < 0.001);
        assert!((b - 1.0).abs() < 0.001);
    }

    #[test]
    fn test_color_from_hex() {
        let c = Color::from_hex("#FF0000").unwrap();
        assert_eq!(c.r, 1.0);
        assert_eq!(c.g, 0.0);
        assert_eq!(c.b, 0.0);

        let c2 = Color::from_hex("#00FF00FF").unwrap();
        assert_eq!(c2.g, 1.0);
        assert_eq!(c2.a, 1.0);

        let c3 = Color::from_hex("F00").unwrap();
        assert_eq!(c3.r, 1.0);
    }

    #[test]
    fn test_color_to_css_opaque() {
        let c = Color::rgb(1.0, 0.0, 0.5);
        let css = c.to_css();
        assert_eq!(css, "#FF0080");
    }

    #[test]
    fn test_color_to_css_transparent() {
        let c = Color::rgba(1.0, 0.0, 0.0, 0.5);
        let css = c.to_css();
        assert_eq!(css, "rgba(255,0,0,0.50)");
    }

    #[test]
    fn test_color_to_css_invisible() {
        let c = Color::rgba(1.0, 0.0, 0.0, 0.0);
        assert_eq!(c.to_css(), "none");
    }

    #[test]
    fn test_color_is_visible() {
        assert!(Color::BLACK.is_visible());
        assert!(Color::WHITE.is_visible());
        assert!(!Color::TRANSPARENT.is_visible());
        assert!(!Color::rgba(1.0, 0.0, 0.0, 0.0).is_visible());
    }

    #[test]
    fn test_color_with_alpha() {
        let c = Color::rgb(1.0, 0.0, 0.0);
        let c2 = c.with_alpha(0.5);
        assert_eq!(c2.r, 1.0);
        assert_eq!(c2.a, 0.5);
    }

    #[test]
    fn test_color_lerp() {
        let c1 = Color::rgb(0.0, 0.0, 0.0);
        let c2 = Color::rgb(1.0, 1.0, 1.0);
        let mid = c1.lerp(c2, 0.5);
        assert!((mid.r - 0.5).abs() < 0.001);
        assert!((mid.g - 0.5).abs() < 0.001);
        assert!((mid.b - 0.5).abs() < 0.001);
    }

    #[test]
    fn test_color_parse() {
        let c: Color = "#FF0000".parse().unwrap();
        assert_eq!(c.r, 1.0);
        assert_eq!(c.g, 0.0);
        assert_eq!(c.b, 0.0);
    }
}
