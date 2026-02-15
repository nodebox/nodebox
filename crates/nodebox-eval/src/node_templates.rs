//! Node template definitions for all available node types.

/// Available node types that can be created.
pub struct NodeTemplate {
    pub name: &'static str,
    pub prototype: &'static str,
    pub category: &'static str,
    pub description: &'static str,
}

/// List of all available node templates.
pub const NODE_TEMPLATES: &[NodeTemplate] = &[
    // ========================
    // Geometry generators
    // ========================
    NodeTemplate {
        name: "ellipse",
        prototype: "corevector.ellipse",
        category: "geometry",
        description: "Create an ellipse or circle",
    },
    NodeTemplate {
        name: "rect",
        prototype: "corevector.rect",
        category: "geometry",
        description: "Create a rectangle",
    },
    NodeTemplate {
        name: "line",
        prototype: "corevector.line",
        category: "geometry",
        description: "Create a line between two points",
    },
    NodeTemplate {
        name: "line_angle",
        prototype: "corevector.line_angle",
        category: "geometry",
        description: "Create a line from a point, angle and distance",
    },
    NodeTemplate {
        name: "polygon",
        prototype: "corevector.polygon",
        category: "geometry",
        description: "Create a regular polygon",
    },
    NodeTemplate {
        name: "star",
        prototype: "corevector.star",
        category: "geometry",
        description: "Create a star shape",
    },
    NodeTemplate {
        name: "arc",
        prototype: "corevector.arc",
        category: "geometry",
        description: "Create an arc or pie slice",
    },
    NodeTemplate {
        name: "quad_curve",
        prototype: "corevector.quad_curve",
        category: "geometry",
        description: "Create a quadratic curve between two points",
    },
    NodeTemplate {
        name: "grid",
        prototype: "corevector.grid",
        category: "geometry",
        description: "Create a grid of points",
    },
    NodeTemplate {
        name: "textpath",
        prototype: "corevector.textpath",
        category: "geometry",
        description: "Convert text to a vector path",
    },
    NodeTemplate {
        name: "connect",
        prototype: "corevector.connect",
        category: "geometry",
        description: "Connect all points in a path",
    },
    NodeTemplate {
        name: "make_point",
        prototype: "corevector.make_point",
        category: "geometry",
        description: "Create a point from X/Y coordinates",
    },
    NodeTemplate {
        name: "freehand",
        prototype: "corevector.freehand",
        category: "geometry",
        description: "Draw directly on the canvas",
    },
    // Combine nodes
    NodeTemplate {
        name: "merge",
        prototype: "corevector.merge",
        category: "geometry",
        description: "Combine multiple shapes",
    },
    NodeTemplate {
        name: "group",
        prototype: "corevector.group",
        category: "geometry",
        description: "Group shapes together",
    },
    NodeTemplate {
        name: "ungroup",
        prototype: "corevector.ungroup",
        category: "geometry",
        description: "Decompose geometry into its paths",
    },
    // Modify nodes
    NodeTemplate {
        name: "resample",
        prototype: "corevector.resample",
        category: "geometry",
        description: "Resample path points",
    },
    NodeTemplate {
        name: "wiggle",
        prototype: "corevector.wiggle",
        category: "geometry",
        description: "Add random displacement to points",
    },
    NodeTemplate {
        name: "align",
        prototype: "corevector.align",
        category: "geometry",
        description: "Align a shape in relation to the origin",
    },
    NodeTemplate {
        name: "fit",
        prototype: "corevector.fit",
        category: "geometry",
        description: "Fit a shape within bounds",
    },
    NodeTemplate {
        name: "fit_to",
        prototype: "corevector.fit_to",
        category: "geometry",
        description: "Fit a shape to another shape",
    },
    NodeTemplate {
        name: "snap",
        prototype: "corevector.snap",
        category: "geometry",
        description: "Snap geometry to a grid",
    },
    NodeTemplate {
        name: "centroid",
        prototype: "corevector.centroid",
        category: "geometry",
        description: "Calculate the center point of a shape",
    },
    NodeTemplate {
        name: "point_on_path",
        prototype: "corevector.point_on_path",
        category: "geometry",
        description: "Calculate a point on a path",
    },
    NodeTemplate {
        name: "scatter",
        prototype: "corevector.scatter",
        category: "geometry",
        description: "Generate points within a shape",
    },
    NodeTemplate {
        name: "delete",
        prototype: "corevector.delete",
        category: "geometry",
        description: "Delete points or paths within a bounding shape",
    },
    NodeTemplate {
        name: "sort",
        prototype: "corevector.sort",
        category: "geometry",
        description: "Sort shapes by position or distance",
    },
    NodeTemplate {
        name: "stack",
        prototype: "corevector.stack",
        category: "geometry",
        description: "Arrange shapes in a layout",
    },
    NodeTemplate {
        name: "link",
        prototype: "corevector.link",
        category: "geometry",
        description: "Generate a visual link between two shapes",
    },
    NodeTemplate {
        name: "shape_on_path",
        prototype: "corevector.shape_on_path",
        category: "geometry",
        description: "Copy shapes along a path",
    },
    NodeTemplate {
        name: "null",
        prototype: "corevector.null",
        category: "geometry",
        description: "Pass through without changes",
    },
    // Import nodes
    NodeTemplate {
        name: "import_svg",
        prototype: "corevector.import_svg",
        category: "geometry",
        description: "Import an SVG file as geometry",
    },
    // ========================
    // Transform nodes
    // ========================
    NodeTemplate {
        name: "translate",
        prototype: "corevector.translate",
        category: "transform",
        description: "Move geometry by offset",
    },
    NodeTemplate {
        name: "rotate",
        prototype: "corevector.rotate",
        category: "transform",
        description: "Rotate geometry around a point",
    },
    NodeTemplate {
        name: "scale",
        prototype: "corevector.scale",
        category: "transform",
        description: "Scale geometry",
    },
    NodeTemplate {
        name: "copy",
        prototype: "corevector.copy",
        category: "transform",
        description: "Create multiple copies",
    },
    NodeTemplate {
        name: "distribute",
        prototype: "corevector.distribute",
        category: "transform",
        description: "Distribute shapes on a horizontal or vertical axis",
    },
    NodeTemplate {
        name: "skew",
        prototype: "corevector.skew",
        category: "transform",
        description: "Skew the shape",
    },
    NodeTemplate {
        name: "reflect",
        prototype: "corevector.reflect",
        category: "transform",
        description: "Mirror geometry around an axis",
    },
    // ========================
    // Color nodes
    // ========================
    NodeTemplate {
        name: "colorize",
        prototype: "corevector.colorize",
        category: "color",
        description: "Set fill and stroke colors",
    },
    NodeTemplate {
        name: "rgb_color",
        prototype: "color.rgb_color",
        category: "color",
        description: "Create a color from RGB components",
    },
    NodeTemplate {
        name: "hsb_color",
        prototype: "color.hsb_color",
        category: "color",
        description: "Create a color from HSB components",
    },
    NodeTemplate {
        name: "gray_color",
        prototype: "color.gray_color",
        category: "color",
        description: "Create a grayscale color",
    },
    NodeTemplate {
        name: "color",
        prototype: "color.color",
        category: "color",
        description: "Create a color value",
    },
    // ========================
    // Math nodes
    // ========================
    NodeTemplate {
        name: "number",
        prototype: "math.number",
        category: "math",
        description: "Create a number value",
    },
    NodeTemplate {
        name: "integer",
        prototype: "math.integer",
        category: "math",
        description: "Create an integer value",
    },
    NodeTemplate {
        name: "boolean",
        prototype: "math.boolean",
        category: "math",
        description: "Create a boolean value",
    },
    NodeTemplate {
        name: "add",
        prototype: "math.add",
        category: "math",
        description: "Add two numbers",
    },
    NodeTemplate {
        name: "subtract",
        prototype: "math.subtract",
        category: "math",
        description: "Subtract two numbers",
    },
    NodeTemplate {
        name: "multiply",
        prototype: "math.multiply",
        category: "math",
        description: "Multiply two numbers",
    },
    NodeTemplate {
        name: "divide",
        prototype: "math.divide",
        category: "math",
        description: "Divide two numbers",
    },
    NodeTemplate {
        name: "mod",
        prototype: "math.mod",
        category: "math",
        description: "Modulo of two numbers",
    },
    NodeTemplate {
        name: "negate",
        prototype: "math.negate",
        category: "math",
        description: "Negate a number",
    },
    NodeTemplate {
        name: "abs",
        prototype: "math.abs",
        category: "math",
        description: "Absolute value",
    },
    NodeTemplate {
        name: "sqrt",
        prototype: "math.sqrt",
        category: "math",
        description: "Square root",
    },
    NodeTemplate {
        name: "pow",
        prototype: "math.pow",
        category: "math",
        description: "Raise to a power",
    },
    NodeTemplate {
        name: "log",
        prototype: "math.log",
        category: "math",
        description: "Natural logarithm",
    },
    NodeTemplate {
        name: "ceil",
        prototype: "math.ceil",
        category: "math",
        description: "Round up to integer",
    },
    NodeTemplate {
        name: "floor",
        prototype: "math.floor",
        category: "math",
        description: "Round down to integer",
    },
    NodeTemplate {
        name: "round",
        prototype: "math.round",
        category: "math",
        description: "Round to nearest integer",
    },
    NodeTemplate {
        name: "sin",
        prototype: "math.sin",
        category: "math",
        description: "Sine function",
    },
    NodeTemplate {
        name: "cos",
        prototype: "math.cos",
        category: "math",
        description: "Cosine function",
    },
    NodeTemplate {
        name: "radians",
        prototype: "math.radians",
        category: "math",
        description: "Convert degrees to radians",
    },
    NodeTemplate {
        name: "degrees",
        prototype: "math.degrees",
        category: "math",
        description: "Convert radians to degrees",
    },
    NodeTemplate {
        name: "pi",
        prototype: "math.pi",
        category: "math",
        description: "The constant pi",
    },
    NodeTemplate {
        name: "e",
        prototype: "math.e",
        category: "math",
        description: "Euler's number",
    },
    NodeTemplate {
        name: "even",
        prototype: "math.even",
        category: "math",
        description: "Check if a number is even",
    },
    NodeTemplate {
        name: "odd",
        prototype: "math.odd",
        category: "math",
        description: "Check if a number is odd",
    },
    NodeTemplate {
        name: "compare",
        prototype: "math.compare",
        category: "math",
        description: "Compare two values",
    },
    NodeTemplate {
        name: "logical",
        prototype: "math.logical",
        category: "math",
        description: "Logical AND/OR of two booleans",
    },
    NodeTemplate {
        name: "angle",
        prototype: "math.angle",
        category: "math",
        description: "Angle between two points",
    },
    NodeTemplate {
        name: "distance",
        prototype: "math.distance",
        category: "math",
        description: "Distance between two points",
    },
    NodeTemplate {
        name: "coordinates",
        prototype: "math.coordinates",
        category: "math",
        description: "Point from angle and distance",
    },
    NodeTemplate {
        name: "math_reflect",
        prototype: "math.reflect",
        category: "math",
        description: "Reflect a point around another",
    },
    NodeTemplate {
        name: "random_numbers",
        prototype: "math.random_numbers",
        category: "math",
        description: "Generate a list of random numbers",
    },
    NodeTemplate {
        name: "range",
        prototype: "math.range",
        category: "math",
        description: "Generate a range of numbers",
    },
    NodeTemplate {
        name: "sample",
        prototype: "math.sample",
        category: "math",
        description: "Generate evenly-spaced samples",
    },
    NodeTemplate {
        name: "wave",
        prototype: "math.wave",
        category: "math",
        description: "Generate a wave value",
    },
    NodeTemplate {
        name: "convert_range",
        prototype: "math.convert_range",
        category: "math",
        description: "Map a value from one range to another",
    },
    NodeTemplate {
        name: "sum",
        prototype: "math.sum",
        category: "math",
        description: "Sum of a list of numbers",
    },
    NodeTemplate {
        name: "average",
        prototype: "math.average",
        category: "math",
        description: "Average of a list of numbers",
    },
    NodeTemplate {
        name: "max",
        prototype: "math.max",
        category: "math",
        description: "Maximum of a list of numbers",
    },
    NodeTemplate {
        name: "min",
        prototype: "math.min",
        category: "math",
        description: "Minimum of a list of numbers",
    },
    NodeTemplate {
        name: "make_numbers",
        prototype: "math.make_numbers",
        category: "math",
        description: "Parse numbers from a string",
    },
    NodeTemplate {
        name: "running_total",
        prototype: "math.running_total",
        category: "math",
        description: "Running total of a list",
    },
    // ========================
    // String nodes
    // ========================
    NodeTemplate {
        name: "string",
        prototype: "string.string",
        category: "string",
        description: "Create a string value",
    },
    NodeTemplate {
        name: "concatenate",
        prototype: "string.concatenate",
        category: "string",
        description: "Join strings together",
    },
    NodeTemplate {
        name: "make_strings",
        prototype: "string.make_strings",
        category: "string",
        description: "Split a string into a list",
    },
    NodeTemplate {
        name: "length",
        prototype: "string.length",
        category: "string",
        description: "Length of a string",
    },
    NodeTemplate {
        name: "word_count",
        prototype: "string.word_count",
        category: "string",
        description: "Count words in a string",
    },
    NodeTemplate {
        name: "change_case",
        prototype: "string.change_case",
        category: "string",
        description: "Change text case",
    },
    NodeTemplate {
        name: "format_number",
        prototype: "string.format_number",
        category: "string",
        description: "Format a number as string",
    },
    NodeTemplate {
        name: "trim",
        prototype: "string.trim",
        category: "string",
        description: "Remove leading/trailing whitespace",
    },
    NodeTemplate {
        name: "replace",
        prototype: "string.replace",
        category: "string",
        description: "Replace text in a string",
    },
    NodeTemplate {
        name: "sub_string",
        prototype: "string.sub_string",
        category: "string",
        description: "Extract part of a string",
    },
    NodeTemplate {
        name: "character_at",
        prototype: "string.character_at",
        category: "string",
        description: "Get character at index",
    },
    NodeTemplate {
        name: "as_binary_string",
        prototype: "string.as_binary_string",
        category: "string",
        description: "Convert string to binary",
    },
    NodeTemplate {
        name: "contains",
        prototype: "string.contains",
        category: "string",
        description: "Check if string contains text",
    },
    NodeTemplate {
        name: "ends_with",
        prototype: "string.ends_with",
        category: "string",
        description: "Check if string ends with text",
    },
    NodeTemplate {
        name: "starts_with",
        prototype: "string.starts_with",
        category: "string",
        description: "Check if string starts with text",
    },
    NodeTemplate {
        name: "equals",
        prototype: "string.equals",
        category: "string",
        description: "Check if two strings are equal",
    },
    NodeTemplate {
        name: "characters",
        prototype: "string.characters",
        category: "string",
        description: "Split string into characters",
    },
    NodeTemplate {
        name: "random_character",
        prototype: "string.random_character",
        category: "string",
        description: "Generate random characters",
    },
    NodeTemplate {
        name: "as_binary_list",
        prototype: "string.as_binary_list",
        category: "string",
        description: "Convert string to binary list",
    },
    NodeTemplate {
        name: "as_number_list",
        prototype: "string.as_number_list",
        category: "string",
        description: "Convert string to number list",
    },
    // ========================
    // List nodes
    // ========================
    NodeTemplate {
        name: "count",
        prototype: "list.count",
        category: "list",
        description: "Count items in a list",
    },
    NodeTemplate {
        name: "first",
        prototype: "list.first",
        category: "list",
        description: "Get the first item of a list",
    },
    NodeTemplate {
        name: "second",
        prototype: "list.second",
        category: "list",
        description: "Get the second item of a list",
    },
    NodeTemplate {
        name: "last",
        prototype: "list.last",
        category: "list",
        description: "Get the last item of a list",
    },
    NodeTemplate {
        name: "rest",
        prototype: "list.rest",
        category: "list",
        description: "Get all items except the first",
    },
    NodeTemplate {
        name: "reverse",
        prototype: "list.reverse",
        category: "list",
        description: "Reverse the order of a list",
    },
    NodeTemplate {
        name: "shuffle",
        prototype: "list.shuffle",
        category: "list",
        description: "Randomize list order",
    },
    NodeTemplate {
        name: "slice",
        prototype: "list.slice",
        category: "list",
        description: "Take a portion of a list",
    },
    NodeTemplate {
        name: "shift",
        prototype: "list.shift",
        category: "list",
        description: "Shift list items by offset",
    },
    NodeTemplate {
        name: "repeat",
        prototype: "list.repeat",
        category: "list",
        description: "Repeat list items",
    },
    NodeTemplate {
        name: "list_sort",
        prototype: "list.sort",
        category: "list",
        description: "Sort a list",
    },
    NodeTemplate {
        name: "pick",
        prototype: "list.pick",
        category: "list",
        description: "Pick random items from a list",
    },
    NodeTemplate {
        name: "cull",
        prototype: "list.cull",
        category: "list",
        description: "Filter list items by boolean pattern",
    },
    NodeTemplate {
        name: "take_every",
        prototype: "list.take_every",
        category: "list",
        description: "Take every Nth item",
    },
    NodeTemplate {
        name: "distinct",
        prototype: "list.distinct",
        category: "list",
        description: "Remove duplicate items",
    },
    NodeTemplate {
        name: "switch",
        prototype: "list.switch",
        category: "list",
        description: "Select from multiple inputs",
    },
    NodeTemplate {
        name: "combine",
        prototype: "list.combine",
        category: "list",
        description: "Combine multiple lists into one",
    },
    NodeTemplate {
        name: "keys",
        prototype: "list.keys",
        category: "list",
        description: "Get the keys from a list of maps",
    },
    NodeTemplate {
        name: "zip_map",
        prototype: "list.zip_map",
        category: "list",
        description: "Combine keys and values into a map",
    },
    // ========================
    // Core nodes
    // ========================
    NodeTemplate {
        name: "frame",
        prototype: "core.frame",
        category: "core",
        description: "Get the current animation frame",
    },
    // ========================
    // Data nodes
    // ========================
    NodeTemplate {
        name: "import_text",
        prototype: "data.import_text",
        category: "data",
        description: "Import lines from a text file",
    },
    NodeTemplate {
        name: "import_csv",
        prototype: "data.import_csv",
        category: "data",
        description: "Import a CSV file as structured data",
    },
    NodeTemplate {
        name: "make_table",
        prototype: "data.make_table",
        category: "data",
        description: "Build a data table from lists",
    },
    NodeTemplate {
        name: "lookup",
        prototype: "data.lookup",
        category: "data",
        description: "Look up a value by key in data",
    },
    NodeTemplate {
        name: "filter_data",
        prototype: "data.filter_data",
        category: "data",
        description: "Filter data rows by key/value comparison",
    },
    // ========================
    // Network nodes
    // ========================
    NodeTemplate {
        name: "http_get",
        prototype: "network.http_get",
        category: "network",
        description: "Fetch content from a URL",
    },
    NodeTemplate {
        name: "encode_url",
        prototype: "network.encode_url",
        category: "network",
        description: "Percent-encode a URL string",
    },
];
