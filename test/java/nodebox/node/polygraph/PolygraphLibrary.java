package nodebox.node.polygraph;

import nodebox.node.*;

public class PolygraphLibrary extends NodeLibrary {

    public PolygraphLibrary() {
        super("polygraph");
        addBuiltin(new PolygonBuiltin());
        addBuiltin(new RectBuiltin());
        addBuiltin(new TranslateBuiltin());
        addBuiltin(new MergeBuiltin());
        Node network = Node.ROOT_NODE.newInstance(this, "network", Polygon.class);
        network.setExported(true);
        add(network);
    }

    private void addBuiltin(Builtin builtin) {
        add(builtin.getInstance());
    }

    /**
     * Generate a polygon out of a simple description language.
     */
    public class PolygonBuiltin extends Builtin {
        public Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(PolygraphLibrary.this, "polygon", Polygon.class);
            n.setExported(true);
            n.addParameter("path", Parameter.Type.STRING);
            return n;
        }

        public Object cook(Node node, ProcessingContext ctx) {
            Polygon p = new Polygon();
            String pathString = node.asString("path");
            for (String pointString : pathString.split(" ")) {
                String[] coords = pointString.split(",");
                assert (coords.length == 2);
                float x = Float.parseFloat(coords[0]);
                float y = Float.parseFloat(coords[1]);
                p.addPoint(new Point(x, y));
            }
            return p;
        }
    }

    public class RectBuiltin extends Builtin {
        public Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(PolygraphLibrary.this, "rect", Polygon.class);
            n.setExported(true);
            n.addParameter("x", Parameter.Type.FLOAT, 0);
            n.addParameter("y", Parameter.Type.FLOAT, 0);
            n.addParameter("width", Parameter.Type.FLOAT, 100);
            n.addParameter("height", Parameter.Type.FLOAT, 100);
            return n;
        }

        public Object cook(Node node, ProcessingContext ctx) {
            float x = node.asFloat("x");
            float y = node.asFloat("y");
            float width = node.asFloat("width");
            float height = node.asFloat("height");
            return Polygon.rect(x, y, width, height);
        }
    }

    /**
     * Translate all the points of a polygon.
     */
    public class TranslateBuiltin extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(PolygraphLibrary.this, "translate", Polygon.class);
            n.setExported(true);
            n.addPort("polygon");
            n.addParameter("tx", Parameter.Type.FLOAT);
            n.addParameter("ty", Parameter.Type.FLOAT);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            float tx = node.asFloat("tx");
            float ty = node.asFloat("ty");
            Polygon p = (Polygon) node.getPortValue("polygon");
            return p.translated(tx, ty);
        }
    }

    /**
     * Merges a number of polygraph nodes together.
     */
    public class MergeBuiltin extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(PolygraphLibrary.this, "merge", Polygon.class);
            n.setExported(true);
            n.addPort("polygons", Port.Cardinality.MULTIPLE);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            Polygon merged = new Polygon();
            for (Object obj : node.getPortValues("polygons")) {
                Polygon p = (Polygon) obj;
                merged.extend(p);
            }
            return merged;
        }
    }

}
