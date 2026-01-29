//! Python bindings for NodeBox.
//!
//! This crate provides Python bindings for NodeBox's core functionality,
//! allowing users to write custom nodes in Python.
//!
//! # Features
//!
//! - **Type Bindings**: Point, Color, Rect, Path types exposed to Python
//! - **Generator Functions**: ellipse, rect, line, polygon, star, arc, grid
//! - **Transform Functions**: translate, rotate, scale, colorize
//! - **Module Loading**: Load Python modules and call their functions from Rust
//! - **Function Registry Integration**: Register Python functions with the node system
//!
//! # Example: Using from Python
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
//!
//! # Example: Loading Python Modules from Rust
//!
//! ```rust,ignore
//! use nodebox_python::{PythonRuntime, PythonBridge};
//! use std::sync::{Arc, Mutex};
//! use std::path::Path;
//!
//! // Create a Python runtime
//! let runtime = Arc::new(Mutex::new(PythonRuntime::new().unwrap()));
//!
//! // Load a Python module
//! {
//!     let mut rt = runtime.lock().unwrap();
//!     rt.load_module(Path::new("pyvector.py"), "pyvector").unwrap();
//! }
//!
//! // Create a bridge for function registration
//! let bridge = PythonBridge::new(runtime);
//! ```

use pyo3::prelude::*;

// Core type bindings
mod types;
mod geometry;
mod operations;

// Python module loading and runtime
pub mod runtime;
pub mod module;
pub mod convert;
pub mod bridge;

// Re-export public types
pub use types::*;
pub use geometry::*;
pub use operations::*;
pub use runtime::PythonRuntime;
pub use module::{PythonModule, PythonFunction};
pub use bridge::PythonBridge;

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
