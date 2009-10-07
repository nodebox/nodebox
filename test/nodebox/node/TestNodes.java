package nodebox.node;

import java.util.List;

public class TestNodes extends NodeLibrary {

    public static final String LIBRARY_NAME = "testlib";

    public TestNodes() {
        super(LIBRARY_NAME);
        addBuiltin(new NumberIn());
        addBuiltin(new Negate());
        addBuiltin(new Add());
        addBuiltin(new AddDirect());
        addBuiltin(new AddConstant());
        addBuiltin(new Multiply());
        addBuiltin(new MultiAdd());
        addBuiltin(new FloatNegate());
        addBuiltin(new ConvertToUppercase());
        addBuiltin(new Crash());
        addBuiltin(new TestNetwork());
    }

    private void addBuiltin(Builtin builtin) {
        add(builtin.getInstance());
    }

    public class NumberIn extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "number", Integer.class);
            n.addParameter("value", Parameter.Type.INT);
            n.addPort("valuePort", Integer.class);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            if (node.getPort("valuePort").isConnected()) {
                return (Integer) node.getPortValue("valuePort");
            } else {
                return node.asInt("value");
            }
        }
    }

    public class Negate extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "negate", Integer.class);
            n.addPort("value", Integer.class);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            int value = (Integer) node.getPortValue("value");
            return -value;
        }
    }

    public class Add extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "add", Integer.class);
            n.addPort("v1", Integer.class);
            n.addPort("v2", Integer.class);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            int v1 = (Integer) node.getPortValue("v1");
            int v2 = (Integer) node.getPortValue("v2");
            return v1 + v2;
        }
    }

    public class AddDirect extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "addDirect", Integer.class);
            n.addParameter("v1", Parameter.Type.INT);
            n.addParameter("v2", Parameter.Type.INT);
            return n;
        }

        public Object cook(Node node, ProcessingContext ctx) {
            return node.asInt("v1") + node.asInt("v2");
        }
    }

    public class AddConstant extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "addConstant", Integer.class);
            n.addPort("value", Integer.class);
            n.addParameter("constant", Parameter.Type.INT);
            return n;
        }

        public Object cook(Node node, ProcessingContext ctx) {
            return ((Integer) node.getPortValue("value")) + node.asInt("constant");
        }
    }


    public class Multiply extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "multiply", Integer.class);
            n.addPort("v1", Integer.class);
            n.addPort("v2", Integer.class);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            int v1 = (Integer) node.getPortValue("v1");
            int v2 = (Integer) node.getPortValue("v2");
            return v1 * v2;
        }
    }

    public class MultiAdd extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "multiAdd", Integer.class);
            n.addPort("values", Integer.class, Port.Cardinality.MULTIPLE);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            List<Object> values = node.getPortValues("values");
            int sum = 0;
            for (Object obj : values) {
                int v = (Integer) obj;
                sum += v;
            }
            return sum;
        }
    }

    public class FloatNegate extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "floatNegate", Float.class);
            n.addPort("value", Float.TYPE);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            float value = (Float) node.getPortValue("value");
            return -value;
        }
    }

    public class ConvertToUppercase extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "convertToUppercase", String.class);
            n.addPort("value", String.class);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            String value = (String) node.getPortValue("value");
            return value.toUpperCase();
        }
    }

    public class Crash extends Builtin {
        protected Node createInstance() {
            Node n = Node.ROOT_NODE.newInstance(TestNodes.this, "crash", Integer.class);
            n.addPort("value", Integer.class);
            return n;
        }

        public Object cook(Node node, ProcessingContext context) {
            int a = 0;
            return 1 / a;
        }
    }

    public class TestNetwork extends Builtin {
        protected Node createInstance() {
            return Node.ROOT_NODE.newInstance(TestNodes.this, "testnet", Integer.class);
        }

        public Object cook(Node node, ProcessingContext context) {
            return node.cook(node, context);
        }
    }

//    public void addToManager(NodeLibraryManager m) {
//        m.addBuiltin(new NumberIn());
//        m.addBuiltin(new Negate());
//        m.addBuiltin(new Add());
//        m.addBuiltin(new AddDirect());
//        m.addBuiltin(new AddConstant());
//        m.addBuiltin(new Multiply());
//        m.addBuiltin(new MultiAdd());
//        m.addBuiltin(new FloatNegate());
//        m.addBuiltin(new ConvertToUppercase());
//        m.addBuiltin(new TestNetwork());
//        m.addBuiltin(new PolygonBuiltin());
//        m.addBuiltin(new RectBuiltin());
//        m.addBuiltin(new TranslateBuiltin());
//    }

}
