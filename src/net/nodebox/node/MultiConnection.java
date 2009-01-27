package net.nodebox.node;

import java.util.ArrayList;
import java.util.List;

public class MultiConnection extends Connection {

    private List<Parameter> outputParameters;

    public MultiConnection(Parameter outputParameter, Parameter inputParameter) {
        this(outputParameter, inputParameter, Connection.Type.EXPLICIT);
    }

    public MultiConnection(Parameter outputParameter, Parameter inputParameter, Type type) {
        super(null, inputParameter, type);
        outputParameters = new ArrayList<Parameter>();
        outputParameters.add(outputParameter);
    }

    public void disconnect() {
        for (Parameter outputParameter : outputParameters) {
            getInputParameter().getNetwork().disconnect(outputParameter, getInputParameter(), getType());
        }
    }

    @Override
    public Parameter getOutputParameter() {
        if (outputParameters.isEmpty()) return null;
        return outputParameters.get(0);
    }

    @Override
    public boolean hasOutputParameter(Parameter parameter) {
        for (Parameter outputParameter : outputParameters) {
            if (outputParameter == parameter)
                return true;
        }
        return false;
    }

    @Override
    public List<Parameter> getOutputParameters() {
        return outputParameters;
    }

    @Override
    public List<Node> getOutputNodes() {
        List<Node> outputNodes = new ArrayList<Node>(outputParameters.size());
        for (Parameter outputParameter : outputParameters)
            outputNodes.add(outputParameter.getNode());
        return outputNodes;
    }

    public void addOutputParameter(Parameter outputParameter) {
        outputParameters.add(outputParameter);
    }

    @Override
    public void update(ProcessingContext ctx) {
        List<Object> values = new ArrayList<Object>();
        for (Parameter outputParameter : outputParameters) {
            // Check if the output node on the connection is not the same as my node.
            // In that case, we don't want to process the node, since it will eventually
            // end up updating this parameter, causing infinite recursion.
            if (outputParameter.getNode() == getInputNode()) continue;
            outputParameter.getNode().update(ctx);
            values.add(outputParameter.getNode().getOutputValue());
        }
        outputValue = values;
    }

    @Override
    public String toString() {
        return getInputParameter() + " => " + getOutputParameters();
    }

    //// Persistence ////

    public void toXml(StringBuffer xml, String spaces) {
        for (Parameter outputParameter : outputParameters) {
            toXml(xml, spaces, outputParameter);
        }
    }

}
