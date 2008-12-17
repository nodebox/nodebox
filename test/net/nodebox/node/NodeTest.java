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

public class NodeTest extends NodeTestCase {

    public void testNaming() {
        Node n = numberType.createNode();
        assertEquals(n.getDefaultName(), "number");
        assertEquals(n.getName(), "number");
    }

    public void testParameters() {
        Node n = addType.createNode();
        try {
            n.getParameter("p1");
            fail("Should have thrown NotFoundException");
        } catch (NotFoundException e) {
        }
        assertTrue(n.hasParameter("v1"));
        try {
            n.getParameter("v1");
        } catch (NotFoundException e) {
            fail("Should not have thrown NotFoundException");
        }
        try {
            n.getParameter("x");
            fail("Should have thrown NotFoundException");
        } catch (NotFoundException e) {
        }
    }

    public void testNodeNaming() {
        Node n = numberType.createNode();
        checkInvalidName(n, "1234", "names cannot start with a digit.");

        checkInvalidName(n, "node", "names can not be one of the reserved words.");
        checkInvalidName(n, "root", "names can not be one of the reserved words.");
        checkInvalidName(n, "network", "names can not be one of the reserved words.");

        checkInvalidName(n, "UPPERCASE", "names cannot be in uppercase.");
        checkInvalidName(n, "uPpercase", "names cannot contain uppercase letters");
        checkInvalidName(n, "__reserved", "names cannot start with double underscores");
        checkInvalidName(n, "what!", "Only lowercase, numbers and underscore are allowed");
        checkInvalidName(n, "$-#34", "Only lowercase, numbers and underscore are allowed");
        checkInvalidName(n, "", "names cannot be empty");
        checkInvalidName(n, "very_very_very_very_very_very_long_name", "names cannot be longer than 30 characters");

        checkValidName(n, "radius");
        checkValidName(n, "_test");
        checkValidName(n, "_");
        checkValidName(n, "_1234");
        checkValidName(n, "a1234");
        checkValidName(n, "node1");
    }

    public void testDirty() {
        Node n = numberType.createNode();
        assertTrue(n.isDirty());
        n.update();
        assertFalse(n.isDirty());
        n.set("value", 12);
        assertTrue(n.isDirty());
        n.update();
        assertFalse(n.isDirty());
        n.getParameter("value").set(13);
        assertTrue(n.isDirty());
        n.update();
        assertFalse(n.isDirty());
    }

    //// Helper functions ////

    private void checkInvalidName(Node n, String newName, String reason) {
        try {
            n.setName(newName);
            fail("the following condition was not met: " + reason);
        } catch (InvalidNameException e) {
        }
    }

    private void checkValidName(Node n, String newName) {
        try {
            n.setName(newName);
        } catch (InvalidNameException e) {
            fail("The name \"" + newName + "\" should have been accepted.");
        }
    }

}
