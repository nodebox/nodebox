/*
 * This file is part of NodeBox.
 *
 * Copyright (C) 2008 Frederik De Bleser (frederik@pandora.be)
 *
 * NodeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NodeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NodeBox. If not, see <http://www.gnu.org/licenses/>.
 */
package nodebox.client;

import nodebox.node.NodeLibrary;
import nodebox.node.NodeLibraryManager;
import nodebox.versioncheck.Host;
import nodebox.versioncheck.Updater;
import nodebox.versioncheck.Version;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class Application implements Host {

    public static final String PREFERENCE_ENABLE_PANE_CUSTOMIZATION = "NBEnablePaneCustomization";

    public static boolean FLAG_ENABLE_MOVIE_EXPORT = false;
    public static boolean ENABLE_PANE_CUSTOMIZATION = false;

    private static Application instance;

    private JFrame hiddenFrame;

    public static Application getInstance() {
        return instance;
    }

    private Updater updater;
    private List<NodeBoxDocument> documents = new ArrayList<NodeBoxDocument>();
    private NodeBoxDocument currentDocument;
    private NodeLibraryManager manager;
    private ProgressDialog startupDialog;
    private Version version;
    private NodeLibrary clipboardLibrary;

    public static final String NAME = "NodeBox";
    private static Logger logger = Logger.getLogger("nodebox.client.Application");


    private Application() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        Thread.currentThread().setUncaughtExceptionHandler(new LastResortHandler());
        // System.setProperty("sun.awt.exception.handler", LastResortHandler.class.getName());

        // Create the user's NodeBox library directories. 
        try {
            setNodeBoxVersion();
            createNodeBoxDataDirectories();
            applyPreferences();
            registerForMacOSXEvents();
            updater = new Updater(this);
        } catch (RuntimeException e) {
            ExceptionDialog ed = new ExceptionDialog(null, e);
            ed.setVisible(true);
            System.exit(-1);
        }
    }

    private void setNodeBoxVersion() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("version.properties"));
            version = new Version(properties.getProperty("nodebox.version"));
        } catch (IOException e) {
            throw new RuntimeException("Could not read NodeBox version file. Please re-install NodeBox.", e);
        }
    }

    private void createNodeBoxDataDirectories() throws RuntimeException {
        PlatformUtils.getUserDataDirectory().mkdir();
        PlatformUtils.getUserScriptsDirectory().mkdir();
        PlatformUtils.getUserPythonDirectory().mkdir();
    }

    private void applyPreferences() {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        ENABLE_PANE_CUSTOMIZATION = Boolean.valueOf(preferences.get(Application.PREFERENCE_ENABLE_PANE_CUSTOMIZATION, "false"));
    }

    private void registerForMacOSXEvents() throws RuntimeException {
        if (!PlatformUtils.onMac()) return;
        try {
            // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
            // use as delegates for various com.apple.eawt.ApplicationListener methods
            OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[]) null));
            OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("showAbout", (Class[]) null));
            OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("showPreferences", (Class[]) null));
            OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("readFromFile", String.class));
        } catch (Exception e) {
            throw new RuntimeException("Error while loading the OS X Adapter.", e);
        }
        // Create hidden window.
        hiddenFrame = new JFrame();
        hiddenFrame.setJMenuBar(new NodeBoxMenuBar());
        hiddenFrame.setUndecorated(true);
        hiddenFrame.setSize(0, 0);
        hiddenFrame.pack();
        hiddenFrame.setVisible(true);
    }

    public boolean quit() {
        // Because documents will disappear from the list once they are closed,
        // make a copy of the list.
        java.util.List<NodeBoxDocument> documents = new ArrayList<NodeBoxDocument>(getDocuments());
        for (NodeBoxDocument d : documents) {
            if (!d.shouldClose())
                return false;
        }
        System.exit(0);
        return true;
    }

    public void showAbout() {
        String javaVersion = System.getProperty("java.runtime.version");
        JOptionPane.showMessageDialog(null, NAME + " version " + getVersion() + "\nJava " + javaVersion, NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    public void showPreferences() {
        PreferencesDialog dialog = new PreferencesDialog();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    public boolean readFromFile(String path) {
        // This method looks unused, but is actually called using reflection by the OS X adapter.
        return openDocument(new File(path));
    }

    private void load() {
        manager = new NodeLibraryManager();
        manager.addSearchPath(PlatformUtils.getApplicationScriptsDirectory());
        manager.addSearchPath(PlatformUtils.getUserScriptsDirectory());
        manager.lookForLibraries();
        int tasks = manager.getLibraries().size() + 1;
        startupDialog = new ProgressDialog(null, "Starting " + NAME, tasks);
        startupDialog.setVisible(true);

        // Initialize Jython
        startupDialog.setMessage("Loading Python");
        Thread t = new Thread(new PythonLoader());
        t.start();
    }

    private void pythonLoadedEvent() {
        startupDialog.tick();
        Thread t = new Thread(new LibraryLoader());
        t.start();
    }

    private void librariesLoadedEvent() {
        startupDialog.setVisible(false);
        if (documents.isEmpty())
            instance.createNewDocument();
        updater.checkForUpdatesInBackground();
    }

    private void librariesErrorEvent(String libraryName, Exception exception) {
        startupDialog.setVisible(false);
        ExceptionDialog ed = new ExceptionDialog(null, exception, "Library: " + libraryName);
        ed.setVisible(true);
    }


    public List<NodeBoxDocument> getDocuments() {
        return documents;
    }

    public int getDocumentCount() {
        return documents.size();
    }

    public void removeDocument(NodeBoxDocument document) {
        documents.remove(document);
    }

    public NodeBoxDocument createNewDocument() {
        NodeLibrary newLibrary = new NodeLibrary("untitled");
        NodeBoxDocument doc = new NodeBoxDocument(newLibrary);
        addDocument(doc);
        return doc;
    }

    public boolean openDocument(File file) {
        // Check if the document is already open.
        String path;
        try {
            path = file.getCanonicalPath();
            for (NodeBoxDocument doc : Application.getInstance().getDocuments()) {
                try {
                    if (doc.getDocumentFile() == null) continue;
                    if (doc.getDocumentFile().getCanonicalPath().equals(path)) {
                        // The document is already open. Bring it to the front.
                        doc.toFront();
                        doc.requestFocus();
                        NodeBoxMenuBar.addRecentFile(file);
                        return true;
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "The document " + doc.getDocumentFile() + " refers to path with errors", e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "The document " + file + " refers to path with errors", e);
        }

        try {
            NodeBoxDocument doc = new NodeBoxDocument(file);
            addDocument(doc);
            NodeBoxMenuBar.addRecentFile(file);
            return true;
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Error while loading " + file, e);
            ExceptionDialog d = new ExceptionDialog(null, e);
            d.setVisible(true);
            return false;
        }
    }

    private void addDocument(NodeBoxDocument doc) {
        doc.setVisible(true);
        doc.requestFocus();
        documents.add(doc);
        currentDocument = doc;
    }

    public NodeBoxDocument getCurrentDocument() {
        return currentDocument;
    }

    void setCurrentDocument(NodeBoxDocument document) {
        currentDocument = document;
    }

    public NodeLibrary getNodeClipboard() {
        return clipboardLibrary;
    }

    public void setNodeClipboard(NodeLibrary clipboardLibrary) {
        this.clipboardLibrary = clipboardLibrary;
    }

    public NodeLibraryManager getManager() {
        return manager;
    }


    //// Host implementation ////


    public String getName() {
        return "NodeBox";
    }

    public String getIconFile() {
        return "res/applogo.png";
    }

    public Version getVersion() {
        return version;
    }

    public String getAppcastURL() {
        return "https://secure.nodebox.net/app/nodebox/appcast.xml";
    }

    public Updater getUpdater() {
        return updater;
    }

    //// Loader classes ////

    public class PythonLoader implements Runnable {
        public void run() {
            PythonUtils.initializePython();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Application.getInstance().pythonLoadedEvent();
                }
            });
        }
    }

    public class LibraryLoader implements Runnable {
        public void run() {
            String libraryName = "";
            Exception currentException = null;
            // Load libraries
            for (NodeLibrary library : manager.getLibraries()) {
                SwingUtilities.invokeLater(new MessageSetter("Loading " + library.getName() + " library"));
                try {
                    // TODO: Implement explicit loading of libraries.
                    //library.load();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Could not load library " + library.getName(), e);
                    currentException = e;
                    libraryName = library.getName();
                    break;
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        startupDialog.tick();
                    }
                });
            }
            final Exception finalException = currentException;
            final String finalLibraryName = libraryName;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (finalException != null) {
                        Application.getInstance().librariesErrorEvent(finalLibraryName, finalException);
                    } else {
                        Application.getInstance().librariesLoadedEvent();
                    }
                }
            });
        }
    }

    private class MessageSetter implements Runnable {
        private String message;

        private MessageSetter(String message) {
            this.message = message;
        }

        public void run() {
            startupDialog.setMessage(message);
        }
    }


    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.contains("--enable-movie-export")) {
                FLAG_ENABLE_MOVIE_EXPORT = true;
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                instance = new Application();
                instance.load();
            }
        });
    }
}
