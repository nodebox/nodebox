package net.nodebox.node.canvas;

import net.nodebox.node.NodeManager;
import net.nodebox.node.NodeType;
import net.nodebox.node.ParameterType;

public abstract class CanvasNodeType extends NodeType {

    public CanvasNodeType(NodeManager manager, String identifier) {
        super(manager, identifier, ParameterType.Type.GROB_CANVAS);
    }

}
