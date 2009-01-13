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
import java.util.List;

/**
 * Represents a connection between two nodes.
 * <p/>
 * Connections are made between ports on the nodes. The connection goes from the output port of the output node
 * (there is only one output port) to an input port on the input node.
 */
public class Connection {

    public enum Type {
        EXPLICIT, IMPLICIT
    }

    private Parameter outputParameter;
    private Parameter inputParameter;
    private Type type;
    protected Object outputValue;

    /**
     * Creates a connection between the output (upstream) node and input (downstream) node.
     *
     * @param outputParameter the output (upstream) parameter
     * @param inputParameter  the input (downstream) parameter
     */
    public Connection(Parameter outputParameter, Parameter inputParameter) {
        this(outputParameter, inputParameter, Type.EXPLICIT);
    }

    public Connection(Parameter outputParameter, Parameter inputParameter, Type type) {
        this.outputParameter = outputParameter;
        this.inputParameter = inputParameter;
        this.type = type;
    }

    public Parameter getOutputParameter() {
        return outputParameter;
    }

    public boolean hasOutputParameter(Parameter parameter) {
        return outputParameter == parameter;
    }

    /**
     * Convenience method that gets overriden in MultiConnection.
     *
     * @return
     */
    public List<Parameter> getOutputParameters() {
        List<Parameter> outputParameters = new ArrayList<Parameter>(1);
        outputParameters.add(outputParameter);
        return outputParameters;
    }

    public Node getOutputNode() {
        if (outputParameter == null) return null;
        return outputParameter.getNode();
    }

    /**
     * Convenience method that gets overriden in MultiConnection.
     *
     * @return
     */
    public List<Node> getOutputNodes() {
        List<Node> outputNodes = new ArrayList<Node>(1);
        if (outputParameter != null)
            outputNodes.add(outputParameter.getNode());
        return outputNodes;
    }


    public Node getInputNode() {
        if (inputParameter == null) return null;
        return inputParameter.getNode();
    }

    public Parameter getInputParameter() {
        return inputParameter;
    }

    public Type getType() {
        return type;
    }

    public boolean isExplicit() {
        return type == Type.EXPLICIT;
    }

    public boolean isImplicit() {
        return type == Type.IMPLICIT;
    }

    public void markDirtyDownstream() {
        getInputNode().markDirty();
    }

    public void update(ProcessingContext ctx) {
        // Check if the output node on the connection is not the same as my node.
        // In that case, we don't want to process the node, since it will eventually
        // end up updating this parameter, causing infinite recursion.
        if (getOutputNode() == getInputNode()) return;
        getOutputNode().update(ctx);
        outputValue = getOutputNode().getOutputValue();
    }

    public Object getOutputValue() {
        return outputValue;
    }

    //// Persistence ////

    public void toXml(StringBuffer xml, String spaces) {
        toXml(xml, spaces, outputParameter);
    }

    protected void toXml(StringBuffer xml, String spaces, Parameter outputParameter) {
        xml.append(spaces);
        xml.append("<connection");
        xml.append(" outputNode=\"").append(outputParameter.getNode().getName()).append("\"");
        xml.append(" inputNode=\"").append(getInputNode().getName()).append("\"");
        xml.append(" inputParameter=\"").append(getInputParameter().getName()).append("\"");
        xml.append("/>\n");
    }

}
