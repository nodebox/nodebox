package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Canvas;
import net.nodebox.node.Parameter;
import net.nodebox.node.ProcessingContext;

public class EllipseNode extends VectorNode {

    private Parameter pX;
    private Parameter pY;
    private Parameter pWidth;
    private Parameter pHeight;
    private Parameter pFillColor;
    private Parameter pStrokeColor;
    private Parameter pStrokeWidth;

    public EllipseNode() {
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
        return "ellipse";
    }

    @Override
    protected boolean process(ProcessingContext ctx) {
        Canvas c = new Canvas();
        BezierPath p = new BezierPath();
        p.setFillColor(pFillColor.asColor());
        p.setStrokeColor(pStrokeColor.asColor());
        p.setStrokeWidth(pStrokeWidth.asFloat());
        p.addEllipse(pX.asFloat(), pY.asFloat(), pWidth.asFloat(), pHeight.asFloat());
        c.add(p);
        setOutputValue(c);
        return true;
    }
}
