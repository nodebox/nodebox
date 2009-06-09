package nodebox.node;

import nodebox.util.FileUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
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


    /**
     * Load a library from the given XML.
     * <p/>
     * This library is not added to the manager. The manager is used only to look up prototypes.
     * You can add the library to the manager yourself using manager.add(), or by calling
     * manager.load().
     *
     * @param libraryName the name of the new library
     * @param xml         the xml data of the library
     * @param manager     the manager used to look up node prototypes.
     * @return a new node library
     * @throws RuntimeException When the string could not be parsed.
     * @see nodebox.node.NodeLibraryManager#add(NodeLibrary)
     * @see nodebox.node.NodeLibraryManager#load(String, String)
     */
    public static NodeLibrary load(String libraryName, String xml, NodeLibraryManager manager) throws RuntimeException {
        try {
            NodeLibrary library = new NodeLibrary(libraryName);
            load(library, new ByteArrayInputStream(xml.getBytes("UTF8")), manager);
            return library;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error in the XML parser configuration", e);
        } catch (SAXException e) {
            throw new RuntimeException("Error while parsing.", e);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while parsing.", e);
        }
    }

    /**
     * Load a library from the given file.
     * <p/>
     * This library is not added to the manager. The manager is used only to look up prototypes.
     * You can add the library to the manager yourself using manager.add(), or by calling
     * manager.load().
     *
     * @param f       the file to load
     * @param manager the manager used to look up node prototypes.
     * @return a new node library
     * @throws RuntimeException When the file could not be found, or parsing failed.
     * @see nodebox.node.NodeLibraryManager#add(NodeLibrary)
     * @see nodebox.node.NodeLibraryManager#load(File)
     */
    public static NodeLibrary load(File f, NodeLibraryManager manager) throws RuntimeException {
        try {
            // The library name is the file name without the ".ndbx" extension.
            // Chop off the .ndbx
            String libraryName = FileUtils.stripExtension(f);
            NodeLibrary library = new NodeLibrary(libraryName, f);
            load(library, new FileInputStream(f), manager);
            return library;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error in the XML parser configuration", e);
        } catch (SAXException e) {
            throw new RuntimeException("Error while parsing: " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found " + f, e);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while parsing " + f, e);
        }
    }

    /**
     * This method gets called from the public load method and does the actual parsing.
     * <p/>
     * The method requires a newly created (empty) library. Nodes are added to this library.
     *
     * @param library the newly created library
     * @param is      the input stream data
     * @param manager the manager used for looking up prototypes.
     * @throws IOException                  when the data could not be loaded
     * @throws ParserConfigurationException when the parser is incorrectly configured
     * @throws SAXException                 when the data could not be parsed
     */
    private static void load(NodeLibrary library, InputStream is, NodeLibraryManager manager) throws IOException, ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        NDBXHandler handler = new NDBXHandler(library, manager);
        parser.parse(is, handler);
    }

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
        for (Node child : getRootNode().getChildren()) {
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
     * @see nodebox.node.Parameter#setExpression(String)
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
     * @see nodebox.node.Parameter#clearExpression()
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
