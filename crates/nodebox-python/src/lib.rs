//! Python bindings for NodeBox.
//!
//! This crate provides Python bindings for NodeBox's core functionality,
//! allowing users to write custom nodes in Python.
//!
//! # Example
//!
//! ```python
//! import nodebox
//!
//! # Create a point
//! p = nodebox.Point(100, 200)
//! print(f"Point: ({p.x}, {p.y})")
//!
//! # Create a color
//! c = nodebox.Color.rgb(1.0, 0.0, 0.0)
//! print(f"Color: rgba({c.r}, {c.g}, {c.b}, {c.a})")
//!
//! # Create geometry
//! circle = nodebox.ellipse(p, 50, 50)
//! ```

use pyo3::prelude::*;

mod types;
mod geometry;
mod operations;

pub use types::*;
pub use geometry::*;
pub use operations::*;

/// The main NodeBox Python module.
#[pymodule]
fn nodebox(m: &Bound<'_, PyModule>) -> PyResult<()> {
    // Register types
    m.add_class::<PyPoint>()?;
    m.add_class::<PyColor>()?;
    m.add_class::<PyRect>()?;
    m.add_class::<PyPath>()?;

    // Register functions
    m.add_function(wrap_pyfunction!(py_ellipse, m)?)?;
    m.add_function(wrap_pyfunction!(py_rect, m)?)?;
    m.add_function(wrap_pyfunction!(py_line, m)?)?;
    m.add_function(wrap_pyfunction!(py_polygon, m)?)?;
    m.add_function(wrap_pyfunction!(py_star, m)?)?;
    m.add_function(wrap_pyfunction!(py_arc, m)?)?;
    m.add_function(wrap_pyfunction!(py_grid, m)?)?;

    // Transform functions
    m.add_function(wrap_pyfunction!(py_translate, m)?)?;
    m.add_function(wrap_pyfunction!(py_rotate, m)?)?;
    m.add_function(wrap_pyfunction!(py_scale, m)?)?;
    m.add_function(wrap_pyfunction!(py_colorize, m)?)?;

    Ok(())
}
