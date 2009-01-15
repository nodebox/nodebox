package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Point;
import net.nodebox.graphics.Rect;
import net.nodebox.handle.DisplayPointsHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.Random;

public class ScatterType extends PathNodeType {

    public ScatterType(NodeTypeLibrary library) {
        super(library, "scatter");
        setDescription("Randomly distributes points inside the shape.");
        ParameterType pPath = addParameterType("path", ParameterType.Type.GROB_PATH);
        ParameterType pPoints = addParameterType("points", ParameterType.Type.INT);
        pPoints.setDefaultValue(20);
        ParameterType pSeed = addParameterType("seed", ParameterType.Type.SEED);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        BezierPath path = (BezierPath) node.asGrob("path");
        int amount = node.asInt("points");
        Random random = new Random(node.asInt("seed"));
        BezierPath newPath = new BezierPath();
        Rect bounds = path.getBounds();
        double bx = bounds.getX();
        double by = bounds.getY();
        double bw = bounds.getWidth();
        double bh = bounds.getHeight();
        Point pt = null;
        for (int i = 0; i < amount; i++) {
            int tries = 100;
            while (tries > 0) {
                pt = new Point(bx + random.nextDouble() * bw, by + random.nextDouble() * bh);
                if (path.contains(pt))
                    break;
                tries--;
            }
            if (tries == 0) {
                node.addWarning("Scatter: no points found on the path.");
            } else {
                newPath.moveto(pt);
            }
        }
        node.setOutputValue(newPath);
        return true;
    }

    @Override
    public Handle createHandle(Node node) {
        return new DisplayPointsHandle(node);
    }
}
