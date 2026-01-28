//! NDBX file parser.

use std::fs;
use std::path::Path;

use quick_xml::events::{BytesStart, Event};
use quick_xml::Reader;

use nodebox_core::node::{Connection, MenuItem, Node, NodeLibrary, Port, PortRange, PortType, Widget};
use nodebox_core::geometry::Point;
use nodebox_core::Value;

use crate::error::{NdbxError, Result};

/// Parses an NDBX file from the given path.
pub fn parse_file(path: impl AsRef<Path>) -> Result<NodeLibrary> {
    let content = fs::read_to_string(path)?;
    parse(&content)
}

/// Parses NDBX content from a string.
pub fn parse(content: &str) -> Result<NodeLibrary> {
    let mut reader = Reader::from_str(content);
    reader.trim_text(true);

    let mut library = NodeLibrary::default();
    let mut buf = Vec::new();

    loop {
        match reader.read_event_into(&mut buf) {
            Ok(Event::Start(ref e)) => {
                if e.name().as_ref() == b"ndbx" {
                    parse_ndbx_element(&mut reader, e, &mut library)?;
                }
            }
            Ok(Event::Eof) => break,
            Err(e) => return Err(e.into()),
            _ => {}
        }
        buf.clear();
    }

    Ok(library)
}

fn parse_ndbx_element(
    reader: &mut Reader<&[u8]>,
    start: &BytesStart,
    library: &mut NodeLibrary,
) -> Result<()> {
    // Parse ndbx attributes
    for attr in start.attributes().flatten() {
        let key = std::str::from_utf8(attr.key.as_ref())?;
        let value = std::str::from_utf8(&attr.value)?;

        match key {
            "formatVersion" => {
                // Handle both integer and float format versions (older files had "0.9" etc.)
                library.format_version = if let Ok(v) = value.parse::<u32>() {
                    v
                } else if let Ok(v) = value.parse::<f64>() {
                    // Convert float versions like 0.9 to 0
                    v.floor() as u32
                } else {
                    return Err(NdbxError::parse_value(value, "u32", "invalid format version"));
                };
            }
            "uuid" => {
                library.uuid = Some(value.to_string());
            }
            "type" => {
                // Store type in properties for now
                library.properties.insert("type".to_string(), value.to_string());
            }
            _ => {}
        }
    }

    let mut buf = Vec::new();
    loop {
        match reader.read_event_into(&mut buf) {
            Ok(Event::Start(ref e)) => {
                let name = e.name();
                match name.as_ref() {
                    b"property" => {
                        parse_property(e, library)?;
                    }
                    b"link" => {
                        parse_link(e, library)?;
                    }
                    b"node" => {
                        let node = parse_node(reader, e)?;
                        library.root = node;
                    }
                    _ => {}
                }
            }
            Ok(Event::Empty(ref e)) => {
                let name = e.name();
                match name.as_ref() {
                    b"property" => {
                        parse_property(e, library)?;
                    }
                    b"link" => {
                        parse_link(e, library)?;
                    }
                    _ => {}
                }
            }
            Ok(Event::End(ref e)) => {
                if e.name().as_ref() == b"ndbx" {
                    break;
                }
            }
            Ok(Event::Eof) => break,
            Err(e) => return Err(e.into()),
            _ => {}
        }
        buf.clear();
    }

    Ok(())
}

fn parse_property(e: &BytesStart, library: &mut NodeLibrary) -> Result<()> {
    let mut name = String::new();
    let mut value = String::new();

    for attr in e.attributes().flatten() {
        let key = std::str::from_utf8(attr.key.as_ref())?;
        let val = std::str::from_utf8(&attr.value)?;

        match key {
            "name" => name = val.to_string(),
            "value" => value = val.to_string(),
            _ => {}
        }
    }

    if !name.is_empty() {
        library.properties.insert(name, value);
    }

    Ok(())
}

fn parse_link(e: &BytesStart, library: &mut NodeLibrary) -> Result<()> {
    let mut href = String::new();
    let mut rel = String::new();

    for attr in e.attributes().flatten() {
        let key = std::str::from_utf8(attr.key.as_ref())?;
        let val = std::str::from_utf8(&attr.value)?;

        match key {
            "href" => href = val.to_string(),
            "rel" => rel = val.to_string(),
            _ => {}
        }
    }

    // Store links as properties with prefix
    if !href.is_empty() {
        let key = format!("link.{}.{}", rel, href.replace(['/', ':'], "_"));
        library.properties.insert(key, href);
    }

    Ok(())
}

fn parse_node(reader: &mut Reader<&[u8]>, start: &BytesStart) -> Result<Node> {
    let mut node = parse_node_attributes(start)?;

    let mut buf = Vec::new();
    loop {
        match reader.read_event_into(&mut buf) {
            Ok(Event::Start(ref e)) => {
                let name = e.name();
                match name.as_ref() {
                    b"node" => {
                        let child = parse_node(reader, e)?;
                        node.children.push(child);
                    }
                    b"port" => {
                        let port = parse_port(reader, e)?;
                        node.inputs.push(port);
                    }
                    _ => {
                        // Skip unknown elements
                        skip_element(reader, &name)?;
                    }
                }
            }
            Ok(Event::Empty(ref e)) => {
                let name = e.name();
                match name.as_ref() {
                    b"node" => {
                        let child = parse_node_attributes(e)?;
                        node.children.push(child);
                    }
                    b"port" => {
                        let port = parse_port_attributes(e)?;
                        node.inputs.push(port);
                    }
                    b"conn" => {
                        if let Some(conn) = parse_connection(e)? {
                            node.connections.push(conn);
                        }
                    }
                    _ => {}
                }
            }
            Ok(Event::End(ref e)) => {
                if e.name().as_ref() == b"node" {
                    break;
                }
            }
            Ok(Event::Eof) => break,
            Err(e) => return Err(e.into()),
            _ => {}
        }
        buf.clear();
    }

    Ok(node)
}

fn parse_node_attributes(e: &BytesStart) -> Result<Node> {
    let mut node = Node::default();

    for attr in e.attributes().flatten() {
        let key = std::str::from_utf8(attr.key.as_ref())?;
        let value = std::str::from_utf8(&attr.value)?;

        match key {
            "name" => node.name = value.to_string(),
            "prototype" => node.prototype = Some(value.to_string()),
            "function" => node.function = Some(value.to_string()),
            "category" => node.category = value.to_string(),
            "description" => node.description = Some(value.to_string()),
            "position" => {
                node.position = parse_point(value)?;
            }
            "outputType" => {
                node.output_type = parse_port_type(value);
            }
            "outputRange" => {
                node.output_range = parse_port_range(value);
            }
            "renderedChild" => {
                node.rendered_child = Some(value.to_string());
            }
            "handle" => {
                node.handle = Some(value.to_string());
            }
            "alwaysRendered" => {
                node.always_rendered = value == "true";
            }
            "image" => {
                // Store image in a way that can be retrieved later
                // For now we'll skip it as it's UI-related
            }
            _ => {}
        }
    }

    // Nodes with children are networks
    // This will be set properly if we encounter child nodes
    if node.prototype.as_deref() == Some("core.network") {
        node.is_network = true;
    }

    Ok(node)
}

fn parse_port(reader: &mut Reader<&[u8]>, start: &BytesStart) -> Result<Port> {
    let mut port = parse_port_attributes(start)?;

    let mut buf = Vec::new();
    loop {
        match reader.read_event_into(&mut buf) {
            Ok(Event::Empty(ref e)) => {
                if e.name().as_ref() == b"menu" {
                    if let Some(item) = parse_menu_item(e)? {
                        port.menu_items.push(item);
                    }
                }
            }
            Ok(Event::End(ref e)) => {
                if e.name().as_ref() == b"port" {
                    break;
                }
            }
            Ok(Event::Eof) => break,
            Err(e) => return Err(e.into()),
            _ => {}
        }
        buf.clear();
    }

    Ok(port)
}

fn parse_port_attributes(e: &BytesStart) -> Result<Port> {
    let mut name = String::new();
    let mut port_type = PortType::Float;
    let mut range = PortRange::Value;
    let mut widget = None;
    let mut value_str = String::new();
    let mut min = None;
    let mut max = None;
    let mut label = None;
    let mut description = None;

    for attr in e.attributes().flatten() {
        let key = std::str::from_utf8(attr.key.as_ref())?;
        let val = std::str::from_utf8(&attr.value)?;

        match key {
            "name" => name = val.to_string(),
            "type" => port_type = parse_port_type(val),
            "range" => range = parse_port_range(val),
            "widget" => widget = Some(parse_widget(val)),
            "value" => value_str = val.to_string(),
            "min" => min = val.parse().ok(),
            "max" => max = val.parse().ok(),
            "label" => label = Some(val.to_string()),
            "description" => description = Some(val.to_string()),
            _ => {}
        }
    }

    // Parse the value based on type
    let value = parse_value(&value_str, &port_type);

    let mut port = Port {
        name,
        port_type,
        range,
        widget: widget.unwrap_or(Widget::None),
        value,
        min,
        max,
        label,
        description,
        menu_items: Vec::new(),
    };

    // If widget wasn't specified, infer from type
    if widget.is_none() {
        port.widget = Widget::default_for_type(&port.port_type);
    }

    Ok(port)
}

fn parse_menu_item(e: &BytesStart) -> Result<Option<MenuItem>> {
    let mut key = String::new();
    let mut label = String::new();

    for attr in e.attributes().flatten() {
        let k = std::str::from_utf8(attr.key.as_ref())?;
        let v = std::str::from_utf8(&attr.value)?;

        match k {
            "key" => key = v.to_string(),
            "label" => label = v.to_string(),
            _ => {}
        }
    }

    if key.is_empty() {
        return Ok(None);
    }

    Ok(Some(MenuItem { key, label }))
}

fn parse_connection(e: &BytesStart) -> Result<Option<Connection>> {
    let mut input = String::new();
    let mut output = String::new();

    for attr in e.attributes().flatten() {
        let key = std::str::from_utf8(attr.key.as_ref())?;
        let value = std::str::from_utf8(&attr.value)?;

        match key {
            "input" => input = value.to_string(),
            "output" => output = value.to_string(),
            _ => {}
        }
    }

    // Parse input="node.port" format
    if let Some((input_node, input_port)) = input.split_once('.') {
        Ok(Some(Connection::new(
            &output,
            input_node,
            input_port,
        )))
    } else {
        Ok(None)
    }
}

fn parse_point(s: &str) -> Result<Point> {
    let parts: Vec<&str> = s.split(',').collect();
    if parts.len() == 2 {
        let x = parts[0].trim().parse().unwrap_or(0.0);
        let y = parts[1].trim().parse().unwrap_or(0.0);
        Ok(Point::new(x, y))
    } else {
        Ok(Point::ZERO)
    }
}

fn parse_port_type(s: &str) -> PortType {
    match s.to_lowercase().as_str() {
        "int" => PortType::Int,
        "float" => PortType::Float,
        "string" => PortType::String,
        "boolean" | "bool" => PortType::Boolean,
        "point" => PortType::Point,
        "color" => PortType::Color,
        "geometry" => PortType::Geometry,
        "list" => PortType::List,
        "context" => PortType::Context,
        "state" => PortType::State,
        _ => PortType::Custom(s.to_string()),
    }
}

fn parse_port_range(s: &str) -> PortRange {
    match s.to_lowercase().as_str() {
        "value" => PortRange::Value,
        "list" => PortRange::List,
        _ => PortRange::Value,
    }
}

fn parse_widget(s: &str) -> Widget {
    match s.to_lowercase().as_str() {
        "int" => Widget::Int,
        "float" => Widget::Float,
        "angle" => Widget::Angle,
        "toggle" => Widget::Toggle,
        "color" => Widget::Color,
        "menu" => Widget::Menu,
        "file" => Widget::File,
        "font" => Widget::Font,
        "text" => Widget::Text,
        "code" => Widget::Text,
        "point" => Widget::Point,
        "data" => Widget::Data,
        "image" => Widget::Image,
        "seed" => Widget::Seed,
        "none" => Widget::None,
        _ => Widget::None,
    }
}

fn parse_value(s: &str, port_type: &PortType) -> Value {
    if s.is_empty() {
        return Value::Null;
    }

    match port_type {
        PortType::Int => s.parse().map(Value::Int).unwrap_or(Value::Null),
        PortType::Float => s.parse().map(Value::Float).unwrap_or(Value::Null),
        PortType::String => Value::String(s.to_string()),
        PortType::Boolean => Value::Boolean(s == "true"),
        PortType::Point => {
            parse_point(s)
                .map(Value::Point)
                .unwrap_or(Value::Null)
        }
        PortType::Color => {
            nodebox_core::Color::from_hex(s)
                .map(Value::Color)
                .unwrap_or(Value::Null)
        }
        _ => Value::String(s.to_string()),
    }
}

fn skip_element(reader: &mut Reader<&[u8]>, name: &quick_xml::name::QName) -> Result<()> {
    let mut depth = 1;
    let mut buf = Vec::new();

    loop {
        match reader.read_event_into(&mut buf) {
            Ok(Event::Start(_)) => depth += 1,
            Ok(Event::End(ref e)) => {
                depth -= 1;
                if depth == 0 && e.name() == *name {
                    break;
                }
            }
            Ok(Event::Eof) => break,
            Err(e) => return Err(e.into()),
            _ => {}
        }
        buf.clear();
    }

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_simple_ndbx() {
        let content = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test-uuid">
    <property name="canvasWidth" value="800"/>
    <property name="canvasHeight" value="600"/>
    <node name="root" prototype="core.network" renderedChild="rect1">
        <node name="rect1" position="1.00,2.00" prototype="corevector.rect">
            <port name="width" type="float" value="100.0"/>
            <port name="height" type="float" value="50.0"/>
        </node>
    </node>
</ndbx>"#;

        let library = parse(content).unwrap();

        assert_eq!(library.format_version, 17);
        assert_eq!(library.uuid, Some("test-uuid".to_string()));
        assert_eq!(library.canvas_width(), 800.0);
        assert_eq!(library.canvas_height(), 600.0);
        assert_eq!(library.root.name, "root");
        assert_eq!(library.root.rendered_child, Some("rect1".to_string()));
        assert_eq!(library.root.children.len(), 1);

        let rect = &library.root.children[0];
        assert_eq!(rect.name, "rect1");
        assert_eq!(rect.position, Point::new(1.0, 2.0));
        assert_eq!(rect.prototype, Some("corevector.rect".to_string()));
        assert_eq!(rect.inputs.len(), 2);
    }

    #[test]
    fn test_parse_connections() {
        let content = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network" renderedChild="colorize1">
        <node name="rect1" prototype="corevector.rect"/>
        <node name="colorize1" prototype="corevector.colorize"/>
        <conn input="colorize1.shape" output="rect1"/>
    </node>
</ndbx>"#;

        let library = parse(content).unwrap();
        assert_eq!(library.root.connections.len(), 1);

        let conn = &library.root.connections[0];
        assert_eq!(conn.output_node, "rect1");
        assert_eq!(conn.input_node, "colorize1");
        assert_eq!(conn.input_port, "shape");
    }

    #[test]
    fn test_parse_port_with_menu() {
        let content = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network">
        <node name="align1" prototype="corevector.align">
            <port name="halign" type="string" value="center" widget="menu">
                <menu key="left" label="Left"/>
                <menu key="center" label="Center"/>
                <menu key="right" label="Right"/>
            </port>
        </node>
    </node>
</ndbx>"#;

        let library = parse(content).unwrap();
        let align = &library.root.children[0];
        assert_eq!(align.inputs.len(), 1);

        let port = &align.inputs[0];
        assert_eq!(port.name, "halign");
        assert_eq!(port.widget, Widget::Menu);
        assert_eq!(port.menu_items.len(), 3);
        assert_eq!(port.menu_items[0].key, "left");
        assert_eq!(port.menu_items[1].label, "Center");
    }

    #[test]
    fn test_parse_color_value() {
        let content = r##"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network">
        <node name="colorize1" prototype="corevector.colorize">
            <port name="fill" type="color" value="#ff0000ff"/>
        </node>
    </node>
</ndbx>"##;

        let library = parse(content).unwrap();
        let colorize = &library.root.children[0];
        let fill = &colorize.inputs[0];

        if let Value::Color(c) = &fill.value {
            assert_eq!(c.r, 1.0);
            assert_eq!(c.g, 0.0);
            assert_eq!(c.b, 0.0);
            assert_eq!(c.a, 1.0);
        } else {
            panic!("Expected color value");
        }
    }

    #[test]
    fn test_parse_point_value() {
        let content = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network">
        <node name="rect1" prototype="corevector.rect">
            <port name="position" type="point" value="10.50,-20.25"/>
        </node>
    </node>
</ndbx>"#;

        let library = parse(content).unwrap();
        let rect = &library.root.children[0];
        let pos = &rect.inputs[0];

        if let Value::Point(p) = &pos.value {
            assert_eq!(p.x, 10.5);
            assert_eq!(p.y, -20.25);
        } else {
            panic!("Expected point value");
        }
    }

    #[test]
    fn test_parse_boolean_value() {
        let content = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network">
        <node name="connect1" prototype="corevector.connect">
            <port name="closed" type="boolean" value="true"/>
        </node>
    </node>
</ndbx>"#;

        let library = parse(content).unwrap();
        let connect = &library.root.children[0];
        let closed = &connect.inputs[0];

        assert_eq!(closed.value, Value::Boolean(true));
    }

    #[test]
    fn test_parse_empty_ndbx() {
        let content = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="21" type="file" uuid="empty">
    <node name="root" prototype="core.network"/>
</ndbx>"#;

        let library = parse(content).unwrap();
        assert_eq!(library.format_version, 21);
        assert_eq!(library.root.name, "root");
        assert!(library.root.children.is_empty());
    }

    #[test]
    fn test_parse_nested_networks() {
        let content = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network">
        <node name="subnet1" prototype="core.network">
            <node name="inner1" prototype="corevector.rect"/>
            <node name="inner2" prototype="corevector.ellipse"/>
        </node>
    </node>
</ndbx>"#;

        let library = parse(content).unwrap();
        assert_eq!(library.root.children.len(), 1);

        let subnet = &library.root.children[0];
        assert_eq!(subnet.name, "subnet1");
        assert!(subnet.is_network);
        assert_eq!(subnet.children.len(), 2);
        assert_eq!(subnet.children[0].name, "inner1");
        assert_eq!(subnet.children[1].name, "inner2");
    }
}
