package nodebox.node;

import com.google.common.collect.ImmutableMap;
import nodebox.function.FunctionRepository;
import nodebox.function.PythonLibrary;
import nodebox.graphics.Color;
import nodebox.graphics.Point;
import nodebox.util.SideEffects;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The render cache must not change what NodeBox renders. For every example document this runs a random
 * sequence of edits a user could make (frames, mouse moves, parameter changes, rewiring, renaming,
 * deleting, undo, switching the rendered node) and after each step compares a render with the cache to a
 * render without it. Each sequence runs twice: with verify mode, which reports a stale cache hit anywhere in
 * the network with its path, and without, which renders exactly as the application does.
 * <p/>
 * The sequence is reproducible: set -Dnodebox.cache.seed and -Dnodebox.cache.steps to replay or extend it.
 */
@RunWith(Parameterized.class)
public class RenderCacheDifferentialTest {

    private static final long SEED = Long.getLong("nodebox.cache.seed", 20260928L);
    private static final int STEPS = Integer.getInteger("nodebox.cache.steps", 25);

    // These call web services through network.http_get.
    private static final List<String> NEEDS_NETWORK = List.of("Geocoding.ndbx", "Twitter API.ndbx");

    private static NodeRepository systemRepository;

    /** The example documents, and a scenario that uses what the examples do not. */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> documents() throws IOException {
        List<Object[]> documents = new ArrayList<Object[]>();
        try (Stream<Path> files = Files.walk(new File("examples").toPath())) {
            for (File f : files.map(Path::toFile)
                    .filter(f -> f.getName().endsWith(".ndbx") && !NEEDS_NETWORK.contains(f.getName()))
                    .sorted().collect(Collectors.toList())) {
                documents.add(new Object[]{f.getName(), f});
            }
        }
        // The scenario is small, so it runs as several independent sequences.
        for (int i = 1; i <= 8; i++) {
            documents.add(new Object[]{"scenario " + i, null});
        }
        // Verify mode reports a stale hit anywhere in the network with its path, but it also runs every hit
        // node again, which hides a skipped side effect. So every sequence runs both ways.
        List<Object[]> runs = new ArrayList<Object[]>();
        for (Object[] document : documents) {
            runs.add(new Object[]{document[0] + " (verify)", document[1], true});
            runs.add(new Object[]{document[0], document[1], false});
        }
        return runs;
    }

    private final String name;
    private final File file;
    private final boolean verify;
    private boolean wasVerifying;
    // Extra edits for the scenario: rewrite the imported file, edit and reload the code.
    private File textFile;
    private File script;

    public RenderCacheDifferentialTest(String name, File file, boolean verify) {
        this.name = name;
        this.file = file;
        this.verify = verify;
    }

    @Before
    public void setUp() {
        wasVerifying = RenderCache.isVerifying();
        RenderCache.setVerifying(verify);
    }

    @After
    public void tearDown() {
        RenderCache.setVerifying(wasVerifying);
    }

    private static synchronized NodeRepository systemRepository() {
        if (systemRepository == null) {
            List<NodeLibrary> libraries = new ArrayList<NodeLibrary>();
            for (String library : List.of("math", "string", "color", "list", "data", "corevector", "network", "device")) {
                libraries.add(NodeLibrary.loadSystemLibrary(library));
            }
            systemRepository = NodeRepository.of(libraries.toArray(new NodeLibrary[0]));
        }
        return systemRepository;
    }

    private NodeLibrary load() throws IOException {
        if (file == null) return scenario();
        try {
            return NodeLibrary.load(file, systemRepository());
        } catch (OutdatedLibraryException e) {
            return NodeLibraryUpgrades.upgrade(file).getLibrary(file, systemRepository());
        }
    }

    /**
     * A document with an imported file, user Python code, a stateful node fed by the mouse, an always-rendered
     * node with a side effect, a subnetwork that is rendered through a published port and one that is not.
     */
    private NodeLibrary scenario() throws IOException {
        File dir = Files.createTempDirectory("nodebox-scenario").toFile();
        dir.deleteOnExit();
        textFile = new File(dir, "words.txt");
        Files.writeString(textFile.toPath(), "alpha\nbeta\n");
        script = new File(dir, "user.py");
        writeScript(1);
        FunctionRepository userFunctions = FunctionRepository.of(
                PythonLibrary.loadScript("user", script.getAbsolutePath()), SideEffects.LIBRARY);
        NodeRepository nodes = systemRepository();

        Node words = nodes.getNode("data.import_text").extend().withName("words")
                .withInputValue("file", textFile.getAbsolutePath());
        Node count = Node.ROOT.withName("count").withFunction("user/count").withOutputType("float")
                .withInputAdded(Port.customPort("items", "list").withRange(Port.Range.LIST));
        Node scale = Node.ROOT.withName("scale").withFunction("user/scale").withOutputType("float")
                .withInputAdded(Port.floatPort("x", 3)).withInputAdded(Port.floatPort("f", 2));
        Node frame = nodes.getNode("core.frame").extend().withName("frame");
        Node mouse = nodes.getNode("device.mouse_position").extend().withName("mouse");
        Node buffer = nodes.getNode("device.buffer_points").extend().withName("buffer").withInputValue("size", 5L);
        Node output = Node.ROOT.withName("output").withFunction("side-effects/setNumber")
                .withInputAdded(Port.intPort("number", 0)).withAlwaysRenderedSet(true);

        // Rendered through a published port: its inner nodes are computed with the network's arguments.
        Node innerWords = words.withName("innerWords");
        Node innerScale = scale.withName("innerScale");
        Node published = Node.NETWORK.withName("published").withOutputRange(Port.Range.LIST)
                .withChildAdded(innerWords).withChildAdded(innerScale).withChildAdded(count.withName("innerCount"))
                .connect("innerWords", "innerCount", "items").connect("innerCount", "innerScale", "x")
                .withRenderedChildName("innerScale")
                .publish("innerScale", "f", "f");
        // No inputs: rendered as a whole, its inner nodes are cached one by one.
        Node closed = Node.NETWORK.withName("closed").withOutputRange(Port.Range.LIST)
                .withChildAdded(words.withName("closedWords")).withRenderedChildName("closedWords");
        // A published port fed by a constant: cached as a whole, while a node inside reads the file.
        Node stable = Node.NETWORK.withName("stable").withOutputRange(Port.Range.LIST)
                .withChildAdded(words.withName("stableWords")).withChildAdded(count.withName("stableCount"))
                .withChildAdded(scale.withName("stableScale"))
                .connect("stableWords", "stableCount", "items").connect("stableCount", "stableScale", "x")
                .withRenderedChildName("stableScale")
                .publish("stableScale", "f", "f");
        Node two = nodes.getNode("math.number").extend().withName("two").withInputValue("value", 2.0);
        // A stateful node with a constant input: it must still grow on every render.
        Node point = nodes.getNode("corevector.make_point").extend().withName("point").withInputValue("x", 7.0);
        Node constantBuffer = buffer.withName("constantBuffer");

        Node combine = nodes.getNode("list.combine").extend().withName("combine");
        Node combineMore = combine.withName("combineMore");
        Node root = Node.NETWORK.withName("root")
                .withChildAdded(words).withChildAdded(count).withChildAdded(scale).withChildAdded(frame)
                .withChildAdded(mouse).withChildAdded(buffer).withChildAdded(output)
                .withChildAdded(published).withChildAdded(closed).withChildAdded(combine)
                .withChildAdded(stable).withChildAdded(two).withChildAdded(point).withChildAdded(constantBuffer)
                .withChildAdded(combineMore)
                .connect("words", "count", "items")
                .connect("count", "scale", "x")
                .connect("frame", "published", "f")
                .connect("mouse", "buffer", "point")
                .connect("count", "output", "number")
                .connect("scale", "combine", "list1")
                .connect("published", "combine", "list2")
                .connect("closed", "combine", "list3")
                .connect("buffer", "combine", "list4")
                .connect("words", "combine", "list5")
                .connect("output", "combine", "list6")
                .connect("two", "stable", "f")
                .connect("point", "constantBuffer", "point")
                .connect("combine", "combineMore", "list1")
                .connect("stable", "combineMore", "list2")
                .connect("constantBuffer", "combineMore", "list3")
                .withRenderedChildName("combineMore");
        return NodeLibrary.create("scenario", root, nodes, userFunctions);
    }

    private void writeScript(int version) throws IOException {
        Files.writeString(script.toPath(), "def count(items):\n" +
                "    return len(items)\n" +
                "def scale(x, f):\n" +
                "    return x * f + " + version + "\n");
    }

    /** The scenario's own edits; null when the document has none or the dice say no. */
    private String scenarioEdit(Random random, FunctionRepository documentFunctions, FunctionRepository functions) throws IOException {
        if (file != null) return null;
        int kind = random.nextInt(8);
        if (kind == 0) {
            StringBuilder text = new StringBuilder();
            for (int i = random.nextInt(5); i >= 0; i--) text.append("word").append(random.nextInt(100)).append('\n');
            Files.writeString(textFile.toPath(), text.toString());
            // File times have a resolution of a second or worse; move the time on explicitly.
            textFile.setLastModified(textFile.lastModified() + 2000 + random.nextInt(100000));
            return "touch " + textFile.getName();
        }
        if (kind == 1) {
            writeScript(random.nextInt(1000));
            // Like NodeBoxDocument.reload.
            documentFunctions.reload();
            functions.invalidateFunctionCache();
            return "reload code";
        }
        return null;
    }

    /** Render like the document does. An error is part of the outcome and must match too. */
    private static Object render(NodeLibrary library, FunctionRepository functions, String network,
                                 Map<String, ?> data, RenderCache cache) {
        try {
            NodeContext context = new NodeContext(library, functions, data, ImmutableMap.<String, Object>of(), cache);
            List<?> results = context.renderNode(network);
            context.renderAlwaysRenderedNodes(network);
            context.commitState();
            return List.of(ResultSnapshot.of(results), "side effect " + SideEffects.theOutput);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("The render cache")) throw e;
            return List.of("error", e.getClass().getName());
        } catch (RuntimeException e) {
            return List.of("error", e.getClass().getName());
        }
    }

    @Test
    public void cachedRenderMatchesUncachedRender() throws IOException {
        Random random = new Random(SEED ^ name.replace(" (verify)", "").hashCode());
        NodeLibrary library = load();
        FunctionRepository functions = FunctionRepository.combine(
                systemRepository().getFunctionRepository(), library.getFunctionRepository());
        NodeLibraryController controller = NodeLibraryController.withLibrary(library);
        List<NodeLibrary> history = new ArrayList<NodeLibrary>();
        List<String> log = new ArrayList<String>();
        RenderCache cache = new RenderCache();
        RenderCache referenceState = new RenderCache();
        double frame = 1;
        Point mouse = Point.ZERO;
        String network = "/";

        int steps = file == null ? 2 * STEPS : STEPS;
        for (int step = 0; step <= steps; step++) {
            if (step > 0) {
                history.add(controller.getNodeLibrary());
                String action = scenarioEdit(random, library.getFunctionRepository(), functions);
                if (action == null) action = edit(random, controller, history);
                if (action.startsWith("frame")) frame = 1 + random.nextInt(100);
                if (action.startsWith("mouse")) mouse = new Point(random.nextInt(1000) - 500, random.nextInt(1000) - 500);
                if (action.startsWith("render")) network = randomNetwork(random, controller.getNodeLibrary());
                if (controller.getNodeLibrary().getFlattenedNodeMap().get(network) == null) network = "/";
                log.add(action);
            }
            NodeLibrary current = controller.getNodeLibrary();
            Map<String, ?> data = ImmutableMap.of("frame", frame, "mouse.position", mouse);
            SideEffects.reset();
            Object cached;
            try {
                cached = render(current, functions, network, data, cache);
            } catch (IllegalStateException e) {
                fail(name + ", step " + step + " after " + log + ": " + e.getMessage());
                return;
            }
            // The reference keeps the state of stateful nodes between steps but no cached results.
            RenderCache reference = referenceState;
            referenceState = new RenderCache();
            referenceState.setState(reference.stateSnapshot());
            SideEffects.reset();
            Object fresh = render(current, functions, network, data, referenceState);
            assertEquals(name + ", step " + step + " after " + log, fresh, cached);
            if (step == 0 && fresh.toString().startsWith("[error")) {
                fail(name + " does not render: " + fresh);
            }
        }
        if (Boolean.getBoolean("nodebox.cache.log")) System.out.println(name + ": " + log);
        assertTrue(name + " never hit the cache, so it tested nothing", cache.hits() > 0);
    }

    /** Apply one random user edit and describe it. */
    private static String edit(Random random, NodeLibraryController controller, List<NodeLibrary> history) {
        List<String> paths = new ArrayList<String>(controller.getNodeLibrary().getFlattenedNodeMap().keySet());
        paths.remove("/");
        int kind = random.nextInt(10);
        if (paths.isEmpty() || kind == 0) return "frame";
        if (kind == 1) return "mouse";
        if (kind == 2) return "render";
        String path = paths.get(random.nextInt(paths.size()));
        Node node = controller.getNode(path);
        String parentPath = parentPath(path);
        if (kind == 3 && history.size() > 1) {
            controller.setNodeLibrary(history.get(random.nextInt(history.size())));
            return "undo";
        }
        if (kind == 4) {
            Node parent = controller.getNode(parentPath);
            if (!parent.getConnections().isEmpty()) {
                Connection c = parent.getConnections().get(random.nextInt(parent.getConnections().size()));
                controller.disconnect(parentPath, c);
                return "disconnect " + c;
            }
        }
        if (kind == 5) {
            String newName = node.getName() + "x";
            controller.renameNode(parentPath, node.getName(), newName);
            return "rename " + path;
        }
        if (kind == 6 && random.nextInt(3) == 0) {
            controller.removeNode(parentPath, node.getName());
            return "remove " + path;
        }
        if (kind == 7) {
            Node parent = controller.getNode(parentPath);
            controller.setRenderedChild(parentPath, parent.getChildren().get(random.nextInt(parent.getChildren().size())).getName());
            return "renderedChild " + parentPath;
        }
        List<Port> ports = new ArrayList<Port>();
        for (Port port : node.getInputs()) {
            if (newValue(random, port) != null) ports.add(port);
        }
        if (ports.isEmpty()) return "frame";
        Port port = ports.get(random.nextInt(ports.size()));
        Object value = newValue(random, port);
        controller.setPortValue(path, port.getName(), value);
        return "set " + path + "." + port.getName() + "=" + value;
    }

    private static Object newValue(Random random, Port port) {
        Object value = port.getValue();
        if (value instanceof Double) return (Double) value + random.nextGaussian() * Math.max(1, Math.abs((Double) value) / 2);
        if (value instanceof Long) return (Long) value + random.nextInt(7) - 3;
        if (value instanceof Boolean) return !(Boolean) value;
        if (value instanceof Point) return new Point(((Point) value).x + random.nextInt(41) - 20, ((Point) value).y + random.nextInt(41) - 20);
        if (value instanceof Color) return new Color(random.nextDouble(), random.nextDouble(), random.nextDouble());
        return null;
    }

    private static String randomNetwork(Random random, NodeLibrary library) {
        List<String> networks = new ArrayList<String>();
        for (Map.Entry<String, Node> entry : library.getFlattenedNodeMap().entrySet()) {
            if (entry.getValue().isNetwork()) networks.add(entry.getKey());
        }
        return networks.get(random.nextInt(networks.size()));
    }

    private static String parentPath(String path) {
        int slash = path.lastIndexOf('/');
        return slash == 0 ? "/" : path.substring(0, slash);
    }
}
