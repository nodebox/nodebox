//! Geometry - a collection of paths, and Grob - a graphic object enum.

use super::{Path, Transform, Rect, Color};

/// A Geometry is a collection of paths.
///
/// This is the primary container for visual output in NodeBox. Most operations
/// produce a Geometry containing one or more paths.
///
/// # Examples
///
/// ```
/// use nodebox_core::geometry::{Geometry, Path};
///
/// let mut geo = Geometry::new();
/// geo.add(Path::rect(0.0, 0.0, 100.0, 100.0));
/// geo.add(Path::ellipse(150.0, 50.0, 80.0, 80.0));
/// ```
#[derive(Clone, Debug, Default, PartialEq)]
pub struct Geometry {
    /// The paths in this geometry.
    pub paths: Vec<Path>,
}

impl Geometry {
    /// Creates a new empty geometry.
    pub fn new() -> Self {
        Geometry { paths: Vec::new() }
    }

    /// Creates a geometry from a single path.
    pub fn from_path(path: Path) -> Self {
        Geometry { paths: vec![path] }
    }

    /// Creates a geometry from multiple paths.
    pub fn from_paths(paths: Vec<Path>) -> Self {
        Geometry { paths }
    }

    /// Returns true if this geometry has no paths.
    #[inline]
    pub fn is_empty(&self) -> bool {
        self.paths.is_empty()
    }

    /// Returns the number of paths in this geometry.
    #[inline]
    pub fn len(&self) -> usize {
        self.paths.len()
    }

    /// Adds a path to this geometry.
    pub fn add(&mut self, path: Path) {
        self.paths.push(path);
    }

    /// Extends this geometry with paths from another geometry.
    pub fn extend(&mut self, other: Geometry) {
        self.paths.extend(other.paths);
    }

    /// Returns the bounding box of all paths.
    ///
    /// Returns `None` if the geometry is empty.
    pub fn bounds(&self) -> Option<Rect> {
        let mut result: Option<Rect> = None;

        for path in &self.paths {
            if let Some(bounds) = path.bounds() {
                result = Some(match result {
                    Some(r) => r.union(&bounds),
                    None => bounds,
                });
            }
        }

        result
    }

    /// Returns a new geometry transformed by the given transform.
    pub fn transform(&self, t: &Transform) -> Geometry {
        Geometry {
            paths: self.paths.iter().map(|p| p.transform(t)).collect(),
        }
    }

    /// Returns a new geometry with all paths colorized with the given fill.
    pub fn colorize(&self, fill: Option<Color>, stroke: Option<Color>, stroke_width: Option<f64>) -> Geometry {
        Geometry {
            paths: self.paths.iter().map(|p| {
                let mut path = p.clone();
                if fill.is_some() || stroke.is_none() {
                    path.fill = fill;
                }
                if stroke.is_some() {
                    path.stroke = stroke;
                }
                if let Some(width) = stroke_width {
                    path.stroke_width = width;
                }
                path
            }).collect(),
        }
    }

    /// Returns the total number of points across all paths.
    pub fn point_count(&self) -> usize {
        self.paths.iter().map(|p| p.point_count()).sum()
    }
}

impl From<Path> for Geometry {
    fn from(path: Path) -> Self {
        Geometry::from_path(path)
    }
}

impl FromIterator<Path> for Geometry {
    fn from_iter<I: IntoIterator<Item = Path>>(iter: I) -> Self {
        Geometry {
            paths: iter.into_iter().collect(),
        }
    }
}

/// A graphic object - can be a Path, Geometry, Text, or nested Canvas.
///
/// This enum allows heterogeneous collections of visual elements.
#[derive(Clone, Debug, PartialEq)]
pub enum Grob {
    /// A single path.
    Path(Path),
    /// A collection of paths.
    Geometry(Geometry),
    /// Text element.
    Text(super::Text),
    /// A nested canvas.
    Canvas(Box<super::Canvas>),
}

impl Grob {
    /// Returns the bounding box of this graphic object.
    pub fn bounds(&self) -> Option<Rect> {
        match self {
            Grob::Path(p) => p.bounds(),
            Grob::Geometry(g) => g.bounds(),
            Grob::Text(t) => t.bounds(),
            Grob::Canvas(c) => Some(c.bounds()),
        }
    }

    /// Converts this Grob to a Geometry.
    pub fn to_geometry(&self) -> Geometry {
        match self {
            Grob::Path(p) => Geometry::from_path(p.clone()),
            Grob::Geometry(g) => g.clone(),
            Grob::Text(_) => Geometry::new(), // Text conversion not yet implemented
            Grob::Canvas(c) => c.to_geometry(),
        }
    }
}

impl From<Path> for Grob {
    fn from(path: Path) -> Self {
        Grob::Path(path)
    }
}

impl From<Geometry> for Grob {
    fn from(geo: Geometry) -> Self {
        Grob::Geometry(geo)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_geometry_new() {
        let geo = Geometry::new();
        assert!(geo.is_empty());
        assert_eq!(geo.len(), 0);
    }

    #[test]
    fn test_geometry_add() {
        let mut geo = Geometry::new();
        geo.add(Path::rect(0.0, 0.0, 100.0, 100.0));
        geo.add(Path::ellipse(150.0, 50.0, 80.0, 80.0));

        assert_eq!(geo.len(), 2);
    }

    #[test]
    fn test_geometry_bounds() {
        let mut geo = Geometry::new();
        geo.add(Path::rect(0.0, 0.0, 100.0, 100.0));
        geo.add(Path::rect(50.0, 50.0, 100.0, 100.0));

        let bounds = geo.bounds().unwrap();
        assert_eq!(bounds.x, 0.0);
        assert_eq!(bounds.y, 0.0);
        assert_eq!(bounds.width, 150.0);
        assert_eq!(bounds.height, 150.0);
    }

    #[test]
    fn test_geometry_transform() {
        let mut geo = Geometry::new();
        geo.add(Path::rect(0.0, 0.0, 100.0, 100.0));

        let t = Transform::translate(10.0, 20.0);
        let transformed = geo.transform(&t);

        let bounds = transformed.bounds().unwrap();
        assert_eq!(bounds.x, 10.0);
        assert_eq!(bounds.y, 20.0);
    }

    #[test]
    fn test_geometry_colorize() {
        let mut geo = Geometry::new();
        geo.add(Path::rect(0.0, 0.0, 100.0, 100.0));

        let red = Color::rgb(1.0, 0.0, 0.0);
        let colorized = geo.colorize(Some(red), None, None);

        assert_eq!(colorized.paths[0].fill, Some(red));
    }

    #[test]
    fn test_geometry_from_path() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let geo = Geometry::from_path(path.clone());

        assert_eq!(geo.len(), 1);
        assert_eq!(geo.paths[0], path);
    }

    #[test]
    fn test_geometry_from_iterator() {
        let paths = vec![
            Path::rect(0.0, 0.0, 100.0, 100.0),
            Path::rect(50.0, 50.0, 100.0, 100.0),
        ];
        let geo: Geometry = paths.into_iter().collect();

        assert_eq!(geo.len(), 2);
    }

    #[test]
    fn test_grob_path() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let grob = Grob::from(path.clone());

        if let Grob::Path(p) = grob {
            assert_eq!(p, path);
        } else {
            panic!("Expected Grob::Path");
        }
    }

    #[test]
    fn test_grob_to_geometry() {
        let path = Path::rect(0.0, 0.0, 100.0, 100.0);
        let grob = Grob::from(path);
        let geo = grob.to_geometry();

        assert_eq!(geo.len(), 1);
    }
}
