package nodebox.node;

import junit.framework.TestCase;
import nodebox.node.polygraph.Polygon;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * All test in this class get a new NodeManager instance whenever they parse source.
 * The NodeManager has some basic types loaded in: testlib.dot and testlib.rotate.
 *
 * @see #resetManager()
 */
public class NDBXHandlerTest extends TestCase {

    public static final String NDBX_HEADER = "<ndbx formatVersion=\"0.9\">";
    public static final String NDBX_FOOTER = "</ndbx>";

    private NodeLibraryManager manager;

    /**
     * Test if the formatVersion is required.
     */
    public void testFormatVersion() {
        String xml;
        xml = "<ndbx></ndbx>";
        assertParsingFails(xml, "required attribute formatVersion");
        xml = "<ndbx formatVersion=\"2.4\"></ndbx>";
        assertParsingFails(xml, "unknown formatVersion");
        xml = NDBX_HEADER + NDBX_FOOTER;
        parseXml(xml);
    }

    public void testUnknownTag() {
        String xml = NDBX_HEADER + "<flower name=\"dandelion\"></flower>" + NDBX_FOOTER;
        assertParsingFails(xml, "unknown tag flower");
    }

    /**
     * Test if node position is persisted.
     */
    public void testPosition() {
        NodeLibrary l = new NodeLibrary("test");
        Node n = Node.ROOT_NODE.newInstance(l, "test");
        n.setPosition(25, 50);
        NodeLibrary lib = parseXml(l.toXml());
        Node test = lib.getRootNode().getChild("test");
        assertEquals(25.0, test.getX());
        assertEquals(50.0, test.getY());
    }

    /**
     * Test required attributes for node.
     */
    public void testInvalidNodeFormat() {
        assertParsingFails(NDBX_HEADER + "<node></node>" + NDBX_FOOTER, "name attribute is required");
        assertParsingFails(NDBX_HEADER + "<node name=\"dot1\"></node>" + NDBX_FOOTER, "prototype attribute is required");
        // Try loading a node with a prototype that does not exist yet.
        assertParsingFails(NDBX_HEADER + "<node name=\"dot1\" prototype=\"testlib.xxxx\"></node>" + NDBX_FOOTER, "xxx");
        // Assert that parsing the load does not store the nodes.
        assertFalse(manager.hasNode("dot1"));
        // Parse and include the basic types.
        parseXml(NDBX_HEADER + "<node name=\"dot1\" prototype=\"testlib.dot\"></node>" + NDBX_FOOTER);
        // Try loading a node with an existing name (but in a different namespace). (Include basic types).
        parseXml(NDBX_HEADER + "<node name=\"dot\" prototype=\"testlib.dot\"></node>" + NDBX_FOOTER);
    }

    public void testInvalidParameterFormat() {
        String NODE_HEADER = NDBX_HEADER + "<node name=\"dot1\" prototype=\"testlib.dot\">";
        String NODE_FOOTER = "</node>" + NDBX_FOOTER;
        // Name is required
        assertParsingFails(NODE_HEADER + "<param></param>" + NODE_FOOTER, "");
        // Strictly speaking, mentioning an existing parameter is not invalid, just useless.
        parseXml(NODE_HEADER + "<param name=\"x\"/>" + NODE_FOOTER);
        // Unknown name, and no value or type given
        assertParsingFails(NODE_HEADER + "<param name=\"test\"></param>" + NODE_FOOTER, "does not exist");
        // Unknown name, and no type given
        assertParsingFails(NODE_HEADER + "<param name=\"test\"><value>hello</value></param>" + NODE_FOOTER, "does not exist");
        // Valid name, but value is of wrong type
        assertParsingFails(NODE_HEADER + "<param name=\"x\"><value>hello</value></param>" + NODE_FOOTER, "could not parse");
        // Valid name, but value is in invalid tag
        assertParsingFails(NODE_HEADER + "<param name=\"x\"><float>hello</float></param>" + NODE_FOOTER, "unknown tag float");
        // Type parameter indicates a new parameter needs to be created, but a parameter with this name already exists
        assertParsingFails(NODE_HEADER + "<param name=\"x\" type=\"string\"><value>hello</value></param>" + NODE_FOOTER, "already exists");
        // Same as above, but type is now the same as prototype's. This should not make a difference though.
        assertParsingFails(NODE_HEADER + "<param name=\"x\" type=\"float\"><value>20.0</value></param>" + NODE_FOOTER, "already exists");
        // Unknown name, but type and value given, so new parameter was created.
        parseXml(NODE_HEADER + "<param name=\"test\" type=\"string\"><value>hello</value></param>" + NODE_FOOTER);
    }

    public void testInvalidCode() {
        String NODE_HEADER = NDBX_HEADER + "<node name=\"dot1\" prototype=\"testlib.dot\"><param name=\"_code\">";
        String NODE_FOOTER = "</param></node>" + NDBX_FOOTER;
        // Value tags for code need a type parameter.
        assertParsingFails(NODE_HEADER + "<value>print 'hello'</value>" + NODE_FOOTER, "type attribute is required");
        // We do not support Cobol (yet).
        assertParsingFails(NODE_HEADER + "<value type=\"cobol\">PROCEDURE DIVISION.\nDisplayPrompt.\n    DISPLAY \"Hello, World!\".\n    STOP RUN.\n</value>" + NODE_FOOTER, "invalid type attribute");
        // TODO: Test CDATA formatting.
    }

    public void testInvalidConnectionFormat() {
        String NODE_HEADER = NDBX_HEADER +
                "<node name=\"dot1\" prototype=\"testlib.dot\"></node>" +
                "<node name=\"rotate1\" prototype=\"testlib.rotate\"></node>";
        String NODE_FOOTER = NDBX_FOOTER;
        // Output is required
        assertParsingFails(NODE_HEADER + "<conn/>" + NODE_FOOTER, "output is required");
        // Input is required
        assertParsingFails(NODE_HEADER + "<conn output=\"dot1\"/>" + NODE_FOOTER, "input is required");
        // Input port is required
        assertParsingFails(NODE_HEADER + "<conn output=\"dot1\" input=\"rotate1\"/>" + NODE_FOOTER, "port is required");
        // Correct syntax
        parseXml(NODE_HEADER + "<conn output=\"dot1\" input=\"rotate1\" port=\"shape\"/>" + NODE_FOOTER);
        // Invalid output/input/port
        assertParsingFails(NODE_HEADER + "<conn output=\"unknown\" input=\"rotate1\" port=\"shape\"/>" + NODE_FOOTER, "does not exist");
        assertParsingFails(NODE_HEADER + "<conn output=\"dot1\" input=\"unknown\" port=\"shape\"/>" + NODE_FOOTER, "does not exist");
        assertParsingFails(NODE_HEADER + "<conn output=\"dot1\" input=\"rotate1\" port=\"unknown\"/>" + NODE_FOOTER, "does not exist");
    }

    /**
     * Load a test file that contains only instances, not new nodes, and no parameters.
     */
    public void testOnlyDefaults() {
        String xml = NDBX_HEADER + "<node name=\"dot1\" prototype=\"testlib.dot\" type=\"nodebox.node.polygraph.Polygon\"></node>" + NDBX_FOOTER;
        NodeLibrary library = parseXml(xml);
        Node protoDot = manager.getNode("testlib.dot");
        assertTrue(library.contains("dot1"));
        Node dot1 = library.getRootNode().getChild("dot1");
        assertEquals(protoDot, dot1.getPrototype());
        // Since dot1 inherits from the prototype, it has all the parameters of the prototype.
        assertTrue(dot1.hasParameter("x"));
        assertTrue(dot1.hasParameter("y"));
        // This is really an implementation detail. We should not make guarantees about the "same-ness" of parameters.
        assertNotSame(protoDot.getParameter("x"), dot1.getParameter("x"));
        assertEquals(0F, dot1.getValue("x"));
        assertEquals(0F, dot1.getValue("y"));
    }

    /**
     * Test if port types are stored/loaded correctly.
     */
    public void testPortTypes() {
        NodeLibrary typeLib = new NodeLibrary("typeLib");
        Node.ROOT_NODE.newInstance(typeLib, "alpha", Polygon.class);
        String xml = typeLib.toXml();
        NodeLibrary library = parseXml(xml);
        Node alpha = library.getRootNode().getChild("alpha");
        assertEquals(Polygon.class, alpha.getDataClass());
        // Create a new instance with the same output type.
        // Store it in a temporary node library.
        NodeLibrary betaLibrary = new NodeLibrary("xxx");
        alpha.newInstance(betaLibrary, "beta");
        String s = betaLibrary.toXml();
        // The output type is the same, so should not be persisted.
        assertFalse(s.contains("Polygon"));
        // Check if ports have their types persisted.
        Node n = Node.ROOT_NODE.newInstance(typeLib, "gamma", Polygon.class);
        n.addPort("polygon");
        xml = typeLib.toXml();
        library = parseXml(xml);
        Node gamma = library.getRootNode().getChild("gamma");
        assertEquals(Polygon.class, gamma.getDataClass());
    }

    /**
     * Test a bug where having a node with the name same as the parent stopped loading.
     */
    public void testSameNameChild() {
        resetManager();
        NodeLibrary test = new NodeLibrary("test");
        Node rect1 = Node.ROOT_NODE.newInstance(test, "rect1", Polygon.class);
        Node innerRect1 = rect1.create(Node.ROOT_NODE, "rect1");
        NodeLibrary newTest = NodeLibrary.load("newTest", test.toXml(), manager);
        Node newRect1 = newTest.getRootNode().getChild("rect1");
        assertNotNull(newRect1);
        assertNotNull(newRect1.getChild("rect1"));
    }

    //// Helper methods ////

    /**
     * Creates a new manager instance.
     */
    private void resetManager() {
        manager = new NodeLibraryManager();
        loadBasicTypes();
    }

    private void loadBasicTypes() {
        NodeLibrary testlib = new NodeLibrary("testlib");
        Node dot = Node.ROOT_NODE.newInstance(testlib, "dot", Polygon.class);
        dot.setExported(true);
        testlib.add(dot);
        dot.addParameter("x", Parameter.Type.FLOAT, 0F);
        dot.addParameter("y", Parameter.Type.FLOAT, 0F);
        Node rotate = Node.ROOT_NODE.newInstance(testlib, "rotate", Polygon.class);
        rotate.setExported(true);
        testlib.add(rotate);
        rotate.addPort("shape");
        rotate.addParameter("rotation", Parameter.Type.FLOAT, 0F);
        manager.add(testlib);
    }

    /**
     * Asserts that parsing the given XML fails with the expected message.
     * <p/>
     * The reason I added the message was that sometimes parsing fails for the wrong reason, e.g. when a closing
     * tag is missing or mistyped. By requiring part of the failing message, you can also test why parsing
     * failed.
     *
     * @param xml             the xml source
     * @param expectedMessage part of the expected error message. Case-insensitive.
     */
    private void assertParsingFails(String xml, String expectedMessage) {
        try {
            parseXml(xml);
            fail("XML should have failed to parse.");
        } catch (RuntimeException e) {
            if (e.getCause() == null) {
                e.printStackTrace();
                fail("The parsing failed with the wrong exception.");
                return;
            }
            if (e.getCause().getClass() != SAXException.class) {
                e.getCause().printStackTrace();
                fail("The parsing failed with the wrong exception: " + e.getCause());
                return;
            }
            if (!e.getCause().getMessage().toLowerCase().contains(expectedMessage.toLowerCase())) {
                // e.getCause().printStackTrace();
                fail("The parsing failed with the wrong reason: " + e.getCause().getMessage() + " (expected: " + expectedMessage + ")");
            }
        }
    }

    private NodeLibrary parseXml(String xml) {
        resetManager();
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser parser = spf.newSAXParser();
            NodeLibrary testLibrary = new NodeLibrary("test");
            NDBXHandler handler = new NDBXHandler(testLibrary, manager);
            InputStream in = new ByteArrayInputStream(xml.getBytes("UTF8"));
            parser.parse(in, handler);
            return testLibrary;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
