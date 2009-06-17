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

import nodebox.node.polygraph.Polygon;
import nodebox.node.polygraph.Rectangle;

import java.util.Set;

public class ExpressionTest extends NodeTestCase {

    public void testSimple() {
        Node n = numberNode.newInstance(testLibrary, "number");
        Parameter pValue = n.getParameter("value");
        Expression e = new Expression(pValue, "1 + 2");
        assertEquals(3, e.asInt());
    }

    /**
     * Test parameter interaction between nodes.
     */
    public void testNodeLocal() {
        Node net = testNetworkNode.newInstance(testLibrary, "net");
        Node addDirect = net.create(addDirectNode);
        Parameter p1 = addDirect.getParameter("v1");
        Parameter p2 = addDirect.getParameter("v2");
        p2.setValue(12);
        assertExpressionEquals(12, p1, "v2");
    }

    public void testExpressionErrors() {
        // Setting an expression immediately evaluates it and returns an error if the expression is invalid.
        Node test = Node.ROOT_NODE.newInstance(testLibrary, "test");
        Parameter pX = test.addParameter("x", Parameter.Type.INT, 3);
        assertInvalidExpression(pX, "y", "could not access: y");
        Parameter pY = test.addParameter("y", Parameter.Type.INT, 5);
        assertExpressionEquals(5, pX, "y");
        // Expression of parameter x is still set to "y"
        assertEquals("y", pX.getExpression());
    }

    /**
     * Test what happens if your expression depends on a parameter that gets removed.
     */
    public void testDeadDependencies() {
        Node test = Node.ROOT_NODE.newInstance(testLibrary, "test");
        Parameter pX = test.addParameter("x", Parameter.Type.INT, 3);
        Parameter pY = test.addParameter("y", Parameter.Type.INT, 5);
        pX.setExpression("y");
        assertTrue(pX.getDependencies().contains(pY));
        pX.update(new ProcessingContext());
        assertEquals(5, pX.getValue());
        test.removeParameter("y");
        // At this point, the parameter dependency should no longer exist.
        assertFalse(pX.getDependencies().contains(pY));
        try {
            pX.update(new ProcessingContext());
        } catch (ExpressionError e) {
            // update throws an error since the expression references a parameter that cannot be found.
            //throw e;
            assertTrue(e.getCause().getMessage().toLowerCase().contains("unable to resolve variable 'y'"));
        }
        // The value hasn't changed.
        assertEquals(5, pX.getValue());
    }

    /**
     * Test the same as testDeadDependencies, but in the reverse. (What happens if a parameter that depends on this
     * parameter gets removed).
     *
     * This is less dramatic than the other case; we just need to make sure that we don't accidentally dereference
     * a dead Parameter.
     */
    public void testDeadDependents() {
        Node net = Node.ROOT_NODE.newInstance(testLibrary, "net");
        Node number1 = net.create(numberNode);
        Node number2 = net.create(numberNode);
        Parameter pValue1 = number1.getParameter("value");
        Parameter pValue2 = number2.getParameter("value");
        number1.setValue("value", 5);
        number2.getParameter("value").setExpression("number1.value");
        number2.setRendered();
        net.update();
        assertFalse(net.isDirty());
        assertEquals(5, net.getOutputValue());
        number1.setValue("value", 13);
        assertTrue(net.isDirty());
        net.update();
        assertEquals(13, net.getOutputValue());
        assertTrue(pValue1.getDependents().contains(pValue2));
        number2.removeParameter("value");
        assertFalse(pValue1.getDependents().contains(pValue2));
    }

    /**
     * You can only reference other parameters and the built-in functions.
     *
     * Test what happens if you break this rule.
     */
    public void testOnlyReferenceParameters() {
        Node net = Node.ROOT_NODE.newInstance(testLibrary, "net");
        Node number1 = net.create(numberNode);
        Node number2 = net.create(numberNode);
        Parameter pValue2 = number2.getParameter("value");
        // Setting the expression does not throw an error.
        pValue2.setExpression("number1");
        // Evaluating the node does.
        assertProcessingError(number2, "value is not an int");
    }

    public void testCycles() {
        Node net = testNetworkNode.newInstance(testLibrary, "net");
        Node number1 = net.create(numberNode);
        Node addDirect1 = net.create(addDirectNode);
        Parameter pValue = number1.getParameter("value");
        Parameter pV1 = addDirect1.getParameter("v1");
        Parameter pV2 = addDirect1.getParameter("v2");
        // Create a direct cycle.
        assertInvalidExpression(pValue, "value", "refers to itself");
        // This should not have created any connections
        assertTrue(pValue.getDependencies().isEmpty());
        number1.setValue("value", 42);
        assertExpressionEquals(42, pV1, "number1.value");
        // Create a 2-node cycle with expressions
        assertInvalidExpression(pValue, "addDirect1.v1", "cyclic dependency");
        // Now create a 2-parameter cycle within the same node.
        pV1.setExpression("v2");
        addDirect1.update();
        assertInvalidExpression(pV2, "v1", "cyclic dependency");
    }

    /**
     * This test checks if parameters that refer to other parameters in the same node have the most recent data.
     * Specifically, it tests if the order of processing doesn't affect the data flow.
     */
    public void testStaleData() {
        Node net = testNetworkNode.newInstance(testLibrary, "net");
        Node number1 = net.create(numberNode);
        Node add1 = net.create(addDirectNode);
        Parameter v1 = add1.getParameter("v1");
        Parameter v2 = add1.getParameter("v2");
        // Basic setup: v2 -> v1 -> number1.value
        // For this to work, v1 needs to update number1 first before v2 gets the data.
        // If the value is not updated, v2 will get the value from v1, which hasn't updated yet,
        // and which will thus return 0.
        number1.setValue("value", 42);
        v1.setExpression("number1.value");
        v2.setExpression("v1");
        add1.update();
        assertEquals(42 + 42, add1.getOutputValue());
        // Because we cannot determine the exact order of processing, we need to run this test twice.
        // So this is the setup in the other direction: v1 -> v2 -> number1.value
        // This time, v2 needs to update number1 first, then v1.
        number1.setValue("value", 33);
        // Setting v1 to the expression v2 would cause a cycle, since v2 is already linked to v1.
        // Clear v2's expression first.
        v2.clearExpression();
        v1.setExpression("v2");
        v2.setExpression("number1.value");
        add1.update();
        assertEquals(33 + 33, add1.getOutputValue());
    }

    public void testNetworkLocal() {
        Node net = testNetworkNode.newInstance(testLibrary, "net");
        net.addParameter("pn", Parameter.Type.INT, 33);
        Node number1 = net.create(numberNode);
        Parameter pValue1 = number1.getParameter("value");
        pValue1.set(84);
        assertEquals("number1", number1.getName());
        //Parameter p1 = test1.addParameter("p1", Parameter.Type.INT);
        Node number2 = net.create(numberNode);
        assertEquals("number2", number2.getName());
        //Parameter p2 = number2.addParameter("p2", Parameter.Type.INT);
        Parameter pValue2 = number2.getParameter("value");
        pValue2.set(12);
        // Trying to get the value of number2 by just using the expression "value" is impossible,
        // since it will retrieve the value parameter of number1, which will cause a cycle.
        assertInvalidExpression(pValue1, "value", "refers to itself");
        // Access p2 through the node name.
        assertExpressionEquals(12, pValue1, "number2.value");
        // Access p2 through the network.
        assertExpressionEquals(12, pValue1, "network.number2.value");
        // Access the pn Parameter on the network.
        assertExpressionEquals(33, pValue1, "network.pn");
    }

    public void testDependencies() {
        Node polynet = Node.ROOT_NODE.newInstance(testLibrary, "polynet");
        Node rect1 = polynet.create(rectNode);
        Node translate1 = polynet.create(manager.getNode("polygraph.translate"));
        assertEquals("translate1", translate1.getName());
        rect1.getParameter("y").setExpression("x");
        Set<Parameter> dependencies = rect1.getParameter("y").getDependencies();
        assertEquals(1, dependencies.size());
        dependencies.contains(rect1.getParameter("x"));
        rect1.getParameter("y").setExpression("translate1.ty + x");
        dependencies = rect1.getParameter("y").getDependencies();
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains(translate1.getParameter("ty")));
        assertTrue(dependencies.contains(rect1.getParameter("x")));
    }

    public void testStamp() {
        Polygon p;

        Node rect1 = rectNode.newInstance(testLibrary, "rect1");
        // Sets the width to a stamp expression. If this node gets executed, it retrieves
        // "mywidth" from the context and uses that. If mywidth could not be found, it uses
        // the default value of 20 for this parameter.
        rect1.getParameter("width").setExpression("stamp(\"mywidth\", 20)");
        // Update the node to see if it works.
        rect1.update();
        p = (Polygon) rect1.getOutputValue();
        assertEquals(new Rectangle(0, 0,  20, 100), p.getBounds());

        // The stamper is a node that relies on copy stamping to replace
        // one of the parameters of the connected node. The connected node (rect1)
        // still needs to use the "stamp" expression.
        Node stamper = translateNode.newInstance(testLibrary, "stamper");
        // Nodes are automatically evaluated once, even though we do not use the output.
        // TODO: Set a flag on the node that allows control over cooking. 
        String code = "def cook(self):\n" +
                "  context.put(self.key, self.value)\n" +
                "  self.node.stampDirty()\n" +
                "  self.node.updateDependencies(context)\n" +
                "  return self.polygon\n";
        stamper.setValue("_code", new PythonCode(code));
        stamper.addParameter("key", Parameter.Type.STRING);
        stamper.addParameter("value", Parameter.Type.FLOAT);
        stamper.setValue("key", "mywidth");
        stamper.setValue("value", 50);
        stamper.getPort("polygon").connect(rect1);
        stamper.update();
        p = (Polygon) stamper.getOutputValue();
        assertEquals(new Rectangle(0, 0,  50, 100), p.getBounds());
    }

    public void assertExpressionEquals(Object expected, Parameter p, String expression) {
        // We don't catch the ExpressionError but let it bubble up.
        p.setExpression(expression);
        p.update(new ProcessingContext());
        assertEquals(expected, p.getValue());
    }

    private void assertInvalidExpression(Parameter p, String expression, String expectedMessage) {
        try {
            p.setExpression(expression);
            fail("Expression should have failed with \"" + expectedMessage + "\"");
        } catch (ExpressionError e) {
            assertTrue("Expected message \"" + expectedMessage + "\", got \"" + e.getCause().getMessage() + "\"",
                    e.getCause().getMessage().toLowerCase().contains(expectedMessage.toLowerCase()));
        }
    }


}
