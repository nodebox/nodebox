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
 * <p/>
 * Connections are made between ports on the nodes. The connection goes from the output port of the output node
 * (there is only one output port) to an input port on the input node.
 */
public class Connection {

    private Parameter outputParameter;
    private Parameter inputParameter;

    /**
     * Creates a connection between the output (upstream) node and input (downstream) node.
     *
     * @param outputParameter the output (upstream) parameter
     * @param inputParameter  the input (downstream) parameter
     */
    public Connection(Parameter outputParameter, Parameter inputParameter) {
        this.outputParameter = outputParameter;
        this.inputParameter = inputParameter;
    }

    public Parameter getOutputParameter() {
        return outputParameter;
    }

    public Node getOutputNode() {
        if (outputParameter == null) return null;
        return outputParameter.getNode();
    }

    public boolean hasOutputNode() {
        return outputParameter != null;
    }

    public Node getInputNode() {
        if (inputParameter == null) return null;
        return inputParameter.getNode();
    }

    public Parameter getInputParameter() {
        return inputParameter;
    }

    public void markDirtyDownstream() {
        getInputNode().markDirty();
    }

    //// Persistence ////

    public void toXml(StringBuffer xml, String spaces) {
        xml.append(spaces);
        xml.append("<connection");
        xml.append(" outputNode=\"").append(getOutputNode().getName()).append("\"");
        xml.append(" inputNode=\"").append(getInputNode().getName()).append("\"");
        xml.append(" inputParameter=\"").append(getInputParameter().getName()).append("\"");
        xml.append("/>\n");
    }

}
