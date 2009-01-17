package net.nodebox.node.vector;

import net.nodebox.graphics.BezierPath;
import net.nodebox.graphics.PathElement;
import net.nodebox.graphics.Point;
import net.nodebox.handle.DisplayPointsHandle;
import net.nodebox.handle.Handle;
import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.ParameterType;
import net.nodebox.node.ProcessingContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class SortType extends PathNodeType {

    public SortType(NodeTypeLibrary library) {
        super(library, "sort");
        setDescription("Sorts the points on the object.");
        ParameterType pPath = addParameterType("path", ParameterType.Type.GROB_PATH);
        ParameterType pSort = addParameterType("sort", ParameterType.Type.MENU);
        pSort.setLabel("Sort order");
        pSort.addMenuItem("none", "No change");
        pSort.addMenuItem("x", "X value");
        pSort.addMenuItem("y", "Y value");
        pSort.addMenuItem("reverse", "Reverse");
        pSort.addMenuItem("random", "Random");
        pSort.addMenuItem("shift", "Shift");
        pSort.addMenuItem("proximity", "Proximity to point");
        pSort.addMenuItem("expression", "Expression");
        pSort.setDefaultValue("none");
        ParameterType pSeed = addParameterType("seed", ParameterType.Type.SEED);
        ParameterType pOffset = addParameterType("offset", ParameterType.Type.INT);
        ParameterType pX = addParameterType("x", ParameterType.Type.FLOAT);
        ParameterType pY = addParameterType("y", ParameterType.Type.FLOAT);
        ParameterType pExpression = addParameterType("expression", ParameterType.Type.STRING);
    }

    public boolean process(Node node, ProcessingContext ctx) {
        BezierPath path = (BezierPath) node.asGrob("path");
        String sort = node.asString("sort");
        // TODO: Work on a per-contour level
        // TODO: Control points are discarded
        ArrayList<Point> points = new ArrayList<Point>();
        for (PathElement el : path.getElements()) {
            if (el.getCommand() == PathElement.CLOSE) continue;
            points.add(el.getPoint());
        }
        if (sort.equals("none")) {
            // Do nothing
        } else if (sort.equals("x")) {
            Collections.sort(points, new XComparator());
        } else if (sort.equals("y")) {
            Collections.sort(points, new YComparator());
        } else if (sort.equals("reverse")) {
            Collections.reverse(points);
        } else if (sort.equals("random")) {
            Collections.shuffle(points, new Random(node.asInt("seed")));
        } else if (sort.equals("shift")) {
            int offset = node.asInt("offset");
            if (offset > 0) {
                for (int i = 0; i < offset; i++) {
                    Point pt = points.remove(0);
                    points.add(pt);
                }
            } else {
                offset = Math.abs(offset);
                int end = points.size() - 1;
                for (int i = 0; i < offset; i++) {
                    Point pt = points.remove(end);
                    points.add(0, pt);
                }
            }
        } else if (sort.equals("proximity")) {
            Collections.sort(points, new ProximityComparator(node.asFloat("x"), node.asFloat("y")));
        } else {
            throw new AssertionError("Unknown sort method " + sort);
        }
        path = path.cloneAndClear();
        path.extend(points);
        node.setOutputValue(path);
        return false;
    }

    @Override
    public Handle createHandle(Node node) {
        DisplayPointsHandle handle = new DisplayPointsHandle(node);
        handle.setDisplayPointNumbers(true);
        return handle;
    }

    public static class XComparator implements Comparator<Point> {
        public int compare(Point p1, Point p2) {
            if (p1.getX() > p2.getX()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public static class YComparator implements Comparator<Point> {
        public int compare(Point p1, Point p2) {
            if (p1.getY() > p2.getY()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    private class ProximityComparator implements Comparator<Point> {
        private double tx;
        private double ty;

        private ProximityComparator(double tx, double ty) {
            this.tx = tx;
            this.ty = ty;
        }

        public int compare(Point p1, Point p2) {
            double distP1 = Math.sqrt(Math.pow(tx - p1.getX(), 2) + Math.pow(ty - p1.getY(), 2));
            double distP2 = Math.sqrt(Math.pow(tx - p2.getX(), 2) + Math.pow(ty - p2.getY(), 2));
            if (distP1 > distP2) {
                return 1;
            } else if (distP1 == distP2) {
                return 0;
            } else
                return -1;
        }
    }
}
