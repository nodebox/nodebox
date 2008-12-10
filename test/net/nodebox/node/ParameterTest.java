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

public class ParameterTest extends TestCase {

    public void testNaming() {
        Node n = new TestNode();
        Parameter p = n.addParameter("test", Parameter.Type.INT);

        checkInvalidName(p, "1234", "names cannot start with a digit.");

        checkInvalidName(p, "node", "names can not be one of the reserved words.");
        checkInvalidName(p, "name", "names can not be one of the reserved words.");

        checkInvalidName(p, "UPPERCASE", "names cannot be in uppercase.");
        checkInvalidName(p, "uPpercase", "names cannot contain uppercase letters");
        checkInvalidName(p, "__reserved", "names cannot start with double underscores");
        checkInvalidName(p, "what!", "Only lowercase, numbers and underscore are allowed");
        checkInvalidName(p, "$-#34", "Only lowercase, numbers and underscore are allowed");
        checkInvalidName(p, "", "names cannot be empty");
        checkInvalidName(p, "very_very_very_very_very_very_long_name", "names cannot be longer than 30 characters");

        checkValidName(p, "radius");
        checkValidName(p, "_test");
        checkValidName(p, "_");
        checkValidName(p, "_1234");
        checkValidName(p, "a1234");

        // TODO: Right now, these names are accepted, although they will conflict with the internal node attributes.
        checkValidName(p, "x");
        checkValidName(p, "y");
        checkValidName(p, "dirty");
        checkValidName(p, "process");
    }

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

    public void testValues() {
        ValueNode n = new ValueNode();
        assertEquals(Parameter.Type.INT, n.pInt.getType());
        assertEquals(Parameter.Type.FLOAT, n.pFloat.getType());
        assertEquals(Parameter.Type.STRING, n.pString.getType());

        assertEquals(0, n.pInt.asInt());
        assertEquals(0.0, n.pFloat.asFloat());
        assertEquals("", n.pString.asString());

        n.pInt.set(12);
        n.pFloat.set(0.5);
        n.pString.set("hello");

        assertEquals(12, n.pInt.asInt());
        assertEquals(0.5, n.pFloat.asFloat());
        assertEquals("hello", n.pString.asString());
    }

    public void testBounding() {
        Node n = new TestNode();
        Parameter angle = n.addParameter("angle", Parameter.Type.ANGLE);
        angle.setBoundingMethod(Parameter.BoundingMethod.SOFT);
        angle.setMinimumValue(-100.0);
        angle.setMaximumValue(100.0);
        checkValidSet(angle, 0.0);
        checkValidSet(angle, 1000.0);
        checkValidSet(angle, -1000.0);
        angle.setBoundingMethod(Parameter.BoundingMethod.HARD);
        assertEquals(-100.0, angle.asFloat()); // Setting the bounding type to hard clamped the value
        checkInvalidSet(angle, 500.0);
        angle.setBoundingMethod(Parameter.BoundingMethod.NONE);
        checkValidSet(angle, 300.0);
        angle.setBoundingMethod(Parameter.BoundingMethod.HARD);
        assertEquals(100.0, angle.asFloat());
    }

    public void testType() {
        Node n = new TestNode();
        Parameter angle = n.addParameter("angle", Parameter.Type.ANGLE);
        assertEquals(Parameter.CoreType.FLOAT, angle.getCoreType());
    }

    //// Helper functions ////

    private void checkInvalidName(Parameter p, String newName, String reason) {
        try {
            p.setName(newName);
            fail("the following condition was not met: " + reason);
        } catch (Parameter.InvalidName e) {
        }
    }

    private void checkValidName(Parameter p, String newName) {
        try {
            p.setName(newName);
        } catch (Parameter.InvalidName e) {
            fail("The name \"" + newName + "\" should have been accepted.");
        }
    }

    private void checkParameterNotFound(Node n, String name) {
        try {
            n.getParameter(name);
            fail("The parameter \"" + name + "\" should not exist.");
        } catch (Parameter.NotFound nf) {
        }
    }

    private void checkValidSet(Parameter p, double value) {
        try {
            p.set(value);
        } catch (ValueError e) {
            fail("Value " + value + " should have been accepted.");
        }
    }

    private void checkInvalidSet(Parameter p, double value) {
        try {
            p.set(value);
            fail("Value " + value + " should not have been accepted.");
        } catch (ValueError e) {
        }
    }

    private class ValueNode extends Node {
        public Parameter pInt, pFloat, pString;

        private ValueNode() {
            super(Parameter.Type.INT);
            pInt = addParameter("int", Parameter.Type.INT);
            pFloat = addParameter("float", Parameter.Type.FLOAT);
            pString = addParameter("string", Parameter.Type.STRING);
        }

        protected boolean process(ProcessingContext ctx) {
            setOutputValue(42);
            return true;
        }
    }
}
