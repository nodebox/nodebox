package net.nodebox.node;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Node library stores a set of (possibly hierarchical) nodes.
 * <p/>
 * Node libraries are both documents and libraries. By mentioning a node on the library search path, you can get
 * all the nodes.
 * <p/>
 * Node libraries can be backed by files, and saved in that same file, or they can be stored in memory only.
 * <p/>
 * This implementation of the node library only stores a root node.
 * Calling get(String) on the library actually forwards the call to getChild() on the root node.
 */
public class NodeLibrary {

    public static final NodeLibrary BUILTINS = new NodeLibrary();

    private String name;
    private File file;
    private Node rootNode;
    private HashMap<String, String> variables;
    private NodeCode code;

    private DependencyGraph<Parameter, Object> parameterGraph = new DependencyGraph<Parameter, Object>();

    private NodeLibrary() {
        this.name = "builtins";
        this.file = null;
        this.rootNode = null;
        this.variables = null;
    }

    public NodeLibrary(String name) {
        this(name, null);
    }

    public NodeLibrary(String name, File file) {
        this.name = name;
        this.file = file;
        this.rootNode = Node.ROOT_NODE.newInstance(this, "root");
        this.variables = new HashMap<String, String>();
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    //// Node management ////

    public Node getRootNode() {
        return rootNode;
    }

    public void add(Node node) {
        if (node.getLibrary() != this) throw new AssertionError("This node is already added to another library.");
        // The root node can be null in only one case: when we're creating the builtins library.
        // In that case, the rootNode becomes the given node.
        if (rootNode == null) {
            rootNode = node;
        } else {
            rootNode.add(node);
        }
    }

    public Node get(String name) {
        if ("root".equals(name)) return rootNode;
        return rootNode.getChild(name);
    }

    public Node remove(String name) {
        Node node = rootNode.getChild(name);
        if (node == null) return null;
        rootNode.remove(node);
        return node;
    }

    public boolean remove(Node node) {
        return rootNode.remove(node);
    }

    public int size() {
        return rootNode.size();
    }

    public boolean contains(String nodeName) {
        return rootNode.contains(nodeName);
    }

    //// Variables ////

    public String getVariable(String name) {
        return variables.get(name);
    }

    public void setVariable(String name, String value) {
        variables.put(name, value);
    }

    //// Code ////

    public void setCode(NodeCode code) {
        this.code = code;
    }

    public NodeCode getCode() {
        return code;
    }

    //// Persistence /////

    public void store() throws IOException, IllegalArgumentException {
        if (file == null)
            throw new IllegalArgumentException("Library was not loaded from a file and no file given to store.");
        store(file);
    }

    public void store(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(toXml().getBytes("UTF-8"));
        fos.close();
    }

    /**
     * Get the full XML data for this library and all of its nodes.
     *
     * @return an XML string
     */
    public String toXml() {
        StringBuffer xml = new StringBuffer();
        // Build the header
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<ndbx type=\"file\" formatVersion=\"0.9\">\n");
        // Write out all the variables
        for (Map.Entry<String, String> var : variables.entrySet()) {
            xml.append("  <var name=\"").append(var.getKey()).append("\" value=\"").append(var.getValue()).append("\"/>\n");
        }
        // Write out all the nodes (skip the root)
        List<Node> children = getRootNode().getChildren();
        // The order in which the nodes are written is important!
        // Since a library can potentially store an instance and its prototype, make sure that the prototype gets
        // stored sequentially before its instance.
        // The NDBXHandler class expects prototypes to be defined before their instances.
        while (!children.isEmpty()) {
            Node child = children.get(0);
            writeOrderedChild(xml, children, child);
        }
        // Add the child connections
        for (Node child : children) {
            for (Connection conn : child.getUpstreamConnections()) {
                conn.toXml(xml, "  ");
            }
        }
        xml.append("</ndbx>");
        return xml.toString();
    }

    /**
     * Write out the child. If the prototype of the child is also in this library, write that out first, recursively.
     *
     * @param xml      the buffer to write to
     * @param children a list of children that were written already.
     *                 When a child is written, we remove it from the list.
     * @param child    the child to write
     */
    private void writeOrderedChild(StringBuffer xml, List<Node> children, Node child) {
        Node prototype = child.getPrototype();
        if (prototype.getLibrary() == this && children.contains(prototype))
            writeOrderedChild(xml, children, prototype);
        child.toXml(xml, "  ");
        children.remove(child);
    }

    //// Parameter dependencies ////

    /**
     * Add a dependency between two parameters.
     * <p/>
     * Whenever the dependent node wants to update, it needs to check if the dependency
     * is clean. Also, whenever the dependency changes, the dependent gets notified.
     * <p/>
     * Do not call this method directly. Instead, let Parameter create the dependencies by using setExpression().
     *
     * @param dependency the parameter that provides the value
     * @param dependent  the parameter that needs the value
     * @see net.nodebox.node.Parameter#setExpression(String)
     */
    public void addParameterDependency(Parameter dependency, Parameter dependent) {
        parameterGraph.addDependency(dependency, dependent);
    }

    /**
     * Remove all dependencies this parameter has.
     * <p/>
     * This method gets called when a parameter clears out its expression.
     * <p/>
     * Do not call this method directly. Instead, let Parameter remove dependencies by using clearExpression().
     *
     * @param p the parameter
     * @see net.nodebox.node.Parameter#clearExpression()
     */
    public void removeParameterDependencies(Parameter p) {
        parameterGraph.removeDependencies(p);
    }

    /**
     * Remove all dependents this parameter has.
     * <p/>
     * This method gets called when the parameter is about to be removed. It signal all of its dependent nodes
     * that the parameter will no longer be available.
     * <p/>
     * Do not call this method directly. Instead, let Parameter remove dependents by using removeParameter().
     *
     * @param p the parameter
     * @see Node#removeParameter(String)
     */
    public void removeParameterDependents(Parameter p) {
        parameterGraph.removeDependents(p);
    }

    /**
     * Get all parameters that rely on this parameter.
     * <p/>
     * These parameters all have expressions that point to this parameter. Whenever this parameter changes,
     * they get notified.
     * <p/>
     * This list contains all "live" parameters when you call it. Please don't hold on to this list for too long,
     * since parameters can be added and removed at will.
     *
     * @param p the parameter
     * @return a list of parameters that depend on this parameter. This list can safely be modified.
     */
    public Set<Parameter> getParameterDependents(Parameter p) {
        return parameterGraph.getDependents(p);
    }

    /**
     * Get all parameters this parameter depends on.
     * <p/>
     * This list contains all "live" parameters when you call it. Please don't hold on to this list for too long,
     * since parameters can be added and removed at will.
     *
     * @param p the parameter
     * @return a list of parameters this parameter depends on. This list can safely be modified.
     */
    public Set<Parameter> getParameterDependencies(Parameter p) {
        return parameterGraph.getDependencies(p);
    }

    //// Standard overrides ////

    @Override
    public String toString() {
        return getName();
    }

}
