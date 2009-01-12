package net.nodebox.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The CycleDetector finds cycles in a Directed Acyclic Graph structure.
 * <p/>
 * Its initializer excepts a list of vertices, and a dictionary of edges,
 * keyed by the vertex, and the value a list of input, or destination
 * vertices.
 * <p/>
 * It keeps its own state and doesn't modify the vertices or edge dict.
 */
public class CycleDetector {

    private enum Color {
        WHITE, GRAY, BLACK
    }

    private List<Parameter> vertices;
    private Map<Parameter, List<Parameter>> edges;
    private Map<Parameter, Color> marks;

    public static CycleDetector initWithNetwork(Network network) {
        List<Node> nodes = network.getNodes();
        List<Parameter> outputParameters = new ArrayList<Parameter>();
        Map<Parameter, List<Parameter>> edges = new HashMap<Parameter, List<Parameter>>();
        for (Node node : nodes) {
            outputParameters.add(node.getOutputParameter());
            ArrayList<Parameter> parameters = new ArrayList<Parameter>();
            for (Connection connection : node.getOutputConnections()) {
                parameters.add(connection.getInputParameter());
            }
            edges.put(node.getOutputParameter(), parameters);
        }
        return new CycleDetector(outputParameters, edges);
    }

    public CycleDetector(List<Parameter> vertices, Map<Parameter, List<Parameter>> edges) {
        this.vertices = vertices;
        this.edges = edges;
        this.marks = new HashMap<Parameter, Color>();
    }

    public boolean hasCycles() {
        for (Parameter vertex : vertices) {
            marks.put(vertex, Color.WHITE);
        }
        for (Parameter vertex : vertices) {
            if (marks.get(vertex) == Color.WHITE) {
                if (visit(vertex))
                    return true;
            }
        }
        return false;
    }

    private boolean visit(Parameter vertex) {
        marks.put(vertex, Color.GRAY);
        for (Parameter input : edges.get(vertex)) {
            if (!marks.containsKey(input)) continue;
            if (marks.get(input) == Color.GRAY) {
                return true;
            } else if (marks.get(input) == Color.WHITE) {
                if (visit(input))
                    return true;
            }
        }
        marks.put(vertex, Color.BLACK);
        return false;
    }

}
