package nodebox.node;

import nodebox.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.io.Writer;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/**
 * Writes the ndbx file format.
 */
public class NDBXWriter {

    public static void write(NodeLibrary library, File file) {
        StreamResult streamResult = new StreamResult(file);
        write(library, streamResult);

    }

    public static void write(NodeLibrary library, Writer writer) {
        StreamResult streamResult = new StreamResult(writer);
        write(library, streamResult);
    }

    public static void write(NodeLibrary library, StreamResult streamResult) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();

            // Build the header.
            Element rootElement = doc.createElement("ndbx");
            doc.appendChild(rootElement);
            rootElement.setAttribute("type", "file");
            rootElement.setAttribute("formatVersion", "0.9");

            // Write out all the variables.
            for (String variableName : library.getVariableNames()) {
                String variableValue = library.getVariable(variableName);
                Element varElement = doc.createElement("var");
                doc.appendChild(varElement);
                varElement.setAttribute("value", variableValue);
            }

            // Write out all the nodes (skip the root)
            List<Node> children = library.getRootNode().getChildren();
            // The order in which the nodes are written is important!
            // Since a library can potentially store an instance and its prototype, make sure that the prototype gets
            // stored sequentially before its instance.
            // The NDBXHandler class expects prototypes to be defined before their instances.
            while (!children.isEmpty()) {
                Node child = children.get(0);
                writeOrderedChild(library, doc, rootElement, children, child);
            }

            // Write out all the child connections.
            for (Node child : library.getRootNode().getChildren()) {
                for (Connection conn : child.getUpstreamConnections()) {
                    writeConnection(doc, rootElement, conn);
                }
            }

            // Convert the document to XML.
            DOMSource domSource = new DOMSource(doc);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(domSource, streamResult);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public static String asString(NodeLibrary library) {
        StringWriter writer = new StringWriter();
        write(library, writer);
        return writer.toString();
    }

    /**
     * Write out the child. If the prototype of the child is also in this library, write that out first, recursively.
     *
     * @param library  the node library
     * @param doc      the XML document
     * @param parent   the parent element
     * @param children a list of children that were written already.
     *                 When a child is written, we remove it from the list.
     * @param child    the child to write
     */
    private static void writeOrderedChild(NodeLibrary library, Document doc, Element parent, List<Node> children, Node child) {
        Node prototype = child.getPrototype();
        if (prototype.getLibrary() == library && children.contains(prototype))
            writeOrderedChild(library, doc, parent, children, prototype);
        writeNode(doc, parent, child);
        children.remove(child);
    }

    private static void writeNode(Document doc, Element parent, Node node) {
        String xPosition = String.format(Locale.US, "%.0f", node.getX());
        String yPosition = String.format(Locale.US, "%.0f", node.getY());
        Element el = doc.createElement("node");
        parent.appendChild(el);
        el.setAttribute("name", node.getName());
        el.setAttribute("prototype", node.getPrototype().getRelativeIdentifier(node));
        el.setAttribute("x", xPosition);
        el.setAttribute("y", yPosition);
        if (node.isRendered())
            el.setAttribute("rendered", "true");

        // Add the output type if it is different from the prototype.
        if (node.getOutputPort().getDataClass() != node.getPrototype().getOutputPort().getDataClass())
            el.setAttribute("type", node.getOutputPort().getDataClass().getName());

        // Add the description
        if (!node.getDescription().equals(node.getPrototype().getDescription())) {
            Element desc = doc.createElement("description");
            el.appendChild(desc);
            Text descText = doc.createTextNode(node.getDescription());
            desc.appendChild(descText);
        }

        // Add the ports
        for (Port port : node.getPorts()) {
            writePort(doc, el, port);
        }

        // Add the parameters
        for (Parameter param : node.getParameters()) {
            // We've written the description above in the <description> tag.
            if (param.getName().equals("_description")) continue;
            writeParameter(doc, el, param);
        }

        // Add all child nodes
        for (Node child : node.getChildren()) {
            writeNode(doc, el, child);
        }


        // Add all child connections
        for (Node child : node.getChildren()) {
            for (Connection conn : child.getUpstreamConnections()) {
                writeConnection(doc,el,conn);
            }
        }
    }

    private static void writeParameter(Document doc, Element parent, Parameter param) {
        // We only write out the attributes that have changed with regards to the prototype.
        Parameter protoParam = param.getPrototype();
        // If the parameter and its prototype are completely equal, don't write anything.
        if (param.prototypeEquals(protoParam)) return;
        Element el = doc.createElement("param");
        parent.appendChild(el);
        // The parameters are not equal, so we can start writing the name.
        el.setAttribute("name", param.getName());
        // Write parameter type
        if (protoParam == null || !param.getType().equals(protoParam.getType()))
            el.setAttribute(NDBXHandler.PARAMETER_TYPE, param.getType().toString().toLowerCase());
        // Write parameter attributes
        attributeToXml(param, el, "widget", NDBXHandler.PARAMETER_WIDGET, protoParam, Parameter.WIDGET_MAPPING.get(param.getType()));
        attributeToXml(param, el, "label", NDBXHandler.PARAMETER_LABEL, protoParam, StringUtils.humanizeName(param.getName()));
        attributeToXml(param, el, "helpText", NDBXHandler.PARAMETER_HELP_TEXT, protoParam, null);
        attributeToXml(param, el, "displayLevel", NDBXHandler.PARAMETER_DISPLAY_LEVEL, protoParam, Parameter.DisplayLevel.HUD);
        attributeToXml(param, el, "boundingMethod", NDBXHandler.PARAMETER_BOUNDING_METHOD, protoParam, Parameter.BoundingMethod.NONE);
        attributeToXml(param, el, "minimumValue", NDBXHandler.PARAMETER_MINIMUM_VALUE, protoParam, null);
        attributeToXml(param, el, "maximumValue", NDBXHandler.PARAMETER_MAXIMUM_VALUE, protoParam, null);
        // Write parameter value / expression
        if (param.hasExpression()) {
            appendText(doc, el, "expression", param.getExpression());
        } else {
            switch (param.getType()) {
                case INT:
                    appendText(doc, el, "value", param.asInt());
                    break;
                case FLOAT:
                    appendText(doc, el, "value", param.asFloat());
                    break;
                case STRING:
                    appendText(doc, el, "value", param.asString());
                    break;
                case COLOR:
                    appendText(doc, el, "value", param.asColor());
                    break;
                case CODE:
                    Element value = doc.createElement("value");
                    value.setAttribute("type", param.asCode().getType());
                    value.appendChild(doc.createCDATASection(param.asCode().getSource()));
                    el.appendChild(value);
                    break;
            }
        }
        // Write menu items
        List<Parameter.MenuItem> items = param.getMenuItems();
        if (items.size() > 0) {
            List<Parameter.MenuItem> protoItems = protoParam == null ? null : protoParam.getMenuItems();
            if (!items.equals(protoItems)) {
                for (Parameter.MenuItem item : items) {
                    Element menu = doc.createElement("menu");
                    el.appendChild(menu);
                    menu.setAttribute("key", item.getKey());
                    Text menuText = doc.createTextNode(item.getLabel());
                    menu.appendChild(menuText);
                }
            }
        }
    }

    private static void writePort(Document doc, Element parent, Port port) {
        // We only write out the ports that have changed with regards to the prototype.
        Node protoNode = port.getNode().getPrototype();
        Port protoPort = null;
        if (protoNode != null)
            protoPort = protoNode.getPort(port.getName());
        // If the port and its prototype are equal, don't write anything.
        if (protoPort != null
                && protoPort.getName().equals(port.getName())
                && protoPort.getDataClass().equals(port.getDataClass())
                && protoPort.getDirection().equals(port.getDirection())
                && protoPort.getCardinality().equals(port.getCardinality())) return;
        Element el = doc.createElement("port");
        el.setAttribute("name", port.getName());
        el.setAttribute("type", port.getDataClass().getName());
        if (port.getCardinality() != Port.Cardinality.SINGLE)
            el.setAttribute("cardinality", port.getCardinality().toString().toLowerCase());
        parent.appendChild(el);
    }

    private static void writeConnection(Document doc, Element parent, Connection conn) {
        for (Port output : conn.getOutputs()) {
            Element connElement = doc.createElement("conn");
            connElement.setAttribute("output", output.getNode().getName());
            connElement.setAttribute("input", conn.getInputNode().getName());
            connElement.setAttribute("port", conn.getInput().getName());
            parent.appendChild(connElement);
        }
    }

    private static void attributeToXml(Parameter param, Element el, String attrName, String xmlName, Parameter protoParam, Object defaultValue) {
        try {
            String methodName = "get" + attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
            Method m = param.getClass().getMethod(methodName);
            Object myValue = m.invoke(param);
            if (myValue == null) return;
            Object protoValue = protoParam != null ? m.invoke(protoParam) : null;
            if (!myValue.equals(protoValue) && !myValue.equals(defaultValue)) {
                // Values that are already strings are written as is.
                // Other values, such as enums, are written as lowercase.
                String stringValue = myValue instanceof String ? (String) myValue : myValue.toString().toLowerCase();
                el.setAttribute(xmlName, stringValue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while trying to get " + attrName + " for parameter " + param, e);
        }
    }

    private static void appendText(Document doc, Element parent, String name, Object text) {
        Element el = doc.createElement(name);
        el.appendChild(doc.createTextNode(text.toString()));
        parent.appendChild(el);
    }
}
