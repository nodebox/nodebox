//! XML serializer for .ndbx files.
//!
//! This module converts NodeLibrary data structures back to XML format.

use std::fs;
use std::io::Write;
use std::path::Path;

use crate::node::{Connection, MenuItem, Node, NodeLibrary, Port, PortRange, PortType, Widget};
use crate::Value;

use super::error::Result;
use super::upgrades::CURRENT_FORMAT_VERSION;

/// Serializes a NodeLibrary to an XML string.
pub fn serialize(library: &NodeLibrary) -> String {
    let mut output = String::new();
    output.push_str(r#"<?xml version="1.0" encoding="UTF-8"?>"#);
    output.push('\n');

    // Start ndbx element
    output.push_str(&format!(
        r#"<ndbx formatVersion="{}" type="file""#,
        CURRENT_FORMAT_VERSION
    ));

    if let Some(uuid) = &library.uuid {
        output.push_str(&format!(r#" uuid="{}""#, escape_xml(uuid)));
    }

    output.push_str(">\n");

    // Write properties (sorted for deterministic output)
    let mut properties: Vec<_> = library.properties.iter().collect();
    properties.sort_by_key(|(k, _)| *k);

    for (name, value) in properties {
        // Skip link properties and the "type" property (it's in the ndbx element)
        if name.starts_with("link.") || name == "type" {
            continue;
        }
        output.push_str(&format!(
            r#"    <property name="{}" value="{}"/>"#,
            escape_xml(name),
            escape_xml(value)
        ));
        output.push('\n');
    }

    // Write root node
    write_node(&mut output, &library.root, 1);

    output.push_str("</ndbx>\n");

    output
}

/// Serializes a NodeLibrary to a file.
pub fn serialize_to_file(library: &NodeLibrary, path: impl AsRef<Path>) -> Result<()> {
    let content = serialize(library);
    let mut file = fs::File::create(path)?;
    file.write_all(content.as_bytes())?;
    Ok(())
}

fn write_node(output: &mut String, node: &Node, indent: usize) {
    let indent_str = "    ".repeat(indent);

    // Start node element
    output.push_str(&indent_str);
    output.push_str(&format!(r#"<node name="{}""#, escape_xml(&node.name)));

    // Write prototype
    if let Some(prototype) = &node.prototype {
        output.push_str(&format!(r#" prototype="{}""#, escape_xml(prototype)));
    }

    // Write function (if different from prototype-inferred)
    if let Some(function) = &node.function {
        output.push_str(&format!(r#" function="{}""#, escape_xml(function)));
    }

    // Write position (if non-zero)
    if node.position.x != 0.0 || node.position.y != 0.0 {
        output.push_str(&format!(
            r#" position="{:.2},{:.2}""#,
            node.position.x, node.position.y
        ));
    }

    // Write category (if non-empty)
    if !node.category.is_empty() {
        output.push_str(&format!(r#" category="{}""#, escape_xml(&node.category)));
    }

    // Write description
    if let Some(desc) = &node.description {
        output.push_str(&format!(r#" description="{}""#, escape_xml(desc)));
    }

    // Write output type (if not default Geometry)
    if node.output_type != PortType::Geometry {
        output.push_str(&format!(r#" outputType="{}""#, format_port_type(&node.output_type)));
    }

    // Write output range (if list)
    if node.output_range == PortRange::List && !node.is_network {
        output.push_str(r#" outputRange="list""#);
    }

    // Write rendered child
    if let Some(rendered) = &node.rendered_child {
        output.push_str(&format!(r#" renderedChild="{}""#, escape_xml(rendered)));
    }

    // Write handle
    if let Some(handle) = &node.handle {
        output.push_str(&format!(r#" handle="{}""#, escape_xml(handle)));
    }

    // Write alwaysRendered
    if node.always_rendered {
        output.push_str(r#" alwaysRendered="true""#);
    }

    // Check if node has content
    let has_content = !node.inputs.is_empty() || !node.children.is_empty() || !node.connections.is_empty();

    if has_content {
        output.push_str(">\n");

        // Write input ports
        for port in &node.inputs {
            write_port(output, port, indent + 1);
        }

        // Write child nodes
        for child in &node.children {
            write_node(output, child, indent + 1);
        }

        // Write connections
        for conn in &node.connections {
            write_connection(output, conn, indent + 1);
        }

        output.push_str(&indent_str);
        output.push_str("</node>\n");
    } else {
        output.push_str("/>\n");
    }
}

fn write_port(output: &mut String, port: &Port, indent: usize) {
    let indent_str = "    ".repeat(indent);

    output.push_str(&indent_str);
    output.push_str(&format!(r#"<port name="{}""#, escape_xml(&port.name)));

    // Write type
    output.push_str(&format!(r#" type="{}""#, format_port_type(&port.port_type)));

    // Write value (if not null/default)
    if !port.value.is_null() {
        if let Some(value_str) = format_value(&port.value, &port.port_type) {
            output.push_str(&format!(r#" value="{}""#, escape_xml(&value_str)));
        }
    }

    // Write range (if list)
    if port.range == PortRange::List {
        output.push_str(r#" range="list""#);
    }

    // Write widget (if not default for type)
    let default_widget = Widget::default_for_type(&port.port_type);
    if port.widget != default_widget && port.widget != Widget::None {
        output.push_str(&format!(r#" widget="{}""#, format_widget(&port.widget)));
    }

    // Write min/max
    if let Some(min) = port.min {
        output.push_str(&format!(r#" min="{}""#, min));
    }
    if let Some(max) = port.max {
        output.push_str(&format!(r#" max="{}""#, max));
    }

    // Write label
    if let Some(label) = &port.label {
        output.push_str(&format!(r#" label="{}""#, escape_xml(label)));
    }

    // Write description
    if let Some(desc) = &port.description {
        output.push_str(&format!(r#" description="{}""#, escape_xml(desc)));
    }

    // Check if port has menu items
    if !port.menu_items.is_empty() {
        output.push_str(">\n");

        for item in &port.menu_items {
            write_menu_item(output, item, indent + 1);
        }

        output.push_str(&indent_str);
        output.push_str("</port>\n");
    } else {
        output.push_str("/>\n");
    }
}

fn write_menu_item(output: &mut String, item: &MenuItem, indent: usize) {
    let indent_str = "    ".repeat(indent);
    output.push_str(&indent_str);
    output.push_str(&format!(
        r#"<menu key="{}" label="{}"/>"#,
        escape_xml(&item.key),
        escape_xml(&item.label)
    ));
    output.push('\n');
}

fn write_connection(output: &mut String, conn: &Connection, indent: usize) {
    let indent_str = "    ".repeat(indent);
    output.push_str(&indent_str);
    output.push_str(&format!(
        r#"<conn input="{}.{}" output="{}"/>"#,
        escape_xml(&conn.input_node),
        escape_xml(&conn.input_port),
        escape_xml(&conn.output_node)
    ));
    output.push('\n');
}

fn format_port_type(port_type: &PortType) -> &str {
    port_type.as_str()
}

fn format_widget(widget: &Widget) -> &str {
    match widget {
        Widget::None => "none",
        Widget::Int => "int",
        Widget::Float => "float",
        Widget::Angle => "angle",
        Widget::String => "string",
        Widget::Text => "text",
        Widget::Password => "password",
        Widget::Toggle => "toggle",
        Widget::Color => "color",
        Widget::Point => "point",
        Widget::Menu => "menu",
        Widget::File => "file",
        Widget::Font => "font",
        Widget::Image => "image",
        Widget::Data => "data",
        Widget::Seed => "seed",
        Widget::Gradient => "gradient",
    }
}

fn format_value(value: &Value, _port_type: &PortType) -> Option<String> {
    match value {
        Value::Null => None,
        Value::Int(i) => Some(i.to_string()),
        Value::Float(f) => Some(format_float(*f)),
        Value::String(s) => Some(s.clone()),
        Value::Boolean(b) => Some(if *b { "true".to_string() } else { "false".to_string() }),
        Value::Point(p) => Some(format!("{:.2},{:.2}", p.x, p.y)),
        Value::Color(c) => Some(c.to_hex().to_lowercase()),
        Value::Path(_) | Value::Geometry(_) | Value::List(_) | Value::Map(_) => None,
    }
}

fn format_float(f: f64) -> String {
    // Format floats consistently - use decimal point, avoid scientific notation
    if f.fract() == 0.0 {
        format!("{:.1}", f)
    } else {
        // Remove trailing zeros but keep at least one decimal place
        let s = format!("{:.6}", f);
        let s = s.trim_end_matches('0');
        if s.ends_with('.') {
            format!("{}0", s)
        } else {
            s.to_string()
        }
    }
}

fn escape_xml(s: &str) -> String {
    s.replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
        .replace('"', "&quot;")
        .replace('\'', "&apos;")
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::geometry::{Color, Point};
    use crate::node::Connection;

    #[test]
    fn test_serialize_empty_library() {
        let library = NodeLibrary::new("test");
        let xml = serialize(&library);

        assert!(xml.contains(r#"formatVersion="22""#));
        assert!(xml.contains(r#"<node name="root""#));
    }

    #[test]
    fn test_serialize_with_uuid() {
        let mut library = NodeLibrary::new("test");
        library.uuid = Some("test-uuid-123".to_string());
        let xml = serialize(&library);

        assert!(xml.contains(r#"uuid="test-uuid-123""#));
    }

    #[test]
    fn test_serialize_with_properties() {
        let mut library = NodeLibrary::new("test");
        library.set_width(800.0);
        library.set_height(600.0);
        let xml = serialize(&library);

        assert!(xml.contains(r#"<property name="canvasHeight" value="600"/>"#));
        assert!(xml.contains(r#"<property name="canvasWidth" value="800"/>"#));
    }

    #[test]
    fn test_serialize_node_with_position() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("rect1")
                    .with_prototype("corevector.rect")
                    .with_position(1.5, 2.5)
            );
        let xml = serialize(&library);

        assert!(xml.contains(r#"name="rect1""#));
        assert!(xml.contains(r#"prototype="corevector.rect""#));
        assert!(xml.contains(r#"position="1.50,2.50""#));
    }

    #[test]
    fn test_serialize_port_with_float_value() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("rect1")
                    .with_prototype("corevector.rect")
                    .with_input(Port::float("width", 100.0))
            );
        let xml = serialize(&library);

        assert!(xml.contains(r#"<port name="width" type="float" value="100.0"/>"#));
    }

    #[test]
    fn test_serialize_port_with_color_value() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("colorize1")
                    .with_prototype("corevector.colorize")
                    .with_input(Port::color("fill", Color::rgba(1.0, 0.0, 0.0, 1.0)))
            );
        let xml = serialize(&library);

        assert!(xml.contains(r#"type="color""#));
        assert!(xml.contains("value=\"#ff0000ff\""));
    }

    #[test]
    fn test_serialize_port_with_point_value() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("rect1")
                    .with_input(Port::point("position", Point::new(10.5, -20.25)))
            );
        let xml = serialize(&library);

        assert!(xml.contains(r#"type="point""#));
        assert!(xml.contains(r#"value="10.50,-20.25""#));
    }

    #[test]
    fn test_serialize_port_with_boolean_value() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(
                Node::new("connect1")
                    .with_input(Port::boolean("closed", true))
            );
        let xml = serialize(&library);

        assert!(xml.contains(r#"type="boolean""#));
        assert!(xml.contains(r#"value="true""#));
    }

    #[test]
    fn test_serialize_connection() {
        let mut library = NodeLibrary::new("test");
        library.root = Node::network("root")
            .with_child(Node::new("rect1"))
            .with_child(Node::new("colorize1"))
            .with_connection(Connection::new("rect1", "colorize1", "shape"))
            .with_rendered_child("colorize1");
        let xml = serialize(&library);

        assert!(xml.contains(r#"<conn input="colorize1.shape" output="rect1"/>"#));
        assert!(xml.contains(r#"renderedChild="colorize1""#));
    }

    #[test]
    fn test_serialize_port_with_menu() {
        let mut library = NodeLibrary::new("test");
        let mut port = Port::string("halign", "center");
        port.widget = Widget::Menu;
        port.menu_items = vec![
            MenuItem::new("left", "Left"),
            MenuItem::new("center", "Center"),
            MenuItem::new("right", "Right"),
        ];

        library.root = Node::network("root")
            .with_child(
                Node::new("align1")
                    .with_input(port)
            );
        let xml = serialize(&library);

        assert!(xml.contains(r#"widget="menu""#));
        assert!(xml.contains(r#"<menu key="left" label="Left"/>"#));
        assert!(xml.contains(r#"<menu key="center" label="Center"/>"#));
    }

    #[test]
    fn test_escape_xml() {
        assert_eq!(escape_xml("hello"), "hello");
        assert_eq!(escape_xml("<>&\"'"), "&lt;&gt;&amp;&quot;&apos;");
    }

    #[test]
    fn test_format_float() {
        assert_eq!(format_float(100.0), "100.0");
        assert_eq!(format_float(10.5), "10.5");
        assert_eq!(format_float(3.14159), "3.14159");
    }

    #[test]
    fn test_round_trip() {
        use crate::ndbx::parse;

        // Create a library with various node types and properties
        let mut original = NodeLibrary::new("test");
        original.uuid = Some("test-uuid-456".to_string());
        original.format_version = 22;
        original.set_width(800.0);
        original.set_height(600.0);

        original.root = Node::network("root")
            .with_child(
                Node::new("rect1")
                    .with_prototype("corevector.rect")
                    .with_position(1.0, 2.0)
                    .with_input(Port::float("width", 100.0))
                    .with_input(Port::float("height", 50.0))
                    .with_input(Port::point("position", Point::new(-50.0, 25.0)))
            )
            .with_child(
                Node::new("colorize1")
                    .with_prototype("corevector.colorize")
                    .with_position(1.0, 4.0)
                    .with_input(Port::color("fill", Color::rgba(1.0, 0.5, 0.0, 1.0)))
                    .with_input(Port::boolean("enabled", true))
            )
            .with_connection(Connection::new("rect1", "colorize1", "shape"))
            .with_rendered_child("colorize1");

        // Serialize to XML
        let xml = serialize(&original);

        // Parse back
        let parsed = parse(&xml).expect("Failed to parse serialized XML");

        // Verify key properties
        assert_eq!(parsed.format_version, 22);
        assert_eq!(parsed.uuid, Some("test-uuid-456".to_string()));
        assert_eq!(parsed.width(), 800.0);
        assert_eq!(parsed.height(), 600.0);

        // Verify root node
        assert_eq!(parsed.root.name, "root");
        assert_eq!(parsed.root.rendered_child, Some("colorize1".to_string()));
        assert_eq!(parsed.root.children.len(), 2);
        assert_eq!(parsed.root.connections.len(), 1);

        // Verify rect1 node
        let rect = parsed.root.child("rect1").expect("Missing rect1");
        assert_eq!(rect.prototype, Some("corevector.rect".to_string()));
        assert_eq!(rect.position.x, 1.0);
        assert_eq!(rect.position.y, 2.0);
        assert_eq!(rect.inputs.len(), 3);

        let width_port = rect.input("width").expect("Missing width port");
        assert_eq!(width_port.value.as_float(), Some(100.0));

        // Verify colorize1 node
        let colorize = parsed.root.child("colorize1").expect("Missing colorize1");
        let fill_port = colorize.input("fill").expect("Missing fill port");
        if let Value::Color(c) = &fill_port.value {
            assert!((c.r - 1.0).abs() < 0.01);
            assert!((c.g - 0.5).abs() < 0.01);
            assert!((c.b - 0.0).abs() < 0.01);
        } else {
            panic!("Expected color value for fill port");
        }

        // Verify connection
        let conn = &parsed.root.connections[0];
        assert_eq!(conn.output_node, "rect1");
        assert_eq!(conn.input_node, "colorize1");
        assert_eq!(conn.input_port, "shape");
    }
}
