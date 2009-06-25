package nodebox.client;

import nodebox.graphics.Grob;
import nodebox.graphics.Rect;
import nodebox.graphics.Text;
import nodebox.node.*;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A NodeBoxDocument manages a NodeLibrary.
 */
public class NodeBoxDocument extends JFrame implements DirtyListener, WindowListener {

    private final static String WINDOW_MODIFIED = "windowModified";

    private static String lastFilePath;
    private static String lastExportPath;
    private static Preferences recentFilesPreferences = Preferences.userRoot().node("/net/nodebox/recent");

    public NewAction newAction = new NewAction();
    public OpenAction openAction = new OpenAction();
    public CloseAction closeAction = new CloseAction();
    public SaveAction saveAction = new SaveAction();
    public SaveAsAction saveAsAction = new SaveAsAction();
    public RevertAction revertAction = new RevertAction();
    public ExportAction exportAction = new ExportAction();
    public QuitAction quitAction = new QuitAction();

    public UndoAction undoAction = new UndoAction();
    public RedoAction redoAction = new RedoAction();
    public CutAction cutAction = new CutAction();
    public CopyAction copyAction = new CopyAction();
    public PasteAction pasteAction = new PasteAction();
    public DeleteAction deleteAction = new DeleteAction();

    public ReloadAction reloadAction = new ReloadAction();
    //public NewLibraryAction newLibraryAction = new NewLibraryAction();

    public MinimizeAction minimizeAction = new MinimizeAction();
    public ZoomAction zoomAction = new ZoomAction();
    public BringAllToFrontAction bringAllToFrontAction = new BringAllToFrontAction();

    public NodeboxSiteAction nodeboxSiteAction = new NodeboxSiteAction();

    private JMenu recentFileMenu;

    private NodeLibrary nodeLibrary;
    private Node activeNetwork;
    private Node activeNode;
    private File documentFile;
    private boolean documentChanged;
    private static Logger logger = Logger.getLogger("nodebox.client.NodeBoxDocument");
    private EventListenerList documentFocusListeners = new EventListenerList();
    private UndoManager undo = new UndoManager();
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
        initMenu();
        nodeLibrary = new NodeLibrary("untitled");
        setNodeLibrary(nodeLibrary);
        //renderThread = new RenderThread();
        //renderThread.start();
    }

    private static void addRecentFile(File f) {
        File canonicalFile;
        try {
            canonicalFile = f.getCanonicalFile();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not get canonical file name", e);
            return;
        }
        ArrayList<File> fileList = getRecentFiles();
        // If the recent file was already in the list, remove it and add it to the top.
        // If the list did not contain the file, the remove call does nothing.
        fileList.remove(canonicalFile);
        fileList.add(0, canonicalFile);
        writeRecentFiles(fileList);
        for (NodeBoxDocument doc : Application.getInstance().getDocuments()) {
            doc.buildRecentFileMenu();
        }
    }

    private static void writeRecentFiles(ArrayList<File> fileList) {
        int i = 1;
        for (File f : fileList) {
            try {
                recentFilesPreferences.put(String.valueOf(i), f.getCanonicalPath());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not get canonical file name", e);
                return;
            }
            i++;
            if (i > 10) break;
        }
        try {
            recentFilesPreferences.flush();
        } catch (BackingStoreException e) {
            logger.log(Level.WARNING, "Could not write recent files preferences", e);
        }
    }

    private static ArrayList<File> getRecentFiles() {
        ArrayList<File> fileList = new ArrayList<File>(10);
        for (int i = 1; i <= 10; i++) {
            File file = new File(recentFilesPreferences.get(String.valueOf(i), ""));
            if (file.exists()) {
                fileList.add(file);
            }
        }
        return fileList;
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(newAction);
        fileMenu.add(openAction);
        recentFileMenu = new JMenu("Open Recent");
        buildRecentFileMenu();
        fileMenu.add(recentFileMenu);
        fileMenu.addSeparator();
        fileMenu.add(closeAction);
        fileMenu.add(saveAction);
        fileMenu.add(saveAsAction);
        fileMenu.add(revertAction);
        fileMenu.addSeparator();
        fileMenu.add(exportAction);
        if (!PlatformUtils.onMac()) {
            fileMenu.addSeparator();
            fileMenu.add(quitAction);
        }
        menuBar.add(fileMenu);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(undoAction);
        editMenu.add(redoAction);
        editMenu.addSeparator();
        editMenu.add(cutAction);
        editMenu.add(copyAction);
        editMenu.add(pasteAction);
        editMenu.addSeparator();
        editMenu.add(deleteAction);
        menuBar.add(editMenu);

        // Node menu
        JMenu pythonMenu = new JMenu("Node");
        pythonMenu.add(reloadAction);
        //pythonMenu.add(newLibraryAction);
        menuBar.add(pythonMenu);

        // Window menu
        JMenu windowMenu = new JMenu("Window");
        JMenu layoutMenu = new JMenu("Layout");
        //layoutMenu.add(new )
        windowMenu.add(minimizeAction);
        windowMenu.add(zoomAction);
        windowMenu.addSeparator();
        windowMenu.add(layoutMenu);
        windowMenu.addSeparator();
        windowMenu.add(bringAllToFrontAction);
        windowMenu.addSeparator();
        menuBar.add(windowMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(nodeboxSiteAction);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void buildRecentFileMenu() {
        recentFileMenu.removeAll();
        for (File f : getRecentFiles()) {
            recentFileMenu.add(new OpenRecentAction(f));
        }
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
        String path = null;
        try {
            path = file.getCanonicalPath();
            for (NodeBoxDocument doc : Application.getInstance().getDocuments()) {

                try {
                    if (doc.getDocumentFile() == null) continue;
                    if (doc.getDocumentFile().getCanonicalPath().equals(path)) {
                        doc.toFront();
                        doc.requestFocus();
                        addRecentFile(file);
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
        addRecentFile(file);
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
            addRecentFile(documentFile);
            return saveToFile(documentFile);
        }
        return false;
    }

    public boolean saveToFile(File file) {
        try {
            nodeLibrary.store(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "An error occurred while saving the file.", "MainController", JOptionPane.ERROR_MESSAGE);
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


    private void close() {
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

    public void nodeDirty(Node node) {
        if (node != activeNetwork) return;
        markChanged();
        updateActiveNetwork();
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

    public class NewAction extends AbstractAction {
        public NewAction() {
            putValue(NAME, "New");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_N));
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().createNewDocument();
        }
    }

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

    public class OpenRecentAction extends AbstractAction {
        private File file;


        public OpenRecentAction(File file) {
            this.file = file;
            putValue(NAME, file.getName());
        }

        public void actionPerformed(ActionEvent e) {
            open(file);
        }
    }


    public class CloseAction extends AbstractAction {
        public CloseAction() {
            putValue(NAME, "Close");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_W));
        }

        public void actionPerformed(ActionEvent e) {
            close();
        }
    }

    public class SaveAction extends AbstractAction {
        public SaveAction() {
            putValue(NAME, "Save");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_S));
        }

        public void actionPerformed(ActionEvent e) {
            save();
        }
    }

    public class SaveAsAction extends AbstractAction {
        public SaveAsAction() {
            putValue(NAME, "Save As...");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            saveAs();
        }
    }

    public class RevertAction extends AbstractAction {
        public RevertAction() {
            putValue(NAME, "Revert to Saved");
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    public class ExportAction extends AbstractAction {
        public ExportAction() {
            putValue(NAME, "Export...");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_E));
        }

        public void actionPerformed(ActionEvent e) {
            export();
        }
    }

    public class QuitAction extends AbstractAction {
        public QuitAction() {
            putValue(NAME, "Quit");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_Q));
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().quit();
        }
    }

    public class UndoAction extends AbstractAction {
        public UndoAction() {
            putValue(NAME, "Undo");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_Z));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
            } catch (CannotUndoException ex) {
                logger.log(Level.WARNING, "Unable to undo.", ex);
            }
            update();
            redoAction.update();
        }

        public void update() {
            if (undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    public class RedoAction extends AbstractAction {
        public RedoAction() {
            putValue(NAME, "Redo");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                undo.redo();
            } catch (CannotRedoException ex) {
                logger.log(Level.WARNING, "Unable to redo.", ex);
            }
            update();
            undoAction.update();
        }

        public void update() {
            if (undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    public class CutAction extends AbstractAction {
        public CutAction() {
            putValue(NAME, "Cut");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_X));
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    public class CopyAction extends AbstractAction {
        public CopyAction() {
            putValue(NAME, "Copy");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_C));
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    public class PasteAction extends AbstractAction {
        public PasteAction() {
            putValue(NAME, "Paste");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_V));
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    public class DeleteAction extends AbstractAction {
        public DeleteAction() {
            putValue(NAME, "Delete");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            // TODO: Find network view, delete selected.
        }
    }

    public class ReloadAction extends AbstractAction {
        public ReloadAction() {
            putValue(NAME, "Reload");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_R));
        }

        public void actionPerformed(ActionEvent e) {
            reloadActiveNode();
        }
    }

//    public class NewLibraryAction extends AbstractAction {
//        public NewLibraryAction() {
//            putValue(NAME, "New Library...");
//        }
//
//        public void actionPerformed(ActionEvent e) {
//            String libraryName = JOptionPane.showInputDialog(NodeBoxDocument.this, "Enter the name for the new library", "Create New Library", JOptionPane.QUESTION_MESSAGE);
//            if (libraryName == null || libraryName.trim().length() == 0) return;
//            createNewLibrary(libraryName);
//        }
//    }

    public class MinimizeAction extends AbstractAction {
        public MinimizeAction() {
            putValue(NAME, "Minimize");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.META_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            setState(Frame.ICONIFIED);
        }
    }

    public class ZoomAction extends AbstractAction {
        public ZoomAction() {
            putValue(NAME, "Zoom");
        }

        public void actionPerformed(ActionEvent e) {
            // TODO: Implement
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public class BringAllToFrontAction extends AbstractAction {
        public BringAllToFrontAction() {
            putValue(NAME, "Bring All to Front");
        }

        public void actionPerformed(ActionEvent e) {
            // TODO: Implement
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public class NodeboxSiteAction extends AbstractAction {
        public NodeboxSiteAction() {
            putValue(NAME, "NodeBox Site");
        }

        public void actionPerformed(ActionEvent e) {
            // TODO: Implement
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
