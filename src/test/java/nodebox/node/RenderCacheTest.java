package nodebox.node;

import com.google.common.collect.ImmutableMap;
import nodebox.function.*;
import nodebox.graphics.Point;
import nodebox.util.SideEffects;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static junit.framework.TestCase.*;
import static nodebox.util.Assertions.assertResultsEqual;

/**
 * Tests for cross-render result caching (see {@link RenderCache}).
 *
 * <p>These are computation-count tests: they assert how many times a node's underlying function runs,
 * using the {@code side-effects/increaseAndCount} probe. That makes them deterministic regression
 * guards — a future change that breaks caching (recomputing unchanged nodes) or breaks invalidation
 * (reusing a stale result after an edit) changes a count or a value and fails the test.
 */
public class RenderCacheTest {

    private final FunctionRepository functions = FunctionRepository.of(
            CoreFunctions.LIBRARY, MathFunctions.LIBRARY, ListFunctions.LIBRARY, SideEffects.LIBRARY);

    @Before
    public void setUp() {
        SideEffects.reset();
    }

    private final Node makeNumbersNode = Node.ROOT
            .withName("makeNumbers")
            .withFunction("math/makeNumbers")
            .withOutputRange(Port.Range.LIST)
            .withInputAdded(Port.stringPort("string", ""))
            .withInputAdded(Port.stringPort("separator", " "));

    private final Node incNode = Node.ROOT
            .withName("inc")
            .withFunction("side-effects/increaseAndCount")
            .withInputAdded(Port.floatPort("number", 0));

    private final Node addNode = Node.ROOT
            .withName("add")
            .withFunction("math/add")
            .withInputAdded(Port.floatPort("v1", 0.0))
            .withInputAdded(Port.floatPort("v2", 0.0));

    private List<?> render(Node root, String childName, RenderCache cache) {
        NodeLibrary library = NodeLibrary.create("test", root, functions);
        Node child = root.getChild(childName);
        return new NodeContext(library, functions, ImmutableMap.<String, Object>of(), ImmutableMap.<String, Object>of(), cache)
                .renderChild("/", child);
    }

    // ------------------------------------------------------------------
    // Reuse across renders.
    // ------------------------------------------------------------------

    @Test
    public void resultReusedAcrossRendersWithSharedCache() {
        Node makeNumbers = makeNumbersNode.withName("makeNumbers").withInputValue("string", "1 2 3");
        Node net = Node.NETWORK
                .withChildAdded(makeNumbers)
                .withChildAdded(incNode)
                .connect("makeNumbers", "inc", "number");

        RenderCache cache = new RenderCache();
        assertResultsEqual(render(net, "inc", cache), 2.0, 3.0, 4.0);
        assertEquals(3, SideEffects.theCounter);

        // A second render with the same cache reuses the result: the function does not run again.
        assertResultsEqual(render(net, "inc", cache), 2.0, 3.0, 4.0);
        assertEquals("Result should be reused across renders, not recomputed", 3, SideEffects.theCounter);
    }

    @Test
    public void withoutSharedCacheResultIsRecomputed() {
        Node makeNumbers = makeNumbersNode.withName("makeNumbers").withInputValue("string", "1 2 3");
        Node net = Node.NETWORK
                .withChildAdded(makeNumbers)
                .withChildAdded(incNode)
                .connect("makeNumbers", "inc", "number");

        // No cache (null): each render recomputes from scratch.
        render(net, "inc", null);
        render(net, "inc", null);
        assertEquals(6, SideEffects.theCounter);
    }

    // ------------------------------------------------------------------
    // Incremental invalidation: only the changed subgraph recomputes.
    // ------------------------------------------------------------------

    @Test
    public void editingDownstreamReusesUpstream() {
        // inc (counted, shared) feeds two add nodes. Editing one add must NOT recompute inc.
        Node makeNumbers = makeNumbersNode.withName("makeNumbers").withInputValue("string", "1 2 3");
        Node addA = addNode.withName("addA").withInputValue("v2", 10.0);
        Node addB = addNode.withName("addB").withInputValue("v2", 100.0);
        Node net = Node.NETWORK
                .withChildAdded(makeNumbers)
                .withChildAdded(incNode)
                .withChildAdded(addA)
                .withChildAdded(addB)
                .connect("makeNumbers", "inc", "number")
                .connect("inc", "addA", "v1")
                .connect("inc", "addB", "v1");

        RenderCache cache = new RenderCache();
        assertResultsEqual(render(net, "addA", cache), 12.0, 13.0, 14.0);
        assertResultsEqual(render(net, "addB", cache), 102.0, 103.0, 104.0);
        assertEquals("inc runs once for its 3 elements", 3, SideEffects.theCounter);

        // Edit only addB. inc (shared upstream) must be reused, so the counter stays at 3.
        Node net2 = net.withChildReplaced("addB", addB.withInputValue("v2", 200.0));
        assertResultsEqual(render(net2, "addB", cache), 202.0, 203.0, 204.0);
        assertEquals("Editing addB must not recompute the shared upstream inc node", 3, SideEffects.theCounter);

        // addA is untouched and fully reused.
        assertResultsEqual(render(net2, "addA", cache), 12.0, 13.0, 14.0);
        assertEquals(3, SideEffects.theCounter);
    }

    @Test
    public void editingUpstreamRecomputesDownstream() {
        Node makeNumbers = makeNumbersNode.withName("makeNumbers").withInputValue("string", "1 2 3");
        Node net = Node.NETWORK
                .withChildAdded(makeNumbers)
                .withChildAdded(incNode)
                .connect("makeNumbers", "inc", "number");

        RenderCache cache = new RenderCache();
        assertResultsEqual(render(net, "inc", cache), 2.0, 3.0, 4.0);
        assertEquals(3, SideEffects.theCounter);

        // Change the upstream source: inc must recompute and reflect the new input.
        Node net2 = net.withChildReplaced("makeNumbers", makeNumbers.withInputValue("string", "10 20 30 40"));
        assertResultsEqual(render(net2, "inc", cache), 11.0, 21.0, 31.0, 41.0);
        assertEquals("inc should recompute for the 4 new values", 7, SideEffects.theCounter);
    }

    // ------------------------------------------------------------------
    // Purity: context-dependent nodes are never cached (no stale results).
    // ------------------------------------------------------------------

    @Test
    public void contextNodeIsNotCachedAcrossFrames() {
        Node frameNode = Node.ROOT
                .withName("frame")
                .withFunction("core/frame")
                .withInputAdded(Port.customPort("context", "context"));
        Node net = Node.NETWORK.withChildAdded(frameNode);
        NodeLibrary library = NodeLibrary.create("test", net, functions);
        RenderCache cache = new RenderCache();

        List<?> atFrame1 = new NodeContext(library, functions, ImmutableMap.<String, Object>of("frame", 1.0),
                ImmutableMap.<String, Object>of(), cache).renderChild("/", frameNode);
        assertResultsEqual(atFrame1, 1.0);

        // Same cache, different frame: the frame node must reflect the new frame, not a stale cached value.
        List<?> atFrame2 = new NodeContext(library, functions, ImmutableMap.<String, Object>of("frame", 2.0),
                ImmutableMap.<String, Object>of(), cache).renderChild("/", frameNode);
        assertResultsEqual(atFrame2, 2.0);
    }

    @Test
    public void networkContainingContextNodeIsNotCacheable() {
        Node frameNode = Node.ROOT
                .withName("frame")
                .withFunction("core/frame")
                .withInputAdded(Port.customPort("context", "context"));
        Node pure = makeNumbersNode.withName("pure").withInputValue("string", "1 2 3");
        Node subnet = Node.NETWORK.withName("subnet")
                .withChildAdded(frameNode)
                .withChildAdded(pure)
                .withRenderedChildName("frame");

        RenderCache cache = new RenderCache();
        assertFalse("A network containing a context node must be uncacheable", cache.isCacheable(subnet, functions));
        assertTrue("A pure leaf node must be cacheable", cache.isCacheable(pure, functions));
    }

    @Test
    public void cacheKeepsOneResultPerNodeAcrossFrames() {
        Node frameNode = Node.ROOT.withName("frame").withFunction("core/frame")
                .withInputAdded(Port.customPort("context", "context"));
        Node numbers = makeNumbersNode.withName("numbers").withInputValue("string", "1 2 3");
        Node add = addNode.withName("add");
        Node net = Node.NETWORK
                .withChildAdded(frameNode)
                .withChildAdded(numbers)
                .withChildAdded(add)
                .connect("numbers", "add", "v1")
                .connect("frame", "add", "v2")
                .withRenderedChildName("add");
        NodeLibrary library = NodeLibrary.create("test", net, functions);
        RenderCache cache = new RenderCache();

        for (int frame = 1; frame <= 500; frame++) {
            List<?> result = new NodeContext(library, functions, ImmutableMap.<String, Object>of("frame", (double) frame),
                    ImmutableMap.<String, Object>of(), cache).renderNode("/");
            assertResultsEqual(result, 1.0 + frame, 2.0 + frame, 3.0 + frame);
        }
        // Results from earlier frames can never be hit again, so they must not accumulate.
        assertTrue("Cache should hold at most one result per node, holds " + cache.size(), cache.size() <= 3);
    }

    @Test
    public void userCodeIsCachedUntilReload() throws IOException {
        File script = File.createTempFile("nodebox-user", ".py");
        script.deleteOnExit();
        Files.writeString(script.toPath(), "def val(x):\n    return x + 1\n");
        FunctionLibrary userLibrary = PythonLibrary.loadScript("user", script.getAbsolutePath());
        FunctionRepository userFunctions = FunctionRepository.of(MathFunctions.LIBRARY, userLibrary);
        Node val = Node.ROOT.withName("val").withFunction("user/val").withInputAdded(Port.floatPort("x", 10));
        Node net = Node.NETWORK.withChildAdded(val).withRenderedChildName("val");
        NodeLibrary library = NodeLibrary.create("test", net, userFunctions);
        RenderCache cache = new RenderCache();

        List<?> first = renderWith(library, userFunctions, cache);
        assertResultsEqual(first, 11.0);
        assertSame("User code is cached like any other function", first, renderWith(library, userFunctions, cache));

        // The user edits the script and reloads (Cmd-R): the same cache must not return the old result.
        Files.writeString(script.toPath(), "def val(x):\n    return x + 2\n");
        userFunctions.reload();
        assertResultsEqual(renderWith(library, userFunctions, cache), 12.0);
    }

    @Test
    public void replacedLibraryInvalidatesCache() throws IOException {
        // The Code Libraries dialog loads a library again as a new instance with the same namespace.
        File script = File.createTempFile("nodebox-user", ".py");
        script.deleteOnExit();
        Files.writeString(script.toPath(), "def val(x):\n    return x + 1\n");
        FunctionRepository before = FunctionRepository.of(PythonLibrary.loadScript("user", script.getAbsolutePath()));
        Node val = Node.ROOT.withName("val").withFunction("user/val").withInputAdded(Port.floatPort("x", 10));
        Node net = Node.NETWORK.withChildAdded(val).withRenderedChildName("val");
        RenderCache cache = new RenderCache();
        assertResultsEqual(renderWith(NodeLibrary.create("test", net, before), before, cache), 11.0);

        Files.writeString(script.toPath(), "def val(x):\n    return x + 2\n");
        FunctionRepository after = FunctionRepository.of(PythonLibrary.loadScript("user", script.getAbsolutePath()));
        assertResultsEqual(renderWith(NodeLibrary.create("test", net, after), after, cache), 12.0);
    }

    @Test
    public void equalFunctionRepositoryKeepsCache() {
        // Undo and redo rebuild the document's function repository from the same libraries.
        Node makeNumbers = makeNumbersNode.withName("makeNumbers").withInputValue("string", "1 2 3");
        Node net = Node.NETWORK.withChildAdded(makeNumbers).withChildAdded(incNode)
                .connect("makeNumbers", "inc", "number").withRenderedChildName("inc");
        NodeLibrary library = NodeLibrary.create("test", net, functions);
        RenderCache cache = new RenderCache();

        // Reload often enough that the library version is no longer a cached Long instance.
        for (int i = 0; i < 200; i++) {
            functions.reload();
        }
        renderWith(library, functions, cache);
        FunctionRepository rebuilt = FunctionRepository.combine(functions);
        renderWith(library, rebuilt, cache);
        assertEquals("An equal repository must not drop the cached results", 3, SideEffects.theCounter);
    }

    // ------------------------------------------------------------------
    // Time-dependent functions run on every render.
    // ------------------------------------------------------------------

    private List<?> renderTwice(String function, FunctionRepository functionRepository) {
        Node node = Node.ROOT.withName("tick").withFunction(function).withInputAdded(Port.floatPort("x", 0));
        Node net = Node.NETWORK.withChildAdded(node).withRenderedChildName("tick");
        NodeLibrary library = NodeLibrary.create("test", net, functionRepository);
        RenderCache cache = new RenderCache();
        renderWith(library, functionRepository, cache);
        return renderWith(library, functionRepository, cache);
    }

    @Test
    public void timeDependentPythonFunctionRunsOnEveryRender() throws IOException {
        File script = File.createTempFile("nodebox-tick", ".py");
        script.deleteOnExit();
        Files.writeString(script.toPath(), "calls = [0]\n" +
                "def tick(x):\n" +
                "    calls[0] += 1\n" +
                "    return calls[0]\n" +
                "tick.timeDependent = True\n");
        FunctionRepository repository = FunctionRepository.of(PythonLibrary.loadScript("ticks", script.getAbsolutePath()));
        assertResultsEqual(renderTwice("ticks/tick", repository), 2L);
    }

    @Test
    public void timeDependentClojureFunctionRunsOnEveryRender() throws IOException {
        File script = File.createTempFile("nodebox-tick", ".clj");
        script.deleteOnExit();
        Files.writeString(script.toPath(), "(ns clojure-ticks)\n" +
                "(def calls (atom 0))\n" +
                "(defn ^:time-dependent tick [x] (swap! calls inc))\n" +
                "(def done true)\n");
        FunctionRepository repository = FunctionRepository.of(FunctionLibrary.load("clojure:" + script.getAbsolutePath()));
        assertResultsEqual(renderTwice("clojure-ticks/tick", repository), 2L);
    }

    @Test
    public void timeDependentJavaFunctionRunsOnEveryRender() {
        renderTwice("side-effects/increaseAndCountEveryRender", functions);
        assertEquals(2, SideEffects.theCounter);
    }

    @Test
    public void fileImportSeesChangesOnDisk() throws IOException {
        File text = File.createTempFile("nodebox-import", ".txt");
        text.deleteOnExit();
        Files.writeString(text.toPath(), "a\nb\n");
        text.setLastModified(1000000000L);
        FunctionRepository dataFunctions = FunctionRepository.of(DataFunctions.LIBRARY);
        Node importText = Node.ROOT.withName("import_text").withFunction("data/importText")
                .withOutputRange(Port.Range.LIST)
                .withInputAdded(Port.stringPort("file", text.getAbsolutePath()).withWidget(Port.Widget.FILE));
        Node net = Node.NETWORK.withChildAdded(importText).withRenderedChildName("import_text");
        NodeLibrary library = NodeLibrary.create("test", net, dataFunctions);
        RenderCache cache = new RenderCache();

        assertResultsEqual(renderWith(library, dataFunctions, cache), "a", "b");
        assertSame("An unchanged file is served from the cache",
                renderWith(library, dataFunctions, cache), renderWith(library, dataFunctions, cache));

        Files.writeString(text.toPath(), "a\nb\nc\n");
        text.setLastModified(2000000000L);
        assertResultsEqual(renderWith(library, dataFunctions, cache), "a", "b", "c");
    }

    @Test
    public void fileImportInsideSubnetworkSeesChangesOnDisk() throws IOException {
        File text = File.createTempFile("nodebox-import", ".txt");
        text.deleteOnExit();
        Files.writeString(text.toPath(), "a\n");
        text.setLastModified(1000000000L);
        FunctionRepository dataFunctions = FunctionRepository.of(DataFunctions.LIBRARY, MathFunctions.LIBRARY);
        Node importText = Node.ROOT.withName("import_text").withFunction("data/importText")
                .withOutputRange(Port.Range.LIST)
                .withInputAdded(Port.stringPort("file", text.getAbsolutePath()).withWidget(Port.Widget.FILE));
        Node number = Node.ROOT.withName("number").withFunction("math/number").withInputAdded(Port.floatPort("value", 1));
        // A published port gives the subnetwork an input, so the parent renders it through the cache.
        Node subnet = Node.NETWORK.withName("subnet").withOutputRange(Port.Range.LIST)
                .withChildAdded(importText).withChildAdded(number)
                .withRenderedChildName("import_text")
                .publish("number", "value", "value");
        Node net = Node.NETWORK.withChildAdded(subnet).withRenderedChildName("subnet");
        NodeLibrary library = NodeLibrary.create("test", net, dataFunctions);
        RenderCache cache = new RenderCache();

        assertResultsEqual(renderWith(library, dataFunctions, cache), "a");
        Files.writeString(text.toPath(), "a\nb\n");
        text.setLastModified(2000000000L);
        assertResultsEqual(renderWith(library, dataFunctions, cache), "a", "b");
    }

    /** A completed render: the state of stateful nodes is committed. */
    private List<?> renderWith(NodeLibrary library, FunctionRepository functionRepository, RenderCache cache) {
        NodeContext context = new NodeContext(library, functionRepository, ImmutableMap.<String, Object>of(),
                ImmutableMap.<String, Object>of(), cache);
        List<?> results = context.renderNode("/");
        context.commitState();
        return results;
    }

    // ------------------------------------------------------------------
    // State: a node with a state port receives its own output of the previous render.
    // ------------------------------------------------------------------

    private final FunctionRepository deviceFunctions = FunctionRepository.of(DeviceFunctions.LIBRARY, SideEffects.LIBRARY);

    private final Node bufferPointsNode = Node.ROOT
            .withName("buffer")
            .withFunction("device/bufferPoints")
            .withOutputRange(Port.Range.LIST)
            .withInputAdded(Port.pointPort("point", new Point(1, 2)))
            .withInputAdded(Port.intPort("size", 2))
            .withInputAdded(Port.customPort("state", Port.TYPE_STATE));

    @Test
    public void statefulNodeReceivesItsPreviousOutput() {
        Node net = Node.NETWORK.withChildAdded(bufferPointsNode).withRenderedChildName("buffer");
        NodeLibrary library = NodeLibrary.create("test", net, deviceFunctions);
        RenderCache cache = new RenderCache();
        Point p = new Point(1, 2);

        assertResultsEqual(renderWith(library, deviceFunctions, cache), p);
        assertResultsEqual(renderWith(library, deviceFunctions, cache), p, p);
        // The buffer keeps its size.
        assertResultsEqual(renderWith(library, deviceFunctions, cache), p, p);

        // Rewind replaces the cache, which starts the state over.
        assertResultsEqual(renderWith(library, deviceFunctions, new RenderCache()), p);
    }

    @Test
    public void unfinishedRenderDoesNotAdvanceState() {
        Node buffer = bufferPointsNode.withInputValue("size", 10L);
        Node net = Node.NETWORK.withChildAdded(buffer).withRenderedChildName("buffer");
        NodeLibrary library = NodeLibrary.create("test", net, deviceFunctions);
        RenderCache cache = new RenderCache();
        Point p = new Point(1, 2);

        assertResultsEqual(renderWith(library, deviceFunctions, cache), p);
        // A render that fails or is canceled is never committed.
        new NodeContext(library, deviceFunctions, ImmutableMap.<String, Object>of(), ImmutableMap.<String, Object>of(), cache)
                .renderNode("/");
        assertResultsEqual(renderWith(library, deviceFunctions, cache), p, p);
    }

    @Test
    public void stateOfNodeThatIsNoLongerRenderedIsDropped() {
        Node other = Node.ROOT.withName("other").withFunction("side-effects/increaseAndCount")
                .withInputAdded(Port.floatPort("number", 0));
        Node net = Node.NETWORK.withChildAdded(bufferPointsNode).withChildAdded(other).withRenderedChildName("buffer");
        NodeLibrary library = NodeLibrary.create("test", net, deviceFunctions);
        RenderCache cache = new RenderCache();
        Point p = new Point(1, 2);

        renderWith(library, deviceFunctions, cache);
        renderWith(library.withRoot(net.withRenderedChildName("other")), deviceFunctions, cache);
        // The buffer was not part of the last render, so it starts over, as it would for a new node.
        assertResultsEqual(renderWith(library, deviceFunctions, cache), p);
    }

    @Test
    public void nodesUpstreamOfStatefulNodeStayCached() {
        Node inc = incNode.withInputValue("number", 1.0);
        Node pointFromNumber = Node.ROOT.withName("buffer").withFunction("device/bufferPoints")
                .withOutputRange(Port.Range.LIST)
                .withInputAdded(Port.pointPort("point", Point.ZERO))
                .withInputAdded(Port.intPort("size", 10))
                .withInputAdded(Port.customPort("state", Port.TYPE_STATE));
        Node net = Node.NETWORK
                .withChildAdded(inc)
                .withChildAdded(pointFromNumber)
                .connect("inc", "buffer", "point")
                .withRenderedChildName("buffer");
        NodeLibrary library = NodeLibrary.create("test", net, deviceFunctions);
        RenderCache cache = new RenderCache();

        for (int i = 1; i <= 3; i++) {
            assertEquals(i, renderWith(library, deviceFunctions, cache).size());
        }
        assertEquals("The pure node feeding the stateful node runs once", 1, SideEffects.theCounter);
    }

    @Test
    public void alwaysRenderedNodeRunsWithItsInputsOnEveryRender() {
        Node number = Node.ROOT.withName("number").withFunction("math/number")
                .withInputAdded(Port.floatPort("value", 5));
        Node output = Node.ROOT.withName("output").withFunction("side-effects/setNumber")
                .withInputAdded(Port.intPort("number", 0))
                .withAlwaysRenderedSet(true);
        Node net = Node.NETWORK
                .withChildAdded(number)
                .withChildAdded(output)
                .connect("number", "output", "number")
                .withRenderedChildName("output");
        NodeLibrary library = NodeLibrary.create("test", net, functions);
        RenderCache cache = new RenderCache();

        for (int i = 0; i < 3; i++) {
            SideEffects.theOutput = -1;
            NodeContext context = new NodeContext(library, functions, ImmutableMap.<String, Object>of(),
                    ImmutableMap.<String, Object>of(), cache);
            context.renderNode("/");
            context.renderAlwaysRenderedNodes("/");
            assertEquals("Render " + (i + 1) + " should run the node with its connected input", 5, SideEffects.theOutput);
        }
    }
}
