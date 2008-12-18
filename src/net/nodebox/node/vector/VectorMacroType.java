package net.nodebox.node.vector;

import net.nodebox.node.Node;
import net.nodebox.node.NodeManager;
import net.nodebox.node.ProcessingContext;

public class VectorMacroType extends VectorNodeType {

    // private VectorNetwork network;

    public VectorMacroType(NodeManager manager, String identifier) {
        super(manager, identifier);
        /// network = new VectorNetwork(this);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        // network.update(ctx);
        return true;
    }
}
