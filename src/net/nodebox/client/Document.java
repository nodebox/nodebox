package net.nodebox.client;

import net.nodebox.graphics.Text;
import net.nodebox.node.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Document extends JFrame implements NetworkDataListener {

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

    public MinimizeAction minimizeAction = new MinimizeAction();
    public ZoomAction zoomAction = new ZoomAction();
    public BringAllToFrontAction bringAllToFrontAction = new BringAllToFrontAction();

    public NodeboxSiteAction nodeboxSiteAction = new NodeboxSiteAction();

    private JMenu recentFileMenu;

    private NodeManager nodeManager;
    private Network rootNetwork;
    private Network activeNetwork;
    private Node activeNode;
    private File documentFile;
    private boolean documentChanged;
    private static Logger logger = Logger.getLogger("net.nodebox.client");
    private EventListenerList documentFocusListeners = new EventListenerList();
    private UndoManager undo = new UndoManager();

    private class DocumentObservable extends Observable {
        public void setChanged() {
            super.setChanged();
        }
    }

    private class AllControlsType extends NodeType {
        private AllControlsType(NodeManager manager) {
            super(manager, "test.allcontrols", ParameterType.Type.GROB_CANVAS);
            addParameterType("angle", ParameterType.Type.ANGLE);
            addParameterType("color", ParameterType.Type.COLOR);
            addParameterType("file", ParameterType.Type.FILE);
            addParameterType("float", ParameterType.Type.FLOAT);
            addParameterType("font", ParameterType.Type.FONT);
            addParameterType("gradient", ParameterType.Type.GRADIENT);
            addParameterType("image", ParameterType.Type.IMAGE);
            addParameterType("int", ParameterType.Type.INT);
            addParameterType("menu", ParameterType.Type.MENU);
            addParameterType("seed", ParameterType.Type.SEED);
            addParameterType("string", ParameterType.Type.STRING);
            addParameterType("text", ParameterType.Type.TEXT);
            addParameterType("toggle", ParameterType.Type.TOGGLE);
            addParameterType("noderef", ParameterType.Type.NODEREF);
            ParameterType ptMenu = getParameterType("menu");
            ptMenu.addMenuItem("red", "Red");
            ptMenu.addMenuItem("green", "Green");
            ptMenu.addMenuItem("blue", "Blue");
            ptMenu.setDefaultValue("blue");
        }

        private void addText(net.nodebox.graphics.Canvas c, Node node, String parameterName, double y) {
            c.add(new Text(parameterName + ": " + node.asString(parameterName), 10, 24 + y * 24));
        }

        public boolean process(Node node, ProcessingContext ctx) {
            net.nodebox.graphics.Canvas c = new net.nodebox.graphics.Canvas();
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

    public Document() {
        nodeManager = new NodeManager();
        JPanel rootPanel = new JPanel(new BorderLayout());
        ViewerPane viewPane = new ViewerPane(this);
        ParameterPane parameterPane = new ParameterPane(this);
        NetworkPane networkPane = new NetworkPane(this);
        PaneSplitter parameterNetworkSplit = new PaneSplitter(PaneSplitter.VERTICAL_SPLIT, parameterPane, networkPane);
        PaneSplitter viewRestSplit = new PaneSplitter(PaneSplitter.HORIZONTAL_SPLIT, viewPane, parameterNetworkSplit);
        rootPanel.add(viewRestSplit, BorderLayout.CENTER);
        setContentPane(rootPanel);
        setSize(1100, 800);
        initMenu();
        registerForMacOSXEvents();
        SwingUtils.centerOnScreen(this);
        setRootNetwork(createEmptyNetwork());
    }

    private void registerForMacOSXEvents() {
        if (PlatformUtils.onMac()) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[]) null));
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAbout", (Class[]) null));
                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("showPreferences", (Class[]) null));
                OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("readFromFile", String.class));
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
    }


    private Network createEmptyNetwork() {
        NodeType canvasNetworkType = nodeManager.getNodeType("net.nodebox.node.canvas.network");
        return (Network) canvasNetworkType.createNode();
    }

    private Network createTestNetwork() {
        NodeType canvasNetworkType = nodeManager.getNodeType("net.nodebox.node.canvas.network");
        NodeType vectorNetworkType = nodeManager.getNodeType("net.nodebox.node.vector.network");
        NodeType imageNetworkType = nodeManager.getNodeType("net.nodebox.node.image.network");
        NodeType ellipseType = nodeManager.getNodeType("net.nodebox.node.vector.ellipse");
        NodeType rectType = nodeManager.getNodeType("net.nodebox.node.vector.rect");
        NodeType transformType = nodeManager.getNodeType("net.nodebox.node.vector.transform");
        NodeType allControlsType = new AllControlsType(nodeManager);
        Network network = (Network) canvasNetworkType.createNode();
        Node allControls = network.create(allControlsType);
        allControls.setPosition(200, 10);
        allControls.setRendered();
        Network vector1 = (Network) network.create(vectorNetworkType);
        vector1.setPosition(10, 10);
        //vector1.setRendered();
        Network vector2 = (Network) network.create(vectorNetworkType);
        vector2.setPosition(10, 110);
        Network image1 = (Network) network.create(imageNetworkType);
        image1.setPosition(10, 210);
        Network image2 = (Network) network.create(imageNetworkType);
        image2.setPosition(10, 310);
        Node ellipse1 = vector1.create(ellipseType);
        ellipse1.setRendered();
        ellipse1.setPosition(100, 30);
        Node ellipse2 = vector1.create(ellipseType);
        ellipse2.setPosition(100, 130);
        Node transform1 = vector1.create(transformType);
        transform1.setPosition(300, 230);
        Node rect1 = vector2.create(rectType);
        rect1.setPosition(40, 40);
        rect1.setRendered();
        Node transform2 = vector2.create(transformType);
        transform2.setPosition(40, 80);
        transform2.setRendered();
        transform2.getParameter("shape").connect(rect1);
        return network;
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
        for (Document doc : Application.getInstance().getDocuments()) {
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
            ((DocumentFocusListener) l).activeNetworkChanged(activeNetwork);
        }
    }

    public void fireActiveNodeChanged() {
        for (EventListener l : documentFocusListeners.getListeners(DocumentFocusListener.class)) {
            ((DocumentFocusListener) l).activeNodeChanged(activeNode);
        }
    }

    public Network getRootNetwork() {
        return rootNetwork;
    }

    public void setRootNetwork(Network rootNetwork) {
        this.rootNetwork = rootNetwork;
        setActiveNetwork(rootNetwork);
    }

    public Network getActiveNetwork() {
        return activeNetwork;
    }

    public void setActiveNetwork(Network activeNetwork) {
        Network oldNetwork = this.activeNetwork;
        if (oldNetwork != null)
            oldNetwork.removeNetworkDataListener(this);
        this.activeNetwork = activeNetwork;
        if (activeNetwork != null)
            activeNetwork.addNetworkDataListener(this);
        fireActiveNetworkChanged();
        if (activeNetwork != null && !activeNetwork.isEmpty()) {
            // Get the first node.
            Iterator<Node> it = activeNetwork.getNodes().iterator();
            Node n = it.next();
            setActiveNode(n);
        } else {
            setActiveNode(null);
        }
        activeNetwork.update();
    }

    public Node getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(Node activeNode) {
        this.activeNode = activeNode;
        fireActiveNodeChanged();
    }

    public NodeManager getNodeManager() {
        return nodeManager;
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
        Document doc = Application.getInstance().createNewDocument();
        if (doc.readFromFile(file)) {
            doc.setDocumentFile(file);
        }
        addRecentFile(file);
    }

    public boolean readFromFile(String path) {
        return readFromFile(new File(path));
    }

    public boolean readFromFile(File file) {
        FileInputStream fis;
        try {
            // Load the document
            fis = new FileInputStream(file);
            InputSource source = new InputSource(fis);

            // Setup the parser
            SAXParserFactory spf = SAXParserFactory.newInstance();
            // The next lines make sure that the SAX parser doesn't try to validate the document,
            // or tries to load in external DTDs (such as those from W3). Non-parsing means you
            // don't need an internet connection to use the program, and speeds up loading the
            // document massively.
            spf.setFeature("http://xml.org/sax/features/validation", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            SAXParser parser = spf.newSAXParser();
            XmlHandler handler = new XmlHandler(nodeManager);
            parser.parse(source, handler);

            // The parsed network is now stored in the reader
            setRootNetwork(handler.getNetwork());
            setDocumentFile(file);
            documentChanged = false;
            return true;
        } catch (ParserConfigurationException e) {
            logger.log(Level.SEVERE, "Error during configuration", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while reading " + file, e);
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Error while parsing" + file, e);
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

    public void showAbout() {
        // TODO: Implement
        Toolkit.getDefaultToolkit().beep();
    }

    public void showPreferences() {
        // TODO: Implement
        Toolkit.getDefaultToolkit().beep();
    }

    public boolean save() {
        if (documentFile == null) {
            return saveAs();
        } else {
            return saveToFile(documentFile);
        }
    }

    public boolean saveAs() {
        File chosenFile = FileUtils.showSaveDialog(this, lastFilePath, "pna", "PNA File");
        if (chosenFile != null) {
            lastFilePath = chosenFile.getParentFile().getAbsolutePath();
            setDocumentFile(chosenFile);
            return saveToFile(documentFile);
        }
        return false;
    }

    public boolean saveToFile(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(rootNetwork.toXml().getBytes("UTF-8"));
            fos.close();
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
        try {
            //return PdfWriter.writeGrob(file, (Grob) network.getOutput());
            return true;
        } catch (ClassCastException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void quit() {
        boolean allClosed = true;
        // Because documents will disappear from the list once they are closed,
        // make a copy of the list.
        java.util.List<Document> documents = new ArrayList<Document>(Application.getInstance().getDocuments());
        for (Document d : documents) {
            allClosed = allClosed && d.shouldClose();
        }
        if (allClosed) {
            System.exit(0);
        }
    }

    public void markChanged() {
        if (!documentChanged) {
            documentChanged = true;
            updateTitle();
            getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.TRUE);
        }
    }


    public void updateTitle() {
        String postfix = "";
        if (!PlatformUtils.onMac()) { // todo: mac only code
            postfix = (documentChanged ? " *" : "") + " - PNA";
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

    //// Network events ////

    public void networkDirty(Network network) {
        if (network != activeNetwork) return;
        activeNetwork.update();
    }

    public void networkUpdated(Network network) {
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
            File chosenFile = FileUtils.showOpenDialog(Document.this, lastFilePath, "ndbx", "NodeBox Document");
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
            if (shouldClose()) {
                if (Application.getInstance().getDocumentCount() > 1) {
                    Application.getInstance().removeDocument(Document.this);
                    dispose();
                } else {
                    // TODO: On mac, the application doesn't quit after the last document is closed.
                    //if (PlatformUtils.onMac()) {
                    //    // On mac, the application doesn't quit after the last document is closed.
                    //    setVisible(false);
                    //} else {
                    System.exit(0);
                    //}
                }
            }
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
            putValue(NAME, "Export");
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
            quit();
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
