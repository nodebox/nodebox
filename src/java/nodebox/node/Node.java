package nodebox.node;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import nodebox.graphics.Point;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

public final class Node {

    public static final Node ROOT = new Node();

    public static String path(String parentPath, Node node) {
        checkNotNull(node);
        return path(parentPath, node.getName());
    }

    public static String path(String parentPath, String nodeName) {
        checkNotNull(parentPath);
        checkNotNull(nodeName);
        checkArgument(parentPath.startsWith("/"), "Only absolute paths are supported.");
        if (parentPath.equals("/")) {
            return "/" + nodeName;
        } else {
            return Joiner.on("/").join(parentPath, nodeName);
        }
    }

    public enum Attribute {PROTOTYPE, NAME, DESCRIPTION, IMAGE, FUNCTION, POSITION, INPUTS, OUTPUT_TYPE, OUTPUT_RANGE, CHILDREN, RENDERED_CHILD_NAME, CONNECTIONS, HANDLE}

    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,29}$");
    private static final Pattern DOUBLE_UNDERSCORE_PATTERN = Pattern.compile("^__.*$");
    private static final Pattern NUMBER_AT_THE_END = Pattern.compile("^(.*?)(\\d*)$");

    private final Node prototype;
    private final String name;
    private final String description;
    private final String image;
    private final String function;
    private final Point position;
    private final ImmutableList<Port> inputs;
    private final String outputType;
    private final Port.Range outputRange;
    private final ImmutableList<Node> children;
    private final String renderedChildName;
    private final ImmutableList<Connection> connections;
    private final String handle;

    //// Constructors ////

    /**
     * Constructor for the root node. This can only be called once.
     */
    private Node() {
        checkState(ROOT == null, "You cannot create more than one root node.");
        prototype = null;
        name = "_root";
        description = "";
        image = "";
        function = "core/zero";
        position = Point.ZERO;
        inputs = ImmutableList.of();
        outputType = Port.TYPE_FLOAT;
        outputRange = Port.DEFAULT_RANGE;
        children = ImmutableList.of();
        renderedChildName = "";
        connections = ImmutableList.of();
        handle = null;
    }

    private void checkAllNotNull(Object... args) {
        for (Object o : args) {
            checkNotNull(o);
        }
    }

    private Node(Node prototype, String name, String description, String image, String function,
                 Point position, ImmutableList<Port> inputs, String outputType, Port.Range outputRange, ImmutableList<Node> children,
                 String renderedChildName, ImmutableList<Connection> connections, String handle) {
        checkAllNotNull(prototype, name, description, image, function,
                position, inputs, outputType, children,
                renderedChildName, connections);
        checkArgument(!name.equals("_root"), "The name _root is a reserved internal name.");
        this.prototype = prototype;
        this.name = name;
        this.description = description;
        this.image = image;
        this.function = function;
        this.position = position;
        this.inputs = inputs;
        this.outputType = outputType;
        this.outputRange = outputRange;
        this.children = children;
        this.renderedChildName = renderedChildName;
        this.connections = connections;
        this.handle = handle;
    }

    //// Getters ////

    public Node getPrototype() {
        return prototype;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public String getFunction() {
        return function;
    }

    public Point getPosition() {
        return position;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public Collection<Node> getChildren() {
        return children;
    }

    public Node getChild(String name) {
        checkNotNull(name, "Name cannot be null.");
        for (Node child : getChildren()) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    public boolean hasChild(String name) {
        checkNotNull(name, "Name cannot be null.");
        return getChild(name) != null;
    }

    public boolean hasChild(Node node) {
        checkNotNull(node, "Node cannot be null.");
        return children.contains(node);
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public List<Port> getInputs() {
        return inputs;
    }

    public Port getInput(String name) {
        checkNotNull(name, "Port name cannot be null.");
        for (Port p : getInputs()) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public ImmutableList<Port> getInputsOfType(String type) {
        ImmutableList.Builder<Port> b = ImmutableList.builder();
        for (Port p : getInputs()) {
            if (p.getType().equals(type)) {
                b.add(p);
            }
        }
        return b.build();
    }

    public boolean hasInput(String name) {
        return getInput(name) != null;
    }


    public String getOutputType() {
        return outputType;
    }

    public Port.Range getOutputRange() {
        return outputRange;
    }

    public boolean hasValueOutputRange() {
        return outputRange.equals(Port.Range.VALUE);
    }

    public boolean hasListOutputRange() {
        return outputRange.equals(Port.Range.LIST);
    }

    public boolean hasListInputs() {
        for (Port port : getInputs()) {
            if (port.hasListRange())
                return true;
        }
        return false;
    }

    /**
     * Get the name of the rendered child. This node is guaranteed to exist as a child on the network.
     * The rendered child name can be null, indicating no child node will be rendered.
     *
     * @return the name of the rendered child or null.
     */
    public String getRenderedChildName() {
        return renderedChildName;
    }

    /**
     * Get the rendered child Node.
     *
     * @return The rendered child node or null if none is set.
     */
    public Node getRenderedChild() {
        if (getRenderedChildName().isEmpty()) return null;
        Node renderedChild = getChild(getRenderedChildName());
        checkNotNull(renderedChild, "The child with name %s cannot be found. This is a bug in NodeBox.", getRenderedChildName());
        return renderedChild;
    }

    public boolean hasRenderedChild() {
        return hasChild(getRenderedChildName());
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public String getHandle() {
        return handle;
    }

    public Object getAttributeValue(Attribute attribute) {
        if (attribute == Attribute.PROTOTYPE) {
            return getPrototype();
        } else if (attribute == Attribute.NAME) {
            return getName();
        } else if (attribute == Attribute.DESCRIPTION) {
            return getDescription();
        } else if (attribute == Attribute.IMAGE) {
            return getImage();
        } else if (attribute == Attribute.FUNCTION) {
            return getFunction();
        } else if (attribute == Attribute.POSITION) {
            return getPosition();
        } else if (attribute == Attribute.INPUTS) {
            return getInputs();
        } else if (attribute == Attribute.OUTPUT_TYPE) {
            return getOutputType();
        } else if (attribute == Attribute.OUTPUT_RANGE) {
            return getOutputRange();
        } else if (attribute == Attribute.CHILDREN) {
            return getChildren();
        } else if (attribute == Attribute.RENDERED_CHILD_NAME) {
            return getRenderedChildName();
        } else if (attribute == Attribute.CONNECTIONS) {
            return getConnections();
        } else if (attribute == Attribute.HANDLE) {
            return getHandle();
        } else {
            throw new AssertionError("Unknown node attribute " + attribute);
        }
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
            if (!hasChild(suggestedName)) {
                // We don't use rename here, since it assumes the node will be in
                // this network.
                return suggestedName;
            }
            ++counter;
        }
    }

    //// Mutation functions ////

    /**
     * Create a new node with this node as the prototype.
     *
     * @return The new node.
     */
    public Node extend() {
        return newNodeWithAttribute(Attribute.PROTOTYPE, this);
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
        //Matcher m3 = RESERVED_WORD_PATTERN.matcher(name);
        if (!m1.matches()) {
            throw new InvalidNameException(null, name, "Names can only contain lowercase letters, numbers, and the underscore. Names cannot be longer than 29 characters.");
        }
        if (m2.matches()) {
            throw new InvalidNameException(null, name, "Names starting with double underscore are reserved for internal use.");
        }
    }

    /**
     * Create a new node with the given name.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param name The new node name.
     * @return A new Node.
     */
    public Node withName(String name) {
        validateName(name);
        return newNodeWithAttribute(Attribute.NAME, name);
    }

    /**
     * Create a new node with the given description.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param description new node description.
     * @return A new Node.
     */
    public Node withDescription(String description) {
        return newNodeWithAttribute(Attribute.DESCRIPTION, description);
    }

    /**
     * Create a new node with the given image.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param image new node image.
     * @return A new Node.
     */
    public Node withImage(String image) {
        return newNodeWithAttribute(Attribute.IMAGE, image);
    }

    /**
     * Create a new node with the given function identifier.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param function The new function identifier.
     * @return A new Node.
     */
    public Node withFunction(String function) {
        return newNodeWithAttribute(Attribute.FUNCTION, function);
    }

    /**
     * Create a new node with the given position.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param position The new position.
     * @return A new Node.
     */
    public Node withPosition(Point position) {
        return newNodeWithAttribute(Attribute.POSITION, position);
    }

    /**
     * Create a new node with the given input port added.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param port The port to add.
     * @return A new Node.
     */
    public Node withInputAdded(Port port) {
        checkNotNull(port, "Port cannot be null.");
        checkArgument(!hasInput(port.getName()), "An input port named %s already exists on node %s.", port.getName(), this);
        ImmutableList.Builder<Port> b = ImmutableList.builder();
        b.addAll(getInputs());
        b.add(port);
        return newNodeWithAttribute(Attribute.INPUTS, b.build());
    }

    /**
     * Create a new node with the given input port removed.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param portName The name of the port to remove.
     * @return A new Node.
     */
    public Node withInputRemoved(String portName) {
        Port portToRemove = getInput(portName);
        checkArgument(portToRemove != null, "Input port %s does not exist on node %s.", portName, this);

        ImmutableList.Builder<Port> b = ImmutableList.builder();
        for (Port port : getInputs()) {
            if (portToRemove != port)
                b.add(port);
        }
        return newNodeWithAttribute(Attribute.INPUTS, b.build());
    }

    /**
     * Create a new node with the given input port replaced.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param portName The name of the port to replace.
     * @param newPort  The new Port instance.
     * @return A new Node.
     */
    public Node withInputChanged(String portName, Port newPort) {
        Port oldPort = getInput(portName);
        checkNotNull(oldPort, "Input port %s does not exist on node %s.", portName, this);
        ImmutableList.Builder<Port> b = ImmutableList.builder();
        // Add all ports back in the correct order.
        for (Port port : getInputs()) {
            if (port == oldPort) {
                b.add(newPort);
            } else {
                b.add(port);
            }
        }
        return newNodeWithAttribute(Attribute.INPUTS, b.build());
    }

    /**
     * Create a new node with the given input port set to a new value.
     * Only standard port types (int, float, string, point) can have their value set.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param portName The name of the port to set.
     * @param value    The new Port value.
     * @return A new Node.
     */
    public Node withInputValue(String portName, Object value) {
        Port p = getInput(portName);
        checkNotNull(p, "Input port %s does not exist on node %s.", portName, this);
        p = p.withValue(value);
        return withInputChanged(portName, p);
    }

    /**
     * Create a new node with the given input port set to the new range.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param portName The name of the port to set.
     * @param range    The new range.
     * @return A new Node.
     */
    public Node withInputRange(String portName, Port.Range range) {
        Port p = getInput(portName);
        checkNotNull(p, "Input port %s does not exist on node %s.", portName, this);
        p = p.withRange(range);
        return withInputChanged(portName, p);
    }

    /**
     * Create a new node with the given output type.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param outputType The new output type.
     * @return A new Node.
     */
    public Node withOutputType(String outputType) {
        return newNodeWithAttribute(Attribute.OUTPUT_TYPE, outputType);
    }

    /**
     * Create a new node with the output range set to the given value.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param outputRange The new output range.
     * @return A new Node.
     */
    public Node withOutputRange(Port.Range outputRange) {
        return newNodeWithAttribute(Attribute.OUTPUT_RANGE, outputRange);
    }

    /**
     * Create a new node with the given child added.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param node The child node to add.
     * @return A new Node.
     */
    public Node withChildAdded(Node node) {
        checkNotNull(node, "Node cannot be null.");
        checkArgument(!hasChild(node.getName()), "A node named %s is already a child of node %s.", node.getName(), this);
        ImmutableList.Builder<Node> b = ImmutableList.builder();
        b.addAll(getChildren());
        b.add(node);
        return newNodeWithAttribute(Attribute.CHILDREN, b.build());
    }

    /**
     * Create a new node with the given child removed.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param childName The name of the child node to remove.
     * @return A new Node.
     */
    public Node withChildRemoved(String childName) {
        Node childToRemove = getChild(childName);
        checkArgument(childToRemove != null, "Node %s is not a child of node %s.", childName, this);
        ImmutableList.Builder<Node> b = ImmutableList.builder();
        for (Node child : getChildren()) {
            if (child != childToRemove)
                b.add(child);
        }
        return newNodeWithAttribute(Attribute.CHILDREN, b.build());
    }

    /**
     * Create a new node with the child replaced by the given node.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param childName The name of the child node to replace.
     * @param newChild  The new child node.
     * @return A new Node.
     */
    public Node withChildReplaced(String childName, Node newChild) {
        Node childToReplace = getChild(childName);
        checkNotNull(newChild);
        checkArgument(newChild.getName().equals(childName), "New child %s does not have the same name as old child %s.", newChild, childName);
        checkArgument(childToReplace != null, "Node %s is not a child of node %s.", childName, this);
        ImmutableList.Builder<Node> b = ImmutableList.builder();
        for (Node child : getChildren()) {
            if (child != childToReplace) {
                b.add(child);
            } else {
                b.add(newChild);
            }
        }
        return newNodeWithAttribute(Attribute.CHILDREN, b.build());
    }

    /**
     * Create a new node with the given child set as rendered.
     * <p/>
     * The rendered node should exist as a child on this node.
     * If you don't want a child node to be rendered, set it to an empty string ("").
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param name The new rendered child.
     * @return A new Node.
     */
    public Node withRenderedChildName(String name) {
        checkNotNull(name, "Rendered child name cannot be null.");
        checkArgument(name.isEmpty() || hasChild(name), "Node does not have a child named %s.", name);
        return newNodeWithAttribute(Attribute.RENDERED_CHILD_NAME, name);
    }

    /**
     * Create a new node with the given child set as rendered.
     * <p/>
     * The rendered node should exist as a child on this node.
     * If you don't want a child node to be rendered, set it to null.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param renderedChild The new rendered child or null if you don't want anything rendered.
     * @return A new Node.
     */
    public Node withRenderedChild(Node renderedChild) {
        return withRenderedChildName(renderedChild == null ? "" : renderedChild.getName());
    }

    /**
     * Create a new node that connects the given child nodes.
     *
     * @param outputNode The name of the output (upstream) Node.
     * @param inputNode  The name of the input (downstream) Node.
     * @param inputPort  The name of the input (downstream) Port.
     * @return A new Node.
     */
    public Node connect(String outputNode, String inputNode, String inputPort) {
        checkArgument(hasChild(outputNode), "Node %s does not have a child named %s.", this, outputNode);
        checkArgument(hasChild(inputNode), "Node %s does not have a child named %s.", this, inputNode);
        Node input = getChild(inputNode);
        checkArgument(input.hasInput(inputPort), "Node %s does not have an input port %s.", inputNode, inputPort);
        Connection newConnection = new Connection(outputNode, inputNode, inputPort);
        ImmutableList.Builder<Connection> b = ImmutableList.builder();
        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(inputNode) && c.getInputPort().equals(inputPort)) {
                // There was already a connection, on this input port.
                // We "disconnect" it by not including it in the new list.
            } else {
                b.add(c);
            }
        }
        b.add(newConnection);
        return newNodeWithAttribute(Attribute.CONNECTIONS, b.build());
    }

    /**
     * Create a new node with the given connection removed.
     *
     * @param connection The connection to remove.
     * @return A new Node.
     */
    public Node disconnect(Connection connection) {
        checkArgument(getConnections().contains(connection), "Node %s does not have a connection %s", this, connection);
        ImmutableList.Builder<Connection> b = ImmutableList.builder();
        for (Connection c : getConnections()) {
            if (c != connection)
                b.add(c);
        }
        return newNodeWithAttribute(Attribute.CONNECTIONS, b.build());
    }

    /**
     * Create a new node with all existing connections of the given child node removed.
     *
     * @param node The node of which to remove all the connections.
     * @return A new Node.
     */
    public Node disconnect(String node) {
        checkArgument(hasChild(node), "Node %s does not have a child named %s.", this, node);
        ImmutableList.Builder<Connection> b = ImmutableList.builder();
        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(node) || c.getOutputNode().equals(node)) {
                // The node is part of this connection,
                // so don't include it in the new list.
            } else {
                b.add(c);
            }
        }
        return newNodeWithAttribute(Attribute.CONNECTIONS, b.build());
    }

    public Node withConnectionAdded(Connection connection) {
        return connect(connection.getOutputNode(), connection.getInputNode(), connection.getInputPort());
    }

    public boolean isConnected(String node) {
        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(node) || c.getOutputNode().equals(node))
                return true;
        }
        return false;
    }

    /**
     * Find the connection with the given inputNode and port.
     *
     * @param inputNode The child input node
     * @param inputPort The child input port
     * @return the Connection object, or null if the connection could not be found.
     */
    public Connection getConnection(String inputNode, String inputPort) {
        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(inputNode) && c.getInputPort().equals(inputPort))
                return c;

        }
        return null;
    }

    /**
     * Create a new node with the given handle added.
     *
     * @param handle The handle to add.
     * @return A new Node.
     */
    public Node withHandle(String handle) {
        return newNodeWithAttribute(Attribute.HANDLE, handle);
    }

    public boolean hasHandle() {
        return handle != null;
    }

    /**
     * Change an attribute on the node and return a new copy.
     * The prototype remains the same.
     * <p/>
     * We use this more complex function instead of having every withXXX method call the constructor, because
     * it allows us a to be more flexible when changing Node attributes.
     *
     * @param attribute The Node's attribute.
     * @param value     The value for the attribute. The type needs to match the internal type.
     * @return A copy of this node with the attribute changed.
     */
    @SuppressWarnings("unchecked")
    private Node newNodeWithAttribute(Attribute attribute, Object value) {
        Node prototype = this.prototype;
        String name = this.name;
        String description = this.description;
        String image = this.image;
        String function = this.function;
        Point position = this.position;
        ImmutableList<Port> inputs = this.inputs;
        String outputType = this.outputType;
        Port.Range outputRange = this.outputRange;
        ImmutableList<Node> children = this.children;
        String renderedChildName = this.renderedChildName;
        ImmutableList<Connection> connections = this.connections;
        String handle = this.handle;

        switch (attribute) {
            case PROTOTYPE:
                prototype = (Node) value;
                break;
            case NAME:
                name = (String) value;
                break;
            case DESCRIPTION:
                description = (String) value;
                break;
            case IMAGE:
                image = (String) value;
                break;
            case FUNCTION:
                function = (String) value;
                break;
            case POSITION:
                position = (Point) value;
                break;
            case INPUTS:
                inputs = (ImmutableList<Port>) value;
                break;
            case OUTPUT_TYPE:
                outputType = (String) value;
                break;
            case OUTPUT_RANGE:
                outputRange = (Port.Range) value;
                break;
            case CHILDREN:
                children = (ImmutableList<Node>) value;
                break;
            case RENDERED_CHILD_NAME:
                renderedChildName = (String) value;
                break;
            case CONNECTIONS:
                connections = (ImmutableList<Connection>) value;
                break;
            case HANDLE:
                handle = (String) value;
                break;
            default:
                throw new AssertionError("Unknown attribute " + attribute);
        }
        // If we're "changing" an attribute on ROOT, make the ROOT the prototype.
        if (prototype == null) {
            prototype = ROOT;

        }

        // The name of a node can never be "_root".
        if (name.equals("_root")) {
            name = "node";
        }

        return new Node(prototype, name, description, image, function, position,
                inputs, outputType, outputRange, children, renderedChildName, connections, handle);
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return Objects.hashCode(prototype, name, description, image, function, position,
                inputs, outputType, outputRange, children, renderedChildName, connections, handle);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Node)) return false;
        final Node other = (Node) o;
        return Objects.equal(prototype, other.prototype)
                && Objects.equal(name, other.name)
                && Objects.equal(description, other.description)
                && Objects.equal(image, other.image)
                && Objects.equal(function, other.function)
                && Objects.equal(position, other.position)
                && Objects.equal(inputs, other.inputs)
                && Objects.equal(outputType, other.outputType)
                && Objects.equal(children, other.children)
                && Objects.equal(renderedChildName, other.renderedChildName)
                && Objects.equal(connections, other.connections)
                && Objects.equal(handle, other.handle);
    }

    @Override
    public String toString() {
        return String.format("<Node %s:%s>", getName(), getFunction());
    }

}
