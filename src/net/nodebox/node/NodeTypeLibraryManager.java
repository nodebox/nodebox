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

package net.nodebox.node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NodeTypeLibraryManager {

    private static Logger logger = Logger.getLogger("net.nodebox.node.NodeTypeLibrary");

    private ArrayList<File> searchPaths = new ArrayList<File>();
    private HashMap<String, NodeTypeLibrary> nodeLibraryMap = new HashMap<String, NodeTypeLibrary>();
    private boolean lookedForLibraries = false;

    public NodeTypeLibraryManager() {
    }

    public NodeTypeLibraryManager(String... searchPaths) throws IOException {
        for (String searchPath : searchPaths) {
            addSearchPath(searchPath);
        }
    }

    public NodeTypeLibraryManager(File... searchPaths) throws IOException {
        for (File searchPath : searchPaths) {
            addSearchPath(searchPath);
        }
    }

    public void addSearchPath(String searchPath) throws IOException {
        File f = new File(searchPath);
        addSearchPath(f);
    }

    public void addSearchPath(File searchPath) throws IOException {
        if (!searchPath.isDirectory()) {
            throw new IOException("The given search path \"" + searchPath + "\" is not a directory.");
        }
        this.searchPaths.add(searchPath);
    }

    public List<NodeTypeLibrary> getLibraries() {
        return new ArrayList<NodeTypeLibrary>(nodeLibraryMap.values());
    }

    public NodeTypeLibrary loadLatestVersion(String libraryName) {
        if (!lookedForLibraries)
            lookForLibraries();
        // TODO: Only one version of a library can be stored at the moment.
        return nodeLibraryMap.get(libraryName);
    }

    /**
     * Searches for available libraries in the plugin search path.
     */
    private void lookForLibraries() {
        for (File searchPath : searchPaths) {
            for (File f : searchPath.listFiles()) {
                if (f.isDirectory() && f.canRead()) {
                    File libraryDescriptionFile = new File(f, NodeTypeLibrary.LIBRARY_DESCRIPTION_FILE);
                    if (libraryDescriptionFile.exists()) {
                        try {
                            NodeTypeLibrary library = pathToLibrary(f.getParent(), f.getName());
                            assert (!nodeLibraryMap.containsKey(library.getName()));
                            nodeLibraryMap.put(library.getName(), library);
                        } catch (RuntimeException e) {
                            logger.log(Level.WARNING, "Cannot load library " + f);
                        }
                    }
                }
            }
        }
    }

    public static NodeTypeLibrary pathToLibrary(String searchPath, String path) throws RuntimeException {
        if (path == null || path.trim().length() == 0)
            throw new RuntimeException("Empty paths are not accepted.");
        String[] pathSplit = path.split("-");
        String libraryName;
        String versionString;
        assert (pathSplit.length > 0);
        if (pathSplit.length == 1) {
            // No version information
            libraryName = pathSplit[0];
            versionString = "0.0.0";
        } else if (pathSplit.length == 2) {
            libraryName = pathSplit[0];
            versionString = pathSplit[1];
        } else {
            throw new RuntimeException("Only one dash excepted (e.g. \"vector-1.2.3\"): " + path);
        }
        Version v = new Version(versionString);
        try {
            return new NodeTypeLibrary(libraryName, v.getMajor(), v.getMinor(), v.getRevision(), new File(searchPath, path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
