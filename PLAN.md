# Plan: Optimize NodeLibrary Cloning with Arc + Copy-on-Write

## Problem

Every time a render is dispatched (`app.rs:419`), the entire `NodeLibrary` is deep-cloned
via `self.state.library.clone()`. This recursively copies the full node tree (all `Node`
children, connections, ports, values, strings, etc.). The cloned library is sent to the
render worker thread, where it is **only read, never mutated**. Additionally, the undo/redo
system (`history.rs`) deep-clones the library on every `save_state` call.

## Solution

Wrap `NodeLibrary` in `Arc<NodeLibrary>` and use copy-on-write (COW) semantics via
`Arc::make_mut()`. This makes:

- **Render dispatch**: O(1) — just increment a reference count instead of deep-cloning
- **Undo/redo snapshots**: O(1) — `Arc::clone()` instead of deep clone
- **Mutations**: Only clone when a render is actively holding a reference (COW); otherwise
  mutate in-place with no overhead

### How `Arc::make_mut` works

- If `Arc` refcount == 1 (no one else holds a reference): returns `&mut T` directly — zero cost
- If `Arc` refcount > 1 (render worker still using it): clones the inner `T` first, then
  returns `&mut T` to the new unique copy

This means mutations are free in the common case (render already finished), and only pay
the clone cost when a render is actually in progress — which would have been necessary anyway.

## Changes

### Step 1: `state.rs` — Change library field type

- Change `pub library: NodeLibrary` → `pub library: Arc<NodeLibrary>`
- Add `use std::sync::Arc;`

### Step 2: `history.rs` — Arc-ify the undo/redo stacks

- Change `undo_stack: Vec<NodeLibrary>` → `Vec<Arc<NodeLibrary>>`
- Change `redo_stack: Vec<NodeLibrary>` → `Vec<Arc<NodeLibrary>>`
- Change `last_saved_state: Option<NodeLibrary>` → `Option<Arc<NodeLibrary>>`
- `save_state(&mut self, library: &Arc<NodeLibrary>)` — use `Arc::clone(library)` instead
  of `library.clone()` (which would deep-clone due to Deref)
- `undo()` and `redo()` return `Option<Arc<NodeLibrary>>` instead of `Option<NodeLibrary>`

### Step 3: `render_worker.rs` — Accept Arc in render requests

- Change `RenderRequest::Evaluate { library: NodeLibrary }` → `library: Arc<NodeLibrary>`
- Update `request_render()` signature: `library: Arc<NodeLibrary>`
- Update `render_worker_loop()` and `drain_to_latest()` to use `Arc<NodeLibrary>`
- At evaluation call site: pass `&final_library` (Arc auto-derefs to `&NodeLibrary`)

### Step 4: `app.rs` — Use Arc::clone for render dispatch, Arc::make_mut for mutations

- Render dispatch (`line ~420`): `Arc::clone(&self.state.library)` instead of
  `self.state.library.clone()`
- New file / load file: `self.state.library = Arc::new(NodeLibrary::new(...))`
- Undo/redo handling: assign `Arc<NodeLibrary>` directly
- Node creation (`line ~784`): `Arc::make_mut(&mut self.state.library).root.children.push(...)`
- `handle_four_point_change`: `Arc::make_mut(&mut self.state.library).root.child_mut(...)`
- `handle_parameter_change`: `Arc::make_mut(&mut self.state.library).root.child_mut(...)`
- History `save_state` calls: pass `&self.state.library` (already Arc)

### Step 5: `network_view.rs` — Accept &mut Arc<NodeLibrary>

- Change `show()` signature from `library: &mut NodeLibrary` → `library: &mut Arc<NodeLibrary>`
- Read-only access (iterating children, reading connections): works via `Deref` — no changes
- Mutation sites (node dragging, connection add/remove, rendered_child, delete):
  use `Arc::make_mut(library)` before mutating

### Step 6: `panels.rs` — Mutate through Arc::make_mut

- Where `state.library.root.child_mut(...)` is used: replace with
  `Arc::make_mut(&mut state.library).root.child_mut(...)`
- Where `state.library.set_width/set_height` is used: replace with
  `Arc::make_mut(&mut state.library).set_width(...)`
- Read-only access (iterating children for display): works via Deref — no changes

### Step 7: `node_library.rs` — Accept &mut Arc<NodeLibrary> for node creation

- Where `library.root.children.push(node)` is used: replace with
  `Arc::make_mut(library).root.children.push(node)`

### Step 8: Fix tests

- Update test code in `app.rs` that directly mutates `app.state.library` to use
  `Arc::make_mut` or `Arc::new()`

### Step 9: Build and verify

- Run `cargo build` and fix any remaining compilation errors
- Run `cargo test` and verify all tests pass
