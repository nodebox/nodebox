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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static Logger logger = Logger.getLogger("net.nodebox.node.Parameter");

    private ParameterType parameterType;
    private Node node;
    private Object value;
    private boolean valueSet = false;
    private Expression expression;
    private boolean expressionEnabled;

    /**
     * A list of listeners that want to be notified when my value changes.
     */
    private List<ParameterDataListener> listeners = new ArrayList<ParameterDataListener>();

    public Parameter(ParameterType parameterType, Node node) {
        this.parameterType = parameterType;
        this.node = node;
        // This returns a clone of the default value.
        if (getCardinality() == ParameterType.Cardinality.SINGLE) {
            this.value = parameterType.getDefaultValue();
        } else {
            this.value = new ArrayList<Object>();
        }
        parameterType.addParameterTypeListener(this);
    }

    //// Basic operations ////

    public Node getNode() {
        return node;
    }

    public Network getNetwork() {
        return node == null ? null : node.getNetwork();
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

    public ParameterType.Cardinality getCardinality() {
        return parameterType.getCardinality();
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
        assertCardinality();
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
        assertCardinality();
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
        assertCardinality();
        if (getCoreType() == ParameterType.CoreType.STRING) {
            return (String) value;
        } else {
            return value.toString();
        }
    }

    public boolean asBoolean() {
        assertCardinality();
        if (getCoreType() == ParameterType.CoreType.INT) {
            int v = (Integer) value;
            return v == 1;
        } else {
            return false;
        }
    }

    public Color asColor() {
        assertCardinality();
        if (getCoreType() == ParameterType.CoreType.COLOR) {
            return (Color) value;
        } else {
            return new Color();
        }
    }

    public Grob asGrob() {
        assertCardinality();
        if (getCoreType() == ParameterType.CoreType.GROB_PATH
                || getCoreType() == ParameterType.CoreType.GROB_CANVAS
                || getCoreType() == ParameterType.CoreType.GROB_IMAGE) {
            return (Grob) value;
        } else {
            return new Group();
        }
    }

    public String asExpression() {
        assertCardinality();
        if (getCoreType() == ParameterType.CoreType.INT) {
            return String.valueOf((Integer) value);
        } else if (getCoreType() == ParameterType.CoreType.FLOAT) {
            return String.valueOf((Double) value);
        } else if (getCoreType() == ParameterType.CoreType.STRING) {
            String v = (String) value;
            // Quote the string
            v = v.replaceAll("\"", "\\\"");
            return "\"" + v + "\"";
        } else if (getCoreType() == ParameterType.CoreType.COLOR) {
            Color v = (Color) value;
            return String.format("Color(%.2f, %.2f, %.2f, %.2f)", v.getRed(), v.getGreen(), v.getBlue(), v.getAlpha());
        } else {
            throw new AssertionError("Cannot convert parameter value " + asString() + " of type " + getCoreType() + " to expression.");
        }
    }

    public Object getValue() {
        assertCardinality();
        return value;
    }

    public List<Object> getValues() {
        if (getCardinality() == ParameterType.Cardinality.SINGLE)
            throw new AssertionError("getValues() is not available for parameter types with single cardinality.");
        assert (value instanceof List);
        return (List<Object>) value;
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
        if (value instanceof Integer && getCoreType() == ParameterType.CoreType.FLOAT) {
            this.value = (double) ((Integer) value);
        } else {
            this.value = value;
        }
        fireValueChanged();
    }

    private void assertCardinality() {
        if (getCardinality() == ParameterType.Cardinality.MULTIPLE)
            throw new AssertionError("You cannot retrieve multi-parameters this way. Use getValues().");

    }

    //// Expressions ////

    public boolean hasExpression() {
        return expression != null;
    }

    public String getExpression() {
        return hasExpression() ? expression.getExpression() : "";
    }

    public void setExpression(String expression) throws ConnectionError {
        if (hasExpression() && getExpression().equals(expression)) {
            return;
        }
        if (expression == null || expression.trim().length() == 0) {
            this.expression = null;
            setExpressionEnabled(false);
        } else {
            this.expression = new Expression(this, expression);
            // Setting an expession automatically enables it and marks the node as dirty.
            setExpressionEnabled(true);
            try {
                updateDependencies();
            } catch (ConnectionError e) {
                // Whilst updating, we might catch a Connection error meaning you are connecting
                // e.g. the parameter to itself. If that happens, we clear out the expression and all of its
                // dependencies.
                clearDependencies();
                this.expression = null;
                setExpressionEnabled(false);
                throw (e);
            }
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
     *
     * @throws ConnectionError when there is a connection error when creating the dependencies.
     */
    private void updateDependencies() throws ConnectionError {
        if (getNetwork() == null)
            throw new AssertionError("The node needs to be in a network to use expressions.");
        clearDependencies();
        for (Parameter p : expression.getDependencies()) {
            // Each expression depency functions as an output to which this parameter connects.
            // This connection is implicit because it was not explicitly created by the user,
            // but through the expression.
            getNetwork().connect(p, this, Connection.Type.IMPLICIT);
        }
    }

    private void clearDependencies() {
        if (getNetwork() == null) return;
        List<Connection> connections = getNetwork().getUpstreamConnections(this);
        for (Connection conn : connections) {
            if (conn.getType() == Connection.Type.IMPLICIT) {
                getNetwork().disconnect(conn.getOutputParameter(), this, Connection.Type.IMPLICIT);
            }
        }
    }

    public List<Parameter> getDependents() {
        if (getNetwork() == null) return new ArrayList<Parameter>();
        // My dependents are represented as implicit connections for which I am the output.
        // The list of dependents is a list of input parameters on these connections.
        List<Parameter> dependents = new ArrayList<Parameter>();
        // Filter out explicit connections
        for (Connection conn : getNetwork().getDownstreamConnections(this)) {
            if (conn.getType() == Connection.Type.IMPLICIT)
                dependents.add(conn.getInputParameter());
        }
        return dependents;
    }

    public List<Parameter> getDependencies() {
        if (getNetwork() == null) return new ArrayList<Parameter>();
        // My depencies are represented as implicit connections for which I am the input.
        // The list of depencies is a list of output parameters on these connections.
        List<Parameter> dependencies = new ArrayList<Parameter>();
        // Filter out explicit connections
        for (Connection conn : getNetwork().getUpstreamConnections(this)) {
            if (conn.getType() == Connection.Type.IMPLICIT)
                dependencies.add(conn.getOutputParameter());
        }
        return dependencies;
    }

    /**
     * Called whenever the value of this parameter changes. This method informs the dependent parameters that my value
     * has changed.
     */
    protected void fireValueChanged() {
        getNode().markDirty();
        for (Parameter p : getDependents()) {
            p.getNode().markDirty();
        }
        for (ParameterDataListener l : listeners) {
            l.valueChanged(this, value);
        }
    }

    //// Connections ////

    public boolean isInputParameter() {
        return true;
    }

    public boolean isOutputParameter() {
        return !isInputParameter();
    }

    public Connection getExplicitConnection() {
        if (getNetwork() == null) return null;
        return getNetwork().getExplicitConnection(this);
    }

    public List<Connection> getConnections() {
        if (getNetwork() == null) return new ArrayList<Connection>();
        return getNetwork().getUpstreamConnections(this);
    }

    public boolean isCompatible(Node outputNode) {
        return outputNode.getOutputParameter().getType().equals(getType());
    }

    private boolean hasExplicitConnection() {
        return getExplicitConnection() != null;
    }

    public boolean isConnected() {
        if (getNetwork() == null) return false;
        return getNetwork().isConnected(this);
    }

    public boolean isConnectedTo(Parameter parameter) {
        if (!isConnected()) return false;
        // Since output and input parameters can be intermingled, check both sides of the connection.
        return getNetwork().isConnectedTo(parameter, this) || getNetwork().isConnectedTo(this, parameter);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isConnectedTo(Node node) {
        if (!isConnected()) return false;
        return getNetwork().isConnectedTo(node.getOutputParameter(), this);
    }

    public boolean canConnectTo(Parameter parameter) {
        // Parameters can only be connected to output parameters.
        // TODO: No longer true for implicit connections
        //if (!(parameter instanceof OutputParameter)) return false;
        return getParameterType().canConnectTo(parameter.getParameterType());
    }

    public boolean canConnectTo(Node outputNode) {
        if (!node.inNetwork()) return false;
        if (!outputNode.inNetwork()) return false;
        if (node.getNetwork() != outputNode.getNetwork()) return false;
        return canConnectTo(outputNode.getOutputParameter());
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
        return getNetwork().connect(outputNode.getOutputParameter(), this);
    }

    /**
     * Disconnects this (input) parameter from its output node.
     * <p/>
     * If no connection was present, this method does nothing.
     *
     * @return true if the connection was removed
     */
    public boolean disconnect() {
        return getNetwork().disconnect(this);
    }

    /**
     * Updates the parameter, making sure all dependencies are clean.
     * <p/>
     * This method can take a long time and should be run in a separate thread.
     *
     * @param ctx the processing context
     */
    public void update(ProcessingContext ctx) {
        // Update all connections.
        for (Connection conn : getConnections()) {
            conn.update(ctx);
        }

        if (hasExplicitConnection()) {
            Connection conn = getExplicitConnection();
            Object outputValue = conn.getOutputValue();
            validate(outputValue);
            value = outputValue;
        } else if (hasExpression()) {
            Object expressionValue = expression.evaluate();
            validate(expressionValue);
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
                xml.append(spaces).append("<int>").append(asInt()).append("</int>\n");
            } else if (getCoreType() == ParameterType.CoreType.FLOAT) {
                xml.append(spaces).append("<float>").append(asFloat()).append("</float>\n");
            } else if (getCoreType() == ParameterType.CoreType.STRING) {
                xml.append(spaces).append("<string>").append(asString()).append("</string>\n");
            } else if (getCoreType() == ParameterType.CoreType.COLOR) {
                xml.append(spaces).append("<color>").append(asColor().toString()).append("</color>\n");
            } else {
                throw new AssertionError("Unknown value class " + getCoreType());
            }
        }
    }

    @Override
    public String toString() {
        return "<Parameter " + getNode().getName() + "." + getName() + " (" + getType().toString().toLowerCase() + ")>";
    }

    /**
     * Copy this field and all its upstream connections, recursively.
     * Used with deferreds.
     *
     * @param newNode the new node that will act as the parent to this parameter.
     * @return the new copy of this parameter.
     */
    public Parameter copyWithUpstream(Node newNode) {
        Constructor parameterConstructor;
        try {
            parameterConstructor = getClass().getConstructor(ParameterType.class, Node.class);
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE, "Class " + getClass() + " has no appropriate constructor.", e);
            return null;
        }
        Parameter newParameter;
        try {
            newParameter = (Parameter) parameterConstructor.newInstance(parameterType, newNode);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Class " + getClass() + " cannot be instantiated.", e);
            return null;
        }

        Connection conn = getNetwork().getExplicitConnection(this);
        if (conn != null) {
            Node newOutputNode = conn.getOutputNode().copyWithUpstream(newNode.getNetwork());
            newParameter.connect(newOutputNode);
        } else if (hasExpression()) {
            newParameter.setExpression(getExpression());
        } else {
            // TODO: Clone the value properly.
            newParameter.value = value;
        }

        return newParameter;
    }
}
