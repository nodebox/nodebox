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

import java.util.List;

public class ExpressionTest extends NodeTestCase {

    public void testSimple() {
        Node n = numberType.createNode();
        Parameter pValue = n.getParameter("value");
        Expression e = new Expression(pValue, "1 + 2");
        assertEquals(3, e.asInt());
    }

    /**
     * Test parameter interaction between nodes.
     */
    public void testNodeLocal() {
        Network net = (Network) testNetworkType.createNode();
        Node multiply = net.create(multiplyType);
        Parameter p1 = multiply.getParameter("v1");
        Parameter p2 = multiply.getParameter("v2");
        p2.setValue(12);
        assertExpressionEquals(12, p1, "v2");
    }

    public void testCycles() {
        Network net = (Network) testNetworkType.createNode();
        Node number1 = net.create(numberType);
        Node add1 = net.create(addType);
        Parameter pValue = number1.getParameter("value");
        Parameter pV1 = add1.getParameter("v1");
        Parameter pV2 = add1.getParameter("v2");
        // Create a direct cycle.
        assertExpressionInvalid(pValue, "value");
        // This should not have created any connections
        assertTrue(pValue.getDependencies().isEmpty());
        number1.set("value", 42);
        assertExpressionEquals(42, pV1, "number1.value");
        // Create a 2-node cycle with expressions
        assertExpressionInvalid(pValue, "add1.v1");
        // Now create a 2-parameter cycle within the same node.
        pV1.setExpression("v2");
        assertTrue(add1.update());
        // This does not cause an error...
        pV2.setExpression("v1");
        // TODO: ... but updating this node does.
        // Currently, this works fine, but all parameters will get their default value.
        assertTrue(add1.update());
    }

    /**
     * This test checks if parameters that refer to other parameters in the same node have the most recent data.
     * Specifically, it tests if the order of processing doesn't affect the data flow.
     */
    public void testStaleData() {
        Network net = (Network) testNetworkType.createNode();
        Node number1 = net.create(numberType);
        Node add1 = net.create(addType);
        Parameter v1 = add1.getParameter("v1");
        Parameter v2 = add1.getParameter("v2");
        // Basic setup: v2 -> v1 -> number1.value
        // For this to work, v1 needs to update number1 first before v2 gets the data.
        // If the value is not updated, v2 will get the value from v1, which hasn't updated yet,
        // and which will thus return 0.
        number1.set("value", 42);
        v1.setExpression("number1.value");
        v2.setExpression("v1");
        assertTrue(add1.update());
        assertEquals(42 + 42, add1.getOutputValue());
        // Because we cannot determine the exact order of processing, we need to run this test twice.
        // So this is the setup in the other direction: v1 -> v2 -> number1.value
        // This time, v2 needs to update number1 first, then v1.
        number1.set("value", 33);
        v1.setExpression("v2");
        v2.setExpression("number1.value");
        assertTrue(add1.update());
        assertEquals(33 + 33, add1.getOutputValue());
    }

    public void testNetworkLocal() {
        NodeType netType = testNetworkType.clone();
        ParameterType pn = netType.addParameterType("pn", ParameterType.Type.INT);
        pn.setDefaultValue(33);
        Network net = (Network) netType.createNode();
        Node number1 = net.create(numberType);
        Parameter pValue1 = number1.getParameter("value");
        pValue1.set(84);
        assertEquals("number1", number1.getName());
        //Parameter p1 = test1.addParameter("p1", Parameter.Type.INT);
        Node number2 = net.create(numberType);
        assertEquals("number2", number2.getName());
        //Parameter p2 = number2.addParameter("p2", Parameter.Type.INT);
        Parameter pValue2 = number2.getParameter("value");
        pValue2.set(12);
        // Trying to get the value of number2 by just using the expression "value" is impossible,
        // since it will retrieve the value parameter of number1.
        assertExpressionInvalid(pValue1, "value");
        // Access p2 through the node name.
        assertExpressionEquals(12, pValue1, "number2.value");
        // Access p2 through the network.
        assertExpressionEquals(12, pValue1, "network.number2.value");
        // Access the pn Parameter on the network.
        assertExpressionEquals(33, pValue1, "network.pn");
    }

    public void testDependencies() {
        Network net = (Network) manager.getNodeType("corevector.vecnet").createNode();
        Node rect = net.create(manager.getNodeType("corevector.rect"));
        Node copy = net.create(manager.getNodeType("corevector.copy"));
        rect.getParameter("y").setExpression("x");
        List<Parameter> dependencies = rect.getParameter("y").getDependencies();
        assertEquals(1, dependencies.size());
        assertEquals(rect.getParameter("x"), dependencies.get(0));
        rect.getParameter("y").setExpression("copy1.ty + x");
        dependencies = rect.getParameter("y").getDependencies();
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains(copy.getParameter("ty")));
        assertTrue(dependencies.contains(rect.getParameter("x")));
    }

    public void assertExpressionEquals(Object expected, Parameter p, String expression) {
        p.setExpression(expression);
        p.update(new ProcessingContext());
        assertEquals(expected, p.getValue());
    }

    public void assertExpressionInvalid(Parameter p, String expression) {
        try {
            p.setExpression(expression);
            p.update(new ProcessingContext());
            fail("Should have thrown exception");
        } catch (Exception e) {
        }
    }

}
