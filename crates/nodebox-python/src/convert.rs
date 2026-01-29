//! Value conversion between Rust and Python.

use pyo3::prelude::*;
use pyo3::types::{PyDict, PyList, PyTuple};

use nodebox_core::geometry::{
    Color as CoreColor,
    Contour as CoreContour,
    Geometry as CoreGeometry,
    Path as CorePath,
    PathPoint as CorePathPoint,
    Point as CorePoint,
    PointType as CorePointType,
};
use nodebox_core::value::Value;

use crate::types::{PyColor, PyPoint};
use crate::geometry::PyPath;

/// Convert a Rust Value to a Python object.
pub fn value_to_python(py: Python<'_>, value: &Value) -> PyResult<PyObject> {
    match value {
        Value::Null => Ok(py.None()),
        Value::Int(i) => Ok(i.into_pyobject(py)?.into_any().unbind()),
        Value::Float(f) => Ok(f.into_pyobject(py)?.into_any().unbind()),
        Value::String(s) => Ok(s.into_pyobject(py)?.into_any().unbind()),
        Value::Boolean(b) => Ok(b.into_pyobject(py)?.into_any().unbind()),
        Value::Point(p) => {
            let py_point = PyPoint::new(p.x, p.y);
            Ok(py_point.into_pyobject(py)?.into_any().unbind())
        }
        Value::Color(c) => {
            let py_color = PyColor::new(c.r, c.g, c.b, Some(c.a));
            Ok(py_color.into_pyobject(py)?.into_any().unbind())
        }
        Value::Path(path) => {
            let py_path = PyPath::from(path.clone());
            Ok(py_path.into_pyobject(py)?.into_any().unbind())
        }
        Value::Geometry(geo) => {
            // Convert geometry to a list of paths
            let paths: Vec<PyObject> = geo
                .paths
                .iter()
                .map(|p| {
                    let py_path = PyPath::from(p.clone());
                    py_path.into_pyobject(py).map(|obj| obj.into_any().unbind())
                })
                .collect::<PyResult<_>>()?;
            Ok(PyList::new(py, &paths)?.into_any().unbind())
        }
        Value::List(items) => {
            let py_items: Vec<PyObject> = items
                .iter()
                .map(|v| value_to_python(py, v))
                .collect::<PyResult<_>>()?;
            Ok(PyList::new(py, &py_items)?.into_any().unbind())
        }
        Value::Map(map) => {
            let dict = PyDict::new(py);
            for (key, val) in map {
                dict.set_item(key, value_to_python(py, val)?)?;
            }
            Ok(dict.into_any().unbind())
        }
    }
}

/// Convert a Python object to a Rust Value.
pub fn python_to_value(py: Python<'_>, obj: Bound<'_, PyAny>) -> PyResult<Value> {
    // Check for None
    if obj.is_none() {
        return Ok(Value::Null);
    }

    // Check for boolean (must be before int, since bool is a subclass of int)
    if let Ok(b) = obj.extract::<bool>() {
        return Ok(Value::Boolean(b));
    }

    // Check for int
    if let Ok(i) = obj.extract::<i64>() {
        return Ok(Value::Int(i));
    }

    // Check for float
    if let Ok(f) = obj.extract::<f64>() {
        return Ok(Value::Float(f));
    }

    // Check for string
    if let Ok(s) = obj.extract::<String>() {
        return Ok(Value::String(s));
    }

    // Check for our custom types
    if let Ok(point) = obj.extract::<PyPoint>() {
        return Ok(Value::Point(point.inner));
    }

    if let Ok(color) = obj.extract::<PyColor>() {
        return Ok(Value::Color(color.inner));
    }

    if let Ok(path) = obj.extract::<PyPath>() {
        return Ok(Value::Path(path.inner));
    }

    // Check for list
    if let Ok(list) = obj.downcast::<PyList>() {
        let items: Vec<Value> = list
            .iter()
            .map(|item| python_to_value(py, item))
            .collect::<PyResult<_>>()?;
        return Ok(Value::List(items));
    }

    // Check for tuple (treat as list)
    if let Ok(tuple) = obj.downcast::<PyTuple>() {
        let items: Vec<Value> = tuple
            .iter()
            .map(|item| python_to_value(py, item))
            .collect::<PyResult<_>>()?;
        return Ok(Value::List(items));
    }

    // Check for dict
    if let Ok(dict) = obj.downcast::<PyDict>() {
        let mut map = std::collections::HashMap::new();
        for (key, val) in dict.iter() {
            let key_str: String = key.extract()?;
            let value = python_to_value(py, val)?;
            map.insert(key_str, value);
        }
        return Ok(Value::Map(map));
    }

    // Try to get geometry-like objects by duck typing
    // Check if object has 'x' and 'y' attributes (Point-like)
    if obj.hasattr("x")? && obj.hasattr("y")? {
        let x: f64 = obj.getattr("x")?.extract()?;
        let y: f64 = obj.getattr("y")?.extract()?;
        return Ok(Value::Point(CorePoint::new(x, y)));
    }

    // Check if object has 'r', 'g', 'b' attributes (Color-like)
    if obj.hasattr("r")? && obj.hasattr("g")? && obj.hasattr("b")? {
        let r: f64 = obj.getattr("r")?.extract()?;
        let g: f64 = obj.getattr("g")?.extract()?;
        let b: f64 = obj.getattr("b")?.extract()?;
        let a: f64 = obj.getattr("a").map(|a| a.extract().unwrap_or(1.0)).unwrap_or(1.0);
        return Ok(Value::Color(CoreColor::rgba(r, g, b, a)));
    }

    // Check if object has 'contours' attribute (Path-like)
    if obj.hasattr("contours")? {
        let path = convert_path_like(py, &obj)?;
        return Ok(Value::Path(path));
    }

    // Check if object has 'paths' attribute (Geometry-like)
    if obj.hasattr("paths")? {
        let paths_attr = obj.getattr("paths")?;
        let paths_list = paths_attr.downcast::<PyList>()?;
        let paths: Vec<CorePath> = paths_list
            .iter()
            .map(|p| convert_path_like(py, &p))
            .collect::<PyResult<_>>()?;
        return Ok(Value::Geometry(CoreGeometry { paths }));
    }

    // Unknown type - try to convert to string representation
    let type_name = obj.get_type().name()?;
    Err(PyErr::new::<pyo3::exceptions::PyTypeError, _>(format!(
        "Cannot convert Python type '{}' to Value",
        type_name
    )))
}

/// Convert a Path-like Python object to CorePath.
fn convert_path_like(py: Python<'_>, obj: &Bound<'_, PyAny>) -> PyResult<CorePath> {
    // If it's already a PyPath, extract it directly
    if let Ok(py_path) = obj.extract::<PyPath>() {
        return Ok(py_path.inner);
    }

    // Otherwise, try to extract path data from attributes
    let contours_attr = obj.getattr("contours")?;
    let contours_list = contours_attr.downcast::<PyList>()?;

    let mut contours = Vec::new();
    for contour_obj in contours_list.iter() {
        let points_attr = contour_obj.getattr("points")?;
        let points_list = points_attr.downcast::<PyList>()?;
        let closed: bool = contour_obj.getattr("closed")?.extract()?;

        let mut points = Vec::new();
        for point_obj in points_list.iter() {
            let x: f64 = point_obj.getattr("x")?.extract()?;
            let y: f64 = point_obj.getattr("y")?.extract()?;
            let point_type_str: String = point_obj
                .getattr("point_type")
                .map(|pt| pt.extract().unwrap_or("lineto".to_string()))
                .unwrap_or_else(|_| "lineto".to_string());

            let point_type = match point_type_str.to_lowercase().as_str() {
                "curveto" | "curve_to" => CorePointType::CurveTo,
                "curvedata" | "curve_data" => CorePointType::CurveData,
                _ => CorePointType::LineTo,
            };

            points.push(CorePathPoint {
                point: CorePoint::new(x, y),
                point_type,
            });
        }

        contours.push(CoreContour { points, closed });
    }

    // Extract optional fill/stroke
    let fill = extract_optional_color(obj, "fill")?;
    let stroke = extract_optional_color(obj, "stroke")?;
    let stroke_width: f64 = obj
        .getattr("stroke_width")
        .map(|sw| sw.extract().unwrap_or(1.0))
        .unwrap_or(1.0);

    Ok(CorePath {
        contours,
        fill,
        stroke,
        stroke_width,
    })
}

/// Extract an optional Color attribute from a Python object.
fn extract_optional_color(obj: &Bound<'_, PyAny>, attr: &str) -> PyResult<Option<CoreColor>> {
    match obj.getattr(attr) {
        Ok(color_obj) => {
            if color_obj.is_none() {
                Ok(None)
            } else if let Ok(py_color) = color_obj.extract::<PyColor>() {
                Ok(Some(py_color.inner))
            } else if color_obj.hasattr("r")? && color_obj.hasattr("g")? && color_obj.hasattr("b")? {
                let r: f64 = color_obj.getattr("r")?.extract()?;
                let g: f64 = color_obj.getattr("g")?.extract()?;
                let b: f64 = color_obj.getattr("b")?.extract()?;
                let a: f64 = color_obj
                    .getattr("a")
                    .map(|a| a.extract().unwrap_or(1.0))
                    .unwrap_or(1.0);
                Ok(Some(CoreColor::rgba(r, g, b, a)))
            } else {
                Ok(None)
            }
        }
        Err(_) => Ok(None),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    // Note: These tests require a Python interpreter to run.
    // They are kept here for documentation and future testing.

    #[test]
    fn test_value_roundtrip_primitives() {
        // Test that primitive values roundtrip correctly
        // This requires Python to be available
    }
}
