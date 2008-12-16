package net.nodebox.node;

import net.nodebox.node.canvas.CanvasNetwork;
import net.nodebox.node.image.ImageNetwork;
import net.nodebox.node.vector.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manager class for retrieving nodes.
 * <p/>
 * The manager always deals with nodes specified by their qualified name. A qualified name is a "full" name in reverse
 * domain notation, e.g. net.nodebox.node.vector.RectNode.
 */
public class NodeManager {

    /**
     * A "fuzzy" way of specifying a node version.
     * <p/>
     * Examples:
     * <ul>
     * <li>"=2.0" -- only version 2.0, nothing above or below that.</li>
     * <!-- <li>"2.0" -- same as above. Used for XML parsing.</li> -->
     * <li>"&gt;=2.0" -- anything greater or equal then version 2.0</li>
     * </ul>
     */
    public static class VersionSpecifier {

        private String specifier;

        public VersionSpecifier(String specifier) {
            this.specifier = specifier;
        }

        public String getSpecifier() {
            return specifier;
        }

        public boolean matches(Node.Version version) {
            return false;
        }

        public boolean matches(int major, int minor) {
            return matches(new Node.Version(major, minor));
        }
    }

    public static class NodeNotFound extends RuntimeException {

        public String qualifiedName;

        public NodeNotFound(String name) {
            this.qualifiedName = name;
        }

        public String getQualifiedName() {
            return qualifiedName;
        }
    }

    /**
     * A list of nodes with the same type but different version.
     * <p/>
     * The list is ordered from the newest (highest) version to the oldest (lowest) version.
     */
    public static class VersionedNodeList {
        private List<Node> nodes = new ArrayList<Node>();

        public void addNode(Node node) {
            Node.Version newVersion = node.getVersion();
            int i = 0;
            for (; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                if (n.getVersion().smallerThan(newVersion))
                    break;
            }
            nodes.add(i, node);
        }

        public Node getLatestVersion() {
            return nodes.get(0);
        }

        public List<Node> getNodes() {
            return nodes;
        }

    }

    private HashMap<String, VersionedNodeList> nodeMap = new HashMap<String, VersionedNodeList>();

    private static Logger logger = Logger.getLogger("net.nodebox.node.NodeManager");

    public NodeManager() {
        // Add builtin nodes
        // Canvas nodes
        addNode(new CanvasNetwork());
        // Image nodes
        addNode(new ImageNetwork());
        // Vecto nodes
        addNode(new CopyNode());
        addNode(new EllipseNode());
        addNode(new RectNode());
        addNode(new TransformNode());
        addNode(new VectorNetwork());
    }

    public void addNode(Node n) {
        VersionedNodeList nodeList = nodeMap.get(n.getTypeName());
        if (nodeList == null) {
            nodeList = new VersionedNodeList();
            nodeMap.put(n.getTypeName(), nodeList);
        }
        nodeList.addNode(n);
    }

    /**
     * Finds and returns the latest version of the node with the given qualified name.
     *
     * @param qualifiedName the fully qualified type name of the node (e.g. net.nodebox.node.vector.RectNode)
     * @return a Node object or null if no node with that name was found.
     * @throws net.nodebox.node.NodeManager.NodeNotFound
     *          if the node could not be found
     */
    public Node getNode(String qualifiedName) throws NodeNotFound {
        VersionedNodeList nodeList = nodeMap.get(qualifiedName);
        if (nodeList == null)
            throw new NodeNotFound(qualifiedName);
        return nodeList.getLatestVersion();
    }

    /**
     * Finds and returns the exact specified version of the node with the given qualified name.
     *
     * @param qualifiedName the fully qualified type name of the node (e.g. net.nodebox.node.vector.RectNode)
     * @param version       the exact version number you want to retrieve.
     * @return a Node object or null if no node with that name was found.
     * @throws net.nodebox.node.NodeManager.NodeNotFound
     *          if the node could not be found
     */
    public Node getNode(String qualifiedName, Node.Version version) throws NodeNotFound {
        VersionedNodeList nodeList = nodeMap.get(qualifiedName);
        if (nodeList == null)
            throw new NodeNotFound(qualifiedName);
        for (Node n : nodeList.getNodes()) {
            if (n.getVersion().equals(version))
                return n;
        }
        throw new NodeNotFound(qualifiedName);
    }

    /**
     * Finds and returns the specified version of the node with the given qualified name.
     *
     * @param qualifiedName the fully qualified type name of the node (e.g. net.nodebox.node.vector.RectNode)
     * @param specifier     the version specifier you want to retrieve.
     * @return a Node object.
     * @throws net.nodebox.node.NodeManager.NodeNotFound
     *          if the node could not be found
     */
    public Node getNode(String qualifiedName, VersionSpecifier specifier) throws NodeNotFound {
        VersionedNodeList nodeList = nodeMap.get(qualifiedName);
        if (nodeList == null)
            throw new NodeNotFound(qualifiedName);
        for (Node n : nodeList.getNodes()) {
            if (specifier.matches(n.getVersion()))
                return n;
        }
        throw new NodeNotFound(qualifiedName);
    }

}
