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

    public void setNodeDescription(String nodePath, String description) {
        Node newNode = getNode(nodePath).withDescription(description);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setNodeImage(String nodePath, String image) {
        Node newNode = getNode(nodePath).withImage(image);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setNodeOutputType(String nodePath, String outputType) {
        Node newNode = getNode(nodePath).withOutputType(outputType);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setNodeOutputRange(String nodePath, Port.Range outputRange) {
        Node newNode = getNode(nodePath).withOutputRange(outputRange);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setNodeFunction(String nodePath, String function) {
        Node newNode = getNode(nodePath).withFunction(function);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setNodeHandle(String nodePath, String handle) {
        Node newNode = getNode(nodePath).withHandle(handle);
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
            Node newNode = node.withPosition(node.getPosition().moved(4, 2));
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

    public void addPort(String nodePath, String portName, String portType) {
        Node newNode = getNode(nodePath).withInputAdded(Port.portForType(portName, portType));
        replaceNodeInPath(nodePath, newNode);
    }

    public void setPortWidget(String nodePath, String portName, Port.Widget widget) {
        Node node = getNode(nodePath);
        Port newPort = node.getInput(portName).withWidget(widget);
        Node newNode = node.withInputChanged(portName, newPort);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setPortRange(String nodePath, String portName, Port.Range range) {
        Node node = getNode(nodePath);
        Port newPort = node.getInput(portName).withRange(range);
        Node newNode = node.withInputChanged(portName, newPort);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setPortMinimumValue(String nodePath, String portName, Double minimumValue) {
        Node node = getNode(nodePath);
        Port newPort = node.getInput(portName).withMinimumValue(minimumValue);
        Node newNode = node.withInputChanged(portName, newPort);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setPortMaximumValue(String nodePath, String portName, Double maximumValue) {
        Node node = getNode(nodePath);
        Port newPort = node.getInput(portName).withMaximumValue(maximumValue);
        Node newNode = node.withInputChanged(portName, newPort);
        replaceNodeInPath(nodePath, newNode);
    }

    public void addPortMenuItem(String nodePath, String portName, String key, String label) {
        Node node = getNode(nodePath);
        Port port = node.getInput(portName);
        ImmutableList.Builder<MenuItem> b = ImmutableList.builder();
        b.addAll(port.getMenuItems());
        b.add(new MenuItem(key, label));
        Port newPort = port.withMenuItems(b.build());
        Node newNode = node.withInputChanged(portName, newPort);
        replaceNodeInPath(nodePath, newNode);
    }

    public void removePortMenuItem(String nodePath, String portName, MenuItem menuItem) {
        Node node = getNode(nodePath);
        Port port = node.getInput(portName);
        ImmutableList.Builder<MenuItem> b = ImmutableList.builder();
        for (MenuItem item : port.getMenuItems()) {
            if (item.equals(menuItem)) {
                // Do nothing
            } else {
                b.add(item);
            }
        }
        Port newPort = port.withMenuItems(b.build());
        Node newNode = node.withInputChanged(portName, newPort);
        replaceNodeInPath(nodePath, newNode);
    }

    public void movePortMenuItem(String nodePath, String portName, int index, boolean up) {
        Node node = getNode(nodePath);
        Port port = node.getInput(portName);
        List<MenuItem> menuItems = new ArrayList<MenuItem>(0);
        menuItems.addAll(port.getMenuItems());
        MenuItem item = menuItems.get(index);
        menuItems.remove(item);
        if (up)
            menuItems.add(index - 1, item);
        else
            menuItems.add(index + 1, item);
        Port newPort = port.withMenuItems(ImmutableList.copyOf(menuItems));
        Node newNode = node.withInputChanged(portName, newPort);
        replaceNodeInPath(nodePath, newNode);
    }

    public void updatePortMenuItem(String nodePath, String portName, int index, String key, String label) {
        Node node = getNode(nodePath);
        Port port = node.getInput(portName);
        List<MenuItem> menuItems = new ArrayList<MenuItem>(0);
        menuItems.addAll(port.getMenuItems());
        if (index < 0 || index >= menuItems.size()) return;
        menuItems.set(index, new MenuItem(key, label));
        Port newPort = port.withMenuItems(ImmutableList.copyOf(menuItems));
        Node newNode = node.withInputChanged(portName, newPort);
        replaceNodeInPath(nodePath, newNode);
    }

    public void setPortValue(String nodePath, String portName, Object value) {
        Node newNode = getNode(nodePath).withInputValue(portName, value);
        replaceNodeInPath(nodePath, newNode);
    }

    public void revertToDefaultPortValue(String nodePath, String portName) {
        Node node = getNode(nodePath);
        Port port = node.getPrototype().getInput(portName);
        if (port != null) {
            Node newNode = node.withInputValue(portName, port.getValue());
            replaceNodeInPath(nodePath, newNode);
        }
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
