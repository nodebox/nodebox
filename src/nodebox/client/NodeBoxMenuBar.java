package nodebox.client;

import javax.swing.*;
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

    public NodeBoxMenuBar() {
        this(null);
    }

    public NodeBoxMenuBar(NodeBoxDocument document) {
        this.document = document;
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new NewAction());
        fileMenu.add(new OpenAction());
        JMenu recentFileMenu = new JMenu("Open Recent");
        recentFileMenus.add(recentFileMenu);
        buildRecentFileMenu();
        fileMenu.add(recentFileMenu);
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction());
        fileMenu.add(new SaveAction());
        fileMenu.add(new SaveAsAction());
        fileMenu.add(new RevertAction());
        fileMenu.addSeparator();
        fileMenu.add(new ExportAction());
        if (!PlatformUtils.onMac()) {
            fileMenu.addSeparator();
            fileMenu.add(new ExitAction());
        }
        add(fileMenu);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new UndoAction());
        editMenu.add(new RedoAction());
        editMenu.addSeparator();
        editMenu.add(new CutAction());
        editMenu.add(new CopyAction());
        editMenu.add(new PasteAction());
        editMenu.addSeparator();
        editMenu.add(new DeleteAction());
        add(editMenu);

        // Node menu
        JMenu pythonMenu = new JMenu("Node");
        pythonMenu.add(new ReloadAction());
        //pythonMenu.add(newLibraryAction);
        add(pythonMenu);

        // Window menu
        JMenu windowMenu = new JMenu("Window");
        JMenu layoutMenu = new JMenu("Layout");
        //layoutMenu.add(new )
        windowMenu.add(new MinimizeAction());
        windowMenu.add(new ZoomAction());
        windowMenu.addSeparator();
        windowMenu.add(layoutMenu);
        windowMenu.addSeparator();
        windowMenu.add(new BringAllToFrontAction());
        windowMenu.addSeparator();
        add(windowMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new GettingStartedAction());
        helpMenu.add(new HelpAndSupportAction());
        helpMenu.addSeparator();
        if (!PlatformUtils.onMac()) {
            helpMenu.add(new AboutAction());
        }
        helpMenu.add(new CheckForUpdatesAction());
        helpMenu.add(new NodeboxSiteAction());
        add(helpMenu);
    }

    public NodeBoxDocument getDocument() {
        return document;
    }

    public boolean isEnabled() {
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

    //// Actions ////

    public abstract class AbstractDocumentAction extends AbstractAction {
        @Override
        public boolean isEnabled() {
            return NodeBoxMenuBar.this.isEnabled();
        }
    }


    public static class NewAction extends AbstractAction {
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


    public class CloseAction extends AbstractDocumentAction {
        public CloseAction() {
            putValue(NAME, "Close");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_W));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().close();
        }
    }

    public class SaveAction extends AbstractDocumentAction {
        public SaveAction() {
            putValue(NAME, "Save");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_S));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().save();
        }
    }

    public class SaveAsAction extends AbstractDocumentAction {
        public SaveAsAction() {
            putValue(NAME, "Save As...");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK));
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

    public class ExportAction extends AbstractDocumentAction {
        public ExportAction() {
            putValue(NAME, "Export...");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_E));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().export();
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
        public UndoAction() {
            putValue(NAME, "Undo");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_Z));
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && getDocument() != null && getDocument().getUndoManager().canUndo();
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().undo();
        }
    }

    public class RedoAction extends AbstractDocumentAction {
        public RedoAction() {
            putValue(NAME, "Redo");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK));
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() && getDocument() != null && getDocument().getUndoManager().canRedo();
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().redo();
        }
    }

    public class CutAction extends AbstractDocumentAction {
        public CutAction() {
            putValue(NAME, "Cut");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_X));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().cut();
        }
    }

    public class CopyAction extends AbstractDocumentAction {
        public CopyAction() {
            putValue(NAME, "Copy");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_C));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().copy();
        }
    }

    public class PasteAction extends AbstractDocumentAction {
        public PasteAction() {
            putValue(NAME, "Paste");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_V));
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
            getDocument().deleteSelected();
        }
    }

    public class ReloadAction extends AbstractDocumentAction {
        public ReloadAction() {
            putValue(NAME, "Reload");
            putValue(ACCELERATOR_KEY, PlatformUtils.getKeyStroke(KeyEvent.VK_R));
        }

        public void actionPerformed(ActionEvent e) {
            getDocument().reloadActiveNode();
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
            PlatformUtils.openURL("http://beta.nodebox.net/");
        }
    }


    public static class GettingStartedAction extends AbstractAction {
        public GettingStartedAction() {
            super("Getting Started");
        }

        public void actionPerformed(ActionEvent e) {
            PlatformUtils.openURL("http://beta.nodebox.net/documentation/getting-started/");
        }
    }

    public static class HelpAndSupportAction extends AbstractAction {
        public HelpAndSupportAction() {
            super("Help and Support");
        }

        public void actionPerformed(ActionEvent e) {
            PlatformUtils.openURL("http://beta.nodebox.net/documentation/");
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
