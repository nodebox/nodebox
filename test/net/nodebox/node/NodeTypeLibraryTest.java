package net.nodebox.node;

import junit.framework.TestCase;
import net.nodebox.client.PlatformUtils;
import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Color;
import net.nodebox.graphics.Group;
import net.nodebox.graphics.Rect;
import org.python.core.PySystemState;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class NodeTypeLibraryTest extends TestCase {

    private void initJython() {
        Properties jythonProperties = new Properties();
        String jythonCacheDir = PlatformUtils.getUserDataDirectory() + PlatformUtils.SEP + "jythoncache";
        jythonProperties.put("python.cachedir", jythonCacheDir);
        PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});

    }

    public void testLoading() {
        // TODO: Python initialization should happen lazily somewhere while loading the library.
        initJython();
        // Find the "<projectDirectory>/libraries/testlib" library.
        String projectDirectory = System.getProperty("user.dir");
        File testlibPath = new File(projectDirectory + PlatformUtils.SEP + "libraries" + PlatformUtils.SEP + "testlib");
        // Create a library object. This is normally something handled by the NodeTypeLibraryManager.
        NodeTypeLibrary library;
        try {
            library = new NodeTypeLibrary("testlib", 1, 0, 0, testlibPath);
        } catch (IOException e) {
            fail("An exception occurred while loading library:" + e);
            return;
        }
        // Load the library. This can throw a multitude of exceptions.
        library.load();
        // Check if everything's there.
        // Number node type.
        NodeType numberType = library.getNodeType("number");
        ParameterType ptValue = numberType.getParameterType("value");
        assertEquals(ParameterType.Type.INT, ptValue.getType());
        // Check if the node executes.
        NodeType negateType = library.getNodeType("negate");
        Node negateNode = negateType.createNode();
        negateNode.set("value", 42);
        assertTrue(negateNode.update());
        assertEquals(-42, negateNode.getOutputValue());
        // Check node with multiple parameters.
        NodeType addType = library.getNodeType("add");
        Node addNode = addType.createNode();
        addNode.set("v1", 50);
        addNode.set("v2", 3);
        assertTrue(addNode.update());
        assertEquals(53, addNode.getOutputValue());
        // Ellipse
        NodeType ellipseType = library.getNodeType("ellipse");
        Node ellipseNode = ellipseType.createNode();
        ellipseNode.set("x", 10.0);
        ellipseNode.set("y", 20.0);
        ellipseNode.set("width", 30.0);
        ellipseNode.set("height", 40.0);
        Color color = new Color(0.1, 0.2, 0.3, 0.4);
        ellipseNode.set("color", color);
        ellipseNode.update();
        for (Node.Message m : ellipseNode.getMessages()) {
            System.out.println("m = " + m);
        }

        assertTrue(ellipseNode.update());
        Object ellipseValue = ellipseNode.getOutputValue();
        assertTrue(ellipseValue instanceof Group);
        Group ellipseGroup = (Group) ellipseValue;
        assertEquals(new Rect(10, 20, 30, 40), ellipseGroup.getBounds());
        assertEquals(1, ellipseGroup.size());
        BezierPath ellipsePath = (BezierPath) ellipseGroup.get(0);
        assertEquals(color, ellipsePath.getFillColor());
    }

}
