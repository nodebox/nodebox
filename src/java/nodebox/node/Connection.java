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

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

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

    private final String outputNode;
    private final String inputNode;
    private final String inputPort;

    /**
     * Creates a connection between the output (upstream) node and input (downstream) node.
     *
     * @param outputNode The name of the output (upstream) Node.
     * @param inputNode  The name of the input (downstream) Node.
     * @param inputPort  The name of the input (downstream) Port.
     */
    public Connection(String outputNode, String inputNode, String inputPort) {
        checkNotNull(outputNode);
        checkNotNull(inputNode);
        checkNotNull(inputPort);
        this.outputNode = outputNode;
        this.inputNode = inputNode;
        this.inputPort = inputPort;
    }

    public String getOutputNode() {
        return outputNode;
    }

    public String getInputNode() {
        return inputNode;
    }

    /**
     * Gets the input (downstream) port.
     *
     * @return the input port.
     */
    public String getInputPort() {
        return inputPort;
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return Objects.hashCode(outputNode, inputNode, inputPort);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Connection)) return false;
        final Connection other = (Connection) o;
        return Objects.equal(outputNode, other.outputNode)
                && Objects.equal(inputNode, other.inputNode)
                && Objects.equal(inputPort, other.inputPort);
    }

    @Override
    public String toString() {
        return String.format("%s <= %s.%s", getOutputNode(), getInputNode(), getInputPort());
    }

}
