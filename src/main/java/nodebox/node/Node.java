package nodebox.node;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import nodebox.graphics.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

public final class Node {

    public static String path(String parentPath, Node node) {
        checkNotNull(node);
        return path(parentPath, node.getName());
    }

    public static String path(String parentPath, String nodeName) {
        checkNotNull(parentPath);
        checkNotNull(nodeName);
        checkArgument(parentPath.startsWith("/"), "Only absolute paths are supported.");
        if (nodeName.isEmpty()) return parentPath;
        if (parentPath.equals("/")) {
            return "/" + nodeName;
        } else {
            return Joiner.on("/").join(parentPath, nodeName);
        }
    }

    private enum Nodes {ROOT_NODE, NETWORK_NODE}

    public enum Attribute {PROTOTYPE, NAME, COMMENT, CATEGORY, DESCRIPTION, IMAGE, FUNCTION, POSITION, INPUTS, OUTPUT_TYPE, OUTPUT_RANGE, IS_NETWORK, CHILDREN, RENDERED_CHILD_NAME, CONNECTIONS, HANDLE, ALWAYS_RENDERED}

    /**
     * Check if data from the output node can be converted and used in the input port.
     * <p/>
     * The relation is not commutative:
     * an output port that can be converted to an input port does not imply the reverse.
     *
     * @param outputNode The output node
     * @param inputPort  The input port
     * @return true if the input port is compatible
     */
    public static boolean isCompatible(Node outputNode, Port inputPort) {
        checkNotNull(outputNode);
        checkNotNull(inputPort);
        return isCompatible(outputNode.getOutputType(), inputPort.getType());
    }

    /**
     * Check if data from the output can be converted and used in the input.
     * <p/>
     * The relation is not commutative:
     * an output port that can be converted to an input port does not imply the reverse.
     *
     * @param outputType The type of the output port of the upstream node
     * @param inputType  The type of the input port of the downstream node
     * @return true if the types are compatible
     */
    public static boolean isCompatible(String outputType, String inputType) {
        checkNotNull(outputType);
        checkNotNull(inputType);
        // If the output and input type are the same, they are compatible.
        if (outputType.equals(inputType)) return true;
        // Everything can be converted to a string.
        if (inputType.equals(Port.TYPE_STRING)) return true;
        // Integers can be converted to floating-point numbers without loss of information.
        if (outputType.equals(Port.TYPE_INT) && inputType.equals(Port.TYPE_FLOAT)) return true;
        // Floating-point numbers can be converted to integers: they are rounded.
        if (outputType.equals(Port.TYPE_FLOAT) && inputType.equals(Port.TYPE_INT)) return true;
        // A number can be converted to a point: both X and Y then get the same value.
        if (outputType.equals(Port.TYPE_INT) && inputType.equals(Port.TYPE_POINT)) return true;
        if (outputType.equals(Port.TYPE_FLOAT) && inputType.equals(Port.TYPE_POINT)) return true;
        // If none of these tests pass, the types are not compatible.
        return false;
    }

    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,29}$");
    private static final Pattern DOUBLE_UNDERSCORE_PATTERN = Pattern.compile("^__.*$");
    private static final Pattern NUMBER_AT_THE_END = Pattern.compile("^(.*?)(\\d*)$");
    private static final Pattern UNDERSCORE_NUMBER_AT_THE_END = Pattern.compile("^(.*?)((\\_(\\d*))?)$");
    private static final Pattern RESERVED_WORD_PATTERN = Pattern.compile("^(node|network)$");

    public static final Node ROOT = new Node(Nodes.ROOT_NODE);
    public static final Node NETWORK = new Node(Nodes.NETWORK_NODE);

    public static final Map<String, Node> coreNodes;

    static {
        ImmutableMap.Builder<String, Node> builder = new ImmutableMap.Builder<String, Node>();
        builder.put("ROOT", ROOT);
        builder.put("NETWORK", NETWORK);
        coreNodes = builder.build();
    }

    private final Node prototype;
    private final String name;
    private final String comment;
    private final String category;
    private final String description;
    private final String image;
    private final String function;
    private final Point position;
    private final ImmutableList<Port> inputs;
    private final String outputType;
    private final Port.Range outputRange;
    private final boolean isNetwork;
    private final ImmutableList<Node> children;
    private final String renderedChildName;
    private final ImmutableList<Connection> connections;
    private final String handle;
    private final boolean isAlwaysRendered;
    private final int hashCode;

    //// Constructors ////

    /**
     * Constructor for the root and network nodes. This can only be called once for each of them.
     */
    private Node(Nodes coreNode) {
        switch (coreNode) {
            case ROOT_NODE:
            default:
                checkState(ROOT == null, "You cannot create more than one root node.");
                prototype = null;
                name = "node";
                comment = "";
                description = "Base node to be extended for custom nodes.";
                image = "node.png";
                outputRange = Port.DEFAULT_RANGE;
                isNetwork = false;
                break;
            case NETWORK_NODE:
                checkState(ROOT != null, "The root node has not been created yet.");
                checkState(NETWORK == null, "You cannot create more than one network node.");
                prototype = ROOT;
                name = "network";
                comment = "";
                image = "network.png";
                description = "Create an empty subnetwork.";
                outputRange = Port.Range.LIST;
                isNetwork = true;
                break;
        }
        category = "";
        function = "core/zero";
        position = Point.ZERO;
        inputs = ImmutableList.of();
        outputType = Port.TYPE_FLOAT;
        children = ImmutableList.of();
        renderedChildName = "";
        connections = ImmutableList.of();
        handle = "";
        isAlwaysRendered = false;
        hashCode = calcHashCode();
    }

    private void checkAllNotNull(Object... args) {
        for (Object o : args) {
            checkNotNull(o);
        }
    }

    private Node(Node prototype, String name, String comment, String category, String description, String image, String function,
                 Point position, ImmutableList<Port> inputs,
                 String outputType, Port.Range outputRange, boolean isNetwork, ImmutableList<Node> children,
                 String renderedChildName, ImmutableList<Connection> connections, String handle, boolean isAlwaysRendered) {
        checkAllNotNull(prototype, name, description, image, function,
                position, inputs, outputType, children,
                renderedChildName, connections);
        checkArgument(!name.equals("node"), "The name node is a reserved internal name.");
        checkArgument(!name.equals("network"), "The name network is a reserved internal name.");
        this.prototype = prototype;
        this.name = name;
        this.comment = comment;
        this.category = category;
        this.description = description;
        this.image = image;
        this.function = function;
        this.position = position;
        this.inputs = inputs;
        this.outputType = outputType;
        this.outputRange = outputRange;
        this.isNetwork = isNetwork;
        this.children = children;
        this.renderedChildName = renderedChildName;
        this.connections = connections;
        this.handle = handle;
        this.isAlwaysRendered = isAlwaysRendered;
        this.hashCode = calcHashCode();
    }

    private int calcHashCode() {
        return Objects.hashCode(prototype, name, comment, category, description, image, function, position,
                inputs, outputType, outputRange, isNetwork, children, renderedChildName, connections, handle, isAlwaysRendered);
    }

    //// Getters ////

    public Node getPrototype() {
        return prototype;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }

    public String getCategory() {
        return category;
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

    public boolean isNetwork() {
        return isNetwork;
    }

    public boolean isAlwaysRendered() {
        return isAlwaysRendered;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public ImmutableList<Node> getChildren() {
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
        } else if (attribute == Attribute.COMMENT) {
            return getComment();
        } else if (attribute == Attribute.CATEGORY) {
            return getCategory();
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
        } else if (attribute == Attribute.IS_NETWORK) {
            return isNetwork();
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

    public String uniqueInputName(String prefix) {
        Matcher m = UNDERSCORE_NUMBER_AT_THE_END.matcher(prefix);
        m.find();
        String namePrefix = m.group(1);
        String number = m.group(4);
        int counter;
        if (number != null && number.length() > 0) {
            counter = Integer.parseInt(number);
        } else {
            counter = 1;
        }
        while (true) {
            String suggestedName = namePrefix + "_" + counter;
            if (!hasInput(suggestedName)) {
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
        Matcher m3 = RESERVED_WORD_PATTERN.matcher(name);
        if (!m1.matches()) {
            throw new InvalidNameException(null, name, "Names can only contain lowercase letters, numbers, and the underscore. Names cannot be longer than 29 characters.");
        }
        if (m2.matches()) {
            throw new InvalidNameException(null, name, "Names starting with double underscore are reserved for internal use.");
        }
        if (m3.matches()) {
            throw new InvalidNameException(null, name, "Names cannot be a reserved word (network, node).");
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
     * Create a new node with the given comment.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param comment The new node comment.
     * @return A new Node.
     */
    public Node withComment(String comment) {
        return newNodeWithAttribute(Attribute.COMMENT, comment);
    }

    /**
     * Create a new node with the given category.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param category new node category.
     * @return A new Node.
     */
    public Node withCategory(String category) {
        return newNodeWithAttribute(Attribute.CATEGORY, category);
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
     * Create a new node with the given child node renamed.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param childName The name of the child node to rename.
     * @param newName   The new name of the child node.
     * @return A new Node.
     */
    public Node withChildRenamed(String childName, String newName) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkArgument(!newName.equals("root"), "A child node of a network cannot have the name 'root'.");
        if (childName.equals(newName)) return this;
        Node newNode = getChild(childName).withName(newName);
        Node newParent = withChildRemoved(childName).withChildAdded(newNode);
        if (renderedChildName.equals(childName))
            newParent = newParent.withRenderedChild(newNode);

        if (hasPublishedChildInputs(childName)) {
            ImmutableList.Builder<Port> b = ImmutableList.builder();
            for (Port p : inputs) {
                if (p.getChildNodeName().equals(childName)) {
                    b.add(Port.publishedPort(newNode, newNode.getInput(p.getChildPortName()), p.getName()));
                } else
                    b.add(p);
            }
            newParent = newParent.newNodeWithAttribute(Attribute.INPUTS, b.build());
        }

        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(childName)) {
                newParent = newParent.connect(c.getOutputNode(), newName, c.getInputPort());
            } else if (c.getOutputNode().equals(childName)) {
                newParent = newParent.connect(newName, c.getInputNode(), c.getInputPort());
            }
        }

        return newParent;
    }

    /**
     * Create a new node with a comment added to the given child.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param childName The name of the child node to comment.
     * @param comment   The new comment of the child node.
     * @return A new Node.
     */
    public Node withChildCommented(String childName, String comment) {
        Node newNode = getChild(childName).withComment(comment);
        return withChildReplaced(childName, newNode);
    }

    /**
     * Create a new node with the given child input port removed.
     * <p/>
     * If you call this on ROOT, extend() is called implicitly.
     *
     * @param childName The name of the child node to which the child port belongs to.
     * @param portName  The name of the child port to remove.
     * @return A new Node.
     */
    public Node withChildInputRemoved(String childName, String portName) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkArgument(hasChild(childName), "Node %s does not have a child named %s.", this, childName);
        Node child = getChild(childName);
        checkArgument(child.hasInput(portName), "Node %s does not have an input port %s.", childName, portName);
        if (hasPublishedInput(childName, portName))
            return unpublish(childName, portName).withChildInputRemoved(childName, portName);
        if (isConnected(childName, portName))
            return disconnect(childName, portName).withChildInputRemoved(childName, portName);
        return withChildReplaced(childName, child.withInputRemoved(portName));
    }

    private Node withChildInputChanged(String childName, String portName, Port newPort) {
        // todo: checks
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        return withChildReplaced(childName, getChild(childName).withInputChanged(portName, newPort));
    }

    public Node withChildPositionChanged(String childName, double xOffset, double yOffset) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkArgument(hasChild(childName), "Node %s does not have a child named %s.", this, childName);
        Node child = getChild(childName);
        return withChildReplaced(childName, child.withPosition(child.getPosition().moved(xOffset, yOffset)));
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
        Node n = this;
        if (isNetwork() && p.isPublishedPort()) {
            String childNodeName = p.getChildNodeName();
            Node newChildNode = p.getChildNode(this).withInputValue(p.getChildPortName(), value);
            n = n.withChildReplaced(childNodeName, newChildNode);
        }
        return n.withInputChanged(portName, p);
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

    public List<Port> getPublishedPorts() {
        if (!isNetwork()) return ImmutableList.of();
        ImmutableList.Builder<Port> b = ImmutableList.builder();
        for (Port p : inputs) {
            if (p.isPublishedPort())
                b.add(p);
        }
        return b.build();
    }

    public Port getPublishedPort(Port port) {
        if (!isNetwork()) return null;
        checkArgument(port.isPublishedPort(), "Given port %s is not a published port.", port);
        return port.getChildPort(this);
    }

    public Port getPublishedPort(String publishedPortName) {
        if (!isNetwork()) return null;
        checkArgument(hasInput(publishedPortName), "Given port %s does not exist.", publishedPortName);
        return getPublishedPort(getInput(publishedPortName));
    }

    public Port getPortByChildReference(Node childNode, Port childPort) {
        return getPortByChildReference(childNode.getName(), childPort.getName());
    }

    public Port getPortByChildReference(String childNodeName, String childPortName) {
        if (!isNetwork()) return null;
        for (Port p : inputs) {
            if (p.isPublishedPort() && p.getChildNodeName().equals(childNodeName) && p.getChildPortName().equals(childPortName)) {
                return p;
            }
        }
        return null;
    }

    public boolean hasPublishedInput(String childNodeName, String childPortName) {
        if (!isNetwork()) return false;
        for (Port p : inputs) {
            if (p.isPublishedPort() && p.getChildNodeName().equals(childNodeName) && p.getChildPortName().equals(childPortName)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPublishedChildInputs(String childNodeName) {
        if (!isNetwork()) return false;
        for (Port p : inputs) {
            if (p.isPublishedPort() && p.getChildNodeName().equals(childNodeName)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPublishedInput(String publishedName) {
        if (!isNetwork()) return false;
        return hasInput(publishedName) && getInput(publishedName).isPublishedPort();
    }

    /**
     * Create a new node with the given child input node/port published.
     *
     * @param childNodeName The name of the child input Node.
     * @param childPortName The name of the child input Port.
     * @param publishedName The name of by which the published port is known.
     * @return A new Node.
     */
    public Node publish(String childNodeName, String childPortName, String publishedName) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkNotNull(publishedName, "Published name cannot be null.");
        checkArgument(hasChild(childNodeName), "Node %s does not have a child named %s.", this, childNodeName);
        Node childNode = getChild(childNodeName);
        checkArgument(childNode.hasInput(childPortName), "Child node %s does not have an child node port %s.", childNodeName, childPortName);
        Port childPort = childNode.getInput(childPortName);
        checkArgument(!hasPublishedInput(childNodeName, childPortName), "The port %s on node %s has already been published.", childPortName, childNodeName);
        checkArgument(!hasInput(publishedName), "Node %s already has an childNode named %s.", this, publishedName);

        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(childNodeName) && c.getInputPort().equals(childPortName))
                return disconnect(c).publish(childNodeName, childPortName, publishedName);
        }


        Port newPort = Port.publishedPort(childNode, childPort, publishedName);
        return withInputAdded(newPort);
    }

    public Node unpublish(String childNodeName, String childPortName) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkArgument(hasChild(childNodeName), "Node %s does not have a child named %s.", this, childNodeName);
        Node childNode = getChild(childNodeName);
        checkArgument(childNode.hasInput(childPortName), "Child node %s does not have a port named %s.", childNodeName, childPortName);
        Port childPort = childNode.getInput(childPortName);

        Port p = getPortByChildReference(childNode, childPort);
        return withInputRemoved(p.getName());
    }

    public Node unpublishChildNode(Node childNode) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkArgument(hasChild(childNode), "Node %s does not have a child named %s.", this, childNode);

        Node n = this;
        for (Port p : getPublishedPorts()) {
            if (p.getChildNodeName().equals(childNode.getName())) {
                n = n.withInputRemoved(p.getName());
            }
        }
        return n;
    }

    public Node unpublish(String publishedName) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        return withInputRemoved(publishedName);
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
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkArgument(!node.getName().equals("root"), "A child node of a network cannot have the name 'root'.");
        if (hasChild(node.getName())) {
            String uniqueName = uniqueName(node.getName());
            node = node.withName(uniqueName);
        }
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
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        Node childToRemove = getChild(childName);
        checkArgument(childToRemove != null, "Node %s is not a child of node %s.", childName, this);
        if (hasPublishedChildInputs(childName))
            return unpublishChildNode(childToRemove).withChildRemoved(childName);
        if (isConnected(childName))
            return disconnect(childName).withChildRemoved(childName);
        if (renderedChildName.equals(childName))
            return withRenderedChild(null).withChildRemoved(childName);
        ImmutableList.Builder<Node> b = ImmutableList.builder();
        for (Node child : getChildren()) {
            if (child != childToRemove)
                b.add(child);
        }
        return newNodeWithAttribute(Attribute.CHILDREN, b.build());
    }


    /**
     * Checks if a new node of which the given node would become a new child node is internally
     * consistent with the published inputs it already has, for example if the network
     * still exposes a child port that was removed from the candidate node.
     *
     * @param childName The name of the child node to be replaced
     * @param newChild  The new candidate node
     * @return true if the resulting network would be internally consistent.
     */
    private boolean isConsistentWithPublishedInputs(String childName, Node newChild) {
        // TODO Implement
        return true;
//        for (PublishedPort pp : publishedInputs) {
//            if (pp.getChildNode().equals(childName)) {
//                if (!newChild.hasInput(pp.getChildPort()))
//                    return false;
//            }
//        }
//        return true;
    }

    /**
     * Checks if a new node of which the given node would become a new child node is internally
     * consistent with the connections it already has, for example if the network
     * still exposes a child port that was removed from the candidate node.
     *
     * @param childName The name of the child node to be replaced
     * @param newChild  The new candidate node
     * @return true if the resulting network would be internally consistent.
     */
    private boolean isConsistentWithConnections(String childName, Node newChild) {
        for (Connection c : connections) {
            if (c.getInputNode().equals(childName)) {
                if (!newChild.hasInput(c.getInputPort()))
                    return false;
            }
        }
        return true;
    }

    /**
     * Create a new node of which the published inputs are consistent with
     * the given node if the given node would become a new child of this node.
     * Note that the given child node is NOT added as a new child on this node.
     *
     * @param childName The name of the child node to be replaced
     * @param newChild  The candidate node
     * @return A new node
     */
    private Node withConsistentPublishedInputs(String childName, Node newChild) {
        // TODO Implement
        return this;
    }

    /**
     * Create a new node of which the connections are consistent with
     * the given node if the given node would become a new child of this node.
     * Note that the given child node is NOT added as a new child on this node.
     *
     * @param childName The name of the child node to be replaced
     * @param newChild  The candidate node
     * @return A new node
     */
    private Node withConsistentConnections(String childName, Node newChild) {
        ImmutableList.Builder<Connection> b = ImmutableList.builder();
        for (Connection c : connections) {
            if (c.getInputNode().equals(childName)) {
                if (newChild.hasInput(c.getInputPort()))
                    b.add(c);
            } else
                b.add(c);
        }
        return newNodeWithAttribute(Attribute.CONNECTIONS, b.build());
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
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        Node childToReplace = getChild(childName);
        checkNotNull(newChild);
        checkArgument(newChild.getName().equals(childName), "New child %s does not have the same name as old child %s.", newChild, childName);
        checkArgument(childToReplace != null, "Node %s is not a child of node %s.", childName, this);

        if (!isConsistentWithPublishedInputs(childName, newChild))
            return withConsistentPublishedInputs(childName, newChild)
                    .withChildReplaced(childName, newChild);

        if (!isConsistentWithConnections(childName, newChild))
            return withConsistentConnections(childName, newChild)
                    .withChildReplaced(childName, newChild);

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
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
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
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkArgument(hasChild(outputNode), "Node %s does not have a child named %s.", this, outputNode);
        checkArgument(hasChild(inputNode), "Node %s does not have a child named %s.", this, inputNode);
        Node input = getChild(inputNode);
        checkArgument(input.hasInput(inputPort), "Node %s does not have an input port %s.", inputNode, inputPort);
        checkArgument(!hasPublishedInput(inputNode, inputPort), "Node %s has a published input for port %s of child %s.", this, inputNode, inputPort);
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
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
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
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
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

    /**
     * Create a new node with a connection to the given child/port removed.
     *
     * @param node     The node of which to remove the connection.
     * @param portName The port of which to remove the connection.
     * @return A new Node.
     */
    public Node disconnect(String node, String portName) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        checkArgument(hasChild(node), "Node %s does not have a child named %s.", this, node);
        Node child = getChild(node);
        checkArgument(child.hasInput(portName), "Node %s does not have an input port %s.", node, portName);
        ImmutableList.Builder<Connection> b = ImmutableList.builder();
        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(node) && c.getInputPort().equals(portName)) {

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
        if (!isNetwork()) return false;
        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(node) || c.getOutputNode().equals(node))
                return true;
        }
        return false;
    }

    public boolean isConnected(String node, String port) {
        if (!isNetwork()) return false;
        for (Connection c : getConnections()) {
            if (c.getInputNode().equals(node) && c.getInputPort().equals(port))
                return true;
        }
        return false;
    }

    /**
     * Create a new node with a number of nodes from another network copied into.
     * </p>
     * The original parent of the nodes is given so that previous connections can be recreated.
     *
     * @param nodesParent The original parent of the nodes to copy.
     * @param nodes       The nodes to copy.
     * @return A new Node.
     */
    public Node withChildrenAdded(Node nodesParent, Iterable<Node> nodes) {
        checkArgument(isNetwork(), "Node %s is not a network node.", this);
        Map<String, String> newNames = new HashMap<String, String>();
        Node newParent = this;

        for (Node node : nodes) {
            newParent = newParent.withChildAdded(node);
            Node newNode = Iterables.getLast(newParent.getChildren());
            newNames.put(node.getName(), newNode.getName());
        }

        // TODO: Recreate published inputs?

        for (Connection c : nodesParent.getConnections()) {
            String outputNodeName = c.getOutputNode();
            String inputNodeName = c.getInputNode();

            if (newNames.containsKey(outputNodeName)) {
                outputNodeName = newNames.get(outputNodeName);
            }

            if (newParent.hasChild(outputNodeName) && newNames.containsKey(inputNodeName)) {
                inputNodeName = newNames.get(inputNodeName);

                if (newParent.hasChild(inputNodeName)) {
                    Node outputNode = newParent.getChild(outputNodeName);
                    Node inputNode = newParent.getChild(inputNodeName);
                    Port inputPort = inputNode.getInput(c.getInputPort());
                    newParent = newParent.connect(outputNode.getName(), inputNode.getName(), inputPort.getName());
                }
            }
        }
        return newParent;
    }

    /**
     * Find the connection with the given inputNode and port.
     *
     * @param inputNode The child input node
     * @param inputPort The child input port
     * @return the Connection object, or null if the connection could not be found.
     */
    public Connection getConnection(String inputNode, String inputPort) {
        if (!isNetwork()) return null;
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

    public Node withAlwaysRenderedSet(boolean alwaysRendered) {
        return newNodeWithAttribute(Attribute.ALWAYS_RENDERED, alwaysRendered);
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
        String comment = this.comment;
        String category = this.category;
        String description = this.description;
        String image = this.image;
        String function = this.function;
        Point position = this.position;
        ImmutableList<Port> inputs = this.inputs;
        String outputType = this.outputType;
        Port.Range outputRange = this.outputRange;
        boolean isNetwork = this.isNetwork;
        ImmutableList<Node> children = this.children;
        String renderedChildName = this.renderedChildName;
        ImmutableList<Connection> connections = this.connections;
        String handle = this.handle;
        boolean alwaysRendered = this.isAlwaysRendered;

        switch (attribute) {
            case PROTOTYPE:
                prototype = (Node) value;
                break;
            case NAME:
                name = (String) value;
                break;
            case COMMENT:
                comment = (String) value;
                break;
            case CATEGORY:
                category = (String) value;
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
            case IS_NETWORK:
                isNetwork = (Boolean) value;
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
            case ALWAYS_RENDERED:
                alwaysRendered = (Boolean) value;
                break;
            default:
                throw new AssertionError("Unknown attribute " + attribute);
        }
        // If we're "changing" an attribute on ROOT or NETWORK, make it the prototype.
        if (this == ROOT || this == NETWORK) {
            prototype = this;

        }

        // The name of a node can never be "node" or "network".
        if (name.equals("node"))
            name = "node1";
        else if (name.equals("network"))
            name = "network1";

        return new Node(prototype, name, comment, category, description, image, function, position,
                inputs, outputType, outputRange, isNetwork, children, renderedChildName, connections, handle, alwaysRendered);
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Node)) return false;
        final Node other = (Node) o;
        return Objects.equal(prototype, other.prototype)
                && Objects.equal(name, other.name)
                && Objects.equal(comment, other.comment)
                && Objects.equal(category, other.category)
                && Objects.equal(description, other.description)
                && Objects.equal(image, other.image)
                && Objects.equal(function, other.function)
                && Objects.equal(position, other.position)
                && Objects.equal(inputs, other.inputs)
                && Objects.equal(outputType, other.outputType)
                && Objects.equal(isNetwork, other.isNetwork)
                && Objects.equal(children, other.children)
                && Objects.equal(renderedChildName, other.renderedChildName)
                && Objects.equal(connections, other.connections)
                && Objects.equal(handle, other.handle)
                && Objects.equal(isAlwaysRendered, other.isAlwaysRendered);
    }

    @Override
    public String toString() {
        return String.format("<Node %s:%s>", getName(), getFunction());
    }

}
