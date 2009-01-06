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

import org.python.core.PySystemState;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Application {

    private static Application instance;

    public static Application getInstance() {
        return instance;
    }

    private List<NodeBoxDocument> documents = new ArrayList<NodeBoxDocument>();
    private NodeBoxDocument currentDocument;

    public static final String NAME = "NodeBox";

    private Application() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        // Initialize Jython
        Properties jythonProperties = new Properties();
        String jythonCacheDir = PlatformUtils.getUserDataDirectory() + PlatformUtils.SEP + "jythoncache";
        jythonProperties.put("python.cachedir", jythonCacheDir);
        PySystemState.initialize(System.getProperties(), jythonProperties, new String[]{""});
        createNewDocument();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                instance = new Application();
            }
        });
    }
}
