package net.nodebox.node.canvas;

import net.nodebox.graphics.Canvas;
import net.nodebox.graphics.Grob;
import net.nodebox.node.Network;
import net.nodebox.node.NodeType;
import net.nodebox.node.ProcessingContext;

public class CanvasNetwork extends Network {
    public CanvasNetwork(NodeType nodeType) {
        super(nodeType);
    }

    @Override
    public boolean process(ProcessingContext ctx) {
        boolean success = updateRenderedNode(ctx);
        if (success) {
            Canvas c = new Canvas(asFloat("width"), asFloat("height"));
            Object outputValue = getRenderedNode().getOutputValue();
            if (outputValue instanceof Grob) {
                Grob g = (Grob) outputValue;
                c.add(g);
                setOutputValue(c);
            } else {
                throw new AssertionError(getAbsolutePath() + ": output of rendered node is not Grob, but " + outputValue);
            }
        } else {
            getOutputParameter().revertToDefault();
        }
        return success;
    }
}
