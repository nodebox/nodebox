package net.nodebox.node;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

public class XmlHandler extends DefaultHandler {

    private enum ParseState {
        OUT_OF_DATA, IN_KEY, IN_INT, IN_FLOAT, IN_STRING, IN_COLOR, IN_EXPRESSION
    }

    private NodeTypeLibraryManager manager;
    private Network rootNetwork;
    private Network currentNetwork;
    private Node currentNode;
    private String currentParameterName;
    private ParseState state = ParseState.OUT_OF_DATA;
    private StringBuffer characterData;

    private static Logger logger = Logger.getLogger("net.nodebox.node.XmlHandler");

    public XmlHandler(NodeTypeLibraryManager manager) {
        this.manager = manager;
    }

    public Network getNetwork() {
        return rootNetwork;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("ndbx")) {
            // Make sure we use the correct format and file type.
            String formatVersion = attributes.getValue("formatVersion");
            if (!formatVersion.equals("0.8")) {
                throw new SAXException("Unknown formatVersion " + formatVersion);
            }
            String type = attributes.getValue("type");
            if (!type.equals("file")) {
                throw new SAXException("Cannot parse ndbx files of type " + type);
            }
        } else if (qName.equals("network")) {
            createNetwork(attributes);
        } else if (qName.equals("node")) {
            createNode(attributes);
        } else if (qName.equals("connection")) {
            createConnection(attributes);
        } else if (qName.equals("data")) {
            state = ParseState.OUT_OF_DATA;
        } else if (qName.equals("key")) {
            state = ParseState.IN_KEY;
            characterData = new StringBuffer();
        } else if (qName.equals("int")) {
            state = ParseState.IN_INT;
            characterData = new StringBuffer();
        } else if (qName.equals("float")) {
            state = ParseState.IN_FLOAT;
            characterData = new StringBuffer();
        } else if (qName.equals("string")) {
            state = ParseState.IN_STRING;
            characterData = new StringBuffer();
        } else if (qName.equals("color")) {
            state = ParseState.IN_COLOR;
            characterData = new StringBuffer();
        } else if (qName.equals("expression")) {
            state = ParseState.IN_EXPRESSION;
            characterData = new StringBuffer();
        } else {
            throw new SAXException("Unknown tag " + qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("ndbx")) {
            // Top level element -- parsing finished.
        } else if (qName.equals("network")) {
            if (currentNetwork != rootNetwork) {
                currentNetwork = currentNetwork.getNetwork();
            }
        } else if (qName.equals("node")) {
            currentNode = null;
        } else if (qName.equals("connection")) {
            // Don't do anything after connection
        } else if (qName.equals("data")) {
            // Don't do anything after data
        } else if (qName.equals("key")) {
            assert (state == ParseState.IN_KEY);
            currentParameterName = characterData.toString();
            state = ParseState.OUT_OF_DATA;
            characterData = null;
        } else if (qName.equals("int")
                || qName.equals("float")
                || qName.equals("string")
                || qName.equals("color")) {
            // Remove state
            state = ParseState.OUT_OF_DATA;
            setValue(currentParameterName, characterData.toString());
            currentParameterName = null;
            characterData = null;
        } else if (qName.equals("expression")) {
            state = ParseState.OUT_OF_DATA;
            setExpression(currentParameterName, characterData.toString());
            currentParameterName = null;
            characterData = null;
        } else {
            throw new SAXException("Unknown tag " + qName);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        switch (state) {
            case IN_KEY:
                characterData.append(ch, start, length);
                currentParameterName = new String(ch, start, length);
                break;
            case IN_INT:
            case IN_FLOAT:
            case IN_STRING:
            case IN_COLOR:
                if (currentParameterName == null)
                    throw new SAXException("Value encountered before key tag.");
                characterData.append(ch, start, length);
                break;
            case IN_EXPRESSION:
                if (currentParameterName == null)
                    throw new SAXException("Expression encountered before key tag.");
                characterData.append(ch, start, length);
        }
    }

    /**
     * Create a network from the given attributes.
     * <p/>
     * This method uses the "type" attribute to get the network type, and looks up the type (and optional version attribute)
     * in the node manager.
     *
     * @param attributes XML attributes. We use "type" and "version".
     * @throws SAXException if the node could not be found, or if the node is not a network.
     */
    private void createNetwork(Attributes attributes) throws SAXException {
        Network newNetwork;
        try {
            newNetwork = (Network) lookupNode(attributes);
        } catch (ClassCastException e) {
            throw new SAXException("Node " + attributes.getValue("type") + " is not a network.");
        }
        if (currentNetwork == null) {
            rootNetwork = newNetwork;
        } else {
            currentNetwork.add(newNetwork);
        }
        currentNode = currentNetwork = newNetwork;
        parseNodeFlags(currentNetwork, attributes);
    }

    private void createNode(Attributes attributes) throws SAXException {
        currentNode = lookupNode(attributes);
        currentNetwork.add(currentNode);
        parseNodeFlags(currentNode, attributes);
    }

    /**
     * Given  a list of XML attributes for the node or network tag, look up the type (with optional version attribute)
     * in the node manager and return a Node. Also sets name and x,y values.
     *
     * @param attributes XML attribute list for node or network tag
     * @return a newly created Node object.
     * @throws SAXException if the node could not be found.
     */
    private Node lookupNode(Attributes attributes) throws SAXException {
        NodeType nodeType;
        Node newNode;
        String identifier = attributes.getValue("type");
        String version = attributes.getValue("version");
        try {
            if (version == null) {
                nodeType = manager.getNodeType(identifier);
            } else {
                nodeType = manager.getNodeType(identifier, new Version(version));
            }
        } catch (NotFoundException e) {
            throw new SAXException("A node with type " + identifier + " and version " + version + " does not exist.");
        }
        newNode = nodeType.createNode();
        String name = attributes.getValue("name");
        if (name != null) {
            newNode.setName(name);
        }
        String x = attributes.getValue("x");
        if (x != null) {
            try {
                newNode.setX(Double.parseDouble(x));
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Could not parse x attribute for node " + name + "[" + identifier + "] value=" + x);
            }
        }
        String y = attributes.getValue("y");
        if (y != null) {
            try {
                newNode.setY(Double.parseDouble(y));
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Could not parse y attribute for node " + name + "[" + identifier + "] value=" + y);
            }
        }
        return newNode;
    }

    /**
     * Parses additional flags in the node. This is done after the node is added to the network.
     * Currently, this parses the "rendered" flag.
     *
     * @param node       the subject node
     * @param attributes XML attributes.
     */
    private void parseNodeFlags(Node node, Attributes attributes) {
        String rendered = attributes.getValue("rendered");
        if ("true".equals(rendered)) {
            node.setRendered();
        }
    }

    private void createConnection(Attributes attributes) throws SAXException {
        String outputNodeName = attributes.getValue("outputNode");
        String inputNodeName = attributes.getValue("inputNode");
        String inputParameterName = attributes.getValue("inputParameter");
        if (outputNodeName == null)
            throw new SAXException("Connection does not have an outputNode attribute set.");
        if (inputNodeName == null)
            throw new SAXException("Connection does not have an inputNode attribute set.");
        if (inputParameterName == null)
            throw new SAXException("Connection does not have an inputParameter attribute set.");
        Node outputNode = currentNetwork.getNode(outputNodeName);
        if (outputNode == null)
            throw new SAXException("Node '" + outputNodeName + "' does not exist in the current network " + currentNetwork + ".");
        Node inputNode = currentNetwork.getNode(inputNodeName);
        if (inputNode == null)
            throw new SAXException("Node '" + inputNodeName + "' does not exist in the current network " + currentNetwork + ".");
        Parameter inputParameter = inputNode.getParameter(inputParameterName);
        if (inputParameter == null)
            throw new SAXException("Node '" + inputNodeName + "' does not have a parameter named " + inputParameterName + ".");
        inputParameter.connect(outputNode);
    }

    /**
     * Sets the value for the given parameter.
     *
     * @param parameterName the name of the parameter to be set
     * @param valueAsString the value of the parameter, to be parsed.
     * @throws org.xml.sax.SAXException when there is no current node, if parameter was not found or if the value could not be parsed.
     */
    private void setValue(String parameterName, String valueAsString) throws SAXException {
        if (currentNode == null)
            throw new SAXException("There is no current node.");
        Parameter parameter;
        try {
            parameter = currentNode.getParameter(parameterName);
        } catch (NotFoundException e) {
            throw new SAXException("Node " + currentNode.getName() + " has no parameter '" + parameterName + "'", e);
        }
        Object value;
        try {
            value = parameter.parseValue(valueAsString);
        } catch (NumberFormatException e) {
            throw new SAXException(parameter.getAbsolutePath() + ": could not parse value '" + valueAsString + "'", e);
        }
        try {
            parameter.setValue(value);
        } catch (ValueError e) {
            throw new SAXException(parameter.getAbsolutePath() + ": value '" + valueAsString + "' invalid for parameter", e);
        }
    }

    /**
     * Sets the expression for the given parameter.
     *
     * @param parameterName the name of the parameter
     * @param expression    the expression string
     * @throws org.xml.sax.SAXException when there is no current node or if parameter was not found.
     */
    private void setExpression(String parameterName, String expression) throws SAXException {
        Parameter parameter = currentNode.getParameter(parameterName);
        if (parameter == null)
            throw new SAXException("Node " + currentNode.getName() + " has no parameter '" + parameterName + "'");
        parameter.setExpression(expression);
    }

}
