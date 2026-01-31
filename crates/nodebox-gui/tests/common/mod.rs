//! Common test utilities for nodebox-gui tests.

use nodebox_gui::{Color, Connection, Node, NodeLibrary, Point, Port, Value};

/// Create a node library with a single ellipse node.
pub fn library_with_ellipse() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        )
        .with_rendered_child("ellipse1");
    library
}

/// Create a node library with a single rect node.
pub fn library_with_rect() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 50.0)),
        )
        .with_rendered_child("rect1");
    library
}

/// Create a node library with a polygon node.
pub fn library_with_polygon() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("polygon1")
                .with_prototype("corevector.polygon")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("radius", 50.0))
                .with_input(Port::int("sides", 6))
                .with_input(Port::boolean("align", true)),
        )
        .with_rendered_child("polygon1");
    library
}

/// Create a node library with a star node.
pub fn library_with_star() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("star1")
                .with_prototype("corevector.star")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::int("points", 5))
                .with_input(Port::float("outer", 50.0))
                .with_input(Port::float("inner", 25.0)),
        )
        .with_rendered_child("star1");
    library
}

/// Create a node library with an arc node.
pub fn library_with_arc() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("arc1")
                .with_prototype("corevector.arc")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0))
                .with_input(Port::float("start_angle", 0.0))
                .with_input(Port::float("degrees", 90.0))
                .with_input(Port::string("type", "pie")),
        )
        .with_rendered_child("arc1");
    library
}

/// Create a node library with a line node.
pub fn library_with_line() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("line1")
                .with_prototype("corevector.line")
                .with_input(Port::point("point1", Point::new(0.0, 0.0)))
                .with_input(Port::point("point2", Point::new(100.0, 100.0)))
                .with_input(Port::int("points", 2)),
        )
        .with_rendered_child("line1");
    library
}

/// Create a node library with two connected nodes (ellipse -> colorize).
pub fn library_with_colorized_ellipse() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        )
        .with_child(
            Node::new("colorize1")
                .with_prototype("corevector.colorize")
                .with_input(Port::geometry("shape"))
                .with_input(Port::color("fill", Color::rgb(1.0, 0.0, 0.0)))
                .with_input(Port::color("stroke", Color::BLACK))
                .with_input(Port::float("strokeWidth", 2.0)),
        )
        .with_connection(Connection::new("ellipse1", "colorize1", "shape"))
        .with_rendered_child("colorize1");
    library
}

/// Create a node library with multiple shapes merged together.
pub fn library_with_merged_shapes() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::point("position", Point::new(-50.0, 0.0)))
                .with_input(Port::float("width", 50.0))
                .with_input(Port::float("height", 50.0)),
        )
        .with_child(
            Node::new("rect1")
                .with_prototype("corevector.rect")
                .with_input(Port::point("position", Point::new(50.0, 0.0)))
                .with_input(Port::float("width", 50.0))
                .with_input(Port::float("height", 50.0)),
        )
        .with_child(
            Node::new("merge1")
                .with_prototype("corevector.merge")
                .with_input(Port::geometry("shapes")),
        )
        .with_connection(Connection::new("ellipse1", "merge1", "shapes"))
        .with_connection(Connection::new("rect1", "merge1", "shapes"))
        .with_rendered_child("merge1");
    library
}

/// Create a node library with a transform chain (ellipse -> translate -> rotate).
pub fn library_with_transform_chain() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    library.root = Node::network("root")
        .with_child(
            Node::new("ellipse1")
                .with_prototype("corevector.ellipse")
                .with_input(Port::point("position", Point::ZERO))
                .with_input(Port::float("width", 100.0))
                .with_input(Port::float("height", 100.0)),
        )
        .with_child(
            Node::new("translate1")
                .with_prototype("corevector.translate")
                .with_input(Port::geometry("shape"))
                .with_input(Port::point("translate", Point::new(50.0, 50.0))),
        )
        .with_child(
            Node::new("rotate1")
                .with_prototype("corevector.rotate")
                .with_input(Port::geometry("shape"))
                .with_input(Port::float("angle", 45.0))
                .with_input(Port::point("origin", Point::ZERO)),
        )
        .with_connection(Connection::new("ellipse1", "translate1", "shape"))
        .with_connection(Connection::new("translate1", "rotate1", "shape"))
        .with_rendered_child("rotate1");
    library
}

/// Create an empty node library (for testing error conditions).
pub fn library_empty() -> NodeLibrary {
    NodeLibrary::new("test")
}

/// Create a node library with a missing connection (should handle gracefully).
pub fn library_with_missing_input() -> NodeLibrary {
    let mut library = NodeLibrary::new("test");
    // Colorize node without connected shape input
    library.root = Node::network("root")
        .with_child(
            Node::new("colorize1")
                .with_prototype("corevector.colorize")
                .with_input(Port::geometry("shape"))
                .with_input(Port::color("fill", Color::rgb(1.0, 0.0, 0.0)))
                .with_input(Port::color("stroke", Color::BLACK))
                .with_input(Port::float("strokeWidth", 2.0)),
        )
        .with_rendered_child("colorize1");
    library
}
