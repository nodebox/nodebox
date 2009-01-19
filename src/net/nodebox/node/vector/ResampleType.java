package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.handle.DisplayPointsHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.ArrayList;

public class ResampleType extends PathNodeType {

    public ResampleType(NodeTypeLibrary library) {
        super(library, "resample");
        setDescription("Creates a new set of points based on the original shape.");
        ParameterType pPath = addParameterType("path", ParameterType.Type.GROB_PATH);
        ParameterType pPoints = addParameterType("points", ParameterType.Type.INT);
        pPoints.setDefaultValue(20);
        pPoints.setBoundingMethod(ParameterType.BoundingMethod.HARD);
        pPoints.setMinimumValue((double) 1);
        ParameterType pPerContour = addParameterType("perContour", ParameterType.Type.TOGGLE);
        pPerContour.setDefaultValue(1);
        ParameterType pEqualLengths = addParameterType("equalLengths", ParameterType.Type.TOGGLE);
        pEqualLengths.setDefaultValue(0);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        BezierPath path = (BezierPath) node.asGrob("path");
        int amount = node.asInt("points");
        BezierPath newPath = path.cloneAndClear();

        boolean perContour = node.asBoolean("perContour");
        boolean equalLengths = node.asBoolean("equalLengths");
        if (!perContour) {
            newPath.extend(path.getPoints(amount));
        } else {
            if (!equalLengths) {
                for (BezierPath contour : path.getContours()) {
                    newPath.extend(contour.getPoints(amount));
                }
            } else {
                java.util.List<Double> lengths = new ArrayList<Double>();
                double totalLength = 0;
                for (BezierPath contour : path.getContours()) {
                    double length = contour.getLength();
                    lengths.add(length);
                    totalLength += length;
                }
                if (totalLength == 0)
                    totalLength = 0.001;
                int i = 0;
                for (BezierPath contour : path.getContours()) {
                    int amountPerContour = (int) ((lengths.get(i) / totalLength) * amount);
                    newPath.extend(contour.getPoints(amountPerContour));
                    i++;
                }
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
