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
package net.nodebox.client;

import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.NodeTypeLibraryManager;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {

    private static Application instance;

    public static Application getInstance() {
        return instance;
    }

    private List<NodeBoxDocument> documents = new ArrayList<NodeBoxDocument>();
    private NodeBoxDocument currentDocument;
    private NodeTypeLibraryManager manager;
    private ProgressDialog startupDialog;

    public static final String NAME = "NodeBox 2";
    private static Logger logger = Logger.getLogger("net.nodebox.client.Application");


    private Application() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        Thread.currentThread().setUncaughtExceptionHandler(new LastResortHandler());
        // System.setProperty("sun.awt.exception.handler", LastResortHandler.class.getName());
    }

    private void load() {
        manager = new NodeTypeLibraryManager();
        manager.addSearchPath(PlatformUtils.getUserNodeTypeLibraryDirectory());
        int tasks = manager.getLibraries().size() + 1;
        startupDialog = new ProgressDialog(null, "Starting NodeBox", tasks);
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
        instance.createNewDocument();
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
        NodeBoxDocument doc = new NodeBoxDocument();
        doc.setVisible(true);
        documents.add(doc);
        currentDocument = doc;
        return doc;
    }

    public NodeTypeLibraryManager getManager() {
        return manager;
    }


    public class PythonLoader implements Runnable {
        public void run() {
            Properties jythonProperties = new Properties();
            String jythonCacheDir = PlatformUtils.getUserDataDirectory() + PlatformUtils.SEP + "jythoncache";
            jythonProperties.put("python.cachedir", jythonCacheDir);
            PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});
            String workingDirectory = System.getProperty("user.dir");
            File pythonLibraries = new File(workingDirectory, "lib" + PlatformUtils.SEP + "python.zip");
            Py.getSystemState().path.add(new PyString(pythonLibraries.getAbsolutePath()));
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
            manager = new NodeTypeLibraryManager();
            manager.addSearchPath(PlatformUtils.getUserNodeTypeLibraryDirectory());
            for (NodeTypeLibrary library : manager.getLibraries()) {
                SwingUtilities.invokeLater(new MessageSetter("Loading " + library.getName() + " library"));
                try {
                    library.load();
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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                instance = new Application();
                instance.load();
            }
        });
    }
}
