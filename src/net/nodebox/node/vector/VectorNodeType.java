package net.nodebox.node.vector;

import net.nodebox.node.NodeManager;
import net.nodebox.node.NodeType;
import net.nodebox.node.ParameterType;


public abstract class VectorNodeType extends NodeType {

    public VectorNodeType(NodeManager manager, String identifier) {
        super(manager, identifier, ParameterType.Type.GROB_VECTOR);
    }

}

