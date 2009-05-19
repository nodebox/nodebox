package net.nodebox.node;

public abstract class Builtin implements NodeCode {

    public final Node getInstance() {
        Node n = createInstance();
        n.setValue("_code", this);
        return n;
    }

    protected abstract Node createInstance();

    /**
     * This method performs the actual processing of the node.
     * The result of this operation will be set as the node's outputValue.
     *
     * @param node    the node to process
     * @param context the processing context
     * @return the result of processing this node.
     */
    public abstract Object cook(Node node, ProcessingContext context);

    public String getSource() {
        return "<source not available>";
    }

    public String getType() {
        return "java";
    }
}
