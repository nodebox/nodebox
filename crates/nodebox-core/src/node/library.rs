//! NodeLibrary - a collection of nodes loaded from an .ndbx file.

use std::collections::HashMap;
use super::Node;

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

    /// Returns the canvas width from properties.
    pub fn canvas_width(&self) -> f64 {
        self.properties
            .get("canvasWidth")
            .and_then(|s| s.parse().ok())
            .unwrap_or(1000.0)
    }

    /// Returns the canvas height from properties.
    pub fn canvas_height(&self) -> f64 {
        self.properties
            .get("canvasHeight")
            .and_then(|s| s.parse().ok())
            .unwrap_or(1000.0)
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
    fn test_library_canvas_size() {
        let mut lib = NodeLibrary::new("test");
        lib.set_property("canvasWidth", "800");
        lib.set_property("canvasHeight", "600");

        assert_eq!(lib.canvas_width(), 800.0);
        assert_eq!(lib.canvas_height(), 600.0);
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
    fn test_library_default_canvas_size() {
        let lib = NodeLibrary::new("test");
        assert_eq!(lib.canvas_width(), 1000.0);
        assert_eq!(lib.canvas_height(), 1000.0);
    }
}
