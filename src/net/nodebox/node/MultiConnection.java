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

    @Override
    public Parameter getOutputParameter() {
        if (outputParameters.isEmpty()) return null;
        return outputParameters.get(0);
    }

    public List<Parameter> getOutputParameters() {
        return outputParameters;
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

    //// Persistence ////

    public void toXml(StringBuffer xml, String spaces) {
        for (Parameter outputParameter : outputParameters) {
            toXml(xml, spaces, outputParameter);
        }
    }

}
