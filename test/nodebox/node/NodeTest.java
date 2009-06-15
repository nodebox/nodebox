/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */
package nodebox.node;

import nodebox.graphics.BezierPath;

public class NodeTest extends NodeTestCase {

    private class TestAttributeListener implements NodeAttributeListener {
        public int libraryCounter, nameCounter, positionCounter, descriptionCounter, parameterCounter;

        public void attributeChanged(Node source, Attribute attribute) {
            switch (attribute) {
                case LIBRARY:
                    ++libraryCounter;
                    break;
                case NAME:
                    ++nameCounter;
                    break;
                case POSITION:
                    ++positionCounter;
                    break;
                case DESCRIPTION:
                    ++descriptionCounter;
                    break;
                case PARAMETER:
                    ++parameterCounter;
                    break;
            }
        }
    }

    private class TestDirtyListener implements DirtyListener {
        public int dirtyCounter, updatedCounter;

        public void nodeDirty(Node node) {
            ++dirtyCounter;
        }

        public void nodeUpdated(Node node, ProcessingContext context) {
            ++updatedCounter;
        }
    }

    public void testBaseNode() {
        Node baseNode = Node.ROOT_NODE;
        Parameter pCode = baseNode.getParameter("_code");
        Parameter pHandle = baseNode.getParameter("_handle");
        assertEquals("_code", pCode.getName());
        assertEquals(Parameter.Type.CODE, pCode.getType());
        assertEquals("_handle", pHandle.getName());
        assertEquals(Parameter.Type.CODE, pHandle.getType());
        baseNode.update();
        assertNull(baseNode.getOutputValue());
    }

    public void testBasicClone() {
        Node myNode = Node.ROOT_NODE.newInstance(testLibrary, "myNode");
        myNode.setValue("_code", new JavaMethodWrapper(getClass(), "_addParameter"));
        myNode.update();
        assertTrue(myNode.hasParameter("myparam"));
        assertEquals("myvalue", myNode.getValue("myparam"));
    }

    public void testBasicUsage() {
        Node dotNode = Node.ROOT_NODE.newInstance(testLibrary, "dotNode");
        dotNode.addParameter("x", Parameter.Type.FLOAT);
        dotNode.addParameter("y", Parameter.Type.FLOAT);
        dotNode.setValue("_code", new JavaMethodWrapper(getClass(), "_dot"));
        dotNode.addParameter("_output", Parameter.Type.STRING);

        // Check default values
        assertEquals(0F, dotNode.getValue("x"));
        assertEquals(0F, dotNode.getValue("y"));
        assertEquals("", dotNode.getValue("_output"));

        // Process
        dotNode.update();
        assertEquals("dot(0.0,0.0)", dotNode.getOutputValue());

        // Create instance and change values
        Node dotInstance = dotNode.newInstance(testLibrary, "dotInstance");
        dotInstance.setValue("x", 25F);
        dotInstance.setValue("y", 42F);
        dotInstance.update();
        assertEquals("dot(25.0,42.0)", dotInstance.getOutputValue());

        // Check that original hasn't changed
        assertEquals(0F, dotNode.asFloat("x"));
        assertEquals(0F, dotNode.asFloat("y"));
        assertEquals("dot(0.0,0.0)", dotNode.getOutputValue());

        // Now let the instance use its own code
        dotInstance.setValue("_code", new JavaMethodWrapper(getClass(), "_dot2"));
        dotInstance.update();
        assertEquals("dot2(25.0,42.0)", dotInstance.getOutputValue());
    }

    public void testGetValue() {
        // Inheritance: A <- B <- C
        Node nodeA = Node.ROOT_NODE.newInstance(testLibrary, "A");
        nodeA.addParameter("a", Parameter.Type.FLOAT, 1F);
        Node nodeB = nodeA.newInstance(testLibrary, "B");
        nodeB.addParameter("b", Parameter.Type.FLOAT, 2F);
        Node nodeC = nodeB.newInstance(testLibrary, "C");
        nodeC.addParameter("c", Parameter.Type.FLOAT, 3F);
        assertEquals(1F, nodeC.asFloat("a"));
        assertEquals(2F, nodeC.asFloat("b"));
        assertEquals(3F, nodeC.asFloat("c"));
    }

    public void testSetValue() {
        // Inheritance: A <- B <- C
        Node nodeA = Node.ROOT_NODE.newInstance(testLibrary, "A");
        nodeA.addParameter("a", Parameter.Type.FLOAT, 1F);
        Node nodeB = nodeA.newInstance(testLibrary, "B");
        nodeB.addParameter("b", Parameter.Type.FLOAT, 2F);
        Node nodeC = nodeB.newInstance(testLibrary, "C");
        nodeC.addParameter("c", Parameter.Type.FLOAT, 3F);
        nodeC.setValue("a", 10F);
        nodeC.setValue("b", 20F);
        nodeC.setValue("c", 30F);
        assertEquals(1F, nodeA.asFloat("a"));
        assertEquals(2F, nodeB.asFloat("b"));
        assertEquals(10F, nodeC.asFloat("a"));
        assertEquals(20F, nodeC.asFloat("b"));
        assertEquals(30F, nodeC.asFloat("c"));
    }

    /**
     * Test propagation behaviour for parameters.
     */
    public void testParameterPropagation() {
        // Inheritance: A <- B
        Node nodeA = Node.ROOT_NODE.newInstance(testLibrary, "A");
        nodeA.addParameter("f", Parameter.Type.FLOAT, 1F);
        Node nodeB = nodeA.newInstance(testLibrary, "B");
        // The parameters of A and B are not the same.
        assertNotSame(nodeA.getParameter("f"), nodeB.getParameter("f"));

        nodeA.setValue("f", 10F);
        // The value for the B parameter doesn't automatically change when A was changed.
        assertEquals(10F, nodeA.asFloat("f"));
        assertEquals(1F, nodeB.asFloat("f"));
        // Setting the value of B does not affect the value of A.
        nodeB.getParameter("f").setValue(55F);
        assertEquals(10F, nodeA.asFloat("f"));
        assertEquals(55F, nodeB.asFloat("f"));
        // Reverting to the default value will force B to load the new parameter value
        // from the prototype.
        nodeB.getParameter("f").revertToDefault();
        assertEquals(10F, nodeB.asFloat("f"));
    }

    /**
     * Test if the attributes on ports are set correctly.
     */
    public void testPortAttributes() {
        Node nodeA = Node.ROOT_NODE.newInstance(testLibrary, "A");
        Port outputPort = nodeA.getOutputPort();
        assertEquals("output", outputPort.getName());
        assertEquals(Port.Direction.OUT, outputPort.getDirection());
        assertEquals(Object.class, outputPort.getDataClass());
        assertEquals(null, outputPort.getValue());
        Port stringPort = nodeA.addPort("stringPort", String.class);
        assertEquals("stringPort", stringPort.getName());
        assertEquals(Port.Direction.IN, stringPort.getDirection());
        assertEquals(String.class, stringPort.getDataClass());
        assertEquals(null, stringPort.getValue());
    }

    /**
     * Test if ports are copied from the prototype to the new instance.
     */
    public void testPortPropagation() {
        Node nodeA = Node.ROOT_NODE.newInstance(testLibrary, "A");
        nodeA.addPort("path", BezierPath.class);
        Node nodeB = nodeA.newInstance(testLibrary, "B");
        assertTrue(nodeB.hasPort("path"));
    }

    /**
     * Test parent/child relationships
     */
    public void testChildNodes() {
        Node net = Node.ROOT_NODE.newInstance(testLibrary, "net");
        Node rect = Node.ROOT_NODE.newInstance(testLibrary, "rect");
        rect.setParent(net);
        assertTrue(net.contains("rect"));
    }

    public void testParameters() {
        Node n = Node.ROOT_NODE;
        assertNull(n.getParameter("p1"));
        assertTrue(n.hasParameter("_code"));
        assertNotNull(n.getParameter("_code"));
        assertNull(n.getParameter("x"));
    }

    public void testNodeNaming() {
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "test1");
        assertInvalidName(n, "1234", "names cannot start with a digit.");

        assertInvalidName(n, "node", "names can not be one of the reserved words.");
        assertInvalidName(n, "root", "names can not be one of the reserved words.");
        assertInvalidName(n, "network", "names can not be one of the reserved words.");
        assertInvalidName(n, "context", "names can not be one of the reserved words.");

        assertInvalidName(n, "__reserved", "names cannot start with double underscores");
        assertInvalidName(n, "what!", "Only lowercase, numbers and underscore are allowed");
        assertInvalidName(n, "$-#34", "Only lowercase, numbers and underscore are allowed");
        assertInvalidName(n, "", "names cannot be empty");
        assertInvalidName(n, "very_very_very_very_very_very_long_name", "names cannot be longer than 30 characters");

        assertValidName(n, "radius");
        assertValidName(n, "_test");
        assertValidName(n, "_");
        assertValidName(n, "_1234");
        assertValidName(n, "a1234");
        assertValidName(n, "node1");
        assertValidName(n, "UPPERCASE");
        assertValidName(n, "uPpercase");
    }

    public void testDirty() {
        Node n = numberNode.newInstance(testLibrary, "number1");
        assertTrue(n.isDirty());
        n.update();
        assertFalse(n.isDirty());
        n.setValue("value", 12);
        assertTrue(n.isDirty());
        n.update();
        assertFalse(n.isDirty());
        n.getParameter("value").set(13);
        assertTrue(n.isDirty());
        n.update();
        assertFalse(n.isDirty());
    }

    public void testError() {
        Node bad = addDirectNode.newInstance(testLibrary, "bad");
        TestDirtyListener listener = new TestDirtyListener();
        bad.addDirtyListener(listener);
        bad.setValue("v1", 12);
        bad.setValue("v2", 3);
        // Since the node starts out as dirty, setting values doesn't increase the counter.
        assertEquals(0, listener.dirtyCounter);
        // This code inherits the default code, which doesn't throw an error.
        bad.update();
        assertEquals(15, bad.getOutputValue());
        // Updating the code marks it as clean.
        assertFalse(bad.isDirty());
        assertEquals(1, listener.updatedCounter);
        assertEquals(0, listener.dirtyCounter);
        // This code causes a division by zero.
        bad.setValue("_code", new PythonCode("def cook(self):\n  return 1 / 0"));
        assertEquals(1, listener.dirtyCounter);
        // We just changed a parameter value, so the node is dirty.
        assertTrue(bad.isDirty());
        // Processing will fail.
        assertProcessingError(bad, "integer division or modulo by zero");
        // After processing failed, events are still called,
        // and the node is marked clean. Output is set to null.
        assertFalse(bad.isDirty());
        assertNull(bad.getOutputValue());
        assertEquals(2, listener.updatedCounter);
        assertEquals(1, listener.dirtyCounter);
    }

    public void testCopyWithUpstream() {
        Node net = testNetworkNode.newInstance(testLibrary, "net1");
        Node number1 = net.create(numberNode);
        Node number2 = net.create(numberNode);
        Node add1 = net.create(addNode);
        assertEquals("number1", number1.getName());
        assertEquals("number2", number2.getName());
        assertEquals("add1", add1.getName());
        add1.getPort("v1").connect(number1);
        add1.getPort("v2").connect(number2);

        // TODO: Implement copyNodeWithUpstream         
//        Node copiedAdd1 = add1.getParent().copyNodeWithUpstream(add1);
//        assertEquals("add1", copiedAdd1.getName());
//        Network copiedNetwork = copiedAdd1.getParent();
//        assertEquals(net.getName(), copiedNetwork.getName());
//        Node copiedNumber1 = copiedAdd1.getParent().get("number1");
//        Node copiedNumber2 = copiedAdd1.getParent().get("number2");
//        assertNotNull(copiedNumber1);
//        assertNotNull(copiedNumber2);
//        assert (copiedAdd1.isConnected());
//        assert (copiedAdd1.getParameter("v1").isConnectedTo(copiedNumber1));
//        assert (copiedAdd1.getParameter("v2").isConnectedTo(copiedNumber2));
    }

    public void testDisconnect() {
        Node net1 = testNetworkNode.newInstance(testLibrary, "net1");
        Node number1 = net1.create(numberNode);
        Node number2 = net1.create(numberNode);
        Node multiAdd1 = net1.create(multiAddNode);
        number1.setValue("value", 5);
        number2.setValue("value", 8);
        multiAdd1.getPort("values").connect(number1);
        multiAdd1.getPort("values").connect(number2);
        multiAdd1.update();
        assertFalse(multiAdd1.isDirty());
        assertEquals(2, multiAdd1.getPort("values").getValues().size());
        assertEquals(13, multiAdd1.getOutputValue());
        multiAdd1.disconnect();
        assertTrue(multiAdd1.isDirty());
        assertFalse(multiAdd1.isConnected());
        assertFalse(number1.isConnected());
        assertFalse(number2.isConnected());
        multiAdd1.update();
        assertEquals(0, multiAdd1.getPort("values").getValues().size());
        assertEquals(0, multiAdd1.getOutputValue());
    }

    public void testNodeAttributeEvent() {
        TestAttributeListener l = new TestAttributeListener();
        Node test = Node.ROOT_NODE.newInstance(testLibrary, "test");
        test.addNodeAttributeListener(l);
        // Setting the name to itself does not trigger an event.
        test.setName("test");
        assertEquals(0, l.nameCounter);
        test.setName("newname");
        assertEquals(1, l.nameCounter);
        Parameter p1 = test.addParameter("p1", Parameter.Type.FLOAT);
        assertEquals(1, l.parameterCounter);
        p1.setName("parameter1");
        assertEquals(2, l.parameterCounter);
        // TODO: These trigger ParameterAttributeChanged
        //p1.setBoundingMethod(Parameter.BoundingMethod.HARD);
        //assertEquals(3, l.parameterCounter);
        //p1.setMinimumValue(0F);
        //assertEquals(4, l.parameterCounter);
        // Changing the value does not trigger the event.
        // The event only happens for metadata, not data.
        // If you want to catch that, use DirtyListener.
        p1.setValue(20F);
        assertEquals(2, l.parameterCounter);
        test.removeParameter("parameter1");
        assertEquals(3, l.parameterCounter);
    }

    /**
     * Test if print messages get output.
     */
    public void testOutput() {
        ProcessingContext ctx;
        PythonCode helloCode = new PythonCode("def cook(self): print 'hello'");
        Node test = Node.ROOT_NODE.newInstance(testLibrary, "test");
        test.setValue("_code", helloCode);
        ctx = new ProcessingContext();
        test.update(ctx);
        assertEquals("hello\n", ctx.getOutput());

        // Try this in a network. All the output of the nodes should be merged.
        Node parent = Node.ROOT_NODE.newInstance(testLibrary, "parent");
        Node child = parent.create(Node.ROOT_NODE, "child");
        child.setValue("_code", helloCode);
        child.setRendered();
        ctx = new ProcessingContext();
        parent.update(ctx);
        assertEquals("hello\n", ctx.getOutput());
    }


    //// Helper functions ////

    private void assertInvalidName(Node n, String newName, String reason) {
        try {
            n.setName(newName);
            fail("the following condition was not met: " + reason);
        } catch (InvalidNameException ignored) {
        }
    }

    private void assertValidName(Node n, String newName) {
        try {
            n.setName(newName);
        } catch (InvalidNameException e) {
            fail("The name \"" + newName + "\" should have been accepted.");
        }
    }

    //// Test node code ////

    public static void _addParameter(Node node, ProcessingContext ctx) {
        node.addParameter("myparam", Parameter.Type.STRING, "myvalue");
    }

    public static String _dot(Node node, ProcessingContext ctx) {
        double x = node.asFloat("x");
        double y = node.asFloat("y");
        return "dot(" + x + "," + y + ")";
    }

    public static String _dot2(Node node, ProcessingContext ctx) {
        double x = node.asFloat("x");
        double y = node.asFloat("y");
        return "dot2(" + x + "," + y + ")";
    }

    public static String _circle(Node node, ProcessingContext ctx) {
        double x = node.asFloat("x");
        double y = node.asFloat("y");
        return "circle(" + x + "," + y + ")";
    }

}
