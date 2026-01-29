//! Python bindings for NodeBox operations.

use pyo3::prelude::*;
use nodebox_core::geometry::{Point as CorePoint, Color as CoreColor};
use nodebox_ops;
use crate::types::{PyPoint, PyColor};
use crate::geometry::PyPath;

// === Generators ===

/// Create an ellipse.
#[pyfunction]
#[pyo3(name = "ellipse", signature = (position=None, width=None, height=None))]
pub fn py_ellipse(
    position: Option<&PyPoint>,
    width: Option<f64>,
    height: Option<f64>,
) -> PyPath {
    let pos = position.map(|p| p.inner).unwrap_or(CorePoint::ZERO);
    let w = width.unwrap_or(100.0);
    let h = height.unwrap_or(w);
    PyPath {
        inner: nodebox_ops::ellipse(pos, w, h),
    }
}

/// Create a rectangle.
#[pyfunction]
#[pyo3(name = "rect", signature = (position=None, width=None, height=None, roundness=None))]
pub fn py_rect(
    position: Option<&PyPoint>,
    width: Option<f64>,
    height: Option<f64>,
    roundness: Option<f64>,
) -> PyPath {
    let pos = position.map(|p| p.inner).unwrap_or(CorePoint::ZERO);
    let w = width.unwrap_or(100.0);
    let h = height.unwrap_or(w);
    let r = roundness.unwrap_or(0.0);
    PyPath {
        inner: nodebox_ops::rect(pos, w, h, CorePoint::new(r, r)),
    }
}

/// Create a line between two points.
#[pyfunction]
#[pyo3(name = "line", signature = (point1=None, point2=None, points=None))]
pub fn py_line(
    point1: Option<&PyPoint>,
    point2: Option<&PyPoint>,
    points: Option<u32>,
) -> PyPath {
    let p1 = point1.map(|p| p.inner).unwrap_or(CorePoint::ZERO);
    let p2 = point2.map(|p| p.inner).unwrap_or(CorePoint::new(100.0, 100.0));
    let pts = points.unwrap_or(2);
    PyPath {
        inner: nodebox_ops::line(p1, p2, pts),
    }
}

/// Create a regular polygon.
#[pyfunction]
#[pyo3(name = "polygon", signature = (position=None, radius=None, sides=None))]
pub fn py_polygon(
    position: Option<&PyPoint>,
    radius: Option<f64>,
    sides: Option<u32>,
) -> PyPath {
    let pos = position.map(|p| p.inner).unwrap_or(CorePoint::ZERO);
    let r = radius.unwrap_or(50.0);
    let s = sides.unwrap_or(6);
    PyPath {
        inner: nodebox_ops::polygon(pos, r, s, true),
    }
}

/// Create a star shape.
#[pyfunction]
#[pyo3(name = "star", signature = (position=None, points=None, outer_radius=None, inner_radius=None))]
pub fn py_star(
    position: Option<&PyPoint>,
    points: Option<u32>,
    outer_radius: Option<f64>,
    inner_radius: Option<f64>,
) -> PyPath {
    let pos = position.map(|p| p.inner).unwrap_or(CorePoint::ZERO);
    let pts = points.unwrap_or(5);
    let outer = outer_radius.unwrap_or(50.0);
    let inner = inner_radius.unwrap_or(25.0);
    PyPath {
        inner: nodebox_ops::star(pos, pts, outer, inner),
    }
}

/// Create an arc or pie slice.
#[pyfunction]
#[pyo3(name = "arc", signature = (position=None, width=None, height=None, start_angle=None, degrees=None, arc_type=None))]
pub fn py_arc(
    position: Option<&PyPoint>,
    width: Option<f64>,
    height: Option<f64>,
    start_angle: Option<f64>,
    degrees: Option<f64>,
    arc_type: Option<&str>,
) -> PyPath {
    let pos = position.map(|p| p.inner).unwrap_or(CorePoint::ZERO);
    let w = width.unwrap_or(100.0);
    let h = height.unwrap_or(w);
    let start = start_angle.unwrap_or(0.0);
    let deg = degrees.unwrap_or(360.0);
    let t = arc_type.unwrap_or("pie");
    PyPath {
        inner: nodebox_ops::arc(pos, w, h, start, deg, t),
    }
}

/// Create a grid of points.
#[pyfunction]
#[pyo3(name = "grid", signature = (rows=None, columns=None, width=None, height=None, position=None))]
pub fn py_grid(
    rows: Option<u32>,
    columns: Option<u32>,
    width: Option<f64>,
    height: Option<f64>,
    position: Option<&PyPoint>,
) -> Vec<PyPoint> {
    let r = rows.unwrap_or(5);
    let c = columns.unwrap_or(5);
    let w = width.unwrap_or(200.0);
    let h = height.unwrap_or(200.0);
    let pos = position.map(|p| p.inner).unwrap_or(CorePoint::ZERO);

    nodebox_ops::grid(c, r, w, h, pos)
        .into_iter()
        .map(PyPoint::from)
        .collect()
}

// === Transforms ===

/// Translate a path by an offset.
#[pyfunction]
#[pyo3(name = "translate", signature = (path, tx=None, ty=None))]
pub fn py_translate(
    path: &PyPath,
    tx: Option<f64>,
    ty: Option<f64>,
) -> PyPath {
    let offset = CorePoint::new(tx.unwrap_or(0.0), ty.unwrap_or(0.0));
    PyPath {
        inner: nodebox_ops::translate(&path.inner, offset),
    }
}

/// Rotate a path around a point.
#[pyfunction]
#[pyo3(name = "rotate", signature = (path, angle, origin=None))]
pub fn py_rotate(
    path: &PyPath,
    angle: f64,
    origin: Option<&PyPoint>,
) -> PyPath {
    let o = origin.map(|p| p.inner).unwrap_or(CorePoint::ZERO);
    PyPath {
        inner: nodebox_ops::rotate(&path.inner, angle, o),
    }
}

/// Scale a path.
#[pyfunction]
#[pyo3(name = "scale", signature = (path, sx=None, sy=None, origin=None))]
pub fn py_scale(
    path: &PyPath,
    sx: Option<f64>,
    sy: Option<f64>,
    origin: Option<&PyPoint>,
) -> PyPath {
    let scale_x = sx.unwrap_or(100.0);
    let scale_y = sy.unwrap_or(scale_x);
    let o = origin.map(|p| p.inner).unwrap_or(CorePoint::ZERO);
    PyPath {
        inner: nodebox_ops::scale(&path.inner, CorePoint::new(scale_x, scale_y), o),
    }
}

/// Set fill and stroke colors on a path.
#[pyfunction]
#[pyo3(name = "colorize", signature = (path, fill=None, stroke=None, stroke_width=None))]
pub fn py_colorize(
    path: &PyPath,
    fill: Option<&PyColor>,
    stroke: Option<&PyColor>,
    stroke_width: Option<f64>,
) -> PyPath {
    let fill_color = fill.map(|c| c.inner).unwrap_or(CoreColor::BLACK);
    let stroke_color = stroke.map(|c| c.inner).unwrap_or(CoreColor::BLACK);
    let sw = stroke_width.unwrap_or(1.0);
    PyPath {
        inner: nodebox_ops::colorize(&path.inner, fill_color, stroke_color, sw),
    }
}
