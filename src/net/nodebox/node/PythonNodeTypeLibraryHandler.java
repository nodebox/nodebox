package net.nodebox.node;

import org.python.core.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.logging.Logger;

public class PythonNodeTypeLibraryHandler extends DefaultHandler {

    private static enum ParseState {
        INVALID, IN_DESCRIPTION
    }

    private static Logger logger = Logger.getLogger("net.nodebox.node.PythonNodeTypeLibraryHandler");

    private PythonNodeTypeLibrary library;
    private NodeType currentNodeType;
    private ParameterType currentParameterType;
    private ParseState parseState = ParseState.INVALID;
    private StringBuffer characterBuffer;

    public PythonNodeTypeLibraryHandler(PythonNodeTypeLibrary library) {
        this.library = library;
    }

    public PythonNodeTypeLibrary getLibrary() {
        return library;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("library")) {
            createNodeTypeLibrary(attributes);
        } else if (qName.equals("type")) {
            createNodeType(attributes);
        } else if (qName.equals("description")) {
            parseState = ParseState.IN_DESCRIPTION;
            characterBuffer = new StringBuffer();
        } else if (qName.equals("parameter")) {
            createParameterType(attributes);
        } else if (qName.equals("option")) {
            addParameterTypeOption(attributes);
        } else {
            throw new SAXException("Unknown tag " + qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("library")) {
            // Top level element -- parsing finished.
        } else if (qName.equals("type")) {
            currentNodeType = null;
        } else if (qName.equals("description")) {
            assert (parseState == ParseState.IN_DESCRIPTION);
            currentNodeType.setDescription(characterBuffer.toString());
            parseState = ParseState.INVALID;
        } else if (qName.equals("parameter")) {
            currentParameterType = null;
        } else if (qName.equals("option")) {
            // End of option tag -- no action required.
        } else {
            throw new SAXException("Unknown tag " + qName);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (parseState == ParseState.IN_DESCRIPTION) {
            characterBuffer.append(ch, start, length);
        }
    }

    private void createNodeTypeLibrary(Attributes attributes) throws SAXException {
        // Make sure we use the correct format and file type.
        String formatVersion = requireAttribute("library", attributes, "formatVersion");
        if (!formatVersion.equals("0.8")) {
            throwLibraryException("unknown format version '" + formatVersion + "'");
        }
        String type = requireAttribute("library", attributes, "type");
        if (type.equals("python")) {
            createPythonNodeTypeLibrary(attributes);
        } else {
            throwLibraryException("cannot parse files of type '" + type + "'");
        }
    }

    private void createPythonNodeTypeLibrary(Attributes attributes) throws SAXException {
        String moduleName = requireAttribute("library", attributes, "module");
        library.setPythonModuleName(moduleName);
        // Load python module
        // TODO: This is the central versioning problem.
        // To properly handle this, we need several system states.
        Py.getSystemState().path.add(new PyString(library.getPath()));

        library.setPythonModule((PyModule) imp.importName(moduleName.intern(), true));
    }

    /**
     * Create a NodeType from the given attributes.
     *
     * @param attributes XML attributes
     * @throws org.xml.sax.SAXException when the method could not be found in the module or is invalid.
     */
    private void createNodeType(Attributes attributes) throws SAXException {
        String name = requireAttribute("type", attributes, "name");
        String outputTypeName = requireAttribute("type", attributes, "outputType");
        String functionName = requireAttribute("type", attributes, "method");
        ParameterType.Type outputType = ParameterType.parseType(outputTypeName);
        PyObject module = library.getPythonModule();
        PyObject functionObject;
        try {
            functionObject = module.__getattr__(functionName.intern());
        } catch (Exception e) {
            throwLibraryException("the method '" + name + "' does not exist in the module " + module);
            return;
        }
        PyFunction function = null;
        try {
            function = (PyFunction) functionObject;
        } catch (ClassCastException e) {
            throwLibraryException("the module attribute '" + functionName + "' is not a Python function.");
        }
        currentNodeType = new PythonNodeType(library, name, outputType, function);
        library.addNodeType(currentNodeType);
    }

    private void throwLibraryException(String message) throws SAXException {
        throw new SAXException("Library " + library.getName() + ": " + message);
    }

    private void createParameterType(Attributes attributes) throws SAXException {
        String name = requireAttribute("parameter", attributes, "name");
        String typeName = requireAttribute("parameter", attributes, "type");
        ParameterType.Type type = ParameterType.parseType(typeName);
        currentParameterType = currentNodeType.addParameterType(name, type);
        String label = attributes.getValue("label");
        String description = attributes.getValue("description");
        String defaultValue = attributes.getValue("defaultValue");
        String boundingMethod = attributes.getValue("boundingMethod");
        String minimumValue = attributes.getValue("minimumValue");
        String maximumValue = attributes.getValue("maximumValue");
        if (label != null)
            currentParameterType.setLabel(label);
        if (description != null)
            currentParameterType.setDescription(description);
        if (defaultValue != null) {
            Object value = currentParameterType.parseValue(defaultValue);
            currentParameterType.setDefaultValue(value);
        }
        if (boundingMethod != null)
            currentParameterType.setBoundingMethod(ParameterType.parseBoundingMethod(boundingMethod));
        if (minimumValue != null) {
            Double value = Double.parseDouble(minimumValue);
            currentParameterType.setMinimumValue(value);
        }
        if (maximumValue != null) {
            Double value = Double.parseDouble(maximumValue);
            currentParameterType.setMaximumValue(value);
        }
    }

    private void addParameterTypeOption(Attributes attributes) throws SAXException {
        if (currentParameterType.getType() != ParameterType.Type.MENU)
            throw new SAXException("Option tag can only be set on menu parameter types.");
        String key = requireAttribute("option", attributes, "key");
        String label = requireAttribute("option", attributes, "label");
        currentParameterType.addMenuItem(key, label);
    }

    private String requireAttribute(String qname, Attributes attributes, String name) throws SAXException {
        String value = attributes.getValue(name);
        if (value == null)
            throwLibraryException("tag " + qname + ": missing required attribute '" + name + "'.");
        return value;
    }


}
