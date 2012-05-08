package nodebox.node;

import com.google.common.collect.ImmutableList;
import nodebox.function.CoreFunctions;
import nodebox.function.FunctionLibrary;
import nodebox.function.FunctionRepository;
import nodebox.graphics.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class provides mutable access to the immutable NodeLibrary.
 * <p/>
 * This class is single-threaded. You can only access it from one concurrent thread.
 * However, the internal nodeLibrary is immutable, so you can keep looking at a NodeLibrary while the controller
 * generates a new one.
 */
public class NodeLibraryController {

    private NodeLibrary nodeLibrary;

    public static NodeLibraryController create() {
        return new NodeLibraryController(NodeLibrary.create("untitled", Node.ROOT, NodeRepository.of(), FunctionRepository.of()));
    }

    public static NodeLibraryController create(String libraryName, NodeRepository nodeRepository, FunctionRepository functionRepository) {
        return new NodeLibraryController(NodeLibrary.create(libraryName, Node.ROOT, nodeRepository, functionRepository));
    }

    public static NodeLibraryController withLibrary(NodeLibrary nodeLibrary) {
        return new NodeLibraryController(nodeLibrary);
    }

    public NodeLibraryController(NodeLibrary nodeLibrary) {
        this.nodeLibrary = nodeLibrary;
    }

    public NodeLibrary getNodeLibrary() {
        return nodeLibrary;
    }

    public void setNodeLibrary(NodeLibrary nodeLibrary) {
        this.nodeLibrary = nodeLibrary;
    }

    public void setFunctionRepository(FunctionRepository functionRepository) {
        nodeLibrary = nodeLibrary.withFunctionRepository(functionRepository);
    }
    
    public Node getNode(String nodePath) {
        return nodeLibrary.getNodeForPath(nodePath);
    }

/*    public void reloadFunctionLibrary(String namespace) {
        checkNotNull(namespace);
        FunctionLibrary newLibrary = nodeLibrary.getFunctionRepository().getLibrary(namespace).reload();
        functionRepository = nodeLibrary.getFunctionRepository().withLibraryAdded(newLibrary);
        nodeLibrary = nodeLibrary.withFunctionRepository(functionRepository);
    } */
    
    public void reloadFunctionRepository() {
        FunctionRepository functionRepository = nodeLibrary.getFunctionRepository();
        for (FunctionLibrary library : functionRepository.getLibraries()) {
            if (library == CoreFunctions.LIBRARY) continue;
            library.reload();
        }
    }

    public Node createNode(String parentPath, Node prototype) {
        Node parent = getNode(parentPath);
        String name = parent.uniqueName(prototype.getName());
        Node newNode = prototype.extend().withName(name);
        addNode(parentPath, newNode);
        return newNode;
    }

    public void setNodePosition(String nodePath, Point point) {
        Node newNode = getNode(nodePath).withPosition(point);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setRenderedChild(String parentPath, String nodeName) {
        Node newParent = getNode(parentPath).withRenderedChildName(nodeName);
        replaceNodeInPath(parentPath, newParent);
    }

    public Node addNode(String parentPath, Node node) {
        Node parent = getNode(parentPath);
        if (parent.hasChild(node.getName())) {
            String uniqueName = parent.uniqueName(node.getName());
            node = node.withName(uniqueName);
        }
        Node newParent = getNode(parentPath).withChildAdded(node);
        replaceNodeInPath(parentPath, newParent);
        return node;
    }

    public List<Node> pasteNodes(String parentPath, Iterable<Node> nodes) {
        Node parent = getNode(parentPath);

        Map<String, String> newNames = new HashMap<String, String>();


        ImmutableList.Builder<Node> b = new ImmutableList.Builder<Node>();
        for (Node node : nodes) {
            Node newNode = node.withPosition(node.getPosition().moved(20, 80));
            newNode = addNode(parentPath, newNode);
            b.add(newNode);
            newNames.put(node.getName(), newNode.getName());
        }

        parent = getNode(parentPath);
        for (Connection c : parent.getConnections()) {
            boolean makeConnection = false;
            String outputNodeName = c.getOutputNode();
            String inputNodeName = c.getInputNode();
            if (newNames.containsKey(outputNodeName)) {
                outputNodeName = newNames.get(outputNodeName);
            }
            if (newNames.containsKey(inputNodeName)) {
                inputNodeName = newNames.get(inputNodeName);
                makeConnection = true;
            }
            if (makeConnection) {
                Node outputNode = parent.getChild(outputNodeName);
                Node inputNode = parent.getChild(inputNodeName);
                Port inputPort = inputNode.getInput(c.getInputPort());
                connect("/", outputNode, inputNode, inputPort);
            }
        }

        return b.build();
    }

    public void removeNode(String parentPath, String nodeName) {
        String renderedChild = getNode(parentPath).getRenderedChildName();
        Node newParent = getNode(parentPath).disconnect(nodeName).withChildRemoved(nodeName);
        if (renderedChild.equals(nodeName))
            newParent = newParent.withRenderedChild(null);
        replaceNodeInPath(parentPath, newParent);
    }

    public void removePort(String nodePath, String portName) {
        Node newNode = getNode(nodePath).withInputRemoved(portName);
        replaceNodeInPath(nodePath, newNode);
    }

    public void renameNode(String parentPath, String nodePath, String newName) {
        List<Connection> connections = getNode(parentPath).getConnections();
        String oldName = getNode(nodePath).getName();

        Node newNode = getNode(nodePath).withName(newName);
        removeNode(parentPath, oldName);
        addNode(parentPath, newNode);

        for (Connection c : connections) {
            if (c.getInputNode().equals(oldName)) {
                Node newParent = getNode(parentPath).connect(c.getOutputNode(), newNode.getName(), c.getInputPort());
                replaceNodeInPath(parentPath, newParent);
            } else if (c.getOutputNode().equals(oldName)) {
                Node newParent = getNode(parentPath).connect(newNode.getName(), c.getInputNode(), c.getInputPort());
                replaceNodeInPath(parentPath, newParent);
            }
        }
    }

    public void setPortValue(String nodePath, String portName, Object value) {
        Node newNode = getNode(nodePath).withInputValue(portName, value);
        replaceNodeInPath(nodePath, newNode);
    }

    public void connect(String parentPath, Node outputNode, Node inputNode, Port inputPort) {
        Node newParent = getNode(parentPath).connect(outputNode.getName(), inputNode.getName(), inputPort.getName());
        replaceNodeInPath(parentPath, newParent);
    }

    public void disconnect(String parentPath, Connection connection) {
        Node newParent = getNode(parentPath).disconnect(connection);
        replaceNodeInPath(parentPath, newParent);
    }

    /**
     * Replace the node at the given path with the new node.
     * Afterwards, the nodeLibrary field is set to the new NodeLibrary.
     *
     * @param nodePath The node path. This path needs to exist.
     * @param node     The new node to put in place of the old node.
     */
    public void replaceNodeInPath(String nodePath, Node node) {
        checkArgument(nodePath.startsWith("/"), "Node path needs to be an absolute path, starting with '/'.");
        nodePath = nodePath.substring(1);
        Node newRoot;
        if (nodePath.isEmpty()) {
            newRoot = node;
        } else {
            // TODO Recursively replace nodes at higher levels.
            checkArgument(!nodePath.contains("/"), "Subpaths are not supported yet.");
            newRoot = nodeLibrary.getRoot().withChildReplaced(nodePath, node);
        }
        nodeLibrary = nodeLibrary.withRoot(newRoot);
    }

}
