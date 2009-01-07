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

import net.nodebox.client.PlatformUtils;
import net.nodebox.node.canvas.CanvasNetworkType;
import net.nodebox.node.image.ImageNetworkType;
import net.nodebox.node.vector.*;
import org.python.core.PyObject;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeTypeLibrary {

    public static final String LIBRARY_DESCRIPTION_FILE = "types.ntl";
    private static final Pattern TYPE_PATTERN = Pattern.compile(".*type\\s*=\\s*\"(python|java)\".*");
    public static final NodeTypeLibrary BUILTIN = new BuiltinNodeTypeLibrary();

    private static Logger logger = Logger.getLogger("net.nodebox.node.NodeTypeLibrary");

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
    private String path;
    private LibraryType type = null;
    private boolean loaded = false;
    private PyObject pythonModule;

    public NodeTypeLibrary(String name, int majorVersion, int minorVersion, int revisionVersion, File path) {
        this.name = name;
        this.version = new Version(majorVersion, minorVersion, revisionVersion);
        try {
            this.path = path.getCanonicalPath();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "The canonical path for library " + name + " throws IOException ", e);
        }
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

    /**
     * Returns the canonical path for this library.
     *
     * @return the canonical path for this library.
     */
    public String getPath() {
        return path;
    }

    public LibraryType getType() {
        if (type == null) {
            detectType();
        }
        return type;
    }

    //// Types collection ////

    public void addNodeType(NodeType nodeType) {
        types.put(nodeType.getName(), nodeType);
    }

    //// Python module support ////

    public PyObject getPythonModule() {
        return pythonModule;
    }

    public void setPythonModule(PyObject pythonModule) {
        this.pythonModule = pythonModule;
    }

    //// Library loading ////

    /**
     * Detect the type of the library.
     * <p/>
     * This operation doesn't require fully loading the library. It only does some file system lookups, and loads
     * the first line of the types.ntl file to find the type.
     *
     * @return the type of the library.
     */
    private LibraryType detectType() {
        // Read the description file.
        // Instead of parsing the entire file, we load the first line and look for the type="python" pattern
        // to determine the library type.
        File descriptionFile = getLibraryDescriptionFile();
        try {
            FileInputStream fis = new FileInputStream(descriptionFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"), 200);
            String firstLine = reader.readLine();
            Matcher m = TYPE_PATTERN.matcher(firstLine);
            if (!m.matches())
                throw new AssertionError("File does not contain type specifier.");
            String typeName = m.group(1);
            type = LibraryType.valueOf(typeName.toUpperCase());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot read library description file " + descriptionFile, e);
            type = LibraryType.UNKNOWN;
        }
        return type;
    }

    /**
     * Loads the library.
     * <p/>
     * This finds the libraryname.ntl file and loads the node type definitions.
     * <p/>
     * Loading is made public so you can catch errors at an opportune time,
     * not when loading is triggered at an arbitrary point.
     */
    public void load() {
        if (isLoaded()) return;

        // File name
        File file = getLibraryDescriptionFile();
        // Load the document
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Could not read file " + file, e);
            throw new RuntimeException("Could not read file " + file, e);
        }
        InputSource source = new InputSource(fis);

        // Setup the parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // The next lines make sure that the SAX parser doesn't try to validate the document,
        // or tries to load in external DTDs (such as those from W3). Non-parsing means you
        // don't need an internet connection to use the program, and speeds up loading the
        // document massively.
        try {
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Parsing feature not supported.", e);
            throw new RuntimeException("Parsing feature not supported.", e);
        }
        SAXParser parser;
        try {
            parser = factory.newSAXParser();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not create parser.", e);
            throw new RuntimeException("Could not create parser.", e);
        }

        // Parse the document
        TypesHandler handler = new TypesHandler(this);
        try {
            parser.parse(source, handler);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during parsing.", e);
            throw new RuntimeException("Error during parsing.", e);
        }

        // Set the loaded flag
        loaded = true;
    }

    private File getLibraryDescriptionFile() {
        return new File(path + PlatformUtils.SEP + LIBRARY_DESCRIPTION_FILE);
    }

    public boolean isLoaded() {
        return loaded;
    }

    //// Node info ////

    public NodeType getNodeType(String nodeName) throws NotFoundException {
        if (!isLoaded()) load();
        if (types.containsKey(nodeName)) {
            return types.get(nodeName);
        } else {
            throw new NotFoundException(this, nodeName, "Node type " + nodeName + " not found in library " + getName());
        }
    }

    public List<NodeType> getNodeTypes() {
        if (!isLoaded()) load();
        return new ArrayList<NodeType>(types.values());
    }

    public static class BuiltinNodeTypeLibrary extends NodeTypeLibrary {

        public BuiltinNodeTypeLibrary() {
            super("builtin", 1, 0, 0, new File(""));
            // Canvas nodes
            super.addNodeType(new CanvasNetworkType(this));
            // Image nodes
            super.addNodeType(new ImageNetworkType(this));
            // Vector nodes
            super.addNodeType(new CopyType(this));
            super.addNodeType(new EllipseType(this));
            super.addNodeType(new RectType(this));
            super.addNodeType(new TransformType(this));
            super.addNodeType(new VectorNetworkType(this));
        }

        @Override
        public LibraryType getType() {
            return LibraryType.JAVA;
        }

        @Override
        public void addNodeType(NodeType nodeType) {
            throw new AssertionError("You cannot add types to the builtin library.");
        }

        @Override
        public void load() {
            // Builtin library is already loaded.
        }

        @Override
        public boolean isLoaded() {
            return true;
        }


    }

}
