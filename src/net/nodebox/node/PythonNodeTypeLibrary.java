package net.nodebox.node;

import org.python.core.Py;
import org.python.core.PyModule;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PythonNodeTypeLibrary extends NodeTypeLibrary {

    private String pythonModuleName;
    private PyModule pythonModule;
    private String path;
    private boolean loaded = false;

    private static Logger logger = Logger.getLogger("net.nodebox.node.PythonNodeTypeLibrary");

    public PythonNodeTypeLibrary(String name, Version version, File path) {
        super(name, version);
        try {
            this.path = path.getCanonicalPath();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "The canonical path for library " + name + " throws IOException ", e);
        }
    }

    /**
     * add a new node type to this library.
     * <p/>
     * If a node type with the given name already exists, migrate all nodes from the old to the new node type.
     *
     * @param nodeType the new node type to add.
     */
    @Override
    protected void addNodeType(NodeType nodeType) {
        String nodeTypeName = nodeType.getName();
        if (hasNodeType(nodeTypeName)) {
            NodeType oldNodeType = getNodeType(nodeTypeName);
            oldNodeType.migrateType(nodeType);
        }
        super.addNodeType(nodeType);
    }

    //// Python module support ////

    public String getPythonModuleName() {
        return pythonModuleName;
    }

    public void setPythonModuleName(String pythonModuleName) {
        this.pythonModuleName = pythonModuleName;
    }

    public PyModule getPythonModule() {
        return pythonModule;
    }

    public void setPythonModule(PyModule pythonModule) {
        this.pythonModule = pythonModule;
    }


    /**
     * Returns the file that defines this module. We take a few guesses, but if the module structure is too difficult,
     * this method will return null, indicating the file was not found.
     *
     * @return the file that implements this module, or null if no file was found.
     */
    public File getPythonModuleFile() {
        // We don't support module names with dots.
        if (pythonModuleName.contains(".")) {
            return null;
        }

        // Check if the module is implemented as a simple python file, e.g. web.py
        File directModuleFile = new File(getPath(), getPythonModuleName() + ".py");
        if (directModuleFile.exists()) return directModuleFile;

        // Check if the module is a package with a directory.
        File moduleDirectory = new File(getPath(), getPythonModuleName());
        if (moduleDirectory.isDirectory()) {
            // The module code will be a file called __init__.py in the directory
            File initFile = new File(moduleDirectory, "__init__.py");
            if (initFile.exists()) return initFile;
        }

        // Bail out
        return null;
    }

    //// Library loading ////

    @Override
    public String getPath() {
        return path;
    }

    protected File getLibraryDescriptionFile() {
        return NodeTypeLibraryManager.getLibraryDescriptionFile(getPath());
    }

    @Override
    public boolean isLoaded() {
        return loaded;
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

        // Set the loaded flag here, because addNodeType checks hasNodeType, which lazy-loads the library, causing
        // infinite recursion.
        loaded = true;

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
        PythonNodeTypeLibraryHandler handler = new PythonNodeTypeLibraryHandler(this);
        try {
            parser.parse(source, handler);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during parsing.", e);
            throw new RuntimeException("Error during parsing.", e);
        }
    }

    @Override
    public boolean reload() {
        // Remove the module from the loaded libraries.
        String moduleName = getPythonModuleName().intern();
        Py.getSystemState().modules.__delitem__(moduleName);
        // Reload the XML file. Set the loaded flag to false and call the load command.
        loaded = false;
        // Load the library. This creates a new set of NodeType objects that are added to this library.
        // addNodeType is overloaded to check if a type with the same name already exists, and will migrate
        // nodes from the old type to the new type.
        load();
//
//
//        pythonModule = (PyModule) imp.importName(moduleName, true);
//        for (NodeType type : getNodeTypes()) {
//            PythonNodeType pnt = (PythonNodeType) type;
//            pnt.reloadPython();
//        }
        return true;
    }
}

