//! Bridge between Python functions and the NodeBox function registry.

use std::sync::{Arc, Mutex};

use nodebox_core::value::Value;
use nodebox_core::node::EvalError;

use crate::runtime::PythonRuntime;
use crate::convert::{value_to_python, python_to_value};

use pyo3::prelude::*;
use pyo3::types::PyTuple;

/// A wrapper that allows Python functions to be called from the node system.
///
/// This struct holds a reference to the Python runtime and the function
/// to call, enabling integration with the FunctionRegistry.
pub struct PythonBridge {
    runtime: Arc<Mutex<PythonRuntime>>,
}

impl PythonBridge {
    /// Create a new Python bridge with a shared runtime.
    pub fn new(runtime: Arc<Mutex<PythonRuntime>>) -> Self {
        Self { runtime }
    }

    /// Create a callable that can be used in the function registry.
    ///
    /// The returned closure captures the namespace and function name,
    /// and calls the Python function when invoked.
    pub fn make_callable(
        &self,
        namespace: &str,
        function: &str,
    ) -> Box<dyn Fn(&[Value]) -> Result<Value, EvalError> + Send + Sync> {
        let runtime = Arc::clone(&self.runtime);
        let namespace = namespace.to_string();
        let function = function.to_string();

        Box::new(move |args: &[Value]| {
            let runtime_guard = runtime.lock().map_err(|e| {
                EvalError::PythonError(format!("Failed to acquire runtime lock: {}", e))
            })?;

            runtime_guard.call_function(&namespace, &function, args).map_err(|e| {
                EvalError::PythonError(format!("Python error: {}", e))
            })
        })
    }

    /// Register all functions from a loaded module with the function registry.
    ///
    /// This creates wrapper functions for each Python function in the module
    /// and registers them with the given prefix.
    pub fn register_module_functions<F>(
        &self,
        namespace: &str,
        register_fn: F,
    ) -> Result<Vec<String>, EvalError>
    where
        F: Fn(&str, Box<dyn Fn(&[Value]) -> Result<Value, EvalError> + Send + Sync>),
    {
        let runtime_guard = self.runtime.lock().map_err(|e| {
            EvalError::PythonError(format!("Failed to acquire runtime lock: {}", e))
        })?;

        let functions = runtime_guard.list_functions(namespace).ok_or_else(|| {
            EvalError::PythonError(format!("Module '{}' not found", namespace))
        })?;

        let mut registered = Vec::new();

        for func_name in functions {
            let full_name = format!("{}/{}", namespace, func_name);
            let callable = self.make_callable(namespace, func_name);
            register_fn(&full_name, callable);
            registered.push(full_name);
        }

        Ok(registered)
    }
}

/// Extension trait for integrating Python with the function registry.
pub trait PythonIntegration {
    /// Load a Python module and register its functions.
    fn register_python_module(
        &mut self,
        bridge: &PythonBridge,
        namespace: &str,
        path: &std::path::Path,
    ) -> Result<Vec<String>, EvalError>;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_bridge_creation() {
        let runtime = Arc::new(Mutex::new(PythonRuntime::new().unwrap()));
        let bridge = PythonBridge::new(runtime);
        // Bridge should be created successfully
    }
}
