package net.nodebox.node;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.reflect.Method;

/**
 * A NodeType holds the meta-information necessary to create a Node instance.
 * <p/>
 * A NodeType is like a Python class: it defines the structure of the node. It contains
 * the library this node was loaded from, its name, output types and processing methods,
 * but doesn't contain instance data. You can create a Node from a NodeType
 * by calling create_node(). This will return a Node that can store instance data, such as
 * parameter values.
 * <p/>
 * Do not create NodeTypes directly: they are loaded from NodeLibrary objects, that are in turn
 * loaded by the NodeLibraryManager.
 */
public class NodeType {

    // TODO: NodeType should hold weakrefs to instantiated nodes so it can propagate
    // changes from the type to the instances.

    private NodeLibrary library;
    private String name;
    private String defaultName;
    private ParameterType outputParameterType;
    private List<ParameterType> parameterTypes;
    private Method processingMethod;

    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]{0,29}$");
    private static final Pattern DOUBLE_UNDERSCORE_PATTERN = Pattern.compile("^__.*$");

    //// Initialization ////

    public NodeType(NodeLibrary library, String name, String outputType, Method processingMethod) {
        this.library = library;
        this.name = name;
        this.defaultName = name;
        this.outputParameterType = new ParameterType(this, "output", outputType, ParameterType.Direction.OUT);
        this.parameterTypes = new ArrayList<ParameterType>();
        this.processingMethod = processingMethod;
    }

    //// Getters ////


    public NodeLibrary getLibrary() {
        return library;
    }

    public String getName() {
        return name;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public Method getProcessingMethod() {
        return processingMethod;
    }

    public ParameterType getOutputParameterType() {
        return outputParameterType;
    }

    /**
     * Checks if the given name would be valid for this node.
     * <p/>
     * Raises a ValueError if the name is invalid.
     * This doesn't check for duplicate names when this node is in a network."""
     *
     * @param name
     */

    public static void validateName(String name) {
        // TODO: implement re.
        Matcher m1 = NODE_NAME_PATTERN.matcher(name);
        Matcher m2 = DOUBLE_UNDERSCORE_PATTERN.matcher(name);
        if (!m1.matches()) {
            throw new ValueError("Name does contain other characters than a-z0-9 or underscore, or is longer than 29 characters.");
        }
        if (m2.matches()) {
            throw new ValueError("Names starting with double underscore are reserved for internal use.");
        }
    }

    //// List operations ////

    public void addParameter(String name, String type) {
        ParameterType p = new ParameterType(this, name, type);
        parameterTypes.add(p);
    }

    public List<ParameterType> getParameterTypes() {
        return new ArrayList<ParameterType>(parameterTypes);
    }


    public ParameterType get(int index) {
        return parameterTypes.get(index);
    }

    public ParameterType get(String name) {
        for (ParameterType p : parameterTypes) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("ParameterType " + name + " not found.");
    }

    public int size() {
        return parameterTypes.size();
    }

    public Iterator<ParameterType> iterator() {
        return parameterTypes.iterator();
    }

    //// Instance creation ////

    public Node createNode() {
        return new Node(this);
    }

    public Node createNode(String name) {
        return new Node(this, name);
    }
}
