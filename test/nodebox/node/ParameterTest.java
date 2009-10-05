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

import nodebox.graphics.Canvas;
import nodebox.graphics.Color;

import java.util.Map;
import java.util.HashMap;

public class ParameterTest extends NodeTestCase {

    private static class TestParameterValueListener implements ParameterValueListener {
        public Map<Parameter, Integer> valueMap = new HashMap<Parameter, Integer>();

        public void valueChanged(Parameter source) {
            Integer counter = valueMap.get(source);
            if (counter == null) counter = 0;
            ++counter;
            valueMap.put(source, counter);
        }

        public int getCounter(Parameter source) {
            Integer counter = valueMap.get(source);
            if (counter == null) counter = 0;
            return counter;
        }
    }

    private static class TestParameterAttributeListener implements ParameterAttributeListener {
        public int changeCounter = 0;

        public void attributeChanged(Parameter source) {
            ++changeCounter;
        }
    }

    public void testNaming() {
        Node n = numberNode.newInstance(testLibrary, "number1");

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
        assertValidName(n, "UPPERCASE");
        assertValidName(n, "uPpercase");

        assertInvalidName(n, "radius", "parameter names must be unique for the node");
        n.addPort("myport", Integer.class);
        assertInvalidName(n, "myport", "parameter names must be unique across parameters and ports");
    }

    public void testDefaultValue() {
        Node n = numberNode.newInstance(testLibrary, "number1");
        Parameter pInt = n.addParameter("int", Parameter.Type.INT);
        Parameter pFloat = n.addParameter("float", Parameter.Type.FLOAT);
        Parameter pString = n.addParameter("string", Parameter.Type.STRING);
        Parameter pColor = n.addParameter("color", Parameter.Type.COLOR);
        Parameter pCode = n.addParameter("code", Parameter.Type.CODE);

        assertEquals(0, pInt.getDefaultValue());
        assertEquals(0F, pFloat.getDefaultValue());
        assertEquals("", pString.getDefaultValue());
        assertEquals(new Color(), pColor.getDefaultValue());
        assertEquals("EmptyCode", pCode.getDefaultValue().getClass().getSimpleName());
    }

    public void testValidate() {
        Node customType = numberNode.newInstance(testLibrary, "number1");
        Parameter pFloat = customType.addParameter("float", Parameter.Type.FLOAT);
        assertInvalidValue(pFloat, "A");
        assertInvalidValue(pFloat, new Color());
        assertInvalidValue(pFloat, new Canvas());
        assertValidValue(pFloat, 1F);
        // As a special exception, floating-point parameters can also accept integers
        assertValidValue(pFloat, 1);

        Parameter pInt = customType.addParameter("int", Parameter.Type.INT);
        assertInvalidValue(pInt, "A");
        assertInvalidValue(pInt, new Color());
        assertInvalidValue(pInt, new Canvas());
        assertValidValue(pInt, 1);
        // You cannot assign floating-point values to integers,
        // so the above exception to the rule only works in one way.
        assertInvalidValue(pInt, 1F);

        Parameter pColor = customType.addParameter("color", Parameter.Type.COLOR);
        assertInvalidValue(pColor, "A");
        assertInvalidValue(pColor, 2);
        assertValidValue(pColor, new Color());

        // Toggle has a hard bounded range between 0 and 1.
        Parameter ptToggle = customType.addParameter("toggle", Parameter.Type.INT);
        ptToggle.setBoundingMethod(Parameter.BoundingMethod.HARD);
        ptToggle.setMinimumValue(0F);
        ptToggle.setMaximumValue(1F);
        assertInvalidValue(ptToggle, "A");
        assertInvalidValue(ptToggle, -1);
        assertInvalidValue(ptToggle, 100);
        assertValidValue(ptToggle, 0);
        assertValidValue(ptToggle, 1);
    }

    public void testBounding() {
        Node n = numberNode.newInstance(testLibrary, "number1");
        Parameter pAngle = n.addParameter("angle", Parameter.Type.FLOAT);
        pAngle.setBoundingMethod(Parameter.BoundingMethod.SOFT);
        pAngle.setMinimumValue(-100F);
        pAngle.setMaximumValue(100F);
        assertValidValue(n, "angle", 0F);
        assertValidValue(n, "angle", 1000F);
        assertValidValue(n, "angle", -1000F);
        pAngle.setBoundingMethod(Parameter.BoundingMethod.HARD);
        assertEquals(-100F, n.asFloat("angle")); // Setting the bounding type to hard clamped the value
        assertInvalidValue(n, "angle", 500F);
        pAngle.setBoundingMethod(Parameter.BoundingMethod.NONE);
        assertValidValue(n, "angle", 300F);
        pAngle.setBoundingMethod(Parameter.BoundingMethod.HARD);
        assertEquals(100F, n.asFloat("angle"));
    }

    public void testCorrectType() {
        Node number = numberNode.newInstance(testLibrary, "number1");
        Parameter pValue = number.getParameter("value");
        assertEquals(Parameter.Type.INT, pValue.getType());
    }

    public void testDirectValue() {
        Node num1 = numberNode.newInstance(testLibrary, "number1");
        assertEquals(null, num1.getOutputValue());
        Node num2 = numberNode.newInstance(testLibrary, "number2");
        num2.setValue("value", 12);
        assertEquals(null, num2.getOutputValue());
        assertFalse(num2.getParameter("value").hasExpression());
    }

    public void testAsFloat() {
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "node");
        n.addParameter("int", Parameter.Type.INT, 42);
        assertEquals(42F, n.asFloat("int"));
        n.addParameter("string", Parameter.Type.STRING, "hello");
        try {
            n.asFloat("string");
            fail("Should have caused an exception.");
        } catch (RuntimeException ignored) {
        }
    }

    public void testAsString() {
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "allControls");
        n.addParameter("int", Parameter.Type.INT);
        n.addParameter("float", Parameter.Type.FLOAT);
        n.addParameter("string", Parameter.Type.STRING);
        n.addParameter("color", Parameter.Type.COLOR);
        n.addParameter("code", Parameter.Type.CODE);
        assertEquals("0", n.asString("int"));
        assertEquals("0.0", n.asString("float"));
        assertEquals("", n.asString("string"));
        assertEquals("#000000ff", n.asString("color"));
        assertEquals("", n.asString("code"));
    }

    public void testInvalidName() {
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "node1");
        Parameter pAlice = n.addParameter("alice", Parameter.Type.FLOAT);
        Parameter pBob = n.addParameter("bob", Parameter.Type.FLOAT);
        assertEquals(pAlice.getName(), "alice");
        assertEquals(pBob.getName(), "bob");
        assertValidName(pBob, "joe");
        assertEquals(pBob.getName(), "joe");
        assertFalse(n.hasParameter("bob"));
        assertTrue(n.hasParameter("joe"));
        assertParameterNotFound(n, "bob");
        assertEquals(pBob, n.getParameter("joe"));
        assertInvalidName(pBob, "alice", "Can not take the name of an existing parameter.");
        assertEquals(pBob, n.getParameter("joe")); // Check the previous setName hasn't affected the current name.
    }

    public void testLabel() {
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "node1");
        Parameter p1 = n.addParameter("width", Parameter.Type.FLOAT);
        assertEquals("Width", p1.getLabel());
        Parameter p2 = n.addParameter("a_somewhat_longer_parameter", Parameter.Type.FLOAT);
        assertEquals("A Somewhat Longer Parameter", p2.getLabel());
        Parameter p3 = n.addParameter("double__underscores__here", Parameter.Type.FLOAT);
        assertEquals("Double Underscores Here", p3.getLabel());
    }

    public void testValues() {
        Node n = new ValueBuiltin().getInstance();
        assertEquals(Parameter.Type.INT, n.getParameter("int").getType());
        assertEquals(Parameter.Type.FLOAT, n.getParameter("float").getType());
        assertEquals(Parameter.Type.STRING, n.getParameter("string").getType());

        assertEquals(0, n.asInt("int"));
        assertEquals(0F, n.asFloat("float"));
        assertEquals("", n.asString("string"));

        n.setValue("int", 12);
        n.setValue("float", 0.5F);
        n.setValue("string", "hello");

        assertEquals(12, n.asInt("int"));
        assertEquals(0.5F, n.asFloat("float"));
        assertEquals("hello", n.asString("string"));
    }

    /**
     * Test the strictness of setValue.
     */
    public void testLenientValues() {
        Node n = new ValueBuiltin().getInstance();
        // Set value of a float with an int.
        n.setValue("float", 12);
        assertEquals(12F, n.asFloat("float"));
        // Set value of a float with a double.
        n.setValue("float", 0.5);
        // Value is still set to previous value.
        assertEquals(0.5f, n.asFloat("float"));
    }

    /**
     * Test if creating expressions creates the correct dependencies.
     */
    public void testExpressionDependencies() {
        Node polynet = Node.ROOT_NODE.newInstance(testLibrary, "polynet");
        Node rect1 = polynet.create(manager.getNode("polygraph.rect"));
        Node rect2 = polynet.create(manager.getNode("polygraph.rect"));
        Node translate1 = polynet.create(manager.getNode("polygraph.translate"));
        translate1.getPort("polygon").connect(rect1);
        translate1.getParameter("tx").setExpression("rect2.x");
        Parameter txParam = translate1.getParameter("tx");
        Parameter xParam = rect2.getParameter("x");
        assertEquals(1, xParam.getDependents().size());
        assertTrue(xParam.getDependents().contains(txParam));
        assertEquals(1, txParam.getDependencies().size());
        assertTrue(txParam.getDependencies().contains(xParam));
    }


    public void testMultiParameters() {
        Node net1 = Node.ROOT_NODE.newInstance(testLibrary, "net1");
        Node number1 = net1.create(numberNode);
        number1.setValue("value", 1);
        Node number2 = net1.create(numberNode);
        number2.setValue("value", 2);
        Node number3 = net1.create(numberNode);
        number3.setValue("value", 3);
        Node multiAdd = net1.create(multiAddNode);
        Connection c1 = multiAdd.getPort("values").connect(number1);
        Connection c2 = multiAdd.getPort("values").connect(number2);
        Connection c3 = multiAdd.getPort("values").connect(number3);
        assertTrue(c1 == c2);
        assertTrue(c1 == c3);
        assertEquals(3, c1.getOutputs().size());
        multiAdd.update();
        assertEquals(1 + 2 + 3, multiAdd.getOutputValue());
        // Check dirty propagation
        assertFalse(multiAdd.isDirty());
        number2.setValue("value", 200);
        assertTrue(multiAdd.isDirty());
        multiAdd.update();
        assertEquals(1 + 200 + 3, multiAdd.getOutputValue());
        // Check disconnect
        number2.disconnect();
        assertFalse(number2.isConnected());
        assertTrue(number1.isConnected());
        assertTrue(number3.isConnected());
        assertTrue(multiAdd.isConnected());
        multiAdd.update();
        assertEquals(1 + 3, multiAdd.getOutputValue());
        multiAdd.disconnect();
        assertFalse(number2.isConnected());
        assertFalse(number1.isConnected());
        assertFalse(number3.isConnected());
        assertFalse(multiAdd.isConnected());
    }

    /**
     * Test if all parameter attributes are cloned.
     */
    public void testParameterCloning() {
        Node nodeA = Node.ROOT_NODE.newInstance(testLibrary, "a");
        Parameter aAngle = nodeA.addParameter("angle", Parameter.Type.FLOAT, 42F);
        aAngle.setBoundingMethod(Parameter.BoundingMethod.HARD);
        aAngle.setMinimumValue(0F);
        aAngle.setMaximumValue(360F);
        aAngle.setLabel("My Angle");
        aAngle.setDisplayLevel(Parameter.DisplayLevel.HUD);
        aAngle.setHelpText("The angle of the node.");
        aAngle.setWidget(Parameter.Widget.ANGLE);
        Node nodeB = nodeA.newInstance(testLibrary, "b");
        Parameter bAngle = nodeB.getParameter("angle");
        assertNotNull(bAngle);
        assertNotSame(aAngle, bAngle);
        assertEquals(aAngle.getBoundingMethod(), bAngle.getBoundingMethod());
        assertEquals(aAngle.getMinimumValue(), bAngle.getMinimumValue());
        assertEquals(aAngle.getMaximumValue(), bAngle.getMaximumValue());
        assertEquals(aAngle.getLabel(), bAngle.getLabel());
        assertEquals(aAngle.getDisplayLevel(), bAngle.getDisplayLevel());
        assertEquals(aAngle.getHelpText(), bAngle.getHelpText());
        assertEquals(aAngle.getWidget(), bAngle.getWidget());
    }

    /**
     * Test if changes to the parameter value fire the correct event.
     */
    public void testParameterValueEvents() {
    TestParameterValueListener l;
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "test");
        l = new TestParameterValueListener();
        n.addParameterValueListener(l);
        Parameter pAlpha = n.addParameter("alpha", Parameter.Type.FLOAT);
        Parameter pBeta = n.addParameter("beta", Parameter.Type.FLOAT);
        // Initialization has triggered the parameter value event.
        assertEquals(1, l.getCounter(pAlpha));
        assertEquals(1, l.getCounter(pBeta));
        // Update the node so new events will get fired.
        n.update();
        pAlpha.setValue(100);
        assertEquals(2, l.getCounter(pAlpha));
        assertEquals(1, l.getCounter(pBeta));
        // Change the value to the current value.
        // This should not trigger the event.
        pAlpha.setValue(100);
        assertEquals(2, l.getCounter(pAlpha));
        assertEquals(1, l.getCounter(pBeta));
        // The node is already dirty, and parameters will not receive any new events.
        // Update the node so it becomes clean again.
        // Set an expression for beta. This triggers the event.
        n.update();
        pAlpha.setValue(3);
        pBeta.setExpression("alpha + 1");
        assertEquals(2, l.getCounter(pBeta));
        // Now change alpha. This will not trigger beta, since the node is not updated yet.
        assertEquals(3, l.getCounter(pAlpha));
        assertEquals(2, l.getCounter(pBeta));
        // Update the node, which will trigger the value changed event.
        n.update();
        assertEquals(3, l.getCounter(pAlpha));
        assertEquals(3, l.getCounter(pBeta));
    }

    /**
     * Test if changes to the parameter metadata fire the correct event.
     */
    public void testParameterMetaEvents() {
        TestParameterAttributeListener l;
        Node alpha = Node.ROOT_NODE.newInstance(testLibrary, "alpha");
        Parameter pMenu = alpha.addParameter("menu", Parameter.Type.STRING);
        l = new TestParameterAttributeListener();
        alpha.addParameterAttributeListener(l);
        assertEquals(0, l.changeCounter);
        pMenu.setWidget(Parameter.Widget.MENU);
        assertEquals(1, l.changeCounter);
        pMenu.addMenuItem("en", "English");
        pMenu.addMenuItem("es", "Spanis");
        assertEquals(3, l.changeCounter);

        Parameter pFloat = alpha.addParameter("float", Parameter.Type.FLOAT);
        l = new TestParameterAttributeListener();
        alpha.addParameterAttributeListener(l);
        assertEquals(0, l.changeCounter);
        pFloat.setBoundingMethod(Parameter.BoundingMethod.HARD);
        assertEquals(1, l.changeCounter);
        pFloat.setMinimumValue(-100f);
        pFloat.setMaximumValue(100f);
        assertEquals(3, l.changeCounter);
    }

    /**
     * Test if the internal type is converted correctly so as to remain consistent.
     */
    public void testInternalType() {
        Node alpha = Node.ROOT_NODE.newInstance(testLibrary, "alpha");
        Parameter pFloat = alpha.addParameter("float", Parameter.Type.FLOAT);
        // We can set a double or integer value to a float parameter.
        pFloat.set(33);
        // However, getValue() should always return a float.
        assertEquals(33f, pFloat.getValue());
        // Now try the same with expressions.
        // Set an expression that returns an integer.
        pFloat.setExpression("12");
        alpha.update();
        // Since this is a float parameter, getValue should always return a float.
        assertEquals(12f, pFloat.getValue());
    }

    /**
     * Test if types are migrated correctly.
     *
     * Also checks if widgets set to this type follow along. Some
     * widgets don't make sense for a certain type.
     */
    public void testTypeMigration() {
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "test");
        // Parameter alpha will be converted from float to string.
        Parameter pAlpha = n.addParameter("alpha", Parameter.Type.FLOAT);
        pAlpha.setValue(12.5f);
        // Change the type to string.
        pAlpha.setType(Parameter.Type.STRING);
        assertEquals("12.5", pAlpha.getValue());
        // String types can't use a float widget, so
        // the widget should revert to the default widget for that type.
        assertEquals(Parameter.Widget.STRING, pAlpha.getWidget());

        // Parameter beta will be converted from color to int.
        Parameter pBeta = n.addParameter("beta", Parameter.Type.COLOR);
        assertEquals(Parameter.Widget.COLOR, pBeta.getWidget());
        pBeta.setValue(new Color(0.1, 0.2, 0.3, 0.4));
        // Change the type to int.
        pBeta.setType(Parameter.Type.INT);
        // The value can't be migrated sensibly, so the default value for int is used.
        assertEquals(Parameter.getDefaultValue(Parameter.Type.INT), pBeta.getValue());
        // The widget also defaults to the correct widget.
        assertEquals(Parameter.Widget.INT, pBeta.getWidget());

        // Parameter gamma will be converted from string to code.
        Parameter pGamma = n.addParameter("gamma", Parameter.Type.STRING);
        assertEquals(Parameter.Widget.STRING, pGamma.getWidget());
        // Set the value to something. This value will not be parsed as code.
        String source = "print 'hello'";
        pGamma.setValue(source);
        // Change the type to code.
        pGamma.setType(Parameter.Type.CODE);
        // The code will not be migrated.
        assertEquals(Parameter.emptyCode, pGamma.getValue());
        NodeCode code = (NodeCode) pGamma.getValue();
        assertEquals("", code.getSource());
        assertNull(code.cook(n, new ProcessingContext()));
    }

    /**
     * Test that changing the widget changes the type.
     */
    public void testWidgetChanges() {
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "test");
        // Parameter alpha will be converted from a float to a string widget.
        Parameter pAlpha = n.addParameter("alpha", Parameter.Type.FLOAT);
        pAlpha.setValue(12.5f);
        assertEquals(Parameter.Widget.FLOAT, pAlpha.getWidget());
        // Change the widget to a string.
        pAlpha.setWidget(Parameter.Widget.STRING);
        // This should change the underlying type and value.
        assertEquals(Parameter.Type.STRING, pAlpha.getType());
        assertEquals(String.class, pAlpha.getValue().getClass());
        assertEquals("12.5", pAlpha.getValue());

        // Parameter beta will be converted from color to int.
        Parameter pBeta = n.addParameter("beta", Parameter.Type.COLOR);
        assertEquals(Parameter.Widget.COLOR, pBeta.getWidget());
        pBeta.setValue(new Color(0.1, 0.2, 0.3, 0.4));
        // Change the widget to int.
        pBeta.setWidget(Parameter.Widget.INT);
        // This will change the type and widget to int.
        assertEquals(Parameter.Type.INT, pBeta.getType());
        assertEquals(Parameter.Widget.INT, pBeta.getWidget());
        // The value can't be migrated, so the default value for int is used.
        assertEquals(Parameter.getDefaultValue(Parameter.Type.INT), pBeta.getValue());
    }

    /**
     * Test corner cases with revertToDefault.
     */
    public void testRevertToDefault() {
        // Create a prototype node with an int parameter with an expression.
        Node proto = Node.ROOT_NODE.newInstance(testLibrary, "proto");
        Parameter pAlpha = proto.addParameter("alpha", Parameter.Type.INT);
        pAlpha.setExpression("40 + 2");
        Parameter pBeta = proto.addParameter("beta", Parameter.Type.INT, 88);
        // Create an instance of this prototype.
        Node test = proto.newInstance(testLibrary, "test");
        Parameter tAlpha = test.getParameter("alpha");
        // Check if the instance inherits the expression.
        assertTrue(tAlpha.hasExpression());
        assertEquals("40 + 2", tAlpha.getExpression());
        // Update the node so the expression gets evaluated.
        test.update();
        // Remove the expression.
        tAlpha.clearExpression();
        assertEquals(42, tAlpha.getValue());
        // Revert to default. The expression should be restored.
        tAlpha.revertToDefault();
        assertTrue(tAlpha.hasExpression());
        assertEquals("40 + 2", tAlpha.getExpression());
        // Now test the other way around. If the prototype has no expression, but the instance does,
        // remove the expression and set the value.
        Parameter tBeta = test.getParameter("beta");
        assertFalse(tBeta.hasExpression());
        tBeta.setExpression("3 - 2");
        test.update();
        assertEquals(1, tBeta.getValue());
        // Revert to default. The beta parameter should have a regular value instead of an expression.
        tBeta.revertToDefault();
        assertFalse(tBeta.hasExpression());
        assertEquals(88, tBeta.getValue());        
    }

    /**
     * Test if the parameter needs to be marked dirty every time we ask for upstream data.
     */
    public void testHasStampExpression() {
        Node n = Node.ROOT_NODE.newInstance(testLibrary, "test");
        Parameter pAlpha = n.addParameter("alpha", Parameter.Type.FLOAT);
        // No expression is set, so the stamp flag should be off.
        assertFalse(pAlpha.hasStampExpression());
        // The expression doesn't reference te stamp function.
        pAlpha.setExpression("12 + 3");
        assertFalse(pAlpha.hasStampExpression());
        // Updating the node shouldn't make any difference in the stamp expression flag.
        n.update();
        assertFalse(pAlpha.hasStampExpression());
        // Now set an expression that does use the stamp function.
        pAlpha.setExpression("stamp(\"myalpha\", 12)");
        assertTrue(pAlpha.hasStampExpression());
        // Again, updating the node shouldn't make any difference.
        n.update();
        assertTrue(pAlpha.hasStampExpression());
        // Set the parameter to the *same* expression.
        pAlpha.setExpression("stamp(\"myalpha\", 12)");
        assertTrue(pAlpha.hasStampExpression());
        // Clear out the expression.
        pAlpha.clearExpression();
        assertFalse(pAlpha.hasStampExpression());
    }

    //// Helper functions ////

    private void assertInvalidName(Node n, String newName, String reason) {
        try {
            n.addParameter(newName, Parameter.Type.INT);
            fail("the following condition was not met: " + reason);
        } catch (InvalidNameException ignored) {
        }
    }

    private void assertValidName(Node n, String newName) {
        try {
            n.addParameter(newName, Parameter.Type.INT);
        } catch (InvalidNameException e) {
            fail("The name \"" + newName + "\" should have been accepted.");
        }
    }

    private void assertValidValue(Parameter p, Object value) {
        try {
            p.validate(value);
        } catch (IllegalArgumentException e) {
            fail("The value '" + value + "' should have been accepted: " + e);
        }
    }

    private void assertValidValue(Node n, String parameterName, Object value) {
        try {
            n.setValue(parameterName, value);
        } catch (IllegalArgumentException e) {
            fail("The value '" + value + "' should have been accepted: " + e);
        }
    }


    private void assertInvalidValue(Parameter p, Object value) {
        try {
            p.validate(value);
            fail("The value '" + value + "' should not have been accepted.");
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void assertInvalidValue(Node n, String parameterName, Object value) {
        try {
            n.setValue(parameterName, value);
            fail("The value '" + value + "' should not have been accepted.");
        } catch (IllegalArgumentException ignored) {
        }
    }

    private class ValueBuiltin extends Builtin {

        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(testLibrary, "value");
            n.addParameter("int", Parameter.Type.INT);
            n.addParameter("float", Parameter.Type.FLOAT);
            n.addParameter("string", Parameter.Type.STRING);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            return 42;
        }

    }

    private void assertValidName(Parameter p, String newName) {
        try {
            p.setName(newName);
        } catch (InvalidNameException e) {
            fail("The name \"" + newName + "\" should have been accepted.");
        }
    }

    private void assertInvalidName(Parameter p, String newName, String reason) {
        try {
            p.setName(newName);
            fail("The name \"" + newName + "\" should not have been accepted: " + reason);
        } catch (InvalidNameException ignored) {
        }
    }

    private void assertParameterNotFound(Node n, String parameterName) {
        assertNull("The parameter \"" + parameterName + "\" should not have been found.", n.getParameter(parameterName));
    }
}
