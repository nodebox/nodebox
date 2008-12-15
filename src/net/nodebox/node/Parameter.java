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

import net.nodebox.graphics.*;
import net.nodebox.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parameter controls the operation of a Node. It provide an interface into the workings of a node and allows a user
 * to change its behaviour. Parameters are represented by standard user interface controls, such as sliders for numbers,
 * text fields for strings, and checkboxes for booleans.
 * <p/>
 * Parameters implement the observer pattern for expressions. Parameters that are dependent on other parameters because
 * of their expressions will observe the parameters they depend on, and marked the node as dirty whenever they receive
 * an update event from one of the parameters they depend on.
 */
public class Parameter extends Observable implements Observer {
    public enum Type {
        ANGLE, COLOR, FILE, FLOAT, FONT, GRADIENT, IMAGE, INT, MENU, SEED, STRING, TEXT, TOGGLE, NODEREF,
        GROB_CANVAS, GROB_VECTOR, GROB_IMAGE
    }

    public enum CoreType {
        INT, FLOAT, STRING, COLOR, GROB_CANVAS, GROB_SHAPE, GROB_IMAGE
    }

    public enum BoundingMethod {
        NONE, SOFT, HARD
    }

    public enum DisplayLevel {
        HIDDEN, DETAIL, HUD
    }

    public static final HashMap<CoreType, Class> CORE_TYPE_MAPPING;
    public static final HashMap<CoreType, Object> CORE_TYPE_DEFAULTS;
    public static final HashMap<Type, CoreType> TYPE_REGISTRY;

    public class MenuEntry {
        private String key;
        private String label;

        public MenuEntry(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }
    }

    static {
        CORE_TYPE_MAPPING = new HashMap<CoreType, Class>();
        CORE_TYPE_MAPPING.put(CoreType.INT, Integer.class);
        CORE_TYPE_MAPPING.put(CoreType.FLOAT, Double.class);
        CORE_TYPE_MAPPING.put(CoreType.STRING, String.class);
        CORE_TYPE_MAPPING.put(CoreType.COLOR, Color.class);
        CORE_TYPE_MAPPING.put(CoreType.GROB_CANVAS, Canvas.class);
        CORE_TYPE_MAPPING.put(CoreType.GROB_SHAPE, Group.class);
        CORE_TYPE_MAPPING.put(CoreType.GROB_IMAGE, Image.class);

        CORE_TYPE_DEFAULTS = new HashMap<CoreType, Object>();
        CORE_TYPE_DEFAULTS.put(CoreType.INT, 0);
        CORE_TYPE_DEFAULTS.put(CoreType.FLOAT, 0.0);
        CORE_TYPE_DEFAULTS.put(CoreType.STRING, "");
        CORE_TYPE_DEFAULTS.put(CoreType.COLOR, new Color());

        TYPE_REGISTRY = new HashMap<Type, CoreType>();
        TYPE_REGISTRY.put(Type.ANGLE, CoreType.FLOAT);
        TYPE_REGISTRY.put(Type.COLOR, CoreType.COLOR);
        TYPE_REGISTRY.put(Type.FILE, CoreType.STRING);
        TYPE_REGISTRY.put(Type.FLOAT, CoreType.FLOAT);
        TYPE_REGISTRY.put(Type.FONT, CoreType.STRING);
        TYPE_REGISTRY.put(Type.GRADIENT, CoreType.STRING);
        TYPE_REGISTRY.put(Type.IMAGE, CoreType.STRING);
        TYPE_REGISTRY.put(Type.INT, CoreType.INT);
        TYPE_REGISTRY.put(Type.MENU, CoreType.STRING);
        TYPE_REGISTRY.put(Type.SEED, CoreType.INT);
        TYPE_REGISTRY.put(Type.STRING, CoreType.STRING);
        TYPE_REGISTRY.put(Type.TEXT, CoreType.STRING);
        TYPE_REGISTRY.put(Type.TOGGLE, CoreType.INT);
        TYPE_REGISTRY.put(Type.NODEREF, CoreType.STRING);
        TYPE_REGISTRY.put(Type.GROB_CANVAS, CoreType.GROB_CANVAS);
        TYPE_REGISTRY.put(Type.GROB_VECTOR, CoreType.GROB_SHAPE);
        TYPE_REGISTRY.put(Type.GROB_IMAGE, CoreType.GROB_IMAGE);
    }

    private Node node;
    private String name;
    private String label;
    private String helpText;
    private Type type;
    private CoreType coreType;
    private BoundingMethod boundingMethod;
    private Double minimumValue;
    private Double maximumValue;
    private DisplayLevel displayLevel;
    private ArrayList<MenuEntry> menuItems;
    private boolean disabled;
    private boolean persistent;
    private Object value;
    private Object defaultValue;
    private boolean valueSet = false;
    private Expression expression;
    private boolean expressionEnabled;
    private Connection connection;
    private List<Connection> expressionConnections = new ArrayList<Connection>();

    public static class NotFound extends RuntimeException {

        private Node node;
        private String parameterName;

        public NotFound(Node node, String parameterName) {
            this.node = node;
            this.parameterName = parameterName;
        }

        public Node getNode() {
            return node;
        }

        public String getParameterName() {
            return parameterName;
        }
    }

    public static class InvalidName extends RuntimeException {
        private Parameter parameter;
        private String name;

        public InvalidName(Parameter parameter, String name) {
            this(parameter, name, "Invalid name \"" + name + "\" for parameter \"" + parameter.getName() + "\"");
        }

        public InvalidName(Parameter parameter, String name, String message) {
            super(message);
            this.parameter = parameter;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Parameter getParameter() {
            return parameter;
        }
    }

    public Parameter(Node node, String name, Type type) {
        this.node = node;
        this.name = name;
        this.label = StringUtils.humanizeName(name);
        setType(type); // this sets the core type, default values, and value.
    }

    //// Basic operations ////

    public Node getNode() {
        return node;
    }

    //// Naming ////

    public boolean validName(String name) {
        // Check if another parameter has the same name.
        if (node.hasParameter(name) && node.getParameter(name) != this) return false;
        Pattern nodeNamePattern = Pattern.compile("^[a-z_][a-z0-9_]{0,29}$");
        Pattern doubleUnderScorePattern = Pattern.compile("^__.*$");
        Pattern reservedPattern = Pattern.compile("^(node|name)$");
        Matcher m1 = nodeNamePattern.matcher(name);
        Matcher m2 = doubleUnderScorePattern.matcher(name);
        Matcher m3 = reservedPattern.matcher(name);
        return m1.matches() && !m2.matches() && !m3.matches();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (validName(name)) {
            node.renameParameter(this, name);
        } else {
            throw new InvalidName(this, name);
        }
    }

    public void _setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        node.fireNodeChanged();
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
        node.fireNodeChanged();
    }

    //// Type ////

    Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (this.type == type) return;
        this.type = type;
        assert (TYPE_REGISTRY.containsKey(type));
        this.coreType = TYPE_REGISTRY.get(type);
        this.defaultValue = CORE_TYPE_DEFAULTS.get(this.coreType);
        // TODO: Change the value to something reasonable.
        this.value = this.defaultValue;
        node.fireNodeChanged();
        markDirty();
    }

    public CoreType getCoreType() {
        return coreType;
    }

    //// Boundaries ////

    public Object getBoundingMethod() {
        return boundingMethod;
    }

    public void setBoundingMethod(BoundingMethod boundingMethod) {
        this.boundingMethod = boundingMethod;
        if (boundingMethod == BoundingMethod.HARD) {
            clampToBounds();
        }
        node.fireNodeChanged();
    }

    public void setMinimumValue(Double minimumValue) {
        if (minimumValue != null && maximumValue != null && minimumValue > maximumValue) {
            return;
        }
        this.minimumValue = minimumValue;
        if (boundingMethod == BoundingMethod.HARD) {
            clampToBounds();
        }
        node.fireNodeChanged();
    }

    public void setMaximumValue(Double maximumValue) {
        if (minimumValue != null && maximumValue != null && maximumValue < minimumValue) {
            return;
        }
        this.maximumValue = maximumValue;
        if (boundingMethod == BoundingMethod.HARD) {
            clampToBounds();
        }
        node.fireNodeChanged();
    }

    public boolean valueCorrectForBounds(double value) {
        if (boundingMethod != BoundingMethod.HARD) {
            return true;
        }
        return value >= minimumValue && value <= maximumValue;
    }

    private void clampToBounds() {
        if (coreType == CoreType.INT) {
            int v = (Integer) value;
            if (minimumValue != null && v < minimumValue) {
                set(minimumValue.intValue());
            } else if (maximumValue != null && v > maximumValue) {
                set(maximumValue.intValue());
            }
        } else if (coreType == CoreType.FLOAT) {
            double v = (Double) value;
            if (minimumValue != null && v < minimumValue) {
                set(minimumValue);
            } else if (maximumValue != null && v > maximumValue) {
                set(maximumValue);
            }
        }
    }

    //// Display level ////

    public DisplayLevel getDisplayLevel() {
        return displayLevel;
    }

    public void setDisplayLevel(DisplayLevel displayLevel) {
        this.displayLevel = displayLevel;
        node.fireNodeChanged();
    }

    //// Menu items ////

    public List<MenuEntry> getMenuItems() {
        return menuItems;
    }

    public void addMenuItem(String key, String label) {
        menuItems.add(new MenuEntry(key, label));
        node.fireNodeChanged();
    }

    //// Flags ////

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        node.fireNodeChanged();
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
        node.fireNodeChanged();
    }

    //// Default value ////

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        validate(defaultValue);
        this.defaultValue = defaultValue;
        if (!valueSet) {
            setValue(this.defaultValue);
        }
    }

    //// Values ////

    public int asInt() {
        if (coreType == CoreType.INT) {
            return (Integer) value;
        } else if (coreType == CoreType.FLOAT) {
            double v = (Double) value;
            return (int) v;
        } else {
            return 0;
        }
    }

    public double asFloat() {
        if (coreType == CoreType.FLOAT) {
            return (Double) value;
        } else if (coreType == CoreType.INT) {
            int v = (Integer) value;
            return (double) v;
        } else {
            return 0;
        }
    }

    public String asString() {
        if (coreType == CoreType.STRING) {
            return (String) value;
        } else {
            return value.toString();
        }
    }

    public Color asColor() {
        if (coreType == CoreType.COLOR) {
            return (Color) value;
        } else {
            return new Color();
        }
    }

    public Grob asGrob() {
        if (coreType == CoreType.GROB_SHAPE || coreType == CoreType.GROB_CANVAS || coreType == CoreType.GROB_IMAGE) {
            return (Grob) value;
        } else {
            return new Group();
        }
    }

    public Object getValue() {
        return value;
    }

    public void set(int value) {
        if (coreType != CoreType.INT) {
            throw new ValueError("Tried setting integer value on parameter with type " + type);
        }
        if (!valueCorrectForBounds(value)) {
            throw new ValueError("Value is out of bounds");
        }
        if (asInt() == value) return;
        this.value = value;
        valueSet = true;
        markDirty();
    }

    public void set(double value) {
        if (coreType != CoreType.FLOAT) {
            throw new ValueError("Tried setting float value on parameter with type " + type);
        }
        if (!valueCorrectForBounds(value)) {
            throw new ValueError("Value is out of bounds");
        }
        if (asFloat() == value) return;
        this.value = value;
        valueSet = true;
        markDirty();
    }

    public void set(String value) {
        if (value == null) return;
        if (coreType != CoreType.STRING) {
            throw new ValueError("Tried setting string value on parameter with type " + type);
        }
        if (asString().equals(value)) return;
        this.value = value;
        valueSet = true;
        markDirty();
    }

    public void set(Color value) {
        if (value == null) return;
        if (coreType != CoreType.COLOR) {
            throw new ValueError("Tried setting color value on parameter with type " + type);
        }
        if (asColor().equals(value)) return;
        this.value = value;
        valueSet = true;
        markDirty();
    }

    public void set(Grob value) {
        if (value == null) return;
        if (coreType != CoreType.GROB_SHAPE && coreType != CoreType.GROB_CANVAS && coreType != CoreType.GROB_IMAGE) {
            throw new ValueError("Tried setting grob value on parameter with type " + type);
        }
        this.value = value;
        valueSet = true;
        markDirty();
    }

    public void setValue(Object value) throws ValueError {
        if (value == null) return;
        if (coreType == CoreType.INT) {
            if (value instanceof Integer) {
                set((Integer) value);
            } else {
                throw new ValueError("Value needs to be an int.");
            }
        } else if (coreType == CoreType.FLOAT) {
            if (value instanceof Float) {
                set((Float) value);
            } else if (value instanceof Double) {
                set((Double) value);
            } else {
                throw new ValueError("Value needs to be a float.");
            }
        } else if (coreType == CoreType.STRING) {
            if (value instanceof String) {
                set((String) value);
            } else {
                throw new ValueError("Value needs to be a string.");
            }
        } else if (coreType == CoreType.COLOR) {
            if (value instanceof Color) {
                set((Color) value);
            } else {
                throw new ValueError("Value needs to be a color.");
            }
        } else if (coreType == CoreType.GROB_SHAPE) {
            if (value instanceof Grob) {
                set((Grob) value);
            } else {
                throw new ValueError("Value needs to be a Grob.");
            }
        } else if (coreType == CoreType.GROB_CANVAS) {
            if (value instanceof Canvas) {
                set((Canvas) value);
            } else {
                throw new ValueError("Value needs to be a Canvas.");
            }
        } else if (coreType == CoreType.GROB_IMAGE) {
            if (value instanceof Image) {
                set((Image) value);
            } else {
                throw new ValueError("Value needs to be an Image.");
            }
        } else {
            throw new AssertionError("Unknown core type " + coreType);
        }
    }

    //// Validation ////

    /**
     * Checks if the given value would fit this parameter.
     * <p/>
     * Raises a ValueError if the value does not match.
     *
     * @param value the value to validate.
     */
    public void validate(Object value) {
        if (value == null) {
            throw new ValueError("Value cannot be null.");
        }
        // Check if the type matches
        Class requiredType = CORE_TYPE_MAPPING.get(coreType);
        if (!value.getClass().isAssignableFrom(requiredType)) {
            throw new ValueError("Value is not of the required type (" + requiredType.getSimpleName() + ")");
        }
        // If hard bounds are set, check if the value falls within the bounds.

        if (getBoundingMethod() == BoundingMethod.HARD) {
            double doubleValue = (Double) value;
            // TODO: Check if bounding is implemented correctly.
//            if (value instanceof Integer) {
//                doubleValue = (Double) value;
//            } else if (value instanceof Double) {
//                doubleValue = (Double) value;
//            }
            if (minimumValue != null && doubleValue < minimumValue) {
                throw new ValueError("Parameter " + getName() + ": value " + value + " is too small. (minimum=" + minimumValue + ")");
            }
            if (maximumValue != null && doubleValue > maximumValue) {
                throw new ValueError("Parameter " + getName() + ": value " + value + " is too big. (maximum=" + maximumValue + ")");
            }
        }
    }

    //// Parsing ////

    public Object parseValue(String value) {
        switch (coreType) {
            case INT:
                return Integer.parseInt(value);
            case FLOAT:
                return Float.parseFloat(value);
            case STRING:
                return value;
            case COLOR:
                return new Color(value);
            default:
                return value;
        }
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
            // Setting an expession automatically enables it and marks the node as dirty.
            setExpressionEnabled(true);
        }
        markDirty();
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
        markDirty();
    }

    //// Expression dependencies ////

    private void addExpressionDependency(Parameter parameter) {
        addObserver(parameter);
    }

    public boolean hasExpressionDependencies() {
        return countObservers() > 0;
    }

    //// Connections ////

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
     * Called whenever a Parameter I depend on for my expression changes.
     * This means I will need to refresh the value of my Parameter, so this method
     * marks the node as dirty.
     *
     * @param o   an Observable object. We ignore this.
     * @param arg ignored
     */
    public void update(Observable o, Object arg) {
        markDirty();
    }

    /**
     * Updates the parameter, making sure all dependencies are clean.
     * <p/>
     * This method can take a long time and should be run in a separate thread.
     */
    public void update(ProcessingContext ctx) {
        if (isConnected()) {
            connection.getOutputNode().update(ctx);
            Object outputValue = connection.getOutputNode().getOutputValue();
            // TODO: validate
            value = outputValue;
        }
        if (hasExpression()) {
            for (Connection c : expressionConnections) {
                c.getOutputNode().update(ctx);
            }
            Object expressionValue = expression.evaluate();
            validate(expressionValue);
            value = expressionValue;
        }
    }

    //// Values ////

    public void revertToDefault() {
        this.value = this.defaultValue;
        markDirty();
    }

    public void markDirty() {
        node.markDirty();
        setChanged();
        notifyObservers();
        clearChanged();
    }
}
