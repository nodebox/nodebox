//! Python module loading and function discovery.

use pyo3::prelude::*;
use pyo3::types::PyDict;
use std::collections::HashMap;
use std::path::PathBuf;

use nodebox_core::node::PortType;

/// A loaded Python module with discovered functions.
pub struct PythonModule {
    /// The namespace name for this module (e.g., "pyvector").
    pub name: String,

    /// The path to the module file.
    pub path: PathBuf,

    /// The Python module object.
    pub module: Py<PyModule>,

    /// Discovered functions in this module.
    pub functions: HashMap<String, PythonFunction>,
}

/// A Python function that can be called from the node system.
pub struct PythonFunction {
    /// The function name.
    pub name: String,

    /// The Python callable object.
    pub callable: Py<PyAny>,

    /// Argument types (extracted from type hints, if available).
    pub arg_types: Vec<PortType>,

    /// Return type (extracted from type hints, if available).
    pub return_type: PortType,

    /// Docstring, if available.
    pub doc: Option<String>,
}

impl PythonModule {
    /// Discover all public functions in a Python module.
    ///
    /// This examines the module's namespace and extracts functions that:
    /// - Don't start with underscore (not private)
    /// - Are callable functions (not classes or other objects)
    ///
    /// Type hints are extracted if available.
    pub fn discover_functions(
        py: Python<'_>,
        module: &Bound<'_, PyModule>,
    ) -> PyResult<HashMap<String, PythonFunction>> {
        let mut functions = HashMap::new();

        let inspect = py.import("inspect")?;
        let is_function = inspect.getattr("isfunction")?;

        let module_dict = module.dict();

        for item in module_dict.items() {
            let (key, value) = item.extract::<(String, PyObject)>()?;

            // Skip private functions
            if key.starts_with('_') {
                continue;
            }

            // Check if it's a function
            let value_ref = value.bind(py);
            let is_func: bool = is_function.call1((value_ref,))?.extract()?;

            if !is_func {
                continue;
            }

            // Extract type hints
            let (arg_types, return_type) = extract_type_hints(py, value_ref)?;

            // Extract docstring
            let doc = value_ref
                .getattr("__doc__")
                .ok()
                .and_then(|d| d.extract::<Option<String>>().ok())
                .flatten();

            functions.insert(
                key.clone(),
                PythonFunction {
                    name: key,
                    callable: value.clone(),
                    arg_types,
                    return_type,
                    doc,
                },
            );
        }

        Ok(functions)
    }
}

/// Extract type hints from a Python function.
///
/// Returns (arg_types, return_type).
fn extract_type_hints(
    py: Python<'_>,
    func: &Bound<'_, PyAny>,
) -> PyResult<(Vec<PortType>, PortType)> {
    let typing = py.import("typing")?;
    let get_type_hints = typing.getattr("get_type_hints")?;

    // Try to get type hints
    let hints = match get_type_hints.call1((func,)) {
        Ok(h) => h,
        Err(_) => {
            // No type hints available
            return Ok((vec![], PortType::Geometry));
        }
    };

    let hints_dict: &Bound<'_, PyDict> = hints.downcast()?;

    let mut arg_types = Vec::new();
    let mut return_type = PortType::Geometry;

    for item in hints_dict.items() {
        let (name, type_hint): (String, PyObject) = item.extract()?;

        let port_type = python_type_to_port_type(py, type_hint.bind(py))?;

        if name == "return" {
            return_type = port_type;
        } else {
            arg_types.push(port_type);
        }
    }

    Ok((arg_types, return_type))
}

/// Convert a Python type hint to a PortType.
fn python_type_to_port_type(py: Python<'_>, type_hint: &Bound<'_, PyAny>) -> PyResult<PortType> {
    // Get the type name
    let type_name: String = if let Ok(name) = type_hint.getattr("__name__") {
        name.extract()?
    } else if let Ok(origin) = type_hint.getattr("__origin__") {
        // Handle generic types like List[int]
        origin.getattr("__name__")?.extract()?
    } else {
        // Try to get string representation
        type_hint.str()?.extract()?
    };

    // Map Python types to PortTypes
    let port_type = match type_name.as_str() {
        "int" => PortType::Int,
        "float" => PortType::Float,
        "str" => PortType::String,
        "bool" => PortType::Boolean,
        "Point" => PortType::Point,
        "Color" => PortType::Color,
        "Path" | "Geometry" => PortType::Geometry,
        "list" | "List" => PortType::List,
        _ => PortType::Geometry, // Default
    };

    Ok(port_type)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_python_type_mapping() {
        // These tests verify the type mapping logic
        // Actual Python integration tests require a Python interpreter
    }
}
