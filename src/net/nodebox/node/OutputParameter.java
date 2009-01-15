package net.nodebox.node;

import java.util.ArrayList;
import java.util.List;

public class OutputParameter extends Parameter {

    /**
     * I have my own value, since setValue in Parameter checks if the parameter is connected, which has different
     * semantics for an OutputParameter.
     */
    private Object value;

    public OutputParameter(ParameterType parameterType, Node node) {
        super(parameterType, node);
        value = getParameterType().getDefaultValue();
    }

    public boolean isInputParameter() {
        return false;
    }

    public List<Connection> getDownstreamConnections() {
        if (getNetwork() == null) {
            return new ArrayList<Connection>();
        } else {
            return getNetwork().getDownstreamConnections(this);
        }
    }

    public void markDirtyDownstream() {
        for (Connection c : getDownstreamConnections()) {
            c.markDirtyDownstream();
        }
    }

    @Override
    public Connection connect(Node outputNode) {
        throw new ConnectionError(outputNode, this, "You can only connect from an input parameter.");
    }

    @Override
    public boolean disconnect() {
        throw new ConnectionError(getNode(), this, "You can only disconnect from an input parameter.");
    }

    @Override
    public void validate(Object value) {
        throw new AssertionError("Code should never validate output parameters.");
    }

    @Override
    public boolean isConnected() {
        return getNetwork().isConnected(this);
    }

    @Override
    public boolean isConnectedTo(Parameter inputParameter) {
        // Output paramters can only be connected to input parameters.
        if (inputParameter instanceof OutputParameter) return false;
        return getNetwork().isConnectedTo(inputParameter, this);
    }

    @Override
    public boolean canConnectTo(Parameter parameter) {
        // Output paramters can only be connected to input parameters.
        if (parameter instanceof OutputParameter) return false;
        return super.canConnectTo(parameter);
    }

    @Override
    public boolean isConnectedTo(Node node) {
        // Look for all input parameters on the node
        for (Parameter p : node.getParameters()) {
            if (getNetwork().isConnectedTo(p, this))
                return true;
        }
        return false;
    }

    public void setValue(Object value) throws ValueError {
        getParameterType().validate(value);
        this.value = value;
        fireValueChanged();
    }

    public Object getValue() {
        return value;
    }

}
