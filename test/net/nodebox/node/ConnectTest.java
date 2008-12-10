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

public class ConnectTest extends TestCase {

    public void testDirty() {
        NumberGenerator ng = new NumberGenerator();
        assertTrue(ng.isDirty());
        ng.update();
        assertFalse(ng.isDirty());
        assertEquals(0, ng.getOutputValue());
        ng.set("number", 12);
        assertTrue(ng.isDirty());
        // Asking for the output value doesn't update the node implicitly.
        assertEquals(0, ng.getOutputValue());
        // You have to explicitly update the node to get the new output value.
        ng.update();
        assertFalse(ng.isDirty());
        assertEquals(12, ng.getOutputValue());
    }


    public void testConnect() {
        NumberGenerator ng = new NumberGenerator();
        Multiplier m = new Multiplier();

        assertFalse(m.getParameter("number").isConnected());
        assertFalse(m.getParameter("number").isConnectedTo(ng));
        assertFalse(ng.isOutputConnected());
        assertFalse(ng.isOutputConnectedTo(m));
        assertFalse(ng.isOutputConnectedTo(m.getParameter("number")));

        assertTrue(m.getParameter("number").canConnectTo(ng));
        assertTrue(m.getParameter("multiplier").canConnectTo(ng));
        assertFalse(m.getParameter("somestring").canConnectTo(ng));

        Connection conn = m.getParameter("number").connect(ng);
        assertTrue(m.getParameter("number").isConnected());
        assertTrue(m.getParameter("number").isConnectedTo(ng));
        assertTrue(ng.isOutputConnected());
        assertTrue(ng.isOutputConnectedTo(m));
        assertTrue(ng.isOutputConnectedTo(m.getParameter("number")));
        assertEquals(m.getParameter("number"), conn.getInputParameter());
        assertEquals(ng.getOutputParameter(), conn.getOutputParameter());
        assertEquals(m, conn.getInputNode());
        assertEquals(ng, conn.getOutputNode());

        assertConnectionError(m, "somestring", ng, "Somestring is of the wrong type and should not be connectable to NumberGenerator's output.");
    }

    public void testCycles() {
        NumberGenerator ng = new NumberGenerator();
//        assertConnectionError(ng, "number", ng, "Nodes cannot connect to themselves.");
        // TODO: more complex cyclic checks (A->B->A)
    }

    public void testDirtyPropagation() {
        NumberGenerator ng = new NumberGenerator();
        Multiplier m = new Multiplier();
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
        m.getParameter("number").connect(ng);
        assertFalse(ng.isDirty());
        assertTrue(m.isDirty());
        m.update();
        assertFalse(ng.isDirty()); // This shouldn't have changed.
        assertFalse(m.isDirty());
        // A change to the upstream node should make downstream nodes dirty.
        ng.set("number", 12);
        assertTrue(ng.isDirty());
        assertTrue(m.isDirty());
        // Updating the downstream node should make all upstreams clean,
        // because their output values are needed to calculate the downstream.
        m.update();
        assertFalse(ng.isDirty());
        assertFalse(m.isDirty());
        // Changes to the downstream node don't affect upstreams.
        m.set("multiplier", 1);
        assertFalse(ng.isDirty());
        assertTrue(m.isDirty());
        m.update();
        assertFalse(m.isDirty());
        // Disconnecting makes the downstream dirty.
        m.getParameter("number").disconnect();
        assertFalse(ng.isDirty());
        assertTrue(m.isDirty());
        // Check is disconnected nodes still propagate.
        ng.update();
        assertFalse(ng.isDirty());
        assertTrue(m.isDirty());
        m.update();
        assertFalse(m.isDirty());
        ng.set("number", 13);
        assertTrue(ng.isDirty());
        assertFalse(m.isDirty());
    }

    public void testValuePropagation() {
        NumberGenerator ng = new NumberGenerator();
        Multiplier m = new Multiplier();
        m.set("multiplier", 2);
        m.getParameter("number").connect(ng);
        assertEquals(0, m.getOutputValue());
        ng.set("number", 3);
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
        m.getParameter("number").disconnect();
        assertTrue(m.isDirty());
        assertFalse(ng.isDirty());
        ng.set("number", 3);
        m.update();
        assertEquals(0, m.getOutputValue());
    }

    public void testDisconnect() {
        NumberGenerator ng = new NumberGenerator();
        Multiplier m = new Multiplier();
        m.set("multiplier", 2);
        ng.set("number", 5);
        m.getParameter("number").connect(ng);
        assertTrue(m.getParameter("number").isConnected());
        assertTrue(ng.isOutputConnected());
        m.update();
        assertEquals(5, m.asInt("number"));
        assertEquals(10, m.getOutputValue());

        m.getParameter("number").disconnect();
        assertTrue(m.isDirty());
        assertFalse(ng.isDirty());
        assertFalse(m.getParameter("number").isConnected());
        assertFalse(ng.isOutputConnected());
        // Numbers reverts to default after disconnection
        m.update();
        assertEquals(0, m.getOutputValue());
    }

    //// Custom assertions ////

    private void assertConnectionError(Node inputNode, String inputParameter, Node outputNode, String message) {
        try {
            inputNode.getParameter(inputParameter).connect(outputNode);
            fail(message);
        } catch (ConnectionError e) {
        }
    }

    //// Custom nodes ////

    private class NumberGenerator extends Node {

        private NumberGenerator() {
            super(Parameter.Type.INT);
            addParameter("number", Parameter.Type.INT);
        }

        @Override
        protected boolean process(ProcessingContext ctx) {
            setOutputValue(asInt("number"));
            return true;
        }
    }

    private class Multiplier extends Node {

        private Multiplier() {
            super(Parameter.Type.INT);
            addParameter("number", Parameter.Type.INT);
            addParameter("multiplier", Parameter.Type.INT);
            addParameter("somestring", Parameter.Type.STRING);
        }

        @Override
        protected boolean process(ProcessingContext ctx) {
            setOutputValue(asInt("number") * asInt("multiplier"));
            return true;
        }
    }

}
