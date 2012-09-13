package nodebox.node;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.function.Function;
import nodebox.function.FunctionRepository;
import nodebox.graphics.Color;
import nodebox.graphics.IGeometry;
import nodebox.graphics.Point;
import nodebox.util.ListUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

public final class NodeContext {

//    private final Map<Node, Iterable<?>> outputValuesMap = new HashMap<Node, Iterable<?>>();
//    private final Map<NodePort, Iterable<?>> inputValuesMap = new HashMap<NodePort, Iterable<?>>();
//    private final Set<Node> renderedNodes = new HashSet<Node>();

    private final static ImmutableMap<ConversionPair, String> conversionMap;
    private final static ImmutableList<Class> ancestorTypes;

    static {
        ImmutableMap.Builder<ConversionPair, String> builder = ImmutableMap.builder();
        builder.put(ConversionPair.of(Port.TYPE_STRING, Object.class), "toString");
        builder.put(ConversionPair.of(Port.TYPE_BOOLEAN, String.class), "stringToBoolean");
        builder.put(ConversionPair.of(Port.TYPE_BOOLEAN, Long.class), "numberToBoolean");
        builder.put(ConversionPair.of(Port.TYPE_BOOLEAN, Double.class), "numberToBoolean");
        builder.put(ConversionPair.of(Port.TYPE_BOOLEAN, Number.class), "numberToBoolean");
        builder.put(ConversionPair.of(Port.TYPE_INT, Double.class), "doubleToInt");
        builder.put(ConversionPair.of(Port.TYPE_INT, String.class), "stringToInt");
        builder.put(ConversionPair.of(Port.TYPE_INT, Boolean.class), "booleanToInt");
        builder.put(ConversionPair.of(Port.TYPE_FLOAT, String.class), "stringToDouble");
        builder.put(ConversionPair.of(Port.TYPE_FLOAT, Boolean.class), "booleanToDouble");
        builder.put(ConversionPair.of(Port.TYPE_POINT, IGeometry.class), "geometryToPoints");
        builder.put(ConversionPair.of(Port.TYPE_POINT, Long.class), "numberToPoint");
        builder.put(ConversionPair.of(Port.TYPE_POINT, Double.class), "numberToPoint");
        builder.put(ConversionPair.of(Port.TYPE_POINT, Number.class), "numberToPoint");
        builder.put(ConversionPair.of(Port.TYPE_POINT, String.class), "stringToPoint");
        builder.put(ConversionPair.of(Port.TYPE_COLOR, Long.class), "intToColor");
        builder.put(ConversionPair.of(Port.TYPE_COLOR, Double.class), "doubleToColor");
        builder.put(ConversionPair.of(Port.TYPE_COLOR, String.class), "stringToColor");
        builder.put(ConversionPair.of(Port.TYPE_COLOR, Boolean.class), "booleanToColor");
        conversionMap = builder.build();

        ImmutableList.Builder<Class> b = ImmutableList.builder();
        b.add(IGeometry.class);
        b.add(Boolean.class);
        b.add(Long.class);
        b.add(Double.class);
        b.add(Number.class);
        b.add(String.class);
        ancestorTypes = b.build();
    }

    private final NodeLibrary nodeLibrary;
    private final FunctionRepository functionRepository;
    private final double frame;


    public NodeContext(NodeLibrary nodeLibrary) {
        this(nodeLibrary, null, 1);
    }


    public NodeContext(NodeLibrary nodeLibrary, double frame) {
        this(nodeLibrary, null, frame);
    }

    public NodeContext(NodeLibrary nodeLibrary, FunctionRepository functionRepository, double frame) {
        this.nodeLibrary = nodeLibrary;
        this.functionRepository = functionRepository != null ? functionRepository : nodeLibrary.getFunctionRepository();
        this.frame = frame;
    }


    public NodeLibrary getNodeLibrary() {
        return nodeLibrary;
    }

    public FunctionRepository getFunctionRepository() {
        return functionRepository;
    }

    public double getFrame() {
        return frame;
    }

    /**
     * Render the node by rendering its rendered child, or the function.
     * Because it can't look at what network it is in, this function does not evaluate dependencies
     * of the node. However, if this node is a network, the whole child node is evaluated.
     *
     * @param node The node to render.
     * @return The list of results.
     * @throws NodeRenderException If processing fails.
     */
    public List<?> renderNode(Node node) throws NodeRenderException {
        checkNotNull(node);
        checkNotNull(functionRepository);

        // If the node has children, forgo the operation of the current node and evaluate the child.
        if (node.hasRenderedChild()) {
            return renderChild(node, node.getRenderedChild());
        } else {
            List<Object> arguments = new ArrayList<Object>();
            for (Port port : node.getAllInputs()) {
                arguments.add(getPortValue(port));
            }
            return postProcessResult(node, invokeNode(node, arguments));
        }
    }

    private List<?> postProcessResult(Node node, Object result) {
        if (node.hasListOutputRange()) {
            return (List<?>) result;
        } else {
            return result == null ? ImmutableList.of() : ImmutableList.of(result);
        }
    }

    public List<?> renderChild(Node network, Node child) throws NodeRenderException {
        // A list of all result objects.
        List<Object> resultsList = new ArrayList<Object>();

        // If the node has no input ports, execute the node once for its side effects.
        if (child.getAllInputs().isEmpty()) {
            return renderNode(child);
        } else {
            // The list of values that need to be processed for this port.
            Map<Port, List<?>> portArguments = new LinkedHashMap<Port, List<?>>();

            for (Port port : child.getInputs()) {
                List<?> result = evaluatePort(network, child, port);
                portArguments.put(port, result);
            }

            // A prepared list of argument lists, each for one invocation of the child node.
            List<List<?>> argumentLists = buildArgumentLists(portArguments);

            for (List<?> arguments : argumentLists) {
                Object result = invokeNode(child, arguments);
                if (child.hasListOutputRange()) {
                    resultsList.addAll((List<?>) result);
                } else {
                    resultsList.add(result);
                }
            }
        }

        return resultsList;
//
//
//        for (PublishedPort pp : node.getPublishedInputs()) {
//            NodePort np = NodePort.of(node.getName(), pp.getPublishedName());
//            if (inputValuesMap.containsKey(np)) {
//                context.inputValuesMap.put(NodePort.of(pp.getChildNode(), pp.getChildPort()),
//                        inputValuesMap.get(np));
//
//            }
//        }
    }

    private Object invokeNode(Node node, List<?> arguments) {
        String functionName = node.getFunction();
        Function function = functionRepository.getFunction(functionName);
        return invokeFunction(node, function, arguments);
    }

    private Class getAncestorType(Class type) {
        for (Class klass : ancestorTypes) {
            Class<?> c = klass;
            if (c.isAssignableFrom(type))
                return c;
        }
        return Object.class;
    }

    private Iterable<?> convert(Iterable<?> outputValues, String inputType) {
        Class outputType = ListUtils.listClass(outputValues);
        Class ancestorType = getAncestorType(outputType);
        String conversionMethod = conversionMap.get(ConversionPair.of(inputType, ancestorType));

        if (conversionMethod != null) {
            try {
                for (Method method : Conversions.class.getDeclaredMethods()) {
                    if (method.getName().equals(conversionMethod))
                        return (Iterable<?>) method.invoke(null, outputValues);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error while accessing conversion method.", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Error while accessing conversion method.", e);
            }
        }

        return outputValues;
    }

    private Node findOutputNode(Node network, Node inputNode, Port inputPort) {
        for (Connection c : network.getConnections()) {
            if (c.getInputNode().equals(inputNode.getName()) && c.getInputPort().equals(inputPort.getName())) {
                return network.getChild(c.getOutputNode());
            }
        }
        return null;
    }

    private List<?> evaluatePort(Node network, Node child, Port childPort) {
        Node outputNode = findOutputNode(network, child, childPort);
        if (outputNode != null) {
            return renderChild(network, outputNode);
        } else {
            return ImmutableList.of(getPortValue(childPort));
        }
    }

    /**
     * Get the value of the port.
     * <p/>
     * This method does some last-minute conversions and lookups on special cases:
     * <ul>
     * <li>If the port type is context, return a reference to the current node context.</li>
     * <li>If the port is a file widget, convert relative to absolute paths.</li>
     * </ul>
     */
    private Object getPortValue(Port port) {
        if (port.getType().equals("context")) {
            return this;
        } else if (port.isFileWidget()) {
            String path = port.stringValue();
            if (!path.startsWith("/")) {
                // Convert relative to absolute path.
                if (nodeLibrary.getFile() != null) {
                    File f = new File(nodeLibrary.getFile().getParentFile(), path);
                    return f.getAbsolutePath();
                }
            } else {
                return path;
            }
        }
        return port.getValue();
    }

    private Object invokeFunction(Node node, Function function, List<?> arguments) throws NodeRenderException {
        try {
            return function.invoke(arguments.toArray());
        } catch (Exception e) {
            throw new NodeRenderException(node, e);
        }
    }

    /**
     * Return the size of the biggest list.
     *
     * @param listOfLists a list of lists.
     * @return The maximum size.
     */
    private static int biggestList(List<List<?>> listOfLists) {
        int maxSize = 0;
        for (List<?> list : listOfLists) {
            maxSize = Math.max(maxSize, list.size());
        }
        return maxSize;
    }

    /**
     * Get the object of the list at the specified index.
     * <p/>
     * If the index is bigger than the list size, the index is wrapped.
     */
    private static Object wrappingGet(List<?> list, int index) {
        return list.get(index % list.size());
    }

    /**
     * Build the arguments to invoke a node with.
     * <p/>
     * Given the following lists per port:
     * [[1 2 3 4 5] ["a" "b"] [true]]
     * <p/>
     * Builds the following argument lists:
     * [
     * [1 "a" true]
     * [2 "b" true]
     * [3 "a" true]
     * [4 "b" true]
     * [5 "a" true]]
     */
    private static List<List<?>> buildArgumentLists(Map<Port, List<?>> argumentsPerPort) {
        List<List<?>> argumentLists = new ArrayList<List<?>>();

        int maxSize = biggestArgumentList(argumentsPerPort);
        for (int i = 0; i < maxSize; i++) {
            ArrayList<Object> argumentList = new ArrayList<Object>(argumentsPerPort.size());
            for (Map.Entry<Port, List<?>> entry : argumentsPerPort.entrySet()) {
                if (entry.getKey().hasListRange()) {
                    argumentList.add(entry.getValue());
                } else {
                    argumentList.add(wrappingGet(entry.getValue(), i));
                }
            }
            argumentLists.add(argumentList);
        }
        return argumentLists;
    }

    private static int biggestArgumentList(Map<Port, List<?>> argumentsPerPort) {
        int maxSize = 0;
        for (Map.Entry<Port, List<?>> entry : argumentsPerPort.entrySet()) {
            maxSize = Math.max(maxSize, argumentListSize(entry.getKey(), entry.getValue()));
        }
        return maxSize;
    }

    private static int argumentListSize(Port port, List<?> arguments) {
        // If the port takes in a list, he will always take the entire argument list as an argument.
        // Therefore, the size of arguments is 1.
        if (port.hasListRange()) {
            return 1;
        } else {
            return arguments.size();
        }
    }

    /**
     * Return true if each element has a next element.
     *
     * @param ll The list of lists or values.
     * @return true if each of the lists has values.
     */
    private static boolean hasElements(List<ValueOrList> ll) {
        checkNotNull(ll);
        if (ll.isEmpty()) return false;
        for (ValueOrList v : ll) {
            if (v.isList()) {
                if (!v.getList().iterator().hasNext()) return false;
            }
        }
        return true;
    }

    private static final class ValueOrList {
        private final boolean isList;
        private final Object value;

        private static ValueOrList ofValue(Object value) {
            return new ValueOrList(false, value);
        }

        private static ValueOrList ofList(Iterable list) {
            return new ValueOrList(true, list);
        }

        private ValueOrList(boolean isList, Object value) {
            checkArgument(!isList || value instanceof Iterable);
            this.isList = isList;
            this.value = value;
        }


        private Object getValue() {
            checkState(!isList);
            return value;
        }

        private Iterable getList() {
            checkState(isList);
            return (Iterable) value;
        }

        private boolean isList() {
            return isList;
        }

    }

    public static final class ConversionPair {
        private final String type;
        private final Class klass;

        public static ConversionPair of(String type, Class klass) {
            return new ConversionPair(type, klass);
        }

        private ConversionPair(String type, Class klass) {
            checkNotNull(type);
            checkNotNull(klass);
            this.type = type;
            this.klass = klass;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ConversionPair)) return false;
            final ConversionPair other = (ConversionPair) o;
            return Objects.equal(type, other.type)
                    && Objects.equal(klass, other.klass);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, klass);
        }
    }

    private static final class Conversions {
        public static Iterable<?> geometryToPoints(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.addAll((Iterable<?>) ((IGeometry) o).getPoints());
            return b.build();
        }

        public static Iterable<?> doubleToInt(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Long) (Math.round((Double) o))).intValue());
            return b.build();
        }

        public static Iterable<?> toString(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(o.toString());
            return b.build();
        }

        public static Iterable<?> stringToInt(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(Integer.parseInt((String) o));
            return b.build();
        }

        public static Iterable<?> stringToDouble(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(Double.parseDouble((String) o));
            return b.build();
        }

        public static Iterable<?> stringToBoolean(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(Boolean.parseBoolean(((String) o).toLowerCase()));
            return b.build();
        }

        public static Iterable<?> booleanToInt(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Boolean) o) ? 1 : 0);
            return b.build();
        }

        public static Iterable<?> booleanToDouble(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Boolean) o) ? 1.0 : 0.0);
            return b.build();
        }

        public static Iterable<?> numberToBoolean(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Integer) o) == 1);
            return b.build();
        }

        public static Iterable<?> stringToColor(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(Color.parseColor((String) o));
            return b.build();
        }

        public static Iterable<?> intToColor(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(new Color(((Long) o) / 255.0));
            return b.build();
        }

        public static Iterable<?> doubleToColor(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(new Color((Double) o));
            return b.build();
        }

        public static Iterable<?> booleanToColor(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Boolean) o) ? Color.WHITE : Color.BLACK);
            return b.build();
        }

        public static Iterable<?> numberToPoint(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values) {
                double d = ((Number) o).doubleValue();
                b.add(new Point(d, d));
            }
            return b.build();
        }

        public static Iterable<?> stringToPoint(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values) {
                b.add(Point.valueOf((String) o));
            }
            return b.build();
        }
    }
}
