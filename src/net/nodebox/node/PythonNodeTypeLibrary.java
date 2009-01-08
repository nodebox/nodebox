package net.nodebox.node;

import org.python.core.Py;
import org.python.core.PyModule;
import org.python.core.imp;
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

        // Set the loaded flag
        loaded = true;
    }

    @Override
    public boolean reload() {
        String moduleName = getPythonModuleName().intern();
        Py.getSystemState().modules.__delitem__(moduleName);
        pythonModule = (PyModule) imp.importName(moduleName, true);
        for (NodeType type : getNodeTypes()) {
            PythonNodeType pnt = (PythonNodeType) type;
            pnt.reloadPython();
        }
        return true;
    }
}

