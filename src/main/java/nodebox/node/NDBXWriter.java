package nodebox.node;

import com.google.common.base.Objects;
import nodebox.function.CoreFunctions;
import nodebox.function.FunctionLibrary;
import nodebox.function.FunctionRepository;
import nodebox.graphics.Point;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Writes the ndbx file format.
 */
public class NDBXWriter {

    public static void write(NodeLibrary library, File file) {
        StreamResult streamResult = new StreamResult(file);
        write(library, streamResult, file);

    }

    public static void write(NodeLibrary library, Writer writer) {
        StreamResult streamResult = new StreamResult(writer);
        write(library, streamResult, null);
    }

    public static void write(NodeLibrary library, StreamResult streamResult, File file) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();

            // Build the header.
            Element rootElement = doc.createElement("ndbx");
            rootElement.setAttribute("type", "file");
            rootElement.setAttribute("formatVersion", NodeLibrary.CURRENT_FORMAT_VERSION);
            rootElement.setAttribute("uuid", library.getUuid().toString());
            doc.appendChild(rootElement);

            // Write out all the document properties.
            Set<String> propertyNames = library.getPropertyNames();
            ArrayList<String> orderedNames = new ArrayList<String>(propertyNames);
            Collections.sort(orderedNames);
            for (String propertyName : orderedNames) {
                String propertyValue = library.getProperty(propertyName);
                Element e = doc.createElement("property");
                e.setAttribute("name", propertyName);
                e.setAttribute("value", propertyValue);
                rootElement.appendChild(e);
            }

            // Write the function repository.
            writeFunctionRepository(doc, rootElement, library.getFunctionRepository(), file);

            writeDevices(doc, rootElement, library.getDevices());

            // Write the root node.
            writeNode(doc, rootElement, library.getRoot(), library.getNodeRepository());

            // Convert the document to XML.
            DOMSource domSource = new DOMSource(doc);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
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
     * Write out links to the function repositories used.
     *
     * @param doc                the XML document
     * @param parent             the parent element
     * @param functionRepository the function repository to write
     * @param baseFile           the file to which the paths of the function libraries are relative to.
     */
    private static void writeFunctionRepository(Document doc, Element parent, FunctionRepository functionRepository, File baseFile) {
        for (FunctionLibrary library : functionRepository.getLibraries()) {
            // The core functions library is implicitly included.
            if (library == CoreFunctions.LIBRARY) continue;
            Element el = doc.createElement("link");
            el.setAttribute("rel", "functions");
            el.setAttribute("href", library.getLink(baseFile));
            parent.appendChild(el);
        }
    }

    /**
     * Write out external devices.
     *
     * @param doc     the XML document
     * @param parent  the parent element
     * @param devices the external devices to write
     */
    private static void writeDevices(Document doc, Element parent, List<Device> devices) {
        for (Device device : devices) {
            Element el = doc.createElement("device");
            el.setAttribute("name", device.getName());
            el.setAttribute("type", device.getType());
            for (Map.Entry<String, String> property : device.getProperties().entrySet()) {
                Element e = doc.createElement("property");
                e.setAttribute("name", property.getKey());
                e.setAttribute("value", property.getValue());
                el.appendChild(e);
            }
            parent.appendChild(el);
        }
    }

    /**
     * Find the libraryname.nodename of the given node.
     * Searches the list of default node repositories to find it.
     *
     * @param node           The node to find.
     * @param nodeRepository The list of node libraries to look for the node.
     * @return the node id, in the format libraryname.nodename.
     */
    private static String findNodeId(Node node, NodeRepository nodeRepository) {
        NodeLibrary library = nodeRepository.nodeLibraryForNode(node);
        if (library == null) {
            return node.getName();
        } else {
            return String.format("%s.%s", library.getName(), node.getName());
        }
    }

    /**
     * Write out the node.
     *
     * @param doc            the XML document
     * @param parent         the parent element
     * @param node           the node to write
     * @param nodeRepository the repository that contains the node prototype
     */
    private static void writeNode(Document doc, Element parent, Node node, NodeRepository nodeRepository) {
        Element el = doc.createElement("node");
        parent.appendChild(el);

        // Write prototype
        if (shouldWriteAttribute(node, Node.Attribute.PROTOTYPE)) {
            if (node.getPrototype() != Node.ROOT)
                el.setAttribute("prototype", findNodeId(node.getPrototype(), nodeRepository));
        }

        // Write name
        if (shouldWriteAttribute(node, Node.Attribute.NAME))
            el.setAttribute("name", node.getName());

        // Write comment
        if (shouldWriteAttribute(node, Node.Attribute.COMMENT))
            el.setAttribute("comment", node.getComment());

        // Write category
        if (shouldWriteAttribute(node, Node.Attribute.CATEGORY))
            el.setAttribute("category", node.getCategory());

        // Write description
        if (shouldWriteAttribute(node, Node.Attribute.DESCRIPTION))
            el.setAttribute("description", node.getDescription());

        // Write output type
        if (shouldWriteAttribute(node, Node.Attribute.OUTPUT_TYPE))
            el.setAttribute("outputType", node.getOutputType());

        // Write output range
        if (shouldWriteAttribute(node, Node.Attribute.OUTPUT_RANGE))
            el.setAttribute("outputRange", node.getOutputRange().toString().toLowerCase(Locale.US));

        // Write image
        if (shouldWriteAttribute(node, Node.Attribute.IMAGE))
            el.setAttribute("image", node.getImage());

        // Write function
        if (shouldWriteAttribute(node, Node.Attribute.FUNCTION))
            el.setAttribute("function", node.getFunction());

        // Write handle function
        if (shouldWriteAttribute(node, Node.Attribute.HANDLE))
            el.setAttribute("handle", node.getHandle());

        // Write position
        if (shouldWriteAttribute(node, Node.Attribute.POSITION)) {
            Point position = node.getPosition();
            el.setAttribute("position", String.valueOf(position));
        }

        // Write rendered child
        if (shouldWriteAttribute(node, Node.Attribute.RENDERED_CHILD_NAME))
            el.setAttribute("renderedChild", node.getRenderedChildName());

        // Add the children
        if (shouldWriteAttribute(node, Node.Attribute.CHILDREN)) {
            // Sort the children.
            ArrayList<Node> children = new ArrayList<Node>();
            children.addAll(node.getChildren());
            Collections.sort(children, new NodeNameComparator());
            // The order in which the nodes are written is important!
            // Since a library can potentially store an instance and its prototype, make sure that the prototype gets
            // stored sequentially before its instance.
            // The reader expects prototypes to be defined before their instances.
            while (!children.isEmpty()) {
                Node child = children.get(0);
                writeOrderedChild(doc, el, children, child, nodeRepository);
            }
        }

        // Add the input ports
        if (shouldWriteAttribute(node, Node.Attribute.INPUTS)) {
            for (Port port : node.getInputs()) {
                writePort(doc, el, node, port, Port.Direction.INPUT);
            }
        }

        // Add all child connections
        if (shouldWriteAttribute(node, Node.Attribute.CONNECTIONS)) {
            for (Connection conn : node.getConnections()) {
                writeConnection(doc, el, conn);
            }
        }
    }

    /**
     * Check if the given attribute should be written.
     * <p/>
     * The attribute should be written if  it's value is different from the prototype value.
     *
     * @param node      The node.
     * @param attribute The name of the attribute.
     * @return true if the attribute should be written.
     */
    private static boolean shouldWriteAttribute(Node node, Node.Attribute attribute) {
        checkArgument(node != Node.ROOT, "You cannot write out the ROOT node.");
        Object prototypeValue = node.getPrototype().getAttributeValue(attribute);
        Object nodeValue = node.getAttributeValue(attribute);
        if (attribute != Node.Attribute.PROTOTYPE) {
            checkNotNull(prototypeValue, "Attribute %s of node %s is empty.", attribute, node.getPrototype());
            checkNotNull(nodeValue, "Attribute %s of node %s is empty.", attribute, node);
            return !prototypeValue.equals(nodeValue);
        } else {
            return prototypeValue != nodeValue;
        }
    }

    /**
     * Write out the child. If the prototype of the child is also in this library, write that out first, recursively.
     *
     * @param doc            the XML document
     * @param parent         the parent element
     * @param children       a list of children that were written already.
     *                       When a child is written, we remove it from the list.
     * @param child          the child to write
     * @param nodeRepository the node repository that contains the node prototype
     */
    private static void writeOrderedChild(Document doc, Element parent, List<Node> children, Node child, NodeRepository nodeRepository) {
        Node prototype = child.getPrototype();
        if (children.contains(prototype))
            writeOrderedChild(doc, parent, children, prototype, nodeRepository);
        writeNode(doc, parent, child, nodeRepository);
        children.remove(child);
    }

    /**
     * Check if the given attribute should be written.
     * <p/>
     * The attribute should be written if  it's value is different from the prototype value.
     *
     * @param node      The node.
     * @param port      The port.
     * @param attribute The name of the attribute.
     * @return true if the attribute should be written.
     */
    private static boolean shouldWriteAttribute(Node node, Port port, Port.Attribute attribute) {
        checkArgument(node != Node.ROOT, "You cannot write out the ROOT node.");
        Port prototypePort = node.getPrototype().getInput(port.getName());
        // If there is no prototype port, we should always write the attribute.
        if (prototypePort == null) return true;
        Object prototypeValue = prototypePort.getAttributeValue(attribute);
        Object value = port.getAttributeValue(attribute);
        // Objects.equal does the correct null-comparison for min / max values.
        return !Objects.equal(prototypeValue, value);
    }

    private static void writePort(Document doc, Element parent, Node node, Port port, Port.Direction direction) {
        // We only write out the ports that have changed with regards to the prototype.
        Node protoNode = node.getPrototype();
        Port protoPort = null;
        if (protoNode != null)
            protoPort = protoNode.getInput(port.getName());
        // If the port and its prototype are equal, don't write anything.
        if (port.equals(protoPort)) return;
        Element el = doc.createElement("port");
        el.setAttribute("name", port.getName());
        el.setAttribute("type", port.getType());
        if (shouldWriteAttribute(node, port, Port.Attribute.LABEL))
            el.setAttribute("label", port.getLabel());
        if (shouldWriteAttribute(node, port, Port.Attribute.CHILD_REFERENCE) && port.getChildReference() != null)
            el.setAttribute("childReference", port.getChildReference());
        if (shouldWriteAttribute(node, port, Port.Attribute.WIDGET))
            el.setAttribute("widget", port.getWidget().toString().toLowerCase(Locale.US));
        if (shouldWriteAttribute(node, port, Port.Attribute.RANGE))
            el.setAttribute("range", port.getRange().toString().toLowerCase(Locale.US));
        if (port.isStandardType())
            el.setAttribute("value", port.stringValue());
        if (shouldWriteAttribute(node, port, Port.Attribute.DESCRIPTION))
            el.setAttribute("description", port.getDescription());
        if (shouldWriteAttribute(node, port, Port.Attribute.MINIMUM_VALUE))
            if (port.getMinimumValue() != null)
                el.setAttribute("min", String.format(Locale.US, "%s", port.getMinimumValue()));
        if (shouldWriteAttribute(node, port, Port.Attribute.MAXIMUM_VALUE))
            if (port.getMaximumValue() != null)
                el.setAttribute("max", String.format(Locale.US, "%s", port.getMaximumValue()));
        if (shouldWriteAttribute(node, port, Port.Attribute.MENU_ITEMS))
            writeMenuItems(doc, el, port.getMenuItems());
        parent.appendChild(el);
    }

    private static void writeMenuItems(Document doc, Element parent, List<MenuItem> menuItems) {
        for (MenuItem item : menuItems) {
            Element el = doc.createElement("menu");
            el.setAttribute("key", item.getKey());
            el.setAttribute("label", item.getLabel());
            parent.appendChild(el);
        }
    }

    private static void writeConnection(Document doc, Element parent, Connection conn) {
        Element connElement = doc.createElement("conn");
        connElement.setAttribute("output", String.format("%s", conn.getOutputNode()));
        connElement.setAttribute("input", String.format("%s.%s", conn.getInputNode(), conn.getInputPort()));
        parent.appendChild(connElement);
    }

    private static class NodeNameComparator implements Comparator<Node> {
        public int compare(Node node1, Node node2) {
            return node1.getName().compareTo(node2.getName());
        }
    }

}
