package net.nodebox.node.vector;

import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ProcessingContext;

public class VectorMacroType extends VectorNodeType {

    // private VectorNetwork network;

    public VectorMacroType(NodeTypeLibrary library, String identifier) {
        super(library, identifier);
        /// network = new VectorNetwork(this);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        // network.update(ctx);
        return true;
    }
}
