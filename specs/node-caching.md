# Node Caching PRD

## Summary
NodeBox evaluation in `NodeContext` re-computes node outputs on every render, even when the same node is invoked multiple times with identical arguments (especially inside subnetworks). This PRD proposes deterministic caching to improve performance while guaranteeing identical outputs.

The design focuses on **per-render (in-memory) caching first**, then optional **cross-render** caching with stricter invalidation. This keeps the initial implementation safe and bounded.

## Goals
- [ ] Improve render performance without changing any output values.
- [ ] Keep caching fully deterministic and transparent to users.
- [ ] Avoid caching for non-deterministic or side-effectful nodes.
- [ ] Provide a clear path to expand caching (subnetworks, cross-render) later.
- [ ] Add tests that prove cached and non-cached renders produce the same output.

## Non-goals
- [ ] No UI changes in this phase.
- [ ] No new persistent storage in Phase 1 (in-memory only).
- [ ] No reordering of evaluation or concurrency changes.

## Current Evaluation Model (as implemented)
- `NodeBoxDocument.render()` creates a fresh `NodeContext` each render. (`NodeBoxDocument.java`)
- `NodeContext.renderNode(nodePath, argumentMap)`:
  - If node is a network: evaluate its rendered child via `renderChild`.
  - Else invoke the node function directly. (`NodeContext.java`)
- `renderChild(networkPath, child)`:
  - Evaluates each input port, builds argument maps, and calls `renderNode` multiple times.
  - Uses `nodeArgumentsResults` cache only for `(networkPath, childName, networkArgumentMap)` (published port inputs), not per iteration.
- `renderResults` is used only for `Port.TYPE_STATE` (previous render outputs).

## Proposed Caching Scope
### Phase 1 (per-render, safe default)
- [ ] Cache node invocations **inside the same `NodeContext`** when the same node is invoked with the same resolved arguments.
- [ ] Cache subnetwork iteration invocations to avoid duplicate work when `buildArgumentMaps` produces the same argument map multiple times.
- [ ] Cache connection lookups and port conversion/clamping results, since those are deterministic and pure.

### Phase 2 (cross-render, optional)
- [ ] Add a persistent or multi-render cache keyed by graph versions + data + state.
- [ ] Introduce bounded eviction and explicit invalidation.

## Cacheability Rules (must preserve exact outputs)
Caching may only happen when outputs are deterministic. Non-deterministic functions *must* opt out.

- [ ] Introduce a cacheability flag for functions (default should be **safe**).
  - Option A (safer default): `Function` exposes `isCacheable()` and defaults to `false`, with allowlist in core libraries.
  - Option B (easier adoption): `Function` defaults to `true`, with an explicit opt-out list for known side-effect libs (e.g., `device`, `side-effects`, random/time without a seed).
  - Prefer **Option A** for correctness; performance can be restored by marking pure libraries as cacheable.
- [ ] Special-case nodes using `Port.TYPE_STATE`: still cacheable inside a render because the resolved argument includes the previous render state (which is fixed for that render).
- [ ] Nodes that take `Port.TYPE_CONTEXT` are cacheable only if the resolved context data fingerprint is part of the cache key (Phase 2).

## Cache Key Design
All cache keys must avoid false positives. A false positive would reuse an output for a different input and change results. False negatives are acceptable (they only miss a performance opportunity).

### Key: NodeInvocationKey (per-render)
Used for caching a node function invocation.

**Fields**
- `nodePath` (string, absolute path, e.g. `/foo/bar`)
- `functionId` (string, e.g. `math/add`) to guard against function swaps
- `argumentFingerprint` (ordered list of value keys)

**Argument ordering**
Use the node’s input port order (`node.getInputs()`) to build the argument list. This matches the existing call order in `invokeNode`.

**ArgumentFingerprint**
For each argument:
- Use a **ValueKey** (see below) that is safe and deterministic.

### Key: ChildInvocationKey (subnetwork iteration)
Used in `renderChild` to cache each individual invocation inside subnetworks.

**Fields**
- `childPath` (absolute path)
- `argumentFingerprint` (same algorithm as above)

This is intentionally independent of `networkArgumentMap`. The `argumentFingerprint` already encodes the resolved input values after published-port overrides.

### Key: PortConversionKey (optional, per-render)
For `convertResultsForPort` + `clampResultsForPort`:
- `portIdentity` (use object identity or stable port hash)
- `rawValuesFingerprint` (list of ValueKeys)
- `portConfigFingerprint` (port type, range, min/max, widget)

### Key: ConnectionLookupKey (per-render)
Build a per-network map once:
- `(networkPath, inputNodeName, inputPortName) -> outputNodeName`

This is a pure lookup table and does not depend on runtime values.

## ValueKey (Safe Hashing Strategy)
The ValueKey is a structural fingerprint of a value that must never claim two different values are equal.

**Rule:**
Only use `.equals()`/`.hashCode()` if the class is known to implement value semantics correctly. Otherwise, use **object identity**.

**Recommended implementation**
- Scalars: `String`, `Long`, `Integer`, `Double`, `Float`, `Boolean` -> value hash
- NodeBox primitives with value semantics (verify): `Point`, `Color`, `Rect`, `Size`, `Transform`, `Path` (if equals is value-based)
- Lists: build a list of ValueKeys in order
- Arrays: wrap as list + ValueKey for each element
- Maps/Sets: only if order-independent and equality is stable; otherwise fall back to identity
- Fallback: `identityHashCode` + object identity check

**Pseudo-code**
```
ValueKey makeValueKey(Object v):
  if v == null: return NullKey
  if v is primitive wrapper or String: return ValueKey(type=v.class, hash=v.hashCode)
  if v is known-immutable value class: return ValueKey(type=v.class, hash=v.hashCode)
  if v is List: return ListKey(map(makeValueKey, v))
  if v is Array: return ListKey(map(makeValueKey, asList(v)))
  else: return IdentityKey(System.identityHashCode(v), v)
```

This approach avoids incorrect cache hits at the cost of fewer cache hits for complex or mutable objects. It preserves correctness.

## Data Structures (Phase 1)
- [ ] `Map<NodeInvocationKey, List<?>> nodeInvocationCache`
- [ ] `Map<ChildInvocationKey, List<?>> childInvocationCache`
- [ ] `Map<PortConversionKey, List<?>> portConversionCache`
- [ ] `Map<ConnectionLookupKey, Node>` connectionLookupCache

All caches live inside `NodeContext` and are cleared when the render ends.

## Invalidation (Phase 1)
No invalidation needed beyond per-render lifecycle, because:
- Every render creates a new `NodeContext`.
- Caches are in-memory and scoped to that render.

## Invalidation (Phase 2, cross-render)
When/if cross-render caching is introduced:
- [ ] Key must include a **GraphFingerprint**:
  - `NodeLibrary.uuid`
  - Root `Node` hash (or a stable structural hash of all nodes + connections)
- [ ] Key must include **ContextFingerprint**:
  - `data` map entries (frame, mouse position, device data)
  - Port overrides (keyed by `nodeName.portName`)
  - Previous render results if `Port.TYPE_STATE` nodes are used
- [ ] Invalidate cache on:
  - Library reload
  - Node graph edits (any node, port, connection change)
  - Function repository change

## Execution Flow Changes (Phase 1)
- [ ] In `renderNode`:
  - After building the resolved argument list, compute `NodeInvocationKey`.
  - If cache hit: return cached `List<?>`.
  - Else: invoke and cache.
- [ ] In `renderChild`:
  - For each `argumentMap` generated by `buildArgumentMaps`, compute `ChildInvocationKey`.
  - If cache hit: append cached results to `resultsList`.
  - Else: call `renderNode`, cache, append.
- [ ] In `evaluatePort`:
  - Use precomputed connection lookup map instead of scanning connections.
- [ ] In `convertResultsForPort` + `clampResultsForPort`:
  - Cache by `PortConversionKey` to avoid repeated work.

## Behavior Guarantees
- [ ] Outputs are identical to current behavior for all deterministic graphs.
- [ ] Non-deterministic nodes must never be cached unless explicitly declared safe.
- [ ] Caching changes only performance, not evaluation order (Phase 1).

## Testing Strategy
### Automated (JUnit)
- [ ] Add a `NodeContext` toggle for caching (on/off) so tests can compare outputs.
- [ ] New tests in `src/test/java/nodebox/node/NodeContextTest.java`:
  - Deterministic graph renders identical outputs with caching ON vs OFF.
  - Subnetwork with repeated argument maps produces identical output and fewer function invocations (use a counter function).
  - Side-effect function is never cached (expected counter increments per invocation).
- [ ] Run `ant test`.

### Manual
- [ ] Load a graph with subnetworks that repeat input values; confirm rendered output unchanged across edits and renders.
- [ ] Test a graph with device input (OSC/audio) to ensure outputs still update per frame (no stale values).

## Open Questions / Follow-ups
- [ ] Which libraries/functions are explicitly cacheable? (Need a list.)
- [ ] How should cacheability be expressed: `Function` interface change vs metadata registry?
- [ ] Do any NodeBox graphics classes lack reliable value-based equality?
- [ ] Should `Port.TYPE_CONTEXT` nodes be considered cacheable in Phase 1, or always opt out?

## Implementation Checklist
- [ ] Add cacheability metadata for functions (safe default).
- [ ] Implement `ValueKey` + `ArgumentFingerprint` utility.
- [ ] Add `NodeInvocationKey` + cache map in `NodeContext`.
- [ ] Add `ChildInvocationKey` cache for subnetwork iterations.
- [ ] Add connection lookup cache for `evaluatePort`.
- [ ] Add conversion/clamping cache for port results.
- [ ] Add tests + toggle for caching.
- [ ] Validate `ant test` passes.
