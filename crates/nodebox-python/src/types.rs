//! Python type wrappers for NodeBox core types.

use pyo3::prelude::*;
use nodebox_core::geometry::{
    Point as CorePoint,
    Color as CoreColor,
    Rect as CoreRect,
};

/// A 2D point with x and y coordinates.
#[pyclass(name = "Point")]
#[derive(Clone, Debug)]
pub struct PyPoint {
    pub inner: CorePoint,
}

#[pymethods]
impl PyPoint {
    /// Create a new point.
    #[new]
    pub fn new(x: f64, y: f64) -> Self {
        Self {
            inner: CorePoint::new(x, y),
        }
    }

    /// The x coordinate.
    #[getter]
    pub fn x(&self) -> f64 {
        self.inner.x
    }

    /// Set the x coordinate.
    #[setter]
    pub fn set_x(&mut self, value: f64) {
        self.inner.x = value;
    }

    /// The y coordinate.
    #[getter]
    pub fn y(&self) -> f64 {
        self.inner.y
    }

    /// Set the y coordinate.
    #[setter]
    pub fn set_y(&mut self, value: f64) {
        self.inner.y = value;
    }

    /// Calculate the distance to another point.
    pub fn distance_to(&self, other: &PyPoint) -> f64 {
        self.inner.distance_to(other.inner)
    }

    /// Return the origin point (0, 0).
    #[staticmethod]
    pub fn origin() -> Self {
        Self {
            inner: CorePoint::ZERO,
        }
    }

    fn __repr__(&self) -> String {
        format!("Point({}, {})", self.inner.x, self.inner.y)
    }

    fn __str__(&self) -> String {
        self.__repr__()
    }
}

impl From<CorePoint> for PyPoint {
    fn from(point: CorePoint) -> Self {
        Self { inner: point }
    }
}

impl From<PyPoint> for CorePoint {
    fn from(point: PyPoint) -> Self {
        point.inner
    }
}

/// An RGBA color.
#[pyclass(name = "Color")]
#[derive(Clone, Debug)]
pub struct PyColor {
    pub inner: CoreColor,
}

#[pymethods]
impl PyColor {
    /// Create a new color from RGBA components (0.0-1.0).
    #[new]
    #[pyo3(signature = (r, g, b, a=None))]
    pub fn new(r: f64, g: f64, b: f64, a: Option<f64>) -> Self {
        Self {
            inner: CoreColor::rgba(r, g, b, a.unwrap_or(1.0)),
        }
    }

    /// Create a color from RGB components.
    #[staticmethod]
    pub fn rgb(r: f64, g: f64, b: f64) -> Self {
        Self {
            inner: CoreColor::rgb(r, g, b),
        }
    }

    /// Create a color from RGBA components.
    #[staticmethod]
    pub fn rgba(r: f64, g: f64, b: f64, a: f64) -> Self {
        Self {
            inner: CoreColor::rgba(r, g, b, a),
        }
    }

    /// Create a color from HSB components.
    #[staticmethod]
    pub fn hsb(h: f64, s: f64, b: f64) -> Self {
        Self {
            inner: CoreColor::hsb(h, s, b),
        }
    }

    /// Create a color from hex string (e.g., "#FF0000" or "FF0000").
    #[staticmethod]
    pub fn from_hex(hex: &str) -> PyResult<Self> {
        CoreColor::from_hex(hex)
            .map(|c| Self { inner: c })
            .map_err(|e| PyErr::new::<pyo3::exceptions::PyValueError, _>(format!("{}", e)))
    }

    /// Red component (0.0-1.0).
    #[getter]
    pub fn r(&self) -> f64 {
        self.inner.r
    }

    #[setter]
    pub fn set_r(&mut self, value: f64) {
        self.inner.r = value;
    }

    /// Green component (0.0-1.0).
    #[getter]
    pub fn g(&self) -> f64 {
        self.inner.g
    }

    #[setter]
    pub fn set_g(&mut self, value: f64) {
        self.inner.g = value;
    }

    /// Blue component (0.0-1.0).
    #[getter]
    pub fn b(&self) -> f64 {
        self.inner.b
    }

    #[setter]
    pub fn set_b(&mut self, value: f64) {
        self.inner.b = value;
    }

    /// Alpha component (0.0-1.0).
    #[getter]
    pub fn a(&self) -> f64 {
        self.inner.a
    }

    #[setter]
    pub fn set_a(&mut self, value: f64) {
        self.inner.a = value;
    }

    /// Black color.
    #[staticmethod]
    pub fn black() -> Self {
        Self {
            inner: CoreColor::BLACK,
        }
    }

    /// White color.
    #[staticmethod]
    pub fn white() -> Self {
        Self {
            inner: CoreColor::WHITE,
        }
    }

    fn __repr__(&self) -> String {
        format!(
            "Color({}, {}, {}, {})",
            self.inner.r, self.inner.g, self.inner.b, self.inner.a
        )
    }
}

impl From<CoreColor> for PyColor {
    fn from(color: CoreColor) -> Self {
        Self { inner: color }
    }
}

impl From<PyColor> for CoreColor {
    fn from(color: PyColor) -> Self {
        color.inner
    }
}

/// A rectangle defined by position and size.
#[pyclass(name = "Rect")]
#[derive(Clone, Debug)]
pub struct PyRect {
    pub inner: CoreRect,
}

#[pymethods]
impl PyRect {
    /// Create a new rectangle.
    #[new]
    pub fn new(x: f64, y: f64, width: f64, height: f64) -> Self {
        Self {
            inner: CoreRect::new(x, y, width, height),
        }
    }

    /// X position.
    #[getter]
    pub fn x(&self) -> f64 {
        self.inner.x
    }

    /// Y position.
    #[getter]
    pub fn y(&self) -> f64 {
        self.inner.y
    }

    /// Width.
    #[getter]
    pub fn width(&self) -> f64 {
        self.inner.width
    }

    /// Height.
    #[getter]
    pub fn height(&self) -> f64 {
        self.inner.height
    }

    /// Center point of the rectangle.
    pub fn center(&self) -> PyPoint {
        PyPoint {
            inner: CorePoint::new(
                self.inner.x + self.inner.width / 2.0,
                self.inner.y + self.inner.height / 2.0,
            ),
        }
    }

    /// Check if a point is inside the rectangle.
    pub fn contains(&self, point: &PyPoint) -> bool {
        self.inner.contains_point(point.inner)
    }

    fn __repr__(&self) -> String {
        format!(
            "Rect({}, {}, {}, {})",
            self.inner.x, self.inner.y, self.inner.width, self.inner.height
        )
    }
}

impl From<CoreRect> for PyRect {
    fn from(rect: CoreRect) -> Self {
        Self { inner: rect }
    }
}
