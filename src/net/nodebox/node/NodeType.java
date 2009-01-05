package net.nodebox.node;

import net.nodebox.handle.Handle;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A NodeType holds the meta-information necessary to create a Node instance.
 * <p/>
 * A NodeType is like a Java class: it defines the structure of the node. It contains
 * the library this node was loaded from, its name, output types and processing methods,
 * but doesn't contain instance data. You can create a Node from a NodeType
 * by calling create_node(). This will return a Node that can store instance data, such as
 * parameter values.
 * <p/>
 * Do not create NodeTypes directly:  use the NodeManager to load them. This also takes care
 * of versioning.
 */
public abstract class NodeType {

    public static final Pattern NODE_NAME_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]{0,29}$");
    public static final Pattern DOUBLE_UNDERSCORE_PATTERN = Pattern.compile("^__.*$");
    public static final Pattern RESERVED_WORD_PATTERN = Pattern.compile("^(node|network|root)$");

    /**
     * The manager object that created this node type. Used for reloading.
     */
    private NodeManager manager;

    /**
     * The full, reverse-DNS identifier for this node, e.g. net.nodebox.node.vector.rect
     */
    private String identifier;

    /**
     * The description of this node.
     */
    private String description = "";

    /**
     * An ordered list with all input parameter types.
     */
    private List<ParameterType> parameterTypes = new ArrayList<ParameterType>();

    /**
     * The output parameter type.
     */
    private ParameterType outputParameterType;

    /**
     * The version of this node.
     */
    private Version version = new Version();

    private static Logger logger = Logger.getLogger("net.nodebox.node.NodeType");

    protected NodeType(NodeManager manager, String identifier, ParameterType.Type outputType) {
        this.manager = manager;
        this.identifier = identifier;
        outputParameterType = new ParameterType(this, "output", outputType, ParameterType.Direction.OUT);
    }

    public NodeManager getManager() {
        return manager;
    }

    //// Identifiers ////

    /**
     * The full name of this type, in Reverse-DNS format, e.g. net.nodebox.node.vector.rect
     *
     * @return the type identifier
     * @see #getShortName()
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * The short identifier of this type. Returns the last part of the identifier. This name is not unique.
     *
     * @return the short name of this type
     * @see #getIdentifier()
     */
    public String getShortName() {
        String[] tokens = identifier.split("\\.");
        return tokens[tokens.length - 1];
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * This method returns the name prefix that will be used when creating new nodes.
     * Subclasses can override this method to define custom names. By default, the short name
     * of the node type is used.
     *
     * @return the default name for creating new nodes.
     * @see #getShortName()
     */
    public String getDefaultName() {
        return getShortName();
    }

    /**
     * Checks if the given name would be valid for this node.
     *
     * @param name
     */
    public static void validateName(String name) throws InvalidNameException {
        Matcher m1 = NODE_NAME_PATTERN.matcher(name);
        Matcher m2 = DOUBLE_UNDERSCORE_PATTERN.matcher(name);
        Matcher m3 = RESERVED_WORD_PATTERN.matcher(name);
        if (!m1.matches()) {
            throw new InvalidNameException(null, name, "Name does contain other characters than a-z0-9 or underscore, or is longer than 29 characters.");
        }
        if (m2.matches()) {
            throw new InvalidNameException(null, name, "Names starting with double underscore are reserved for internal use.");
        }
        if (m3.matches()) {
            throw new InvalidNameException(null, name, "Names cannot be a reserved word (network, node, root).");
        }
    }

    //// Versioning ////

    public Version getVersion() {
        return (Version) version.clone();
    }

    public String getVersionAsString() {
        return version.toString();
    }

    public void setVersion(Version v) {
        version = new Version(v.getMajor(), v.getMinor());
    }

    public void setVersion(int major, int minor) {
        version = new Version(major, minor);
    }

    public void setVersion(String v) {
        version = new Version(v);
    }

    //// Parameters ////

    public ParameterType addParameterType(String name, ParameterType.Type type) {
        ParameterType p = new ParameterType(this, name, type);
        parameterTypes.add(p);
        return p;
    }

    public List<ParameterType> getParameterTypes() {
        return parameterTypes;
    }

    public int getParameterTypeCount() {
        return parameterTypes.size();
    }

    public ParameterType getParameterType(String name) throws NotFoundException {
        for (ParameterType pt : parameterTypes) {
            if (pt.getName().equals(name))
                return pt;
        }
        throw new NotFoundException(this, name, "Node type " + getIdentifier() + " does not have a parameter type '" + name + "'");
    }

    public boolean hasParameterType(String name) {
        for (ParameterType pt : parameterTypes) {
            if (pt.getName().equals(name))
                return true;
        }
        return false;
    }

    public ParameterType getOutputParameterType() {
        return outputParameterType;
    }

    //// Instance creation ////

    /**
     * Creates a node instance based on this node type.
     * <p/>
     * Override this method if your node type requires a special type of node subclass.
     *
     * @return a Node instance (or subclass)
     */
    public Node createNode() {
        return new Node(this);
    }

    //// Handle support ////

    /**
     * Creates and returns a Handle object that can be used for direct manipulation of the parameters of this node.
     * By default, this code returns null to indicate that no handle is available. Classes can override this method
     * to provide an appropriate handle implementation.
     *
     * @param node the node instance that is bound to this handle.
     * @return a handle instance bound to this node, or null.
     */
    public Handle createHandle(Node node) {
        return null;
    }

    //// Node processing ////

    /**
     * Processes the node and sets its output.
     *
     * @param node the node instance to process.
     * @param ctx  a context object
     * @return false if a processing error occurred. Check the node's messages.
     */
    public abstract boolean process(Node node, ProcessingContext ctx);

    /**
     * Reloads the code behind the node type.
     * By default, this method returns false to indicate that the code cannot be reloaded.
     *
     * @return false if the code cannot be reloaded
     */
    public boolean reload() {
        return false;
    }

    //// Cloning ////

    /**
     * Creates a copy of this node type. All parameter types are cloned as well. The identifier and version number
     * are the same, so be aware that this node type will no longer be unique.
     * <p/>
     * The cloned type receives the same NodeManager, but is not automatically added to that manager.
     *
     * @return a cloned copy of this node type.
     */
    public NodeType clone() {
        NodeType newType;
        try {
            Constructor c = getClass().getConstructor(NodeManager.class);
            newType = (NodeType) c.newInstance(manager);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not clone node type '" + getIdentifier() + "'.  No appropriate constructor found.");
            return null;
        }
        newType.identifier = identifier;
        newType.description = description;
        newType.version = version.clone();
        newType.outputParameterType = outputParameterType.clone(newType);
        newType.parameterTypes.clear();
        for (ParameterType pt : getParameterTypes()) {
            newType.parameterTypes.add(pt.clone(newType));
        }
        return newType;
    }

}
