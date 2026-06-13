# Desktop Integration — Session Handoff

_Last updated: 2026-06-13_

This is the working state of the "make NodeBox a proper native desktop app" effort.
The full strategy and rationale live in **`docs/desktop-integration-plan.html`** (open in a
browser) — this file is the operational handoff: what's done, what's next, and the
non-obvious things that will bite you.

## TL;DR

We chose the **"egui shell + thin platform adapters"** strategy (Option C in the plan):
keep eframe/egui as the shell, add small per-OS adapters for the lifecycle things winit
doesn't surface. **Phase 1 (quick wins) and Phase 2 (multi-document) are done and verified
live on macOS.** The macOS open-file (Finder double-click) adapter is also done. Remaining
work is Windows/Linux delivery, a macOS Window menu, and polish.

## Branch & commits

- **Branch:** `desktop-integration-phase1` (off `rewrite-in-rust`)
- **Not yet pushed.** 5 commits ahead of `rewrite-in-rust`:

```
20a50779 Support multiple open documents, one OS window each.   (Phase 2)
b2aa7675 Fix README launch instructions after crate consolidation.
751a49d2 Deliver macOS open-file events to the app.             (Phase 3, macOS only)
45bf5aad Add unsaved-changes guard, window title sync, file drag-and-drop, and window persistence.  (Phase 1)
fc380b05 Add desktop integration plan and ignore Electron experiment leftovers.
```

PRs target `rewrite-in-rust`, **never `master`** (see AGENTS.md). Not yet PR'd — the user
was asked and we paused before pushing.

## What's done (verified)

### Phase 1 — quick wins (`45bf5aad`)
- **Unsaved-changes guard**: closing a window, quitting, New, Open, or dropping a file
  prompts Save / Don't Save / Cancel when dirty (`rfd::MessageDialog`). macOS ⌘Q was
  rerouted off muda's `PredefinedMenuItem::quit` (which calls `NSApp terminate:` and
  bypasses winit) to a custom item → app close path, so the guard applies.
- **Window title**: `document.ndbx — NodeBox` with a `•` dirty indicator, pushed via
  `ViewportCommand::Title` only on change.
- **Drag-and-drop** of `.ndbx` onto a window opens it.
- **Window geometry persistence** via eframe's `persistence` feature.
- **Bug fixed along the way**: loading a document marked it dirty + created a bogus undo
  entry (the change-detection hash baseline wasn't resynced). Fixed with
  `Document::reset_document_baseline()`; regression test
  `test_loading_document_does_not_mark_dirty`.
- Cleanup: removed stale `native_menu.rs` doc comment + blanket `#![allow(dead_code)]`;
  gitignored Electron leftovers (`packages/`, `node_modules/`, `pnpm-lock.yaml`, `.vite/`,
  `.playwright-mcp/`).

### Phase 3 (macOS slice) — open-file events (`751a49d2`)
- `crates/nodebox-desktop/src/platform_integration/{mod.rs,macos.rs}`.
- Finder double-click / dock drop / `open file.ndbx` arrive as `application:openURLs:`
  Apple events, **not** argv. winit's delegate doesn't implement that selector, so the
  event is dropped. We inject the method into winit's delegate **class** at runtime via
  `class_addMethod`, forwarding paths through an `mpsc` channel the app drains each frame.
- **Critical timing:** registration happens from an
  `NSApplicationWillFinishLaunchingNotification` observer set up in `init()` **before** the
  event loop starts (`lib.rs::run`). Registering later (e.g. in eframe's creation context)
  loses the launch-time open event — AppKit may deliver it before your code runs. Both
  cold-launch and while-running opens verified working.
- `objc2`/`block2`/`objc2-app-kit`/`objc2-foundation` pinned to 0.5/0.2 to match winit+muda;
  no new duplicate crates in the tree.

### Phase 2 — multi-document (`20a50779`)
The load-bearing refactor. **The move was counterintuitive:** the old `NodeBoxApp` struct
already *was* one window's worth of state, so we renamed `app.rs` → `document.rs` wholesale
and the struct became `Document` (~2,100 lines, mostly untouched). A new thin
`NodeBoxApp` shell (~300 lines in a fresh `app.rs`) owns the document collection and
app-level state.

Architecture:
- `NodeBoxApp { documents: Vec<Document>, native_menu, recent_files, open_file_rx, ... }`.
- **First document → root viewport; others → egui _immediate_ child viewports** (real OS
  windows). Immediate (not deferred) viewports mean synchronous render inside the shell's
  `update`, so **no `Arc<Mutex>` / no `Send+Sync` plumbing**. Each `Document` has its own
  `render_worker`, so windows stay independent and responsive.
- Documents can't touch app state, so they push **`AppRequest`** (NewDocument,
  OpenDocument, AddRecentFile, ClearRecentFiles, CloseDocument, Quit) into a per-document
  `requests: Vec<AppRequest>` that the shell drains every frame.
- `DocumentEnv { is_root, other_documents_exist, recent_files }` is passed into
  `Document::show(ctx, env, frame)` each frame.
- **Open semantics** (in `NodeBoxApp::open_document`): reuse a *pristine* (untitled,
  unmodified, no undo) window; never open an already-open file twice; otherwise new window.
- Menu actions route to the last-focused document (`focused_document` index, sticky).
- **Quit** prompts per dirty document (`confirm_quit`).
- CLI now opens one window per `.ndbx` arg (`new_with_port` takes `Vec<PathBuf>`).

## How to build / run / verify

```bash
# Run from source (one or more files optional)
cargo run -- "examples/01 Basics/06 Lists/Compare/Compare.ndbx"

# Proper macOS .app bundle (needed to test Finder file associations / open events)
./scripts/build-mac-bundle.sh --debug
open "$(pwd)/target/debug/NodeBox.app" "examples/.../Compare.ndbx"

# Tests + warnings gate (AGENTS.md requires zero warnings)
cargo check --workspace --exclude nodebox-python   # must be 0 warnings
cargo test -p nodebox-desktop                      # 196 tests, all passing
```

### ⚠️ macOS window-verification gotcha
System Events / AppleScript window queries **and `screencapture`** only see the **active
macOS Space**. Mid-session it will look like all NodeBox windows vanished — they're on
another Space. To enumerate windows reliably across Spaces, use a CGWindowList script:

```python
# uv run with: # /// script
#              # dependencies = ["pyobjc-framework-Quartz"]
#              # ///
import Quartz
wins = Quartz.CGWindowListCopyWindowInfo(Quartz.kCGWindowListOptionAll, Quartz.kCGNullWindowID)
for w in wins:
    if "NodeBox" in str(w.get("kCGWindowOwnerName", "")):
        print(w.get("kCGWindowOwnerPID"), "|", w.get("kCGWindowName", ""))
```

## Next steps (rough priority order)

1. **Push branch + open PR** against `rewrite-in-rust` (was about to do this; user paused).
2. **macOS Window menu** listing open documents (switch/focus). Native menu is app-global;
   needs to enumerate `documents` and focus a viewport.
3. **Root-window close UX**: closing the root window promotes the next document into the
   root viewport, but it doesn't reposition — visible geometry jump. Decide whether to copy
   geometry or keep child windows as child windows even when doc 0 closes.
4. **Per-window geometry persistence**: child (immediate-viewport) windows don't persist
   size/position; only the root does (via eframe persistence).
5. **Windows delivery**: single-instance mutex + forward argv from 2nd invocation to the
   running process (named pipe) → same `open_file_rx` channel. Argv already opens windows,
   so this is just the single-instance forwarding.
6. **Linux delivery**: `.desktop` file + shared-mime-info XML for `.ndbx` in the AppImage/deb
   packaging scripts (`scripts/build-linux-*.sh`). Argv path already works.
7. **Phase 4 polish**: autosave/crash recovery, macOS proxy icon + `setDocumentEdited:`
   close-button dot, auto-update.

## Key files

| File | Role |
|------|------|
| `crates/nodebox-desktop/src/app.rs` | **NEW** thin `NodeBoxApp` shell: `Vec<Document>`, viewports, request draining, quit. ~300 lines. |
| `crates/nodebox-desktop/src/document.rs` | **WAS `app.rs`.** `Document` = one window: state, panels, history, render worker, `AppRequest` emission, `show()`. ~2,100 lines. |
| `crates/nodebox-desktop/src/platform_integration/mod.rs` | `init()` (call before event loop) + `take_open_file_receiver()`. |
| `crates/nodebox-desktop/src/platform_integration/macos.rs` | `application:openURLs:` injection via `class_addMethod` + will-finish-launching observer. |
| `crates/nodebox-desktop/src/native_menu.rs` | macOS NSMenu via `muda`. Custom Quit item routes through app close path. |
| `crates/nodebox-desktop/src/lib.rs` | `run()`: `platform_integration::init()`, parse argv `.ndbx` paths, launch. |
| `docs/desktop-integration-plan.html` | Full strategy doc (the "why"). |

## Conventions / gotchas
- **Never modify Java** (`src/main/java`) — legacy, read-only reference only.
- **Zero compiler warnings** before handoff: `cargo check --workspace --exclude nodebox-python`.
- `nodebox-python` excluded from default builds (pyo3 dev-header dependency).
- GUI work: consult `STYLE_GUIDE.md` (Linear-inspired: sharp corners, no borders, violet accent).
