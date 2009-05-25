package net.nodebox.node;

import net.nodebox.util.FileUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeLibraryManager {

    public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^(([\\w_]+\\.?)+)\\.([\\w_]+)$");
    public static final Pattern INT_PATTERN = Pattern.compile("^\\-?[0-9]+$");
    public static final Pattern FLOAT_PATTERN = Pattern.compile("^\\-?[0-9]+\\.[0-9]+$");
    public static final Pattern CODE_PATTERN = Pattern.compile("^(java):(([\\w_]+\\.?)+)\\.([\\w_]+)$");

    private Map<String, NodeLibrary> libraries = new HashMap<String, NodeLibrary>();

    public NodeLibraryManager() {
        add(NodeLibrary.BUILTINS);
    }

    //// Library management ////

    public NodeLibrary get(String libraryName) {
        return libraries.get(libraryName);
    }

    public void add(NodeLibrary library) {
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

    public NodeLibrary load(String libraryName, String xml) {
        try {
            if (contains(libraryName))
                throw new RuntimeException("The manager already has a node library called " + libraryName);
            NodeLibrary library = new NodeLibrary(libraryName);
            load(library, new ByteArrayInputStream(xml.getBytes("UTF8")));
            return library;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error in the XML parser configuration", e);
        } catch (SAXException e) {
            throw new RuntimeException("Error while parsing.", e);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while parsing.", e);
        }
    }

    public NodeLibrary load(File f) throws RuntimeException {
        try {
            // The library name is the file name without the ".ndbx" extension.
            // Chop off the .ndbx
            String libraryName = FileUtils.stripExtension(f);
            if (contains(libraryName))
                throw new RuntimeException("The manager already has a node library called " + libraryName);
            NodeLibrary library = new NodeLibrary(libraryName, f);
            load(library, new FileInputStream(f));
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

    private void load(NodeLibrary library, InputStream is) throws IOException, ParserConfigurationException, SAXException {
        // Because the library can define both prototypes and instances that use these prototypes, we need to be
        // able to retrieve nodes from this library as well. The handler uses manager.getNode() to retrieve a prototype,
        // so we need to add the library in advance to be able to load nodes from it.
        add(library);
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        NDBXHandler handler = new NDBXHandler(this, library);
        parser.parse(is, handler);
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
        List<Node> nodes = new ArrayList<Node>();
        // Add the root node separately.
        nodes.add(Node.ROOT_NODE);
        for (NodeLibrary library : libraries.values()) {
            nodes.addAll(library.getRootNode().getChildren());
        }
        return nodes;
    }
}