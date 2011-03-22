package nodebox.client;

import nodebox.base.Preconditions;
import nodebox.graphics.Grob;
import nodebox.graphics.PDFRenderer;
import nodebox.node.*;
import nodebox.node.event.NodeDirtyEvent;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A NodeBoxDocument manages a NodeLibrary.
 */
public class NodeBoxDocument extends JFrame implements WindowListener, NodeEventListener {

    private final static String WINDOW_MODIFIED = "windowModified";

    public static String lastFilePath;
    public static String lastExportPath;

    private NodeLibrary nodeLibrary;
    private Node activeNetwork;
    private Node activeNode;
    private File documentFile;
    private boolean documentChanged;
    private static Logger logger = Logger.getLogger("nodebox.client.NodeBoxDocument");
    private EventListenerList documentFocusListeners = new EventListenerList();
    private EventListenerList codeChangeListeners = new EventListenerList();
    private UndoManager undoManager = new UndoManager();
    private boolean holdEdits = false;
    private AddressBar addressBar;
    private NodeBoxMenuBar menuBar;
    private AnimationBar animationBar;
    private AnimationTimer animationTimer;
    private String lastEditType = null;
    private Object lastEditObject = null;
    //private RenderThread renderThread;
    private ArrayList<ParameterEditor> parameterEditors = new ArrayList<ParameterEditor>();
    private boolean loaded = false;
    public HashMap<Parameter, String> changedCodeParameters = new HashMap<Parameter, String>();;

    public static NodeBoxDocument getCurrentDocument() {
        return Application.getInstance().getCurrentDocument();
    }

    private class DocumentObservable extends Observable {
        public void setChanged() {
            super.setChanged();
        }
    }

    public NodeBoxDocument(NodeLibrary library) {
        setNodeLibrary(library);
        JPanel rootPanel = new JPanel(new BorderLayout());
        ViewerPane viewPane = new ViewerPane(this);
        EditorPane editorPane = new EditorPane(this);
        ParameterPane parameterPane = new ParameterPane(this);
        NetworkPane networkPane = new NetworkPane(this);
        PaneSplitter viewEditorSplit = new PaneSplitter(NSplitter.Orientation.VERTICAL, viewPane, editorPane);
        PaneSplitter parameterNetworkSplit = new PaneSplitter(NSplitter.Orientation.VERTICAL, parameterPane, networkPane);
        PaneSplitter topSplit = new PaneSplitter(NSplitter.Orientation.HORIZONTAL, viewEditorSplit, parameterNetworkSplit);
        addressBar = new AddressBar(this);
        rootPanel.add(addressBar, BorderLayout.NORTH);
        rootPanel.add(topSplit, BorderLayout.CENTER);

        if (Application.FLAG_ENABLE_ANIMATION) {
            animationTimer = new AnimationTimer(this);
            animationBar = new AnimationBar(this);
            rootPanel.add(animationBar, BorderLayout.SOUTH);
        }

        setContentPane(rootPanel);
        setLocationByPlatform(true);
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        updateTitle();
        menuBar = new NodeBoxMenuBar(this);
        setJMenuBar(menuBar);
        loaded = true;
        requestActiveNetworkUpdate();
        //renderThread = new RenderThread();
        //renderThread.start();
    }

    public NodeBoxDocument(File file) throws RuntimeException {
        this(NodeLibrary.load(file, Application.getInstance().getManager()));
        lastFilePath = file.getParentFile().getAbsolutePath();
        setDocumentFile(file);
    }

    //// Document events ////

    public void addDocumentFocusListener(DocumentFocusListener l) {
        documentFocusListeners.add(DocumentFocusListener.class, l);
    }

    public void removeDocumentFocusListener(DocumentFocusListener l) {
        documentFocusListeners.remove(DocumentFocusListener.class, l);
    }

    public void fireActiveNetworkChanged() {
        for (EventListener l : documentFocusListeners.getListeners(DocumentFocusListener.class)) {
            ((DocumentFocusListener) l).currentNodeChanged(activeNetwork);
        }
    }

    public void fireActiveNodeChanged() {
        for (EventListener l : documentFocusListeners.getListeners(DocumentFocusListener.class)) {
            ((DocumentFocusListener) l).focusedNodeChanged(activeNode);
        }
    }

    public NodeLibrary getNodeLibrary() {
        return nodeLibrary;
    }

    public void setNodeLibrary(NodeLibrary newNodeLibrary) {
        Preconditions.checkNotNull(newNodeLibrary, "Node library cannot be null.");
        List<NodeEventListener> listeners = null;
        NodeLibrary oldLibrary = this.nodeLibrary;
        if (oldLibrary != null) {
            // Remove the listeners from the old node library.
            listeners = oldLibrary.getListeners();
            for (NodeEventListener listener : listeners) {
                oldLibrary.removeListener(listener);
            }
        }
        this.nodeLibrary = newNodeLibrary;
        // Add the listeners to the new library.
        if (listeners != null) {
            for (NodeEventListener listener : listeners) {
                newNodeLibrary.addListener(listener);
            }
        } else {
            newNodeLibrary.addListener(this);

        }
        setActiveNetwork(newNodeLibrary.getRootNode());
    }

    public void addNodeLibraryListener(NodeEventListener listener) {
        nodeLibrary.addListener(listener);
    }

    public void removeNodeLibraryListener(NodeEventListener listener) {
        nodeLibrary.removeListener(listener);
    }

    public Node getActiveNetwork() {
        return activeNetwork;
    }

    public String getActiveNetworkPath() {
        if (activeNetwork == null) return "";
        return activeNetwork.getAbsolutePath();
    }

    public void setActiveNetwork(Node activeNetwork) {
        this.activeNetwork = activeNetwork;
        fireActiveNetworkChanged();
        if (activeNetwork != null && !activeNetwork.isEmpty()) {
            // Set the active node to the rendered child if available.
            if (activeNetwork.getRenderedChild() != null) {
                setActiveNode(activeNetwork.getRenderedChild());
            } else {
                // Otherwise set it to the first node.
                setActiveNode(activeNetwork.getChildAt(0));
            }
        } else {
            setActiveNode((Node) null);
        }
        if (activeNetwork != null) {
            requestActiveNetworkUpdate();
        }
    }

    public void setActiveNetwork(String path) {
        Node network = nodeLibrary.getNodeForPath(path);
        setActiveNetwork(network);
    }

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
     * @param activeNode the node to change to.
     */
    public void setActiveNode(Node activeNode) {
        // Ensure that the active node is in the active network.
        if (activeNode != null && activeNode.getParent() != activeNetwork) {
            setActiveNetwork(activeNode.getParent());
        }
        this.activeNode = activeNode;
        fireActiveNodeChanged();
    }

    /**
     * Set the active node based on an absolute path.
     * This will also change the active network if necessary.
     *
     * @param path the absolute path
     * @see #getActiveNodePath()
     * @see nodebox.node.Node#getAbsolutePath()
     */
    public void setActiveNode(String path) {
        Node node = nodeLibrary.getNodeForPath(path);
        setActiveNode(node);
    }

    public NodeLibraryManager getManager() {
        return Application.getInstance().getManager();
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

    public void addCodeChangeListener(CodeChangeListener l) {
        codeChangeListeners.add(CodeChangeListener.class, l);
    }

    public void removeCodeChangeListener(CodeChangeListener l) {
        codeChangeListeners.remove(CodeChangeListener.class, l);
    }

    public String getChangedCodeForParameter(Parameter parameter) {
        return changedCodeParameters.get(parameter);
    }

    public void setChangedCodeForParameter(Parameter parameter, String code) {
        changedCodeParameters.put(parameter, code);
    }

    public void removeChangedCodeForParameter(Parameter parameter) {
        changedCodeParameters.remove(parameter);
        Node node = parameter.getNode();
        for (Parameter p : changedCodeParameters.keySet()) {
            if (p.getNode() == node)
                return;
        }
        fireCodeChanged(node, false);
    }

    public void fireCodeChanged(Node node, boolean changed) {
        for (EventListener l : codeChangeListeners.getListeners(CodeChangeListener.class)) {
            ((CodeChangeListener) l).codeChanged(node, changed);
        }
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

    public boolean shouldClose() {
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

    public boolean saveToFile(File file) {
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

    public boolean exportToFile(File file) {
        return exportToFile(file, activeNetwork);
    }

    public boolean exportToFile(File file, Node exportNetwork) {
        // Make sure the file ends with ".pdf".
        String fullPath = null;
        try {
            fullPath = file.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException("Unable to access file " + file, e);
        }
        if (!fullPath.toLowerCase(Locale.US).endsWith(".pdf")) {
            fullPath = fullPath.concat(".pdf");
        }
        file = new File(fullPath);

        // todo: file export only works on grobs.
        if (exportNetwork == null || exportNetwork.getRenderedChild() == null) return false;
        Object outputValue = exportNetwork.getRenderedChild().getOutputValue();
        if (outputValue instanceof Grob) {
            Grob g = (Grob) outputValue;
            PDFRenderer.render(g, file);
            return true;
        } else {
            throw new RuntimeException("This type of output cannot be exported " + outputValue);
        }
    }


    public void markChanged() {
        if (!documentChanged && loaded) {
            documentChanged = true;
            updateTitle();
            getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.TRUE);
        }
    }

    public void cut() {
        NetworkView networkView = currentNetworkView();
        if (networkView == null) {
            beep();
            return;
        }
        networkView.cutSelected();
    }

    public void copy() {
        NetworkView networkView = currentNetworkView();
        if (networkView == null) {
            beep();
            return;
        }
        networkView.copySelected();
    }

    public void paste() {
        addEdit("Paste node");
        NetworkView networkView = currentNetworkView();
        if (networkView == null) {
            beep();
            return;
        }
        networkView.pasteSelected();
    }

    private NetworkView currentNetworkView() {
        // Find current network view.
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) return null;
        Pane pane = (Pane) SwingUtilities.getAncestorOfClass(Pane.class, focusOwner);
        if (pane == null) return null;
        PaneView paneView = pane.getPaneView();
        if (!(paneView instanceof NetworkView)) return null;
        return (NetworkView) paneView;
    }


    public void deleteSelected() {
        NetworkView networkView = currentNetworkView();
        if (networkView == null) {
            beep();
            return;
        }
        networkView.deleteSelected();
    }

    private void updateTitle() {
        String postfix = "";
        if (!PlatformUtils.onMac()) { // todo: mac only code
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

    public boolean export() {
        File chosenFile = FileUtils.showSaveDialog(this, lastExportPath, "pdf", "PDF file");
        if (chosenFile != null) {
            lastExportPath = chosenFile.getParentFile().getAbsolutePath();
            return exportToFile(chosenFile);
        }
        return false;
    }

    public boolean exportRange() {
        File exportDirectory = lastExportPath == null ? null : new File(lastExportPath);
        ExportRangeDialog d = new ExportRangeDialog(this, exportDirectory);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        String exportPrefix = d.getExportPrefix();
        File directory = d.getExportDirectory();
        int fromValue = d.getFromValue();
        int toValue = d.getToValue();
        if (directory == null) return false;
        lastExportPath = directory.getAbsolutePath();
        exportRange(exportPrefix, directory, fromValue, toValue);
        return true;
    }

    public void exportRange(final String exportPrefix, final File directory, final int fromValue, final int toValue) {
        // Show the progress dialog
        final ProgressDialog d = new InterruptableProgressDialog(this, "Exporting...", toValue - fromValue + 1);
        d.setVisible(true);
        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.setAlwaysOnTop(true);

        String xml = nodeLibrary.toXml();
        final NodeLibrary exportLibrary = NodeLibrary.load(nodeLibrary.getName(), xml, getManager());
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
                            markTimeDependentNodesDirty(exportNetwork, frame);
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
                        File exportFile = new File(directory, exportPrefix + "-" + frame + ".pdf");
                        exportToFile(exportFile, exportNetwork);
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

    public boolean reloadActiveNode() {
        if (activeNode == null) return false;
        Pane p = SwingUtils.getPaneForComponent(getFocusOwner());
        if (p == null || !(p instanceof EditorPane)) return false;
        return ((EditorPane) p).reload();
    }

    //// Node operations ////

    /**
     * Create a node in the active network.
     * This node is based on a prototype.
     *
     * @param prototype the prototype node
     * @return the newly created node.
     */
    public Node createNode(Node prototype) {
        addEdit("Create Node");
        return getActiveNetwork().create(prototype);
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
    }

    /**
     * Change the rendered node to the given node
     *
     * @param node the node to set rendered
     */
    public void setRenderedNode(Node node) {
        addEdit("Set Rendered");
        node.setRendered();
    }

    /**
     * Remove the given node from the active network.
     *
     * @param node the node to remove.
     */
    public void removeNode(Node node) {
        addEdit("Remove Node");
        getActiveNetwork().remove(node);
    }

    /**
     * Create a connection from the given output to the given input.
     *
     * @param output the output port
     * @param input  the input port
     * @return the created connection
     */
    public Connection connect(Port output, Port input) {
        addEdit("Connect");
        return getActiveNetwork().connectChildren(input, output);
    }

    /**
     * Remove the given connection from the network.
     *
     * @param connection the connection to remove
     */
    public void disconnect(Connection connection) {
        addEdit("Disconnect");
        getActiveNetwork().disconnect(connection);
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
    }

    public void setParameterExpression(Parameter parameter, String expression) {
        addEdit("Change Parameter Expression");
        parameter.setExpression(expression);
    }

    public void clearParameterExpression(Parameter parameter) {
        addEdit("Clear Parameter Expression");
        parameter.clearExpression();
    }

    public void revertParameterToDefault(Parameter parameter) {
        addEdit("Revert Parameter to Default");
        parameter.revertToDefault();
    }

    public float getFrame() {
        return nodeLibrary.getFrame();
    }

    public void setFrame(float frame) {
        nodeLibrary.setFrame(frame);
        animationBar.updateFrame();
        markTimeDependentNodesDirty(activeNetwork, frame);
        requestActiveNetworkUpdate();
    }

    public void markTimeDependentNodesDirty(Node network, float frame) {
        // TODO: This is a really hacky version of finding time-dependent nodes.
        // We simply traverse through the first level of the network and find all
        // nodes that have parameters with expression with the word FRAME in them.
        // Those are marked dirty.
        for (Node n : network.getChildren()) {
            for (Parameter p : n.getParameters()) {
                if (p.hasExpression() && p.getExpression().contains("FRAME")) {
                    p.markDirty();
                }
            }
        }
    }

    public void nextFrame() {
        setFrame(getFrame() + 1);
    }

    public void startAnimation() {
        animationTimer.start();
    }

    public void stopAnimation() {
        animationTimer.stop();
    }

    public void resetAnimation() {
        stopAnimation();
        setFrame(1);
    }

//    public void createNewLibrary(String libraryName) {
//        // First check if a library with this name already exists.
//        if (getManager().hasLibrary(libraryName)) {
//            JOptionPane.showMessageDialog(this, "A library with the name \"" + libraryName + "\" already exists.");
//            return;
//        }
//        getManager().createPythonLibrary(libraryName);
//    }


    public void close() {
        if (shouldClose()) {
            //renderThread.shutdown();
            Application.getInstance().getManager().remove(nodeLibrary);
            Application.getInstance().removeDocument(NodeBoxDocument.this);
            for (ParameterEditor editor : parameterEditors) {
                editor.dispose();
            }
            dispose();
            // On Mac the application does not close if the last window is closed.
            if (PlatformUtils.onMac()) return;
            // If there are no more documents, exit the application.
            if (Application.getInstance().getDocumentCount() == 0) {
                System.exit(0);
            }
        }
    }

    private void beep() {
        Toolkit.getDefaultToolkit().beep();
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

    //// Network events ////

    public void receive(NodeEvent event) {
        if (event instanceof NodeDirtyEvent && event.getSource() == activeNetwork) {
            requestActiveNetworkUpdate();
        }
    }

    private void requestActiveNetworkUpdate() {
        if (!loaded) return;
        addressBar.setProgressVisible(true);


        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // If meanwhile the node has been marked clean, ignore the event.
                if (!activeNetwork.isDirty()) return;
                try {
                    activeNetwork.update();
                } catch (ProcessingError processingError) {
                    Logger.getLogger("NodeBoxDocument").log(Level.WARNING, "Error while processing", processingError);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            addressBar.setProgressVisible(false);
                        }
                    });
                }
            }
        });
    }
}
