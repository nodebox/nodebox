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

import nodebox.graphics.Color;
import nodebox.graphics.Point;
import nodebox.handle.Handle;

import javax.swing.event.EventListenerList;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class Node implements NodeCode, NodeAttributeListener {

    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,29}$");
    private static final Pattern DOUBLE_UNDERSCORE_PATTERN = Pattern.compile("^__.*$");
    private static final Pattern RESERVED_WORD_PATTERN = Pattern.compile("^(node|network|root|context)$");
    private static final Pattern NUMBER_AT_THE_END = Pattern.compile("^(.*?)(\\d*)$");

    public static final String IMAGE_GENERIC = "__generic";

    public static final String OUTPUT_PORT_NAME = "output";

    public static final Node ROOT_NODE;

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
     * <p/>
     * This data structure is created on-demand, and will be null by default.
     */
    private DependencyGraph<Port, Connection> childGraph;

    /**
     * All connections linked to the output port of this node.
     */
    //private List<Connection> downstreams = new ArrayList<Connection>();

    /**
     * All connections keyed by the input and going upstream to the output.
     * Key = input port
     * Value = a connection to the output node.
     */
    //private HashMap<Port, Connection> upstreams = new HashMap<Port, Connection>();

    /**
     * The processing error. Null if no error occurred during processing.
     */
    private Throwable error;

    /**
     * Listeners list for all events that occur on this class.
     */
    private EventListenerList listenerList = new EventListenerList();

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
        this.name = name;
        fireNodeAttributeChanged(Attribute.NAME);
    }

    public NodeLibrary getLibrary() {
        return library;
    }

    public void setLibrary(NodeLibrary library) {
        this.library.remove(getName());
        this.library = library;
        this.library.add(this);
        fireNodeAttributeChanged(Attribute.LIBRARY);
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
        fireNodeAttributeChanged(Attribute.DESCRIPTION);
    }

    public String getImage() {
        return asString("_image");
    }

    public void setImage(String image) {
        setValue("_image", image);
        fireNodeAttributeChanged(Attribute.IMAGE);
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
        if (parent != null && parent.contains(this)) return;
        if (parent != null && parent.contains(name))
            throw new InvalidNameException(this, name, "There is already a node named \"" + name + "\" in " + parent);
        // Since this node will reside under a different parent, it can no longer maintain connections within
        // the previous parent. Break all connections. We need to do this before the parent changes.
        disconnect();
        if (this.parent != null)
            this.parent.remove(this);
        this.parent = parent;
        if (parent != null) {
            parent.children.put(name, this);
            addNodeAttributeListener(parent);
            for (Port p : ports.values()) {
                if (parent.childGraph == null)
                    parent.childGraph = new DependencyGraph<Port, Connection>();
                parent.childGraph.addDependency(p, outputPort);
            }
            // We're on the child node, so we need to fire the child added event
            // on the parent with this child as the argument.
            parent.fireChildAdded(this);
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
//        if (contains(node.getName())) {
//            throw new InvalidNameException(this, node.getName(), "There is already a node named \"" + node.getName() + "\" in network " + getAbsolutePath());
//        }
//        node.parent = this;
//        node.addNodeAttributeListener(this);
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
        if (!contains(node))
            return false;
        node.markDirty();
        node.disconnect();
        node.parent = null;
        children.remove(node.getName());
        if (node == renderedChild) {
            setRenderedChild(null);
        }
        node.removeNodeAttributeListener(this);
        fireChildRemoved(node);
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
            if (!contains(suggestedName)) {
                // We don't use rename here, since it assumes the node will be in
                // this network.
                return suggestedName;
            }
            ++counter;
        }
    }

    public boolean contains(String nodeName) {
        return children.containsKey(nodeName);
    }

    public boolean contains(Node node) {
        return children.containsValue(node);
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

    /**
     * Whenever the name of a child node changes, this event gets called.
     * Make sure the child node is still stored under the correct name.
     *
     * @param source    the Node this event comes from
     * @param attribute the changed attribute
     */
    public void attributeChanged(Node source, Attribute attribute) {
        // We only need to react to name changes.
        if (attribute != Attribute.NAME) return;
        // Check if the node exists and remove it in one operation.
        // If remove() returns true, the given node is not a child
        // and we should not store it.
        if (!children.values().remove(source)) return;
        children.put(source.getName(), source);
    }

    //// Rendered ////

    public Node getRenderedChild() {
        return renderedChild;
    }

    public void setRenderedChild(Node renderedChild) {
        if (renderedChild != null && !contains(renderedChild)) {
            throw new NotFoundException(this, renderedChild.getName(), "Node '" + renderedChild.getAbsolutePath() + "' is not in this network (" + getAbsolutePath() + ")");
        }
        if (this.renderedChild == renderedChild) return;
        this.renderedChild = renderedChild;
        markDirty();
        fireRenderedChildChanged(renderedChild);
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
        StringBuffer name = new StringBuffer("/");
        Node parent = getParent();
        while (parent != null) {
            name.insert(1, parent.getName() + "/");
            parent = parent.getParent();
        }
        name.append(getName());
        return name.toString();
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
        fireNodeAttributeChanged(Attribute.POSITION);
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        fireNodeAttributeChanged(Attribute.POSITION);
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
        fireNodeAttributeChanged(Attribute.POSITION);
    }

    //// Export flag ////


    public boolean isExported() {
        return exported;
    }

    public void setExported(boolean exported) {
        this.exported = exported;
        fireNodeAttributeChanged(Attribute.EXPORT);
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
        fireNodeAttributeChanged(Attribute.PARAMETER);
        return p;
    }

    public Parameter addParameter(String name, Parameter.Type type, Object value) {
        Parameter p = addParameter(name, type);
        p.setValue(value);
        fireNodeAttributeChanged(Attribute.PARAMETER);
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
        fireNodeAttributeChanged(Attribute.PARAMETER);
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

    public void silentSet(String parameterName, Object value) {
        try {
            setValue(parameterName, value);
        } catch (Exception ignored) {
        }
    }

    //// Ports ////

    public Port addPort(String name) {
        return addPort(name, Port.Cardinality.SINGLE);
    }

    public Port addPort(String name, Port.Cardinality cardinality) {
        Port p = new Port(this, name, cardinality);
        ports.put(name, p);
        if (parent != null) {
            if (parent.childGraph == null)
                parent.childGraph = new DependencyGraph<Port, Connection>();
            parent.childGraph.addDependency(p, outputPort);
        }
        fireNodeAttributeChanged(Attribute.PORT);
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

    //// Event handling ////

    /**
     * Add a listener that responds when this node is marked dirty.
     *
     * @param l the listener
     */
    public void addDirtyListener(DirtyListener l) {
        listenerList.add(DirtyListener.class, l);
    }

    /**
     * Remove a dirty listener.
     *
     * @param l the listener
     */
    public void removeDirtyListener(DirtyListener l) {
        listenerList.remove(DirtyListener.class, l);
    }

    /**
     * Invoked when the node is marked dirty.
     */
    public void fireNodeDirty() {
        // Some event listeners remove themselves from the node as a
        // result of handling the event. This modifies the listener list,
        // and can cause some listeners to be skipped.
        // By counting backwards, listeners can remove themselves
        // without causing problems.
        // This technique was adapted from Swing Hacks.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == DirtyListener.class) {
                ((DirtyListener) listeners[i + 1]).nodeDirty(this);
            }
        }
    }

    /**
     * Invoked when the node is updated.
     *
     * @param context the processing context.
     */
    public void fireNodeUpdated(ProcessingContext context) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == DirtyListener.class) {
                ((DirtyListener) listeners[i + 1]).nodeUpdated(this, context);
            }
        }
    }

    /**
     * Add a listener that responds to changes in the node's metadata.
     *
     * @param l the listener
     */
    public void addNodeAttributeListener(NodeAttributeListener l) {
        listenerList.add(NodeAttributeListener.class, l);
    }

    /**
     * Remove a node attribute listener.
     *
     * @param l the listener
     */
    public void removeNodeAttributeListener(NodeAttributeListener l) {
        listenerList.remove(NodeAttributeListener.class, l);
    }

    /**
     * Invoked when an attribute on the node was changed.
     * <p/>
     * Possible attributes are name, namespace, description, x, y.
     *
     * @param attribute the changed attribute
     */
    public void fireNodeAttributeChanged(Attribute attribute) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeAttributeListener.class) {
                ((NodeAttributeListener) listeners[i + 1]).attributeChanged(this, attribute);
            }
        }
        if (hasParent())
            getParent().fireChildAttributeChanged(this, attribute);
    }


    /**
     * Add a listener that responds to changes in the node's metadata.
     *
     * @param l the listener
     */
    public void addNodeChildListener(NodeChildListener l) {
        listenerList.add(NodeChildListener.class, l);
    }

    /**
     * Remove a node attribute listener.
     *
     * @param l the listener
     */
    public void removeNodeChildListener(NodeChildListener l) {
        listenerList.remove(NodeChildListener.class, l);
    }

    /**
     * Invoked when a child was added to this node.
     *
     * @param child the child node
     */
    public void fireChildAdded(Node child) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeChildListener.class) {
                ((NodeChildListener) listeners[i + 1]).childAdded(this, child);
            }
        }
    }

    /**
     * Invoked when a child was removed from this node.
     *
     * @param child the child node
     */
    public void fireChildRemoved(Node child) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeChildListener.class) {
                ((NodeChildListener) listeners[i + 1]).childRemoved(this, child);
            }
        }
    }

    /**
     * Invoked when a connection was added to this node.
     *
     * @param c the connection
     */
    public void fireConnectionAdded(Connection c) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeChildListener.class) {
                ((NodeChildListener) listeners[i + 1]).connectionAdded(this, c);
            }
        }
    }

    /**
     * Invoked when a connection was removed from this node.
     *
     * @param c the connection
     */
    public void fireConnectionRemoved(Connection c) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeChildListener.class) {
                ((NodeChildListener) listeners[i + 1]).connectionRemoved(this, c);
            }
        }
    }

    /**
     * Invoked when the rendered child was changed.
     *
     * @param child the child node
     */
    public void fireRenderedChildChanged(Node child) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeChildListener.class) {
                ((NodeChildListener) listeners[i + 1]).renderedChildChanged(this, child);
            }
        }
    }

    /**
     * Invoked when an attribute on the child was changed.
     *
     * @param child     the child node
     * @param attribute the changed attribute
     * @see nodebox.node.NodeAttributeListener.Attribute
     */
    public void fireChildAttributeChanged(Node child, Attribute attribute) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NodeChildListener.class) {
                ((NodeChildListener) listeners[i + 1]).childAttributeChanged(this, child, attribute);
            }
        }
    }

    /**
     * Add a listener that responds to changes in the value of a parameter.
     *
     * @param l the listener
     */
    public void addParameterValueListener(ParameterValueListener l) {
        listenerList.add(ParameterValueListener.class, l);
    }

    /**
     * Remove a parameter value listener.
     *
     * @param l the listener
     */
    public void removeParameterValueListener(ParameterValueListener l) {
        listenerList.remove(ParameterValueListener.class, l);
    }

    /**
     * Invoked when the value of the parameter was changed.
     *
     * @param source a Parameter
     */
    public void fireParameterValueChanged(Parameter source) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ParameterValueListener.class) {
                ((ParameterValueListener) listeners[i + 1]).valueChanged(source);
            }
        }
    }

    /**
     * Add a listener that responds to changes in the metadata of a parameter.
     *
     * @param l the listener
     */
    public void addParameterAttributeListener(ParameterAttributeListener l) {
        listenerList.add(ParameterAttributeListener.class, l);
    }

    public void removeParameterAttributeListener(ParameterAttributeListener l) {
        listenerList.remove(ParameterAttributeListener.class, l);
    }

    /**
     * Invoked when an attribute on the parameter was changed.
     *
     * @param source a Parameter
     */
    public void fireParameterAttributeChanged(Parameter source) {
        // See comment in #fireNodeDirty.
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ParameterAttributeListener.class) {
                ((ParameterAttributeListener) listeners[i + 1]).attributeChanged(source);
            }
        }
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
     * Connect the port on the given (input) node to the output port of the given (output) node.
     *
     * @param inputNode  the downstream node
     * @param portName   the downstream (input) port
     * @param outputNode the upstream node
     * @return the Connection object.
     */
    public Connection connect(Node inputNode, String portName, Node outputNode) {
        Port outputPort = outputNode.getOutputPort();
        Port inputPort = inputNode.getPort(portName);
        return connect(inputPort, outputPort);
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
     * @return the connection object.
     */
    public Connection connect(Port input, Port output) {
        // Sanity checks
        if (output == null || input == null)
            throw new IllegalArgumentException("The output and input ports cannot be null.");
        if (output.getParentNode() == null)
            throw new IllegalArgumentException("The output node does not have a parent.");
        if (input.getParentNode() == null)
            throw new IllegalArgumentException("The input node does not have a parent.");
        if (output.getParentNode() != input.getParentNode())
            throw new IllegalArgumentException("The output and input nodes do not have the same parent.");
        if (!output.isOutputPort())
            throw new IllegalArgumentException("The first argument is not an output port.");
        if (!input.isInputPort())
            throw new IllegalArgumentException("The second argument is not an input port.");
        if (!input.canConnectTo(output))
            throw new IllegalArgumentException("The input and output data classes are not compatible.");
        // The child graph is lazily created.
        if (parent.childGraph == null)
            parent.childGraph = new DependencyGraph<Port, Connection>();
        // If ports can have only one connection (cardinality == SINGLE), disconnect the port first.
        Connection c;
        if (input.getCardinality() == Port.Cardinality.SINGLE) {
            disconnect(input);
            c = new Connection(output, input);
        } else {
            // Ports with multiple cardinality will add output ports to an existing connection.
            // See if we can find an existing connection and add a port, otherwise create a new connection.
            c = parent.childGraph.getInfo(input);
            if (c == null) {
                c = new Connection(output, input);
            } else {
                c.addOutput(output);
            }
        }
        // It could be that the connection is already in there (for existing multi-connections),
        // but replacing it with itself doesn't hurt.
        parent.childGraph.addDependency(output, input, c);
        input.getNode().markDirty();
        parent.fireConnectionAdded(c);
        return c;
    }

    /**
     * Remove all connections to and from this node.
     *
     * @return true if connections were removed.
     */
    public boolean disconnect() {
        if (parent == null) return false;
        DependencyGraph<Port, Connection> dg = parent.childGraph;
        if (dg == null) return false;
        boolean removedSomething = false;
        // Disconnect all my inputs.
        for (Port p : getPorts()) {
            // Due to lazy evaluation, removedSomething needs to be at the end.
            removedSomething = disconnect(p) | removedSomething;
        }
        // Disconnect all my outputs.
        removedSomething = disconnect(outputPort) | removedSomething;
        return removedSomething;
    }

    /**
     * Removes all connection from the given (input or output) port.
     *
     * @param port the (input or output) port on this node.
     * @return true if a connection was removed.
     */
    public boolean disconnect(Port port) {
        if (port == null)
            throw new IllegalArgumentException("The input port cannot be null.");
        if (port.getNode() != this)
            throw new IllegalArgumentException("The input port is not on this node.");
        Node parent = port.getNode().getParent();
        if (parent == null) return false;
        DependencyGraph<Port, Connection> dg = parent.childGraph;
        if (dg == null) return false;
        if (!port.isConnected()) return false;
        if (port.isInputPort()) {
            boolean removedSomething = dg.removeDependencies(port);
            Connection c = dg.getInfo(port);
            dg.removeInfo(port);
            if (removedSomething) {
                port.reset();
                // This port was changed. Mark the node as dirty.
                port.getNode().markDirty();
                parent.fireConnectionRemoved(c);
            }
            return removedSomething;
        } else { // Output port
            boolean removedSomething = false;
            // Remove internal connections.
            dg.removeDependencies(port);
            for (Port p : dg.getDependents(port)) {
                Connection c = dg.getInfo(p);
                c.removeOutput(port);
                // If this output was the last output in the connection,
                // remove the connection.
                if (!c.hasOutputs())
                    dg.removeInfo(p);
                p.reset();
                p.getNode().markDirty();
                removedSomething = true;
                parent.fireConnectionRemoved(c);
            }
            dg.removeDependents(port);
            return removedSomething;
        }
    }

    /**
     * Removes the connection between the output port of the given node and the input port.
     *
     * @param input      the input port
     * @param outputNode the output node
     * @return true if a connection was found and removed.
     */
    public boolean disconnect(Port input, Node outputNode) {
        if (input == null)
            throw new IllegalArgumentException("The input port cannot be null.");
        if (outputNode == null)
            throw new IllegalArgumentException("The output node cannot be null.");
        if (input.getParentNode() != outputNode.getParent())
            throw new IllegalArgumentException("The input and output are not under the same parent.");
        if (!input.isInputPort())
            throw new IllegalArgumentException("The given port is not an input.");
        Node parent = input.getParentNode();
        if (parent == null) return false;
        DependencyGraph<Port, Connection> dg = parent.childGraph;
        if (dg == null) return false;
        Connection c = dg.getInfo(input);
        Port output = outputNode.outputPort;
        boolean removedSomething = dg.removeDependency(output, input);
        if (removedSomething) {
            // We remove the output port from the connection.
            c.removeOutput(output);
            // If the connection has no more output ports, remove the connection entirely.
            if (!c.hasOutputs())
                dg.removeInfo(input);
            input.reset();
            // This port was changed. Mark the node as dirty.
            input.getNode().markDirty();
            parent.fireConnectionRemoved(c);
        }
        return removedSomething;
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

    public Connection getUpstreamConnection(Port input) {
        if (childGraph == null) return null;
        return childGraph.getInfo(input);
    }

    /**
     * Get a list of all upstream connections on input ports of this node.
     *
     * @return a list of Connection objects. This list can safely be modified.
     */
    public Set<Connection> getUpstreamConnections() {
        if (parent == null || parent.childGraph == null)
            return new HashSet<Connection>(0);
        Set<Connection> connections = new HashSet<Connection>();
        for (Port p : ports.values()) {
            Connection c = parent.childGraph.getInfo(p);
            if (c != null)
                connections.add(c);
        }
        return connections;
    }

    /**
     * Get a list of all downstream connections on the output port of this node.
     *
     * @return a list of Connections objects. This list can safely be modified.
     */
    public Set<Connection> getDownstreamConnections() {
        if (parent == null || parent.childGraph == null)
            return new HashSet<Connection>(0);
        Set<Connection> connections = new HashSet<Connection>();
        // Connections are stored on the dependent (downstream) port.
        // Get all dependents for the output port, and add the info.
        for (Port p : parent.childGraph.getDependents(outputPort)) {
            connections.add(parent.childGraph.getInfo(p));
        }
        return connections;
    }

    /**
     * Get a set of all connection objects.
     *
     * @return a set of Connections objects. This list can safely be modified.
     */
    public Set<Connection> getConnections() {
        Set<Connection> connections = new HashSet<Connection>();
        connections.addAll(getUpstreamConnections());
        connections.addAll(getDownstreamConnections());
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
        if (parent == null) return false;
        if (parent.childGraph == null) return false;
        // Check output port for downstream connections.
        // We check this first because it goes fast.
        if (parent.childGraph.getDependents(outputPort).size() > 0)
            return true;
        // Check parameters for upstream connections.
        for (Port p : ports.values()) {
            if (parent.childGraph.getDependencies(p).size() > 0)
                return true;
        }
        return false;
    }

    /**
     * Check if the given port on this node is connected.
     *
     * @param port a port on this node.
     * @return true if this port is connected.
     */
    public boolean isConnected(Port port) {
        if (port == null)
            throw new IllegalArgumentException("Port cannot be null.");
        if (port.getNode() != this)
            throw new IllegalArgumentException("This node does not own the given port.");
        // The port needs to be in a parent to be connected.
        if (parent == null) return false;
        if (parent.childGraph == null) return false;
        if (port.isInputPort())
            return parent.childGraph.getDependencies(port).size() > 0;
        else
            return parent.childGraph.getDependents(port).size() > 0;
    }

    /**
     * Check if the two ports are connected together.
     * At least one of these ports needs to be on this node.
     *
     * @param port1 input or output port
     * @param port2 input or output port
     * @return true if the two ports are connected.
     * @throws IllegalArgumentException if neither of the ports are on this node.
     */
    public boolean isConnectedTo(Port port1, Port port2) throws IllegalArgumentException {
        // The order of the ports is unimportant, but one needs to be
        // an input and the other an output. If the two ports have
        // the same direction, they can never be connected.
        if (port1.getDirection() == port2.getDirection()) return false;
        Port output = port1.isOutputPort() ? port1 : port2;
        Port input = port1.isInputPort() ? port1 : port2;
        return output.getNode().isConnectedTo(input.getNode());
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
        if (inputNode == null)
            throw new IllegalArgumentException("Input node cannot be null.");
        // Both nodes need to have the same parent to be connected.
        if (parent == null || inputNode.parent == null || parent != inputNode.parent) return false;
        if (parent.childGraph == null) return false;
        Port output = getOutputPort();
        for (Port p : inputNode.ports.values()) {
            if (parent.childGraph.hasDependency(output, p))
                return true;
        }
        return false;
    }

    /**
     * Check if the output port is connected to the given input port.
     *
     * @param input the inpurt port
     * @return true if this node's output port is connected to the given input port.
     */
    public boolean isOutputConnectedTo(Port input) {
        if (input == null)
            throw new IllegalArgumentException("Input port cannot be null.");
        if (!input.isInputPort())
            throw new IllegalArgumentException("The given port is not an input.");
        return parent != null && parent.childGraph != null && parent.childGraph.hasDependency(outputPort, input);
    }

    //// Dirty handling ////

    public void markDirty() {
        if (dirty)
            return;
        dirty = true;
        if (parent != null) {
            // Mark all downstream connections dirty.
            // These are stored in the child graph of the parent.
            if (parent.childGraph != null) {
                for (Port p : parent.childGraph.getDependents(outputPort)) {
                    p.getNode().markDirty();
                }
            }
            if (!parent.dirty) {
                // Only changes to the rendered node should make the parent dirty.
                // TODO: Check for corner cases.
                if (parent.getRenderedChild() == this) {
                    parent.markDirty();
                }
            }
        }
        fireNodeDirty();
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
        stampDirty(false);
    }

    /**
     * Mark all upstream nodes that have stamp expressions dirty, recursive.
     * This method does the actual upstream marking.
     *
     * @param upstream if true, we're beyond the first node and can start marking parameters dirty.
     */
    private void stampDirty(boolean upstream) {
        if (parent != null && parent.childGraph != null) {
            for (Port port : ports.values()) {
                Connection conn = parent.childGraph.getInfo(port);
                if (conn == null) continue;
                for (Node n : conn.getOutputNodes()) {
                    n.stampDirty(true);
                }
            }
        }
        if (upstream) {
            for (Parameter p : parameters.values()) {
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
        update(new ProcessingContext());
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
        fireNodeUpdated(ctx);
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
        if (parent != null && parent.childGraph != null) {
            for (Port port : ports.values()) {
                Connection conn = parent.childGraph.getInfo(port);
                if (conn == null) continue;
                // Updating the connection sets the value of the corresponding input port.
                conn.update(ctx);
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
            // TODO: Adjust for cardinality
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
     * @param node    the node to process
     * @param context the processing context
     * @return the return value of the rendered child or null if the node doesn't have children
     * @throws ProcessingError if there are children, but no child note to render, or if the update of the child failed.
     */
    public Object cook(Node node, ProcessingContext context) throws ProcessingError {
        if (!node.hasChildren())
            return null;
        Node renderedChild = node.getRenderedChild();
        if (renderedChild == null)
            throw new ProcessingError(this, "No child node to render.");
        renderedChild.update(context);
        return renderedChild.getOutputValue();
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
        Object handleObj = handleCode.cook(this, new ProcessingContext());
        if (handleObj == null) return null;
        if (!(handleObj instanceof Handle))
            throw new AssertionError("Handle code for node " + getName() + " does not return Handle object.");
        return (Handle) handleObj;
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
        if (newParent.contains(getName())) {
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
            if (!contains(n)) {
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
