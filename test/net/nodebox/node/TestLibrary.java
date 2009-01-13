package net.nodebox.node;

import java.util.List;

public class TestLibrary extends CoreNodeTypeLibrary {

    public static abstract class Unary extends NodeType {

        public Unary(NodeTypeLibrary library, String name) {
            super(library, name, ParameterType.Type.INT);
            addParameterType("value", ParameterType.Type.INT);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(process(node.asInt("value")));
            return true;
        }

        public abstract int process(int value);

    }

    public static abstract class Binary extends NodeType {

        public Binary(NodeTypeLibrary library, String name) {
            super(library, name, ParameterType.Type.INT);
            addParameterType("v1", ParameterType.Type.INT);
            addParameterType("v2", ParameterType.Type.INT);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(process(node.asInt("v1"), node.asInt("v2")));
            return true;
        }

        public abstract int process(int v1, int v2);
    }

    public static abstract class Multi extends NodeType {
        public Multi(NodeTypeLibrary library, String name) {
            super(library, name, ParameterType.Type.INT);
            ParameterType ptValues = addParameterType("values", ParameterType.Type.INT);
            ptValues.setCardinality(ParameterType.Cardinality.MULTIPLE);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            List<Object> objectValues = node.getValues("values");
            int[] values = new int[objectValues.size()];
            for (int i = 0; i < objectValues.size(); i++) {
                values[i] = (Integer) objectValues.get(i);
            }
            node.setOutputValue(process(values));
            return true;
        }

        public abstract int process(int[] values);
    }

    public static class Number extends Unary {

        public Number(NodeTypeLibrary library) {
            super(library, "number");
        }

        public int process(int value) {
            return value;
        }
    }

    public static class Negate extends Unary {

        public Negate(NodeTypeLibrary library) {
            super(library, "negate");
        }

        public int process(int value) {
            return value;
        }
    }

    public static class Add extends Binary {
        public Add(NodeTypeLibrary library) {
            super(library, "add");
        }

        public int process(int v1, int v2) {
            return v1 + v2;
        }
    }

    public static class Multiply extends Binary {
        public Multiply(NodeTypeLibrary library) {
            super(library, "multiply");
            addParameterType("somestring", ParameterType.Type.STRING);
        }

        public int process(int v1, int v2) {
            return v1 * v2;
        }
    }

    public static class MultiAdd extends Multi {
        public MultiAdd(NodeTypeLibrary library) {
            super(library, "multiAdd");
        }

        public int process(int[] values) {
            int sum = 0;
            for (int v : values)
                sum += v;
            return sum;
        }
    }

    public static class TestNetworkType extends NodeType {
        public TestNetworkType(NodeTypeLibrary library) {
            super(library, "testnet", ParameterType.Type.INT);
        }

        @Override
        public boolean process(Node node, ProcessingContext ctx) {
            throw new RuntimeException("Image network is not implemented yet.");
        }

        @Override
        public Node createNode() {
            return new Network(this);
        }
    }

    public TestLibrary() {
        super("test", new Version(1, 0, 0));
        addNodeType(new Number(this));
        addNodeType(new Negate(this));
        addNodeType(new Add(this));
        addNodeType(new Multiply(this));
        addNodeType(new MultiAdd(this));
        addNodeType(new TestNetworkType(this));
    }

    @Override
    public boolean isLoaded() {
        return true;
    }
}
