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
import net.nodebox.graphics.Point;
import net.nodebox.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Node is a building block in a network and encapsulates specific functionality.
 * <p/>
 * The operation of the Node is specified through its parameters. The data that flows
 * through the node passes through ports.
 */
public abstract class Node {

    public static final String SIGNAL_NODE_CHANGED_NETWORK = "node_changed_network";
    public static final String SIGNAL_NODE_MARKED_DIRTY = "node_marked_dirty";
    public static final String SIGNAL_NODE_MOVED = "node_moved";
    public static final String SIGNAL_NODE_RENAMED = "node_renamed";
    public static final String SIGNAL_NODE_SET_RENDERED = "node_set_rendered";
    public static final String SIGNAL_NODE_UPDATED = "node_updated";
    public static final String SIGNAL_NETWORK_NODE_ADDED = "network_node_added";
    public static final String SIGNAL_NETWORK_NODE_REMOVED = "network_node_removed";
    public static final String SIGNAL_NETWORK_RENDERED_NODE_CHANGED = "network_rendered_node_changed";
    public static final String SIGNAL_PARAMETER_CONNECTED = "parameter_connected";
    public static final String SIGNAL_PARAMETER_DISCONNECTED = "parameter_disconnected";
    public static final String SIGNAL_PARAMETER_EXPRESSION_CHANGED = "parameter_expression_changed";
    public static final String SIGNAL_PARAMETER_VALUE_CHANGED = "parameter_value_changed";

    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]{0,29}$");
    private static final Pattern DOUBLE_UNDERSCORE_PATTERN = Pattern.compile("^__.*$");
    public static final int MAXIMUM_INPUTS = Integer.MAX_VALUE;

    /**
     * The parent network for this node.
     */
    private Network network;

    /**
     * The name of this node.
     */
    private String name;

    /**
     * Position of this node in the interface.
     */
    private double x, y;

    /**
     * A flag that indicates whether this node is in need of processing.
     * The dirty flag is set using markDirty and cleared while processing.
     */
    private transient boolean dirty = true;

    /**
     * A map of all parameters, both connectable and not.
     */
    private LinkedHashMap<String, Parameter> parameters = new LinkedHashMap<String, Parameter>();

    /**
     * The output parameter.
     */
    private OutputParameter outputParameter;


    /**
     * A list of messages that occurred during processing.
     */
    private List<Message> messages = new ArrayList<Message>();


    /**
     * A list of event listeners for this component.
     */
    //private transient EventListenerList listenerList = new EventListenerList();

    public enum MessageLevel {
        DEBUG, INFO, WARNING, ERROR
    }

    class Message {

        private MessageLevel level;
        private String message;

        Message(MessageLevel level, String message) {
            this.level = level;
            this.message = message;
        }

        public MessageLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public boolean isDebug() {
            return level == MessageLevel.DEBUG;
        }

        public boolean isInfo() {
            return level == MessageLevel.INFO;
        }

        public boolean isWarning() {
            return level == MessageLevel.WARNING;
        }

        public boolean isError() {
            return level == MessageLevel.ERROR;
        }
    }

    //// Exceptions ////

    public static class NotFound extends RuntimeException {

        private Network network;
        private String nodeName;

        public NotFound(Network network, String nodeName) {
            this.network = network;
            this.nodeName = nodeName;
        }

        public Network getNetwork() {
            return network;
        }

        public String getNodeName() {
            return nodeName;
        }
    }

    public static class InvalidName extends RuntimeException {
        private Node node;
        private String name;

        public InvalidName(Node node, String name) {
            this(node, name, "Invalid name \"" + name + "\" for node \"" + node.getName() + "\"");
        }

        public InvalidName(Node node, String name, String message) {
            super(message);
            this.node = node;
            this.name = name;
        }

        public Node getNode() {
            return node;
        }

        public String getName() {
            return name;
        }
    }

    //// Constructors ////

    public Node(Parameter.Type outputType) {
        this(outputType, null);
    }

    public Node(Parameter.Type outputType, String name) {
        if (name != null) {
            this.name = name;
        } else {
            this.name = defaultName();
        }
        outputParameter = new OutputParameter(this, outputType);
    }

    //// Basic attributes ////

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        fireNodeChanged();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        fireNodeChanged();
    }

    public Point getPosition() {
        return new Point(x, y);
    }

    public void setPosition(Point p) {
        this.x = p.getX();
        this.y = p.getY();
        fireNodeChanged();
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        fireNodeChanged();
    }

    //// Naming ////

    public String defaultName() {
        return getClass().getSimpleName().toLowerCase();
    }

    public String getName() {
        return name;
    }

    public void validateName(String name) {
        Matcher m1 = NODE_NAME_PATTERN.matcher(name);
        Matcher m2 = DOUBLE_UNDERSCORE_PATTERN.matcher(name);
        if (!m1.matches()) {
            throw new InvalidName(this, name, "Name does contain other characters than a-z0-9 or underscore, or is longer than 29 characters.");
        }
        if (m2.matches()) {
            throw new InvalidName(this, name, "Names starting with double underscore are reserved for internal use.");
        }
    }

    public void setName(String name) throws InvalidName {
        // Since the network does the rename, fireNodeChanged() will be called from the network.
        if (inNetwork()) {
            network.rename(this, name);
        } else {
            validateName(name);
            this.name = name;
        }
    }

    protected void _setName(String name) {
        this.name = name;
    }

    //// Parameters ////

    public Collection<Parameter> getParameters() {
        return parameters.values();
    }

    public int parameterSize() {
        return parameters.size();
    }

    public Parameter getParameter(String name) {
        if (hasParameter(name)) {
            return parameters.get(name);
        } else {
            throw new Parameter.NotFound(this, name);
        }
    }

    public boolean hasParameter(String name) {
        return parameters.containsKey(name);
    }

    public Parameter addParameter(String name, Parameter.Type type) {
        Parameter p = new Parameter(this, name, type);
        parameters.put(name, p);
        fireNodeChanged();
        return p;
    }

    public void renameParameter(Parameter parameter, String name) {
        String oldName = parameter.getName();
        if (oldName.equals(name)) return;
        if (hasParameter(oldName)) {
            parameters.remove(oldName);
            parameter._setName(name);
            parameters.put(name, parameter);
            fireNodeChanged();
        } else {
            throw new Parameter.NotFound(this, oldName);
        }
    }

    public OutputParameter getOutputParameter() {
        return outputParameter;
    }

    //// Network ////

    public Network getNetwork() {
        return network;
    }

    protected void _setNetwork(Network network) {
        this.network = network;
    }

    public void setNetwork(Network network) throws InvalidName {
        if (inNetwork() && this.network != network) {
            network.remove(this);
        }
        if (network != null) {
            // Network.add checks if this node was already added in the network,
            // so we don't need to check it here.
            network.add(this);
        }
    }

    public boolean inNetwork() {
        return network != null;
    }

    public boolean isRendered() {
        return inNetwork() && network.getRenderedNode() == this;
    }

    public void setRendered() {
        if (!inNetwork()) return;
        network.setRenderedNode(this);
    }

    public String getNetworkPath() {
        List<String> parts = new ArrayList<String>();
        parts.add(name);
        Network parent = network;
        while (parent != null) {
            parts.add(0, parent.getName());
            parent = parent.getNetwork();
        }
        return "/" + StringUtils.join(parts, "/");
    }

    public void fireNodeChanged() {
        if (inNetwork())
            getNetwork().fireNodeChanged(this);
    }


    //// Value shortcuts ////

    public int asInt(String name) {
        return getParameter(name).asInt();
    }

    public double asFloat(String name) {
        return getParameter(name).asFloat();
    }

    public String asString(String name) {
        return getParameter(name).asString();
    }

    public Color asColor(String name) {
        return getParameter(name).asColor();
    }

    public Object getValue(String name) {
        return getParameter(name).getValue();
    }

    public void set(String name, int value) {
        getParameter(name).set(value);
    }

    public void set(String name, double value) {
        getParameter(name).set(value);
    }

    public void set(String name, String value) {
        getParameter(name).set(value);
    }

    public void set(String name, Color value) {
        getParameter(name).set(value);
    }

    public void setValue(String name, Object value) {
        getParameter(name).setValue(value);
    }

    //// Output value shortcuts ////

    public Object getOutputValue() {
        return outputParameter.getValue();
    }

    protected void setOutputValue(Object value) throws ValueError {
        outputParameter.setValue(value);
    }

    //// Expression shortcuts ////

    //// Connection shortcuts ////

    /**
     * Removes all connections from and to this node.
     *
     * @return true if connections were removed.
     */
    public boolean disconnect() {
        boolean removedSomething = false;

        // Disconnect all my inputs.
        for (Parameter p : parameters.values()) {
            removedSomething = p.disconnect() | removedSomething;
        }

        // Disconnect all my outputs.
        // Copy the list of downstreams, since you will be removing elements
        // from it while iterating.
        List<Connection> downstreamConnections = new ArrayList<Connection>(getOutputParameter().getDownstreamConnections());
        for (Connection c : downstreamConnections) {
            removedSomething = c.getInputParameter().disconnect() | removedSomething;
        }

        return removedSomething;
    }

    public boolean isConnected() {
        // Check parameters for upstream connections.
        for (Parameter p : parameters.values()) {
            if (p.isConnected())
                return true;
        }

        // Check output parameter for downstream connections.
        return getOutputParameter().isConnected();
    }

    public boolean isOutputConnected() {
        return getOutputParameter().isConnected();
    }

    public boolean isOutputConnectedTo(Node inputNode) {
        for (Connection c : getOutputParameter().getDownstreamConnections()) {
            if (c.getInputNode() == inputNode)
                return true;
        }
        return false;
    }

    public boolean isOutputConnectedTo(Parameter inputParameter) {
        for (Connection c : getOutputParameter().getDownstreamConnections()) {
            if (c.getInputParameter() == inputParameter)
                return true;
        }
        return false;
    }

    //// Change notification ////

    public void markDirty() {
        if (dirty)
            return;
        dirty = true;
        getOutputParameter().markDirtyDownstream();
        if (inNetwork() && !network.isDirty()) {
            // Only changes to the rendered node should make the network dirty.
            // TODO: Check for corner cases.
            if (network.getRenderedNode() == this) {
                network.markDirty();
            }
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    //// Error handling ////

    public void addDebug(String msg) {
        messages.add(new Message(MessageLevel.DEBUG, msg));
    }

    public void addInfo(String msg) {
        messages.add(new Message(MessageLevel.DEBUG, msg));
    }

    public void addWarning(String msg) {
        messages.add(new Message(MessageLevel.DEBUG, msg));
    }

    public void addError(String msg) {
        messages.add(new Message(MessageLevel.DEBUG, msg));
    }

    public boolean hasError() {
        for (Message msg : messages) {
            if (msg.isError())
                return true;
        }
        return false;
    }

    public boolean hasWarning() {
        for (Message msg : messages) {
            if (msg.isWarning())
                return true;
        }
        return false;
    }

    //// Processing ////

    /**
     * Updates the node by processing all required dependencies.
     * <p/>
     * This method will process only dirty nodes.
     * This operation can take a long time, and should be run in a separate thread.
     *
     * @return true if the operation was successful
     */
    public boolean update() {
        return update(new ProcessingContext());
    }

    /**
     * Updates the node by processing all required dependencies.
     * <p/>
     * This method will process only dirty nodes.
     * This operation can take a long time, and should be run in a separate thread.
     *
     * @param ctx meta-information about the processing operation.
     * @return true if the operation was successful
     */
    public boolean update(ProcessingContext ctx) {
        if (!dirty) return true;
        for (Parameter p : parameters.values()) {
            p.update(ctx);
        }
        messages.clear();
        boolean success = process(ctx);
        dirty = false;
        return success;
    }

    /**
     * This method does the actual functionality of the node.
     *
     * @param ctx meta-information about the processing operation.
     * @return true if the evaluation succeeded.
     */
    protected abstract boolean process(ProcessingContext ctx);

    //// Path ////

    public String getAbsolutePath() {
        StringBuffer name = new StringBuffer("/");
        Network parent = getNetwork();
        while (parent != null) {
            name.insert(1, parent.getName() + "/");
            parent = parent.getNetwork();
        }
        name.append(getName());
        return name.toString();
    }

    //// Output ////

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ": " + name + ">";
    }

    //// Persistence ////

}
