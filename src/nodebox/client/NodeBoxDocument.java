package nodebox.client;

import nodebox.client.movie.Movie;
import nodebox.client.movie.VideoFormat;
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
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static nodebox.base.Preconditions.checkArgument;
import static nodebox.base.Preconditions.checkNotNull;

/**
 * A NodeBoxDocument manages a NodeLibrary.
 */
public class NodeBoxDocument extends JFrame implements WindowListener, ViewerEventListener {

    private final static String WINDOW_MODIFIED = "windowModified";

    public static String lastFilePath;
    public static String lastExportPath;

    private File documentFile;
    private boolean documentChanged;
    private static Logger logger = Logger.getLogger("nodebox.client.NodeBoxDocument");
    private AnimationTimer animationTimer;
    private ArrayList<ParameterEditor> parameterEditors = new ArrayList<ParameterEditor>();
    private boolean loaded = false;
    private SpotlightPanel spotlightPanel;

    private final Map<Node, String> workingCodeMap = new WeakHashMap<Node, String>();

    private UndoManager undoManager = new UndoManager();
    private boolean holdEdits = false;
    private String lastEditType = null;
    private Object lastEditObject = null;

    private NodeLibrary nodeLibrary;
    private Node activeNetwork;
    private Node activeNode;
    private String activeCodeParameter = "_code";

    // GUI components
    private final NodeBoxMenuBar menuBar;
    private final AnimationBar animationBar;
    private final AddressBar addressBar;
    private final Viewer viewer;
    private final EditorPane editorPane;
    private final ParameterView parameterView;
    private final NetworkView networkView;

    public static NodeBoxDocument getCurrentDocument() {
        return Application.getInstance().getCurrentDocument();
    }

    public static NodeLibraryManager getManager() {
        return Application.getInstance().getManager();
    }

    public NodeBoxDocument(NodeLibrary library) {
        setNodeLibrary(library);
        JPanel rootPanel = new JPanel(new BorderLayout());
        ViewerPane viewerPane = new ViewerPane();
        viewer = viewerPane.getViewer();
        viewer.setEventListener(this);
        editorPane = new EditorPane();
        editorPane.setDelegate(new EditorPane.Delegate() {
            public void codeEdited(EditorPane editorPane, String source) {
                Parameter codeParameter = activeNode.getParameter(activeCodeParameter);
                String currentSource = codeParameter.asCode().getSource();
                if (!currentSource.equals(source)) {
                    workingCodeMap.put(activeNode, source);
                    networkView.codeChanged(activeNode, true);
                } else {
                    workingCodeMap.remove(activeNode);
                }
            }

            public void codeReloaded(EditorPane editorPane, String source) {
                setActiveNodeCode(source);
            }

            public void codeParameterChanged(EditorPane editorPane, String codeParameter) {

                // TODO Implement
            }
        });
        ParameterPane parameterPane = new ParameterPane();
        parameterPane.setEditMetadataListener(new ParameterPane.EditMetadataListener() {
            public void onEditMetadata() {
                if (activeNode == null) return;
                addEdit("Node Metadata");
                NodeAttributesEditor editor = new NodeAttributesEditor(activeNode);
                JDialog editorDialog = new JDialog(NodeBoxDocument.this, activeNode.getName() + " Metadata");
                editorDialog.getContentPane().add(editor);
                editorDialog.setSize(580, 751);
                editorDialog.setResizable(false);
                editorDialog.setModal(true);
                editorDialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
                editorDialog.setLocationRelativeTo(NodeBoxDocument.this);
                editorDialog.setVisible(true);
            }
        });
        parameterView = parameterPane.getParameterView();
        parameterView.setDocument(this); // TODO Remove this once parameter view is fully decoupled.
        NetworkPane networkPane = new NetworkPane();
        networkView = networkPane.getNetworkView();
        networkView.setDocument(this); // TODO Remove this once it is fully decoupled.
        networkView.setDelegate(new NetworkView.Delegate() {
            public void activeNodeChanged(Node node) {
                setActiveNode(node);
            }
        });
        PaneSplitter viewEditorSplit = new PaneSplitter(NSplitter.Orientation.VERTICAL, viewerPane, editorPane);
        PaneSplitter parameterNetworkSplit = new PaneSplitter(NSplitter.Orientation.VERTICAL, parameterPane, networkPane);
        PaneSplitter topSplit = new PaneSplitter(NSplitter.Orientation.HORIZONTAL, viewEditorSplit, parameterNetworkSplit);
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
        updateEditorPaneSource();
        parameterView.setActiveNode(activeNode);
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
     * Set the parameter to the given value.
     *
     * @param parameter the parameter to set
     * @param value     the new value
     */
    public void setParameterValue(Parameter parameter, Object value) {
        addEdit("Change Value", "changeValue", parameter);
        parameter.set(value);
        if (parameter.getNode() == nodeLibrary.getRootNode()) {
            nodeLibrary.setVariable(parameter.getName(), parameter.asString());
        }

        parameterView.updateParameterValue(parameter, value);
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
        viewer.setHandleEnabled(activeNetwork.hasEnabledHandle());
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
//        if (activeNode != null) {
//            handle = activeNode.createHandle();
//            if (handle != null) {
//                handle.setViewer(this);
//            }
//        } else {
//            handle = null;
//        }
//        checkIfHandleEnabled();
        viewer.repaint();
        parameterView.setActiveNode(activeNode);
        networkView.setActiveNode(activeNode);
        updateEditorPaneSource();
    }

    private void updateEditorPaneSource() {
        if (activeNode != null) {
            if (workingCodeMap.containsKey(activeNode)) {
                editorPane.setSource(workingCodeMap.get(activeNode));
            } else {
                Parameter codeParameter = activeNode.getParameter(activeCodeParameter);
                editorPane.setSource(codeParameter.asCode().getSource());
            }
        } else {
            editorPane.setSource(null);
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
        // TODO: Why?
        if (activeNode != null) {
            viewer.setHandleEnabled(activeNode.hasEnabledHandle());
        }
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

    public void setActiveNodeCode(String source) {
        if (activeNode == null) return;
        Parameter pCode = activeNode.getParameter(activeCodeParameter);
        if (pCode == null) return;
        NodeCode code = new PythonCode(source);
        pCode.set(code);
        if (activeCodeParameter.equals("_handle")) {
            viewer.reloadHandle();
        }
        // Since the code is now up-to-date, remove it from the temporary map.
        workingCodeMap.remove(activeNode);
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
        networkView.cutSelected();
    }

    public void copy() {
        networkView.copySelected();
    }

    public void paste() {
        addEdit("Paste node");
        networkView.pasteSelected();
    }

    public void deleteSelected() {
        networkView.deleteSelected();
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
        setActiveNodeCode(editorPane.getSource());
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

    //// Window events ////

    public void windowOpened(WindowEvent e) {
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
