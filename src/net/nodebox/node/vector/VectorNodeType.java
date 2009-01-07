package net.nodebox.node.vector;

import net.nodebox.node.NodeType;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;


public abstract class VectorNodeType extends NodeType {

    public VectorNodeType(NodeTypeLibrary library, String identifier) {
        super(library, identifier, ParameterType.Type.GROB_VECTOR);
    }

}

