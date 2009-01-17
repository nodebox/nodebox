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
import net.nodebox.graphics.Point;
import net.nodebox.handle.Handle;
import net.nodebox.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Node is a building block in a network and encapsulates specific functionality.
 * <p/>
 * The operation of the Node is specified through its parameters. The data that flows
 * through the node passes through ports.
 */
public class Node {

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
     * The type of this node. This contains all the meta-information about the node.
     */
    private NodeType nodeType;

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
    protected List<Message> messages = new ArrayList<Message>();

    private static Logger logger = Logger.getLogger("net.nodebox.node.Node");


    public enum MessageLevel {
        DEBUG, INFO, WARNING, ERROR
    }

    public static class Message {

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

        @Override
        public String toString() {
            return level + ": " + message;
        }
    }

    //// Constructors ////

    public Node(NodeType nodeType) {
        this.nodeType = nodeType;
        this.name = getDefaultName();
        for (ParameterType pt : nodeType.getParameterTypes()) {
            parameters.put(pt.getName(), pt.createParameter(this));
        }
        outputParameter = (OutputParameter) nodeType.getOutputParameterType().createParameter(this);
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

    //// Type ////

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        // TODO: migrate parameters
    }

    //// Naming ////

    public String getDefaultName() {
        return getNodeType().getDefaultName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidNameException {
        // Since the network does the rename, fireNodeChanged() will be called from the network.
        if (inNetwork()) {
            network.rename(this, name);
        } else {
            getNodeType().validateName(name);
            this.name = name;
        }
    }

    protected void _setName(String name) {
        this.name = name;
    }

    //// Parameters ////


    public List<Parameter> getParameters() {
        // Parameters are stored in a map, and are not ordered.
        // The correct order is that of ParameterTypes in the NodeType.
        List<Parameter> plist = new ArrayList<Parameter>();
        for (ParameterType pt : getNodeType().getParameterTypes()) {
            plist.add(getParameter(pt.getName()));
        }
        return plist;
    }

    public int parameterCount() {
        return parameters.size();
    }

    public Parameter getParameter(String name) throws NotFoundException {
        if (hasParameter(name)) {
            return parameters.get(name);
        } else {
            throw new NotFoundException(this, name, "The node " + getAbsolutePath() + " does not have a parameter '" + name + "'");
        }
    }

    public boolean hasParameter(String name) {
        return parameters.containsKey(name);
    }


    public OutputParameter getOutputParameter() {
        return outputParameter;
    }

    //// Parameter events /////
    // These come from the NodeType.

    /**
     * Invoked when the type/core type were changed.
     *
     * @param source the ParameterType this event comes from.
     */
    public void typeChangedEvent(ParameterType source) {
    }

    /**
     * Invoked when the bounding method or minimum/maximum values were changed.
     *
     * @param source the ParameterType this event comes from.
     */
    public void boundingChangedEvent(ParameterType source) {
        getParameter(source.getName()).boundingChangedEvent(source);
    }

    /**
     * Invoked when the display level was changed.
     *
     * @param source the ParameterType this event comes from.
     */
    public void displayLevelChangedEvent(ParameterType source) {
    }

    /**
     * Invoked when the null allowed flag was changed.
     *
     * @param source the ParameterType this event comes from.
     */
    public void nullAllowedChangedEvent(ParameterType source) {
    }

    //// Network ////

    public Network getNetwork() {
        return network;
    }

    public Network getRootNetwork() {
        if (!inNetwork()) return null;
        Network net = network;
        while (net.getNetwork() != null) {
            net = net.getNetwork();
        }
        return net;
    }


    protected void _setNetwork(Network network) {
        this.network = network;
    }

    public void setNetwork(Network network) throws InvalidNameException {
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

    public Grob asGrob(String name) {
        return getParameter(name).asGrob();
    }

    public boolean asBoolean(String name) {
        return getParameter(name).asBoolean();
    }

    public Object getValue(String name) {
        return getParameter(name).getValue();
    }

    public List<Object> getValues(String name) {
        return getParameter(name).getValues();
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

    /**
     * Set the value or fail silently.
     * <p/>
     * The normal set method throws an error whenever the parameter is connected.
     * This version silently discards the error.
     *
     * @param name  name of the parameter.
     * @param value value of the parameter.
     */
    public void silentSet(String name, int value) {
        try {
            getParameter(name).set(value);
        } catch (ValueError e) {
        }
    }

    /**
     * Set the value or fail silently.
     * <p/>
     * The normal set method throws an error whenever the parameter is connected.
     * This version silently discards the error.
     *
     * @param name  name of the parameter.
     * @param value value of the parameter.
     */
    public void silentSet(String name, double value) {
        try {
            getParameter(name).set(value);
        } catch (ValueError e) {
        }
    }

    /**
     * Set the value or fail silently.
     * <p/>
     * The normal set method throws an error whenever the parameter is connected.
     * This version silently discards the error.
     *
     * @param name  name of the parameter.
     * @param value value of the parameter.
     */
    public void silentSet(String name, String value) {
        try {
            getParameter(name).set(value);
        } catch (ValueError e) {
        }
    }

    /**
     * Set the value or fail silently.
     * <p/>
     * The normal set method throws an error whenever the parameter is connected.
     * This version silently discards the error.
     *
     * @param name  name of the parameter.
     * @param value value of the parameter.
     */
    public void silentSet(String name, Color value) {
        try {
            getParameter(name).set(value);
        } catch (ValueError e) {
        }
    }

    /**
     * Set the value or fail silently.
     * <p/>
     * The normal set method throws an error whenever the parameter is connected.
     * This version silently discards the error.
     *
     * @param name  name of the parameter.
     * @param value value of the parameter.
     */
    public void silentSet(String name, Object value) {
        getParameter(name).setValue(value);
    }

    public void setValue(String name, Object value) {
        getParameter(name).setValue(value);
    }
    //// Output value shortcuts ////

    public Object getOutputValue() {
        return outputParameter.getValue();
    }

    public void setOutputValue(Object value) throws ValueError {
        outputParameter.setValue(value);
    }

    //// Expression shortcuts ////

    //// Connection shortcuts ////

    /**
     * Return a list of all parameters on this Node that can be connected to the given output node.
     *
     * @param outputNode the output node
     * @return a list of parameters.
     */
    public List<Parameter> getCompatibleInputs(Node outputNode) {
        ParameterType.Type outputType = outputNode.getOutputParameter().getType();
        List<Parameter> compatibleParameters = new ArrayList<Parameter>();
        for (Parameter p : getParameters()) {
            if (p.canConnectTo(outputNode))
                compatibleParameters.add(p);
        }
        return compatibleParameters;
    }

    public List<Connection> getInputConnections() {
        List<Connection> inputConnections = new ArrayList<Connection>();
        for (Parameter p : parameters.values()) {
            List<Connection> connections = getNetwork().getUpstreamConnections(p);
            if (connections != null)
                inputConnections.addAll(connections);
        }
        return inputConnections;
    }

    public List<Connection> getOutputConnections() {
        List<Connection> outputConnections = new ArrayList<Connection>();
        outputConnections.addAll(getOutputParameter().getDownstreamConnections());
        for (Parameter p : parameters.values()) {
            List<Connection> connections = getNetwork().getDownstreamConnections(p);
            if (connections != null)
                outputConnections.addAll(connections);
        }
        return outputConnections;
    }

    public List<Connection> getConnections() {
        List<Connection> connections = new ArrayList<Connection>();
        connections.addAll(getInputConnections());
        connections.addAll(getOutputConnections());
        return connections;
    }

    /**
     * Removes all connections from and to this node.
     * This only removes explicit connections.
     *
     * @return true if connections were removed.
     */
    public boolean disconnect() {
        boolean removedSomething = false;

        // Disconnect all my inputs.
        for (Parameter p : parameters.values()) {
            removedSomething = network.disconnect(p) | removedSomething;
        }

        // Disconnect all my outputs.
        // Copy the list of downstreams, since you will be removing elements
        // from it while iterating.
        List<Connection> downstreamConnections = new ArrayList<Connection>(getOutputParameter().getDownstreamConnections());
        for (Connection c : downstreamConnections) {
            removedSomething = network.disconnect(getOutputParameter(), c.getInputParameter(), Connection.Type.EXPLICIT) | removedSomething;
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
        messages.add(new Message(MessageLevel.INFO, msg));
    }

    public void addWarning(String msg) {
        messages.add(new Message(MessageLevel.WARNING, msg));
    }

    public void addError(String msg) {
        messages.add(new Message(MessageLevel.ERROR, msg));
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

    public List<Message> getMessages() {
        return new ArrayList<Message>(messages);
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
            try {
                p.update(ctx);
            } catch (Exception e) {
                messages.add(new Message(MessageLevel.ERROR, p.getName() + ": " + e.getMessage()));
                dirty = false;
                return false;
            }
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
    public boolean process(ProcessingContext ctx) {
        try {
            return getNodeType().process(this, ctx);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            addError("Error while processing " + getAbsolutePath() + "\n" + sw.toString());
            return false;
        }
    }

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

    //// Handle support ////

    /**
     * Creates and returns a Handle object that can be used for direct manipulation of the parameters of this node.
     * The handle is bound to this node.
     * <p/>
     * This method may return null to indicate that no handle is available.
     * <p/>
     * You should not override this method, but rather the createHandle method on the NodeType.
     *
     * @return a handle instance bound to this node, or null.
     * @see net.nodebox.node.NodeType#createHandle(Node)
     */
    public Handle createHandle() {
        return getNodeType().createHandle(this);
    }

    //// Cloning ////

    /**
     * Copy this node and all its upstream connections.
     * Used with deferreds.
     *
     * @param newNetwork the new network that will be the parent of the newly cloned node.
     * @return a copy of the node with copies to all of its upstream connections.
     */
    public Node copyWithUpstream(Network newNetwork) {
        Constructor nodeConstructor;
        try {
            nodeConstructor = getClass().getConstructor(NodeType.class);
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE, "Class " + getClass() + " has no appropriate constructor.", e);
            return null;
        }


        Node newNode;
        try {
            newNode = (Node) nodeConstructor.newInstance(nodeType);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Class " + getClass() + " cannot be instantiated.", e);
            return null;
        }
        newNode.setName(getName());
        newNode.setNetwork(newNetwork);

        for (Parameter p : parameters.values()) {
            newNode.parameters.remove(p);
            newNode.parameters.put(p.getName(), p.copyWithUpstream(newNode));
        }
        return newNode;
    }

    //// Output ////

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + ": " + name + ">";
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
        // Build the node
        xml.append(spaces).append("<node");
        xml.append(" name=\"").append(getName()).append("\"");
        xml.append(" type=\"").append(getNodeType().getQualifiedName()).append("\"");
        xml.append(" version=\"").append(getNodeType().getVersionAsString()).append("\"");
        xml.append(" x=\"").append(getX()).append("\"");
        xml.append(" y=\"").append(getY()).append("\"");
        if (isRendered())
            xml.append(" rendered=\"true\"");
        xml.append(">\n");
        xml.append(spaces);
        xml.append("  <data>\n");

        // Build the parameter list
        dataToXml(xml, spaces);

        // End the node
        xml.append(spaces);
        xml.append("  </data>\n");
        xml.append(spaces);
        xml.append("</node>\n");
    }

    public void dataToXml(StringBuffer xml, String spaces) {
        for (Parameter p : getParameters()) {
            p.toXml(xml, spaces + "  ");
        }
    }


}
