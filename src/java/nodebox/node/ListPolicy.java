package nodebox.node;


/**
 * Describe how NodeBox handles lists.
 */
public enum ListPolicy {

    /**
     * If the node is list-aware, NodeBox does not handle individual elements of the list,
     * put passes it as-is to the function. The node function thus needs to accept and return lists of data.
     * <p/>
     * If the node is list-unaware, the other match strategies come into effect.
     */
    LIST_AWARE,

    /**
     * NodeBox executes the function multiple times, until the shortest list runs out of data.
     */
    SHORTEST_LIST,

    /**
     * NodeBox executes the function as many times as there are elements in the longest list.
     * Other (shorter) lists loop around, so if a function needs 5 values from the list [1, 2, 3], the node
     * context will pass n [1, 2, 3, 1, 2].
     */
    LONGEST_LIST,

    /**
     * NodeBox executes the function for each element in all the different input lists.
     * If the node has a port A with elements [1, 2] and port B with elements [3, 4], the node function will get
     * called for (1, 3), (1, 4), (2, 3) and (2, 4).
     * <p/>
     * Note that cross-referenced lists can become really large because all values of all lists are combined.
     */
    CROSS_REFERENCE;

    public final boolean isListAware() {
        return this == LIST_AWARE;
    }

}
