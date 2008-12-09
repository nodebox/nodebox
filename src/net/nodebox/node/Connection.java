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
    private Parameter inputParameter;

    /**
     * Creates a connection between the output (upstream) node and input (downstream) node.
     *
     * @param outputNode the output node
     * @param inputNode the input node
     * @param inputParameter the input parameter on the node
     */
    public Connection(Node outputNode, Node inputNode, Parameter inputParameter) {
        this.outputNode = outputNode;
        this.inputNode = inputNode;
        this.inputParameter = inputParameter;
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

    public Parameter getInputParameter() {
        return inputParameter;
    }

    public void markDirtyDownstream() {
        getInputNode().markDirty();
    }

}
