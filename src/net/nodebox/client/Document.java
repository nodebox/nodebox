package net.nodebox.client;

import net.nodebox.node.Network;
import net.nodebox.node.Node;
import net.nodebox.node.canvas.CanvasNetwork;
import net.nodebox.node.image.ImageNetwork;
import net.nodebox.node.vector.EllipseNode;
import net.nodebox.node.vector.RectNode;
import net.nodebox.node.vector.VectorNetwork;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
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
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Document extends JFrame {

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

    private DocumentObservable documentObservable = new DocumentObservable();
    private Network rootNetwork;
    private Network activeNetwork;
    private Node activeNode;
    private File documentFile;
    private boolean documentChanged;
    private static Logger logger = Logger.getLogger("net.nodebox.client");

    private class DocumentObservable extends Observable {
        public void setChanged() {
            super.setChanged();
        }
    }

    public Document() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        rootNetwork = new CanvasNetwork("root");
        Network vector1 = (Network) rootNetwork.create(VectorNetwork.class);
        vector1.setPosition(10, 10);
        Network vector2 = (Network) rootNetwork.create(VectorNetwork.class);
        vector2.setPosition(10, 110);
        Network image1 = (Network) rootNetwork.create(ImageNetwork.class);
        image1.setPosition(10, 210);
        Network image2 = (Network) rootNetwork.create(ImageNetwork.class);
        image2.setPosition(10, 310);
        Node ellipse1 = vector1.create(EllipseNode.class);
        ellipse1.setPosition(100, 30);
        Node rect1 = vector2.create(RectNode.class);
        rect1.setPosition(40, 40);

        JPanel rootPanel = new JPanel(new BorderLayout());
        NetworkPane viewPane = new NetworkPane(this);
        ParameterPane parameterPane = new ParameterPane(this);
        NetworkPane networkPane = new NetworkPane(this);
        PaneSplitter parameterNetworkSplit = new PaneSplitter(PaneSplitter.VERTICAL_SPLIT, parameterPane, networkPane);
        PaneSplitter viewRestSplit = new PaneSplitter(PaneSplitter.HORIZONTAL_SPLIT, viewPane, parameterNetworkSplit);
        rootPanel.add(viewRestSplit, BorderLayout.CENTER);
        setContentPane(rootPanel);
        setActiveNetwork(rootNetwork);
        setActiveNode(vector1);
        setSize(800, 600);
    }

    public void addObserver(Observer o) {
        documentObservable.addObserver(o);
    }

    public void removeObserver(Observer o) {
        documentObservable.deleteObserver(o);
    }

    public Network getRootNetwork() {
        return rootNetwork;
    }

    public Network getActiveNetwork() {
        return activeNetwork;
    }

    public void setActiveNetwork(Network activeNetwork) {
        this.activeNetwork = activeNetwork;
        SelectionChangedEvent event = new SelectionChangedEvent(SelectionChangedEvent.NETWORK, activeNetwork);
        documentObservable.setChanged();
        documentObservable.notifyObservers(event);
        if (!activeNetwork.isEmpty()) {
            // Get the first node.
            Iterator<Node> it = activeNetwork.getNodes().iterator();
            Node n = it.next();
            setActiveNode(n);
        } else {
            setActiveNode(null);
        }
    }

    public Node getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(Node activeNode) {
        this.activeNode = activeNode;
        SelectionChangedEvent event = new SelectionChangedEvent(SelectionChangedEvent.NODE, activeNode);
        documentObservable.setChanged();
        documentObservable.notifyObservers(event);
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


}
