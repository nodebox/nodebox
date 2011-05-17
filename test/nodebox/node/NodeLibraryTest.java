package nodebox.node;

import junit.framework.TestCase;
import nodebox.client.PythonUtils;
import nodebox.node.polygraph.Polygon;
import nodebox.node.polygraph.Rectangle;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeLibraryTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        PythonUtils.initializePython();
    }

    /**
     * Test if changing the node name updates the correspondent mapping in the library.
     */
    public void testNodeNameChange() {
        NodeLibrary test = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(test, "alpha");
        // We export the node since we want to test the NodeLibrary#get method, which only returns exported nodes.
        alpha.setExported(true);
        test.add(alpha);
        assertEquals(alpha, test.get("alpha"));
        // now change the name
        alpha.setName("beta");
        assertEquals(alpha, test.get("beta"));
    }

    /**
     * Test if new instance creates it in the correct library.
     */
    public void testNewInstance() {
        NodeLibrary test = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(test, "alpha");
        assertTrue(test.contains("alpha"));
        assertTrue(test.getRootNode().containsChildNode("alpha"));
        assertTrue(test.getRootNode().containsChildNode(alpha));
    }

    public void testLoading() {
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary library = manager.load(new File("test/polynodes.ndbx"));
        assertTrue(library.contains("rect"));
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node rect = manager.getNode("polynodes.rect");
        Parameter pX = rect.getParameter("x");
        assertEquals(Parameter.Type.FLOAT, pX.getType());
        Node rect1 = rect.newInstance(testLibrary, "rect1");
        rect1.setValue("x", 20);
        rect1.setValue("y", 30);
        rect1.setValue("width", 40);
        try {
            rect1.setValue("height", 50);
            fail("Height has an expression set.");
        } catch (IllegalArgumentException e) {
            rect1.getParameter("height").clearExpression();
            rect1.setValue("height", 50);
        }
        rect1.update();
        Object value = rect1.getOutputValue();
        assertEquals(Polygon.class, value.getClass());
        Polygon polygon = (Polygon) value;
        assertEquals(new Rectangle(20, 30, 40, 50), polygon.getBounds());
    }


    /**
     * Test to check if a node where a parameter has a value set but in its prototype
     * contains an expression (and thus is overridden) loads without errors.
     */
    public void testLoadingOverriddenExpression() {
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary library = manager.load(new File("test/polynodes.ndbx"));
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node rect = manager.getNode("polynodes.rect");
        Node rect1 = rect.newInstance(testLibrary, "rect1");
        rect1.clearExpression("height");
        rect1.setValue("height", 120);
        rect1.setExpression("y", "x+20");
        rect1.setExported(true);
        manager = new NodeLibraryManager();
        manager.add(library);
        NodeLibrary newLibrary = null;
        try {
            newLibrary = NodeLibrary.load("test", testLibrary.toXml(), manager);
            assertNotNull(newLibrary);
            manager.add(newLibrary);
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }

        // Perform the same check for a parameter without expression but whose parent have an expression set,
        // but whose original prototype doesn't.
        rect = manager.getNode("test.rect1");
        Node rect2 = rect.newInstance(newLibrary, "rect2");
        assertEquals("x+20", rect2.getParameter("y").getExpression());
        rect2.clearExpression("y");
        rect2.setValue("y", 20);
        manager = new NodeLibraryManager();
        manager.add(library);
        manager.add(newLibrary);
        try {
            newLibrary = NodeLibrary.load("test", newLibrary.toXml(), manager);
            assertNotNull(newLibrary);
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    public void testLoadingErrors() {
        // Use one manager with the polynodes library loaded in,
        // and restore it using a manager without the polynodes library.
        NodeLibraryManager manager = new NodeLibraryManager();
        manager.load(new File("test/polynodes.ndbx"));
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node polyRect = manager.getNode("polynodes.rect");
        polyRect.newInstance(testLibrary, "myrect");
        String xml = testLibrary.toXml();

        NodeLibraryManager emptyManager = new NodeLibraryManager();
        try {
            emptyManager.load("test", xml);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().toLowerCase().contains("unknown prototype polynodes.rect"));
        }
    }

    public void testLoadingChangedType() {
        NodeLibraryManager manager = new NodeLibraryManager();
        manager.load(new File("test/polynodes.ndbx"));
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node polyRect = manager.getNode("polynodes.rect");
        Node myrect = polyRect.newInstance(testLibrary, "myrect");
        myrect.getParameter("x").setType(Parameter.Type.INT);
        String xml = testLibrary.toXml();
        assertOnlyOnce(xml, "name=\"x\"");
        try {
            manager.load("test", xml);
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    public void testLoadingChangedWidget() {
        NodeLibrary library = new NodeLibrary("lib");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha");
        alpha.addParameter("x", Parameter.Type.FLOAT);
        alpha.setValue("x", 20);
        alpha.addParameter("y", Parameter.Type.INT);
        alpha.setValue("x", 30);
        alpha.addParameter("s", Parameter.Type.STRING);
        alpha.setValue("s", "hello");
        alpha.setExported(true);
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = manager.load("newlib", library.toXml());
        Node n = manager.getNode("newlib.alpha");
        NodeLibrary testLibrary = new NodeLibrary("test");
        Node alpha1 = n.newInstance(testLibrary, "alpha1");
        alpha1.getParameter("x").setWidget(Parameter.Widget.INT);
        alpha1.getParameter("y").setWidget(Parameter.Widget.TOGGLE);
        alpha1.getParameter("s").setWidget(Parameter.Widget.MENU);
        String xml = testLibrary.toXml();
        assertOnlyOnce(xml, "name=\"x\"");
        assertOnlyOnce(xml, "name=\"y\"");
        assertOnlyOnce(xml, "name=\"s\"");
        try {
            manager.load("test", xml);
        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    /**
     * There is a difference between loading a library using a static method on NodeLibrary
     * and using the NodeManager.load(). NodeManager.load() automatically adds the library
     * to the manager, whereas NodeLibrary only uses the given manager to look up prototypes.
     * <p/>
     * This method tests the differences.
     */
    public void testStoreInLibrary() {
        NodeLibraryManager manager;
        NodeLibrary library;
        // First try loading from within the manager
        manager = new NodeLibraryManager();
        library = manager.load(new File("test/polynodes.ndbx"));
        assertTrue(manager.contains("polynodes"));
        assertTrue(library.contains("rect"));
        // Now try loading using the NodeLibrary.load static method.
        manager = new NodeLibraryManager();
        // We pass in the manager to figure out the prototypes.
        library = NodeLibrary.load(new File("test/polynodes.ndbx"), manager);
        assertFalse(manager.contains("polynodes"));
        // You can add the library yourself.
        manager.add(library);
        assertTrue(manager.contains("polynodes"));
    }

    /**
     * Test if connections are persisted.
     */
    public void testStoreConnections() {
        NodeLibrary library = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha", Polygon.class);
        Node beta = Node.ROOT_NODE.newInstance(library, "beta", Polygon.class);
        beta.addPort("polygon");
        beta.getPort("polygon").connect(alpha);
        assertTrue(alpha.isConnectedTo(beta));
        assertTrue(beta.isConnectedTo(alpha));
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", library.toXml(), manager);
        Node newAlpha = newLibrary.getRootNode().getChild("alpha");
        Node newBeta = newLibrary.getRootNode().getChild("beta");
        assertTrue(newAlpha.isConnectedTo(newBeta));
        assertTrue(newBeta.isConnectedTo(newAlpha));
    }

    /**
     * Test if expressions are persisted correctly.
     */
    public void testStoreExpressions() {
        NodeLibrary library = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha");
        alpha.addParameter("v", Parameter.Type.INT);
        alpha.setValue("v", 10);
        alpha.getParameter("v").setExpression("44 - 2");
        // Inherit from alpha.
        Node beta = alpha.newInstance(library, "beta");

        // Check if the expression tag only appears once.
        String xml = library.toXml();
        assertOnlyOnce(xml, "<expression>");

        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        Node newAlpha = newLibrary.getRootNode().getChild("alpha");
        assertEquals("44 - 2", newAlpha.getParameter("v").getExpression());
        newAlpha.update();
        assertEquals(42, newAlpha.getValue("v"));
    }

    /**
     * Test if all attributes are persisted.
     */
    public void testStoreParameterAttributes() {
        NodeLibrary library = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha", Polygon.class);
        Parameter pAngle = alpha.addParameter("angle", Parameter.Type.FLOAT, 42);
        pAngle.setWidget(Parameter.Widget.ANGLE);
        pAngle.setEnableExpression("5 > 10");
        pAngle.setMinimumValue(-360f);
        pAngle.setMaximumValue(360f);
        pAngle.setBoundingMethod(Parameter.BoundingMethod.HARD);
        Parameter pMenu = alpha.addParameter("menu", Parameter.Type.STRING, "es");
        pMenu.setWidget(Parameter.Widget.MENU);
        pMenu.addMenuItem("en", "English");
        pMenu.addMenuItem("es", "Spanish");
        Parameter pHidden = alpha.addParameter("hidden", Parameter.Type.STRING, "invisible");
        pHidden.setDisplayLevel(Parameter.DisplayLevel.HIDDEN);
        Parameter pLabel = alpha.addParameter("label", Parameter.Type.STRING, "label + help text");
        pLabel.setLabel("My Label");
        pLabel.setHelpText("My Help Text");
        // Inherit from alpha. This is used to test if prototype data is stored only once.
        alpha.newInstance(library, "beta");

        String xml = library.toXml();
        assertOnlyOnce(xml, "<param name=\"menu\"");

        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        Node newAlpha = newLibrary.getRootNode().getChild("alpha");
        Parameter newAngle = newAlpha.getParameter("angle");
        assertEquals(Parameter.Widget.ANGLE, newAngle.getWidget());
        assertEquals("5 > 10", newAngle.getEnableExpression());
        assertFalse(newAngle.isEnabled());
        assertEquals(Parameter.BoundingMethod.HARD, newAngle.getBoundingMethod());
        assertEquals(-360f, newAngle.getMinimumValue());
        assertEquals(360f, newAngle.getMaximumValue());
        Parameter newMenu = newAlpha.getParameter("menu");
        assertEquals(Parameter.Widget.MENU, newMenu.getWidget());
        Parameter.MenuItem item0 = newMenu.getMenuItems().get(0);
        Parameter.MenuItem item1 = newMenu.getMenuItems().get(1);
        assertEquals("en", item0.getKey());
        assertEquals("English", item0.getLabel());
        assertEquals("es", item1.getKey());
        assertEquals("Spanish", item1.getLabel());
        Parameter newHidden = newAlpha.getParameter("hidden");
        assertEquals(Parameter.DisplayLevel.HIDDEN, newHidden.getDisplayLevel());
        assertEquals("invisible", newHidden.getValue());
        Parameter newLabel = newAlpha.getParameter("label");
        assertEquals("My Label", newLabel.getLabel());
        assertEquals("My Help Text", newLabel.getHelpText());
    }

    /**
     * Test if child nodes are stored correctly.
     */
    public void testStoreChildren() {
        NodeLibrary library = new NodeLibrary("test");
        Node net = Node.ROOT_NODE.newInstance(library, "net", Polygon.class);
        Node alpha = net.create(Node.ROOT_NODE, "alpha", Polygon.class);
        Node beta = net.create(Node.ROOT_NODE, "beta", Polygon.class);
        Port pPolygon = beta.addPort("polygon");
        pPolygon.connect(alpha);

        String xml = library.toXml();
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        Node newNet = newLibrary.getRootNode().getChild("net");
        assertTrue(newNet.hasChildren());
        Node newAlpha = newNet.getChild("alpha");
        Node newBeta = newNet.getChild("beta");
        assertTrue(newBeta.isConnectedTo(newAlpha));
    }

    /**
     * Test if nodes are stored in a stable order.
     */
    public void testStoreOrder() {
        NodeLibrary library = new NodeLibrary("test");
        Node.ROOT_NODE.newInstance(library, "a");
        Node.ROOT_NODE.newInstance(library, "b");
        Node.ROOT_NODE.newInstance(library, "c");
        String xml = library.toXml();
        Pattern p = Pattern.compile("<node name=\"(.*?)\"");
        Matcher m = p.matcher(xml);
        m.find();
        assertEquals("a", m.group(1));
        m.find();
        assertEquals("b", m.group(1));
        m.find();
        assertEquals("c", m.group(1));
    }

    /**
     * Test if nodes are stored in a stable order, even when using prototypes.
     */
    public void testStoreOrderPrototypes() {
        NodeLibrary library = new NodeLibrary("test");
        Node z = Node.ROOT_NODE.newInstance(library, "z");
        z.newInstance(library, "a");
        z.newInstance(library, "b");
        z.newInstance(library, "c");
        String xml = library.toXml();
        Pattern p = Pattern.compile("<node name=\"(.*?)\"");
        Matcher m = p.matcher(xml);
        m.find();
        assertEquals("z", m.group(1));
        m.find();
        assertEquals("a", m.group(1));
        m.find();
        assertEquals("b", m.group(1));
        m.find();
        assertEquals("c", m.group(1));
    }

    /**
     * Test a number of sneaky characters to see if they are encoded correctly.
     */
    public void testEntityEncoding() {
        String[] testStrings = {
                "test", // A regular string, for sanity checking
                "&", // The ampersand is used to encode entities
                "\"", // Double quote needs to be escaped in XML attributes
                "\'", // Single quote could cause some problems also
                "<", // XML open tag needs to be escaped in XML text
                ">", // XML close tag needs to be escaped in XML text
                "<![CDATA[", // Beginning CDATA section
                "]]>", // End of CDATA section
                "<![CDATA[test]]>", // Full CDATA section
        };

        for (String testString : testStrings) {
            assertCanStoreValue(Parameter.Type.STRING, testString);
            assertCanStoreHelpText(testString);
        }
    }

    /**
     * Test if you can store/load a file with expression errors.
     */
    public void testStoreWithExpressionErrors() {
        NodeLibrary library = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha");
        Parameter pValue = alpha.addParameter("value", Parameter.Type.INT);
        assertCanStoreExpression(pValue, "10 + 1"); // Correct expression.
        assertCanStoreExpression(pValue, "12 + ????"); // Compilation error.
        assertCanStoreExpression(pValue, "y"); // Evaluation error: y does not exist.
        alpha.addParameter("bob", Parameter.Type.INT);
        assertCanStoreExpression(pValue, "bob"); // Correct since bob exists.
        alpha.removeParameter("bob");
        assertCanStoreExpression(pValue, "bob"); // Bob is gone, but the script still needs to save.
    }

    /**
     * Test if only nodes with the export flags show up in the manager.
     */
    public void testExportFlag() {
        NodeLibrary library = new NodeLibrary("test");
        Node exportMe = Node.ROOT_NODE.newInstance(library, "exportMe");
        exportMe.setExported(true);
        Node hideMe = Node.ROOT_NODE.newInstance(library, "hideMe");
        List<Node> exportedNodes = library.getExportedNodes();
        assertEquals(1, exportedNodes.size());
        assertEquals(exportMe, exportedNodes.get(0));
        assertTrue(exportMe.isExported());
        assertFalse(hideMe.isExported());

        // Test if the exported flag is persisted.
        String xml = library.toXml();
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        List<Node> newExportedNodes = newLibrary.getExportedNodes();
        assertEquals(1, newExportedNodes.size());
        Node newExportMe = newExportedNodes.get(0);
        assertEquals("exportMe", newExportMe.getName());
        assertTrue(newExportMe.isExported());
        // You can still access the non-exported nodes using getRootNode().getChildren()
        Node newHideMe = newLibrary.getRootNode().getChild("hideMe");
        assertEquals("hideMe", newHideMe.getName());
        assertFalse(newHideMe.isExported());
        // Try accessing through the library
        assertEquals(newExportMe, newLibrary.get("exportMe"));
        assertNull(newLibrary.get("hideMe"));


        // Test if a new instance based on this prototype loses the flag.
        NodeLibrary doc = new NodeLibrary("doc");
        Node myExportInstance = exportMe.newInstance(doc, "myExportInstance");
        assertFalse(myExportInstance.isExported());
        // Note that you can create instances of non-exported nodes as well.
        // They just don't show up in library.getExportedNodes().
        Node myHideMeInstance = hideMe.newInstance(doc, "myHideMeInstance");
        assertFalse(myHideMeInstance.isExported());
    }

    /**
     * Test if internal instances can still be loaded even if not exported.
     */
    public void testExportInternalInstances() {
        NodeLibrary library = new NodeLibrary("test");
        // Alpha and beta are both non-exported.
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha");
        Node beta = alpha.newInstance(library, "beta");
        // Store and load this library.
        String xml = library.toXml();
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        assertEquals(0, newLibrary.getExportedNodes().size());
        Node newAlpha = newLibrary.getRootNode().getChild("alpha");
        Node newBeta = newLibrary.getRootNode().getChild("beta");
        assertEquals(newAlpha, newBeta.getPrototype());
    }

    /**
     * Test if we can retrieve absolute paths.
     */
    public void testGetNodeForPath() {
        NodeLibrary library = new NodeLibrary("test");
        Node root = library.getRootNode();
        Node alpha = root.create(Node.ROOT_NODE, "alpha");
        Node beta = alpha.create(Node.ROOT_NODE, "beta");
        assertSame(root, library.getNodeForPath(root.getAbsolutePath()));
        assertSame(alpha, library.getNodeForPath(alpha.getAbsolutePath()));
        assertSame(beta, library.getNodeForPath(beta.getAbsolutePath()));
        assertSame(root, library.getNodeForPath("xxx"));
        assertSame(alpha, library.getNodeForPath("/alpha/xxx"));
        assertSame(beta, library.getNodeForPath("/alpha/beta/////"));
        // If you forget the first slash, the path cannot be interpreted.
        assertSame(root, library.getNodeForPath("alpha/beta"));
    }

    /**
     * Test the handling of external dependencies.
     */
    public void testExternalDependencies() {
        NodeLibrary library = new NodeLibrary("test");
        Node root = library.getRootNode();
        Node n = root.create(Node.ROOT_NODE, "n");
        Parameter p = n.addParameter("p", Parameter.Type.FLOAT);
        n.update();
        assertFalse(n.isDirty());

        // After setting the external dependency on the parameter, triggering the dependency makes the node dirty.
        library.addExternalDependency(p, NodeLibrary.ExternalEvent.FRAME);
        library.externalDependencyTriggered(NodeLibrary.ExternalEvent.FRAME);
        assertTrue(n.isDirty());

        // Updating the node makes it clean.
        n.update();
        assertFalse(n.isDirty());
        // After removing all external dependencies on the parameter,
        // triggering an external dependency does not make the node dirty.
        library.removeExternalDependencies(p);
        library.externalDependencyTriggered(NodeLibrary.ExternalEvent.FRAME);
        assertFalse(n.isDirty());
    }

    /**
     * Test handling dependencies on the current frame number by setting expressions.
     */
    public void testFrameDependency() {
        NodeLibrary library = new NodeLibrary("test");
        Node root = library.getRootNode();
        Node n = root.create(Node.ROOT_NODE, "n");
        Parameter p = n.addParameter("p", Parameter.Type.FLOAT);
        // Setting the expression to frame declares an external dependency on the parameter.
        p.setExpression("FRAME + 5");

        n.update();
        assertFalse(n.isDirty());

        // Changing the frame should trigger the external dependency.
        library.setFrame(100);
        assertTrue(n.isDirty());
        n.update();
        assertFalse(n.isDirty());
        assertEquals(105f, p.asFloat());

        // Clearing the expression removes the external dependency.
        p.clearExpression();
        // Update the node again to make it clean.
        n.update();
        assertFalse(n.isDirty());
        assertEquals(105f, p.asFloat());

        // Because we no longer refer to frame, setting the frame does not mark the node dirty.
        library.setFrame(200);
        assertFalse(n.isDirty());
    }

    public void testCanvasDependency() {
        NodeLibrary library = new NodeLibrary("test");
        Node root = library.getRootNode();
        Node n = root.create(Node.ROOT_NODE, "n");
        n.setRendered();
        Parameter p = n.addParameter("p", Parameter.Type.FLOAT);
        // Setting the expression to WIDTH declares an external dependency on the parameter.
        p.setExpression("WIDTH / 2");
        root.update();
        assertFalse(n.isDirty());

        // Changing the canvas values should trigger the external dependency.
        root.setValue(NodeLibrary.CANVAS_WIDTH, 500f);
        assertTrue(n.isDirty());
        n.update();
        assertFalse(n.isDirty());
        assertEquals(250f, p.asFloat());

        // Clearing the expression removes the external dependency.
        p.clearExpression();
        // Update the node again to make it clean.
        n.update();
        assertFalse(n.isDirty());
        assertEquals(250f, p.asFloat());

        // Because we no longer refer to frame, setting the frame does not mark the node dirty.
        root.setValue(NodeLibrary.CANVAS_WIDTH, 400f);
        assertFalse(n.isDirty());
    }

    /**
     * Assert that the search string only appears once in the source.
     *
     * @param source       the source string to search in
     * @param searchString the string to search for
     */
    public void assertOnlyOnce(String source, String searchString) {
        // If the first position where it appears == the last position, it only appears once.
        assertTrue(source.indexOf(searchString) >= 0 && source.indexOf(searchString) == source.lastIndexOf(searchString));
    }

    /**
     * Assert that the given text can be used as the description for a parameter.
     * This checks if storing/loading will return the same string, and no errors occur.
     *
     * @param helpText the help text
     */
    public void assertCanStoreHelpText(String helpText) {
        // Create a library and node to store the value.
        NodeLibrary library = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha", Polygon.class);
        Parameter pValue = alpha.addParameter("value", Parameter.Type.STRING);
        pValue.setHelpText(helpText);
        // Store the library to XML.
        String xml = library.toXml();
        // Load the library from the XML, and retrieve the value.
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        Node newAlpha = newLibrary.getRootNode().getChild("alpha");
        Parameter newValue = newAlpha.getParameter("value");
        assertEquals(helpText, newValue.getHelpText());
    }

    /**
     * Assert that the given value can be stored as a parameter value in a NodeBox script.
     * The original value and the restored value will be compared using equals() to support strings.
     *
     * @param type  the type for the value.
     * @param value the value.
     */
    public void assertCanStoreValue(Parameter.Type type, Object value) {
        // Create a library and node to store the value.
        NodeLibrary library = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha", Polygon.class);
        alpha.addParameter("value", type, value);
        // Store the library to XML.
        String xml = library.toXml();
        // Load the library from the XML, and retrieve the value.
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        Node newAlpha = newLibrary.getRootNode().getChild("alpha");
        assertEquals(value, newAlpha.getValue("value"));
    }

    /**
     * Assert that the given expression can be stored into the parameters without problems.
     * This checks if storing/loading will return the same expression, and no errors occur.
     *
     * @param p          the parameter
     * @param expression the expression
     */
    public void assertCanStoreExpression(Parameter p, String expression) {
        String nodeName = p.getNode().getName();
        String parameterName = p.getName();
        p.setExpression(expression);
        String xml = p.getLibrary().toXml();
        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        Node newAlpha = newLibrary.getRootNode().getChild(nodeName);
        Parameter newParameter = newAlpha.getParameter(parameterName);
        assertEquals(expression, newParameter.getExpression());
    }


}
