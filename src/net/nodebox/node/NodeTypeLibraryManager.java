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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeTypeLibraryManager {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^([a-z]+)\\.([a-z]+)$");


    /**
     * A list of node type libraries with the same name but different versions.
     * <p/>
     * The list is ordered from the newest (highest) version to the oldest (lowest) version.
     */
    public static class VersionedLibraryList {
        private List<NodeTypeLibrary> libraries = new ArrayList<NodeTypeLibrary>();

        public void addLibrary(NodeTypeLibrary library) {
            Version newVersion = library.getVersion();
            int i = 0;
            for (; i < libraries.size(); i++) {
                NodeTypeLibrary l = libraries.get(i);
                if (l.getVersion().smallerThan(newVersion))
                    break;
            }
            libraries.add(i, library);
        }

        public NodeTypeLibrary getLatestVersion() {
            return libraries.get(0);
        }

        public List<NodeTypeLibrary> getLibraries() {
            return libraries;
        }

    }

    private static Logger logger = Logger.getLogger("net.nodebox.node.NodeTypeLibrary");

    private ArrayList<File> searchPaths = new ArrayList<File>();
    private HashMap<String, VersionedLibraryList> libraryMap = new HashMap<String, VersionedLibraryList>();
    private boolean lookedForLibraries = false;

    public NodeTypeLibraryManager() {
        addLibrary(NodeTypeLibrary.BUILTIN);
    }

    public NodeTypeLibraryManager(String... searchPaths) throws IOException {
        addLibrary(NodeTypeLibrary.BUILTIN);
        for (String searchPath : searchPaths) {
            addSearchPath(searchPath);
        }
    }

    public NodeTypeLibraryManager(File... searchPaths) throws IOException {
        addLibrary(NodeTypeLibrary.BUILTIN);
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


    /**
     * Finds and returns the latest version of the library with the given name.
     *
     * @param libraryName the name of the library
     * @return a NodeTypeLibrary object.
     * @throws NotFoundException if the node could not be found
     */
    public NodeTypeLibrary getLibrary(String libraryName) throws NotFoundException {
        if (!lookedForLibraries)
            lookForLibraries();
        VersionedLibraryList libraryList = libraryMap.get(libraryName);
        if (libraryList == null)
            throw new NotFoundException(this, libraryName, "The manager cannot find library '" + libraryName + "'.");
        return libraryList.getLatestVersion();
    }

    /**
     * Finds and returns the exact specified version of the library type with the given name.
     *
     * @param libraryName the identifier in reverse-DNS format (e.g. net.nodebox.node.vector.RectNode)
     * @param version     the exact version number you want to retrieve.
     * @return a Node object or null if no node with that name was found.
     * @throws NotFoundException if the node type could not be found
     */
    public NodeTypeLibrary getLibrary(String libraryName, Version version) throws NotFoundException {
        if (!lookedForLibraries)
            lookForLibraries();
        VersionedLibraryList libraryList = libraryMap.get(libraryName);
        if (libraryList == null)
            throw new NotFoundException(this, libraryName, "The manager cannot find library '" + libraryName + "'.");
        for (NodeTypeLibrary l : libraryList.getLibraries()) {
            if (l.getVersion().equals(version))
                return l;
        }
        throw new NotFoundException(this, libraryName, "The manager cannot find library '" + libraryName + "'.");
    }

    /**
     * Finds and returns the specified version of the node with the given qualified name.
     *
     * @param libraryName the identifier in reverse-DNS format (e.g. net.nodebox.node.vector.RectNode)
     * @param specifier   the version specifier you want to retrieve.
     * @return a Node object.
     * @throws NotFoundException if the node type could not be found
     */
    public NodeTypeLibrary getLibrary(String libraryName, VersionSpecifier specifier) throws NotFoundException {
        if (!lookedForLibraries)
            lookForLibraries();
        VersionedLibraryList libraryList = libraryMap.get(libraryName);
        if (libraryList == null)
            throw new NotFoundException(this, libraryName, "The manager cannot find library '" + libraryName + "'.");
        for (NodeTypeLibrary l : libraryList.getLibraries()) {
            if (specifier.matches(l.getVersion()))
                return l;
        }
        throw new NotFoundException(this, libraryName, "The manager cannot find library '" + libraryName + "'.");
    }

    /**
     * Gets the latest version of all available libraries. Each library only occurs once, and with its latest version.
     *
     * @return a list of libraries.
     */
    public List<NodeTypeLibrary> getLibraries() {
        List<NodeTypeLibrary> libraries = new ArrayList<NodeTypeLibrary>();
        for (VersionedLibraryList versionedList : libraryMap.values()) {
            libraries.add(versionedList.getLatestVersion());
        }
        return libraries;
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
                            assert (!libraryMap.containsKey(library.getName()));
                            addLibrary(library);
                        } catch (RuntimeException e) {
                            logger.log(Level.WARNING, "Cannot load library " + f);
                        }
                    }
                }
            }
        }
    }

    public void addLibrary(NodeTypeLibrary library) {
        VersionedLibraryList libraryList = libraryMap.get(library.getName());
        if (libraryList == null) {
            libraryList = new VersionedLibraryList();
            libraryMap.put(library.getName(), libraryList);
        }
        libraryList.addLibrary(library);
    }

    //// NodeType lookup shortcuts ////

    /**
     * Looks up the node type given an identifier that contains a library name.
     * <p/>
     * The identifier has a library name and node type name separated by a dot, e.g.
     * "corevector.ellipse".
     *
     * @param identifier the identifier, e.g. "corevector.ellipse"
     * @return the NodeType
     * @throws NotFoundException if the library or node type could not be found.
     */
    public NodeType getNodeType(String identifier) throws NotFoundException {
        Matcher m = IDENTIFIER_PATTERN.matcher(identifier);
        if (!m.matches())
            throw new NotFoundException(this, identifier, "The identifier pattern is not in the correct format");
        assert (m.groupCount() == 2);
        String libraryName = m.group(1);
        String nodeTypeName = m.group(2);
        NodeTypeLibrary library = getLibrary(libraryName);
        return library.getNodeType(nodeTypeName);
    }

    /**
     * Looks up a specific version of the node type given an identifier composed of library and type name.
     * <p/>
     * The identifier has a library name and node type name separated by a dot, e.g.
     * "corevector.ellipse".
     *
     * @param identifier the identifier, e.g. "corevector.ellipse"
     * @param version    the specific library version to retrieve.
     * @return the NodeType
     * @throws NotFoundException if the library or node type could not be found.
     */
    public NodeType getNodeType(String identifier, Version version) throws NotFoundException {
        Matcher m = IDENTIFIER_PATTERN.matcher(identifier);
        if (!m.matches())
            throw new NotFoundException(this, identifier, "The identifier pattern is not in the correct format");
        assert (m.groupCount() == 2);
        String libraryName = m.group(1);
        String nodeTypeName = m.group(2);
        NodeTypeLibrary library = getLibrary(libraryName, version);
        return library.getNodeType(nodeTypeName);
    }

    /**
     * Returns a list of all NodeTypes available in the latest versions of all libraries.
     *
     * @return a list of NodeTypes.
     */
    public List<NodeType> getNodeTypes() {
        List<NodeType> types = new ArrayList<NodeType>();
        for (NodeTypeLibrary library : getLibraries()) {
            types.addAll(library.getNodeTypes());
        }
        return types;
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
        return new NodeTypeLibrary(libraryName, v.getMajor(), v.getMinor(), v.getRevision(), new File(searchPath, path));
    }

}
