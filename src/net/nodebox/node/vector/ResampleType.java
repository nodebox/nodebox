package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.Grob;
import net.nodebox.handle.DisplayPointsHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.ArrayList;

public class ResampleType extends VectorNodeType {

    public ResampleType(NodeTypeLibrary library) {
        super(library, "resample");
        setDescription("Creates a new set of points based on the original shape.");
        ParameterType pShape = addParameterType("shape", ParameterType.Type.GROB_VECTOR);
        ParameterType pPoints = addParameterType("points", ParameterType.Type.INT);
        pPoints.setDefaultValue(20);
        ParameterType pPerContour = addParameterType("perContour", ParameterType.Type.TOGGLE);
        pPerContour.setDefaultValue(1);
        ParameterType pEqualLengths = addParameterType("equalLengths", ParameterType.Type.TOGGLE);
        pEqualLengths.setDefaultValue(0);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        Grob shape = node.asGrob("shape").clone();
        int amount = node.asInt("points");

        for (Grob grob : shape.getChildren(BezierPath.class)) {
            BezierPath p = (BezierPath) grob;
            // We just want all the path settings, not the path data,
            // so copy and clear the path.
            BezierPath oldPath = p.clone();
            p.clear();

            boolean perContour = node.asBoolean("perContour");
            boolean equalLengths = node.asBoolean("equalLengths");
            if (!perContour) {
                p.extend(oldPath.getPoints(amount));
            } else {
                if (!equalLengths) {
                    for (BezierPath contour : oldPath.getContours()) {
                        p.extend(contour.getPoints(amount));
                    }
                } else {
                    java.util.List<Double> lengths = new ArrayList<Double>();
                    double totalLength = 0;
                    for (BezierPath contour : oldPath.getContours()) {
                        double length = contour.getLength();
                        lengths.add(length);
                        totalLength += length;
                    }
                    if (totalLength == 0)
                        totalLength = 0.001;
                    int i = 0;
                    for (BezierPath contour : oldPath.getContours()) {
                        int amountPerContour = (int) ((lengths.get(i) / totalLength) * amount);
                        p.extend(contour.getPoints(amountPerContour));
                        i++;
                    }
                }
            }
        }
        node.setOutputValue(shape);
        return true;
    }

    @Override
    public Handle createHandle(Node node) {
        return new DisplayPointsHandle(node);
    }
}
