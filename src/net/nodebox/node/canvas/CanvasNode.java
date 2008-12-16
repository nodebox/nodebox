package net.nodebox.node.canvas;

import net.nodebox.node.Node;
import net.nodebox.node.Parameter;

public abstract class CanvasNode extends Node {

    public CanvasNode() {
        super(Parameter.Type.GROB_CANVAS);
    }

    public CanvasNode(String name) {
        super(Parameter.Type.GROB_CANVAS, name);
    }

}
