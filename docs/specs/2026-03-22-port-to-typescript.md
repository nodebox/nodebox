# NodeBox TypeScript Port — Design Specification

## Overview

Port NodeBox from Java to a TypeScript monorepo with two packages:

- **`nodebox-core`** — Pure TypeScript library. No DOM dependencies. Runs in browser, Node.js, and Web Workers. Contains: geometry types, node model, evaluation engine, all node operations, NDBX parser/serializer, SVG import/export, Platform abstraction.
- **`nodebox-desktop`** — Electron desktop application. React 19 + Zustand + immer + Tailwind CSS + Canvas2D rendering.

Managed with [Vite+](https://viteplus.dev/) (`vp create vite:monorepo`).

### Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| File format | `.ndbx` XML, full backward compat | 90+ example files, canonical library definitions |
| Format upgrades | Port entire v1→v21 chain | Open any historical NodeBox file |
| Rendering | Canvas2D | Proven in prior electron-app, simple, sufficient |
| Curve support | Cubic + quadratic beziers | Canvas2D Path2D supports both natively |
| SVG arcs | Convert to cubic curves on import | Clean internal representation |
| Evaluation | Web Worker | Keeps UI responsive during heavy graphs |
| Node scope | All ~100+ nodes, all tested | Full parity with Java version |
| Platform | Single interface (Rust design) | Simple, proven, capability flags for environment detection |
| State management | Zustand + immer (desktop only) | Proven in prior electron-app, nice API for nested mutations |
| Fonts | opentype.js + bundled Inter | Always-available font + system fonts via Platform |
| Testing | Unit tests + golden tests vs Java | High confidence in correctness |
| Data model | Plain interfaces + pure functions | Survives postMessage/structuredClone, functional style |
| Stateful nodes | NOT ported | Badly executed feature, removed from scope |

---

## 1. Monorepo Structure

```
nodebox/
├── vite.config.ts              # Root vite+ config
├── package.json                # Workspace root (pnpm workspaces)
├── packages/
│   ├── nodebox-core/           # Pure TypeScript, zero DOM deps
│   │   ├── vite.config.ts
│   │   ├── package.json
│   │   ├── src/
│   │   │   ├── geometry/       # Point, Path, Contour, Color, Rect, Transform
│   │   │   ├── node/           # Node, Port, Connection, NodeLibrary, Value
│   │   │   ├── ops/            # generators, filters, math, list, string, color, data, network
│   │   │   ├── ndbx/           # Parser, serializer, upgrades (v1→v21)
│   │   │   ├── eval/           # Evaluation engine + function registry + type conversions
│   │   │   ├── svg/            # SVG import (with arc→curve), SVG export
│   │   │   ├── platform.ts     # Platform interface + types
│   │   │   └── index.ts        # Public API re-exports
│   │   ├── fonts/
│   │   │   └── Inter.ttf       # Bundled default font
│   │   └── tests/
│   │       ├── geometry/
│   │       ├── ops/
│   │       ├── eval/
│   │       ├── ndbx/
│   │       ├── platform/
│   │       └── golden/         # Golden test runner + expected outputs
│   │
│   └── nodebox-desktop/        # Electron + React + Zustand
│       ├── vite.config.ts
│       ├── package.json
│       ├── index.html
│       ├── src/
│       │   ├── main/           # Electron main process
│       │   │   ├── index.ts    # Window management, IPC handlers
│       │   │   ├── menu.ts     # Native menu bar
│       │   │   └── fonts.ts    # System font enumeration
│       │   ├── preload/
│       │   │   └── index.ts    # Secure IPC bridge
│       │   ├── renderer/
│       │   │   ├── App.tsx
│       │   │   ├── main.tsx
│       │   │   ├── components/
│       │   │   │   ├── AppLayout.tsx
│       │   │   │   ├── NetworkCanvas.tsx
│       │   │   │   ├── ViewerCanvas.tsx
│       │   │   │   ├── ParameterPanel.tsx
│       │   │   │   ├── AnimationBar.tsx
│       │   │   │   ├── AddressBar.tsx
│       │   │   │   ├── NodeSelectionDialog.tsx
│       │   │   │   ├── DataViewer.tsx
│       │   │   │   ├── DragValue.tsx
│       │   │   │   └── NotificationBanner.tsx
│       │   │   ├── state/
│       │   │   │   ├── store.ts
│       │   │   │   ├── library-slice.ts
│       │   │   │   ├── selection-slice.ts
│       │   │   │   ├── history-slice.ts
│       │   │   │   ├── render-slice.ts
│       │   │   │   ├── animation-slice.ts
│       │   │   │   └── ui-slice.ts
│       │   │   ├── viewer/
│       │   │   │   ├── canvas-renderer.ts
│       │   │   │   ├── four-point-handle.ts
│       │   │   │   └── handle-resolver.ts
│       │   │   ├── worker/
│       │   │   │   ├── eval-worker.ts
│       │   │   │   └── eval-bridge.ts
│       │   │   ├── platform/
│       │   │   │   └── electron-platform.ts
│       │   │   ├── hooks/
│       │   │   │   ├── usePanZoom.ts
│       │   │   │   ├── useCanvasRenderer.ts
│       │   │   │   └── useKeyboardShortcuts.ts
│       │   │   └── theme/
│       │   │       └── tokens.ts
│       │   └── shared/
│       │       └── ipc-channels.ts
│       └── tests/
│           ├── unit/
│           └── e2e/
│
├── tools/
│   └── golden-export/          # Java shim for generating golden SVGs
│       ├── build.xml           # Ant build file
│       └── src/
│           └── GoldenExport.java
│
├── examples/                   # Existing .ndbx example files
└── libraries/                  # Existing .ndbx library definitions
```

### Dependencies

**nodebox-core** (minimal):
- `opentype.js` — Font parsing, glyph-to-path conversion
- `@xmldom/xmldom` — XML parsing in Node.js/Worker (browser uses native DOMParser)

**nodebox-desktop**:
- `react`, `react-dom` — UI framework
- `zustand` — State management
- `immer` — Immutable updates with mutable API
- `electron` — Desktop shell
- `tailwindcss` — Styling
- `lucide-react` — Icons
- `vite-plugin-electron` — Electron + Vite integration

**Dev dependencies**:
- `vitest` — Unit testing
- `@playwright/test` — E2E testing
- `typescript` — Type checking

---

## 2. Core Data Model

### 2.1 Geometry Types (`nodebox-core/src/geometry/`)

```ts
// point.ts
export interface Point {
  x: number;
  y: number;
}

// path.ts
export type PointType = 'lineTo' | 'curveTo' | 'curveData' | 'quadTo' | 'quadData';

export interface PathPoint {
  point: Point;
  type: PointType;
}

export interface Contour {
  points: PathPoint[];
  closed: boolean;
}

export interface Path {
  contours: Contour[];
  fill: Color | null;
  stroke: Color | null;
  strokeWidth: number;
}

// color.ts — values in 0.0–1.0 range
export interface Color {
  r: number;
  g: number;
  b: number;
  a: number;
}

// rect.ts
export interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

// transform.ts — 2D affine matrix [a, b, c, d, tx, ty]
export interface Transform {
  m: [number, number, number, number, number, number];
}

// text.ts
export type TextAlign = 'left' | 'center' | 'right';

export interface Text {
  text: string;
  position: Point;
  fontFamily: string;
  fontSize: number;
  align: TextAlign;
  fill: Color | null;
}
```

### 2.2 Geometry Utility Functions

Each type has associated pure functions in the same module:

```ts
// point.ts functions
export function addPoints(a: Point, b: Point): Point;
export function subtractPoints(a: Point, b: Point): Point;
export function scalePoint(p: Point, s: number): Point;
export function distanceBetween(a: Point, b: Point): number;
export function angleBetween(a: Point, b: Point): number;
export function lerpPoint(a: Point, b: Point, t: number): Point;

// path.ts functions
export function createPath(): Path;
export function moveTo(path: Path, x: number, y: number): Path;
export function lineTo(path: Path, x: number, y: number): Path;
export function curveTo(path: Path, cx1: number, cy1: number, cx2: number, cy2: number, x: number, y: number): Path;
export function quadTo(path: Path, cx: number, cy: number, x: number, y: number): Path;
export function closePath(path: Path): Path;
export function pathBounds(path: Path): Rect;
export function transformPath(path: Path, transform: Transform): Path;
export function pathLength(path: Path): number;
export function pointOnPath(path: Path, t: number): Point;
export function resampleByAmount(path: Path, amount: number): Path;
export function resampleByLength(path: Path, maxLength: number): Path;
export function pathContains(path: Path, point: Point): boolean;
export function clonePath(path: Path): Path;

// contour.ts functions
export function contourLength(contour: Contour): number;
export function contourBounds(contour: Contour): Rect;

// color.ts functions
export function colorFromHex(hex: string): Color;
export function colorToHex(color: Color): string;
export function colorFromHSB(h: number, s: number, b: number, a: number): Color;
export function colorToCSS(color: Color): string;

// transform.ts functions
export function identityTransform(): Transform;
export function translateTransform(tx: number, ty: number): Transform;
export function rotateTransform(degrees: number): Transform;
export function scaleTransform(sx: number, sy: number): Transform;
export function skewTransform(sx: number, sy: number): Transform;
export function multiplyTransforms(a: Transform, b: Transform): Transform;
export function transformPoint(transform: Transform, point: Point): Point;
```

### 2.3 Node Model (`nodebox-core/src/node/`)

```ts
// port.ts
export type PortType = 'int' | 'float' | 'string' | 'boolean' | 'point'
                     | 'color' | 'geometry' | 'list' | 'data' | 'context';

export type Widget = 'none' | 'int' | 'float' | 'angle' | 'string' | 'text'
                   | 'password' | 'toggle' | 'color' | 'point' | 'menu'
                   | 'file' | 'font' | 'image' | 'data' | 'seed' | 'gradient';

export type PortRange = 'value' | 'list';

export interface MenuItem {
  key: string;
  label: string;
}

export interface Port {
  name: string;
  type: PortType;
  label: string | null;
  description: string | null;
  widget: Widget;
  range: PortRange;
  value: Value;
  minimumValue: number | null;
  maximumValue: number | null;
  menuItems: MenuItem[];
  childReference: string | null;  // for published ports on networks: "childNode/childPort"
                                  // allows a network to expose an inner node's port on its own interface
}

// node.ts
export interface Connection {
  outputNode: string;   // source node name (nodes have a single implicit output)
  inputNode: string;    // target node name
  inputPort: string;    // target port name
}

export interface Node {
  name: string;
  prototype: string | null;
  function: string | null;
  category: string | null;
  description: string | null;
  image: string | null;       // icon filename (e.g., "rect.png"), loaded from library dir or bundled
  position: Point;
  comment: string | null;     // user annotation
  inputs: Port[];
  outputType: string;         // free-form string (e.g., "geometry", "point", "float", "int", "boolean")
                              // uses same values as PortType but is a string because it can also be
                              // "data", "list", or custom types. Kept as string for Java compatibility.
  outputRange: PortRange;
  children: Node[];           // non-empty = this is a network node
  connections: Connection[];
  renderedChild: string | null;
  handle: string | null;
  alwaysRendered: boolean;    // if true, evaluate even when not the rendered child (side effects)
}

// library.ts
export interface NodeLibrary {
  formatVersion: number;
  uuid: string;
  root: Node;
  properties: Record<string, string>;
  functionLinks: string[];
}

// value.ts — discriminated union for runtime values
export type Value =
  | { type: 'int'; value: number }
  | { type: 'float'; value: number }
  | { type: 'string'; value: string }
  | { type: 'boolean'; value: boolean }
  | { type: 'point'; value: Point }
  | { type: 'color'; value: Color }
  | { type: 'geometry'; value: Path[] }  // Geometry is always Path[]. A single Path is Path[] with one element.
  | { type: 'list'; value: Value[] }
  | { type: 'data'; value: Record<string, unknown> }
  | { type: 'null' };
```

**Important notes on the Value type:**

- **No separate `path` type.** In Java, both `Path` and `Geometry` are used as output. In TypeScript, we normalize: a single `Path` becomes `{ type: 'geometry', value: [path] }`. This simplifies the evaluator — all geometry is `Path[]`.
- **Port default values in `.ndbx` are raw strings** (e.g., `value="200.0"`, `value="0.00,0.00"`, `value="#ff0000ff"`). The NDBX parser must convert these to `Value` objects based on the port's `type` field:
  - `int` → parse as integer: `{ type: 'int', value: parseInt(str) }`
  - `float` → parse as float: `{ type: 'float', value: parseFloat(str) }`
  - `boolean` → `{ type: 'boolean', value: str === 'true' }`
  - `string` → `{ type: 'string', value: str }`
  - `point` → parse `"x,y"`: `{ type: 'point', value: { x, y } }`
  - `color` → parse hex `"#rrggbbaa"`: `{ type: 'color', value: { r, g, b, a } }`
  - `geometry`, `list`, `data` → default to `{ type: 'null' }` (no meaningful string representation)

**Geometry/Point output rules for the evaluator:**

- A node with `outputType: 'geometry'` and `outputRange: 'value'` returns `Value` with `type: 'geometry'`
- A node with `outputType: 'point'` and `outputRange: 'list'` (e.g., `grid`) returns `Value[]` where each element is `{ type: 'point', value: ... }`
- When a `Point[]` result connects to a `geometry` port with `range: 'value'`, the type conversion wraps the point list into a `Value` of type `'geometry'` (for Java compatibility — see `NodeContext.convertResultsForPort`)

### 2.4 Type Compatibility

Port from Java's `Node.isCompatible()`:

```ts
// node/type-compat.ts
export function isCompatible(outputType: string, inputType: string): boolean;
```

Rules (matching Java exactly):
- Same type → compatible
- Anything → `string` (via toString)
- `int` ↔ `float` (rounding for float→int)
- `int`/`float` → `point` (x=y=value)

---

## 3. Evaluation Engine (`nodebox-core/src/eval/`)

### 3.1 Function Registry

```ts
// eval/function-registry.ts
export type NodeFunction = (...args: Value[]) => Value | Value[];

export interface FunctionRegistry {
  get(name: string): NodeFunction | undefined;
  register(name: string, fn: NodeFunction): void;
  has(name: string): boolean;
}

export function createFunctionRegistry(): FunctionRegistry;
export function createDefaultRegistry(): FunctionRegistry; // registers all ~100 node functions
```

### 3.2 Evaluation

```ts
// eval/evaluate.ts
export interface EvalError {
  nodePath: string;
  message: string;
}

export interface EvalResult {
  paths: Path[];           // geometry output (rendered as vector paths on Canvas2D)
  texts: Text[];           // text objects that should be rendered as text (not converted to paths)
                           // Note: the `textpath` node converts text to Path outlines. But if a
                           // future "text" node outputs styled text for display, it goes here.
                           // For now, this is primarily used for data labels or future text rendering.
  output: Value[];         // raw output values (for data viewer — shows numbers, lists, tables)
  errors: EvalError[];     // per-node errors (e.g., missing connection, division by zero)
}

export interface EvalOptions {
  library: NodeLibrary;
  frame: number;
  platform: Platform;
  functionRegistry?: FunctionRegistry;  // defaults to createDefaultRegistry()
}

export async function evaluate(options: EvalOptions): Promise<EvalResult>;
```

The function is async because some nodes perform I/O (importCsv, importSvg, httpGet) via the Platform interface.

### 3.3 Evaluation Algorithm

Matching Java's `NodeContext`:

1. **Build flattened node map**: `Map<string, Node>` where key is the full path (e.g., `/root/mesh/line1`). This allows O(1) lookup during evaluation.

2. **Find root network's `renderedChild`** and begin evaluation from there.

3. **`renderChild(networkPath, child)`** — evaluate a child node within a network:
   a. For each input port of child:
      - If connected → recursively evaluate the upstream output node via `renderChild`
      - If unconnected → use the port's default value
      - Apply type conversions for port compatibility (see section 3.5)
      - Clamp values to port min/max if specified
   b. Build argument maps via list matching (see section 3.4)
   c. For each argument map: call `renderNode(childPath, argumentMap)`, collect results
   d. Cache results in `nodeArgumentsResults` to avoid re-evaluation with same inputs

4. **`renderNode(nodePath, argumentMap?)`** — evaluate a single node:
   - If node **is a network** (has children):
     - **Pass published port data to inner nodes** (see section 3.3.1 below)
     - Evaluate the network's `renderedChild` via `renderChild`
     - Also evaluate any `alwaysRendered` children
   - If node **has a function** → invoke via function registry with argument map
   - **Post-process result**:
     - If `outputRange === 'list'` and result is not already a list → wrap in list
     - If `outputRange === 'value'` and result is a list → wrap entire result as single item
     - If result is `null` → return empty list
   - Cache results per node path

5. **Cache results** per node path within the evaluation context. Each evaluation frame starts with a fresh cache.

### 3.3.1 Subnetwork Evaluation (Published Ports)

Subnetworks are the key compositional mechanism in NodeBox. A network node contains child nodes, connections between them, and exposes selected child ports as "published ports" on its own interface.

**Example** (from `Mesh.ndbx`):
```xml
<node name="mesh" prototype="core.network" renderedChild="line1">
    <node name="line1" prototype="corevector.line"/>
    <node name="slice1" prototype="list.slice"/>
    <!-- Published ports: expose inner ports on the network's interface -->
    <port childReference="slice1.list" name="list" range="list" type="list" widget="none"/>
    <port childReference="slice1.start_index" name="start_index" range="value" type="int" value="1"/>
    <port childReference="line1.point2" name="point2" range="list" type="point"/>
    <!-- Internal connections -->
    <conn input="line1.point1" output="slice1"/>
</node>
```

**How published ports work:**

1. A published port on a network has a `childReference` like `"slice1.list"` — meaning it maps to the `list` port on the child node `slice1`.
2. When data flows into the network's published port (via a connection from an outer node), it is passed to the corresponding child port **as if** the child port were directly connected.
3. During evaluation of the network, the evaluator checks if any published ports have incoming data (from the `networkArgumentMap`) and routes that data to the referenced child ports.

**Evaluation flow for subnetworks:**

```
renderChild(parentPath, meshNode):
  1. Evaluate upstream nodes connected to mesh's published ports
     → mesh.list gets data from point1
     → mesh.start_index gets data from range1
     → mesh.point2 gets data from point1
  2. Call renderNode("/root/mesh", argumentMap={list: ..., start_index: ..., point2: ...})

renderNode("/root/mesh", argumentMap):
  1. Node is a network → evaluate its renderedChild "line1"
  2. renderChild("/root/mesh", line1):
     a. line1.point1 is connected to slice1 → evaluate slice1
     b. line1.point2 is a published port → get value from networkArgumentMap
     c. For slice1: slice1.list is a published port → get from networkArgumentMap
                    slice1.start_index is a published port → get from networkArgumentMap
  3. Return line1's output as the mesh network's output
```

**Key implementation details:**

- The `networkArgumentMap` passes data from published ports to inner child ports. When evaluating a child node inside a network, check if any of its ports are published (have a corresponding entry in the parent network's `inputs` with matching `childReference`). If so, use the published port's incoming data instead of the child port's default value.
- Published ports inherit the child port's type, widget, range, min/max, and menu items.
- A published port can override `value` (providing a different default) and `range` (e.g., the inner port might be `value` but the published port exposes it as `list`).
- Networks can be nested arbitrarily deep. The evaluation is recursive.

### 3.3.2 Address Bar / Path Navigation

The desktop UI needs to support navigating into subnetworks:

- The **address bar** shows the current path as breadcrumbs: `root / mesh / line1`
- Clicking a breadcrumb navigates to that level
- Double-clicking a network node in the network view enters it
- The network view then shows the subnetwork's children
- The parameter panel shows the selected child's ports
- The viewer shows the output of the subnetwork's rendered child

This requires the store to track:
```ts
// In UISlice or a new navigation slice:
currentNetworkPath: string;  // e.g., "/root" or "/root/mesh"
```

When navigating into a subnetwork, the network view displays that subnetwork's children and connections. When navigating back, it restores the parent view.

### 3.4 List Matching Algorithm

The "longest list" algorithm — core to NodeBox's dataflow:

```ts
function wrappingGet<T>(list: T[], index: number): T {
  return list[index % list.length];
}

function buildArgumentMaps(portArguments: Map<Port, Value[]>): Map<Port, Value>[] {
  // 1. For ports with range 'list': argument is always the full list (counts as size 1)
  // 2. For ports with range 'value': argument cycles through the list
  // 3. Number of invocations = length of the longest 'value' port list
  // 4. If any list is empty, return no invocations

  const maxSize = Math.max(...[...portArguments.entries()]
    .map(([port, values]) => port.range === 'list' ? 1 : values.length));

  if (maxSize === 0) return [];

  const maps: Map<Port, Value>[] = [];
  for (let i = 0; i < maxSize; i++) {
    const map = new Map<Port, Value>();
    for (const [port, values] of portArguments) {
      if (port.range === 'list') {
        map.set(port, { type: 'list', value: values });
      } else {
        map.set(port, wrappingGet(values, i));
      }
    }
    maps.push(map);
  }
  return maps;
}
```

### 3.5 Type Conversions

```ts
// eval/type-conversions.ts
export function convert(value: Value, targetType: PortType): Value;
export function clampValue(value: Value, min: number | null, max: number | null): Value;
```

**Complete conversion table** (matching Java's `TypeConversions` — all combinations):

| From → To | int | float | boolean | string | point | color | geometry |
|-----------|-----|-------|---------|--------|-------|-------|----------|
| **int** | — | direct | >0 → true | toString | {x:v, y:v} | grayscale v/255 | — |
| **float** | Math.round() | — | >0 → true | toString | {x:v, y:v} | grayscale v/255 | — |
| **boolean** | true→1, false→0 | true→1.0, false→0.0 | — | "true"/"false" | — | true→white, false→black | — |
| **string** | parseInt | parseFloat | "true"→true | — | parse "x,y" | parse "#rrggbbaa" | — |
| **point** | — | — | — | "x,y" | — | — | — |
| **color** | — | — | — | "#rrggbbaa" | — | — | — |
| **geometry** | — | — | — | toString | extract points | — | — |
| **list** | — | — | — | — | — | — | — |
| **any** | — | — | — | toString | — | — | pass-through |

Additional rules:
- **`list` input type**: pass through without conversion (any value is accepted)
- **`data` input type**: pass through without conversion
- **`context` input type**: injected by evaluator (see section 3.6)
- Geometry → Point[]: extract all points from all contours of all paths
- If conversion is not defined (marked `—`), the value passes through unchanged

### 3.6 Context Port Handling

Ports of type `context` are special — they are not connected in the graph and have no user-editable value. Instead, the evaluation engine automatically injects a context object containing:

```ts
interface EvalContextData {
  frame: number;        // current animation frame (default 1)
  mouse: Point | null;  // mouse position (if available, null in headless)
}
```

When the evaluator encounters a port with `type: 'context'`, it injects the context data as the argument instead of looking for a connection or default value. This is how the `core/frame` node gets the current frame number.

The `core/frame` node has a single input port of type `context` and returns the frame number as a float:
```ts
registry.register('core/frame', (ctx) => {
  return { type: 'float', value: asContext(ctx).frame };
});
```

### 3.7 Function Registry Adapter Pattern

Node operation functions are written with plain TypeScript types (not `Value` wrappers):

```ts
// The actual implementation — clean, testable:
function rect(position: Point, width: number, height: number, roundness: Point): Path { ... }
```

The function registry adapter wraps/unwraps `Value` arguments:

```ts
// eval/register-ops.ts
function registerGenerator(
  registry: FunctionRegistry,
  name: string,
  fn: (...args: any[]) => any,
  outputType: string,
  outputRange: PortRange,
): void {
  registry.register(name, (...args: Value[]) => {
    // Unwrap Value[] to plain types based on each Value's type
    const unwrapped = args.map(unwrapValue);
    const result = fn(...unwrapped);
    // Wrap return value based on outputType
    return wrapResult(result, outputType, outputRange);
  });
}

function unwrapValue(v: Value): any {
  if (v.type === 'null') return null;
  return v.value;  // Point, number, string, boolean, Color, Path[], etc.
}

function wrapResult(result: any, outputType: string, outputRange: PortRange): Value {
  if (result === null || result === undefined) return { type: 'null' };
  if (outputType === 'geometry') {
    // Normalize: single Path → Path[], Path[] stays Path[]
    const paths = Array.isArray(result) ? result : [result];
    return { type: 'geometry', value: paths };
  }
  if (outputType === 'point') return { type: 'point', value: result };
  if (outputType === 'float') return { type: 'float', value: result };
  if (outputType === 'int') return { type: 'int', value: result };
  if (outputType === 'boolean') return { type: 'boolean', value: result };
  if (outputType === 'string') return { type: 'string', value: result };
  if (outputType === 'color') return { type: 'color', value: result };
  if (outputType === 'data') return { type: 'data', value: result };
  return { type: 'null' };
}
```

This pattern means:
- Op functions are clean and testable without Value wrappers
- The adapter is generated once at registration time
- Context ports are handled specially (injected, not unwrapped)

---

## 4. Operations (`nodebox-core/src/ops/`)

All node functions are pure functions. They receive unwrapped values (not `Value` wrappers) and return plain types. The function registry adapter handles wrapping/unwrapping (see section 3.7).

### 4.0 Core Operations (`ops/core.ts`)

Essential built-in operations. Reference: `libraries/core/core.ndbx`.

| Function | Signature | Notes |
|----------|-----------|-------|
| `core/zero` | `() → 0.0` | Default function for nodes with no function. Returns 0. |
| `core/frame` | `(context: EvalContextData) → number` | Returns current frame number from context. Essential for animation. |

### 4.1 Generators (`ops/generators.ts`)

Functions that create new geometry. Reference: `libraries/corevector/corevector.ndbx` + `CoreVectorFunctions.java` + `pyvector.py`.

| Function | Signature | Notes |
|----------|-----------|-------|
| `rect` | `(position: Point, width: number, height: number, roundness: Point) → Path` | Rounded corners via roundness |
| `ellipse` | `(position: Point, width: number, height: number) → Path` | Approximated with 4 cubic beziers |
| `arc` | `(position: Point, width: number, height: number, startAngle: number, degrees: number, type: 'pie'\|'chord'\|'open') → Path` | |
| `line` | `(point1: Point, point2: Point, points: number) → Path` | Interpolated points along line |
| `lineAngle` | `(position: Point, angle: number, distance: number, points: number) → Path` | |
| `polygon` | `(position: Point, radius: number, sides: number, align: boolean) → Path` | Regular polygon |
| `star` | `(position: Point, points: number, outer: number, inner: number) → Path` | |
| `grid` | `(columns: number, rows: number, width: number, height: number, position: Point) → Point[]` | Returns points, not geometry |
| `connect` | `(points: Point[], closed: boolean) → Path` | Connect points into path |
| `quadCurve` | `(point1: Point, point2: Point, t: number, distance: number) → Path` | Quadratic curve |
| `link` | `(shape1: Path, shape2: Path, orientation: 'horizontal'\|'vertical') → Path` | Visual connector |
| `makePoint` | `(x: number, y: number) → Point` | |
| `freehand` | `(pathData: string) → Path` | Parse internal path data |
| `group` | `(shapes: Path[]) → Path[]` | Combine into single geometry |
| `importSvg` | `(file: string, centered: boolean, position: Point, platform: Platform) → Path` | Async, via Platform |
| `textpath` | `(text: string, fontName: string, fontSize: number, align: TextAlign, position: Point, width: number) → Path` | Via opentype.js |
| `textOnPath` | `(text: string, path: Path, fontName: string, fontSize: number, alignment: string, margin: number, baselineOffset: number) → Path` | |
| `shapeOnPath` | `(shapes: Path[], path: Path, amount: number, alignment: string, spacing: number, margin: number, baselineOffset: number) → Path[]` | |

### 4.2 Filters (`ops/filters.ts`)

Functions that transform existing geometry.

| Function | Signature | Notes |
|----------|-----------|-------|
| `align` | `(shape: Path, position: Point, halign: string, valign: string) → Path` | |
| `colorize` | `(shape: Path, fill: Color, stroke: Color, strokeWidth: number) → Path` | |
| `copy` | `(shape: Path, copies: number, order: string, translate: Point, rotate: number, scale: Point) → Path[]` | |
| `fit` | `(shape: Path, position: Point, width: number, height: number, keepProportions: boolean) → Path` | |
| `fitTo` | `(shape: Path, bounding: Path, keepProportions: boolean) → Path` | |
| `translate` | `(shape: Path, translate: Point) → Path` | |
| `rotate` | `(shape: Path, angle: number, origin: Point) → Path` | |
| `scale` | `(shape: Path, scale: Point, origin: Point) → Path` | Scale as percentage (100 = 1x) |
| `skew` | `(shape: Path, skew: Point, origin: Point) → Path` | |
| `reflect` | `(shape: Path, position: Point, angle: number, keepOriginal: boolean) → Path` | |
| `snap` | `(shape: Path, distance: number, strength: number, position: Point) → Path` | |
| `resample` | `(shape: Path, method: 'length'\|'amount', length: number, points: number, perContour: boolean) → Path` | |
| `wiggle` | `(shape: Path, scope: 'points'\|'contours'\|'paths', offset: Point, seed: number) → Path[]` | |
| `ungroup` | `(shape: Path) → Path[]` | Decompose geometry |
| `compound` | `(shape1: Path, shape2: Path, function: 'united'\|'subtracted'\|'intersected', invertDifference: boolean) → Path` | Boolean operations |
| `scatter` | `(shape: Path, amount: number, seed: number) → Point[]` | Random points within shape |
| `centroid` | `(shape: Path) → Point` | Geometric center |
| `pointOnPath` | `(shape: Path, t: number) → Point` | |
| `sort` | `(shapes: Path[], orderBy: string, position: Point) → Path[]` | |
| `stack` | `(shapes: Path[], direction: string, margin: number) → Path[]` | |
| `distribute` | `(shapes: Path[], horizontal: string, vertical: string) → Path[]` | |
| `delete` | `(shape: Path, bounding: Path, scope: 'points'\|'paths', operation: 'selected'\|'non-selected') → Path` | |
| `roundSegments` | `(shape: Path, d: number) → Path` | |

### 4.3 Math (`ops/math.ts`)

All math operations. Reference: `libraries/math/math.ndbx` + `MathFunctions.java`.

| Function | Notes |
|----------|-------|
| `abs`, `ceil`, `floor`, `round` | Standard math |
| `add`, `subtract`, `multiply`, `divide`, `mod`, `negate` | Arithmetic |
| `sin`, `cos`, `sqrt`, `pow`, `log` | Trigonometry (sin/cos take degrees, not radians) |
| `radians`, `degrees` | Convert between degrees and radians |
| `pi`, `e` | Constants |
| `even`, `odd` | Boolean predicates |
| `min`, `max`, `average`, `sum` | Aggregate (take lists) |
| `number`, `integer`, `boolean` | Value constructors |
| `range` | `(start, end, step) → number[]` |
| `sample` | `(amount, start, end) → number[]` (evenly spaced) |
| `randomNumbers` | `(amount, start, end, seed) → number[]` |
| `convertRange` | `(value, srcStart, srcEnd, tgtStart, tgtEnd, method) → number` |
| `coordinates` | `(position, angle, distance) → Point` |
| `angle`, `distance`, `reflect` | Point math |
| `makeNumbers` | `(string, separator) → number[]` |
| `runningTotal` | `(values) → number[]` |
| `wave` | `(min, max, period, offset, type) → number` |
| `compare` | `(value1, value2, comparator) → boolean` |
| `logicOperator` | `(bool1, bool2, op) → boolean` |

### 4.4 List (`ops/list.ts`)

| Function | Notes |
|----------|-------|
| `combine` | Merge up to 7 lists |
| `count` | List length |
| `first`, `second`, `last`, `rest` | Accessors |
| `slice` | `(list, startIndex, size, invert)` |
| `sort` | `(list, key)` |
| `reverse`, `shuffle`, `shift` | Reordering |
| `repeat` | `(list, amount, perItem)` |
| `pick` | `(list, amount, seed)` — random selection |
| `takeEvery` | `(list, n)` |
| `cull` | `(list, booleans)` — filter by boolean mask |
| `distinct` | `(list, key)` |
| `switch` | `(inputs[], index)` — select one list |
| `keys` | Extract keys from maps |
| `zipMap` | `(keys, values) → map` |

### 4.5 String (`ops/string.ts`)

| Function | Notes |
|----------|-------|
| `string` | Value constructor |
| `makeStrings` | Split by separator |
| `length`, `wordCount` | Counts |
| `concatenate` | Join up to 7 strings |
| `formatNumber` | Printf-style formatting |
| `characters`, `characterAt` | Character access |
| `contains`, `startsWith`, `endsWith`, `equals` | Predicates |
| `replace`, `subString`, `trim`, `changeCase` | Manipulation |
| `randomCharacter` | `(chars, amount, seed)` |
| `asBinaryString`, `asBinaryList`, `asNumberList` | Encoding |

### 4.6 Color (`ops/color.ts`)

| Function | Notes |
|----------|-------|
| `color` | Pass-through constructor |
| `grayColor` | `(gray, alpha, range)` |
| `hsbColor` | `(hue, saturation, brightness, alpha, range)` |
| `rgbColor` | `(red, green, blue, alpha, range)` |

### 4.7 Data (`ops/data.ts`)

| Function | Notes |
|----------|-------|
| `importCsv` | `(file, delimiter, quotes, numberSeparator)` — via Platform |
| `importText` | `(file)` — via Platform |
| `lookup` | `(list, key)` |
| `filterData` | `(data, key, op, value)` |
| `makeTable` | `(headers, list1..list6)` |

### 4.8 Network (`ops/network.ts`)

| Function | Notes |
|----------|-------|
| `httpGet` | `(url, username, password, refreshTime)` — via Platform |
| `queryJson` | `(json, query)` — JSONPath query |
| `encodeUrl` | `(value)` |

---

## 5. NDBX Parser & Upgrades (`nodebox-core/src/ndbx/`)

### 5.1 Parser

```ts
// ndbx/parser.ts
export function parseNdbx(xml: string, libraryLoader?: (name: string) => NodeLibrary): NodeLibrary;
```

Parsing flow:
1. Parse XML string into DOM (DOMParser in browser, @xmldom/xmldom in Node.js)
2. Extract `formatVersion` from `<ndbx>` root element
3. If version < current: run upgrade chain
4. Parse `<node>` elements recursively (children, ports, connections)
5. Resolve `prototype` references against loaded libraries
6. Return `NodeLibrary`

Library resolution: Node prototypes like `corevector.rect` reference library definitions. The parser takes an optional `libraryLoader` callback that loads library `.ndbx` files (via Platform). Standard libraries (core, corevector, math, list, color, string, data, device, network) are loaded on demand.

### 5.2 Serializer

```ts
// ndbx/serializer.ts
export function serializeNdbx(library: NodeLibrary): string;
```

Generates well-formed XML matching the Java `NDBXWriter` output. Always writes the current format version.

### 5.3 Upgrade Chain

```ts
// ndbx/upgrades.ts
export const CURRENT_FORMAT_VERSION = 21;

export function upgradeNdbx(xml: string, fromVersion: number): { xml: string; warnings: string[] };
```

Port all upgrade functions from Java's `NodeLibraryUpgrades.java`:

| Upgrade | Key changes |
|---------|-------------|
| v1→v2 | Rotate nodes 90°, pixel→grid unit conversion |
| v2→v3 | Rename `corevector.generator` function refs |
| v3→v4 | `to_points` → `point` node rename |
| v4→v5 | Port type changes |
| v5→v6 | Filter rename |
| v6→v7 | `list.filter` → `list.cull` |
| v7→v8 | Connection format changes |
| v8→v9 through v20→v21 | Various node/port renames and type changes |

Each upgrade operates on the raw XML string (regex/DOM manipulation), matching the Java approach exactly. This ensures even very old .ndbx files load correctly.

---

## 6. SVG Import & Export (`nodebox-core/src/svg/`)

### 6.1 SVG Import

```ts
// svg/import.ts
export function importSvg(svgString: string): Path[];
```

Parse SVG path data into NodeBox `Path` objects:
- Support all SVG path commands: M, L, C, Q, S, T, A, Z (absolute and relative)
- **Arc commands (A)**: Convert to cubic bezier curves on import using the SVG spec's endpoint-to-center parameterization algorithm
- Parse `<path>`, `<rect>`, `<circle>`, `<ellipse>`, `<line>`, `<polyline>`, `<polygon>` elements
- Apply transforms from `transform` attribute
- Extract fill/stroke colors

### 6.2 SVG Export

```ts
// svg/export.ts
export interface SvgExportOptions {
  width: number;
  height: number;
  precision?: number;        // decimal places, default 2
  background?: Color | null;
}

export function exportSvg(paths: Path[], texts: Text[], options: SvgExportOptions): string;
```

Generate SVG string from evaluated geometry. Used for file export and golden test comparison.

---

## 7. Platform Interface (`nodebox-core/src/platform.ts`)

```ts
export interface PlatformInfo {
  osName: string;
  isWeb: boolean;
  isMobile: boolean;
  hasFilesystem: boolean;
  hasNativeDialogs: boolean;
}

export interface ProjectContext {
  root: string | null;
  projectFile: string | null;
  frame: number;
}

export interface DirectoryEntry {
  name: string;
  isDirectory: boolean;
}

export interface FileFilter {
  name: string;
  extensions: string[];
}

export type LogLevel = 'error' | 'warn' | 'info' | 'debug';

export class PlatformError extends Error {
  constructor(
    public code: 'unsupported' | 'not_found' | 'permission_denied' | 'sandbox_violation' | 'network' | 'io' | 'other',
    message: string,
  ) {
    super(message);
  }
}

export interface Platform {
  platformInfo(): PlatformInfo;

  // File I/O (sandboxed to project directory)
  readTextFile(ctx: ProjectContext, path: string): Promise<string>;
  readBinaryFile(ctx: ProjectContext, path: string): Promise<Uint8Array>;
  writeFile(ctx: ProjectContext, path: string, data: Uint8Array): Promise<void>;
  listDirectory(ctx: ProjectContext, path: string): Promise<DirectoryEntry[]>;

  // Libraries
  loadLibrary(name: string): Promise<string>;
  loadAppResource(name: string): Promise<Uint8Array>;

  // Fonts
  listFonts(): Promise<string[]>;
  getFontBytes(fontFamily: string): Promise<Uint8Array>;

  // Network
  httpGet(url: string): Promise<Uint8Array>;

  // Logging
  log(level: LogLevel, message: string): void;
}
```

### Sandboxing

File operations enforce that paths stay within the project directory:
- Reject absolute paths
- Reject paths containing `..`
- Reject Windows drive letters
- All paths are relative to `ProjectContext.root`

### Implementations

- **`ElectronPlatform`** (in `nodebox-desktop`): Delegates to Electron IPC for filesystem, uses Node.js `fs` in main process, `rfd`-style native dialogs, system font enumeration.
- **`BrowserPlatform`** (in `nodebox-core`): Bundled libraries only, no filesystem, bundled Inter font only. For future web deployment.
- **`TestPlatform`** (in `nodebox-core`): In-memory filesystem for unit tests. Pre-load files as `Map<string, string>`.

---

## 8. Desktop Application (`nodebox-desktop`)

### 8.1 Zustand Store

```ts
// state/store.ts
export type AppState = LibrarySlice & SelectionSlice & HistorySlice
                     & UISlice & AnimationSlice & RenderSlice;

export const useStore = create<AppState>()(
  immer((set, get) => ({
    ...createLibrarySlice(set),
    ...createSelectionSlice(set),
    ...createHistorySlice(set, get),
    ...createUISlice(set),
    ...createAnimationSlice(set),
    ...createRenderSlice(set),
  })),
);
```

**Slices:**

| Slice | Responsibility |
|-------|---------------|
| `LibrarySlice` | Node graph data, file path, dirty flag. Mutations via immer: addNode, removeNode, setPortValue, addConnection, removeConnection, setRenderedChild, setNodePosition, setCanvasProperty |
| `SelectionSlice` | Selected node names, multi-select |
| `HistorySlice` | Undo/redo with 50-level snapshot history. Snapshots the library on each mutation. |
| `UISlice` | Dialog visibility, view toggles (show points, show origin, show bounds, show handles), `currentNetworkPath` for subnetwork navigation |
| `AnimationSlice` | Current frame, playing/stopped, frame range, FPS |
| `RenderSlice` | Latest EvalResult (paths, texts, errors), loading state |

### 8.2 Web Worker Evaluation

```ts
// worker/eval-worker.ts (runs in Web Worker)
import { evaluate, createDefaultRegistry } from 'nodebox-core';

self.onmessage = async (e: MessageEvent) => {
  const { library, frame, files } = e.data;
  // Create a WorkerPlatform that reads from the pre-loaded files map
  const platform = new WorkerPlatform(files);
  const result = await evaluate({ library, frame, platform });
  self.postMessage({ result });
};

// worker/eval-bridge.ts (main thread)
// Manages worker lifecycle
// Debounces evaluation during parameter drags (requestAnimationFrame)
// Updates render-slice with results
```

**Flow:**
1. Library mutation in Zustand store triggers evaluation
2. Bridge serializes library + frame + pre-loaded files → Worker
3. Worker evaluates, posts back `EvalResult`
4. Bridge updates `RenderSlice` with paths/texts/errors
5. ViewerCanvas re-renders

### 8.3 Canvas2D Renderer

```ts
// viewer/canvas-renderer.ts
export function renderPaths(ctx: CanvasRenderingContext2D, paths: Path[]): void;
export function renderTexts(ctx: CanvasRenderingContext2D, texts: Text[]): void;
export function contourToPath2D(contour: Contour): Path2D;
```

Supports:
- Cubic beziers (`bezierCurveTo`)
- Quadratic beziers (`quadraticCurveTo`)
- Fill and stroke with colors
- Point visualization (circles at each PathPoint)
- Handle visualization (four-point handles for shape manipulation)
- Pan/zoom via transform matrix

### 8.4 UI Components

All components ported from the electron-app branch, adapted to remove Rust/WASM dependency:

| Component | Description |
|-----------|-------------|
| `AppLayout` | Three-pane layout with draggable splitters |
| `NetworkCanvas` | Canvas-based node graph editor with drag, connect, select |
| `ViewerCanvas` | Canvas2D geometry renderer with pan/zoom |
| `ParameterPanel` | Dynamic widget rendering for selected node's ports |
| `AnimationBar` | Frame display, play/pause/stop, frame range |
| `AddressBar` | Breadcrumb navigation, rendered child selector |
| `NodeSelectionDialog` | Tab-key popup, category filter pills, search |
| `DataViewer` | Table view for list/data output |
| `DragValue` | Draggable number input (float/int) |
| `NotificationBanner` | Dismissible notification display |

### 8.5 Electron Main Process

| Handler | Function |
|---------|----------|
| File → New | Create empty NodeLibrary |
| File → Open | Native dialog, load .ndbx, parse, set library |
| File → Save/Save As | Serialize to .ndbx, write via fs |
| File → Export SVG | Evaluate, export SVG string, write |
| File → Export PNG | Evaluate, render to offscreen canvas, export PNG |
| Edit → Undo/Redo | Delegate to history-slice |
| Asset read IPC | Sandboxed file read for data/SVG import nodes |
| Font IPC | System font enumeration and loading |

---

## 9. Testing Strategy

### 9.1 Unit Tests (`packages/nodebox-core/tests/`)

```
tests/
├── geometry/
│   ├── point.test.ts         # Point arithmetic, distance, angle
│   ├── path.test.ts          # Path construction, bounds, length, transform
│   ├── contour.test.ts       # Contour length, bounds
│   ├── color.test.ts         # Hex parsing, HSB conversion, CSS output
│   └── transform.test.ts     # Matrix operations, compose, apply
├── ops/
│   ├── generators.test.ts    # Every generator function
│   ├── filters.test.ts       # Every filter function
│   ├── math.test.ts          # Every math function
│   ├── list.test.ts          # Every list function
│   ├── string.test.ts        # Every string function
│   ├── color-ops.test.ts     # Color constructors
│   ├── data.test.ts          # CSV parsing, lookup, filter
│   └── network.test.ts       # URL encoding, JSON query
├── eval/
│   ├── evaluate.test.ts      # Full graph evaluation with hand-built graphs
│   ├── list-matching.test.ts # wrappingGet, buildArgumentMaps edge cases
│   └── type-conversions.test.ts # All conversion rules
├── ndbx/
│   ├── parser.test.ts        # Parse all example .ndbx files without error
│   ├── serializer.test.ts    # Round-trip: parse → serialize → parse → compare
│   └── upgrades.test.ts      # Each upgrade step preserves semantics
├── svg/
│   ├── import.test.ts        # SVG path parsing, arc conversion
│   └── export.test.ts        # SVG output correctness
├── platform/
│   └── sandbox.test.ts       # Path validation, sandbox escape prevention
└── golden/
    ├── golden.test.ts         # Compare TS evaluation output vs Java golden SVGs
    └── expected/              # Golden SVG files generated by Java
```

Every node function gets at least one test verifying basic output. Edge cases (null input, empty list, division by zero) get dedicated tests.

### 9.2 Golden Tests

**Generation** (`tools/golden-export/`):

A small Java CLI that:
1. Loads each `.ndbx` file from `examples/`
2. Evaluates using Java's `NodeContext`
3. Exports as SVG using Java's `SVGRenderer`
4. Saves to `packages/nodebox-core/tests/golden/expected/`

**Comparison**:

The golden test runner:
1. Loads the same `.ndbx` file with the TypeScript parser
2. Evaluates with the TypeScript engine
3. Exports as SVG with the TypeScript exporter
4. Compares against golden SVG (structural comparison, with floating-point tolerance)

### 9.3 E2E Tests (`packages/nodebox-desktop/tests/e2e/`)

Playwright tests, ported from the electron-app branch:

```
tests/e2e/
├── app-launch.spec.ts          # App starts, shows three panes
├── file-operations.spec.ts     # Open, save, save-as, export
├── node-creation.spec.ts       # Add nodes via dialog
├── node-deletion.spec.ts       # Delete selected nodes
├── connections.spec.ts         # Connect/disconnect nodes
├── parameter-editing.spec.ts   # Edit float, string, color, point, menu, toggle
├── evaluation.spec.ts          # Changes trigger re-evaluation
├── undo-redo.spec.ts           # Undo/redo for all operations
├── viewer.spec.ts              # Pan, zoom, point display
├── network-view.spec.ts        # Pan, zoom, node drag
├── animation.spec.ts           # Play, pause, frame changes
└── textpath.spec.ts            # Text rendering
```

---

## 10. Implementation Phases

Ordered by dependency. Each phase has clear success criteria (tests pass). Designed for automated sequential execution.

### Phase 1: Foundation
**Scope:** Monorepo scaffolding with vite+, geometry type definitions, geometry utility functions. Also creates the `Platform` interface type definition and `TestPlatform` (in-memory implementation) — these are needed by many later phases.
**Files:** All `packages/nodebox-core/src/geometry/*.ts`, `packages/nodebox-core/src/platform.ts`, `packages/nodebox-core/tests/geometry/*.test.ts`, `packages/nodebox-core/tests/platform/sandbox.test.ts`
**Success:** `vp test` passes all geometry and platform tests. Point arithmetic, path construction, bounds calculation, transform composition, color parsing all work. TestPlatform with in-memory filesystem works. Sandbox validation prevents path traversal.
**Dependencies:** None.

### Phase 2: Node Model
**Scope:** Node, Port, Connection, NodeLibrary, Value type definitions. Type compatibility checking. Library manipulation helpers. Includes the `core/zero` and `core/frame` function stubs.
**Files:** All `packages/nodebox-core/src/node/*.ts`, tests.
**Success:** Can create nodes, ports, connections programmatically. Type compatibility matches Java rules. Published ports (childReference) work.
**Dependencies:** Phase 1 (geometry types used by Point/Color ports).

### Phase 3: NDBX Parser & Serializer
**Scope:** XML parser, serializer, full upgrade chain (v1→v21). Must handle `<importCoreNode>` elements, `<link>` elements for function libraries, and prototype resolution. Parse all library and example files.
**Files:** `packages/nodebox-core/src/ndbx/*.ts`, tests.
**Success:** Every `.ndbx` file in `libraries/` parses without error (libraries have no external prototype dependencies). Every `.ndbx` file in `examples/` parses without error when library `.ndbx` files are provided via `libraryLoader`. Round-trip (parse → serialize → parse) produces identical libraries. Upgrade tests pass for each version step. The parser correctly maps `function="python:pyvector.py"` references to the corresponding TypeScript function names (e.g., `"pyvector/polygon"` → `"corevector/polygon"`).
**Dependencies:** Phase 1 (Platform/TestPlatform for loading library files), Phase 2 (node model types).

### Phase 4: Evaluation Engine
**Scope:** Function registry, evaluation algorithm, list matching, type conversions (full table — see section 3.5), context port injection (section 3.6), function adapter pattern (section 3.7). Register `core/zero` and `core/frame`. Test with mock functions.
**Files:** `packages/nodebox-core/src/eval/*.ts`, tests.
**Success:** Hand-built node graphs evaluate correctly. List matching (`wrappingGet`) handles all edge cases including empty lists. Type conversions match the full Java conversion table. Context ports receive frame data. The `outputRange` post-processing correctly handles list vs value nodes.
**Dependencies:** Phase 2 (node model).

### Phase 5: Generator Operations
**Scope:** All generator functions: rect, ellipse, arc, line, lineAngle, polygon, star, grid, connect, quadCurve, link, makePoint, freehand, group, doNothing (null node). Excludes importSvg (Phase 10) and textpath/textOnPath (Phase 7).
**Files:** `packages/nodebox-core/src/ops/generators.ts`, `ops/core.ts`, tests.
**Success:** Each generator produces correct geometry. Unit tests verify point positions, contour structure, fill/stroke defaults. `grid` returns `Point[]`. Functions registered in function registry.
**Dependencies:** Phase 1 (geometry), Phase 4 (registry for integration test).

### Phase 6: Filter Operations
**Scope:** All filter functions: align, colorize, copy, fit, fitTo, translate, rotate, scale, skew, reflect, snap, resample, wiggle, ungroup, scatter, centroid, pointOnPath, sort, stack, distribute, delete, roundSegments, shapeOnPath. Excludes compound (boolean ops) — see note.
**Files:** `packages/nodebox-core/src/ops/filters.ts`, tests.
**Success:** Each filter transforms geometry correctly. All registered in function registry.
**Note on compound (boolean operations):** Boolean operations on bezier paths are computationally complex. The Java version uses `java.awt.geom.Area`. For TypeScript, investigate using a path boolean library (e.g., `paper.js` path operations extracted, or a dedicated library like `polygon-clipping` adapted for bezier paths). If no suitable library exists, implement a simplified version that works for common cases. Document limitations.
**Dependencies:** Phase 5 (generators needed as inputs to filters).

### Phase 7: Text Operations
**Scope:** opentype.js integration, textpath, textOnPath. Bundle Inter.ttf as default font. Use TestPlatform to provide font bytes in tests.
**Files:** `packages/nodebox-core/src/ops/generators.ts` (textpath, textOnPath additions), font integration code, tests.
**Success:** `textpath("hello", "Inter", 24, ...)` produces correct path geometry using bundled Inter font. Text on a curved path follows the curve.
**Dependencies:** Phase 1 (Platform/TestPlatform for font loading), Phase 5 (generators), Phase 1 (geometry path operations for path following).

### Phase 8: Math, List, Color, String Operations
**Scope:** All remaining node functions across math.ts, list.ts, color.ts, string.ts. Includes `radians`, `degrees`, `compare`, `logicOperator`. Math `sin`/`cos` take degrees (not radians). `importText` returns `string[]` (one per line, matching Java's `List<String>` return).
**Files:** `packages/nodebox-core/src/ops/math.ts`, `list.ts`, `color.ts`, `string.ts`, tests.
**Success:** Every function tested. Math functions match Java output. List operations handle empty lists gracefully. String operations handle edge cases.
**Dependencies:** Phase 4 (evaluation engine for integration).

### Phase 9: Data & Network Operations
**Scope:** importCsv, importText, lookup, filterData, makeTable, httpGet, queryJson, encodeUrl. Uses TestPlatform with pre-loaded mock files for testing.
**Files:** `packages/nodebox-core/src/ops/data.ts`, `network.ts`, tests.
**Success:** CSV parsing handles all delimiter/quote configurations. Lookup and filter match Java behavior. TestPlatform provides mock CSV/text files.
**Dependencies:** Phase 1 (Platform/TestPlatform), Phase 4 (evaluation).

### Phase 10: SVG Import & Export
**Scope:** SVG parser (all path commands including arc→curve conversion), SVG exporter. Wire `importSvg` generator into function registry.
**Files:** `packages/nodebox-core/src/svg/*.ts`, tests.
**Success:** Import SVG files, verify paths match. Export geometry to SVG, verify valid SVG output. Arc commands (`A`/`a`) converted to cubic beziers correctly using SVG spec endpoint-to-center parameterization.
**Dependencies:** Phase 1 (geometry), Phase 5 (generators for test geometry).

### Phase 11: Platform Implementations (BrowserPlatform + ElectronPlatform skeleton)
**Scope:** BrowserPlatform (bundled libraries and fonts, no filesystem). ElectronPlatform skeleton (to be completed in Phase 17). Platform interface was already defined in Phase 1; TestPlatform was already created in Phase 1.
**Files:** `packages/nodebox-core/src/browser-platform.ts`, `packages/nodebox-desktop/src/renderer/platform/electron-platform.ts` (skeleton), tests.
**Success:** BrowserPlatform can load bundled library `.ndbx` files and bundled Inter font.
**Dependencies:** Phase 1 (Platform interface).

### Phase 12: Golden Test Infrastructure
**Scope:** Java golden export tool. Golden test runner. Generate golden SVGs for all examples. Compare TypeScript output vs Java output.
**Files:** `tools/golden-export/`, `packages/nodebox-core/tests/golden/`, golden SVG files.
**Success:** All example `.ndbx` files produce SVG output that matches Java within floating-point tolerance. Any mismatches investigated and fixed.
**Dependencies:** Phases 1–11 (full core library).

### Phase 13: Desktop Scaffolding
**Scope:** Electron + React + Zustand + Tailwind + vite. Window creation, basic layout, IPC bridge, menus.
**Files:** All `packages/nodebox-desktop/` scaffolding.
**Success:** `vp dev` launches Electron window with three-pane layout. Menus work. IPC bridge functional.
**Dependencies:** Phase 1-2 (core types imported by desktop).

### Phase 14: Viewer Canvas
**Scope:** Canvas2D renderer, pan/zoom, point visualization, handle system, origin/bounds display.
**Files:** `packages/nodebox-desktop/src/renderer/viewer/`, `ViewerCanvas.tsx`, tests.
**Success:** Renders Path[] and Text[] correctly. Pan/zoom with mouse. Point display toggles. FourPointHandle works for shape manipulation.
**Dependencies:** Phase 13 (desktop shell), Phase 1 (geometry types).

### Phase 15: Network View
**Scope:** Node graph editor canvas. Node rendering with icons (network nodes visually distinguished). Connection drawing (colored by type, straight for short). Drag to move nodes. Drag to connect ports. Selection (click, shift-click, marquee). Node deletion. **Subnetwork navigation**: double-click a network node to enter it; address bar breadcrumbs to navigate back. `currentNetworkPath` in UISlice tracks the current view level. Published port visualization on network nodes.
**Files:** `NetworkCanvas.tsx`, `AddressBar.tsx`, supporting hooks.
**Success:** Can visually create, move, connect, select, and delete nodes. Connection lines colored by port type. Can navigate into and out of subnetworks. Network nodes show published ports.
**Dependencies:** Phase 13 (desktop shell), Phase 2 (node model).

### Phase 16: Parameter Panel
**Scope:** All widget types: DragValue (int/float), angle, string, text, password, toggle, color picker, point editor, menu dropdown, file picker, font picker, seed.
**Files:** `ParameterPanel.tsx`, `DragValue.tsx`, widget components.
**Success:** Select a node → see all its ports as editable widgets. Editing a value updates the library store.
**Dependencies:** Phase 13, Phase 2 (port/widget types).

### Phase 17: File Operations
**Scope:** ElectronPlatform: open file, save, save as, export SVG, export PNG. Recent files. Dirty flag + save confirmation on close.
**Files:** `main/index.ts` IPC handlers, `electron-platform.ts`, file operation integration.
**Success:** Can open any example `.ndbx` file, edit it, save it. Export to SVG/PNG works.
**Dependencies:** Phase 3 (NDBX parser/serializer), Phase 11 (ElectronPlatform), Phase 13.

### Phase 18: Evaluation Loop
**Scope:** Web Worker bridge. Debounced re-evaluation on library changes. Error display (red node backgrounds in network view, error panel). File pre-loading for data/SVG import nodes.
**Files:** `worker/eval-worker.ts`, `worker/eval-bridge.ts`, `render-slice.ts` integration.
**Success:** Change a parameter → viewer updates. Errors shown on failing nodes. Heavy graphs don't freeze the UI.
**Dependencies:** Phase 4 (evaluation engine), Phase 14 (viewer), Phase 13 (desktop).

### Phase 19: Desktop Polish
**Scope:** Undo/redo (50 levels). Animation bar (play/pause/stop/frame). Keyboard shortcuts (Tab for node dialog, Delete, Ctrl+Z/Y, spacebar play). Node selection dialog with category filters. Data viewer for table output. Address bar with rendered child selector. Alt+drag to clone nodes.
**Files:** Various components, `history-slice.ts`, `animation-slice.ts`, `ui-slice.ts`.
**Success:** All E2E tests from electron-app branch pass (adapted for new architecture).
**Dependencies:** Phases 13–18.

### Phase 20: Full Validation
**Scope:** Run complete test suite. Verify all golden tests pass. Run all E2E tests. Load every example file and verify it renders. Fix any remaining issues.
**Files:** No new files — fix existing code.
**Success:** `vp test` green. `vp run test:e2e` green. `vp run test:golden` green. Every example `.ndbx` file loads, evaluates, and renders correctly.
**Dependencies:** All previous phases.

---

## Appendix A: Node Library Reference

Complete list of all nodes to implement, organized by library. Each node's authoritative definition is in the corresponding `.ndbx` file in `libraries/`.

**Source of truth for each node:**
1. Port names, types, defaults, ranges, widgets → `libraries/<lib>/<lib>.ndbx`
2. Implementation behavior → Java source (`src/main/java/nodebox/function/`) and Python source (`libraries/corevector/pyvector.py`)
3. Edge cases → Java unit tests (`src/test/java/`)

## Appendix B: .ndbx Format Reference

The `.ndbx` format is XML. Current format version: 21.

### Root element
```xml
<ndbx formatVersion="21" type="file" uuid="...">
```

### Properties
```xml
<property name="canvasWidth" value="1000"/>
<property name="canvasHeight" value="1000"/>
```

### Function library links
```xml
<link href="java:nodebox.function.CoreVectorFunctions" rel="functions"/>
<link href="python:pyvector.py" rel="functions"/>
```

### Nodes
```xml
<node name="rect1" position="1.00,1.00" prototype="corevector.rect">
    <port name="width" type="float" value="200.0"/>
    <port name="height" type="float" value="150.0"/>
</node>
```

### Connections
```xml
<conn input="colorize1.shape" output="rect1"/>
```

Connection format: `input` is `nodeName.portName`, `output` is just `nodeName` (implicit single output).

### Network nodes
```xml
<node name="root" prototype="core.network" renderedChild="combine1">
    <!-- child nodes -->
    <!-- connections -->
</node>
```

## Appendix C: Excluded Features

These Java features are intentionally NOT ported:

| Feature | Reason |
|---------|--------|
| State ports / stateful nodes | Badly executed, removed from scope. The `state` port type is parsed but ignored at evaluation. |
| Device nodes (OSC, audio, mouse buffer) | Specialist hardware features, can be added later as plugins. The `device` library `.ndbx` is still parsed but device node functions return errors at evaluation. |
| Python/Clojure function evaluation | TypeScript replaces these entirely. `.ndbx` files referencing `python:pyvector.py` or `java:nodebox.function.*` are mapped to TypeScript function names. |
| Expression system | Complex, rarely used. May revisit later. |
| Code library dialog | Not needed — libraries are bundled. |
| Handle system (interactive manipulation) | Port the FourPointHandle only; other handles can follow later. |

## Appendix C.1: Prototype Resolution Algorithm

When the NDBX parser encounters a node with `prototype="corevector.rect"`:

1. Split on `.` → library name `corevector`, node name `rect`
2. Load the library `.ndbx` (via `libraryLoader` callback) if not already loaded
3. Find the prototype node definition in the library
4. **Merge**: The instance inherits all attributes from the prototype that it doesn't override:
   - Ports: prototype ports are the base set. Instance `<port>` elements override by name (match on `port.name`). Ports in the prototype but not in the instance are inherited with their default values.
   - `function`, `outputType`, `outputRange`, `category`, `description`, `image`, `handle`: inherited if not specified on instance
5. **Prototype chaining**: Prototypes can themselves have prototypes (e.g., `corevector.rect` → `corevector.generator` → `core.ROOT`). Resolution walks the chain until reaching a root node.
6. **`<importCoreNode ref="ROOT"/>`**: In `core.ndbx`, this element imports the built-in ROOT node (the base template for all nodes). Similarly `ref="NETWORK"` imports the NETWORK base. These are hardcoded in the parser.

## Appendix C.2: Node Icons

Node `image` attributes (e.g., `image="rect.png"`) reference icon files in the library directory. For the desktop app:
- SVG icons are bundled as static assets in `packages/nodebox-desktop/public/icons/<library>/`
- The electron-app branch already has these SVG icons — port them directly
- In the network view, icons are rendered inside node rectangles
- If an icon is missing, show the node name as text instead

## Appendix C.3: Golden Test Comparison Method

Golden SVG comparison uses structural comparison:
1. Parse both SVGs (expected and actual) as XML DOM
2. Walk the element tree in parallel
3. For each `<path>` element, parse the `d` attribute into path commands
4. Compare path command sequences with floating-point tolerance (epsilon = 0.01)
5. Compare fill/stroke color attributes (exact string match after normalization)
6. Compare transform attributes numerically with tolerance
7. Report first mismatch with element path for debugging

## Appendix D: Key Reference Files

When implementing a node or feature, consult these files:

| What | Where |
|------|-------|
| Node definitions (ports, types, defaults) | `libraries/<lib>/<lib>.ndbx` |
| Java node implementations | `src/main/java/nodebox/function/<Lib>Functions.java` |
| Python node implementations | `libraries/corevector/pyvector.py` |
| Evaluation engine | `src/main/java/nodebox/node/NodeContext.java` |
| Type conversions | `src/main/java/nodebox/node/TypeConversions.java` |
| NDBX parser | `src/main/java/nodebox/node/NodeLibrary.java` (loading section) |
| NDBX serializer | `src/main/java/nodebox/node/NDBXWriter.java` |
| NDBX upgrades | `src/main/java/nodebox/node/NodeLibraryUpgrades.java` |
| Geometry types | `src/main/java/nodebox/graphics/*.java` |
| Rust architecture reference | `crates/nodebox-core/src/` (on `rewrite-in-rust` branch) |
| Electron-app reference | `electron-app/` (on `electron-app` branch) |
| Example files | `examples/` |
