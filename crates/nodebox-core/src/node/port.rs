//! Port - input and output ports on nodes.

use crate::geometry::{Point, Color};
use crate::Value;

/// The data type of a port.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub enum PortType {
    Int,
    Float,
    String,
    Boolean,
    Point,
    Color,
    Geometry,
    List,
    Context,
    State,
    /// Custom type with a name.
    Custom(String),
}

impl PortType {
    /// Parses a port type from a string.
    pub fn from_str(s: &str) -> Self {
        match s {
            "int" => PortType::Int,
            "float" => PortType::Float,
            "string" => PortType::String,
            "boolean" => PortType::Boolean,
            "point" => PortType::Point,
            "color" => PortType::Color,
            "geometry" => PortType::Geometry,
            "list" => PortType::List,
            "context" => PortType::Context,
            "state" => PortType::State,
            other => PortType::Custom(other.to_string()),
        }
    }

    /// Converts the port type to a string.
    pub fn as_str(&self) -> &str {
        match self {
            PortType::Int => "int",
            PortType::Float => "float",
            PortType::String => "string",
            PortType::Boolean => "boolean",
            PortType::Point => "point",
            PortType::Color => "color",
            PortType::Geometry => "geometry",
            PortType::List => "list",
            PortType::Context => "context",
            PortType::State => "state",
            PortType::Custom(s) => s,
        }
    }

    /// Returns the default value for this port type.
    pub fn default_value(&self) -> Value {
        match self {
            PortType::Int => Value::Int(0),
            PortType::Float => Value::Float(0.0),
            PortType::String => Value::String(String::new()),
            PortType::Boolean => Value::Boolean(false),
            PortType::Point => Value::Point(Point::ZERO),
            PortType::Color => Value::Color(Color::BLACK),
            PortType::Geometry => Value::Null,
            PortType::List => Value::List(Vec::new()),
            PortType::Context | PortType::State | PortType::Custom(_) => Value::Null,
        }
    }

    /// Checks if an output type is compatible with an input type.
    ///
    /// This determines whether a connection can be made between nodes.
    pub fn is_compatible(output_type: &PortType, input_type: &PortType) -> bool {
        // Same type is always compatible
        if output_type == input_type {
            return true;
        }

        // Everything can be converted to a string
        if matches!(input_type, PortType::String) {
            return true;
        }

        // Int <-> Float conversions
        if matches!(output_type, PortType::Int) && matches!(input_type, PortType::Float) {
            return true;
        }
        if matches!(output_type, PortType::Float) && matches!(input_type, PortType::Int) {
            return true;
        }

        // Number -> Point (both components get same value)
        if matches!(output_type, PortType::Int | PortType::Float)
            && matches!(input_type, PortType::Point)
        {
            return true;
        }

        false
    }
}

impl Default for PortType {
    fn default() -> Self {
        PortType::Geometry
    }
}

/// The UI widget type for a port.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Hash)]
pub enum Widget {
    #[default]
    None,
    Int,
    Float,
    Angle,
    String,
    Text,
    Password,
    Toggle,
    Color,
    Point,
    Menu,
    File,
    Font,
    Image,
    Data,
    Seed,
    Gradient,
}

impl Widget {
    /// Parses a widget from a string.
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "int" => Widget::Int,
            "float" => Widget::Float,
            "angle" => Widget::Angle,
            "string" => Widget::String,
            "text" => Widget::Text,
            "password" => Widget::Password,
            "toggle" => Widget::Toggle,
            "color" => Widget::Color,
            "point" => Widget::Point,
            "menu" => Widget::Menu,
            "file" => Widget::File,
            "font" => Widget::Font,
            "image" => Widget::Image,
            "data" => Widget::Data,
            "seed" => Widget::Seed,
            "gradient" => Widget::Gradient,
            _ => Widget::None,
        }
    }

    /// Returns the appropriate default widget for a port type.
    pub fn default_for_type(port_type: &PortType) -> Self {
        match port_type {
            PortType::Int => Widget::Int,
            PortType::Float => Widget::Float,
            PortType::String => Widget::String,
            PortType::Boolean => Widget::Toggle,
            PortType::Point => Widget::Point,
            PortType::Color => Widget::Color,
            _ => Widget::None,
        }
    }
}

/// Whether a port expects a single value or a list.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Hash)]
pub enum PortRange {
    /// Single value.
    #[default]
    Value,
    /// List of values.
    List,
}

impl PortRange {
    pub fn from_str(s: &str) -> Self {
        match s.to_lowercase().as_str() {
            "list" => PortRange::List,
            _ => PortRange::Value,
        }
    }
}

/// A menu item for menu-based ports.
#[derive(Clone, Debug, PartialEq)]
pub struct MenuItem {
    pub key: String,
    pub label: String,
}

impl MenuItem {
    pub fn new(key: impl Into<String>, label: impl Into<String>) -> Self {
        MenuItem {
            key: key.into(),
            label: label.into(),
        }
    }
}

/// An input or output port on a node.
#[derive(Clone, Debug, PartialEq)]
pub struct Port {
    /// The port name (identifier).
    pub name: String,
    /// The data type of this port.
    pub port_type: PortType,
    /// Optional label for display (defaults to name if not set).
    pub label: Option<String>,
    /// Description of what this port does.
    pub description: Option<String>,
    /// The UI widget type.
    pub widget: Widget,
    /// Whether this port expects a single value or list.
    pub range: PortRange,
    /// The current value of this port.
    pub value: Value,
    /// Minimum value for numeric ports.
    pub min: Option<f64>,
    /// Maximum value for numeric ports.
    pub max: Option<f64>,
    /// Menu items for menu-based ports.
    pub menu_items: Vec<MenuItem>,
}

impl Port {
    /// Creates a new port with the given name and type.
    pub fn new(name: impl Into<String>, port_type: PortType) -> Self {
        let name = name.into();
        let widget = Widget::default_for_type(&port_type);
        let value = port_type.default_value();
        Port {
            name,
            port_type,
            label: None,
            description: None,
            widget,
            range: PortRange::Value,
            value,
            min: None,
            max: None,
            menu_items: Vec::new(),
        }
    }

    /// Creates an integer port.
    pub fn int(name: impl Into<String>, value: i64) -> Self {
        let mut port = Port::new(name, PortType::Int);
        port.value = Value::Int(value);
        port
    }

    /// Creates a float port.
    pub fn float(name: impl Into<String>, value: f64) -> Self {
        let mut port = Port::new(name, PortType::Float);
        port.value = Value::Float(value);
        port
    }

    /// Creates a string port.
    pub fn string(name: impl Into<String>, value: impl Into<String>) -> Self {
        let mut port = Port::new(name, PortType::String);
        port.value = Value::String(value.into());
        port
    }

    /// Creates a boolean port.
    pub fn boolean(name: impl Into<String>, value: bool) -> Self {
        let mut port = Port::new(name, PortType::Boolean);
        port.value = Value::Boolean(value);
        port
    }

    /// Creates a point port.
    pub fn point(name: impl Into<String>, value: Point) -> Self {
        let mut port = Port::new(name, PortType::Point);
        port.value = Value::Point(value);
        port
    }

    /// Creates a color port.
    pub fn color(name: impl Into<String>, value: Color) -> Self {
        let mut port = Port::new(name, PortType::Color);
        port.value = Value::Color(value);
        port
    }

    /// Creates a geometry port.
    pub fn geometry(name: impl Into<String>) -> Self {
        Port::new(name, PortType::Geometry)
    }

    /// Returns the display label (falls back to name).
    pub fn display_label(&self) -> &str {
        self.label.as_deref().unwrap_or(&self.name)
    }

    /// Sets the minimum value.
    pub fn with_min(mut self, min: f64) -> Self {
        self.min = Some(min);
        self
    }

    /// Sets the maximum value.
    pub fn with_max(mut self, max: f64) -> Self {
        self.max = Some(max);
        self
    }

    /// Sets the range (min and max).
    pub fn with_range_values(mut self, min: f64, max: f64) -> Self {
        self.min = Some(min);
        self.max = Some(max);
        self
    }

    /// Sets the port range (value or list).
    pub fn with_port_range(mut self, range: PortRange) -> Self {
        self.range = range;
        self
    }

    /// Sets the widget type.
    pub fn with_widget(mut self, widget: Widget) -> Self {
        self.widget = widget;
        self
    }

    /// Sets the description.
    pub fn with_description(mut self, description: impl Into<String>) -> Self {
        self.description = Some(description.into());
        self
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_port_type_from_str() {
        assert_eq!(PortType::from_str("int"), PortType::Int);
        assert_eq!(PortType::from_str("float"), PortType::Float);
        assert_eq!(PortType::from_str("geometry"), PortType::Geometry);
        assert_eq!(
            PortType::from_str("custom"),
            PortType::Custom("custom".to_string())
        );
    }

    #[test]
    fn test_port_type_compatibility() {
        // Same type
        assert!(PortType::is_compatible(&PortType::Int, &PortType::Int));

        // Everything -> String
        assert!(PortType::is_compatible(&PortType::Int, &PortType::String));
        assert!(PortType::is_compatible(&PortType::Geometry, &PortType::String));

        // Int <-> Float
        assert!(PortType::is_compatible(&PortType::Int, &PortType::Float));
        assert!(PortType::is_compatible(&PortType::Float, &PortType::Int));

        // Number -> Point
        assert!(PortType::is_compatible(&PortType::Int, &PortType::Point));
        assert!(PortType::is_compatible(&PortType::Float, &PortType::Point));

        // Incompatible
        assert!(!PortType::is_compatible(&PortType::String, &PortType::Int));
        assert!(!PortType::is_compatible(&PortType::Point, &PortType::Color));
    }

    #[test]
    fn test_port_creation() {
        let port = Port::int("x", 42);
        assert_eq!(port.name, "x");
        assert_eq!(port.port_type, PortType::Int);
        assert_eq!(port.value.as_int(), Some(42));
    }

    #[test]
    fn test_port_float() {
        let port = Port::float("angle", 45.0).with_widget(Widget::Angle);
        assert_eq!(port.port_type, PortType::Float);
        assert_eq!(port.widget, Widget::Angle);
    }

    #[test]
    fn test_port_with_range() {
        let port = Port::float("x", 0.0).with_range_values(0.0, 100.0);
        assert_eq!(port.min, Some(0.0));
        assert_eq!(port.max, Some(100.0));
    }

    #[test]
    fn test_widget_default_for_type() {
        assert_eq!(Widget::default_for_type(&PortType::Int), Widget::Int);
        assert_eq!(Widget::default_for_type(&PortType::Float), Widget::Float);
        assert_eq!(Widget::default_for_type(&PortType::Boolean), Widget::Toggle);
        assert_eq!(Widget::default_for_type(&PortType::Color), Widget::Color);
    }
}
