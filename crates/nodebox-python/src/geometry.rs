//! Python bindings for geometry types.

use pyo3::prelude::*;
use nodebox_core::geometry::{
    Path as CorePath,
    Contour as CoreContour,
    PathPoint as CorePathPoint,
    PointType as CorePointType,
    Point as CorePoint,
};
use crate::types::{PyColor, PyRect};

/// A geometric path consisting of one or more contours.
#[pyclass(name = "Path")]
#[derive(Clone, Debug)]
pub struct PyPath {
    pub inner: CorePath,
}

#[pymethods]
impl PyPath {
    /// Create a new empty path.
    #[new]
    pub fn new() -> Self {
        Self {
            inner: CorePath::new(),
        }
    }

    /// Get the fill color.
    #[getter]
    pub fn fill(&self) -> Option<PyColor> {
        self.inner.fill.map(PyColor::from)
    }

    /// Set the fill color.
    #[setter]
    pub fn set_fill(&mut self, color: Option<PyColor>) {
        self.inner.fill = color.map(|c| c.inner);
    }

    /// Get the stroke color.
    #[getter]
    pub fn stroke(&self) -> Option<PyColor> {
        self.inner.stroke.map(PyColor::from)
    }

    /// Set the stroke color.
    #[setter]
    pub fn set_stroke(&mut self, color: Option<PyColor>) {
        self.inner.stroke = color.map(|c| c.inner);
    }

    /// Get the stroke width.
    #[getter]
    pub fn stroke_width(&self) -> f64 {
        self.inner.stroke_width
    }

    /// Set the stroke width.
    #[setter]
    pub fn set_stroke_width(&mut self, width: f64) {
        self.inner.stroke_width = width;
    }

    /// Get the bounding rectangle.
    pub fn bounds(&self) -> Option<PyRect> {
        self.inner.bounds().map(PyRect::from)
    }

    /// Get the number of contours.
    pub fn contour_count(&self) -> usize {
        self.inner.contours.len()
    }

    /// Get the total number of points.
    pub fn point_count(&self) -> usize {
        self.inner.contours.iter().map(|c| c.points.len()).sum()
    }

    /// Get all points as a list of (x, y) tuples.
    pub fn points(&self) -> Vec<(f64, f64)> {
        let mut points = Vec::new();
        for contour in &self.inner.contours {
            for pp in &contour.points {
                points.push((pp.point.x, pp.point.y));
            }
        }
        points
    }

    /// Move to a point (start a new contour).
    pub fn moveto(&mut self, x: f64, y: f64) {
        let contour = CoreContour {
            points: vec![CorePathPoint {
                point: CorePoint::new(x, y),
                point_type: CorePointType::LineTo,
            }],
            closed: false,
        };
        self.inner.contours.push(contour);
    }

    /// Draw a line to a point.
    pub fn lineto(&mut self, x: f64, y: f64) {
        if let Some(contour) = self.inner.contours.last_mut() {
            contour.points.push(CorePathPoint {
                point: CorePoint::new(x, y),
                point_type: CorePointType::LineTo,
            });
        } else {
            self.moveto(x, y);
        }
    }

    /// Draw a cubic bezier curve.
    pub fn curveto(&mut self, x1: f64, y1: f64, x2: f64, y2: f64, x3: f64, y3: f64) {
        if let Some(contour) = self.inner.contours.last_mut() {
            contour.points.push(CorePathPoint {
                point: CorePoint::new(x1, y1),
                point_type: CorePointType::CurveTo,
            });
            contour.points.push(CorePathPoint {
                point: CorePoint::new(x2, y2),
                point_type: CorePointType::CurveData,
            });
            contour.points.push(CorePathPoint {
                point: CorePoint::new(x3, y3),
                point_type: CorePointType::CurveData,
            });
        }
    }

    /// Close the current contour.
    pub fn close(&mut self) {
        if let Some(contour) = self.inner.contours.last_mut() {
            contour.closed = true;
        }
    }

    /// Create a copy of this path.
    pub fn copy(&self) -> Self {
        Self {
            inner: self.inner.clone(),
        }
    }

    fn __repr__(&self) -> String {
        format!(
            "Path(contours={}, points={})",
            self.contour_count(),
            self.point_count()
        )
    }
}

impl Default for PyPath {
    fn default() -> Self {
        Self::new()
    }
}

impl From<CorePath> for PyPath {
    fn from(path: CorePath) -> Self {
        Self { inner: path }
    }
}

impl From<PyPath> for CorePath {
    fn from(path: PyPath) -> Self {
        path.inner
    }
}
