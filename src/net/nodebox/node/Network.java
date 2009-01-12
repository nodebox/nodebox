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

import org.xml.sax.InputSource;

import javax.swing.event.EventListenerList;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Network is a collection of nodes in a graph structure.
 * <p/>
 * The important part about grouping a collection of nodes is that you can connect them together.
 * This allows for many processing possibilities, where you can connect several nodes together forming
 * very complicated networks. Networks, in turn, can be rigged up to form sort of black-boxes, with some
 * input parameters and an output parameter, so they form a Node themselves, that can be used to form
 * even more complicated networks, etc.
 * <p/>
 * Central in this concept is the directed acyclic graph, or DAG. This is a graph where all the edges
 * are directed, and no cycles can be formed, so you do not run into recursive loops. The vertexes of
 * the graph are the nodes, and the edges are the connections between them.
 * <p/>
 * One of the vertexes in the graph is set as the rendered node, and from there on, the processing starts,
 * working its way upwards in the network, processing other nodes (and their inputs) as they come along.
 */
public class Network extends Node {

    /**
     * A list of all the nodes in this network.
     */
    private HashMap<String, Node> nodes = new HashMap<String, Node>();

    /**
     * All connections, keyed by the output (or destination), and going downstream,
     * to the input (or origin).
     * Key = output parameter
     * Value = ArrayList of input Parameters on other nodes.
     */
    private HashMap<Parameter, List<Connection>> downstreams = new HashMap<Parameter, List<Connection>>();

    /**
     * All connections, keyed by the input (or origin), and going upstream,
     * to the output (or destination).
     * Key = input Parameter
     * Value = List of connection objects.
     */
    private HashMap<Parameter, List<Connection>> upstreams = new HashMap<Parameter, List<Connection>>();


    /**
     * The node being rendered in this network.
     */
    private Node renderedNode = null;

    /**
     * The event listeners registered to listen to this network.
     */
    private EventListenerList listeners = new EventListenerList();

    private static Logger logger = Logger.getLogger("net.nodebox.node.Network");

    //// Constructors ////

    public Network(NodeType nodeType) {
        super(nodeType);
    }

    //// Container operations ////

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public int size() {
        return nodes.size();
    }

    public void add(Node node) {
        assert (node != null);
        if (contains(node)) {
            return;
        }
        if (contains(node.getName())) {
            throw new InvalidNameException(this, node.getName(), "There is already a node named \"" + node.getName() + "\" in network " + getAbsolutePath());
        }
        node._setNetwork(this);
        nodes.put(node.getName(), node);
        fireNodeAdded(node);
    }

    public Node create(NodeType nodeType) {
        Node newNode = nodeType.createNode();
        setUniqueNodeName(newNode);
        add(newNode);
        return newNode;
    }

    public boolean remove(Node node) {
        assert (node != null);
        if (!contains(node)) {
            return false;
        }
        node.markDirty();
        // TODO: disconnect node from the old network.
        node.disconnect();
        nodes.remove(node.getName());
        if (node == renderedNode) {
            setRenderedNode(null);
        }
        fireNodeRemoved(node);
        return true;
    }

    public boolean contains(Node node) {
        return nodes.containsValue(node);
    }

    public boolean contains(String nodeName) {
        return nodes.containsKey(nodeName);
    }

    public Node getNode(String nodeName) {
        if (!contains(nodeName)) {
            throw new NotFoundException(this, nodeName, "Could not find node '" + nodeName + "' in network " + getAbsolutePath());
        }
        return nodes.get(nodeName);
    }

    public List<Node> getNodes() {
        return new ArrayList<Node>(nodes.values());
    }

    //// Naming operations ////

    /**
     * Changes the name of the given node to one that is unique within this network.
     * <p/>
     * Note that the node doesn't have to be in the network.
     *
     * @param node the node
     * @return the unique node name
     */
    public String setUniqueNodeName(Node node) {
        int counter = 1;
        while (true) {
            String suggestedName = node.getDefaultName() + counter;
            if (!contains(suggestedName)) {
                // We don't use rename here, since it assumes the node will be in 
                // this network.
                node.setName(suggestedName);
                return suggestedName;
            }
            ++counter;
        }
    }

    public boolean rename(Node node, String newName) {
        assert (contains(node));
        if (node.getName().equals(newName)) {
            return true;
        }
        if (contains(newName)) {
            return false;
        }
        nodes.remove(node.getName());
        node._setName(newName);
        nodes.put(newName, node);
        fireNodeChanged(node);
        return true;
    }

    //// Connections ////

    /**
     * Convenience method for connecting nodes to eachother.
     *
     * @param outputNode    the upstream node whose output parameter is to be connected.
     * @param inputNode     the downstream node whose input is to be connected.
     * @param parameterName the name of the parameter on the input node.
     * @return the connection object.
     */
    public Connection connect(Node outputNode, Node inputNode, String parameterName) {
        Parameter outputParameter = outputNode.getOutputParameter();
        Parameter inputParameter = inputNode.getParameter(parameterName);
        return connect(outputParameter, inputParameter);
    }

    /**
     * Connect two parameters to eachother.
     *
     * @param outputParameter the output parameter on the destination (upstream) node.
     * @param inputParameter  the input parameter on the origin (downstream) node.
     * @return the connection object.
     */
    public Connection connect(Parameter outputParameter, Parameter inputParameter) {
        return connect(outputParameter, inputParameter, Connection.Type.EXPLICIT);
    }

    /**
     * Connect two parameters to eachother.
     *
     * @param outputParameter the output parameter on the destination (upstream) node.
     * @param inputParameter  the input parameter on the origin (downstream) node.
     * @return the connection object.
     */
    public Connection connect(Parameter outputParameter, Parameter inputParameter, Connection.Type type) {
        // Sanity checks
        if (!inputParameter.isConnectable())
            throw new ConnectionError(inputParameter.getNode(), inputParameter, "Input parameter is not connectable.");
        if (!outputParameter.isConnectable())
            throw new ConnectionError(inputParameter.getNode(), inputParameter, "Output parameter is not connectable.");
        if (!inputParameter.isInputParameter())
            throw new ConnectionError(inputParameter.getNode(), inputParameter, "Input parameter is not an input parameter.");
        if (type == Connection.Type.EXPLICIT && !outputParameter.isOutputParameter())
            throw new ConnectionError(inputParameter.getNode(), outputParameter, "Output parameter is not an output parameter.");
        if (inputParameter.getNetwork() != this)
            throw new ConnectionError(inputParameter.getNode(), outputParameter, "The input parameter is not in this network.");
        if (!inputParameter.canConnectTo(outputParameter))
            throw new ConnectionError(inputParameter.getNode(), outputParameter, "The parameter types do not match.");
        if (type == Connection.Type.EXPLICIT && outputParameter.getNetwork() != this)
            throw new ConnectionError(inputParameter.getNode(), outputParameter, "The output parameter is not in this network.");
        if (inputParameter == outputParameter)
            throw new ConnectionError(inputParameter.getNode(), outputParameter, "The input and output parameter are the same.");

        // Check if there already is a connection of the same type between the input and output.
        Connection conn = getConnection(inputParameter, outputParameter, type);
        if (conn != null) return conn;

        // Check if there already is an explicit (non-expression) connection on this input.
        if (type == Connection.Type.EXPLICIT) {
            Connection explicitConn = getExplicitConnection(inputParameter);
            if (explicitConn != null) {
                // TODO: Multi-input input parameters should not automatically disconnect.
                disconnect(outputParameter, inputParameter, type);
            }
        }

        conn = new Connection(outputParameter, inputParameter, type);
        addUpstreamConnection(inputParameter, conn);
        addDownstreamConnection(outputParameter, conn);
        inputParameter.getNode().markDirty();
        fireConnectionAdded(conn);
        return conn;
    }

    /**
     * Remove an explicit connection between the output and input parameters.
     *
     * @param outputParameter
     * @param inputParameter  the input parameter on the origin (downstream) node.
     * @return true if the connection was found and disconnected.
     */
    public boolean disconnect(Parameter outputParameter, Parameter inputParameter) {
        return disconnect(outputParameter, inputParameter, Connection.Type.EXPLICIT);
    }

    /**
     * Disconnect two parameters.
     *
     * @param outputParameter the output parameter on the upstream node.
     * @param inputParameter  the input parameter on the origin (downstream) node.
     * @return true if the connection was found and disconnected.
     */
    public boolean disconnect(Parameter outputParameter, Parameter inputParameter, Connection.Type type) {
        assert inputParameter.getNode().getNetwork() == this;
        assert outputParameter.getNode().getNetwork() == this;
        assert inputParameter.isConnectable();
        assert outputParameter.isConnectable();
        assert outputParameter.isOutputParameter();

        // Find the connection
        Connection conn = getConnection(outputParameter, inputParameter, type);
        if (conn == null) return false;

        removeUpstreamConnection(inputParameter, conn);
        removeDownstreamConnection(outputParameter, conn);
        inputParameter.revertToDefault();
        inputParameter.getNode().markDirty();
        fireConnectionRemoved(conn);
        return true;
    }

    public boolean disconnect(Parameter inputParameter) {
        // Create a copy so the event firing can change the original list around.
        List<Connection> connections = upstreams.get(inputParameter);
        if (connections == null) return false;
        connections = new ArrayList<Connection>(connections);
        boolean removedSomething = false;
        for (Connection conn : connections) {
            boolean disconnected = disconnect(conn.getOutputParameter(), conn.getInputParameter(), conn.getType());
            assert (disconnected);
            fireConnectionRemoved(conn);
        }
        inputParameter.revertToDefault();
        inputParameter.getNode().markDirty();
        return true;
    }

    public Connection getExplicitConnection(Parameter inputParameter) {
        List<Connection> connections = upstreams.get(inputParameter);
        if (connections == null) return null;
        for (Connection conn : connections) {
            if (conn.getType() == Connection.Type.EXPLICIT)
                return conn;
        }
        return null;
    }

    public Connection getConnection(Parameter outputParameter, Parameter inputParameter) {
        for (Connection conn : upstreams.get(inputParameter)) {
            if (conn.getOutputParameter() == outputParameter)
                return conn;
        }
        return null;
    }

    public Connection getConnection(Parameter outputParameter, Parameter inputParameter, Connection.Type type) {
        List<Connection> connections = upstreams.get(inputParameter);
        if (connections == null) return null;
        for (Connection conn : connections) {
            if (conn.getOutputParameter() == outputParameter && conn.getType() == type)
                return conn;
        }
        return null;
    }

    /**
     * Returns whether the given parameter is connected.
     *
     * @param parameter the input or output parameter
     * @return true if the parameter is connected.
     */
    public boolean isConnected(Parameter parameter) {
        if (parameter.isOutputParameter()) {
            return downstreams.containsKey(parameter);
        } else {
            return upstreams.containsKey(parameter);
        }
    }

    /**
     * Returns whether the given input parameter is connected to the given output parameter.
     *
     * @param outputParameter the output parameter
     * @param inputParameter  the input parameter
     * @return true if the parameters are connected.
     */
    public boolean isConnectedTo(Parameter outputParameter, Parameter inputParameter) {
        for (Connection conn : upstreams.get(inputParameter)) {
            if (conn.getOutputParameter() == outputParameter)
                return true;
        }
        return false;
    }

    /**
     * Return a collection of connections in the network.
     *
     * @return an Iterator of all the connections.
     */
    public List<Connection> getConnections() {
        List<Connection> allConnections = new ArrayList<Connection>();
        for (List<Connection> connectionsForParameter : downstreams.values()) {
            allConnections.addAll(connectionsForParameter);
        }
        return allConnections;
    }

    /**
     * Return a Connection object containing all the outputs connected to this input.
     *
     * @param inputParameter the input Parameter
     * @return a Connection object.
     */
    public List<Connection> getUpstreamConnections(Parameter inputParameter) {
        List<Connection> connections = upstreams.get(inputParameter);
        if (connections == null) {
            return new ArrayList<Connection>();
        } else {
            return new ArrayList<Connection>(connections);
        }
    }

    /**
     * Return a list of all connections of this output parameter.
     *
     * @param outputParameter the output Parameter.
     * @return an Iterator of Connection objects.
     */
    public List<Connection> getDownstreamConnections(Parameter outputParameter) {
        List<Connection> connections = downstreams.get(outputParameter);
        if (connections == null) {
            return new ArrayList<Connection>();
        } else {
            return new ArrayList<Connection>(connections);
        }
    }

    private void addUpstreamConnection(Parameter inputParameter, Connection connection) {
        List<Connection> connections = upstreams.get(inputParameter);
        if (connections == null) {
            connections = new ArrayList<Connection>();
            upstreams.put(inputParameter, connections);
        }
        connections.add(connection);
    }

    private void addDownstreamConnection(Parameter outputParameter, Connection connection) {
        List<Connection> connections = downstreams.get(outputParameter);
        if (connections == null) {
            connections = new ArrayList<Connection>();
            downstreams.put(outputParameter, connections);
        }
        connections.add(connection);
    }

    private void removeUpstreamConnection(Parameter inputParameter, Connection connection) {
        List<Connection> connections = upstreams.get(inputParameter);
        boolean removed = connections.remove(connection);
        assert (removed);
        if (connections.isEmpty()) {
            upstreams.remove(inputParameter);
        }
    }

    private void removeDownstreamConnection(Parameter outputParameter, Connection connection) {
        List<Connection> connections = downstreams.get(outputParameter);
        boolean removed = connections.remove(connection);
        assert (removed);
        if (connections.isEmpty()) {
            downstreams.remove(outputParameter);
        }
    }

    //// Rendered node ////

    public Node getRenderedNode() {
        return renderedNode;
    }

    public void setRenderedNode(Node renderedNode) {
        if (renderedNode != null && !contains(renderedNode)) {
            throw new NotFoundException(this, renderedNode.getName(), "Node '" + renderedNode.getAbsolutePath() + "' is not in this network (" + getAbsolutePath() + ")");
        }
        if (this.renderedNode == renderedNode) return;
        this.renderedNode = renderedNode;
        markDirty();
        fireRenderedNodeChanged(renderedNode);
    }

    //// Processing ////

    @Override
    public void markDirty() {
        if (isDirty()) return;
        super.markDirty();
        fireNetworkDirty();
    }

    @Override
    public boolean update(ProcessingContext ctx) {
        boolean success = super.update(ctx);
        for (Node node : getNodes()) {
            for (Message m : node.getMessages()) {
                messages.add(new Message(m.getLevel(), node.getName() + ": " + m.getMessage()));
            }
        }
        fireNetworkUpdated();
        return success;
    }

    protected boolean updateRenderedNode(ProcessingContext ctx) {
        if (renderedNode == null) {
            addError("No node to render");
            return false;
        }
        assert (contains(renderedNode));
        return renderedNode.update(ctx);
    }

    @Override
    public boolean process(ProcessingContext ctx) {
        // TODO: this method does NOT get overridden by the different network types!
        boolean success = updateRenderedNode(ctx);
        if (success) {
            setOutputValue(renderedNode.getOutputValue());
        } else {
            // If there is nothing to render, set the output to a sane default.
            getOutputParameter().revertToDefault();
        }
        return success;
    }

    //// Cycle detection ////

    public boolean containsCycles() {
        return false;
    }

    //// Event handling ////

    public void addNetworkEventListener(NetworkEventListener l) {
        listeners.add(NetworkEventListener.class, l);
    }

    public void removeNetworkEventListener(NetworkEventListener l) {
        listeners.remove(NetworkEventListener.class, l);
    }

    public void fireNodeAdded(Node node) {
        if (listeners == null) return;
        for (EventListener l : listeners.getListeners(NetworkEventListener.class))
            ((NetworkEventListener) l).nodeAdded(this, node);
    }

    public void fireNodeRemoved(Node node) {
        if (listeners == null) return;
        for (EventListener l : listeners.getListeners(NetworkEventListener.class))
            ((NetworkEventListener) l).nodeRemoved(this, node);
    }

    public void fireConnectionAdded(Connection connection) {
        if (listeners == null) return;
        for (EventListener l : listeners.getListeners(NetworkEventListener.class))
            ((NetworkEventListener) l).connectionAdded(this, connection);
    }

    public void fireConnectionRemoved(Connection connection) {
        if (listeners == null) return;
        for (EventListener l : listeners.getListeners(NetworkEventListener.class))
            ((NetworkEventListener) l).connectionRemoved(this, connection);
    }

    public void fireRenderedNodeChanged(Node node) {
        if (listeners == null) return;
        for (EventListener l : listeners.getListeners(NetworkEventListener.class))
            ((NetworkEventListener) l).renderedNodeChanged(this, node);
    }

    public void fireNodeChanged(Node node) {
        if (listeners == null) return;
        for (EventListener l : listeners.getListeners(NetworkEventListener.class))
            ((NetworkEventListener) l).nodeChanged(this, node);
    }

    public void addNetworkDataListener(NetworkDataListener l) {
        listeners.add(NetworkDataListener.class, l);
    }

    public void removeNetworkDataListener(NetworkDataListener l) {
        listeners.remove(NetworkDataListener.class, l);
    }

    public void fireNetworkDirty() {
        if (listeners == null) return;
        for (EventListener l : listeners.getListeners(NetworkDataListener.class))
            ((NetworkDataListener) l).networkDirty(this);
    }

    public void fireNetworkUpdated() {
        if (listeners == null) return;
        for (EventListener l : listeners.getListeners(NetworkDataListener.class))
            ((NetworkDataListener) l).networkUpdated(this);
    }

    //// Persistence ////

    public String toXml() {
        StringBuffer xml = new StringBuffer();
        // Build the header
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<ndbx type=\"file\" formatVersion=\"0.8\">\n");
        toXml(xml, "  ");
        xml.append("</ndbx>");
        return xml.toString();
    }

    /**
     * Converts the data structure to xml. The xml String is appended to the given StringBuffer.
     *
     * @param xml    the StringBuffer to use when appending.
     * @param spaces the indentation
     * @see Network#toXml for returning the Network as a full xml document
     */
    public void toXml(StringBuffer xml, String spaces) {
        // Build the node
        xml.append(spaces);
        xml.append("<network");
        xml.append(" name=\"").append(getName()).append("\"");
        xml.append(" type=\"").append(getNodeType().getQualifiedName()).append("\"");
        xml.append(" version=\"").append(getNodeType().getVersionAsString()).append("\"");
        xml.append(" x=\"").append(getX()).append("\"");
        xml.append(" y=\"").append(getY()).append("\"");
        xml.append(">\n");

        // Create the data
        dataToXml(xml, spaces);

        // Include all vertices
        for (Node n : getNodes()) {
            n.toXml(xml, spaces + "  ");
        }

        // Include all edges
        for (Node n : getNodes()) {
            for (Connection c : n.getOutputConnections()) {
                if (c.isExplicit())
                    c.toXml(xml, spaces + "  ");
            }
        }

        // End the node
        xml.append(spaces).append("</network>\n");
    }

    public static Network load(NodeTypeLibraryManager manager, String s) throws RuntimeException {
        StringReader reader = new StringReader(s);
        return load(manager, new InputSource(reader));
    }

    public static Network load(NodeTypeLibraryManager manager, File file) throws RuntimeException {
        // Load the document
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Could not read file " + file, e);
            throw new RuntimeException("Could not read file " + file, e);
        }
        return load(manager, new InputSource(fis));
    }

    private static Network load(NodeTypeLibraryManager manager, InputSource source) throws RuntimeException {
        // Setup the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // The next lines make sure that the SAX parser doesn't try to validate the document,
        // or tries to load in external DTDs (such as those from W3). Non-parsing means you
        // don't need an internet connection to use the program, and speeds up loading the
        // document massively.
        try {
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Parsing feature not supported.", e);
            throw new RuntimeException("Parsing feature not supported.", e);
        }
        SAXParser parser;
        try {
            parser = factory.newSAXParser();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not create parser.", e);
            throw new RuntimeException("Could not create parser.", e);
        }

        // Parse the document
        XmlHandler handler = new XmlHandler(manager);
        try {
            parser.parse(source, handler);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during parsing.", e);
            throw new RuntimeException("Error during parsing.", e);
        }
        return handler.getNetwork();

    }


}
