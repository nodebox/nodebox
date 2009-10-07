package nodebox.client;

import nodebox.graphics.Grob;
import nodebox.graphics.Rect;
import nodebox.graphics.Text;
import nodebox.node.*;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A NodeBoxDocument manages a NodeLibrary.
 */
public class NodeBoxDocument extends JFrame implements DirtyListener, WindowListener {

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
    private UndoManager undoManager = new UndoManager();
    private AddressBar addressBar;
    //private RenderThread renderThread;
    private ArrayList<ParameterEditor> parameterEditors = new ArrayList<ParameterEditor>();

    public static NodeBoxDocument getCurrentDocument() {
        for (Frame f : JFrame.getFrames()) {
            if (f.isActive() && f instanceof NodeBoxDocument) {
                return (NodeBoxDocument) f;
            }
        }
        return null;
    }

    private class DocumentObservable extends Observable {
        public void setChanged() {
            super.setChanged();
        }
    }

    public static class AllControlsType extends Builtin {
        protected Node createInstance() {
            NodeLibrary library = new NodeLibrary("allcontrols");
            Node n = Node.ROOT_NODE.newInstance(library, "allcontrols", Canvas.class);
            n.addParameter("angle", Parameter.Type.FLOAT).setWidget(Parameter.Widget.ANGLE);
            n.addParameter("color", Parameter.Type.COLOR).setWidget(Parameter.Widget.COLOR);
            n.addParameter("file", Parameter.Type.STRING).setWidget(Parameter.Widget.FILE);
            n.addParameter("float", Parameter.Type.FLOAT).setWidget(Parameter.Widget.FLOAT);
            n.addParameter("font", Parameter.Type.STRING).setWidget(Parameter.Widget.FONT);
            n.addParameter("gradient", Parameter.Type.COLOR).setWidget(Parameter.Widget.GRADIENT);
            n.addParameter("image", Parameter.Type.STRING).setWidget(Parameter.Widget.IMAGE);
            n.addParameter("int", Parameter.Type.INT).setWidget(Parameter.Widget.INT);
            n.addParameter("menu", Parameter.Type.STRING).setWidget(Parameter.Widget.MENU);
            n.addParameter("seed", Parameter.Type.INT).setWidget(Parameter.Widget.SEED);
            n.addParameter("string", Parameter.Type.STRING).setWidget(Parameter.Widget.STRING);
            n.addParameter("text", Parameter.Type.STRING).setWidget(Parameter.Widget.TEXT);
            n.addParameter("toggle", Parameter.Type.INT).setWidget(Parameter.Widget.TOGGLE);
            n.addParameter("noderef", Parameter.Type.STRING).setWidget(Parameter.Widget.NODEREF);
            Parameter pMenu = n.getParameter("menu");
            pMenu.addMenuItem("red", "Red");
            pMenu.addMenuItem("green", "Green");
            pMenu.addMenuItem("blue", "Blue");
            pMenu.setValue("blue");
            return n;
        }

        private void addText(nodebox.graphics.Canvas c, Node node, String parameterName, double y) {
            c.add(new Text(parameterName + ": " + node.asString(parameterName), 10, 24 + y * 24));
        }

        public Object cook(Node node, ProcessingContext context) {
            nodebox.graphics.Canvas c = new nodebox.graphics.Canvas();
            addText(c, node, "angle", 1);
            addText(c, node, "color", 2);
            addText(c, node, "file", 3);
            addText(c, node, "float", 4);
            addText(c, node, "font", 5);
            addText(c, node, "gradient", 6);
            addText(c, node, "image", 7);
            addText(c, node, "int", 8);
            addText(c, node, "menu", 9);
            addText(c, node, "seed", 10);
            addText(c, node, "string", 11);
            addText(c, node, "text", 12);
            addText(c, node, "toggle", 13);
            addText(c, node, "noderef", 14);
            node.setOutputValue(c);
            return true;
        }
    }

    public NodeBoxDocument() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        ViewerPane viewPane = new ViewerPane(this);
        EditorPane editorPane = new EditorPane(this);
        ParameterPane parameterPane = new ParameterPane(this);
        NetworkPane networkPane = new NetworkPane(this);
        PaneSplitter viewEditorSplit = new PaneSplitter(PaneSplitter.VERTICAL_SPLIT, viewPane, editorPane);
        PaneSplitter parameterNetworkSplit = new PaneSplitter(PaneSplitter.VERTICAL_SPLIT, parameterPane, networkPane);
        PaneSplitter topSplit = new PaneSplitter(PaneSplitter.HORIZONTAL_SPLIT, viewEditorSplit, parameterNetworkSplit);
        addressBar = new AddressBar(this);
        rootPanel.add(addressBar, BorderLayout.NORTH);
        rootPanel.add(topSplit, BorderLayout.CENTER);
        setContentPane(rootPanel);
        setLocationByPlatform(true);
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        updateTitle();
        setJMenuBar(new NodeBoxMenuBar());
        nodeLibrary = new NodeLibrary("untitled");
        setNodeLibrary(nodeLibrary);
        //renderThread = new RenderThread();
        //renderThread.start();
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

    public void setNodeLibrary(NodeLibrary nodeLibrary) {
        this.nodeLibrary = nodeLibrary;
        setActiveNetwork(nodeLibrary.getRootNode());
    }

    public Node getActiveNetwork() {
        return activeNetwork;
    }

    public void setActiveNetwork(Node activeNetwork) {
        Node oldNetwork = this.activeNetwork;
        if (oldNetwork != null)
            oldNetwork.removeDirtyListener(this);
        this.activeNetwork = activeNetwork;
        if (activeNetwork != null)
            activeNetwork.addDirtyListener(this);
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
            setActiveNode(null);
        }
        if (activeNetwork != null) {
            updateActiveNetwork();
        }
        addressBar.setNode(activeNetwork);
    }

    public Node getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(Node activeNode) {
        this.activeNode = activeNode;
        fireActiveNodeChanged();
    }

    public NodeLibraryManager getManager() {
        return Application.getInstance().getManager();
    }

    //// Parameter editor actions ////

    public void addParameterEditor(ParameterEditor editor) {
        if (parameterEditors.contains(editor)) return;
        parameterEditors.add(editor);
    }

    public void removeParameterEditor(ParameterEditor editor) {
        parameterEditors.remove(editor);
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

    public static void open(File file) {
        lastFilePath = file.getParentFile().getAbsolutePath();
        String path;
        try {
            path = file.getCanonicalPath();
            for (NodeBoxDocument doc : Application.getInstance().getDocuments()) {

                try {
                    if (doc.getDocumentFile() == null) continue;
                    if (doc.getDocumentFile().getCanonicalPath().equals(path)) {
                        doc.toFront();
                        doc.requestFocus();
                        NodeBoxMenuBar.addRecentFile(file);
                        return;
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "The document " + doc.getDocumentFile() + " refers to path with errors", e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "The document " + file + " refers to path with errors", e);
        }
        NodeBoxDocument doc = Application.getInstance().createNewDocument();
        if (doc.readFromFile(file)) {
            doc.setDocumentFile(file);
        }
        NodeBoxMenuBar.addRecentFile(file);
    }

    public boolean readFromFile(String path) {
        return readFromFile(new File(path));
    }

    public boolean readFromFile(File file) {
        try {
            NodeLibrary library = NodeLibrary.load(file, getManager());
            setNodeLibrary(library);
            setDocumentFile(file);
            // The parsed network is now stored in the reader
            documentChanged = false;
            return true;
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Error while parsing" + file, e);
            ExceptionDialog d = new ExceptionDialog(this, e);
            d.setVisible(true);
        }
        return false;
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
        // todo: file export only works on grobs.
        if (activeNetwork == null || activeNetwork.getRenderedChild() == null) return false;
        Object outputValue = activeNetwork.getRenderedChild().getOutputValue();
        nodebox.graphics.Canvas canvas;
        if (outputValue instanceof nodebox.graphics.Canvas) {
            canvas = (nodebox.graphics.Canvas) outputValue;
        } else if (outputValue instanceof Grob) {
            Grob g = (Grob) outputValue;
            Rect bounds = g.getBounds();
            canvas = new nodebox.graphics.Canvas(bounds.getWidth(), bounds.getHeight());
            // We need to translate the canvas to compensate for the x/y value of the grob.
            double dx = bounds.getWidth() / 2 + bounds.getX();
            double dy = bounds.getHeight() / 2 + bounds.getY();
            // TODO: canvas.translate(-dx, -dy);
            canvas.add(g);
        } else {
            throw new RuntimeException("This type of output cannot be exported " + outputValue);
        }
        canvas.save(file);
        return true;
    }


    public void markChanged() {
        if (!documentChanged) {
            documentChanged = true;
            updateTitle();
            getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.TRUE);
        }
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void undo() {
        // TODO: Implement undo
        JOptionPane.showMessageDialog(this, "Undo is not implemented yet.", "NodeBox", JOptionPane.ERROR_MESSAGE);
    }

    public void redo() {
        // TODO: Implement redo
        JOptionPane.showMessageDialog(this, "Redo is not implemented yet.", "NodeBox", JOptionPane.ERROR_MESSAGE);
    }

    public void cut() {
        // TODO: Implement cut/copy/paste
        JOptionPane.showMessageDialog(this, "Cut/copy/paste is not implemented yet.", "NodeBox", JOptionPane.ERROR_MESSAGE);
    }

    public void copy() {
        JOptionPane.showMessageDialog(this, "Cut/copy/paste is not implemented yet.", "NodeBox", JOptionPane.ERROR_MESSAGE);
    }

    public void paste() {
        JOptionPane.showMessageDialog(this, "Cut/copy/paste is not implemented yet.", "NodeBox", JOptionPane.ERROR_MESSAGE);
    }

    public void deleteSelected() {
        // TODO: Find network view, delete selected.
        JOptionPane.showMessageDialog(this, "Delete selected.", "NodeBox", JOptionPane.ERROR_MESSAGE);        
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

    public boolean reloadActiveNode() {
        if (activeNode == null) return false;
        Pane p = SwingUtils.getPaneForComponent(getFocusOwner());
        if (p == null || !(p instanceof EditorPane)) return false;
        return ((EditorPane) p).reload();
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
            // TODO: On mac, the application doesn't quit after the last document is closed.
            if (!PlatformUtils.onMac()) {
                //    // On mac, the application doesn't quit after the last document is closed.
                //    setVisible(false);
                //} else {
                System.exit(0);
            }
        }
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
    }

    public void windowDeactivated(WindowEvent e) {
    }

    //// Network events ////

    public void nodeDirty(final Node node) {
        if (node != activeNetwork) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // If meanwhile the node has been marked clean, ignore the event.
                if (!node.isDirty()) return;
                markChanged();
                updateActiveNetwork();
            }
        });
    }

    private void updateActiveNetwork() {
        try {
            activeNetwork.update();
        } catch (ProcessingError processingError) {
            Logger.getLogger("NodeBoxDocument").log(Level.WARNING, "Error while processing", processingError);
        }
    }

    private void doRender() {
        //renderThread.render(activeNetwork);
    }

    public void nodeUpdated(Node node, ProcessingContext context) {
        // Just here to statisfy DirtyListener interface.
    }

    //// Document Action classes ////

    public class OpenAction extends AbstractAction {
        public OpenAction() {
            putValue(NAME, "Open...");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_O));
        }

        public void actionPerformed(ActionEvent e) {
            File chosenFile = FileUtils.showOpenDialog(NodeBoxDocument.this, lastFilePath, "ndbx", "NodeBox Document");
            if (chosenFile != null) {
                open(chosenFile);
            }
        }
    }

}
