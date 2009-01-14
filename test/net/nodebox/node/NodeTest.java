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
        checkValidName(n, "UPPERCASE");
        checkValidName(n, "uPpercase");
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

    public void testCopyWithUpstream() {
        Network net = (Network) testNetworkType.createNode();
        Node number1 = net.create(numberType);
        Node number2 = net.create(numberType);
        Node add1 = net.create(addType);
        assertEquals("number1", number1.getName());
        assertEquals("number2", number2.getName());
        assertEquals("add1", add1.getName());
        add1.getParameter("v1").connect(number1);
        add1.getParameter("v2").connect(number2);

        Node copiedAdd1 = add1.getNetwork().copyNodeWithUpstream(add1);
        assertEquals("add1", copiedAdd1.getName());
        Network copiedNetwork = copiedAdd1.getNetwork();
        assertEquals(net.getName(), copiedNetwork.getName());
        Node copiedNumber1 = copiedAdd1.getNetwork().getNode("number1");
        Node copiedNumber2 = copiedAdd1.getNetwork().getNode("number2");
        assertNotNull(copiedNumber1);
        assertNotNull(copiedNumber2);
        assert (copiedAdd1.isConnected());
        assert (copiedAdd1.getParameter("v1").isConnectedTo(copiedNumber1));
        assert (copiedAdd1.getParameter("v2").isConnectedTo(copiedNumber2));
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
