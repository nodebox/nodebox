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
import nodebox.node.NodeRepository;
import nodebox.ui.ExceptionDialog;
import nodebox.ui.LastResortHandler;
import nodebox.ui.Platform;
import nodebox.ui.ProgressDialog;
import nodebox.versioncheck.Host;
import nodebox.versioncheck.Updater;
import nodebox.versioncheck.Version;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class Application implements Host {

    public static final String PREFERENCE_ENABLE_DEVICE_SUPPORT = "NBEnableDeviceSupport";
    public static boolean ENABLE_DEVICE_SUPPORT = false;

    private static Application instance;

    private JFrame hiddenFrame;
    private ExamplesBrowser examplesBrowser;

    public static Application getInstance() {
        return instance;
    }

    private AtomicBoolean startingUp = new AtomicBoolean(true);
    private SwingWorker<Throwable, String> startupWorker;
    private Updater updater;
    private List<NodeBoxDocument> documents = new ArrayList<NodeBoxDocument>();
    private NodeBoxDocument currentDocument;
    private NodeRepository systemRepository;
    private ProgressDialog startupDialog;
    private Version version;
    private List<File> filesToLoad = Collections.synchronizedList(new ArrayList<File>());
    private Console console = null;

    public static final String NAME = "NodeBox";
    private static Logger logger = Logger.getLogger("nodebox.client.Application");

    private Application() {
        instance = this;

        initLastResortHandler();
        initLookAndFeel();

        System.setProperty("line.separator", "\n");
    }

    //// Application Load ////

    /**
     * Starts a SwingWorker that loads the application in the background.
     * <p>
     * Called in the event dispatch thread using invokeLater.
     */
    private void run() {
        showProgressDialog();
        startupWorker = new SwingWorker<Throwable, String>() {
            @Override
            protected Throwable doInBackground() throws Exception {
                try {
                    publish("Starting NodeBox");
                    initApplication();
                    checkForUpdates();
                } catch (RuntimeException ex) {
                    return ex;
                }
                return null;
            }

            @Override
            protected void process(List<String> strings) {
                final String firstString = strings.get(0);
                startupDialog.setMessage(firstString);
            }

            @Override
            protected void done() {
                startingUp.set(false);
                startupDialog.setVisible(false);

                // See if application startup has generated an exception.
                Throwable t;
                try {
                    t = get();
                } catch (Exception e) {
                    t = e;
                }
                if (t != null) {
                    ExceptionDialog ed = new ExceptionDialog(null, t);
                    ed.setVisible(true);
                    System.exit(-1);
                }

                if (documents.isEmpty() && filesToLoad.isEmpty()) {
                    instance.createNewDocument();
                } else {
                    for (File f : filesToLoad) {
                        openDocument(f);
                    }
                }
            }
        };
        startupWorker.execute();
    }

    private void initApplication() {
        installDefaultExceptionHandler();
        setNodeBoxVersion();
        createNodeBoxDataDirectories();
        applyPreferences();
        registerForMacOSXEvents();
        initPython();
    }

    private void installDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, final Throwable e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ExceptionDialog d = new ExceptionDialog(null, e);
                        d.setVisible(true);
                    }
                });
            }
        });
    }

    private void checkForUpdates() {
        updater = new Updater(Application.this);
        updater.checkForUpdatesInBackground();
    }

    /**
     * Sets a handler for uncaught exceptions that pops up a message dialog with the exception.
     * <p>
     * Called from the constructor, in the main thread.
     */
    private void initLastResortHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(new LastResortHandler());
    }

    /**
     * Initializes Swing's look and feel to the system native look and feel.
     * On Mac, uses the system menu bar.
     */
    private void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        UIManager.put("Table.alternateRowColor", new Color(243, 246, 250));
    }

    /**
     * Shows the progress dialog.
     * <p>
     * Called from the run() method (which is called in invokeLater).
     */
    private void showProgressDialog() {
        startupDialog = new ProgressDialog(null, "Starting " + NAME);
        startupDialog.setVisible(true);
    }

    /**
     * Retrieves the NodeBox version number from the version.properties file and sets it in the app.
     *
     * @throws RuntimeException if we're not able to retrieve the version number. This is fatal.
     */
    private void setNodeBoxVersion() throws RuntimeException {
        Properties properties = new Properties();
        try {
            InputStream in = Application.class.getResourceAsStream("/version.properties");
            properties.load(in);
            in.close();
            version = new Version(properties.getProperty("nodebox.version"));
        } catch (IOException e) {
            throw new RuntimeException("Could not read NodeBox version file. Please re-install NodeBox.", e);
        }
    }

    /**
     * Creates the necessary directories used for storing user scripts and Python libraries.
     *
     * @throws RuntimeException if we can't create the user directories. This is fatal.
     */
    private void createNodeBoxDataDirectories() throws RuntimeException {
        Platform.getUserDataDirectory().mkdir();
        Platform.getUserScriptsDirectory().mkdir();
        Platform.getUserPythonDirectory().mkdir();
    }

    /**
     * Load the preferences and make them available to the Application object.
     */
    private void applyPreferences() {
        Preferences preferences = Preferences.userNodeForPackage(Application.class);
        ENABLE_DEVICE_SUPPORT = Boolean.valueOf(preferences.get(Application.PREFERENCE_ENABLE_DEVICE_SUPPORT, "false"));
    }

    /**
     * Register for special events available on the Mac, such as showing the about screen,
     * showing the preferences or double-clicking a file.
     *
     * @throws RuntimeException if the adapter methods could not be loaded.
     */
    private void registerForMacOSXEvents() throws RuntimeException {
        if (!Platform.onMac()) return;
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
        // On the Mac, if all windows are closed the menu bar will be empty.
        // To solve this, we create an off-screen window with the same menu bar as visible windows.
        hiddenFrame = new JFrame();
        hiddenFrame.setJMenuBar(new NodeBoxMenuBar());
        hiddenFrame.setUndecorated(true);
        hiddenFrame.setSize(0, 0);
        hiddenFrame.setLocation(-100, -100);
        hiddenFrame.pack();
        hiddenFrame.setVisible(true);
    }

    private void initPython() {
        // Actually initializing Python happens in the library.
        lookForLibraries();
    }

    private void lookForLibraries() {

        List<NodeLibrary> libraries = new ArrayList<NodeLibrary>();
        libraries.add(NodeLibrary.loadSystemLibrary("math"));
        libraries.add(NodeLibrary.loadSystemLibrary("string"));
        libraries.add(NodeLibrary.loadSystemLibrary("color"));
        libraries.add(NodeLibrary.loadSystemLibrary("list"));
        libraries.add(NodeLibrary.loadSystemLibrary("data"));
        libraries.add(NodeLibrary.loadSystemLibrary("corevector"));
        libraries.add(NodeLibrary.loadSystemLibrary("network"));
        if (Application.ENABLE_DEVICE_SUPPORT) {
            libraries.add(NodeLibrary.loadSystemLibrary("device"));
        }
        systemRepository = NodeRepository.of(libraries.toArray(new NodeLibrary[]{}));
    }

    //// Application events ////

    public boolean quit() {
        // Because documents will disappear from the list once they are closed,
        // make a copy of the list.
        java.util.List<NodeBoxDocument> documents = new ArrayList<NodeBoxDocument>(getDocuments());
        for (NodeBoxDocument d : documents) {
            if (!d.close())
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
        PreferencePanel preferencePanel = new PreferencePanel(this, getCurrentDocument());
        preferencePanel.setLocationRelativeTo(getCurrentDocument());
        preferencePanel.setVisible(true);
    }

    public void showConsole() {
        java.awt.Dimension d = new java.awt.Dimension(400, 400);

        if (console == null) {
            console = new Console();
            console.setPreferredSize(d);
            console.setSize(d);
            console.setLocationRelativeTo(getCurrentDocument());
            console.setVisible(true);
        }

        console.setLocationRelativeTo(getCurrentDocument());
        console.setVisible(true);

        for (NodeBoxDocument document : documents) {
            //document.onConsoleVisibleEvent(true);
        }
    }

    public void hideConsole() {
        console.setVisible(false);
        onHideConsole();
    }

    public void onHideConsole() {
        for (NodeBoxDocument document : documents) {
            //document.onConsoleVisibleEvent(false);
        }
    }

    public boolean isConsoleOpened() {
        if (console == null) return false;
        return console.isVisible();
    }

    public void readFromFile(String path) {
        // This method looks unused, but is actually called using reflection by the OS X adapter.
        // If the application is still starting up, don't open the document immediately but place it in a file loading queue.
        if (startingUp.get()) {
            filesToLoad.add(new File(path));
        } else {
            openDocument(new File(path));
        }
    }

    //// Document management ////

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
        NodeBoxDocument doc = new NodeBoxDocument();
        addDocument(doc);
        return doc;
    }

    public NodeBoxDocument openExample(File file) {
        NodeBoxDocument doc = openDocument(file);
        if (doc != null) {
            doc.setNeedsResave(true);
        }
        return doc;
    }

    public NodeBoxDocument openDocument(File file) {
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
                        return doc;
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "The document " + doc.getDocumentFile() + " refers to path with errors", e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "The document " + file + " refers to path with errors", e);
        }

        try {
            NodeBoxDocument doc = NodeBoxDocument.load(file);
            addDocument(doc);
            NodeBoxMenuBar.addRecentFile(file);
            return doc;
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Error while loading " + file, e);
            ExceptionDialog d = new ExceptionDialog(null, e);
            d.setVisible(true);
            return null;
        }
    }

    private void addDocument(NodeBoxDocument doc) {
        doc.setVisible(true);
        doc.requestFocus();
        doc.focusNetworkView();
        doc.setActiveNetwork("/");
        documents.add(doc);
        currentDocument = doc;
    }

    public NodeBoxDocument getCurrentDocument() {
        return currentDocument;
    }

    void setCurrentDocument(NodeBoxDocument document) {
        currentDocument = document;
    }

    public NodeRepository getSystemRepository() {
        return systemRepository;
    }

    public void takeScreenshotOfDocument(String documentFileName) {
        File documentFile = new File(documentFileName);
        File documentDirectory = documentFile.getParentFile();
        String imageName = FileUtils.getBaseName(documentFile.getName()) + ".png";
        File screenshotFile = new File(documentDirectory, imageName);
        NodeBoxDocument doc = NodeBoxDocument.load(documentFile);
        addDocument(doc);
        doc.setVisible(true);
        doc.takeScreenshot(screenshotFile);
        doc.close();
    }

    public void openExamplesBrowser() {
        if (examplesBrowser == null) {
            examplesBrowser = new ExamplesBrowser();
        }
        examplesBrowser.setVisible(true);
        examplesBrowser.toFront();
    }

    //// Host implementation ////

    public String getName() {
        return "NodeBox";
    }

    public URL getIconFile() {
        return Application.class.getResource("/application-logo.png");
    }

    public Version getVersion() {
        return version;
    }

    public String getAppcastURL() {
        StringBuilder b = new StringBuilder("https://secure.nodebox.net/app/nodebox/appcast.xml");
        b.append("?v=");
        b.append(getVersion().toString());
        b.append("&p=");
        b.append(Platform.current_platform);
        return b.toString();
    }

    public Updater getUpdater() {
        return updater;
    }

    public static void main(String[] args) {
        final Application app = new Application();
        // Ignore OS X's weird launch parameter.
        if (args.length == 1 && !args[0].startsWith("-psn")) {
            app.filesToLoad.add(new File(args[0]));
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                app.run();
            }
        });
    }
}
