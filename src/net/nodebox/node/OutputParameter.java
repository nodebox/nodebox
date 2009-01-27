package net.nodebox.node;

import java.util.ArrayList;
import java.util.List;

public class OutputParameter extends Parameter {

    public OutputParameter(ParameterType parameterType, Node node) {
        super(parameterType, node);
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
        throw new ConnectionError(this, null, "You can only connect from an input parameter.");
    }

    @Override
    public boolean disconnect() {
        throw new ConnectionError(this, null, "You can only disconnect from an input parameter.");
    }

    @Override
    public void validate(Object value) {
        throw new AssertionError("Code should never validate output parameters.");
    }

    @Override
    public boolean isConnected() {
        if (getNetwork() == null) return false;
        return getNetwork().isConnected(this);
    }

    @Override
    public boolean isConnectedTo(Parameter inputParameter) {
        // Output parameters can only be connected to input parameters.
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
        // OutputParameter accesses the value directly,  since setValue in Parameter checks if the parameter is connected,
        // which has different semantics for an OutputParameter.
        getParameterType().validate(value);
        this.value = value;
        fireValueChanged();
    }


    public void setParameterType(ParameterType parameterType) {
        ParameterType oldType = getParameterType();
        ParameterType newType = parameterType;
        super.setParameterType(parameterType);
        // If the new parameter type has a different core type,
        // we reset the output value to the default value.
        // The default behaviour (implemented in Parameter) of converting
        // the type will almost certainly give the wrong result, since the
        // processing will be different.
        if (oldType.getCoreType() != newType.getCoreType()) {
            if (isConnected()) {
                for (Connection c : getDownstreamConnections()) {
                    c.disconnect();
                }
            }
            value = newType.getDefaultValue();
        }
    }

}
