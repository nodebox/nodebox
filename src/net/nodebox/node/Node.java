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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node extends Observable {

    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]{0,29}$");
    private static final Pattern DOUBLE_UNDERSCORE_PATTERN = Pattern.compile("^__.*$");

    private Network network;
    private String name;
    private double x, y;
    private boolean dirty = true;
    private int minimumInputs;
    private int maximumInputs;
    private HashMap<String, Parameter> parameterMap = new HashMap<String, Parameter>();
    private List<Parameter> parameters = new ArrayList<Parameter>();
    private List<Connection> upstreams = new ArrayList<Connection>();
    private List<Connection> downstreams = new ArrayList<Connection>();

    public Node() {
        this(null);
    }

    public Node(String name) {
        if (name != null) {
            this.name = name;
        } else {
            this.name = defaultName();
        }
    }

    //// Basic attributes ////

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        setChanged();
        notifyObservers();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        setChanged();
        notifyObservers();
    }

    //// Naming ////

    public String defaultName() {
        return getClass().getSimpleName().toLowerCase();
    }

    public String getName() {
        return name;
    }

    public static void validateName(String name) {
        Matcher m1 = NODE_NAME_PATTERN.matcher(name);
        Matcher m2 = DOUBLE_UNDERSCORE_PATTERN.matcher(name);
        if (!m1.matches()) {
            throw new ValueError("Name does contain other characters than a-z0-9 or underscore, or is longer than 29 characters.");
        }
        if (m2.matches()) {
            throw new ValueError("Names starting with double underscore are reserved for internal use.");
        }
    }

    public void setName(String name) {
        if (inNetwork()) {
            network.rename(this, name);
        } else {
            validateName(name);
            this.name = name;
            setChanged();
            notifyObservers();
        }
    }

    //// Ports ////

    public int getMinimumInputs() {
        return minimumInputs;
    }

    public int getMaximumInputs() {
        return maximumInputs;
    }

    public void setMinimumInputs(int minimumInputs) {
        this.minimumInputs = minimumInputs;
        // TODO: Check the amount of connections, and disconnect if necessary.
    }

    public void setMaximumInputs(int maximumInputs) {
        this.maximumInputs = maximumInputs;
        // TODO: Check the amount of connections, and disconnect if necessary.
    }

    //// Parameters ////

    public List<Parameter> getParameters() {
        return parameters;
    }

    public int parameterSize() {
        return parameters.size();
    }

    public Parameter getParameter(String name) {
        if (hasParameter(name)) {
            return parameterMap.get(name);
        } else {
            throw new Parameter.NotFound(this, name);
        }
    }

    public boolean hasParameter(String name) {
        return parameterMap.containsKey(name);
    }

    public Parameter addParameter(String name, String type) {
        Parameter p = new Parameter(this, name, type);
        parameterMap.put(name, p);
        parameters.add(p);
        return p;
    }

    public Parameter addParameter(String name, String type, ParameterGroup g) {
        Parameter p = new Parameter(this, name, type);
        parameterMap.put(name, p);
        g.addParameter(p);
        return p;
    }

    //// Network ////

    public Network getNetwork() {
        return network;
    }

    public void _setNetwork(Network network) {
        this.network = network;
    }

    public void setNetwork(Network network) {
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

    public int asInt(String name, int channel) {
        return getParameter(name).asInt(channel);
    }

    public double asFloat(String name) {
        return getParameter(name).asFloat();
    }

    public double asFloat(String name, int channel) {
        return getParameter(name).asFloat(channel);
    }

    public String asString(String name) {
        return getParameter(name).asString();
    }

    public String asString(String name, int channel) {
        return getParameter(name).asString(channel);
    }

    public Object asData(String name) {
        return getParameter(name).asData();
    }

    public Object asData(String name, int channel) {
        return getParameter(name).asData(channel);
    }

    public void set(String name, int value) {
        getParameter(name).set(value);
    }

    public void set(String name, int value, int channel) {
        getParameter(name).set(value, channel);
    }

    public void set(String name, double value) {
        getParameter(name).set(value);
    }

    public void set(String name, double value, int channel) {
        getParameter(name).set(value, channel);
    }

    public void set(String name, String value) {
        getParameter(name).set(value);
    }

    public void set(String name, String value, int channel) {
        getParameter(name).set(value, channel);
    }

    public void set(String name, Object value) {
        getParameter(name).set(value);
    }

    public void set(String name, Object value, int channel) {
        getParameter(name).set(value, channel);
    }

    //// Expression shortcuts ////

    //// Connection shortcuts ////

    /**
     * Returns if there is a connection on the specified port.
     *
     * @param port the port index
     * @return true if the port is connected
     */
    public boolean isInputConnected(int port) {
        if (upstreams.isEmpty()) return false;
        return getInputConnection(port) != null;
    }

    /**
     * Return the Connection object for the specified port.
     * If the port is non-existant or nothing is connected to the port,
     * this method returns null.
     *
     * @param port the port index for this connection
     * @return a Connection object or null.
     * @see #isInputConnected(int)
     */
    public Connection getInputConnection(int port) {
        if (port < 0 | port > maximumInputs) return null;
        for (Connection c : upstreams) {
            if (c.getInputPort() == port)
                return c;
        }
        return null;
    }

    public List<Connection> getInputConnections() {
        return new ArrayList<Connection>(upstreams);
    }

    public List<Connection> getOutputConnections() {
        return new ArrayList<Connection>(downstreams);
    }

    /**
     * Disconnects a specific input from the node.
     * Also removes the downstream connection in the corresponding output node.
     *
     * @param port the port number you want to disconnect
     * @return true if the port was disconnected, false if nothing was connected to the port
     *         or if the port does not exist.
     */
    public boolean disconnectInput(int port) {
        Connection c = getInputConnection(port);
        if (c == null) return false;
        upstreams.remove(c);
        c.getOutputNode().downstreams.remove(c);
        setChanged();
        notifyObservers();
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
        for (Connection c : upstreams) {
            removedSomething = disconnectInput(c.getInputPort()) | removedSomething;
        }

        // Disconnect all my outputs.
        for (Connection c : downstreams) {
            removedSomething = c.getInputNode().disconnectInput(c.getInputPort()) | removedSomething;
        }

        return removedSomething;
    }

    public boolean isConnected() {
        return !upstreams.isEmpty() || !downstreams.isEmpty();
    }

    //// Change notification ////

    public void setChanged() {
        super.setChanged();
        notifyObservers();
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
        notifyObservers();
    }

    public boolean isDirty() {
        return dirty;
    }

    /**
     * This method does the actual functionality of the node.
     *
     * @return true if the evaluation succeeded.
     */
    protected boolean evaluate(ProcessingContext ctx) {
        return true;
    }

    //// Output ////

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ": " + name + ">";
    }

    //// Persistence ////


}
