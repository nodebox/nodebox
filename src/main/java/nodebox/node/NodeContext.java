package nodebox.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.function.Function;
import nodebox.function.FunctionRepository;
import nodebox.graphics.Point;
import nodebox.util.ListUtils;

import java.io.File;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class NodeContext {

    private final NodeLibrary nodeLibrary;
    private final FunctionRepository functionRepository;
    private final double frame;
    private final ImmutableMap<String, ?> data;
    private final ImmutableMap<Node, List<?>> previousRenderResults;
    private final Map<Node, List<?>> renderResults;


    public NodeContext(NodeLibrary nodeLibrary) {
        this(nodeLibrary, null, 1, ImmutableMap.<String, Object>of(), ImmutableMap.<Node, List<?>>of());
    }

    public NodeContext(NodeLibrary nodeLibrary, double frame) {
        this(nodeLibrary, null, frame, ImmutableMap.<String, Object>of(), ImmutableMap.<Node, List<?>>of());
    }

    public NodeContext(NodeLibrary nodeLibrary, FunctionRepository functionRepository, double frame, Map<String, ?> data, Map<Node, List<?>> previousRenderResults) {
        this.nodeLibrary = nodeLibrary;
        this.functionRepository = functionRepository != null ? functionRepository : nodeLibrary.getFunctionRepository();
        this.frame = frame;
        this.data = ImmutableMap.copyOf(data);
        this.renderResults = new HashMap<Node, List<?>>();
        this.previousRenderResults = ImmutableMap.copyOf(previousRenderResults);
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

    public Map<String, ?> getData() {
        return data;
    }

    public Map<Node, List<?>> getRenderResults() {
        return renderResults;
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
        List<?> results = postProcessResult(node, result);
        renderResults.put(node, results);
        return results;
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
        } else if (result instanceof List) {
            List<?> results = (List<?>) result;
            if (results.isEmpty())
                return results;
            Class outputType = ListUtils.listClass(results);
            if (outputType.equals(Point.class) && node.getOutputType().equals("geometry"))
                return results;
        }
        return result == null ? ImmutableList.of() : ImmutableList.of(result);
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
                result = convertResultsForPort(port, result);
                result = clampResultsForPort(port, result);
                portArguments.put(port, result);
            }

            // Data from the network (through published ports) overrides the arguments.
            for (Map.Entry<Port, ?> argumentEntry : networkArgumentMap.entrySet()) {
                Port networkPort = argumentEntry.getKey();
                checkState(networkPort.isPublishedPort(), "Given port %s is not a published port.", networkPort);
                if (networkPort.getChildNode(network) == child) {
                    Port childPort = networkPort.getChildPort(network);
                    Object value = argumentEntry.getValue();
                    List<?> values;
                    if (value instanceof List) {
                        values = (List<?>) value;
                    } else {
                        values = ImmutableList.of(value);
                    }
                    portArguments.put(childPort, values);
                }
            }

            // A prepared list of argument lists, each for one invocation of the child node.
            Iterable<Map<Port, ?>> argumentMaps = buildArgumentMaps(portArguments);

            for (Map<Port, ?> argumentMap : argumentMaps) {
                List<?> results = renderNode(child, argumentMap);
                resultsList.addAll(results);
            }
        }

        return resultsList;
    }

    private Object invokeNode(Node node, Map<Port, ?> argumentMap) {
        List<Object> arguments = new LinkedList<Object>();
        for (Port port : node.getInputs()) {
            if (argumentMap.containsKey(port)) {
                arguments.add(argumentMap.get(port));
            } else if (port.hasValueRange()) {
                arguments.add(getPortValue(node, port));
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

    private List<?> convertResultsForPort(Port port, List<?> values) {
        Class outputType = ListUtils.listClass(values);

        // This is a special case: when working with geometry nodes, we may want to work with either
        // single IGeometry objects or a list of Point's.
        // In the latter case, we have to wrap these values in a list.
        if (outputType.equals(Point.class) && port.getType().equals("geometry") && port.hasValueRange())
            return ImmutableList.of(values);

        return TypeConversions.convert(outputType, port.getType(), values);
    }

    private List<?> clampResultsForPort(Port port, List<?> values) {
        if (port.getMinimumValue() == null && port.getMaximumValue() == null) return values;
        ImmutableList.Builder<Object> b = ImmutableList.builder();
        for (Object v : values) {
            b.add(port.clampValue(v));
        }
        return b.build();
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
            Object value = getPortValue(child, childPort);
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
    private Object getPortValue(Node node, Port port) {
        if (port.getType().equals("context")) {
            return this;
        } else if (port.getType().equals(Port.TYPE_STATE)) {
            // The state of the node is the output value of the previous render of that node.
            Object previousState = previousRenderResults.get(node);
            if (previousState != null) {
                return previousState;
            } else {
                return ImmutableList.of();
            }
        } else if (port.isFileWidget() && !port.stringValue().isEmpty()) {
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
    private static Iterable<Map<Port, ?>> buildArgumentMaps(final Map<Port, List<?>> argumentsPerPort) {
        final int minSize = smallestArgumentList(argumentsPerPort);
        if (minSize == 0) return Collections.emptyList();

        final int maxSize = biggestArgumentList(argumentsPerPort);
        return new Iterable<Map<Port, ?>>() {
            int i = 0 ;
            @Override
            public Iterator<Map<Port, ?>> iterator() {
                return new Iterator<Map<Port, ?>>() {
                    @Override
                    public boolean hasNext() {
                        return i < maxSize;
                    }

                    @Override
                    public Map<Port, ?> next() {
                        Map<Port, Object> argumentMap = new HashMap<Port, Object>(argumentsPerPort.size());
                        for (Map.Entry<Port, List<?>> entry : argumentsPerPort.entrySet()) {
                            if (entry.getKey().hasListRange()) {
                                argumentMap.put(entry.getKey(), entry.getValue());
                            } else {
                                argumentMap.put(entry.getKey(), wrappingGet(entry.getValue(), i));
                            }
                        }
                        i++;
                        return argumentMap;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
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

}
