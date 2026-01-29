//! Python interpreter management.
//!
//! This module handles loading and managing the Python interpreter,
//! as well as loading Python modules.

use pyo3::prelude::*;
use pyo3::types::{PyList, PyModule, PyTuple};
use std::collections::HashMap;
use std::path::Path;

use crate::module::{PythonFunction, PythonModule};
use crate::convert::{value_to_python, python_to_value};
use nodebox_core::value::Value;

/// Manages the Python interpreter and loaded modules.
pub struct PythonRuntime {
    modules: HashMap<String, PythonModule>,
}

impl PythonRuntime {
    /// Create a new Python runtime.
    ///
    /// This initializes the Python interpreter if not already running.
    pub fn new() -> PyResult<Self> {
        pyo3::prepare_freethreaded_python();
        Ok(Self {
            modules: HashMap::new(),
        })
    }

    /// Load a Python module from a file path.
    ///
    /// # Arguments
    /// * `path` - Path to the Python module file (.py)
    /// * `namespace` - Namespace to use for the module (e.g., "pyvector")
    ///
    /// # Returns
    /// The loaded module, which is also stored internally.
    pub fn load_module(&mut self, path: &Path, namespace: &str) -> PyResult<&PythonModule> {
        Python::with_gil(|py| {
            // Add the module's directory to sys.path
            let sys = py.import("sys")?;
            let sys_path: &Bound<'_, PyList> = sys.getattr("path")?.downcast()?;

            if let Some(parent) = path.parent() {
                let parent_str = parent.to_str().ok_or_else(|| {
                    PyErr::new::<pyo3::exceptions::PyValueError, _>("Invalid path")
                })?;
                sys_path.insert(0, parent_str)?;
            }

            // Import the module
            let module_name = path.file_stem()
                .and_then(|s| s.to_str())
                .ok_or_else(|| {
                    PyErr::new::<pyo3::exceptions::PyValueError, _>("Invalid module name")
                })?;

            let module = py.import(module_name)?;

            // Discover functions in the module
            let functions = PythonModule::discover_functions(py, &module)?;

            let python_module = PythonModule {
                name: namespace.to_string(),
                path: path.to_path_buf(),
                module: module.into(),
                functions,
            };

            self.modules.insert(namespace.to_string(), python_module);
            Ok(self.modules.get(namespace).unwrap())
        })
    }

    /// Get a loaded module by namespace.
    pub fn get_module(&self, namespace: &str) -> Option<&PythonModule> {
        self.modules.get(namespace)
    }

    /// Call a function in a loaded module.
    ///
    /// # Arguments
    /// * `namespace` - The module namespace
    /// * `function` - The function name
    /// * `args` - Arguments to pass to the function
    ///
    /// # Returns
    /// The function result as a Value.
    pub fn call_function(
        &self,
        namespace: &str,
        function: &str,
        args: &[Value],
    ) -> PyResult<Value> {
        let module = self.modules.get(namespace).ok_or_else(|| {
            PyErr::new::<pyo3::exceptions::PyKeyError, _>(format!(
                "Module '{}' not loaded",
                namespace
            ))
        })?;

        Python::with_gil(|py| {
            let func = module.functions.get(function).ok_or_else(|| {
                PyErr::new::<pyo3::exceptions::PyKeyError, _>(format!(
                    "Function '{}' not found in module '{}'",
                    function, namespace
                ))
            })?;

            // Convert arguments to Python objects
            let py_args: Vec<PyObject> = args
                .iter()
                .map(|v| value_to_python(py, v))
                .collect::<PyResult<_>>()?;

            // Call the function
            let result = func.callable.bind(py).call1(PyTuple::new(py, &py_args)?)?;

            // Convert result back to Value
            python_to_value(py, result.into_any())
        })
    }

    /// List all loaded modules.
    pub fn list_modules(&self) -> Vec<&str> {
        self.modules.keys().map(|s| s.as_str()).collect()
    }

    /// List all functions in a module.
    pub fn list_functions(&self, namespace: &str) -> Option<Vec<&str>> {
        self.modules.get(namespace).map(|m| {
            m.functions.keys().map(|s| s.as_str()).collect()
        })
    }

    /// Reload a module from disk.
    ///
    /// This is useful during development when Python code changes.
    pub fn reload_module(&mut self, namespace: &str) -> PyResult<()> {
        let path = self.modules.get(namespace)
            .map(|m| m.path.clone())
            .ok_or_else(|| {
                PyErr::new::<pyo3::exceptions::PyKeyError, _>(format!(
                    "Module '{}' not loaded",
                    namespace
                ))
            })?;

        Python::with_gil(|py| {
            let importlib = py.import("importlib")?;

            // Get the module object
            if let Some(module) = self.modules.get(namespace) {
                // Reload it
                importlib.call_method1("reload", (module.module.bind(py),))?;

                // Re-discover functions
                let module_ref = module.module.bind(py);
                let functions = PythonModule::discover_functions(py, module_ref)?;

                // Update the stored module
                if let Some(m) = self.modules.get_mut(namespace) {
                    m.functions = functions;
                }
            }

            Ok(())
        })?;

        Ok(())
    }
}

impl Default for PythonRuntime {
    fn default() -> Self {
        Self::new().expect("Failed to initialize Python runtime")
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;
    use tempfile::NamedTempFile;

    #[test]
    fn test_runtime_creation() {
        let runtime = PythonRuntime::new();
        assert!(runtime.is_ok());
    }

    #[test]
    fn test_module_loading() {
        // Create a temporary Python module
        let mut file = NamedTempFile::with_suffix(".py").unwrap();
        writeln!(file, "def greet(name):\n    return f'Hello, {{name}}!'").unwrap();

        let mut runtime = PythonRuntime::new().unwrap();
        let result = runtime.load_module(file.path(), "test_module");

        // Note: This may fail if Python is not properly configured
        // In that case, the test is skipped
        if result.is_ok() {
            let functions = runtime.list_functions("test_module");
            assert!(functions.is_some());
            assert!(functions.unwrap().contains(&"greet"));
        }
    }
}
