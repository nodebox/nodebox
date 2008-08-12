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

import net.nodebox.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parameter {
    // TODO: MenuItem support
    public static final String TYPE_ANGLE = "angle";
    public static final String TYPE_COLOR = "color";
    public static final String TYPE_CUSTOM = "custom";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_GRADIENT = "gradient";
    public static final String TYPE_GROUP = "group";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_INT = "int";
    public static final String TYPE_MENU = "menu";
    public static final String TYPE_POINT = "point";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_TOGGLE = "toggle";
    public static final String TYPE_NODE = "node";
    public static final String TYPE_NETWORK = "network";
    public static final int CORE_TYPE_FLOAT = 1;
    public static final int CORE_TYPE_INT = 2;
    public static final int CORE_TYPE_STRING = 3;
    public static final int CORE_TYPE_DATA = 4;
    public static final int DIRECTION_IN = 1;
    public static final int DIRECTION_OUT = 2;
    public static final int BOUNDING_NONE = 0;
    public static final int BOUNDING_SOFT = 1;
    public static final int BOUNDING_HARD = 2;
    public static final int DISPLAY_LEVEL_HIDDEN = 0;
    public static final int DISPLAY_LEVEL_DETAIL = 1;
    public static final int DISPLAY_LEVEL_HUD = 2;
    private Node node;
    private String name;
    private String label;
    private String helpText;
    private String type;
    private int coreType;
    private int direction;
    private int channelCount;
    private int boundingType;
    private double minimum;
    private double maximum;
    private int displayLevel;
    //private ArrayList<MenuItem> menuItems;
    private boolean disabled;
    private boolean persistent;
    private Object[] channels;
    private Expression expression;
    private boolean expressionEnabled;
    private Connection connection;

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

    public Parameter(Node node, String name, String type) {
        this(node, name, type, DIRECTION_IN);
    }

    public Parameter(Node node, String name, String type, int direction) {
        this.node = node;
        this.name = name;
        this.label = StringUtils.humanizeName(name);
        this.type = type;
        this.direction = direction;

        if (type.equals(TYPE_ANGLE)) {
            coreType = CORE_TYPE_FLOAT;
            channelCount = 1;
        } else if (type.equals(TYPE_COLOR)) {
            coreType = CORE_TYPE_FLOAT;
            channelCount = 4;
        } else if (type.equals(TYPE_CUSTOM)) {
            coreType = CORE_TYPE_DATA;
            channelCount = 1;
        } else if (type.equals(TYPE_FILE)) {
            coreType = CORE_TYPE_STRING;
            channelCount = 1;
        } else if (type.equals(TYPE_FLOAT)) {
            coreType = CORE_TYPE_FLOAT;
            channelCount = 1;
        } else if (type.equals(TYPE_GRADIENT)) {
            coreType = CORE_TYPE_STRING;
            channelCount = 1;
        } else if (type.equals(TYPE_GROUP)) {
            coreType = CORE_TYPE_INT;
            channelCount = 1;
        } else if (type.equals(TYPE_IMAGE)) {
            coreType = CORE_TYPE_STRING;
            channelCount = 1;
        } else if (type.equals(TYPE_INT)) {
            coreType = CORE_TYPE_INT;
            channelCount = 1;
        } else if (type.equals(TYPE_MENU)) {
            coreType = CORE_TYPE_INT;
            channelCount = 1;
        } else if (type.equals(TYPE_POINT)) {
            coreType = CORE_TYPE_FLOAT;
            channelCount = 2;
        } else if (type.equals(TYPE_STRING)) {
            coreType = CORE_TYPE_STRING;
            channelCount = 1;
        } else if (type.equals(TYPE_TOGGLE)) {
            coreType = CORE_TYPE_INT;
            channelCount = 1;
        } else {
            coreType = CORE_TYPE_DATA;
            channelCount = 1;
        }
        assert (channelCount >= 1);
        channels = new Object[channelCount];
        revertToDefault();
    }

    //// Basic operations ////
    public Node getNode() {
        return node;
    }

    public int getDirection() {
        return direction;
    }

    public boolean isInputParameter() {
        return direction == DIRECTION_IN;
    }

    public boolean isOutputParameter() {
        return direction == DIRECTION_OUT;
    }

    public int channelCount() {
        return channelCount;
    }

    //// Naming ////
    public String getName() {
        return name;
    }

    public void setName(String name) {
        node.renameParameter(this, name);
        // TODO: notify
    }

    public void _setName(String name) {
        if (validName(name)) {
            this.name = name;
        } else {
            throw new InvalidName(this, name);
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        // TODO: notify
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
        // TODO: notify
    }

    public static boolean validName(String name) {
        Pattern nodeNamePattern = Pattern.compile("^[a-z_][a-z0-9_]{0,29}$");
        Pattern doubleUnderScorePattern = Pattern.compile("^__.*$");
        Pattern reservedPattern = Pattern.compile("^(node|name)$");
        Matcher m1 = nodeNamePattern.matcher(name);
        Matcher m2 = doubleUnderScorePattern.matcher(name);
        Matcher m3 = reservedPattern.matcher(name);
        return m1.matches() && !m2.matches() && !m3.matches();
    }

    //// Type ////
    String getType() {
        return type;
    }

    int getCoreType() {
        return coreType;
    }

    //// Boundaries ////
    public void setBoundingType(int boundingType) {
        this.boundingType = boundingType;
        if (boundingType == BOUNDING_HARD) {
            clampToBounds();
        }
    }

    public void setMinimum(double minimum) {
        if (minimum > maximum) {
            return;
        }
        this.minimum = minimum;
        if (boundingType == BOUNDING_HARD) {
            clampToBounds();
        }
    }

    public void setMaximum(double maximum) {
        if (maximum < minimum) {
            return;
        }
        this.maximum = maximum;
        if (boundingType == BOUNDING_HARD) {
            clampToBounds();
        }
    }

    public boolean valueCorrectForBounds(double value) {
        if (boundingType != BOUNDING_HARD) {
            return true;
        }
        if (value >= minimum && value <= maximum) {
            return true;
        }
        return false;
    }

    private void clampToBounds() {
        if (coreType == CORE_TYPE_INT) {
            for (int i = 0; i < channelCount; i++) {
                int v = (Integer) channels[i];
                if (v < minimum) {
                    set((int)minimum, i);
                } else if (v > maximum) {
                    set((int)maximum, i);
                }
            }
        } else if (coreType == CORE_TYPE_FLOAT) {
            for (int i = 0; i < channelCount; i++) {
                double v = (Double) channels[i];
                if (v < minimum) {
                    set(minimum, i);
                } else if (v > maximum) {
                    set(maximum, i);
                }
            }
        }
    }

    //// Display level ////
    public void setDisplayLevel(int displayLevel) {
        this.displayLevel = displayLevel;
        // TODO: notify
    }

    //// Menu items ////
    public void addMenuItem(int key, String label) {
        // TODO: implement
    }

    //// Flags ////
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    //// Values ////
    public int asInt() {
        return asInt(0);
    }

    public int asInt(int channel) {
        if (coreType == CORE_TYPE_INT) {
            return (Integer) channels[channel];
        } else if (coreType == CORE_TYPE_FLOAT) {
            double v = (Double) channels[channel];
            return (int) v;
        } else {
            return 0;
        }
    }

    public double asFloat() {
        return asFloat(0);
    }

    public double asFloat(int channel) {
        if (coreType == CORE_TYPE_FLOAT) {
            return (Double) channels[channel];
        } else if (coreType == CORE_TYPE_INT) {
            int v = (Integer) channels[channel];
            return (double) v;
        } else {
            return 0;
        }
    }

    public String asString() {
        return asString(0);
    }

    public String asString(int channel) {
        if (coreType == CORE_TYPE_STRING) {
            return (String) channels[channel];
        } else {
            return "";
        }
    }

    public Object asData() {
        return asData(0);
    }

    public Object asData(int channel) {
        return channels[channel];
    }

    public void set(int value) {
        set(value, 0);
    }

    public void set(int value, int channel) {
        if (channel < 0 || channel > channelCount) {
            throw new ValueError("Invalid channel " + channel);
        }
        if (coreType != CORE_TYPE_INT) {
            throw new ValueError("Tried setting integer value on parameter with type " + type);
        }
        if (!valueCorrectForBounds(value)) {
            throw new ValueError("Value is out of bounds");
        }
        // TODO: lazy setting
        preSet();
        channels[channel] = value;
        postSet();
    }

    public void set(double value) {
        set(value, 0);
    }

    public void set(double value, int channel) {
        if (channel < 0 || channel >= channelCount) {
            throw new ValueError("Invalid channel " + channel);
        }
        if (coreType != CORE_TYPE_FLOAT) {
            throw new ValueError("Tried setting float value on parameter with type " + type);
        }
        if (!valueCorrectForBounds(value)) {
            throw new ValueError("Value is out of bounds");
        }
        // TODO: lazy setting
        preSet();
        channels[channel] = value;
        postSet();
    }

    public void set(String value) {
        set(value, 0);
    }

    public void set(String value, int channel) {
        if (channel < 0 || channel > channelCount) {
            throw new ValueError("Invalid channel " + channel);
        }
        if (coreType != CORE_TYPE_STRING) {
            throw new ValueError("Tried setting string value on parameter with type " + type);
        }
        // TODO: lazy setting
        preSet();
        channels[channel] = value;
        postSet();
    }

    public void set(Object value) {
        set(value, 0);
    }

    public void set(Object value, int channel) {
        if (channel < 0 || channel > channelCount) {
            throw new ValueError("Invalid channel " + channel);
        }

        if (coreType != CORE_TYPE_DATA) {
            throw new ValueError("Tried setting object value on parameter with type " + type);
        }
        // TODO: lazy setting
        preSet();
        channels[channel] = value;
        postSet();
    }

    //// Expressions ////
    public boolean hasExpression() {
        return expression != null;
    }

    public String getExpression() {
        return hasExpression() ? expression.getExpression() : null;
    }

    public void setExpression(String expression) {
        if (coreType == CORE_TYPE_DATA) {
            throw new ValueError("Cannot set expression on data type");
        }
        if (hasExpression()) {
            if (expression.equals(getExpression())) {
                return;
            }
        }
        // TODO: empty expression should clear out the expression
        this.expression = new Expression(this, expression);

        // Setting an expession automaticaclly enables it and marks the node as dirty.
        setExpressionEnabled(true);
        node.markDirty();
    }

    public void setExpressionEnabled(boolean enabled) {
        if (this.expressionEnabled == enabled) {
            return;
            // Before you can enable the expression, you have to have one.
        }
        if (enabled && !hasExpression()) {
            return;
        }
        // You cannot enable expressions on data core types.
        if (coreType == CORE_TYPE_DATA) {
            return;
        }
        this.expressionEnabled = enabled;
        // Since the value of this parameter will change, we mark the node as dirty.
        node.markDirty();
    }

    //// Connection methods ////
    public boolean canConnectTo(Node node) {
        if (isOutputParameter()) {
            return false;
        }
        return node.getOutputParameter().getType().equals(getType());
    }

    public Connection connect(Node outputNode) {
        if (!isInputParameter()) {
            throw new ConnectionError(outputNode, this, "Can only connect input nodes");
        }
        if (this.node == outputNode) {
            throw new ConnectionError(outputNode, this, "Cannot connect to myself");
        }
        if (!canConnectTo(outputNode)) {

            throw new ConnectionError(outputNode, this, "Cannot connect to " + outputNode.getName());
        }
        disconnect();
        connection = new Connection(outputNode.getOutputParameter(), this);
        outputNode._addDownstream(connection);
        getNode().markDirty();
        // TODO: notify
        return connection;
    }

    public boolean disconnect() {
        // TODO: also support disconnecting output parameters.
        assert (isInputParameter());
        if (!isConnected()) {
            return false;
        }
        if (connection.hasOutput()) {
            assert (connection.getOutputNode().isOutputConnectedTo(this));
            connection.getOutputNode()._removeDownstream(connection);
        }
        connection = null;
        revertToDefault();
        node.markDirty();
        // TODO: notify
        return true;
    }

    public boolean isConnected() {
        return connection != null;
    }

    public boolean isConnectedTo(Node node) {
        if (!isConnected()) {
            return false;
        } else {
            return connection.getOutputNode() == node;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void update() {
        // The connection takes precedence over the expression.
        if (isConnected()) {
            connection.getOutputNode().update();
            assert (connection.getOutputParameter().getType().equals(getType()));
            for (int i = 0; i < channelCount; i++) {
                // TODO: There is no type checking here. Should there be?
                channels[i] = connection.getOutputParameter().asData(i);
            }
        } else if (expressionEnabled) {
            // TODO: Currently, the expression runs for each channel.
            // The system could be smarter: when the expression returns a list,
            // assume that this list represents the different channels, and set
            // the different channels to the corresponding list items.
            for (int i = 0; i < channelCount; i++) {
                channels[i] = expression.asData(i);
            }
        }
    }

    //// Values ////
    public void revertToDefault() {
        for (int i = 0; i < channelCount; i++) {
            if (coreType == CORE_TYPE_INT) {
                channels[i] = new Integer(0);
            } else if (coreType == CORE_TYPE_FLOAT) {
                channels[i] = new Double(0);
            } else if (coreType == CORE_TYPE_STRING) {
                channels[i] = new String();
            } else {
                channels[i] = null;
            }
        }
    }

    protected void preSet() {
        // TODO: validate
    }

    protected void postSet() {
        node.markDirty();
    }
}
