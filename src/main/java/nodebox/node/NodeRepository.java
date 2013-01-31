package nodebox.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import nodebox.function.FunctionRepository;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages a set of node libraries.
 */
public class NodeRepository {

    public static NodeRepository empty() {
        return new NodeRepository(ImmutableMap.<String, NodeLibrary>of());
    }

    public static NodeRepository of() {
        return new NodeRepository(ImmutableMap.<String, NodeLibrary>of("core", NodeLibrary.coreLibrary));
    }

    public static NodeRepository of(NodeLibrary... libraries) {
        ImmutableMap.Builder<String, NodeLibrary> builder = ImmutableMap.builder();
        for (NodeLibrary library : libraries) {
            if (library.getName().equals("core")) continue;
            builder.put(library.getName(), library);
        }
        builder.put("core", NodeLibrary.coreLibrary);
        return new NodeRepository(builder.build());
    }

    private final ImmutableMap<String, NodeLibrary> libraryMap;

    private NodeRepository(ImmutableMap<String, NodeLibrary> nodeLibraries) {
        libraryMap = nodeLibraries;
    }

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
        checkNotNull(identifier);
        if (identifier.equals("_root")) return Node.ROOT;
        String[] names = identifier.split("\\.");
        checkArgument(names.length == 2, "The node identifier should look like libraryname.nodename, not %s", identifier);
        String libraryName = names[0];
        NodeLibrary library = libraryMap.get(libraryName);
        checkNotNull(library, "Library %s not found.", libraryName);
        String nodeName = names[1];
        Node node = library.getRoot().getChild(nodeName);
        checkNotNull(node, "Node %s not found.", identifier);
        return node;
    }

    public Collection<NodeLibrary> getLibraries() {
        return libraryMap.values();
    }

    public List<Node> getNodes() {
        ImmutableList.Builder<Node> builder = ImmutableList.builder();
        for (NodeLibrary library : libraryMap.values())
            builder.addAll(library.getRoot().getChildren());
        return builder.build();
    }

    public List<Node> getNodesByCategory(String category) {
        if (category == null) return getNodes();
        ImmutableList.Builder<Node> builder = ImmutableList.builder();
        for (Node node : getNodes()) {
            if (node.getCategory().equals(category))
                builder.add(node);
        }
        if (category.equals("core")) {
            builder.add(Node.ROOT);
            builder.add(Node.NETWORK);
        }
        return builder.build();
    }

    public List<String> getCategories() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (Node node : getNodes()) {
            builder.add(node.getCategory());
        }
        return ImmutableList.copyOf(builder.build());
    }

    /**
     * Find the given library that contains the given node.
     *
     * @param node The node to find.
     * @return The NodeLibrary or null if the node could not be found.
     */
    public NodeLibrary nodeLibraryForNode(Node node) {
        for (NodeLibrary library : libraryMap.values()) {
            if (library.getRoot().hasChild(node)) {
                return library;
            }
        }
        return null;
    }

    /**
     * Find the combination of all function libraries used by all node libraries.
     *
     * This function traverses all the node libraries, and compiles a list of all function repositories used.
     *
     * The node libraries are traversed every time.
     *
     * @return the combined FunctionRepository.
     */
    public FunctionRepository getFunctionRepository() {
        FunctionRepository allFunctions = FunctionRepository.of();
        for (NodeLibrary library: getLibraries()) {
            allFunctions = FunctionRepository.combine(allFunctions, library.getFunctionRepository());
        }
        return allFunctions;
    }

}
