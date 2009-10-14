package nodebox.node;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses the ndbx file format.
 * <p/>
 * Because you can define both prototypes and "instances" that use these prototypes in the same file,
 * you have to make sure that you define the prototypes before you instantiate them. NDBX can't handle
 * files with arbitrary ordering. NodeLibrary.store() has support for sequential ordering of prototypes.
 * This does not concern prototypes that are defined in other files or are builtin, since they will be
 * loaded already.
 */
public class NDBXHandler extends DefaultHandler {

    enum ParseState {
        INVALID, IN_CODE, IN_DESCRIPTION, IN_VALUE, IN_MENU, IN_EXPRESSION
    }

    enum CodeType {
        INVALID, PYTHON, JAVA
    }

    public static final String NDBX_FORMAT_VERSION = "formatVersion";
    public static final String VAR_NAME = "name";
    public static final String VAR_VALUE = "value";
    public static final String CODE_TYPE = "type";
    public static final String NODE_NAME = "name";
    public static final String NODE_PROTOTYPE = "prototype";
    public static final String NODE_TYPE = "type";
    public static final String NODE_X = "x";
    public static final String NODE_Y = "y";
    public static final String NODE_RENDERED = "rendered";
    public static final String MENU_KEY = "key";
    public static final String PARAMETER_NAME = "name";
    public static final String PARAMETER_TYPE = "type";
    public static final String PARAMETER_WIDGET = "widget";
    public static final String PARAMETER_LABEL = "label";
    public static final String PARAMETER_HELP_TEXT = "help";
    public static final String PARAMETER_DISPLAY_LEVEL = "display";
    public static final String PARAMETER_BOUNDING_METHOD = "bounding";
    public static final String PARAMETER_MINIMUM_VALUE = "min";
    public static final String PARAMETER_MAXIMUM_VALUE = "max";
    public static final String VALUE_TYPE = "type";
    public static final String PORT_NAME = "name";
    public static final String PORT_CARDINALITY = "cardinality";
    public static final String CONNECTION_OUTPUT = "output";
    public static final String CONNECTION_INPUT = "input";
    public static final String CONNECTION_PORT = "port";

    private NodeLibraryManager manager;
    private NodeLibrary library;
    private Node rootNode;
    private Node currentNode;
    private Parameter currentParameter;
    private String currentMenuKey;
    private CodeType currentCodeType = CodeType.INVALID;
    private Map<Parameter, String> expressionMap = new HashMap<Parameter, String>();
    private ParseState state = ParseState.INVALID;
    private StringBuffer characterData;

    public NDBXHandler(NodeLibrary library, NodeLibraryManager manager) {
        this.manager = manager;
        this.library = library;
        this.rootNode = library.getRootNode();
        currentNode = null;
    }

    public NodeLibrary geNodeLibrary() {
        return library;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("ndbx")) {
            startNdbxTag(attributes);
        } else if (qName.equals("var")) {
            startVarTag(attributes);
        } else if (qName.equals("code")) {
            startCodeTag(attributes);
        } else if (qName.equals("node")) {
            startNodeTag(attributes);
        } else if (qName.equals("description")) {
            startDescriptionTag(attributes);
        } else if (qName.equals("param")) {
            startParameterTag(attributes);
        } else if (qName.equals("value")) {
            startValueTag(attributes);
        } else if (qName.equals("expression")) {
            startExpressionTag(attributes);
        } else if (qName.equals("menu")) {
            startMenuTag(attributes);
        } else if (qName.equals("port")) {
            startPortTag(attributes);
        } else if (qName.equals("conn")) {
            startConnectionTag(attributes);
        } else {
            throw new SAXException("Unknown tag " + qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("ndbx")) {
            // Top level element -- parsing finished.
        } else if (qName.equals("var")) {
            // Do nothing after var tag
        } else if (qName.equals("code")) {
            setLibraryCode(characterData.toString());
            resetState();
            currentCodeType = CodeType.INVALID;
        } else if (qName.equals("node")) {
            // Traverse up to the parent.
            // This can result in currentNode being null if we traversed all the way up
            currentNode = currentNode.getParent();
        } else if (qName.equals("description")) {
            setDescription(characterData.toString());
            resetState();
        } else if (qName.equals("param")) {
            currentParameter = null;
        } else if (qName.equals("value")) {
            setValue(characterData.toString());
            resetState();
            currentCodeType = CodeType.INVALID;
        } else if (qName.equals("expression")) {
            setTemporaryExpression(characterData.toString());
            resetState();
        } else if (qName.equals("menu")) {
            setMenuItem(characterData.toString());
            resetState();
            currentMenuKey = null;
        } else if (qName.equals("port")) {
            // Do nothing after port tag
        } else if (qName.equals("conn")) {
            // Do nothing after conn tag
        } else {
            // This should never happen, since the SAX parser has already formally validated the document.
            // Unknown tags should be caught in startElement.
            throw new AssertionError("Unknown end tag " + qName);
        }
    }

    /**
     * Called after valid character data was processed.
     * <p/>
     * This makes sure no extraneous data is added.
     */
    private void resetState() {
        state = ParseState.INVALID;
        characterData = null;
        currentMenuKey = null;
    }

    @Override
    public void endDocument() throws SAXException {
        // Since parameter expressions can refer to arbitrary other parameters in the network,
        // we need to have the fully created document first before setting expressions.
        // Expressions are evaluated once they are set so they can create dependencies on other
        // parameters.
        for (Map.Entry<Parameter, String> entry : expressionMap.entrySet()) {
            entry.getKey().setExpression(entry.getValue());
        }
    }


    private void startNdbxTag(Attributes attributes) throws SAXException {
        // Make sure we use the correct format and file type.
        String formatVersion = attributes.getValue(NDBX_FORMAT_VERSION);
        if (formatVersion == null)
            throw new SAXException("NodeBox file does not have required attribute formatVersion.");
        if (!formatVersion.equals("0.9"))
            throw new SAXException("Unknown formatVersion " + formatVersion);
    }

    private void startVarTag(Attributes attributes) throws SAXException {
        // Variables that get stored in the NodeBox library.
        String name = attributes.getValue(VAR_NAME);
        String value = attributes.getValue(VAR_VALUE);
        if (name == null) throw new SAXException("Name attribute is required in var tags.");
        if (value == null) throw new SAXException("Value attribute is required in var tags.");
        library.setVariable(name, value);
    }

    private void startCodeTag(Attributes attributes) throws SAXException {
        String type = attributes.getValue(CODE_TYPE);
        if (type == null) throw new SAXException("Type attribute is required in code tags.");
        try {
            currentCodeType = CodeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SAXException("Invalid type attribute in code tag: should be python or java, not " + type + ".");
        }
        state = ParseState.IN_CODE;
        characterData = new StringBuffer();
    }

    private void setLibraryCode(String code) throws SAXException {
        library.setCode(parseCode(code));
    }

    private void startNodeTag(Attributes attributes) throws SAXException {
        String name = attributes.getValue(NODE_NAME);
        String prototypeId = attributes.getValue(NODE_PROTOTYPE);
        String typeAsString = attributes.getValue(NODE_TYPE);
        if (name == null) throw new SAXException("Name attribute is required in node tags.");
        if (prototypeId == null) throw new SAXException("Prototype attribute is required in node tags.");
        Class dataClass = null;
        if (typeAsString != null) {
            try {
                dataClass = Class.forName(typeAsString);
            } catch (ClassNotFoundException e) {
                throw new SAXException("Given type " + typeAsString + " not found.");
            }
        }
        // Switch between relative and long identifiers.
        // Long identifiers (e.g. "polygraph.rect") contain both a library and name and should be looked up using the manager.
        // Short identifiers (e.g. "beta") contain only a name and are in the same library as this node.
        // They should be looked up using the library.
        Node prototype;
        if (prototypeId.contains(".")) {
            // Long identifier
            prototype = manager.getNode(prototypeId);
        } else {
            // Short identifier
            prototype = library.get(prototypeId);
        }
        if (prototype == null) throw new SAXException("Unknown prototype " + prototypeId + " for node " + name);
        Node newNode = prototype.newInstance(library, name, dataClass);
        // Add the child to the node library or its parent
        if (currentNode == null) {
            library.add(newNode);
        } else {
            currentNode.add(newNode);
        }
        // Parse additional node flags.
        String x = attributes.getValue(NODE_X);
        String y = attributes.getValue(NODE_Y);
        if (x != null)
            newNode.setX(Double.parseDouble(x));
        if (y != null)
            newNode.setY(Double.parseDouble(y));
        if ("true".equals(attributes.getValue(NODE_RENDERED)))
            newNode.setRendered();
        // Go down into the current node; this will now become the current network.
        currentNode = newNode;
    }

    private void startDescriptionTag(Attributes attributes) throws SAXException {
        if (currentNode == null) throw new SAXException("Description tag encountered without a current node.");
        state = ParseState.IN_DESCRIPTION;
        characterData = new StringBuffer();
    }

    private void setDescription(String description) throws SAXException {
        if (currentNode == null) throw new SAXException("Description tag ended without a current node.");
        currentNode.setDescription(description);
    }

    private void startParameterTag(Attributes attributes) throws SAXException {
        String name = attributes.getValue(PARAMETER_NAME);
        String typeAsString = attributes.getValue(PARAMETER_TYPE);

        if (currentNode == null) throw new SAXException("Parameter tag encountered without a current node.");
        if (name == null)
            throw new SAXException("Name is required for parameter on node '" + currentNode.getName() + "'.");

        if (typeAsString == null) {
            // No type attribute was given, so the parameter should already exist.
            currentParameter = currentNode.getParameter(name);
            if (currentParameter == null)
                throw new SAXException("Parameter '" + name + "' for node '" + currentNode.getName() + "' does not exist.");
        } else {
            // Type was given, so this is a new parameter.
            // TODO: If type is given and parameter exists, migrate type.
            if (currentNode.hasParameter(name))
                throw new SAXException("Parameter '" + name + "' for node '" + currentNode.getName() + "' already exists.");
            Parameter.Type type = Parameter.Type.valueOf(typeAsString.toUpperCase());
            currentParameter = currentNode.addParameter(name, type);
        }
        // Parse parameter attributes.
        String widget = attributes.getValue(PARAMETER_WIDGET);
        String label = attributes.getValue(PARAMETER_LABEL);
        String helpText = attributes.getValue(PARAMETER_HELP_TEXT);
        String displayLevel = attributes.getValue(PARAMETER_DISPLAY_LEVEL);
        String boundingMethod = attributes.getValue(PARAMETER_BOUNDING_METHOD);
        String minimumValue = attributes.getValue(PARAMETER_MINIMUM_VALUE);
        String maximumValue = attributes.getValue(PARAMETER_MAXIMUM_VALUE);
        if (widget != null)
            currentParameter.setWidget(Parameter.Widget.valueOf(widget.toUpperCase()));
        if (label != null)
            currentParameter.setLabel(label);
        if (helpText != null)
            currentParameter.setHelpText(helpText);
        if (displayLevel!= null)
            currentParameter.setDisplayLevel(Parameter.DisplayLevel.valueOf(displayLevel.toUpperCase()));
        if (boundingMethod != null)
            currentParameter.setBoundingMethod(Parameter.BoundingMethod.valueOf(boundingMethod.toUpperCase()));
        if (minimumValue != null)
            currentParameter.setMinimumValue(Float.parseFloat(minimumValue));
        if (maximumValue != null)
            currentParameter.setMaximumValue(Float.parseFloat(maximumValue));
    }

    /**
     * Parse the value tag. This tag is inside of the param tag.
     *
     * @param attributes tag attributes
     * @throws SAXException if the current parameter is null or a code parameter has no or invalid value type.
     */
    private void startValueTag(Attributes attributes) throws SAXException {
        if (currentParameter == null) throw new SAXException("Value tag encountered without current parameter.");
        state = ParseState.IN_VALUE;
        characterData = new StringBuffer();
        // The value tag should be empty except when the parameter type is code.
        // Then the value tag has a type attribute that specifies the code type.
        if (currentParameter.getType() != Parameter.Type.CODE) return;
        String type = attributes.getValue(VALUE_TYPE);
        if (type == null) throw new SAXException("Type attribute is required in code type parameters.");
        try {
            currentCodeType = CodeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SAXException("Invalid type attribute in code tag: should be python or java, not " + type + ".");
        }
    }

    /**
     * Sets the value on the current parameter.
     *
     * @param valueAsString the value of the parameter, to be parsed.
     * @throws org.xml.sax.SAXException when there is no current node, if parameter was not found or if the value could not be parsed.
     */
    private void setValue(String valueAsString) throws SAXException {
        if (currentParameter == null) throw new SAXException("There is no current parameter.");
        Object value;
        if (currentParameter.getType() == Parameter.Type.CODE) {
            value = parseCode(valueAsString);
        } else {
            try {
                value = currentParameter.parseValue(valueAsString);
            } catch (IllegalArgumentException e) {
                throw new SAXException(currentParameter.getAbsolutePath() + ": could not parse value '" + valueAsString + "'", e);
            }
        }
        try {
            currentParameter.setValue(value);
        } catch (IllegalArgumentException e) {
            throw new SAXException(currentParameter.getAbsolutePath() + ": value '" + valueAsString + "' invalid for parameter", e);
        }
    }


    /**
     * Parse the expression tag. This tag is inside of the param tag.
     *
     * @param attributes tag attributes
     * @throws SAXException if the current parameter is null or a code parameter has no or invalid value type.
     */
    private void startExpressionTag(Attributes attributes) throws SAXException {
        if (currentParameter == null) throw new SAXException("Expression tag encountered without current parameter.");
        state = ParseState.IN_EXPRESSION;
        characterData = new StringBuffer();
    }

    /**
     * Parse the expression tag. This tag is inside of the param tag.
     *
     * @param attributes tag attributes
     * @throws SAXException if the current parameter is null or a code parameter has no or invalid value type.
     */
    private void startMenuTag(Attributes attributes) throws SAXException {
        if (currentParameter == null) throw new SAXException("Menu tag encountered without current parameter.");
        state = ParseState.IN_MENU;
        String key = attributes.getValue(MENU_KEY);
        if (key == null)
            throw new SAXException("Attribute key for menu tag cannot be null.");
        currentMenuKey = key;
        characterData = new StringBuffer();
    }

    /**
     * Sets the menu item on the current parameter.
     * <p/>
     * The menu key was already set as an attribute on the menu start tag.
     *
     * @param label the character data for the menu label.
     */
    private void setMenuItem(String label) {
        if (currentMenuKey == null) throw new AssertionError("Menu tag ends, but menu key is null.");
        currentParameter.addMenuItem(currentMenuKey, label);
    }

    /**
     * Parses the given source code and returns a new NodeCode object of the correct type.
     * <p/>
     * This method assumes that the currentCodeType is set.
     *
     * @param source the source code
     * @return a NodeCode object
     * @throws org.xml.sax.SAXException when there is no current node, if parameter was not found or if the value could not be parsed.
     */
    private NodeCode parseCode(String source) throws SAXException {
        if (currentCodeType == CodeType.PYTHON) {
            return new PythonCode(source);
        } else if (currentCodeType == CodeType.JAVA) {
            throw new SAXException("Support for Java code is not implemented yet.");
        } else {
            throw new SAXException("Invalid code type.");
        }
    }

    /**
     * Sets the expression on the current parameter.
     * <p/>
     * Expressions are not set directly, because all dependencies can not always be met directly.
     * So expressions are stored in a temporary map. When the whole document is parsed, all expressions will be set,
     * which will also set the correct dependencies.
     *
     * @param expression the expression string
     * @throws org.xml.sax.SAXException when there is no current node or if parameter was not found.
     */
    private void setTemporaryExpression(String expression) throws SAXException {
        if (currentParameter == null) throw new SAXException("There is no current parameter.");
        expressionMap.put(currentParameter, expression);
    }

    private void startPortTag(Attributes attributes) throws SAXException {
        String name = attributes.getValue(PORT_NAME);
        String cardinalityAsString = attributes.getValue(PORT_CARDINALITY);
        if (name == null)
            throw new SAXException("Name is required for port on node '" + currentNode.getName() + "'.");
        Port.Cardinality cardinality = Port.Cardinality.SINGLE;
        if (cardinalityAsString != null) {
            try {
                cardinality = Port.Cardinality.valueOf(cardinalityAsString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new SAXException("Invalid cardinality attribute in port tag: should be single or multiple, not " + cardinalityAsString + ".");
            }
        }
        currentNode.addPort(name, cardinality);
    }

    private void startConnectionTag(Attributes attributes) throws SAXException {
        // output node identifier, without package
        String outputAsString = attributes.getValue(CONNECTION_OUTPUT);
        // input node identifier, without package
        String inputAsString = attributes.getValue(CONNECTION_INPUT);
        // input port identifier
        String portAsString = attributes.getValue(CONNECTION_PORT);

        String currentNodeString = currentNode == null ? "<null>" : currentNode.getName();

        if (outputAsString == null)
            throw new SAXException("Output is required for connection in node '" + currentNodeString + "'.");
        if (inputAsString == null)
            throw new SAXException("Input is required for connection in node '" + currentNodeString + "'.");
        if (portAsString == null)
            throw new SAXException("Port is required for connection in node '" + currentNodeString + "'.");

        Node output, input;
        if (currentNode == null) {
            output = library.get(outputAsString);
            input = library.get(inputAsString);
        } else {
            output = currentNode.getChild(outputAsString);
            input = currentNode.getChild(inputAsString);
        }
        if (output == null)
            throw new SAXException("Output node '" + outputAsString + "' does not exist.");
        if (input == null)
            throw new SAXException("Input node '" + inputAsString + "' does not exist.");
        Port port = input.getPort(portAsString);
        if (port == null)
            throw new SAXException("Port '" + portAsString + "' on node '" + inputAsString + "' does not exist.");
        port.connect(output);
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        switch (state) {
            case IN_CODE:
                if (currentCodeType == null)
                    throw new SAXException("Code encountered, but no current code type.");
                break;
            case IN_DESCRIPTION:
                if (currentNode == null)
                    throw new SAXException("Description encountered, but no current node.");
                break;
            case IN_VALUE:
                if (currentParameter == null)
                    throw new SAXException("Value encountered, but no current parameter.");
                break;
            case IN_EXPRESSION:
                if (currentParameter == null)
                    throw new SAXException("Expression encountered, but no current parameter.");
                break;
            case IN_MENU:
                if (currentParameter == null)
                    throw new SAXException("Menu encountered, but no current parameter.");
                break;
            default:
                // Bail out when we don't recognize this state.
                return;
        }
        // We have a valid character state, so we can safely append to characterData.
        characterData.append(ch, start, length);
    }

}
