# NodeBox TypeScript Port

Monorepo: `packages/nodebox-core` (pure TS library) + `packages/nodebox-desktop` (Electron+React app).

## Core (`packages/nodebox-core`)
- `src/geometry/` — Point, Path, Color, Rect, Transform
- `src/node/` — Node, Port, Connection, Value, NodeLibrary
- `src/eval/` — Evaluation engine, function registry, type conversions
- `src/ops/` — All node operations (generators, filters, math, list, string, color, data)
- `src/ndbx/` — .ndbx XML parser/serializer, upgrade chain v1→v21
- `src/svg/` — SVG import (with arc→curve) and export
- `src/platform.ts` — Platform interface + TestPlatform
- Tests: `tests/` — 364 tests, run with `pnpm test`

## Desktop (`packages/nodebox-desktop`)
- `src/renderer/state/` — Zustand+immer store (6 slices)
- `src/renderer/components/` — React components (AppLayout, ViewerCanvas, NetworkCanvas, ParameterPanel, etc.)
- `src/renderer/node-templates.ts` — Node catalog with ports/defaults for the selection dialog
- `src/renderer/hooks/` — usePanZoom, useCanvasRenderer, useKeyboardShortcuts
- `src/renderer/theme/tokens.ts` — Dark theme design tokens (Zinc+Violet)
- `src/main/` — Electron main process + menu
- Dev: `cd packages/nodebox-desktop && npx vite` → http://localhost:5173

## Key decisions
- All state is plain interfaces (no classes) — survives structuredClone/postMessage
- Store uses immer: mutate drafts in set(), snapshot via get() for undo
- Spec: `docs/specs/2026-03-22-port-to-typescript.md`
- Java source (reference): `src/main/java/nodebox/`, `libraries/*.ndbx`
