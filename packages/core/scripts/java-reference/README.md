# Java reference harness

`JavaRender.java` renders the top-level nodes of a `.ndbx` document with the original NodeBox 3
Java engine (the sources under `src/main/java`) and prints every node's results in the same
shape as `scripts/ndbx-inspect.mts` prints them with `NDBX_INSPECT_PLAIN=1 NDBX_INSPECT_POINTS=1`.
Diffing the two traces with `scripts/ndbx-compare-traces.mts` is how the TypeScript port was
checked against Java, node by node, on the Cartan Node Library demos.

```bash
# From the repository root. Java 11+ and Maven are needed; AWT needs a display (xvfb-run).
mvn -q compile -DskipTests
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
CP="target/classes:packages/core/scripts/java-reference:$(cat /tmp/cp.txt)"
javac -cp "$CP" -d packages/core/scripts/java-reference packages/core/scripts/java-reference/JavaRender.java

# Java trace (VERBOSE=1 prints every node; STACK=1 prints stack traces of failures)
VERBOSE=1 xvfb-run -a java -Xmx4g -cp "$CP" JavaRender demo.ndbx > demo.java.log
# TypeScript trace
cd packages/core
NDBX_INSPECT_PLAIN=1 NDBX_INSPECT_POINTS=1 node --import tsx scripts/ndbx-inspect.mts demo.ndbx /demo_ > demo.ts.log
node --import tsx scripts/ndbx-compare-traces.mts demo.java.log demo.ts.log
```

Both traces record the _last_ invocation of a node, so inside networks that run once per list
item the inner values differ harmlessly; compare the network's own output first. Python and
Clojure function modules of a document load in Java only when the `.py`/`.clj` files sit next to
the document.
