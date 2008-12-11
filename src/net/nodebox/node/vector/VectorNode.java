package net.nodebox.node.vector;

import net.nodebox.node.Node;
import net.nodebox.node.Parameter;


public abstract class VectorNode extends Node {

    public VectorNode() {
        super(Parameter.Type.GROB_VECTOR);
    }

    public VectorNode(String name) {
        super(Parameter.Type.GROB_VECTOR, name);
    }
}

