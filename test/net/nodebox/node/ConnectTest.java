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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConnectTest extends NodeTestCase {

    public void testDirty() {
        Node ng = numberNode.newInstance(testLibrary, "number1");
        assertTrue(ng.isDirty());
        ng.update();
        assertFalse(ng.isDirty());
        assertEquals(0, ng.getOutputValue());
        ng.setValue("value", 12);
        assertTrue(ng.isDirty());
        // Asking for the output value doesn't update the node implicitly.
        assertEquals(0, ng.getOutputValue());
        // You have to explicitly update the node to get the new output value.
        ng.update();
        assertFalse(ng.isDirty());
        assertEquals(12, ng.getOutputValue());
    }

    public void testConnect() {
        Node number1 = numberNode.newInstance(testLibrary, "number1");
        Node multiply1 = multiplyNode.newInstance(testLibrary, "multiply1");
        Node upper1 = convertToUppercaseNode.newInstance(testLibrary, "upper1");

        assertFalse(multiply1.getPort("v1").isConnected());
        assertFalse(multiply1.getPort("v1").isConnectedTo(number1));
        assertFalse(number1.isOutputConnected());
        assertFalse(number1.isOutputConnectedTo(multiply1));
        assertFalse(number1.isOutputConnectedTo(multiply1.getPort("v1")));

        assertTrue(multiply1.getPort("v1").canConnectTo(number1));
        assertTrue(multiply1.getPort("v2").canConnectTo(number1));
        assertFalse(convertToUppercaseNode.getPort("value").canConnectTo(number1));

        Connection conn = multiply1.getPort("v1").connect(number1);
        assertTrue(multiply1.getPort("v1").isConnected());
        assertTrue(multiply1.getPort("v1").isConnectedTo(number1));
        assertTrue(number1.isOutputConnected());
        assertTrue(number1.isOutputConnectedTo(multiply1));
        assertTrue(number1.isOutputConnectedTo(multiply1.getPort("v1")));
        assertEquals(multiply1.getPort("v1"), conn.getInput());
        assertEquals(number1.getOutputPort(), conn.getOutput());
        assertEquals(multiply1, conn.getInputNode());
        assertEquals(number1, conn.getOutputNode());

        assertConnectionError(upper1, "value", number1, "Value is of the wrong type and should not be connectable to NumberIn's output.");
    }

    /**
     * Test if output values can be cast to their superclass.
     */
    public void testConnectCasting() {
        Node upstream, downstream;
        // Both are of the same type. Should be able to connect.
        upstream = Node.ROOT_NODE.newInstance(testLibrary, "upstream", HashMap.class);
        downstream = Node.ROOT_NODE.newInstance(testLibrary, "downstream");
        downstream.addPort("value", HashMap.class);
        downstream.getPort("value").connect(upstream);
        // Reset the library
        testLibrary = new NodeLibrary("test");
        // Upstream is a more specific type, which is allowed.
        upstream = Node.ROOT_NODE.newInstance(testLibrary, "upstream", LinkedHashMap.class);
        downstream = Node.ROOT_NODE.newInstance(testLibrary, "downstream");
        downstream.addPort("value", HashMap.class);
        downstream.getPort("value").connect(upstream);
        // Reset the library
        testLibrary = new NodeLibrary("test");
        // Now downstream is more specific, which is NOT allowed.
        upstream = Node.ROOT_NODE.newInstance(testLibrary, "upstream", HashMap.class);
        downstream = Node.ROOT_NODE.newInstance(testLibrary, "downstream");
        downstream.addPort("value", LinkedHashMap.class);
        assertConnectionError(downstream, "value", upstream, "Downstream is a more specific type.");
        // Reset the library
        testLibrary = new NodeLibrary("test");
        // Downstream is an interface which upstream implements.
        upstream = Node.ROOT_NODE.newInstance(testLibrary, "upstream", LinkedHashMap.class);
        downstream = Node.ROOT_NODE.newInstance(testLibrary, "downstream");
        downstream.addPort("value", Map.class);
        downstream.getPort("value").connect(upstream);
    }

    public void testCycles() {
        Node number1 = numberNode.newInstance(testLibrary, "number1");
//        assertConnectionError(ng, "number", ng, "Nodes cannot connect to themselves.");
        // TODO: more complex cyclic checks (A->B->A)
    }

    public void testDirtyPropagation() {
        Node number1 = numberNode.newInstance(testLibrary, "number1");
        Node addConstant1 = addConstantNode.newInstance(testLibrary, "addConstant1");
        // Nodes start out dirty
        assertTrue(number1.isDirty());
        assertTrue(addConstant1.isDirty());
        // Updating makes them clean
        number1.update();
        // addConstant1 will throw an error since it needs input.
        assertProcessingError(addConstant1, NullPointerException.class);
        assertFalse(number1.isDirty());
        // When a node throws an error it is not clean.
        assertTrue(addConstant1.isDirty());
        // Connecting the add constant to another node makes it dirty.
        // The output (upstream) node doesn't become dirty.
        addConstant1.getPort("value").connect(number1);
        assertFalse(number1.isDirty());
        assertTrue(addConstant1.isDirty());
        addConstant1.update();
        assertFalse(number1.isDirty()); // This shouldn't have changed.
        assertFalse(addConstant1.isDirty());
        // A change to the upstream node should make downstream nodes dirty.
        number1.setValue("value", 12);
        assertTrue(number1.isDirty());
        assertTrue(addConstant1.isDirty());
        // Updating the downstream node should make all upstreams clean,
        // because their output values are needed to calculate the downstream.
        addConstant1.update();
        assertFalse(number1.isDirty());
        assertFalse(addConstant1.isDirty());
        // Changes to the downstream node don't affect upstreams.
        addConstant1.setValue("constant", 1);
        assertFalse(number1.isDirty());
        assertTrue(addConstant1.isDirty());
        addConstant1.update();
        assertFalse(addConstant1.isDirty());
        // Disconnecting makes the downstream dirty.
        addConstant1.getPort("value").disconnect();
        assertFalse(number1.isDirty());
        assertTrue(addConstant1.isDirty());
        // Connect addConstant1 to a new node.
        Node number2 = numberNode.newInstance(testLibrary, "number2");
        addConstant1.getPort("value").connect(number2);
        // Check if disconnected nodes still propagate.
        number1.update();
        assertFalse(number1.isDirty());
        assertTrue(addConstant1.isDirty());
        addConstant1.update();
        assertFalse(addConstant1.isDirty());
        number1.setValue("value", 13);
        assertTrue(number1.isDirty());
        assertFalse(addConstant1.isDirty());
    }

    public void testValuePropagation() {
        Node number1 = numberNode.newInstance(testLibrary, "number1");
        Node number2 = numberNode.newInstance(testLibrary, "number2");
        Node m = multiplyNode.newInstance(testLibrary, "multiply1");
        m.getPort("v1").connect(number1);
        m.getPort("v2").connect(number2);
        assertNull(m.getOutputValue());
        number1.setValue("value", 3);
        number2.setValue("value", 2);
        assertTrue(m.isDirty());
        assertNull(m.getOutputValue());
        // Updating the NumberIn node has no effect on the multiplier node.
        number1.update();
        assertTrue(m.isDirty());
        assertNull(m.getOutputValue());
        m.update();
        assertFalse(m.isDirty());
        assertEquals(6, m.getOutputValue());
        // Test if value stops propagating after disconnection.
        m.getPort("v1").disconnect();
        assertFalse(m.getPort("v1").isConnected());
        assertTrue(m.isDirty());
        // The value is still the old value because the node has not been updated yet.
        assertEquals(6, m.getOutputValue());
        assertFalse(number1.isDirty());
        number1.setValue("value", 3);
        assertProcessingError(m, NullPointerException.class);
        assertNull(m.getOutputValue());
    }

    public void testDisconnect() {
        Node number1 = numberNode.newInstance(testLibrary, "number1");
        Node number2 = numberNode.newInstance(testLibrary, "number2");
        Node m = multiplyNode.newInstance(testLibrary, "multiply1");
        number1.setValue("value", 5);
        number2.setValue("value", 2);
        m.getPort("v1").connect(number1);
        m.getPort("v2").connect(number2);
        assertTrue(m.getPort("v1").isConnected());
        assertTrue(number1.isOutputConnected());
        m.update();
        assertEquals(5, m.getPort("v1").getValue());
        assertEquals(10, m.getOutputValue());

        // Disconnecting a port makes the dependent nodes dirty, but not the upstream nodes.
        // "Dirt flows downstream"
        m.getPort("v1").disconnect();
        assertTrue(m.isDirty());
        assertFalse(number1.isDirty());
        assertFalse(m.getPort("v1").isConnected());
        assertFalse(number1.isOutputConnected());
        // The value of the input port is set to null after disconnection.
        // Since our simple multiply node doesn't handle null, it throws
        // a NullPointerException, which gets wrapped in a ProcessingError.
        assertProcessingError(m, NullPointerException.class);
        assertNull(m.getOutputValue());
    }

    public void testOnlyOneConnect() {
        Node number1 = numberNode.newInstance(testLibrary, "number1");
        Node number2 = numberNode.newInstance(testLibrary, "number2");
        Node negate1 = negateNode.newInstance(testLibrary, "negate1");
        negate1.getPort("value").connect(number1);
        assertTrue(number1.isConnected());
        assertFalse(number2.isConnected());
        assertTrue(negate1.isConnected());
        // Now change the connection to number2.
        negate1.getPort("value").connect(number2);
        assertFalse(number1.isConnected());
        assertTrue(number2.isConnected());
        assertTrue(negate1.isConnected());
    }

    public void testRemove() {
        // First add a node
        Node net = testNetworkNode.newInstance(testLibrary, "net1");
        Node number1 = net.create(numberNode);
        number1.setValue("value", 42);
        number1.setRendered();
        net.update();
        assertEquals(42, net.getOutputValue());
        // Now remove and update again
        net.remove(number1);
        assertNull(net.getRenderedChild());
        // This should cause the network to complain that there is no node to render.
        assertProcessingError(net, ProcessingError.class);
        // The output value should revert to null.
        assertEquals(null, net.getOutputValue());
    }

    public void testMultiConnect() {
        Node net = testNetworkNode.newInstance(testLibrary, "net1");
        Node number1 = net.create(numberNode);
        number1.setValue("value", 5);
        Node number2 = net.create(numberNode);
        number2.setValue("value", 10);
        Node multiAdd1 = net.create(multiAddNode);
        multiAdd1.getPort("values").connect(number1);
        multiAdd1.getPort("values").connect(number2);
        assertTrue(number1.isConnected());
        assertTrue(number2.isConnected());
        multiAdd1.setRendered();
        // Test default behaviour
        net.update();
        assertFalse(net.isDirty());
        assertFalse(multiAdd1.isDirty());
        assertEquals(15, net.getOutputValue());
        // Change number1 and see if the change propagates.
        number1.setValue("value", 3);
        assertTrue(net.isDirty());
        assertTrue(multiAdd1.isDirty());
        net.update();
        assertEquals(13, net.getOutputValue());
        // change number2 and see if the change propagates.
        number2.setValue("value", 4);
        assertTrue(net.isDirty());
        assertTrue(multiAdd1.isDirty());
        net.update();
        assertEquals(7, net.getOutputValue());
    }

}
