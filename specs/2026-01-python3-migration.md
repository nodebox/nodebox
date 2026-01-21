# Python 3 Migration Strategy (Parking Doc)

## Summary
NodeBox currently embeds Jython 2.7.2. A Python 3-capable Jython release does **not** exist today; the project has ongoing work but no released Jython 3. The two viable paths are:

1. **GraalPy (preferred JVM-native path)**
   - Python 3 runtime for the JVM with Java interop.
   - Provides compatibility modes for Jython users.
   - Requires refactoring away from `org.python.*` APIs to a runtime abstraction.

2. **CPython out-of-process**
   - Full CPython compatibility (native extensions OK).
   - Requires IPC boundary, per‑platform packaging, and more complex deployment.

**Decision for this sprint:** keep Jython 2.7.2 and defer Python 3 migration.

## Constraints & Risks
- Large existing user base and libraries depend on Python 2 semantics.
- NodeBox uses Jython-specific APIs directly (e.g., `PythonInterpreter`, `PyObject`), so runtime swap is non-trivial.
- Packaging changes would be required if a new runtime is introduced.

## User Migration UX (Future Work)
- Add a per-project/runtime selector (Python 2 vs Python 3) with a clear banner when Python 2 is active.
- Provide a migration assistant that:
  - Scans user code and libraries for Python 2-only constructs.
  - Runs an automatic conversion pass (e.g., `2to3`) with a preview diff.
  - Generates a compatibility report highlighting manual fixes (I/O, unicode, dict iteration, exceptions).
- Surface migration status in the UI (e.g., “Auto-migrated: X files; Manual fixes: Y files”).
- Allow rollback to Python 2 for a project if migration fails.
- Add in-app links to migration docs and a “report incompatibility” action.

## Proposed Phases (Future Work)
### Phase 0 — Requirements & Scope
- Decide whether native CPython packages are required.
- Identify Python surfaces: console, node libraries, examples, and user scripts.

### Phase 1 — Runtime Abstraction
- Introduce a `PythonRuntime` interface; create `JythonRuntime` as baseline.
- Replace direct `org.python.*` usage with the abstraction in the call sites.

### Phase 2 — Python 3 Runtime Choice
- Prototype **GraalPy** embedding (preferred if JVM interop is key).
- Prototype **CPython via subprocess** if native extensions are a requirement.

### Phase 3 — Compatibility & Migration
- Build a migration test harness to execute existing libraries/examples.
- Add per-project runtime selection (Jython vs Python 3).
- Provide conversion guidance and tooling (`2to3`, compatibility report).

### Phase 4 — Transition
- Default new projects to Python 3.
- Deprecate Python 2 runtime after sufficient compatibility coverage.

## Open Questions
- Do we need native extension support (NumPy, etc.) inside NodeBox?
- Can we accept differences in Java interop behavior under GraalPy?
- What is the rollout timeline and deprecation policy for Python 2?
