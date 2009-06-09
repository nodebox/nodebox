package nodebox.node;

import java.util.*;

/**
 * Nodes can have only one depency, but can have multiple dependents.
 * <p/>
 * TODO: Implement WeakReferences.
 *
 * @param <T> The type of nodes to store.
 * @param <I> The type of information to store.
 */
public class DependencyGraph<T, I> {

    /**
     * The list of all nodes in the graph
     */
    private Set<T> nodes = new HashSet<T>();

    /**
     * All edges, keyed by the output (or destination), and going downstream,
     * to the input (or origin).
     */
    private HashMap<T, Set<T>> downstreams = new HashMap<T, Set<T>>();

    /**
     * All edges, keyed by the input (or origin), and going upstream,
     * to the output (or destination).
     */
    private HashMap<T, Set<T>> upstreams = new HashMap<T, Set<T>>();

    /**
     * Extra information attached to a node in the graph.
     */
    private HashMap<T, I> nodeInfo = new HashMap<T, I>();

    /**
     * Colors are used for the cycle detector
     */
    private enum Color {
        WHITE, GRAY, BLACK
    }

    /**
     * Marks are used for the cycle detector.
     */
    private Map<T, Color> marks;


    public void addNode(T node) {
        nodes.add(node);
    }

    /**
     * Create a dependency between two nodes.
     *
     * @param dependency the upstream (output) node that the dependent node relies on
     * @param dependent  the downstream (input) node that is dependent on the dependency
     * @throws IllegalArgumentException if adding this dependency would create a cycle.
     */
    public void addDependency(T dependency, T dependent) throws IllegalArgumentException {
        if (dependency.equals(dependent)) {
            throw new IllegalArgumentException("The dependency '" + dependency + "' refers to itself.");
        }
        nodes.add(dependency);
        nodes.add(dependent);

        Set<T> dependencies = upstreams.get(dependent);
        if (dependencies == null) {
            dependencies = new HashSet<T>();
            upstreams.put(dependent, dependencies);
        }
        dependencies.add(dependency);

        Set<T> dependents = downstreams.get(dependency);
        if (dependents == null) {
            dependents = new HashSet<T>();
            downstreams.put(dependency, dependents);
        }
        dependents.add(dependent);

        // Check for cycles and remove the dependency if cycles were found.
        if (hasCycles()) {
            upstreams.remove(dependent);
            dependents.remove(dependent);
            if (dependents.size() == 0) {
                downstreams.remove(dependency);
            }
            throw new IllegalArgumentException("Adding a dependency from '" + dependent + "' to '" + dependency + "' would cause a cyclic dependency.");
        }
    }

    /**
     * Create a dependency between two nodes and add extra information.
     * This information is stored under the dependent node.
     *
     * @param dependency the upstream (output) node that the dependent node relies on
     * @param dependent  the downstream (input) node that is dependent on the dependency
     * @param info       extra information about the dependency
     * @throws IllegalArgumentException if adding this dependency would create a cycle.
     * @see #getInfo(Object)
     */
    public void addDependency(T dependency, T dependent, I info) throws IllegalArgumentException {
        addDependency(dependency, dependent);
        setInfo(dependent, info);
    }

    public boolean removeDependency(T dependency, T dependent) {
        Set<T> dependencies = upstreams.get(dependent);
        Set<T> dependents = downstreams.get(dependency);
        boolean removedSomething = false;
        if (dependencies != null)
            removedSomething = dependencies.remove(dependency);
        if (dependents != null)
            removedSomething = dependents.remove(dependency) | removedSomething;
        return removedSomething;
    }

    /**
     * Checks if the dependent (second argument) depends on the dependency (first argument)
     *
     * @param dependency the potential dependency
     * @param dependent  the potential dependent
     * @return true if the dependent relies on the value of the dependency.
     */
    public boolean hasDependency(T dependency, T dependent) {
        Set<T> dependencies = upstreams.get(dependent);
        return dependencies != null && dependencies.contains(dependency);
    }

    //// Dependency info ////

    /**
     * Get extra information about a node.
     * <p/>
     * This information needs to be set in advance, either using setInfo
     * or using addDependency with the info argument.
     *
     * @param node the downstream (input) node
     * @return the information, or null if no info is available.
     */
    public I getInfo(T node) {
        return nodeInfo.get(node);
    }

    /**
     * Sets extra information on the node.
     *
     * @param node the downstream (input) node
     * @param info the extra information.
     */
    public void setInfo(T node, I info) {
        nodeInfo.put(node, info);
    }

    /**
     * Remove extra information about this node.
     *
     * @param node the downstream (input) node
     */
    public void removeInfo(T node) {
        nodeInfo.remove(node);
    }

    public Set<I> getInfos() {
        Set<I> infos = new HashSet<I>(nodeInfo.size());
        infos.addAll(nodeInfo.values());
        return infos;
    }

    //// Utility methods ////

    public List<T> getTopNodes() {
        if (nodes.isEmpty()) return null;
        List<T> topNodes = new ArrayList<T>();
        // For all of the nodes, check if they have no dependencies.
        for (T node : nodes) {
            if (!upstreams.containsKey(node))
                topNodes.add(node);
        }
        return topNodes;
    }

    public Set<T> getDependents(T node) {
        Set<T> dependents = downstreams.get(node);
        if (dependents == null) {
            return new HashSet<T>(0);
        } else {
            return dependents;
        }

//        Set<Parameter> set = new HashSet<Parameter>(dependents.size());
//        for (WeakReference<Parameter> ref : dependents) {
//            Parameter p = ref.get();
//            if (p != null)
//                set.add(p);
//        }
//        return set;

    }

    public Set<T> getDependencies(T node) {
        Set<T> dependencies = upstreams.get(node);
        if (dependencies == null) {
            return new HashSet<T>(0);
        } else {
            return dependencies;
        }

//        Set<Parameter> set = new HashSet<Parameter>(dependencies.size());
//        for (WeakReference<Parameter> ref : dependencies) {
//            Parameter p = ref.get();
//            if (p != null)
//                set.add(p);
//        }
//        return set;

    }

    public boolean removeDependencies(T dependent) {
        Set<T> dependencies = upstreams.get(dependent);
        if (dependencies == null) return false;
        for (T dependency : dependencies) {
            Set<T> dependents = downstreams.get(dependency);
            dependents.remove(dependent);
        }
        upstreams.remove(dependent);
        // TODO: Check removeInfo(dependent);
        return true;
    }

    public boolean removeDependents(T dependency) {
        Set<T> dependents = downstreams.get(dependency);
        if (dependents == null) return false;
        for (T dependent : dependents) {
            Set<T> dependencies = upstreams.get(dependent);
            dependencies.remove(dependency);
            // TODO: Check removeInfo(dependent);
        }
        downstreams.remove(dependency);
        return true;
    }

    public Iterator<T> getBreadthFirstIterator() {
        return new GraphIterator(this);
    }

    /**
     * Find cycles in a Directed Acyclic Graph structure.
     * <p/>
     * hasCycles gets invoked each time a new dependency is established.
     *
     * @return true if this graph contains cycles.
     */
    private boolean hasCycles() {
        // The cycle detector stores its state in the marks map.
        // This map is nulled at the end of the method.
        marks = new HashMap<T, Color>(nodes.size());
        for (T node : nodes) {
            marks.put(node, Color.WHITE);
        }
        for (T node : nodes) {
            if (marks.get(node) == Color.WHITE) {
                if (visit(node)) {
                    marks = null;
                    return true;
                }
            }
        }
        marks = null;
        return false;
    }

    private boolean visit(T node) {
        marks.put(node, Color.GRAY);
        Set<T> outputNodes = downstreams.get(node);
        if (outputNodes != null) {
            for (T output : outputNodes) {
                if (!marks.containsKey(output)) continue;
                if (marks.get(output) == Color.GRAY) {
                    return true;
                } else if (marks.get(output) == Color.WHITE) {
                    if (visit(output))
                        return true;
                } else {
                    // Visiting black vertices is okay.
                }
            }
        }
        marks.put(node, Color.BLACK);
        return false;
    }

    public class GraphIterator implements Iterator {
        Queue<T> q = new LinkedList<T>();

        public GraphIterator(DependencyGraph<T, I> dg) {
            for (T node : dg.getTopNodes()) {
                q.add(node);
            }
        }

        public boolean hasNext() {
            return q.size() > 0;
        }

        public T next() {
            T node = q.remove();
            Set<T> children = downstreams.get(node);
            if (children != null) {
                for (T child : children) {
                    q.add(child);
                }
            }
            return node;
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove not supported.");
        }
    }
}
