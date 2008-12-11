package net.nodebox.node.canvas;

import net.nodebox.graphics.Canvas;
import net.nodebox.graphics.Grob;
import net.nodebox.node.Network;
import net.nodebox.node.Parameter;
import net.nodebox.node.ProcessingContext;

public class CanvasNetwork extends Network {

    private Parameter pWidth;
    private Parameter pHeight;

    public CanvasNetwork() {
        this(null);
    }

    public CanvasNetwork(String name) {
        super(Parameter.Type.GROB_CANVAS, name);
        pWidth = addParameter("width", Parameter.Type.FLOAT);
        pWidth.setDefaultValue(1000.0);
        pHeight = addParameter("height", Parameter.Type.FLOAT);
        pHeight.setDefaultValue(1000.0);
    }

    @Override
    protected boolean process(ProcessingContext ctx) {
        boolean success = updateRenderedNode(ctx);
        if (success) {
            Canvas c = new Canvas(pWidth.asFloat(), pHeight.asFloat());
            Object outputValue = getRenderedNode().getOutputValue();
            if (outputValue instanceof Grob) {
                c.add((Grob) outputValue);
            } else {
                throw new AssertionError(getName() + ": output of rendered node is not Grob, but " + outputValue);
            }
            setOutputValue(c);
        } else {
            getOutputParameter().revertToDefault();
        }
        return success;
    }
}
