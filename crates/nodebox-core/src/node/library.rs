//! NodeLibrary - a collection of nodes loaded from an .ndbx file.

use std::collections::HashMap;
use super::Node;
use super::PortType;

/// A library of nodes, typically loaded from an .ndbx file.
///
/// NodeLibrary represents a document or a built-in library of nodes.
/// It has a root network node that contains all other nodes.
#[derive(Clone, Debug, PartialEq)]
pub struct NodeLibrary {
    /// The library name (e.g., "corevector", "math").
    pub name: String,
    /// The root network node.
    pub root: Node,
    /// Properties (e.g., canvas size).
    pub properties: HashMap<String, String>,
    /// The UUID of this library.
    pub uuid: Option<String>,
    /// Format version of the .ndbx file.
    pub format_version: u32,
}

impl Default for NodeLibrary {
    fn default() -> Self {
        NodeLibrary {
            name: String::new(),
            root: Node::network("root"),
            properties: HashMap::new(),
            uuid: None,
            format_version: 21,
        }
    }
}

impl NodeLibrary {
    /// Creates a new empty library with the given name.
    pub fn new(name: impl Into<String>) -> Self {
        NodeLibrary {
            name: name.into(),
            ..Default::default()
        }
    }

    /// Returns the document width from properties.
    /// Note: internally stored as "canvasWidth" for backwards compatibility.
    pub fn width(&self) -> f64 {
        self.properties
            .get("canvasWidth")
            .and_then(|s| s.parse().ok())
            .unwrap_or(1000.0)
    }

    /// Returns the document height from properties.
    /// Note: internally stored as "canvasHeight" for backwards compatibility.
    pub fn height(&self) -> f64 {
        self.properties
            .get("canvasHeight")
            .and_then(|s| s.parse().ok())
            .unwrap_or(1000.0)
    }

    /// Sets the document width.
    /// Note: internally stored as "canvasWidth" for backwards compatibility.
    pub fn set_width(&mut self, width: f64) {
        self.set_property("canvasWidth", (width as i64).to_string());
    }

    /// Sets the document height.
    /// Note: internally stored as "canvasHeight" for backwards compatibility.
    pub fn set_height(&mut self, height: f64) {
        self.set_property("canvasHeight", (height as i64).to_string());
    }

    /// Sets a property.
    pub fn set_property(&mut self, key: impl Into<String>, value: impl Into<String>) {
        self.properties.insert(key.into(), value.into());
    }

    /// Gets a property.
    pub fn property(&self, key: &str) -> Option<&str> {
        self.properties.get(key).map(String::as_str)
    }

    /// Finds a node by path (e.g., "/root/rect1").
    pub fn node_at_path(&self, path: &str) -> Option<&Node> {
        if path == "/" || path.is_empty() {
            return Some(&self.root);
        }

        let path = path.trim_start_matches('/');
        let parts: Vec<&str> = path.split('/').collect();

        // First part should be "root" or we start from root
        let mut current = &self.root;
        let start_idx = if parts.first() == Some(&"root") { 1 } else { 0 };

        for part in parts.iter().skip(start_idx) {
            match current.child(part) {
                Some(child) => current = child,
                None => return None,
            }
        }

        Some(current)
    }

    /// Finds a node by path (mutable).
    pub fn node_at_path_mut(&mut self, path: &str) -> Option<&mut Node> {
        if path == "/" || path.is_empty() {
            return Some(&mut self.root);
        }

        let path = path.trim_start_matches('/');
        let parts: Vec<&str> = path.split('/').collect();

        let mut current = &mut self.root;
        let start_idx = if parts.first() == Some(&"root") { 1 } else { 0 };

        for part in parts.iter().skip(start_idx) {
            current = current.child_mut(part)?;
        }

        Some(current)
    }

    /// Returns true if the currently rendered node outputs Point type.
    pub fn is_rendered_output_point(&self) -> bool {
        self.root.rendered_child.as_ref()
            .and_then(|name| self.root.child(name))
            .map(|node| node.output_type == PortType::Point)
            .unwrap_or(false)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_library_new() {
        let lib = NodeLibrary::new("test");
        assert_eq!(lib.name, "test");
        assert!(lib.root.is_network);
    }

    #[test]
    fn test_library_size() {
        let mut lib = NodeLibrary::new("test");
        lib.set_width(800.0);
        lib.set_height(600.0);

        assert_eq!(lib.width(), 800.0);
        assert_eq!(lib.height(), 600.0);
        // Verify backwards compatibility - stored as canvasWidth/canvasHeight
        assert_eq!(lib.property("canvasWidth"), Some("800"));
        assert_eq!(lib.property("canvasHeight"), Some("600"));
    }

    #[test]
    fn test_library_node_at_path() {
        let mut lib = NodeLibrary::new("test");
        lib.root = Node::network("root")
            .with_child(Node::new("rect1"))
            .with_child(
                Node::network("subnetwork")
                    .with_child(Node::new("inner1"))
            );

        assert!(lib.node_at_path("/").is_some());
        assert!(lib.node_at_path("/rect1").is_some());
        assert!(lib.node_at_path("/subnetwork").is_some());
        assert!(lib.node_at_path("/subnetwork/inner1").is_some());
        assert!(lib.node_at_path("/missing").is_none());
    }

    #[test]
    fn test_library_default_size() {
        let lib = NodeLibrary::new("test");
        assert_eq!(lib.width(), 1000.0);
        assert_eq!(lib.height(), 1000.0);
    }

    #[test]
    fn test_is_rendered_output_point() {
        let mut lib = NodeLibrary::new("test");

        // No rendered child - should return false
        assert!(!lib.is_rendered_output_point());

        // Add a node with default Geometry output type
        lib.root = Node::network("root")
            .with_child(Node::new("rect1"))
            .with_rendered_child("rect1");
        assert!(!lib.is_rendered_output_point());

        // Add a node with Point output type
        lib.root = Node::network("root")
            .with_child(Node::new("grid1").with_output_type(PortType::Point))
            .with_rendered_child("grid1");
        assert!(lib.is_rendered_output_point());
    }
}
