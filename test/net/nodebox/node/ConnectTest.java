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

public class ConnectTest extends NodeTestCase {

    public void testDirty() {
        Node ng = numberType.createNode();
        assertTrue(ng.isDirty());
        ng.update();
        assertFalse(ng.isDirty());
        assertEquals(0, ng.getOutputValue());
        ng.set("value", 12);
        assertTrue(ng.isDirty());
        // Asking for the output value doesn't update the node implicitly.
        assertEquals(0, ng.getOutputValue());
        // You have to explicitly update the node to get the new output value.
        ng.update();
        assertFalse(ng.isDirty());
        assertEquals(12, ng.getOutputValue());
    }

    public void testConnect() {
        Network net = (Network) testNetworkType.createNode();
        Node ng = net.create(numberType);
        Node m = net.create(multiplyType);

        assertFalse(m.getParameter("v1").isConnected());
        assertFalse(m.getParameter("v1").isConnectedTo(ng));
        assertFalse(ng.isOutputConnected());
        assertFalse(ng.isOutputConnectedTo(m));
        assertFalse(ng.isOutputConnectedTo(m.getParameter("v1")));

        assertTrue(m.getParameter("v1").canConnectTo(ng));
        assertTrue(m.getParameter("v2").canConnectTo(ng));
        assertFalse(m.getParameter("somestring").canConnectTo(ng));

        Connection conn = m.getParameter("v1").connect(ng);
        assertTrue(m.getParameter("v1").isConnected());
        assertTrue(m.getParameter("v1").isConnectedTo(ng));
        assertTrue(ng.isOutputConnected());
        assertTrue(ng.isOutputConnectedTo(m));
        assertTrue(ng.isOutputConnectedTo(m.getParameter("v1")));
        assertEquals(m.getParameter("v1"), conn.getInputParameter());
        assertEquals(ng.getOutputParameter(), conn.getOutputParameter());
        assertEquals(m, conn.getInputNode());
        assertEquals(ng, conn.getOutputNode());

        assertConnectionError(m, "somestring", ng, "Somestring is of the wrong type and should not be connectable to NumberGenerator's output.");
    }

    public void testCycles() {
        Node ng = numberType.createNode();
//        assertConnectionError(ng, "number", ng, "Nodes cannot connect to themselves.");
        // TODO: more complex cyclic checks (A->B->A)
    }

    public void testDirtyPropagation() {
        Network net = (Network) testNetworkType.createNode();
        Node ng = net.create(numberType);
        Node m = net.create(multiplyType);
        // Nodes start out dirty
        assertTrue(ng.isDirty());
        assertTrue(m.isDirty());
        // Updating makes them clean
        ng.update();
        m.update();
        assertFalse(ng.isDirty());
        assertFalse(m.isDirty());
        // Connecting the multiplier to another node makes it dirty.
        // The output node doesn't become dirty.
        m.getParameter("v1").connect(ng);
        assertFalse(ng.isDirty());
        assertTrue(m.isDirty());
        m.update();
        assertFalse(ng.isDirty()); // This shouldn't have changed.
        assertFalse(m.isDirty());
        // A change to the upstream node should make downstream nodes dirty.
        ng.set("value", 12);
        assertTrue(ng.isDirty());
        assertTrue(m.isDirty());
        // Updating the downstream node should make all upstreams clean,
        // because their output values are needed to calculate the downstream.
        m.update();
        assertFalse(ng.isDirty());
        assertFalse(m.isDirty());
        // Changes to the downstream node don't affect upstreams.
        m.set("v2", 1);
        assertFalse(ng.isDirty());
        assertTrue(m.isDirty());
        m.update();
        assertFalse(m.isDirty());
        // Disconnecting makes the downstream dirty.
        m.getParameter("v1").disconnect();
        assertFalse(ng.isDirty());
        assertTrue(m.isDirty());
        // Check is disconnected nodes still propagate.
        ng.update();
        assertFalse(ng.isDirty());
        assertTrue(m.isDirty());
        m.update();
        assertFalse(m.isDirty());
        ng.set("value", 13);
        assertTrue(ng.isDirty());
        assertFalse(m.isDirty());
    }

    public void testValuePropagation() {
        Network net = (Network) testNetworkType.createNode();
        Node ng = net.create(numberType);
        Node m = net.create(multiplyType);
        m.set("v2", 2);
        m.getParameter("v1").connect(ng);
        assertEquals(0, m.getOutputValue());
        ng.set("value", 3);
        assertTrue(m.isDirty());
        assertEquals(0, m.getOutputValue());
        // Updating the NumberGenerator node has no effect on the multiplier node.
        ng.update();
        assertTrue(m.isDirty());
        assertEquals(0, m.getOutputValue());
        m.update();
        assertFalse(m.isDirty());
        assertEquals(6, m.getOutputValue());
        // Test if value stops propagating after disconnection.
        m.getParameter("v1").disconnect();
        assertTrue(m.isDirty());
        assertFalse(ng.isDirty());
        ng.set("value", 3);
        m.update();
        assertEquals(0, m.getOutputValue());
    }

    public void testDisconnect() {
        Network net = (Network) testNetworkType.createNode();
        Node ng = net.create(numberType);
        Node m = net.create(multiplyType);
        m.set("v2", 2);
        ng.set("value", 5);
        m.getParameter("v1").connect(ng);
        assertTrue(m.getParameter("v1").isConnected());
        assertTrue(ng.isOutputConnected());
        m.update();
        assertEquals(5, m.asInt("v1"));
        assertEquals(10, m.getOutputValue());

        m.getParameter("v1").disconnect();
        assertTrue(m.isDirty());
        assertFalse(ng.isDirty());
        assertFalse(m.getParameter("v1").isConnected());
        assertFalse(ng.isOutputConnected());
        // Numbers reverts to default after disconnection
        m.update();
        assertEquals(0, m.getOutputValue());
    }

    public void testOnlyOneConnect() {
        Network net = (Network) testNetworkType.createNode();
        Node number1 = net.create(numberType);
        Node number2 = net.create(numberType);
        Node negate1 = net.create(negateType);
        negate1.getParameter("value").connect(number1);
        assertTrue(number1.isConnected());
        assertFalse(number2.isConnected());
        assertTrue(negate1.isConnected());
        // Now change the connection to number2.
        negate1.getParameter("value").connect(number2);
        assertFalse(number1.isConnected());
        assertTrue(number2.isConnected());
        assertTrue(negate1.isConnected());
    }

    public void testRemove() {
        // First add a node
        Network net = (Network) testNetworkType.createNode();
        Node number1 = net.create(numberType);
        number1.set("value", 42);
        number1.setRendered();
        net.update();
        assertEquals(42, net.getOutputValue());
        // Now remove and update again
        net.remove(number1);
        net.update();
        // This should cause the network to complain that there is no node to render.
        assertTrue(net.hasError());
        // The output value should revert to the default value.
        assertEquals(0, net.getOutputValue());
    }

    public void testMultiConnect() {
        Network net = (Network) testNetworkType.createNode();
        Node number1 = net.create(numberType);
        number1.set("value", 5);
        Node number2 = net.create(numberType);
        number2.set("value", 10);
        Node multiAdd1 = net.create(multiAddType);
        net.connect(number1, multiAdd1, "values");
        net.connect(number2, multiAdd1, "values");
        assertTrue(number1.isConnected());
        assertTrue(number2.isConnected());
        multiAdd1.setRendered();
        // Test default behaviour
        net.update();
        assertFalse(net.isDirty());
        assertFalse(multiAdd1.isDirty());
        assertEquals(15, net.getOutputValue());
        // Change number1 and see if the change propagates.
        number1.set("value", 3);
        assertTrue(net.isDirty());
        assertTrue(multiAdd1.isDirty());
        net.update();
        assertEquals(13, net.getOutputValue());
        // change number2 and see if the change propagates.
        number2.set("value", 4);
        assertTrue(net.isDirty());
        assertTrue(multiAdd1.isDirty());
        net.update();
        assertEquals(7, net.getOutputValue());
    }

    //// Custom assertions ////

    private void assertConnectionError(Node inputNode, String inputParameter, Node outputNode, String message) {
        try {
            inputNode.getParameter(inputParameter).connect(outputNode);
            fail(message);
        } catch (ConnectionError e) {
        }
    }

}
