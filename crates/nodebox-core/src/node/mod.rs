//! Node graph model for NodeBox.
//!
//! This module contains:
//! - [`Port`] - Input ports on nodes with type and value
//! - [`Connection`] - Connections between nodes
//! - [`Node`] - A node in the graph
//! - [`NodeLibrary`] - A collection of nodes
//! - [`EvalError`] - Errors during node evaluation

mod port;
mod connection;
mod node;
mod library;

pub use port::{Port, PortType, PortRange, Widget, MenuItem};
pub use connection::Connection;
pub use node::Node;
pub use library::NodeLibrary;

/// Errors that can occur during node evaluation.
#[derive(Debug, Clone)]
pub enum EvalError {
    /// A required node was not found.
    NodeNotFound(String),

    /// A required port was not found.
    PortNotFound { node: String, port: String },

    /// A function was not found in the registry.
    FunctionNotFound(String),

    /// Type mismatch during evaluation.
    TypeError { expected: String, got: String },

    /// A cycle was detected in the node graph.
    CycleDetected(Vec<String>),

    /// An error occurred in a Python function.
    PythonError(String),

    /// A general evaluation error.
    Other(String),
}

impl std::fmt::Display for EvalError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            EvalError::NodeNotFound(name) => write!(f, "Node not found: {}", name),
            EvalError::PortNotFound { node, port } => {
                write!(f, "Port '{}' not found on node '{}'", port, node)
            }
            EvalError::FunctionNotFound(name) => write!(f, "Function not found: {}", name),
            EvalError::TypeError { expected, got } => {
                write!(f, "Type error: expected {}, got {}", expected, got)
            }
            EvalError::CycleDetected(nodes) => {
                write!(f, "Cycle detected: {}", nodes.join(" -> "))
            }
            EvalError::PythonError(msg) => write!(f, "Python error: {}", msg),
            EvalError::Other(msg) => write!(f, "{}", msg),
        }
    }
}

impl std::error::Error for EvalError {}
