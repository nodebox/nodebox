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

public class NodeLibraryManager {

    private ArrayList<File> searchPaths = new ArrayList<File>();
    private HashMap<String, NodeLibrary> nodeLibraryMap = new HashMap<String, NodeLibrary>();
    private boolean lookedForLibraries = false;

    public NodeLibraryManager(String... searchPaths) throws IOException {
        for (String searchPath : searchPaths) {
            addSearchPath(searchPath);
        }
    }

    public NodeLibraryManager(File... searchPaths) throws IOException {
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

    public List<NodeLibrary> getLibraries() {
        return new ArrayList<NodeLibrary>(nodeLibraryMap.values());
    }

    public NodeLibrary loadLatestVersion(String libraryName) {
        throw new AssertionError("Not implemented yet.");
    }

    /**
     * Searches for available libraries in the plugin search path.
     */
    private void lookForLibraries() {
        
    }

    private NodeLibrary directoryNameToLibrary(String searchPath, String directoryName) {
        //searchPath.split("-")
        throw new AssertionError("Not implemented yet.");
    }


}
