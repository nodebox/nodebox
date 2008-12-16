package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Group;
import net.nodebox.node.Parameter;
import net.nodebox.node.ProcessingContext;

public class RectNode extends VectorNode {

    private Parameter pX;
    private Parameter pY;
    private Parameter pWidth;
    private Parameter pHeight;
    private Parameter pFillColor;
    private Parameter pStrokeColor;
    private Parameter pStrokeWidth;

    public RectNode() {
        pX = addParameter("x", Parameter.Type.FLOAT);
        pY = addParameter("y", Parameter.Type.FLOAT);
        pWidth = addParameter("width", Parameter.Type.FLOAT);
        pWidth.setDefaultValue(100.0);
        pHeight = addParameter("height", Parameter.Type.FLOAT);
        pHeight.setDefaultValue(100.0);
        pFillColor = addParameter("fill", Parameter.Type.COLOR);
        pStrokeColor = addParameter("stroke", Parameter.Type.COLOR);
        pStrokeWidth = addParameter("strokewidth", Parameter.Type.FLOAT);
        pStrokeWidth.setMinimumValue(0.0);
    }

    @Override
    public String defaultName() {
        return "rect";
    }

    @Override
    protected boolean process(ProcessingContext ctx) {
        Group g = new Group();
        BezierPath p = new BezierPath();
        p.setFillColor(pFillColor.asColor());
        p.setStrokeColor(pStrokeColor.asColor());
        p.setStrokeWidth(pStrokeWidth.asFloat());
        p.addRect(pX.asFloat(), pY.asFloat(), pWidth.asFloat(), pHeight.asFloat());
        g.add(p);
        setOutputValue(g);
        return true;
    }
}
