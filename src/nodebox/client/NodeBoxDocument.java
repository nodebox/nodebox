package nodebox.client;

import nodebox.client.movie.Movie;
import nodebox.client.movie.VideoFormat;
import nodebox.handle.Handle;
import nodebox.handle.HandleDelegate;
import nodebox.node.*;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nodebox.base.Preconditions.checkArgument;
import static nodebox.base.Preconditions.checkNotNull;

/**
 * A NodeBoxDocument manages a NodeLibrary.
 */
public class NodeBoxDocument extends JFrame implements WindowListener, ViewerEventListener, HandleDelegate {

    private final static String WINDOW_MODIFIED = "windowModified";

    public static String lastFilePath;
    public static String lastExportPath;

    private static NodeLibrary clipboardLibrary;

    private File documentFile;
    private boolean documentChanged;
    private static Logger logger = Logger.getLogger("nodebox.client.NodeBoxDocument");
    private AnimationTimer animationTimer;
    private ArrayList<ParameterEditor> parameterEditors = new ArrayList<ParameterEditor>();
    private boolean loaded = false;
    private SpotlightPanel spotlightPanel;

    private UndoManager undoManager = new UndoManager();
    private boolean holdEdits = false;
    private String lastEditType = null;
    private Object lastEditObject = null;

    private NodeLibrary nodeLibrary;
    private Node activeNetwork;
    private Node activeNode;

    // GUI components
    private final NodeBoxMenuBar menuBar;
    private final AnimationBar animationBar;
    private final AddressBar addressBar;
    private final Viewer viewer;
    private final EditorPane editorPane;
    private final ParameterView parameterView;
    private final NetworkView networkView;
    private JSplitPane viewEditorSplit;
    private JSplitPane parameterNetworkSplit;
    private JSplitPane topSplit;

    public static NodeBoxDocument getCurrentDocument() {
        return Application.getInstance().getCurrentDocument();
    }

    public static NodeLibraryManager getManager() {
        return Application.getInstance().getManager();
    }

    public static NodeLibrary getNodeClipboard() {
        return clipboardLibrary;
    }

    public static void setNodeClipboard(NodeLibrary clipboardLibrary) {
        NodeBoxDocument.clipboardLibrary = clipboardLibrary;
    }

    public NodeBoxDocument(NodeLibrary library) {
        setNodeLibrary(library);
        JPanel rootPanel = new JPanel(new BorderLayout());
        ViewerPane viewerPane = new ViewerPane(this);
        viewer = viewerPane.getViewer();
        editorPane = new EditorPane(this);
        ParameterPane parameterPane = new ParameterPane();
        parameterPane.setEditMetadataListener(new ParameterPane.EditMetadataListener() {
            public void onEditMetadata() {
                if (activeNode == null) return;
                JDialog editorDialog = new NodeAttributesDialog(NodeBoxDocument.this);
                editorDialog.setSize(580, 751);
                editorDialog.setLocationRelativeTo(NodeBoxDocument.this);
                editorDialog.setVisible(true);
            }
        });
        parameterView = parameterPane.getParameterView();
        parameterView.setDocument(this); // TODO Remove this once parameter view is fully decoupled.
        NetworkPane networkPane = new NetworkPane(this);
        networkView = networkPane.getNetworkView();
        networkView.setDelegate(new NetworkView.Delegate() {
            public void activeNodeChanged(Node node) {
                setActiveNode(node);
            }
        });
        viewEditorSplit = new CustomSplitPane(JSplitPane.VERTICAL_SPLIT, viewerPane, editorPane);
        parameterNetworkSplit = new CustomSplitPane(JSplitPane.VERTICAL_SPLIT, parameterPane, networkPane);
        topSplit = new CustomSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewEditorSplit, parameterNetworkSplit);
        addressBar = new AddressBar();
        addressBar.setOnPartClickListener(new AddressBar.OnPartClickListener() {
            public void onPartClicked(Node n) {
                setActiveNetwork(n);
            }
        });

        rootPanel.add(addressBar, BorderLayout.NORTH);
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

        setActiveNetwork(library.getRootNode());
        // setActiveNode is not called because it registers that the current node is already null.
        // The parameter view is a special case since it does need to show something when the active node is null.
        Node rootNode = nodeLibrary.getRootNode();
        if (rootNode != null && rootNode.getRenderedChild() != null)
            parameterView.setActiveNode(rootNode.getRenderedChild());
        else
            parameterView.setActiveNode(rootNode);


        spotlightPanel = new SpotlightPanel(networkPane);
        setGlassPane(spotlightPanel);
        spotlightPanel.setVisible(true);
        spotlightPanel.setOpaque(false);
    }

    public NodeBoxDocument(File file) throws RuntimeException {
        this(NodeLibrary.load(file, Application.getInstance().getManager()));
        lastFilePath = file.getParentFile().getAbsolutePath();
        setDocumentFile(file);
        spotlightPanel.hideSpotlightPanel();
    }

    //// Node Library management ////

    public NodeLibrary getNodeLibrary() {
        return nodeLibrary;
    }

    public void setNodeLibrary(NodeLibrary newLibrary) {
        checkNotNull(newLibrary, "Node library cannot be null.");
        boolean startingUp = this.nodeLibrary == null;
        this.nodeLibrary = newLibrary;
        if (!startingUp) {
            setActiveNetwork(newLibrary.getRootNode());
        }
    }

    //// Node operations ////

    /**
     * Create a node in the active network.
     * This node is based on a prototype.
     *
     * @param prototype The prototype node.
     * @param pt        The initial node position.
     */
    public void createNode(Node prototype, Point pt) {
        startEdits("Create Node");
        Node n = getActiveNetwork().create(prototype);
        setNodePosition(n, new nodebox.graphics.Point(pt));
        setRenderedNode(n);
        setActiveNode(n);
        stopEdits();

        networkView.updateNodes();
        networkView.setActiveNode(activeNode);
        parameterView.setActiveNode(activeNode);
        editorPane.setActiveNode(activeNode);
    }

    /**
     * Change the node position of the given node.
     *
     * @param node  the node to move
     * @param point the point to move to
     */
    public void setNodePosition(Node node, nodebox.graphics.Point point) {
        // Note that we're passing in the parent network of the node.
        // This means that all move changes to the parent network are grouped
        // together under one edit, instead of for each node individually.
        addEdit("Move Node", "moveNode", node.getParent());
        node.setPosition(point);

        networkView.updatePosition(node);
    }

    /**
     * Change the node name.
     *
     * @param node The node to rename.
     * @param name The new node name.
     */
    public void setNodeName(Node node, String name) {
        node.setName(name);
        networkView.updateNodes();
        // Renaming the node can have an effect on expressions, so recalculate the network.
        render();
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
        if (node == activeNode) {
            parameterView.updateAll();
            // Updating the metadata could cause changes to a handle.
            viewer.repaint();
        }
        render();
    }

    /**
     * Change the rendered node to the given node
     *
     * @param node the node to set rendered
     */
    public void setRenderedNode(Node node) {
        addEdit("Set Rendered");
        node.setRendered();
        networkView.updateNodes();
        networkView.setActiveNode(activeNode);
        render();
    }

    public void setNodeExported(Node node, boolean exported) {
        addEdit("Set Exported");
        node.setExported(exported);
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
        render();
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
        render();
    }

    /**
     * Helper method used by removeNode and removeNodes to do the removal and update the parameter view, if needed.
     *
     * @param node The node to remove.
     */
    private void removeNodeImpl(Node node) {
        checkNotNull(node, "Node to remove cannot be null.");
        checkArgument(node.getParent() == activeNetwork, "Node to remove is not in active network.");
        getActiveNetwork().remove(node);
        // If the removed node was the active one, reset the parameter view.
        if (node == activeNode) {
            setActiveNode(null);
        }
    }

    /**
     * Create a connection from the given output to the given input.
     *
     * @param output the output port
     * @param input  the input port
     */
    public void connect(Port output, Port input) {
        addEdit("Connect");
        getActiveNetwork().connectChildren(input, output);

        if (input.getNode() == activeNode) {
            parameterView.updateConnectionPanel();
        }
        render();
    }

    /**
     * Changes the ordering of output connections by moving the given connection a specified number of positions.
     * <p/>
     * To move the specified connection up one position, set the deltaIndex to -1. To move a connection down, set
     * the deltaIndex to 1.
     * <p/>
     * If the delta index is larger or smaller than the number of positions this connection can move, it will
     * move the connection to the beginning or end. This will not result in an error.
     *
     * @param connection the connection to reorder
     * @param deltaIndex the number of places to move.
     * @param multi      the connection should only be reordered among connections connected to the same input port (with cardinality MULTIPLE).
     */
    public void reorderConnection(Connection connection, int deltaIndex, boolean multi) {
        connection.getInput().getParentNode().reorderConnection(connection, deltaIndex, multi);

        parameterView.updateConnectionPanel();
        networkView.updateConnections();
        render();
    }

    /**
     * Remove the given connection from the network.
     *
     * @param connection the connection to remove
     */
    public void disconnect(Connection connection) {
        addEdit("Disconnect");
        getActiveNetwork().disconnect(connection);

        networkView.updateConnections();
        if (connection.getInputNode() == activeNode) {
            parameterView.updateConnectionPanel();
        }
        render();
    }

    /**
     * Copy children of this network to the new parent.
     *
     * @param children  the children to copy
     * @param oldParent the old parent
     * @param newParent the new parent
     * @return the newly copied node
     */
    public Collection<Node> copyChildren(Collection<Node> children, Node oldParent, Node newParent) {
        addEdit("Copy");
        return oldParent.copyChildren(children, newParent);
    }

    /**
     *
     * @param node  the node on which to add the parameter
     * @param parameterName the name of the new parameter
     */
    public void addParameter(Node node, String parameterName) {
        addEdit("Add Parameter");
        Parameter parameter = node.addParameter(parameterName, Parameter.Type.FLOAT);
        if (node == activeNode) {
            parameterView.updateAll();
            viewer.repaint();
        }
    }

    /**
     *
     * @param node  the node on which to remove the parameter
     * @param parameterName the name of the parameter
     */
    public void removeParameter(Node node, String parameterName) {
        addEdit("Remove Parameter");
        node.removeParameter(parameterName);
        if (node == activeNode) {
            parameterView.updateAll();
            viewer.repaint();
        }
    }

    public void addPort(Node node, String portName, Port.Cardinality cardinality) {
        addEdit("Add Port");
        Port port = node.addPort(portName, cardinality);
        if (node == activeNode && port.getCardinality() == Port.Cardinality.MULTIPLE) {
            parameterView.updateAll();
        }
        networkView.updateNodes();
    }

    /**
     * Set the parameter to the given value.
     *
     * @param parameter the parameter to set
     * @param value     the new value
     */
    public void setParameterValue(Parameter parameter, Object value) {
        checkNotNull(parameter, "Parameter cannot be null.");
        addEdit("Change Value", "changeValue", parameter);
        parameter.set(value);
        if (parameter.getNode() == nodeLibrary.getRootNode()) {
            nodeLibrary.setVariable(parameter.getName(), parameter.asString());
        }

        parameterView.updateParameterValue(parameter, value);
        // Setting a parameter might change enable expressions, and thus change the enabled state of a parameter row.
        parameterView.updateEnabledState();
        // Setting a parameter might change the enabled state of the handle.
        viewer.setHandleEnabled(activeNode != null && activeNode.hasEnabledHandle());
        if (parameter.getName().equals("_image"))
            networkView.updateNodes();
        render();
    }

    public void setParameterExpression(Parameter parameter, String expression) {
        addEdit("Change Parameter Expression");
        parameter.setExpression(expression);

        parameterView.updateParameter(parameter);
        render();
    }

    public void clearParameterExpression(Parameter parameter) {
        addEdit("Clear Parameter Expression");
        parameter.clearExpression();

        parameterView.updateParameter(parameter);
        render();
    }

    public void revertParameterToDefault(Parameter parameter) {
        addEdit("Revert Parameter to Default");
        parameter.revertToDefault();

        parameterView.updateParameter(parameter);
        render();
    }

    public void setParameterLabel(Parameter parameter, String label) {
        addEdit("Set Parameter Label");
        parameter.setLabel(label);

        parameterView.updateParameter(parameter);
    }

    public void setParameterHelpText(Parameter parameter, String helpText) {
        addEdit("Set Parameter Help Text");
        parameter.setHelpText(helpText);

        parameterView.updateParameter(parameter);
    }

    public void setParameterWidget(Parameter parameter, Parameter.Widget widget) {
        addEdit("Set Parameter Widget");
        parameter.setWidget(widget);

        parameterView.updateParameter(parameter);
        render();
    }

    public void setParameterEnableExpression(Parameter parameter, String enableExpression) {
        addEdit("Set Parameter Enable Expression");
        parameter.setEnableExpression(enableExpression);

        parameterView.updateParameter(parameter);
        render();
    }

    public void setParameterBoundingMethod(Parameter parameter, Parameter.BoundingMethod method) {
        addEdit("Set Parameter Bounding Method");
        parameter.setBoundingMethod(method);

        parameterView.updateParameter(parameter);
        render();
    }

    public void setParameterMinimumValue(Parameter parameter, Float minimumValue) {
        addEdit("Set Parameter Minimum Value");
        parameter.setMinimumValue(minimumValue);

        parameterView.updateParameter(parameter);
        render();
    }

    public void setParameterMaximumValue(Parameter parameter, Float maximumValue) {
        addEdit("Set Parameter Maximum Value");
        parameter.setMaximumValue(maximumValue);

        parameterView.updateParameter(parameter);
        render();
    }

    public void setParameterDisplayLevel(Parameter parameter, Parameter.DisplayLevel displayLevel) {
        addEdit("Set Parameter Display Level");
        parameter.setDisplayLevel(displayLevel);

        parameterView.updateParameter(parameter);
    }

    public void addParameterMenuItem(Parameter parameter, String key, String label) {
        addEdit("Add Parameter Menu Item");
        parameter.addMenuItem(key, label);

        parameterView.updateParameter(parameter);
        render();
    }

    public void removeParameterMenuItem(Parameter parameter, Parameter.MenuItem item) {
        addEdit("Remove Parameter Menu Item");
        parameter.removeMenuItem(item);

        parameterView.updateParameter(parameter);
        render();
    }

    public void moveParameterItemDown(Parameter parameter, int itemIndex) {
        addEdit("Move Parameter Item Down");
        java.util.List<Parameter.MenuItem> items = parameter.getMenuItems();
        Parameter.MenuItem item = items.get(itemIndex);
        items.remove(item);
        items.add(itemIndex + 1, item);
        parameter.fireAttributeChanged();

        parameterView.updateParameter(parameter);
    }

    public void moveParameterItemUp(Parameter parameter, int itemIndex) {
        addEdit("Move Parameter Item Up");
        java.util.List<Parameter.MenuItem> items = parameter.getMenuItems();
        Parameter.MenuItem item = items.get(itemIndex);
        items.remove(item);
        items.add(itemIndex - 1, item);
        parameter.fireAttributeChanged();

        parameterView.updateParameter(parameter);
    }


    //// Editor pane callbacks ////

    public void codeEdited(String source) {
        networkView.codeChanged(activeNode, true);
    }

    //// HandleDelegate implementation ////

    // TODO Merge setParameterValue and setValue.
    public void setValue(Node node, String parameterName, Object value) {
        checkNotNull(node, "Node cannot be null");
        Parameter parameter = node.getParameter(parameterName);
        checkNotNull(parameter, "Parameter '" + parameterName + "' is not a parameter on node " + node);
        setParameterValue(parameter, value);
    }

    public void silentSet(Node node, String parameterName, Object value) {
        try {
            Parameter parameter = node.getParameter(parameterName);
            setParameterValue(parameter, value);
        } catch (Exception ignored) {
        }
    }

    // TODO Merge stopEditing and stopCombiningEdits.
    public void stopEditing(Node node) {
        stopEdits();
        stopCombiningEdits();
    }

    public void updateHandle(Node node) {
        if (viewer.getHandle() != null)
            viewer.getHandle().update();
        // TODO Make viewer repaint more fine-grained.
        viewer.repaint();
    }

    //// Active network / node ////

    /**
     * Return the network that is currently "open": shown in the network view.
     *
     * @return The currently active network.
     */
    public Node getActiveNetwork() {
        return activeNetwork;
    }

    public String getActiveNetworkPath() {
        if (activeNetwork == null) return "";
        return activeNetwork.getAbsolutePath();
    }

    public void setActiveNetwork(Node activeNetwork) {
        checkNotNull(activeNetwork, "Active network cannot be null.");
        this.activeNetwork = activeNetwork;
        if (activeNetwork.getRenderedChild() != null) {
            setActiveNode(activeNetwork.getRenderedChild());
        } else if (!activeNetwork.isEmpty()) {
            setActiveNode(activeNetwork.getChildAt(0));
        } else {
            setActiveNode(null);
        }

        addressBar.setActiveNetwork(activeNetwork);
        viewer.setHandleEnabled(activeNode != null && activeNode.hasEnabledHandle());
        viewer.repaint();
        networkView.setActiveNetwork(activeNetwork);
        networkView.setActiveNode(activeNode);

        render();
    }

    public void setActiveNetwork(String path) {
        Node network = nodeLibrary.getNodeForPath(path);
        setActiveNetwork(network);
    }

    /**
     * Return the node that is currently focused:
     * visible in the parameter view, and whose handles are displayed in the viewer.
     *
     * @return
     */
    public Node getActiveNode() {
        return activeNode;
    }

    public String getActiveNodePath() {
        if (activeNode == null) return "";
        return activeNode.getAbsolutePath();
    }

    /**
     * Set the active node to the given node.
     * <p/>
     * The active node is the one whose parameters are displayed in the parameter pane,
     * and whose handle is displayed in the viewer.
     * <p/>
     * This will also change the active network if necessary.
     *
     * @param node the node to change to.
     */
    public void setActiveNode(Node node) {
        stopCombiningEdits();
        if (activeNode == node) return;
        activeNode = node;
        createHandleForActiveNode();
        viewer.repaint();
        parameterView.setActiveNode(activeNode == null ? nodeLibrary.getRootNode() : activeNode);
        networkView.setActiveNode(activeNode);
        editorPane.setActiveNode(activeNode);
    }

    private void createHandleForActiveNode() {
        if (activeNode != null) {
            Handle handle = null;
            try {
                handle = activeNode.createHandle();
                // If the handle was created successfully, remove the messages.
                editorPane.clearMessages();
            } catch (Exception e) {
                editorPane.setMessages(e.toString());
            }
            if (handle != null) {
                handle.setHandleDelegate(this);
                // TODO Remove this. Find out why the handle needs access to the viewer (only repaint?) and put that in the HandleDelegate.
                handle.setViewer(viewer);
                viewer.setHandleEnabled(activeNode.hasEnabledHandle());
            }
            viewer.setHandle(handle);
        } else {
            viewer.setHandle(null);
        }
    }

    //// Animation ////

    public float getFrame() {
        return nodeLibrary.getFrame();
    }

    public void setFrame(float frame) {
        nodeLibrary.setFrame(frame);
        animationBar.updateFrame();
        render();
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
     * Called when the active network will start rendering.
     * Called on the Swing EDT so you can update the GUI.
     *
     * @param context The processing context.
     */
    public void startRendering(ProcessingContext context) {
        addressBar.setProgressVisible(true);
    }

    /**
     * Called when the active network has finished rendering.
     *
     * @param context The processing context.
     */
    public void finishedRendering(ProcessingContext context) {
        addressBar.setProgressVisible(false);
        editorPane.updateMessages(activeNode, context);
        viewer.setOutputValue(activeNetwork.getOutputValue());
        networkView.checkErrorAndRepaint();
        // TODO I don't know if this is the best way to do this.
        if (viewer.getHandle() != null)
            viewer.getHandle().update();
    }

    private void render() {
        if (!loaded) return;
        if (!activeNetwork.isDirty()) return;
        final ProcessingContext context = new ProcessingContext(activeNetwork);
        startRendering(context);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // If meanwhile the node has been marked clean, ignore the event.
                // This avoids double renders.
                if (!activeNetwork.isDirty()) return;
                try {
                    activeNetwork.update(context);
                } catch (ProcessingError processingError) {
                    Logger.getLogger("NodeBoxDocument").log(Level.WARNING, "Error while processing", processingError);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            finishedRendering(context);
                        }
                    });
                }
            }
        });
    }

    public void setActiveNodeCode(Parameter codeParameter, String source) {
        if (activeNode == null) return;
        if (codeParameter == null) return;
        NodeCode code = new PythonCode(source);
        codeParameter.set(code);
        if (codeParameter.getName().equals("_handle")) {
            createHandleForActiveNode();
        }
        networkView.codeChanged(activeNode, false);
        render();
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
     * @param object  the edited object. This will be compared using ==.
     */
    public void addEdit(String command, String type, Object object) {
        if (!holdEdits) {
            markChanged();
            if (lastEditType != null && lastEditType.equals(type) && lastEditObject == object) {
                // If the last edit type and last edit id are the same,
                // we combine the two edits into one.
                // Since we've already saved the last state, we don't need to do anything.
            } else {
                addEdit(command);
                lastEditType = type;
                lastEditObject = object;
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
        lastEditObject = null;

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

    //// Parameter editor actions ////

    public void addParameterEditor(ParameterEditor editor) {
        if (parameterEditors.contains(editor)) return;
        parameterEditors.add(editor);
    }

    public void removeParameterEditor(ParameterEditor editor) {
        parameterEditors.remove(editor);
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
            //renderThread.shutdown();
            Application.getInstance().getManager().remove(nodeLibrary);
            Application.getInstance().removeDocument(NodeBoxDocument.this);
            for (ParameterEditor editor : parameterEditors) {
                editor.dispose();
            }
            dispose();
            // On Mac the application does not close if the last window is closed.
            if (!PlatformUtils.onMac()) {
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
            nodeLibrary.store(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "An error occurred while saving the file.", "NodeBox", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "An error occurred while saving the file.", e);
            return false;
        }
        documentChanged = false;
        updateTitle();
        return true;

    }

    private boolean exportToFile(File file, ImageFormat format) {
        return exportToFile(file, activeNetwork, format);
    }

    private boolean exportToFile(File file, Node exportNetwork, ImageFormat format) {
        file = format.ensureFileExtension(file);
        if (exportNetwork == null) return false;
        Object outputValue = exportNetwork.getOutputValue();
        if (outputValue instanceof nodebox.graphics.Canvas) {
            nodebox.graphics.Canvas c = (nodebox.graphics.Canvas) outputValue;
            c.save(file);
            return true;
        } else {
            throw new RuntimeException("This type of output cannot be exported " + outputValue);
        }
    }

    private void markChanged() {
        if (!documentChanged && loaded) {
            documentChanged = true;
            updateTitle();
            getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.TRUE);
        }
    }

    public void cut() {
        copy();
        deleteSelection();
    }

    public void copy() {
        // When copying, create copies of all the nodes and store them under a new parent.
        // The parent is used to preserve the connections, and also to save the state of the
        // copied nodes.
        // This parent is the root of a new library.
        NodeLibrary clipboardLibrary = new NodeLibrary("clipboard");
        Node clipboardRoot = clipboardLibrary.getRootNode();
        copyChildren(networkView.getSelectedNodes(), getActiveNetwork(), clipboardRoot);
        setNodeClipboard(clipboardLibrary);
    }

    public void paste() {
        addEdit("Paste node");
        NodeLibrary clipboardLibrary = getNodeClipboard();
        if (clipboardLibrary == null) return;
        Node clipboardRoot = clipboardLibrary.getRootNode();
        if (clipboardRoot.size() == 0) return;
        Collection<Node> newNodes = copyChildren(clipboardRoot.getChildren(), clipboardRoot, getActiveNetwork());
        for (Node newNode : newNodes) {
            nodebox.graphics.Point pt = newNode.getPosition();
            pt.x += 20;
            pt.y += 80;
            newNode.setPosition(pt);
        }

        networkView.updateAll();
        networkView.select(newNodes);
    }

    public void deleteSelection() {
        java.util.List<Node> selectedNodes = networkView.getSelectedNodes();
        if (! selectedNodes.isEmpty())
            removeNodes(networkView.getSelectedNodes());
        else if (networkView.hasSelectedConnection())
            networkView.deleteSelectedConnection();
    }

    private void updateTitle() {
        String postfix = "";
        if (!PlatformUtils.onMac()) {
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

    /**
     * Start the dialog that allows a user to create a new node.
     */
    public void createNewNode() {
        // TODO Move this from the NetworkView to here.
        networkView.showNodeSelectionDialog();
    }

    public void reload() {
        editorPane.reload();
    }

    public boolean export() {
        File chosenFile = FileUtils.showSaveDialog(this, lastExportPath, "pdf", "PDF file");
        if (chosenFile != null) {
            lastExportPath = chosenFile.getParentFile().getAbsolutePath();
            return exportToFile(chosenFile, ImageFormat.PDF);
        }
        return false;
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
        ImageFormat format = d.getFormat();
        if (directory == null) return false;
        lastExportPath = directory.getAbsolutePath();
        exportRange(exportPrefix, directory, fromValue, toValue, format);
        return true;
    }

    public void exportRange(final String exportPrefix, final File directory, final int fromValue, final int toValue, final ImageFormat format) {
        // Show the progress dialog
        final ProgressDialog d = new InterruptableProgressDialog(this, "Exporting...");
        d.setTaskCount(toValue - fromValue + 1);
        d.setVisible(true);
        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.setAlwaysOnTop(true);

        String xml = nodeLibrary.toXml();
        final NodeLibrary exportLibrary = NodeLibrary.load(nodeLibrary.getName(), xml, getManager());
        exportLibrary.setFile(nodeLibrary.getFile());
        final Node exportNetwork = exportLibrary.getRootNode();
        final ExportViewer viewer = new ExportViewer(exportNetwork);

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int frame = fromValue; frame <= toValue; frame++) {
                        if (Thread.currentThread().isInterrupted())
                            break;
                        // TODO: Check if rendered node is not null.
                        try {
                            exportLibrary.setFrame(frame);
                            exportNetwork.update();
                            viewer.updateFrame();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                d.tick();
                            }
                        });
                        File exportFile = new File(directory, exportPrefix + "-" + frame);
                        exportToFile(exportFile, exportNetwork, format);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
        ((InterruptableProgressDialog) d).setThread(t);
        t.start();
        viewer.setVisible(true);
    }

    public boolean exportMovie() {
        ExportMovieDialog d = new ExportMovieDialog(this, lastExportPath == null ? null : new File(lastExportPath));
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        if (!d.isDialogSuccessful()) return false;
        File chosenFile = d.getExportPath();
        if (chosenFile != null) {
            lastExportPath = chosenFile.getParentFile().getAbsolutePath();
            // TODO: support different codec types as well.
            exportToMovieFile(chosenFile, d.getVideoFormat(), d.getFromValue(), d.getToValue());
            return true;
        }
        return false;
    }

    private void exportToMovieFile(File file, final VideoFormat videoFormat, final int fromValue, final int toValue) {
        final ProgressDialog d = new InterruptableProgressDialog(this, null);
        d.setTaskCount(toValue - fromValue + 1);
        d.setTitle("Exporting " + (toValue - fromValue + 1) + " frames...");
        d.setVisible(true);
        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.setAlwaysOnTop(true);

        file = videoFormat.ensureFileExtension(file);
        String xml = nodeLibrary.toXml();
        final NodeLibrary exportLibrary = NodeLibrary.load(nodeLibrary.getName(), xml, getManager());
        exportLibrary.setFile(nodeLibrary.getFile());
        final Node exportNetwork = exportLibrary.getRootNode();
        final int width = (int) exportNetwork.asFloat(NodeLibrary.CANVAS_WIDTH);
        final int height = (int) exportNetwork.asFloat(NodeLibrary.CANVAS_HEIGHT);
        final Movie movie = new Movie(file.getAbsolutePath(), videoFormat, width, height, false);
        final ExportViewer viewer = new ExportViewer(exportNetwork);

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int frame = fromValue; frame <= toValue; frame++) {
                        if (Thread.currentThread().isInterrupted())
                            break;
                        // TODO: Check if rendered node is not null.
                        try {
                            exportLibrary.setFrame(frame);
                            exportNetwork.update();
                            viewer.updateFrame();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                d.tick();
                            }
                        });

                        Object outputValue = exportNetwork.getOutputValue();
                        if (outputValue instanceof nodebox.graphics.Canvas) {
                            nodebox.graphics.Canvas c = (nodebox.graphics.Canvas) outputValue;
                            movie.addFrame(c.asImage());
                        } else break;
                    }
                    d.setTitle("Converting frames to movie...");
                    d.reset();
                    FramesWriter w = new FramesWriter(d);
                    movie.save(w);

                } catch (Exception e) {
                    e.printStackTrace();
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
        ((InterruptableProgressDialog) d).setThread(t);
        t.start();
        viewer.setVisible(true);
    }

    public void onConsoleVisibleEvent(boolean visible) {
        menuBar.setShowConsoleChecked(visible);
    }

    //// Window events ////

    public void windowOpened(WindowEvent e) {
        viewEditorSplit.setDividerLocation(0.5);
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

    //// Spotlight ////

    private static class SpotlightPanel extends JPanel {

        private Pane networkPane;
        private AWTEventListener eventListener;

        private SpotlightPanel(Pane networkPane) {
            this.networkPane = networkPane;
            this.eventListener = new AWTEventListener() {
                public void eventDispatched(AWTEvent e) {
                    MouseEvent me = (MouseEvent) e;
                    if (me.getButton() > 0) {
                        hideSpotlightPanel();
                    }
                }
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(this.eventListener, AWTEvent.MOUSE_EVENT_MASK);
        }

        private void hideSpotlightPanel() {
            setVisible(false);
            // We don't need the glass pane anymore.
            Toolkit.getDefaultToolkit().removeAWTEventListener(this.eventListener);
            // Remove our reference to the network pane.
            // Since panes can change, holding on to the network pane would cause a memory leak.
            networkPane = null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Mask out the spotlight
            // The point refers to the location of the "new node" button in the network pane.
            // This position is hard-coded.
            Point pt = SwingUtilities.convertPoint(networkPane, 159, 12, getRootPane());
            Rectangle2D screen = new Rectangle2D.Double(0, 0, getWidth(), getHeight());
            Ellipse2D spotlight = new Ellipse2D.Float(pt.x - 42, pt.y - 42, 84, 84);
            Area mask = new Area(screen);
            mask.subtract(new Area(spotlight));

            // Fill the mask
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fill(mask);
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
