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
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class NodeContext {

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
        return renderNode(node, Collections.<Port, Object>emptyMap());
    }

    public List<?> renderNode(Node node, Map<Port, ?> argumentMap) {
        checkNotNull(node);
        checkNotNull(functionRepository);

        // If the node has children, forgo the operation of the current node and evaluate the child.
        Object result;
        if (node.hasRenderedChild()) {
            result = renderChild(node, node.getRenderedChild(), argumentMap);
        } else {
            result = invokeNode(node, argumentMap);
        }
        return postProcessResult(node, result);
    }

    private List<?> postProcessResult(Node node, Object result) {
        if (node.hasListOutputRange()) {
            // TODO This is a temporary fix for networks that have no rendered nodes.
            // They execute the "core/zero" function which returns a single value, not a list.
            if (result instanceof List<?>) {
                return (List<?>) result;
            } else {
                return ImmutableList.of(result);
            }
        } else if (result instanceof List && ((List) result).isEmpty()) {
            return (List<?>) result;
        } else {
            return result == null ? ImmutableList.of() : ImmutableList.of(result);
        }
    }

    public List<?> renderChild(Node network, Node child) throws NodeRenderException {
        return renderChild(network, child, Collections.<Port, Object>emptyMap());
    }

    public List<?> renderChild(Node network, Node child, Map<Port, ?> networkArgumentMap) {
        // A list of all result objects.
        List<Object> resultsList = new ArrayList<Object>();

        // If the node has no input ports, execute the node once for its side effects.
        if (child.getInputs().isEmpty()) {
            return renderNode(child);
        } else {
            // The list of values that need to be processed for this port.
            Map<Port, List<?>> portArguments = new LinkedHashMap<Port, List<?>>();

            // Evaluate the port data.
            for (Port port : child.getInputs()) {
                List<?> result = evaluatePort(network, child, port, networkArgumentMap);
                List<?> convertedResult = convertResultsForPort(port, result);
                portArguments.put(port, convertedResult);
            }

            // Data from the network (through published ports) overrides the arguments.
            for (Map.Entry<Port, ?> argumentEntry : networkArgumentMap.entrySet()) {
                Port networkPort = argumentEntry.getKey();
                checkState(networkPort.isPublishedPort(), "Given port %s is not a published port.", networkPort);
                if (networkPort.getChildNode(network) == child) {
                    Port childPort = networkPort.getChildPort(network);
                    List<?> values = preprocessInput(networkPort, childPort, argumentEntry.getValue());
                    portArguments.put(childPort, values);
                }
            }

            // A prepared list of argument lists, each for one invocation of the child node.
            List<Map<Port, ?>> argumentMaps = buildArgumentMaps(portArguments);

            for (Map<Port, ?> argumentMap : argumentMaps) {
                List<?> results = renderNode(child, argumentMap);
                resultsList.addAll(results);
            }
        }

        return resultsList;
    }

    private List<?> preprocessInput(Port networkPort, Port childPort, Object value) {
        if (networkPort.hasListRange()) {
            if (childPort.hasListRange()) {
                return (List<?>) value;
            } else {
                return (List<?>) value;
            }
        } else {
            if (childPort.hasListRange()) {
                return ImmutableList.of(value);
            } else {
                return ImmutableList.of(value);
            }
        }
    }

    private Object invokeNode(Node node, Map<Port, ?> argumentMap) {
        List<Object> arguments = new LinkedList<Object>();
        for (Port port : node.getInputs()) {
            if (argumentMap.containsKey(port)) {
                arguments.add(argumentMap.get(port));
            } else if (port.hasValueRange()) {
                arguments.add(getPortValue(port));
            } else {
                // The port expects a list but nothing is connected. Evaluate with an empty list.
                arguments.add(ImmutableList.of());
            }
        }
        return invokeNode(node, arguments);
    }

    private Object invokeNode(Node node, List<?> arguments) {
        Function function = functionRepository.getFunction(node.getFunction());
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

    private List<?> convertResultsForPort(Port port, List<?> values) {
        Class outputType = ListUtils.listClass(values);
        Class ancestorType = getAncestorType(outputType);
        String conversionMethod = conversionMap.get(ConversionPair.of(port.getType(), ancestorType));

        if (conversionMethod != null) {
            try {
                for (Method method : Conversions.class.getDeclaredMethods()) {
                    if (method.getName().equals(conversionMethod))
                        return (List<?>) method.invoke(null, values);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error while accessing conversion method.", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Error while accessing conversion method.", e);
            }
        }

        return values;
    }

    private Node findOutputNode(Node network, Node inputNode, Port inputPort) {
        for (Connection c : network.getConnections()) {
            if (c.getInputNode().equals(inputNode.getName()) && c.getInputPort().equals(inputPort.getName())) {
                return network.getChild(c.getOutputNode());
            }
        }
        return null;
    }

    private List<?> evaluatePort(Node network, Node child, Port childPort, Map<Port, ?> networkArgumentMap) {
        Node outputNode = findOutputNode(network, child, childPort);
        if (outputNode != null) {
            return renderChild(network, outputNode, networkArgumentMap);
        } else {
            Object value = getPortValue(childPort);
            if (value == null) {
                return ImmutableList.of();
            } else {
                return ImmutableList.of(value);
            }
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
     * {alpha:[1 2 3 4 5]
     * beta: ["a" "b"]
     * gamma: [true]}
     * <p/>
     * Builds the following argument maps:
     * [
     * {alpha: 1 beta:"a" gamma:true}
     * {alpha: 2 beta:"b" gamma:true}
     * {alpha: 3 beta:"a" gamma:true}
     * {alpha: 4 beta:"b" gamma:true}
     * {alpha: 5 beta:"a" gamma:true}]
     */
    private static List<Map<Port, ?>> buildArgumentMaps(Map<Port, List<?>> argumentsPerPort) {
        List<Map<Port, ?>> argumentMaps = new ArrayList<Map<Port, ?>>();

        int minSize = smallestArgumentList(argumentsPerPort);
        if (minSize == 0) return Collections.emptyList();

        int maxSize = biggestArgumentList(argumentsPerPort);
        for (int i = 0; i < maxSize; i++) {
            Map<Port, Object> argumentMap = new HashMap<Port, Object>(argumentsPerPort.size());
            for (Map.Entry<Port, List<?>> entry : argumentsPerPort.entrySet()) {
                if (entry.getKey().hasListRange()) {
                    argumentMap.put(entry.getKey(), entry.getValue());
                } else {
                    argumentMap.put(entry.getKey(), wrappingGet(entry.getValue(), i));
                }
            }
            argumentMaps.add(argumentMap);
        }
        return argumentMaps;
    }

    private static int smallestArgumentList(Map<Port, List<?>> argumentsPerPort) {
        int minSize = Integer.MAX_VALUE;
        for (Map.Entry<Port, List<?>> entry : argumentsPerPort.entrySet()) {
            minSize = Math.min(minSize, argumentListSize(entry.getKey(), entry.getValue()));
        }
        return minSize;
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
        public static List<?> geometryToPoints(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.addAll((Iterable<?>) ((IGeometry) o).getPoints());
            return b.build();
        }

        public static List<?> doubleToInt(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Long) (Math.round((Double) o))).intValue());
            return b.build();
        }

        public static List<?> toString(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(o.toString());
            return b.build();
        }

        public static List<?> stringToInt(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(Integer.parseInt((String) o));
            return b.build();
        }

        public static List<?> stringToDouble(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(Double.parseDouble((String) o));
            return b.build();
        }

        public static List<?> stringToBoolean(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(Boolean.parseBoolean(((String) o).toLowerCase()));
            return b.build();
        }

        public static List<?> booleanToInt(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Boolean) o) ? 1 : 0);
            return b.build();
        }

        public static List<?> booleanToDouble(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Boolean) o) ? 1.0 : 0.0);
            return b.build();
        }

        public static List<?> numberToBoolean(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Integer) o) == 1);
            return b.build();
        }

        public static List<?> stringToColor(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(Color.parseColor((String) o));
            return b.build();
        }

        public static List<?> intToColor(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(new Color(((Long) o) / 255.0));
            return b.build();
        }

        public static List<?> doubleToColor(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(new Color((Double) o));
            return b.build();
        }

        public static List<?> booleanToColor(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values)
                b.add(((Boolean) o) ? Color.WHITE : Color.BLACK);
            return b.build();
        }

        public static List<?> numberToPoint(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values) {
                double d = ((Number) o).doubleValue();
                b.add(new Point(d, d));
            }
            return b.build();
        }

        public static List<?> stringToPoint(Iterable<?> values) {
            ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
            for (Object o : values) {
                b.add(Point.valueOf((String) o));
            }
            return b.build();
        }
    }
}
