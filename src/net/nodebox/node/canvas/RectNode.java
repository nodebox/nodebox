package net.nodebox.node.canvas;

import net.nodebox.node.Parameter;
import net.nodebox.node.ProcessingContext;
import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Canvas;

public class RectNode extends CanvasNode {

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
        pHeight = addParameter("height", Parameter.Type.FLOAT);
        pFillColor = addParameter("fill", Parameter.Type.COLOR);
        pStrokeColor = addParameter("stroke", Parameter.Type.COLOR);
        pStrokeWidth = addParameter("strokewidth", Parameter.Type.FLOAT);
    }

    @Override
    public String defaultName() {
        return "rect";
    }

    @Override
    protected boolean process(ProcessingContext ctx) {
        Canvas c = new Canvas();
        BezierPath p = new BezierPath();
        p.rect(pX.asFloat(), pY.asFloat(), pWidth.asFloat(), pHeight.asFloat());
        c.append(p);
        // TODO: Set fill/stroke color
        outputValue = c;
        return true;
    }
}
