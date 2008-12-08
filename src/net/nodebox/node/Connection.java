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

/**
 * Represents a connection between two nodes.
 *
 * Connections are made between ports on the nodes. The connection goes from the output port of the output node
 * (there is only one output port) to an input port on the input node.
 */
public class Connection {

    private Node outputNode;
    private Node inputNode;
    private int inputPort;

    /**
     * Creates a connection between the output (upstream) node and input (downstream) node.
     *
     * @param fromNode the output node
     * @param toNode   the input node
     * @param toInput  the input port on the node
     */
    public Connection(Node fromNode, Node toNode, int toInput) {
        outputNode = fromNode;
        inputNode = toNode;
        inputPort = toInput;
    }

    public Node getOutputNode() {
        return outputNode;
    }

    public boolean hasOutputNode() {
        return outputNode != null;
    }

    public Node getInputNode() {
        return inputNode;
    }

    public int getInputPort() {
        return inputPort;
    }

    public void markDirtyDownstream() {
        getInputNode().markDirty();
    }
    
}
