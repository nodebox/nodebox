package net.nodebox.node.vector;

import net.nodebox.graphics.Grob;
import net.nodebox.node.Network;
import net.nodebox.node.Parameter;
import net.nodebox.node.ProcessingContext;

public class VectorNetwork extends Network {

    private Parameter pTx;
    private Parameter pTy;
    private Parameter pSx;
    private Parameter pSy;

    @Override
    public String defaultName() {
        return "vecnet";
    }

    public VectorNetwork() {
        this(null);
    }

    public VectorNetwork(String name) {
        super(Parameter.Type.GROB_VECTOR, name);
        pTx = addParameter("tx", Parameter.Type.FLOAT);
        pTx.setLabel("Transform X");
        pTy = addParameter("ty", Parameter.Type.FLOAT);
        pTx.setLabel("Transform Y");
        pSx = addParameter("sx", Parameter.Type.FLOAT);
        pSx.setLabel("Scale X");
        pSx.setDefaultValue(1.0);
        pSy = addParameter("sy", Parameter.Type.FLOAT);
        pSy.setLabel("Scale Y");
        pSy.setDefaultValue(1.0);
    }

    @Override
    protected boolean process(ProcessingContext ctx) {
        boolean success = updateRenderedNode(ctx);
        if (success) {
            Object outputValue = getRenderedNode().getOutputValue();
            if (outputValue instanceof Grob) {
                Grob g = (Grob) outputValue;
                g.translate(pTx.asFloat(), pTy.asFloat());
                g.scale(pSx.asFloat(), pSy.asFloat());
                setOutputValue(g);
            } else {
                throw new AssertionError(getName() + ": output of rendered node is not Grob, but " + outputValue);
            }
        } else {
            getOutputParameter().revertToDefault();
        }
        return success;
    }
}
