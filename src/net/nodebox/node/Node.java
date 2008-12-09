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
import net.nodebox.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Node is a building block in a network and encapsulates specific functionality.
 * <p/>
 * The operation of the Node is specified through its parameters. The data that flows
 * through the node passes through ports.
 */
public abstract class Node {

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
    private Parameter outputParameter;

    /**
     * The input connections for this node.
     */
    private HashMap<Parameter, Connection> upstreams = new HashMap<Parameter, Connection>();

    /**
     * The output connections for this node.
     */
    private List<Connection> downstreams = new ArrayList<Connection>();

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
        outputParameter = new Parameter(this, "output", outputType);
    }

    //// Basic attributes ////

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        setChanged();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        setChanged();
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
        if (inNetwork()) {
            network.rename(this, name);
        } else {
            validateName(name);
            this.name = name;
            setChanged();
        }
    }

    protected void _setName(String name) {
        this.name = name;
        setChanged();
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
        return p;
    }

    public void renameParameter(Parameter parameter, String name) {
        String oldName = parameter.getName();
        if (oldName.equals(name)) return;
        if (hasParameter(oldName)) {
            parameters.remove(oldName);
            parameter._setName(name);
            parameters.put(name, parameter);
            setChanged();
        } else {
            throw new Parameter.NotFound(this, oldName);
        }
    }

    public Parameter getOutputParameter() {
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

    public boolean connectTo(Node inputNode, String parameterName) {
        Parameter p = inputNode.getParameter(parameterName);
        Connection c = new Connection(this, inputNode, p);
        downstreams.add(c);
        inputNode.upstreams.put(p, c);
        return true;
    }


    /**
     * Returns if there is a connection on the specified parameter.
     *
     * @param parameterName the name of the parameter
     * @return true if the parameter is connected
     */
    public boolean isInputConnected(String parameterName) {
        Parameter p = getParameter(parameterName);
        return isInputConnected(p);
    }

    /**
     * Returns if there is a connection on the specified port.
     *
     * @param p the parameter
     * @return true if the port is connected
     */
    public boolean isInputConnected(Parameter p) {
        if (upstreams.isEmpty()) return false;
        return getInputConnection(p) != null;
    }

    /**
     * Return the Connection object for the specified port.
     * If the port is non-existant or nothing is connected to the port,
     * this method returns null.
     *
     * @param parameterName the name of the parameter
     * @return a Connection object or null.
     * @see #isInputConnected(String)
     */
    public Connection getInputConnection(String parameterName) {
        return getInputConnection(getParameter(parameterName));
    }

    /**
     * Return the Connection object for the specified port.
     * If the port is non-existant or nothing is connected to the port,
     * this method returns null.
     *
     * @param parameter the name of the parameter
     * @return a Connection object or null.
     * @see #isInputConnected(Parameter)
     */
    public Connection getInputConnection(Parameter parameter) {
        return upstreams.get(parameter);
    }

    public List<Connection> getInputConnections() {
        return new ArrayList<Connection>(upstreams.values());
    }

    public List<Connection> getOutputConnections() {
        return new ArrayList<Connection>(downstreams);
    }

    /**
     * Disconnects a specific input from the node.
     * Also removes the downstream connection in the corresponding output node.
     *
     * @param parameterName the name of the parameter you want to disconnect
     * @return true if the port was disconnected, false if nothing was connected to the port
     *         or if the port does not exist.
     */
    public boolean disconnectInput(String parameterName) {
        return disconnectInput(getParameter(parameterName));
    }

    /**
     * Disconnects a specific input from the node.
     * Also removes the downstream connection in the corresponding output node.
     *
     * @param parameter the parameter you want to disconnect
     * @return true if the port was disconnected, false if nothing was connected to the port
     *         or if the port does not exist.
     */
    public boolean disconnectInput(Parameter parameter) {
        Connection c = getInputConnection(parameter);
        if (c == null) return false;
        upstreams.remove(parameter);
        c.getOutputNode().downstreams.remove(c);
        setChanged();
        return true;
    }

    /**
     * Removes all connections from and to this node.
     *
     * @return true if connections were removed.
     */
    public boolean disconnect() {
        boolean removedSomething = false;

        // Disconnect all my inputs.
        for (Connection c : upstreams.values()) {
            removedSomething = disconnectInput(c.getInputParameter()) | removedSomething;
        }

        // Disconnect all my outputs.
        for (Connection c : downstreams) {
            removedSomething = c.getInputNode().disconnectInput(c.getInputParameter()) | removedSomething;
        }

        return removedSomething;
    }

    public boolean isConnected() {
        return !upstreams.isEmpty() || !downstreams.isEmpty();
    }

    //// Change notification ////

    public void setChanged() {
        // TODO: Implement finer-grained change notification
    }

    public void markDirty() {
        if (dirty)
            return;
        dirty = true;
        for (Connection connection : downstreams) {
            connection.markDirtyDownstream();
        }
        if (inNetwork() && !network.isDirty()) {
            // Only changes to the rendered node should make the network dirty.
            // TODO: Check for corner cases.
            if (network.getRenderedNode() == this) {
                network.markDirty();
            }
        }
        setChanged();
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
            p.update();
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
