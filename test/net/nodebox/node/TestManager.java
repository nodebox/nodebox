package net.nodebox.node;

public class TestManager extends NodeManager {

    public static abstract class Unary extends NodeType {

        public Unary(NodeManager manager, String identifier) {
            super(manager, identifier, ParameterType.Type.INT);
            addParameterType("value", ParameterType.Type.INT);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(process(node.asInt("value")));
            return true;
        }

        public abstract int process(int value);

    }

    public static abstract class Binary extends NodeType {

        public Binary(NodeManager manager, String identifier) {
            super(manager, identifier, ParameterType.Type.INT);
            addParameterType("v1", ParameterType.Type.INT);
            addParameterType("v2", ParameterType.Type.INT);
        }

        public boolean process(Node node, ProcessingContext ctx) {
            node.setOutputValue(process(node.asInt("v1"), node.asInt("v2")));
            return true;
        }

        public abstract int process(int v1, int v2);
    }

    public static class Number extends Unary {

        public Number(NodeManager manager) {
            super(manager, "net.nodebox.node.test.number");
        }

        public int process(int value) {
            return value;
        }
    }

    public static class Negate extends Unary {

        public Negate(NodeManager manager) {
            super(manager, "net.nodebox.node.test.negate");
        }

        public int process(int value) {
            return value;
        }
    }

    public static class Add extends Binary {
        public Add(NodeManager manager) {
            super(manager, "net.nodebox.node.test.add");
        }

        public int process(int v1, int v2) {
            return v1 + v2;
        }
    }

    public static class Multiply extends Binary {
        public Multiply(NodeManager manager) {
            super(manager, "net.nodebox.node.test.multiply");
            addParameterType("somestring", ParameterType.Type.STRING);
        }

        public int process(int v1, int v2) {
            return v1 * v2;
        }
    }

    public static class TestNetworkType extends NodeType {
        public TestNetworkType(NodeManager manager) {
            super(manager, "net.nodebox.node.test.network", ParameterType.Type.INT);
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

    public TestManager() {
        addNodeType(new Number(this));
        addNodeType(new Negate(this));
        addNodeType(new Add(this));
        addNodeType(new Multiply(this));
        addNodeType(new TestNetworkType(this));
    }
}
