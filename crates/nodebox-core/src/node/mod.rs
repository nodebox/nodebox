//! Node graph model for NodeBox.
//!
//! This module contains:
//! - [`Port`] - Input ports on nodes with type and value
//! - [`Connection`] - Connections between nodes
//! - [`Node`] - A node in the graph
//! - [`NodeLibrary`] - A collection of nodes

mod port;
mod connection;
mod node;
mod library;

pub use port::{Port, PortType, PortRange, Widget, MenuItem};
pub use connection::Connection;
pub use node::Node;
pub use library::NodeLibrary;
