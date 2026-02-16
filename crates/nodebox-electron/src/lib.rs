//! WASM bindings for the NodeBox Electron app.
//!
//! This crate provides a JavaScript-callable interface to the NodeBox engine,
//! wrapping `nodebox-core` and `nodebox-eval` types behind opaque handles
//! that communicate via JSON.

mod platform_bridge;

use std::sync::Arc;
use wasm_bindgen::prelude::*;
use nodebox_core::geometry::{Color, Point};
use nodebox_core::geometry::font;
use nodebox_core::node::{Connection, NodeLibrary, PortType};
use nodebox_core::platform::{Platform, ProjectContext};
use nodebox_core::Value;
use nodebox_eval::eval::{evaluate_network, NodeOutput};
use nodebox_eval::node_templates::NODE_TEMPLATES;
use nodebox_eval::node_factory::create_node_from_template;
use platform_bridge::WasmPlatform;

/// Initialize the WASM module (call once on startup).
#[wasm_bindgen]
pub fn init() {
    console_error_panic_hook::set_once();
}

/// Get all available node templates as a JSON array.
///
/// Returns JSON: `[{ "name": "ellipse", "prototype": "corevector.ellipse", "category": "geometry", "description": "..." }, ...]`
#[wasm_bindgen]
pub fn get_node_templates() -> String {
    let temp_lib = NodeLibrary::new("_temp");
    let templates: Vec<serde_json::Value> = NODE_TEMPLATES
        .iter()
        .map(|t| {
            let node = create_node_from_template(t, &temp_lib, Point::ZERO);
            let first_input_type: Option<serde_json::Value> = node
                .inputs
                .first()
                .and_then(|p| serde_json::to_value(&p.port_type).ok());
            serde_json::json!({
                "name": t.name,
                "prototype": t.prototype,
                "category": t.category,
                "description": t.description,
                "first_input_type": first_input_type,
            })
        })
        .collect();
    serde_json::to_string(&templates).unwrap_or_else(|_| "[]".to_string())
}

/// Create a node from a template and return it as JSON.
///
/// Takes the template name, the current library state as JSON (for unique naming),
/// and the desired grid position. Returns the full Node as JSON.
#[wasm_bindgen]
pub fn create_node(template_name: &str, library_json: &str, x: f64, y: f64) -> Result<String, JsError> {
    let template = NODE_TEMPLATES
        .iter()
        .find(|t| t.name == template_name)
        .ok_or_else(|| JsError::new(&format!("Unknown template: {}", template_name)))?;

    let library: NodeLibrary = serde_json::from_str(library_json)
        .map_err(|e| JsError::new(&format!("Failed to parse library: {}", e)))?;

    let position = Point::new(x, y);
    let node = create_node_from_template(template, &library, position);

    serde_json::to_string(&node)
        .map_err(|e| JsError::new(&format!("Failed to serialize node: {}", e)))
}

/// Opaque handle wrapping a `NodeLibrary`.
///
/// JavaScript code interacts with this through the provided methods.
/// The library state is fully owned by this handle.
#[wasm_bindgen]
pub struct WasmNodeLibrary {
    library: NodeLibrary,
}

#[wasm_bindgen]
impl WasmNodeLibrary {
    /// Create a new empty library.
    #[wasm_bindgen(constructor)]
    pub fn new() -> Self {
        Self {
            library: NodeLibrary::new("root"),
        }
    }

    /// Parse a library from NDBX (XML) content.
    #[wasm_bindgen]
    pub fn from_ndbx(ndbx_content: &str) -> Result<WasmNodeLibrary, JsError> {
        let mut library = nodebox_core::ndbx::parse(ndbx_content)
            .map_err(|e| JsError::new(&format!("Failed to parse NDBX: {}", e)))?;
        resolve_prototype_ports(&mut library);
        Ok(Self { library })
    }

    /// Serialize the library to NDBX (XML) format.
    #[wasm_bindgen]
    pub fn to_ndbx(&self) -> String {
        nodebox_core::ndbx::serialize(&self.library)
    }

    /// Serialize the library to JSON.
    #[wasm_bindgen]
    pub fn to_json(&self) -> Result<String, JsError> {
        serde_json::to_string(&self.library)
            .map_err(|e| JsError::new(&format!("Failed to serialize to JSON: {}", e)))
    }

    /// Deserialize a library from JSON.
    #[wasm_bindgen]
    pub fn from_json(json: &str) -> Result<WasmNodeLibrary, JsError> {
        let library: NodeLibrary = serde_json::from_str(json)
            .map_err(|e| JsError::new(&format!("Failed to parse JSON: {}", e)))?;
        Ok(Self { library })
    }

    /// Add a node by template name at the given position.
    /// Returns the name of the created node.
    #[wasm_bindgen]
    pub fn add_node(&mut self, template_name: &str, x: f64, y: f64) -> Result<String, JsError> {
        let template = NODE_TEMPLATES
            .iter()
            .find(|t| t.name == template_name)
            .ok_or_else(|| JsError::new(&format!("Unknown template: {}", template_name)))?;
        let position = Point::new(x, y);
        let node = create_node_from_template(template, &self.library, position);
        let name = node.name.clone();
        self.library.root.children.push(node);
        Ok(name)
    }

    /// Remove a node by name.
    #[wasm_bindgen]
    pub fn remove_node(&mut self, name: &str) {
        self.library.root.children.retain(|n| n.name != name);
        // Also remove any connections involving this node
        self.library.root.connections.retain(|c| {
            c.output_node != name && c.input_node != name
        });
        // Clear rendered child if it was removed
        if self.library.root.rendered_child.as_deref() == Some(name) {
            self.library.root.rendered_child = None;
        }
    }

    /// Set an input port value on a node.
    /// `value_json` should be a JSON value matching the port type.
    #[wasm_bindgen]
    pub fn set_port_value(&mut self, node_name: &str, port_name: &str, value_json: &str) -> Result<(), JsError> {
        let node = self.library.root.children.iter_mut()
            .find(|n| n.name == node_name)
            .ok_or_else(|| JsError::new(&format!("Node not found: {}", node_name)))?;

        let port = node.inputs.iter_mut()
            .find(|p| p.name == port_name)
            .ok_or_else(|| JsError::new(&format!("Port not found: {}.{}", node_name, port_name)))?;

        let json_value: serde_json::Value = serde_json::from_str(value_json)
            .map_err(|e| JsError::new(&format!("Invalid JSON value: {}", e)))?;

        // Set the port value based on its type
        match port.port_type {
            PortType::Float => {
                if let Some(v) = json_value.as_f64() {
                    port.value = Value::Float(v);
                }
            }
            PortType::Int => {
                if let Some(v) = json_value.as_i64() {
                    port.value = Value::Int(v);
                }
            }
            PortType::Boolean => {
                if let Some(v) = json_value.as_bool() {
                    port.value = Value::Boolean(v);
                }
            }
            PortType::String => {
                if let Some(v) = json_value.as_str() {
                    port.value = Value::String(v.to_string());
                }
            }
            PortType::Point => {
                if let Some(obj) = json_value.as_object() {
                    let x = obj.get("x").and_then(|v| v.as_f64()).unwrap_or(0.0);
                    let y = obj.get("y").and_then(|v| v.as_f64()).unwrap_or(0.0);
                    port.value = Value::Point(Point::new(x, y));
                }
            }
            PortType::Color => {
                if let Some(obj) = json_value.as_object() {
                    let r = obj.get("r").and_then(|v| v.as_f64()).unwrap_or(0.0);
                    let g = obj.get("g").and_then(|v| v.as_f64()).unwrap_or(0.0);
                    let b = obj.get("b").and_then(|v| v.as_f64()).unwrap_or(0.0);
                    let a = obj.get("a").and_then(|v| v.as_f64()).unwrap_or(1.0);
                    port.value = Value::Color(Color::rgba(r, g, b, a));
                }
            }
            _ => {
                // For menu/geometry/list/data types, try string
                if let Some(v) = json_value.as_str() {
                    port.value = Value::String(v.to_string());
                }
            }
        }
        Ok(())
    }

    /// Add a connection between two nodes.
    #[wasm_bindgen]
    pub fn add_connection(&mut self, output_node: &str, input_node: &str, input_port: &str) {
        let conn = Connection::new(output_node, input_node, input_port);
        self.library.root.connections.push(conn);
    }

    /// Remove a connection.
    #[wasm_bindgen]
    pub fn remove_connection(&mut self, output_node: &str, input_node: &str, input_port: &str) {
        self.library.root.connections.retain(|c| {
            !(c.output_node == output_node && c.input_node == input_node && c.input_port == input_port)
        });
    }

    /// Set which node is rendered (the output node).
    #[wasm_bindgen]
    pub fn set_rendered_child(&mut self, name: &str) {
        self.library.root.rendered_child = Some(name.to_string());
    }

    /// Get the name of the currently rendered child.
    #[wasm_bindgen]
    pub fn get_rendered_child(&self) -> Option<String> {
        self.library.root.rendered_child.clone()
    }

    /// Evaluate the node graph and return the result as JSON.
    ///
    /// Returns JSON with structure:
    /// ```json
    /// {
    ///   "svg": "<svg>...</svg>",
    ///   "output": { "type": "Paths", "count": 25 },
    ///   "errors": [{ "node": "rect1", "message": "..." }]
    /// }
    /// ```
    #[wasm_bindgen]
    pub fn evaluate(&self, frame: u32) -> String {
        let platform: Arc<dyn Platform> = Arc::new(WasmPlatform::new());
        let mut ctx = ProjectContext::new_unsaved();
        ctx.frame = frame;

        let (geometry, output, errors) = evaluate_network(&self.library, &platform, &ctx);

        // Get canvas dimensions from library properties
        let width = self.library.width();
        let height = self.library.height();

        // Render geometry to SVG
        let svg = nodebox_core::svg::render_to_svg(&geometry, width, height);

        // Build result JSON
        let output_info = describe_output(&output);
        let error_list: Vec<serde_json::Value> = errors
            .iter()
            .map(|e| {
                serde_json::json!({
                    "node": e.node_name,
                    "message": e.message,
                })
            })
            .collect();

        let result = serde_json::json!({
            "svg": svg,
            "output": output_info,
            "errors": error_list,
        });

        serde_json::to_string(&result).unwrap_or_else(|_| r#"{"svg":"","output":null,"errors":[]}"#.to_string())
    }

    /// Get the library state as JSON (nodes, connections, properties).
    #[wasm_bindgen]
    pub fn get_state(&self) -> String {
        let nodes: Vec<serde_json::Value> = self.library.root.children.iter().map(|n| {
            let inputs: Vec<serde_json::Value> = n.inputs.iter().map(|p| {
                let value_json = value_to_json(&p.value);
                serde_json::json!({
                    "name": p.name,
                    "type": p.port_type.as_str(),
                    "value": value_json,
                })
            }).collect();

            serde_json::json!({
                "name": n.name,
                "prototype": n.prototype,
                "category": n.category,
                "position": { "x": n.position.x, "y": n.position.y },
                "inputs": inputs,
                "outputType": n.output_type.as_str(),
            })
        }).collect();

        let connections: Vec<serde_json::Value> = self.library.root.connections.iter().map(|c| {
            serde_json::json!({
                "outputNode": c.output_node,
                "inputNode": c.input_node,
                "inputPort": c.input_port,
            })
        }).collect();

        let result = serde_json::json!({
            "renderedChild": self.library.root.rendered_child,
            "nodes": nodes,
            "connections": connections,
            "properties": {
                "width": self.library.width(),
                "height": self.library.height(),
            },
        });

        serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string())
    }
}

/// Convert a Value to a JSON value for the state API.
fn value_to_json(value: &Value) -> serde_json::Value {
    match value {
        Value::Null => serde_json::Value::Null,
        Value::Int(v) => serde_json::json!(v),
        Value::Float(v) => serde_json::json!(v),
        Value::String(v) => serde_json::json!(v),
        Value::Boolean(v) => serde_json::json!(v),
        Value::Point(p) => serde_json::json!({"x": p.x, "y": p.y}),
        Value::Color(c) => serde_json::json!({"r": c.r, "g": c.g, "b": c.b, "a": c.a}),
        Value::Path(_) | Value::Geometry(_) => serde_json::json!("[geometry]"),
        Value::List(items) => {
            let arr: Vec<serde_json::Value> = items.iter().map(value_to_json).collect();
            serde_json::Value::Array(arr)
        }
        Value::Map(map) => {
            let obj: serde_json::Map<String, serde_json::Value> = map
                .iter()
                .map(|(k, v)| (k.clone(), value_to_json(v)))
                .collect();
            serde_json::Value::Object(obj)
        }
    }
}

/// Describe a NodeOutput for the JSON result.
fn describe_output(output: &NodeOutput) -> serde_json::Value {
    match output {
        NodeOutput::None => serde_json::json!(null),
        NodeOutput::Path(_) => serde_json::json!({"type": "Path", "count": 1}),
        NodeOutput::Paths(ps) => serde_json::json!({"type": "Paths", "count": ps.len()}),
        NodeOutput::Point(p) => serde_json::json!({"type": "Point", "value": {"x": p.x, "y": p.y}}),
        NodeOutput::Points(ps) => serde_json::json!({"type": "Points", "count": ps.len()}),
        NodeOutput::Float(v) => serde_json::json!({"type": "Float", "value": v}),
        NodeOutput::Floats(vs) => serde_json::json!({"type": "Floats", "count": vs.len()}),
        NodeOutput::Int(v) => serde_json::json!({"type": "Int", "value": v}),
        NodeOutput::Ints(vs) => serde_json::json!({"type": "Ints", "count": vs.len()}),
        NodeOutput::String(s) => serde_json::json!({"type": "String", "value": s}),
        NodeOutput::Strings(ss) => serde_json::json!({"type": "Strings", "count": ss.len()}),
        NodeOutput::Color(c) => serde_json::json!({"type": "Color", "value": {"r": c.r, "g": c.g, "b": c.b, "a": c.a}}),
        NodeOutput::Colors(cs) => serde_json::json!({"type": "Colors", "count": cs.len()}),
        NodeOutput::Boolean(v) => serde_json::json!({"type": "Boolean", "value": v}),
        NodeOutput::Booleans(vs) => serde_json::json!({"type": "Booleans", "count": vs.len()}),
        NodeOutput::DataRow(row) => serde_json::json!({"type": "DataRow", "keys": row.keys().collect::<Vec<_>>()}),
        NodeOutput::DataRows(rows) => serde_json::json!({"type": "DataRows", "count": rows.len()}),
    }
}

/// Evaluate a NodeLibrary from JSON and return the result as JSON.
///
/// Takes a JSON-serialized NodeLibrary and frame number, evaluates the node graph,
/// and returns JSON matching the TypeScript EvalResult interface:
/// ```json
/// {
///   "paths": [{ "contours": [...], "fill": {...}, "stroke": null, "stroke_width": 1.0 }],
///   "texts": [],
///   "output": { "type": "Geometry", "isMultiple": false, "values": ["Path 0", ...] },
///   "errors": [{ "nodeName": "rect1", "message": "..." }]
/// }
/// ```
#[wasm_bindgen]
pub fn evaluate_library(library_json: &str, frame: u32) -> String {
    let library: NodeLibrary = match serde_json::from_str(library_json) {
        Ok(lib) => lib,
        Err(e) => {
            return error_result_json(&format!("Failed to parse library: {}", e));
        }
    };

    let platform: Arc<dyn Platform> = Arc::new(WasmPlatform::new());
    let mut ctx = ProjectContext::new_unsaved();
    ctx.frame = frame;

    let (paths, output, errors) = evaluate_network(&library, &platform, &ctx);

    serialize_eval_result(&paths, &output, &errors)
}

/// Build an EvalResult JSON with just an error.
fn error_result_json(message: &str) -> String {
    serde_json::json!({
        "paths": [],
        "texts": [],
        "output": { "type": "none", "isMultiple": false, "values": [] },
        "errors": [{ "nodeName": "root", "message": message }],
    })
    .to_string()
}

/// Serialize evaluation results to JSON matching the TS EvalResult type.
fn serialize_eval_result(
    paths: &[nodebox_core::geometry::Path],
    output: &NodeOutput,
    errors: &[nodebox_eval::eval::NodeError],
) -> String {
    // Paths serialize directly via serde (field names match TS PathRenderData)
    let paths_json = serde_json::to_value(paths).unwrap_or(serde_json::json!([]));

    // Build output info
    let (output_type, is_multiple) = match output {
        NodeOutput::None => ("none", false),
        NodeOutput::Path(_) => ("Geometry", false),
        NodeOutput::Paths(_) => ("Geometry", true),
        NodeOutput::Point(_) => ("Point", false),
        NodeOutput::Points(_) => ("Point", true),
        NodeOutput::Float(_) => ("Float", false),
        NodeOutput::Floats(_) => ("Float", true),
        NodeOutput::Int(_) => ("Int", false),
        NodeOutput::Ints(_) => ("Int", true),
        NodeOutput::String(_) => ("String", false),
        NodeOutput::Strings(_) => ("String", true),
        NodeOutput::Color(_) => ("Color", false),
        NodeOutput::Colors(_) => ("Color", true),
        NodeOutput::Boolean(_) => ("Boolean", false),
        NodeOutput::Booleans(_) => ("Boolean", true),
        NodeOutput::DataRow(_) => ("Data", false),
        NodeOutput::DataRows(_) => ("Data", true),
    };

    let values: Vec<String> = (0..paths.len())
        .map(|i| format!("Path {}", i))
        .collect();

    let error_list: Vec<serde_json::Value> = errors
        .iter()
        .map(|e| {
            serde_json::json!({
                "nodeName": e.node_name,
                "message": e.message,
            })
        })
        .collect();

    let result = serde_json::json!({
        "paths": paths_json,
        "texts": [],
        "output": {
            "type": output_type,
            "isMultiple": is_multiple,
            "values": values,
        },
        "errors": error_list,
    });

    serde_json::to_string(&result)
        .unwrap_or_else(|_| error_result_json("Failed to serialize result"))
}

/// Resolve prototype ports for all nodes in the library.
///
/// After parsing an .ndbx file, nodes only contain explicitly overridden port values.
/// Ports inherited from the prototype (e.g., `shape` from `corevector.filter`) are missing.
/// This function merges each node's ports with the full port list from its template,
/// preserving any overridden values from the file.
fn resolve_prototype_ports(library: &mut NodeLibrary) {
    let temp_lib = NodeLibrary::new("_temp");

    for child in &mut library.root.children {
        resolve_node_ports(child, &temp_lib);
    }
}

/// Recursively resolve prototype ports for a node and its children.
fn resolve_node_ports(node: &mut nodebox_core::node::Node, temp_lib: &NodeLibrary) {
    // Resolve this node's ports from its prototype
    if let Some(prototype) = &node.prototype {
        if let Some(template) = NODE_TEMPLATES.iter().find(|t| t.prototype == prototype) {
            let template_node = create_node_from_template(template, temp_lib, Point::ZERO);

            // Merge: template ports as base, overlay parsed values
            let mut merged_inputs = template_node.inputs;
            for merged_port in &mut merged_inputs {
                if let Some(parsed_port) = node.inputs.iter().find(|p| p.name == merged_port.name) {
                    merged_port.value = parsed_port.value.clone();
                }
            }
            node.inputs = merged_inputs;
            node.output_type = template_node.output_type;
            node.output_range = template_node.output_range;
        }
    }

    // Recursively resolve children (for subnet networks)
    for child in &mut node.children {
        resolve_node_ports(child, temp_lib);
    }
}

/// Convert text to vector path contours using the bundled font.
///
/// Returns JSON array of contours, each with points and closed flag.
/// Uses the bundled Inter font (no system font access needed).
#[wasm_bindgen]
pub fn text_to_path(text: &str, font_size: f64, position_x: f64, position_y: f64) -> String {
    let font_bytes = font::BUNDLED_FONT_BYTES;
    let position = Point::new(position_x, position_y);

    match font::text_to_path_from_bytes(text, font_bytes, font_size, position) {
        Ok(path) => {
            let contours: Vec<serde_json::Value> = path.contours.iter().map(|c| {
                let points: Vec<serde_json::Value> = c.points.iter().map(|p| {
                    serde_json::json!({
                        "x": p.x(),
                        "y": p.y(),
                        "type": match p.point_type {
                            nodebox_core::geometry::PointType::LineTo => "lineTo",
                            nodebox_core::geometry::PointType::CurveTo => "curveTo",
                            nodebox_core::geometry::PointType::CurveData => "curveData",
                            nodebox_core::geometry::PointType::QuadTo => "quadTo",
                            nodebox_core::geometry::PointType::QuadData => "quadData",
                        },
                    })
                }).collect();
                serde_json::json!({
                    "points": points,
                    "closed": c.closed,
                })
            }).collect();
            serde_json::to_string(&contours).unwrap_or_else(|_| "[]".to_string())
        }
        Err(_) => "[]".to_string(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use nodebox_core::node::PortType;

    #[test]
    fn test_resolve_prototype_ports_merges_template_ports() {
        // A copy node in .ndbx only has overridden ports (copies=13).
        // The template also defines `shape`, `order`, `translate`, `rotate`, `scale`.
        let ndbx = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network" renderedChild="copy1">
        <node name="rect1" prototype="corevector.rect"/>
        <node name="copy1" prototype="corevector.copy" position="2.00,0.00">
            <port name="copies" type="int" value="13"/>
        </node>
        <conn input="copy1.shape" output="rect1"/>
    </node>
</ndbx>"#;

        let mut library = nodebox_core::ndbx::parse(ndbx).unwrap();

        // Before resolution: copy1 only has the one overridden port
        let copy_before = library.root.children.iter().find(|n| n.name == "copy1").unwrap();
        assert_eq!(copy_before.inputs.len(), 1);
        assert_eq!(copy_before.inputs[0].name, "copies");

        resolve_prototype_ports(&mut library);

        let copy = library.root.children.iter().find(|n| n.name == "copy1").unwrap();

        // After resolution: copy1 has all template ports
        assert!(copy.inputs.len() > 1, "Expected multiple ports, got {}", copy.inputs.len());

        // The `shape` port should exist (from template)
        let shape_port = copy.inputs.iter().find(|p| p.name == "shape");
        assert!(shape_port.is_some(), "Expected 'shape' port from template");

        // The overridden value should be preserved
        let copies_port = copy.inputs.iter().find(|p| p.name == "copies").unwrap();
        assert_eq!(copies_port.value, Value::Int(13));

        // Output type should be set from template
        assert_eq!(copy.output_type, PortType::Geometry);
    }

    #[test]
    fn test_resolve_prototype_ports_rect_gets_all_ports() {
        let ndbx = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network" renderedChild="rect1">
        <node name="rect1" prototype="corevector.rect">
            <port name="width" type="float" value="200.0"/>
        </node>
    </node>
</ndbx>"#;

        let mut library = nodebox_core::ndbx::parse(ndbx).unwrap();
        resolve_prototype_ports(&mut library);

        let rect = &library.root.children[0];

        // rect template has: position, width, height, roundness
        let port_names: Vec<&str> = rect.inputs.iter().map(|p| p.name.as_str()).collect();
        assert!(port_names.contains(&"position"), "Missing 'position' port");
        assert!(port_names.contains(&"width"), "Missing 'width' port");
        assert!(port_names.contains(&"height"), "Missing 'height' port");

        // Overridden width=200 should be preserved
        let width = rect.inputs.iter().find(|p| p.name == "width").unwrap();
        assert_eq!(width.value, Value::Float(200.0));

        // Non-overridden height should have template default (100.0)
        let height = rect.inputs.iter().find(|p| p.name == "height").unwrap();
        assert_eq!(height.value, Value::Float(100.0));
    }

    #[test]
    fn test_resolve_prototype_ports_unknown_prototype_unchanged() {
        let ndbx = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network">
        <node name="custom1" prototype="some.unknown.node">
            <port name="x" type="float" value="42.0"/>
        </node>
    </node>
</ndbx>"#;

        let mut library = nodebox_core::ndbx::parse(ndbx).unwrap();
        resolve_prototype_ports(&mut library);

        // Unknown prototype: ports should remain unchanged
        let custom = &library.root.children[0];
        assert_eq!(custom.inputs.len(), 1);
        assert_eq!(custom.inputs[0].name, "x");
    }

    #[test]
    fn test_resolve_prototype_ports_nested_networks() {
        let ndbx = r#"<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="17" type="file" uuid="test">
    <node name="root" prototype="core.network">
        <node name="subnet1" prototype="core.network">
            <node name="ellipse1" prototype="corevector.ellipse">
                <port name="width" type="float" value="50.0"/>
            </node>
        </node>
    </node>
</ndbx>"#;

        let mut library = nodebox_core::ndbx::parse(ndbx).unwrap();
        resolve_prototype_ports(&mut library);

        // The nested ellipse should also have its ports resolved
        let subnet = &library.root.children[0];
        let ellipse = &subnet.children[0];
        let port_names: Vec<&str> = ellipse.inputs.iter().map(|p| p.name.as_str()).collect();
        assert!(port_names.contains(&"position"), "Missing 'position' port in nested node");
        assert!(port_names.contains(&"width"), "Missing 'width' port in nested node");
        assert!(port_names.contains(&"height"), "Missing 'height' port in nested node");

        // Overridden width should be preserved
        let width = ellipse.inputs.iter().find(|p| p.name == "width").unwrap();
        assert_eq!(width.value, Value::Float(50.0));
    }
}
