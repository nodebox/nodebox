package net.nodebox.node.vector;

import net.nodebox.graphics.Grob;
import net.nodebox.graphics.Transform;
import net.nodebox.node.Parameter;
import net.nodebox.node.ProcessingContext;

public class TransformNode extends VectorNode {

    private Parameter pShape;
    private Parameter pTx;
    private Parameter pTy;
    private Parameter pR;
    private Parameter pSx;
    private Parameter pSy;

    public TransformNode() {
        this(null);
    }

    public TransformNode(String name) {
        super(name);
        pShape = addParameter("shape", Parameter.Type.GROB_VECTOR);
        pTx = addParameter("tx", Parameter.Type.FLOAT);
        pTx.setLabel("Transform X");
        pTy = addParameter("ty", Parameter.Type.FLOAT);
        pTy.setLabel("Transform Y");
        pR = addParameter("r", Parameter.Type.FLOAT);
        pR.setLabel("Rotation");
        pSx = addParameter("sx", Parameter.Type.FLOAT);
        pSx.setLabel("Scale X");
        pSx.setDefaultValue(1.0);
        pSy = addParameter("sy", Parameter.Type.FLOAT);
        pSy.setLabel("Scale Y");
        pSy.setDefaultValue(1.0);
    }

    @Override
    public String defaultName() {
        return "transform";
    }

    @Override
    protected boolean process(ProcessingContext ctx) {
        Grob g = (Grob) pShape.getValue();
        Grob outGrob = g.clone();
        Transform t = new Transform();
        t.translate(pTx.asFloat(), pTy.asFloat());
        t.rotate(pR.asFloat());
        t.scale(pSx.asFloat(), pSy.asFloat());
        outGrob.appendTransform(t);
        setOutputValue(outGrob);
        return true;
    }

}
