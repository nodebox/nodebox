package nodebox.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import nodebox.function.FunctionLibrary;
import nodebox.function.FunctionRepository;

import java.io.File;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A cache of node render results that persists <i>across</i> renders.
 *
 * <p>NodeBox builds a fresh {@link NodeContext} for every render (frame change, parameter tweak, ...).
 * Historically that meant re-evaluating the entire rendered network from scratch every time. This
 * cache lets unchanged parts of the network be reused between renders.
 *
 * <h2>Why this is correct</h2>
 *
 * <p>{@link Node} and {@link NodeLibrary} are immutable, and editing a network reuses the unchanged
 * child nodes by reference (structural sharing, see {@code Node.withChildReplaced}). So a node's
 * output is a pure function of:
 * <ul>
 *   <li>the {@link Node} itself (which captures its function, its literal port values and — for a
 *       subnetwork — its entire subtree), and</li>
 *   <li>the outputs of the nodes connected to its inputs.</li>
 * </ul>
 * The cache key (see {@link Key}) is therefore the identity of the child node plus the identities of
 * the result lists feeding its connected input ports. When the user edits one node, that node and its
 * ancestors get fresh identities while its siblings keep theirs, so only the changed subgraph misses
 * the cache. No explicit dirty-tracking is needed.
 *
 * <h2>Purity</h2>
 *
 * <p>The reasoning above only holds for <i>pure</i> nodes. Impurity in NodeBox flows through
 * <i>context</i> ports (frame, mouse position, device input): a node reading the context can return a
 * different value on every render even though its node identity and connected inputs are unchanged.
 * Such nodes — and, transitively, any subnetwork that contains one — are excluded from the cache via
 * {@link #isCacheable(Node, FunctionRepository)}. So are always-rendered nodes, which exist for their
 * side effect, and functions that declare themselves time-dependent (see
 * {@link nodebox.function.Function#isTimeDependent()}): they read the clock, the network or a device,
 * or act on the outside world, without a context port. Every other function, including user code in
 * Python or Clojure, is assumed to be pure.
 *
 * <p>A node with a state port receives its <i>own output of the previous render</i>. That is not a
 * function of its inputs, so such a node is never cached; it behaves like a context node, and the
 * nodes upstream of it stay cached. Its previous output is kept here as well (see
 * {@link #stateSnapshot()}), so that state and cache share one lifetime.
 *
 * <p>Cached results belong to the functions they were computed with. When a library is added, removed or
 * reloaded after the user edited its code, the results are dropped (see {@link #useFunctions}). Nodes that
 * read files through file ports are cached with the modification time and size of those files in the key.
 *
 * <h2>Threading</h2>
 *
 * <p>A document renders on a single background worker at a time, so a cache instance is only touched by
 * one thread during a render. It is not safe for concurrent use; give separate render pipelines (e.g.
 * an export job) their own cache.
 */
public final class RenderCache {

    /**
     * Upper bound on the number of slots. A slot holds the latest result of one node path, so this only
     * matters when many nodes are deleted or renamed over a long session. Evicted least-recently-used.
     */
    private static final int MAX_SLOTS = 10000;

    // One slot per node path, holding only the latest key and result for that node. A result for older
    // inputs can never be hit again once an input changed (every frame, every drag step), so keeping it
    // would only grow memory: an animation would otherwise add one result per node per frame.
    private final LinkedHashMap<String, Slot> slots = new LinkedHashMap<String, Slot>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Slot> eldest) {
            return size() > MAX_SLOTS;
        }
    };

    // Memoizes transitive purity per node instance. Weak identity keys: purity is a pure function of the
    // (immutable) node, edited nodes are new instances, and old instances are dropped with the document
    // state that referenced them.
    private final Map<Node, Boolean> cacheable = new MapMaker().weakKeys().makeMap();

    // The function libraries the cached results were computed with, by identity, and the version of their
    // code at the time.
    private Map<FunctionLibrary, Long> libraryVersions = new IdentityHashMap<FunctionLibrary, Long>();

    // The latest output of each node with a state port, by node path. Unlike a cached result, this is
    // not derived data: it is the memory of a stateful node, and it lives as long as this cache (until
    // the document rewinds, or for the length of an export).
    private ImmutableMap<String, List<?>> state = ImmutableMap.of();

    /**
     * Prepare the cache for a render with the given functions. When the functions differ from the ones the
     * cached results were computed with (a library was added, removed or loaded again, or reloaded after the
     * user edited its code), those results are dropped. The state of stateful nodes is kept.
     */
    public void useFunctions(FunctionRepository functionRepository) {
        Map<FunctionLibrary, Long> versions = new IdentityHashMap<FunctionLibrary, Long>();
        for (FunctionLibrary library : functionRepository.getLibraries()) {
            versions.put(library, library.getVersion());
        }
        if (sameVersions(versions, libraryVersions)) return;
        slots.clear();
        cacheable.clear();
        libraryVersions = versions;
    }

    // IdentityHashMap.equals compares the values by reference too, which fails for boxed longs above 127.
    private static boolean sameVersions(Map<FunctionLibrary, Long> a, Map<FunctionLibrary, Long> b) {
        if (a.size() != b.size()) return false;
        for (Map.Entry<FunctionLibrary, Long> entry : a.entrySet()) {
            if (!entry.getValue().equals(b.get(entry.getKey()))) return false;
        }
        return true;
    }

    /**
     * Return the cached result for the node at {@code slot} (its path), if it was computed for this key.
     */
    public List<?> get(String slot, Key key) {
        Slot s = slots.get(slot);
        return s != null && s.key.equals(key) ? s.value : null;
    }

    public void put(String slot, Key key, List<?> value) {
        slots.put(slot, new Slot(key, value));
    }

    /**
     * The outputs of the stateful nodes as of now. A render reads this snapshot throughout, so a node
     * that runs several times in one render sees the output of the previous render each time.
     */
    public ImmutableMap<String, List<?>> stateSnapshot() {
        return state;
    }

    /** Replace the state with the outputs of the stateful nodes in a completed render, by node path. */
    public void setState(Map<String, List<?>> state) {
        this.state = ImmutableMap.copyOf(state);
    }

    int size() {
        return slots.size();
    }

    /**
     * Whether the given node's result may be cached across renders: true unless the node, or anything in
     * its subtree, reads the execution context, has a state port, is always rendered or runs a
     * time-dependent function. A network that contains a node reading files is not cacheable either.
     * The answer is memoized per node until the functions change.
     */
    public boolean isCacheable(Node node, FunctionRepository functionRepository) {
        Boolean known = cacheable.get(node);
        if (known != null) return known;
        boolean result = computeCacheable(node, functionRepository);
        cacheable.put(node, result);
        return result;
    }

    private boolean computeCacheable(Node node, FunctionRepository functionRepository) {
        // Always-rendered nodes exist for their side effect, which has to happen on every render.
        if (node.isAlwaysRendered()) return false;
        for (Port port : node.getInputs()) {
            String type = port.getType();
            if (type.equals(Port.TYPE_CONTEXT) || type.equals(Port.TYPE_STATE)) return false;
        }
        if (isTimeDependent(node.getFunction(), functionRepository)) return false;
        if (node.isNetwork()) {
            for (Node child : node.getChildren()) {
                if (!isCacheable(child, functionRepository)) return false;
                // A file node is cached with the state of its files in its key, but the key of the network
                // around it does not have them.
                if (hasFilePort(child)) return false;
            }
        }
        return true;
    }

    private static boolean hasFilePort(Node node) {
        for (Port port : node.getInputs()) {
            if (port.isFileWidget()) return true;
        }
        return false;
    }

    private static boolean isTimeDependent(String function, FunctionRepository functionRepository) {
        // An unknown function fails when the node renders; there is nothing to cache.
        return functionRepository.hasFunction(function) && functionRepository.getFunction(function).isTimeDependent();
    }

    /**
     * Build a cache key for a child node being rendered. {@code connectedInputs} holds, in input-port
     * order, the result list feeding each <i>connected</i> input port (literal port values are already
     * captured by the node's identity and are not included here). {@code files} holds the file names
     * the node reads through file ports; their modification time and size become part of the key, so a
     * file that changes on disk is read again.
     */
    public static Key key(Node node, List<List<?>> connectedInputs, List<String> files) {
        ImmutableList.Builder<String> stamps = ImmutableList.builder();
        for (String fileName : files) {
            File f = new File(fileName);
            stamps.add(fileName + " " + f.lastModified() + " " + f.length());
        }
        return new Key(node, connectedInputs, stamps.build());
    }

    private static final class Slot {
        private final Key key;
        private final List<?> value;

        private Slot(Key key, List<?> value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * An identity-based cache key. The node and the connected-input result lists are compared by
     * reference (==): the engine reuses the same node and result-list instances across renders for
     * unchanged subgraphs, so reference equality is both correct and cheap (no deep hashing of large
     * geometry lists). File stamps are compared by value.
     */
    public static final class Key {
        private final Node node;
        private final List<?>[] inputs;
        private final ImmutableList<String> fileStamps;
        private final int hash;

        private Key(Node node, List<List<?>> connectedInputs, ImmutableList<String> fileStamps) {
            this.node = node;
            this.inputs = connectedInputs.toArray(new List<?>[0]);
            this.fileStamps = fileStamps;
            int h = System.identityHashCode(node);
            for (List<?> input : inputs) {
                h = h * 31 + System.identityHashCode(input);
            }
            this.hash = h * 31 + fileStamps.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key other = (Key) o;
            if (node != other.node || inputs.length != other.inputs.length) return false;
            for (int i = 0; i < inputs.length; i++) {
                if (inputs[i] != other.inputs[i]) return false;
            }
            return fileStamps.equals(other.fileStamps);
        }
    }
}
