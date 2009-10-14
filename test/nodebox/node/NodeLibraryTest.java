package nodebox.node;

import junit.framework.TestCase;
import nodebox.node.polygraph.Polygon;
import nodebox.node.polygraph.Rectangle;

import java.io.File;

public class NodeLibraryTest extends TestCase {

    /**
     * Test if changing the node name updates the correspondent mapping in the library.
     */
    public void testNodeNameChange() {
        NodeLibrary test = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(test, "alpha");
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
        assertTrue(test.getRootNode().contains("alpha"));
        assertTrue(test.getRootNode().contains(alpha));
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
        Node newAlpha = newLibrary.get("alpha");
        Node newBeta = newLibrary.get("beta");
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
        Node newAlpha = newLibrary.get("alpha");
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
        Node beta = alpha.newInstance(library, "beta");

        String xml = library.toXml();
        assertOnlyOnce(xml, "<param name=\"menu\"");

        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", xml, manager);
        Node newAlpha = newLibrary.get("alpha");
        Parameter newAngle = newAlpha.getParameter("angle");
        assertEquals(Parameter.Widget.ANGLE, newAngle.getWidget());
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
        Node newNet = newLibrary.get("net");
        assertTrue(newNet.hasChildren());
        Node newAlpha = newNet.getChild("alpha");
        Node newBeta = newNet.getChild("beta");
        assertTrue(newBeta.isConnectedTo(newAlpha));
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
        Node newAlpha = newLibrary.get("alpha");
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
        Node newAlpha = newLibrary.get("alpha");
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
        Node newAlpha = newLibrary.get(nodeName);
        Parameter newParameter = newAlpha.getParameter(parameterName);
        assertEquals(expression, newParameter.getExpression());
    }


}
