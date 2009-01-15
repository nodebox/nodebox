package net.nodebox.node.grob;

import net.nodebox.graphics.*;
import net.nodebox.handle.Handle;
import net.nodebox.handle.PointHandle;
import net.nodebox.node.*;

import java.util.ArrayList;
import java.util.List;

public class CopyType extends GrobNodeType {

    public CopyType(NodeTypeLibrary library) {
        super(library, "copy");
        setDescription("Creates copies of the input.");
        ParameterType pShape = addParameterType("shape", ParameterType.Type.GROB);
        ParameterType pTemplate = addParameterType("template", ParameterType.Type.GROB_PATH);
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
        ParameterType pExpression = addParameterType("expression", ParameterType.Type.STRING);
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
        Grob template = node.getParameter("template").isConnected() ? node.asGrob("template") : null;
        int copies = node.asInt("copies");
        double tx = node.asFloat("tx");
        double ty = node.asFloat("ty");
        double r = node.asFloat("r");
        double sx = node.asFloat("sx");
        double sy = node.asFloat("sy");
        String expression = node.asString("expression");
        Expression expressionObject = new Expression(expression, true);
        ProcessingContext copyContext = (ProcessingContext) ctx.clone();
        if (expression == null || expression.trim().length() == 0) {
            if (template == null) {  // copy source geometry according to transformation parameters
                doCopy(outputGroup, shape, 0, 0, copies, tx, ty, r, sx, sy);
            } else { // copy source geometry according to template
                // Go over each point in the template geometry, and put a copy of
                // the source geometry there.
                for (Point p : pointsForGrob(template)) {
                    doCopy(outputGroup, shape, p.getX(), p.getY(), copies, tx, ty, r, sx, sy);
                }
            }
        } else {
            // Expression set.
            // The expression allows you to modify field values on upstream
            // nodes while copying.
            //The expression has access to all the nodes by their name (e.g. rect1),
            //and can set values on them.
            //The CY local variable contains the copy number (starting from zero)
            Parameter pShape = node.getParameter("shape");
            if (!pShape.isConnected())
                throw new AssertionError("The shape is not connected.");
            List<Point> points;
            if (template == null) {
                points = new ArrayList<Point>();
                points.add(new Point());
            } else {
                points = pointsForGrob(template);
            }
            int copyIndex = 0;
            for (Point p : points) {
                Transform t = new Transform();
                t.translate(p.getX(), p.getY());
                for (int i = 0; i < copies; i++) {
                    Node upstreamNode = pShape.getExplicitConnection().getOutputNode();
                    Node copiedUpstreamNode = upstreamNode.getNetwork().copyNodeWithUpstream(upstreamNode);
                    // These expressions can mutate the values; that's sort of the point.
                    expressionObject.setParameter(copiedUpstreamNode.getOutputParameter());
                    // The expression object changes the node values, so I don't care about the output.
                    copyContext.put("COPY", copyIndex);
                    expressionObject.evaluate(copyContext);
                    // Now evaluate the output of the new upstream node.
                    copiedUpstreamNode.update(ctx);
                    if (copiedUpstreamNode.hasError())
                        throw new ProcessingError(node, "Upstream node contained errors:" + copiedUpstreamNode.getMessages().toString());
                    // We do not need to clone the output shape.
                    Grob outputShape = (Grob) copiedUpstreamNode.getOutputValue();
                    outputShape.prependTransform(t);
                    outputGroup.add(outputShape);
                    t.translate(tx, ty);
                    t.rotate(r);
                    t.scale(sx, sy);
                    copyIndex++;
                }
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
        for (int i = 0; i < copies; i++) {
            // Clone the input shape so we can change transformations on it.
            Grob newGrob = shape.clone();
            newGrob.prependTransform(t);
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

    //// Handle support ////

    /**
     * Creates and returns a Handle object that can be used for direct manipulation of the parameters of this node.
     * By default, this code returns null to indicate that no handle is available. Classes can override this method
     * to provide an appropriate handle implementation.
     *
     * @param node the node instance that is bound to this handle.
     * @return a handle instance bound to this node, or null.
     */
    @Override
    public Handle createHandle(Node node) {
        return new PointHandle(node, "tx", "ty");
    }
}