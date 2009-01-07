package net.nodebox.node.canvas;

import net.nodebox.node.NodeType;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;

public abstract class CanvasNodeType extends NodeType {

    public CanvasNodeType(NodeTypeLibrary library, String identifier) {
        super(library, identifier, ParameterType.Type.GROB_CANVAS);
    }

}
