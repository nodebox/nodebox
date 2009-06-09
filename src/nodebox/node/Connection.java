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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a connection between two nodes.
 * <p/>
 * Connections are made between ports on the nodes. The connection goes from the output port of the output node
 * (there is only one output port) to an input port on the input node.
 * <p/>
 * This class can only store the connection between one output and one input. Some nodes, such as the merge node,
 * have multiple outputs that connect to the same input. These are connected using a MultiConnection.
 */
public class Connection {

    private List<Port> outputs;
    private Port input;

    /**
     * Creates a connection between the output (upstream) node and input (downstream) node.
     *
     * @param output the output (upstream) parameter
     * @param input  the input (downstream) parameter
     */
    public Connection(Port output, Port input) {
        assert output != null;
        assert input != null;
        this.outputs = new ArrayList<Port>(1);
        this.outputs.add(output);
        this.input = input;
    }

    /**
     * Gets the first output (upstream) port.
     *
     * @return the output port.
     */
    public Port getOutput() {
        assert outputs.size() > 0;
        return outputs.get(0);
    }

    public boolean hasOutput(Port port) {
        return outputs.contains(port);
    }

    /**
     * Get a list with all output ports in this connection.
     *
     * @return the list of output ports. This list can safely be modified.
     */
    public List<Port> getOutputs() {
        return new ArrayList<Port>(outputs);
    }

    /**
     * Check if this connection still has any output ports.
     *
     * @return true if this connection has output ports.
     */
    public boolean hasOutputs() {
        return outputs.size() > 0;
    }

    /**
     * Add the given output port to this connection.
     *
     * @param port the output port to add.
     * @return true if the port was added, false if this connection already contains the output.
     */
    public boolean addOutput(Port port) {
        if (port == null)
            throw new IllegalArgumentException("The given port cannot be null.");
        if (outputs.contains(port)) return false;
        // Sanity check to see if the output and input ports are not the same,
        // which would cause infinite recursion.
        if (port.getNode() == input.getNode())
            throw new IllegalArgumentException("The output port cannot be on the same node as the input port.");
        outputs.add(port);
        return true;
    }

    /**
     * Remove the given output port from this connection.
     *
     * @param port the port to remove
     * @return true if there are no ouputs remaining.
     * @throws IllegalArgumentException if the given port is not in this connection
     */
    public boolean removeOutput(Port port) throws IllegalArgumentException {
        if (!outputs.contains(port))
            throw new IllegalArgumentException("The given port does not participate in this connection.");
        outputs.remove(port);
        return outputs.isEmpty();
    }

    /**
     * Remove the given node from this connection.
     *
     * @param node the node to remove
     * @return true if there are no ouputs remaining.
     * @throws IllegalArgumentException if the given port is not in this connection
     */
    public boolean removeOutputNode(Node node) throws IllegalArgumentException {
        return removeOutput(node.getOutputPort());
    }

    /**
     * Return the node for the first output port in this connection.
     *
     * @return a Node or null if there are no output ports
     */
    public Node getOutputNode() {
        if (outputs.size() == 0) return null;
        return outputs.get(0).getNode();
    }

    /**
     * Gets a list of all nodes for the output ports in this connection.
     *
     * @return a list of nodes. This list can safely be modified.
     */
    public List<Node> getOutputNodes() {
        List<Node> nodes = new ArrayList<Node>(1);
        for (Port p : outputs) {
            nodes.add(p.getNode());
        }
        return nodes;
    }

    /**
     * Gets the input (downstream) port.
     *
     * @return the input port.
     */
    public Port getInput() {
        return input;
    }

    /**
     * Return the node for the input port in this connection.
     *
     * @return the Node for the input port.
     */
    public Node getInputNode() {
        if (input == null) return null;
        return input.getNode();
    }

    public void markDirtyDownstream() {
        getInputNode().markDirty();
    }

    /**
     * Updates the connection.
     * <p/>
     * Updates all output nodes and combines the results, which is stored in the input port.
     * <p/>
     * Depending on the cardinality of the port, you can retrieve the value using getValue() or getValues().
     *
     * @param ctx the metadata about the update operation.
     * @see nodebox.node.Port#getValue()
     * @see nodebox.node.Port#getValues()
     */
    public void update(ProcessingContext ctx) {
        // TODO: Check if we can break the system with infinite recursion.
        // Clear out the value(s) of the port.
        input.reset();
        if (input.getCardinality() == Port.Cardinality.SINGLE) {
            Node node = getOutputNode();
            node.update(ctx);
            // TODO: We should clone the value here.
            input.setValue(node.getOutputValue());
        } else {
            for (Port output : outputs) {
                Node node = output.getNode();
                node.update(ctx);
                // TODO: We should clone the value here.
                input.addValue(node.getOutputValue());
            }
        }
    }

    @Override
    public String toString() {
        return getOutputs() + " <= " + getInput();
    }

    //// Persistence ////

    public void toXml(StringBuffer xml, String spaces) {
        for (Port output : outputs) {
            toXml(xml, spaces, output);
        }
    }

    protected void toXml(StringBuffer xml, String spaces, Port output) {
        xml.append(spaces);
        xml.append("<conn");
        xml.append(" output=\"").append(output.getNode().getName()).append("\"");
        xml.append(" input=\"").append(getInputNode().getName()).append("\"");
        xml.append(" port=\"").append(getInput().getName()).append("\"");
        xml.append("/>\n");
    }

}
