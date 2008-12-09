package net.nodebox.node.canvas;

import net.nodebox.node.Network;
import net.nodebox.node.Parameter;
import net.nodebox.graphics.Canvas;

import java.util.Map;

public class CanvasNode extends Network {

    protected Map<String,Canvas> inputValues;
    protected Canvas outputValue;

    public CanvasNode() {
        super(Parameter.Type.CANVAS);
    }

    public CanvasNode(String name) {
        super(Parameter.Type.CANVAS, name);
    }
}
