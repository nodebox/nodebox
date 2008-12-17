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
        Node multiply = multiplyType.createNode();
        Parameter p1 = multiply.getParameter("v1");
        Parameter p2 = multiply.getParameter("v2");
        p2.setValue(12);
        assertExpressionEquals(12, p1, "v2");
    }

    public void xtestCycles() {
        Node n = numberType.createNode();
        Parameter pValue = n.getParameter("value");
        assertExpressionInvalid(pValue, "value");
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
        // Trying to retrieve the value of number2 by just using the expression "value" is impossible,
        // since it will retrieve the value parameter of number1.
        assertExpressionEquals(84, pValue1, "value");
        // Access p2 through the node name.
        assertExpressionEquals(12, pValue1, "number2.value");
        // Access p2 through the network.
        assertExpressionEquals(12, pValue1, "network.number2.value");
        // Access the pn Parameter on the network.
        assertExpressionEquals(33, pValue1, "network.pn");
    }


    public void assertExpressionEquals(Object expected, Parameter p, String expression) {
        p.setExpression(expression);
        p.update(new ProcessingContext());
        assertEquals(expected, p.getValue());
    }

    public void assertExpressionInvalid(Parameter p, String expression) {
        p.setExpression(expression);
        try {
            p.update(new ProcessingContext());
            fail("Should have thrown exception");
        } catch (Exception e) {
        }
    }

}
