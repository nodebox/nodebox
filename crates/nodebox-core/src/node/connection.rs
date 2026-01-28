//! Connection - represents a connection between two nodes.

use std::fmt;

/// A connection between an output node and an input port on another node.
///
/// Connections go from the output of one node (there's only one output per node)
/// to a specific input port on another node.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct Connection {
    /// The name of the upstream (output) node.
    pub output_node: String,
    /// The name of the downstream (input) node.
    pub input_node: String,
    /// The name of the input port on the downstream node.
    pub input_port: String,
}

impl Connection {
    /// Creates a new connection.
    pub fn new(
        output_node: impl Into<String>,
        input_node: impl Into<String>,
        input_port: impl Into<String>,
    ) -> Self {
        Connection {
            output_node: output_node.into(),
            input_node: input_node.into(),
            input_port: input_port.into(),
        }
    }

    /// Parses a connection from the NDBX format.
    ///
    /// Format: "inputNode.inputPort" for input, "outputNode" for output
    pub fn from_ndbx(input: &str, output: &str) -> Option<Self> {
        let parts: Vec<&str> = input.split('.').collect();
        if parts.len() != 2 {
            return None;
        }

        Some(Connection::new(output, parts[0], parts[1]))
    }
}

impl fmt::Display for Connection {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "{} => {}.{}",
            self.output_node, self.input_node, self.input_port
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_connection_new() {
        let conn = Connection::new("rect1", "colorize1", "shape");
        assert_eq!(conn.output_node, "rect1");
        assert_eq!(conn.input_node, "colorize1");
        assert_eq!(conn.input_port, "shape");
    }

    #[test]
    fn test_connection_from_ndbx() {
        let conn = Connection::from_ndbx("colorize1.shape", "rect1").unwrap();
        assert_eq!(conn.output_node, "rect1");
        assert_eq!(conn.input_node, "colorize1");
        assert_eq!(conn.input_port, "shape");
    }

    #[test]
    fn test_connection_display() {
        let conn = Connection::new("rect1", "colorize1", "shape");
        assert_eq!(format!("{}", conn), "rect1 => colorize1.shape");
    }

    #[test]
    fn test_connection_equality() {
        let conn1 = Connection::new("a", "b", "c");
        let conn2 = Connection::new("a", "b", "c");
        let conn3 = Connection::new("a", "b", "d");

        assert_eq!(conn1, conn2);
        assert_ne!(conn1, conn3);
    }
}
