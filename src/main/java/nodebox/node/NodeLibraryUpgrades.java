package nodebox.node;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import nodebox.graphics.Point;
import nodebox.util.LoadException;
import org.python.google.common.collect.ImmutableList;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper class that contains all NodeLibrary upgrade migrations.
 */
public class NodeLibraryUpgrades {

    private static final Pattern formatVersionPattern = Pattern.compile("formatVersion=['\"]([\\d\\.]+)['\"]");
    private static Map<String, Method> upgradeMap = new HashMap<String, Method>();

    /**
     * Lookup an upgrade method by name.
     * <p/>
     * This method is not compatible
     *
     * @param methodName The upgrade method name.
     * @return The Method object, to be invoked.
     */
    private static Method upgradeMethod(String methodName) {
        try {
            return NodeLibraryUpgrades.class.getMethod(methodName, String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        upgradeMap.put("1.0", upgradeMethod("upgrade1to2"));
        upgradeMap.put("2", upgradeMethod("upgrade2to3"));
        upgradeMap.put("3", upgradeMethod("upgrade3to4"));
        upgradeMap.put("4", upgradeMethod("upgrade4to5"));
        upgradeMap.put("5", upgradeMethod("upgrade5to6"));
        upgradeMap.put("6", upgradeMethod("upgrade6to7"));
        upgradeMap.put("7", upgradeMethod("upgrade7to8"));
        upgradeMap.put("8", upgradeMethod("upgrade8to9"));
        upgradeMap.put("9", upgradeMethod("upgrade9to10"));
        upgradeMap.put("10", upgradeMethod("upgrade10to11"));
        upgradeMap.put("11", upgradeMethod("upgrade11to12"));
        upgradeMap.put("12", upgradeMethod("upgrade12to13"));
        upgradeMap.put("13", upgradeMethod("upgrade13to14"));
        upgradeMap.put("14", upgradeMethod("upgrade14to15"));
        upgradeMap.put("15", upgradeMethod("upgrade15to16"));
        upgradeMap.put("16", upgradeMethod("upgrade16to17"));
        upgradeMap.put("17", upgradeMethod("upgrade17to18"));
        upgradeMap.put("18", upgradeMethod("upgrade18to19"));
        upgradeMap.put("19", upgradeMethod("upgrade19to20"));
        upgradeMap.put("20", upgradeMethod("upgrade20to21"));
    }

    public static String parseFormatVersion(String xml) {
        Matcher m = formatVersionPattern.matcher(xml);
        if (!m.find()) throw new RuntimeException("Invalid NodeBox file: " + xml);
        return m.group(1);
    }

    private static String readFile(File file) {
        try {
            return Files.toString(file, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Upgrade the given file to the latest library version. The file is supposed to be a NDBX file.
     * <p/>
     * It is harmless to pass in current version NDBX files.
     *
     * @param file The .ndbx file to upgrade.
     * @return An upgrade result, containing warnings, correct XML code and a NodeLibrary object.
     * @throws nodebox.util.LoadException If the upgrade fails for some reason.
     */
    public static UpgradeResult upgrade(File file) throws LoadException {
        return upgradeTo(file, NodeLibrary.CURRENT_FORMAT_VERSION);
    }

    /**
     * Upgrade the given file to the target version. The file is supposed to be a NDBX file.
     * <p/>
     * It is harmless to pass in current version NDBX files.
     *
     * @param file The .ndbx file to upgrade.
     * @return An upgrade result, containing warnings, correct XML code and a NodeLibrary object.
     * @throws LoadException If the upgrade fails for some reason.
     */
    public static UpgradeResult upgradeTo(File file, String targetVersion) throws LoadException {
        String currentXml = readFile(file);
        String currentVersion = parseFormatVersion(currentXml);
        ArrayList<String> warnings = new ArrayList<String>();
        // Avoid upgrades getting stuck in an infinite loop.
        int tries = 0;
        if (currentVersion.equals("0.9")) {
            throw new LoadException(file, "This is a NodeBox 2 file. Download NodeBox 2 from http://beta.nodebox.net/");
        }
        while (!currentVersion.equals(targetVersion) && tries < 100) {
            Method upgradeMethod = upgradeMap.get(currentVersion);
            if (upgradeMethod == null) {
                throw new LoadException(file, "Unsupported version " + currentVersion + ": this file is too new. Try downloading a new version of NodeBox from http://nodebox.net/download/");
            }
            try {
                UpgradeStringResult result = (UpgradeStringResult) upgradeMethod.invoke(null, currentXml);
                warnings.addAll(result.warnings);
                currentXml = result.xml;
            } catch (Exception e) {
                throw new LoadException(file, "Upgrading to " + currentVersion + " failed.", e);
            }
            currentVersion = parseFormatVersion(currentXml);
            tries++;
        }
        if (tries >= 100) {
            throw new LoadException(file, "Got stuck in an infinite loop when trying to upgrade from " + currentVersion);
        }
        return new UpgradeResult(file, currentXml, warnings);
    }

    public static UpgradeStringResult upgrade1to2(String inputXml) throws LoadException {
        // Version 2: Vertical node networks
        // 1. Rotate all nodes 90 degrees by reversing X and Y positions.
        // 2. Convert from pixel units to grid units by dividing by GRID_CELL_SIZE.
        final int GRID_CELL_SIZE = 48;
        UpgradeOp verticalNodesOp = new UpgradeOp() {
            @Override
            public void apply(Element e) {
                if (!e.getTagName().equals("node")) return;
                Attr position = e.getAttributeNode("position");
                if (position == null) return;
                Point pt = Point.valueOf(position.getValue());
                Point reversedPoint = new Point(pt.y, pt.x);
                Point gridPoint = new Point(Math.round(reversedPoint.x / GRID_CELL_SIZE) * 3, Math.round(reversedPoint.y / GRID_CELL_SIZE));
                position.setValue(String.valueOf(gridPoint));
            }

            @Override
            public void end(Element root) {
                addWarning("Nodes have been rotated. Your network will look different.");
            }
        };
        return transformXml(inputXml, "2", verticalNodesOp);
    }

    public static UpgradeStringResult upgrade2to3(String inputXml) throws LoadException {
        // Version 3: Rename math.to_integer to math.round.
        UpgradeOp changePrototypeOp = new ChangePrototypeOp("math.to_integer", "math.round");
        UpgradeOp renameOp = new RenameNodeOp("to_integer", "round");
        return transformXml(inputXml, "3", changePrototypeOp, renameOp);
    }

    public static UpgradeStringResult upgrade3to4(String inputXml) throws LoadException {
        // Version 4: Convert corevector.to_points nodes to corevector.point nodes.
        UpgradeOp changePrototypeOp = new ChangePrototypeOp("corevector.to_points", "corevector.point");
        UpgradeOp renameOp = new RenameNodeOp("to_points", "point");
        // todo: write test code for renamePortOp addition.
        UpgradeOp renamePortOp = new RenamePortOp("corevector.to_points", "shape", "value");
        return transformXml(inputXml, "4", renamePortOp, changePrototypeOp, renameOp);
    }

    public static UpgradeStringResult upgrade4to5(String inputXml) throws LoadException {
        // Version 5: The corevector.textpath node loses the height port.
        UpgradeOp removeInputOp = new RemoveInputOp("corevector.textpath", "height");
        return transformXml(inputXml, "5", removeInputOp);
    }

    public static UpgradeStringResult upgrade5to6(String inputXml) throws LoadException {
        // Version 6: Change delete.delete_selected boolean to menu options.
        Map<String, String> mappings = ImmutableMap.of("true", "selected", "false", "non-selected");
        UpgradeOp renamePortOp = new RenamePortOp("corevector.delete", "delete_selected", "operation");
        UpgradeOp changePortTypeOp = new ChangePortTypeOp("corevector.delete", "operation", "string", mappings);
        return transformXml(inputXml, "6", renamePortOp, changePortTypeOp);
    }

    public static UpgradeStringResult upgrade6to7(String inputXml) throws LoadException {
        // Version 7: Replace instances of list.filter with list.cull.
        UpgradeOp changePrototypeOp = new ChangePrototypeOp("list.filter", "list.cull");
        UpgradeOp renameOp = new RenameNodeOp("filter", "cull");
        return transformXml(inputXml, "7", changePrototypeOp, renameOp);
    }

    public static UpgradeStringResult upgrade7to8(String inputXml) throws LoadException {
        // Version 8: The corevector.point_on_path node loses the range port.
        UpgradeOp removeInputOp = new RemoveInputOp("corevector.point_on_path", "range");
        return transformXml(inputXml, "8", removeInputOp);
    }

    public static UpgradeStringResult upgrade8to9(String inputXml) throws LoadException {
        // Version 9: corevector's resample_by_amount and resample_by_length nodes
        // are replaced by the more generic resample node.
        UpgradeOp addInputOp1 = new AddInputOp("corevector.resample_by_amount", "method", "string", "amount");
        UpgradeOp changePrototypeOp1 = new ChangePrototypeOp("corevector.resample_by_amount", "corevector.resample");
        UpgradeOp renameOp1 = new RenameNodeOp("resample_by_amount", "resample");

        UpgradeOp addInputOp2 = new AddInputOp("corevector.resample_by_length", "method", "string", "length");
        UpgradeOp changePrototypeOp2 = new ChangePrototypeOp("corevector.resample_by_length", "corevector.resample");
        UpgradeOp renameOp2 = new RenameNodeOp("resample_by_length", "resample");

        return transformXml(inputXml, "9",
                addInputOp1, changePrototypeOp1, renameOp1,
                addInputOp2, changePrototypeOp2, renameOp2);
    }

    public static UpgradeStringResult upgrade9to10(String inputXml) throws LoadException {
        // Version 10: corevector's wiggle_contours, wiggle_paths and wiggle_points nodes
        // are replaced by the more generic wiggle node.
        UpgradeOp addInputOp1 = new AddInputOp("corevector.wiggle_contours", "scope", "string", "contours");
        UpgradeOp changePrototypeOp1 = new ChangePrototypeOp("corevector.wiggle_contours", "corevector.wiggle");
        UpgradeOp renameOp1 = new RenameNodeOp("wiggle_contours", "wiggle");

        UpgradeOp addInputOp2 = new AddInputOp("corevector.wiggle_paths", "scope", "string", "paths");
        UpgradeOp changePrototypeOp2 = new ChangePrototypeOp("corevector.wiggle_paths", "corevector.wiggle");
        UpgradeOp renameOp2 = new RenameNodeOp("wiggle_paths", "wiggle");

        UpgradeOp addInputOp3 = new AddInputOp("corevector.wiggle_points", "scope", "string", "points");
        UpgradeOp changePrototypeOp3 = new ChangePrototypeOp("corevector.wiggle_points", "corevector.wiggle");
        UpgradeOp renameOp3 = new RenameNodeOp("wiggle_points", "wiggle");

        return transformXml(inputXml, "10",
                addInputOp1, changePrototypeOp1, renameOp1,
                addInputOp2, changePrototypeOp2, renameOp2,
                addInputOp3, changePrototypeOp3, renameOp3);
    }

    public static UpgradeStringResult upgrade10to11(String inputXml) throws LoadException {
        UpgradeOp removeNodeOp = new RemoveNodeOp("corevector.draw_path");
        return transformXml(inputXml, "11", removeNodeOp);
    }

    public static UpgradeStringResult upgrade11to12(String inputXml) throws LoadException {
        UpgradeOp renamePortOp1 = new RenamePortOp("corevector.shape_on_path", "template", "path");
        UpgradeOp renamePortOp2 = new RenamePortOp("corevector.shape_on_path", "dist", "spacing");
        UpgradeOp renamePortOp3 = new RenamePortOp("corevector.shape_on_path", "start", "margin");
        return transformXml(inputXml, "12", renamePortOp1, renamePortOp2, renamePortOp3);
    }

    public static UpgradeStringResult upgrade12to13(String inputXml) throws LoadException {
        UpgradeOp renamePortOp1 = new RenamePortOp("corevector.text_on_path", "shape", "path");
        UpgradeOp renamePortOp2 = new RenamePortOp("corevector.text_on_path", "position", "margin");
        UpgradeOp renamePortOp3 = new RenamePortOp("corevector.text_on_path", "offset", "baseline_offset");
        UpgradeOp removeInputOp = new RemoveInputOp("corevector.text_on_path", "keep_geometry");
        return transformXml(inputXml, "13", renamePortOp1, renamePortOp2, renamePortOp3, removeInputOp);
    }

    public static UpgradeStringResult upgrade13to14(String inputXml) throws LoadException {
        UpgradeOp renamePortOp1 = new RenamePortOp("math.wave", "speed", "period");
        UpgradeOp renamePortOp2 = new RenamePortOp("math.wave", "frame", "offset");
        return transformXml(inputXml, "14", renamePortOp1, renamePortOp2);
    }

    public static UpgradeStringResult upgrade14to15(String inputXml) throws LoadException {
        UpgradeOp renameNodeOp = new RenameNodeOp("make_strings", "split");
        return transformXml(inputXml, "15", renameNodeOp);
    }

    public static UpgradeStringResult upgrade15to16(String inputXml) throws LoadException {
        // Version 16: 'network' and 'node' are reserved names. Nodes with those names have to be renamed.
        // Besides this, only the top level node in any network is allowed to have the name 'root'.
        UpgradeOp renameNodeOp1 = new ExactRenameNodeOp("network", "network");
        UpgradeOp renameNodeOp2 = new ExactRenameNodeOp("node", "node");
        ExactRenameNodeOp renameNodeOp3 = new ExactRenameNodeOp("root", "node");
        renameNodeOp3.skipRootNode();
        UpgradeOp addAttributeOp = new AddAttributeOp("corevector.geonet", "outputType", "geometry");
        UpgradeOp changePrototypeOp = new ChangePrototypeOp("corevector.geonet", "core.network");
        return transformXml(inputXml, "16", renameNodeOp1, renameNodeOp2, renameNodeOp3, addAttributeOp, changePrototypeOp);
    }

    public static UpgradeStringResult upgrade16to17(String inputXml) throws LoadException {
        UpgradeOp convertOSCPropertyOp = new ConvertOSCPropertyFormatOp();
        return transformXml(inputXml, "17", convertOSCPropertyOp);
    }

    public static UpgradeStringResult upgrade17to18(String inputXml) throws LoadException {
        // Version 18: "switch" and "combine" nodes have more ports. This doesn't change anything in the file
        // but does make the files backward-incompatible.
        UpgradeOp convertOSCPropertyOp = new ConvertOSCPropertyFormatOp();
        return transformXml(inputXml, "18");
    }

    public static UpgradeStringResult upgrade18to19(String inputXml) throws LoadException {
        // Version 19: audioplayer devices previously had their default device name set to "audioplayer1".
        // This has changed to "audio1", so to have backward compatibility we have to make sure the
        // old name is set explicitly and not derived from the prototype.

        UpgradeOp renameDeviceNameOp1 = new SetOldDefaultAudioDeviceNameOp("device.audio_analysis", "device_name", "audioplayer1");
        UpgradeOp renameDeviceNameOp2 = new SetOldDefaultAudioDeviceNameOp("device.audio_wave", "device_name", "audioplayer1");
        UpgradeOp renameDeviceNameOp3 = new SetOldDefaultAudioDeviceNameOp("device.beat_detect", "device_name", "audioplayer1");
        return transformXml(inputXml, "19", renameDeviceNameOp1, renameDeviceNameOp2, renameDeviceNameOp3);
    }

    public static UpgradeStringResult upgrade19to20(String inputXml) throws LoadException {
        UpgradeOp renameDevicePropertyOp1 = new ConvertDevicePropertyNameOp("osc", "autostart", "sync_with_timeline");
        UpgradeOp renameDevicePropertyOp2 = new ConvertDevicePropertyNameOp("audioplayer", "autostart", "sync_with_timeline");
        UpgradeOp renameDevicePropertyOp3 = new ConvertDevicePropertyNameOp("audioinput", "autostart", "sync_with_timeline");
        return transformXml(inputXml, "20", renameDevicePropertyOp1, renameDevicePropertyOp2, renameDevicePropertyOp3);
    }

    public static UpgradeStringResult upgrade20to21(String inputXml) throws LoadException {
        // Version 21: Use percentages for the scale parameter in the copy node, like in the scale node.
        UpgradeOp copyScaleValueOp = new UpgradeOp() {
            @Override
            public void apply(Element e) {
                if (!e.getTagName().equals("node")) return;
                if (isNodeWithPrototype(e, "corevector.copy")) {
                    Element scalePort = portWithName(e, "scale");
                    if (scalePort != null) {
                        Attr scaleValue = scalePort.getAttributeNode("value");
                        if (scaleValue == null) { return; }
                        Point pt = Point.valueOf(scaleValue.getValue());
                        pt = new Point((pt.x + 1) * 100, (pt.y + 1) * 100);
                        scaleValue.setValue(String.valueOf(pt));
                    }
                }
            }
        };
        return transformXml(inputXml, "21", copyScaleValueOp);
    }

    private static List<Node> childNodes(Node parent) {
        ArrayList<Node> childNodes = new ArrayList<Node>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            childNodes.add(children.item(i));
        }
        return childNodes;
    }

    private static List<Element> childElements(Node parent) {
        ArrayList<Element> childElements = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                childElements.add((Element) node);
            }
        }
        return childElements;
    }

    /**
     * Return direct descendant elements of parent node with the given childName.
     */
    private static List<Element> childElementsWithName(Node parent, String childName) {
        ArrayList<Element> childElements = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && ((Element) node).getTagName().equals(childName)) {
                childElements.add((Element) node);
            }
        }
        return childElements;
    }

    private static Set<String> getChildNodeNames(Element parent) {
        HashSet<String> names = new HashSet<String>();
        for (Element e : childElementsWithName(parent, "node")) {
            names.add(e.getAttribute("name"));
        }
        return names;
    }

    private static String uniqueName(String prefix, Set<String> existingNames) {
        int counter = 1;
        while (true) {
            String suggestedName = prefix + counter;
            if (!existingNames.contains(suggestedName)) {
                return suggestedName;
            }
            counter++;
        }
    }

    private static void renameRenderedChildReference(Element element, String oldNodeName, String newNodeName) {
        Attr renderedChildReference = element.getAttributeNode("renderedChild");
        if (renderedChildReference == null) return;
        String oldRenderedChild = renderedChildReference.getValue();
        if (oldRenderedChild.equals(oldNodeName)) {
            if (newNodeName == null || newNodeName.length() == 0)
                element.removeAttributeNode(renderedChildReference);
            else
                renderedChildReference.setValue(newNodeName);
        }
    }

    private static void renamePortReference(List<Element> elements, String attributeName, String oldNodeName, String newNodeName) {
        for (Element c : elements) {
            Attr portReference = c.getAttributeNode(attributeName);
            if (portReference == null) continue;
            Iterator<String> portRefIterator = NodeLibrary.PORT_NAME_SPLITTER.split(portReference.getValue()).iterator();
            String nodeName = portRefIterator.next();
            String portName = portRefIterator.next();
            if (oldNodeName.equals(nodeName)) {
                portReference.setValue(String.format("%s.%s", newNodeName, portName));
            }
        }
    }

    private static void renamePortInNodeList(List<Element> elements, String attributeName, String nodeName, String oldPortName, String newPortName) {
        for (Element c : elements) {
            Attr portReference = c.getAttributeNode(attributeName);
            if (portReference == null) continue;
            Iterator<String> portRefIterator = NodeLibrary.PORT_NAME_SPLITTER.split(portReference.getValue()).iterator();
            String nodeRef = portRefIterator.next();
            String portRef = portRefIterator.next();
            if (nodeRef.equals(nodeName) && portRef.equals(oldPortName)) {
                portReference.setValue(String.format("%s.%s", nodeName, newPortName));
            }
        }
    }

    private static void renameNodeReference(List<Element> elements, String attributeName, String oldNodeName, String newNodeName) {
        for (Element c : elements) {
            Attr nodeRef = c.getAttributeNode(attributeName);
            String nodeName = nodeRef.getValue();
            if (oldNodeName.equals(nodeName)) {
                nodeRef.setValue(newNodeName);
            }
        }
    }

    private static void removeNodeInput(Element node, String input) {
        for (Element port : childElementsWithName(node, "port")) {
            Attr nameAttr = port.getAttributeNode("name");
            String portName = nameAttr.getValue();
            if (portName.equals(input)) {
                node.removeChild(port);
            }
        }
        if (node.getAttributeNode("name") != null) {
            Element parent = (Element) node.getParentNode();
            String child = node.getAttributeNode("name").getValue();
            removeConnection(parent, child, input);
            String publishedInput = getParentPublishedInput(parent, child, input);
            if (publishedInput != null)
                removeNodeInput(parent, publishedInput);
        }

    }

    private static void removeConnection(Element parent, String child, String input) {
        for (Element conn : childElementsWithName(parent, "conn")) {
            String inputPort = conn.getAttribute("input");
            if (inputPort.equals(String.format("%s.%s", child, input))) {
                parent.removeChild(conn);
            }
        }
    }

    private static void removeConnections(Element parent, String child) {
        for (Element conn : childElementsWithName(parent, "conn")) {
            String inputPort = conn.getAttribute("input");
            String inputNode = inputPort.split("\\.")[0];

            String outputNode = conn.getAttribute("output");

            if (inputNode.equals(child) || outputNode.equals(child))
                parent.removeChild(conn);
        }
    }

    private static String getParentPublishedInput(Element parent, String child, String input) {
        for (Element port : childElementsWithName(parent, "port")) {
            Attr childRef = port.getAttributeNode("childReference");
            if (childRef != null && childRef.getValue().equals(String.format("%s.%s", child, input))) {
                return port.getAttribute("name");
            }
        }
        return null;
    }

    private static List<String> getParentPublishedInputs(Element parent, String child) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (Element port : childElementsWithName(parent, "port")) {
            Attr childRef = port.getAttributeNode("childReference");
            if (childRef != null && childRef.getValue().split("\\.")[0].equals(child)) {
                builder.add(port.getAttribute("name"));
            }
        }
        return builder.build();
    }

    private static boolean isNodeWithPrototype(Element e, String nodePrototype) {
        if (e.getTagName().equals("node")) {
            Attr prototype = e.getAttributeNode("prototype");
            if (prototype != null && prototype.getValue().equals(nodePrototype)) {
                return true;
            }
        }
        return false;
    }

    private static Element portWithName(Element nodeElement, String portName) {
        for (Element port : childElementsWithName(nodeElement, "port")) {
            if (port.getAttribute("name").equals(portName)) {
                return port;
            }
        }
        return null;
    }

    private static UpgradeStringResult transformXml(String xml, String newFormatVersion, UpgradeOp... ops) {
        try {

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));


            // Check that this is a NodeBox document and set the new formatVersion.
            Element root = document.getDocumentElement();
            checkArgument(root.getTagName().equals("ndbx"), "This is not a valid NodeBox document.");
            root.setAttribute("formatVersion", newFormatVersion);

            // Loop through all upgrade operations.
            ArrayList<String> warnings = new ArrayList<String>();
            for (UpgradeOp op : ops) {
                op.start(root);
                transformXmlRecursive(document.getDocumentElement(), op);
                op.end(root);
                warnings.addAll(op.getWarnings());
            }


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            transformer.transform(source, result);
            return new UpgradeStringResult(sw.toString(), warnings);
        } catch (Exception e) {
            throw new RuntimeException("Error while upgrading to " + newFormatVersion + ".", e);
        }
    }

    private static void transformXmlRecursive(Element e, UpgradeOp op) {
        op.apply(e);
        for (Element child : childElements(e)) {
            transformXmlRecursive(child, op);
        }
    }

    private static abstract class UpgradeOp {
        private List<String> warnings = new ArrayList<String>();

        public void start(Element root) {
        }

        public void end(Element root) {
        }

        public abstract void apply(Element e);

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    private static class ChangePrototypeOp extends UpgradeOp {
        private String oldPrototype;
        private String newPrototype;

        private ChangePrototypeOp(String oldPrototype, String newPrototype) {
            this.oldPrototype = oldPrototype;
            this.newPrototype = newPrototype;
        }

        public void apply(Element e) {
            if (isNodeWithPrototype(e, oldPrototype)) {
                Attr prototype = e.getAttributeNode("prototype");
                prototype.setValue(newPrototype);
            }
        }
    }

    private static class SetOldDefaultAudioDeviceNameOp extends UpgradeOp {
        private String prototype;
        private String portName;
        private String deviceName;

        private SetOldDefaultAudioDeviceNameOp(String prototype, String portName, String deviceName) {
            this.prototype = prototype;
            this.portName = portName;
            this.deviceName = deviceName;
        }

        public void apply(Element e) {
            if (isNodeWithPrototype(e, prototype)) {
                Element port = portWithName(e, portName);
                if (port == null) {
                    port = e.getOwnerDocument().createElement("port");
                    port.setAttribute("name", portName);
                    port.setAttribute("type", "string");
                    port.setAttribute("value", deviceName);
                    e.appendChild(port);
                }
            }
        }
    }

    private static class AddAttributeOp extends UpgradeOp {
        private String prototype;
        private String attributeName;
        private String attributeValue;

        private AddAttributeOp(String prototype, String attributeName, String attributeValue) {
            this.prototype = prototype;
            this.attributeName = attributeName;
            this.attributeValue = attributeValue;
        }

        public void apply(Element e) {
            if (isNodeWithPrototype(e, prototype)) {
                e.setAttribute(attributeName, attributeValue);
            }
        }
    }

    private static class ConvertOSCPropertyFormatOp extends UpgradeOp {
        @Override
        public void apply(Element e) {
            if (e.getTagName().equals("property")) {
                Element parent = (Element) e.getParentNode();
                if (parent != null && parent.getTagName().equals("ndbx")) {
                    Attr name = e.getAttributeNode("name");
                    Attr value = e.getAttributeNode("value");
                    if (name != null && name.getValue().equals("oscPort")) {
                        if (value != null) {
                            Element device = e.getOwnerDocument().createElement("device");
                            device.setAttribute("name", "osc1");
                            device.setAttribute("type", "osc");
                            Element portProperty = e.getOwnerDocument().createElement("property");
                            portProperty.setAttribute("name", "port");
                            portProperty.setAttribute("value", value.getValue());
                            device.appendChild(portProperty);
                            Element autostartProperty = e.getOwnerDocument().createElement("property");
                            autostartProperty.setAttribute("name", "autostart");
                            autostartProperty.setAttribute("value", "true");
                            device.appendChild(autostartProperty);
                            parent.replaceChild(device, e);
                        } else {
                            parent.removeChild(e);
                        }
                    }
                }
            }
        }
    }

    private static class ConvertDevicePropertyNameOp extends UpgradeOp {
        private String deviceType;
        private String oldPropertyName;
        private String newPropertyName;

        private ConvertDevicePropertyNameOp(String deviceType, String oldPropertyName, String newPropertyName) {
            this.deviceType = deviceType;
            this.oldPropertyName = oldPropertyName;
            this.newPropertyName = newPropertyName;
        }

        @Override
        public void apply(Element e) {
            if (e.getTagName().equals("property")) {
                Element parent = (Element) e.getParentNode();
                if (parent != null && parent.getTagName().equals("device")) {
                    Attr type = parent.getAttributeNode("type");
                    if (type != null && type.getValue().equals(this.deviceType)) {
                        Attr name = e.getAttributeNode("name");
                        if (name != null && name.getValue().equals(oldPropertyName))
                            name.setValue(newPropertyName);
                    }
                }
            }
        }
    }

    private static class RenameNodeOp extends UpgradeOp {
        private String oldPrefix;
        private String newPrefix;

        private RenameNodeOp(String oldPrefix, String newPrefix) {
            this.oldPrefix = oldPrefix;
            this.newPrefix = newPrefix;
        }

        @Override
        public void apply(Element e) {
            if (e.getTagName().equals("node")) {
                Attr name = e.getAttributeNode("name");
                if (name != null && name.getValue().startsWith(oldPrefix)) {
                    String oldNodeName = name.getValue();
                    Set<String> childNames = getChildNodeNames((Element) e.getParentNode());
                    String newNodeName = uniqueName(newPrefix, childNames);
                    name.setValue(newNodeName);

                    Element parent = (Element) e.getParentNode();
                    renameRenderedChildReference(parent, oldNodeName, newNodeName);
                    List<Element> connections = childElementsWithName(parent, "conn");
                    renamePortReference(connections, "input", oldNodeName, newNodeName);
                    renameNodeReference(connections, "output", oldNodeName, newNodeName);

                    List<Element> ports = childElementsWithName(parent, "port");
                    renamePortReference(ports, "childReference", oldNodeName, newNodeName);
                }
            }
        }
    }

    private static class ExactRenameNodeOp extends UpgradeOp {
        private String oldNodeName;
        private String newPrefix;
        private boolean shouldSkipRoot = false;

        private ExactRenameNodeOp(String oldNodeName, String newPrefix) {
            this.oldNodeName = oldNodeName;
            this.newPrefix = newPrefix;
        }

        private void skipRootNode() {
            this.shouldSkipRoot = true;
        }

        @Override
        public void apply(Element e) {
            if (e.getTagName().equals("node")) {
                if (shouldSkipRoot) {
                    Element parent = (Element) e.getParentNode();
                    if (parent != null && !parent.getTagName().equals("node"))
                        return;
                }
                Attr name = e.getAttributeNode("name");
                if (name != null && name.getValue().equals(oldNodeName)) {
                    Set<String> childNames = getChildNodeNames((Element) e.getParentNode());
                    String newNodeName = uniqueName(newPrefix, childNames);
                    name.setValue(newNodeName);

                    Element parent = (Element) e.getParentNode();
                    renameRenderedChildReference(parent, oldNodeName, newNodeName);
                    List<Element> connections = childElementsWithName(parent, "conn");
                    renamePortReference(connections, "input", oldNodeName, newNodeName);
                    renameNodeReference(connections, "output", oldNodeName, newNodeName);

                    List<Element> ports = childElementsWithName(parent, "port");
                    renamePortReference(ports, "childReference", oldNodeName, newNodeName);
                }
            }
        }
    }

    private static class RemoveInputOp extends UpgradeOp {
        private String nodePrototype;
        private String inputToRemove;

        private RemoveInputOp(String nodePrototype, String inputToRemove) {
            this.nodePrototype = nodePrototype;
            this.inputToRemove = inputToRemove;
        }

        @Override
        public void apply(Element e) {
            if (isNodeWithPrototype(e, nodePrototype)) {
                removeNodeInput(e, inputToRemove);
            }
        }
    }

    private static class RenamePortOp extends UpgradeOp {
        private String nodePrototype;
        private String oldPortName;
        private String newPortName;

        private RenamePortOp(String nodePrototype, String oldPortName, String newPortName) {
            this.nodePrototype = nodePrototype;
            this.oldPortName = oldPortName;
            this.newPortName = newPortName;
        }

        @Override
        public void apply(Element e) {
            if (isNodeWithPrototype(e, nodePrototype)) {
                String nodeName = e.getAttribute("name");
                Element port = portWithName(e, oldPortName);
                if (port != null)
                    port.setAttribute("name", newPortName);
                Element parent = (Element) e.getParentNode();
                List<Element> connections = childElementsWithName(parent, "conn");
                renamePortInNodeList(connections, "input", nodeName, oldPortName, newPortName);
                List<Element> ports = childElementsWithName(parent, "port");
                renamePortInNodeList(ports, "childReference", nodeName, oldPortName, newPortName);
            }
        }
    }

    private static class ChangePortTypeOp extends UpgradeOp {
        private String nodePrototype;
        private String portName;
        private String newType;
        // The value mappings are strings, since that's what's stored in the XML file.
        private Map<String, String> valueMappings;


        public ChangePortTypeOp(String nodePrototype, String portName, String newType, Map<String, String> valueMappings) {
            this.nodePrototype = nodePrototype;
            this.portName = portName;
            this.newType = newType;
            this.valueMappings = valueMappings;
        }

        @Override
        public void apply(Element e) {
            if (isNodeWithPrototype(e, nodePrototype)) {
                Element port = portWithName(e, portName);
                if (port != null) {
                    Attr type = port.getAttributeNode("type");
                    type.setValue(newType);
                    Attr value = port.getAttributeNode("value");
                    if (value != null) {
                        String newValue = valueMappings.get(value.getValue());
                        checkState(newValue != null,
                                "Change port type (%s.%s -> %s): value %s not found in value mappings.",
                                nodePrototype, portName, newType, value.getValue());
                        value.setValue(newValue);
                    }
                }
            }
        }
    }

    private static class AddInputOp extends UpgradeOp {
        private String nodePrototype;
        private String name;
        private String type;
        private String value;

        private AddInputOp(String nodePrototype, String name, String type, String value) {
            this.nodePrototype = nodePrototype;
            this.name = name;
            this.type = type;
            this.value = value;
        }

        @Override
        public void apply(Element e) {
            if (isNodeWithPrototype(e, nodePrototype)) {
                Element port = e.getOwnerDocument().createElement("port");
                port.setAttribute("name", this.name);
                port.setAttribute("type", this.type);
                port.setAttribute("value", this.value);
                e.appendChild(port);
            }
        }
    }

    private static class RemoveNodeOp extends UpgradeOp {
        private String nodePrototype;
        private List<String> removedNodes;

        private RemoveNodeOp(String nodePrototype) {
            this.nodePrototype = nodePrototype;
            removedNodes = new ArrayList<String>();
        }

        @Override
        public void apply(Element e) {
            if (isNodeWithPrototype(e, nodePrototype)) {
                Element parent = (Element) e.getParentNode();
                String child = e.getAttributeNode("name").getValue();
                removedNodes.add(child);

                List<String> publishedInputs = getParentPublishedInputs(parent, child);
                for (String publishedInput : publishedInputs)
                    removeNodeInput(parent, publishedInput);


                removeConnections(parent, child);
                renameRenderedChildReference(parent, child, null);
                e.getParentNode().removeChild(e);
            }
        }

        @Override
        public void end(Element root) {
            if (removedNodes.size() > 0)
                addWarning(String.format("The '%s' node became obsolete, the following nodes in your network got removed: %s", nodePrototype, removedNodes));
        }
    }

    private static class UpgradeStringResult {
        private final String xml;
        private final List<String> warnings;

        private UpgradeStringResult(String xml, List<String> warnings) {
            this.xml = xml;
            this.warnings = warnings;
        }
    }

}
