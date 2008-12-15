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

import junit.framework.TestCase;

public class ExpressionTest extends TestCase {

    public void testSimple() {
        Node n = new TestNode();
        Parameter p1 = n.addParameter("p1", Parameter.Type.INT);
        Expression e = new Expression(p1, "1 + 2");
        assertEquals(3, e.asInt());
    }

    /**
     * Test parameter interaction between nodes.
     */
    public void testNodeLocal() {
        Node n = new TestNode();
        Parameter p1 = n.addParameter("p1", Parameter.Type.INT);
        Parameter p2 = n.addParameter("p2", Parameter.Type.INT);
        p2.setDefaultValue(12);
        assertExpressionEquals(12, p1, "p2");
    }

    public void xtestCycles() {
        Node n = new TestNode();
        Parameter p1 = n.addParameter("p1", Parameter.Type.INT);
        assertExpressionInvalid(p1, "p1");
    }

    public void testNetworkLocal() {
        Network n = new TestNetwork();
        Parameter pn = n.addParameter("pn", Parameter.Type.INT);
        pn.setDefaultValue(33);
        Node test1 = n.create(TestNode.class);
        assertEquals("test1", test1.getName());
        Parameter p1 = test1.addParameter("p1", Parameter.Type.INT);
        Node test2 = n.create(TestNode.class);
        assertEquals("test2", test2.getName());
        Parameter p2 = test2.addParameter("p2", Parameter.Type.INT);
        p2.setDefaultValue(12);
        // Try to access p2 directly. Should fail, since p2 is on another node.
        assertExpressionInvalid(p1, "p2");
        // Access p2 through the node name.
        assertExpressionEquals(12, p1, "test2.p2");
        // Access p2 through the network.
        assertExpressionEquals(12, p1, "network.test2.p2");
        // Access the pn Parameter on the network.
        assertExpressionEquals(33, p1, "network.pn");
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
