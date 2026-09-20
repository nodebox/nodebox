# NodeBox: one core, two shells

This branch joins NodeBox 3 (the Java desktop application) and NodeBox Live (the web application) on
one TypeScript engine, `@ndbx/core`, and gives that engine two shells: React in an Electron window on
the desktop, React served by a Hono application on Cloudflare on the web. Both file formats keep
working: `.ndbx` documents from NodeBox 3 (format versions 1 to 22) and `project.json` documents from
NodeBox Live (format versions 1 to 4).

## Layout

```
packages/
├── g/         @ndbx/g        graphics primitives and SVG rendering (from NodeBox Live)
├── core/      @ndbx/core     document model, .ndbx and Live formats, evaluator, built-in libraries
├── runtime/   @ndbx/runtime  NodeBox Live's engine and player, plus the switch to the core engine
├── web/       @ndbx/web      the React editor (Vite, Tailwind, preact signals)
├── server/    @ndbx/server   the Hono application: API, auth, assets, guide; Cloudflare Worker + R2
└── desktop/   @ndbx/desktop  Electron shell: the same Hono app on localhost, projects on disk
src/, libraries/, examples/   NodeBox 3 (Java), untouched; libraries/*.ndbx are the core's built-ins
```

Dependency flow: `g` → `core` → `runtime` → `web`; `server` and `desktop` serve `web`, `desktop` also
uses `core` and `server` directly.

## The core

`@ndbx/core` is a flexible library with no UI and no I/O of its own: hosts install a text file reader,
a DOM parser (for SVG import), a font provider (for text outlines) and an OSC sender when they have
them. Everything else is pure TypeScript.

### Document model

The model is NodeBox 3's: a library holds a root network; nodes extend prototypes ("corevector.rect",
"core.network" or a sibling in the same file) and are flattened at load time; ports have a type, a
widget, a **range** (`value` or `list`), literal values, min/max and menu items; published ports
forward to a child port; a network renders its rendered child. Three things were added so NodeBox
Live projects map onto it without loss: named outputs (a Live node can have several), expressions on
ports (`{ type: "EXPRESSION" }` values), and stickies. Format-specific extras (Live ids, canvas
settings, sections) travel in `node.meta` and `library.meta`, so each format writes back what it read.

### Formats

- `ndbx/`: an XML parser and serializer, the reader (a port of `NodeLibrary.parseNDBX`, prototype
  flattening included), the writer (a port of `NDBXWriter`: only what differs from the prototype is
  written), and all 21 upgrade steps from `NodeLibraryUpgrades` (1.0→2 … 21→22).
- `live/`: the reader and writer for `project.json`, the Live format upgrades (1→4), the static
  analysis that derives a function item's parameters and ports from its source (a port of Live's lexer
  and `parseNodeStatements`), the module loader (imports of `@ndbx/g` and `project:Name` rewritten to
  importable URLs) and the bridge that runs Live function items as node functions.
- `live/native.ts`: NodeBox 3's built-in nodes exposed as Live dependency projects (`nodebox/corevector`,
  `nodebox/math`, …) whose items are *native*: their parameters and ports are spelled out and their
  behaviour lives in the core. `libraryToLiveProject` converts a `.ndbx` document into a project the
  Live editor can display, hoisting subnetworks into network items with inlets and outlets.

### Evaluator

`runtime/context.ts` is a port of `NodeContext`, NodeBox 3's list-matching evaluator, made
asynchronous so that Live nodes (which fetch, load assets and import modules) run through it:

- a node is invoked once per element of its longest value-range input; shorter value-range inputs
  cycle; list-range inputs receive the whole list; an empty value-range input means no invocations;
  results are concatenated, and the output range decides whether a returned list flattens or wraps;
- values are converted to the port type after evaluation (`TypeConversions`, including geometry to
  points and Live shapes to geometry) and clamped to min/max;
- values entering a network through published ports override the referenced child ports;
- functions come from a `FunctionRepository` of libraries: the built-in TypeScript libraries, the
  Live bridge (`live/<user>/<project>/<Item>`), and whatever a host adds (a Python runner, for
  instance). Unknown functions fail at render time with the node path, not at load time, so a
  document that links a Python module still loads and renders everything that does not need it.

The Live model is a special case of this: Live's table ports are list-range ports, its shape ports
value-range ports, its parameters value-range ports with widgets. Subnetworks with inlets and outlets,
which Live declared but never implemented, work because they are ordinary networks here.

### Built-in libraries

`functions/` ports every function class of NodeBox 3 to TypeScript: `math`, `list`, `string`,
`color`, `data`, `network`, `device`, `core` and `corevector` (both the Java half and `pyvector.py`).
Seeded nodes use a bit-exact `java.util.Random`, so `random_numbers`, `shuffle` and `pick` produce the
sequences NodeBox 3 users know (`scatter` and `wiggle` came from Jython's Mersenne Twister and are
random in a different way). The nine node libraries under `libraries/*.ndbx` are embedded at build
time; they stay the single source of truth for the built-in nodes.

`graphics/` is NodeBox 3's geometry model (Point with a type, Contour, Path, Geometry, Rect, Color,
Transform, Text) with the same resampling, length and point-at-t arithmetic, boolean operations on
flattened outlines, and conversion to and from `@ndbx/g` shapes for rendering and for Live nodes.

## The shells

- **Web**: `packages/server` is unchanged in kind: a Hono application on a Cloudflare Worker with an
  R2 bucket, serving the editor build and the API. The editor picks the core engine automatically for
  projects that need it (native NodeBox 3 items or subnetwork calls) and can be forced either way
  (`localStorage.engine = "core" | "live" | "auto"`).
- **Desktop**: `packages/desktop` runs the *same* Hono application on localhost inside Electron, with
  an R2-compatible file bucket under the user's application data folder and a single local user. The
  File menu opens `.ndbx` and `project.json` documents (`.ndbx` through `@ndbx/core`, converted to a
  project with native items) and saves projects back as either format. Packaging uses electron-builder
  and registers the `.ndbx` file association.

## Compatibility

| Source | What | Result |
| --- | --- | --- |
| NodeBox 3 examples (`examples/`, 48 documents) | load, upgrade, render headlessly | all 48 load; 47 render (the Twitter example needs the network) |
| NodeBox 3 upgrade fixtures (`src/test/files/upgrade-v1…v21.ndbx`) | upgrade to format 22 and load | all pass; 0.9 and 999 are refused with the same messages as the Java app |
| NodeBox 3 built-in libraries (`libraries/*.ndbx`, 154 nodes) | load as prototypes | all resolve; every function is provided |
| NodeBox Live core library (`packages/runtime/functions/g`) | ports derived from source, nodes run through the bridge | passes (`test/live.test.ts`) |
| NodeBox Live example projects | load, evaluate, write back | the welcome project round-trips and renders identically |
| Cartan Node Library 3.7 (274 MB, 915,470 nodes, 205 nodes + demos) | load; render every demo | loads in 35 s, 0 unresolved prototypes; 163 of 205 demos render with the built-ins alone (see below) |

### John Cartan's node library

The library is a single `.ndbx` (format 21) plus 18 Python and 2 Clojure modules. It uses 127
distinct built-in prototypes, all of which resolve. Of its 205 nodes, 181 are pure subnetworks and
render unchanged. The remaining 24 call functions from his modules (`voronoi/voronoi`,
`treemap/squarify`, `noise/noise`, …); those nodes fail at render time with a clear message naming the
missing function library and render as soon as a host provides a Python/Clojure function library
(the `FunctionLibrary` interface is small: namespace, `getFunction`, `getFunctionNames`). The modules
are Jython 2 code with Java imports, so running them means either a Jython-compatible runner or a
port; `nodelist.py` and `canvas.py` also reach into the NodeBox 3 application object model and would
need a small host API. Two demos import SVG files and need a DOM parser installed.

Run the check yourself:

```bash
cd packages/core
node --max-old-space-size=12000 --import tsx scripts/ndbx-check.mts "node library 3-7.ndbx"
node --max-old-space-size=12000 --import tsx scripts/ndbx-render-children.mts "node library 3-7.ndbx"
```

## What is not there yet

- The editor still draws NodeBox 3 documents with Live's conventions (pixel positions, inlets and
  outlets instead of published ports on the network node). Handles, the NodeBox 3 viewer overlays and
  the animation timeline are not ported.
- Python and Clojure function libraries need a host runner; the interface exists, no runner ships.
- Devices (OSC, audio) read from the render data; the desktop shell does not yet feed them.
- Text outlines need a font provider; the desktop shell should register the system fonts through
  `installOpenTypeFonts`, the web shell a set of bundled fonts. Without one, text renders as text.
