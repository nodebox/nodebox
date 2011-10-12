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
package nodebox.node;

import nodebox.client.NodeBoxDocument;
import nodebox.graphics.Color;
import nodebox.graphics.Point;
import nodebox.handle.Handle;
import nodebox.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static nodebox.base.Preconditions.*;

/**
 * A Node is a building block in a network and encapsulates specific functionality.
 * <p/>
 * The operation of the Node is specified through its parameters. The data that flows
 * through the node passes through ports.
 * <p/>
 * Nodes can be nested using parent/child relationships. Then, you can connect them together.
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
public class Node implements NodeCode {

    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,29}$");
    private static final Pattern DOUBLE_UNDERSCORE_PATTERN = Pattern.compile("^__.*$");
    private static final Pattern RESERVED_WORD_PATTERN = Pattern.compile("^(node|network|root|context)$");
    private static final Pattern NUMBER_AT_THE_END = Pattern.compile("^(.*?)(\\d*)$");

    public static final String IMAGE_GENERIC = "__generic";

    public static final String OUTPUT_PORT_NAME = "output";

    public static final Node ROOT_NODE;

    public enum Attribute {
        LIBRARY, NAME, POSITION, EXPORT, DESCRIPTION, IMAGE, PARAMETER, PORT
    }

    static {
        ROOT_NODE = new Node(NodeLibrary.BUILTINS, "root", Object.class);
        ROOT_NODE.addParameter("_code", Parameter.Type.CODE, ROOT_NODE);
        ROOT_NODE.addParameter("_handle", Parameter.Type.CODE, new JavaMethodWrapper(Node.class, "doNothing"));
        ROOT_NODE.addParameter("_description", Parameter.Type.STRING, "Base node instance.");
        ROOT_NODE.addParameter("_image", Parameter.Type.STRING, IMAGE_GENERIC);
        NodeLibrary.BUILTINS.add(ROOT_NODE);
    }

    /**
     * The name of this node.
     */
    private String name;

    /**
     * The library this node is in.
     */
    private NodeLibrary library;

    /**
     * The parent for this node.
     */
    private Node parent;

    /**
     * The children of this node.
     */
    private HashMap<String, Node> children = new HashMap<String, Node>();

    /**
     * The node's prototype. NodeBox uses prototype-based inheritance to blur the lines between classes and instances.
     */
    private Node prototype;

    /**
     * The type of data that will be processed by this node.
     */
    private Class dataClass;

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
     * A flag that indicates that this node will be exported.
     * This flag only has effect for nodes directly under the root node in a library.
     */
    private boolean exported;

    /**
     * A map of all parameters.
     */
    private LinkedHashMap<String, Parameter> parameters = new LinkedHashMap<String, Parameter>();


    /**
     * A map of all the data ports within the system.
     */
    private LinkedHashMap<String, Port> ports = new LinkedHashMap<String, Port>();

    /**
     * The output port. This port will contain the processed data for this node.
     */
    private Port outputPort;

    /**
     * The child node to render.
     */
    private Node renderedChild;

    /**
     * All child connections within this node.
     */
    private List<Connection> connections = new ArrayList<Connection>();

    /**
     * The processing error. Null if no error occurred during processing.
     */
    private Throwable error;

    //// Constructors ////

    private Node(NodeLibrary library, String name, Class dataClass) {
        assert library != null;
        this.library = library;
        this.name = name;
        this.dataClass = dataClass;
        this.outputPort = new Port(this, OUTPUT_PORT_NAME, Port.Direction.OUT);
    }

    //// Naming /////

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidNameException {
        if (this.name.equals(name)) return;
        validateName(name);
        this.parent.children.remove(this.name);
        this.name = name;
        this.parent.children.put(this.name, this);
        getLibrary().fireNodeAttributeChanged(this, Attribute.NAME);
    }

    public NodeLibrary getLibrary() {
        return library;
    }

    public String getIdentifier() {
        return library + "." + name;
    }

    /**
     * Get an identifier that is relative to the given node.
     * <p/>
     * This means that if the node and prototype are in the same library, the identifier
     * is just the name of the prototype. Otherwise, the library name is added.
     *
     * @param relativeTo the instance this prototype is relative to
     * @return a short or long identifier
     */
    public String getRelativeIdentifier(Node relativeTo) {
        if (relativeTo.library == library) {
            return name;
        } else {
            return getIdentifier();
        }
    }

    public String getDescription() {
        return asString("_description");
    }

    public void setDescription(String description) {
        setValue("_description", description);
        getLibrary().fireNodeAttributeChanged(this, Attribute.DESCRIPTION);
    }

    public String getImage() {
        return asString("_image");
    }

    public void setImage(String image) {
        setValue("_image", image);
        getLibrary().fireNodeAttributeChanged(this, Attribute.IMAGE);
    }

    /**
     * Checks if the given name would be valid for this node.
     *
     * @param name the name to check.
     * @throws InvalidNameException if the name was invalid.
     */
    public static void validateName(String name) throws InvalidNameException {
        Matcher m1 = NODE_NAME_PATTERN.matcher(name);
        Matcher m2 = DOUBLE_UNDERSCORE_PATTERN.matcher(name);
        Matcher m3 = RESERVED_WORD_PATTERN.matcher(name);
        if (!m1.matches()) {
            throw new InvalidNameException(null, name, "Names can only contain lowercase letters, numbers, and the underscore. Names cannot be longer than 29 characters.");
        }
        if (m2.matches()) {
            throw new InvalidNameException(null, name, "Names starting with double underscore are reserved for internal use.");
        }
        if (m3.matches()) {
            throw new InvalidNameException(null, name, "Names cannot be a reserved word (network, node, root).");
        }
    }

    //// Parent/child relationship ////

    public Node getParent() {
        return parent;
    }

    public Node getRoot() {
        Node n = this;
        while (n.getParent() != null) {
            n = n.getParent();
        }
        return n;
    }

    /**
     * Reparent the node.
     * <p/>
     * This breaks all connections.
     *
     * @param parent the new parent
     */
    public void setParent(Node parent) {
        // This method is called indirectly by newInstance.
        // newInstance has set the parent, but has not added it to
        // the library yet. Therefore, we cannot do this.parent == parent,
        // but need to check parent.contains()
        if (parent != null && parent.containsChildNode(this)) return;
        if (parent != null && parent.containsChildNode(name))
            throw new InvalidNameException(this, name, "There is already a node named \"" + name + "\" in " + parent);
        // Since this node will reside under a different parent, it can no longer maintain connections within
        // the previous parent. Break all connections. We need to do this before the parent changes.
        disconnect();
        if (this.parent != null)
            this.parent.remove(this);
        this.parent = parent;
        if (parent != null) {
            parent.children.put(name, this);
            // We're on the child node, so we need to fire the child added event
            // on the parent with this child as the argument.
            getLibrary().fireChildAdded(parent, this);
        }
    }

    public boolean hasParent() {
        return parent != null;
    }

    public boolean isLeaf() {
        return isEmpty();
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public int size() {
        return children.size();
    }

    public void add(Node node) {
        if (node == null)
            throw new IllegalArgumentException("The node cannot be null.");
        node.setParent(this);
    }

    /**
     * Create a child node under this node from the given prototype.
     * The name for this child is generated automatically.
     *
     * @param prototype the prototype node
     * @return a new Node
     */
    public Node create(Node prototype) {
        if (prototype == null) throw new IllegalArgumentException("Prototype cannot be null.");
        return create(prototype, null, null);
    }

    /**
     * Create a child node under this node from the given prototype.
     *
     * @param prototype the prototype node
     * @param name      the name of the new node
     * @return a new Node
     */
    public Node create(Node prototype, String name) {
        return create(prototype, name, null);
    }

    /**
     * Create a child node under this node from the given prototype.
     * The name for this child is generated automatically.
     *
     * @param prototype the prototype node
     * @param dataClass the type of data this new node instance will output.
     * @return a new Node
     */
    public Node create(Node prototype, Class dataClass) {
        return create(prototype, null, dataClass);
    }

    /**
     * Create a child node under this node from the given prototype.
     *
     * @param prototype the prototype node
     * @param name      the name of the new node
     * @param dataClass the type of data this new node instance will output.
     * @return a new Node
     */
    public Node create(Node prototype, String name, Class dataClass) {
        if (prototype == null) throw new IllegalArgumentException("Prototype cannot be null.");
        if (dataClass == null) dataClass = prototype.getDataClass();
        if (name == null) name = uniqueName(prototype.getName());
        Node newNode = prototype.rawInstance(library, name, dataClass);
        add(newNode);
        return newNode;
    }

    public boolean remove(Node node) {
        assert (node != null);
        if (!containsChildNode(node))
            return false;
        node.markDirty();
        node.disconnect();
        node.parent = null;
        children.remove(node.getName());
        if (node == renderedChild) {
            setRenderedChild(null);
        }
        getLibrary().fireChildRemoved(this, node);
        return true;
    }

    public String uniqueName(String prefix) {
        Matcher m = NUMBER_AT_THE_END.matcher(prefix);
        m.find();
        String namePrefix = m.group(1);
        String number = m.group(2);
        int counter;
        if (number.length() > 0) {
            counter = Integer.parseInt(number);
        } else {
            counter = 1;
        }
        while (true) {
            String suggestedName = namePrefix + counter;
            if (!containsChildNode(suggestedName)) {
                // We don't use rename here, since it assumes the node will be in
                // this network.
                return suggestedName;
            }
            ++counter;
        }
    }

    public boolean containsChildNode(String nodeName) {
        return children.containsKey(nodeName);
    }

    public boolean containsChildNode(Node node) {
        return children.containsValue(node);
    }

    public boolean containsChildPort(Port port) {
        // TODO: This check will need to change once we move to readonly.
        return port.getParentNode() == this;
    }

    public Node getChild(String nodeName) {
        return children.get(nodeName);
    }

    public Node getExportedChild(String nodeName) {
        Node child = getChild(nodeName);
        if (child == null) return null;
        if (child.isExported()) {
            return child;
        } else {
            return null;
        }
    }

    public Node getChildAt(int index) {
        Collection c = children.values();
        if (index >= c.size()) return null;
        return (Node) c.toArray()[index];
    }

    public int getChildCount() {
        return children.size();
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public List<Node> getChildren() {
        return new ArrayList<Node>(children.values());
    }

    //// Rendered ////

    public Node getRenderedChild() {
        return renderedChild;
    }

    public void setRenderedChild(Node renderedChild) {
        if (renderedChild != null && !containsChildNode(renderedChild)) {
            throw new NotFoundException(this, renderedChild.getName(), "Node '" + renderedChild.getAbsolutePath() + "' is not in this network (" + getAbsolutePath() + ")");
        }
        if (this.renderedChild == renderedChild) return;
        this.renderedChild = renderedChild;
        markDirty();
        getLibrary().fireRenderedChildChanged(this, renderedChild);
    }

    public boolean isRendered() {
        return parent != null && parent.getRenderedChild() == this;
    }

    public void setRendered() {
        if (parent == null) return;
        parent.setRenderedChild(this);
    }

    //// Path ////

    public String getAbsolutePath() {
        ArrayList<String> parts = new ArrayList<String>();
        Node child = this;
        Node root = getLibrary().getRootNode();
        while (child != null && child != root) {
            parts.add(0, child.getName());
            child = child.getParent();
        }
        if (parts.isEmpty()) {
            return "/";
        } else {
            return "/" + StringUtils.join(parts, "/");
        }
    }

    //// Prototype ////

    public Node getPrototype() {
        return prototype;
    }

    //// Data Class ////

    public Class getDataClass() {
        return dataClass;
    }

    public void validate(Object value) throws IllegalArgumentException {
        // Null is accepted as a default value.
        if (value == null) return;
        if (!getDataClass().isAssignableFrom(value.getClass()))
            throw new IllegalArgumentException("Value " + value + " is not of required class (was " + value.getClass() + ", required " + getDataClass());
    }

    //// Position ////

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        getLibrary().fireNodeAttributeChanged(this, Attribute.POSITION);
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        getLibrary().fireNodeAttributeChanged(this, Attribute.POSITION);
    }

    public Point getPosition() {
        return new Point((float) x, (float) y);
    }

    public void setPosition(Point p) {
        setPosition(p.getX(), p.getY());
    }

    public void setPosition(double x, double y) {
        if (this.x == x && this.y == y) return;
        this.x = x;
        this.y = y;
        getLibrary().fireNodeAttributeChanged(this, Attribute.POSITION);
    }

    //// Export flag ////


    public boolean isExported() {
        return exported;
    }

    public void setExported(boolean exported) {
        this.exported = exported;
        getLibrary().fireNodeAttributeChanged(this, Attribute.EXPORT);
    }


    //// Parameters ////

    /**
     * Get a list of all parameters for this node.
     *
     * @return a list of all the parameters for this node.
     */
    public List<Parameter> getParameters() {
        return new ArrayList<Parameter>(parameters.values());
    }

    public int getParameterCount() {
        return parameters.size();
    }

    public Parameter addParameter(String name, Parameter.Type type) {
        Parameter p = new Parameter(this, name, type);
        parameters.put(name, p);
        getLibrary().fireNodeAttributeChanged(this, Attribute.PARAMETER);
        return p;
    }

    public Parameter addParameter(String name, Parameter.Type type, Object value) {
        Parameter p = addParameter(name, type);
        p.setValue(value);
        getLibrary().fireNodeAttributeChanged(this, Attribute.PARAMETER);
        return p;
    }

    /**
     * Remove a parameter with the given name.
     * <p/>
     * If the parameter does not exist, this method returns false.
     *
     * @param name the parameter name
     * @return true if the parameter exists and was removed.
     */
    public boolean removeParameter(String name) {
        // First remove all dependencies to and from this parameter.
        // Don't rewrite any expressions.
        Parameter p = parameters.get(name);
        if (p == null) return false;
        p.removedEvent();
        parameters.remove(name);
        getLibrary().fireNodeAttributeChanged(this, Attribute.PARAMETER);
        markDirty();
        return true;
    }

    /**
     * Get a parameter with the given name
     *
     * @param name the parameter name
     * @return a Parameter or null if the parameter could not be found.
     */
    public Parameter getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Checks if this node has a parameter with the given name.
     *
     * @param name the parameter name
     * @return true if a Parameter with that name exists
     */
    public boolean hasParameter(String name) {
        return getParameter(name) != null;
    }

    /**
     * This method gets called by Parameter.setName().
     * At this point, the Parameter already has its new name, but still needs to be stored
     * under its new name in parameters.
     *
     * @param p       the parameter to rename.
     * @param oldName the old name
     * @param newName the new name.
     */
    /* package private */ void renameParameter(Parameter p, String oldName, String newName) {
        assert (p.getName().equals(newName));
        parameters.remove(oldName);
        parameters.put(newName, p);
    }

    //// Parameter values ////

    public Object getValue(String parameterName) {
        Parameter p = getParameter(parameterName);
        if (p == null) return null;
        return p.getValue();
    }

    public int asInt(String parameterName) {
        Parameter p = getParameter(parameterName);
        if (p.getType() != Parameter.Type.INT) {
            throw new RuntimeException("Parameter " + parameterName + " is not an integer.");
        }
        return p.asInt();
    }

    public float asFloat(String parameterName) {
        Parameter p = getParameter(parameterName);
        if (p.getType() != Parameter.Type.FLOAT && p.getType() != Parameter.Type.INT) {
            throw new RuntimeException("Parameter " + parameterName + " is not a float.");
        }
        return p.asFloat();
    }

    public String asString(String parameterName) {
        Parameter p = getParameter(parameterName);
        // No type checking is performed here. Any parameter type can be converted to a String.
        return p.asString();
    }

    public Color asColor(String parameterName) {
        Parameter p = getParameter(parameterName);
        if (p.getType() != Parameter.Type.COLOR) {
            throw new RuntimeException("Parameter " + parameterName + " is not a color.");
        }
        return p.asColor();
    }

    public NodeCode asCode(String parameterName) {
        Parameter p = getParameter(parameterName);
        if (p.getType() != Parameter.Type.CODE) {
            throw new RuntimeException("Parameter " + parameterName + " is not a string.");
        }
        return p.asCode();
    }

    public void setValue(String parameterName, Object value) throws IllegalArgumentException {
        Parameter p = parameters.get(parameterName);
        if (p == null)
            throw new IllegalArgumentException("Parameter " + parameterName + " does not exist.");
        p.setValue(value);
    }

    /**
     * Sets a parameter value on this node without raising any errors.
     *
     * @param parameterName The parameter name.
     * @param value         The new value.
     * @deprecated Will be removed in NodeBox 2.3. Handles should migrate to their own silentSet() method.
     */
    public void silentSet(String parameterName, Object value) {
        // HACK this method now refers to the current document because otherwise the set will not trigger a network update.
        NodeBoxDocument.getCurrentDocument().silentSet(this, parameterName, value);
    }

    //// Ports ////

    public Port addPort(String name) {
        return addPort(name, Port.Cardinality.SINGLE);
    }

    public Port addPort(String name, Port.Cardinality cardinality) {
        Port p = new Port(this, name, cardinality);
        ports.put(name, p);
        // TODO: Test this removal!
//        if (parent != null) {
//            if (parent.childGraph == null)
//                parent.childGraph = new DependencyGraph<Port, Connection>();
//            parent.childGraph.addDependency(p, outputPort);
//        }
        getLibrary().fireNodeAttributeChanged(this, Attribute.PORT);
        return p;
    }

    public void removePort(String name) {
        throw new UnsupportedOperationException("removePort is not implemented yet.");
        // TODO: Implement, make sure to remove internal dependencies.
        // parent.childGraph.removeDependency(p, outputPort);
    }

    public Port getPort(String name) {
        return ports.get(name);
    }

    public boolean hasPort(String portName) {
        return ports.containsKey(portName);
    }

    public List<Port> getPorts() {
        return new ArrayList<Port>(ports.values());
    }

    public Port getOutputPort() {
        return outputPort;
    }

    /**
     * Get the value of a port.
     * <p/>
     * This only works for ports with single cardinality.
     *
     * @param name the name of the port
     * @return the value of the port
     */
    public Object getPortValue(String name) {
        return ports.get(name).getValue();
    }

    /**
     * Get the values of a port as a list of objects.
     * <p/>
     * This only works for ports with multiple cardinality.
     *
     * @param name the name of the port
     * @return the values of the port
     */
    public List<Object> getPortValues(String name) {
        return ports.get(name).getValues();
    }

    public Object getOutputValue() {
        return outputPort.getValue();
    }

    public void setPortValue(String name, Object value) {
        ports.get(name).setValue(value);
    }

    public void setOutputValue(Object value) {
        outputPort.setValue(value);
    }

    //// Expression shortcuts ////

    public boolean setExpression(String parameterName, String expression) {
        Parameter p = parameters.get(parameterName);
        if (p == null)
            throw new IllegalArgumentException("Parameter " + parameterName + " does not exist.");
        return p.setExpression(expression);
    }

    public void clearExpression(String parameterName) {
        Parameter p = parameters.get(parameterName);
        if (p == null)
            throw new IllegalArgumentException("Parameter " + parameterName + " does not exist.");
        p.clearExpression();
    }

    /**
     * Check if one of my parameters uses a stamp expression.
     * <p/>
     * This method is used to determine if parameters and nodes should be marked as dirty when re-evaluating upstream,
     * which is what happens in the copy node.
     *
     * @return true if one of my parameters uses a stamp expression.
     */
    public boolean hasStampExpression() {
        for (Parameter p : parameters.values()) {
            if (p.hasStampExpression()) return true;
        }
        return false;
    }

    //// Connection shortcuts ////

    /**
     * Check if the child ports can be connected.
     *
     * @param input  the input child port
     * @param output the output child port
     * @return true if the input port can connect to the output port
     */
    public boolean canConnectChildren(Port input, Port output) {
        // TODO: Move implementation from Port here once we move to readonly.
        checkNotNull(input);
        checkNotNull(output);
        return input.canConnectTo(output);
    }

    /**
     * Connect the port on the given (input) child node to the output port of the given (output) child node.
     *
     * @param inputNode  the downstream node
     * @param portName   the downstream (input) port
     * @param outputNode the upstream node
     * @return the Connection object.
     */
    public Connection connectChildren(Node inputNode, String portName, Node outputNode) {
        Port inputPort = inputNode.getPort(portName);
        Port outputPort = outputNode.getOutputPort();
        return connectChildren(inputPort, outputPort);
    }

    /**
     * Connect the downstream input port to the upstream output port.
     * <p/>
     * Both the output and input ports need to be on child nodes of this node.
     * <p/>
     * If the input port was already connected, and its cardinality is single, the connection is broken.
     *
     * @param input  the downstream port
     * @param output the upstream port
     * @return the connection object
     * @throws IllegalArgumentException if the two ports could not be connected
     */
    public Connection connectChildren(Port input, Port output) {
        checkNotNull(input, "The input port cannot be null.");
        checkNotNull(output, "The output port cannot be null.");
        checkState(containsChildPort(input), "The input port is not on a child node of this parent.");
        checkState(containsChildPort(output), "The output port is not on a child node of this parent.");
        checkArgument(input.isInputPort(), "The first argument is not an input port.");
        checkArgument(output.isOutputPort(), "The second argument is not an output port.");
        checkArgument(canConnectChildren(input, output), "The input and output data classes are not compatible.");
        // If ports can have only one connection (cardinality == SINGLE), disconnectChildPort the port first.
        if (input.getCardinality() == Port.Cardinality.SINGLE) {
            disconnectChildPort(input);
        }
        Connection c = new Connection(output, input);
        // Create a new list of connections, and check this list for a cyclic dependency.
        // We create a defensive copy of the original list to make sure we don't need to disconnect
        // if we discover a cycle.
        ArrayList<Connection> newConnections = new ArrayList<Connection>(connections);
        newConnections.add(c);
        CycleDetector detector = new CycleDetector(newConnections);
        // This check will throw an IllegalArgumentException, which is the exception we want.
        checkArgument(!detector.hasCycles(), "Creating this connection would cause a cyclic dependency.");
        connections = newConnections;
        input.getNode().markDirty();
        getLibrary().fireConnectionAdded(this, c);
        return c;
    }

    /**
     * Changes the ordering of output connections by moving the given connection a specified number of positions.
     * <p/>
     * To move the specified connection up one position, set the deltaIndex to -1. To move a connection down, set
     * the deltaIndex to 1.
     * <p/>
     * If the delta index is larger or smaller than the number of positions this connection can move, it will
     * move the connection to the beginning or end. This will not result in an error.
     *
     * @param connection the connection to reorder
     * @param deltaIndex the number of places to move.
     * @return true if changes were made to the ordering.
     */
    public boolean reorderConnection(Connection connection, int deltaIndex) {
        int index = connections.indexOf(connection);
        int newIndex = index + deltaIndex;
        newIndex = Math.max(0, Math.min(connections.size() - 1, newIndex));
        if (index == newIndex) return false;
        connections.remove(connection);
        connections.add(newIndex, connection);
        connection.getInputNode().markDirty();
        return true;
    }

    /**
     * Changes the ordering of output connections by moving the given connection a specified number of positions.
     * <p/>
     * To move the specified connection up one position, set the deltaIndex to -1. To move a connection down, set
     * the deltaIndex to 1.
     * <p/>
     * If the delta index is larger or smaller than the number of positions this connection can move, it will
     * move the connection to the beginning or end. This will not result in an error.
     *
     * @param connection the connection to reorder
     * @param deltaIndex the number of places to move.
     * @param multi      the connection should only be reordered among connections connected to the same input port (with cardinality MULTIPLE).
     * @return true if changes were made to the ordering.
     */
    public boolean reorderConnection(Connection connection, int deltaIndex, boolean multi) {
        if (multi) {
            List<Connection> mConnections = connection.getInput().getConnections();
            int index = mConnections.indexOf(connection);
            int newIndex = index + deltaIndex;
            newIndex = Math.max(0, Math.min(mConnections.size() - 1, newIndex));
            if (index == newIndex) return false;
            connections.removeAll(mConnections);
            mConnections.remove(connection);
            mConnections.add(newIndex, connection);
            connections.addAll(0, mConnections);
            connection.getInputNode().markDirty();
            return true;
        } else
            return reorderConnection(connection, deltaIndex);
    }

    /**
     * Remove all connections to and from the given child node.
     *
     * @param child the child node on this parent
     * @return true if connections were removed
     */
    public boolean disconnectChildren(Node child) {
        boolean removedSomething = false;
        // Disconnect all my inputs.
        for (Port p : child.getPorts()) {
            // Due to lazy evaluation, removedSomething needs to be at the end.
            removedSomething = disconnectChildPort(p) | removedSomething;
        }
        // Disconnect all my outputs.
        removedSomething = disconnectChildPort(child.outputPort) | removedSomething;
        return removedSomething;
    }

    /**
     * Remove all connections to and from this node.
     *
     * @return true if connections were removed.
     */
    public boolean disconnect() {
        if (!hasParent()) return false;
        return parent.disconnectChildren(this);
    }

    public void disconnect(Connection c) {
        checkNotNull(c);
        checkArgument(connections.contains(c), "Connection %s is not one of my connections.", c);
        connections.remove(c);
        Port input = c.getInput();
        input.reset();
        input.getNode().markDirty();
        getLibrary().fireConnectionRemoved(this, c);
    }

    /**
     * Removes all connection from the given (input or output) child port.
     *
     * @param port the (input or output) port on the child node.
     * @return true if a connection was removed.
     */
    public boolean disconnectChildPort(Port port) {
        checkNotNull(port, "Port cannot be null.");
        checkArgument(containsChildPort(port), "Port %s is not on a child node of this parent.", port);
        List<Connection> connectionsToRemove = new ArrayList<Connection>();
        for (Connection c : connections) {
            if (port == c.getInput() || port == c.getOutput()) {
                port.reset();
                // This port was changed. Mark the node as dirty.
                port.getNode().markDirty();
                getLibrary().fireConnectionRemoved(this, c);
                connectionsToRemove.add(c);
            }
        }
        if (connectionsToRemove.isEmpty()) return false;
        for (Connection c : connectionsToRemove) {
            connections.remove(c);
        }
        return true;
    }

    /**
     * Removes the connection between the output port of the given node and the input port.
     *
     * @param input      the input port
     * @param outputNode the output node
     * @return true if a connection was found and removed.
     */
    public boolean disconnectChildPort(Port input, Node outputNode) {
        checkNotNull(input, "The input port cannot be null.");
        checkNotNull(outputNode, "The output node cannot be null.");
        checkArgument(containsChildPort(input), "Port %s is not on a child node of this parent.", input);
        checkArgument(containsChildNode(outputNode), "Node %s is not a child of this parent.", outputNode);
        checkArgument(input.isInputPort(), "The given port is not an input.");
        Connection toRemove = null;
        for (Connection c : connections) {
            if (input == c.getInput() && outputNode == c.getOutputNode()) {
                toRemove = c;
                break;
            }
        }
        if (toRemove == null) return false;
        connections.remove(toRemove);
        input.reset();
        // This port was changed. Mark the node as dirty.
        input.getNode().markDirty();
        getLibrary().fireConnectionRemoved(this, toRemove);
        return true;
    }

    /**
     * Get a list of all parameters on this Node that can be connected to the given output node.
     *
     * @param outputNode the output (upstream) node
     * @return a list of parameters.
     */
    public List<Port> getCompatibleInputs(Node outputNode) {
        List<Port> compatiblePorts = new ArrayList<Port>();
        for (Port p : getPorts()) {
            if (p.canConnectTo(outputNode))
                compatiblePorts.add(p);
        }
        return compatiblePorts;
    }

    /**
     * Get a set of all connection objects.
     *
     * @return a set of Connections objects. This list should not be modified.
     */
    public List<Connection> getConnections() {
        return connections;
    }

    /**
     * Checks if this node is connected.
     * <p/>
     * This method checks both input and output connections.
     *
     * @return true if this node is connected.
     */
    public boolean isConnected() {
        if (!hasParent()) return false;
        return getParent().isChildConnected(this);
    }

    /**
     * Check if the given child node is connected
     *
     * @param node the child node to check
     * @return true if the child is connected
     */
    public boolean isChildConnected(Node node) {
        if (node == null) return false;
        checkArgument(containsChildNode(node), "Node %s is not a child of this parent.", node);
        for (Connection c : connections) {
            if (node == c.getOutputNode() || node == c.getInputNode()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given child port is connected.
     *
     * @param port a port on a child of this node.
     * @return true if the port is connected.
     */
    public boolean isChildConnected(Port port) {
        checkNotNull(port);
        checkArgument(containsChildPort(port), "Port %s is not on a child node of this parent.", port);
        for (Connection c : connections) {
            if (port == c.getOutput() || port == c.getInput()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the two child ports are connected together.
     * Both of these ports need to be on children of this node.
     *
     * @param port1 input or output port
     * @param port2 input or output port
     * @return true if the two ports are connected.
     * @throws IllegalArgumentException if neither of the ports are on this node.
     */
    public boolean isChildConnectedTo(Port port1, Port port2) throws IllegalArgumentException {
        // The order of the ports is unimportant, but one needs to be
        // an input and the other an output. If the two ports have
        // the same direction, they can never be connected.
        if (port1.getDirection() == port2.getDirection()) return false;
        checkArgument(containsChildPort(port1), "Port %s is not on a child node of this parent.", port1);
        checkArgument(containsChildPort(port2), "Port %s is not on a child node of this parent.", port2);
        Port output = port1.isOutputPort() ? port1 : port2;
        Port input = port1.isInputPort() ? port1 : port2;
        for (Connection c : connections) {
            if (output == c.getOutput() || input == c.getInput()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this node is connected to the given node.
     * <p/>
     * Both input and output connections are checked.
     * Only connections are checked, not parameter dependencies.
     *
     * @param other the other node.
     * @return true if the two nodes are connected.
     */
    public boolean isConnectedTo(Node other) {
        if (other == null) return false;
        if (other == this) return false;
        if (this.isOutputConnectedTo(other)) {
            return true;
        } else if (other.isOutputConnectedTo(this)) {
            return true;
        }
        return false;
    }

    /**
     * Check if one of the input ports are connected to the output port of the given node.
     *
     * @param outputNode the node whose output will be checked
     * @return true if this node's output is connected to the given node.
     */
    public boolean isInputConnectedTo(Node outputNode) {
        return outputNode.isOutputConnectedTo(this);
    }

    /**
     * Check if the output port is connected.
     *
     * @return true if the output port is connected.
     */
    public boolean isOutputConnected() {
        return outputPort.isConnected();
    }

    /**
     * Check if the output port is connected to one of the inputs of the given node.
     *
     * @param inputNode the node whose inputs will be checked
     * @return true if this node's output is connected to the given node.
     */
    public boolean isOutputConnectedTo(Node inputNode) {
        checkNotNull(inputNode);
        if (!inputNode.hasParent()) return false;
        if (!hasParent()) return false;
        return getParent().areChildrenConnected(this, inputNode);
    }

    /**
     * Check if the output port is connected to the given input port.
     *
     * @param input the input port
     * @return true if this node's output port is connected to the given input port.
     */
    public boolean isOutputConnectedTo(Port input) {
        checkNotNull(input);
        checkArgument(input.isInputPort(), "Port %s is not an input.", input);
        if (!input.hasParentNode()) return false;
        return input.getParentNode().isChildConnectedTo(input, this);
    }

    /**
     * Check if the given child nodes are connected to eachother.
     *
     * @param output the output child node
     * @param input  the input child node
     * @return true if they are connected.
     */
    public boolean areChildrenConnected(Node output, Node input) {
        checkNotNull(output);
        checkNotNull(input);
        if (!containsChildNode(output)) return false;
        if (!containsChildNode(input)) return false;
        for (Connection c : connections) {
            if (output == c.getOutputNode() && input == c.getInputNode()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Check if the given output node is connected to the given input.
     *
     * @param input  the input port
     * @param output the output node
     * @return true if they are connected.
     */
    public boolean isChildConnectedTo(Port input, Node output) {
        checkNotNull(input);
        checkNotNull(output);
        if (!containsChildPort(input)) return false;
        if (!containsChildNode(output)) return false;
        for (Connection c : connections) {
            if (input == c.getInput() && output == c.getOutputNode()) {
                return true;
            }
        }
        return false;
    }

    //// Dirty handling ////

    public void markDirty() {
        if (dirty)
            return;
        dirty = true;
        if (hasParent()) {
            parent.markChildDirty(this);
            if (!parent.isDirty()) {
                // Only changes to the rendered node should make the parent dirty.
                // TODO: Check for corner cases.
                if (parent.getRenderedChild() == this) {
                    parent.markDirty();
                }
            }
        }
        getLibrary().fireNodeDirty(this);
    }

    private void markChildDirty(Node node) {
        checkNotNull(node);
        for (Connection c : connections) {
            if (node == c.getOutputNode()) {
                c.getInputNode().markDirty();
            }
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    /**
     * Update all upstream nodes with stamp expressions.
     *
     * @param ctx the processing context
     */
    public void stampExpressions(ProcessingContext ctx) {
        stampDirty();
        updateDependencies(ctx);
    }

    /**
     * Mark all upstream nodes that have stamp expressions dirty.
     * <p/>
     * This method is used for the copy node, where nodes that have parameters with stamp expressions should
     * be marked dirty so the expressions can re-evaluate based on new stamp key/values set in the processing
     * context.
     */
    public void stampDirty() {
        if (!hasParent()) return;
        getParent().stampChildDirty(this, false);
    }

    /**
     * Mark all upstream nodes that have stamp expressions dirty, recursive.
     * This method does the actual upstream marking.
     *
     * @param node     the child node to stamp
     * @param upstream if true, we're beyond the first node and can start marking parameters dirty.
     */
    private void stampChildDirty(Node node, boolean upstream) {
        checkNotNull(node);
        checkArgument(containsChildNode(node));
        for (Connection c : connections) {
            if (node == c.getInputNode()) {
                stampChildDirty(c.getOutputNode(), true);
            }
        }
        if (upstream) {
            for (Parameter p : node.getParameters()) {
                if (p.hasStampExpression())
                    p.markDirty();

            }
        }
    }

    //// Processing ////

    /**
     * Updates the node by processing all required dependencies.
     * <p/>
     * This method will process only dirty nodes.
     * This operation can take a long time, and should be run in a separate thread.
     *
     * @throws nodebox.node.ProcessingError when an error happened during procesing.
     */
    public void update() throws ProcessingError {
        update(new ProcessingContext(this));
    }

    /**
     * Updates the node by processing all required dependencies.
     * <p/>
     * This method will process only dirty nodes.
     * This operation can take a long time, and should be run in a separate thread.
     *
     * @param ctx meta-information about the processing operation.
     * @throws nodebox.node.ProcessingError when an error happened during procesing.
     */
    public void update(ProcessingContext ctx) throws ProcessingError {
        if (!dirty) return;
        // Set the current context global. 
        ProcessingContext.setCurrentContext(ctx);
        // Set the current node as the one being processed.
        ctx.setNode(this);
        // Update the dependencies.
        // This might cause an exception which we don't catch, instead letting it boil up.
        updateDependencies(ctx);
        // All dependencies are up-to-date. Process the node.
        ProcessingError pe = null;
        try {
            process(ctx);
        } catch (ProcessingError e) {
            pe = e;
        }
        // Even if an error occurred the node is still marked as clean, and events are fired.
        // Only after these steps is the error thrown.
        // It is important to mark the node as clean so that subsequent changes to the node mark it as dirty,
        // triggering an event. This allows you to fix the cause of the error in the node.
        dirty = false;
        getLibrary().fireNodeUpdated(this, ctx);
        // If exception occurs, throw it.
        if (pe != null)
            throw pe;
    }

    /**
     * Update all dependencies of this node.
     * <p/>
     * This will update both the ports and parameters.
     *
     * @param ctx meta-information about the processing operation.
     * @throws nodebox.node.ProcessingError when an error happened while updating the dependencies.
     */
    public void updateDependencies(ProcessingContext ctx) throws ProcessingError {
        // Update the ports
        try {
            updatePorts(ctx);
        } catch (ProcessingError e) {
            // If an error occurs while updating the ports, this node will fail as well.
            // The error is not saved in this node since it occurred on a dependency.
            // This makes it easier to track down the error.
            dirty = false;
            outputPort.setValue(null);
            throw e;
        }
        // Update the parameters
        try {
            updateParameters(ctx);
        } catch (ProcessingError e) {
            // If an error occurs while updating the parameters, this node will fail as well.
            // It also sets the error flag on this node since there might be a problem with the expression.
            error = e;
            dirty = false;
            outputPort.setValue(null);
            throw e;
        }
    }

    /**
     * Update the parameters of this node.
     * <p/>
     * This causes all expressions to be evaluated and dependencies to be resolved this way.
     *
     * @param ctx meta-information about the processing operation.
     * @throws nodebox.node.ProcessingError when an error happened during processing.
     */
    private void updateParameters(ProcessingContext ctx) throws ProcessingError {
        // Update all parameter expressions.
        for (Parameter param : parameters.values()) {
            try {
                param.update(ctx);
            } catch (Exception e) {
                throw new ProcessingError(this, "Error occurred while updating parameter " + param + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Update everything this node depends on.
     * <p/>
     * This method will update all upstream node and all parameter expressions.
     *
     * @param ctx meta-information about the processing operation.
     * @throws nodebox.node.ProcessingError when an error happened during processing.
     */
    private void updatePorts(ProcessingContext ctx) throws ProcessingError {
        // Update all upstream nodes.
        if (!hasParent()) return;
        for (Port port : ports.values()) {
            port.reset();
            parent.updateChildPort(port, ctx);
        }
    }

    /**
     * Update all dependencies on the child port.
     *
     * @param port a child port
     * @param ctx  the processing context
     * @throws ProcessingError if an error happens during processing.
     */
    private void updateChildPort(Port port, ProcessingContext ctx) throws ProcessingError {
        for (Connection c : connections) {
            if (port == c.getInput()) {
                Node outputNode = c.getOutputNode();
                outputNode.update(ctx);
                // TODO: This does not work for multi-connections, where we should use input.addValue().
                // Maybe the first time we encounter the input port we can call reset, and use addValue
                // all the time.
                if (port.getCardinality() == Port.Cardinality.SINGLE) {
                    port.setValue(outputNode.getOutputValue());
                } else {
                    port.addValue(outputNode.getOutputValue());
                }
            }
        }
    }

    /**
     * This method does the actual functionality of the node.
     *
     * @param ctx meta-information about the processing operation.
     * @throws nodebox.node.ProcessingError when an error happened during procesing.
     */
    public void process(ProcessingContext ctx) throws ProcessingError {
        try {
            NodeCode code = asCode("_code");
            Object returnValue = code.cook(this, ctx);
            outputPort.setValue(returnValue);
            error = null;
        } catch (ProcessingError e) {
            error = e;
            outputPort.setValue(null);
            throw e;
        } catch (Exception e) {
            error = e;
            outputPort.setValue(null);
            throw new ProcessingError(this, e);
        }
    }

    /**
     * This is the default cook implementation of the node.
     * <p/>
     * If this node has children, it will look up the rendered child and update it. The return value will be the
     * return value of the rendered child.
     * <p/>
     * If the node doesn't have children, this method returns null.
     *
     * @param node    The node to process.
     * @param context The processing context.
     * @return The return value of the rendered child or null if the node doesn't have children.
     * @throws ProcessingError if the update of the child node failed.
     */
    public Object cook(Node node, ProcessingContext context) throws ProcessingError {
        if (!node.hasChildren())
            return null;
        Node renderedChild = node.getRenderedChild();
        if (renderedChild != null) {
            renderedChild.update(context);
            return renderedChild.getOutputValue();
        } else {
            return null;
        }
    }

    /**
     * Get the source code for the root node.
     *
     * @return an empty string.
     */
    public String getSource() {
        return "def cook(self):\n    return None";
    }

    /**
     * Get the code type for the root node.
     *
     * @return "java"
     */
    public String getType() {
        return "java";
    }

    /**
     * Checks if an error occurred during the last update of this node.
     *
     * @return true if this node is in an error state.
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Get the error that occurred during the last update.
     *
     * @return the error or null if no error occurred.
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Default operation. As the name implies, does nothing.
     *
     * @param n   the node to work on
     * @param ctx the processing context
     * @return the return value. In this case, null.
     */
    public static Object doNothing(Node n, ProcessingContext ctx) {
        return null;
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
     */
    public Handle createHandle() {
        NodeCode handleCode = asCode("_handle");
        if (handleCode == null) return null;
        // TODO: Do we need the ProcessingContext in the handle or can we pass null?
        Object handleObj = handleCode.cook(this, new ProcessingContext(this));
        if (handleObj == null) return null;
        if (!(handleObj instanceof Handle))
            throw new AssertionError("Handle code for node " + getName() + " does not return Handle object.");
        return (Handle) handleObj;
    }

    /**
     * Check if the node has a handle parameter and if it is enabled.
     *
     * @return true if the node has an enabled handle.
     */
    public boolean hasEnabledHandle() {
        Parameter handleParameter = getParameter("_handle");
        return handleParameter != null && handleParameter.isEnabled();
    }


    //// Cloning ////

    /**
     * Creates a new instance with this node as the prototype.
     *
     * @param library the namespace of the new node.
     * @param name    the name of the new node.
     * @return a new Node with this node as the prototype.
     */
    public Node newInstance(NodeLibrary library, String name) {
        return newInstance(library, name, getDataClass());
    }

    /**
     * Creates a new instance with this node as the prototype.
     * <p/>
     * When a new instance is created, all parameters and ports are copied to the new instance.
     * That means that changes to the prototype don't automatically propagate to the children.
     * If you want to get the changes from the prototype, you would need to do a revertToPrototype
     * operation on the parameter.
     * <p/>
     * Connections are not cloned. If you want to this, you'll need to copy a node.
     *
     * @param library   the namespace of the new node.
     * @param name      the name of the new node.
     * @param dataClass the type of data from the output port. If null, the dataClass will be inherited.
     * @return a new Node with this node as the prototype.
     */
    public Node newInstance(NodeLibrary library, String name, Class dataClass) {
        Node n = rawInstance(library, name, dataClass);
        library.add(n);
        return n;
    }

    /**
     * Create a raw instance. The node's parent is not set, and it is not added to the library.
     *
     * @param library   the namespace of the new node.
     * @param name      the name of the new node.
     * @param dataClass the type of data from the output port. If null, the dataClass will be inherited.
     * @return a new Node with this node as the prototype.
     * @see #newInstance
     */
    private Node rawInstance(NodeLibrary library, String name, Class dataClass) {
        if (library == null) throw new IllegalArgumentException("Library parameter cannot be null.");
        if (dataClass == null) dataClass = getDataClass();
        Node n = new Node(library, name, dataClass);
        n.prototype = this;
        n.dirty = true;
        // Clone all parameters.
        for (Parameter p : parameters.values()) {
            n.parameters.put(p.getName(), p.clone(n));
        }
        // Clone all ports.
        for (Port p : ports.values()) {
            n.ports.put(p.getName(), p.clone(n));
        }
        // Copy all children.
        copyChildren(n);
        return n;
    }

    /**
     * Copy this node.
     * <p/>
     * Connection are not copied. Use copyChildren on the parent for that.
     *
     * @param newParent the node that will be the parent of the newly cloned node.
     * @return a copy of the node with copies to all of its upstream connections.
     */
    public Node copy(Node newParent) {
        String name;
        if (newParent.containsChildNode(getName())) {
            name = newParent.uniqueName(getName());
        } else {
            name = getName();
        }
        Node newNode = newParent.create(getPrototype(), name, getDataClass());
        // Set position.
        if (parent == newParent) {
            newNode.setX(getX() + 20);
            newNode.setY(getY() + 80);
        } else {
            newNode.setX(getX());
            newNode.setY(getY());
        }
        // Copy all parameters.
        for (Parameter p : parameters.values()) {
            newNode.parameters.remove(p.getName());
            newNode.parameters.put(p.getName(), p.copyWithUpstream(newNode));
        }
        // Copy all ports.
        for (Port p : ports.values()) {
            newNode.ports.remove(p.getName());
            newNode.ports.put(p.getName(), p.copy(newNode));
        }
        copyChildren(newNode);
        return newNode;
    }

    /**
     * Copy all of my children to the new parent.
     *
     * @param newParent the new parent to copy the children under.
     * @return a new collection of children.
     */

    public Collection<Node> copyChildren(Node newParent) {
        // Copy children.
        Collection<Node> newChildren = copyChildren(children.values(), newParent);
        // Copy rendered child.
        if (getRenderedChild() != null) {
            newParent.setRenderedChild(newParent.getChild(getRenderedChild().getName()));
        }
        return newChildren;
    }

    /**
     * Copy all of the given children under the new parent.
     * <p/>
     * The children need to be contained in this node.
     *
     * @param children  a list of my children to copy. They need to be direct children of this node.
     * @param newParent the new parent to copy the children under.
     * @return a new collection of children.
     */
    public Collection<Node> copyChildren(Collection<Node> children, Node newParent) {
        HashMap<Node, Node> copyMap = new HashMap<Node, Node>(children.size());
        for (Node n : children) {
            if (!containsChildNode(n)) {
                throw new IllegalArgumentException("The given node is not a child of this parent: " + n + " parent: " + this);
            }
            Node newNode = n.copy(newParent);
            copyMap.put(n, newNode);
        }
        for (Node n : children) {
            Node newNode = copyMap.get(n);
            assert newNode != null;
            for (Port p : newNode.getPorts()) {
                Port oldPort = n.getPort(p.getName());
                oldPort.cloneConnection(p, copyMap);
            }
        }
        return copyMap.values();
    }

    /**
     * Copy one child of this node under the new parent.
     * <p/>
     * The child needs to be a direct descendant of this node (so no grandchildren).
     *
     * @param child     a child to copy. This child needs to be a direct child of this node.
     * @param newParent the new parent to copy the child under.
     * @return the new child.
     */
    public Node copyChild(Node child, Node newParent) {
        ArrayList<Node> children = new ArrayList<Node>(1);
        children.add(child);
        Collection<Node> newChildren = copyChildren(children, newParent);
        assert newChildren.size() == 1;
        return newChildren.iterator().next();
    }

    //// Output ////

    @Override
    public String toString() {
        if (prototype == null) {
            return String.format(Locale.US, "<Node %s>", getIdentifier());
        } else {
            return String.format(Locale.US, "<Node %s (%s)>", getIdentifier(), prototype.getIdentifier());
        }
    }
}
