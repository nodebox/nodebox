package nodebox.node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The CycleDetector finds cycles in a Directed Acyclic Graph structure. The detector gets run each time a new
 * connection gets established, or other changes to the network are made that might cause cycles.
 * <p/>
 * Explicit connections can only create new cycles when a new connection is made. Implicit connections (expression
 * dependencies) are a bit trickier: setting or changing an expression might cause a cycle, but also, renaming a node
 * might cause an expression somewhere to evaluate differently, which could cause a cycle.
 * <p/>
 * The constructor excepts a list of vertices, and a dictionary of edges,
 * keyed by the vertex, and the value a list of input, or destination
 * vertices.
 * <p/>
 * The cycle detector keeps its own state and doesn't modify the vertices or edge dict.
 * <p/>
 * A Cycle detector might work on a per-node or per-parameter basis.
 * Per-node means that parameters within different nodes cannot refer back to eachother, e.g.
 * rect1.x -> ellipse1.x and ellipse1.y -> rect1.y would cause a cycle, because on a node level this connection would be
 * cyclic. On a per-parameter basis, the example would not cause a cycle. This implementation works on a per-node basis.
 */
public class CycleDetector {

    private enum Color {
        WHITE, GRAY, BLACK
    }

    private Collection<Connection> connections;
    private Map<Node, Color> marks;

    public CycleDetector(Collection<Connection> connections) {
        this.connections = connections;

    }

    public boolean hasCycles() {
        marks = new HashMap<Node, Color>(connections.size());
        for (Connection c : connections) {
            marks.put(c.getOutputNode(), Color.WHITE);
        }
        for (Connection c : connections) {
            if (marks.get(c.getOutputNode()) == Color.WHITE) {
                if (visit(c.getOutputNode())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean visit(Node node) {
        marks.put(node, Color.GRAY);
        for (Connection c : connections) {
            Node outputNode = c.getOutputNode();
            // Only use the ones I'm the input for (downstream connections for this node).
            if (c.getInputNode() != node) continue;
            if (!marks.containsKey(outputNode)) continue;
            if (marks.get(outputNode) == Color.GRAY) {
                return true;
            } else if (marks.get(outputNode) == Color.WHITE) {
                if (visit(outputNode)) {
                    return true;
                }
            } else {
                // Visiting black vertices is okay.
            }
        }
        marks.put(node, Color.BLACK);
        return false;
    }

}
