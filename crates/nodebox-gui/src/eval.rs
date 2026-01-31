//! Network evaluation - executes node graphs to produce geometry.

use std::collections::HashMap;
use nodebox_core::geometry::{Path, Point, Color};
use nodebox_core::node::{Node, NodeLibrary};
use nodebox_core::Value;

/// The result of evaluating a node.
#[derive(Clone, Debug)]
pub enum NodeOutput {
    /// No output (node not found or error).
    None,
    /// A single path.
    Path(Path),
    /// A list of paths.
    Paths(Vec<Path>),
    /// A single point.
    Point(Point),
    /// A list of points.
    Points(Vec<Point>),
    /// A float value.
    Float(f64),
    /// An integer value.
    Int(i64),
    /// A string value.
    String(String),
    /// A color value.
    Color(Color),
    /// A boolean value.
    Boolean(bool),
}

impl NodeOutput {
    /// Convert to a list of paths (for rendering).
    pub fn to_paths(&self) -> Vec<Path> {
        match self {
            NodeOutput::Path(p) => vec![p.clone()],
            NodeOutput::Paths(ps) => ps.clone(),
            _ => Vec::new(),
        }
    }

    /// Get as a single path if available.
    pub fn as_path(&self) -> Option<&Path> {
        match self {
            NodeOutput::Path(p) => Some(p),
            _ => None,
        }
    }

    /// Get as paths (single or list).
    pub fn as_paths(&self) -> Option<Vec<Path>> {
        match self {
            NodeOutput::Path(p) => Some(vec![p.clone()]),
            NodeOutput::Paths(ps) => Some(ps.clone()),
            _ => None,
        }
    }
}

/// Evaluate a node network and return the output of the rendered node.
pub fn evaluate_network(library: &NodeLibrary) -> Vec<Path> {
    let network = &library.root;

    // Find the rendered child
    let rendered_name = match &network.rendered_child {
        Some(name) => name.clone(),
        None => {
            // No rendered child, return empty
            return Vec::new();
        }
    };

    // Create a cache for node outputs
    let mut cache: HashMap<String, NodeOutput> = HashMap::new();

    // Evaluate the rendered node (this will recursively evaluate dependencies)
    let output = evaluate_node(network, &rendered_name, &mut cache);

    output.to_paths()
}

/// Evaluate a single node, recursively evaluating its dependencies.
fn evaluate_node(
    network: &Node,
    node_name: &str,
    cache: &mut HashMap<String, NodeOutput>,
) -> NodeOutput {
    // Check cache first
    if let Some(output) = cache.get(node_name) {
        return output.clone();
    }

    // Find the node
    let node = match network.child(node_name) {
        Some(n) => n,
        None => return NodeOutput::None,
    };

    // Collect input values for this node
    let mut inputs: HashMap<String, NodeOutput> = HashMap::new();

    // For each input port, check if there are connections
    for port in &node.inputs {
        // Get ALL connections to this port (for merge/combine operations)
        let connections: Vec<_> = network.connections
            .iter()
            .filter(|c| c.input_node == node_name && c.input_port == port.name)
            .collect();

        if connections.is_empty() {
            // No connections - use the port's default value
            inputs.insert(port.name.clone(), value_to_output(&port.value));
        } else if connections.len() == 1 {
            // Single connection - evaluate and use directly
            let upstream_output = evaluate_node(network, &connections[0].output_node, cache);
            inputs.insert(port.name.clone(), upstream_output);
        } else {
            // Multiple connections - collect all outputs as paths
            let mut all_paths: Vec<Path> = Vec::new();
            for conn in connections {
                let upstream_output = evaluate_node(network, &conn.output_node, cache);
                all_paths.extend(upstream_output.to_paths());
            }
            inputs.insert(port.name.clone(), NodeOutput::Paths(all_paths));
        }
    }

    // Execute the node function
    let output = execute_node(node, &inputs);

    // Cache and return
    cache.insert(node_name.to_string(), output.clone());
    output
}

/// Convert a Value to a NodeOutput.
fn value_to_output(value: &Value) -> NodeOutput {
    match value {
        Value::Float(f) => NodeOutput::Float(*f),
        Value::Int(i) => NodeOutput::Int(*i),
        Value::String(s) => NodeOutput::String(s.clone()),
        Value::Boolean(b) => NodeOutput::Boolean(*b),
        Value::Point(p) => NodeOutput::Point(*p),
        Value::Color(c) => NodeOutput::Color(*c),
        Value::Geometry(_) => NodeOutput::None, // Will be filled by connections
        Value::List(_) => NodeOutput::None, // TODO: handle lists
        Value::Null => NodeOutput::None,
        Value::Path(p) => NodeOutput::Path(p.clone()),
        Value::Map(_) => NodeOutput::None, // TODO: handle maps
    }
}

/// Get a float input value.
fn get_float(inputs: &HashMap<String, NodeOutput>, name: &str, default: f64) -> f64 {
    match inputs.get(name) {
        Some(NodeOutput::Float(f)) => *f,
        Some(NodeOutput::Int(i)) => *i as f64,
        _ => default,
    }
}

/// Get an integer input value.
fn get_int(inputs: &HashMap<String, NodeOutput>, name: &str, default: i64) -> i64 {
    match inputs.get(name) {
        Some(NodeOutput::Int(i)) => *i,
        Some(NodeOutput::Float(f)) => *f as i64,
        _ => default,
    }
}

/// Get a point input value.
fn get_point(inputs: &HashMap<String, NodeOutput>, name: &str, default: Point) -> Point {
    match inputs.get(name) {
        Some(NodeOutput::Point(p)) => *p,
        _ => default,
    }
}

/// Get a color input value.
fn get_color(inputs: &HashMap<String, NodeOutput>, name: &str, default: Color) -> Color {
    match inputs.get(name) {
        Some(NodeOutput::Color(c)) => *c,
        _ => default,
    }
}

/// Get a path input value.
fn get_path(inputs: &HashMap<String, NodeOutput>, name: &str) -> Option<Path> {
    match inputs.get(name) {
        Some(NodeOutput::Path(p)) => Some(p.clone()),
        Some(NodeOutput::Paths(ps)) if !ps.is_empty() => Some(ps[0].clone()),
        _ => None,
    }
}

/// Get paths input value (for merge/combine operations).
fn get_paths(inputs: &HashMap<String, NodeOutput>, name: &str) -> Vec<Path> {
    match inputs.get(name) {
        Some(NodeOutput::Path(p)) => vec![p.clone()],
        Some(NodeOutput::Paths(ps)) => ps.clone(),
        _ => Vec::new(),
    }
}

/// Get a boolean input value.
fn get_bool(inputs: &HashMap<String, NodeOutput>, name: &str, default: bool) -> bool {
    match inputs.get(name) {
        Some(NodeOutput::Boolean(b)) => *b,
        _ => default,
    }
}

/// Get a string input value.
fn get_string(inputs: &HashMap<String, NodeOutput>, name: &str, default: &str) -> String {
    match inputs.get(name) {
        Some(NodeOutput::String(s)) => s.clone(),
        _ => default.to_string(),
    }
}

/// Execute a node and return its output.
fn execute_node(node: &Node, inputs: &HashMap<String, NodeOutput>) -> NodeOutput {
    // Get the function name (prototype determines what the node does)
    let proto = match &node.prototype {
        Some(p) => p.as_str(),
        None => return NodeOutput::None,
    };

    match proto {
        // Geometry generators
        // Note: These use "position" (Point) as per corevector.ndbx library definition
        "corevector.ellipse" => {
            let position = get_point(inputs, "position", Point::ZERO);
            let width = get_float(inputs, "width", 100.0);
            let height = get_float(inputs, "height", 100.0);
            let path = nodebox_ops::ellipse(position, width, height);
            NodeOutput::Path(path)
        }
        "corevector.rect" => {
            let position = get_point(inputs, "position", Point::ZERO);
            let width = get_float(inputs, "width", 100.0);
            let height = get_float(inputs, "height", 100.0);
            // Note: corevector.ndbx uses "roundness" (Point), not rx/ry
            let roundness = get_point(inputs, "roundness", Point::ZERO);
            let path = nodebox_ops::rect(position, width, height, roundness);
            NodeOutput::Path(path)
        }
        "corevector.line" => {
            let p1 = get_point(inputs, "point1", Point::ZERO);
            let p2 = get_point(inputs, "point2", Point::new(100.0, 100.0));
            let points = get_int(inputs, "points", 2) as u32;
            let path = nodebox_ops::line(p1, p2, points);
            NodeOutput::Path(path)
        }
        "corevector.polygon" => {
            let position = get_point(inputs, "position", Point::ZERO);
            let radius = get_float(inputs, "radius", 50.0);
            let sides = get_int(inputs, "sides", 6) as u32;
            let align = get_bool(inputs, "align", true);
            let path = nodebox_ops::polygon(position, radius, sides, align);
            NodeOutput::Path(path)
        }
        "corevector.star" => {
            let position = get_point(inputs, "position", Point::ZERO);
            let points = get_int(inputs, "points", 5) as u32;
            let outer = get_float(inputs, "outer", 50.0);
            let inner = get_float(inputs, "inner", 25.0);
            let path = nodebox_ops::star(position, points, outer, inner);
            NodeOutput::Path(path)
        }
        "corevector.arc" => {
            let position = get_point(inputs, "position", Point::ZERO);
            let width = get_float(inputs, "width", 100.0);
            let height = get_float(inputs, "height", 100.0);
            // Note: corevector.ndbx uses "start_angle" (underscore), not "startAngle"
            let start_angle = get_float(inputs, "start_angle", 0.0);
            let degrees = get_float(inputs, "degrees", 90.0);
            let arc_type = get_string(inputs, "type", "pie");
            let path = nodebox_ops::arc(position, width, height, start_angle, degrees, &arc_type);
            NodeOutput::Path(path)
        }

        // Filters/transforms
        "corevector.colorize" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            let fill = get_color(inputs, "fill", Color::WHITE);
            let stroke = get_color(inputs, "stroke", Color::BLACK);
            let stroke_width = get_float(inputs, "strokeWidth", 1.0);
            let path = nodebox_ops::colorize(&shape, fill, stroke, stroke_width);
            NodeOutput::Path(path)
        }
        "corevector.translate" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            let offset = get_point(inputs, "translate", Point::ZERO);
            let path = nodebox_ops::translate(&shape, offset);
            NodeOutput::Path(path)
        }
        "corevector.rotate" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            let angle = get_float(inputs, "angle", 0.0);
            let origin = get_point(inputs, "origin", Point::ZERO);
            let path = nodebox_ops::rotate(&shape, angle, origin);
            NodeOutput::Path(path)
        }
        "corevector.scale" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            let scale = get_point(inputs, "scale", Point::new(100.0, 100.0));
            let origin = get_point(inputs, "origin", Point::ZERO);
            let path = nodebox_ops::scale(&shape, scale, origin);
            NodeOutput::Path(path)
        }
        "corevector.align" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            let position = get_point(inputs, "position", Point::ZERO);
            let halign = get_string(inputs, "halign", "center");
            let valign = get_string(inputs, "valign", "middle");
            let path = nodebox_ops::align_str(&shape, position, &halign, &valign);
            NodeOutput::Path(path)
        }
        "corevector.fit" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            // Note: corevector.ndbx uses "position" (Point) and "keep_proportions" (underscore)
            let position = get_point(inputs, "position", Point::ZERO);
            let width = get_float(inputs, "width", 100.0);
            let height = get_float(inputs, "height", 100.0);
            let keep_proportions = get_bool(inputs, "keep_proportions", true);
            let path = nodebox_ops::fit(&shape, position, width, height, keep_proportions);
            NodeOutput::Path(path)
        }
        "corevector.copy" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            let copies = get_int(inputs, "copies", 1) as u32;
            let order = nodebox_ops::CopyOrder::from_str(&get_string(inputs, "order", "tsr"));
            // Note: corevector.ndbx uses "translate" (Point) and "scale" (Point)
            let translate = get_point(inputs, "translate", Point::ZERO);
            let rotate = get_float(inputs, "rotate", 0.0);
            let scale = get_point(inputs, "scale", Point::new(100.0, 100.0));
            let paths = nodebox_ops::copy(&shape, copies, order, translate, rotate, scale);
            NodeOutput::Paths(paths)
        }

        // Combine operations
        "corevector.merge" | "corevector.combine" => {
            // Merge/combine takes multiple shapes and combines them
            let shapes = get_paths(inputs, "shapes");
            if shapes.is_empty() {
                // Try "shape" port as fallback
                let shape = get_paths(inputs, "shape");
                if shape.is_empty() {
                    return NodeOutput::None;
                }
                return NodeOutput::Paths(shape);
            }
            NodeOutput::Paths(shapes)
        }

        // List combine - combines multiple lists into one
        "list.combine" => {
            let mut all_paths: Vec<Path> = Vec::new();
            // Collect from list1 through list5
            for port_name in ["list1", "list2", "list3", "list4", "list5"] {
                let paths = get_paths(inputs, port_name);
                all_paths.extend(paths);
            }
            if all_paths.is_empty() {
                NodeOutput::None
            } else {
                NodeOutput::Paths(all_paths)
            }
        }

        // Resample
        "corevector.resample" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            let points = get_int(inputs, "points", 20) as usize;
            let path = nodebox_ops::resample(&shape, points);
            NodeOutput::Path(path)
        }

        // Wiggle
        "corevector.wiggle" => {
            let shape = match get_path(inputs, "shape") {
                Some(p) => p,
                None => return NodeOutput::None,
            };
            let scope = nodebox_ops::WiggleScope::from_str(&get_string(inputs, "scope", "points"));
            // Note: corevector.ndbx uses "offset" (Point), not offsetX/offsetY
            let offset = get_point(inputs, "offset", Point::new(10.0, 10.0));
            let seed = get_int(inputs, "seed", 0) as u64;
            let path = nodebox_ops::wiggle(&shape, scope, offset, seed);
            NodeOutput::Path(path)
        }

        // Connect points
        "corevector.connect" => {
            // Get points from input
            let closed = get_bool(inputs, "closed", false);
            match inputs.get("points") {
                Some(NodeOutput::Points(pts)) => {
                    let path = nodebox_ops::connect(pts, closed);
                    NodeOutput::Path(path)
                }
                _ => NodeOutput::None,
            }
        }

        // Grid of points
        "corevector.grid" => {
            let columns = get_int(inputs, "columns", 3) as u32;
            let rows = get_int(inputs, "rows", 3) as u32;
            let width = get_float(inputs, "width", 100.0);
            let height = get_float(inputs, "height", 100.0);
            // Note: corevector.ndbx uses "position" (Point), not x/y
            let position = get_point(inputs, "position", Point::ZERO);
            let points = nodebox_ops::grid(columns, rows, width, height, position);
            NodeOutput::Points(points)
        }

        // Make point
        "corevector.point" | "corevector.makePoint" => {
            let x = get_float(inputs, "x", 0.0);
            let y = get_float(inputs, "y", 0.0);
            NodeOutput::Point(Point::new(x, y))
        }

        // Default: pass-through or unknown node
        _ => {
            // For unknown nodes, try to pass through a shape input
            if let Some(path) = get_path(inputs, "shape") {
                NodeOutput::Path(path)
            } else if let Some(path) = get_path(inputs, "shapes") {
                NodeOutput::Path(path)
            } else {
                log::warn!("Unknown node prototype: {}", proto);
                NodeOutput::None
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use nodebox_core::node::{Port, Connection};

    #[test]
    fn test_evaluate_simple_ellipse() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::new(100.0, 100.0)))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            )
            .with_rendered_child("ellipse1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        assert!((bounds.width - 50.0).abs() < 0.1);
        assert!((bounds.height - 50.0).abs() < 0.1);
    }

    #[test]
    fn test_evaluate_colorized_ellipse() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::new(100.0, 100.0)))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            )
            .with_child(
                Node::new("colorize1")
                    .with_prototype("corevector.colorize")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::color("fill", Color::rgb(1.0, 0.0, 0.0)))
                    .with_input(Port::color("stroke", Color::BLACK))
                    .with_input(Port::float("strokeWidth", 2.0))
            )
            .with_connection(Connection::new("ellipse1", "colorize1", "shape"))
            .with_rendered_child("colorize1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        // Check that the colorize was applied
        assert!(paths[0].fill.is_some());
        let fill = paths[0].fill.unwrap();
        assert!((fill.r - 1.0).abs() < 0.01);
        assert!(fill.g < 0.01);
        assert!(fill.b < 0.01);
    }

    #[test]
    fn test_evaluate_merged_shapes() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            )
            .with_child(
                Node::new("rect1")
                    .with_prototype("corevector.rect")
                    .with_input(Port::point("position", Point::new(100.0, 0.0)))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            )
            .with_child(
                Node::new("merge1")
                    .with_prototype("corevector.merge")
                    .with_input(Port::geometry("shapes"))
            )
            .with_connection(Connection::new("ellipse1", "merge1", "shapes"))
            .with_connection(Connection::new("rect1", "merge1", "shapes"))
            .with_rendered_child("merge1");

        let paths = evaluate_network(&library);
        // Merge collects all connected shapes
        assert_eq!(paths.len(), 2);
    }

    #[test]
    fn test_evaluate_rect() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("rect1")
                    .with_prototype("corevector.rect")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 80.0))
                    .with_input(Port::float("height", 40.0))
            )
            .with_rendered_child("rect1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        assert!((bounds.width - 80.0).abs() < 0.1);
        assert!((bounds.height - 40.0).abs() < 0.1);
    }

    #[test]
    fn test_evaluate_line() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("line1")
                    .with_prototype("corevector.line")
                    .with_input(Port::point("point1", Point::new(0.0, 0.0)))
                    .with_input(Port::point("point2", Point::new(100.0, 50.0)))
                    .with_input(Port::int("points", 2))
            )
            .with_rendered_child("line1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        assert!((bounds.width - 100.0).abs() < 0.1);
        assert!((bounds.height - 50.0).abs() < 0.1);
    }

    #[test]
    fn test_evaluate_polygon() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("polygon1")
                    .with_prototype("corevector.polygon")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("radius", 50.0))
                    .with_input(Port::int("sides", 6))
                    .with_input(Port::boolean("align", true))
            )
            .with_rendered_child("polygon1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        // Hexagon with radius 50 should have bounds approximately 100x86 (2*r x sqrt(3)*r)
        let bounds = paths[0].bounds().unwrap();
        assert!(bounds.width > 80.0 && bounds.width < 110.0);
        assert!(bounds.height > 80.0 && bounds.height < 110.0);
    }

    #[test]
    fn test_evaluate_star() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("star1")
                    .with_prototype("corevector.star")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::int("points", 5))
                    .with_input(Port::float("outer", 50.0))
                    .with_input(Port::float("inner", 25.0))
            )
            .with_rendered_child("star1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        // Star with outer radius 50 should have bounds approximately 100x100
        let bounds = paths[0].bounds().unwrap();
        assert!(bounds.width > 80.0 && bounds.width < 110.0);
    }

    #[test]
    fn test_evaluate_arc() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("arc1")
                    .with_prototype("corevector.arc")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0))
                    .with_input(Port::float("start_angle", 0.0))
                    .with_input(Port::float("degrees", 180.0))
                    .with_input(Port::string("type", "pie"))
            )
            .with_rendered_child("arc1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);
    }

    #[test]
    fn test_evaluate_translate() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            )
            .with_child(
                Node::new("translate1")
                    .with_prototype("corevector.translate")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::point("translate", Point::new(100.0, 50.0)))
            )
            .with_connection(Connection::new("ellipse1", "translate1", "shape"))
            .with_rendered_child("translate1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        // Original ellipse centered at (0,0) translated by (100, 50)
        // Center should now be at (100, 50)
        let center_x = bounds.x + bounds.width / 2.0;
        let center_y = bounds.y + bounds.height / 2.0;
        assert!((center_x - 100.0).abs() < 1.0);
        assert!((center_y - 50.0).abs() < 1.0);
    }

    #[test]
    fn test_evaluate_scale() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0))
            )
            .with_child(
                Node::new("scale1")
                    .with_prototype("corevector.scale")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::point("scale", Point::new(50.0, 200.0))) // 50% x, 200% y
                    .with_input(Port::point("origin", Point::ZERO))
            )
            .with_connection(Connection::new("ellipse1", "scale1", "shape"))
            .with_rendered_child("scale1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        // Width should be 50, height should be 200
        assert!((bounds.width - 50.0).abs() < 1.0);
        assert!((bounds.height - 200.0).abs() < 1.0);
    }

    #[test]
    fn test_evaluate_copy() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            )
            .with_child(
                Node::new("copy1")
                    .with_prototype("corevector.copy")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::int("copies", 3))
                    .with_input(Port::string("order", "tsr"))
                    .with_input(Port::point("translate", Point::new(60.0, 0.0)))
                    .with_input(Port::float("rotate", 0.0))
                    .with_input(Port::point("scale", Point::new(100.0, 100.0)))
            )
            .with_connection(Connection::new("ellipse1", "copy1", "shape"))
            .with_rendered_child("copy1");

        let paths = evaluate_network(&library);
        // Should have 3 copies
        assert_eq!(paths.len(), 3);
    }

    #[test]
    fn test_evaluate_empty_network() {
        let library = NodeLibrary::new("test");
        let paths = evaluate_network(&library);
        assert!(paths.is_empty());
    }

    #[test]
    fn test_evaluate_no_rendered_child() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            );
        // No rendered_child set

        let paths = evaluate_network(&library);
        assert!(paths.is_empty());
    }

    #[test]
    fn test_evaluate_colorize_without_input() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("colorize1")
                    .with_prototype("corevector.colorize")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::color("fill", Color::rgb(1.0, 0.0, 0.0)))
                    .with_input(Port::color("stroke", Color::BLACK))
                    .with_input(Port::float("strokeWidth", 2.0))
            )
            .with_rendered_child("colorize1");

        // Should handle missing input gracefully
        let paths = evaluate_network(&library);
        assert!(paths.is_empty());
    }

    #[test]
    fn test_evaluate_unknown_node_type() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("unknown1")
                    .with_prototype("corevector.nonexistent")
            )
            .with_rendered_child("unknown1");

        // Should handle unknown node type gracefully
        let paths = evaluate_network(&library);
        assert!(paths.is_empty());
    }

    #[test]
    fn test_evaluate_resample() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::ZERO))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0))
            )
            .with_child(
                Node::new("resample1")
                    .with_prototype("corevector.resample")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::int("points", 20))
            )
            .with_connection(Connection::new("ellipse1", "resample1", "shape"))
            .with_rendered_child("resample1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);
        // Resampled path should have the specified number of points
        // Note: exact point count depends on implementation
    }

    #[test]
    fn test_evaluate_grid() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("grid1")
                    .with_prototype("corevector.grid")
                    .with_input(Port::int("columns", 3))
                    .with_input(Port::int("rows", 3))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0))
                    .with_input(Port::point("position", Point::ZERO))
            )
            .with_child(
                Node::new("connect1")
                    .with_prototype("corevector.connect")
                    .with_input(Port::geometry("points"))
                    .with_input(Port::boolean("closed", false))
            )
            .with_connection(Connection::new("grid1", "connect1", "points"))
            .with_rendered_child("connect1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);
    }

    // =========================================================================
    // Tests for correct port names (matching corevector.ndbx library)
    // These tests verify that nodes use "position" (Point) instead of x/y
    // =========================================================================

    #[test]
    fn test_ellipse_with_position_port() {
        // According to corevector.ndbx, ellipse should use "position" (Point), not x/y
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::new(100.0, 50.0)))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            )
            .with_rendered_child("ellipse1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        // Ellipse centered at (100, 50) with width/height 50
        // Bounds should be approximately (75, 25) to (125, 75)
        let center_x = bounds.x + bounds.width / 2.0;
        let center_y = bounds.y + bounds.height / 2.0;
        assert!((center_x - 100.0).abs() < 1.0, "Center X should be 100, got {}", center_x);
        assert!((center_y - 50.0).abs() < 1.0, "Center Y should be 50, got {}", center_y);
    }

    #[test]
    fn test_rect_with_position_port() {
        // According to corevector.ndbx, rect should use "position" (Point), not x/y
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("rect1")
                    .with_prototype("corevector.rect")
                    .with_input(Port::point("position", Point::new(-50.0, 25.0)))
                    .with_input(Port::float("width", 80.0))
                    .with_input(Port::float("height", 40.0))
            )
            .with_rendered_child("rect1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        let center_x = bounds.x + bounds.width / 2.0;
        let center_y = bounds.y + bounds.height / 2.0;
        assert!((center_x - (-50.0)).abs() < 1.0, "Center X should be -50, got {}", center_x);
        assert!((center_y - 25.0).abs() < 1.0, "Center Y should be 25, got {}", center_y);
    }

    #[test]
    fn test_rect_with_roundness_port() {
        // According to corevector.ndbx, rect should use "roundness" (Point), not rx/ry
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("rect1")
                    .with_prototype("corevector.rect")
                    .with_input(Port::point("position", Point::new(0.0, 0.0)))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0))
                    .with_input(Port::point("roundness", Point::new(10.0, 10.0)))
            )
            .with_rendered_child("rect1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);
        // If roundness is applied, the path should have more points than a simple rect
    }

    #[test]
    fn test_polygon_with_position_port() {
        // According to corevector.ndbx, polygon should use "position" (Point), not x/y
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("polygon1")
                    .with_prototype("corevector.polygon")
                    .with_input(Port::point("position", Point::new(200.0, -100.0)))
                    .with_input(Port::float("radius", 50.0))
                    .with_input(Port::int("sides", 6))
                    .with_input(Port::boolean("align", true))
            )
            .with_rendered_child("polygon1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        let center_x = bounds.x + bounds.width / 2.0;
        let center_y = bounds.y + bounds.height / 2.0;
        assert!((center_x - 200.0).abs() < 1.0, "Center X should be 200, got {}", center_x);
        assert!((center_y - (-100.0)).abs() < 1.0, "Center Y should be -100, got {}", center_y);
    }

    #[test]
    fn test_star_with_position_port() {
        // According to corevector.ndbx, star should use "position" (Point), not x/y
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("star1")
                    .with_prototype("corevector.star")
                    .with_input(Port::point("position", Point::new(75.0, 75.0)))
                    .with_input(Port::int("points", 5))
                    .with_input(Port::float("outer", 50.0))
                    .with_input(Port::float("inner", 25.0))
            )
            .with_rendered_child("star1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        let center_x = bounds.x + bounds.width / 2.0;
        let center_y = bounds.y + bounds.height / 2.0;
        // Star geometry may not be perfectly symmetric, allow some tolerance
        assert!((center_x - 75.0).abs() < 10.0, "Center X should be near 75, got {}", center_x);
        assert!((center_y - 75.0).abs() < 10.0, "Center Y should be near 75, got {}", center_y);
    }

    #[test]
    fn test_arc_with_position_and_start_angle() {
        // According to corevector.ndbx, arc uses "position" and "start_angle" (underscore)
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("arc1")
                    .with_prototype("corevector.arc")
                    .with_input(Port::point("position", Point::new(50.0, -50.0)))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0))
                    .with_input(Port::float("start_angle", 0.0))
                    .with_input(Port::float("degrees", 180.0))
                    .with_input(Port::string("type", "pie"))
            )
            .with_rendered_child("arc1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        let center_x = bounds.x + bounds.width / 2.0;
        // Arc center should be near (50, -50)
        assert!((center_x - 50.0).abs() < 10.0, "Center X should be near 50, got {}", center_x);
    }

    #[test]
    fn test_copy_with_translate_and_scale_points() {
        // According to corevector.ndbx, copy uses "translate" (Point) and "scale" (Point)
        // not tx/ty and sx/sy
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::new(0.0, 0.0)))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
            )
            .with_child(
                Node::new("copy1")
                    .with_prototype("corevector.copy")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::int("copies", 3))
                    .with_input(Port::string("order", "tsr"))
                    .with_input(Port::point("translate", Point::new(60.0, 0.0)))
                    .with_input(Port::float("rotate", 0.0))
                    .with_input(Port::point("scale", Point::new(100.0, 100.0)))
            )
            .with_connection(Connection::new("ellipse1", "copy1", "shape"))
            .with_rendered_child("copy1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 3, "Should have 3 copies");

        // First copy at x=0, second at x=60, third at x=120
        // Check that copies are actually spread out
        let bounds0 = paths[0].bounds().unwrap();
        let bounds2 = paths[2].bounds().unwrap();
        let center0_x = bounds0.x + bounds0.width / 2.0;
        let center2_x = bounds2.x + bounds2.width / 2.0;
        assert!((center2_x - center0_x - 120.0).abs() < 1.0,
            "Third copy should be 120 units from first, got {}", center2_x - center0_x);
    }

    #[test]
    fn test_grid_with_position_port() {
        // According to corevector.ndbx, grid uses "position" (Point), not x/y
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("grid1")
                    .with_prototype("corevector.grid")
                    .with_input(Port::int("columns", 3))
                    .with_input(Port::int("rows", 3))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0))
                    .with_input(Port::point("position", Point::new(50.0, 50.0)))
            )
            .with_child(
                Node::new("connect1")
                    .with_prototype("corevector.connect")
                    .with_input(Port::geometry("points"))
                    .with_input(Port::boolean("closed", false))
            )
            .with_connection(Connection::new("grid1", "connect1", "points"))
            .with_rendered_child("connect1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        let bounds = paths[0].bounds().unwrap();
        let center_x = bounds.x + bounds.width / 2.0;
        let center_y = bounds.y + bounds.height / 2.0;
        assert!((center_x - 50.0).abs() < 1.0, "Center X should be 50, got {}", center_x);
        assert!((center_y - 50.0).abs() < 1.0, "Center Y should be 50, got {}", center_y);
    }

    #[test]
    fn test_wiggle_with_offset_point() {
        // According to corevector.ndbx, wiggle uses "offset" (Point), not offsetX/offsetY
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::new(0.0, 0.0)))
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 100.0))
            )
            .with_child(
                Node::new("wiggle1")
                    .with_prototype("corevector.wiggle")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::string("scope", "points"))
                    .with_input(Port::point("offset", Point::new(10.0, 10.0)))
                    .with_input(Port::int("seed", 42))
            )
            .with_connection(Connection::new("ellipse1", "wiggle1", "shape"))
            .with_rendered_child("wiggle1");

        let paths = evaluate_network(&library);
        assert!(!paths.is_empty(), "Wiggle should produce output");
    }

    #[test]
    fn test_fit_with_position_and_keep_proportions() {
        // According to corevector.ndbx, fit uses "position" (Point) and "keep_proportions"
        // Test that fit reads from position port (not x/y) and keep_proportions (not keepProportions)
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("ellipse1")
                    .with_prototype("corevector.ellipse")
                    .with_input(Port::point("position", Point::new(0.0, 0.0)))
                    .with_input(Port::float("width", 200.0))
                    .with_input(Port::float("height", 100.0))
            )
            .with_child(
                Node::new("fit1")
                    .with_prototype("corevector.fit")
                    .with_input(Port::geometry("shape"))
                    .with_input(Port::point("position", Point::new(100.0, 100.0)))
                    .with_input(Port::float("width", 50.0))
                    .with_input(Port::float("height", 50.0))
                    .with_input(Port::boolean("keep_proportions", true))
            )
            .with_connection(Connection::new("ellipse1", "fit1", "shape"))
            .with_rendered_child("fit1");

        let paths = evaluate_network(&library);
        assert_eq!(paths.len(), 1);

        // Verify fit produced output - the shape should be constrained to max 50x50
        // With keep_proportions=true and input 200x100, output should be 50x25
        let bounds = paths[0].bounds().unwrap();
        assert!(bounds.width <= 51.0, "Width should be at most 50, got {}", bounds.width);
        assert!(bounds.height <= 51.0, "Height should be at most 50, got {}", bounds.height);
    }

    #[test]
    fn test_node_output_conversions() {
        // Test to_paths()
        let path = Path::new();
        let output = NodeOutput::Path(path.clone());
        assert_eq!(output.to_paths().len(), 1);

        let output = NodeOutput::Paths(vec![path.clone(), path.clone()]);
        assert_eq!(output.to_paths().len(), 2);

        let output = NodeOutput::Float(1.0);
        assert!(output.to_paths().is_empty());

        // Test as_path()
        let output = NodeOutput::Path(path.clone());
        assert!(output.as_path().is_some());

        let output = NodeOutput::Float(1.0);
        assert!(output.as_path().is_none());

        // Test as_paths()
        let output = NodeOutput::Path(path.clone());
        assert!(output.as_paths().is_some());
        assert_eq!(output.as_paths().unwrap().len(), 1);

        let output = NodeOutput::Paths(vec![path.clone(), path.clone()]);
        assert!(output.as_paths().is_some());
        assert_eq!(output.as_paths().unwrap().len(), 2);

        let output = NodeOutput::Float(1.0);
        assert!(output.as_paths().is_none());
    }
}
