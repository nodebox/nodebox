package net.nodebox.node.vector;

import net.nodebox.graphics.*;
import net.nodebox.node.Node;
import net.nodebox.node.NodeManager;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.ArrayList;
import java.util.List;

public class CopyType extends VectorNodeType {

    public CopyType(NodeManager manager) {
        super(manager, "net.nodebox.node.vector.copy");
        ParameterType pShape = addParameterType("shape", ParameterType.Type.GROB_VECTOR);
        ParameterType pTemplate = addParameterType("template", ParameterType.Type.GROB_VECTOR);
        pTemplate.setNullAllowed(true);
        ParameterType pCopies = addParameterType("copies", ParameterType.Type.INT);
        pCopies.setDefaultValue(1);
        ParameterType pTx = addParameterType("tx", ParameterType.Type.FLOAT);
        ParameterType pTy = addParameterType("ty", ParameterType.Type.FLOAT);
        ParameterType pR = addParameterType("r", ParameterType.Type.FLOAT);
        ParameterType pSx = addParameterType("sx", ParameterType.Type.FLOAT);
        pSx.setDefaultValue(1.0);
        ParameterType pSy = addParameterType("sy", ParameterType.Type.FLOAT);
        pSy.setDefaultValue(1.0);
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

    public boolean process(Node node, ProcessingContext ctx) {
        Group outputGroup = new Group();
        Grob shape = node.asGrob("shape");
        Grob template = node.asGrob("template");
        int copies = node.asInt("copies");
        double tx = node.asFloat("tx");
        double ty = node.asFloat("ty");
        double r = node.asFloat("r");
        double sx = node.asFloat("sx");
        double sy = node.asFloat("sy");
        if (template == null) {  // copy source geometry according to transformation parameters
            doCopy(outputGroup, shape, 0, 0, copies, tx, ty, r, sx, sy);
        } else { // copy source geometry according to template
            // Go over each point in the template geometry, and put a copy of
            // the source geometry there.
            for (Point p : pointsForGrob(template)) {
                doCopy(outputGroup, shape, p.getX(), p.getY(), copies, tx, ty, r, sx, sy);
            }
        }
        node.setOutputValue(outputGroup);
        return true;
    }

    private void doCopy(Group outputGroup, Grob shape, double startx, double starty, int copies, double tx, double ty, double r, double sx, double sy) {
        // Set up the transform
        Transform t = new Transform();
        t.translate(startx, starty);

        // Loop through the number of copies
        for (int i = copies; i > 0; i--) {

            // Clone the input shape so we can change transformations on it.
            Grob newGrob = shape.clone();
            newGrob.appendTransform(t);
            // Appending the input path to the new path.
            outputGroup.add(newGrob);

            // Do the additional transformations
            // This is done after appending the path, since the
            // first copy will come in place of the original
            // source geometry.
            t.translate(tx, ty);
            t.rotate(r);
            t.scale(sx, sy);
        }
    }

}