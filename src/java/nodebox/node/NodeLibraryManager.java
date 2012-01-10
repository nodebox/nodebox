package nodebox.node;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeLibraryManager {

    public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^(([\\w_]+\\.?)+)\\.([\\w_]+)$");
    public static final Pattern INT_PATTERN = Pattern.compile("^\\-?[0-9]+$");
    public static final Pattern FLOAT_PATTERN = Pattern.compile("^\\-?[0-9]+\\.[0-9]+$");
    public static final Pattern CODE_PATTERN = Pattern.compile("^(java):(([\\w_]+\\.?)+)\\.([\\w_]+)$");

    private List<File> searchPaths = new ArrayList<File>();
    private Map<String, NodeLibrary> libraries = new HashMap<String, NodeLibrary>();
    private boolean lookedForLibraries = false;

    public NodeLibraryManager() {
        add(NodeLibrary.BUILTINS);
    }

    //// Search paths ////

    public void addSearchPath(File f) {
        if (!f.isDirectory())
            throw new IllegalArgumentException("The given file should be a directory: " + f);
        searchPaths.add(f);
    }

    //// Library management ////

    public NodeLibrary get(String libraryName) {
        return libraries.get(libraryName);
    }

    public void add(NodeLibrary library) {
        if (contains(library.getName()))
            throw new RuntimeException("The manager already has a node library called " + library.getName());
        libraries.put(library.getName(), library);
    }

    public void remove(NodeLibrary library) {
        libraries.remove(library.getName());
    }

    public Set<NodeLibrary> getLibraries() {
        return new HashSet<NodeLibrary>(libraries.values());
    }

    public boolean contains(String libraryName) {
        return libraries.containsKey(libraryName);
    }

    public int size() {
        return libraries.size();
    }

    //// Loading ////

    public void lookForLibraries() {
        if (lookedForLibraries) return;
        refreshLibraries();
    }

    private void refreshLibraries() {
        lookedForLibraries = true;
        for (File path : searchPaths) {
            lookForLibraries(path);
        }
    }

    private void lookForLibraries(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                lookForLibraries(f);
            } else if (f.getName().endsWith(".ndbx")) {
                load(f);
            }
        }
    }

    //// Node shortcuts ////

    /**
     * Get a node based on an identifier.
     * <p/>
     * The node identifier is in the form libraryname.nodename. The libraryname can have multiple dots, e.g.
     * "colors.tints.blue". This signifies the "blue" node in the "colors.tints" libraryname.
     *
     * @param identifier a node identifier
     * @return a Node or null if a node could not be found.
     */
    public Node getNode(String identifier) {
        Matcher m = IDENTIFIER_PATTERN.matcher(identifier);
        if (!m.matches()) return null;
        String libraryName = m.group(1);
        String name = m.group(3);
        NodeLibrary library = libraries.get(libraryName);
        if (library == null) return null;
        return library.get(name);
    }

    public boolean hasNode(String identifier) {
        return getNode(identifier) != null;
    }

//    public void add(Node node) {
//        // Check if I also know about the protototype.
//        // This ensures that I can load the node from file later on.
//        if (node.getPrototype() != null && !hasNode(node.getPrototype().getIdentifier()))
//            throw new IllegalArgumentException("The node prototype for " + node + " is unknown. (" + node.getPrototype().getIdentifier() + ")");
//        if (hasNode(node.getIdentifier()))
//            throw new IllegalArgumentException("A node with this name already exists. " + node.getIdentifier());
//        NodeLibrary library = libraries.get(node.getLibrary());
//        if (library == null) {
//            library = new NodeLibrary(node.getLibrary());
//            libraries.put(node.getLibrary(), library);
//        }
//        library.add(node);
//    }

    /**
     * Loads the given XML string as a library and adds it to the manager.
     *
     * @param libraryName the name of the library
     * @param xml         the XML data
     * @return a new NodeLibrary
     * @throws RuntimeException if the XML data could not be parsed
     */
    public NodeLibrary load(String libraryName, String xml) throws RuntimeException {
        NodeLibrary library = NodeLibrary.load(libraryName, xml, this);
        add(library);
        return library;
    }

    /**
     * Loads the given file as a node library and adds it to the manager.
     *
     * @param f the file
     * @return a new NodeLibrary
     * @throws RuntimeException if the file is invalid or the XML data could not be parsed
     */
    public NodeLibrary load(File f) throws RuntimeException {
        NodeLibrary library = NodeLibrary.load(f, this);
        add(library);
        return library;
    }

    /**
     * Given a component identifier, load the appropriate piece of code.
     * A component identifier looks like this:
     * <pre>java/net.nodebox.node.NodeTest._circle</pre>
     * It is a method specifier.
     *
     * @param identifier the component identifier
     * @return the NodeCode
     * @throws IllegalArgumentException if the identifier could not be parsed.
     */
    public static NodeCode getCode(String identifier) throws IllegalArgumentException {
        Matcher m = CODE_PATTERN.matcher(identifier);
        if (!m.matches() || m.groupCount() != 4) {
            throw new IllegalArgumentException("The given identifier '" + identifier + "' is not valid.");
        }
        String runtime = m.group(1);
        String className = m.group(2);
        // This is group 4, because of the nested group in our regex pattern.
        String methodName = m.group(4);

        if (!"java".equals(runtime)) {
            throw new IllegalArgumentException("Invalid runtime; only java is supported.");
        }
        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("The class '" + className + "' could not be found.", e);
        }
        try {
            return new JavaMethodWrapper(clazz, methodName);
        } catch (Exception e) {
            throw new IllegalArgumentException("The method '" + methodName + "' for class '" + className + "' does not exist, has the wrong signature or is not static.", e);
        }
    }

    /**
     * Get a list of all nodes in every library.
     * Only nodes directly under the root node are returned.
     * TODO: Implement and add support for export flag.
     *
     * @return a list of all nodes
     */
    public List<Node> getNodes() {
        lookForLibraries();
        List<Node> nodes = new ArrayList<Node>();
        // Add the root node separately.
        nodes.add(Node.ROOT_NODE);
        for (NodeLibrary library : libraries.values()) {
            nodes.addAll(library.getExportedNodes());
        }
        return nodes;
    }

    private class FileTraversal {
        public final void traverse(File f) {
            if (f.isDirectory()) {

            }
        }
    }
}