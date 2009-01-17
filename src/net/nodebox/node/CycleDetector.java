package net.nodebox.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    private Set<Node> vertices;
    private Map<Node, Set<Node>> edges;
    private Map<Node, Color> marks;

    public CycleDetector(Network network) {
        // If this code ever gets changed to a per-parameter basis, don't forget to create edges from the input
        // parameters to the output parameter.
        vertices = new HashSet<Node>();
        edges = new HashMap<Node, Set<Node>>();
        marks = new HashMap<Node, Color>();
        vertices.addAll(network.getNodes());
        for (Connection connection : network.getConnections()) {
            Node inputNode = connection.getInputNode();
            for (Node outputNode : connection.getOutputNodes()) {
                // Connections to myself are allowed for implicit connections (expressions)
                if (inputNode == outputNode && connection.getType() == Connection.Type.IMPLICIT)
                    continue;
                Set<Node> edgesForNode = edges.get(inputNode);
                if (edgesForNode == null) {
                    edgesForNode = new HashSet<Node>();
                    edges.put(inputNode, edgesForNode);
                }
                edgesForNode.add(outputNode);
            }
        }
    }

    public boolean hasCycles() {
        for (Node vertex : vertices) {
            marks.put(vertex, Color.WHITE);
        }
        for (Node vertex : vertices) {
            if (marks.get(vertex) == Color.WHITE) {
                if (visit(vertex))
                    return true;
            }
        }
        return false;
    }

    private boolean visit(Node vertex) {
        marks.put(vertex, Color.GRAY);
        Set<Node> outputNodes = edges.get(vertex);
        if (outputNodes != null) {
            for (Node output : outputNodes) {
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
        marks.put(vertex, Color.BLACK);
        return false;
    }

}
