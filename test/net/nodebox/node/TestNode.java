package net.nodebox.node;

public class TestNode extends Node {
    public TestNode() {
        super(Parameter.Type.INT);
    }

    protected boolean process(ProcessingContext ctx) {
        setOutputValue(42);
        return true;
    }
}