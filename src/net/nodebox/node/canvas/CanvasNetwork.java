package net.nodebox.node.canvas;

import net.nodebox.graphics.Canvas;
import net.nodebox.node.Network;
import net.nodebox.node.Parameter;

import java.util.Map;

public class CanvasNetwork extends Network {

    protected Map<String, Canvas> inputValues;
    protected Canvas outputValue;

    public CanvasNetwork() {
        super(Parameter.Type.GROB_CANVAS);
    }

    public CanvasNetwork(String name) {
        super(Parameter.Type.GROB_CANVAS, name);
    }
}
