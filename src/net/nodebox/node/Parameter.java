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

import net.nodebox.graphics.Color;
import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * A parameter controls the operation of a Node. It provide an interface into the workings of a node and allows a user
 * to change its behaviour. Parameters are represented by standard user interface controls, such as sliders for numbers,
 * text fields for strings, and checkboxes for booleans.
 * <p/>
 * Parameters implement the observer pattern for expressions. Parameters that are dependent on other parameters because
 * of their expressions will observe the parameters they depend on, and marked the node as dirty whenever they receive
 * an update event from one of the parameters they depend on.
 */
public class Parameter implements ParameterTypeListener {

    private ParameterType parameterType;
    private Node node;
    private Object value;
    private boolean valueSet = false;
    private Expression expression;
    private boolean expressionEnabled;
    private Connection connection;
    private List<Connection> expressionConnections = new ArrayList<Connection>();
    /**
     * A list of Parameters that want to be notified when my value changes.
     */
    private List<Parameter> dependents = new ArrayList<Parameter>();
    /**
     * A list of Parameters that I rely on, and that need to notify me when their value changes.
     */
    private List<Parameter> dependencies = new ArrayList<Parameter>();

    /**
     * A list of listeners that want to be notified when my value changes.
     */
    private List<ParameterDataListener> listeners = new ArrayList<ParameterDataListener>();

    public Parameter(ParameterType parameterType, Node node) {
        this.parameterType = parameterType;
        this.node = node;
        this.value = parameterType.getDefaultValue();
    }

    //// Basic operations ////

    public Node getNode() {
        return node;
    }

    //// Naming ////

    public String getName() {
        return parameterType.getName();
    }

    public String getAbsolutePath() {
        return getNode().getAbsolutePath() + "/" + getName();
    }

    public String getLabel() {
        return parameterType.getLabel();
    }

    public String getHelpText() {
        return parameterType.getDescription();
    }

    //// Type ////

    public ParameterType getParameterType() {
        return parameterType;
    }

    public ParameterType.Type getType() {
        return parameterType.getType();
    }

    public boolean isPrimitive() {
        return parameterType.isPrimitive();
    }

    public ParameterType.CoreType getCoreType() {
        return parameterType.getCoreType();
    }

    public void typeChanged(ParameterType source) {
        // todo: Migrate type to new type.
    }

    //// Bounding ////

    public void boundingChanged(ParameterType source) {
        if (source.getBoundingMethod() == ParameterType.BoundingMethod.HARD)
            clampToBounds();
    }

    private void clampToBounds() {
        if (getCoreType() == ParameterType.CoreType.INT) {
            int v = (Integer) value;
            if (parameterType.getMinimumValue() != null && v < parameterType.getMinimumValue()) {
                set(parameterType.getMinimumValue().intValue());
            } else if (parameterType.getMaximumValue() != null && v > parameterType.getMaximumValue()) {
                set(parameterType.getMaximumValue().intValue());
            }
        } else if (getCoreType() == ParameterType.CoreType.FLOAT) {
            double v = (Double) value;
            if (parameterType.getMinimumValue() != null && v < parameterType.getMinimumValue()) {
                set(parameterType.getMinimumValue());
            } else if (parameterType.getMaximumValue() != null && v > parameterType.getMaximumValue()) {
                set(parameterType.getMaximumValue());
            }
        }
    }

    //// Display level ////

    public void displayLevelChanged(ParameterType source) {
        // TODO: inform node of metadata changes so views can update.
    }

    //// Values ////

    public int asInt() {
        if (getCoreType() == ParameterType.CoreType.INT) {
            return (Integer) value;
        } else if (getCoreType() == ParameterType.CoreType.FLOAT) {
            double v = (Double) value;
            return (int) v;
        } else {
            return 0;
        }
    }

    public double asFloat() {
        if (getCoreType() == ParameterType.CoreType.FLOAT) {
            return (Double) value;
        } else if (getCoreType() == ParameterType.CoreType.INT) {
            int v = (Integer) value;
            return (double) v;
        } else {
            return 0;
        }
    }

    public String asString() {
        if (getCoreType() == ParameterType.CoreType.STRING) {
            return (String) value;
        } else {
            return value.toString();
        }
    }

    public Color asColor() {
        if (getCoreType() == ParameterType.CoreType.COLOR) {
            return (Color) value;
        } else {
            return new Color();
        }
    }

    public Grob asGrob() {
        if (getCoreType() == ParameterType.CoreType.GROB_SHAPE
                || getCoreType() == ParameterType.CoreType.GROB_CANVAS
                || getCoreType() == ParameterType.CoreType.GROB_IMAGE) {
            return (Grob) value;
        } else {
            return new Group();
        }
    }

    public Object getValue() {
        return value;
    }

    public void set(Object value) {
        setValue(value);
    }

    public void setValue(Object value) throws ValueError {
        if (isConnected()) {
            throw new ValueError("The parameter is connected.");
        }
        if (hasExpression()) {
            throw new ValueError("The parameter has an expression set.");
        }
        parameterType.validate(value);
        this.value = value;
        fireValueChanged();
    }

    //// Expressions ////

    public boolean hasExpression() {
        return expression != null;
    }

    public String getExpression() {
        return hasExpression() ? expression.getExpression() : "";
    }

    public void setExpression(String expression) {
        if (hasExpression() && expression.equals(getExpression())) {
            return;
        }
        if (expression == null || expression.trim().length() == 0) {
            this.expression = null;
            setExpressionEnabled(false);
        } else {
            this.expression = new Expression(this, expression);
            updateDependencies();
            // Setting an expession automatically enables it and marks the node as dirty.
            setExpressionEnabled(true);
        }
        fireValueChanged();
    }

    public void setExpressionEnabled(boolean enabled) {
        if (this.expressionEnabled == enabled) {
            return;
            // Before you can enable the expression, you have to have one.
        }
        if (enabled && !hasExpression()) {
            return;
        }
        this.expressionEnabled = enabled;
        // Since the value of this parameter will change, we mark the node as dirty.
        node.fireNodeChanged();
        fireValueChanged();
    }

    //// Expression dependencies ////

    /**
     * The parameter dependencies function like a directed-acyclic graph, just like the node framework itself.
     * Parameter depencies are created by setting expressions that refer to other parameters. Once these parameters
     * are changed, the dependent parameters need to be changed as well.
     */
    private void updateDependencies() {
        clearDependencies();
        for (Parameter p : expression.getDependencies()) {
            p.addDependent(this);
            dependencies.add(p);
        }
    }

    private void clearDependencies() {
        for (Parameter p : dependencies) {
            p.removeDependent(this);
        }
        dependencies.clear();
    }

    private void addDependent(Parameter parameter) {
        assert (!dependents.contains(parameter));
        dependents.add(this);
    }

    private void removeDependent(Parameter parameter) {
        assert (dependents.contains(parameter));
        dependents.remove(this);
    }

    public List<Parameter> getDependents() {
        return new ArrayList<Parameter>(dependents);
    }

    public List<Parameter> getDependencies() {
        return new ArrayList<Parameter>(dependencies);
    }

    /**
     * Called whenever the value of this parameter changes. This method informs the dependent parameters that my value
     * has changed. I call the dependencyValueChanged method on these parameters.
     *
     * @see #dependencyValueChanged(Parameter)
     */
    protected void fireValueChanged() {
        for (Parameter p : dependents) {
            p.dependencyValueChanged(this);
        }
        for (ParameterDataListener l : listeners) {
            l.valueChanged(this, value);
        }
        getNode().markDirty();
    }

    /**
     * Called by my parameter dependencies whenever their value changes.
     *
     * @param parameter the parameter that has changed its value.
     */
    private void dependencyValueChanged(Parameter parameter) {
        getNode().markDirty();
    }

    //// Connections ////

    public Connection getConnection() {
        return connection;
    }

    public boolean isCompatible(Node outputNode) {
        return outputNode.getOutputParameter().getType().equals(getType());
    }

    public boolean isConnected() {
        return connection != null;
    }

    public boolean isConnectedTo(Parameter parameter) {
        if (!isConnected()) return false;
        // Parameters can only be connected to output parameters.
        if (!(parameter instanceof OutputParameter)) return false;
        return connection.getOutputParameter() == parameter;
    }

    public boolean isConnectedTo(Node node) {
        if (!isConnected()) return false;
        return connection.getOutputParameter() == node.getOutputParameter();
    }

    public boolean canConnectTo(Parameter parameter) {
        // Parameters can only be connected to output parameters.
        if (!(parameter instanceof OutputParameter)) return false;
        return parameter.getCoreType() == getCoreType();
    }

    public boolean canConnectTo(Node node) {
        return node.getOutputParameter().getCoreType() == getCoreType();
    }

    /**
     * Connects this (input) parameter to the given output node.
     * <p/>
     * Once connected, the node is marked dirty.
     *
     * @param outputNode the upstream node to connect to.
     * @return true if the connection succeeded.
     */
    public Connection connect(Node outputNode) {
        if (node == outputNode)
            throw new ConnectionError(outputNode, this, "You cannot connect a node to itself.");
        if (!canConnectTo(outputNode))
            throw new ConnectionError(outputNode, this, "The parameter types do not match.");
        if (connection != null) disconnect();
        connection = new Connection(outputNode.getOutputParameter(), this);
        outputNode.getOutputParameter().addDownstreamConnection(connection);
        // After the connection is made, check if it creates a cycle, and
        // remove the connection if it does.
        if (getNode().getNetwork() != null) {
            if (getNode().getNetwork().containsCycles()) {
                disconnect();
                throw new ConnectionError(outputNode, this, "This creates a cyclic connection.");
            }
        }
        getNode().markDirty();
        if (getNode().inNetwork())
            getNode().getNetwork().fireConnectionAdded(connection);
        return connection;
    }

    /**
     * Disconnects this (input) parameter from its output node.
     * <p/>
     * If no connection was present, this method does nothing.
     *
     * @return true if the connection was removed
     */
    public boolean disconnect() {
        if (!isConnected()) return false;
        Node outputNode = connection.getOutputNode();
        boolean downstreamRemoved = outputNode.getOutputParameter().getDownstreamConnections().remove(connection);
        assert (downstreamRemoved);
        Connection oldConnection = connection;
        connection = null;
        revertToDefault();
        if (getNode().inNetwork())
            getNode().getNetwork().fireConnectionRemoved(oldConnection);
        return true;
    }

    /**
     * Updates the parameter, making sure all dependencies are clean.
     * <p/>
     * This method can take a long time and should be run in a separate thread.
     *
     * @param ctx the processing context
     */
    public void update(ProcessingContext ctx) {
        if (isConnected()) {
            connection.getOutputNode().update(ctx);
            Object outputValue = connection.getOutputNode().getOutputValue();
            validate(outputValue);
            value = outputValue;
        }
        if (hasExpression()) {
            for (Connection c : expressionConnections) {
                c.getOutputNode().update(ctx);
            }
            Object expressionValue = expression.evaluate();
            parameterType.validate(expressionValue);
            value = expressionValue;
        }
    }

    //// Values ////

    public void nullAllowedChanged(ParameterType source) {
        if (!parameterType.isNullAllowed() && value == null) {
            value = parameterType.getDefaultValue();
        }
    }

    public void revertToDefault() {
        this.value = parameterType.getDefaultValue();
        fireValueChanged();
    }

    public Object parseValue(String valueAsString) {
        return getParameterType().parseValue(valueAsString);
    }

    public void validate(Object value) {
        getParameterType().validate(value);
    }

    //// Event handling ////

    public void addDataListener(ParameterDataListener listener) {
        listeners.add(listener);
    }

    public void removeDataListener(ParameterDataListener listener) {
        listeners.remove(listener);
    }

    //// Persistence ////

    /**
     * Converts the data structure to xml. The xml String is appended to the given StringBuffer.
     *
     * @param xml    the StringBuffer to use when appending.
     * @param spaces the indentation.
     * @see Network#toXml for returning the Network as a full xml document
     */
    public void toXml(StringBuffer xml, String spaces) {
        // Don't do non-primitive parameters.
        if (!isPrimitive()) return;
        // Write parameter name
        xml.append(spaces).append("<key>").append(getName()).append("</key>\n");
        if (hasExpression()) {
            xml.append(spaces).append("<expression>").append(getExpression()).append("</expression>\n");
        } else {
            if (getCoreType() == ParameterType.CoreType.INT) {
                xml.append(spaces).append("<int>").append(asFloat()).append("</int>\n");
            } else if (getCoreType() == ParameterType.CoreType.FLOAT) {
                xml.append(spaces).append("<float>").append(asFloat()).append("</float>\n");
            } else if (getCoreType() == ParameterType.CoreType.STRING) {
                xml.append(spaces).append("<float>").append(asFloat()).append("</float>\n");
            } else if (getCoreType() == ParameterType.CoreType.COLOR) {
                xml.append(spaces).append("<color>").append(asColor().toString()).append("</color>\n");
            } else {
                throw new AssertionError("Unknown value class " + getCoreType());
            }
        }
    }

}
