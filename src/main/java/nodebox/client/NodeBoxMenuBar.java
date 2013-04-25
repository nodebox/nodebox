package nodebox.client;

import nodebox.ui.Platform;
import nodebox.util.FileUtils;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The main menu bar for the NodeBox application.
 */
public class NodeBoxMenuBar extends JMenuBar {

    private NodeBoxDocument document;
    private boolean enabled;
    private static ArrayList<JMenu> recentFileMenus = new ArrayList<JMenu>();
    private static Preferences recentFilesPreferences = Preferences.userRoot().node("/nodebox/recent");
    private static Logger logger = Logger.getLogger("nodebox.client.NodeBoxMenuBar");
    private JMenuItem showConsoleItem;

    private UndoManager undoManager;
    private UndoAction undoAction;
    private RedoAction redoAction;

    public NodeBoxMenuBar() {
        this(null);
    }

    public NodeBoxMenuBar(NodeBoxDocument document) {
        this.document = document;
        if (document != null)
            this.undoManager = document.getUndoManager();
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new NewAction());
        fileMenu.add(new OpenAction());
        JMenu recentFileMenu = new JMenu("Open Recent");
        recentFileMenus.add(recentFileMenu);
        buildRecentFileMenu();
        fileMenu.add(recentFileMenu);
        fileMenu.add(new OpenExamplesAction());
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction());
        fileMenu.add(new SaveAction());
        fileMenu.add(new SaveAsAction());
        fileMenu.add(new RevertAction());
        fileMenu.addSeparator();
        if (Application.ENABLE_DEVICE_SUPPORT)
            fileMenu.add(new DevicesAction());
        fileMenu.add(new CodeLibrariesAction());
        fileMenu.add(new DocumentPropertiesAction());
        fileMenu.addSeparator();
        fileMenu.add(new ExportAction());
        fileMenu.add(new ExportRangeAction());
        fileMenu.add(new ExportMovieAction());
        if (!Platform.onMac()) {
            fileMenu.addSeparator();
            fileMenu.add(new ExitAction());
        }
        add(fileMenu);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(undoAction = new UndoAction());
        editMenu.add(redoAction = new RedoAction());
        editMenu.addSeparator();
        editMenu.add(new CutAction());
        editMenu.add(new CopyAction());
        editMenu.add(new PasteAction());
        editMenu.addSeparator();
        editMenu.add(new DeleteAction());
        if (!Platform.onMac()) {
            editMenu.addSeparator();
            editMenu.add(new PreferencesAction());
        }
        add(editMenu);

        // Node menu
        JMenu nodeMenu = new JMenu("Node");
        nodeMenu.add(new NewNodeAction());
        nodeMenu.add(new ReloadAction());
        nodeMenu.add(new PlayPauseAction());
        nodeMenu.add(new RewindAction());
        nodeMenu.add(new FullScreenAction());
        //nodeMenu.add(newLibraryAction);
        add(nodeMenu);

        // Window menu
        JMenu windowMenu = new JMenu("Window");
        windowMenu.add(new MinimizeAction());
        windowMenu.add(new ZoomAction());
        showConsoleItem = windowMenu.add(new JCheckBoxMenuItem(new ShowConsoleAction()));
        setShowConsoleChecked(Application.getInstance() != null && Application.getInstance().isConsoleOpened());
        windowMenu.addSeparator();
        windowMenu.add(new BringAllToFrontAction());
        // TODO Add all active windows.
        add(windowMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new GettingStartedAction());
        helpMenu.add(new HelpAndSupportAction());
        helpMenu.add(new ReportAnIssueAction());

        helpMenu.addSeparator();
        if (!Platform.onMac()) {
            helpMenu.add(new AboutAction());
        }
        helpMenu.add(new CheckForUpdatesAction());
        helpMenu.add(new NodeboxSiteAction());
        add(helpMenu);
    }

    public void updateUndoRedoState() {
        undoAction.update();
        redoAction.update();
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    public boolean hasDocument() {
        return document != null;
    }

    public static void addRecentFile(File f) {
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
        buildRecentFileMenu();
    }

    public static void writeRecentFiles(ArrayList<File> fileList) {
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

    public static ArrayList<File> getRecentFiles() {
        ArrayList<File> fileList = new ArrayList<File>(10);
        for (int i = 1; i <= 10; i++) {
            File file = new File(recentFilesPreferences.get(String.valueOf(i), ""));
            if (file.exists()) {
                fileList.add(file);
            }
        }
        return fileList;
    }

    private static void buildRecentFileMenu() {
        for (JMenu recentFileMenu : recentFileMenus) {
            recentFileMenu.removeAll();
            for (File f : getRecentFiles()) {
                recentFileMenu.add(new OpenRecentAction(f));
            }
        }
    }

    public void setShowConsoleChecked(boolean checked) {
        showConsoleItem.getModel().setSelected(checked);
    }

    //// Actions ////

    public abstract class AbstractDocumentAction extends AbstractAction {
        @Override
        public boolean isEnabled() {
            return NodeBoxMenuBar.this.hasDocument() && super.isEnabled();
        }
    }


    public static class NewAction extends AbstractAction {
        public NewAction() {
            putValue(NAME, "New");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_N));
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().createNewDocument();
        }
    }

    public class OpenAction extends AbstractAction {
        public OpenAction() {
            putValue(NAME, "Open...");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_O));
        }

        public void actionPerformed(ActionEvent e) {
            File chosenFile = FileUtils.showOpenDialog(getDocument(), NodeBoxDocument.lastFilePath, "ndbx", "NodeBox Document");
            if (chosenFile != null) {
                Application.getInstance().openDocument(chosenFile);
            }
        }
    }

    public static class OpenRecentAction extends AbstractAction {
        private File file;


        public OpenRecentAction(File file) {
            this.file = file;
            putValue(NAME, file.getName());
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().openDocument(file);
        }
    }

    public static class OpenExamplesAction extends AbstractAction {



        public OpenExamplesAction() {
            putValue(NAME, "Open Examples...");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_O, Event.SHIFT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().openExamplesBrowser();
        }
    }


    public class CloseAction extends AbstractDocumentAction {
        public CloseAction() {
            putValue(NAME, "Close");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_W));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().close();
        }
    }

    public class SaveAction extends AbstractDocumentAction {
        public SaveAction() {
            putValue(NAME, "Save");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_S));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().save();
        }
    }

    public class SaveAsAction extends AbstractDocumentAction {
        public SaveAsAction() {
            putValue(NAME, "Save As...");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().saveAs();
        }
    }

    public class RevertAction extends AbstractDocumentAction {
        public RevertAction() {
            putValue(NAME, "Revert to Saved");
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().revert();
        }
    }

    public class DevicesAction extends AbstractDocumentAction {
        public DevicesAction() {
            putValue(NAME, "Devices...");
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().showDevices();
        }
    }

    public class CodeLibrariesAction extends AbstractDocumentAction {
        public CodeLibrariesAction() {
            putValue(NAME, "Code Libraries...");
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().showCodeLibraries();
        }
    }

    public class DocumentPropertiesAction extends AbstractDocumentAction {
        public DocumentPropertiesAction() {
            putValue(NAME, "Document Properties...");
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().showDocumentProperties();
        }
    }

    public class ExportAction extends AbstractDocumentAction {
        public ExportAction() {
            putValue(NAME, "Export...");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_E));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().doExport();
        }
    }

    public class ExportRangeAction extends AbstractDocumentAction {
        public ExportRangeAction() {
            putValue(NAME, "Export Range...");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_E, Event.SHIFT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().exportRange();
        }
    }

    public class ExportMovieAction extends AbstractDocumentAction {
        public ExportMovieAction() {
            putValue(NAME, "Export Movie...");
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().exportMovie();
        }
    }

    public static class ExitAction extends AbstractAction {
        public ExitAction() {
            putValue(NAME, "Exit");
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().quit();
        }
    }

    public class UndoAction extends AbstractDocumentAction {
        private String undoText = UIManager.getString("AbstractUndoableEdit.undoText");

        public UndoAction() {
            putValue(NAME, undoText);
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_Z));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            Component c = getDocument().getFocusOwner();
                getDocument().undo();
            updateUndoRedoState();
        }

        public void update() {
            Component c = getDocument().getFocusOwner();
                if (undoManager != null && undoManager.canUndo()) {
                    setEnabled(true);
                    putValue(Action.NAME, undoManager.getUndoPresentationName());
                } else {
                    setEnabled(false);
                    putValue(Action.NAME, undoText);
                }
        }
    }

    public class RedoAction extends AbstractDocumentAction {
        private String redoText = UIManager.getString("AbstractUndoableEdit.redoText");

        public RedoAction() {
            putValue(NAME, redoText);
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            Component c = getDocument().getFocusOwner();
                getDocument().redo();
            updateUndoRedoState();
        }

        public void update() {
            Component c = getDocument().getFocusOwner();
                if (undoManager != null && undoManager.canRedo()) {
                    setEnabled(true);
                    putValue(Action.NAME, undoManager.getRedoPresentationName());
                } else {
                    setEnabled(false);
                    putValue(Action.NAME, redoText);
                }
        }
    }

    public class CutAction extends AbstractDocumentAction {
        public CutAction() {
            putValue(NAME, "Cut");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_X));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().cut();
        }
    }

    public class CopyAction extends AbstractDocumentAction {
        public CopyAction() {
            putValue(NAME, "Copy");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_C));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().copy();
        }
    }

    public class PasteAction extends AbstractDocumentAction {
        public PasteAction() {
            putValue(NAME, "Paste");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_V));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().paste();
        }
    }

    public class DeleteAction extends AbstractDocumentAction {
        public DeleteAction() {
            putValue(NAME, "Delete");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().deleteSelection();
        }
    }

    public static class PreferencesAction extends AbstractAction {
        public PreferencesAction() {
            putValue(NAME, "Preferences");
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().showPreferences();
        }
    }

    public class NewNodeAction extends AbstractDocumentAction {
        public NewNodeAction() {
            putValue(NAME, "Create New Node...");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_N, Event.SHIFT_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            document.showNodeSelectionDialog();
        }
    }

    public class ReloadAction extends AbstractDocumentAction {
        public ReloadAction() {
            putValue(NAME, "Reload");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_R));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().reload();
        }
    }

    public class PlayPauseAction extends AbstractDocumentAction {
        public PlayPauseAction() {
            putValue(NAME, "Play/Pause");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_P, Event.META_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            getDocument().toggleAnimation();
        }
    }

    public class RewindAction extends  AbstractDocumentAction {
        public RewindAction() {
            putValue(NAME, "Rewind");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_P, Event.META_MASK | Event.SHIFT_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            getDocument().doRewind();
        }
    }

    public class FullScreenAction extends  AbstractDocumentAction {
        public FullScreenAction() {
            putValue(NAME, "Full Screen");
            putValue(ACCELERATOR_KEY, Platform.getKeyStroke(KeyEvent.VK_F, Event.META_MASK | Event.SHIFT_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            getDocument().renderFullScreen();
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

    public class MinimizeAction extends AbstractDocumentAction {
        public MinimizeAction() {
            putValue(NAME, "Minimize");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.META_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().setState(Frame.ICONIFIED);
        }
    }

    public class ZoomAction extends AbstractDocumentAction {
        public ZoomAction() {
            putValue(NAME, "Zoom");
        }

        public void actionPerformed(ActionEvent e) {
            // TODO: Implement
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public class ShowConsoleAction extends AbstractDocumentAction {
        public ShowConsoleAction() {
            putValue(NAME, "Show Console");
        }

        public void actionPerformed(ActionEvent e) {
            Application instance = Application.getInstance();
            if (instance.isConsoleOpened())
                instance.hideConsole();
            else
                instance.showConsole();
        }
    }

    public class BringAllToFrontAction extends AbstractDocumentAction {
        public BringAllToFrontAction() {
            putValue(NAME, "Bring All to Front");
        }

        public void actionPerformed(ActionEvent e) {
            // TODO: Implement
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static class AboutAction extends AbstractAction {
        public AboutAction() {
            super("About");
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().showAbout();
        }
    }


    public static class NodeboxSiteAction extends AbstractAction {
        public NodeboxSiteAction() {
            putValue(NAME, "NodeBox Site");
        }

        public void actionPerformed(ActionEvent e) {
            Platform.openURL("http://nodebox.net/");
        }
    }


    public static class GettingStartedAction extends AbstractAction {
        public GettingStartedAction() {
            super("Getting Started");
        }

        public void actionPerformed(ActionEvent e) {
            Platform.openURL("http://nodebox.net/node/documentation/tutorial/getting-started.html");
        }
    }

    public static class HelpAndSupportAction extends AbstractAction {
        public HelpAndSupportAction() {
            super("Help and Support");
        }

        public void actionPerformed(ActionEvent e) {
            Platform.openURL("http://nodebox.net/node/documentation/");
        }
    }

    public static class ReportAnIssueAction extends AbstractAction {
        public ReportAnIssueAction() {
            super("Report an Issue...");
        }

        public void actionPerformed(ActionEvent e) {
            Platform.openURL("https://github.com/nodebox/nodebox/issues");
        }
    }

    public static class CheckForUpdatesAction extends AbstractAction {
        public CheckForUpdatesAction() {
            super("Check for Updates...");
        }

        public void actionPerformed(ActionEvent e) {
            Application.getInstance().getUpdater().checkForUpdates();
        }
    }

}
