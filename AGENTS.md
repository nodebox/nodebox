# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java` holds the core Java application (`nodebox.*` packages).
- `src/main/python` contains bundled Python node libraries and helpers.
- `src/main/resources` stores runtime assets and `version.properties`.
- `src/test/java` contains JUnit tests; `src/test/python` and `src/test/clojure` hold language fixtures.
- `libraries/` and `examples/` ship built-in node libraries and example projects.
- `res/`, `artwork/`, and `platform/` contain assets and platform-specific launchers.
- `build/` and `dist/` are generated outputs; avoid manual edits.
- `build.xml` (Ant) and `pom.xml` (Maven deps) define the build and test pipeline.

## Build, Test, and Development Commands
- `ant run` builds and launches NodeBox.
- `ant test` compiles and runs JUnit tests; XML reports land in `reports/`.
- `ant generate-test-reports` renders HTML reports from `reports/TEST-*.xml`.
- `ant dist-mac` / `ant dist-win` create packaged apps in `dist/`.
- `ant clean` removes build artifacts.

Prereqs: Java JDK and Apache Ant are required; Maven is used for dependency resolution (see `README.md`).

## Coding Style & Naming Conventions
- Java: 4-space indentation, braces on the same line, and standard Java naming (classes `UpperCamelCase`, methods `lowerCamelCase`, constants `UPPER_SNAKE_CASE`).
- Python: follow existing API naming (many public helpers are `lowerCamelCase`), keep function signatures consistent with current modules.
- Keep edits localized and match the surrounding file’s formatting and ordering.

## Testing Guidelines
- JUnit is the primary test framework; tests are discovered by `**/*Test.class` in `src/test/java`.
- Name new Java tests `SomethingTest.java` and keep them close to the package they cover.
- Run `ant test` before shipping changes that affect core behavior or UI flows.

## Branching Strategy
- **Use `rewrite-in-rust` as the main branch.** All new development and PRs should target this branch.
- **NEVER commit or merge directly into `master`.** The `master` branch exists for legacy reasons only and should not be modified.
- Create feature branches from `rewrite-in-rust` and merge back into it.

## Commit & Pull Request Guidelines
- Recent history favors short, sentence-style commit messages (e.g., "Use Ctrl key on Windows."). Keep messages concise and specific.
- PRs should describe the user-visible change, list test commands run, and include screenshots or recordings for UI updates.
- Link relevant issues or tickets when applicable.

## Notes for Contributors
- Versioning lives in `src/main/resources/version.properties`; update it when preparing a release build.
- **NEVER modify the Java code** (`src/main/java`). The Java codebase is legacy and read-only; use it only as a reference. All new development happens in the Rust crates under `crates/`.

## Async Node Implementation

For nodes that perform I/O operations or expensive computations, see **[docs/async_nodes.md](docs/async_nodes.md)** for:
- Cancellation token usage
- Async I/O patterns with smol
- Best practices for responsive cancellation
- Testing async-aware nodes

## Node Definitions and Implementations

Node definitions and their implementations are split across multiple locations:

### Authoritative Node Definitions (Java `.ndbx` files)
- `libraries/corevector/corevector.ndbx` — XML definitions for core vector nodes (authoritative source)
- Each `<node>` element specifies:
  - `function` — the implementation to call (e.g., `corevector/grid`)
  - `outputType` — the data type produced (e.g., `point`, `geometry`)
  - `outputRange` — whether output is `value` (single) or `list` (multiple)
  - `<port>` elements — input parameters with types, defaults, and widgets

### Java Function Implementations (reference only)
- **Java** (`corevector/*`): `src/main/java/nodebox/function/CoreVectorFunctions.java`
- **Python** (`pyvector/*`): `src/main/python/` modules

### Rust Implementations
- **Node operations**: `crates/nodebox-ops/src/` (generators.rs, filters.rs, etc.)
- **Node registration**: `crates/nodebox-gui/src/node_library.rs` and `node_selection_dialog.rs`
- **Node evaluation**: `crates/nodebox-gui/src/eval.rs`

## Porting Nodes from Java to Rust

When porting node functions from Java to Rust, follow this checklist:

1. **Find the authoritative definition**: Look up the node in `libraries/corevector/corevector.ndbx` to see `outputType`, `outputRange`, and all `<port>` elements. This is the source of truth.

2. **Verify the return type in Java**: Check the Java method signature in `CoreVectorFunctions.java`. For example, `grid` returns `List<Point>`, not `Geometry`.

3. **Match output type in Rust registration**: When registering the node in `node_library.rs` and `node_selection_dialog.rs`, set:
   - `.with_output_type(PortType::X)` — must match the `.ndbx` `outputType`
   - `.with_output_range(PortRange::List)` — if `.ndbx` has `outputRange="list"`

4. **Match all input ports**: Add all `<port>` elements from the `.ndbx` as `.with_input(Port::...)` calls, with matching names, types, and default values.

5. **Match parameter types exactly**: Pay attention to `long` vs `int`, `double` vs `float`. The Java/`.ndbx` source is authoritative.

6. **Preserve edge-case behavior**: Check how the Java code handles edge cases (e.g., `columns=1`). The Rust implementation should match exactly.

7. **Return correct type from eval.rs**: In `execute_node()`, return the appropriate `NodeOutput` variant (e.g., `NodeOutput::Points` for `outputType="point"` + `outputRange="list"`).

## UI Design System (Rust GUI)

**IMPORTANT: When working on any GUI component, always consult `STYLE_GUIDE.md` first.**

The NodeBox GUI follows a **Linear-inspired design philosophy**:

- **Sharp & geometric** — 90° angles, straight lines, zero corner radius by default
- **No borders** — use background color differentiation between panels
- **Violet accent** — purple/violet for selections, links, and highlights
- **Deep dark theme** — rich blacks with subtle cool undertones
- **Subtle rounding only for selections** — 4px rounding on selected/hovered items

### Quick Reference

All tokens are in `crates/nodebox-gui/src/theme.rs`. Key patterns:

```rust
use crate::theme::{
    // Backgrounds (layered, darkest to lightest)
    PANEL_BG, TAB_BAR_BG, SURFACE_ELEVATED, HOVER_BG, SELECTION_BG,

    // Text (brightest to dimmest)
    TEXT_STRONG, TEXT_DEFAULT, TEXT_SUBDUED, TEXT_DISABLED,

    // Accents
    VIOLET_400, VIOLET_500, VIOLET_900,

    // Spacing (4px grid)
    PADDING, PADDING_SMALL, PADDING_LARGE,
};
```

**See `STYLE_GUIDE.md` for complete documentation including:**
- Full Linear-inspired color palette
- Sharp corners philosophy (0px default, 4px for selections)
- No-border panel differentiation patterns
- Component patterns with code examples
- Do's and Don'ts checklist

## API Design & Backwards Compatibility

### Property Names
When renaming properties in the API, keep internal storage names for backwards compatibility:
- Internal property: `canvasWidth`, `canvasHeight` (for file format compatibility)
- External API: `width()`, `height()` (cleaner public interface)

```rust
// In NodeLibrary
pub fn width(&self) -> f64 {
    self.properties
        .get("canvasWidth")  // Internal name for backwards compat
        .and_then(|s| s.parse().ok())
        .unwrap_or(1000.0)
}
```

### Centered Coordinate System
The canvas uses a centered coordinate system where:
- Geometry is positioned relative to the origin (0, 0)
- Canvas extends from `-width/2` to `+width/2` and `-height/2` to `+height/2`
- This matches standard graphics conventions and simplifies transforms

**For SVG export:**
```rust
// Use centered viewBox
let half_w = width / 2.0;
let half_h = height / 2.0;
format!(r#"viewBox="{} {} {} {}""#, -half_w, -half_h, width, height)
```

**For PNG export with tiny_skia:**
```rust
// Center the transform
let transform = Transform::from_translate(width as f32 / 2.0, height as f32 / 2.0);
```

## Screen-space Rendering

For UI elements that should remain constant size regardless of zoom (handles, borders, guides):
- Apply zoom transform to world coordinates first
- Use fixed pixel values for stroke width and sizes after transformation

```rust
// Canvas border with constant 1px stroke
let screen_top_left = self.pan_zoom.world_to_screen(top_left, center);
let screen_bottom_right = self.pan_zoom.world_to_screen(bottom_right, center);
painter.rect_stroke(canvas_rect, 0.0, Stroke::new(1.0, border_color));
```

## Rust Dead Code Warnings

### Module-level suppression
For WIP modules or test utilities where many items may be unused:
```rust
#![allow(dead_code)]
```

### Item-level suppression
For individual items that are intentionally kept for future use or API completeness:
```rust
#[allow(dead_code)]
pub fn some_future_method(&self) { ... }
```

### Test-only methods
Methods marked `#[cfg(test)]` still generate warnings if unused within tests:
```rust
#[cfg(test)]
#[allow(dead_code)]
pub fn new_for_testing() -> Self { ... }
```

## egui Migration Notes

### Deprecated methods
- `ui.allocate_ui_at_rect(rect, |ui| { ... })` is deprecated
- Use `ui.allocate_new_ui(egui::UiBuilder::new().max_rect(rect), |ui| { ... })` instead

### Fixed-size DragValue widgets
DragValue shifts by a few pixels when entering text edit mode due to different padding between button and text edit states. To prevent this:

```rust
// Save original state
let old_visuals = ui.visuals().clone();
let old_spacing = ui.spacing().clone();

// Set expansion=0 on all widget states to prevent size changes
ui.visuals_mut().widgets.inactive.expansion = 0.0;
ui.visuals_mut().widgets.hovered.expansion = 0.0;
ui.visuals_mut().widgets.active.expansion = 0.0;
ui.visuals_mut().widgets.noninteractive.expansion = 0.0;

// Use consistent padding for button and text edit modes
ui.spacing_mut().button_padding = egui::vec2(4.0, 2.0);

// Allocate exact size first, then place widget inside
let (rect, _) = ui.allocate_exact_size(
    egui::vec2(width, height),
    egui::Sense::hover(),
);
let response = ui.put(rect, egui::DragValue::new(value).range(range));

// Restore original state
*ui.visuals_mut() = old_visuals;
*ui.spacing_mut() = old_spacing;
```

### Styling widgets to follow the style guide
When styling egui widgets (DragValue, checkbox, etc.) to match the style guide:

1. Override `bg_fill` AND `weak_bg_fill` - some widgets use one or the other
2. Set `bg_stroke = Stroke::NONE` to remove borders
3. Set `rounding = Rounding::ZERO` for sharp corners
4. Override ALL states: `inactive`, `hovered`, `active`, `noninteractive`
5. Save and restore both `visuals` and `spacing` to avoid affecting other widgets

## Build Commands

### Excluding problematic crates
The `nodebox-python` crate has pyo3 dependencies that may cause build issues. Exclude it when not needed:
```bash
cargo build --workspace --exclude nodebox-python
cargo test --workspace --exclude nodebox-python
```

### Running specific crates
```bash
cargo run -p nodebox-gui          # Run the GUI
cargo run -p nodebox-cli          # Run the CLI
cargo test -p nodebox-core        # Test specific crate
```
