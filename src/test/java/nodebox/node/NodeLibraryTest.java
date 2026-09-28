package nodebox.node;

import com.google.common.collect.ImmutableList;
import nodebox.client.PythonUtils;
import nodebox.function.FunctionLibrary;
import nodebox.function.FunctionRepository;
import nodebox.function.ListFunctions;
import nodebox.function.MathFunctions;
import nodebox.graphics.Color;
import nodebox.graphics.Point;
import nodebox.util.LoadException;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static junit.framework.TestCase.*;
import static nodebox.util.Assertions.assertResultsEqual;

public class NodeLibraryTest {

    private final NodeLibrary library;
    private final Node child1;
    private final Node child2;
    private final Node parent;
    private final Node root;
    private final FunctionRepository functions;
    private final File userDir = new File(System.getProperty("user.dir"));

    public NodeLibraryTest() {
        PythonUtils.initializePython();
        child1 = Node.ROOT.withName("child1");
        child2 = Node.ROOT.withName("child2");
        parent = Node.NETWORK.withName("parent")
                .withChildAdded(child1)
                .withChildAdded(child2);
        root = Node.NETWORK.withChildAdded(parent);
        library = NodeLibrary.create("test", root, FunctionRepository.of());
        functions = FunctionRepository.of(MathFunctions.LIBRARY, ListFunctions.LIBRARY);
    }

    @Test()
    public void testCoreNodes() {
        assertTrue(NodeLibrary.coreLibrary.getRoot().hasChild(Node.ROOT));
        assertTrue(NodeLibrary.coreLibrary.getRoot().hasChild(Node.NETWORK));
    }

    @Test
    public void testNonExistingCoreNode() {
        try {
            File f = new File("src/test/files/bad-corenode.ndbx");
            NodeLibrary library = NodeLibrary.load(f, NodeRepository.of());
            fail("Should have thrown a LoadException.");
        } catch (LoadException e) {
        }
    }

    @Test
    public void testNodeForPath() {
        assertEquals(root, library.getNodeForPath("/"));
        assertEquals(parent, library.getNodeForPath("/parent"));
        assertEquals(child1, library.getNodeForPath("/parent/child1"));
        assertEquals(child2, library.getNodeForPath("/parent/child2"));

        assertNull("Invalid names return null.", library.getNodeForPath("/foo"));
        assertNull("Invalid nested names return null.", library.getNodeForPath("/parent/foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRelativePath() {
        library.getNodeForPath("parent");
    }

    @Test
    public void testSimpleReadWrite() {
        NodeLibrary simple = NodeLibrary.create("test", Node.ROOT.extend(), FunctionRepository.of());
        assertReadWriteEquals(simple, NodeRepository.of());
    }

    @Test
    public void testNestedReadWrite() {
        assertReadWriteEquals(library, NodeRepository.of());
    }

    @Test
    public void testPrototypeInSameLibrary() {
        // You can refer to a prototype in the same library as the current node.
        Node invert = Node.ROOT
                .withName("negate")
                .withFunction("math/negate")
                .withInputAdded(Port.floatPort("number", 0));
        Node invert1 = invert.extend().withName("invert1").withInputValue("number", 42.0);
        Node net = Node.NETWORK
                .withName("root")
                .withChildAdded(invert)
                .withChildAdded(invert1)
                .withRenderedChild(invert1);
        NodeLibrary originalLibrary = NodeLibrary.create("test", net, FunctionRepository.of(MathFunctions.LIBRARY));
        // Assert the original library returns the correct result.
        NodeContext context = new NodeContext(originalLibrary);
        assertResultsEqual(context.renderNode("/"), -42.0);

        // Persist / load the library and assert it still returns the correct result.
        NodeLibrary restoredLibrary = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        context = new NodeContext(restoredLibrary);
        assertResultsEqual(context.renderNode("/"), -42.0);
    }

    @Test
    public void testRenderedNode() {
        Node child1 = Node.ROOT.withName("child1");
        Node originalRoot = Node.NETWORK.withChildAdded(child1).withRenderedChild(child1);
        NodeLibrary originalLibrary = NodeLibrary.create("test", originalRoot, FunctionRepository.of());
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        assertEquals("child1", library.getRoot().getRenderedChildName());
        assertNotNull(library.getRoot().getRenderedChild());
    }

    @Test
    public void testPortSerialization() {
        assertPortSerialization(Port.intPort("int", 42));
        assertPortSerialization(Port.floatPort("float", 33.3));
        assertPortSerialization(Port.stringPort("string", "hello"));
        assertPortSerialization(Port.colorPort("color", Color.BLACK));
        assertPortSerialization(Port.pointPort("point", new Point(11, 22)));
        assertPortSerialization(Port.customPort("geometry", "nodebox.graphics.Geometry"));
    }

    @Test
    public void testLink() {
        Node originalAdd = Node.ROOT
                .withName("add")
                .withFunction("math/add")
                .withInputAdded(Port.floatPort("v1", 11))
                .withInputAdded(Port.floatPort("v2", 22));
        NodeLibrary originalLibrary = NodeLibrary.create("test", originalAdd, FunctionRepository.of(MathFunctions.LIBRARY));
        assertSingleResult(33.0, originalAdd, originalLibrary.getFunctionRepository());
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        assertTrue(library.getFunctionRepository().hasLibrary("math"));
        Node add = library.getRoot();
        assertEquals("add", add.getName());
        assertEquals("math/add", add.getFunction());
        assertSingleResult(33.0, add, library.getFunctionRepository());
    }

    /**
     * Test if the NodeLibrary stores / loads function libraries relative to the path location of the library correctly.
     */
    @Test
    public void testRelativeImport() {
        File relativeImportFile = new File("src/test/files/relative-import.ndbx");
        NodeLibrary originalLibrary = NodeLibrary.load(relativeImportFile, NodeRepository.of());
        FunctionRepository repository = originalLibrary.getFunctionRepository();
        assertTrue(repository.hasLibrary("relative"));
        assertTrue(repository.hasFunction("relative/concat"));
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        FunctionLibrary relativeLibrary = library.getFunctionRepository().getLibrary("relative");
        assertEquals("python:src/test/files/relative.py", relativeLibrary.getLink(new File(userDir, "test.ndbx")));
        assertEquals("python:relative.py", relativeLibrary.getLink(relativeImportFile));
    }

    /**
     * Test if the NodeLibrary stores / loads the port range correctly.
     */
    @Test
    public void testPortRangePersistence() {
        // Default check.
        Node makeNumbers = Node.ROOT
                .withName("makeNumbers")
                .withFunction("math/makeNumbers")
                .withOutputRange(Port.Range.LIST)
                .withInputAdded(Port.stringPort("s", "1 2 3 4 5"))
                .withInputAdded(Port.stringPort("sep", " "));
        Node reverse = Node.ROOT
                .withName("reverse")
                .withFunction("list/reverse")
                .withInputAdded(Port.customPort("list", "list"))
                .withInputRange("list", Port.Range.LIST)
                .withOutputRange(Port.Range.LIST);
        Node net = Node.NETWORK
                .withChildAdded(makeNumbers)
                .withChildAdded(reverse)
                .withRenderedChild(reverse)
                .connect("makeNumbers", "reverse", "list");
        NodeLibrary originalLibrary = NodeLibrary.create("test", net, functions);
        assertResultsEqual(originalLibrary.getRoot(), 5.0, 4.0, 3.0, 2.0, 1.0);
        // Now save / load the library and check the output.
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        assertResultsEqual(library.getRoot(), 5.0, 4.0, 3.0, 2.0, 1.0);
    }

    @Test
    public void testPortRangeOverride() {
        NodeLibrary listLibrary = NodeLibrary.load(new File("libraries/list/list.ndbx"), NodeRepository.of());
        Node slicePrototype = listLibrary.getRoot().getChild("slice");
        Node slice1 = slicePrototype.extend().withName("slice1").withInputChanged("list", slicePrototype.getInput("list").withRange(Port.Range.VALUE));
        NodeLibrary originalLibrary = NodeLibrary.create("test", slice1, NodeRepository.of(listLibrary), functions);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of(listLibrary));
        assertEquals(Port.Range.VALUE, library.getRoot().getInput("list").getRange());
    }

    @Test
    public void testPrototypeOverridePersistence() {
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        Node rangePrototype = mathLibrary.getRoot().getChild("range");
        Node range1 = rangePrototype.extend().withName("range1").withInputValue("end", 5.0);
        assertResultsEqual(range1, 0.0, 1.0, 2.0, 3.0, 4.0);
        NodeLibrary originalLibrary = NodeLibrary.create("test", range1, NodeRepository.of(mathLibrary), functions);
        // Now save / load the library and check the output.
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of(mathLibrary));
        assertResultsEqual(library.getRoot(), 0.0, 1.0, 2.0, 3.0, 4.0);
    }

    @Test
    public void testOutputTypeSerialization() {
        Node myRect = Node.ROOT.withName("rect").withOutputType("geometry");
        NodeLibrary originalLibrary = NodeLibrary.create("test", myRect);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        assertEquals("geometry", library.getRoot().getOutputType());
    }

    /**
     * Test if ports can persist their min / max values.
     */
    @Test
    public void testMinMaxPersistence() {
        Node originalRoot = Node.ROOT.withName("root").withInputAdded(Port.floatPort("v", 5.0, 0.0, 10.0));
        NodeLibrary originalLibrary = NodeLibrary.create("test", originalRoot);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        Port v = library.getRoot().getInput("v");
        assertEquals(0.0, v.getMinimumValue());
        assertEquals(10.0, v.getMaximumValue());
    }

    /**
     * Test if file writing is independent from the locale.
     * <p/>
     * We use String.format in writing points, which is locale-dependent.
     * Having the "wrong" locale would mean that the returned point was invalid.
     */
    @Test
    public void testLocale() {
        Locale savedLocale = Locale.getDefault();
        // The german locale uses a comma to separate the decimals, which could make points fail.
        Locale.setDefault(Locale.GERMAN);
        try {
            // We use points for the position and for the input port.
            Node originalRoot = Node.ROOT
                    .withName("root")
                    .withPosition(new Point(12, 34))
                    .withInputAdded(Port.pointPort("point", new Point(12, 34)));
            NodeLibrary originalLibrary = NodeLibrary.create("test", originalRoot);
            NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
            Node root = library.getRoot();
            assertPointEquals(root.getPosition(), 12.0, 34.0);
            assertPointEquals(root.getInput("point").pointValue(), 12.0, 34.0);
        } finally {
            Locale.setDefault(savedLocale);
        }
    }

    @Test
    public void testReadMenus() {
        NodeLibrary menuLibrary = NodeLibrary.load(new File("src/test/files/menus.ndbx"), NodeRepository.of());
        Port thePort = menuLibrary.getRoot().getInput("thePort");
        assertTrue(thePort.hasMenu());
        assertEquals(2, thePort.getMenuItems().size());
        assertEquals(new MenuItem("a", "Alpha"), thePort.getMenuItems().get(0));
        assertEquals(new MenuItem("b", "Beta"), thePort.getMenuItems().get(1));
    }

    @Test
    public void testMenuSerialization() {
        Node originalRoot = makeLetterMenuNode();
        NodeLibrary originalLibrary = NodeLibrary.create("test", originalRoot);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        Port letterPort = library.getRoot().getInput("letter");
        assertTrue(letterPort.hasMenu());
        assertEquals(2, letterPort.getMenuItems().size());
        assertEquals(new MenuItem("a", "Alpha"), letterPort.getMenuItems().get(0));
        assertEquals(new MenuItem("b", "Beta"), letterPort.getMenuItems().get(1));
    }

    /**
     * Test if a port using a menu prototype is correctly serialized.
     */
    @Test
    public void testMenuPrototypeSerialization() {
        Node letterPrototype = makeLetterMenuNode();
        Node letterNode = letterPrototype.extend().withName("my_letter").withInputValue("letter", "b");
        Node originalRoot = Node.NETWORK
                .withChildAdded(letterPrototype)
                .withChildAdded(letterNode);
        NodeLibrary originalLibrary = NodeLibrary.create("test", originalRoot);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        Port letterPort = library.getRoot().getChild("my_letter").getInput("letter");
        assertTrue(letterPort.hasMenu());
        assertEquals(2, letterPort.getMenuItems().size());
        assertEquals("b", letterPort.getValue());
    }

    @Test
    public void testWidgetSerialization() {
        Node originalNode = Node.ROOT
                .withInputAdded(Port.stringPort("file", "").withWidget(Port.Widget.FILE));
        NodeLibrary originalLibrary = NodeLibrary.create("test", originalNode);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        Port filePort = library.getRoot().getInput("file");
        assertEquals(Port.Widget.FILE, filePort.getWidget());
    }

    @Test
    public void testWritePrototypeFirst() {
        // We execute the test multiple times with the names switched to avoid that the order is accidentally correct.
        // This can happen because of the hashing algorithm.
        assertPrototypeBeforeInstance("alpha", "beta", "gamma");
        assertPrototypeBeforeInstance("beta", "alpha", "gamma");
        assertPrototypeBeforeInstance("gamma", "alpha", "beta");
    }

    private void assertPrototypeBeforeInstance(String prototypeName, String... instanceNames) {
        Node originalPrototype = Node.ROOT.withName(prototypeName);
        Node network = Node.NETWORK.withChildAdded(originalPrototype);
        for (String instanceName : instanceNames) {
            Node originalInstance = originalPrototype.extend().withName(instanceName);
            network = network.withChildAdded(originalInstance);
        }
        NodeLibrary originalLibrary = NodeLibrary.create("test", network);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        Node prototype = library.getRoot().getChild(prototypeName);
        for (String instanceName : instanceNames) {
            Node instance = library.getRoot().getChild(instanceName);
            assertSame(prototype, instance.getPrototype());
        }
    }

    @Test
    public void testRelativePathsInWidgets() {
        NodeLibrary library = NodeLibrary.load(new File("src/test/files/relative-file.ndbx"), NodeRepository.of());
        NodeContext context = new NodeContext(library);
        Iterable<?> results = context.renderNode("/");
        Object firstResult = results.iterator().next();
        assertEquals(true, firstResult);
    }

    @Test
    public void testPublishedPortSerialization() {
        Node inner = Node.ROOT.withName("inner")
                .withInputAdded(Port.floatPort("value", 0.0))
                .withFunction("math/number");
        Node outer = Node.NETWORK.withName("outer")
                .withChildAdded(inner)
                .publish("inner", "value", "v")
                .withInputValue("v", 11.0);
        assertEquals(11, outer.getChild("inner").getInput("value").intValue());
        NodeLibrary originalLibrary = libraryWithChildren("test", outer);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        assertTrue(library.getNodeForPath("/outer").hasPublishedInput("v"));
        assertEquals(11, library.getNodeForPath("/outer/inner").getInput("value").intValue());
    }

    @Test
    public void testParseFormatVersion() {
        assertEquals("1.0", NodeLibraryUpgrades.parseFormatVersion("<ndbx formatVersion='1.0'>"));
        assertEquals("2", NodeLibraryUpgrades.parseFormatVersion("<ndbx type=\"file\" formatVersion=\"2\">"));
    }

    /**
     * Test if a file upgrade succeeds.
     */
    @Test
    public void testUpgrade1to2() {
        File version1File = new File("src/test/files/upgrade-v1.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version1File);
        assertTrue("Result should contain updated position: " + result.getXml(), result.getXml().contains("position=\"12.00,2.00\""));
        NodeLibrary upgradedLibrary = result.getLibrary(version1File, NodeRepository.of());
        Node root = upgradedLibrary.getRoot();
        Node alpha = root.getChild("alpha");
        assertEquals(new Point(12, 2), alpha.getPosition());
    }

    @Test
    public void testUpgrade2to3() {
        File version2File = new File("src/test/files/upgrade-v2.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version2File);
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version2File, NodeRepository.of(mathLibrary));
        Node root = upgradedLibrary.getRoot();
        assertTrue(root.hasChild("round2"));
        Node round2 = root.getChild("round2");
        assertEquals("round", round2.getPrototype().getName());
        Node subnet1 = root.getChild("subnet1");
        assertTrue(subnet1.hasChild("round1"));
        assertEquals("round1", subnet1.getRenderedChildName());
        Port value = subnet1.getInput("value");
        assertEquals("round1.value", value.getChildReference());
    }

    @Test
    public void testUpgrade3to4() {
        File version3File = new File("src/test/files/upgrade-v3.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version3File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary listLibrary = NodeLibrary.load(new File("libraries/list/list.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version3File, NodeRepository.of(corevectorLibrary, listLibrary));
        Node root = upgradedLibrary.getRoot();
        assertEquals("make_point", root.getChild("point1").getPrototype().getName());
        assertTrue(root.hasChild("point2"));
        assertEquals("point", root.getChild("point2").getPrototype().getName());
        assertTrue(root.getConnections().contains(new Connection("point2", "count1", "list")));
        Node subnet1 = root.getChild("subnet1");
        assertTrue(subnet1.hasChild("point1"));
        assertEquals("point1", subnet1.getRenderedChildName());
        Port value = subnet1.getInput("shape");
        assertEquals("point1.value", value.getChildReference());
    }

    @Test
    public void testUpgrade4to5() {
        File version4File = new File("src/test/files/upgrade-v4.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version4File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version4File, NodeRepository.of(corevectorLibrary, mathLibrary));
        Node root = upgradedLibrary.getRoot();
        assertFalse(root.getChild("textpath1").hasInput("height"));
        assertNull(root.getConnection("textpath2", "height"));
        Node subnet1 = root.getChild("subnet1");
        assertFalse(subnet1.hasInput("height"));
        Node subnet2 = root.getChild("subnet2");
        assertFalse(subnet2.hasInput("height"));
    }

    @Test
    public void testUpgrade5to6() {
        File version5File = new File("src/test/files/upgrade-v5.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version5File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version5File, NodeRepository.of(corevectorLibrary));
        Node root = upgradedLibrary.getRoot();
        Node delete1 = root.getChild("delete1");
        assertTrue(delete1.hasInput("operation"));
        assertFalse(delete1.hasInput("delete_selected"));
        assertEquals("selected", delete1.getInput("operation").stringValue());
        Node delete2 = root.getChild("delete2");
        assertEquals("non-selected", delete2.getInput("operation").stringValue());
    }

    @Test
    public void testUpgrade6to7() {
        File version6File = new File("src/test/files/upgrade-v6.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version6File);
        NodeLibrary listLibrary = NodeLibrary.load(new File("libraries/list/list.ndbx"), NodeRepository.of());
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version6File, NodeRepository.of(listLibrary, mathLibrary));
        Node root = upgradedLibrary.getRoot();
        assertFalse(root.hasChild("filter1"));
        assertTrue(root.hasChild("cull1"));
        assertEquals("cull1", root.getRenderedChildName());
        assertResultsEqual(root, 0.0, 2.0, 4.0, 6.0, 8.0);
    }

    @Test
    public void testUpgrade7to8() {
        File version7File = new File("src/test/files/upgrade-v7.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version7File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version7File, NodeRepository.of(corevectorLibrary, mathLibrary));
        Node root = upgradedLibrary.getRoot();
        assertFalse(root.getChild("point_on_path1").hasInput("range"));
        assertNull(root.getConnection("point_on_path2", "range"));
        Node subnet1 = root.getChild("subnet1");
        assertFalse(subnet1.hasInput("range"));
    }

    @Test
    public void testUpgrade8to9() {
        File version8File = new File("src/test/files/upgrade-v8.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version8File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version8File, NodeRepository.of(corevectorLibrary));
        Node root = upgradedLibrary.getRoot();
        assertTrue(root.hasChild("resample2"));
        assertTrue(root.hasChild("resample3"));
        assertEquals("resample", root.getChild("resample2").getPrototype().getName());
        assertEquals("amount", root.getChild("resample2").getInput("method").getValue());
        assertEquals("resample", root.getChild("resample3").getPrototype().getName());
        assertEquals("length", root.getChild("resample3").getInput("method").getValue());
        assertEquals("rect1", root.getConnection("resample2", "shape").getOutputNode());
        assertEquals("rect1", root.getConnection("resample3", "shape").getOutputNode());
        assertTrue(root.hasChild("subnet1"));
        Node subnet1 = root.getChild("subnet1");
        assertEquals("resample1", subnet1.getRenderedChildName());
        assertEquals("resample", subnet1.getChild("resample1").getPrototype().getName());
        assertEquals("amount", subnet1.getChild("resample1").getInput("method").getValue());
        assertEquals("resample1.shape", subnet1.getInput("shape").getChildReference());
        Node subnet2 = root.getChild("subnet2");
        assertEquals("resample1", subnet2.getRenderedChildName());
        assertEquals("resample", subnet2.getChild("resample1").getPrototype().getName());
        assertEquals("length", subnet2.getChild("resample1").getInput("method").getValue());
        assertEquals("resample1.shape", subnet2.getInput("shape").getChildReference());
    }

    @Test
    public void testUpgrade9to10() {
        File version9File = new File("src/test/files/upgrade-v9.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version9File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version9File, NodeRepository.of(corevectorLibrary));
        Node root = upgradedLibrary.getRoot();
        assertTrue(root.hasChild("wiggle2"));
        assertTrue(root.hasChild("wiggle3"));
        assertTrue(root.hasChild("wiggle4"));
        assertEquals("wiggle", root.getChild("wiggle2").getPrototype().getName());
        assertEquals("contours", root.getChild("wiggle2").getInput("scope").getValue());
        assertEquals("paths", root.getChild("wiggle3").getInput("scope").getValue());
        assertEquals("points", root.getChild("wiggle4").getInput("scope").getValue());
        assertEquals(7, root.getConnections().size());
        assertEquals("textpath1", root.getConnection("wiggle2", "shape").getOutputNode());
        assertTrue(root.hasChild("subnet1"));
        assertEquals("wiggle1", root.getChild("subnet1").getRenderedChildName());
        assertEquals("wiggle", root.getChild("subnet2").getChild("wiggle1").getPrototype().getName());
        assertEquals("contours", root.getChild("subnet1").getChild("wiggle1").getInput("scope").getValue());
        assertEquals("paths", root.getChild("subnet2").getChild("wiggle1").getInput("scope").getValue());
        assertEquals("points", root.getChild("subnet3").getChild("wiggle1").getInput("scope").getValue());
        assertEquals("wiggle1.shape", root.getChild("subnet3").getInput("shape").getChildReference());
    }

    @Test
    public void testUpgrade10to11() {
        File version10File = new File("src/test/files/upgrade-v10.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version10File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary stringLibrary = NodeLibrary.load(new File("libraries/string/string.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version10File, NodeRepository.of(corevectorLibrary, stringLibrary));
        Node root = upgradedLibrary.getRoot();
        assertFalse(root.hasChild("draw_path1"));
        assertFalse(root.hasChild("draw_path2"));
        Node subnet1 = root.getChild("subnet1");
        assertFalse(subnet1.hasRenderedChild());
        assertFalse(subnet1.hasInput("path"));
        assertEquals(1, subnet1.getChildren().size());
        assertEquals(0, root.getConnections().size());
    }

    @Test
    public void testUpgrade11to12() {
        File version11File = new File("src/test/files/upgrade-v11.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version11File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version11File, NodeRepository.of(corevectorLibrary, mathLibrary));
        Node root = upgradedLibrary.getRoot();
        Node shapeOnPath1Node = root.getChild("shape_on_path1");
        assertNotNull(shapeOnPath1Node);
        assertTrue(shapeOnPath1Node.hasInput("path"));
        assertEquals(10L, shapeOnPath1Node.getInput("amount").getValue());
        assertEquals(15.0, shapeOnPath1Node.getInput("spacing").getValue());
        assertEquals(10.0, shapeOnPath1Node.getInput("margin").getValue());
        assertEquals("rect1", root.getConnection("shape_on_path2", "path").getOutputNode());
        assertEquals("number1", root.getConnection("shape_on_path2", "spacing").getOutputNode());
        assertEquals("number1", root.getConnection("shape_on_path2", "margin").getOutputNode());
        Node subnet = root.getChild("subnet1");
        assertEquals("shape_on_path1.path", subnet.getInput("template").getChildReference());
        assertEquals("shape_on_path1.spacing", subnet.getInput("dist").getChildReference());
        assertEquals("shape_on_path1.margin", subnet.getInput("start").getChildReference());
        assertEquals("rect1", root.getConnection("subnet1", "template").getOutputNode());
        assertEquals("number1", root.getConnection("subnet1", "dist").getOutputNode());
        assertEquals("number1", root.getConnection("subnet1", "start").getOutputNode());
    }

    @Test
    public void testUpgrade12to13() {
        File version12File = new File("src/test/files/upgrade-v12.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version12File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version12File, NodeRepository.of(corevectorLibrary, mathLibrary));
        Node root = upgradedLibrary.getRoot();
        Node textOnPath1Node = root.getChild("text_on_path1");
        assertNotNull(textOnPath1Node);
        assertTrue(textOnPath1Node.hasInput("path"));
        assertEquals(10.0, textOnPath1Node.getInput("margin").getValue());
        assertEquals(5.0, textOnPath1Node.getInput("baseline_offset").getValue());
        assertFalse(textOnPath1Node.hasInput("keep_geometry"));
        assertEquals("rect1", root.getConnection("text_on_path2", "path").getOutputNode());
        assertEquals("number1", root.getConnection("text_on_path2", "margin").getOutputNode());
        assertEquals("number2", root.getConnection("text_on_path2", "baseline_offset").getOutputNode());
        Node subnet = root.getChild("subnet1");
        assertEquals("text_on_path1.path", subnet.getInput("shape").getChildReference());
        assertEquals("text_on_path1.margin", subnet.getInput("position").getChildReference());
        assertEquals("text_on_path1.baseline_offset", subnet.getInput("offset").getChildReference());
        assertFalse(subnet.hasInput("keep_geometry"));
        assertEquals("rect1", root.getConnection("subnet1", "shape").getOutputNode());
        assertEquals("number1", root.getConnection("subnet1", "position").getOutputNode());
        assertEquals("number2", root.getConnection("subnet1", "offset").getOutputNode());
        assertNull(root.getConnection("subnet1", "keep_geometry"));
    }

    @Test
    public void testUpgrade13to14() {
        File version13File = new File("src/test/files/upgrade-v13.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version13File);
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version13File, NodeRepository.of(mathLibrary));
        Node root = upgradedLibrary.getRoot();
        Node waveNode = root.getChild("wave1");
        assertFalse(waveNode.hasInput("speed"));
        assertFalse(waveNode.hasInput("frame"));
        assertEquals(100.0, waveNode.getInput("period").getValue());
        assertEquals(20.0, waveNode.getInput("offset").getValue());
    }

    @Test
    public void testUpgrade14to15() {
        File version14File = new File("src/test/files/upgrade-v14.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version14File);
        NodeLibrary stringLibrary = NodeLibrary.load(new File("libraries/string/string.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version14File, NodeRepository.of(stringLibrary));
        Node root = upgradedLibrary.getRoot();
        assertTrue(root.hasChild("split1"));
        assertEquals("split1", root.getConnection("length1", "string").getOutputNode());
        Node subnet = root.getChild("subnet1");
        assertEquals("split1", subnet.getRenderedChildName());
        assertEquals("split1.string", subnet.getInput("string").getChildReference());
        assertEquals("string1", root.getConnection("subnet1", "string").getOutputNode());
        assertEquals("subnet1", root.getConnection("length2", "string").getOutputNode());
    }

    @Test
    public void testUpgrade15to16() {
        File version15File = new File("src/test/files/upgrade-v15.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version15File);
        NodeLibrary mathLibrary = NodeLibrary.load(new File("libraries/math/math.ndbx"), NodeRepository.of());
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version15File, NodeRepository.of(mathLibrary, corevectorLibrary));
        Node root = upgradedLibrary.getRoot();
        assertEquals("root", root.getName());
        assertEquals("network2", root.getRenderedChildName());
        assertEquals("number", root.getChild("node3").getPrototype().getName());
        assertEquals("node3", root.getConnection("add1", "value1").getOutputNode());
        assertEquals(5.0, root.getChild("node2").getInput("value").getValue());
        assertEquals("abs", root.getChild("root123").getPrototype().getName());
        assertEquals(11.0, root.getChild("network1").getChild("node1").getInput("value").getValue());
        assertEquals("node1", root.getChild("network1").getRenderedChildName());
        assertEquals("node1", root.getChild("network2").getRenderedChildName());
        assertEquals("node2", root.getChild("network2").getChild("node1").getRenderedChildName());
        assertEquals(17.0, root.getChild("network2").getChild("node1").getChild("node2").getInput("value").getValue());
        assertEquals(Node.NETWORK, root.getChild("geonet1").getPrototype());
        assertEquals("geometry", root.getChild("geonet1").getOutputType());
    }

    @Test
    public void testUpgrade16to17() {
        File version16File = new File("src/test/files/upgrade-v16.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version16File);
        NodeLibrary upgradedLibrary = result.getLibrary(version16File, NodeRepository.of());
        assertFalse(upgradedLibrary.hasProperty("oscPort"));
        assertTrue(upgradedLibrary.hasDevice("osc1"));
        assertEquals(1, upgradedLibrary.getDevices().size());
        assertEquals("2084", upgradedLibrary.getDevices().get(0).getProperties().get("port"));
        assertEquals("true", upgradedLibrary.getDevices().get(0).getProperties().get("sync_with_timeline")); // was: autostart
    }

    @Test
    public void testUpgrade18to19() {
        File version18File = new File("src/test/files/upgrade-v18.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version18File);
        NodeLibrary devicesLibrary = NodeLibrary.load(new File("libraries/device/device.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version18File, NodeRepository.of(devicesLibrary));
        Node root = upgradedLibrary.getRoot();
        assertEquals("audioplayer1", root.getChild("audio_analysis1").getInput("device_name").getValue());
        assertEquals("audioplayer2", root.getChild("audio_analysis2").getInput("device_name").getValue());
        assertEquals("audioplayer1", root.getChild("audio_wave1").getInput("device_name").getValue());
        assertEquals("audioplayer1", root.getChild("beat_detect1").getInput("device_name").getValue());
    }

    @Test
    public void testUpgrade19to20() {
        File version19File = new File("src/test/files/upgrade-v19.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version19File);
        NodeLibrary devicesLibrary = NodeLibrary.load(new File("libraries/device/device.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version19File, NodeRepository.of(devicesLibrary));
        assertEquals("false", upgradedLibrary.getDevice("audioinput1").getProperty("sync_with_timeline"));
        assertEquals("true", upgradedLibrary.getDevice("audioplayer1").getProperty("sync_with_timeline"));
        assertEquals("true", upgradedLibrary.getDevice("osc1").getProperty("sync_with_timeline"));
    }

    @Test
    public void testUpgrade20to21() {
        File version20File = new File("src/test/files/upgrade-v20.ndbx");
        UpgradeResult result = NodeLibraryUpgrades.upgrade(version20File);
        NodeLibrary corevectorLibrary = NodeLibrary.load(new File("libraries/corevector/corevector.ndbx"), NodeRepository.of());
        NodeLibrary upgradedLibrary = result.getLibrary(version20File, NodeRepository.of(corevectorLibrary));
        Node root = upgradedLibrary.getRoot();
        assertEquals(new Point(200, 150), root.getChild("copy1").getInput("scale").getValue());
        assertEquals(new Point(100, 100), root.getChild("copy2").getInput("scale").getValue());
    }
    
    /**
     * Test upgrading from 0.9 files, which should fail since we don't support those conversions.
     */
    @Test
    public void testTooOldToUpgrade() {
        File version09File = new File("src/test/files/upgrade-v0.9.ndbx");
        try {
            NodeLibraryUpgrades.upgrade(version09File);
            fail("Should have thrown a LoadException.");
        } catch (LoadException e) {
            assertTrue(e.getMessage().contains("NodeBox 2"));
        }
    }

    /**
     * Test upgrading from 999 files, which should fail since this format is too new.
     */
    @Test
    public void testTooNewToUpgrade() {
        File version999Files = new File("src/test/files/upgrade-v999.ndbx");
        try {
            NodeLibraryUpgrades.upgrade(version999Files);
            fail("Should have thrown a LoadException.");
        } catch (LoadException e) {
            assertTrue(e.getMessage().contains("too new"));
        }
    }

    @Test
    public void testDocumentProperties() {
        NodeLibrary library = NodeLibrary.create("test", Node.ROOT);
        assertFalse(library.hasProperty("alpha"));
        library = library.withProperty("alpha", "42");
        assertTrue(library.hasProperty("alpha"));
        assertEquals("42", library.getProperty("alpha"));
        library = library.withPropertyRemoved("alpha");
        assertFalse(library.hasProperty("alpha"));
        assertEquals("notFound", library.getProperty("alpha", "notFound"));
    }

    @Test
    public void testDocumentPropertiesSerialization() {
        NodeLibrary library = NodeLibrary.create("test", Node.ROOT.extend());
        library = library.withProperty("alpha", "42");
        String xml = library.toXml();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, NodeRepository.of());
        assertTrue(newLibrary.hasProperty("alpha"));
        assertEquals("42", newLibrary.getProperty("alpha"));
    }

    public Node makeLetterMenuNode() {
        MenuItem alpha = new MenuItem("a", "Alpha");
        MenuItem beta = new MenuItem("b", "Beta");
        return Node.ROOT.withName("letter")
                .withInputAdded(Port.stringPort("letter", "a", ImmutableList.of(alpha, beta)));
    }

    private void assertPointEquals(Point point, double x, double y) {
        assertEquals(x, point.getX());
        assertEquals(y, point.getY());
    }

    private void assertSingleResult(Double expected, Node node, FunctionRepository functionRepository) {
        NodeLibrary testLibrary = NodeLibrary.create("test", node, functionRepository);
        NodeContext context = new NodeContext(testLibrary);
        List<Object> values = ImmutableList.copyOf(context.renderNode("/"));
        assertEquals(1, values.size());
        assertEquals(expected, values.get(0));
    }

    /**
     * Assert that the value that goes in to the port comes out correctly in XML.
     *
     * @param originalPort The port to serialize / deserialize
     */
    private void assertPortSerialization(Port originalPort) {
        Node originalNode;
        originalNode = Node.ROOT.withInputAdded(originalPort);
        NodeLibrary originalLibrary = libraryWithChildren("test", originalNode);
        NodeLibrary library = NodeLibrary.load("test", originalLibrary.toXml(), NodeRepository.of());
        Node node = library.getRoot().getChild("node1");
        assertNotNull(node);
        Port port;
        port = node.getInput(originalPort.getName());
        assertEquals(originalPort.getName(), port.getName());
        assertEquals(originalPort.getType(), port.getType());
        assertEquals(originalPort.getValue(), port.getValue());
    }

    private NodeLibrary libraryWithChildren(String libraryName, Node... children) {
        Node root = Node.NETWORK.withName("root");
        for (Node child : children) {
            root = root.withChildAdded(child);
        }
        return NodeLibrary.create(libraryName, root, FunctionRepository.of());
    }

    /**
     * Assert that a NodeLibrary equals itself after reading and writing.
     *
     * @param library        The NodeLibrary.
     * @param nodeRepository The repository of NodeLibraries.
     */
    private void assertReadWriteEquals(NodeLibrary library, NodeRepository nodeRepository) {
        String xml = library.toXml();
        assertEquals(library, NodeLibrary.load(library.getName(), xml, nodeRepository));
    }

}
