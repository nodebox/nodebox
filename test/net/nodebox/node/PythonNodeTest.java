package net.nodebox.node;

import net.nodebox.node.polygraph.Polygon;
import net.nodebox.node.polygraph.Rectangle;

public class PythonNodeTest extends NodeTestCase {

    /**
     * Test a Python node that generates a Polygon
     */
    public void testBasicGenerator() {
        Node rect1 = Node.ROOT_NODE.newInstance(testLibrary, "rect1", Polygon.class);
        rect1.addParameter("x", Parameter.Type.INT, 20);
        rect1.addParameter("y", Parameter.Type.INT, 30);
        rect1.addParameter("width", Parameter.Type.INT, 40);
        rect1.addParameter("height", Parameter.Type.INT, 50);
        PythonCode code = new PythonCode("from net.nodebox.node.polygraph import Polygon\n" +
                "def cook(self):\n" +
                "  return Polygon.rect(self.x, self.y, self.width, self.height)");
        rect1.setValue("_code", code);
        rect1.update();
        Polygon polygon = (Polygon) rect1.getOutputValue();
        assertEquals(new Rectangle(20, 30, 40, 50), polygon.getBounds());
    }

    /**
     * Test a Python node that filters a Polygon
     */
    public void testBasicFilter() {
        Node rect1 = rectNode.newInstance(testLibrary, "rect1");
        Node mover = Node.ROOT_NODE.newInstance(testLibrary, "mover", Polygon.class);
        mover.addPort("polygon", Polygon.class);
        mover.addParameter("tx", Parameter.Type.INT, 10);
        mover.addParameter("ty", Parameter.Type.INT, 20);
        PythonCode code = new PythonCode("def cook(self):\n" +
                "  return self.polygon.translated(self.tx, self.ty)");
        mover.setValue("_code", code);
        mover.getPort("polygon").connect(rect1);
        mover.update();
        Polygon polygon = (Polygon) mover.getOutputValue();
        assertEquals(new Rectangle(10, 20, 100, 100), polygon.getBounds());
    }

    /**
     * Test what happens when the return value of the cook() function is different from the
     * required return value.
     */
    public void testIncorrectReturnType() {
        Node rect1 = Node.ROOT_NODE.newInstance(testLibrary, "rect1", Polygon.class);
        PythonCode code = new PythonCode("def cook(self): return 'hello'");
        rect1.setValue("_code", code);
        assertProcessingError(rect1, "Value hello is not of required class");
    }

    /**
     * Test how input ports with multiple cardinality are handled.
     */
    public void testMultiPort() {
        Node multiAdd = Node.ROOT_NODE.newInstance(testLibrary, "multiAdd", Integer.class);
        Port pValues = multiAdd.addPort("values", Integer.class, Port.Cardinality.MULTIPLE);
        PythonCode code = new PythonCode("def cook(self): return sum(self.values)");
        multiAdd.setValue("_code", code);
        multiAdd.update();
        // No values were given, so the output returns 0.
        assertEquals(0, multiAdd.getOutputValue());
        Node number1 = numberNode.newInstance(testLibrary, "number1");
        Node number2 = numberNode.newInstance(testLibrary, "number2");
        Node number3 = numberNode.newInstance(testLibrary, "number3");
        number1.setValue("value", 1);
        number2.setValue("value", 3);
        number3.setValue("value", 5);
        pValues.connect(number1);
        pValues.connect(number2);
        pValues.connect(number3);
        multiAdd.update();
        assertEquals(9, multiAdd.getOutputValue());
        pValues.disconnect();
        multiAdd.update();
        assertEquals(0, multiAdd.getOutputValue());
    }

    /**
     * Test what happens if a required parameter gets removed.
     */
    public void testParameterGone() {
        Node stringIn = Node.ROOT_NODE.newInstance(testLibrary, "stringIn", String.class);
        stringIn.addParameter("string", Parameter.Type.STRING);
        PythonCode code = new PythonCode("def cook(self):\n  return self.string");
        stringIn.setValue("_code", code);
        stringIn.setValue("string", "hello");
        stringIn.update();
        assertEquals("hello", stringIn.getOutputValue());
        stringIn.removeParameter("string");
        assertProcessingError(stringIn, "Node 'test.stringIn' has no parameter or port 'string'");
    }

    public void testConnecting() {
        Node stringIn = Node.ROOT_NODE.newInstance(testLibrary, "stringIn", String.class);
        stringIn.addParameter("string", Parameter.Type.STRING);
        PythonCode stringInCode = new PythonCode("def cook(self):\n  return self.string");
        stringIn.setValue("_code", stringInCode);

        Node upper = Node.ROOT_NODE.newInstance(testLibrary, "upper", String.class);
        upper.addPort("string", String.class);
        PythonCode upperCode = new PythonCode("def cook(self):\n  return self.string.upper()");
        upper.setValue("_code", upperCode);

        upper.getPort("string").connect(stringIn);
        stringIn.setValue("string", "hello");
        upper.update();
        assertEquals("HELLO", upper.getOutputValue());
    }

}
