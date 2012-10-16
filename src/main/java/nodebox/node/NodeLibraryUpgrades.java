package nodebox.node;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import nodebox.graphics.Point;
import nodebox.util.LoadException;
import nu.xom.*;

import java.io.File;
import java.io.IOException;
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
    }

    private static final Pattern formatVersionPattern = Pattern.compile("formatVersion=['\"]([\\d\\.]+)['\"]");

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
        while (!currentVersion.equals(targetVersion) && tries < 100) {
            Method upgradeMethod = upgradeMap.get(currentVersion);
            if (upgradeMethod == null) {
                throw new LoadException(file, "Unsupported version " + currentVersion + ": this file is too old or too new.");
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
                if (!e.getLocalName().equals("node")) return;
                Attribute position = e.getAttribute("position");
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
        return transformXml(inputXml, "4", changePrototypeOp, renameOp);
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

    private static Set<String> getChildNodeNames(ParentNode parent) {
        HashSet<String> names = new HashSet<String>();
        Nodes children = parent.query("node");
        for (int i = 0; i < children.size(); i++) {
            nu.xom.Node childNode = children.get(i);
            if (childNode instanceof Element) {
                Element e = (Element) children.get(i);
                names.add(e.getAttribute("name").getValue());
            }
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
        Attribute renderedChildReference = element.getAttribute("renderedChild");
        if (renderedChildReference == null) return;
        String oldRenderedChild = renderedChildReference.getValue();
        if (oldRenderedChild.equals(oldNodeName))
            renderedChildReference.setValue(newNodeName);

    }

    private static void renamePortReference(Elements elements, String attributeName, String oldNodeName, String newNodeName) {
        for (int i = 0; i < elements.size(); i++) {
            Element c = elements.get(i);
            Attribute portReference = c.getAttribute(attributeName);
            if (portReference == null) continue;
            Iterator<String> portRefIterator = NodeLibrary.PORT_NAME_SPLITTER.split(portReference.getValue()).iterator();
            String nodeName = portRefIterator.next();
            String portName = portRefIterator.next();
            if (oldNodeName.equals(nodeName)) {
                portReference.setValue(String.format("%s.%s", newNodeName, portName));
            }
        }
    }

    private static void renamePortInElements(Elements elements, String attributeName, String nodeName, String oldPortName, String newPortName) {
        for (int i = 0; i < elements.size(); i++) {
            Element c = elements.get(i);
            Attribute portReference = c.getAttribute(attributeName);
            if (portReference == null) continue;
            Iterator<String> portRefIterator = NodeLibrary.PORT_NAME_SPLITTER.split(portReference.getValue()).iterator();
            String nodeRef = portRefIterator.next();
            String portRef = portRefIterator.next();
            if (nodeRef.equals(nodeName) && portRef.equals(oldPortName)) {
                portReference.setValue(String.format("%s.%s", nodeName, newPortName));
            }
        }
    }

    private static void renameNodeReference(Elements elements, String attributeName, String oldNodeName, String newNodeName) {
        for (int i = 0; i < elements.size(); i++) {
            Element c = elements.get(i);
            Attribute nodeRef = c.getAttribute(attributeName);
            String nodeName = nodeRef.getValue();
            if (oldNodeName.equals(nodeName)) {
                nodeRef.setValue(newNodeName);
            }
        }
    }

    private static void removeNodeInput(Element node, String input) {
        Elements ports = node.getChildElements("port");
        for (int i = 0; i < ports.size(); i++) {
            Element port = ports.get(i);
            Attribute nameAttr = port.getAttribute("name");
            String portName = nameAttr.getValue();
            if (portName.equals(input)) {
                node.removeChild(port);
            }
        }
        if (node.getAttribute("name") != null) {
            Element parent = (Element) node.getParent();
            String child = node.getAttribute("name").getValue();
            removeConnection(parent, child, input);
            String publishedInput = getParentPublishedInput(parent, child, input);
            if (publishedInput != null)
                removeNodeInput(parent, publishedInput);
        }

    }

    private static void removeConnection(Element parent, String child, String input) {
        Elements connections = parent.getChildElements("conn");
        for (int i = 0; i < connections.size(); i++) {
            Element conn = connections.get(i);
            Attribute inputAttr = conn.getAttribute("input");
            String inputPort = inputAttr.getValue();
            if (inputPort.equals(String.format("%s.%s", child, input))) {
                parent.removeChild(conn);
            }
        }
    }

    private static String getParentPublishedInput(Element parent, String child, String input) {
        Elements ports = parent.getChildElements("port");
        for (int i = 0; i < ports.size(); i++) {
            Element port = ports.get(i);
            Attribute childRef = port.getAttribute("childReference");
            if (childRef != null && childRef.getValue().equals(String.format("%s.%s", child, input))) {
                return port.getAttribute("name").getValue();
            }
        }
        return null;
    }

    private static boolean isNodeWithPrototype(Element e, String nodePrototype) {
        if (e.getLocalName().equals("node")) {
            Attribute prototype = e.getAttribute("prototype");
            if (prototype != null && prototype.getValue().equals(nodePrototype)) {
                return true;
            }
        }
        return false;
    }

    private static Element portWithName(Element nodeElement, String portName) {
        Elements ports = nodeElement.getChildElements("port");
        for (int i = 0; i < ports.size(); i++) {
            Element port = ports.get(i);
            Attribute name = port.getAttribute("name");
            if (name != null && name.getValue().equals(portName)) {
                return port;
            }
        }
        return null;
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
                Attribute prototype = e.getAttribute("prototype");
                prototype.setValue(newPrototype);
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
            if (e.getLocalName().equals("node")) {
                Attribute name = e.getAttribute("name");
                if (name != null && name.getValue().startsWith(oldPrefix)) {
                    String oldNodeName = name.getValue();
                    Set<String> childNames = getChildNodeNames(e.getParent());
                    String newNodeName = uniqueName(newPrefix, childNames);
                    name.setValue(newNodeName);

                    Element parent = (Element) e.getParent();
                    renameRenderedChildReference(parent, oldNodeName, newNodeName);
                    Elements connections = parent.getChildElements("conn");
                    renamePortReference(connections, "input", oldNodeName, newNodeName);
                    renameNodeReference(connections, "output", oldNodeName, newNodeName);

                    Elements ports = parent.getChildElements("port");
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
                String nodeName = e.getAttributeValue("name");
                Element port = portWithName(e, oldPortName);
                if (port != null) {
                    port.getAttribute("name").setValue(newPortName);
                    Element parent = (Element) e.getParent();
                    Elements connections = parent.getChildElements("conn");
                    renamePortInElements(connections, "input", nodeName, oldPortName, newPortName);
                    Elements ports = parent.getChildElements("port");
                    renamePortInElements(ports, "childReference", nodeName, oldPortName, newPortName);
                }
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
                    Attribute type = port.getAttribute("type");
                    type.setValue(newType);
                    Attribute value = port.getAttribute("value");
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
                Element port = new Element("port");
                port.addAttribute(new Attribute("name", this.name));
                port.addAttribute(new Attribute("type", this.type));
                port.addAttribute(new Attribute("value", this.value));
                e.insertChild(port, 0);
            }
        }
    }

    private static UpgradeStringResult transformXml(String xml, String newFormatVersion, UpgradeOp... ops) {
        try {
            Document document = new Builder().build(xml, null);

            // Check that this is a NodeBox document and set the new formatVersion.
            Element root = document.getRootElement();
            checkArgument(root.getLocalName().equals("ndbx"), "This is not a valid NodeBox document.");
            root.addAttribute(new Attribute("formatVersion", newFormatVersion));

            // Loop through all upgrade operations.
            ArrayList<String> warnings = new ArrayList<String>();
            for (UpgradeOp op : ops) {
                op.start(root);
                transformXmlRecursive(document.getRootElement(), op);
                op.end(root);
                warnings.addAll(op.getWarnings());
            }
            return new UpgradeStringResult(document.toXML(), warnings);
        } catch (Exception e) {
            throw new RuntimeException("Error while upgrading to " + newFormatVersion + ".", e);
        }
    }

    private static void transformXmlRecursive(Element e, UpgradeOp op) {
        op.apply(e);
        Elements children = e.getChildElements();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            transformXmlRecursive(child, op);
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
