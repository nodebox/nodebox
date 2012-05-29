package nodebox.node;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import nodebox.function.Function;
import nodebox.function.FunctionRepository;
import nodebox.graphics.IGeometry;
import nodebox.util.ListUtils;

import java.io.File;
import java.util.*;

import static com.google.common.base.Preconditions.*;

public class NodeContext {

    private final NodeLibrary nodeLibrary;
    private final FunctionRepository functionRepository;
    private final double frame;
    private final Map<Node, Iterable<?>> outputValuesMap = new HashMap<Node, Iterable<?>>();
    private final Map<NodePort, Iterable<?>> inputValuesMap = new HashMap<NodePort, Iterable<?>>();
    private final Set<Node> renderedNodes = new HashSet<Node>();


    public NodeContext(NodeLibrary nodeLibrary) {
        this(nodeLibrary, null, 1);
    }

    public NodeContext(NodeLibrary nodeLibrary, double frame) {
        this(nodeLibrary, null, frame);
    }

    public NodeContext(NodeLibrary nodeLibrary, FunctionRepository functionRepository, double frame) {
        checkNotNull(nodeLibrary);
        this.nodeLibrary = nodeLibrary;
        this.functionRepository = functionRepository != null ? functionRepository : nodeLibrary.getFunctionRepository();
        this.frame = frame;
    }

    public Map<Node, Iterable<?>> getResultsMap() {
        return outputValuesMap;
    }

    public Iterable<?> getResults(Node node) {
        return outputValuesMap.get(node);
    }

    /**
     * Render the network by rendering its rendered child.
     *
     * @param network The network to render.
     * @throws NodeRenderException If processing fails.
     */
    public void renderNetwork(Node network) throws NodeRenderException {
        checkNotNull(network);
        if (network.getRenderedChild() != null) {
            renderChild(network, network.getRenderedChild());
        }
    }

    /**
     * Render the child in the network.
     *
     * @param network The network to render.
     * @param child   The child node to render.
     * @return The list of rendered values.
     * @throws NodeRenderException If processing fails.
     */
    public Iterable<?> renderChild(Node network, Node child) throws NodeRenderException {
        checkNotNull(network);
        checkNotNull(child);
        checkArgument(network.hasChild(child));

        // Check if child was already rendered.
        if (renderedNodes.contains(child)) return outputValuesMap.get(child);
        renderedNodes.add(child);

        // Process dependencies
        for (Connection c : network.getConnections()) {
            if (Thread.currentThread().isInterrupted()) throw new NodeRenderException(child, "Interrupted");
            if (c.getInputNode().equals(child.getName())) {
                Node outputNode = network.getChild(c.getOutputNode());
                renderChild(network, outputNode);
                Iterable<?> result = convert(outputValuesMap.get(outputNode), child.getInput(c.getInputPort()).getType());
                // Check if the result is null. This can happen if there is a cycle in the network.
                if (result != null) {
                    inputValuesMap.put(NodePort.of(child, c.getInputPort()), result);
                }
            }
        }

        return renderNode(child);
    }

    private Iterable<?> convert(Iterable<?> outputValues, String inputType) {
        if (level(outputValues) == 0) {
            Class outputType = ListUtils.listClass(outputValues);
            // Convert IGeometry type to points
            if (inputType.equals(Port.TYPE_POINT) && IGeometry.class.isAssignableFrom(outputType)) {
                ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
                for (Object o : outputValues) {
                    b.addAll((Iterable<?>) ((IGeometry) o).getPoints());
                }
                return b.build();
            } else if (inputType.equals(Port.TYPE_INT) && outputType.equals(Double.class)) {
                ImmutableList.Builder<Object> b = new ImmutableList.Builder<Object>();
                for (Object o : outputValues) {
                    b.add(((Double) o).intValue());
                }
                return b.build();
            }
            return outputValues;
        } else {
            List list = new ArrayList();
            for (Object o : outputValues) {
                list.add(convert((Iterable) o, inputType));
            }
            return list;
        }
    }

    /**
     * Render a single node.
     * This doesn't evaluate child dependencies.
     * On the network, renderNetwork the renderedChild.
     * Note that we pass in the network, not the node to renderNetwork!
     * This is because we can't go up from the node to the network to retrieve the connections.
     *
     * @param node The node to render.
     * @return The list of rendered values.
     * @throws NodeRenderException If processing fails.
     */
    public Iterable<?> renderNode(Node node) throws NodeRenderException {
        checkNotNull(node);
        checkState(!outputValuesMap.containsKey(node), "Node %s already has a rendered value.", node);

        // If the node has children, forgo the operation of the current node and evaluate the child.
        if (node.hasRenderedChild()) {
            return renderChild(node, node.getRenderedChild());
        }

        // Get the function.
        String functionName = node.getFunction();
        Function function = functionRepository.getFunction(functionName);

        // Get the input values.
        ArrayList<ValueOrList> inputValues = new ArrayList<ValueOrList>();
        for (Port p : node.getInputs()) {
            NodePort np = NodePort.of(node, p);
            if (p.getType().equals("context")) {
                inputValues.add(ValueOrList.ofValue(this));
            } else if (inputValuesMap.containsKey(np)) {
                inputValues.add(ValueOrList.ofList(inputValuesMap.get(np)));
            } else {
                Object o = p.getValue();
                if (p.isFileWidget()) {
                    String path = (String) o;
                    if (!path.startsWith("/")) {
                        // Convert relative to absolute path.
                        if (nodeLibrary.getFile() != null) {
                            File f = new File(nodeLibrary.getFile().getParentFile(), (String) o);
                            o = f.getAbsolutePath();
                        }
                    }
                }
                inputValues.add(ValueOrList.ofValue(o));
            }
        }

        // Invoke the node function.
        Iterable<?> results = mapValues(node, function, inputValues);
        outputValuesMap.put(node, results);
        return results;
    }

    /**
     * Calculate the level of nesting for a given iterable.
     *
     * @param it The iterable to check
     * @return  The level of nesting
     */
    private int level(Iterable it) {
        if (it == null) return 0;
        Iterator<Object> iterator = it.iterator();
        if (! iterator.hasNext()) return 0;
        Object first = iterator.next();
        // We check if the structure implements List rather than Iterable,
        // because we might be interested in certain kind of iterables (for example a Path object).
        // Deconstructing those kind of iterables may not be what we want.
        if (first instanceof List) {
            return 1 + level((Iterable) first);
        }
        return 0;
    }

    /**
     * Calculate the level of nesting for a given input.
     * Only if the given value contains a list, a value higher could be returned.
     *
     * @param v The input port value
     * @return  The level of nesting
     */
    private int level(ValueOrList v) {
        if (!v.isList()) return 0;
        return level(v.getList());
    }

    /**
     * Calculate the expected level of nesting for the given set of input values.
     *
     * @param values A set of input values, either nested or unnested
     * @return       The nesting output level.
     */
    private int outputLevel(List<ValueOrList> values) {
        int sum = 0;
        int counter = 0;
        for (ValueOrList v : values) {
            sum += level(v);
            counter += 1;
        }
        return sum;
    }

    /**
     * Perform the function over all the sets of input values, returning a list or
     * combining the results in a list.
     * <p/>
     * If the input lists are of different length, stop processing after the shortest list.
     *
     * @param node        The node to render.
     * @param function    The node's function implementation.
     * @param inputValues A list of all values for the input ports.
     * @return The list of return values.
     */
    private Iterable<?> mapValues(final Node node, final Function function, List<ValueOrList> inputValues) {
        // If the node has no input ports, execute the node once for its side effects.
        if (node.getInputs().isEmpty()) {
            Object returnValue = invokeFunction(node, function, ImmutableList.of());
            if (returnValue != null) {
                return ImmutableList.of(returnValue);
            } else {
                return ImmutableList.of();
            }
        }

        List results;

        int l = outputLevel(inputValues);
        if (l == 0 || (l == 1 && node.hasListInputs())) {
            results = mapValuesInternal(node, inputValues, new FunctionInvoker() {
                public void call(List<Object> arguments, List<Object> results) {
                    Object returnValue = invokeFunction(node, function, arguments);
                    if (returnValue != null) {
                        results.add(returnValue);
                    }
                }
            });
        } else {
            results = mapNestedValues(node, function, inputValues);
        }

        if (results.isEmpty())
            return ImmutableList.of();
        else if (node.hasListOutputRange() && results.size() == 1)
            return (Iterable<?>) results.get(0);
        else
            return results;
    }


    /**
     * Recursively extracts the nested list(s) until suitable input sets are found to run
     * the node's function against. The results are then recursively built up and returned.
     *
     * @param node        The node to render.
     * @param function    The node's function implementation.
     * @param inputValues A list of all values for the input ports.
     * @return The nested list of return values.
     */
    private List mapNestedValues(Node node, Function function, List<ValueOrList> inputValues) {
        List results = new ArrayList();
        int i = 0;
        for (ValueOrList value : inputValues) {
            if (level(value) > 0) {
                for (Object o : value.getList()) {
                    ArrayList<ValueOrList> nestedInputValues = new ArrayList<ValueOrList>();
                    nestedInputValues.addAll(inputValues);
                    if (o instanceof Iterable)
                        nestedInputValues.set(i, ValueOrList.ofList((Iterable) o));
                    else
                        nestedInputValues.set(i, ValueOrList.ofValue(o));
                    List nestedResults = (List) mapValues(node, function, nestedInputValues);
                    if (nestedResults.size() == 1 && node.hasValueOutputRange() && node.hasListInputs()) {
                        // flatten the results when the output is a single result from input with one
                        // or more lists
                        results.add(nestedResults.get(0));
                    } else
                        results.add(nestedResults);
                }
                break;
            }
            i += 1;
        }
        return results;
    }

    /**
     * Do the actual mapping function. This uses a higher-order function "FunctionInvoker" that is free to execute
     * something with the arguments it gets and add to the results.
     *
     * @param node        The node to render.
     * @param inputValues A list of all values for the input ports.
     * @param op          The higher-order function that receives arguments and can manipulate the results.
     * @return The list of results.
     */
    private List<?> mapValuesInternal(Node node, List<ValueOrList> inputValues, FunctionInvoker op) {
        // If the node has no input ports, or if the minimum list size is zero, return an empty list.
        if (node.getInputs().isEmpty() || !hasElements(inputValues)) {
            return ImmutableList.of();
        }

        List<Object> results = new ArrayList<Object>();
        Map<ValueOrList, Iterator> iteratorMap = new HashMap<ValueOrList, Iterator>();
        boolean hasListArgument = false;

        ArrayList<ValueOrList> toExhaustList = new ArrayList<ValueOrList>();
        toExhaustList.addAll(inputValues);

        while (true) {
            if (Thread.currentThread().isInterrupted()) throw new NodeRenderException(node, "Interrupted.");
            // Collect arguments by going through the input values.
            List<Object> arguments = new ArrayList<Object>();
            for (int i = 0; i < inputValues.size(); i++) {
                ValueOrList v = inputValues.get(i);
                Port p = node.getInputs().get(i);

                if (v.isList()) {
                    if (p.hasListRange() && level(v) == 0) {
                        toExhaustList.remove(v);
                        arguments.add(v.getList());
                    } else {
                        // Store each iterator in the map.
                        if (!iteratorMap.containsKey(v)) {
                            iteratorMap.put(v, v.getList().iterator());
                        }
                        Iterator iterator = iteratorMap.get(v);
                        if (!iterator.hasNext()) {
                            toExhaustList.remove(v);
                            // End when the all lists are exhausted.
                            if (toExhaustList.isEmpty()) {
                                return results;
                            }
                            iterator = v.getList().iterator();
                            iteratorMap.put(v, iterator);
                        }
                        arguments.add(iterator.next());
                        hasListArgument = true;
                    }
                } else {
                    toExhaustList.remove(v);
                    arguments.add(v.getValue());
                }
            }
            // Invoke the function.
            op.call(arguments, results);
            // If none of the arguments are lists, we're done.
            if (!hasListArgument) break;
        }
        return results;
    }

    private Object invokeFunction(Node node, Function function, List<?> arguments) throws NodeRenderException {
        try {
            return function.invoke(arguments.toArray());
        } catch (Exception e) {
            throw new NodeRenderException(node, e);
        }
    }

    public double getFrame() {
        return frame;
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

    /**
     * This is used as the key for the inputValuesMap.
     */
    public static final class NodePort {
        private final Node node;
        private final Port port;

        public static NodePort of(Node node, Port port) {
            return new NodePort(node, port);
        }

        public static NodePort of(Node node, String portName) {
            return new NodePort(node, node.getInput(portName));
        }

        private NodePort(Node node, Port port) {
            checkNotNull(node);
            checkNotNull(port);
            this.node = node;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NodePort)) return false;
            final NodePort other = (NodePort) o;
            return Objects.equal(node, other.node)
                    && Objects.equal(port, other.port);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(node, port);
        }
    }

    /**
     * Higher-order function that receives a list of arguments to invoke a function with.
     * It can add something to the list of results, if it wants.
     */
    private interface FunctionInvoker {
        public void call(List<Object> arguments, List<Object> results);
    }
}
