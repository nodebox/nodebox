package net.nodebox.client;

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

public class Document extends JFrame implements NetworkDataListener {

    private final static String WINDOW_MODIFIED = "windowModified";

    private static String lastFilePath;
    private static String lastExportPath;


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

    public Document() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        nodeManager = new NodeManager();
        NodeType canvasNetworkType = nodeManager.getNodeType("net.nodebox.node.canvas.network");
        NodeType vectorNetworkType = nodeManager.getNodeType("net.nodebox.node.vector.network");
        NodeType imageNetworkType = nodeManager.getNodeType("net.nodebox.node.image.network");
        NodeType ellipseType = nodeManager.getNodeType("net.nodebox.node.vector.ellipse");
        NodeType rectType = nodeManager.getNodeType("net.nodebox.node.vector.rect");
        NodeType transformType = nodeManager.getNodeType("net.nodebox.node.vector.transform");
        rootNetwork = (Network) canvasNetworkType.createNode();
        Network vector1 = (Network) rootNetwork.create(vectorNetworkType);
        vector1.setPosition(10, 10);
        vector1.setRendered();
        Network vector2 = (Network) rootNetwork.create(vectorNetworkType);
        vector2.setPosition(10, 110);
        Network image1 = (Network) rootNetwork.create(imageNetworkType);
        image1.setPosition(10, 210);
        Network image2 = (Network) rootNetwork.create(imageNetworkType);
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

        JPanel rootPanel = new JPanel(new BorderLayout());
        ViewerPane viewPane = new ViewerPane(this);
        ParameterPane parameterPane = new ParameterPane(this);
        NetworkPane networkPane = new NetworkPane(this);
        PaneSplitter parameterNetworkSplit = new PaneSplitter(PaneSplitter.VERTICAL_SPLIT, parameterPane, networkPane);
        PaneSplitter viewRestSplit = new PaneSplitter(PaneSplitter.HORIZONTAL_SPLIT, viewPane, parameterNetworkSplit);
        rootPanel.add(viewRestSplit, BorderLayout.CENTER);
        setContentPane(rootPanel);
        setActiveNetwork(rootNetwork);
        setActiveNode(vector1);
        setSize(1100, 800);
        initMenu();
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(newAction));
        fileMenu.add(new JMenuItem(openAction));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(closeAction));
        fileMenu.add(new JMenuItem(saveAction));
        fileMenu.add(new JMenuItem(saveAsAction));
        fileMenu.add(new JMenuItem(revertAction));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(exportAction));
        if (!PlatformUtils.onMac()) {
            fileMenu.addSeparator();
            fileMenu.add(new JMenuItem(quitAction));
        }
        menuBar.add(fileMenu);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem(undoAction));
        editMenu.add(new JMenuItem(redoAction));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem(cutAction));
        editMenu.add(new JMenuItem(copyAction));
        editMenu.add(new JMenuItem(pasteAction));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem(deleteAction));
        menuBar.add(editMenu);

        setJMenuBar(menuBar);
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
        if (!activeNetwork.isEmpty()) {
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

    public boolean readFromFile(File file) {
        FileInputStream fis = null;
        try {
            // Load the document
            fis = new FileInputStream(file);
            InputSource source = new InputSource(fis);

            // Setup the parser
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser parser = spf.newSAXParser();
            // TODO: Parsing
            /*
            PnaXmlReader reader = new PnaXmlReader();
            parser.setContentHandler(reader);
            // The next lines make sure that the SAX parser doesn't try to validate the document,
            // or tries to load in external DTDs (such as those from W3). Non-parsing means you
            // don't need an internet connection to use the program, and speeds up loading the
            // document massively.
            parser.setFeature("http://xml.org/sax/features/validation", false);
            parser.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            parser.setFeature("http://xml.org/sax/features/external-general-entities", false);
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            // Parse the document
            parser.parse(source);

            // The parsed network is now stored in the reader
            setRootNetwork(reader.getNetwork());
            setDocumentFile(file);
            documentChanged = false;
            */
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
            // TODO: Implement the toXml()
            //fos.write(rootNetwork.toXml().getBytes());
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


    public void markChanged() {
        if (!documentChanged) {
            documentChanged = true;
            updateTitle();
            getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.TRUE);
        }
    }


    public void updateTitle() {
        String postfix = "";
        if (true || !PlatformUtils.onMac()) { // todo: mac only code
            postfix = (documentChanged ? " *" : "") + " - PNA";
        }
        if (documentFile == null) {
            setTitle("Untitled" + postfix);
        } else {
            setTitle(documentFile.getName() + postfix);
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
            putValue(NAME, "Open");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_O));
        }

        public void actionPerformed(ActionEvent e) {
            //PnaDocumentFrame requester = getCurrentDocument();
            File chosenFile = FileUtils.showOpenDialog(Document.this, lastFilePath, "pna", "PNA File");
            if (chosenFile != null) {
                lastFilePath = chosenFile.getParentFile().getAbsolutePath();
                if (rootNetwork == null || rootNetwork.isEmpty()) { // Re-use the requesting object if it's empty.
                    if (readFromFile(chosenFile)) {
                        setDocumentFile(chosenFile);
                        setVisible(true);
                    }
                } else { // Create a new document.
                    Document doc = Application.getInstance().createNewDocument();
                    if (doc.readFromFile(chosenFile)) {
                        doc.setDocumentFile(chosenFile);
                    }
                }
            }
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
            putValue(NAME, "Save As");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            saveAs();
        }
    }

    public class RevertAction extends AbstractAction {
        public RevertAction() {
            putValue(NAME, "Revert");
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


}
