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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Frederik
 */
public class Node {

    public static final String LOG_NODE = "node";
    private double x, y;
    private String name;
    private Network network;
    private HashMap<String, Parameter> parameters = new HashMap<String, Parameter>();
    private Parameter outputParameter;
    private ArrayList<Connection> downstreams = new ArrayList<Connection>();
    // TODO: add exception
    boolean dirty = true;

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

        public String getName() {
            return name;
        }

        public Node getNode() {
            return node;
        }
    }

    public Node(String outputType) {
        name = defaultName();
        outputParameter = new Parameter(this, "out", outputType, Parameter.DIRECTION_OUT);
    }

    public void _setNetwork(Network network) {
        this.network = network;
    }

    //// Naming ////
    public String defaultName() {
        return getClass().getSimpleName().toLowerCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (inNetwork()) {
            network.rename(this, name);
        } else {
            _setName(name);
        }
    }

    public static boolean validName(String name) {
        Pattern nodeNamePattern = Pattern.compile("^[a-z_][a-z0-9_]{0,29}$");
        Pattern doubleUnderScorePattern = Pattern.compile("^__.*$");
        Matcher m1 = nodeNamePattern.matcher(name);
        Matcher m2 = doubleUnderScorePattern.matcher(name);
        return m1.matches() && !m2.matches();
    }

    public void _setName(String name) {
        if (validName(name)) {
            this.name = name;
        } else {
            throw new InvalidName(this, name);
        }
    }

    //// Network ////
    public Network getNetwork() {
        return network;
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

    //// Position ////
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        // TODO: notify
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        // TODO: notify
    }

    //// Parameter operations ////
    public Parameter addParameter(String name, String type) {
        if (hasParameter(name)) {
            throw new Parameter.InvalidName(null, name, "There is already a parameter called \"" + name + "\" for this node.");
        }
        Parameter p = new Parameter(this, name, type);
        parameters.put(name, p);
        markDirty();
        return p;
    }

    public Parameter getParameter(String name) {
        if (hasParameter(name)) {
            return parameters.get(name);
        } else {
            throw new Parameter.NotFound(this, name);
        }
    }

    public void renameParameter(Parameter parameter, String newName) {
        if (parameter.getName().equals(newName)) {
            return;
        }
        if (hasParameter(newName)) {
            throw new Parameter.InvalidName(parameter, newName, "You tried to rename your parameter to \"" + newName + "\", which is the name of another parameter for this node.");
        }
        parameters.remove(parameter.getName());
        parameter._setName(newName);
        parameters.put(newName, parameter);
    }

    public boolean hasParameter(String name) {
        return parameters.containsKey(name);
    }

    public Parameter getOutputParameter() {
        return outputParameter;
    }

    public String getOutputType() {
        return outputParameter.getType();
    }

    public Collection<Parameter> getParameters() {
        return new ArrayList<Parameter>(parameters.values());
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

    public int outputAsInt() {
        return outputParameter.asInt();
    }

    public int outputAsInt(int channel) {
        return outputParameter.asInt(channel);
    }

    public double outputAsFloat() {
        return outputParameter.asFloat();
    }

    public double outputAsFloat(int channel) {
        return outputParameter.asFloat(channel);
    }

    public String outputAsString() {
        return outputParameter.asString();
    }

    public String outputAsString(int channel) {
        return outputParameter.asString(channel);
    }

    public Object outputAsData() {
        return outputParameter.asData();
    }

    public Object outputAsData(int channel) {
        return outputParameter.asData(channel);
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

    //// Dirty management ////
    public void markDirty() {
        if (dirty) {
            return;
        }
        dirty = true;
        for (Connection c : downstreams) {
            c.getInputNode().markDirty();
        }
        if (inNetwork() && !network.isDirty()) {
            // TODO: this is not ideal, since only changes to the rendered node should make the network dirty.
            network.markDirty();
        }
        // TODO: notify
    }

    public boolean isDirty() {
        return dirty;
    }

    public void update() {
        if (dirty) {
            for (Parameter p : parameters.values()) {
                p.update();
            }
        }
        process();
        dirty = false;
    }

    //// Output ////
    public boolean isOutputConnected() {
        return downstreams.size() > 0;
    }

    public boolean isOutputConnectedTo(Node node) {
        for (Connection c : downstreams) {
            if (c.getInputNode() == node) {
                return true;
            }
        }
        return false;
    }

    public boolean isOutputConnectedTo(Parameter parameter) {
        for (Connection c : downstreams) {
            if (c.getInputParameter() == parameter) {
                return true;
            }
        }
        return false;
    }

    public Collection<Connection> getOutputConnections() {
        return downstreams;
    }

    public void _setOutput(int value) {
        outputParameter.set(value);
    }

    public void _setOutput(int value, int channel) {
        outputParameter.set(value, channel);
    }

    public void _setOutput(double value) {
        outputParameter.set(value);
    }

    public void _setOutput(double value, int channel) {
        outputParameter.set(value, channel);
    }

    public void _setOutput(String value) {
        outputParameter.set(value);
    }

    public void _setOutput(String value, int channel) {
        outputParameter.set(value, channel);
    }

    public void _setOutput(Object value) {
        outputParameter.set(value);
    }

    public void _setOutput(Object value, int channel) {
        outputParameter.set(value, channel);
    }

    //// Processing ////
    protected void process() {
        // This space intentionally left blank.
    }

    //// Connection ////
    public boolean isConnected() {
        // Check upstream connections
        for (Parameter p : parameters.values()) {
            if (p.isConnected()) {
                return true;
            }
        }
        // Check downstream connections
        return !downstreams.isEmpty();
    }

    public void disconnect() {
        // Disconnect upstream
        for (Parameter p : parameters.values()) {
            p.disconnect();
        }
        // Disconnect downstream
        outputParameter.disconnect();
    }

    public void _addDownstream(Connection connection) {
        // TODO: Check if the connection/parameter is already in the list.
        assert (connection != null);
        assert (connection.getOutputNode() == this);
        assert (connection.getInputNode() != this);
        downstreams.add(connection);
    }

    public void _removeDownstream(Connection connection) {
        if (!downstreams.remove(connection)) {
            Logger.getLogger(LOG_NODE).log(Level.WARNING, "Could not remove connection " + connection + " on node " + name);
            assert (false);
        }
    }

    //// Output ////
    @Override
    public String toString() {
        return "Node(" + name + ")";
    }
}
