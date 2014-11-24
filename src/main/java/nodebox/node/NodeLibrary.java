package nodebox.node;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.function.FunctionLibrary;
import nodebox.function.FunctionRepository;
import nodebox.graphics.Point;
import nodebox.graphics.Rect;
import nodebox.util.FileUtils;
import nodebox.util.LoadException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

public class NodeLibrary {

    public static final File systemLibrariesDir;

    static {
        final File defaultDir = new File("libraries");
        if (defaultDir.isDirectory()) {
            systemLibrariesDir = defaultDir;
        } else {
            final URL url = NodeLibrary.class.getProtectionDomain().getCodeSource().getLocation();
            final File jarFile;
            try {
                jarFile = new File(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            systemLibrariesDir = new File(jarFile.getParentFile(), "libraries");
        }
    }

    private static final Pattern NUMBER_AT_THE_END = Pattern.compile("^(.*?)(\\d*)$");

    public static final String CURRENT_FORMAT_VERSION = "20";

    public static final Splitter PORT_NAME_SPLITTER = Splitter.on(".");

    public static final NodeLibrary coreLibrary = NodeLibrary.loadSystemLibrary("core");

    public static NodeLibrary create(String libraryName, Node root) {
        return create(libraryName, root, NodeRepository.of(), FunctionRepository.of(), UUID.randomUUID());
    }

    public static NodeLibrary create(String libraryName, Node root, FunctionRepository functionRepository) {
        return create(libraryName, root, NodeRepository.of(), functionRepository);
    }

    public static NodeLibrary create(String libraryName, Node root, NodeRepository nodeRepository, FunctionRepository functionRepository) {
        return create(libraryName, root, nodeRepository, functionRepository, UUID.randomUUID());
    }

    private static NodeLibrary create(String libraryName, Node root, NodeRepository nodeRepository, FunctionRepository functionRepository, UUID uuid) {
        return new NodeLibrary(libraryName, null, root, nodeRepository, functionRepository, ImmutableMap.<String, String>of(), ImmutableList.<Device>of(), uuid);
    }

    public static NodeLibrary loadSystemLibrary(String libraryName) throws LoadException {
        File libraryFile = new File(systemLibrariesDir, String.format("%s/%s.ndbx", libraryName, libraryName));
        NodeRepository repository = libraryName.equals("core") ? NodeRepository.empty() : NodeRepository.of();
        return load(libraryFile, repository);
    }

    public static NodeLibrary load(String libraryName, String xml, NodeRepository nodeRepository) throws LoadException {
        checkNotNull(libraryName, "Library name cannot be null.");
        checkNotNull(xml, "XML string cannot be null.");
        try {
            return load(libraryName, null, new StringReader(xml), nodeRepository);
        } catch (XMLStreamException e) {
            throw new LoadException(null, "Could not read NDBX string", e);
        }
    }

    public static NodeLibrary load(String libraryName, String xml, File baseFile, NodeRepository nodeRepository) throws LoadException {
        checkNotNull(libraryName, "Library name cannot be null.");
        checkNotNull(xml, "XML string cannot be null.");
        try {
            return load(libraryName, baseFile, new StringReader(xml), nodeRepository);
        } catch (XMLStreamException e) {
            throw new LoadException(null, "Could not read NDBX string", e);
        }
    }

    public static NodeLibrary load(File f, NodeRepository nodeRepository) throws LoadException {
        checkNotNull(f, "File cannot be null.");
        String libraryName = FileUtils.stripExtension(f);
        try {
            return load(libraryName, f, createFileReader(f), nodeRepository);
        } catch (FileNotFoundException e) {
            throw new LoadException(f, "File not found.");
        } catch (XMLStreamException e) {
            throw new LoadException(f, "Could not read NDBX file", e);
        }
    }

    public static Map<String, String> parseHeader(File f) {
        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(createFileReader(f));
            while (reader.hasNext()) {
                int eventType = reader.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    String tagName = reader.getLocalName();
                    if (tagName.equals("ndbx")) {
                        return parseHeader(reader);
                    } else {
                        throw new XMLStreamException("Only tag ndbx allowed, not " + tagName, reader.getLocation());
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }

    private static Map<String, String> parseHeader(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> propertyMap = new HashMap<String, String>();
        while (true) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("property")) {
                    parseProperty(reader, propertyMap);
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("ndbx"))
                    break;
            }
        }
        return propertyMap;
    }

    /**
     * Create a file reader using the UTF-8 encoding.
     * <p/>
     * Unfortunately, Java's FileReader constructor does not accept an encoding, which is an oversight in the API.
     * Instead, it opts to create a reader with the default platform encoding. This means it differs between platforms,
     * and even inside and outside of the IDE.
     * <p/>
     * This method removes the ambiguity and always reads files in UTF-8.
     *
     * @param file the file to read.
     * @return A Reader.
     */
    private static Reader createFileReader(File file) throws FileNotFoundException {
        try {
            return new InputStreamReader(new FileInputStream(file), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private final String name;
    private final File file;
    private final Node root;
    private final NodeRepository nodeRepository;
    private final FunctionRepository functionRepository;
    private final ImmutableMap<String, String> properties;
    private final ImmutableList<Device> devices;
    private final UUID uuid;

    private NodeLibrary(String name, File file, Node root, NodeRepository nodeRepository, FunctionRepository functionRepository, Map<String, String> properties, List<Device> devices, UUID uuid) {
        checkNotNull(name, "Name cannot be null.");
        checkNotNull(root, "Root node cannot be null.");
        checkNotNull(functionRepository, "Function repository cannot be null.");
        this.name = name;
        this.root = root;
        this.nodeRepository = nodeRepository;
        this.functionRepository = functionRepository;
        this.file = file;
        this.properties = ImmutableMap.copyOf(properties);
        this.devices = ImmutableList.copyOf(devices);
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Node getRoot() {
        return root;
    }

    public Node getNodeForPath(String path) {
        checkArgument(path.startsWith("/"), "Only absolute paths are supported.");
        if (path.length() == 1) return root;

        Node node = root;
        path = path.substring(1);
        for (String name : Splitter.on("/").split(path)) {
            node = node.getChild(name);
            if (node == null) return null;
        }
        return node;
    }

    public NodeRepository getNodeRepository() {
        return nodeRepository;
    }

    public FunctionRepository getFunctionRepository() {
        return functionRepository;
    }

    //// Properties ////

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    public String getProperty(String name, String defaultValue) {
        if (hasProperty(name)) {
            return properties.get(name);
        } else {
            return defaultValue;
        }
    }

    public int getIntProperty(String name, int defaultValue) {
        if (hasProperty(name)) {
            try {
                return Integer.parseInt(properties.get(name));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Rect getBounds() {
        return Rect.centeredRect(getIntProperty("canvasX", 0),
                getIntProperty("canvasY", 0),
                getIntProperty("canvasWidth", 0),
                getIntProperty("canvasHeight", 0));
    }

    public boolean isValidPropertyName(String name) {
        checkNotNull(name);
        // no whitespace, only lowercase, numbers + period.
        return true;
    }

    public NodeLibrary withProperty(String name, String value) {
        ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        checkArgument(isValidPropertyName(name), "Property name '%s' is not valid.", name);
        b.putAll(properties);
        b.put(name, value);
        return withProperties(b.build());
    }

    public NodeLibrary withPropertyRemoved(String name) {
        if (!hasProperty(name)) return this;
        ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
        for (Map.Entry<String, String> entry : this.properties.entrySet()) {
            if (!entry.getKey().equals(name)) {
                b.put(entry);
            }
        }
        return withProperties(b.build());
    }

    public NodeLibrary withProperties(Map<String, String> properties) {
        return new NodeLibrary(this.name, this.file, this.root, this.nodeRepository, this.functionRepository, ImmutableMap.copyOf(properties), this.devices, this.uuid);
    }


    public ImmutableList<Device> getDevices() {
        return devices;
    }

    public boolean hasDevice(String name) {
        for (Device device : devices) {
            if (device.getName().equals(name))
                return true;
        }
        return false;
    }

    public Device getDevice(String name) {
        for (Device device : devices) {
            if (device.getName().equals(name))
                return device;
        }
        return null;
    }

    public NodeLibrary withDeviceAdded(Device device) {
        checkNotNull(device, "Device cannot be null.");
        checkArgument(!hasDevice(device.getName()), "There is already a device named %s", device.getName());
        ImmutableList.Builder<Device> b = ImmutableList.builder();
        b.addAll(getDevices());
        b.add(device);
        return new NodeLibrary(this.name, this.file, this.root, this.nodeRepository, this.functionRepository, this.properties, b.build(), this.uuid);
    }

    public NodeLibrary withDeviceRemoved(Device device) {
        return withDeviceRemoved(device.getName());
    }

    public String uniqueName(String prefix) {
        Matcher m = NUMBER_AT_THE_END.matcher(prefix);
        m.find();
        String namePrefix = m.group(1);
        String number = m.group(2);
        int counter;
        if (number.length() > 0) {
            counter = Integer.parseInt(number);
        } else {
            counter = 1;
        }
        while (true) {
            String suggestedName = namePrefix + counter;
            if (!hasDevice(suggestedName)) {
                return suggestedName;
            }
            ++counter;
        }
    }

    public NodeLibrary withDeviceRemoved(String name) {
        ImmutableList.Builder<Device> b = ImmutableList.builder();
        for (Device device : getDevices()) {
            if (!device.getName().equals(name))
                b.add(device);
        }
        return new NodeLibrary(this.name, this.file, this.root, this.nodeRepository, this.functionRepository, this.properties, b.build(), this.uuid);
    }

    public NodeLibrary withDevicePropertyChanged(String deviceName, String propertyName, String propertyValue) {
        checkArgument(hasDevice(deviceName), "No device %s present.");
        Device newDevice = getDevice(deviceName).withProperty(propertyName, propertyValue);
        ImmutableList.Builder<Device> b = ImmutableList.builder();
        for (Device device : getDevices()) {
            if (device.getName().equals(deviceName))
                b.add(newDevice);
            else
                b.add(device);
        }
        return new NodeLibrary(this.name, this.file, this.root, this.nodeRepository, this.functionRepository, this.properties, b.build(), this.uuid);
    }

    //// Loading ////

    private static NodeLibrary load(String libraryName, File file, Reader r, NodeRepository nodeRepository) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(r);
        NodeLibrary nodeLibrary = null;
        while (reader.hasNext()) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("ndbx")) {
                    String formatVersion = reader.getAttributeValue(null, "formatVersion");
                    if (formatVersion != null && !CURRENT_FORMAT_VERSION.equals(formatVersion)) {
                        throw new OutdatedLibraryException(file, "File uses version " + formatVersion + ", current version is " + CURRENT_FORMAT_VERSION + ".");
                    }
                    String uuidString = reader.getAttributeValue(null, "uuid");
                    UUID uuid = (uuidString == null) ? UUID.randomUUID() : UUID.fromString(uuidString);
                    nodeLibrary = parseNDBX(libraryName, file, reader, nodeRepository, uuid);
                } else {
                    throw new XMLStreamException("Only tag ndbx allowed, not " + tagName, reader.getLocation());
                }
            }
        }
        return nodeLibrary;
    }

    private static NodeLibrary parseNDBX(String libraryName, File file, XMLStreamReader reader, NodeRepository nodeRepository, UUID uuid) throws XMLStreamException {
        List<FunctionLibrary> functionLibraries = new LinkedList<FunctionLibrary>();
        Map<String, String> propertyMap = new HashMap<String, String>();
        Node rootNode = Node.ROOT;
        List<Device> devices = new LinkedList<Device>();

        while (true) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("property")) {
                    parseProperty(reader, propertyMap);
                } else if (tagName.equals("link")) {
                    FunctionLibrary functionLibrary = parseLink(file, reader);
                    functionLibraries.add(functionLibrary);
                } else if (tagName.equals("device")) {
                    Device device = parseDevice(reader);
                    devices.add(device);
                } else if (tagName.equals("node")) {
                    rootNode = parseNode(reader, rootNode, nodeRepository);
                } else {
                    throw new XMLStreamException("Unknown tag " + tagName, reader.getLocation());
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("ndbx"))
                    break;
            }
        }
        FunctionLibrary[] fl = functionLibraries.toArray(new FunctionLibrary[functionLibraries.size()]);
        return new NodeLibrary(libraryName, file, rootNode, nodeRepository, FunctionRepository.of(fl), propertyMap, devices, uuid);
    }

    private static FunctionLibrary parseLink(File file, XMLStreamReader reader) throws XMLStreamException {
        String linkRelation = reader.getAttributeValue(null, "rel");
        checkState(linkRelation.equals("functions"));
        String ref = reader.getAttributeValue(null, "href");
        // loading should happen lazily?
        return FunctionLibrary.load(file, ref);
    }

    /**
     * Parse the <property> tag and add the result to the propertyMap.
     */
    private static void parseProperty(XMLStreamReader reader, Map<String, String> propertyMap) throws XMLStreamException {
        String name = reader.getAttributeValue(null, "name");
        String value = reader.getAttributeValue(null, "value");
        if (name == null || value == null) return;
        propertyMap.put(name, value);
    }

    /**
     * Parse the external devices.
     */
    private static Device parseDevice(XMLStreamReader reader) throws XMLStreamException {
        String name = reader.getAttributeValue(null, "name");
        String type = reader.getAttributeValue(null, "type");

        Device device = Device.deviceForType(name, type);

        while (true) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("property")) {
                    String propertyName = reader.getAttributeValue(null, "name");
                    String propertyValue = reader.getAttributeValue(null, "value");
                    if (propertyName == null || propertyValue == null) continue;
                    device = device.withProperty(propertyName, propertyValue);
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("device"))
                    break;
            }
        }
        return device;
    }

    /**
     * Parse a specific node attribute value and add it to the attributeMap.
     */
    private static void parseNodeAttribute(XMLStreamReader reader, Map<String, String> attributeMap, String attribute) throws XMLStreamException {
        attributeMap.put(attribute, reader.getAttributeValue(null, attribute));
    }

    /**
     * Parse the <node> tag's attribute values.
     */
    private static Map<String, String> parseNodeAttributes(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> attributeMap = new HashMap<String, String>();
        String[] attributes = {"prototype", "name", "comment", "category", "description", "image", "function",
                "outputType", "outputRange", "position", "renderedChild", "handle", "alwaysRendered"};
        for (String attribute : attributes)
            parseNodeAttribute(reader, attributeMap, attribute);
        return attributeMap;
    }

    /**
     * @param attributeMap   The map containing node attributes.
     * @param extendFromNode The node from which to extend when there is no specified prototype.
     * @param parent         The parent node.
     * @param nodeRepository The node library dependencies.
     * @return A new node.
     */

    private static Node createNode(Map<String, String> attributeMap, Node extendFromNode, Node parent, NodeRepository nodeRepository) {
        String prototypeId = attributeMap.get("prototype");
        String name = attributeMap.get("name");
        String comment = attributeMap.get("comment");
        String category = attributeMap.get("category");
        String description = attributeMap.get("description");
        String image = attributeMap.get("image");
        String function = attributeMap.get("function");
        String outputType = attributeMap.get("outputType");
        String outputRange = attributeMap.get("outputRange");
        String position = attributeMap.get("position");
        String handle = attributeMap.get("handle");
        String alwaysRendered = attributeMap.get("alwaysRendered");

        Node prototype = prototypeId == null ? extendFromNode : lookupNode(prototypeId, parent, nodeRepository);
        if (prototype == null) return null;
        Node node = prototype.extend();

        if (name != null)
            node = node.withName(name);
        if (comment != null)
            node = node.withComment(comment);
        if (category != null)
            node = node.withCategory(category);
        if (description != null)
            node = node.withDescription(description);
        if (image != null)
            node = node.withImage(image);
        if (function != null)
            node = node.withFunction(function);
        if (outputType != null)
            node = node.withOutputType(outputType);
        if (outputRange != null)
            node = node.withOutputRange(Port.Range.valueOf(outputRange.toUpperCase()));
        if (position != null)
            node = node.withPosition(Point.valueOf(position));
        if (handle != null)
            node = node.withHandle(handle);
        if (alwaysRendered != null)
            node = node.withAlwaysRenderedSet(Boolean.parseBoolean(alwaysRendered));
        return node;
    }

    /**
     * Parse the <node> tag.
     *
     * @param reader         The XML stream.
     * @param parent         The parent node.
     * @param nodeRepository The node library dependencies.
     * @return The new node.
     * @throws XMLStreamException if a parse error occurs.
     */
    private static Node parseNode(XMLStreamReader reader, Node parent, NodeRepository nodeRepository) throws XMLStreamException {
        Map<String, String> attributeMap = parseNodeAttributes(reader);
        Node node = createNode(attributeMap, Node.ROOT, parent, nodeRepository);
        String prototypeId = attributeMap.get("prototype");
        if (node == null) {
            throw new XMLStreamException("Prototype " + prototypeId + " could not be found.", reader.getLocation());
        }

        while (true) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();

                if (tagName.equals("node") || tagName.equals("importCoreNode")) {
                    if (prototypeId == null && !node.isNetwork())
                        node = createNode(attributeMap, Node.NETWORK, parent, nodeRepository);
                }

                if (tagName.equals("node")) {
                    node = node.withChildAdded(parseNode(reader, node, nodeRepository));
                } else if (tagName.equals("importCoreNode")) {
                    String s = reader.getAttributeValue(null, "ref");
                    Node coreNode = Node.coreNodes.get(s);
                    if (coreNode == null) {
                        throw new XMLStreamException("Core node '" + s + "' does not exist.", reader.getLocation());
                    }
                    node = node.withChildAdded(coreNode);
                } else if (tagName.equals("port")) {
                    String portName = reader.getAttributeValue(null, "name");
                    // Remove the port if it is already on the prototype.
                    if (node.hasInput(portName)) {
                        node = node.withInputChanged(portName, parsePort(reader, node.getInput(portName)));
                    } else {
                        node = node.withInputAdded(parsePort(reader, null));
                    }
                } else if (tagName.equals("conn")) {
                    node = node.withConnectionAdded(parseConnection(reader));
                } else {
                    throw new XMLStreamException("Unknown tag " + tagName, reader.getLocation());
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("node"))
                    break;
            }
        }

        // This has to come at the end, since the child first needs to exist.
        String renderedChildName = attributeMap.get("renderedChild");
        if (renderedChildName != null)
            node = node.withRenderedChildName(renderedChildName);

        return node;
    }

    /**
     * Lookup the node in the node repository.
     * <p/>
     * If the node id consists of just a node name, without spaces, it is looked up in the parent node.
     *
     * @param nodeId         The node id.
     * @param parent         The parent node.
     * @param nodeRepository The node repository.
     * @return The existing node.
     */
    private static Node lookupNode(String nodeId, Node parent, NodeRepository nodeRepository) {
        if (nodeId.contains(".")) {
            return nodeRepository.getNode(nodeId);
        } else {
            return parent.getChild(nodeId);
        }
    }

    private static Port parsePort(XMLStreamReader reader, Port prototype) throws XMLStreamException {
        // Name and type are always required.
        String name = reader.getAttributeValue(null, "name");
        String type = reader.getAttributeValue(null, "type");
        String label = reader.getAttributeValue(null, "label");
        String childReference = reader.getAttributeValue(null, "childReference");
        String widget = reader.getAttributeValue(null, "widget");
        String range = reader.getAttributeValue(null, "range");
        String value = reader.getAttributeValue(null, "value");
        String description = reader.getAttributeValue(null, "description");
        String min = reader.getAttributeValue(null, "min");
        String max = reader.getAttributeValue(null, "max");

        Port port;
        if (prototype == null) {
            port = Port.portForType(name, type);
        } else {
            port = prototype;
        }

        // Widget, value, min, max are optional and could come from the prototype.
        if (label != null)
            port = port.withParsedAttribute(Port.Attribute.LABEL, label);
        if (childReference != null)
            port = port.withParsedAttribute(Port.Attribute.CHILD_REFERENCE, childReference);
        if (widget != null)
            port = port.withParsedAttribute(Port.Attribute.WIDGET, widget);
        if (range != null)
            port = port.withParsedAttribute(Port.Attribute.RANGE, range);
        if (value != null)
            port = port.withParsedAttribute(Port.Attribute.VALUE, value);
        if (description != null)
            port = port.withParsedAttribute(Port.Attribute.DESCRIPTION, description);
        if (min != null)
            port = port.withParsedAttribute(Port.Attribute.MINIMUM_VALUE, min);
        if (max != null)
            port = port.withParsedAttribute(Port.Attribute.MAXIMUM_VALUE, max);

        ImmutableList.Builder<MenuItem> b = ImmutableList.builder();

        while (true) {
            int eventType = reader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("menu")) {
                    b.add(parseMenuItem(reader));
                } else {
                    throw new XMLStreamException("Unknown tag " + tagName, reader.getLocation());
                }
            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                String tagName = reader.getLocalName();
                if (tagName.equals("port"))
                    break;
            }
        }
        ImmutableList<MenuItem> items = b.build();
        if (!items.isEmpty())
            port = port.withMenuItems(items);
        return port;
    }

    private static MenuItem parseMenuItem(XMLStreamReader reader) throws XMLStreamException {
        String key = reader.getAttributeValue(null, "key");
        String label = reader.getAttributeValue(null, "label");
        if (key == null)
            throw new XMLStreamException("Menu item key cannot be null.", reader.getLocation());
        return new MenuItem(key, label != null ? label : key);
    }

    private static Connection parseConnection(XMLStreamReader reader) throws XMLStreamException {
        String outputNode = reader.getAttributeValue(null, "output");
        String input = reader.getAttributeValue(null, "input");
        Iterator<String> inputIterator = PORT_NAME_SPLITTER.split(input).iterator();
        String inputNode = inputIterator.next();
        String inputPort = inputIterator.next();
        return new Connection(outputNode, inputNode, inputPort);
    }

    ///// Mutation methods ////

    public NodeLibrary withRoot(Node newRoot) {
        return new NodeLibrary(this.name, this.file, newRoot, this.nodeRepository, this.functionRepository, this.properties, this.devices, this.uuid);
    }

    public NodeLibrary withFunctionRepository(FunctionRepository newRepository) {
        return new NodeLibrary(this.name, this.file, this.root, this.nodeRepository, newRepository, this.properties, this.devices, this.uuid);
    }

    public NodeLibrary withFile(File newFile) {
        return new NodeLibrary(this.name, newFile, this.root, this.nodeRepository, this.functionRepository, this.properties, this.devices, this.uuid);
    }

    //// Saving ////

    public String toXml() {
        return NDBXWriter.asString(this);
    }

    /**
     * Write the NodeLibrary to a file.
     *
     * @param file The file to save.
     * @throws java.io.IOException When file saving fails.
     */
    public void store(File file) throws IOException {
        NDBXWriter.write(this, file);
    }

    //// Object overrides ////

    @Override
    public int hashCode() {
        return Objects.hashCode(name, root, functionRepository);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodeLibrary)) return false;
        final NodeLibrary other = (NodeLibrary) o;
        return Objects.equal(name, other.name)
                && Objects.equal(root, other.root)
                && Objects.equal(functionRepository, other.functionRepository);
    }

    @Override
    public String toString() {
        return String.format("<NodeLibrary %s>", name);
    }

}
