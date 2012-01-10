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

import static nodebox.base.Preconditions.checkNotNull;

/**
 * Represents a connection between two ports.
 * <p/>
 * Connections are made between ports on the nodes. The connection goes from the output port of the output node
 * (there is only one output port) to an input port on the input node.
 * <p/>
 * This class can only store the connection between one output and one input. Some nodes, such as the merge node,
 * have multiple outputs that connect to the same input. These are connected using multiple connection objects.
 */
public class Connection {

    private final Port output;
    private final Port input;

    /**
     * Creates a connection between the output (upstream) node and input (downstream) node.
     *
     * @param output the output (upstream) parameter
     * @param input  the input (downstream) parameter
     */
    public Connection(Port output, Port input) {
        checkNotNull(output);
        checkNotNull(input);
        this.output = output;
        this.input = input;
    }

    /**
     * Gets the output (upstream) port.
     *
     * @return the output port.
     */
    public Port getOutput() {
        return output;
    }

    /**
     * Return the node for the first output port in this connection.
     *
     * @return the output node
     */
    public Node getOutputNode() {
        return output.getNode();
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

    @Override
    public String toString() {
        return String.format("%s <= %s", getOutput(), getInput());
    }

}
