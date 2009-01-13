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
package net.nodebox.node;

public class ParameterTest extends NodeTestCase {

    public void testCorrectType() {
        Node number = numberType.createNode();
        ParameterType valueType = number.getParameter("value").getParameterType();
        assertEquals(ParameterType.Type.INT, valueType.getType());
        assertEquals(ParameterType.CoreType.INT, valueType.getCoreType());
    }

    public void testDirectValue() {
        Node num1 = numberType.createNode();
        assertEquals(0, num1.getOutputValue());
        Node num2 = numberType.createNode();
        num2.setValue("value", 12);
        assertEquals(0, num2.getOutputValue());
        assertFalse(num2.getParameter("value").hasExpression());
        assertFalse(num2.getParameter("value").isConnected());
    }

    public void testAsString() {
        NodeType allControlsType = new NodeType(null, "", ParameterType.Type.INT) {
            public boolean process(Node node, ProcessingContext ctx) {
                return false;
            }
        };
        allControlsType.addParameterType("int", ParameterType.Type.INT);
        allControlsType.addParameterType("float", ParameterType.Type.FLOAT);
        allControlsType.addParameterType("string", ParameterType.Type.STRING);
        allControlsType.addParameterType("color", ParameterType.Type.COLOR);
        Node n = allControlsType.createNode();
        assertEquals("0", n.asString("int"));
        assertEquals("0.0", n.asString("float"));
        assertEquals("", n.asString("string"));
        assertEquals("#000000ff", n.asString("color"));
    }

    /*

    public void testInvalidName() {
        Node n = new TestNode();
        Parameter pAlice = n.addParameter("alice", Parameter.Type.FLOAT);
        Parameter pBob = n.addParameter("bob", Parameter.Type.FLOAT);
        assertEquals(pAlice.getName(), "alice");
        assertEquals(pBob.getName(), "bob");
        checkValidName(pBob, "joe");
        assertEquals(pBob.getName(), "joe");
        assertFalse(n.hasParameter("bob"));
        assertTrue(n.hasParameter("joe"));
        checkParameterNotFound(n, "bob");
        assertEquals(pBob, n.getParameter("joe"));
        checkInvalidName(pBob, "alice", "Can not take the name of an existing parameter.");
        assertEquals(pBob, n.getParameter("joe")); // Check the previous setName hasn't affected the current name.
    }

    public void testLabel() {
        Node n = new TestNode();
        Parameter p1 = n.addParameter("width", Parameter.Type.FLOAT);
        assertEquals("Width", p1.getLabel());
        Parameter p2 = n.addParameter("a_somewhat_longer_parameter", Parameter.Type.FLOAT);
        assertEquals("A Somewhat Longer Parameter", p2.getLabel());
        Parameter p3 = n.addParameter("double__underscores__everywhere", Parameter.Type.FLOAT);
        assertEquals("Double Underscores Everywhere", p3.getLabel());
    }
*/
    public void testValues() {
        ValueNodeType valueNodeType = new ValueNodeType(null);
        Node n = valueNodeType.createNode();
        assertEquals(ParameterType.Type.INT, valueNodeType.pInt.getType());
        assertEquals(ParameterType.Type.FLOAT, valueNodeType.pFloat.getType());
        assertEquals(ParameterType.Type.STRING, valueNodeType.pString.getType());

        assertEquals(0, n.asInt("int"));
        assertEquals(0.0, n.asFloat("float"));
        assertEquals("", n.asString("string"));

        n.set("int", 12);
        n.set("float", 0.5);
        n.set("string", "hello");

        assertEquals(12, n.asInt("int"));
        assertEquals(0.5, n.asFloat("float"));
        assertEquals("hello", n.asString("string"));
    }

    public void testExpressionConnections() {
        Network net = (Network) manager.getNodeType("corevector.vecnet").createNode();
        Node rect1 = net.create(manager.getNodeType("corevector.rect"));
        Node ellipse1 = net.create(manager.getNodeType("corevector.ellipse"));
        Node copy1 = net.create(manager.getNodeType("corevector.copy"));
        copy1.getParameter("shape").connect(rect1);
        copy1.getParameter("tx").setExpression("ellipse1.x");
        Parameter xParam = ellipse1.getParameter("x");
        assertEquals(1, xParam.getDependents().size());
        assert (xParam.getDependents().contains(copy1.getParameter("tx")));
        assertEquals(2, copy1.getConnections().size());
        assertEquals(1, rect1.getConnections().size());
        assertEquals(1, ellipse1.getConnections().size());
    }

    public void testMultiParameters() {
        Network net = (Network) testNetworkType.createNode();
        Node number1 = net.create(numberType);
        number1.set("value", 1);
        Node number2 = net.create(numberType);
        number2.set("value", 2);
        Node number3 = net.create(numberType);
        number3.set("value", 3);
        Node multiAdd1 = net.create(multiAddType);
        Connection c1 = multiAdd1.getParameter("values").connect(number1);
        Connection c2 = multiAdd1.getParameter("values").connect(number2);
        Connection c3 = multiAdd1.getParameter("values").connect(number3);
        assertTrue(c1 == c2);
        assertTrue(c1 == c3);
        assertTrue(c1 instanceof MultiConnection);
        assertEquals(3, ((MultiConnection) c1).getOutputParameters().size());
        multiAdd1.update();
        assertEquals(6, multiAdd1.getOutputValue());
    }

    //// Helper functions ////

    private class ValueNodeType extends NodeType {
        public ParameterType pInt, pFloat, pString;

        private ValueNodeType(NodeTypeLibrary library) {
            super(library, "test.value", ParameterType.Type.INT);
            pInt = addParameterType("int", ParameterType.Type.INT);
            pFloat = addParameterType("float", ParameterType.Type.FLOAT);
            pString = addParameterType("string", ParameterType.Type.STRING);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(42);
            return true;
        }
    }
}
