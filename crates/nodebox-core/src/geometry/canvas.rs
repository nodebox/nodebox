//! Canvas - a drawing surface that can contain graphic objects.

use super::{Grob, Geometry, Color, Rect, Point};

/// A canvas is a drawing surface with a size, background color, and items.
///
/// Canvases can be nested, and are the top-level container for visual output.
#[derive(Clone, Debug, PartialEq)]
pub struct Canvas {
    /// Canvas width.
    pub width: f64,
    /// Canvas height.
    pub height: f64,
    /// Background color (None for transparent).
    pub background: Option<Color>,
    /// Offset for centering content.
    pub offset: Point,
    /// The graphic items on this canvas.
    pub items: Vec<Grob>,
}

impl Default for Canvas {
    fn default() -> Self {
        Canvas::new(1000.0, 1000.0)
    }
}

impl Canvas {
    /// Creates a new canvas with the given dimensions.
    pub fn new(width: f64, height: f64) -> Self {
        Canvas {
            width,
            height,
            background: Some(Color::WHITE),
            offset: Point::ZERO,
            items: Vec::new(),
        }
    }

    /// Creates a canvas with a specific background color.
    pub fn with_background(width: f64, height: f64, background: Color) -> Self {
        Canvas {
            width,
            height,
            background: Some(background),
            offset: Point::ZERO,
            items: Vec::new(),
        }
    }

    /// Adds a graphic item to this canvas.
    pub fn add(&mut self, item: impl Into<Grob>) {
        self.items.push(item.into());
    }

    /// Adds a geometry to this canvas.
    pub fn add_geometry(&mut self, geo: Geometry) {
        self.items.push(Grob::Geometry(geo));
    }

    /// Returns the bounds of this canvas.
    pub fn bounds(&self) -> Rect {
        Rect::new(0.0, 0.0, self.width, self.height)
    }

    /// Returns the center point of this canvas.
    pub fn center(&self) -> Point {
        Point::new(self.width / 2.0, self.height / 2.0)
    }

    /// Converts all items to a single Geometry.
    pub fn to_geometry(&self) -> Geometry {
        let mut geo = Geometry::new();
        for item in &self.items {
            geo.extend(item.to_geometry());
        }
        geo
    }

    /// Returns true if this canvas has no items.
    #[inline]
    pub fn is_empty(&self) -> bool {
        self.items.is_empty()
    }

    /// Returns the content bounds (bounds of all items).
    ///
    /// Returns `None` if there are no items.
    pub fn content_bounds(&self) -> Option<Rect> {
        let mut result: Option<Rect> = None;

        for item in &self.items {
            if let Some(bounds) = item.bounds() {
                result = Some(match result {
                    Some(r) => r.union(&bounds),
                    None => bounds,
                });
            }
        }

        result
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::geometry::Path;

    #[test]
    fn test_canvas_new() {
        let canvas = Canvas::new(800.0, 600.0);
        assert_eq!(canvas.width, 800.0);
        assert_eq!(canvas.height, 600.0);
        assert!(canvas.background.is_some());
        assert!(canvas.is_empty());
    }

    #[test]
    fn test_canvas_add() {
        let mut canvas = Canvas::new(800.0, 600.0);
        canvas.add(Path::rect(0.0, 0.0, 100.0, 100.0));
        assert!(!canvas.is_empty());
    }

    #[test]
    fn test_canvas_bounds() {
        let canvas = Canvas::new(800.0, 600.0);
        let bounds = canvas.bounds();
        assert_eq!(bounds.x, 0.0);
        assert_eq!(bounds.y, 0.0);
        assert_eq!(bounds.width, 800.0);
        assert_eq!(bounds.height, 600.0);
    }

    #[test]
    fn test_canvas_center() {
        let canvas = Canvas::new(800.0, 600.0);
        let center = canvas.center();
        assert_eq!(center, Point::new(400.0, 300.0));
    }

    #[test]
    fn test_canvas_to_geometry() {
        let mut canvas = Canvas::new(800.0, 600.0);
        canvas.add(Path::rect(0.0, 0.0, 100.0, 100.0));
        canvas.add(Path::rect(200.0, 200.0, 100.0, 100.0));

        let geo = canvas.to_geometry();
        assert_eq!(geo.len(), 2);
    }

    #[test]
    fn test_canvas_content_bounds() {
        let mut canvas = Canvas::new(800.0, 600.0);
        canvas.add(Path::rect(10.0, 20.0, 100.0, 100.0));
        canvas.add(Path::rect(200.0, 50.0, 100.0, 100.0));

        let bounds = canvas.content_bounds().unwrap();
        assert_eq!(bounds.x, 10.0);
        assert_eq!(bounds.y, 20.0);
        assert_eq!(bounds.max_x(), 300.0);
        assert_eq!(bounds.max_y(), 150.0);
    }
}
