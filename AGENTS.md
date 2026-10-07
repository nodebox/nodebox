# Repository Guidelines

## Overview

NodeBox is being rewritten from Java to **Rust + Electron**. The current active codebase is:

- **`crates/`** — Rust core library (`nodebox-core`) and desktop GUI (`nodebox-desktop`, egui-based)
- **`electron-app/`** — Electron app (React 19 + TypeScript + Vite) — the primary GUI under active development

The Java/Python code (`src/main/java`, `src/main/python`) is **legacy and read-only**. It exists solely as a reference for verifying behavior when porting nodes and features. **Never modify Java or Python code.**

## Project Structure

### Active code
- `electron-app/` — Electron GUI (see detailed section below)
- `crates/nodebox-core/` — Rust core: geometry types, node operations, evaluator, fonts, node templates
- `crates/nodebox-vello/` — Vello scene building and rendering, shared by both GUIs
- `crates/nodebox-desktop/` — Rust desktop GUI (egui)
- `crates/nodebox-electron/` — WASM module for Electron: evaluation, the Vello viewer, export (built with wasm-pack)

### Reference / legacy (read-only)
- `src/main/java/` — Legacy Java application (`nodebox.*` packages)
- `src/main/python/` — Legacy Python node libraries
- `libraries/corevector/corevector.ndbx` — Authoritative node definitions (XML) used as source of truth when porting nodes
- `src/main/java/nodebox/function/CoreVectorFunctions.java` — Java node implementations (reference for porting)
- `examples/` — Example `.ndbx` project files

### Build artifacts (do not edit)
- `build/`, `dist/` — Generated outputs

## Build, Test, and Development Commands

### Electron app (primary)
```bash
cd electron-app
npm run dev                              # Start dev server with HMR
npm run build                            # TypeScript check + Vite production build
npm run test                             # Run Vitest unit tests
npx tsc --noEmit                         # Type-check without emitting
npm run build && npx playwright test     # Build + run all E2E tests
```

### Rust crates
```bash
cargo check --workspace --exclude nodebox-python   # Type-check
cargo build --workspace --exclude nodebox-python   # Build
cargo test --workspace --exclude nodebox-python    # Run tests
cargo test -p nodebox-core                         # Test specific crate
cargo run                                          # Run Rust desktop GUI
```

The `nodebox-python` crate has pyo3 dependencies that may cause build issues. Always exclude it unless specifically needed.

## Coding Style & Naming Conventions
- **TypeScript:** 2-space indentation, single quotes, trailing commas. Follow existing patterns in `electron-app/src/`.
- **Rust:** Standard `rustfmt` formatting, `snake_case` for functions/variables, `UPPER_SNAKE_CASE` for constants.
- Keep edits localized and match the surrounding file's formatting and ordering.

## Testing Guidelines
- **Electron app:** Playwright E2E tests in `electron-app/tests/e2e/`, Vitest unit tests in `electron-app/tests/unit/`. Run `npm run build && npx playwright test` before shipping changes.
- **Rust:** `cargo test --workspace --exclude nodebox-python` for all crate tests.

## Branching Strategy
- **Use `rewrite-in-rust` as the main branch.** All new development and PRs should target this branch.
- **NEVER commit or merge directly into `master`.** The `master` branch exists for legacy reasons only and should not be modified.
- **PRs should ALWAYS use `rewrite-in-rust` as the base branch**, not `master`, unless explicitly specified otherwise.
- Create feature branches from `rewrite-in-rust` and merge back into it.

## Commit & Pull Request Guidelines
- Recent history favors short, sentence-style commit messages (e.g., "Use Ctrl key on Windows."). Keep messages concise and specific.
- PRs should describe the user-visible change, list test commands run, and include screenshots or recordings for UI updates.
- Link relevant issues or tickets when applicable.

## Code Quality
- **Rust:** Fix all compiler warnings before handing off code. Run `cargo check --workspace --exclude nodebox-python` and ensure zero warnings before completing a task. Deprecation warnings, unused imports, dead code warnings, and any other diagnostics must be resolved — not suppressed — unless there is a documented reason (see "Rust Dead Code Warnings" for approved suppression patterns).
- **Electron app:** Run `npx tsc --noEmit` for zero type errors and `npm run build && npx playwright test` for all E2E tests to pass before handing off code.

## Async Node Implementation

For nodes that perform I/O operations or expensive computations, see **[docs/async_nodes.md](docs/async_nodes.md)** for:
- Cancellation token usage
- Async I/O patterns with smol
- Best practices for responsive cancellation
- Testing async-aware nodes

## Node Definitions and Implementations

Node definitions live in several places. When porting a node, use the legacy `.ndbx` and Java code as the **source of truth** for behavior, then implement in Rust/TypeScript.

### Source of truth (legacy, read-only)
- `libraries/corevector/corevector.ndbx` — XML definitions for core vector nodes (authoritative for port names, types, defaults, output types)
- `src/main/java/nodebox/function/CoreVectorFunctions.java` — Java implementations (reference for edge-case behavior)
- `src/main/python/` modules — Python node implementations (reference only)

### Current implementations
All node logic is in Rust. The Electron app evaluates through the WASM module and holds no node implementations of its own; its TypeScript types in `electron-app/src/renderer/types/` mirror the Rust ones.

### Rust Implementations
- **Node operations**: `crates/nodebox-core/src/ops/` (generators.rs, filters.rs, etc.)
- **Node templates**: `crates/nodebox-core/src/node/templates.rs` (the dialog is `crates/nodebox-desktop/src/node_selection_dialog.rs`)
- **Default ports on load**: `crates/nodebox-core/src/node/default_ports.rs`
- **Node evaluation**: `crates/nodebox-core/src/eval.rs`
- **Fonts**: `crates/nodebox-core/src/geometry/font.rs` (fontdb + ttf-parser; no system fonts on wasm, the host calls `register_font`)
- **Vello scene building**: `crates/nodebox-vello/`

Code without egui in it belongs in `nodebox-core` (or `nodebox-vello` when it needs Vello), so other front ends can use it. `nodebox-core` must keep building for `wasm32-unknown-unknown`.

## Porting Nodes from Java

When porting node functions from Java to Rust, follow this checklist:

1. **Find the authoritative definition**: Look up the node in `libraries/corevector/corevector.ndbx` to see `outputType`, `outputRange`, and all `<port>` elements. This is the source of truth.

2. **Verify the return type in Java**: Check the Java method signature in `CoreVectorFunctions.java`. For example, `grid` returns `List<Point>`, not `Geometry`.

3. **Match output type in Rust registration**: When registering the node in `node/templates.rs` (and its default ports in `node/default_ports.rs`), set:
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

All tokens are in `crates/nodebox-desktop/src/theme.rs`. Key patterns:

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

**For SVG export**, `nodebox_core::svg::SvgOptions::with_centered(true)` wraps the geometry in a group translated by half the width and height.

**For Vello**, the view transform puts the origin where the document centre should appear:
```rust
let transform = Affine::translate((offset_x, offset_y)) * Affine::scale(scale);
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

## NodeLibrary Arc Pattern

The `NodeLibrary` is wrapped in `Arc<NodeLibrary>` for cheap cloning and copy-on-write semantics. This enables:
- **Render dispatch**: The render worker receives a cheap `Arc::clone` of the library without deep-copying the entire node graph.
- **Undo/redo history**: `History` stores `Vec<Arc<NodeLibrary>>` snapshots that share unchanged data.

### Reading (no mutation)
Pass `&Arc<NodeLibrary>` or clone the Arc for background threads:
```rust
render_worker.submit(Arc::clone(&state.library));
```

### Writing (mutation)
Use `Arc::make_mut` to get a mutable reference. This clones the inner data only if other Arcs still reference it (copy-on-write):
```rust
Arc::make_mut(&mut state.library).root.children.push(new_node);
```

For multiple mutations in a block, bind `Arc::make_mut` once:
```rust
let lib = Arc::make_mut(&mut state.library);
lib.root.children.retain(|n| &n.name != name);
lib.root.connections.retain(|c| &c.output_node != name);
```

### Function signatures
- Read-only: `fn show(&self, library: &Arc<NodeLibrary>)`
- Mutating: `fn show(&mut self, library: &mut Arc<NodeLibrary>)`

---

# Electron App (`electron-app/`)

The Electron app is the **primary GUI** for NodeBox, built with React 19 + TypeScript. It shares design language and node evaluation logic with the Rust desktop app.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Shell | Electron | 33.x |
| UI Framework | React | 19.x |
| Build Tool | Vite | 6.x (with `vite-plugin-electron`) |
| Styling | Tailwind CSS | 4.x (PostCSS plugin) |
| State Management | Zustand | 5.x (with Immer middleware) |
| Language | TypeScript | 5.7+ |
| E2E Tests | Playwright | 1.49+ |
| Unit Tests | Vitest | 2.1+ (jsdom environment) |
| Icons | Lucide React | 0.468+ |
| WASM | wasm-pack output | `wasm/nodebox_electron_bg.wasm` |

## Project Structure

```
electron-app/
├── src/
│   ├── main/              # Electron main process
│   │   ├── index.ts        # App entry, window creation, IPC handlers
│   │   └── menu.ts         # Native app menu
│   ├── preload/
│   │   └── index.ts        # Context bridge (electronAPI)
│   ├── shared/
│   │   └── ipc-channels.ts # IPC channel constants + MenuAction type
│   └── renderer/           # React app (renderer process)
│       ├── main.tsx         # React entry, __storeState__ for E2E
│       ├── App.tsx          # Root component
│       ├── App.css          # Tailwind import, @theme, global styles
│       ├── components/      # React components
│       ├── state/           # Zustand store slices
│       ├── hooks/           # Custom React hooks
│       ├── eval/            # Calls into the WASM module (evaluation, viewer, export, fonts)
│       ├── theme/           # Design tokens (JS constants for canvas drawing)
│       ├── types/           # TypeScript type definitions
│       └── viewer/          # Viewer handle logic (FourPointHandle, hit testing)
├── wasm/                   # WASM module (built by wasm-pack from crates/)
├── tests/
│   ├── e2e/                # Playwright E2E tests
│   └── unit/               # Vitest unit tests
├── package.json
├── vite.config.ts
├── playwright.config.ts
└── tsconfig.json
```

## Build & Development Commands

```bash
cd electron-app
npm run dev           # Start dev server with HMR
npm run build         # TypeScript check + Vite production build
npm run test          # Run Vitest unit tests
npm run test:e2e      # Run Playwright E2E tests (requires build first)
npx tsc --noEmit      # Type-check without emitting
```

**Build + test before committing:**
```bash
npm run build && npx playwright test
```

## Architecture

### Electron Process Model

- **Main process** (`src/main/index.ts`): Window management, native file dialogs, app menu. Enables WebGPU and grants the `local-fonts` permission. Uses `contextIsolation: true` and `nodeIntegration: false`.
- **Preload** (`src/preload/index.ts`): Bridges main↔renderer via `contextBridge.exposeInMainWorld('electronAPI', ...)`. Exposes file I/O, export, and menu action handlers.
- **Renderer** (`src/renderer/`): Full React app. No direct Node.js access — all system operations go through the `electronAPI` bridge.

### IPC Channels

Defined in `src/shared/ipc-channels.ts`:
- `file:new`, `file:open`, `file:save`, `file:save-as` — file operations
- `export:svg`, `export:png` — export operations
- `menu:action` — menu command dispatch (undo, redo, delete, zoom, toggle view options)

### State Management (Zustand + Immer)

The store is composed of 6 slices, all using Immer for immutable updates:

| Slice | File | Purpose |
|-------|------|---------|
| `LibrarySlice` | `state/library-slice.ts` | Node graph (children, connections, ports, rendered child) |
| `SelectionSlice` | `state/selection-slice.ts` | Selected/active nodes |
| `HistorySlice` | `state/history-slice.ts` | Undo/redo stacks (structuredClone snapshots, max 50) |
| `UISlice` | `state/ui-slice.ts` | View toggles, splitter ratios, dialog visibility, viewer mode/zoom |
| `AnimationSlice` | `state/animation-slice.ts` | Frame, play state, frame range |
| `RenderSlice` | `state/render-slice.ts` | Evaluation result (paths, output info, errors) |

Store is created in `state/store.ts` and accessed via `useStore` hook.

### Node Evaluation

Evaluation runs in Rust, in the WASM module built from `crates/nodebox-electron/`. `eval/evaluator.ts` only prepares the call:

1. It reads the files that File-widget ports point to, through the `electronAPI` bridge, because the evaluator cannot block on IPC.
2. It calls `evaluate_library(libraryJson, filesJson, frame)` and parses the returned `EvalResult` (paths, output info, errors).
3. If the document asked for fonts that are not registered yet, it reads them with the Local Font Access API (`queryLocalFonts`), registers them with `register_font`, and evaluates again. Until then text uses the bundled Inter font.

A node type the evaluator does not implement comes back as an error that names the node.

### WASM Integration

`wasm/` is the wasm-pack output of `crates/nodebox-electron/`. It is not committed. Building it needs `rustup target add wasm32-unknown-unknown` and `wasm-pack`; `./scripts/dev.sh` builds it and starts the app. Rebuild it after changing Rust code:

```bash
cd crates && wasm-pack build nodebox-electron --target web --out-dir ../electron-app/wasm
```

`eval/wasm.ts` loads the module and wraps its exports: `evaluate_library`, `get_node_templates`, `create_node`, `WasmNodeLibrary` (parse and serialize `.ndbx`), `register_font`, `take_missing_fonts`, `export_svg`, and the viewer functions `viewer_init`, `viewer_render` and `viewer_render_pixels`.

Vite path alias `@wasm` → `wasm/` is configured in `vite.config.ts`.

### Canvas Rendering

The network view draws with Canvas2D. The viewer has two stacked canvases:

- a WebGPU canvas, on which **Vello** draws the document background and the geometry (`viewer_render` in `crates/nodebox-electron/src/viewer.rs`). The paths of the last evaluation stay in WASM memory, so drawing does not parse them again;
- a Canvas2D canvas above it for the overlays and the pointer input.

The same Vello renderer produces PNG export and the pixels that E2E tests read (`viewer_render_pixels`), because a WebGPU canvas has no `getImageData`.

Shared hooks:

- **`useCanvasRenderer` hook** — handles DPR-aware canvas sizing, `requestAnimationFrame` scheduling, and `ResizeObserver` auto-rerender
- **`usePanZoom` hook** — mouse wheel zoom (ctrl/meta for zoom, plain scroll for pan), middle-mouse/alt-click drag pan, `worldToScreen`/`screenToWorld` coordinate transforms

**NetworkCanvas** renders:
- Grid background, node bodies (colored by output type), node names, category icons
- Connection lines (colored by port type), port indicators
- Selection highlights, rubber band selection, drag preview

**ViewerCanvas** renders on its 2D overlay:
- Canvas border, origin crosshair
- Control points (colored circles by point type: green=lineTo, red=curveTo, blue=curveData)
- Point numbers (bitmap digit cache, toggled via UI)
- Interactive FourPointHandle for rect/ellipse (drag corners to resize, center to reposition)

### Network Grid Coordinate System

Nodes are positioned on a grid. Screen coordinates are calculated as:
```
screenX = panX + gridX * CELL_SIZE + NODE_PADDING
screenY = panY + gridY * CELL_SIZE + NODE_PADDING
```
Where `CELL_SIZE = 48` and `NODE_PADDING = 8`. Default pan is `(8, 8)` so grid position (1,1) appears at the expected visual location.

## Styling Approach

### Dual Token Systems

1. **Tailwind CSS (`App.css` `@theme`)** — for DOM components. Defines the Zinc/Violet palette, semantic colors, category colors, point type colors, and spacing as CSS custom properties. Usage: `className="bg-zinc-800 text-zinc-100"`.

2. **JS Constants (`theme/tokens.ts`)** — for canvas drawing. Same values as CSS, but as TypeScript string constants. Canvas2D code (`NetworkCanvas.tsx`, `ViewerCanvas.tsx`) must import from here since it draws programmatically.

**Rule:** DOM components should use Tailwind classes. Only Canvas2D code uses `tokens.ts`.

### Key Tailwind Custom Values

```css
@theme {
  --color-panel: #27272a;        /* bg-panel */
  --color-field-hover: #484851;  /* bg-field-hover */
  --color-port-label: #27272a;   /* bg-port-label */
  --color-port-value: #3f3f46;   /* bg-port-value */
  --color-selection: #4c3a76;    /* bg-selection */
  --color-cat-geometry: #5078c8; /* category colors */
  --spacing-label-w: 112px;     /* parameter label width */
  --spacing-row-h: 36px;        /* parameter row height */
}
```

### Design Philosophy

Same as the Rust GUI — **Linear-inspired dark theme**:
- Zero corner radius by default, 4px for selections/hover
- Background color differentiation instead of borders
- Violet accent for selections
- System font stack: `-apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif`
- Title bar: `titleBarStyle: 'hiddenInset'` with traffic light offset `(12, 10)`

## Components

| Component | Description |
|-----------|-------------|
| `AppLayout.tsx` | Main layout with horizontal/vertical splitters, viewer/network/params panels |
| `NetworkCanvas.tsx` | Canvas2D network editor: node rendering, connections, drag, rubber band |
| `ViewerCanvas.tsx` | Geometry viewer: Vello canvas for paths, Canvas2D overlay for points, handles, origin |
| `ParameterPanel.tsx` | Parameter editor: DragValue for numbers, text inputs, color pickers, point editors |
| `DragValue.tsx` | Numeric input with click-to-edit and drag-to-adjust |
| `NodeSelectionDialog.tsx` | Fuzzy search dialog for adding nodes (opened via double-click or Tab) |
| `AnimationBar.tsx` | Timeline controls: play/stop, frame scrubber, frame range |
| `AddressBar.tsx` | Breadcrumb path showing current network hierarchy |
| `DataViewer.tsx` | Table view of evaluation output data |
| `AboutDialog.tsx` | App info dialog |

## E2E Testing

Tests use Playwright's Electron support via `@playwright/test`:

- **Helper** (`tests/e2e/helpers.ts`): `launchApp()` launches Electron and waits for React mount. `getStoreState()` reads Zustand state via `window.__storeState__()`. `waitForUpdate()` pauses for re-renders. `sendMenuAction()` triggers menu commands via IPC.
- **Config** (`playwright.config.ts`): 30s timeout, 1 retry, trace on first retry.
- **State access**: `main.tsx` exposes `__storeState__` on `window` — serializes selected nodes, active node, children (with positions, ports), connections, render results, viewer mode, animation state.

### Test Files

| File | Coverage |
|------|----------|
| `app-launch.spec.ts` | App launches, window visible, canvas rendered |
| `network-interactions.spec.ts` | Click select, drag node, double-click render, rubber band, clear selection |
| `network-view.spec.ts` | Grid rendering, node visualization |
| `connections.spec.ts` | Drag-to-connect ports |
| `parameter-panel.spec.ts` | Parameter rows, DragValue edit/drag, label drag, two-tone background |
| `parameter-editing.spec.ts` | Port value editing workflows |
| `node-creation.spec.ts` | Add nodes via dialog |
| `node-deletion.spec.ts` | Delete nodes |
| `node-categories.spec.ts` | Category-based node filtering |
| `viewer.spec.ts` | Viewer rendering, zoom, mode switching |
| `evaluation.spec.ts` | Node evaluation correctness, including pixels read back from Vello |
| `undo-redo.spec.ts` | History operations |
| `animation.spec.ts` | Play/stop, frame changes |
| `file-operations.spec.ts` | New/save/open via IPC |
| `export.spec.ts` | SVG/PNG export |
| `textpath.spec.ts` | Text-to-path through the WASM evaluator |

## Type System

TypeScript types mirror the Rust crate types:

| TS File | Mirrors |
|---------|---------|
| `types/node.ts` | `crates/nodebox-core/src/node/` — Node, Port, Connection, NodeLibrary |
| `types/geometry.ts` | `crates/nodebox-core/src/geometry/` — Point, Color, Path, Contour, PathPoint |
| `types/value.ts` | `crates/nodebox-core/src/value.rs` — tagged union Value type |
| `types/eval-result.ts` | Evaluation result: PathRenderData, EvalResult |

### Value Type (tagged union)

```typescript
type Value =
  | { type: 'null' }
  | { type: 'int'; value: number }
  | { type: 'float'; value: number }
  | { type: 'string'; value: string }
  | { type: 'boolean'; value: boolean }
  | { type: 'point'; value: Point }
  | { type: 'color'; value: Color }
  | { type: 'path'; value: Path }
  | { type: 'list'; value: Value[] }
  | { type: 'map'; value: Record<string, Value> };
```

### Path Rendering

Paths use contours with typed points: `moveTo`, `lineTo`, `curveTo`/`curveData` (cubic bezier), `quadTo`/`quadData` (quadratic bezier from TrueType fonts). Vello fills each path with the nonzero winding rule (matching TrueType conventions). The `editable` flag suppresses handle visualization for generated paths (e.g., font glyphs).

## Vite Configuration

- `@vitejs/plugin-react` — React JSX transform
- `vite-plugin-electron` — builds main + preload entries to `dist-electron/`
- `vite-plugin-electron-renderer` — enables renderer process Node.js API access where needed
- Path aliases: `@` → `src/renderer/`, `@shared` → `src/shared/`, `@wasm` → `wasm/`

## Electron Window Configuration

```typescript
{
  width: 1280, height: 800,
  minWidth: 800, minHeight: 500,
  backgroundColor: '#27272a',  // ZINC_800
  titleBarStyle: 'hiddenInset',
  trafficLightPosition: { x: 12, y: 10 },
  webPreferences: {
    contextIsolation: true,
    nodeIntegration: false,
    sandbox: false,
  },
}
```
