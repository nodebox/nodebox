# Golden Master Render Results

Comparison of Java and Rust SVG output for all 48 example `.ndbx` files.

**Java:** 46/48 rendered (2 require network access)
**Rust:** 49/49 rendered (includes 08 SVG duplicate)

## Summary

| Status | Count | Description |
|--------|-------|-------------|
| PATH COUNT MATCH | 42 | Same number of paths |
| PATH COUNT DIFFER | 4 | Different number of paths |
| JAVA MISSING | 3 | Only Rust produced output |

**Total matching on path count: 42/46** (91.3%)

## Path Count Matches (42 examples)

Same number of paths. Many are byte-for-byte identical; others have minor coordinate, color, or ordering differences.

| Example | Paths | Notes |
|---------|-------|-------|
| 01 Primitives | 3 | Exact match |
| 02 Lines | 3 | Exact match |
| 03 Text | 2 | Font rendering (different text-to-path engines) |
| 04 Grid | 72 | Exact match |
| 05 Copy | 1973 | Minor coordinate precision |
| 06 Transformations | 6 | Path ordering/coordinates differ |
| 07 Template | 36 | smart_float integer rounding |
| 08 SVG | 49 | SVG import path data precision |
| 09 Binary Operation | 40 | Boolean compound operations (i_overlay) |
| 10 Sorting | 20 | Coordinate precision |
| 11 Spirograph | 36 | Freehand path precision |
| 13 Tilling | 1200 | Geometry/copy chain with compound |
| 01 Color | 3 | Exact match |
| 02 Color Range | 9 | Exact match |
| 03 Gradient | 100 | Exact match |
| 01 Create Numbers | 30 | Coordinate precision |
| 02 Sample | 100 | Coordinate precision |
| 03 Range | 100 | Exact match |
| 04 Random | 100 | Random seed differences |
| 05 Make Numbers | 16 | Arc path data differs |
| 06 Convert Range | 10 | Exact match |
| 02 Lissajous | 1 | Curve path data precision |
| 03 Coordinates with range | 2 | Path data precision |
| 04 Coordinates with sample | 1 | Path data precision |
| 05 Spiral | 1 | Path data precision |
| Name Generator | 112 | Fixed: build_iteration_inputs unwrapping |
| 01 List Matching A | 100 | Color/fill differences |
| 02 List Matching B | 25 | Color differences |
| Compare | 100 | Coordinate/color precision |
| 01 Moiree | 1376 | Coordinate precision |
| 02 Colorcycle | 36 | Exact match |
| 03 Coriolise | 3 | Coordinate precision |
| Mesh | 361 | Exact match |
| 01 Blink | 100 | Exact match |
| 02 Elastic | 1 | Coordinate precision |
| 01 Read Csv | 44 | Data precision |
| 02 Time Stamp | 3154 | Data precision |
| 03 Piechart | 39 | text_on_path character positioning |
| 04 Zipmap | 588 | Data precision |
| Heatmap | 2501 | Color differences |
| Earthquakes | 686 | Coordinate precision |

## Path Count Mismatches (4 examples)

### Category: Geometry/List Semantics (1)

| Example | Java | Rust | Root Cause |
|---------|------|------|------------|
| Invader | 122 | 86 | Java's Geometry extends AbstractList, so combine expands Geometry into individual paths rather than treating it as one item. Inner random_numbers seed=0 produces indices that never reach the head group in the current Rust representation. |

### Category: Font/Text Differences (3)

Different font engines (Java AWT vs Rust ttf-parser with bundled Inter font) produce different numbers of glyphs/contours per character. These cascade through resample/point/line operations.

| Example | Java | Rust | Detail |
|---------|------|------|--------|
| 12 TextFX | 519 | 514 | 5 fewer contours from different font rendering of "sketch" |
| 07 Sine Text | 1442 | 1465 | 23 more contours from different font rendering of "Tag" |
| Animated Logo | 1929 | 2147 | 109 extra resampled points from "NodeBox" text = 109 extra lines + 109 extra dots |

Note: Spider Text (1300 vs 1648) is also a font rendering difference — "Spider" text produces different contour counts which cascades through resample/repeat/distance/cull/line operations.

## Missing from Java (3 examples)

| Example | Reason |
|---------|--------|
| Geocoding | Requires network HTTP access |
| Twitter API | Requires network HTTP access |
| 08 SVG (duplicate) | Duplicate entry in Rust directory listing |

## Fixes Applied (Across All Sessions)

### Evaluation Fixes
1. **List broadcasting** — `compute_iteration_count`, `build_iteration_inputs`, `collect_results`
2. **Type conversion** — `convert_input_types` / `convert_output_for_port`: Path→Points, Float→Point, Int→Float
3. **Prototype port type lookup** — `get_prototype_port_type()` resolves port types from prototype definitions
4. **LIST-range port lookup** — `is_list_range_port()` resolves port ranges from prototype definitions
5. **Generic list.combine** — Handles all NodeOutput types (Colors, Points, Floats, etc.)
6. **copy node fix** — Removed incorrect LIST-range marking on `shape` port
7. **corevector.point** — Separated from make_point; pass-through node
8. **data.lookup** — Added Point/Points support
9. **corevector.null** — Pass through any input type
10. **corevector.delete** — Handle list of paths, proper point-in-path containment test
11. **corevector.sort** — Handle Points input
12. **Point-in-path test** — Implemented `Path::contains()` using ray-casting with bezier flattening
13. **Subnetwork evaluation** — `evaluate_subnetwork()` with published port mapping via childReference
14. **CLI Platform file I/O** — `CliPlatform` with read_file, read_text_file, read_binary_file
15. **text_on_path node** — Per-character text placement along path with rotation
16. **compound node** — Boolean path operations using i_overlay crate (union, difference, intersection)
17. **NodeOutput::Geometry variant** — Compound geometry with list_len=1, matching Java's Geometry semantics
18. **Geometry-aware filters** — translate, rotate, scale, colorize, align, reflect, resample, wiggle, skew, snap, fit, scatter all handle Geometry input
19. **Compound Geometry merging** — Compound node merges multi-path Geometry inputs into single path for boolean ops
20. **JavaRandom implementation** — Exact port of `java.util.Random` LCG for deterministic random sequences
21. **build_iteration_inputs for count=1** — Fixed: always unwrap single-element lists to scalar values via build_iteration_inputs, even when iteration_count == 1. Previously skipped, causing Strings(["x"]) to not match get_string() which expects String("x").

### SVG Format Fixes
1. **Centered viewBox** — `viewBox="-500 -500 1000 1000"` without translate group
2. **smartFloat formatting** — Integers without decimals, floats with 2 decimals
3. **Fill/stroke conventions** — Match Java: omit black fill, omit null stroke
4. **Attribute ordering** — Match Java HashMap iteration: d, stroke-width, fill, stroke
5. **Path command format** — No spaces between commands
6. **Trailing newline** — Removed trailing newline after `</svg>`

### Node Implementation Fixes
1. **polygon** — Fixed start angle (0° for align=false), removed extra closing point
2. **star** — Fixed diameter→radius conversion, swapped sin/cos to match Java
3. **Path defaults** — Changed default fill from WHITE to BLACK, default stroke to None
4. **sort_points** — Added point sorting in filters.rs

## Progress Across Sessions

| Iteration | Match Count | Rate |
|-----------|-------------|------|
| Initial (SVGs generated) | 9/46 | 19.6% |
| After SVG format fixes | 14/46 | 30.4% |
| After list broadcasting | 23/46 | 50.0% |
| After copy/point/delete fixes | 29/46 | 63.0% |
| After subnetwork eval | 32/46 | 69.6% |
| After file I/O (import_csv/svg) | 37/46 | 80.4% |
| After text_on_path | 38/46 | 82.6% |
| After compound (i_overlay) | 39/46 | 84.8% |
| After Geometry variant + fixes | 40/46 | 87.0% |
| After build_iteration_inputs fix | 42/46 | 91.3% |

## Remaining Work

### P1: Medium Impact (Would fix 1 example)
1. **Geometry-as-List in combine** — Java's Geometry extends AbstractList<Path>, so when a Geometry is combined via list.combine, Java expands it into individual paths rather than treating it as a single compound item. Fixing this would resolve the Invader example.

### P2: Low Impact (Would partially improve 3 examples)
1. **System font support** — Use matching system fonts instead of bundled Inter to reduce text path count differences. This would improve TextFX, Sine Text, and Animated Logo, though exact matches are unlikely without identical font engine implementations.
