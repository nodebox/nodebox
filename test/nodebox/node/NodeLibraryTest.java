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
        beta.addPort("polygon", Polygon.class);
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
     * Test if expressions are persisted.
     */
    public void testStoreExpressions() {
        NodeLibrary library = new NodeLibrary("test");
        Node alpha = Node.ROOT_NODE.newInstance(library, "alpha");
        alpha.addParameter("v", Parameter.Type.INT);
        alpha.setValue("v", 10);
        alpha.getParameter("v").setExpression("44 - 2");
        // Inherit from alpha.
        Node beta = alpha.newInstance(library, "beta");

        // Roundabout way to check if the expression tag only appears once.
        // If the first position where it appears == the last position, it only appears once.
        String xml = library.toXml();
        assertTrue(xml.indexOf("<expression>") == xml.lastIndexOf("<expression>"));

        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", library.toXml(), manager);
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

        NodeLibraryManager manager = new NodeLibraryManager();
        NodeLibrary newLibrary = NodeLibrary.load("test", library.toXml(), manager);
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


}
