# NodeBox Rust Translation Plan

This document outlines a comprehensive plan for translating NodeBox from Java/Python to Rust, organized into three phases.

## Executive Summary

NodeBox is a ~230-file Java application with Python scripting support, implementing:
- A vector graphics system (Path, Geometry, Canvas, Transform)
- A node-graph evaluation engine with ~158 built-in operations
- XML-based file format (.ndbx) for node libraries and documents
- A Swing-based UI for visual node editing
- SVG/PDF/CSV export capabilities

The Rust translation will leverage Rust's strengths: memory safety, fearless concurrency, excellent CLI tooling, and strong type system.

---

## Phase 1: Rust Core Library

**Goal**: A headless, fully-tested Rust library that can parse `.ndbx` files and render SVG output from the command line.

### 1.1 Project Structure

Rust code lives at the repository root alongside the existing Java code. This allows easy cross-referencing during migration while keeping both build systems independent.

```
nodebox/
├── Cargo.toml                    # Rust workspace root
├── Cargo.lock
├── crates/                       # Rust crates
│   ├── nodebox-core/             # Core types and traits
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── geometry/         # Graphics primitives
│   │   │   │   ├── mod.rs
│   │   │   │   ├── point.rs
│   │   │   │   ├── rect.rs
│   │   │   │   ├── transform.rs
│   │   │   │   ├── color.rs
│   │   │   │   ├── contour.rs
│   │   │   │   ├── path.rs
│   │   │   │   ├── geometry.rs   # Multi-path container
│   │   │   │   ├── text.rs
│   │   │   │   └── canvas.rs
│   │   │   ├── node/             # Node graph model
│   │   │   │   ├── mod.rs
│   │   │   │   ├── node.rs
│   │   │   │   ├── port.rs
│   │   │   │   ├── connection.rs
│   │   │   │   ├── library.rs
│   │   │   │   └── context.rs    # Evaluation context
│   │   │   └── value.rs          # Runtime value types
│   │   └── Cargo.toml
│   │
│   ├── nodebox-ops/              # Built-in operations (functions)
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── corevector.rs     # 58 vector operations
│   │   │   ├── math.rs           # 41 math operations
│   │   │   ├── list.rs           # 21 list operations
│   │   │   ├── string.rs         # 24 string operations
│   │   │   ├── data.rs           # 5 data operations
│   │   │   └── color.rs          # 4 color operations
│   │   └── Cargo.toml
│   │
│   ├── nodebox-ndbx/             # NDBX file format parser/writer
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── parser.rs         # XML parsing (quick-xml)
│   │   │   ├── writer.rs         # XML serialization
│   │   │   └── upgrade.rs        # Version migration
│   │   └── Cargo.toml
│   │
│   ├── nodebox-render/           # Output renderers
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── svg.rs            # SVG renderer
│   │   │   ├── pdf.rs            # PDF renderer (optional)
│   │   │   └── csv.rs            # CSV data export
│   │   └── Cargo.toml
│   │
│   └── nodebox-cli/              # Command-line interface
│       ├── src/
│       │   └── main.rs
│       └── Cargo.toml
│
├── tests/                        # Rust integration tests
│   ├── golden/                   # Golden master SVG files
│   └── fixtures/                 # Test .ndbx files
│
├── benches/                      # Rust performance benchmarks
│
│── ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  # Java (kept for reference)
│
├── build.xml                     # Java Ant build
├── pom.xml                       # Java Maven dependencies
├── src/                          # Java source code
│   ├── main/java/nodebox/        # Java implementation
│   ├── main/python/              # Python modules (shared)
│   └── test/                     # Java tests
│
├── libraries/                    # .ndbx node libraries (shared!)
│   ├── corevector/
│   ├── math/
│   ├── list/
│   └── ...
│
└── docs/
    └── rust-translation-plan.md
```

**Key points:**
- `Cargo.toml` at root, `crates/` for Rust workspace members
- `ant build` still works for Java, `cargo build` for Rust
- `libraries/` directory is shared - both implementations read the same `.ndbx` files
- Easy to compare implementations side-by-side:
  - `src/main/java/nodebox/graphics/Path.java` ↔ `crates/nodebox-core/src/geometry/path.rs`
  - `src/main/java/nodebox/node/Node.java` ↔ `crates/nodebox-core/src/node/node.rs`

### 1.2 Graphics Package (`nodebox-core/geometry`)

#### 1.2.1 Core Types

```rust
// point.rs
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Point {
    pub x: f64,
    pub y: f64,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PointType {
    LineTo,
    CurveTo,
    CurveData,  // Control point for Bezier curves
}

#[derive(Clone, Debug)]
pub struct PathPoint {
    pub point: Point,
    pub point_type: PointType,
}

// rect.rs
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Rect {
    pub x: f64,
    pub y: f64,
    pub width: f64,
    pub height: f64,
}

// color.rs
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Color {
    pub r: f64,  // 0.0-1.0
    pub g: f64,
    pub b: f64,
    pub a: f64,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ColorMode {
    Rgb,
    Hsb,
    Cmyk,
}

// transform.rs
#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Transform {
    // 2D affine transform matrix [a, b, c, d, tx, ty]
    pub matrix: [f64; 6],
}
```

#### 1.2.2 Path and Geometry

```rust
// contour.rs
#[derive(Clone, Debug)]
pub struct Contour {
    pub points: Vec<PathPoint>,
    pub closed: bool,
}

// path.rs
#[derive(Clone, Debug)]
pub struct Path {
    pub contours: Vec<Contour>,
    pub fill: Option<Color>,
    pub stroke: Option<Color>,
    pub stroke_width: f64,
}

// geometry.rs - Multi-path container
#[derive(Clone, Debug, Default)]
pub struct Geometry {
    pub paths: Vec<Path>,
}

// canvas.rs
#[derive(Clone, Debug)]
pub struct Canvas {
    pub width: f64,
    pub height: f64,
    pub background: Option<Color>,
    pub items: Vec<Grob>,
}

// Graphic object enum
#[derive(Clone, Debug)]
pub enum Grob {
    Path(Path),
    Geometry(Geometry),
    Text(Text),
    Canvas(Box<Canvas>),
}
```

#### 1.2.3 Key Implementation Notes

| Java Feature | Rust Equivalent |
|--------------|-----------------|
| Immutable classes | `#[derive(Clone)]` with owned data |
| Builder pattern | Builder structs with `build()` methods |
| `ImmutableList<T>` | `Vec<T>` (owned) or `Arc<[T]>` (shared) |
| `null` checks | `Option<T>` |
| `AffineTransform` | Custom `Transform` or `kurbo::Affine` |
| `Rectangle2D` | Custom `Rect` or `kurbo::Rect` |

**Recommended Dependencies:**
- `kurbo` - 2D geometry primitives (MIT, well-tested)
- `palette` - Color space conversions (optional)

### 1.3 Node System (`nodebox-core/node`)

#### 1.3.1 Core Types

```rust
// port.rs
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
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PortRange {
    Value,  // Single value
    List,   // List of values
}

#[derive(Clone, Debug)]
pub struct Port {
    pub name: String,
    pub port_type: PortType,
    pub range: PortRange,
    pub value: Value,
    pub widget: Widget,
    pub min: Option<f64>,
    pub max: Option<f64>,
}

// connection.rs
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct Connection {
    pub output_node: String,
    pub input_node: String,
    pub input_port: String,
}

// node.rs
#[derive(Clone, Debug)]
pub struct Node {
    pub name: String,
    pub prototype: Option<String>,  // "corevector.ellipse"
    pub function: Option<String>,   // "corevector/ellipse"
    pub category: String,
    pub position: Point,
    pub inputs: Vec<Port>,
    pub output_type: PortType,
    pub output_range: PortRange,
    pub is_network: bool,
    pub children: Vec<Node>,        // If is_network
    pub rendered_child: Option<String>,
    pub connections: Vec<Connection>,
}
```

#### 1.3.2 Value Type (Runtime)

```rust
// value.rs - Dynamic value type for node evaluation
#[derive(Clone, Debug)]
pub enum Value {
    Null,
    Int(i64),
    Float(f64),
    String(String),
    Boolean(bool),
    Point(Point),
    Color(Color),
    Path(Path),
    Geometry(Geometry),
    List(Vec<Value>),
    Map(HashMap<String, Value>),
}

impl Value {
    pub fn as_float(&self) -> Option<f64>;
    pub fn as_geometry(&self) -> Option<&Geometry>;
    pub fn into_list(self) -> Vec<Value>;
    pub fn coerce_to(&self, target: PortType) -> Result<Value, TypeError>;
}
```

#### 1.3.3 Node Evaluation Context

```rust
// context.rs
pub struct NodeContext<'a> {
    library: &'a NodeLibrary,
    function_registry: &'a FunctionRegistry,
    render_cache: HashMap<String, Value>,
    frame: f64,
}

impl<'a> NodeContext<'a> {
    pub fn render_node(&mut self, path: &str) -> Result<Value, EvalError>;
    pub fn render_network(&mut self, network: &Node) -> Result<Value, EvalError>;

    // Handles list expansion (when input is list but function expects single value)
    fn expand_arguments(&self, node: &Node, args: &[Value]) -> Vec<Vec<Value>>;
}
```

### 1.4 Operations (`nodebox-ops`)

#### 1.4.1 Function Registry

```rust
pub type NodeFn = fn(&[Value]) -> Result<Value, EvalError>;

pub struct FunctionRegistry {
    functions: HashMap<String, NodeFn>,
}

impl FunctionRegistry {
    pub fn new() -> Self;
    pub fn register(&mut self, name: &str, f: NodeFn);
    pub fn get(&self, name: &str) -> Option<NodeFn>;
}

// Registration macro for cleaner syntax
macro_rules! register_ops {
    ($registry:expr, $($name:literal => $func:expr),* $(,)?) => {
        $($registry.register($name, $func);)*
    };
}
```

#### 1.4.2 Operations to Implement

**Priority 1 - Geometry Generators (10 operations):**
- `ellipse`, `rect`, `line`, `arc`, `polygon`, `star`
- `grid`, `lineAngle`, `quad_curve`, `link`

**Priority 2 - Geometry Manipulators (26 operations):**
- `translate`, `rotate`, `scale`, `skew`
- `colorize`, `copy`, `align`, `fit`, `fitTo`
- `connect`, `group`, `ungroup`, `resample`
- `compound` (boolean ops - use `lyon` or `geo`)
- `reflect`, `scatter`, `sort`, `stack`, `distribute`
- `pointOnPath`, `textpath`, `shape_on_path`, `text_on_path`
- `wiggle`, `snap`, `delete`, `round_segments`, `freehand`

**Priority 3 - Math (41 operations):**
- Basic: `add`, `subtract`, `multiply`, `divide`, `mod`, `negate`, `abs`, `sqrt`, `pow`
- Trig: `sin`, `cos`, `radians`, `degrees`, `pi`, `e`
- Aggregation: `sum`, `average`, `min`, `max`, `range`, `runningTotal`
- Rounding: `ceil`, `floor`, `round`, `convertRange`, `sample`
- Geometry: `angle`, `distance`, `coordinates`, `reflect`
- Logic: `compare`, `logicOperator`
- Random: `randomNumbers`, `wave`

**Priority 4 - List (21 operations):**
- Access: `first`, `second`, `last`, `rest`, `count`
- Transform: `combine`, `slice`, `shift`, `repeat`, `reverse`, `shuffle`, `pick`, `distinct`
- Filter: `cull`, `takeEvery`
- Sort: `sort`, `doSwitch`
- Data: `zipMap`, `keys`

**Priority 5 - String (24 operations):**
- Create: `string`, `makeStrings`
- Properties: `length`, `wordCount`
- Manipulate: `concatenate`, `changeCase`, `characters`, `characterAt`, `subString`, `replace`, `trim`, `formatNumber`
- Test: `contains`, `startsWith`, `endsWith`, `equal`
- Encode: `asBinaryString`, `asBinaryList`, `asNumberList`, `randomCharacter`

**Priority 6 - Data (5 operations):**
- `importText`, `importCSV`, `lookup`, `filterData`, `makeTable`

**Priority 7 - Color (4 operations):**
- `color`, `gray`, `rgb`, `hsb`

#### 1.4.3 Parallelism Opportunities

Rust's type system enables safe parallelism. Key opportunities:

```rust
use rayon::prelude::*;

// Parallel list operations
pub fn parallel_map<T, U, F>(items: &[T], f: F) -> Vec<U>
where
    T: Sync,
    U: Send,
    F: Fn(&T) -> U + Sync,
{
    items.par_iter().map(f).collect()
}

// Parallel node evaluation (independent branches)
impl NodeContext<'_> {
    pub fn render_children_parallel(&mut self, children: &[&str]) -> Vec<Result<Value, EvalError>> {
        // Safe because each child evaluation is independent
        children.par_iter()
            .map(|child| self.render_node(child))
            .collect()
    }
}

// Parallel geometry operations
impl Geometry {
    pub fn transform_parallel(&self, t: &Transform) -> Geometry {
        Geometry {
            paths: self.paths.par_iter()
                .map(|p| p.transform(t))
                .collect()
        }
    }
}
```

**Parallelizable Operations:**
- `copy` - Each copy transformation is independent
- `scatter` - Point generation is embarrassingly parallel
- `resample` - Each path can be resampled independently
- `colorize` - Color application to paths
- `sort` - Parallel sort algorithms (rayon)
- `compound` - Boolean ops on path pairs (with careful synchronization)
- List `map` operations - Via `rayon::par_iter()`

### 1.5 NDBX Parser (`nodebox-ndbx`)

#### 1.5.1 Format Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ndbx formatVersion="21" type="file" uuid="...">
    <property name="canvasWidth" value="1000"/>
    <property name="canvasHeight" value="1000"/>
    <link href="python:pyvector.py" rel="functions"/>
    <link href="java:nodebox.function.CoreVectorFunctions" rel="functions"/>

    <node name="root" prototype="core.network" renderedChild="ellipse1">
        <node name="ellipse1" position="1.00,7.00" prototype="corevector.ellipse">
            <port name="width" type="float" value="30.0"/>
        </node>
        <conn input="colorize1.shape" output="ellipse1"/>
    </node>
</ndbx>
```

#### 1.5.2 Parser Implementation

```rust
use quick_xml::Reader;
use quick_xml::events::Event;

pub struct NdbxParser {
    format_version: u32,
}

impl NdbxParser {
    pub fn parse<R: BufRead>(reader: R) -> Result<NodeLibrary, ParseError> {
        let mut xml = Reader::from_reader(reader);
        let mut buf = Vec::new();

        loop {
            match xml.read_event_into(&mut buf)? {
                Event::Start(e) => match e.name().as_ref() {
                    b"ndbx" => self.parse_header(&e)?,
                    b"property" => self.parse_property(&e)?,
                    b"link" => self.parse_link(&e)?,
                    b"node" => self.parse_node(&mut xml, &e)?,
                    b"conn" => self.parse_connection(&e)?,
                    _ => {}
                },
                Event::Eof => break,
                _ => {}
            }
            buf.clear();
        }

        Ok(self.build_library())
    }
}
```

**Recommended Dependencies:**
- `quick-xml` - Fast, low-allocation XML parser
- `uuid` - UUID generation/parsing

### 1.6 SVG Renderer (`nodebox-render`)

#### 1.6.1 Implementation

```rust
use std::fmt::Write;

pub struct SvgRenderer {
    precision: usize,
}

impl SvgRenderer {
    pub fn render(&self, geometry: &Geometry, bounds: Rect) -> String {
        let mut svg = String::new();

        writeln!(svg, r#"<?xml version="1.0" encoding="UTF-8"?>"#).unwrap();
        writeln!(svg, r#"<svg xmlns="http://www.w3.org/2000/svg""#).unwrap();
        writeln!(svg, r#"     width="{}" height="{}""#, bounds.width, bounds.height).unwrap();
        writeln!(svg, r#"     viewBox="{} {} {} {}">"#,
            bounds.x, bounds.y, bounds.width, bounds.height).unwrap();

        for path in &geometry.paths {
            self.render_path(&mut svg, path);
        }

        writeln!(svg, "</svg>").unwrap();
        svg
    }

    fn render_path(&self, svg: &mut String, path: &Path) {
        write!(svg, r#"<path d=""#).unwrap();

        for contour in &path.contours {
            let mut first = true;
            for point in &contour.points {
                match (first, point.point_type) {
                    (true, _) => write!(svg, "M{},{}", point.point.x, point.point.y),
                    (false, PointType::LineTo) => write!(svg, "L{},{}", point.point.x, point.point.y),
                    (false, PointType::CurveTo) => write!(svg, "C..."), // Bezier
                    _ => Ok(()),
                }.unwrap();
                first = false;
            }
            if contour.closed {
                write!(svg, "Z").unwrap();
            }
        }

        write!(svg, r#"""#).unwrap();

        // Fill and stroke
        if let Some(fill) = &path.fill {
            write!(svg, r#" fill="{}""#, fill.to_css()).unwrap();
        } else {
            write!(svg, r#" fill="none""#).unwrap();
        }

        if let Some(stroke) = &path.stroke {
            write!(svg, r#" stroke="{}" stroke-width="{}""#,
                stroke.to_css(), path.stroke_width).unwrap();
        }

        writeln!(svg, "/>").unwrap();
    }
}
```

### 1.7 CLI Application (`nodebox-cli`)

```rust
use clap::Parser;

#[derive(Parser)]
#[command(name = "nodebox")]
#[command(about = "NodeBox command-line renderer")]
struct Cli {
    /// Input .ndbx file
    #[arg(short, long)]
    input: PathBuf,

    /// Output file (SVG, PDF, or PNG)
    #[arg(short, long)]
    output: PathBuf,

    /// Frame number to render
    #[arg(short, long, default_value = "1")]
    frame: f64,

    /// Canvas width override
    #[arg(long)]
    width: Option<f64>,

    /// Canvas height override
    #[arg(long)]
    height: Option<f64>,

    /// Number of parallel threads
    #[arg(short = 'j', long, default_value = "0")]
    threads: usize,
}

fn main() -> Result<(), Box<dyn Error>> {
    let cli = Cli::parse();

    // Configure thread pool
    if cli.threads > 0 {
        rayon::ThreadPoolBuilder::new()
            .num_threads(cli.threads)
            .build_global()?;
    }

    // Parse input
    let library = nodebox_ndbx::parse_file(&cli.input)?;

    // Build function registry
    let registry = nodebox_ops::default_registry();

    // Create evaluation context
    let mut ctx = NodeContext::new(&library, &registry);
    ctx.set_frame(cli.frame);

    // Render
    let result = ctx.render_node("/")?;
    let geometry = result.as_geometry()
        .ok_or("Root node did not produce geometry")?;

    // Export based on output extension
    match cli.output.extension().and_then(|e| e.to_str()) {
        Some("svg") => {
            let svg = SvgRenderer::new().render(geometry, bounds);
            std::fs::write(&cli.output, svg)?;
        }
        Some("pdf") => {
            nodebox_render::pdf::render_to_file(geometry, &cli.output)?;
        }
        _ => return Err("Unsupported output format".into()),
    }

    Ok(())
}
```

### 1.8 Testing Strategy

#### 1.8.1 Golden Master Testing

```rust
// tests/golden_tests.rs
use nodebox_core::*;
use nodebox_ndbx::*;
use nodebox_render::svg::*;
use std::fs;

#[test]
fn test_ellipse_golden() {
    let library = parse_file("tests/fixtures/ellipse.ndbx").unwrap();
    let registry = nodebox_ops::default_registry();
    let mut ctx = NodeContext::new(&library, &registry);

    let result = ctx.render_node("/").unwrap();
    let svg = SvgRenderer::new().render(result.as_geometry().unwrap(), bounds);

    let expected = fs::read_to_string("tests/golden/ellipse.svg").unwrap();
    assert_eq!(normalize_svg(&svg), normalize_svg(&expected));
}

fn normalize_svg(svg: &str) -> String {
    // Normalize whitespace, floating-point precision for comparison
    svg.lines()
        .map(|l| l.trim())
        .collect::<Vec<_>>()
        .join("\n")
}
```

#### 1.8.2 Test Categories

```
tests/
├── golden/                      # Golden master SVGs
│   ├── primitives/
│   │   ├── ellipse.svg
│   │   ├── rect.svg
│   │   ├── line.svg
│   │   └── ...
│   ├── transforms/
│   │   ├── translate.svg
│   │   ├── rotate.svg
│   │   └── ...
│   └── compound/
│       ├── boolean_union.svg
│       └── ...
├── fixtures/                    # Test .ndbx files
│   ├── primitives/
│   ├── transforms/
│   └── compound/
├── unit/                        # Unit tests
│   ├── geometry_tests.rs
│   ├── node_tests.rs
│   └── parser_tests.rs
└── integration/
    └── cli_tests.rs
```

#### 1.8.3 Property-Based Testing

```rust
use proptest::prelude::*;

proptest! {
    #[test]
    fn transform_composition_is_associative(
        t1 in any_transform(),
        t2 in any_transform(),
        t3 in any_transform(),
    ) {
        let a = t1.concat(&t2).concat(&t3);
        let b = t1.concat(&t2.concat(&t3));
        assert_transforms_equal(a, b);
    }

    #[test]
    fn path_bounds_contains_all_points(path in any_path()) {
        let bounds = path.bounds();
        for contour in &path.contours {
            for point in &contour.points {
                assert!(bounds.contains(point.point));
            }
        }
    }
}
```

### 1.9 Migration from Java Test Files

The existing Java test files can be converted:

| Java Test File | Rust Equivalent |
|----------------|-----------------|
| `PathTest.java` | `tests/unit/path_tests.rs` |
| `PointTest.java` | `tests/unit/point_tests.rs` |
| `TransformTest.java` | `tests/unit/transform_tests.rs` |
| `NodeTest.java` | `tests/unit/node_tests.rs` |
| `SVGRendererTest.java` | `tests/unit/svg_tests.rs` |
| `*FunctionsTest.java` | `tests/unit/ops/*.rs` |

### 1.10 Phase 1 Dependencies Summary

```toml
[workspace.dependencies]
# Core
kurbo = "0.11"           # 2D geometry primitives
palette = "0.7"          # Color space conversions (optional)

# Parsing
quick-xml = "0.31"       # XML parsing
uuid = "1.6"             # UUID handling

# CLI
clap = { version = "4.4", features = ["derive"] }

# Parallelism
rayon = "1.8"

# Boolean operations (for compound)
lyon_algorithms = "1.0"  # Path algorithms
geo = "0.27"             # Geometric operations

# Testing
proptest = "1.4"         # Property-based testing
similar = "2.4"          # Diff for golden tests
insta = "1.34"           # Snapshot testing (alternative)

# PDF (optional)
pdf-writer = "0.9"       # PDF generation
```

### 1.11 Phase 1 Milestones

| Milestone | Description | Deliverables |
|-----------|-------------|--------------|
| **M1.1** | Geometry primitives | Point, Rect, Transform, Color, Path, Geometry |
| **M1.2** | Node model | Node, Port, Connection, Value types |
| **M1.3** | NDBX parser | Parse/write .ndbx files |
| **M1.4** | Basic operations | Geometry generators (ellipse, rect, line, etc.) |
| **M1.5** | Transform operations | translate, rotate, scale, copy |
| **M1.6** | SVG renderer | Full SVG export |
| **M1.7** | Math/List/String ops | All non-visual operations |
| **M1.8** | Advanced geometry | compound, resample, boolean ops |
| **M1.9** | CLI tool | Full command-line interface |
| **M1.10** | Golden master tests | 100+ golden test cases |
| **M1.11** | Parallelism | Rayon integration for list ops |

---

## Phase 2: GUI Implementation

**Goal**: A native GUI that resembles NodeBox's current interface but modernized.

### 2.1 GUI Framework Options

#### Option 1: egui + eframe (Recommended)

**egui** is an immediate-mode GUI library, similar to Dear ImGui.

**Pros:**
- Pure Rust, excellent documentation
- Immediate-mode = easy to prototype
- Built-in widgets for node editors
- Cross-platform (Windows, macOS, Linux, Web)
- Active development and community
- Good integration with GPU rendering

**Cons:**
- Immediate-mode can be less efficient for complex UIs
- Custom styling requires more work
- Not native look-and-feel

```rust
// Example: Basic node editor with egui
use egui_node_graph::{Graph, NodeId, InputId, OutputId};

struct NodeBoxApp {
    graph: Graph<NodeData, DataType, ValueType>,
    viewer: ViewerState,
}

impl eframe::App for NodeBoxApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        // Left panel: Node graph editor
        egui::SidePanel::left("network").show(ctx, |ui| {
            self.graph.draw(ui);
        });

        // Center: Canvas viewer
        egui::CentralPanel::default().show(ctx, |ui| {
            self.viewer.draw(ui, &self.render_result);
        });

        // Right panel: Parameters
        egui::SidePanel::right("parameters").show(ctx, |ui| {
            self.draw_port_editor(ui);
        });
    }
}
```

**Key Libraries:**
- `egui` - Core UI framework
- `eframe` - Native window integration
- `egui_node_graph` - Node graph widget (or custom)
- `egui_extras` - Additional widgets

#### Option 2: iced

**iced** is an Elm-inspired, reactive GUI library.

**Pros:**
- Elm architecture (Model-View-Update)
- Clean, functional API
- Cross-platform
- Native styling on some platforms
- Good for complex state management

**Cons:**
- Steeper learning curve
- Less mature than egui
- Node graph editor would need to be built from scratch
- Slower iteration than immediate-mode

```rust
// Example: iced application structure
use iced::{Application, Command, Element, Settings};

struct NodeBox {
    library: NodeLibrary,
    selected_node: Option<String>,
    render_result: Option<Geometry>,
}

#[derive(Debug, Clone)]
enum Message {
    NodeSelected(String),
    PortValueChanged(String, String, Value),
    ConnectionCreated(Connection),
    Render,
}

impl Application for NodeBox {
    type Message = Message;

    fn update(&mut self, message: Message) -> Command<Message> {
        match message {
            Message::NodeSelected(name) => {
                self.selected_node = Some(name);
            }
            Message::PortValueChanged(node, port, value) => {
                self.library.set_port_value(&node, &port, value);
                return Command::perform(async {}, |_| Message::Render);
            }
            // ...
        }
        Command::none()
    }

    fn view(&self) -> Element<Message> {
        // Build view hierarchy
    }
}
```

#### Option 3: Slint

**Slint** uses a declarative markup language with Rust backend.

**Pros:**
- Declarative UI definition (.slint files)
- Native look-and-feel
- Commercial support available
- Good for complex layouts
- Live preview during development

**Cons:**
- Separate markup language to learn
- Node graph editor needs custom implementation
- Smaller community than egui
- GPL license (or commercial)

```slint
// Example: Slint UI definition
import { Button, Slider } from "std-widgets.slint";

component PortEditor {
    in property <string> name;
    in property <float> value;
    callback value-changed(float);

    HorizontalLayout {
        Text { text: name; }
        Slider {
            value: value;
            changed => { value-changed(self.value); }
        }
    }
}

export component MainWindow inherits Window {
    // Layout definition
}
```

#### Option 4: Tauri + Web UI

**Tauri** wraps a web frontend with a Rust backend.

**Pros:**
- Full power of web UI (React, Vue, etc.)
- Mature node graph libraries exist (React Flow, etc.)
- Easy to make beautiful UIs
- Smallest binary size
- Cross-platform including mobile

**Cons:**
- Two languages (Rust + TypeScript/JavaScript)
- WebView dependency
- IPC overhead between frontend and backend
- Not "pure Rust"

### 2.2 Recommendation Matrix

| Criterion | egui | iced | Slint | Tauri |
|-----------|------|------|-------|-------|
| Learning curve | Easy | Medium | Medium | Easy (if know web) |
| Node graph support | Good (libs exist) | Build from scratch | Build from scratch | Excellent (React Flow) |
| Performance | Excellent | Good | Good | Good |
| Look & feel | Custom | Partial native | Native | Web |
| Pure Rust | Yes | Yes | Yes | No (web frontend) |
| Community | Large | Medium | Small | Large |
| Maturity | Mature | Maturing | Maturing | Mature |

**Recommendation: egui + eframe**

For a NodeBox-style application, egui is the best fit because:
1. Immediate-mode is perfect for interactive tools
2. `egui_node_graph` provides node graph editing
3. Custom canvas rendering is straightforward
4. Active community with lots of examples
5. Pure Rust, single language

### 2.3 GUI Architecture

```
crates/nodebox-gui/
├── src/
│   ├── main.rs
│   ├── app.rs              # Main application state
│   ├── widgets/
│   │   ├── mod.rs
│   │   ├── network_view.rs # Node graph editor
│   │   ├── viewer.rs       # Canvas output viewer
│   │   ├── port_editor.rs  # Parameter editor
│   │   ├── address_bar.rs  # Network breadcrumb
│   │   └── animation.rs    # Timeline controls
│   ├── handles/
│   │   ├── mod.rs
│   │   ├── point_handle.rs
│   │   └── transform_handle.rs
│   └── render/
│       ├── mod.rs
│       └── gpu_canvas.rs   # GPU-accelerated rendering
└── Cargo.toml
```

### 2.4 Key UI Components

#### 2.4.1 Network View (Node Graph Editor)

```rust
pub struct NetworkView {
    nodes: HashMap<NodeId, NodeWidget>,
    connections: Vec<ConnectionWidget>,
    selected: HashSet<NodeId>,
    pan: Vec2,
    zoom: f32,
    dragging: Option<DragState>,
}

impl NetworkView {
    pub fn ui(&mut self, ui: &mut egui::Ui, library: &mut NodeLibrary) {
        let (response, painter) = ui.allocate_painter(
            ui.available_size(),
            egui::Sense::click_and_drag(),
        );

        // Handle pan/zoom
        self.handle_input(&response);

        // Transform to view coordinates
        let transform = egui::emath::TSTransform::new(self.pan, self.zoom);

        // Draw connections first (behind nodes)
        for conn in &self.connections {
            self.draw_connection(&painter, conn, &transform);
        }

        // Draw nodes
        for (id, node) in &self.nodes {
            self.draw_node(&painter, ui, node, &transform, self.selected.contains(id));
        }

        // Handle connection dragging
        if let Some(DragState::Connecting { from, to }) = &self.dragging {
            self.draw_wire(&painter, *from, *to);
        }
    }
}
```

#### 2.4.2 Canvas Viewer

```rust
pub struct Viewer {
    texture: Option<egui::TextureHandle>,
    geometry: Option<Geometry>,
    zoom: f32,
    pan: Vec2,
    show_handles: bool,
    active_handle: Option<Box<dyn Handle>>,
}

impl Viewer {
    pub fn ui(&mut self, ui: &mut egui::Ui) {
        let (response, painter) = ui.allocate_painter(
            ui.available_size(),
            egui::Sense::click_and_drag(),
        );

        // Draw background
        painter.rect_filled(response.rect, 0.0, egui::Color32::WHITE);

        // Draw geometry
        if let Some(ref geo) = self.geometry {
            self.render_geometry(&painter, geo);
        }

        // Draw handles
        if self.show_handles {
            if let Some(ref mut handle) = self.active_handle {
                handle.draw(&painter, &response);
            }
        }
    }

    fn render_geometry(&self, painter: &egui::Painter, geo: &Geometry) {
        for path in &geo.paths {
            let shape = self.path_to_egui_shape(path);
            painter.add(shape);
        }
    }
}
```

### 2.5 Phase 2 Milestones

| Milestone | Description |
|-----------|-------------|
| **M2.1** | Basic window with egui |
| **M2.2** | Canvas viewer with geometry rendering |
| **M2.3** | Node graph widget (using egui_node_graph or custom) |
| **M2.4** | Port/parameter editor panel |
| **M2.5** | Node selection and editing |
| **M2.6** | Connection creation/deletion |
| **M2.7** | Interactive handles on canvas |
| **M2.8** | File open/save dialogs |
| **M2.9** | Undo/redo system |
| **M2.10** | Animation timeline |
| **M2.11** | Export dialogs (SVG, PDF, PNG) |

---

## Phase 3: Python Extensibility

**Goal**: Support Python nodes for accessing the rich Python ecosystem.

### 3.1 Python Integration Options

#### Option 1: PyO3 (Recommended)

**PyO3** provides Rust bindings to Python via the C API.

**Pros:**
- Most mature Rust-Python integration
- Can embed Python interpreter or be called from Python
- Good performance
- Active development
- Supports async Python

**Cons:**
- Links to specific Python version at compile time
- GIL (Global Interpreter Lock) limits parallelism
- Binary must match Python version on target system

```rust
use pyo3::prelude::*;
use pyo3::types::{PyDict, PyList};

pub struct PythonRuntime {
    // Held across calls
}

impl PythonRuntime {
    pub fn new() -> PyResult<Self> {
        pyo3::prepare_freethreaded_python();
        Ok(Self {})
    }

    pub fn load_module(&self, path: &Path) -> PyResult<PythonModule> {
        Python::with_gil(|py| {
            // Add path to sys.path
            let sys = py.import("sys")?;
            let sys_path: &PyList = sys.getattr("path")?.downcast()?;
            sys_path.insert(0, path.parent().unwrap().to_str().unwrap())?;

            // Import module
            let module_name = path.file_stem().unwrap().to_str().unwrap();
            let module = py.import(module_name)?;

            // Discover functions
            let functions = self.discover_functions(py, &module)?;

            Ok(PythonModule { module: module.into(), functions })
        })
    }

    pub fn call_function(
        &self,
        module: &PythonModule,
        name: &str,
        args: &[Value],
    ) -> PyResult<Value> {
        Python::with_gil(|py| {
            let func = module.module.as_ref(py).getattr(name)?;

            // Convert args to Python
            let py_args: Vec<PyObject> = args.iter()
                .map(|v| value_to_python(py, v))
                .collect::<PyResult<_>>()?;

            // Call function
            let result = func.call1(PyTuple::new(py, &py_args))?;

            // Convert result back to Rust
            python_to_value(py, result)
        })
    }
}

fn value_to_python(py: Python<'_>, value: &Value) -> PyResult<PyObject> {
    match value {
        Value::Int(i) => Ok(i.into_py(py)),
        Value::Float(f) => Ok(f.into_py(py)),
        Value::String(s) => Ok(s.into_py(py)),
        Value::Boolean(b) => Ok(b.into_py(py)),
        Value::Point(p) => {
            // Create nodebox.graphics.Point
            let point_class = py.import("nodebox.graphics")?.getattr("Point")?;
            Ok(point_class.call1((p.x, p.y))?.into())
        }
        Value::Geometry(g) => {
            // Convert Geometry to Python
            geometry_to_python(py, g)
        }
        Value::List(items) => {
            let py_items: Vec<PyObject> = items.iter()
                .map(|v| value_to_python(py, v))
                .collect::<PyResult<_>>()?;
            Ok(PyList::new(py, &py_items).into())
        }
        _ => Err(PyErr::new::<pyo3::exceptions::PyTypeError, _>(
            "Unsupported value type"
        )),
    }
}
```

#### Option 2: RustPython

**RustPython** is a Python interpreter written in Rust.

**Pros:**
- Pure Rust, no external Python dependency
- Can be compiled to WebAssembly
- No GIL issues
- Sandboxable

**Cons:**
- Not fully Python 3 compatible
- Missing many C extension modules (numpy, etc.)
- Slower than CPython for most workloads
- Less mature

#### Option 3: Subprocess Communication

Run Python as a separate process, communicate via JSON/MessagePack.

**Pros:**
- Complete isolation
- Any Python version
- No linking issues
- Crash isolation

**Cons:**
- IPC overhead
- Complex geometry serialization
- Harder to debug
- Process management complexity

### 3.2 Recommendation: PyO3 with Fallback

Use PyO3 as the primary integration, with optional RustPython for environments without Python.

### 3.3 Python Bridge Architecture

```
crates/nodebox-python/
├── src/
│   ├── lib.rs
│   ├── runtime.rs      # Python interpreter management
│   ├── module.rs       # Module loading and function discovery
│   ├── convert.rs      # Value conversion (Rust <-> Python)
│   └── bridge/
│       ├── mod.rs
│       ├── geometry.rs # Geometry type bindings
│       ├── path.rs     # Path type bindings
│       └── color.rs    # Color type bindings
├── python/
│   └── nodebox/
│       ├── __init__.py
│       ├── graphics.py # Python-side geometry classes
│       └── compat.py   # Compatibility layer
└── Cargo.toml
```

### 3.4 Python-Side API

```python
# nodebox/graphics.py
"""
Python-side geometry classes that mirror Rust types.
These are passed to/from Rust via PyO3.
"""

class Point:
    def __init__(self, x: float, y: float):
        self.x = x
        self.y = y

    def __add__(self, other: 'Point') -> 'Point':
        return Point(self.x + other.x, self.y + other.y)

class Color:
    def __init__(self, r: float, g: float, b: float, a: float = 1.0):
        self.r = r
        self.g = g
        self.b = b
        self.a = a

class Path:
    """Wrapper around Rust Path, with Pythonic API."""

    def __init__(self, _rust_path=None):
        self._rust_path = _rust_path or _create_path()

    def moveto(self, x: float, y: float) -> 'Path':
        return Path(_path_moveto(self._rust_path, x, y))

    def lineto(self, x: float, y: float) -> 'Path':
        return Path(_path_lineto(self._rust_path, x, y))

    def curveto(self, x1, y1, x2, y2, x3, y3) -> 'Path':
        return Path(_path_curveto(self._rust_path, x1, y1, x2, y2, x3, y3))

    def close(self) -> 'Path':
        return Path(_path_close(self._rust_path))

class Geometry:
    """Collection of paths."""

    def __init__(self, paths=None):
        self.paths = paths or []

    def add(self, path: Path) -> 'Geometry':
        return Geometry(self.paths + [path])
```

### 3.5 Rust-Side Bindings

```rust
// bridge/geometry.rs
use pyo3::prelude::*;

#[pyclass(name = "Point")]
#[derive(Clone)]
pub struct PyPoint {
    #[pyo3(get, set)]
    pub x: f64,
    #[pyo3(get, set)]
    pub y: f64,
}

#[pymethods]
impl PyPoint {
    #[new]
    fn new(x: f64, y: f64) -> Self {
        PyPoint { x, y }
    }

    fn __add__(&self, other: &PyPoint) -> PyPoint {
        PyPoint {
            x: self.x + other.x,
            y: self.y + other.y,
        }
    }
}

impl From<Point> for PyPoint {
    fn from(p: Point) -> Self {
        PyPoint { x: p.x, y: p.y }
    }
}

impl From<PyPoint> for Point {
    fn from(p: PyPoint) -> Self {
        Point { x: p.x, y: p.y }
    }
}

#[pyclass(name = "Path")]
pub struct PyPath {
    pub inner: Path,
}

#[pymethods]
impl PyPath {
    #[new]
    fn new() -> Self {
        PyPath { inner: Path::new() }
    }

    fn moveto(&self, x: f64, y: f64) -> PyPath {
        PyPath {
            inner: self.inner.clone().move_to(Point { x, y }),
        }
    }

    fn lineto(&self, x: f64, y: f64) -> PyPath {
        PyPath {
            inner: self.inner.clone().line_to(Point { x, y }),
        }
    }

    // ... more methods
}

#[pymodule]
fn nodebox_core(_py: Python<'_>, m: &PyModule) -> PyResult<()> {
    m.add_class::<PyPoint>()?;
    m.add_class::<PyPath>()?;
    m.add_class::<PyGeometry>()?;
    m.add_class::<PyColor>()?;
    Ok(())
}
```

### 3.6 Function Discovery and Registration

```rust
// module.rs
pub struct PythonModule {
    module: Py<PyModule>,
    functions: HashMap<String, PythonFunction>,
}

pub struct PythonFunction {
    name: String,
    callable: Py<PyAny>,
    // Optional: parsed type hints for validation
    arg_types: Vec<PortType>,
    return_type: PortType,
}

impl PythonModule {
    pub fn discover_functions(py: Python<'_>, module: &PyModule) -> PyResult<HashMap<String, PythonFunction>> {
        let mut functions = HashMap::new();

        let inspect = py.import("inspect")?;
        let is_function = inspect.getattr("isfunction")?;

        for (name, obj) in module.dict().iter() {
            let name: String = name.extract()?;

            // Skip private functions
            if name.starts_with('_') {
                continue;
            }

            // Check if it's a function
            if is_function.call1((obj,))?.is_true()? {
                // Try to extract type hints
                let annotations = obj.getattr("__annotations__").ok();
                let (arg_types, return_type) = if let Some(ann) = annotations {
                    parse_type_hints(py, ann)?
                } else {
                    (vec![], PortType::Geometry) // Default
                };

                functions.insert(name.clone(), PythonFunction {
                    name,
                    callable: obj.into(),
                    arg_types,
                    return_type,
                });
            }
        }

        Ok(functions)
    }
}
```

### 3.7 Integration with Node System

```rust
// In nodebox-core, extend FunctionRegistry
impl FunctionRegistry {
    pub fn register_python_module(&mut self, namespace: &str, module: PythonModule) {
        for (name, func) in module.functions {
            let full_name = format!("{}/{}", namespace, name);

            // Create a wrapper that calls Python
            let callable = func.callable.clone();
            let wrapper: NodeFn = Box::new(move |args: &[Value]| {
                Python::with_gil(|py| {
                    // Convert args
                    let py_args: Vec<PyObject> = args.iter()
                        .map(|v| value_to_python(py, v))
                        .collect::<PyResult<_>>()
                        .map_err(|e| EvalError::PythonError(e.to_string()))?;

                    // Call
                    let result = callable.as_ref(py)
                        .call1(PyTuple::new(py, &py_args))
                        .map_err(|e| EvalError::PythonError(e.to_string()))?;

                    // Convert result
                    python_to_value(py, result)
                        .map_err(|e| EvalError::PythonError(e.to_string()))
                })
            });

            self.register(&full_name, wrapper);
        }
    }
}
```

### 3.8 Handling the GIL and Parallelism

The Python GIL prevents true parallelism for Python code. Strategy:

```rust
impl NodeContext<'_> {
    pub fn render_node_parallel(&mut self, nodes: &[&str]) -> Vec<Result<Value, EvalError>> {
        // Partition into Python and Rust nodes
        let (python_nodes, rust_nodes): (Vec<_>, Vec<_>) = nodes
            .iter()
            .partition(|n| self.is_python_node(n));

        // Run Rust nodes in parallel
        let rust_results: Vec<_> = rust_nodes
            .par_iter()
            .map(|n| self.render_node(n))
            .collect();

        // Run Python nodes sequentially (GIL)
        let python_results: Vec<_> = python_nodes
            .iter()
            .map(|n| self.render_node(n))
            .collect();

        // Merge results in original order
        self.merge_results(nodes, rust_results, python_results)
    }
}
```

### 3.9 Compatibility with Existing Python Code

To support existing NodeBox Python scripts (`pyvector.py`, etc.):

```python
# nodebox/compat.py
"""
Compatibility layer for existing NodeBox Python code.
Maps old Java-based APIs to new Rust-based APIs.
"""

from nodebox_core import Point, Path, Geometry, Color

# Alias for backwards compatibility
from nodebox_core import Point as Pt

def polygon(x, y, radius, sides, align=True):
    """
    Create a polygon path.
    Compatible with existing pyvector.polygon().
    """
    from math import pi, sin, cos

    path = Path()
    angle_step = 2 * pi / sides
    start_angle = -pi / 2 if align else 0

    for i in range(sides):
        angle = start_angle + i * angle_step
        px = x + radius * cos(angle)
        py = y + radius * sin(angle)
        if i == 0:
            path = path.moveto(px, py)
        else:
            path = path.lineto(px, py)

    return path.close()
```

### 3.10 Phase 3 Milestones

| Milestone | Description |
|-----------|-------------|
| **M3.1** | PyO3 integration scaffold |
| **M3.2** | Point, Color type bindings |
| **M3.3** | Path, Geometry type bindings |
| **M3.4** | Python module loading |
| **M3.5** | Function discovery and registration |
| **M3.6** | Value conversion (bidirectional) |
| **M3.7** | Integration with FunctionRegistry |
| **M3.8** | Compatibility layer for existing scripts |
| **M3.9** | Port existing pyvector.py |
| **M3.10** | Documentation for writing Python nodes |

---

## Timeline Overview

```
Phase 1: Core Library (3-4 months)
├── M1.1-M1.3: Foundation (4 weeks)
│   └── Geometry, Node model, NDBX parser
├── M1.4-M1.6: Basic operations (4 weeks)
│   └── Generators, transforms, SVG output
├── M1.7-M1.8: Advanced operations (4 weeks)
│   └── Math, list, string, compound
└── M1.9-M1.11: CLI and testing (4 weeks)
    └── CLI tool, golden masters, parallelism

Phase 2: GUI (2-3 months)
├── M2.1-M2.4: Core UI (4 weeks)
│   └── Window, viewer, node graph, parameters
├── M2.5-M2.7: Interaction (3 weeks)
│   └── Selection, connections, handles
└── M2.8-M2.11: Polish (3 weeks)
    └── File dialogs, undo/redo, animation, export

Phase 3: Python (1-2 months)
├── M3.1-M3.4: Bridge (3 weeks)
│   └── PyO3 setup, type bindings, module loading
├── M3.5-M3.7: Integration (2 weeks)
│   └── Function discovery, registry integration
└── M3.8-M3.10: Compatibility (2 weeks)
    └── Compat layer, port existing scripts, docs
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Boolean operations complexity | Use proven `lyon` or `geo` crate |
| Python GIL performance | Partition work, use Rust for heavy lifting |
| egui node graph limitations | Custom implementation if needed |
| NDBX format edge cases | Comprehensive test suite from Java version |
| Cross-platform GUI issues | CI testing on all platforms |
| Font/text rendering | Use `cosmic-text` or `ab_glyph` |

---

## Success Criteria

### Phase 1 Complete When:
- [ ] CLI can parse any valid `.ndbx` file from NodeBox 3
- [ ] All 158 built-in operations implemented
- [ ] SVG output matches Java version (golden master tests pass)
- [ ] Parallel execution shows speedup on multi-core

### Phase 2 Complete When:
- [ ] GUI can open, edit, and save `.ndbx` files
- [ ] Node graph editing works (create, delete, connect)
- [ ] Parameter editing works with appropriate widgets
- [ ] Canvas handles work for interactive editing
- [ ] Undo/redo works

### Phase 3 Complete When:
- [ ] Python modules can be loaded from `.ndbx` links
- [ ] Existing `pyvector.py` works without modification
- [ ] Users can write new Python nodes
- [ ] Documentation exists for Python node development
