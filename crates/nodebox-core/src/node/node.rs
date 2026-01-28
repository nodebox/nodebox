//! Node - a node in the node graph.

use crate::geometry::Point;
use super::{Port, PortType, PortRange, Connection};

/// A node in the node graph.
///
/// Nodes are the fundamental building blocks of NodeBox. Each node:
/// - Has a name and optional prototype (for inheritance)
/// - Has input ports that receive data
/// - Produces output of a specific type
/// - Can be a network containing child nodes
///
/// Nodes are immutable; "mutations" return new node instances.
#[derive(Clone, Debug, PartialEq)]
pub struct Node {
    /// The node's unique name within its parent network.
    pub name: String,
    /// The prototype this node extends (e.g., "corevector.ellipse").
    pub prototype: Option<String>,
    /// The function to execute (e.g., "corevector/ellipse").
    pub function: Option<String>,
    /// The category for UI organization.
    pub category: String,
    /// Human-readable description.
    pub description: Option<String>,
    /// Comment text (user annotation).
    pub comment: Option<String>,
    /// Position in the network view (grid units).
    pub position: Point,
    /// Input ports.
    pub inputs: Vec<Port>,
    /// Output type.
    pub output_type: PortType,
    /// Whether output is a single value or list.
    pub output_range: PortRange,
    /// Whether this node is a network (contains child nodes).
    pub is_network: bool,
    /// Child nodes (only if is_network is true).
    pub children: Vec<Node>,
    /// The name of the rendered child (only if is_network is true).
    pub rendered_child: Option<String>,
    /// Connections between child nodes.
    pub connections: Vec<Connection>,
    /// The handle function for interactive editing.
    pub handle: Option<String>,
    /// Whether this node should always be rendered (even if not connected).
    pub always_rendered: bool,
}

impl Default for Node {
    fn default() -> Self {
        Node {
            name: String::new(),
            prototype: None,
            function: None,
            category: String::new(),
            description: None,
            comment: None,
            position: Point::ZERO,
            inputs: Vec::new(),
            output_type: PortType::Geometry,
            output_range: PortRange::Value,
            is_network: false,
            children: Vec::new(),
            rendered_child: None,
            connections: Vec::new(),
            handle: None,
            always_rendered: false,
        }
    }
}

impl Node {
    /// Creates a new node with the given name.
    pub fn new(name: impl Into<String>) -> Self {
        Node {
            name: name.into(),
            ..Default::default()
        }
    }

    /// Creates a network node with the given name.
    pub fn network(name: impl Into<String>) -> Self {
        Node {
            name: name.into(),
            is_network: true,
            output_range: PortRange::List,
            ..Default::default()
        }
    }

    /// Returns the full path for a child node.
    pub fn child_path(parent_path: &str, node_name: &str) -> String {
        if parent_path == "/" {
            format!("/{}", node_name)
        } else {
            format!("{}/{}", parent_path, node_name)
        }
    }

    /// Finds an input port by name.
    pub fn input(&self, name: &str) -> Option<&Port> {
        self.inputs.iter().find(|p| p.name == name)
    }

    /// Finds an input port by name (mutable).
    pub fn input_mut(&mut self, name: &str) -> Option<&mut Port> {
        self.inputs.iter_mut().find(|p| p.name == name)
    }

    /// Finds a child node by name.
    pub fn child(&self, name: &str) -> Option<&Node> {
        self.children.iter().find(|n| n.name == name)
    }

    /// Finds a child node by name (mutable).
    pub fn child_mut(&mut self, name: &str) -> Option<&mut Node> {
        self.children.iter_mut().find(|n| n.name == name)
    }

    /// Returns connections that output from the given node.
    pub fn connections_from(&self, node_name: &str) -> Vec<&Connection> {
        self.connections
            .iter()
            .filter(|c| c.output_node == node_name)
            .collect()
    }

    /// Returns connections that input to the given node.
    pub fn connections_to(&self, node_name: &str) -> Vec<&Connection> {
        self.connections
            .iter()
            .filter(|c| c.input_node == node_name)
            .collect()
    }

    /// Returns the connection to a specific input port, if any.
    pub fn connection_to_port(&self, node_name: &str, port_name: &str) -> Option<&Connection> {
        self.connections
            .iter()
            .find(|c| c.input_node == node_name && c.input_port == port_name)
    }

    /// Adds an input port.
    pub fn with_input(mut self, port: Port) -> Self {
        self.inputs.push(port);
        self
    }

    /// Sets the prototype.
    pub fn with_prototype(mut self, prototype: impl Into<String>) -> Self {
        self.prototype = Some(prototype.into());
        self
    }

    /// Sets the function.
    pub fn with_function(mut self, function: impl Into<String>) -> Self {
        self.function = Some(function.into());
        self
    }

    /// Sets the position.
    pub fn with_position(mut self, x: f64, y: f64) -> Self {
        self.position = Point::new(x, y);
        self
    }

    /// Sets the output type.
    pub fn with_output_type(mut self, output_type: PortType) -> Self {
        self.output_type = output_type;
        self
    }

    /// Sets the category.
    pub fn with_category(mut self, category: impl Into<String>) -> Self {
        self.category = category.into();
        self
    }

    /// Adds a child node.
    pub fn with_child(mut self, child: Node) -> Self {
        self.children.push(child);
        self
    }

    /// Adds a connection.
    pub fn with_connection(mut self, connection: Connection) -> Self {
        self.connections.push(connection);
        self
    }

    /// Sets the rendered child.
    pub fn with_rendered_child(mut self, name: impl Into<String>) -> Self {
        self.rendered_child = Some(name.into());
        self
    }

    /// Generates a unique name for a new child based on a prefix.
    pub fn unique_child_name(&self, prefix: &str) -> String {
        let existing: std::collections::HashSet<_> =
            self.children.iter().map(|c| c.name.as_str()).collect();

        if !existing.contains(prefix) {
            return prefix.to_string();
        }

        for i in 1..1000 {
            let name = format!("{}{}", prefix, i);
            if !existing.contains(name.as_str()) {
                return name;
            }
        }

        // Fallback (shouldn't happen)
        format!("{}{}", prefix, uuid::Uuid::new_v4())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::node::Port;

    #[test]
    fn test_node_new() {
        let node = Node::new("rect1");
        assert_eq!(node.name, "rect1");
        assert!(!node.is_network);
    }

    #[test]
    fn test_node_network() {
        let node = Node::network("root");
        assert!(node.is_network);
        assert_eq!(node.output_range, PortRange::List);
    }

    #[test]
    fn test_node_with_inputs() {
        let node = Node::new("rect1")
            .with_input(Port::float("x", 0.0))
            .with_input(Port::float("y", 0.0))
            .with_input(Port::float("width", 100.0))
            .with_input(Port::float("height", 100.0));

        assert_eq!(node.inputs.len(), 4);
        assert_eq!(node.input("width").unwrap().value.as_float(), Some(100.0));
    }

    #[test]
    fn test_node_child_path() {
        assert_eq!(Node::child_path("/", "rect1"), "/rect1");
        assert_eq!(Node::child_path("/root", "rect1"), "/root/rect1");
    }

    #[test]
    fn test_node_find_child() {
        let node = Node::network("root")
            .with_child(Node::new("rect1"))
            .with_child(Node::new("colorize1"));

        assert!(node.child("rect1").is_some());
        assert!(node.child("missing").is_none());
    }

    #[test]
    fn test_node_connections() {
        let node = Node::network("root")
            .with_child(Node::new("rect1"))
            .with_child(Node::new("colorize1"))
            .with_connection(Connection::new("rect1", "colorize1", "shape"));

        assert_eq!(node.connections.len(), 1);
        assert_eq!(node.connections_from("rect1").len(), 1);
        assert_eq!(node.connections_to("colorize1").len(), 1);
        assert!(node.connection_to_port("colorize1", "shape").is_some());
    }

    #[test]
    fn test_node_unique_name() {
        let node = Node::network("root")
            .with_child(Node::new("rect"))
            .with_child(Node::new("rect1"));

        assert_eq!(node.unique_child_name("ellipse"), "ellipse");
        assert_eq!(node.unique_child_name("rect"), "rect2");
    }
}
