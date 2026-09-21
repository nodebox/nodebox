# Classic NodeBox Live

The first NodeBox Live (the `nodeboxdead` branch of `nodebox/nodeboxlive`) evaluated graphs the way
NodeBox always had: **the runtime does the list matching, and a node is a plain function**. A node
declares its parameters; if one of them receives a list, the runtime calls the function once per
element and collects the results. Nothing inside the function knows about lists.

The NodeBox Live that followed moved that job into the nodes: a node receives a table and loops over
it itself, and subnetwork inlets were never finished (`evaluateNetwork` still throws
`Unimplemented: Inlet value`). That is the behaviour difference between the two, and it is why the
demos and tutorials that ship in `packages/server/data` stopped working.

The core evaluator inherited NodeBox 3's list matching, which is the same rule, so classic projects
run on it unchanged. `@ndbx/core` reads them directly.

## Running one

```ts
import { openClassicProject, NodeContext } from "@ndbx/core";
import g from "g.js";

const document = openClassicProject("demo/flocking", project, {
  dependencies,                    // other classic projects; core/g comes from the core itself
  assets,                          // by file name: text as a string, binary as an ArrayBuffer
  namespaces: { g },               // where the library functions live
  globals: { _: lodash, opentype },// further names the function sources see
});
const context = new NodeContext(document.library, document.functions);
const results = await context.renderEntryPoint("/main");
```

`renderEntryPoint` returns the rendered child's results as they are. Rendering the network node
itself would apply the network's own output range, which is what happens when it is used as a node
inside another network, not when it is the thing being shown.

## How the two models line up

| Classic (`cycleMap`, `_evaluateNode`)        | Core (`buildArgumentMaps`, `postProcess`)      |
| -------------------------------------------- | ---------------------------------------------- |
| longest argument list sets the invocations    | longest value-range port sets the invocations   |
| shorter lists cycle (`arg[i % l]`)            | shorter lists cycle (`values[i % values.length]`) |
| an empty list means no invocation             | a port with no values means no invocation       |
| `takesList` parameter gets the whole list     | a list-range port gets the whole list           |
| `returnsList` concatenates the results        | a list output range concatenates the results    |
| a network's `parameters` are its inlets       | published ports (`childReference`)              |

Four things are classic's own, and the core now carries each of them:

- **`masterList`** on a node names the parameter whose length sets the number of invocations,
  instead of the longest one. In the core this is `Node.masterInput`.
- **An inlet can feed several child ports.** NodeBox 3 publishes a port to exactly one child, so
  `Port.childReferences` holds the rest.
- **A list of points reaching a `shape` parameter is one argument**, not one point per invocation,
  and when a value-range `shape` node returns such a list from each invocation, the node's result is
  the *first* of those lists rather than all of them joined. Both are duck-typed on `x` and `y`.
- **Nothing is converted.** Classic parameters are positional and untyped at run time: a value
  reaches the function exactly as it was stored, and `null` is a value rather than "no value". The
  declared types are kept as `classic:<type>` so that none of NodeBox 3's Java conversions apply,
  while the editor can still show the right control.

## Functions

A classic project is a list of functions. A `code` function is JavaScript that assigns itself into
its project's namespace (`g.frame = function () { ... }`); a `network` function is a graph. The
original runtime evaluated the sources against the browser's global object. `ClassicRuntime`
evaluates them against namespace objects passed in as arguments, so one project cannot reach or
clobber another. The exception is a `.js` asset, which `loadScript` runs in the global scope the way
a `<script>` tag did, because that is how those projects share code.

Most of `core/g` has no source of its own: its 172 functions are declarations over the
[`g.js`](https://github.com/nodebox/g.js) package, which the host passes in as a namespace. The
declarations are built into the core (`libraries/classic-g.json`), so only the package is needed.

Two pins matter for identical output:

- **`g.js` 0.1.17**, the version the classic app shipped.
- **`opentype.js` 1.3.4** (as `opentype-classic`), because 2.x builds different glyph outlines and
  every text-heavy project would drift.

## Checking against the original

`scripts/classic-render.mts` renders classic projects on the core and prints every node's results.
The original runtime can be run the same way in Node, which is how this was checked: all 81 demos
and tutorials that render under the original runtime produce identical results on the core.
`test/classic.test.ts` keeps those numbers, and `test/classic-results.json` records them.

```bash
cd packages/core
node --import tsx scripts/classic-render.mts ../server/data demo/flocking tutorial/a3listmatch
VERBOSE=1 node --import tsx scripts/classic-render.mts ../server/data demo/simple   # every node
```

Fourteen further projects fail under the original runtime too, for reasons that have nothing to do
with evaluation: six depend on a `core/anim` project that was never published, two need a real
canvas for image filters, and the rest hit a missing function or an unparsable colour.
