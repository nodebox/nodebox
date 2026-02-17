# Golden Master: Render Learnings

Lessons learned from comparing Java and Rust NodeBox rendering outputs.

## Architectural Differences

### 1. List Broadcasting (Most Impactful Difference)

The Java evaluator has a "broadcasting" mechanism: when a node's single-value port is connected to an upstream node that produces multiple values (a list), the Java evaluator calls the node function once per value in the list, collecting results into a list.

**Java (NodeContext.java):** Uses `NodeArgumentIterator` which creates argument maps for each index up to `biggestArgumentList`. For list-range ports, the entire list is passed. For value-range ports, individual elements are passed via `wrappingGet`.

**Rust (eval.rs):** `compute_iteration_count`, `build_iteration_inputs`, `collect_results` implement the same mechanism.

**Examples affected:** Most examples that use grid → translate, number lists → shapes, etc.

### 2. Geometry vs Paths Semantics (Fundamental Type System Limitation)

**Java:** `Geometry` is a first-class type that holds multiple `Path` objects as a single compound value. When used in list broadcasting, `Geometry` has `list_len=1`. A `List<Geometry>` has `list_len=N`.

**Rust:** We added `NodeOutput::Geometry(Vec<Path>)` with `list_len=1`. However, when `collect_results` collects N Geometry items (from N broadcasting iterations), it flattens them into `Paths(total_paths)` with `list_len=total_paths`. This changes downstream broadcasting behavior.

**Example:** reflect with keep_original=true returns Geometry(2) per iteration. N iterations → collect_results → Paths(2N) with list_len=2N, instead of Java's N items with list_len=N.

**Fix attempted and reverted:** Merging all contours into a single Path preserves list_len=1 per iteration but loses path rendering identity (halves path count for rendering).

**Proper fix:** Would require a `NodeOutput::Geometries(Vec<Vec<Path>>)` variant to represent "list of Geometry" with correct list_len semantics.

**Examples affected:** Invader, Name Generator

### 3. SVG Rendering Strategy

**Java SVGRenderer:**
- Uses centered `viewBox`: `viewBox="-w/2 -h/2 w h"`
- Path-level rendering: each Path becomes a `<path>` element
- Smart number formatting: integers without decimals, floats with 2 decimal places
- Minimal attributes: omits fill when black (SVG default), omits stroke when not set

**Rust SVG renderer (after fixes):**
- Same centered `viewBox`
- Same smart number formatting
- Same fill/stroke attribute strategy

### 4. Boolean Path Operations (Compound Node)

**Java:** Uses `java.awt.geom.Area` for boolean operations. Area natively handles compound shapes.

**Rust:** Uses `i_overlay` crate with f32 SingleFloatOverlay. Bezier curves are flattened to 16-segment line approximations before boolean ops. When compound receives a Geometry input, all paths are merged into a single compound path (concatenating contours) before the boolean operation, matching Java's Area behavior.

### 5. Text-on-Path Algorithm

**Java:** Python `pyvector.text_on_path()` function places text characters along a path using font metrics, parametric t values, and rotation.

**Rust:** Implemented in `font.rs` as `text_on_path()`. Per-character algorithm:
1. Calculate parametric position `t` along the path
2. Get point and tangent angle at `t`
3. Render character glyph using `TtfOutlineBuilder`
4. Apply transform: `rotate(angle - 180°).then(translate(point))`

Supports "leading" and "trailing" alignment with margin and baseline_offset parameters.

### 6. Fill/Stroke Defaults

**Java Path:** Default fill = `Color.BLACK`, default stroke = `null`
**Rust Path (after fix):** Same defaults

### 7. LIST-range Port Resolution

When nodes are loaded from `.ndbx` files, they typically only have port overrides. The `PortRange` (VALUE vs LIST) comes from prototype definitions.

**Solution:** Added `is_list_range_port()` lookup table mapping `(prototype, port_name)` → `bool`.

### 8. Type Conversion

Java's `TypeConversions.java` automatically converts upstream outputs to match port types. Most critical: `Geometry → Points` (extract contour points).

**Rust:** `convert_input_types()` + `convert_output_for_port()` + `get_prototype_port_type()`.

### 9. Font Rendering Differences

Java uses system fonts (AWT) while Rust uses a bundled Inter font via ttf-parser. This causes path count differences in text-heavy examples because different fonts have different glyph decompositions.

**Affected examples:** TextFX (-5 paths), Sine Text (+23), Animated Logo (+218), Spider Text (+430)

## Resolution Strategies (Final State)

### Completed (19 fixes)
1. SVG format alignment (centered viewBox, smartFloat, fill/stroke, path format, attribute order)
2. List broadcasting (compute_iteration_count, build_iteration_inputs, collect_results)
3. LIST-range port resolution (is_list_range_port lookup table)
4. Generic list.combine (handles all NodeOutput types)
5. Type conversion (Path→Points, Float→Point, Int→Float)
6. Default values alignment (fill, stroke, polygon, star, grid, arc, etc.)
7. copy node fix (removed incorrect LIST-range on shape port)
8. corevector.point (separated from make_point, Point pass-through)
9. corevector.sort (handle Points input)
10. data.lookup (Point x/y property support)
11. corevector.null (pass through any input type)
12. corevector.delete (handle lists of paths + point-in-path containment)
13. Point-in-path test (Path::contains() with ray-casting + bezier flattening)
14. Prototype port type lookup (get_prototype_port_type() for type conversion)
15. Subnetwork evaluation (evaluate_subnetwork() with published port mapping)
16. CLI Platform file I/O (CliPlatform with read_file, read_text_file, read_binary_file)
17. text_on_path node (per-character text placement along path)
18. compound node (boolean path operations using i_overlay crate)
19. NodeOutput::Geometry variant + Geometry-aware filters

### Remaining (known limitations)
1. **List-of-Geometry type** — Would fix Invader and Name Generator (2 examples)
2. **System font support** — Would reduce text path count differences (4 examples)

## Files Modified/Created

| File | Purpose |
|------|---------|
| `src/dev/java/nodebox/BatchRenderer.java` | Java CLI SVG renderer |
| `crates/nodebox-cli/` | Rust CLI SVG renderer crate |
| `crates/nodebox-core/src/ops/generators.rs` | Fixed polygon, star |
| `crates/nodebox-core/src/ops/filters.rs` | Added compound (CompoundOp, i_overlay integration) |
| `crates/nodebox-core/src/svg/renderer.rs` | SVG format alignment |
| `crates/nodebox-core/src/geometry/font.rs` | text_on_path implementation |
| `crates/nodebox-core/Cargo.toml` | Added i_overlay dependency |
| `crates/nodebox-eval/src/eval.rs` | List broadcasting, type conversion, Geometry variant, compound/text_on_path dispatch |
| `crates/nodebox-electron/src/lib.rs` | Added Geometry variant handling |
| `render_all_java.sh` | Script to render all Java golden masters |
| `render_all_rust.sh` | Script to render all Rust outputs |
| `compare.sh` | Quick comparison script |
| `golden-master/java/` | Java SVG outputs (46 files) |
| `golden-master/rust/` | Rust SVG outputs (49 files) |

## Progress

| Iteration | Match Count | Rate |
|-----------|-------------|------|
| Initial | 9/46 | 19.6% |
| After SVG fixes | 14/46 | 30.4% |
| After list broadcasting | 23/46 | 50.0% |
| After copy/point/delete fixes | 29/46 | 63.0% |
| After subnetwork eval | 32/46 | 69.6% |
| After CLI file I/O | 37/46 | 80.4% |
| After text_on_path | 38/46 | 82.6% |
| After compound (i_overlay) | 39/46 | 84.8% |
| After Geometry variant + fixes | 40/46 | 87.0% |
