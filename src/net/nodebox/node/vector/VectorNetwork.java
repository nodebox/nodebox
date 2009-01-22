package net.nodebox.node.vector;

import net.nodebox.graphics.Grob;
import net.nodebox.node.Network;
import net.nodebox.node.NodeType;
import net.nodebox.node.ProcessingContext;

public class VectorNetwork extends Network {
    public VectorNetwork(NodeType nodeType) {
        super(nodeType);
    }

    @Override
    public boolean process(ProcessingContext ctx) {
        boolean success = updateRenderedNode(ctx);
        if (success) {
            Object outputValue = getRenderedNode().getOutputValue();
            if (outputValue instanceof Grob) {
                Grob g = (Grob) outputValue;
                g.translate(asFloat("tx"), asFloat("ty"));
                g.rotate(asFloat("r"));
                g.scale(asFloat("sx") / 100.0, asFloat("sy") / 100.0);
                setOutputValue(g);
            } else {
                throw new AssertionError(getAbsolutePath() + ": output of rendered node is not Grob, but " + outputValue);
            }
        } else {
            getOutputParameter().revertToDefault();
        }
        return success;
    }
}
