package nodebox.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.client.devicehandler.DeviceHandler;
import nodebox.client.devicehandler.DeviceHandlerFactory;
import nodebox.function.Function;
import nodebox.function.FunctionRepository;
import nodebox.handle.Handle;
import nodebox.handle.HandleDelegate;
import nodebox.movie.Movie;
import nodebox.movie.VideoFormat;
import nodebox.node.*;
import nodebox.node.MenuItem;
import nodebox.ui.*;
import nodebox.util.FileUtils;
import nodebox.util.LoadException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
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
    private static Image APPLICATION_ICON_IMAGE;

    static {
        try {
            APPLICATION_ICON_IMAGE = ImageIO.read(NodeBoxDocument.class.getResourceAsStream("/application-logo.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // State
    private final NodeLibraryController controller;
    // Rendering
    private final AtomicBoolean isRendering = new AtomicBoolean(false);
    private final AtomicBoolean shouldRender = new AtomicBoolean(false);
    // GUI components
    private final NodeBoxMenuBar menuBar;
    private final AnimationBar animationBar;
    private final AddressBar addressBar;
    private final ViewerPane viewerPane;
    private final DataSheet dataSheet;
    private final PortView portView;
    private final NetworkPane networkPane;
    private final NetworkView networkView;
    private final ProgressPanel progressPanel;
    private File documentFile;
    private boolean documentChanged;
    private boolean needsResave;
    private AnimationTimer animationTimer;
    private boolean loaded = false;
    private UndoManager undoManager = new UndoManager();
    private boolean holdEdits = false;
    private String lastEditType = null;
    private String lastEditObjectId = null;
    private FunctionRepository functionRepository;
    private String activeNetworkPath = "";
    private String activeNodeName = "";
    private boolean restoring = false;
    private boolean invalidateFunctionRepository = false;
    private double frame = 1;
    private Map<String, double[]> networkPanZoomValues = new HashMap<String, double[]>();
    private SwingWorker<List<?>, Node> currentRender = null;
    private Iterable<?> lastRenderResult = null;
    private Map<String, List<?>> renderResults = ImmutableMap.of();
    private JSplitPane parameterNetworkSplit;
    private JSplitPane topSplit;
    private FullScreenFrame fullScreenFrame = null;
    private List<Zoom> zoomListeners = new ArrayList<Zoom>();
    private List<DeviceHandler> deviceHandlers = new ArrayList<DeviceHandler>();
    private DevicesDialog devicesDialog;

    public NodeBoxDocument() {
        this(createNewLibrary());
    }

    public NodeBoxDocument(NodeLibrary nodeLibrary) {
        if (!nodeLibrary.hasProperty("canvasX"))
            nodeLibrary = nodeLibrary.withProperty("canvasX", "0");
        if (!nodeLibrary.hasProperty("canvasY"))
            nodeLibrary = nodeLibrary.withProperty("canvasY", "0");
        if (!nodeLibrary.hasProperty("canvasWidth"))
            nodeLibrary = nodeLibrary.withProperty("canvasWidth", "1000");
        if (!nodeLibrary.hasProperty("canvasHeight"))
            nodeLibrary = nodeLibrary.withProperty("canvasHeight", "1000");

        controller = NodeLibraryController.withLibrary(nodeLibrary);
        invalidateFunctionRepository = true;
        JPanel rootPanel = new JPanel(new BorderLayout());
        this.viewerPane = new ViewerPane(this);
        viewerPane.getViewer().setCanvasBounds(getCanvasBounds());
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

        // Zoom in / out shortcuts.
        KeyStroke zoomInStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() + InputEvent.SHIFT_MASK);
        KeyStroke zoomInStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke zoomInStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        ActionListener zoomInHandler = new ZoomInHandler();
        getRootPane().registerKeyboardAction(zoomInHandler, zoomInStroke1, JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(zoomInHandler, zoomInStroke2, JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(zoomInHandler, zoomInStroke3, JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke zoomOutStroke = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        getRootPane().registerKeyboardAction(new ZoomOutHandler(), zoomOutStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        setContentPane(rootPanel);
        setLocationByPlatform(true);
        setSize(1100, 800);
        setIconImage(APPLICATION_ICON_IMAGE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        if (Platform.onMac()) {
            enableOSXFullscreen(this);
        }
        updateTitle();
        menuBar = new NodeBoxMenuBar(this);
        setJMenuBar(menuBar);
        loaded = true;

        if (Application.ENABLE_DEVICE_SUPPORT) {
            for (Device device : getNodeLibrary().getDevices()) {
                DeviceHandler handler = DeviceHandlerFactory.createDeviceHandler(device);
                if (handler != null)
                    deviceHandlers.add(handler);
            }
            devicesDialog = new DevicesDialog(this);
//            addressBar.setMessage("OSC Port " + getOSCPort());
        }
    }

    public static NodeBoxDocument getCurrentDocument() {
        return Application.getInstance().getCurrentDocument();
    }

    /**
     * Static factory method to create a NodeBoxDocument from a file.
     * <p/>
     * This method can handle file upgrades.
     *
     * @param file the file to load.
     * @return A NodeBoxDocument.
     */
    public static NodeBoxDocument load(File file) {
        NodeLibrary library;
        NodeBoxDocument document;
        try {
            library = NodeLibrary.load(file, Application.getInstance().getSystemRepository());
            document = new NodeBoxDocument(library);
            document.setDocumentFile(file);
        } catch (OutdatedLibraryException e) {
            UpgradeResult result = NodeLibraryUpgrades.upgrade(file);
            // The file is used here as the base name for finding relative libraries.
            library = result.getLibrary(file, Application.getInstance().getSystemRepository());
            document = new NodeBoxDocument(library);
            document.setDocumentFile(file);
            document.showUpgradeResult(result);
        } catch (LoadException e) {
            throw new RuntimeException("Could not load " + file, e);
        }
        lastFilePath = file.getParentFile().getAbsolutePath();
        return document;
    }

    private static NodeLibrary createNewLibrary() {
        NodeRepository nodeRepository = Application.getInstance().getSystemRepository();
        Node root = Node.NETWORK.withName("root");
        Node rectPrototype = nodeRepository.getNode("corevector.rect");
        String name = root.uniqueName(rectPrototype.getName());
        Node rect1 = rectPrototype.extend().withName(name).withPosition(new nodebox.graphics.Point(1, 1));
        root = root
                .withChildAdded(rect1)
                .withRenderedChild(rect1);
        return NodeLibrary.create("untitled", root, nodeRepository, FunctionRepository.of());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void enableOSXFullscreen(Window window) {
        Preconditions.checkNotNull(window);
        try {
            Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
            Class params[] = new Class[]{Window.class, Boolean.TYPE};
            Method method = util.getMethod("setWindowCanFullScreen", params);
            method.invoke(util, window, true);
        } catch (ClassNotFoundException e1) {
        } catch (Exception e) {
            System.err.println("OS X: Cannot set fullscreen mode.");
        }
    }

    /**
     * Display the result of upgrading in a dialog box.
     *
     * @param result The UpgradeResult.
     */
    private void showUpgradeResult(UpgradeResult result) {
        checkNotNull(result);
        if (result.getWarnings().isEmpty()) return;
        final UpgradeWarningsDialog dialog = new UpgradeWarningsDialog(result);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                dialog.setVisible(true);
            }
        });
    }

    public List<DeviceHandler> getDeviceHandlers() {
        return ImmutableList.copyOf(deviceHandlers);
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
        controller.renameNode(activeNetworkPath, node.getName(), name);

        String nodePath = Node.path(activeNetworkPath, node.getName());
        if (networkPanZoomValues.containsKey(nodePath)) {
            String newNodePath = Node.path(activeNetworkPath, name);
            for (String key : ImmutableList.copyOf(networkPanZoomValues.keySet())) {
                if (key.equals(nodePath) || key.startsWith(nodePath + "/")) {
                    String newKey = key.replace(nodePath, newNodePath);
                    networkPanZoomValues.put(newKey, networkPanZoomValues.get(key));
                }
            }
        }

        setActiveNode(name);
        networkView.updateNodes();
        networkView.singleSelect(getActiveNode());
        requestRender();
    }

    /**
     * Change the comment for the node.
     *
     * @param node    The node to be commented.
     * @param comment The new comment.
     */
    public void setNodeComment(Node node, String comment) {
        checkNotNull(node);
        checkNotNull(comment);
        addEdit("Set Node Comment");
        controller.commentNode(activeNetworkPath, node.getName(), comment.trim());
    }

    /**
     * Change the category for the node.
     *
     * @param node     The node to change.
     * @param category The new category.
     */
    public void setNodeCategory(Node node, String category) {
        checkNotNull(node);
        checkNotNull(category);
        addEdit("Set Node Category");
        String nodePath = Node.path(activeNetworkPath, node);
        controller.setNodeCategory(nodePath, category);
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
     * @param node     The node to change.
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
     * @param node   The node to change.
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

        // If the removed node is the active one, reset the port view and handles.
        if (node == getActiveNode()) {
            setActiveNode((Node) null);
        }

        controller.removeNode(activeNetworkPath, node.getName());

    }

    /**
     * Create a connection from the given output to the given input.
     *
     * @param outputNode The output node.
     * @param inputNode  The input node.
     * @param inputPort  The input port.
     */
    public void connect(String outputNode, String inputNode, String inputPort) {
        if (outputNode.equals(inputNode)) return;
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

    public void publish(String inputNode, String inputPort, String publishedName) {
        addEdit("Publish");
        controller.publish(activeNetworkPath, inputNode, inputPort, publishedName);
    }

    public void unpublish(String publishedName) {
        addEdit("Unpublish");
        controller.unpublish(activeNetworkPath, publishedName);
    }

    /**
     * @param node     the node on which to add the port
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
        controller.removePort(activeNetworkPath, node.getName(), portName);

        if (node == getActiveNode()) {
            portView.updateAll();
            viewerPane.repaint();
            dataSheet.repaint();
        }
    }

    /**
     * Change the label for the given port
     *
     * @param portName The name of the port to change.
     * @param label    The new label.
     */
    public void setPortLabel(String portName, String label) {
        checkValidPort(portName);
        addEdit("Change Label");
        controller.setPortLabel(getActiveNodePath(), portName, label);
        portView.updateAll();
        requestRender();
    }

    /**
     * Change the description for the given port
     *
     * @param portName    The name of the port to change.
     * @param description The new description.
     */
    public void setPortDescription(String portName, String description) {
        checkValidPort(portName);
        addEdit("Change Description");
        controller.setPortDescription(getActiveNodePath(), portName, description);
        portView.updateAll();
        requestRender();
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
     * @param item     The menu item to remove
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
        controller.movePortMenuItemDown(getActiveNodePath(), portName, itemIndex);
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
        controller.movePortMenuItemUp(getActiveNodePath(), portName, itemIndex);
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
        if (getActiveNode() == null) {
            return null;
        }
        Port port = checkValidPort(portName);
        return port.getValue();
    }

    /**
     * Set the port with the given node path to a new value.
     *
     * @param nodePath The path inside the network of the node the port belongs to.
     * @param portName The name of the port.
     * @param value    The new value.
     */
    public void setValue(String nodePath, String portName, Object value) {
        checkNotNull(getNodeLibrary().getNodeForPath(nodePath));
        addEdit("Change Value", "changeValue", nodePath + "#" + portName);

        controller.setPortValue(nodePath, portName, value);

        // TODO set variables on the root port.
//        if (port.getNode() == nodeLibrary.getRoot()) {
//            nodeLibrary.setVariable(port.getName(), port.asString());
//        }

        portView.updatePortValue(portName, value);
        // Setting a port might change enable expressions, and thus change the enabled state of a port row.
        portView.updateEnabledState();
        // Setting a port might change the enabled state of the handle.
        // viewer.setHandleEnabled(activeNode != null && activeNode.hasEnabledHandle());
        requestRender();
    }

    public void revertPortToDefault(String portName) {
        Port port = checkValidPort(portName);
        addEdit("Revert Port to Default");
        controller.revertToDefaultPortValue(getActiveNodePath(), portName);
        portView.updateAll();
        portView.updateEnabledState();
        requestRender();
    }

    public void addDevice(String deviceType, String deviceName) {
        // todo: undo / redo
        Device device = controller.addDevice(deviceType, deviceName);
        DeviceHandler handler = DeviceHandlerFactory.createDeviceHandler(device);
        if (handler != null)
            deviceHandlers.add(handler);
    }

    public void removeDevice(String deviceName) {
        // todo: undo / redo
        for (DeviceHandler handler : getDeviceHandlers()) {
            if (handler.getName().equals(deviceName)) {
                handler.stop();
                deviceHandlers.remove(handler);
                controller.removeDevice(deviceName);
            }
        }
    }

    public void startDeviceHandlers() {
        if (Application.ENABLE_DEVICE_SUPPORT) {
            for (DeviceHandler handler : deviceHandlers) {
                if (handler.isSyncedWithTimeline()) {
                    handler.resume();
                }
            }
            if (devicesDialog.isVisible())
                devicesDialog.rebuildInterface();
        }
    }

    public void stopDeviceHandlers(boolean pause) {
        if (Application.ENABLE_DEVICE_SUPPORT) {
            for (DeviceHandler handler : deviceHandlers) {
                if (handler.isSyncedWithTimeline()) {
                    if (pause) {
                        handler.pause();
                    } else {
                        handler.stop();
                    }
                }
            }
            if (devicesDialog.isVisible())
                devicesDialog.rebuildInterface();
        }
    }

    public void setDeviceProperty(String deviceName, String propertyName, String propertyValue) {
        checkNotNull(deviceName, "Device name cannot be null.");
        checkArgument(getNodeLibrary().hasDevice(deviceName));
        addEdit("Change Device Property");
        controller.setDeviceProperty(deviceName, propertyName, propertyValue);
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

    public void editMetadata() {
        if (getActiveNode() == null) return;
        JDialog editorDialog = new NodeAttributesDialog(NodeBoxDocument.this);
        editorDialog.setSize(580, 751);
        editorDialog.setLocationRelativeTo(NodeBoxDocument.this);
        editorDialog.setVisible(true);
    }

    //// Port pane callbacks ////

    public void takeScreenshot(File outputFile) {
        Container c = getContentPane();
        BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        c.paint(g2);
        try {
            ImageIO.write(img, "png", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //// Screen shot ////

    public void silentSet(String portName, Object value) {
        try {
            setValue(getActiveNodePath(), portName, value);
        } catch (Exception ignored) {
        }
    }

    //// HandleDelegate implementation ////

    public void stopEditing() {
        stopCombiningEdits();
    }

    // TODO Merge stopEditing and stopCombiningEdits.

    public void updateHandle() {
        if (viewerPane.getHandle() != null)
            viewerPane.getHandle().update();
        // TODO Make viewer repaint more fine-grained.
        viewerPane.repaint();
    }

    /**
     * Return the network that is currently "open": shown in the network view.
     *
     * @return The currently active network.
     */
    public Node getActiveNetwork() {
        // TODO This might be a potential bottleneck.
        return getNodeLibrary().getNodeForPath(activeNetworkPath);
    }

    //// Active network / node ////

    public void setActiveNetwork(String path) {
        checkNotNull(path);
        activeNetworkPath = path;
        Node network = getNodeLibrary().getNodeForPath(path);

        if (!restoring) {
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
        if (networkPanZoomValues.containsKey(activeNetworkPath)) {
            double[] pz = networkPanZoomValues.get(activeNetworkPath);
            networkView.setViewTransform(pz[0], pz[1], pz[2]);
        } else if (!restoring)
            networkView.resetViewTransform();
        if (!restoring)
            networkView.singleSelect(getActiveNode());
        viewerPane.repaint();
        dataSheet.repaint();

        requestRender();
    }

    public String getActiveNetworkPath() {
        return activeNetworkPath;
    }

    private String getRenderedNode() {
        if (viewerPane.shouldAlwaysRenderRoot()) return "/";
        return activeNetworkPath;
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

    public String getActiveNodePath() {
        return Node.path(activeNetworkPath, activeNodeName);
    }

    public String getActiveNodeName() {
        return activeNodeName;
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
                    LOG.log(Level.WARNING, "Error while creating handle for " + activeNode, e);
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

    public boolean isConnected(String portName) {
        Node network = getActiveNetwork();
        Node node = getActiveNode();
        if (network == null || node == null) return false;
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

    public void toggleAnimation() {
        animationBar.toggleAnimation();
    }

    public void doRewind() {
        animationBar.rewindAnimation();
    }

    public void playAnimation() {
        startDeviceHandlers();
        animationTimer.start();
    }

    public void stopAnimation() {
        stopDeviceHandlers(true);
        animationTimer.stop();
    }

    public void rewindAnimation() {
        stopAnimation();
        stopDeviceHandlers(false);
        resetRenderResults();
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
    public void requestRender() {
        // If we're already rendering, request the next renderNetwork.
        if (isRendering.compareAndSet(false, true)) {
            // If we're not rendering, start rendering.
            render();
        } else {
            shouldRender.set(true);
        }
    }

    public void renderFullScreen() {
        if (fullScreenFrame != null)
            closeFullScreenWindow();
        fullScreenFrame = new FullScreenFrame(this);
        fullScreenFrame.setVisible(true);
        fullScreenFrame.setOutputValues(lastRenderResult);
        fullScreenFrame.getViewer().setHandle(viewerPane.getHandle());
        fullScreenFrame.getViewer().setShowHandle(viewerPane.getViewer().hasVisibleHandle());
    }

    public void closeFullScreenWindow() {
        if (fullScreenFrame != null) {
            fullScreenFrame.setVisible(false);
            fullScreenFrame.dispose();
            fullScreenFrame = null;
            viewerPane.setOutputValues(lastRenderResult);
        }
    }

    private Viewer getViewer() {
        if (fullScreenFrame != null)
            return fullScreenFrame.getViewer();
        else
            return viewerPane.getViewer();
    }

    /**
     * Ask the document to stop the active rendering.
     */
    public synchronized void stopRendering() {
        if (currentRender != null) {
            currentRender.cancel(true);
        }
    }

    private void render() {
        checkState(SwingUtilities.isEventDispatchThread());
        checkState(currentRender == null);
        progressPanel.setInProgress(true);
        final NodeLibrary renderLibrary = getNodeLibrary();
        final String renderNetwork = getRenderedNode();

        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("frame", frame);
        dataMap.put("mouse.position", viewerPane.getViewer().getLastMousePosition());
        for (DeviceHandler handler : deviceHandlers)
            handler.addData(dataMap);
        final ImmutableMap<String, ?> data = ImmutableMap.copyOf(dataMap);

        final NodeContext context = new NodeContext(renderLibrary, getFunctionRepository(), data, renderResults, ImmutableMap.<String, Object>of());
        currentRender = new SwingWorker<List<?>, Node>() {
            @Override
            protected List<?> doInBackground() throws Exception {
                List<?> results = context.renderNode(renderNetwork);
                context.renderAlwaysRenderedNodes(renderNetwork);
                renderResults = context.getRenderResults();
                return results;
            }

            @Override
            protected void done() {
                networkPane.clearError();
                isRendering.set(false);
                currentRender = null;
                List<?> results;
                try {
                    results = get();
                } catch (CancellationException e) {
                    results = ImmutableList.of();
                } catch (InterruptedException e) {
                    results = ImmutableList.of();
                } catch (ExecutionException e) {
                    networkPane.setError(e.getCause());
                    results = ImmutableList.of();
                }

                lastRenderResult = results;

                networkView.checkErrorAndRepaint();
                progressPanel.setInProgress(false);
                if (fullScreenFrame != null)
                    fullScreenFrame.setOutputValues(results);
                else
                    viewerPane.setOutputValues(results);

                if (shouldRender.getAndSet(false)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            requestRender();
                        }
                    });
                }
            }
        };
        currentRender.execute();
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

    private synchronized void resetRenderResults() {
        renderResults = ImmutableMap.of();
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
     * @param command  the command name.
     * @param type     the type of edit
     * @param objectId the id for the edited object. This will be compared against.
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
        controller.setNodeLibraryFile(documentFile);
        updateTitle();
    }

    public boolean isChanged() {
        return documentChanged;
    }

    public boolean close() {
        stopAnimation();
        if (shouldClose()) {
            Application.getInstance().removeDocument(this);
            for (DeviceHandler handler : deviceHandlers)
                handler.stop();
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
        if (documentFile == null || needsResave()) {
            return saveAs();
        } else {
            boolean saved = saveToFile(documentFile);
            if (saved)
                NodeBoxMenuBar.addRecentFile(documentFile);
            return saved;
        }
    }

    public boolean saveAs() {
        File chosenFile = FileUtils.showSaveDialog(this, lastFilePath, "ndbx", "NodeBox File");
        if (chosenFile != null) {
            if (!chosenFile.getAbsolutePath().endsWith(".ndbx")) {
                chosenFile = new File(chosenFile.getAbsolutePath() + ".ndbx");
                if (chosenFile.exists()) {
                    boolean shouldReplace = ReplaceDialog.showForFile(chosenFile);
                    if (shouldReplace) {
                        return saveAs();
                    }
                }
            }
            lastFilePath = chosenFile.getParentFile().getAbsolutePath();
            setDocumentFile(chosenFile);
            boolean saved = saveToFile(documentFile);
            if (saved) {
                setNeedsResave(false);
                NodeBoxMenuBar.addRecentFile(documentFile);
            }
            return saved;
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

    private ExportFormat imageFormatForFile(File file) {
        if (file.getName().toLowerCase(Locale.US).endsWith(".pdf"))
            return ExportFormat.PDF;
        return ExportFormat.PNG;
    }

    public void doExport() {
        ExportDialog d = new ExportDialog(this);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        if (!d.isDialogSuccessful()) return;
        ExportFormat chosenFormat = d.getFormat();
        Map<String, ?> options = d.getExportOptions();
        File chosenFile = FileUtils.showSaveDialog(this, lastExportPath, "png,pdf,svg,csv", "Image file");
        if (chosenFile == null) return;
        lastExportPath = chosenFile.getParentFile().getAbsolutePath();
        exportToFile(chosenFile, chosenFormat, options);
    }

    private void exportToFile(File file, ExportFormat format, Map<String, ?> options) {
        // get data from last export.
        if (lastRenderResult == null) {
            JOptionPane.showMessageDialog(this, "There is no last render result.");
        } else {
            exportToFile(file, lastRenderResult, format, options);
        }
    }

    private void exportToFile(File file, Iterable<?> objects, ExportFormat format, Map<String, ?> options) {
        file = format.ensureFileExtension(file);
        ObjectsRenderer.render(objects, getCanvasBounds().getBounds2D(), file, options);
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
        ExportFormat format = d.getFormat();
        if (directory == null) return false;
        lastExportPath = directory.getAbsolutePath();
        exportRange(exportPrefix, directory, fromValue, toValue, format);
        return true;
    }

    public void exportRange(final String exportPrefix, final File directory, final int fromValue, final int toValue, final ExportFormat format) {
        exportThreadedRange(getNodeLibrary(), fromValue, toValue, new ExportDelegate() {
            int count = 1;

            @Override
            public void frameDone(double frame, Iterable<?> results) {
                File exportFile = new File(directory, exportPrefix + "-" + String.format("%05d", count));
                exportToFile(exportFile, results, format, ImmutableMap.<String, Object>of());
                count += 1;
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
        final Rectangle2D bounds = getCanvasBounds().getBounds2D();
        final int width = (int) Math.round(bounds.getWidth());
        final int height = (int) Math.round(bounds.getHeight());
        final Movie movie = new Movie(file.getAbsolutePath(), videoFormat, width, height, false);
        exportThreadedRange(controller.getNodeLibrary(), fromValue, toValue, new ExportDelegate() {
            @Override
            public void frameDone(double frame, Iterable<?> results) {
                movie.addFrame(ObjectsRenderer.createMovieImage(results, bounds));
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

    public boolean needsResave() {
        return needsResave;
    }

    public void setNeedsResave(boolean needsResave) {
        this.needsResave = needsResave;
    }

    private void exportThreadedRange(final NodeLibrary library, final int fromValue, final int toValue, final ExportDelegate exportDelegate) {
        int frameCount = toValue - fromValue;
        final InterruptibleProgressDialog d = new InterruptibleProgressDialog(this, "Exporting " + frameCount + " frames...");
        d.setTaskCount(toValue - fromValue + 1);
        d.setVisible(true);
        exportDelegate.progressDialog = d;

        final NodeLibrary exportLibrary = getNodeLibrary();
        final FunctionRepository exportFunctionRepository = getFunctionRepository();
        final Viewer viewer = new Viewer();

        final JFrame frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.setSize(getCanvasWidth(), getCanvasHeight());
        frame.setTitle("Exporting...");
        frame.add(viewer, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Map<String, List<?>> renderResults = ImmutableMap.of();
                    for (int frame = fromValue; frame <= toValue; frame++) {
                        if (Thread.currentThread().isInterrupted())
                            break;
                        HashMap<String, Object> data = new HashMap<String, Object>();
                        data.put("frame", (double) frame);
                        data.put("mouse.position", viewer.getLastMousePosition());
                        NodeContext context = new NodeContext(exportLibrary, exportFunctionRepository, data, renderResults, ImmutableMap.<String, Object>of());

                        List<?> results = context.renderNode("/");
                        renderResults = context.getRenderResults();
                        viewer.setOutputValues((List<?>) results);
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
                            frame.setVisible(false);
                        }
                    });
                }
            }
        });
        d.setThread(t);
        t.start();
        frame.setVisible(true);
    }

    private Rectangle getCanvasBounds() {
        return new Rectangle(-(getCanvasX() + getCanvasWidth() / 2), -(getCanvasY() + getCanvasHeight() / 2), getCanvasWidth(), getCanvasHeight());
    }

    private int getCanvasX() {
        return getIntProperty("canvasX", 0);
    }

    private int getCanvasY() {
        return getIntProperty("canvasY", 0);
    }

    private int getCanvasWidth() {
        return getIntProperty("canvasWidth", 1000);
    }

    private int getCanvasHeight() {
        return getIntProperty("canvasHeight", 1000);
    }

    private int getIntProperty(String name, int defaultValue) {
        try {
            return Integer.parseInt(getNodeLibrary().getProperty(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    //// Copy / Paste ////

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
        List<Node> newNodes = controller.pasteNodes(activeNetworkPath, nodeClipboard.network, nodeClipboard.nodes);

        networkView.updateAll();
        setActiveNode(newNodes.get(0));
        networkView.select(newNodes);
    }

    public void dragCopy() {
        List<Node> newNodes = controller.pasteNodes(activeNetworkPath, getActiveNetwork(), networkView.getSelectedNodes(), 0, 0);
        networkView.updateAll();
        networkView.select(newNodes);
    }

    public void deleteSelection() {
        networkView.deleteSelection();
    }

    public void groupIntoNetwork(nodebox.graphics.Point pt) {
        String networkName = getActiveNetwork().uniqueName("network");
        String name = JOptionPane.showInputDialog(this, "Network name:", networkName);
        if (name == null) return;

        startEdits("Group into Network");
        String renderedChild = getActiveNetwork().getRenderedChildName();
        Node subnet = controller.groupIntoNetwork(activeNetworkPath, networkView.getSelectedNodes(), networkName);
        controller.setNodePosition(Node.path(activeNetworkPath, subnet.getName()), pt);
        if (renderedChild.equals(subnet.getRenderedChildName()))
            controller.setRenderedChild(activeNetworkPath, subnet.getName());

        if (!name.equals(subnet.getName())) {
            controller.renameNode(activeNetworkPath, subnet.getName(), name);
            subnet = getActiveNetwork().getChild(name);
        }

        if (networkPanZoomValues.containsKey(activeNetworkPath))
            networkPanZoomValues.put(Node.path(activeNetworkPath, name), networkPanZoomValues.get(activeNetworkPath));

        stopEdits();

        setActiveNode(subnet);
        networkView.updateAll();
        networkView.select(subnet);
        requestRender();
    }

    /**
     * Start the dialog that allows a user to create a new node.
     */
    public void showNodeSelectionDialog() {
        showNodeSelectionDialog(networkView.centerGridPoint());
    }

    /**
     * Start the dialog that allows a user to create a new node.
     *
     * @param pt The point in "grid space"
     */
    public void showNodeSelectionDialog(Point pt) {
        NodeRepository repository = getNodeRepository();
        NodeSelectionDialog dialog = new NodeSelectionDialog(this, controller.getNodeLibrary(), repository);
        dialog.setVisible(true);
        if (dialog.getSelectedNode() != null) {
            createNode(dialog.getSelectedNode(), new nodebox.graphics.Point(pt));
        }
    }

    public void showCodeLibraries() {
        CodeLibrariesDialog dialog = new CodeLibrariesDialog(this, getNodeLibrary().getFunctionRepository());
        dialog.setVisible(true);
        FunctionRepository functionRepository = dialog.getFunctionRepository();
        if (functionRepository != null) {
            addEdit("Change function repository");
            controller.setFunctionRepository(functionRepository);
            invalidateFunctionRepository = true;
            requestRender();
        }
    }

    public void showDocumentProperties() {
        DocumentPropertiesDialog dialog = new DocumentPropertiesDialog(this);
        dialog.setVisible(true);
        if (dialog.isCommitted()) {
            addEdit("Change document properties");
            controller.setProperties(dialog.getProperties());
            getViewer().setCanvasBounds(getCanvasBounds().getBounds2D());
            requestRender();
        }
    }

    public void showDevices() {
        devicesDialog.setVisible(true);
    }

    public void reload() {
        controller.reloadFunctionRepository();
        functionRepository.invalidateFunctionCache();
        requestRender();
    }

    public void zoomView(double scaleDelta) {
        PointerInfo a = MouseInfo.getPointerInfo();
        Point point = new Point(a.getLocation());
        for (Zoom zoomListener : zoomListeners) {
            if (zoomListener.containsPoint(point))
                zoomListener.zoom(scaleDelta);
        }
    }

    public void addZoomListener(Zoom listener) {
        zoomListeners.add(listener);
    }

    public void removeZoomListener(Zoom listener) {
        zoomListeners.remove(listener);
    }

    public void setActiveNetworkPanZoom(double viewX, double viewY, double viewScale) {
        double[] pz = new double[]{viewX, viewY, viewScale};
        networkPanZoomValues.put(getActiveNetworkPath(), pz);
    }

    public void windowOpened(WindowEvent e) {
        //viewEditorSplit.setDividerLocation(0.5);
        parameterNetworkSplit.setDividerLocation(0.5);
        topSplit.setDividerLocation(0.5);
    }

    public void windowClosing(WindowEvent e) {
        close();
    }

    //// Window events ////

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

    private abstract class ExportDelegate {
        protected InterruptibleProgressDialog progressDialog;

        void frameDone(double frame, Iterable<?> results) {
        }

        void exportDone() {
        }
    }

    private class NodeClipboard {
        private final Node network;
        private final ImmutableList<Node> nodes;

        private NodeClipboard(Node network, Iterable<Node> nodes) {
            this.network = network;
            this.nodes = ImmutableList.copyOf(nodes);
        }
    }

    private class ZoomInHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            zoomView(1.05);
        }
    }

    private class ZoomOutHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            zoomView(0.95);
        }
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
