package nodebox.client;

import nodebox.base.Preconditions;
import nodebox.node.*;
import nodebox.node.event.NodeDirtyEvent;

import javax.swing.*;
import javax.swing.event.EventListenerList;
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
    public HashMap<Parameter, String> changedCodeParameters = new HashMap<Parameter, String>();
    private SpotlightPanel spotlightPanel;

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
        requestActiveNetworkUpdate();
        //renderThread = new RenderThread();
        //renderThread.start();

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

    public List<Pane> getDocumentPanes() {
        List<Pane> panes = new ArrayList<Pane>();
        for (DocumentFocusListener listener : documentFocusListeners.getListeners(DocumentFocusListener.class)) {
            if (listener instanceof Pane)
                panes.add((Pane) listener);
        }
        return panes;
    }

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

    public boolean exportToFile(File file, ImageFormat format) {
        return exportToFile(file, activeNetwork, format);
    }

    public boolean exportToFile(File file, Node exportNetwork, ImageFormat format) {
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
            exportToMovieFile(chosenFile, d.getFromValue(), d.getToValue(), d.getQuality(), d.getFormat());
            return true;
        }
        return false;
    }

    private void exportToMovieFile(File file, final int fromValue, final int toValue, final Movie.CompressionQuality quality, final MovieFormat format) {
        final ProgressDialog d = new InterruptableProgressDialog(this, null);
        d.setTaskCount(toValue - fromValue + 1);
        d.setTitle("Exporting " + (toValue - fromValue + 1) + " frames...");
        d.setVisible(true);
        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.setAlwaysOnTop(true);

        file = format.ensureFileExtension(file);
        String xml = nodeLibrary.toXml();
        final NodeLibrary exportLibrary = NodeLibrary.load(nodeLibrary.getName(), xml, getManager());
        exportLibrary.setFile(nodeLibrary.getFile());
        final Node exportNetwork = exportLibrary.getRootNode();
        final int width = (int) exportNetwork.asFloat(NodeLibrary.CANVAS_WIDTH);
        final int height = (int) exportNetwork.asFloat(NodeLibrary.CANVAS_HEIGHT);
        final Movie movie = new Movie(file.getAbsolutePath(), width, height, Movie.CodecType.H264, quality, format,  false);
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
        if (parameter.getNode() == nodeLibrary.getRootNode()) {
            nodeLibrary.setVariable(parameter.getName(), parameter.asString());
        }
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
        requestActiveNetworkUpdate();
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

//    public void createNewLibrary(String libraryName) {
//        // First check if a library with this name already exists.
//        if (getManager().hasLibrary(libraryName)) {
//            JOptionPane.showMessageDialog(this, "A library with the name \"" + libraryName + "\" already exists.");
//            return;
//        }
//        getManager().createPythonLibrary(libraryName);
//    }


    public void close() {
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
