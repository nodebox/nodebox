package net.nodebox.node.vector;

import net.nodebox.graphics.*;
import net.nodebox.node.Parameter;
import net.nodebox.node.ProcessingContext;

import java.util.ArrayList;
import java.util.List;

public class CopyNode extends VectorNode {

    private Parameter pShape, pTemplate, pCopies, pTx, pTy, pR, pSx, pSy;

    public CopyNode() {
        pShape = addParameter("shape", Parameter.Type.GROB_VECTOR);
        pTemplate = addParameter("template", Parameter.Type.GROB_VECTOR);
        pCopies = addParameter("copies", Parameter.Type.INT);
        pTx = addParameter("tx", Parameter.Type.FLOAT);
        pTy = addParameter("ty", Parameter.Type.FLOAT);
        pR = addParameter("r", Parameter.Type.FLOAT);
        pSx = addParameter("sx", Parameter.Type.FLOAT);
        pSy = addParameter("sy", Parameter.Type.FLOAT);
    }

    @Override
    public String defaultName() {
        return "copy";
    }

    /**
     * Extracts all points for this group, recursively.
     *
     * @param g the group
     * @return a list of points.
     */
    public List<Point> pointsForGrob(Grob g) {
        ArrayList<Point> points = new ArrayList<Point>();
        if (g instanceof Group) {
            for (Grob gg : ((Group) g).getGrobs()) {
                points.addAll(pointsForGrob(gg));
            }
        } else if (g instanceof BezierPath) {
            for (PathElement el : ((BezierPath) g).getElements()) {
                points.add(el.getPoint());
            }
        }
        return points;
    }

    protected boolean process(ProcessingContext ctx) {
        Group outputGroup = new Group();
        if (pTemplate.getValue() == null) {  // copy source geometry according to transformation parameters
            doCopy(outputGroup, 0, 0);
        } else { // copy source geometry according to template
            // Go over each point in the template geometry, and put a copy of
            // the source geometry there.
            for (Point p : pointsForGrob(pTemplate.asGrob())) {
                doCopy(outputGroup, p.getX(), p.getY());
            }
        }
        setOutputValue(outputGroup);
        return true;
    }

    private void doCopy(Group outputGroup, double tx, double ty) {
        // Set up the transform
        Transform t = new Transform();
        t.translate(tx, ty);

        // Loop through the number of copies
        for (int i = pCopies.asInt(); i > 0; i--) {

            // Clone the input shape so we can change transformations on it.
            Grob newGrob = pShape.asGrob().clone();
            newGrob.appendTransform(t);
            // Appending the input path to the new path.
            outputGroup.add(newGrob);

            // Do the additional transformations
            // This is done after appending the path, since the
            // first copy will come in place of the original
            // source geometry.
            t.translate(pTx.asFloat(), pTy.asFloat());
            t.rotate(pR.asFloat());
            t.scale(pSx.asFloat(), pSy.asFloat());
        }
    }

}