package nodebox.node;

import nodebox.node.event.*;
import nodebox.util.FileUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;

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
    private float frame = 1F;
    private HashMap<String, String> variables;
    private NodeCode code;
    private NodeEventBus eventBus = new NodeEventBus();

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
            throw new RuntimeException("Error while parsing: " + e.getMessage(), e);
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
        setCanvasParameter(library, "canvasX");
        setCanvasParameter(library, "canvasY");
        setCanvasParameter(library, "canvasWidth");
        setCanvasParameter(library, "canvasHeight");
        setCanvasParameter(library, "canvasBackground");
    }

    private static void setCanvasParameter(NodeLibrary library, String name) {
        String valueAsString = library.getVariable(name);
        Parameter param = library.getRootNode().getParameter(name);
        if (param != null && valueAsString != null)
            param.set(param.parseValue(valueAsString));
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
        this.variables = new LinkedHashMap<String, String>();
        Parameter pCanvasX = rootNode.addParameter("canvasX", Parameter.Type.FLOAT, 0f);
        Parameter pCanvasY = rootNode.addParameter("canvasY", Parameter.Type.FLOAT, 0f);
        Parameter pCanvasWidth = rootNode.addParameter("canvasWidth", Parameter.Type.FLOAT, 1000f);
        Parameter pCanvasHeight = rootNode.addParameter("canvasHeight", Parameter.Type.FLOAT, 1000f);
        Parameter pCanvasBackground = rootNode.addParameter("canvasBackground", Parameter.Type.COLOR, new nodebox.graphics.Color(1, 1, 1, 0));
        pCanvasX.setLabel("Offset X");
        pCanvasY.setLabel("Offset Y");
        pCanvasWidth.setLabel("Canvas Width");
        pCanvasHeight.setLabel("Canvas Height");
        pCanvasBackground.setLabel("Background Color");
        getRootNode().setValue("_code", new WrapInCanvasCode());
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

    public List<Node> getExportedNodes() {
        List<Node> allChildren = rootNode.getChildren();
        List<Node> exportedChildren = new ArrayList<Node>(allChildren.size());
        for (Node child : allChildren) {
            if (child.isExported()) {
                exportedChildren.add(child);
            }
        }
        return exportedChildren;
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

    /**
     * Get a node from this library.
     * <p/>
     * Only exported nodes are returned. If you want all nodes, use getRootNode().getChild()
     *
     * @param name the name of the node
     * @return the node, or null if a node with this name could not be found.
     */
    public Node get(String name) {
        if ("root".equals(name)) return rootNode;
        return rootNode.getExportedChild(name);
    }

    /**
     * Get the node at the given absolute path.
     * This method does a best effort to get the most specific node. If it fails to find a given segment,
     * it stops and returns the parent.
     *
     * @param path the path to parse
     * @return a node somewhere within the path, hopefully at the end.
     * @see nodebox.node.Node#getAbsolutePath()
     */
    public Node getNodeForPath(String path) {
        Node parent = getRootNode();
        if (!path.startsWith("/")) return parent;
        for (String part : path.substring(1).split("/")) {
            Node child = parent.getChild(part);
            if (child == null) {
                break;
            } else {
                parent = child;
            }
        }
        return parent;
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
        return rootNode.containsChildNode(nodeName);
    }

    //// Variables ////

    public String[] getVariableNames() {
        return variables.keySet().toArray(new String[variables.keySet().size()]);
    }

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

    //// Animation ////

    public float getFrame() {
        return frame;
    }

    public void setFrame(float frame) {
        this.frame = frame;
    }

    //// Persistence /////

    public void store() throws IOException, IllegalArgumentException {
        if (file == null)
            throw new IllegalArgumentException("Library was not loaded from a file and no file given to store.");
        store(file);
    }

    public void store(File f) throws IOException {
        file = f;
        NDBXWriter.write(this, f);
    }

    /**
     * Get the full XML data for this library and all of its nodes.
     *
     * @return an XML string
     */
    public String toXml() {
        return NDBXWriter.asString(this);
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

    //// Events ////

    public void addListener(NodeEventListener l) {
        eventBus.addListener(l);
    }

    public boolean removeListener(NodeEventListener l) {
        return eventBus.removeListener(l);
    }

    public List<NodeEventListener> getListeners() {
        return eventBus.getListeners();
    }

    public void fireNodeDirty(Node source) {
        eventBus.send(new NodeDirtyEvent(source));
    }

    public void fireNodeUpdated(Node source, ProcessingContext context) {
        eventBus.send(new NodeUpdatedEvent(source, context));
    }

    public void fireNodeAttributeChanged(Node source, Node.Attribute attribute) {
        eventBus.send(new NodeAttributeChangedEvent(source, attribute));
    }

    public void fireChildAdded(Node source, Node child) {
        eventBus.send(new ChildAddedEvent(source, child));
    }

    public void fireChildRemoved(Node source, Node child) {
        eventBus.send(new ChildRemovedEvent(source, child));
    }

    public void fireConnectionAdded(Node source, Connection c) {
        eventBus.send(new ConnectionAddedEvent(source, c));
    }

    public void fireConnectionRemoved(Node source, Connection c) {
        eventBus.send(new ConnectionRemovedEvent(source, c));
    }

    public void fireRenderedChildChanged(Node source, Node child) {
        eventBus.send(new RenderedChildChangedEvent(source, child));
    }

    public void fireValueChanged(Node source, Parameter parameter) {
        eventBus.send(new ValueChangedEvent(source, parameter));
    }

    //// Standard overrides ////

    @Override
    public String toString() {
        return getName();
    }

}
