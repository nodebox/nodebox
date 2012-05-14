package nodebox.client;

import com.google.common.collect.ImmutableList;
import nodebox.function.*;
import nodebox.graphics.ObjectsRenderer;
import nodebox.handle.Handle;
import nodebox.handle.HandleDelegate;
import nodebox.movie.Movie;
import nodebox.movie.VideoFormat;
import nodebox.node.*;
import nodebox.node.MenuItem;
import nodebox.ui.*;
import nodebox.util.FileUtils;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.*;

/**
 * A NodeBoxDocument manages a NodeLibrary.
 */
public class NodeBoxDocument extends JFrame implements WindowListener, HandleDelegate {

    private static final Logger LOG = Logger.getLogger(NodeBoxDocument.class.getName());
    private static final String WINDOW_MODIFIED = "windowModified";

    public static String lastFilePath;
    public static String lastExportPath;

    private static NodeClipboard nodeClipboard;

    private File documentFile;
    private boolean documentChanged;
    private AnimationTimer animationTimer;
    private boolean loaded = false;

    private UndoManager undoManager = new UndoManager();
    private boolean holdEdits = false;
    private String lastEditType = null;
    private String lastEditObjectId = null;

    // State
    private final NodeLibraryController controller;
    private FunctionRepository functionRepository;
    private String activeNetworkPath = "";
    private String activeNodeName = "";
    private boolean restoring = false;
    private boolean invalidateFunctionRepository = false;
    private double frame;

    // Rendering
    private final AtomicBoolean isRendering = new AtomicBoolean(false);
    private final AtomicBoolean shouldRender = new AtomicBoolean(false);
    private final ExecutorService renderService;
    private Future currentRender = null;
    private Iterable<?> lastRenderResult = null;

    // GUI components
    private final NodeBoxMenuBar menuBar;
    private final AnimationBar animationBar;
    private final AddressBar addressBar;
    private final ViewerPane viewerPane;
    private final DataSheet dataSheet;
    private final PortView portView;
    private final NetworkPane networkPane;
    private final NetworkView networkView;
    private JSplitPane parameterNetworkSplit;
    private JSplitPane topSplit;
    private final ProgressPanel progressPanel;

    public static NodeBoxDocument getCurrentDocument() {
        return Application.getInstance().getCurrentDocument();
    }

    private static NodeLibrary createNewLibrary() {
        NodeRepository nodeRepository = Application.getInstance().getSystemRepository();
        Node root = Node.ROOT.withName("root");
        Node rectPrototype = nodeRepository.getNode("corevector.rect");
        String name = root.uniqueName(rectPrototype.getName());
        Node rect1 = rectPrototype.extend().withName(name).withPosition(new nodebox.graphics.Point(20, 20));
        root = root.withChildAdded(rect1).withRenderedChild(rect1);
        return NodeLibrary.create("untitled", root, nodeRepository, FunctionRepository.of());
    }

    public NodeBoxDocument() {
        this(createNewLibrary());
    }

    public NodeBoxDocument(NodeLibrary nodeLibrary) {
        renderService = Executors.newFixedThreadPool(1, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "node-renderer");
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
        controller = NodeLibraryController.withLibrary(nodeLibrary);
        invalidateFunctionRepository = true;
        JPanel rootPanel = new JPanel(new BorderLayout());
        this.viewerPane = new ViewerPane(this);
        dataSheet = viewerPane.getDataSheet();
        PortPane portPane = new PortPane(this);
        portView = portPane.getPortView();
        networkPane = new NetworkPane(this);
        networkView = networkPane.getNetworkView();
        parameterNetworkSplit = new CustomSplitPane(JSplitPane.VERTICAL_SPLIT, portPane, networkPane);
        topSplit = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewerPane, parameterNetworkSplit);

        addressBar = new AddressBar();
        addressBar.setOnSegmentClickListener(new AddressBar.OnSegmentClickListener() {
            public void onSegmentClicked(String fullPath) {
                setActiveNetwork(fullPath);
            }
        });
        progressPanel = new ProgressPanel(this);
        JPanel addressPanel = new JPanel(new BorderLayout());
        addressPanel.add(addressBar, BorderLayout.CENTER);
        addressPanel.add(progressPanel, BorderLayout.EAST);

        rootPanel.add(addressPanel, BorderLayout.NORTH);
        rootPanel.add(topSplit, BorderLayout.CENTER);

        // Animation properties.
        animationTimer = new AnimationTimer(this);
        animationBar = new AnimationBar(this);
        rootPanel.add(animationBar, BorderLayout.SOUTH);

        setContentPane(rootPanel);
        setLocationByPlatform(true);
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        updateTitle();
        menuBar = new NodeBoxMenuBar(this);
        setJMenuBar(menuBar);
        loaded = true;
    }

    public NodeBoxDocument(File file) throws RuntimeException {
        this(NodeLibrary.load(file, Application.getInstance().getSystemRepository()));
        lastFilePath = file.getParentFile().getAbsolutePath();
        setDocumentFile(file);
    }

    //// Node Library management ////

    public NodeLibrary getNodeLibrary() {
        return controller.getNodeLibrary();
    }

    public NodeRepository getNodeRepository() {
        return Application.getInstance().getSystemRepository();
    }

    public FunctionRepository getFunctionRepository() {
        if (invalidateFunctionRepository) {
            functionRepository = FunctionRepository.combine(getNodeRepository().getFunctionRepository(), getNodeLibrary().getFunctionRepository());
            invalidateFunctionRepository = false;
        }
        return functionRepository;
    }

    /**
     * Restore the node library to a different undo state.
     *
     * @param nodeLibrary The node library to restore.
     * @param networkPath The active network path.
     * @param nodeName    The active node name. Can be an empty string.
     */
    public void restoreState(NodeLibrary nodeLibrary, String networkPath, String nodeName) {
        controller.setNodeLibrary(nodeLibrary);
        invalidateFunctionRepository = true;
        restoring = true;
        setActiveNetwork(networkPath);
        setActiveNode(nodeName);
        restoring = false;
    }

    //// Node operations ////

    /**
     * Create a node in the active network.
     * This node is based on a prototype.
     *
     * @param prototype The prototype node.
     * @param pt        The initial node position.
     */
    public void createNode(Node prototype, nodebox.graphics.Point pt) {
        startEdits("Create Node");
        Node newNode = controller.createNode(activeNetworkPath, prototype);
        String newNodePath = Node.path(activeNetworkPath, newNode);
        controller.setNodePosition(newNodePath, pt);
        controller.setRenderedChild(activeNetworkPath, newNode.getName());
        setActiveNode(newNode);
        stopEdits();

        Node activeNode = getActiveNode();
        networkView.updateNodes();
        networkView.singleSelect(activeNode);
        portView.updateAll();

        requestRender();
    }


    /**
     * Change the node position of the given node.
     *
     * @param node  The node to move.
     * @param point The point to move to.
     */
    public void setNodePosition(Node node, nodebox.graphics.Point point) {
        checkNotNull(node);
        checkNotNull(point);
        checkArgument(getActiveNetwork().hasChild(node));
        // Note that we're passing in the parent network of the node.
        // This means that all move changes to the parent network are grouped
        // together under one edit, instead of for each node individually.
        addEdit("Move Node", "moveNode", getActiveNetworkPath());
        String nodePath = Node.path(activeNetworkPath, node);
        controller.setNodePosition(nodePath, point);

        networkView.updatePosition(node);
    }

    /**
     * Change the node name.
     *
     * @param node The node to rename.
     * @param name The new node name.
     */
    public void setNodeName(Node node, String name) {
        checkNotNull(node);
        checkNotNull(name);
        controller.renameNode(activeNetworkPath, Node.path(activeNetworkPath, node), name);
        networkView.updateNodes();
        // Renaming the node can have an effect on expressions, so recalculate the network.
        requestRender();
    }

    /**
     * Change the description for the node.
     *
     * @param node        The node to change.
     * @param description The new description.
     */
    public void setNodeDescription(Node node, String description) {
        checkNotNull(node);
        checkNotNull(description);
        addEdit("Set Node Description");
        String nodePath = Node.path(activeNetworkPath, node);
        controller.setNodeDescription(nodePath, description);
    }

    /**
     * Change the node image icon.
     *
     * @param node  The node to change.
     * @param image The new image icon.
     */
    public void setNodeImage(Node node, String image) {
        checkNotNull(node);
        checkNotNull(image);
        addEdit("Set Node Image");
        String nodePath = Node.path(activeNetworkPath, node);
        controller.setNodeImage(nodePath, image);
        networkView.updateNodes();
    }

    /**
     * Change the output type for the node.
     *
     * @param node       The node to change.
     * @param outputType The new output type.
     */
    public void setNodeOutputType(Node node, String outputType) {
        checkNotNull(node);
        checkNotNull(outputType);
        addEdit("Set Output Type");
        String nodePath = Node.path(activeNetworkPath, node);
        controller.setNodeOutputType(nodePath, outputType);
        networkView.updateNodes();
    }

    /**
     * Change the output range for the node.
     *
     * @param node        The node to change.
     * @param outputRange The new output range.
     */
    public void setNodeOutputRange(Node node, Port.Range outputRange) {
        checkNotNull(node);
        addEdit("Change Node Output Range");
        String nodePath = Node.path(activeNetworkPath, node);
        controller.setNodeOutputRange(nodePath, outputRange);
        requestRender();
    }

    /**
     * Change the node function.
     *
     * @param node  The node to change.
     * @param function The new function.
     */
    public void setNodeFunction(Node node, String function) {
        checkNotNull(node);
        checkNotNull(function);
        addEdit("Set Node Function");
        String nodePath = Node.path(activeNetworkPath, node);
        controller.setNodeFunction(nodePath, function);
        networkView.updateNodes();
        requestRender();
    }

    /**
     * Change the node handle function.
     *
     * @param node  The node to change.
     * @param handle The new handle function.
     */
    public void setNodeHandle(Node node, String handle) {
        checkNotNull(node);
        checkNotNull(handle);
        addEdit("Set Node Handle");
        String nodePath = Node.path(activeNetworkPath, node);
        controller.setNodeHandle(nodePath, handle);
        createHandleForActiveNode();
        networkView.updateNodes();
        requestRender();
    }

    /**
     * Set the node metadata to the given metadata.
     * Note that this method is not called when the node position or name changes.
     *
     * @param node     The node to change.
     * @param metadata A map of metadata.
     */
    public void setNodeMetadata(Node node, Object metadata) {
        // TODO: Implement
        // TODO: Make NodeAttributesEditor use this.
        // Metadata changes could mean the icon has changed.
        networkView.updateNodes();
        if (node == getActiveNode()) {
            portView.updateAll();
            // Updating the metadata could cause changes to a handle.
            viewerPane.repaint();
            dataSheet.repaint();
        }
        requestRender();
    }

    /**
     * Change the rendered node to the given node
     *
     * @param node the node to set rendered
     */
    public void setRenderedNode(Node node) {
        checkNotNull(node);
        checkArgument(getActiveNetwork().hasChild(node));
        addEdit("Set Rendered");
        controller.setRenderedChild(activeNetworkPath, node.getName());

        networkView.updateNodes();
        networkView.singleSelect(node);
        requestRender();
    }

    public void setNodeExported(Node node, boolean exported) {
        throw new UnsupportedOperationException("Not implemented yet.");
        //addEdit("Set Exported");
    }

    /**
     * Remove the given node from the active network.
     *
     * @param node The node to remove.
     */
    public void removeNode(Node node) {
        addEdit("Remove Node");
        removeNodeImpl(node);
        networkView.updateAll();
        requestRender();
    }

    /**
     * Remove the given nodes from the active network.
     *
     * @param nodes The node to remove.
     */
    public void removeNodes(Iterable<Node> nodes) {
        addEdit("Delete Nodes");
        for (Node node : nodes) {
            removeNodeImpl(node);
        }
        networkView.updateAll();
        portView.updateAll();
        requestRender();
    }

    /**
     * Helper method used by removeNode and removeNodes to do the removal and update the port view, if needed.
     *
     * @param node The node to remove.
     */
    private void removeNodeImpl(Node node) {
        checkNotNull(node, "Node to remove cannot be null.");
        checkArgument(getActiveNetwork().hasChild(node), "Node to remove is not in active network.");
        controller.removeNode(activeNetworkPath, node.getName());
        // If the removed node was the active one, reset the port view.
        if (node == getActiveNode()) {
            setActiveNode((Node) null);
        }
    }

    /**
     * Create a connection from the given output to the given input.
     *
     * @param outputNode The output node.
     * @param inputNode  The input node.
     * @param inputPort  The input port.
     */
    public void connect(Node outputNode, Node inputNode, Port inputPort) {
        addEdit("Connect");
        controller.connect(activeNetworkPath, outputNode, inputNode, inputPort);

        portView.updateAll();
        viewerPane.updateHandle();
        requestRender();
    }

    /**
     * Remove the given connection from the network.
     *
     * @param connection the connection to remove
     */
    public void disconnect(Connection connection) {
        addEdit("Disconnect");
        controller.disconnect(activeNetworkPath, connection);

        portView.updateAll();
        networkView.updateConnections();
        viewerPane.updateHandle();
        requestRender();
    }

    /**
     * @param node          the node on which to add the port
     * @param portName the name of the new port
     * @param portType the type of the new port
     */
    public void addPort(Node node, String portName, String portType) {
        checkArgument(getActiveNetwork().hasChild(node));
        addEdit("Add Port");
        controller.addPort(Node.path(activeNetworkPath, node), portName, portType);
        portView.updateAll();
        networkView.updateAll();
    }

    /**
     * Remove the port from the node.
     *
     * @param node     The node on which to remove the port.
     * @param portName The name of the port
     */
    public void removePort(Node node, String portName) {
        checkArgument(getActiveNetwork().hasChild(node));
        addEdit("Remove Port");
        controller.removePort(Node.path(activeNetworkPath, node), portName);

        if (node == getActiveNode()) {
            portView.updateAll();
            viewerPane.repaint();
            dataSheet.repaint();
        }
    }

    /**
     * Change the widget for the given port
     *
     * @param portName The name of the port to change.
     * @param widget   The new widget.
     */
    public void setPortWidget(String portName, Port.Widget widget) {
        checkValidPort(portName);
        addEdit("Change Widget");
        controller.setPortWidget(getActiveNodePath(), portName, widget);
        portView.updateAll();
        requestRender();
    }

    /**
     * Change the port range of the given port
     *
     * @param portName The name of the port to change.
     * @param range    The new port range.
     */
    public void setPortRange(String portName, Port.Range range) {
        checkValidPort(portName);
        addEdit("Change Port Range");
        controller.setPortRange(getActiveNodePath(), portName, range);
        requestRender();
    }

    /**
     * Change the minimum value for the given port
     *
     * @param portName     The name of the port to change.
     * @param minimumValue The new minimum value.
     */
    public void setPortMinimumValue(String portName, Double minimumValue) {
        checkValidPort(portName);
        addEdit("Change Minimum Value");
        controller.setPortMinimumValue(getActiveNodePath(), portName, minimumValue);
        portView.updateAll();
        requestRender();
    }

    /**
     * Change the maximum value for the given port
     *
     * @param portName     The name of the port to change.
     * @param maximumValue The new maximum value.
     */
    public void setPortMaximumValue(String portName, Double maximumValue) {
        checkValidPort(portName);
        addEdit("Change Maximum Value");
        controller.setPortMaximumValue(getActiveNodePath(), portName, maximumValue);
        portView.updateAll();
        requestRender();
    }

    /**
     * Add a new menu item for the given port's menu.
     *
     * @param portName The name of the port to add a new menu item for.
     * @param key      The key of the new menu item.
     * @param label    The label of the new menu item.
     */
    public void addPortMenuItem(String portName, String key, String label) {
        checkValidPort(portName);
        addEdit("Add Port Menu Item");

        controller.addPortMenuItem(getActiveNodePath(), portName, key, label);

        portView.updateAll();
        requestRender();
    }

    /**
     * Remove a menu item from the given port's menu.
     *
     * @param portName The name of the port to remove the menu item from.
     * @param item      The menu item to remove
     */
    public void removePortMenuItem(String portName, MenuItem item) {
        checkValidPort(portName);
        addEdit("Remove Parameter Menu Item");

        controller.removePortMenuItem(getActiveNodePath(), portName, item);

        Node n = getActiveNode();
        portView.updateAll();
        requestRender();
    }

    /**
     * Move a menu item down from the given port's menu.
     *
     * @param portName  The name of the port of which to update the menu.
     * @param itemIndex The index of the menu item to move down.
     */
    public void movePortMenuItemDown(String portName, int itemIndex) {
        checkValidPort(portName);
        addEdit("Move Port Item Down");
        controller.movePortMenuItem(getActiveNodePath(), portName, itemIndex, false);
        portView.updateAll();
    }

    /**
     * Move a menu item up from the given port's menu.
     *
     * @param portName  The name of the port of which to update the menu.
     * @param itemIndex The index of the menu item to move up.
     */
    public void movePortMenuItemUp(String portName, int itemIndex) {
        checkValidPort(portName);
        addEdit("Move Port Item Up");
        controller.movePortMenuItem(getActiveNodePath(), portName, itemIndex, true);
        portView.updateAll();
    }

    /**
     * Change a menu item's key and label in the given port's menu.
     *
     * @param portName  The name of the port of which to update the menu.
     * @param itemIndex The index of the menu item to change.
     * @param key       The new key of the menu item.
     * @param label     The new label of the menu item.
     */
    public void updatePortMenuItem(String portName, int itemIndex, String key, String label) {
        checkValidPort(portName);
        addEdit("Update Port Menu Item");
        controller.updatePortMenuItem(getActiveNodePath(), portName, itemIndex, key, label);
        portView.updateAll();
    }

    public Object getValue(String portName) {
        Port port = checkValidPort(portName);
        return port.getValue();
    }

    /**
     * Set the port of the active node to the given value.
     *
     * @param portName The name of the port on the active node.
     * @param value    The new value.
     */
    public void setValue(String portName, Object value) {
        Port port = checkValidPort(portName);
        addEdit("Change Value", "changeValue", getActiveNodePath() + "#" + portName);

        controller.setPortValue(getActiveNodePath(), portName, value);

        // TODO set variables on the root port.
//        if (port.getNode() == nodeLibrary.getRoot()) {
//            nodeLibrary.setVariable(port.getName(), port.asString());
//        }

        portView.updatePortValue(port, value);
        // Setting a port might change enable expressions, and thus change the enabled state of a port row.
        portView.updateEnabledState();
        // Setting a port might change the enabled state of the handle.
        // viewer.setHandleEnabled(activeNode != null && activeNode.hasEnabledHandle());
        requestRender();
    }

    public void revertPortToDefault(Port port) {
        addEdit("Revert Port to Default");
        throw new UnsupportedOperationException("Not implemented yet.");

        //portView.updatePort(parameter);
        //renderNetwork();
    }

    public void setPortMetadata(Port port, String key, String value) {
        addEdit("Change Port Metadata");
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private Port checkValidPort(String portName) {
        checkNotNull(portName, "Port cannot be null.");
        Port port = getActiveNode().getInput(portName);
        checkArgument(port != null, "Port %s does not exist on node %s", portName, getActiveNode());
        return port;
    }

    //// Port pane callbacks ////

    public void editMetadata() {
        if (getActiveNode() == null) return;
        JDialog editorDialog = new NodeAttributesDialog(NodeBoxDocument.this);
        editorDialog.setSize(580, 751);
        editorDialog.setLocationRelativeTo(NodeBoxDocument.this);
        editorDialog.setVisible(true);
    }

    //// HandleDelegate implementation ////

    public void silentSet(String portName, Object value) {
        try {
            Port port = getActiveNode().getInput(portName);
            setValue(portName, value);
        } catch (Exception ignored) {
        }
    }

    // TODO Merge stopEditing and stopCombiningEdits.

    public void stopEditing() {
        stopCombiningEdits();
    }

    public void updateHandle() {
        if (viewerPane.getHandle() != null)
            viewerPane.getHandle().update();
        // TODO Make viewer repaint more fine-grained.
        viewerPane.repaint();
    }

    //// Active network / node ////

    /**
     * Return the network that is currently "open": shown in the network view.
     *
     * @return The currently active network.
     */
    public Node getActiveNetwork() {
        // TODO This might be a potential bottleneck.
        return getNodeLibrary().getNodeForPath(activeNetworkPath);
    }

    public String getActiveNetworkPath() {
        return activeNetworkPath;
    }

    public void setActiveNetwork(String path) {
        checkNotNull(path);
        activeNetworkPath = path;
        Node network = getNodeLibrary().getNodeForPath(path);

        if (! restoring) {
            if (network.getRenderedChild() != null) {
                setActiveNode(network.getRenderedChildName());
            } else if (!network.isEmpty()) {
                // Set the active node to the first child.
                setActiveNode(network.getChildren().iterator().next());
            } else {
                setActiveNode((Node) null);
            }
        }

        addressBar.setPath(activeNetworkPath);
        //viewer.setHandleEnabled(activeNode != null && activeNode.hasEnabledHandle());
        networkView.updateNodes();
        if (! restoring)
            networkView.singleSelect(getActiveNode());
        viewerPane.repaint();
        dataSheet.repaint();

        requestRender();
    }

    /**
     * Set the active network to the parent network.
     */
    public void goUp() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }


    /**
     * Return the node that is currently focused:
     * visible in the port view, and whose handles are displayed in the viewer.
     *
     * @return The active node. Can be null.
     */
    public Node getActiveNode() {
        if (activeNodeName.isEmpty()) {
            return getActiveNetwork();
        } else {
            return getNodeLibrary().getNodeForPath(getActiveNodePath());
        }
    }

    public String getActiveNodePath() {
        return Node.path(activeNetworkPath, activeNodeName);
    }

    public String getActiveNodeName() {
        return activeNodeName;
    }

    /**
     * Set the active node to the given node.
     * <p/>
     * The active node is the one whose parameters are displayed in the port pane,
     * and whose handle is displayed in the viewer.
     * <p/>
     * This will also change the active network if necessary.
     *
     * @param node the node to change to.
     */
    public void setActiveNode(Node node) {
        setActiveNode(node != null ? node.getName() : "");
    }

    public void setActiveNode(String nodeName) {
        if (!restoring && getActiveNodeName().equals(nodeName)) return;
        stopCombiningEdits();
        if (nodeName.isEmpty()) {
            activeNodeName = "";
        } else {
            checkArgument(getActiveNetwork().hasChild(nodeName));
            activeNodeName = nodeName;
        }

        Node n = getActiveNode();
        createHandleForActiveNode();
        //editorPane.setActiveNode(activeNode);
        // TODO If we draw handles again, we should repaint the viewer pane.
        //viewerPane.repaint(); // For the handle
        portView.updateAll();
        restoring = false;
        networkView.singleSelect(n);
    }

    private void createHandleForActiveNode() {
        Node activeNode = getActiveNode();
        if (activeNode != null) {
            Handle handle = null;

            if (getFunctionRepository().hasFunction(activeNode.getHandle())) {
                Function handleFunction = getFunctionRepository().getFunction(activeNode.getHandle());
                try {
                    handle = (Handle) handleFunction.invoke();
                } catch (Exception e) {
                    // todo: error reporting
                }
            }

            if (handle != null) {
                handle.setHandleDelegate(this);
                handle.update();
                viewerPane.setHandle(handle);
            } else {
                viewerPane.setHandle(null);
            }
        }
    }
//        if (activeNode != null) {
//            Handle handle = null;
//            try {
//                handle = activeNode.createHandle();
//                // If the handle was created successfully, remove the messages.
//                editorPane.clearMessages();
//            } catch (Exception e) {
//                editorPane.setMessages(e.toString());
//            }
//            if (handle != null) {
//                handle.setHandleDelegate(this);
//                // TODO Remove this. Find out why the handle needs access to the viewer (only repaint?) and put that in the HandleDelegate.
//                handle.setViewer(viewer);
//                viewer.setHandleEnabled(activeNode.hasEnabledHandle());
//            }
//            viewer.setHandle(handle);
//        } else {
//            viewer.setHandle(null);
//        }
//    }

    // todo: this method feels like it doesn't belong here (maybe rename it?)
    public boolean hasInput(String portName) {
        Node node = getActiveNode();
        return node.hasInput(portName);
    }

    public boolean isConnected(Port p) {
        return isConnected(p.getName());
    }

    public boolean isConnected(String portName) {
        Node network = getActiveNetwork();
        Node node = getActiveNode();
        for (Connection c : network.getConnections()) {
            if (c.getInputNode().equals(node.getName()) && c.getInputPort().equals(portName))
                return true;
        }
        return false;
    }


    //// Animation ////

    public double getFrame() {
        return frame;
    }

    public void setFrame(double frame) {
        this.frame = frame;

        animationBar.setFrame(frame);
        requestRender();
    }

    public void nextFrame() {
        setFrame(getFrame() + 1);
    }

    public void playAnimation() {
        animationTimer.start();
    }

    public void stopAnimation() {
        animationTimer.stop();
    }

    public void rewindAnimation() {
        stopAnimation();
        setFrame(1);
    }

    //// Rendering ////

    /**
     * Request a renderNetwork operation.
     * <p/>
     * This method does a number of checks to see if the renderNetwork goes through.
     * <p/>
     * The renderer could already be running.
     * <p/>
     * If all checks pass, a renderNetwork request is made.
     */
    public synchronized void requestRender() {
        // If we're already rendering, request the next renderNetwork.
        if (isRendering.get()) {
            shouldRender.set(true);
        } else {
            // If we're not rendering, start rendering.
            render();
        }
    }


    /**
     * Called when the active network will start rendering.
     * Called on the Swing EDT so you can update the GUI.
     *
     * @param context The node context.
     */
    public synchronized void startRendering(final NodeContext context) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressPanel.setInProgress(true);
            }
        });
    }

    /**
     * Ask the document to stop the active rendering.
     */
    public synchronized void stopRendering() {
        if (currentRender != null) {
            currentRender.cancel(true);
        }
    }

    /**
     * Called when the active network has finished rendering.
     * Called on the Swing EDT so you can update the GUI.
     *
     * @param context         The node context.
     * @param renderedNetwork The network that was rendered.
     */
    public synchronized void finishedRendering(final NodeContext context, final Node renderedNetwork) {
        finishCurrentRender();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Node renderedChild = renderedNetwork.getRenderedChild();
                Iterable<?> results = context.getResults(renderedChild);
                lastRenderResult = results;
                viewerPane.setOutputValues(results);
                networkPane.clearError();
                networkView.checkErrorAndRepaint();
            }
        });
    }

    private synchronized void finishedRenderingWithError(NodeContext context, Node network, final Exception e) {
        finishCurrentRender();
        lastRenderResult = null;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                networkPane.setError(e);
            }
        });
    }

    private synchronized void finishCurrentRender() {
        currentRender = null;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressPanel.setInProgress(false);
            }
        });
    }


    /**
     * Returns the first output value, or null if the map of output values is empty.
     *
     * @param outputValues The map of output values.
     * @return The output value.
     */
    private Object firstOutputValue(final Map<String, Object> outputValues) {
        if (outputValues.isEmpty()) return null;
        return outputValues.values().iterator().next();
    }

    private synchronized void render() {
        // If we're already rendering, return.
        if (isRendering.get()) return;

        // Before starting the renderNetwork, turn the "should render" flag off and the "is rendering" flag on.
        synchronized (shouldRender) {
            synchronized (isRendering) {
                shouldRender.set(false);
                isRendering.set(true);
            }
        }

        final NodeLibrary renderLibrary = getNodeLibrary();
        final Node renderNetwork = getActiveNetwork();
        checkState(currentRender == null, "Another render is still in progress.");
        currentRender = renderService.submit(new Runnable() {
            public void run() {
                final NodeContext context = new NodeContext(renderLibrary, getFunctionRepository(), frame);
                Exception renderException = null;
                startRendering(context);
                try {
                    context.renderNetwork(renderNetwork);
                } catch (NodeRenderException e) {
                    LOG.log(Level.WARNING, "Error while processing", e);
                    renderException = e;
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Other error while processing", e);
                    renderException = e;
                }

                // We finished rendering so set the renderNetwork flag off.
                isRendering.set(false);

                if (renderException == null) {
                    finishedRendering(context, renderNetwork);

                    // If, in the meantime, we got a new renderNetwork request, call the renderNetwork method again.
                    if (shouldRender.get()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                render();
                            }
                        });
                    }
                } else {
                    finishedRenderingWithError(context, renderNetwork, renderException);
                }
            }
        });
    }

    //// Undo ////

    /**
     * Edits are no longer recorded until you call stopEdits. This allows you to batch edits.
     *
     * @param command the command name of the edit batch
     */
    public void startEdits(String command) {
        addEdit(command);
        holdEdits = true;
    }

    /**
     * Edits are recorded again.
     */
    public void stopEdits() {
        holdEdits = false;
    }

    /**
     * Add an edit to the undo manager.
     * <p/>
     * Since we don't specify the edit type or name, further edits will not be staggered.
     *
     * @param command the command name.
     */
    public void addEdit(String command) {
        if (!holdEdits) {
            markChanged();
            undoManager.addEdit(new NodeLibraryUndoableEdit(this, command));
            menuBar.updateUndoRedoState();
            stopCombiningEdits();
        }
    }

    /**
     * Add an edit to the undo manager.
     *
     * @param command the command name.
     * @param type    the type of edit
     * @param objectId  the id for the edited object. This will be compared against.
     */
    public void addEdit(String command, String type, String objectId) {
        if (!holdEdits) {
            markChanged();

            if (lastEditType != null && lastEditType.equals(type) && lastEditObjectId.equals(objectId)) {
                // If the last edit type and last edit id are the same,
                // we combine the two edits into one.
                // Since we've already saved the last state, we don't need to do anything.
            } else {
                addEdit(command);
                lastEditType = type;
                lastEditObjectId = objectId;
            }
        }
    }

    /**
     * Normally edits of the same type and object are combined into one.
     * Calling this method will ensure that you create a  new edit.
     * <p/>
     * Use this method e.g. for breaking apart overzealous edit grouping.
     */
    public void stopCombiningEdits() {
        // We just reset the last edit type and object so that addEdit will be forced to create a new edit.
        lastEditType = null;
        lastEditObjectId = null;
        stopEdits();
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void undo() {
        if (!undoManager.canUndo()) return;
        undoManager.undo();
        menuBar.updateUndoRedoState();
    }

    public void redo() {
        if (!undoManager.canRedo()) return;
        undoManager.redo();
        menuBar.updateUndoRedoState();
    }

    //// Code editor actions ////

    public void fireCodeChanged(Node node, boolean changed) {
        networkView.codeChanged(node, changed);
    }

    //// Document actions ////

    public File getDocumentFile() {
        return documentFile;
    }

    public void setDocumentFile(File documentFile) {
        this.documentFile = documentFile;
        updateTitle();
    }

    public boolean isChanged() {
        return documentChanged;
    }

    public boolean close() {
        stopAnimation();
        if (shouldClose()) {
            Application.getInstance().removeDocument(this);
            dispose();
            // On Mac the application does not close if the last window is closed.
            if (!Platform.onMac()) {
                // If there are no more documents, exit the application.
                if (Application.getInstance().getDocumentCount() == 0) {
                    System.exit(0);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean shouldClose() {
        if (isChanged()) {
            SaveDialog sd = new SaveDialog();
            int retVal = sd.show(this);
            if (retVal == JOptionPane.YES_OPTION) {
                return save();
            } else if (retVal == JOptionPane.NO_OPTION) {
                return true;
            } else if (retVal == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }
        return true;
    }

    public boolean save() {
        if (documentFile == null) {
            return saveAs();
        } else {
            return saveToFile(documentFile);
        }
    }

    public boolean saveAs() {
        File chosenFile = FileUtils.showSaveDialog(this, lastFilePath, "ndbx", "NodeBox File");
        if (chosenFile != null) {
            if (!chosenFile.getAbsolutePath().endsWith(".ndbx")) {
                chosenFile = new File(chosenFile.getAbsolutePath() + ".ndbx");
            }
            lastFilePath = chosenFile.getParentFile().getAbsolutePath();
            setDocumentFile(chosenFile);
            NodeBoxMenuBar.addRecentFile(documentFile);
            return saveToFile(documentFile);
        }
        return false;
    }

    public void revert() {
        // TODO: Implement revert
        JOptionPane.showMessageDialog(this, "Revert is not implemented yet.", "NodeBox", JOptionPane.ERROR_MESSAGE);
    }

    private boolean saveToFile(File file) {
        try {
            getNodeLibrary().store(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "An error occurred while saving the file.", "NodeBox", JOptionPane.ERROR_MESSAGE);
            LOG.log(Level.SEVERE, "An error occurred while saving the file.", e);
            return false;
        }
        documentChanged = false;
        updateTitle();
        return true;
    }

    private void markChanged() {
        if (!documentChanged && loaded) {
            documentChanged = true;
            updateTitle();
            getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.TRUE);
        }
    }

    private void updateTitle() {
        String postfix = "";
        if (!Platform.onMac()) {
            postfix = (documentChanged ? " *" : "");
        } else {
            getRootPane().putClientProperty("Window.documentModified", documentChanged);
        }
        if (documentFile == null) {
            setTitle("Untitled" + postfix);
        } else {
            setTitle(documentFile.getName() + postfix);
            getRootPane().putClientProperty("Window.documentFile", documentFile);
        }
    }

    public void focusNetworkView() {
        networkView.requestFocus();
    }

    //// Export ////

    public void doExport() {
        File chosenFile = FileUtils.showSaveDialog(this, lastExportPath, "pdf", "PDF file");
        if (chosenFile == null) return;
        lastExportPath = chosenFile.getParentFile().getAbsolutePath();
        if (chosenFile.getName().endsWith(".png")) {
            exportToFile(chosenFile, ImageFormat.PNG);
        } else {
            exportToFile(chosenFile, ImageFormat.PDF);
        }
    }

    private void exportToFile(File file, ImageFormat format) {
        // get data from last export.
        if (lastRenderResult == null) {
            JOptionPane.showMessageDialog(this, "There is no last render result.");
        } else {
            exportToFile(file, lastRenderResult, format);
        }
    }

    private void exportToFile(File file, Iterable<?> objects, ImageFormat format) {
        file = format.ensureFileExtension(file);
        ObjectsRenderer.render(objects, file);
    }

    public boolean exportRange() {
        File exportDirectory = lastExportPath == null ? null : new File(lastExportPath);
        if (exportDirectory != null && !exportDirectory.exists())
            exportDirectory = null;
        ExportRangeDialog d = new ExportRangeDialog(this, exportDirectory);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        if (!d.isDialogSuccessful()) return false;
        String exportPrefix = d.getExportPrefix();
        File directory = d.getExportDirectory();
        int fromValue = d.getFromValue();
        int toValue = d.getToValue();
        nodebox.ui.ImageFormat format = d.getFormat();
        if (directory == null) return false;
        lastExportPath = directory.getAbsolutePath();
        exportRange(exportPrefix, directory, fromValue, toValue, format);
        return true;
    }

    public void exportRange(final String exportPrefix, final File directory, final int fromValue, final int toValue, final ImageFormat format) {
        exportThreadedRange(getNodeLibrary(), fromValue, toValue, new ExportDelegate() {
            @Override
            public void frameDone(double frame, Iterable<?> results) {
                File exportFile = new File(directory, exportPrefix + "-" + frame);
                exportToFile(exportFile, results, format);
            }
        });
    }

    public boolean exportMovie() {
        ExportMovieDialog d = new ExportMovieDialog(this, lastExportPath == null ? null : new File(lastExportPath));
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        if (!d.isDialogSuccessful()) return false;
        File chosenFile = d.getExportPath();
        if (chosenFile != null) {
            lastExportPath = chosenFile.getParentFile().getAbsolutePath();
            exportToMovieFile(chosenFile, d.getVideoFormat(), d.getFromValue(), d.getToValue());
            return true;
        }
        return false;
    }

    private void exportToMovieFile(File file, final VideoFormat videoFormat, final int fromValue, final int toValue) {
        file = videoFormat.ensureFileExtension(file);
        final long width = getNodeLibrary().getRoot().getInput("width").intValue();
        final long height = getNodeLibrary().getRoot().getInput("height").intValue();
        final Movie movie = new Movie(file.getAbsolutePath(), videoFormat, (int) width, (int) height, false);
        exportThreadedRange(controller.getNodeLibrary(), fromValue, toValue, new ExportDelegate() {
            @Override
            public void frameDone(double frame, Iterable<?> results) {
                movie.addFrame(ObjectsRenderer.createImage(results));
            }

            @Override
            void exportDone() {
                progressDialog.setTitle("Converting frames to movie...");
                progressDialog.reset();
                FramesWriter w = new FramesWriter(progressDialog);
                movie.save(w);
            }
        });
    }

    private abstract class ExportDelegate {
        protected InterruptibleProgressDialog progressDialog;

        void frameDone(double frame, Iterable<?> results) {
        }

        void exportDone() {
        }
    }

    private void exportThreadedRange(final NodeLibrary library, final int fromValue, final int toValue, final ExportDelegate exportDelegate) {
        int frameCount = toValue - fromValue;
        final InterruptibleProgressDialog d = new InterruptibleProgressDialog(this, "Exporting " + frameCount + " frames...");
        d.setTaskCount(toValue - fromValue + 1);
        d.setVisible(true);
        exportDelegate.progressDialog = d;

        final NodeLibrary exportLibrary = getNodeLibrary();
        final FunctionRepository exportFunctionRepository = getFunctionRepository();
        final Node exportNetwork = library.getRoot();
        final ExportViewer viewer = new ExportViewer();

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int frame = fromValue; frame <= toValue; frame++) {
                        if (Thread.currentThread().isInterrupted())
                            break;

                        NodeContext context = new NodeContext(exportLibrary, exportFunctionRepository, frame);
                        context.renderNetwork(exportNetwork);
                        Node renderedChild = exportNetwork.getRenderedChild();
                        Iterable<?> results = context.getResults(renderedChild);
                        viewer.setOutputValue(results);
                        exportDelegate.frameDone(frame, results);

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                d.tick();
                            }
                        });
                    }
                    exportDelegate.exportDone();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error while exporting", e);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            d.setVisible(false);
                            viewer.setVisible(false);
                        }
                    });
                }
            }
        });
        d.setThread(t);
        t.start();
        viewer.setVisible(true);
    }

    //// Copy / Paste ////

    private class NodeClipboard {
        private final Node network;
        private final ImmutableList<Node> nodes;

        private NodeClipboard(Node network, Iterable<Node> nodes) {
            this.network = network;
            this.nodes = ImmutableList.copyOf(nodes);
        }
    }

    public void cut() {
        copy();
        deleteSelection();
    }

    public void copy() {
        // When copying, save a reference to the nodes and the parent network.
        // Since the model is immutable, we don't need to make defensive copies.
        nodeClipboard = new NodeClipboard(getActiveNetwork(), networkView.getSelectedNodes());
    }

    public void paste() {
        addEdit("Paste node");
        if (nodeClipboard == null) return;
        List<Node> newNodes = controller.pasteNodes(activeNetworkPath, nodeClipboard.nodes);

        networkView.updateAll();
        networkView.select(newNodes);
        setActiveNode(newNodes.get(0));
    }

    public void deleteSelection() {
        java.util.List<Node> selectedNodes = networkView.getSelectedNodes();
        if (!selectedNodes.isEmpty()) {
            Node node = getActiveNode();
            if (node != null && selectedNodes.contains(node))
                viewerPane.setHandle(null);
            removeNodes(networkView.getSelectedNodes());
        }
        else if (networkView.hasSelectedConnection())
            networkView.deleteSelectedConnection();
    }

    /**
     * Start the dialog that allows a user to create a new node.
     */
    public void showNodeSelectionDialog() {
        NodeRepository repository = getNodeRepository();
        NodeSelectionDialog dialog = new NodeSelectionDialog(this, controller.getNodeLibrary(), repository);
        Point pt = networkView.getMousePosition();
        if (pt == null) {
            pt = new Point((int) (Math.random() * 300), (int) (Math.random() * 300));
        } else {
            pt.x -= NodeView.NODE_IMAGE_SIZE / 2;
            pt.y -= NodeView.NODE_IMAGE_SIZE / 2;
        }
        pt = (Point) networkView.getCamera().localToView(pt);
        dialog.setVisible(true);
        if (dialog.getSelectedNode() != null) {
            createNode(dialog.getSelectedNode(), new nodebox.graphics.Point(pt));
        }
    }

    public void showDocumentProperties() {
        DocumentPropertiesDialog dialog = new DocumentPropertiesDialog(this, getNodeLibrary().getFunctionRepository());
        dialog.setVisible(true);
        FunctionRepository functionRepository = dialog.getFunctionRepository();
        if (functionRepository != null) {
            addEdit("Change function repository");
            controller.setFunctionRepository(functionRepository);
            invalidateFunctionRepository = true;
            requestRender();
        }
    }

    public void reload() {
        controller.reloadFunctionRepository();
        requestRender();
        //editorPane.reload();
    }

    //// Window events ////

    public void windowOpened(WindowEvent e) {
        //viewEditorSplit.setDividerLocation(0.5);
        parameterNetworkSplit.setDividerLocation(0.5);
        topSplit.setDividerLocation(0.5);
    }

    public void windowClosing(WindowEvent e) {
        close();
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
        Application.getInstance().setCurrentDocument(this);
    }

    public void windowDeactivated(WindowEvent e) {
    }

    private class FramesWriter extends StringWriter {
        private final ProgressDialog dialog;

        public FramesWriter(ProgressDialog d) {
            super();
            dialog = d;
        }

        @Override
        public void write(String s, int n1, int n2) {
            super.write(s, n1, n2);
            if (s.startsWith("frame=")) {
                int frame = Integer.parseInt(s.substring(6, s.indexOf("fps")).trim());
                dialog.updateProgress(frame);
            }
        }
    }
}
