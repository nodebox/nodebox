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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NodeLibrary {


    enum LibraryType {
        UNKNOWN, JAVA, PYTHON
    }

    public static class NotFound extends RuntimeException {

        private String name;

        public NotFound(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class LoadError extends RuntimeException {

        private String libraryName;

        public LoadError(String libraryName) {
            this.libraryName = libraryName;
        }

        public LoadError(String libraryName, String message) {
            super(message);
            this.libraryName = libraryName;
        }

        public String getLibraryName() {
            return libraryName;
        }
    }


    private String name;
    private HashMap<String, NodeType> types = new HashMap<String, NodeType>();
    private Version version;
    private File path;
    private LibraryType type = null;
    private boolean loaded = false;

    public NodeLibrary(String name, int majorVersion, int minorVersion, int revisionVersion, File path) {
        this.name = name;
        this.version = new Version(majorVersion, minorVersion, revisionVersion);
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    public String versionAsString() {
        return version.toString();
    }

    public File getPath() {
        return path;
    }

    public LibraryType getType() {
        if (type == null) {
            detectType();
        }
        return type;
    }

    //// Library loading ////

    /**
     * This operation doesn't require fully loading the library. It only does some file system lookups.
     *
     * @return the type of the library.
     */
    private LibraryType detectType() {
        File javaLibrary = new File(getJavaLibraryPath());
        if (javaLibrary.exists()) {
            type = LibraryType.JAVA;
            return type;
        }

        File pythonLibrary = new File(getPythonLibraryPath());
        if (pythonLibrary.exists()) {
            type = LibraryType.PYTHON;
            return type;
        }

        type = LibraryType.UNKNOWN;
        return type;
    }

    /**
     * Load throws RuntimExceptions when an error happened during loading.
     * Loading is made public so you can catch errors at an opportune time,
     * not when loading is triggered at an arbitrary point.
     */
    public void load() {
        if (isLoaded()) return;
        if (getType() == LibraryType.JAVA) {
            loadJavaLibrary();
        } else if (getType() == LibraryType.PYTHON) {
            loadPythonLibrary();
        } else {
            throw new LoadError(name, "Unknown library type " + getType());
        }
    }

    private void loadJavaLibrary() {

    }

    private void loadPythonLibrary() {

    }

    public boolean isLoaded() {
        return loaded;
    }

    //// Node info ////

    NodeType getNodeType(String nodeName) {
        if (!isLoaded()) load();
        if (types.containsKey(nodeName)) {
            return types.get(nodeName);
        } else {
            throw new NotFoundException(this, nodeName, "Node type " + nodeName + " not found in library " + getName());
        }
    }

    List<NodeType> getNodeTypes() {
        if (!isLoaded()) load();
        return new ArrayList<NodeType>(types.values());
    }

    //// Path information ////

    /**
     * A Java library has one file inside of the library folder,
     * called <em>libname</em>.jar (Java archive)
     * The full path would be plugins/libname-1.2.3/libname.jar.
     *
     * @return the full path to the Java library
     */
    private String getJavaLibraryPath() {
        return path + File.separator + name + ".jar";
    }

    /**
     * A python library has at least a file inside of the library folder
     * called <em>libname</em>.py.
     * The full path would be plugins/libname-1.2.3/libname.py.
     *
     * @return the full path to the Python library
     */
    private String getPythonLibraryPath() {
        return path + File.separator + name + ".py";
    }
}
