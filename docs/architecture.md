# Architecture Overview

This document gives a high-level view of NodeBox and where core logic lives. It is intentionally brief; expand as systems change.

## Core Areas

- `src/main/java/nodebox/client`: Application shell and UI (`Application`, `NodeBoxDocument`, viewer panes, menus).
- `src/main/java/nodebox/node`: Node graph model, ports, connections, libraries, and upgrade logic.
- `src/main/java/nodebox/function`: Function repositories and language bindings.
- `src/main/python`: Bundled Python nodes and helpers used by the node libraries.
- `libraries/`: Built-in node libraries in `.ndbx` format.
- `examples/`: Sample documents used for demos and tests.

## Rendering Flow (simplified)

1) UI edits mutate the `NodeLibrary` via `NodeLibraryController`.
2) The active `NodeBoxDocument` calls `requestRender()`.
3) Rendering produces results for the viewer and export flows.

## Testing

- Unit tests: `src/test/java` (JUnit).
- E2E UI tests: `src/test/java/nodebox/e2e` (Robot-driven).
